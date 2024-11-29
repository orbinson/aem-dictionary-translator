(function () {
    $(window).adaptTo("foundation-registry").register("foundation.collection.action.activecondition", {
        name: "aem-dictionary-translator.dictionary.editable",
        handler: function (name, el, config, collection, selections) {
            return $(selections[0]).data("foundation-collection-item-editable");
        }
    });

    $(window).adaptTo("foundation-registry").register("foundation.collection.action.activecondition", {
        name: "aem-dictionary-translator.dictionary.readonly",
        handler: function (name, el, config, collection, selections) {
            return !$(selections[0]).data("foundation-collection-item-editable");
        }
    });
})();
