package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import com.adobe.granite.translation.api.TranslationConfig;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
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
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class DeleteLabelServletTest {

    private final AemContext context = new AemContext();

    DeleteLabelServlet servlet;


    DictionaryService dictionaryService;

    @Mock
    TranslationConfig translationConfig;

    @BeforeEach
    void beforeEach() {
        translationConfig = context.registerService(TranslationConfig.class, translationConfig);
        dictionaryService = context.registerInjectActivateService(new DictionaryServiceImpl());

        servlet = context.registerInjectActivateService(new DeleteLabelServlet());
    }

    @Test
    void doPostWithoutParams() throws ServletException, IOException {
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of());

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void deleteLabelWithNonExistingKey() throws ServletException, IOException {
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "labels", "/content/dictionaries/i18n/en/apple"
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void deleteExistingLabel() throws ServletException, IOException {
        context.create().resource("/content/dictionaries/i18n/appel");
        context.create().resource("/content/dictionaries/i18n/peer");
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "labels", new String[]{"/content/dictionaries/i18n/appel"}
        ));

        servlet.service(context.request(), context.response());

        assertNull(context.resourceResolver().getResource("/content/dictionaries/i18n/appel"));
        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/i18n/peer"));
        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void deleteMultipleLabels() throws ServletException, IOException {
        context.create().resource("/content/dictionaries/i18n/en/appel");
        context.create().resource("/content/dictionaries/i18n/en/peer");
        context.create().resource("/content/dictionaries/i18n/en/framboos");
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "labels", new String[]{"/content/dictionaries/i18n/en/appel,/content/dictionaries/i18n/en/peer"}
        ));

        servlet.service(context.request(), context.response());

        assertNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en/appel"));
        assertNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en/peer"));
        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en/framboos"));
        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void deleteNonExistingLabel() throws ServletException, IOException {
        context.create().resource("/content/dictionaries/i18n/en/appel");
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "labels", new String[]{"/content/dictionaries/i18n/fr/peer"}
        ));

        servlet.service(context.request(), context.response());

        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en/appel"));
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

}
