package be.orbinson.aem.dictionarytranslator.servlets.datasource;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "aem-dictionary-translator/datasource/breadcrumbs",
        methods = "GET"
)
public class BreadcrumbsDataSource extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) {
        List<Resource> syntheticItemResources = new ArrayList<>();
        addCurrentDictionaryCrumb(request, syntheticItemResources);
        addDictionariesCrumb(request, syntheticItemResources);
        DataSource dataSource = new SimpleDataSource(syntheticItemResources.iterator());
        request.setAttribute(DataSource.class.getName(), dataSource);
    }

    private void addCurrentDictionaryCrumb(SlingHttpServletRequest request, List<Resource> syntheticItemResources) {
        ValueMap crumbVM = new ValueMapDecorator(new HashMap<>());
        crumbVM.put("title", request.getRequestPathInfo().getSuffix());
        crumbVM.put("href", request.getRequestURI());
        syntheticItemResources.add(new ValueMapResource(request.getResourceResolver(), new ResourceMetadata(), "nt:unstructured", crumbVM));

    }

    private void addDictionariesCrumb(@NotNull SlingHttpServletRequest request, List<Resource> syntheticItemResources) {
        ValueMap crumbVM = new ValueMapDecorator(new HashMap<>());
        crumbVM.put("title", "Dictionaries");
        crumbVM.put("href", "/apps/aem-dictionary-translator/content/dictionaries.html");
        syntheticItemResources.add(new ValueMapResource(request.getResourceResolver(), new ResourceMetadata(), "nt:unstructured", crumbVM));
    }
}
