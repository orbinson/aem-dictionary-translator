package be.orbinson.aem.dictionarytranslator.servlets.action;

import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_KEY;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGE;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGEENTRY;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class CreateMessageEntryServletTest {
    private final AemContext context = new AemContext();

    CreateMessageEntryServlet servlet;

    DictionaryServiceImpl dictionaryService;

    @BeforeEach
    void beforeEach() {
        context.registerService(Replicator.class, mock(Replicator.class));
        dictionaryService = context.registerInjectActivateService(new DictionaryServiceImpl());

        servlet = context.registerInjectActivateService(new CreateMessageEntryServlet());
    }

    @Test
    void doPostWithoutParams() throws IOException {
        context.request().setParameterMap(Map.of());

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void createMessageEntryInNonExistingDictionary() throws IOException {
        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/fruit/i18n",
                "key", "greeting",
                "en", "Hello"
        ));

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void doPostWithValidParams() throws IOException {
        context.create().resource(
                "/content/dictionaries/fruit/i18n/en",
                Map.of("jcr:language", "en")
        );
        context.create().resource(
                "/content/dictionaries/fruit/i18n/fr",
                Map.of("jcr:language", "fr")
        );

        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/fruit/i18n",
                "key", "apple",
                "en", "Apple",
                "fr", "Pomme"
        ));

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        ValueMap properties = context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en/apple").getValueMap();
        assertEquals(SLING_MESSAGEENTRY, properties.get(JCR_PRIMARYTYPE));
        assertEquals("apple", properties.get(SLING_KEY));
        assertEquals("Apple", properties.get(SLING_MESSAGE));

        properties = context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/fr/apple").getValueMap();
        assertEquals(SLING_MESSAGEENTRY, properties.get(JCR_PRIMARYTYPE));
        assertEquals("apple", properties.get(SLING_KEY));
        assertEquals("Pomme", properties.get(SLING_MESSAGE));
    }

    @Test
    void doPostWithEmptyMessage() throws IOException {
        context.create().resource("/content/dictionaries/i18n/en", Map.of("jcr:language", "en"));
        context.create().resource("/content/dictionaries/i18n/fr", Map.of("jcr:language", "fr"));

        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/i18n",
                "key", "greeting",
                "en", "Hello",
                "fr", ""
        ));

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        Resource resource = context.resourceResolver().getResource("/content/dictionaries/i18n/fr/greeting");
        ValueMap properties = resource.getValueMap();
        assertNotNull(resource);
        assertEquals(SLING_MESSAGEENTRY, properties.get(JCR_PRIMARYTYPE));
        assertNull(properties.get(SLING_MESSAGE));
    }

    @Test
    void doPostCaseSensitiveKeys() throws IOException {
        context.create().resource("/content/dictionaries/i18n/en", Map.of("jcr:language", "en"));
        context.create().resource("/content/dictionaries/i18n/fr", Map.of("jcr:language", "fr"));

        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/i18n",
                "key", "greeting",
                "en", "Hello",
                "fr", ""
        ));
        servlet.doPost(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/i18n",
                "key", "Greeting",
                "en", "Hello2",
                "fr", ""
        ));
        servlet.doPost(context.request(), context.response());

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
    void createMessageEntryThatAlreadyExists() throws IOException {
        context.create().resource("/content/dictionaries/i18n/en", Map.of("jcr:language", "en"));
        context.create().resource("/content/dictionaries/i18n/fr", Map.of("jcr:language", "fr"));

        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/i18n",
                "key", "greeting",
                "en", "Hello",
                "fr", "Bonjour"
        ));

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        // invalidate cache
        dictionaryService.onChange(List.of(new ResourceChange(ChangeType.ADDED, "/content/dictionaries/i18n/en", false)));
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }
}
