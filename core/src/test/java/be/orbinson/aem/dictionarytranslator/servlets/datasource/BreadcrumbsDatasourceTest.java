package be.orbinson.aem.dictionarytranslator.servlets.datasource;

import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.day.cq.replication.Replicator;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(AemContextExtension.class)
class BreadcrumbsDatasourceTest {

    private final AemContext context = new AemContext();
    BreadcrumbsDatasource servlet;

    @BeforeEach
    void beforeEach() {
        context.registerService(Replicator.class, mock(Replicator.class));
        context.registerInjectActivateService(new DictionaryServiceImpl());

        servlet = context.registerInjectActivateService(new BreadcrumbsDatasource());

        context.load().json("/content.json", "/content");
    }

    @Test
    void getDataSource() {
        context.requestPathInfo().setSuffix("/content/dictionaries/fruit/i18n");

        servlet.doGet(context.request(), context.response());

        SimpleDataSource dataSource = (SimpleDataSource) context.request().getAttribute(DataSource.class.getName());

        Iterator<org.apache.sling.api.resource.Resource> iterator = dataSource.iterator();

        Resource currectResource = iterator.next();
        assertEquals("/", currectResource.getValueMap().get("href"));
        assertEquals("/content/dictionaries/fruit/i18n", currectResource.getValueMap().get("title"));

        Resource dictionaryResource = iterator.next();
        assertEquals("/tools/translation/dictionaries.html", dictionaryResource.getValueMap().get("href"));
        assertEquals("Dictionaries", dictionaryResource.getValueMap().get("title"));
    }
}