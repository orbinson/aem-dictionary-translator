package be.orbinson.aem.dictionarytranslator.services.impl;

import be.orbinson.aem.dictionarytranslator.models.Dictionary;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import com.day.cq.replication.Replicator;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class DictionaryServiceImplTest {

    private final AemContext context = new AemContext();

    DictionaryService dictionaryService;

    @BeforeEach
    void setup() {
        context.registerService(Replicator.class, mock(Replicator.class));
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
    void dictionaryServiceShouldAddCorrectBasenameIfEmptry() throws PersistenceException {
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
}
