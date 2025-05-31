package be.orbinson.aem.dictionarytranslator.servlets.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.builder.ContentBuilder;
import org.apache.sling.testing.resourceresolver.MockFindQueryResources;
import org.apache.sling.testing.resourceresolver.MockFindResourcesHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import be.orbinson.aem.dictionarytranslator.services.Dictionary.Message;
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

    private List<String> dictionaryPaths;

    @BeforeEach
    void beforeEach() {
        replicator = context.registerService(Replicator.class, replicator);

        dictionaryService = context.registerInjectActivateService(new DictionaryServiceImpl());

        servlet = context.registerInjectActivateService(new DeleteMessageEntryServlet());
        context.request().setMethod("POST");
        
        dictionaryPaths = new ArrayList<>();
        MockFindResourcesHandler handler = new MockFindResourcesHandler() {
            @Override
            public @Nullable Iterator<Resource> findResources(@NotNull String query, String language) {
                return dictionaryPaths.stream().map(p -> context.resourceResolver().getResource(p)).iterator();
            }
        };
        MockFindQueryResources.addFindResourceHandler(context.resourceResolver(), handler);
    }

    @Test
    void doPostWithoutParams() throws IOException, ServletException {
        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void deleteMessageEntryWithNonExistingKey() throws IOException, ServletException {
        context.request().setParameterMap(Map.of(
                DeleteMessageEntryServlet.ITEM_PARAM, "/content/dictionaries/i18n/en/apple"
        ));
        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    private void createDictionaryResourceWithLanguages(ContentBuilder builder, String path, Locale... languages) {
        builder.resource(path);
        for (Locale language : languages) {
            builder.resource(path + "/" + language.toLanguageTag(), JcrConstants.JCR_LANGUAGE, language);
        }
    }
    
    private void createCombiningMessageEntryResource(ContentBuilder builder, String dictionaryPath, String key, Map<Locale, Message> messagePerLanguage) {
        String path = CombiningMessageEntryResourceProvider.ROOT + dictionaryPath + "/" + key;
        builder.resource(path, CombiningMessageEntryResourceProvider.createResourceProperties(path, true, messagePerLanguage, Optional.empty()));
        for (Locale language : messagePerLanguage.keySet()) {
            builder.resource(dictionaryPath + "/" + language.toLanguageTag() + "/" + key, ResourceResolver.PROPERTY_RESOURCE_TYPE, DictionaryConstants.SLING_MESSAGEENTRY);
        }
    }

    @Test
    void deleteExistingMessageEntry() throws IOException, ReplicationException, ServletException {
        createDictionaryResourceWithLanguages(context.create(), "/content/dictionaries/i18n", Locale.ENGLISH, Locale.FRENCH);
        Map<Locale, Message> messagePerLanguage = Map.of(
                Locale.ENGLISH, new Message("appel", "/content/dictionaries/i18n/en/appel"),
                Locale.FRENCH, new Message("appel", "/content/dictionaries/i18n/fr/appel")
        );
        createCombiningMessageEntryResource(context.create(), "/content/dictionaries/i18n", "appel", messagePerLanguage);
        
        messagePerLanguage = Map.of(
                Locale.ENGLISH, new Message("appel", "/content/dictionaries/i18n/en/peer"),
                Locale.FRENCH, new Message("appel", "/content/dictionaries/i18n/fr/peer")
        );
        createCombiningMessageEntryResource(context.create(), "/content/dictionaries/i18n", "peer", messagePerLanguage);
        
        messagePerLanguage = Map.of(
                Locale.ENGLISH, new Message("appel", "/content/dictionaries/i18n/en/framboos"),
                Locale.FRENCH, new Message("appel", "/content/dictionaries/i18n/fr/framboos")
        );
        createCombiningMessageEntryResource(context.create(), "/content/dictionaries/i18n", "framboos", messagePerLanguage);
        context.resourceResolver().commit(); // expose to other resource resolvers
        dictionaryPaths.add("/content/dictionaries/i18n/en");
        dictionaryPaths.add("/content/dictionaries/i18n/fr");
        
        context.request().setParameterMap(Map.of(
                DeleteMessageEntryServlet.ITEM_PARAM, new String[]{"/mnt/dictionary/content/dictionaries/i18n/appel"}
        ));

        servlet.service(context.request(), context.response());

        assertNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en/appel"));
        verify(replicator).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq("/content/dictionaries/i18n/en/appel"));
        verify(replicator).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq("/content/dictionaries/i18n/fr/appel"));

        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en/peer"));
        verify(replicator, times(0)).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq("/content/dictionaries/i18n/en/peer"));

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void deleteMultipleMessageEntries() throws IOException, ServletException {
        createDictionaryResourceWithLanguages(context.create(), "/content/dictionaries/i18n", Locale.ENGLISH, Locale.FRENCH);
        Map<Locale, Message> messagePerLanguage = Map.of(
                Locale.ENGLISH, new Message("appel", "/content/dictionaries/i18n/en/appel"),
                Locale.FRENCH, new Message("appel", "/content/dictionaries/i18n/fr/appel")
        );
        createCombiningMessageEntryResource(context.create(), "/content/dictionaries/i18n", "appel", messagePerLanguage);
        
        messagePerLanguage = Map.of(
                Locale.ENGLISH, new Message("appel", "/content/dictionaries/i18n/en/peer"),
                Locale.FRENCH, new Message("appel", "/content/dictionaries/i18n/fr/peer")
        );
        createCombiningMessageEntryResource(context.create(), "/content/dictionaries/i18n", "peer", messagePerLanguage);
        
        messagePerLanguage = Map.of(
                Locale.ENGLISH, new Message("appel", "/content/dictionaries/i18n/en/framboos"),
                Locale.FRENCH, new Message("appel", "/content/dictionaries/i18n/fr/framboos")
        );
        createCombiningMessageEntryResource(context.create(), "/content/dictionaries/i18n", "framboos", messagePerLanguage);

        context.request().setParameterMap(Map.of(
                DeleteMessageEntryServlet.ITEM_PARAM, new String[]{"/mnt/dictionary/content/dictionaries/i18n/appel", "/mnt/dictionary/content/dictionaries/i18n/peer"}
        ));
        context.resourceResolver().commit(); // expose to other resource resolvers
        dictionaryPaths.add("/content/dictionaries/i18n/en");
        dictionaryPaths.add("/content/dictionaries/i18n/fr");
        servlet.service(context.request(), context.response());

        assertNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en/appel"));
        assertNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en/peer"));
        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en/framboos"));
        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void deleteNonExistingMessageEntry() throws IOException, ServletException {
        context.create().resource("/content/dictionaries/i18n/en/appel");
        context.request().setParameterMap(Map.of(
                DeleteMessageEntryServlet.ITEM_PARAM, new String[]{"/content/dictionaries/i18n/fr/peer"}
        ));

        servlet.service(context.request(), context.response());

        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/i18n/en/appel"));
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }
}
