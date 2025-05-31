package be.orbinson.aem.dictionarytranslator.servlets.datasource;

import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
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

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.Dictionary;
import be.orbinson.aem.dictionarytranslator.services.Dictionary.Type;

/**
 * This data source is used to populate the table of dictionaries (only for the actual rows, the header is populated statically without a data source)
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "aem-dictionary-translator/datasource/dictionary",
        methods = "GET"
)
public class DictionaryDatasource extends SortedAndPaginatedDataSource {


    private static final Logger LOG = LoggerFactory.getLogger(DictionaryDatasource.class);

    private final DictionaryService dictionaryService;


    @Activate
    public DictionaryDatasource(@Reference DictionaryService dictionaryService, @Reference ExpressionResolver expressionResolver) {
        super(expressionResolver);
        this.dictionaryService = dictionaryService;
    }
    
    @Override
    public Function<Resource, Comparable<?>> getResourceValueForSortName(String sortName) {
        switch(sortName) {
        case "path":
            return Resource::getPath;
        case "message-entries":
            return resource -> resource.getValueMap().get("numEntries", Integer.class);
        default:
            return resource -> resource.getValueMap().get(sortName, String.class);
        }
    }

    @Override
    protected void populateResources(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response,
            ResourceResolver resolver, Collection<Resource> resources) throws DictionaryException, ServletException, IOException {
        Map<String, SortedMap<Locale, Dictionary>> dictionaries = dictionaryService.getAllDictionariesByParentPath(resolver);
        for (Entry<String, SortedMap<Locale,Dictionary>> entry : dictionaries.entrySet()) {
            Resource dictionaryResource = new ValueMapResource(
                    resolver,
                    entry.getKey(), "aem-dictionary-translator/components/dictionary", new ValueMapDecorator(createProperties(resolver, entry.getValue()))
            );
            resources.add(dictionaryResource);
        }
    }

    private Map<String, Object> createProperties(ResourceResolver resourceResolver, Map<Locale, Dictionary> dictionaries) {
        // count unique keys across all dictionaries
        long numEntries = dictionaries.values().stream().flatMap(d -> {
            try {
                return d.getEntries().keySet().stream();
            } catch (DictionaryException e) {
                LOG.warn("Error while counting entries in dictionary: " + d.getPath(), e);
                return Stream.empty();
            }
        }).distinct().count();
        boolean isEditable = dictionaries.values().stream().allMatch(d -> d.isEditable(resourceResolver));
        Collection<String> baseNames = dictionaries.values().stream()
                .flatMap(d -> d.getBaseNames().stream())
                .collect(Collectors.toSet());
        Set<Type> types = dictionaries.values().stream()
                .map(Dictionary::getType)
                .collect(Collectors.toSet());
        Type type = types.size() == 1 ? types.iterator().next() : Type.MIXED; // if all dictionaries have the same type, use it, otherwise use artificial type MIXED
        return Map.of(
                "languages", dictionaries.keySet().stream().map(Locale::toLanguageTag).collect(Collectors.toList()),
                "baseNames", baseNames,
                "editable", isEditable,
                "type", type,
                "numEntries", numEntries
        );
    }
}
