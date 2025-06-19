package be.orbinson.aem.dictionarytranslator.servlets.action;

import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGE;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGEENTRY;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.testing.resourceresolver.MockFindQueryResources;
import org.apache.sling.testing.resourceresolver.MockFindResourcesHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    List<String> dictionaryPaths;

    @BeforeEach
    void beforeEach() {
        context.registerService(Replicator.class, mock(Replicator.class));
        dictionaryService = context.registerInjectActivateService(new DictionaryServiceImpl(true));
        context.request().setMethod("POST");
        servlet = context.registerInjectActivateService(new CreateMessageEntryServlet());

        dictionaryPaths = new ArrayList<>();
        MockFindResourcesHandler handler = new MockFindResourcesHandler() {
            @Override
            public @Nullable Iterator<Resource> findResources(@NotNull String query, String language) {
                return dictionaryPaths.stream().map(p -> context.resourceResolver().getResource(p)).iterator();
            }
        };
        MockFindQueryResources.addFindResourceHandler(context.resourceResolver(), handler);
    }

    @Test
    void doPostWithoutParams() throws IOException, ServletException {
        context.request().setParameterMap(Map.of());

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void createMessageEntryInNonExistingDictionary() throws IOException, ServletException {
        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/fruit/i18n",
                "key", "greeting",
                "en", "Hello"
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void doPostWithValidParams() throws IOException, ServletException {
        context.create().resource(
                "/content/dictionaries/fruit/i18n/en",
                Map.of("jcr:language", "en")
        );
        context.create().resource(
                "/content/dictionaries/fruit/i18n/fr",
                Map.of("jcr:language", "fr")
        );

        context.resourceResolver().commit(); // expose to other resource resolvers
        dictionaryPaths.add("/content/dictionaries/fruit/i18n/en");
        dictionaryPaths.add("/content/dictionaries/fruit/i18n/fr");
        
        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/fruit/i18n",
                "key", "apple",
                "en", "Apple",
                "fr", "Pomme"
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_CREATED, context.response().getStatus());

        Resource resource = context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en/apple");
        assertNotNull(resource);
        ValueMap properties = resource.getValueMap();
        assertEquals(SLING_MESSAGEENTRY, properties.get(JCR_PRIMARYTYPE));
        assertEquals("Apple", properties.get(SLING_MESSAGE));

        resource = context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/fr/apple");
        assertNotNull(resource);
        properties = resource.getValueMap();
        assertEquals(SLING_MESSAGEENTRY, properties.get(JCR_PRIMARYTYPE));
        assertEquals("Pomme", properties.get(SLING_MESSAGE));
    }

    @Test
    void doPostWithEmptyMessage() throws IOException, ServletException {
        context.create().resource("/content/dictionaries/i18n/en", Map.of("jcr:language", "en"));
        context.create().resource("/content/dictionaries/i18n/fr", Map.of("jcr:language", "fr"));
        context.resourceResolver().commit(); // expose to other resource resolvers
        dictionaryPaths.add("/content/dictionaries/i18n/en");
        dictionaryPaths.add("/content/dictionaries/i18n/fr");

        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/i18n",
                "key", "greeting",
                "en", "Hello",
                "fr", ""
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_CREATED, context.response().getStatus());

        Resource resource = context.resourceResolver().getResource("/content/dictionaries/i18n/fr/greeting");
        assertNotNull(resource);
        ValueMap properties = resource.getValueMap();
        assertEquals(SLING_MESSAGEENTRY, properties.get(JCR_PRIMARYTYPE));
        assertFalse(properties.containsKey(SLING_MESSAGE));
    }

    @Test
    void doPostWithEmptyMessageAndUseEmpty() throws IOException, ServletException {
        context.create().resource("/content/dictionaries/i18n/en", Map.of("jcr:language", "en"));
        context.create().resource("/content/dictionaries/i18n/fr", Map.of("jcr:language", "fr"));
        context.resourceResolver().commit(); // expose to other resource resolvers
        dictionaryPaths.add("/content/dictionaries/i18n/en");
        dictionaryPaths.add("/content/dictionaries/i18n/fr");

        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/i18n",
                "key", "greeting",
                "en", "Hello",
                "fr", "",
                "fr_useEmpty", "true"
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_CREATED, context.response().getStatus());

        Resource resource = context.resourceResolver().getResource("/content/dictionaries/i18n/fr/greeting");
        assertNotNull(resource);
        ValueMap properties = resource.getValueMap();
        assertEquals(SLING_MESSAGEENTRY, properties.get(JCR_PRIMARYTYPE));
        assertEquals("", properties.get(SLING_MESSAGE));
    }

    @Test
    void doPostCaseSensitiveKeys() throws IOException, ServletException {
        context.create().resource("/content/dictionaries/i18n/en", Map.of("jcr:language", "en"));
        context.create().resource("/content/dictionaries/i18n/fr", Map.of("jcr:language", "fr"));
        context.resourceResolver().commit(); // expose to other resource resolvers
        dictionaryPaths.add("/content/dictionaries/i18n/en");
        dictionaryPaths.add("/content/dictionaries/i18n/fr");
        
        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/i18n",
                "key", "greeting",
                "en", "Hello",
                "fr", ""
        ));
        servlet.service(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_CREATED, context.response().getStatus());

        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/i18n",
                "key", "Greeting",
                "en", "Hello2",
                "fr", ""
        ));
        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_CREATED, context.response().getStatus());

        Resource resource = context.resourceResolver().getResource("/content/dictionaries/i18n/en/greeting");
        ValueMap properties = resource.getValueMap();
        assertNotNull(resource);
        assertEquals(SLING_MESSAGEENTRY, properties.get(JCR_PRIMARYTYPE));
        assertEquals("Hello", properties.get(SLING_MESSAGE));

        resource = context.resourceResolver().getResource("/content/dictionaries/i18n/en/Greeting");
        properties = resource.getValueMap();
        assertNotNull(resource);
        assertEquals(SLING_MESSAGEENTRY, properties.get(JCR_PRIMARYTYPE));
        assertEquals("Hello2", properties.get(SLING_MESSAGE));
    }

    @Test
    void createMessageEntryThatAlreadyExists() throws IOException, ServletException {
        context.create().resource("/content/dictionaries/i18n/en", Map.of("jcr:language", "en"));
        context.create().resource("/content/dictionaries/i18n/fr", Map.of("jcr:language", "fr"));
        context.resourceResolver().commit(); // expose to other resource resolvers
        dictionaryPaths.add("/content/dictionaries/i18n/en");
        dictionaryPaths.add("/content/dictionaries/i18n/fr");
        
        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/i18n",
                "key", "greeting",
                "en", "Hello",
                "fr", "Bonjour"
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_CREATED, context.response().getStatus());

        // invalidate cache
        dictionaryService.onChange(List.of(new ResourceChange(ChangeType.ADDED, "/content/dictionaries/i18n/en", false)));
        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }
}
