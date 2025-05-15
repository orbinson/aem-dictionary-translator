package be.orbinson.aem.dictionarytranslator.services.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.day.cq.replication.Replicator;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class CombiningMessageEntryResourceProviderTest {

    private final AemContext context = new AemContext(ResourceResolverType.RESOURCEPROVIDER_MOCK);

    CombiningMessageEntryResourceProvider resourceProvider;

    @BeforeEach
    void setup() {
        context.registerService(Replicator.class, mock(Replicator.class));
        context.registerInjectActivateService(new DictionaryServiceImpl());

        resourceProvider = context.registerInjectActivateService(new CombiningMessageEntryResourceProvider());

        context.load().json("/content.json", "/content");
    }

    @Test
    void rootPathShouldNotReturnNull() {
        assertNotNull(context.resourceResolver().getResource("/mnt/dictionary"));
    }

    @Test
    void syntheticMessageEntryPathShouldReturnNonNullResource() {
        Resource resource = context.resourceResolver().getResource("/mnt/dictionary/content/dictionaries/fruit/i18n/apple");
        assertNotNull(resource);
        ValueMap properties = resource.getValueMap();
        Map<String, Object> expectedProperties = Map.of(
                "path", "/mnt/dictionary/content/dictionaries/fruit/i18n/apple",
                "languages", new String[]{"en", "nl_BE"}, // languages must always be in alphabetical order
                "nl_BE", "Appel",
                "en", "Apple",
                "editable", Boolean.FALSE, // because resource resolver is not adaptable to Session
                "key", "apple",
                "dictionaryPath", "/content/dictionaries/fruit/i18n",
                "messageEntryPaths", new String[]{
                        "/content/dictionaries/fruit/i18n/en/apple", "/content/dictionaries/fruit/i18n/nl_be/apple"
                }
        );
        expectedProperties.forEach((key, value) -> {
            Object actualValue = properties.get(key);
            assertNotNull(actualValue, "Property " + key + " should not be null");
            if (actualValue instanceof Collection) {
                assertArrayEquals((Object[])value, ((Collection<?>)actualValue).toArray());
            } else {
                assertEquals(value, actualValue);
            }
        });
        assertEquals(expectedProperties.size(), properties.size());
    }

    @Test
    void listChildrenShouldNotFailOnResource() {
        assertDoesNotThrow(() -> context.resourceResolver().getResource("/mnt/dictionary/content/dictionaries/fruit/i18n/apple").listChildren());
    }
    
}