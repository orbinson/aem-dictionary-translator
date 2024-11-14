(function () {
    var REPLICATE_URL = Granite.HTTP.externalize("/bin/replicate");

    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "dictionary.publish",
        handler: function (name, el, config, collection, selections) {
            var message = createEl("div");
            var intro = createEl("p").appendTo(message);
            if (selections.length === 1) {
                intro.text(Granite.I18n.get("The item will be published"));
            } else {
                intro.text(Granite.I18n.get("{0} items will be published.", selections.length));
            }
            var publishPaths = getPublishPaths(selections);
            var ui = $(window).adaptTo("foundation-ui");
            ui.prompt("Publish", message.html(), "notice", [{
                text: "cancel"
            }, {
                text: "publish",
                primary: true,
                handler: function () {
                    activatePublishPaths(ui, collection, publishPaths)
                }
            }])
        }
    })

    function getPublishPaths(selections) {
        return selections.flatMap(selection => Array.from(
            selection.querySelectorAll('[data-publish-path]'))
            .map(element => element.getAttribute('data-publish-path'))
            .filter(value => value !== null)
        );
    }

    function createEl(name) {
        return $(document.createElement(name))
    }

    function activatePublishPaths(ui, collection, paths) {
        $.ajax({
            url: REPLICATE_URL,
            type: "POST",
            data: {
                _charset_: "utf-8",
                cmd: "Activate",
                path: paths,
                agentId: "publish"
            }
        }).always(function () {
            ui.clearWait()
        }).done(function () {
            var api = $(collection).adaptTo("foundation-collection");
            if (api && "reload" in api) {
                api.reload();
                ui.notify(null, getSuccessMessage(publishPaths));
                return
            }
            var contentApi = $(".foundation-content").adaptTo("foundation-content");
            if (contentApi)
                contentApi.refresh();
            ui.notify(null, getSuccessMessage(publishPaths))
        }).fail(function (xhr) {
            var title = Granite.I18n.get("Error");
            var message = Granite.I18n.getVar($(xhr.responseText).find("#Message").html());
            ui.alert(title, message, "error")
        })
    }

    function getSuccessMessage(paths) {
        var successMessage = Granite.I18n.get("{0} items have been published", paths.length);
        if (paths.length === 1)
            successMessage = Granite.I18n.get("The item has been published");
        return successMessage
    }
})();
