package be.orbinson.aem.dictionarytranslator.servlets.datasource;

import com.adobe.granite.translation.api.TranslationConfig;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
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
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.util.*;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "aem-dictionary-translator/datasource/available-language",
        methods = "GET"
)
public class AvailableLanguageDatasource extends SlingSafeMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(AvailableLanguageDatasource.class);

    @Reference
    transient TranslationConfig translationConfig;

    public static Map<String, String> sortByValue(Map<String, String> languageMap) {
        List<Map.Entry<String, String>> list =
                new LinkedList<>(languageMap.entrySet());

        list.sort(Map.Entry.comparingByValue());

        HashMap<String, String> temp = new LinkedHashMap<>();
        for (Map.Entry<String, String> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static void filterExistingLanguages(Map<String, String> languageMap, ResourceResolver resourceResolver, String languagePath) {
        Resource resource = resourceResolver.getResource(languagePath);
        List<String> currentSelectedLanguages = new ArrayList<>();
        if (resource != null) {
            resourceResolver.getChildren(resource)
                    .iterator()
                    .forEachRemaining(l -> currentSelectedLanguages.add(l.getPath().replace(languagePath + "/", "")));
            currentSelectedLanguages.forEach(languageMap::remove);
        }
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) {
        LOG.debug("Build languages datasource");
        ResourceResolver resourceResolver = request.getResourceResolver();
        String languageParameter = request.getRequestPathInfo().getSuffix();

        Map<String, String> languageMap = new HashMap<>(translationConfig.getLanguages(resourceResolver));
        filterExistingLanguages(languageMap, resourceResolver, languageParameter);
        Map<String, String> sortedLanguageMap = sortByValue(languageMap);

        List<Resource> resourceList = new ArrayList<>();
        sortedLanguageMap.forEach((key, value) -> {
            String text = String.format("%s (%s)", value, key);
            ValueMap valueMap = new ValueMapDecorator(Map.of("value", key, "text", text));

            LOG.debug("Add language '{}' to datasource with key '{}'", text, key);
            resourceList.add(new ValueMapResource(resourceResolver, "", "", valueMap));
        });

        DataSource dataSource = new SimpleDataSource(resourceList.iterator());
        request.setAttribute(DataSource.class.getName(), dataSource);
    }

}
