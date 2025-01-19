package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider;
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
class DeleteMessageEntryServletTest {

    private final AemContext context = new AemContext();

    DeleteMessageEntryServlet servlet;

    DictionaryService dictionaryService;

    @Mock
    Replicator replicator;

    @BeforeEach
    void beforeEach() {
        replicator = context.registerService(Replicator.class, replicator);

        dictionaryService = context.registerInjectActivateService(new DictionaryServiceImpl());

        servlet = context.registerInjectActivateService(new DeleteMessageEntryServlet());
    }

    @Test
    void doPostWithoutParams() throws IOException {
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void deleteMessageEntryWithNonExistingKey() throws IOException {
        context.request().setParameterMap(Map.of(
                DeleteMessageEntryServlet.ITEM_PARAM, "/content/dictionaries/i18n/en/apple"
        ));

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void deleteExistingMessageEntry() throws IOException, ReplicationException {
        context.create().resource("/content/dictionaries/i18n/en/appel");
        context.create().resource("/content/dictionaries/i18n/fr/appel");
        context.create().resource("/mnt/dictionaries/content/dictionaries/i18n/appel",
                CombiningMessageEntryResourceProvider.MESSAGE_ENTRY_PATHS, new String[]{"/content/dictionaries/i18n/en/appel", "/content/dictionaries/i18n/fr/appel"});

        context.create().resource("/content/dictionaries/i18n/en/peer");
        context.create().resource("/content/dictionaries/i18n/fr/peer");
        context.create().resource("/mnt/dictionaries/content/dictionaries/i18n/peer",
                CombiningMessageEntryResourceProvider.MESSAGE_ENTRY_PATHS, new String[]{"/content/dictionaries/i18n/en/peer", "/content/dictionaries/i18n/fr/peer"});

        context.create().resource("/content/dictionaries/i18n/en/framboos");
        context.create().resource("/content/dictionaries/i18n/fr/framboos");
        context.create().resource("/mnt/dictionaries/content/dictionaries/i18n/framboos",
                CombiningMessageEntryResourceProvider.MESSAGE_ENTRY_PATHS, new String[]{"/content/dictionaries/i18n/en/framboos", "/content/dictionaries/i18n/fr/framboos"});

        context.request().setParameterMap(Map.of(
                DeleteMessageEntryServlet.ITEM_PARAM, new String[]{"/mnt/dictionaries/content/dictionaries/i18n/appel"}
        ));

        servlet.doPost(context.request(), context.response());

        assertNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en/appel"));
        verify(replicator).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq("/content/dictionaries/i18n/en/appel"));
        verify(replicator).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq("/content/dictionaries/i18n/fr/appel"));

        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en/peer"));
        verify(replicator, times(0)).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq("/content/dictionaries/i18n/en/peer"));

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void deleteMultipleMessageEntries() throws IOException {
        context.create().resource("/content/dictionaries/i18n/en/appel");
        context.create().resource("/content/dictionaries/i18n/fr/appel");
        context.create().resource("/mnt/dictionaries/content/dictionaries/i18n/appel",
                CombiningMessageEntryResourceProvider.MESSAGE_ENTRY_PATHS, new String[]{"/content/dictionaries/i18n/en/appel", "/content/dictionaries/i18n/fr/appel"});

        context.create().resource("/content/dictionaries/i18n/en/peer");
        context.create().resource("/content/dictionaries/i18n/fr/peer");
        context.create().resource("/mnt/dictionaries/content/dictionaries/i18n/peer",
                CombiningMessageEntryResourceProvider.MESSAGE_ENTRY_PATHS, new String[]{"/content/dictionaries/i18n/en/peer", "/content/dictionaries/i18n/fr/peer"});

        context.create().resource("/content/dictionaries/i18n/en/framboos");
        context.create().resource("/content/dictionaries/i18n/fr/framboos");
        context.create().resource("/mnt/dictionaries/content/dictionaries/i18n/framboos",
                CombiningMessageEntryResourceProvider.MESSAGE_ENTRY_PATHS, new String[]{"/content/dictionaries/i18n/en/framboos", "/content/dictionaries/i18n/fr/framboos"});

        context.request().setParameterMap(Map.of(
                DeleteMessageEntryServlet.ITEM_PARAM, new String[]{"/mnt/dictionaries/content/dictionaries/i18n/appel", "/mnt/dictionaries/content/dictionaries/i18n/peer"}
        ));

        servlet.doPost(context.request(), context.response());

        assertNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en/appel"));
        assertNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en/peer"));
        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en/framboos"));
        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void deleteNonExistingMessageEntry() throws IOException {
        context.create().resource("/content/dictionaries/i18n/en/appel");
        context.request().setParameterMap(Map.of(
                DeleteMessageEntryServlet.ITEM_PARAM, new String[]{"/content/dictionaries/i18n/fr/peer"}
        ));

        servlet.doPost(context.request(), context.response());

        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en/appel"));
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

}
