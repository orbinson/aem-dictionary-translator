package be.orbinson.aem.dictionarytranslator.servlets.datasource;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.iterators.TransformIterator;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryImpl;
import be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.ValueMapResourceBuilderFactory;
import be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.graniteui.ComponentValueMapResourceBuilder;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "aem-dictionary-translator/datasource/dictionary-language",
        methods = "GET"
)
public class LanguageDatasource extends SlingSafeMethodsServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(LanguageDatasource.class);

    @Reference
    transient DictionaryService dictionaryService;

    private static class GraniteFormSelectItem {
        private final ValueMap properties;
        private GraniteFormSelectItem(ValueMap properties) {
            this.properties = properties;
        }

        public static GraniteFormSelectItem fromResource(Resource resource) {
            return new GraniteFormSelectItem(resource.getValueMap());
        }

        public String getText() {
            return properties.get("text", String.class);
        }

        public String getValue() {
            return properties.get("value", String.class);
        }

    }

    public static Map<Locale, String> getAllAvailableLanguages(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        // TranslationConfig.getLanguages(ResourceResolver) does never return the country label,
        // therefore use the data source which is also used in the Page Properties dialog (Advanced Tab in Language)
        RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setForceResourceType("cq/gui/components/common/datasources/languages");
        request.getRequestDispatcher(request.getResource(), options).include(request, response);
        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        List<GraniteFormSelectItem> resources = IteratorUtils.toList(new TransformIterator<>(dataSource.iterator(), r -> GraniteFormSelectItem.fromResource(r)));
        return toLanguageMap(resources);
    }

    private static Map<Locale, String> toLanguageMap(List<GraniteFormSelectItem> resources) {
        return resources.stream()
                // the upstream data source does not filter access control child resource
                .filter(r -> !AccessControlConstants.REP_POLICY.equals(r.getValue()))
                .collect(Collectors.toMap(
                        r -> DictionaryImpl.toLocale(r.getValue()),
                        r -> r.getText() + " (" + r.getValue() + ")",
                        (oldValue, newValue) -> {
                            LOG.warn("Duplicate language/country code: {}", oldValue);
                            return oldValue;
                        }));
    }

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        // populate language map and filter
        String dictionaryPath = request.getRequestPathInfo().getSuffix();

        Map<Locale, String> languageMap = getAllAvailableLanguages(request, response);
        Set<Locale> dictionaryLanguages = dictionaryService.getDictionaries(request.getResourceResolver(), dictionaryPath).stream().map(d -> d.getLanguage()).collect(Collectors.toSet());
        Predicate<Locale> languageFilter;
        // evaluate data source configuration
        Config dsCfg = new Config(request.getResource().getChild("datasource"));
        if (dsCfg.get("hideNonDictionaryLanguages", false)) {
            languageFilter = dictionaryLanguages::contains;
            // add missing languages to dictionary
            dictionaryLanguages.forEach(l -> languageMap.putIfAbsent(l, l.toLanguageTag()));
        } else {
            languageFilter = l -> !dictionaryLanguages.contains(l);
        }
        final List<Resource> resources;
        boolean forCreateEntryDialog = dsCfg.get("forCreateEntryDialog", false);
        if (forCreateEntryDialog) {
            // populate the create entry dialog
            Locale[] languages = languageMap.entrySet().stream()
                    .filter(e -> languageFilter.test(e.getKey()))
                    .map(Map.Entry::getKey)
                    .toArray(Locale[]::new);
            resources = new LinkedList<>();
            CombiningMessageEntryDatasourceForDialog.createCombiningMessageEntryDataSource(request.getLocale(), languageMap, request.getResourceResolver(), resources, new ValueMapDecorator(Map.of()), languages);
        } else {
            ValueMapResourceBuilderFactory factory = new ValueMapResourceBuilderFactory(request.getResourceResolver(), "/languages");
            // otherwise just populate a select control (granite/ui/components/coral/foundation/form/select)
            resources = languageMap.entrySet().stream()
                    .filter(e -> languageFilter.test(e.getKey()))
                    .map(e -> {
                        return ComponentValueMapResourceBuilder.forFormSelectItem(factory, e.getValue(), e.getKey().toLanguageTag()).build();
                    }).collect(Collectors.toList());
            // sort by language label
            CombiningMessageEntryDatasourceForDialog.sortResourcesByProperty("text", request.getLocale(), resources);
        }
        
        // create data source (only accepts iterator over Resource, not of subclasses so we need to transform)
        DataSource dataSource = new SimpleDataSource(new TransformIterator<>(resources.iterator(), r -> (Resource) r));
        request.setAttribute(DataSource.class.getName(), dataSource);
    }
}
