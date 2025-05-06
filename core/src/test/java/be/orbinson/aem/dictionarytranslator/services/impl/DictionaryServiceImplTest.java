package be.orbinson.aem.dictionarytranslator.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.models.Dictionary;
import be.orbinson.aem.dictionarytranslator.models.impl.DictionaryImpl;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class DictionaryServiceImplTest {

    private final AemContext context = new AemContext();

    DictionaryService dictionaryService;

    @Mock
    Replicator replicator;

    @BeforeEach
    void setup() {
        context.addModelsForClasses(DictionaryImpl.class);

        replicator = context.registerService(Replicator.class, replicator);
        dictionaryService = context.registerInjectActivateService(new DictionaryServiceImpl());

        context.load().json("/content.json", "/content");
    }

    @Test
    void dictionaryListShouldNotContainDuplicates() {
        ResourceResolver resourceResolver = spy(context.resourceResolver());

        doReturn(List.of(
                        context.resourceResolver().getResource("/content/dictionaries/fruit/i18n"),
                        context.resourceResolver().getResource("/content/dictionaries/fruit/i18n"),
                        context.resourceResolver().getResource("/content/dictionaries/vegetables/i18n")
                ).iterator()
        ).when(resourceResolver).findResources(anyString(), anyString());

        List<Resource> dictionaries = dictionaryService.getDictionaries(resourceResolver);

        assertEquals(2, dictionaries.size());
        assertEquals("/content/dictionaries/fruit/i18n", dictionaries.get(0).getPath());
        assertEquals("/content/dictionaries/vegetables/i18n", dictionaries.get(1).getPath());
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    void dictionaryShouldBeEditableWhenPrivilegesAndCapabilitiesAreFullfilled(boolean hasPrivileges, boolean hasCapability) throws RepositoryException {
        Session session = Mockito.mock(Session.class);
        context.registerAdapter(ResourceResolver.class, Session.class, session);
        AccessControlManager acm = Mockito.mock(AccessControlManager.class);
        when(session.getAccessControlManager()).thenReturn(acm);
        when(session.hasCapability(anyString(), any(), any())).thenReturn(hasCapability);
        Mockito.lenient().when(acm.hasPrivileges(anyString(), any())).thenReturn(hasPrivileges);

        context.currentResource("/content/dictionaries/fruit/i18n");

        assertEquals(hasPrivileges && hasCapability, dictionaryService.isEditableDictionary(context.currentResource()));
    }

    @Test
    void dictionaryShouldNotBeEditableWhenPrivilegesCanNotBeDetermined() throws RepositoryException {
        Session session = Mockito.mock(Session.class);
        context.registerAdapter(ResourceResolver.class, Session.class, session);
        AccessControlManager acm = Mockito.mock(AccessControlManager.class);
        when(session.getAccessControlManager()).thenReturn(acm);
        when(session.hasCapability(anyString(), any(), any())).thenReturn(true);

        doThrow(new RepositoryException("Failed to determine privileges")).when(acm).hasPrivileges(anyString(), any());

        context.currentResource("/content/dictionaries/fruit/i18n");

        assertFalse(dictionaryService.isEditableDictionary(context.currentResource()));
    }

    @Test
    void dictionaryServiceShouldReturnCorrectLanguages() {
        Resource dictionary = context.currentResource("/content/dictionaries/fruit/i18n");

        assertEquals(List.of("en", "nl_BE"), dictionaryService.getLanguages(dictionary));
    }

    @Test
    void dictionaryServiceShouldBeAbleToAddNewLanguage() throws PersistenceException {
        Resource dictionaryResource = context.currentResource("/content/dictionaries/fruit/i18n");

        dictionaryService.addLanguage(dictionaryResource, "fr", "/content/dictionaries/fruit/i18n");

        assertEquals(List.of("en", "fr", "nl_BE"), dictionaryService.getLanguages(dictionaryResource));
    }

    @Test
    void dictionaryServiceShouldAddCorrectBasenameIfEmpty() throws PersistenceException {
        Resource dictionaryResource = context.currentResource("/content/dictionaries/fruit/i18n");

        dictionaryService.addLanguage(dictionaryResource, "fr", "");

        Dictionary dictionary = context.request().adaptTo(Dictionary.class);

        assertEquals("/content/dictionaries/fruit/i18n", dictionary.getBasename());
    }

    @Test
    void dictionaryServiceShouldBeAbleToCreateDictionary() throws PersistenceException {
        Resource parent = context.currentResource("/content/dictionaries");

        dictionaryService.createDictionary(parent, "meat", new String[]{"en", "ar"}, null);

        context.currentResource("/content/dictionaries/meat/i18n");
        Dictionary dictionary = context.request().adaptTo(Dictionary.class);

        assertNotNull(dictionary);
        assertEquals(List.of("ar", "en"), dictionary.getLanguages());
    }

    @Test
    void dictionaryServiceShouldBeAbleToDeleteDictionary() throws DictionaryException {
        assertNotNull(context.resourceResolver().getResource("/content/dictionaries/fruit/i18n"));

        dictionaryService.deleteDictionary(context.resourceResolver(), "/content/dictionaries/fruit/i18n");

        assertNull(context.resourceResolver().getResource("/content/dictionaries/fruit/i18n"));
    }

    @Test
    void replicationExceptionOnDeletingDictionaryShouldThrowNewException() throws ReplicationException {
        doThrow(new ReplicationException("Replication failed")).when(replicator).replicate(any(), any(), any());

        assertThrows(DictionaryException.class, () -> {
            dictionaryService.deleteDictionary(context.resourceResolver(), "/content/dictionaries/fruit/i18n");
        });
    }

    @Test
    void deletingDictionaryShouldDeactivate() throws DictionaryException, ReplicationException {
        dictionaryService.deleteDictionary(context.resourceResolver(), "/content/dictionaries/fruit/i18n");

        verify(replicator, times(1)).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq("/content/dictionaries/fruit/i18n"));
    }

    @Test
    void deletingNonExistingDictionaryShouldThrowExeption() {
        assertThrows(DictionaryException.class, () -> {
            dictionaryService.deleteDictionary(context.resourceResolver(), "/content/dictionaries/nonexisting/i18n");
        });
    }

    @Test
    void dictionaryServiceShouldBeAbleToDeleteLanguage() throws DictionaryException {
        context.currentResource("/content/dictionaries/fruit/i18n");

        dictionaryService.deleteLanguage(context.resourceResolver(), context.currentResource(), "en");

        assertEquals(List.of("nl_BE"), dictionaryService.getLanguages(context.currentResource()));
    }

    @Test
    void deletingNonExistingLanguageShouldThrowExeption() {
        context.currentResource("/content/dictionaries/fruit/i18n");
        assertThrows(DictionaryException.class, () -> {
            dictionaryService.deleteLanguage(context.resourceResolver(), context.currentResource(), "ar");
        });
    }

    @Test
    void dictionaryShouldUpdateMessageEntry() throws PersistenceException, RepositoryException {
        context.currentResource("/content/dictionaries/fruit/i18n");

        dictionaryService.updateMessageEntry(context.resourceResolver(), context.currentResource(), "en", "apple", "Not a banana");

        assertEquals("Not a banana", context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en/apple").getValueMap().get("sling:message"));
    }

    @Test
    void dictionaryShouldUpdateNonExistingMessageEntry() throws PersistenceException, RepositoryException {
        context.currentResource("/content/dictionaries/fruit/i18n");

        dictionaryService.updateMessageEntry(context.resourceResolver(), context.currentResource(), "en", "kaboeboe", "Kaboeboe");

        assertEquals("Kaboeboe", context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en/kaboeboe").getValueMap().get("sling:message"));
    }

    @Test
    void dictionaryWithEmptyMessageShouldDeleteMessageEntry() throws PersistenceException, RepositoryException {
        context.currentResource("/content/dictionaries/fruit/i18n");

        dictionaryService.updateMessageEntry(context.resourceResolver(), context.currentResource(), "en", "apple", "");

        assertFalse(context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en/apple").getValueMap().containsKey("sling:message"));
    }
}
