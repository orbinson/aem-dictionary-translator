package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import com.adobe.granite.translation.api.TranslationConfig;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class CreateDictionaryServletTest {
    private final AemContext context = new AemContext();

    CreateDictionaryServlet servlet;

    DictionaryService dictionaryService;

    @Mock
    TranslationConfig translationConfig;

    @BeforeEach
    void beforeEach() {
        translationConfig = context.registerService(TranslationConfig.class, translationConfig);
        dictionaryService = context.registerInjectActivateService(new DictionaryServiceImpl());

        servlet = context.registerInjectActivateService(new CreateDictionaryServlet());
    }

    @Test
    void doPostWithoutParams() throws ServletException, IOException {
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of());

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void doPostInvalidParams() throws ServletException, IOException {
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "name", "/content/dictionaries",
                "path", "/nonexistent/path",
                "language", new String[]{"nonexistent-language"},
                "basename", new String[]{"nonexistent-basename"}
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void doPostValidParams() throws ServletException, IOException {
        context.create().resource("/content/dictionaries/site");
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "name", new String[]{"i18n"},
                "path", new String[]{"/content/dictionaries/site"},
                "language", new String[]{"en", "fr"},
                "basename", new String[]{"/content/dictionaries/site", "/content/dictionaries/site"}
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void checkDictionaryIsCreated() throws ServletException, IOException {
        context.create().resource("/content/dictionaries/site");
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "name", new String[]{"i18n"},
                "path", new String[]{"/content/dictionaries/site"},
                "language", new String[]{"en"},
                "basename", new String[]{"/content/dictionaries/site"}
        ));

        servlet.service(context.request(), context.response());

        Resource resource = context.resourceResolver().getResource("/content/dictionaries/site/i18n");
        assertNotNull(resource);
        ValueMap properties = resource.getChild("en").getValueMap();
        assertEquals("en", properties.get("jcr:language", String.class));
        assertEquals("mix:language", properties.get("jcr:mixinTypes", String.class));
        assertEquals("/content/dictionaries/site", properties.get("sling:basename", String.class));
        assertEquals("sling:Folder", properties.get("sling:resourceType", String.class));
    }

    @Test
    void checkDictionaryIsCreatedWithoutBasenames() throws ServletException, IOException {
        context.create().resource("/content/dictionaries/site");
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "name", new String[]{"i18n"},
                "path", new String[]{"/content/dictionaries/site"},
                "language", new String[]{"en"}
        ));

        servlet.service(context.request(), context.response());

        Resource resource = context.resourceResolver().getResource("/content/dictionaries/site/i18n");
        assertNotNull(resource);
        ValueMap properties = resource.getChild("en").getValueMap();
        assertEquals("en", properties.get("jcr:language", String.class));
        assertEquals("mix:language", properties.get("jcr:mixinTypes", String.class));
        assertEquals("/content/dictionaries/site", properties.get("sling:basename", String.class));
        assertEquals("sling:Folder", properties.get("sling:resourceType", String.class));
    }
}
