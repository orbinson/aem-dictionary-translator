package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import com.adobe.granite.translation.api.TranslationConfig;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
class UpdateLabelServletTest {

    private final AemContext context = new AemContext();

    UpdateLabelServlet servlet;

    DictionaryService dictionaryService;

    @Mock
    TranslationConfig translationConfig;

    @BeforeEach
    void beforeEach() {
        translationConfig = context.registerService(TranslationConfig.class, translationConfig);
        dictionaryService = context.registerInjectActivateService(new DictionaryServiceImpl());

        servlet = context.registerInjectActivateService(new UpdateLabelServlet());
    }

    @Test
    void doPostWithoutParams() throws ServletException, IOException {
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of());

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void updateLabelInNonExistingDictionary() throws ServletException, IOException {
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/i18n",
                "key", "greeting",
                "en", "Hello"
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    @Disabled("Testing CI")
    void doPostWithValidParams() throws ServletException, IOException {
        context.create().resource("/content/dictionaries/i18n/en", Map.of("jcr:language", "en"));
        context.create().resource("/content/dictionaries/i18n/en/appel", Map.of(
                "dictionary", "/content/dictionaries/i18n", "key", "appel",
                "en", "apple", "sling:MessageEntry", "jcr:primaryType")
        );
        context.create().resource("/content/dictionaries/i18n/fr", Map.of("jcr:language", "fr"));
        context.create().resource("/content/dictionaries/i18n/en/appel", Map.of(
                "dictionary", "/content/dictionaries/i18n", "key", "appel",
                "fr", "pomme", "sling:MessageEntry", "jcr:primaryType")
        );

        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/i18n",
                "key", "appel",
                "en", "Hello",
                "fr", "Bonjour"
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        Resource resource = context.resourceResolver().getResource("/content/dictionaries/i18n/en/appel");
        ValueMap properties = resource.getValueMap();
        assertNotNull(resource);
        assertEquals("sling:MessageEntry", properties.get("jcr:primaryType"));
        assertEquals("appel", properties.get("sling:key"));
        assertEquals("Hello", properties.get("sling:message"));
    }
}
