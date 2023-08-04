package be.orbinson.aem.dictionarytranslator.servlets.action;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({AemContextExtension.class})
class DeleteLanguageServletTest {
    private final AemContext context = new AemContext();

    DeleteLanguageServlet servlet;

    @BeforeEach
    void beforeEach() {
        servlet = context.registerService(new DeleteLanguageServlet());
    }

    @Test
    void doPostWithoutParams() throws ServletException, IOException {
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of());

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void deleteExistingLanguage() throws ServletException, IOException {
        context.create().resource("/content/dictionaries/i18n/en");
        context.create().resource("/content/dictionaries/i18n/fr");
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "dictionary", new String[]{"/content/dictionaries/i18n"},
                "language", "fr"
        ));

        servlet.service(context.request(), context.response());

        assertNull(context.resourceResolver().getResource("/content/dictionaries/i18n/fr"));
        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en"));
        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void deleteNonExistingLanguage() throws ServletException, IOException {
        context.create().resource("/content/dictionaries/i18n/en");
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "dictionary", new String[]{"/content/dictionaries/i18n"},
                "language", "fr"
        ));

        servlet.service(context.request(), context.response());

        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en"));
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }
}
