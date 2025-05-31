package be.orbinson.aem.dictionarytranslator.services.impl;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.testing.resourceresolver.MockFindQueryResources;
import org.apache.sling.testing.resourceresolver.MockFindResourcesHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.LanguageDictionary;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class DictionaryServiceImplTest {

    private final AemContext context = new AemContext();

    DictionaryServiceImpl dictionaryService;

    @Mock
    Replicator replicator;
    
    /** Contains the paths of all resources which are returned by the findResources() method, necessary because the RR mock does not support queries natively */
    List<String> dictionaryPaths;

    @BeforeEach
    void setup() {
        dictionaryService = context.registerInjectActivateService(new DictionaryServiceImpl());

        context.load().json("/content.json", "/content");
        dictionaryPaths = new ArrayList<>(List.of(
                "/content/dictionaries/fruit/i18n/en",
                "/content/dictionaries/fruit/i18n/nl_be",
                "/content/dictionaries/vegetables/i18n/en")
        );
        MockFindResourcesHandler handler = new MockFindResourcesHandler() {
            @Override
            public @Nullable Iterator<Resource> findResources(@NotNull String query, String language) {
                return dictionaryPaths.stream().map(p -> context.resourceResolver().getResource(p)).iterator();
            }
        };
        MockFindQueryResources.addFindResourceHandler(context.resourceResolver(), handler);
    }

    @Test
    void dictionaryListShouldNotContainDuplicates() {
        Collection<LanguageDictionary> dictionaries = dictionaryService.getAllDictionaries(context.resourceResolver());

        assertEquals(List.of("/content/dictionaries/fruit/i18n/en", 
                "/content/dictionaries/fruit/i18n/nl_be",
                "/content/dictionaries/vegetables/i18n/en"), 
                dictionaries.stream().map(LanguageDictionary::getPath).collect(Collectors.toList()));
    }


    @Test
    void dictionaryServiceShouldBeAbleToAddNewLanguage() throws PersistenceException, DictionaryException {
        dictionaryService.createDictionary(context.resourceResolver(), "/content/dictionaries/fruit/i18n", Locale.FRENCH, Collections.singleton("/content/dictionaries/fruit/i18n"));
        dictionaryService.onChange(List.of(new ResourceChange(ChangeType.ADDED, "/content/dictionaries/fruit/i18n/fr", false)));
        // return additional path in context.resourceResolver().findResources()
        dictionaryPaths.add("/content/dictionaries/fruit/i18n/fr");
        assertEquals(List.of("en", "fr", "nl-BE"), dictionaryService.getDictionaries(context.resourceResolver(), "/content/dictionaries/fruit/i18n").stream()
                .map(d -> d.getLanguage().toLanguageTag())
                .collect(Collectors.toList()));
    }

    @Test
    void dictionaryServiceShouldAddCorrectBasenameIfEmpty() throws PersistenceException, DictionaryException {
        dictionaryService.createDictionary(context.resourceResolver(), "/content/dictionaries/fruit/i18n", Locale.FRENCH, Collections.emptyList());

        // return additional path in context.resourceResolver().findResources()
        dictionaryPaths.add("/content/dictionaries/fruit/i18n/fr");

        LanguageDictionary dictionary = dictionaryService.getDictionary(context.resourceResolver(), "/content/dictionaries/fruit/i18n", Locale.FRENCH).orElse(null);
        assertNotNull(dictionary);
        assertIterableEquals(List.of("/content/dictionaries/fruit/i18n"), dictionary.getBaseNames());
    }

    
    @Test
    void dictionaryServiceShouldBeAbleToCreateDictionaries() throws PersistenceException, DictionaryException {
        dictionaryService.createDictionaries(context.resourceResolver(), "/content/dictionaries/meat", List.of(Locale.ENGLISH, new Locale("ar")), Collections.emptyList());

        // return additional paths in context.resourceResolver().findResources()
        dictionaryPaths.add("/content/dictionaries/meat/en");
        dictionaryPaths.add("/content/dictionaries/meat/ar");
        assertEquals(List.of("ar", "en"), dictionaryService.getDictionaries(context.resourceResolver(), "/content/dictionaries/meat").stream()
                .map(d -> d.getLanguage().toLanguageTag())
                .collect(Collectors.toList()));
    }

    @Test
    void dictionaryServiceShouldBeAbleToDeleteDictionary() throws DictionaryException, PersistenceException, ReplicationException {
        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/fruit/i18n"));

        dictionaryService.deleteDictionaries(replicator, context.resourceResolver(), "/content/dictionaries/fruit/i18n");

        assertNull(context.resourceResolver().getResource("/content/dictionaries/fruit/i18n"));
    }

    @Test
    void replicationExceptionOnDeletingDictionaryShouldThrowNewException() throws ReplicationException {
        doThrow(new ReplicationException("Replication failed")).when(replicator).replicate(any(), any(), any());

        assertThrows(ReplicationException.class, () -> {
            dictionaryService.deleteDictionaries(replicator, context.resourceResolver(), "/content/dictionaries/fruit/i18n");
        });
    }

    @Test
    void deletingDictionaryShouldDeactivate() throws DictionaryException, ReplicationException, PersistenceException {
        dictionaryService.deleteDictionaries(replicator, context.resourceResolver(), "/content/dictionaries/fruit/i18n");

        verify(replicator, times(1)).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq("/content/dictionaries/fruit/i18n"));
    }

    @Test
    void deletingNonExistingDictionaryShouldThrowExeption() {
        assertThrows(DictionaryException.class, () -> {
            dictionaryService.deleteDictionaries(replicator, context.resourceResolver(), "/content/dictionaries/nonexisting/i18n");
        });
    }

    @Test
    void dictionaryServiceShouldBeAbleToDeleteLanguage() throws DictionaryException, PersistenceException, ReplicationException {

        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en"));
        dictionaryService.deleteDictionary(replicator, context.resourceResolver(), "/content/dictionaries/fruit/i18n", Locale.ENGLISH);

        assertNull(context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en"));
    }

    @Test
    void deletingNonExistingLanguageShouldThrowExeption() {
        context.currentResource("/content/dictionaries/fruit/i18n");
        assertThrows(DictionaryException.class, () -> {
            dictionaryService.deleteDictionary(replicator, context.resourceResolver(), "/content/dictionaries/fruit/i18n", new Locale("ar"));
        });
    }

    @Test
    void dictionaryShouldReturnConflictingDictionaryForSameKeyHigherPrecedence() {
        context.load().json("/content.json", "/apps");
        // return additional paths in context.resourceResolver().findResources()
        dictionaryPaths.add("/apps/dictionaries/fruit/i18n/en");
        dictionaryPaths.add("/apps/dictionaries/fruit/i18n/nl_be");
        dictionaryPaths.add("/apps/dictionaries/vegetables/i18n/en");

        LanguageDictionary dictionary = dictionaryService.getDictionary(context.resourceResolver(), "/content/dictionaries/fruit/i18n", Locale.ENGLISH).orElse(null);
        assertNotNull(dictionary);
        assertEquals("/apps/dictionaries/fruit/i18n/en", dictionaryService.getConflictingDictionary(context.resourceResolver(), dictionary, "apple").get().getPath());
    }

    @Test
    void dictionaryShouldNotReturnConflictingDictionaryForSameKeyLowerPrecedence() {
        context.load().json("/content.json", "/apps");
        // return additional paths in context.resourceResolver().findResources()
        dictionaryPaths.add("/apps/dictionaries/fruit/i18n/en");
        dictionaryPaths.add("/apps/dictionaries/fruit/i18n/nl_be");
        dictionaryPaths.add("/apps/dictionaries/vegetables/i18n/en");

        LanguageDictionary dictionary = dictionaryService.getDictionary(context.resourceResolver(), "/apps/dictionaries/fruit/i18n", Locale.ENGLISH).orElse(null);
        assertNotNull(dictionary);
        assertEquals(Optional.empty(), dictionaryService.getConflictingDictionary(context.resourceResolver(), dictionary, "apple"));
    }
}
