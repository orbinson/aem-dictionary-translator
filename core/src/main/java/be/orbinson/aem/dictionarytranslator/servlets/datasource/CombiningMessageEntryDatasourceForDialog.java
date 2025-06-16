package be.orbinson.aem.dictionarytranslator.servlets.datasource;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.EmptyDataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.day.cq.i18n.I18n;

import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider;
import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider.ValidationMessage;
import be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.ValueMapResourceBuilderFactory;
import be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.graniteui.ComponentValueMapResourceBuilder;
import be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.graniteui.ComponentValueMapResourceBuilder.AlertVariant;
import be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.graniteui.ContainerValueMapResourceBuilder;
import be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.graniteui.FieldValueMapResourceBuilder;

/**
 * This data source is used to populate a dialog allowing to maintain translations for a single key in multiple languages
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "aem-dictionary-translator/datasource/combining-message-entry-for-dialog",
        methods = "GET"
)
public class CombiningMessageEntryDatasourceForDialog extends SlingSafeMethodsServlet {

    public static final String SUFFIX_USE_EMPTY = "_useEmpty";
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(CombiningMessageEntryDatasourceForDialog.class);

    private static Resource addValidationMessagesResource(I18n i18n, ResourceResolver resourceResolver, Map<Locale, String> languageMap, ValidationMessage... validationMessages) {
        if (validationMessages == null || validationMessages.length == 0) {
            return null;
        }
        int index = 0;
        // artifical path to avoid collision with other resources
        ValueMapResourceBuilderFactory factory = new ValueMapResourceBuilderFactory(resourceResolver,  "/dialog");
        ContainerValueMapResourceBuilder<?> fieldSet = ContainerValueMapResourceBuilder.forFieldSet(factory, "validation");
        for (ValidationMessage validationMessage : validationMessages) {
            String label = new StringBuilder().append(i18n.get("Language")).append(" ").append(languageMap.getOrDefault(validationMessage.getLanguage(), validationMessage.getLanguage().toLanguageTag())).toString();
            String text =  i18n.get(validationMessage.getI18nKey(), null, (Object[])validationMessage.getArguments());
            fieldSet.withItem(ComponentValueMapResourceBuilder.forAlert(fieldSet.getItemFactory(), "item" + index, label, text, AlertVariant.valueOf(validationMessage.getSeverity().name().toLowerCase(Locale.ENGLISH))));
        }
        return fieldSet.build();
    }

    private static Resource createTranslationResource(ResourceResolver resourceResolver, String label, String name, Optional<String> translation) {
        String checkboxName = name+ SUFFIX_USE_EMPTY;
        ValueMapResourceBuilderFactory factory = new ValueMapResourceBuilderFactory(resourceResolver,  "/dialog");
        ContainerValueMapResourceBuilder<?> well = ContainerValueMapResourceBuilder.forWell(factory, name).withProperty("sortKey", label);
        well.withItem(FieldValueMapResourceBuilder.forTextField(well.getItemFactory(), name, label, translation.orElse("")).withDataAttribute("show-checkbox-with-name-when-empty", checkboxName));
        boolean isChecked = translation.isPresent() && translation.get().isEmpty();
        FieldValueMapResourceBuilder<?> checkbox = FieldValueMapResourceBuilder.forCheckboxField(well.getItemFactory(), checkboxName, "Use empty value", isChecked);
        // field description is not properly hidden dynamically, therefore not used
        //    .withDescription("Check this box if a this translation should be kept as empty value. Otherwise all empty value entries will be removed from the dictionary.");
        // checkbox needs to be hidden with clientside logic due to not supporting Granite UIs field property "renderHidden"
        well.withItem(checkbox);
        return well.build();
    }

    static void sortResourcesByProperty(String propertyName, Locale locale, List<Resource> resources) {
        Collator collator = Collator.getInstance(locale);
        resources.sort((o1, o2) -> {
            ValueMap properties1 = o1.getValueMap();
            ValueMap properties2 = o2.getValueMap();
            return collator.compare(properties1.get(propertyName, ""), properties2.get(propertyName, ""));
        });
    }

    static void createCombiningMessageEntryDataSource(I18n i18n, Locale locale, Map<Locale, String> languageMap, ResourceResolver resourceResolver, String combiningMessageEntryPath, List<Resource> resourceList) {
        Resource combiningMessageEntryResource = resourceResolver.getResource(combiningMessageEntryPath);
        if (combiningMessageEntryResource != null) {
            ValueMap properties = combiningMessageEntryResource.getValueMap();
            Locale[] languages = properties.get(CombiningMessageEntryResourceProvider.LANGUAGES, Locale[].class);

            createCombiningMessageEntryDataSource(locale, languageMap, resourceResolver, resourceList, properties, languages);
            // make sure that key is always at the top
            ValueMapResourceBuilderFactory resourceBuilderFactory = new ValueMapResourceBuilderFactory(resourceResolver, "/dialog/item");
            String escapedKey = properties.get(CombiningMessageEntryResourceProvider.ESCAPED_KEY, String.class);
            resourceList.add(0, FieldValueMapResourceBuilder.forTextField(resourceBuilderFactory, "key", "Key", escapedKey).disabled().build());
            Resource validationContainer = addValidationMessagesResource(i18n, resourceResolver, languageMap, properties.get(CombiningMessageEntryResourceProvider.VALIDATION_MESSAGES, ValidationMessage[].class));
            if (validationContainer != null) {
                resourceList.add(0, validationContainer);
            }
        }
    }

    static void createCombiningMessageEntryDataSource(Locale locale, Map<Locale, String> languageMap,
            ResourceResolver resourceResolver, List<Resource> resourceList, ValueMap properties, Locale[] languages) {
        if (languages != null) {
            for (Locale language : languages) {
                @NotNull String name = language.toLanguageTag();
                String messageValue = properties.get(name, String.class);
                Optional<String> message = Optional.ofNullable(messageValue);
                String label = languageMap.getOrDefault(language, language.toLanguageTag());
                resourceList.add(createTranslationResource(resourceResolver, label, language.toLanguageTag(), message));
            }
            // sort by fieldLabel
            sortResourcesByProperty("sortKey", locale, resourceList);
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
