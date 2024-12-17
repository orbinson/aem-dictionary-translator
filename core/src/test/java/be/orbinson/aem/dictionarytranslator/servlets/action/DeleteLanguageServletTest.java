package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import static com.day.cq.commons.jcr.JcrConstants.JCR_LANGUAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class DeleteLanguageServletTest {
    private final AemContext context = new AemContext();

    DeleteLanguageServlet servlet;

    @Mock
    Replicator replicator;

    @BeforeEach
    void beforeEach() {
        replicator = context.registerService(Replicator.class, replicator);
        context.registerInjectActivateService(new DictionaryServiceImpl());
        servlet = context.registerInjectActivateService(new DeleteLanguageServlet());
    }

    @Test
    void doPostWithoutParams() throws IOException {
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void deleteExistingLanguage() throws IOException, ReplicationException {
        context.create().resource("/content/dictionaries/i18n/en", Map.of(JCR_LANGUAGE, "en"));
        context.create().resource("/content/dictionaries/i18n/fr", Map.of(JCR_LANGUAGE, "fr"));
        context.request().setParameterMap(Map.of(
                DeleteLanguageServlet.DICTIONARY_PARAM, new String[]{"/content/dictionaries/i18n"},
                DeleteLanguageServlet.LANGUAGE_PARAM, "fr"
        ));

        servlet.doPost(context.request(), context.response());

        assertNull(context.resourceResolver().getResource("/content/dictionaries/i18n/fr"));
        verify(replicator).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq("/content/dictionaries/i18n/fr"));

        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en"));
        verify(replicator, times(0)).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq("/content/dictionaries/i18n/en"));

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void deleteNonExistingLanguage() throws IOException {
        context.create().resource("/content/dictionaries/i18n/en");
        context.request().setParameterMap(Map.of(
                DeleteLanguageServlet.DICTIONARY_PARAM, new String[]{"/content/dictionaries/i18n"},
                DeleteLanguageServlet.LANGUAGE_PARAM, "fr"
        ));

        servlet.doPost(context.request(), context.response());

        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en"));
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }
}
