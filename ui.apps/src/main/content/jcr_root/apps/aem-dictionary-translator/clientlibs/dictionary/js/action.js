(function () {
    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "aem-dictionary-translator.dictionary.publish",
        handler: function (name, el, config, collection, selections) {
            let ui = $(window).adaptTo("foundation-ui");
            let title = Granite.I18n.get("Publish dictionary");
            let message = Granite.I18n.get(`{0} item${selections.length > 1 ? 's' : ''} will be published.`, selections.length);

            ui.prompt(title, message, "notice", [
                {
                    text: Granite.I18n.get("Cancel")
                },
                {
                    text: Granite.I18n.get("Publish"),
                    primary: true,
                    handler: function () {
                        let agentId = config.data?.agentId || "";
                        replicateRecursive(ui, collection, agentId, "/content/dictionaries/fruit");
                    }
                }
            ]);
        }
    });

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

    async function replicateRecursive(ui, collection, agentId, path, batchSize = 100) {
        let offset = 0;
        let total = null;

        do {
            const response = await query(path, batchSize, offset);
            total = total ?? response.total;

            const paths = response.hits.map(hit => hit["jcr:path"]);
            replicate(ui, collection, agentId, paths);

            offset += batchSize;
        } while (offset < total);
    }

    function replicate(ui, collection, agentId, paths) {
        $.ajax({
            url: Granite.HTTP.externalize("/bin/replicate"),
            type: "POST",
            data: {
                _charset_: "utf-8",
                cmd: "Activate",
                path: paths,
                agentId: agentId || "publish",
                batch: "true"
            }
        }).always(function () {
            ui.clearWait();
        }).done(function () {
            let contentApi = $(".foundation-content").adaptTo("foundation-content");
            if (contentApi) {
                contentApi.refresh();
            }
            ui.notify(null, Granite.I18n.get(`{0} item${paths.length > 1 ? 's' : ''} have been published`, paths.length), "success");
        }).fail(function (xhr) {
            let title = Granite.I18n.get("Publication of dictionary failed");
            let message = Granite.I18n.getVar($(xhr.responseText).find("#Message").html());
            ui.alert(title, message, "error");
        })
    }
})();
