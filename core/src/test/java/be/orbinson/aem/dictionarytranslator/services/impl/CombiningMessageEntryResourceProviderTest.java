package be.orbinson.aem.dictionarytranslator.services.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.hamcrest.ResourceMatchers;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.resourceresolver.MockFindQueryResources;
import org.apache.sling.testing.resourceresolver.MockFindResourcesHandler;
import org.hamcrest.MatcherAssert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider.ValidationMessage;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class CombiningMessageEntryResourceProviderTest {

    private static final String KEY_SPECIAL_CHARACTERS = "test\r\n%&special  test\t characters";

    private final AemContext context = new AemContext(ResourceResolverType.RESOURCEPROVIDER_MOCK);

    CombiningMessageEntryResourceProvider resourceProvider;

    /** Contains the paths of all resources which are returned by the findResources() method, necessary because the RR mock does not support queries natively */
    List<String> dictionaryPaths;

    @BeforeEach
    void setup() {
        context.registerService(Replicator.class, mock(Replicator.class));

        DictionaryServiceImpl dictionaryService = new DictionaryServiceImpl();
        context.registerInjectActivateService(dictionaryService);
        Converter converter = Converters.standardConverter();
        CombiningMessageEntryResourceProvider.Config config = converter.convert(Map.of("enableValidation", true)).to(CombiningMessageEntryResourceProvider.Config.class);
        resourceProvider = context.registerInjectActivateService(new CombiningMessageEntryResourceProvider(dictionaryService, config));

        context.load().json("/content.json", "/content");
        dictionaryPaths = new ArrayList<>(List.of(
                "/content/dictionaries/fruit/i18n/en",
                "/content/dictionaries/fruit/i18n/nl_be",
                "/content/dictionaries/vegetables/i18n/en")
        );
        
        MockFindResourcesHandler handler = new MockFindResourcesHandler() {
            @Override
            public @Nullable Iterator<Resource> findResources(@NotNull String query, String language) {
                return dictionaryPaths.stream().map(p -> context.resourceResolver().getResource(p)).iterator();
            }
        };
        MockFindQueryResources.addFindResourceHandler(context.resourceResolver(), handler);
    }

    @Test
    void rootPathShouldNotReturnNull() {
        assertNotNull(context.resourceResolver().getResource("/mnt/dictionary"));
    }

    @Test
    void syntheticMessageEntryPathShouldReturnProperResource() {
        // add additional message entry with key having special characters
        context.build().resource("/content/dictionaries/fruit/i18n/en/specialkey", "jcr:primaryType", "sling:MessageEntry",
                "sling:message", "foo",
                "sling:key", KEY_SPECIAL_CHARACTERS);
        
        // /mnt/dictionary/content/dictionaries/fruit/i18n/en/apple
        String path = CombiningMessageEntryResourceProvider.createPath("/content/dictionaries/fruit/i18n", KEY_SPECIAL_CHARACTERS);
        Resource resource = context.resourceResolver().getResource(path);
        assertNotNull(resource);
        ValueMap properties = resource.getValueMap();
        List<String> expectedMessageEntryPaths = new LinkedList<>();
        expectedMessageEntryPaths.add("/content/dictionaries/fruit/i18n/en/specialkey");
        SortedSet<ValidationMessage> expectedValidationMessages = new TreeSet<>();
        Map<String, Object> expectedProperties = Map.of(
                "languages", new Locale[]{Locale.ENGLISH, new Locale("nl", "BE")}, // languages must always be in alphabetical order
                "en", "foo",
                "editable", Boolean.TRUE, // because resource resolver is not adaptable to Session
                "key", KEY_SPECIAL_CHARACTERS,
                "escapedKey", "test\\r\\n%&special··test\\t·characters",
                "dictionaryPath", "/content/dictionaries/fruit/i18n",
                "messageEntryPaths", expectedMessageEntryPaths,
                "validationMessages", expectedValidationMessages
        );
        MatcherAssert.assertThat(resource, ResourceMatchers.props(expectedProperties));
        assertEquals(expectedProperties.size(), properties.size());
    }

    @Test
    void listChildrenShouldNotFailOnResource() {
        assertDoesNotThrow(() -> context.resourceResolver().getResource("/mnt/dictionary/content/dictionaries/fruit/i18n/apple").listChildren());
    }

    @Test
    void testPathEscaping() {
        String key = "\n\rkey/with%23special%20characters&=";
        String escapedPath = CombiningMessageEntryResourceProvider.createPath("/my/path", key);
        assertEquals(key, CombiningMessageEntryResourceProvider.extractKeyFromPath(escapedPath));
        assertEquals(5, StringUtils.countMatches(escapedPath, '/')); // no additional slash in the key part of the path
        assertEquals(0, StringUtils.countMatches(escapedPath, '%')); // no percent sign at all in the path
        assertEquals(0, StringUtils.countMatches(escapedPath, '\n')); // no new lines at all in the path
        assertEquals(0, StringUtils.countMatches(escapedPath, '\r'));
        // make sure the heuristic in /libs/clientlibs/granite/uritemplate/URITemplate.js in its "isEncoded(String)" method returns false
        assertEquals(escapedPath, URLDecoder.decode(escapedPath, StandardCharsets.UTF_8));
        key = "..";
        escapedPath = CombiningMessageEntryResourceProvider.createPath("/my/path", key);
        assertEquals(key, CombiningMessageEntryResourceProvider.extractKeyFromPath(escapedPath));
        assertEquals(0, StringUtils.countMatches(escapedPath, '.')); // no additional dots in the key part of the path
    }
}