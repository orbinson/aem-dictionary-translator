package be.orbinson.aem.dictionarytranslator.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class DictionaryImplTest {

    private final AemContext context = new AemContext();

    private DictionaryImpl dictionary;

    @BeforeEach
    void setup() {
        context.load().json("/content.json", "/content");
        context.currentResource("/content/dictionaries/fruit/i18n/nl_be");
        
        dictionary = new DictionaryImpl(context.currentResource(), context::resourceResolver) {
            @Override
            public Type getType() {
                throw new UnsupportedOperationException("Not implemented in test");
            }

            @Override
            public void createEntry(ResourceResolver resourceResolver, String key, Optional<String> message)
                    throws PersistenceException, DictionaryException {
                throw new UnsupportedOperationException("Not implemented in test");
            }

            @Override
            public void updateEntry(ResourceResolver resourceResolver, String key, Optional<String> message)
                    throws PersistenceException, DictionaryException {
                throw new UnsupportedOperationException("Not implemented in test");
            }

            @Override
            public void deleteEntry(Replicator replicator, ResourceResolver resourceResolver, String key)
                    throws PersistenceException, ReplicationException, DictionaryException {
                throw new UnsupportedOperationException("Not implemented in test");
            }

            @Override
            public Map<String, Message> loadMessages(Resource dictionaryResource) throws DictionaryException {
                throw new UnsupportedOperationException("Not implemented in test");
            }
        };

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

        assertEquals(hasPrivileges && hasCapability, dictionary.isEditable(context.resourceResolver()));
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

        assertFalse(dictionary.isEditable(context.resourceResolver()));
    }

    @Test
    void dictionaryServiceShouldReturnCorrectLanguage() {
        assertEquals(new Locale("nl", "BE"), dictionary.getLanguage());
    }

    @Test
    void testDictionaryOrdinal() {
        assertEquals(2, dictionary.getOrdinal());
    }
}
