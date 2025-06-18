package be.orbinson.aem.dictionarytranslator.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.Dictionary;
import be.orbinson.aem.dictionarytranslator.services.Dictionary.Message;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith({ AemContextExtension.class, MockitoExtension.class })
class SlingMessageDictionaryImplTest {

    private final AemContext context = new AemContext();

    private Dictionary dictionary;

    @BeforeEach
    void setUp() {
        context.load().json("/content.json", "/content");
        context.currentResource("/content/dictionaries/fruit/i18n/en");
        dictionary = new SlingMessageDictionaryImpl(context.currentResource(), context::resourceResolver);
    }

    @Test
    void shouldReturnMessages() throws DictionaryException {
        assertEquals(Map.of("apple", new Message("Apple", "/content/dictionaries/fruit/i18n/en/apple"), 
                "banana", new Message("Banana", "/content/dictionaries/fruit/i18n/en/banana"),
                "cherry", new Message("Cherry", "/content/dictionaries/fruit/i18n/en/cherry"),
                "papaya", new Message("Papaya", "/content/dictionaries/fruit/i18n/en/papaya"),
                "mango", new Message("Mango", "/content/dictionaries/fruit/i18n/en/mango"),
                "pear", new Message("", "/content/dictionaries/fruit/i18n/en/pear")),
                dictionary.getEntries());
    }

    @Test
    void dictionaryShouldUpdateMessageEntry() throws PersistenceException, DictionaryException {
        dictionary.updateEntry(context.resourceResolver(), "apple", Optional.of("Not a banana"));

        assertEquals("Not a banana", context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en/apple").getValueMap().get("sling:message"));
    }

    @Test
    void dictionaryShouldUpdateNonExistingMessageEntry() throws PersistenceException, DictionaryException {
        dictionary.updateEntry(context.resourceResolver(), "kaboeboe", Optional.of("Kaboeboe"));

        assertEquals("Kaboeboe", context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en/kaboeboe").getValueMap().get("sling:message"));
    }

    @Test
    void dictionaryWithEmptyMessageShouldNotDeleteMessageEntry() throws PersistenceException, DictionaryException {
        dictionary.updateEntry(context.resourceResolver(), "apple", Optional.of(""));

        assertEquals("", context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en/apple").getValueMap().get("sling:message"));
    }
    
    @Test
    void dictionaryWithMissingMessageShouldNotDeleteMessageEntry() throws PersistenceException, DictionaryException {
        dictionary.updateEntry(context.resourceResolver(), "apple", Optional.empty());

        Resource resource = context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en/apple");
        assertNotNull(resource);
        assertFalse(resource.getValueMap().containsKey("sling:message"));
    }

    @Test
    void dictionaryShouldCreateNonConflictingResourceNameForNewKey() throws PersistenceException, DictionaryException {
        context.create().resource("/content/dictionaries/fruit/i18n/en/generic", Map.of("sling:message", "foo", "sling:key", "original"));
        int numChildrenBefore = IteratorUtils.size(context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en").listChildren());
        // create a item with a key which equals the resource name of an existing item (with another key set in the properties)
        dictionary.createEntry(context.resourceResolver(), "generic", Optional.of("bar"));
        // the old resource should not be overwritten, but a new resource should be created with with a resource name close to the key
        assertEquals("original", context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en/generic").getValueMap().get("sling:key"));
        // one new resource should have been created (name is impl specific)
        assertEquals(numChildrenBefore + 1, IteratorUtils.size(context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en").listChildren()));
    }
}
