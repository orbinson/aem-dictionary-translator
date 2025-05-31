package be.orbinson.aem.dictionarytranslator.servlets.action;

import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_KEY;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGE;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGEENTRY;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.resourceresolver.MockFindQueryResources;
import org.apache.sling.testing.resourceresolver.MockFindResourcesHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider;
import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class UpdateMessageEntryServletTest {

    private final AemContext context = new AemContext(ResourceResolverType.RESOURCEPROVIDER_MOCK);

    UpdateMessageEntryServlet servlet;

    private List<String> dictionaryPaths;

    @BeforeEach
    void beforeEach() {
        context.registerService(Replicator.class, mock(Replicator.class));
        DictionaryService dictionaryService = new DictionaryServiceImpl(true);
        context.registerInjectActivateService(dictionaryService);
        Converter converter = Converters.standardConverter();
        CombiningMessageEntryResourceProvider.Config config = converter.convert(Map.of("enableValidation", true)).to(CombiningMessageEntryResourceProvider.Config.class);
        context.registerInjectActivateService(new CombiningMessageEntryResourceProvider(dictionaryService, config));
        context.request().setMethod("POST");
        servlet = context.registerInjectActivateService(new UpdateMessageEntryServlet());

        dictionaryPaths = new ArrayList<>();
        MockFindResourcesHandler handler = new MockFindResourcesHandler() {
            @Override
            public @Nullable Iterator<Resource> findResources(@NotNull String query, String language) {
                return dictionaryPaths.stream().map(p -> context.resourceResolver().getResource(p)).iterator();
            }
        };
        MockFindQueryResources.addFindResourceHandler(context.resourceResolver(), handler);
    }

    @Test
    void doPostWithoutParams() throws IOException, ServletException {
        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void updateCombiningMessageEntryInNonExistingDictionary() throws IOException, ServletException {
        context.request().setParameterMap(Map.of(
                "item", "/mnt/dictionary/content/dictionaries/non-existing/i18",
                "key", "greeting",
                "en", "Hello"
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void doPostWithValidParams() throws IOException, ServletException {
        context.load().json("/content.json", "/content");
        dictionaryPaths.add("/content/dictionaries/fruit/i18n/en");
        dictionaryPaths.add("/content/dictionaries/fruit/i18n/nl_be");
        context.request().setParameterMap(Map.of(
                "item", "/mnt/dictionary/content/dictionaries/fruit/i18n/apple",
                "key", "appel",
                "en", "New Apple",
                "nl-BE", "Nieuwe Appel"
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        Resource resource = context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en/appel");
        assertNotNull(resource);
        ValueMap properties = resource.getValueMap();
        assertNotNull(resource);
        assertEquals(SLING_MESSAGEENTRY, properties.get(JCR_PRIMARYTYPE));
        assertEquals("appel", properties.get(SLING_KEY));
        assertEquals("New Apple", properties.get(SLING_MESSAGE));

        resource = context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/nl_be/appel");
        properties = resource.getValueMap();
        assertNotNull(resource);
        assertEquals(SLING_MESSAGEENTRY, properties.get(JCR_PRIMARYTYPE));
        assertEquals("appel", properties.get(SLING_KEY));
        assertEquals("Nieuwe Appel", properties.get(SLING_MESSAGE));
    }
}
