package be.orbinson.aem.dictionarytranslator.servlets.datasource;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.iterators.TransformIterator;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.text.Collator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    public static Map<String, String> getAllAvailableLanguages(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        // TranslationConfig.getLanguages(ResourceResolver) does never return the country label,
        // therefore use the data source which is also used in the Page Properties dialog (Advanced Tab in Language)
        RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setForceResourceType("cq/gui/components/common/datasources/languages");
        request.getRequestDispatcher(request.getResource(), options).include(request, response);
        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        List<ValueTextResource> resources = IteratorUtils.toList(new TransformIterator<>(dataSource.iterator(), r -> ValueTextResource.fromResource(request.getLocale(), r)));
        return toLanguageMap(resources);
    }

    private static Map<String, String> toLanguageMap(List<ValueTextResource> resources) {
        return resources.stream()
                // the upstream data source does not filter access control child resource
                .filter(r -> !AccessControlConstants.REP_POLICY.equals(r.getValue()))
                .collect(Collectors.toMap(
                        ValueTextResource::getValue,
                        r -> r.getText() + " (" + r.getValue() + ")",
                        (oldValue, newValue) -> {
                            LOG.warn("Duplicate language/country code: {}", oldValue);
                            return oldValue;
                        }));
    }

    private Set<String> getDictionaryLanguages(ResourceResolver resourceResolver, @Nullable String dictionaryPath) {
        if (dictionaryPath != null) {
            Resource dictionaryResource = resourceResolver.getResource(dictionaryPath);
            if (dictionaryResource != null) {
                return new HashSet<>(dictionaryService.getLanguages(dictionaryResource));
            }
        }
        return new HashSet<>();
    }

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        // populate language map and filter
        String dictionaryPath = request.getRequestPathInfo().getSuffix();

        Map<String, String> languageMap = getAllAvailableLanguages(request, response);
        Set<String> dictionaryLanguages = getDictionaryLanguages(request.getResourceResolver(), dictionaryPath);
        Predicate<String> languageFilter;
        // evaluate data source configuration
        Config dsCfg = new Config(request.getResource().getChild("datasource"));
        if (dsCfg.get("hideNonDictionaryLanguages", false)) {
            languageFilter = dictionaryLanguages::contains;
            // add missing languages to dictionary
            dictionaryLanguages.forEach(l -> languageMap.putIfAbsent(l, l));
        } else {
            languageFilter = l -> !dictionaryLanguages.contains(l);
        }
        // convert to list of resources
        boolean emitTextFieldResources = dsCfg.get("emitTextFieldResources", false);
        List<OrderedValueMapResource> resourceList = languageMap.entrySet().stream()
                .filter(e -> languageFilter.test(e.getKey()))
                .map(e -> {
                    if (emitTextFieldResources) {
                        return TextFieldResource.create(request.getLocale(), request.getResourceResolver(), e.getKey(), e.getValue());
                    } else {
                        return ValueTextResource.create(request.getLocale(), request.getResourceResolver(), e.getKey(), e.getValue());
                    }
                }).sorted().collect(Collectors.toList());
        // sort by display names
        // create data source (only accepts iterator over Resource, not of subclasses so we need to transform)
        DataSource dataSource = new SimpleDataSource(new TransformIterator<>(resourceList.iterator(), r -> (Resource) r));
        request.setAttribute(DataSource.class.getName(), dataSource);
    }

    private abstract static class OrderedValueMapResource extends ValueMapResource implements Comparable<OrderedValueMapResource> {

        private final Collator collator;

        protected OrderedValueMapResource(Locale locale, ResourceResolver resourceResolver, String resourceType, ValueMap vm) {
            super(resourceResolver, "", resourceType, vm);
            collator = Collator.getInstance(locale);
        }

        abstract String getLabel();

        @Override
        public int compareTo(OrderedValueMapResource o) {
            return collator.compare(getLabel(), o.getLabel());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof OrderedValueMapResource && collator.equals(((OrderedValueMapResource) obj).collator);
        }
    }

    private static class TextFieldResource extends OrderedValueMapResource {

        private TextFieldResource(Locale locale, ResourceResolver resolver, ValueMap valueMap) {
            super(locale, resolver, "granite/ui/components/coral/foundation/form/textfield", valueMap);
        }

        public static TextFieldResource create(Locale locale, ResourceResolver resolver, String value, String text) {
            ValueMap valueMap = new ValueMapDecorator(Map.of("fieldLabel", text, "name", value));
            return new TextFieldResource(locale, resolver, valueMap);
        }

        String getLabel() {
            return getValueMap().get("fieldLabel", String.class);
        }
    }

    private static class ValueTextResource extends OrderedValueMapResource {
        private ValueTextResource(Locale locale, ResourceResolver resolver, ValueMap valueMap) {
            super(locale, resolver, "nt:unstructured", valueMap);
        }

        public static ValueTextResource fromResource(Locale locale, Resource resource) {
            return new ValueTextResource(locale, resource.getResourceResolver(), resource.getValueMap());
        }

        public static ValueTextResource create(Locale locale, ResourceResolver resolver, String value, String text) {
            ValueMap valueMap = new ValueMapDecorator(Map.of("value", value, "text", text));
            return new ValueTextResource(locale, resolver, valueMap);
        }

        public String getText() {
            return getValueMap().get("text", String.class);
        }

        public String getValue() {
            return getValueMap().get("value", String.class);
        }

        @Override
        String getLabel() {
            return getText();
        }
    }
}
