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
                        replicate(ui, collection, agentId, getPublishPaths(selections));
                    }
                }
            ]);
        }
    })

    function getPublishPaths(selections) {
        return selections.flatMap(selection =>
            Array.from(selection.querySelectorAll("[data-publish-path]"))
                .map(element => element.getAttribute("data-publish-path"))
                .filter(value => value !== null)
        );
    }

    function replicate(ui, collection, agentId, paths) {
        $.ajax({
            url: Granite.HTTP.externalize("/bin/replicate"),
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
