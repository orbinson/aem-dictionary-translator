/** Similar to https://github.com/Adobe-Consulting-Services/acs-aem-commons/blob/master/ui.apps/src/main/content/jcr_root/apps/acs-commons/touchui-widgets/showhidedialogfields/source/showhidedialogfieldstabs.js */
(function(document, $) {
    "use strict";

    // set during dialog initialization
    $(document).on("foundation-contentloaded", function() {
            // if there is already an inital value make sure the according target element becomes visible
         $("[data-show-checkbox-with-name-when-empty]").each(function(i, element) {
            showHideCheckboxForElement(element)
         });
    });

    // for each input change event on translations
    $(document).on("input", "[data-show-checkbox-with-name-when-empty]", function(e) {
        showHideCheckboxForElement(e.target, e);
    });

    function showHideCheckboxForElement(element) {
        var name = element.dataset.showCheckboxWithNameWhenEmpty;
        var value = element.value;
        if (value.length === 0) {
            showHideCheckbox(name, false);
        } else {
            showHideCheckbox(name, true);
        }
    }

    function showHideCheckbox(name, isHide) {
        var checkbox = $('coral-checkbox[name="' + name + '"]')[0];
        Coral.commons.ready(checkbox, function() {
            if (isHide) {
                checkbox.hide();// this does not hide the description icon + hover, therefore only works without description
                // always uncheck it if hidden
                checkbox.set("checked", false, false);
            } else {
                checkbox.show();
            }
        });
    }

})(document,Granite.$);