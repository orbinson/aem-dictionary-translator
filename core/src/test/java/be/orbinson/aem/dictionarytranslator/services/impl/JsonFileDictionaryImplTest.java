package be.orbinson.aem.dictionarytranslator.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

import org.apache.sling.api.resource.ModifiableValueMap;
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

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
public class JsonFileDictionaryImplTest {

    private final AemContext context = new AemContext();

    private Dictionary dictionary;

    @BeforeEach
    void setUp() throws IOException {
        context.load().json("/content.json", "/content");
        try (InputStream is = getClass().getResourceAsStream("/fruit.de.json")) {
            Objects.requireNonNull(is);
            Resource resource = context.load().binaryFile(is, "/content/dictionaries/fruit/i18n/de.json");
            ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);
            Objects.requireNonNull(properties);
            properties.put("jcr:language", "de");
        }
        context.currentResource("/content/dictionaries/fruit/i18n/de.json");
        dictionary = new JsonFileDictionary(context.currentResource(), context::resourceResolver);
    }

    @Test
    void shouldReturnMessages() throws DictionaryException, IOException {
        assertEquals(Map.of("apple", new Message("Apfel", null), "banana", new Message("Banane", null), "cherry", new Message("Kirsche", null)), 
                dictionary.getEntries());
    }

    @Test
    void testIsEditable() {
        assertFalse(dictionary.isEditable(context.resourceResolver()));
    }
}
