package be.orbinson.aem.dictionarytranslator.servlets.datasource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.jackrabbit.util.Text;
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

import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.ExpressionHelper;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.jcr.JcrConstants;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.models.Dictionary;
import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider;

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
public class CombiningMessageEntriesDatasourceForTable extends SlingSafeMethodsServlet {
    
    @Reference
    private transient ModelFactory modelFactory;

    @Reference
    private ExpressionResolver expressionResolver;

    private static void setColumnsDataSource(ResourceResolver resourceResolver, List<Resource> resourceList, Dictionary dictionary, Map<String, String> languageMap) {
        resourceList.add(getColumn(resourceResolver, "select", true));
        resourceList.add(getColumn(resourceResolver, JcrConstants.JCR_TITLE, "Key"));
        resourceList.add(getColumn(resourceResolver, JcrConstants.JCR_TITLE, "Validation"));

        dictionary.getLanguages().forEach(language -> {
                    String title = languageMap.getOrDefault(language, language);
                    resourceList.add(getColumn(resourceResolver, JcrConstants.JCR_TITLE, title));
                }
        );
    }

    @NotNull
    static ValueMapResource getColumn(ResourceResolver resourceResolver, String key, Object value) {
        ValueMap valueMap = new ValueMapDecorator(Map.of(
                key, value,
                "sortable", true
        ));
        return new ValueMapResource(resourceResolver, "", "", valueMap);
    }

    private static void setDataSource(ResourceResolver resourceResolver, List<Resource> resourceList, Dictionary dictionary) throws DictionaryException {
        for (String key : dictionary.getKeys()) {
            // the escaping of the key is necessary as it may contain "/" which has a special meaning (even outside the JCR provider)
            String path = CombiningMessageEntryResourceProvider.ROOT + dictionary.getResource().getPath() + '/' + Text.escapeIllegalJcrChars(key);
            Resource keyResource = resourceResolver.getResource(path);
            if (keyResource != null) {
                resourceList.add(keyResource);
            } else {
                throw new IllegalStateException("Could not get resource for path: " + path + ". Probably the mandatory CombiningMessageEntryResourceProvider is not active.");
            }
        }
    }

   
    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        List<Resource> resourceList = new ArrayList<>();
        ResourceResolver resourceResolver = request.getResourceResolver();
        
        String dictionaryPath = request.getRequestPathInfo().getSuffix();
        if (dictionaryPath != null) {
            try {
                createDictionaryDataSource(request, response, resourceResolver, dictionaryPath, resourceList);
            } catch (DictionaryException e) {
                response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                return;
            }
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

        DataSource dataSource = new SimpleDataSource(resourceList.iterator());
        request.setAttribute(DataSource.class.getName(), dataSource);
    }

    private void createDictionaryDataSource(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response, ResourceResolver resourceResolver, String dictionaryPath, List<Resource> resourceList) throws DictionaryException, ServletException, IOException {
        Resource dictionaryResource = resourceResolver.getResource(dictionaryPath);
        if (dictionaryResource != null) {
            Dictionary dictionary = modelFactory.getModelFromWrappedRequest(request, dictionaryResource, Dictionary.class);
            if (dictionary != null) {
                if ("columnsdatasource".equals(request.getResource().getName())) {
                    setColumnsDataSource(resourceResolver, resourceList, dictionary, LanguageDatasource.getAllAvailableLanguages(request, response));
                } else {
                    setDataSource(resourceResolver, resourceList, dictionary);
                }
            }
        }
    }
}
