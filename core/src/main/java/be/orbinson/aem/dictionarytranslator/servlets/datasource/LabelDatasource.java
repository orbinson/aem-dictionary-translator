package be.orbinson.aem.dictionarytranslator.servlets.datasource;

import be.orbinson.aem.dictionarytranslator.models.Dictionary;
import be.orbinson.aem.dictionarytranslator.services.LabelResourceProvider;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "aem-dictionary-translator/datasource/label",
        methods = "GET"
)
public class LabelDatasource extends SlingSafeMethodsServlet {
    @Reference
    private transient ModelFactory modelFactory;

    private static void setColumnsDataSource(ResourceResolver resourceResolver, List<Resource> resourceList, Dictionary dictionary) {
        resourceList.add(getColumn(resourceResolver, "select", true));
        resourceList.add(getColumn(resourceResolver, "jcr:title", "key"));

        dictionary.getLanguages().forEach(language -> resourceList.add(getColumn(resourceResolver, "jcr:title", language)));
    }

    @NotNull
    private static ValueMapResource getColumn(ResourceResolver resourceResolver, String key, Object value) {
        ValueMap valueMap = new ValueMapDecorator(Map.of(key, value));
        return new ValueMapResource(resourceResolver, "", "", valueMap);
    }

    private static void setDataSource(ResourceResolver resourceResolver, List<Resource> resourceList, Dictionary dictionary) {
        for (String key : dictionary.getKeys()) {
            String path = LabelResourceProvider.ROOT + dictionary.getResource().getPath() + '/' + key;
            Resource keyResource = resourceResolver.getResource(path);
            if (keyResource != null) {
                resourceList.add(keyResource);
            }
        }
    }

    private static void createTextFieldResource(ResourceResolver resourceResolver, List<Resource> resourceList, String key, String value) {
        createTextFieldResource(resourceResolver, resourceList, key, value, false, false);
    }


    private static void createTextFieldResource(ResourceResolver resourceResolver, List<Resource> resourceList, String key, String value, boolean required, boolean disabled) {
        ValueMap valueMap = new ValueMapDecorator(Map.of(
                "fieldLabel", key,
                "name", key,
                "value", value,
                "disabled", disabled,
                "required", required)
        );
        resourceList.add(new ValueMapResource(resourceResolver, "", "granite/ui/components/coral/foundation/form/textfield", valueMap));
    }

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) {
        List<Resource> resourceList = new ArrayList<>();
        ResourceResolver resourceResolver = request.getResourceResolver();

        String dictionaryPath = request.getRequestPathInfo().getSuffix();
        if (dictionaryPath != null) {
            createDictionaryDataSource(request, resourceResolver, dictionaryPath, resourceList);
        }

        String labelPath = request.getParameter("label");
        if (labelPath != null) {
            createLabelDataSource(resourceResolver, labelPath, resourceList);
        }

        DataSource dataSource = new SimpleDataSource(resourceList.iterator());
        request.setAttribute(DataSource.class.getName(), dataSource);
    }

    private static void createLabelDataSource(ResourceResolver resourceResolver, String labelPath, List<Resource> resourceList) {
        Resource resource = resourceResolver.getResource(labelPath);
        if (resource != null) {
            ValueMap properties = resource.getValueMap();
            String[] languages = properties.get("languages", String[].class);

            createTextFieldResource(resourceResolver, resourceList, "Label", properties.get("key", String.class), false, true);
            if (languages != null) {
                for (String language : languages) {
                    String label = properties.get(language, StringUtils.EMPTY);
                    createTextFieldResource(resourceResolver, resourceList, language, label);
                }
            }
        }
    }

    private void createDictionaryDataSource(@NotNull SlingHttpServletRequest request, ResourceResolver resourceResolver, String dictionaryPath, List<Resource> resourceList) {
        Resource dictionaryResource = resourceResolver.getResource(dictionaryPath);
        if (dictionaryResource != null) {
            Dictionary dictionary = modelFactory.getModelFromWrappedRequest(request, dictionaryResource, Dictionary.class);
            if (dictionary != null) {
                if ("columnsdatasource".equals(request.getResource().getName())) {
                    setColumnsDataSource(resourceResolver, resourceList, dictionary);
                } else {
                    setDataSource(resourceResolver, resourceList, dictionary);
                }
            }
        }
    }
}
