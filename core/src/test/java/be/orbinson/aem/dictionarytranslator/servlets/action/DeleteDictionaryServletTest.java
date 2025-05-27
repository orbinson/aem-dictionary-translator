package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class DeleteDictionaryServletTest {
    private final AemContext context = new AemContext();

    DeleteDictionaryServlet servlet;

    @Mock
    Replicator replicator;

    @BeforeEach
    void beforeEach() {
        replicator = context.registerService(Replicator.class, replicator);
        context.registerInjectActivateService(new DictionaryServiceImpl());
        servlet = context.registerInjectActivateService(new DeleteDictionaryServlet());
        context.request().setMethod("POST");
    }

    @Test
    void doPostWithoutParams() throws IOException, ServletException {
        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void deleteExistingDictionary() throws IOException, ReplicationException, ServletException {
        String dictionaryToDelete = "/content/dictionaries/site-a/i18n";
        context.create().resource(dictionaryToDelete);
        String dictionaryToKeep = "/content/dictionaries/site-b/i18n";
        context.create().resource(dictionaryToKeep);

        context.request().setParameterMap(Map.of(
                DeleteDictionaryServlet.DICTIONARIES_PARAM, new String[]{dictionaryToDelete}
        ));

        servlet.service(context.request(), context.response());

        assertNull(context.resourceResolver().getResource(dictionaryToDelete));
        verify(replicator).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq(dictionaryToDelete));

        assertNotNull(context.resourceResolver().getResource(dictionaryToKeep));
        verify(replicator, times(0)).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq(dictionaryToKeep));

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void deleteMultipleDictionaries() throws IOException {
        context.create().resource("/content/dictionaries/site-a/i18n");
        context.create().resource("/content/dictionaries/site-b/i18n");
        context.create().resource("/content/dictionaries/site-c/i18n");
        context.request().setParameterMap(Map.of(
                DeleteDictionaryServlet.DICTIONARIES_PARAM, new String[]{"/content/dictionaries/site-a/i18n", "/content/dictionaries/site-b/i18n"}
        ));

        servlet.doPost(context.request(), context.response());

        assertNull(context.resourceResolver().getResource("/content/dictionaries/site-a/i18n"));
        assertNull(context.resourceResolver().getResource("/content/dictionaries/site-b/i18n"));
        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/site-c/i18n"));
        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void deleteNonExistingDictionary() throws IOException {
        context.create().resource("/content/dictionaries/site-a/i18n");
        context.request().setParameterMap(Map.of(
                DeleteDictionaryServlet.DICTIONARIES_PARAM, new String[]{"/content/dictionaries/site-b/i18n"}
        ));

        servlet.doPost(context.request(), context.response());

        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/site-a/i18n"));
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }
}
