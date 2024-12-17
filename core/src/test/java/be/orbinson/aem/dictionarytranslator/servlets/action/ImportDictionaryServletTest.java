package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGE;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(AemContextExtension.class)
class ImportDictionaryServletTest {

    private final AemContext context = new AemContext();

    ImportDictionaryServlet servlet;

    @BeforeEach
    void setUp() {
        context.registerService(new DictionaryServiceImpl());
        servlet = context.registerInjectActivateService(new ImportDictionaryServlet());

        context.load().json("/content.json", "/content");
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

        servlet.doPost(context.request(), context.response());

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

        servlet.doPost(context.request(), context.response());

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

        servlet.doPost(context.request(), context.response());
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
    void doPostWrongHeader() throws Exception {
        context.request().addRequestParameter("dictionary", "/content/dictionaries/fruit/i18n");
        String csvData = "WRONGHEADER,en,nl\nhello,Hello,Hallo\n";
        InputStream csvStream = new ByteArrayInputStream(csvData.getBytes());
        context.request().addRequestParameter("csvfile", csvStream.readAllBytes(), "text/csv", "translations.csv");

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void doPostNoMessageEntries() throws Exception {
        context.request().addRequestParameter("dictionary", "/content/dictionaries/fruit/i18n");
        String csvData = "KEY,en,nl_BE\n";
        InputStream csvStream = new ByteArrayInputStream(csvData.getBytes());
        context.request().addRequestParameter("csvfile", csvStream.readAllBytes(), "text/csv", "translations.csv");

        servlet.doPost(context.request(), context.response());

        assertEquals(200, context.response().getStatus());
    }

    @Test
    void doPostUnsupportedDelimiter() throws Exception {
        context.request().addRequestParameter("dictionary", "/content/dictionaries/fruit/i18n");
        String csvData = "KEY-en-nl\nhello-Hello-Hallo\n";
        InputStream csvStream = new ByteArrayInputStream(csvData.getBytes());
        context.request().addRequestParameter("csvfile", csvStream.readAllBytes(), "text/csv", "translations.csv");

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }
}

