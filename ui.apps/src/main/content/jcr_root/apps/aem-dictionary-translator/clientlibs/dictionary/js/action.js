(function () {
    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "aem-dictionary-translator.dictionary.publish",
        handler: (name, element, config, collection, selections) => {
            const agentId = config.data?.agentId || "publish";
            // is it dictionary items or dictionaries?
            let paths;
            let isDictionaryEntries;
            if (selections[0].dataset.dictionaryEntryPublishPaths) {
                isDictionaryEntries = true;
                paths = selections.map(selection => selection.dataset.dictionaryEntryPublishPaths.split(",")).flat();
            } else {
                isDictionaryEntries = false;
                paths = selections.map(selection => selection.dataset.dictionaryPublishPaths.split(",")).flat();
            }
            publishPaths(agentId, paths, selections.length, isDictionaryEntries);
        }
    });

    async function publishPaths(agentId, paths, size, isDictionaryEntries) {
        const ui = $(window).adaptTo("foundation-ui");
        const title = Granite.I18n.get(`Publish dictionary${isDictionaryEntries === true ? '  entries' : ''}`);
        const message = Granite.I18n.get(`{0} item${size > 1 ? 's' : ''} will be published.`, size);

        ui.prompt(title, message, "notice", [
            {
                text: Granite.I18n.get("Publish"),
                primary: true,
                handler: async () => {
                    try {
                        if (isDictionaryEntries) {
                            await Promise.all(await replicateBatchedDictionaryEntries(agentId, paths));
                        } else {
                            await Promise.all(await replicateDictionaries(agentId, paths));
                        }
                        ui.notify(null, Granite.I18n.get("Publication succeeded."), "success");
                    } catch (error) {
                        console.error(error, error.stack);
                        const title = Granite.I18n.get("Publication failed");
                        ui.alert(title, error.message, "error");
                    }
                }
            },
            {text: Granite.I18n.get("Cancel")},
        ]);
    }

    async function replicateDictionaries(agentId, paths) {
        let replications = [];
        replications.push(replicate(agentId, paths, false));
        return replications;
    }

    async function replicateBatchedDictionaryEntries(agentId, paths, batchSize = 100) {
        const replications = [];

        for (let i = 0; i < paths.length; i += batchSize) {
            const batch = paths.slice(i, i + batchSize);
            replications.push(replicate(agentId, batch, true));
        }

        return replications;
    }

    function replicate(agentId, paths, isDictionaryEntries) {
        let url;
        let data;
        if (isDictionaryEntries) {
            url = Granite.HTTP.externalize("/bin/replicate");
            data = {
                _charset_: "utf-8",
                cmd: "Activate",
                path: paths,
                agentId: agentId,
                batch: "true"
            }
        } else {
            url = Granite.HTTP.externalize("/apps/aem-dictionary-translator/content/granite/dialog/dictionary/replicate");
            data = {
                _charset_: "utf-8",
                action: "Activate",
                parentPath: paths,
                agentId: agentId
            }
        }
        return new Promise((resolve, reject) => {
            $.ajax({
                url: url,
                type: "POST",
                data: data
            }).done((data) => {
                resolve();
            }).fail(xhr => {
                const message = $(xhr.responseText).find("#Message").html() || "Failed to publish dictionary items.";
                reject(new Error(message));
            });
        });
    }
    
})();
