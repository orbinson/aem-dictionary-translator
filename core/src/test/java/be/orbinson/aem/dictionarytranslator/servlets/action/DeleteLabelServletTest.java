package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import com.adobe.granite.translation.api.TranslationConfig;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class DeleteLabelServletTest {

    private final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    DeleteLabelServlet servlet;


    DictionaryService dictionaryService;

    @Mock
    TranslationConfig translationConfig;

    @Mock
    Replicator replicator;

    @BeforeEach
    void beforeEach() {
        translationConfig = context.registerService(TranslationConfig.class, translationConfig);
        replicator = context.registerService(Replicator.class, replicator);

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
                DeleteLabelServlet.LABEL_PARAM, "/content/dictionaries/i18n/en/apple"
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void deleteExistingLabel() throws ServletException, IOException, ReplicationException {
        context.create().resource("/mnt/dictionaries/i18n/appel",
                "labelPaths", new String[]{"/content/dictionaries/i18n/appel/fr", "/content/dictionaries/i18n/appel/en"}
        );
        context.create().resource("/content/dictionaries/i18n/peer");
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                DeleteLabelServlet.LABEL_PARAM, new String[]{"/mnt/dictionaries/i18n/appel"}
        ));

        servlet.service(context.request(), context.response());

        assertNull(context.resourceResolver().getResource("/content/dictionaries/i18n/appel"));
        verify(replicator).replicate(any(Session.class), eq(ReplicationActionType.DEACTIVATE), eq("/content/dictionaries/i18n/appel/fr"));
        verify(replicator).replicate(any(Session.class), eq(ReplicationActionType.DEACTIVATE), eq("/content/dictionaries/i18n/appel/en"));

        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/i18n/peer"));
        verify(replicator, times(0)).replicate(any(Session.class), eq(ReplicationActionType.DEACTIVATE), eq("/content/dictionaries/i18n/peer"));

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void deleteMultipleLabels() throws ServletException, IOException {
        context.create().resource("/content/dictionaries/i18n/en/appel");
        context.create().resource("/content/dictionaries/i18n/en/peer");
        context.create().resource("/content/dictionaries/i18n/en/framboos");
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                DeleteLabelServlet.LABEL_PARAM, new String[]{"/content/dictionaries/i18n/en/appel", "/content/dictionaries/i18n/en/peer"}
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
                DeleteLabelServlet.LABEL_PARAM, new String[]{"/content/dictionaries/i18n/fr/peer"}
        ));

        servlet.service(context.request(), context.response());

        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en/appel"));
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

}
