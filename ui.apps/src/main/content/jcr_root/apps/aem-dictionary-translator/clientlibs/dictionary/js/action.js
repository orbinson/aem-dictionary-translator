(function () {
    let REPLICATE_URL = Granite.HTTP.externalize("/bin/replicate");

    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "aem-dictionary-translator.dictionary.publish",
        handler: function (name, el, config, collection, selections) {
            let message = $(document.createElement("div"));
            let intro = $(document.createElement("p")).appendTo(message);
            if (selections.length === 1) {
                intro.text(Granite.I18n.get("The item will be published"));
            } else {
                intro.text(Granite.I18n.get("{0} items will be published.", selections.length));
            }
            let publishPaths = getPublishPaths(selections);
            let ui = $(window).adaptTo("foundation-ui");
            ui.prompt("Publish", message.html(), "notice", [{
                text: "cancel"
            }, {
                text: "publish",
                primary: true,
                handler: function () {
                    let agentId = config.data?.agentId || "";
                    activatePublishPaths(ui, collection, agentId, publishPaths);
                }
            }])
        }
    })

    function getPublishPaths(selections) {
        return selections.flatMap(selection =>
            Array.from(selection.querySelectorAll('[data-publish-path]'))
                .map(element => element.getAttribute('data-publish-path'))
                .filter(value => value !== null)
        );
    }

    function activatePublishPaths(ui, collection, agentId, paths) {
        $.ajax({
            url: REPLICATE_URL,
            type: "POST",
            data: {
                _charset_: "utf-8",
                cmd: "Activate",
                path: paths,
                agentId: agentId || "publish"
            }
        }).always(function () {
            ui.clearWait();
        }).done(function () {
            let api = $(collection).adaptTo("foundation-collection");
            if (api && "reload" in api) {
                // TODO: Throwing exception in 6.5
                // api.reload();
                ui.notify(null, getSuccessMessage(paths));
                return;
            }
            let contentApi = $(".foundation-content").adaptTo("foundation-content");
            if (contentApi) {
                contentApi.refresh();
            }
            ui.notify(null, getSuccessMessage(paths));
        }).fail(function (xhr) {
            let title = Granite.I18n.get("Error");
            let message = Granite.I18n.getVar($(xhr.responseText).find("#Message").html());
            ui.alert(title, message, "error");
        })
    }

    function getSuccessMessage(paths) {
        let successMessage = Granite.I18n.get("{0} items have been published", paths.length);
        if (paths.length === 1) {
            successMessage = Granite.I18n.get("The item has been published");
        }
        return successMessage;
    }
})();
