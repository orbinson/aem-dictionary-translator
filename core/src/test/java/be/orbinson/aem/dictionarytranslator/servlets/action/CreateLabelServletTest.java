package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import com.adobe.granite.translation.api.TranslationConfig;
import com.day.cq.replication.Replicator;
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

import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_KEY;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGE;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGEENTRY;
import static junit.framework.Assert.assertNull;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class CreateLabelServletTest {
    private final AemContext context = new AemContext();

    CreateLabelServlet servlet;

    DictionaryService dictionaryService;

    @Mock
    TranslationConfig translationConfig;

    @BeforeEach
    void beforeEach() {
        translationConfig = context.registerService(TranslationConfig.class, translationConfig);
        context.registerService(Replicator.class, mock(Replicator.class));
        dictionaryService = context.registerInjectActivateService(new DictionaryServiceImpl());

        servlet = context.registerInjectActivateService(new CreateLabelServlet());
    }

    @Test
    void doPostWithoutParams() throws ServletException, IOException {
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of());

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void createLabelInNonExistingDictionary() throws ServletException, IOException {
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
    void doPostWithValidParams() throws ServletException, IOException {
        context.create().resource("/content/dictionaries/i18n/en", Map.of("jcr:language", "en"));
        context.create().resource("/content/dictionaries/i18n/fr", Map.of("jcr:language", "fr"));

        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/i18n",
                "key", "greeting",
                "en", "Hello",
                "fr", "Bonjour"
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        Resource resource = context.resourceResolver().getResource("/content/dictionaries/i18n/en/greeting");
        ValueMap properties = resource.getValueMap();
        assertNotNull(resource);
        assertEquals(SLING_MESSAGEENTRY, properties.get(JCR_PRIMARYTYPE));
        assertEquals("greeting", properties.get(SLING_KEY));
        assertEquals("Hello", properties.get(SLING_MESSAGE));
    }

    @Test
    void doPostWithEmptyMessage() throws ServletException, IOException {
        context.create().resource("/content/dictionaries/i18n/en", Map.of("jcr:language", "en"));
        context.create().resource("/content/dictionaries/i18n/fr", Map.of("jcr:language", "fr"));

        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/i18n",
                "key", "greeting",
                "en", "Hello",
                "fr", ""
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        Resource resource = context.resourceResolver().getResource("/content/dictionaries/i18n/fr/greeting");
        ValueMap properties = resource.getValueMap();
        assertNotNull(resource);
        assertEquals(SLING_MESSAGEENTRY, properties.get(JCR_PRIMARYTYPE));
        assertEquals("greeting", properties.get(SLING_KEY));
        assertNull(properties.get(SLING_MESSAGE));
    }

    @Test
    void doPostCaseSensitiveKeys() throws ServletException, IOException {
        context.create().resource("/content/dictionaries/i18n/en", Map.of("jcr:language", "en"));
        context.create().resource("/content/dictionaries/i18n/fr", Map.of("jcr:language", "fr"));

        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/i18n",
                "key", "greeting",
                "en", "Hello",
                "fr", ""
        ));
        servlet.service(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/i18n",
                "key", "Greeting",
                "en", "Hello2",
                "fr", ""
        ));
        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        Resource resource = context.resourceResolver().getResource("/content/dictionaries/i18n/en/greeting");
        ValueMap properties = resource.getValueMap();
        assertNotNull(resource);
        assertEquals(SLING_MESSAGEENTRY, properties.get(JCR_PRIMARYTYPE));
        assertEquals("greeting", properties.get(SLING_KEY));
        assertEquals("Hello", properties.get(SLING_MESSAGE));

        resource = context.resourceResolver().getResource("/content/dictionaries/i18n/en/Greeting");
        properties = resource.getValueMap();
        assertNotNull(resource);
        assertEquals(SLING_MESSAGEENTRY, properties.get(JCR_PRIMARYTYPE));
        assertEquals("Greeting", properties.get(SLING_KEY));
        assertEquals("Hello2", properties.get(SLING_MESSAGE));
    }

    @Test
    void createLabelThatAlreadyExists() throws ServletException, IOException {
        context.create().resource("/content/dictionaries/i18n/en", Map.of("jcr:language", "en"));
        context.create().resource("/content/dictionaries/i18n/fr", Map.of("jcr:language", "fr"));

        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/i18n",
                "key", "greeting",
                "en", "Hello",
                "fr", "Bonjour"
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());

    }
}
