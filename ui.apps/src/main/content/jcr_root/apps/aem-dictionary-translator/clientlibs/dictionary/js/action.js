(function () {
    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "aem-dictionary-translator.dictionary.publish",
        handler: (name, element, config, collection, selections) => {
            const agentId = config.data?.agentId || "publish";
            const paths = selections
                .map(selection => selection.dataset.dictionaryPublishPaths.split(","))
                .flat();
            const recursive = selections[0].dataset.dictionaryPublishRecursive === "true";
            publishPaths(agentId, paths, selections.length, recursive);
        }
    });

    async function publishPaths(agentId, paths, size, recursive) {
        const ui = $(window).adaptTo("foundation-ui");
        const title = Granite.I18n.get("Publish dictionary");
        const message = Granite.I18n.get(`{0} item${size > 1 ? 's' : ''} will be published.`, size);

        ui.prompt(title, message, "notice", [
            {
                text: Granite.I18n.get("Publish"),
                primary: true,
                handler: async () => {
                    try {
                        if (recursive) {
                            await Promise.all(await replicateRecursive(agentId, paths[0]));
                        } else {
                            await Promise.all(await replicateBatched(agentId, paths));
                        }
                        ui.notify(null, Granite.I18n.get("Publication succeeded."), "success");
                    } catch (error) {
                        const title = Granite.I18n.get("Publication failed");
                        ui.alert(title, error.message, "error");
                    }
                }
            },
            {text: Granite.I18n.get("Cancel")},
        ]);
    }

    function query(path, limit, offset) {
        const params = new URLSearchParams({
            "path": path,
            "p.limit": limit,
            "p.offset": offset,
            "p.hits": "selective",
            "p.properties": "jcr:path",
        });
        const endpoint = Granite.HTTP.externalize("/bin/querybuilder.json");

        return fetch(`${endpoint}?${params.toString()}`)
            .then(response => response.json());
    }

    async function replicateRecursive(agentId, path, batchSize = 100) {
        let offset = 0;
        let total = null;
        let replications = [];

        do {
            const response = await query(path, batchSize, offset);
            total = total ?? response.total;

            const paths = response.hits.map(hit => hit["jcr:path"]);
            replications.push(replicate(agentId, paths));

            offset += batchSize;
        } while (offset < total);

        return replications;
    }

    async function replicateBatched(agentId, paths, batchSize = 100) {
        const replications = [];

        for (let i = 0; i < paths.length; i += batchSize) {
            const batch = paths.slice(i, i + batchSize);
            replications.push(replicate(agentId, batch));
        }

        return replications;
    }

    function replicate(agentId, paths) {
        return new Promise((resolve, reject) => {
            $.ajax({
                url: Granite.HTTP.externalize("/bin/replicate"),
                type: "POST",
                data: {
                    _charset_: "utf-8",
                    cmd: "Activate",
                    path: paths,
                    agentId: agentId,
                    batch: "true"
                }
            }).done((data) => {
                resolve();
            }).fail(xhr => {
                const message = $(xhr.responseText).find("#Message").html() || "Failed to publish dictionary.";
                reject(new Error(message));
            });
        });
    }
})();
