package be.orbinson.aem.dictionarytranslator.servlets.datasource;

import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.day.cq.replication.Replicator;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Iterator;
import java.util.List;

import static junitx.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class DictionaryDatasourceTest {

    private final AemContext context = new AemContext();
    DictionaryDatasource servlet;

    @BeforeEach
    void beforeEach() {
        context.registerService(Replicator.class, mock(Replicator.class));
        context.registerInjectActivateService(new DictionaryServiceImpl());

        servlet = context.registerInjectActivateService(new DictionaryDatasource());

        context.load().json("/content.json", "/content");
    }

    @Test
    void getDataSource() {
        ResourceResolver resourceResolver = spy(context.resourceResolver());
        doReturn(List.of(
                        resourceResolver.getResource("/content/dictionaries/fruit/i18n"),
                        resourceResolver.getResource("/content/dictionaries/vegetables/i18n")
                ).iterator()
        ).when(resourceResolver).findResources(anyString(), anyString());
        SlingHttpServletRequest request = spy(context.request());
        when(request.getResourceResolver()).thenReturn(resourceResolver);

        context.currentResource("/content/dictionaries/fruit/i18n");

        servlet.doGet(request, context.response());

        SimpleDataSource dataSource = (SimpleDataSource) request.getAttribute(DataSource.class.getName());

        Iterator<Resource> iterator = dataSource.iterator();
        List.of("/content/dictionaries/fruit/i18n", "/content/dictionaries/vegetables/i18n").forEach(path -> {
            Resource resource = iterator.next();
            assertEquals(path, resource.getPath());
            assertEquals("aem-dictionary-translator/components/dictionary", resource.getResourceType());
        });
    }
}