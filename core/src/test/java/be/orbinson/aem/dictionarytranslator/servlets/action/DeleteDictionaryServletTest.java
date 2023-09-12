package be.orbinson.aem.dictionarytranslator.servlets.action;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({AemContextExtension.class})
class DeleteDictionaryServletTest {
    private final AemContext context = new AemContext();

    DeleteDictionaryServlet servlet;

    @BeforeEach
    void beforeEach() {
        servlet = context.registerService(new DeleteDictionaryServlet());
    }

    @Test
    void doPostWithoutParams() throws ServletException, IOException {
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of());

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    @Disabled("Temporary disabled to test CI")
    void deleteExistingDictionary() throws ServletException, IOException {
        context.create().resource("/content/dictionaries/site-a/i18n");
        context.create().resource("/content/dictionaries/site-b/i18n");
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "dictionary", new String[]{"/content/dictionaries/site-a/i18n"}
        ));

        servlet.service(context.request(), context.response());

        assertNull(context.resourceResolver().getResource("/content/dictionaries/site-a/i18n"));
        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/site-b/i18n"));
        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    @Disabled("Temporary disabled to test CI")
    void deleteMultipleDictionaries() throws ServletException, IOException {
        context.create().resource("/content/dictionaries/site-a/i18n");
        context.create().resource("/content/dictionaries/site-b/i18n");
        context.create().resource("/content/dictionaries/site-c/i18n");
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "dictionary", new String[]{"/content/dictionaries/site-a/i18n,/content/dictionaries/site-b/i18n"}
        ));

        servlet.service(context.request(), context.response());

        assertNull(context.resourceResolver().getResource("/content/dictionaries/site-a/i18n"));
        assertNull(context.resourceResolver().getResource("/content/dictionaries/site-b/i18n"));
        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/site-c/i18n"));
        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    @Disabled("Temporary disabled to test CI")
    void deleteNonExistingDictionary() throws ServletException, IOException {
        context.create().resource("/content/dictionaries/site-a/i18n");
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "dictionary", new String[]{"/content/dictionaries/site-b/i18n"}
        ));

        servlet.service(context.request(), context.response());

        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/site-a/i18n"));
        assertEquals(HttpServletResponse.SC_NOT_FOUND, context.response().getStatus());
    }
}
