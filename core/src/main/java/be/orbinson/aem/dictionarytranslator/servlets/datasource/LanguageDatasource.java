package be.orbinson.aem.dictionarytranslator.servlets.datasource;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
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
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "aem-dictionary-translator/datasource/language",
        methods = "GET"
)
public class LanguageDatasource extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageDatasource.class);
    @Reference
    private transient DictionaryService dictionaryService;

    private static void createResource(ResourceResolver resourceResolver, List<Resource> resourceList, String language, String value, String resourceType) {
        if ("granite/ui/components/coral/foundation/container".equals(resourceType)) {
            createTextFieldResource(resourceResolver, resourceList, language, value);
        } else {
            createSelectResource(resourceResolver, resourceList, language, value);
        }
    }

    private static void createTextFieldResource(ResourceResolver resourceResolver, List<Resource> resourceList, String language, String value) {
        ValueMap valueMap = new ValueMapDecorator(Map.of("fieldLabel", value + " (" + language + ")", "name", language));
        resourceList.add(new ValueMapResource(resourceResolver, "", "granite/ui/components/coral/foundation/form/textfield", valueMap));
    }

    private static void createSelectResource(ResourceResolver resourceResolver, List<Resource> resourceList, String language, String value) {
        ValueMap valueMap = new ValueMapDecorator(Map.of("value", language, "text", value + " (" + language + ")"));
        resourceList.add(new ValueMapResource(resourceResolver, "", "", valueMap));
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) {
        String dictionaryPath = request.getRequestPathInfo().getSuffix();

        if (StringUtils.isEmpty(dictionaryPath)) {
            return;
        }

        ResourceResolver resourceResolver = request.getResourceResolver();

        String resourceType = request.getResource().getValueMap().get("sling:resourceType", String.class);
        List<Resource> resourceList = getResources(resourceResolver, dictionaryPath, resourceType);

        DataSource dataSource = new SimpleDataSource(resourceList.iterator());
        request.setAttribute(DataSource.class.getName(), dataSource);
    }

    @NotNull
    private List<Resource> getResources(ResourceResolver resourceResolver, String dictionaryPath, String resourceType) {
        List<Resource> resourceList = new ArrayList<>();

        if (dictionaryService != null) {
            dictionaryService.getLanguagesForPath(resourceResolver, dictionaryPath).forEach((language, value) -> createResource(resourceResolver, resourceList, language, value, resourceType));
        } else {
            LOG.error("TextFields can not be determined when Dictionary Service is null");
        }

        return resourceList;
    }
}
