package be.orbinson.aem.dictionarytranslator.services.impl;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import com.day.cq.replication.Replicator;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class DictionaryServiceImplTest {

    private final AemContext context = new AemContext();

    DictionaryService dictionaryService;

    @BeforeEach
    void setup() {
        context.registerService(Replicator.class, mock(Replicator.class));
        dictionaryService = context.registerInjectActivateService(new DictionaryServiceImpl());
    }

    @Test
    void returnsUniqueDictionaries() {
        context.create().resource("/content/dictionaries/site-a/i18/en", Map.of("jcr:language", "en"));
        context.create().resource("/content/dictionaries/site-a/i18/fr", Map.of("jcr:language", "fr"));
        context.create().resource("/content/dictionaries/site-b/i18/en", Map.of("jcr:language", "en"));

        ResourceResolver resourceResolver = spy(context.resourceResolver());
        doReturn(
                List.of(
                        context.resourceResolver().getResource("/content/dictionaries/site-a/i18"),
                        context.resourceResolver().getResource("/content/dictionaries/site-a/i18"),
                        context.resourceResolver().getResource("/content/dictionaries/site-b/i18")
                ).iterator()
        ).when(resourceResolver).findResources(anyString(), anyString());

        List<Resource> dictionaries = dictionaryService.getDictionaries(resourceResolver);

        assertEquals(2, dictionaries.size());
        assertEquals("/content/dictionaries/site-a/i18", dictionaries.get(0).getPath());
        assertEquals("/content/dictionaries/site-b/i18", dictionaries.get(1).getPath());
    }

    @Test
    void returnsCorrectLanguages() {
        context.create().resource("/content/dictionaries/site/i18");
        context.create().resource("/content/dictionaries/site/i18/fr", Map.of("jcr:language", "fr"));
        context.create().resource("/content/dictionaries/site/i18/rep:policy");
        context.create().resource("/content/dictionaries/site/i18/en", Map.of("jcr:language", "en"));

        List<String> languages = dictionaryService.getLanguages(context.currentResource("/content/dictionaries/site/i18"));

        assertEquals("en", languages.get(0));
        assertEquals("fr", languages.get(1));
    }


    @Test
    public void testIsEditableDictionary() {
        // Test cases where the path should be editable
        assertTrue(dictionaryService.isEditableDictionary("/content/example"), "Path starting with /content/ should be editable");
        assertTrue(dictionaryService.isEditableDictionary("/conf/example"), "Path starting with /conf/ should be editable");

        // Test cases where the path should not be editable
        assertFalse(dictionaryService.isEditableDictionary("/other/example"), "Path not starting with /content/ or /conf/ should not be editable");
        assertFalse(dictionaryService.isEditableDictionary("/configuration"), "Path starting with /configuration should not be editable");
        assertFalse(dictionaryService.isEditableDictionary("/contents"), "Path starting with /contents should not be editable");
    }

}
