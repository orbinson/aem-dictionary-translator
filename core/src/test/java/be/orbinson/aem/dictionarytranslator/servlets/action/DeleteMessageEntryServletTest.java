package be.orbinson.aem.dictionarytranslator.servlets.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.builder.ContentBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService.Message;
import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider;
import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

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

    private void createDictionaryResourceWithLanguages(ContentBuilder builder, String path, String... languages) {
        builder.resource(path);
        for (String language : languages) {
            builder.resource(path + "/" + language, JcrConstants.JCR_LANGUAGE, language);
        }
    }
    
    private void createCombiningMessageEntryResource(ContentBuilder builder, String dictionaryPath, String key, Map<String, Message> messagePerLanguage) {
        String path = CombiningMessageEntryResourceProvider.ROOT + dictionaryPath + "/" + key;
        builder.resource(path, CombiningMessageEntryResourceProvider.createResourceProperties(path, true, messagePerLanguage, Optional.empty()));
        for (String language : messagePerLanguage.keySet()) {
            builder.resource(dictionaryPath + "/" + language + "/" + key, ResourceResolver.PROPERTY_RESOURCE_TYPE, DictionaryConstants.SLING_MESSAGEENTRY);
        }
    }

    @Test
    void deleteExistingMessageEntry() throws IOException, ReplicationException {
        createDictionaryResourceWithLanguages(context.create(), "/content/dictionaries/i18n", "en", "fr");
        Map<String, Message> messagePerLanguage = Map.of(
                "en", new Message("appel", "/content/dictionaries/i18n/en/appel"),
                "fr", new Message("appel", "/content/dictionaries/i18n/fr/appel")
        );
        createCombiningMessageEntryResource(context.create(), "/content/dictionaries/i18n", "appel", messagePerLanguage);
        
        messagePerLanguage = Map.of(
                "en", new Message("appel", "/content/dictionaries/i18n/en/peer"),
                "fr", new Message("appel", "/content/dictionaries/i18n/fr/peer")
        );
        createCombiningMessageEntryResource(context.create(), "/content/dictionaries/i18n", "peer", messagePerLanguage);
        
        messagePerLanguage = Map.of(
                "en", new Message("appel", "/content/dictionaries/i18n/en/framboos"),
                "fr", new Message("appel", "/content/dictionaries/i18n/fr/framboos")
        );
        createCombiningMessageEntryResource(context.create(), "/content/dictionaries/i18n", "framboos", messagePerLanguage);
        
        context.request().setParameterMap(Map.of(
                DeleteMessageEntryServlet.ITEM_PARAM, new String[]{"/mnt/dictionary/content/dictionaries/i18n/appel"}
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
        createDictionaryResourceWithLanguages(context.create(), "/content/dictionaries/i18n", "en", "fr");
        Map<String, Message> messagePerLanguage = Map.of(
                "en", new Message("appel", "/content/dictionaries/i18n/en/appel"),
                "fr", new Message("appel", "/content/dictionaries/i18n/fr/appel")
        );
        createCombiningMessageEntryResource(context.create(), "/content/dictionaries/i18n", "appel", messagePerLanguage);
        
        messagePerLanguage = Map.of(
                "en", new Message("appel", "/content/dictionaries/i18n/en/peer"),
                "fr", new Message("appel", "/content/dictionaries/i18n/fr/peer")
        );
        createCombiningMessageEntryResource(context.create(), "/content/dictionaries/i18n", "peer", messagePerLanguage);
        
        messagePerLanguage = Map.of(
                "en", new Message("appel", "/content/dictionaries/i18n/en/framboos"),
                "fr", new Message("appel", "/content/dictionaries/i18n/fr/framboos")
        );
        createCombiningMessageEntryResource(context.create(), "/content/dictionaries/i18n", "framboos", messagePerLanguage);

        context.request().setParameterMap(Map.of(
                DeleteMessageEntryServlet.ITEM_PARAM, new String[]{"/mnt/dictionary/content/dictionaries/i18n/appel", "/mnt/dictionary/content/dictionaries/i18n/peer"}
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
