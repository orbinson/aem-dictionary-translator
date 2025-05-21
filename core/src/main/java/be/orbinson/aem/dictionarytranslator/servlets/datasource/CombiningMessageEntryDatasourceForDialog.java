package be.orbinson.aem.dictionarytranslator.servlets.datasource;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.EmptyDataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.i18n.I18n;

import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider;
import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider.ValidationMessage;

/**
 * This data source is used to populate a dialog allowing to maintain translations for a single key in multiple languages
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "aem-dictionary-translator/datasource/combining-message-entry-for-dialog",
        methods = "GET"
)
public class CombiningMessageEntryDatasourceForDialog extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    public static final String FIELD_LABEL = "fieldLabel";
    private static final Logger LOG = LoggerFactory.getLogger(CombiningMessageEntryDatasourceForDialog.class);

    private static Resource createTextFieldResource(ResourceResolver resourceResolver, String label, String name, String value) {
        return createTextFieldResource(resourceResolver, label, name, value, false, false);
    }

    private static Resource createTextFieldResource(ResourceResolver resourceResolver, String label, String name, String value, boolean required, boolean disabled) {
        return createTextFieldResource(resourceResolver, "", label, name, value, required, disabled);
    }
    private static Resource createTextFieldResource(ResourceResolver resourceResolver, String path, String label, String name, String value, boolean required, boolean disabled) {
        ValueMap valueMap = new ValueMapDecorator(Map.of(
                FIELD_LABEL, label,
                "name", name,
                "value", value,
                "disabled", disabled,
                "required", required)
        );
        return new ValueMapResource(resourceResolver, path, "granite/ui/components/coral/foundation/form/textfield", valueMap);
    }

    private static Resource createHiddenFieldResource(ResourceResolver resourceResolver, String key, String value) {
        ValueMap valueMap = new ValueMapDecorator(Map.of(
                FIELD_LABEL, key,
                "name", key,
                "value", value)
        );
        return new ValueMapResource(resourceResolver, "", "granite/ui/components/coral/foundation/form/hidden", valueMap);
    }

    private static Resource createAlertResource(ResourceResolver resourceResolver, String path, String title, String text, String variant) {
        ValueMap valueMap = new ValueMapDecorator(Map.of(
                JcrConstants.JCR_TITLE, title,
                "text", text,
                "variant", variant
        ));
        return new ValueMapResource(resourceResolver, path, "granite/ui/components/coral/foundation/alert", valueMap);
    }

    private static Resource createFieldSetResource(ResourceResolver resourceResolver, String path, Collection<Resource> childResources) {
        Resource items = new ValueMapResource(resourceResolver, path+"/items", "nt:unstructured", null, childResources);
        return new ValueMapResource(resourceResolver, path, "granite/ui/components/coral/foundation/form/fieldset", null, Collections.singleton(items));
    }

    private static Resource addValidationMessagesResource(I18n i18n, ResourceResolver resourceResolver, Map<String, String> languageMap, ValidationMessage... validationMessages) {
        if (validationMessages == null || validationMessages.length == 0) {
            return null;
        }
        List<Resource> validationResources = new ArrayList<>();
        int index = 0;
        String fieldSetPath = "/dialog/validation"; // artifical path to avoid collision with other resources
        for (ValidationMessage validationMessage : validationMessages) {
            String textFieldPath = fieldSetPath + "/items/item" + index++;
            String label = new StringBuilder().append(i18n.get("Language")).append(" ").append(languageMap.getOrDefault(validationMessage.getLanguage(), validationMessage.getLanguage())).toString();
            String text =  i18n.get(validationMessage.getI18nKey(), null, (Object[])validationMessage.getArguments());
            validationResources.add(createAlertResource(resourceResolver, textFieldPath, label, text, validationMessage.getSeverity().name().toLowerCase(Locale.ENGLISH)));
        }
        return createFieldSetResource(resourceResolver, fieldSetPath, validationResources);
    }

    private static void sortResourcesByProperty(String propertyName, Locale locale, List<Resource> resources) {
        Collator collator = Collator.getInstance(locale);
        resources.sort((o1, o2) -> {
            ValueMap properties1 = o1.getValueMap();
            ValueMap properties2 = o2.getValueMap();
            return collator.compare(properties1.get(propertyName, ""), properties2.get(propertyName, ""));
        });
    }

    private static void createCombiningMessageEntryDataSource(I18n i18n, Locale locale, Map<String, String> languageMap, ResourceResolver resourceResolver, String combiningMessageEntryPath, List<Resource> resourceList) {
        Resource combiningMessageEntryResource = resourceResolver.getResource(combiningMessageEntryPath);
        if (combiningMessageEntryResource != null) {
            ValueMap properties = combiningMessageEntryResource.getValueMap();
            String[] languages = properties.get(CombiningMessageEntryResourceProvider.LANGUAGES, String[].class);
            String key = properties.get(CombiningMessageEntryResourceProvider.KEY, String.class);

            if (languages != null) {
                for (String language : languages) {
                    String message = properties.get(language, StringUtils.EMPTY);
                    String label = languageMap.getOrDefault(language, language);
                    resourceList.add(createTextFieldResource(resourceResolver, label, language, message));
                }
                // sort by fieldLabel
                sortResourcesByProperty(FIELD_LABEL, locale, resourceList);
            }
            // make sure that key is always at the top
            resourceList.add(0, createTextFieldResource(resourceResolver, "Key", key, key, false, true));
            resourceList.add(1, createHiddenFieldResource(resourceResolver, "key", key));
            Resource validationContainer = addValidationMessagesResource(i18n, resourceResolver, languageMap, properties.get(CombiningMessageEntryResourceProvider.VALIDATION_MESSAGES, ValidationMessage[].class));
            if (validationContainer != null) {
                resourceList.add(0, validationContainer);
            }
        }
    }

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        List<Resource> resourceList = new ArrayList<>();
        ResourceResolver resourceResolver = request.getResourceResolver();
        final DataSource dataSource;
        // expose only data for one item or
        String combiningMessageEntryPath = request.getParameter("item");
        if (combiningMessageEntryPath != null) {
            createCombiningMessageEntryDataSource(new I18n(request), request.getLocale(), LanguageDatasource.getAllAvailableLanguages(request, response), resourceResolver, combiningMessageEntryPath, resourceList);
            dataSource = new SimpleDataSource(resourceList.iterator());
        } else {
            LOG.error("No item parameter found in request");
            dataSource = EmptyDataSource.instance();
        }
        request.setAttribute(DataSource.class.getName(), dataSource);
    }
}
