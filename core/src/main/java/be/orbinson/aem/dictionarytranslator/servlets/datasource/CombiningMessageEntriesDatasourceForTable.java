package be.orbinson.aem.dictionarytranslator.servlets.datasource;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.jcr.JcrConstants;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.Dictionary;
import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider;
import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider.ValidationMessage;

/**
 * This data source has two different use cases:
 * <ol>
 * <li>It is used to populate coral table's cells to display dictionary entries for all languages below a given dictionary</li>
 * <li>It is used to populate coral table's columns</li>
 * </ol>
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "aem-dictionary-translator/datasource/combining-message-entries-for-table",
        methods = "GET"
)
public class CombiningMessageEntriesDatasourceForTable extends SortedAndPaginatedDataSource {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(CombiningMessageEntriesDatasourceForTable.class);

    private final DictionaryService dictionaryService;

    private final CombiningMessageEntryResourceProvider combiningMessageEntryResourceProvider;

    @Activate
    public CombiningMessageEntriesDatasourceForTable(@Reference DictionaryService dictionaryService,
            @Reference CombiningMessageEntryResourceProvider combiningMessageEntryResourceProvider,
            @Reference ExpressionResolver resolver) {
        super(resolver);
        this.dictionaryService = dictionaryService;
        this.combiningMessageEntryResourceProvider = combiningMessageEntryResourceProvider;
    }
    
    private void setColumnsDataSource(ResourceResolver resourceResolver, Collection<Resource> resourceList, Collection<Locale> languages, Map<Locale, String> languageMap) {
        resourceList.add(getColumn(resourceResolver, "select", true, Optional.empty()));
        resourceList.add(getColumn(resourceResolver, JcrConstants.JCR_TITLE, "Key", Optional.of("Key")));
        if (combiningMessageEntryResourceProvider.isValidationEnabled()) {
            resourceList.add(getColumn(resourceResolver, JcrConstants.JCR_TITLE, "Validation", Optional.of("Validation")));
        }
        languages.forEach(language -> {
                    String title = languageMap.getOrDefault(language, language.toLanguageTag());
                    resourceList.add(getColumn(resourceResolver, JcrConstants.JCR_TITLE, title, Optional.of(language.toLanguageTag())));
                }
        );
    }

    @NotNull
    static ValueMapResource getColumn(ResourceResolver resourceResolver, String key, Object value, Optional<String> sortName) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(key, value);
        if (sortName.isPresent()) {
            properties.put("sortable", true);
            properties.put("name", sortName.get());
        }
        ValueMap valueMap = new ValueMapDecorator(properties);
        return new ValueMapResource(resourceResolver, "", "", valueMap);
    }

    private static void setDataSource(ResourceResolver resourceResolver, Collection<Resource> resourceList, String dictionaryPath, Collection<String> keys) throws DictionaryException {
        for (String key : keys) {
            // the escaping of the key is necessary as it may contain "/" which has a special meaning (even outside the JCR provider)
            String path = CombiningMessageEntryResourceProvider.createPath(dictionaryPath, key);
            // wrap the original resource 
            Resource keyResource = resourceResolver.getResource(path);
            if (keyResource != null) {
                resourceList.add(keyResource);
            } else {
                throw new IllegalStateException("Could not get resource for path: '" + path + "'.");
            }
        }
    }

    private void createDictionaryDataSource(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response, ResourceResolver resourceResolver, String dictionaryPath, Collection<Resource> resourceList) throws DictionaryException, ServletException, IOException {
        Collection<Dictionary> dictionaries = dictionaryService.getDictionaries(resourceResolver, dictionaryPath);
        if ("columnsdatasource".equals(request.getResource().getName())) {
            setColumnsDataSource(resourceResolver, resourceList, dictionaries.stream().map(Dictionary::getLanguage).collect(Collectors.toList()), LanguageDatasource.getAllAvailableLanguages(request, response));
        } else {
            // sort by key by default
            Collection<String> keys = dictionaries.stream().flatMap(
                    d -> {
                        try {
                            return d.getEntries().keySet().stream();
                        } catch (DictionaryException e) {
                            LOG.warn("Unable to get entries for dictionary {}, skipping it", d.getPath(), e);
                            return Stream.<String>empty();
                        }
                    })
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            setDataSource(resourceResolver, resourceList, dictionaryPath, keys);
        }
    }

    @Override
    public Function<Resource, Comparable<?>> getResourceValueForSortName(String sortName) {
        switch (sortName) {
            case "Key":
                return resource -> resource.getValueMap().get(CombiningMessageEntryResourceProvider.KEY, String.class);
            case "Validation":
                return resource -> {
                    SortedSet<ValidationMessage> validationMessages = resource.getValueMap().get(CombiningMessageEntryResourceProvider.VALIDATION_MESSAGES, SortedSet.class);
                    if (validationMessages != null && !validationMessages.isEmpty()) {
                        // return the first validation severity string
                        return validationMessages.first().getSeverity().name().toLowerCase(Locale.ENGLISH);
                    }
                    return "";
                };
            default:
                // assume sortName is a language code
                return resource -> resource.getValueMap().get(sortName, String.class); // default to empty string if unknown
        }
    }


    @Override
    protected void populateResources(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response, ResourceResolver resolver, Collection<Resource> resources) throws DictionaryException, ServletException, IOException {
        String dictionaryPath = request.getRequestPathInfo().getSuffix();
        if (dictionaryPath != null) {
            createDictionaryDataSource(request, response, resolver, request.getRequestPathInfo().getSuffix(), resources);
        } else {
            throw new IllegalArgumentException("No dictionary path provided in request: " + request.getRequestPathInfo().getSuffix());
        }
    }

    

}
