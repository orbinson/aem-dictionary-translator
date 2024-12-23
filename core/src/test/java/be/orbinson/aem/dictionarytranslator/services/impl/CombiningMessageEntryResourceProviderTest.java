package be.orbinson.aem.dictionarytranslator.services.impl;

import com.day.cq.replication.Replicator;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

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
    void syntheticPathShouldReturnNonNullResource() {
        assertNotNull(context.resourceResolver().getResource("/mnt/dictionary/content/dictionaries/fruit/i18n/apple"));
    }

    @Test
    void listChildrenShouldNotFailOnResource() {
        assertDoesNotThrow(() -> context.resourceResolver().getResource("/mnt/dictionary/content/dictionaries/fruit/i18n/apple").listChildren());
    }
}