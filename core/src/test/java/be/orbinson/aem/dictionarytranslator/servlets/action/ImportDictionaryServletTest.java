package be.orbinson.aem.dictionarytranslator.servlets.action;

import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
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
class ImportDictionaryServletTest {

    private final AemContext context = new AemContext();

    ImportDictionaryServlet servlet;

    private List<String> dictionaryPaths;

    @BeforeEach
    void setUp() {
        context.registerInjectActivateService(new DictionaryServiceImpl());
        context.registerService(Replicator.class, mock(Replicator.class));
        servlet = context.registerInjectActivateService(new ImportDictionaryServlet());
        context.request().setMethod("POST");

        context.load().json("/content.json", "/content");
        dictionaryPaths = new ArrayList<>();
        dictionaryPaths.add("/content/dictionaries/fruit/i18n/en");
        dictionaryPaths.add("/content/dictionaries/fruit/i18n/nl_be");
        MockFindResourcesHandler handler = new MockFindResourcesHandler() {
            @Override
            public @Nullable Iterator<Resource> findResources(@NotNull String query, String language) {
                return dictionaryPaths.stream().map(p -> context.resourceResolver().getResource(p)).iterator();
            }
        };
        MockFindQueryResources.addFindResourceHandler(context.resourceResolver(), handler);
    }

    @Test
    void doPostOneMessageEntry() throws Exception {
        context.request().addRequestParameter("dictionary", "/content/dictionaries/fruit/i18n");
        String csvData = "KEY,en,nl_BE\ngrape,Grape,Druif\n";
        InputStream csvStream = new ByteArrayInputStream(csvData.getBytes());
        context.request().addRequestParameter(
                "csvfile",
                csvStream.readAllBytes(),
                "text/csv",
                "translations.csv"
        );

        servlet.service(context.request(), context.response());

        assertEquals(200, context.response().getStatus());

        ResourceResolver resourceResolver = context.resourceResolver();
        Resource resource = resourceResolver.getResource("/content/dictionaries/fruit/i18n/en/grape");
        assertNotNull(resource);
        ValueMap properties = resource.getValueMap();
        String message = properties.get(SLING_MESSAGE, String.class);
        assertEquals("Grape", message);

        resource = resourceResolver.getResource("/content/dictionaries/fruit/i18n/nl_be/grape");
        assertNotNull(resource);
        properties = resource.getValueMap();
        message = properties.get(SLING_MESSAGE, String.class);
        assertEquals("Druif", message);
    }

    @Test
    void doPostOneMessageEntryWrongLanguage() throws Exception {
        context.request().addRequestParameter("dictionary", "/content/dictionaries/fruit/i18n");

        String csvData = "KEY,en,nl_FR\ngrape,Grape,Druif\n";
        InputStream csvStream = new ByteArrayInputStream(csvData.getBytes());
        context.request().addRequestParameter(
                "csvfile",
                csvStream.readAllBytes(),
                "text/csv",
                "translations.csv"
        );

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());

        ResourceResolver resourceResolver = context.resourceResolver();
        Resource resource = resourceResolver.getResource("/content/dictionaries/fruit/i18n/en/grape");
        assertNull(resource);

