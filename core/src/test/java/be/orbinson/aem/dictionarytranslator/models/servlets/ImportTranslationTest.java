package be.orbinson.aem.dictionarytranslator.models.servlets;

import be.orbinson.aem.dictionarytranslator.servlets.ImportTranslation;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(AemContextExtension.class)
public class ImportTranslationTest {

    private ImportTranslation underTest;

    private AemContext context;

    @BeforeEach
    void setUp(final AemContext ctx) {
        context = ctx;
        underTest = context.registerInjectActivateService(new ImportTranslation());
        context.create().resource("/content");
    }

    @Test
    void testDoPost() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        // Set request parameters
        request.addRequestParameter("resourcePath", "/content");
        String csvData = "Labelname,EN,NL\nhello,Hello,Hallo\n";
        InputStream csvStream = new ByteArrayInputStream(csvData.getBytes());
        request.addRequestParameter("csvfile", csvStream.readAllBytes(), "text/csv", "translations.csv");

        // Call servlet's doPost method
        underTest.doPost(request, response);

        // Assert successful response
        assertEquals(200, response.getStatus());

        // Get the resourceResolver and assert that the nodes have been created
        ResourceResolver resourceResolver = context.resourceResolver();
        Resource enHelloResource = resourceResolver.getResource("/content/EN/hello");
        assertNotNull(enHelloResource);

        // Check the value of the sling:message property
        ValueMap properties = enHelloResource.getValueMap();
        String message = properties.get("sling:message", String.class);
        assertEquals("Hello", message);

        Resource nlHelloResource = resourceResolver.getResource("/content/NL/hello");
        assertNotNull(nlHelloResource);

        // Check the value of the sling:message property for Dutch
        properties = nlHelloResource.getValueMap();
        message = properties.get("sling:message", String.class);
        assertEquals("Hallo", message);
    }
}
