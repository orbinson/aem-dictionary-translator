package be.orbinson.aem.dictionarytranslator.servlets.datasource;

import be.orbinson.aem.dictionarytranslator.models.Dictionary;
import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider;
import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.ExpressionHelper;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "aem-dictionary-translator/datasource/combining-message-entry",
        methods = "GET"
)
public class CombiningMessageEntryDatasource extends SlingSafeMethodsServlet {
    public static final String FIELD_LABEL = "fieldLabel";

    @Reference
    private transient ModelFactory modelFactory;

    @Reference
    private ExpressionResolver expressionResolver;

    private static void setColumnsDataSource(ResourceResolver resourceResolver, List<Resource> resourceList, Dictionary dictionary, Map<String, String> languageMap) {
        resourceList.add(getColumn(resourceResolver, "select", true));
        resourceList.add(getColumn(resourceResolver, "jcr:title", "Key"));

        dictionary.getLanguages().forEach(language -> {
                    String title = languageMap.getOrDefault(language, language);
                    resourceList.add(getColumn(resourceResolver, "jcr:title", title));
                }
        );
    }

    @NotNull
    private static ValueMapResource getColumn(ResourceResolver resourceResolver, String key, Object value) {
        ValueMap valueMap = new ValueMapDecorator(Map.of(
                key, value,
                "sortable", true
        ));
        return new ValueMapResource(resourceResolver, "", "", valueMap);
    }

    private static void setDataSource(ResourceResolver resourceResolver, List<Resource> resourceList, Dictionary dictionary) {
        for (String key : dictionary.getKeys()) {
            String path = CombiningMessageEntryResourceProvider.ROOT + dictionary.getResource().getPath() + '/' + key;
            Resource keyResource = resourceResolver.getResource(path);
            if (keyResource != null) {
                resourceList.add(keyResource);
            }
        }
    }

    private static Resource createTextFieldResource(ResourceResolver resourceResolver, String label, String name, String value) {
        return createTextFieldResource(resourceResolver, label, name, value, false, false);
    }


    private static Resource createTextFieldResource(ResourceResolver resourceResolver, String label, String name, String value, boolean required, boolean disabled) {
        ValueMap valueMap = new ValueMapDecorator(Map.of(
                FIELD_LABEL, label,
                "name", name,
                "value", value,
                "disabled", disabled,
                "required", required)
        );
        return new ValueMapResource(resourceResolver, "", "granite/ui/components/coral/foundation/form/textfield", valueMap);
    }

    private static Resource createHiddenFieldResource(ResourceResolver resourceResolver, String key, String value) {
        ValueMap valueMap = new ValueMapDecorator(Map.of(
                FIELD_LABEL, key,
                "name", key,
                "value", value)
        );
        return new ValueMapResource(resourceResolver, "", "granite/ui/components/coral/foundation/form/hidden", valueMap);
    }

    private static void sortResourcesByProperty(String propertyName, Locale locale, List<Resource> resources) {
        Collator collator = Collator.getInstance(locale);
        resources.sort((o1, o2) -> {
            ValueMap properties1 = o1.getValueMap();
            ValueMap properties2 = o2.getValueMap();
            return collator.compare(properties1.get(propertyName, ""), properties2.get(propertyName, ""));
        });
    }

    private static void createCombiningMessageEntryDataSource(Locale locale, Map<String, String> languageMap, ResourceResolver resourceResolver, String combiningMessageEntryPath, List<Resource> resourceList) {
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
        }
    }

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        List<Resource> resourceList = new ArrayList<>();
        ResourceResolver resourceResolver = request.getResourceResolver();
        Map<String, String> languageMap = LanguageDatasource.getAllAvailableLanguages(request, response);

        String dictionaryPath = request.getRequestPathInfo().getSuffix();
        if (dictionaryPath != null) {
            createDictionaryDataSource(request, resourceResolver, dictionaryPath, resourceList, languageMap);
            if ("list".equals(request.getResource().getName())) {
                Config dsCfg = new Config(request.getResource().getChild("datasource"));
                ExpressionHelper expressionHelper = new ExpressionHelper(expressionResolver, request);
                Integer limit = expressionHelper.get(dsCfg.get("limit"), Integer.class);
                Integer offset = expressionHelper.get(dsCfg.get("offset"), Integer.class);
                if (offset > resourceList.size()) {
                    offset = 0;
                }
                resourceList = resourceList.subList(offset, Math.min(offset + limit, resourceList.size()));
            }
        }

        String combiningMessageEntryPath = request.getParameter("item");
        if (combiningMessageEntryPath != null) {
            createCombiningMessageEntryDataSource(request.getLocale(), languageMap, resourceResolver, combiningMessageEntryPath, resourceList);
        }

        DataSource dataSource = new SimpleDataSource(resourceList.iterator());
        request.setAttribute(DataSource.class.getName(), dataSource);
    }

    private void createDictionaryDataSource(@NotNull SlingHttpServletRequest request, ResourceResolver resourceResolver, String dictionaryPath, List<Resource> resourceList, Map<String, String> languageMap) {
        Resource dictionaryResource = resourceResolver.getResource(dictionaryPath);
        if (dictionaryResource != null) {
            Dictionary dictionary = modelFactory.getModelFromWrappedRequest(request, dictionaryResource, Dictionary.class);
            if (dictionary != null) {
                if ("columnsdatasource".equals(request.getResource().getName())) {
                    setColumnsDataSource(resourceResolver, resourceList, dictionary, languageMap);
                } else {
                    setDataSource(resourceResolver, resourceList, dictionary);
                }
            }
        }
    }
}
