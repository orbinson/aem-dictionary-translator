package be.orbinson.aem.dictionarytranslator.models.impl;

import be.orbinson.aem.dictionarytranslator.models.Dictionary;
import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import com.day.cq.replication.Replicator;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class DictionaryImplTest {

    private final AemContext context = new AemContext();

    @BeforeEach
    public void setUp() {
        context.addModelsForClasses(DictionaryImpl.class);

        context.registerService(Replicator.class, mock(Replicator.class));
        context.registerInjectActivateService(new DictionaryServiceImpl());

        context.load().json("/content.json", "/content");
    }

    @Test
    void dictionaryShouldReturnCorrectLanguages() {
        context.currentResource("/content/dictionaries/fruit/i18n");

        Dictionary dictionary = context.request().adaptTo(Dictionary.class);

        assertEquals(List.of("en", "nl_BE"), dictionary.getLanguages());
    }

    @Test
    void dictionaryShouldReturnCorrectKeyCount() {
        context.currentResource("/content/dictionaries/fruit/i18n");

        Dictionary dictionary = context.request().adaptTo(Dictionary.class);

        assertEquals(3, dictionary.getKeyCount());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void dictionaryShouldBeEditable(boolean hasPrivileges) throws RepositoryException {
        Session session = MockJcr.newSession();
        context.registerAdapter(ResourceResolver.class, Session.class, session);
        AccessControlManager acm = Mockito.mock(AccessControlManager.class);
        MockJcr.setAccessControlManager(session, acm);
        when(acm.hasPrivileges(anyString(), any())).thenReturn(hasPrivileges);

        context.currentResource("/content/dictionaries/fruit/i18n");

        Dictionary dictionary = context.request().adaptTo(Dictionary.class);

        assertEquals(hasPrivileges, dictionary.isEditable());
    }

    @Test
    void dictionaryShouldReturnCorrectKeys() {
        context.currentResource("/content/dictionaries/fruit/i18n");

        Dictionary dictionary = context.request().adaptTo(Dictionary.class);

        assertEquals(List.of("apple", "banana", "cherry"), dictionary.getKeys());
    }

    @Test
    void dictionaryShouldReturnCorrectResource() {
        context.currentResource("/content/dictionaries/fruit/i18n");

        Dictionary dictionary = context.request().adaptTo(Dictionary.class);

        assertEquals("/content/dictionaries/fruit/i18n", dictionary.getResource().getPath());
    }

    @Test
    void dictionaryShouldReturnCorrectBasename() {
        context.currentResource("/content/dictionaries/fruit/i18n");

        Dictionary dictionary = context.request().adaptTo(Dictionary.class);

        assertEquals("/content/dictionaries/fruit/i18n", dictionary.getBasename());
    }

    @Test
    void dictionaryShouldReturnCorrectLanguageList() {
        context.currentResource("/content/dictionaries/fruit/i18n");

        Dictionary dictionary = context.request().adaptTo(Dictionary.class);

        assertEquals("en, nl_BE", dictionary.getLanguageList());
    }
}
