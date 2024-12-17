package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import com.day.cq.replication.Replicator;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class CreateDictionaryServletTest {
    private final AemContext context = new AemContext();

    CreateDictionaryServlet servlet;

    @Spy
    DictionaryService dictionaryService;

    @BeforeEach
    void beforeEach() {
        context.registerService(Replicator.class, mock(Replicator.class));
        dictionaryService = context.registerInjectActivateService(new DictionaryServiceImpl());

        servlet = context.registerInjectActivateService(new CreateDictionaryServlet());
    }

    @Test
    void doPostWithoutParams() throws IOException {
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void doPostInvalidParams() throws IOException {
        context.request().setParameterMap(Map.of(
                "name", "dictionaries",
                "path", "/nonexistent/path",
                "language", "nl"
        ));

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void doPostValidParams() throws IOException {
        String[] languages = new String[]{"en", "fr"};
        context.create().resource("/content/dictionaries");
        context.request().setParameterMap(Map.of(
                "name", "fruit",
                "path", "/content/dictionaries",
                "language", languages,
                "basename", "/content/dictionaries/fruit"
        ));

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        for (String language : languages) {
            Resource resource = context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/" + language);
            assertNotNull(resource);
            ValueMap properties = resource.getValueMap();
            assertEquals(language, properties.get("jcr:language"));
            assertEquals("mix:language", properties.get("jcr:mixinTypes"));
            assertEquals(language, properties.get("jcr:language"));
            assertEquals("/content/dictionaries/fruit", properties.get("sling:basename"));
        }
    }
}
