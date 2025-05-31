package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import com.day.cq.replication.Replicator;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@ExtendWith(AemContextExtension.class)
class CreateLanguageServletTest {

    private final AemContext context = new AemContext();

    CreateLanguageServlet servlet;

    DictionaryService dictionaryService;

    @BeforeEach
    void beforeEach() {
        context.registerService(Replicator.class, mock(Replicator.class));
        dictionaryService = context.registerInjectActivateService(new DictionaryServiceImpl(true));

        servlet = context.registerInjectActivateService(new CreateLanguageServlet());
    }

    @Test
    void doPostWithoutParams() throws ServletException, IOException {
        context.request().setMethod("POST");
        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void doPostWithInvalidParams() throws ServletException, IOException {
        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/i18n",
                "language", "en")
        );

        context.request().setMethod("POST");
        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void doPostWithValidParams() throws ServletException, IOException {
        context.create().resource("/content/dictionaries/fruit/i18n");

        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/fruit/i18n",
                "language", "en")
        );

        context.request().setMethod("POST");
        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        Resource resource = context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en");
        ValueMap properties = resource.getValueMap();
        assertNotNull(resource);
        assertEquals("en", properties.get("jcr:language"));
        assertEquals("mix:language", properties.get("jcr:mixinTypes", String.class));
        assertEquals("/content/dictionaries/fruit/i18n", properties.get("sling:basename", String.class));
    }

    @Test
    void doPostWithAllValidParams() throws ServletException, IOException {
        context.create().resource("/content/dictionaries/fruit/i18n");

        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/fruit/i18n",
                "language", "en",
                "basename", "namespace"
        ));

        context.request().setMethod("POST");
        servlet.service(context.request(), context.response());

        Resource resource = context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en");
        ValueMap properties = resource.getValueMap();

        assertNotNull(resource);
        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals("en", properties.get("jcr:language"));
        assertEquals("mix:language", properties.get("jcr:mixinTypes", String.class));
        assertEquals("namespace", properties.get("sling:basename", String.class));
    }
}
