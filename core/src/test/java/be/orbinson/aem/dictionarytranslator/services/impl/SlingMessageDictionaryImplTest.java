package be.orbinson.aem.dictionarytranslator.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
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
                "cherry", new Message("Cherry", "/content/dictionaries/fruit/i18n/en/cherry")),
                dictionary.getEntries());
    }

    @Test
    void dictionaryShouldUpdateMessageEntry() throws PersistenceException, DictionaryException {
        dictionary.createOrUpdateEntry(context.resourceResolver(), "apple", "Not a banana");

        assertEquals("Not a banana", context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en/apple").getValueMap().get("sling:message"));
    }

    @Test
    void dictionaryShouldUpdateNonExistingMessageEntry() throws PersistenceException, DictionaryException {
        dictionary.createOrUpdateEntry(context.resourceResolver(), "kaboeboe", "Kaboeboe");

        assertEquals("Kaboeboe", context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en/kaboeboe").getValueMap().get("sling:message"));
    }

    @Test
    void dictionaryWithEmptyMessageShouldDeleteMessageEntry() throws PersistenceException, DictionaryException {
        dictionary.createOrUpdateEntry(context.resourceResolver(), "apple", "");

        assertFalse(context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en/apple").getValueMap().containsKey("sling:message"));
    }
}
