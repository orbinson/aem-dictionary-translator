package be.orbinson.aem.dictionarytranslator.servlets.datasource;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.day.cq.replication.Replicator;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.resourceresolver.MockFindQueryResources;
import org.apache.sling.testing.resourceresolver.MockFindResourcesHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;

import static junitx.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class DictionaryDatasourceTest {

    private final AemContext context = new AemContext();
    DictionaryDatasource servlet;

    @Mock
    ExpressionResolver expressionResolver;

    
    /** Contains the paths of all resources which are returned by the findResources() method, necessary because the RR mock does not support queries natively */
    List<String> dictionaryPaths;

    @BeforeEach
    void beforeEach() {
        context.registerService(Replicator.class, mock(Replicator.class));
        expressionResolver = context.registerService(ExpressionResolver.class, expressionResolver);

        DictionaryService dictionaryService = new DictionaryServiceImpl();
        context.registerInjectActivateService(dictionaryService);
        servlet = context.registerInjectActivateService(new DictionaryDatasource(dictionaryService, expressionResolver));

        context.load().json("/apps.json", "/apps");
        context.load().json("/content.json", "/content");
        
        dictionaryPaths = new ArrayList<>(List.of(
                "/content/dictionaries/fruit/i18n/en",
                "/content/dictionaries/fruit/i18n/nl_be",
                "/content/dictionaries/vegetables/i18n/en")
        );
        
        MockFindResourcesHandler handler = new MockFindResourcesHandler() {
            @Override
            public @Nullable Iterator<Resource> findResources(@NotNull String query, String language) {
                return dictionaryPaths.stream().map(p -> context.resourceResolver().getResource(p)).iterator();
            }
        };
        MockFindQueryResources.addFindResourceHandler(context.resourceResolver(), handler);
    }

    @Test
    void getDataSource() throws ServletException, IOException {
        context.currentResource("/apps/aem-dictionary-translator/content/dictionaries/jcr:content/views/list");

        SlingHttpServletRequest request = context.request();
        context.requestPathInfo().setSelectorString(".0.20");
        servlet.doGet(request, context.response());

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());

        Iterator<Resource> iterator = dataSource.iterator();
        List.of("/content/dictionaries/fruit/i18n", "/content/dictionaries/vegetables/i18n").forEach(path -> {
            Resource resource = iterator.next();
            assertEquals(path, resource.getPath());
            assertEquals("aem-dictionary-translator/components/dictionary", resource.getResourceType());
        });
    }
}