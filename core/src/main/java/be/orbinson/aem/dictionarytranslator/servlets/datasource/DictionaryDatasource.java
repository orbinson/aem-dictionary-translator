package be.orbinson.aem.dictionarytranslator.servlets.datasource;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.ExpressionHelper;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import java.util.ArrayList;
import java.util.List;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "aem-dictionary-translator/datasource/dictionary",
        methods = "GET"
)
public class DictionaryDatasource extends SlingSafeMethodsServlet {

    @Reference
    private transient DictionaryService dictionaryService;

    @Reference
    private ExpressionResolver expressionResolver;

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) {
        ResourceResolver resourceResolver = request.getResourceResolver();
        List<Resource> resourceList = new ArrayList<>();

        Config dsCfg = new Config(request.getResource().getChild("datasource"));
        ExpressionHelper expressionHelper = new ExpressionHelper(expressionResolver, request);
        Integer limit = expressionHelper.get(dsCfg.get("limit"), Integer.class);
        Integer offset = expressionHelper.get(dsCfg.get("offset"), Integer.class);

        List<Resource> dictionaries = dictionaryService.getDictionaries(resourceResolver);
        if (offset > dictionaries.size()) {
            offset = 0;
        }

        dictionaries.subList(offset, Math.min(offset + limit, dictionaries.size()))
                .forEach(resource ->
                        resourceList.add(
                                new ValueMapResource(
                                        resourceResolver,
                                        resource.getPath(), "aem-dictionary-translator/components/dictionary", resource.getValueMap()
                                )
                        )
                );

        DataSource dataSource = new SimpleDataSource(resourceList.iterator());
        request.setAttribute(DataSource.class.getName(), dataSource);
    }
}
