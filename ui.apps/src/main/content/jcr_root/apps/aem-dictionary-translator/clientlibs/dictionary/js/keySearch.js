(function(document, $) {
    "use strict";

    var SEARCH_FIELD = ".dictionary-translator-key-search";
    var CLEAR_BUTTON = ".dictionary-translator-key-search-clear";
    var COLLECTION = ".key-list";
    var PARAMETER = "q";
    // the term is rendered into the literal part of the table's src, see message-entries/.content.xml
    var PARAMETER_IN_SRC = new RegExp("([?&])" + PARAMETER + "=[^&{]*");
    var AUTO_SEARCH_MIN_LENGTH = 3;
    var AUTO_SEARCH_DELAY_MS = 500;

    var currentQuery = new URLSearchParams(window.location.search).get(PARAMETER) || "";
    var autoSearchTimeout;
    var reloading = false;

    function search(query) {
        clearTimeout(autoSearchTimeout);
        if (query === currentQuery) {
            return;
        }
        currentQuery = query;
        var params = new URLSearchParams(window.location.search);
        if (query) {
            params.set(PARAMETER, query);
        } else {
            params.delete(PARAMETER);
        }
        var queryString = params.toString();
        // keeps the search when the page is reloaded or shared, without adding a history entry per search
        window.history.replaceState(window.history.state, "",
                window.location.pathname + (queryString ? "?" + queryString : "") + window.location.hash);
        reloadCollection();
    }

    // reloads only the table instead of the whole page, so the search field keeps its focus
    function reloadCollection() {
        if (reloading) {
            return;
        }
        var collection = document.querySelector(COLLECTION);
        var query = currentQuery;
        collection.dataset.foundationCollectionSrc = collection.dataset.foundationCollectionSrc
                .replace(PARAMETER_IN_SRC, "$1" + PARAMETER + "=" + encodeURIComponent(query));
        reloading = true;
        $.when($(collection).adaptTo("foundation-collection").reload()).always(function() {
            reloading = false;
            // the term changed while the table was loading
            if (query !== currentQuery) {
                reloadCollection();
            }
        });
    }

    function updateClearButton() {
        $(CLEAR_BUTTON).prop("disabled", !$(SEARCH_FIELD).val());
    }

    $(document).one("foundation-contentloaded", function() {
        $(SEARCH_FIELD).val(currentQuery);
        updateClearButton();
    });

    $(document).on("input", SEARCH_FIELD, function(e) {
        var query = e.target.value.trim();
        updateClearButton();
        clearTimeout(autoSearchTimeout);
        // the minimum length only gates starting a search, an active one keeps following the field
        if (query.length >= AUTO_SEARCH_MIN_LENGTH || currentQuery) {
            autoSearchTimeout = setTimeout(function() {
                search(query);
            }, AUTO_SEARCH_DELAY_MS);
        }
    });

    $(document).on("keydown", SEARCH_FIELD, function(e) {
        if (e.key !== "Enter") {
            return;
        }
        e.preventDefault();
        search(e.target.value.trim());
    });

    $(document).on("click", CLEAR_BUTTON, function() {
        $(SEARCH_FIELD).val("");
        search("");
        updateClearButton();
        $(SEARCH_FIELD).trigger("focus");
    });

})(document, Granite.$);