        resource = resourceResolver.getResource("/content/dictionaries/fruit/i18n/nl_fr/grape");
        assertNull(resource);
    }

    @Test
    void doPostMultipleMessageEntries() throws Exception {
        context.request().addRequestParameter("dictionary", "/content/dictionaries/fruit/i18n");

        String csvData = "KEY,en,nl_BE\ngrape,Grape,Druif\npineapple,Pineapple,Ananas";
        InputStream csvStream = new ByteArrayInputStream(csvData.getBytes());
        context.request().addRequestParameter(
                "csvfile",
                csvStream.readAllBytes(),
                "text/csv",
                "translations.csv"
        );

        servlet.service(context.request(), context.response());

        assertEquals(200, context.response().getStatus());

        ResourceResolver resourceResolver = context.resourceResolver();
        Resource resource = resourceResolver.getResource("/content/dictionaries/fruit/i18n/en/grape");
        assertNotNull(resource);
        ValueMap properties = resource.getValueMap();
        String message = properties.get(SLING_MESSAGE, String.class);
        assertEquals("Grape", message);

        resource = resourceResolver.getResource("/content/dictionaries/fruit/i18n/nl_be/grape");
        assertNotNull(resource);
        properties = resource.getValueMap();
        message = properties.get(SLING_MESSAGE, String.class);
        assertEquals("Druif", message);

        resource = resourceResolver.getResource("/content/dictionaries/fruit/i18n/en/pineapple");
        assertNotNull(resource);
        properties = resource.getValueMap();
        message = properties.get(SLING_MESSAGE, String.class);
        assertEquals("Pineapple", message);

        resource = resourceResolver.getResource("/content/dictionaries/fruit/i18n/nl_be/pineapple");
        assertNotNull(resource);
        properties = resource.getValueMap();
        message = properties.get(SLING_MESSAGE, String.class);
        assertEquals("Ananas", message);
    }

    @Test
    void doPostEmptyMessageEntries() throws Exception {
        context.request().addRequestParameter("dictionary", "/content/dictionaries/fruit/i18n");

        // test empty message entries both with empty and with special <empty> value for existing and non existing keys
        String csvData = "KEY,en\ngrape,\napple,\nbanana,<empty>\npineapple,<empty>";
        InputStream csvStream = new ByteArrayInputStream(csvData.getBytes());
        context.request().addRequestParameter(
                "csvfile",
                csvStream.readAllBytes(),
                "text/csv",
                "translations.csv"
        );

        servlet.service(context.request(), context.response());

        assertEquals(200, context.response().getStatus());

        // new entries with empty values should not be created
        ResourceResolver resourceResolver = context.resourceResolver();
        Resource resource = resourceResolver.getResource("/content/dictionaries/fruit/i18n/en/grape");
        assertNull(resource);

        // existing entries with empty values should not be deleted, but their sling:message property should be removed
        resource = resourceResolver.getResource("/content/dictionaries/fruit/i18n/en/apple");
        assertNotNull(resource);
        ValueMap properties = resource.getValueMap();
        assertFalse(properties.containsKey(SLING_MESSAGE));

        // existing entries with <empty> value should be updated with empty sling:message property
        resource = resourceResolver.getResource("/content/dictionaries/fruit/i18n/en/banana");
        assertNotNull(resource);
        properties = resource.getValueMap();
        String message = properties.get(SLING_MESSAGE, String.class);
        assertEquals("", message);

        // new entries with <empty> value should be created with empty sling:message property
        resource = resourceResolver.getResource("/content/dictionaries/fruit/i18n/en/pineapple");
        assertNotNull(resource);
        properties = resource.getValueMap();
        message = properties.get(SLING_MESSAGE, String.class);
        assertEquals("", message);
    }

    @Test
    void doPostWrongHeader() throws Exception {
        context.request().addRequestParameter("dictionary", "/content/dictionaries/fruit/i18n");
        String csvData = "WRONGHEADER,en,nl\nhello,Hello,Hallo\n";
        InputStream csvStream = new ByteArrayInputStream(csvData.getBytes());
        context.request().addRequestParameter("csvfile", csvStream.readAllBytes(), "text/csv", "translations.csv");

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void doPostNoMessageEntries() throws Exception {
        context.request().addRequestParameter("dictionary", "/content/dictionaries/fruit/i18n");
        String csvData = "KEY,en,nl_BE\n";
        InputStream csvStream = new ByteArrayInputStream(csvData.getBytes());
        context.request().addRequestParameter("csvfile", csvStream.readAllBytes(), "text/csv", "translations.csv");

        servlet.service(context.request(), context.response());

        assertEquals(200, context.response().getStatus());
    }

    @Test
    void doPostUnsupportedDelimiter() throws Exception {
        context.request().addRequestParameter("dictionary", "/content/dictionaries/fruit/i18n");
        String csvData = "KEY-en-nl\nhello-Hello-Hallo\n";
        InputStream csvStream = new ByteArrayInputStream(csvData.getBytes());
        context.request().addRequestParameter("csvfile", csvStream.readAllBytes(), "text/csv", "translations.csv");

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void doPostValuesWithDelimiter() throws Exception {
        context.request().addRequestParameter("dictionary", "/content/dictionaries/fruit/i18n");
        String csvData = "KEY,en,nl_BE\nhello,Hello;Hi,Hallo\n";
        InputStream csvStream = new ByteArrayInputStream(csvData.getBytes());
        context.request().addRequestParameter("csvfile", csvStream.readAllBytes(), "text/csv", "translations.csv");

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void doPostLineEndings() throws Exception {
        context.request().addRequestParameter("dictionary", "/content/dictionaries/fruit/i18n");
        String csvData = "KEY,en,nl_BE\r\nhello,Hello;Hi,Hallo\r\n";
        InputStream csvStream = new ByteArrayInputStream(csvData.getBytes());
        context.request().addRequestParameter("csvfile", csvStream.readAllBytes(), "text/csv", "translations.csv");

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }
}

