package be.orbinson.aem.dictionarytranslator.servlets.action;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGE;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(AemContextExtension.class)
class ImportDictionaryServletTest {

    private ImportDictionaryServlet importTranslation;

    private AemContext context = new AemContext();

    @BeforeEach
    void setUp() {
        importTranslation = context.registerInjectActivateService(new ImportDictionaryServlet());
        context.create().resource("/content");
        context.create().resource("/content/en");
        context.create().resource("/content/nl");
    }

    @Test
    void doPostOneLabel() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        request.addRequestParameter("dictionary", "/content");
        String csvData = "KEY,en,nl\nhello,Hello,Hallo\n";
        InputStream csvStream = new ByteArrayInputStream(csvData.getBytes());
        request.addRequestParameter("csvfile", csvStream.readAllBytes(), "text/csv", "translations.csv");

        importTranslation.doPost(request, response);

        assertEquals(200, response.getStatus());

        ResourceResolver resourceResolver = context.resourceResolver();
        Resource enHelloResource = resourceResolver.getResource("/content/en/hello");
        assertNotNull(enHelloResource);

        ValueMap properties = enHelloResource.getValueMap();
        String message = properties.get(SLING_MESSAGE, String.class);
        assertEquals("Hello", message);

        Resource nlHelloResource = resourceResolver.getResource("/content/nl/hello");
        assertNotNull(nlHelloResource);

        properties = nlHelloResource.getValueMap();
        message = properties.get(SLING_MESSAGE, String.class);
        assertEquals("Hallo", message);
    }

    @Test
    void doPostOneLabelWrongLanguage() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        request.addRequestParameter("dictionary", "/content");
        String csvData = "KEY,en,fr\nhello,Hello,Bonjour\n";
        InputStream csvStream = new ByteArrayInputStream(csvData.getBytes());
        request.addRequestParameter("csvfile", csvStream.readAllBytes(), "text/csv", "translations.csv");

        importTranslation.doPost(request, response);

        assertEquals(400, response.getStatus());

        ResourceResolver resourceResolver = context.resourceResolver();
        Resource enHelloResource = resourceResolver.getResource("/content/en/hello");
        assertNull(enHelloResource);

        Resource nlHelloResource = resourceResolver.getResource("/content/fr/hello");
        assertNull(nlHelloResource);
    }

    @Test
    void doPostMultipleLabels() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        request.addRequestParameter("dictionary", "/content");
        String csvData = "KEY,en,nl\nhello,Hello,Hallo\nday,Day,Dag\n";
        InputStream csvStream = new ByteArrayInputStream(csvData.getBytes());
        request.addRequestParameter("csvfile", csvStream.readAllBytes(), "text/csv", "translations.csv");

        importTranslation.doPost(request, response);

        assertEquals(200, response.getStatus());

        ResourceResolver resourceResolver = context.resourceResolver();
        Resource enHelloResource = resourceResolver.getResource("/content/en/hello");
        assertNotNull(enHelloResource);

        ValueMap helloProperties = enHelloResource.getValueMap();
        String message = helloProperties.get(SLING_MESSAGE, String.class);
        assertEquals("Hello", message);

        Resource nlHelloResource = resourceResolver.getResource("/content/nl/hello");
        assertNotNull(nlHelloResource);

        helloProperties = nlHelloResource.getValueMap();
        message = helloProperties.get(SLING_MESSAGE, String.class);
        assertEquals("Hallo", message);

        Resource enDayResource = resourceResolver.getResource("/content/en/day");
        assertNotNull(enDayResource);

        ValueMap dayProperties = enDayResource.getValueMap();
        String dayMessage = dayProperties.get(SLING_MESSAGE, String.class);
        assertEquals("Day", dayMessage);

        Resource nlDayResource = resourceResolver.getResource("/content/nl/day");
        assertNotNull(nlDayResource);

        dayProperties = nlDayResource.getValueMap();
        dayMessage = dayProperties.get(SLING_MESSAGE, String.class);
        assertEquals("Dag", dayMessage);
    }

    @Test
    void doPostWrongHeader() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        request.addRequestParameter("dictionary", "/content");
        String csvData = "WRONGHEADER,en,nl\nhello,Hello,Hallo\n";
        InputStream csvStream = new ByteArrayInputStream(csvData.getBytes());
        request.addRequestParameter("csvfile", csvStream.readAllBytes(), "text/csv", "translations.csv");

        importTranslation.doPost(request, response);

        assertEquals(400, response.getStatus());
    }

    @Test
    void doPostNoLabels() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        request.addRequestParameter("dictionary", "/content");
        String csvData = "KEY,en,nl\n";
        InputStream csvStream = new ByteArrayInputStream(csvData.getBytes());
        request.addRequestParameter("csvfile", csvStream.readAllBytes(), "text/csv", "translations.csv");

        importTranslation.doPost(request, response);

        assertEquals(200, response.getStatus());
    }

    @Test
    void doPostUnsupportedDelimiter() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        request.addRequestParameter("dictionary", "/content");
        String csvData = "KEY-en-nl\nhello-Hello-Hallo\n";
        InputStream csvStream = new ByteArrayInputStream(csvData.getBytes());
        request.addRequestParameter("csvfile", csvStream.readAllBytes(), "text/csv", "translations.csv");

        importTranslation.doPost(request, response);

        assertEquals(400, response.getStatus());
    }
}

