package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider;
import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import com.day.cq.replication.Replicator;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_KEY;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGE;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGEENTRY;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@ExtendWith(AemContextExtension.class)
class UpdateMessageEntryServletTest {

    private final AemContext context = new AemContext(ResourceResolverType.RESOURCEPROVIDER_MOCK);

    UpdateMessageEntryServlet servlet;

    DictionaryService dictionaryService;

    @BeforeEach
    void beforeEach() {
        context.registerService(Replicator.class, mock(Replicator.class));
        DictionaryServiceImpl dictionaryService = new DictionaryServiceImpl();
        context.registerInjectActivateService(dictionaryService);
        Converter converter = Converters.standardConverter();
        CombiningMessageEntryResourceProvider.Config config = converter.convert(Map.of("enableValidation", true)).to(CombiningMessageEntryResourceProvider.Config.class);
        context.registerInjectActivateService(new CombiningMessageEntryResourceProvider(dictionaryService, config));

        servlet = context.registerInjectActivateService(new UpdateMessageEntryServlet());
    }

    @Test
    void doPostWithoutParams() throws IOException {
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void updateCombiningMessageEntryInNonExistingDictionary() throws IOException {
        context.request().setParameterMap(Map.of(
                "item", "/mnt/dictionary/content/dictionaries/non-existing/i18",
                "key", "greeting",
                "en", "Hello"
        ));

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void doPostWithValidParams() throws IOException {
        context.load().json("/content.json", "/content");

        context.request().setParameterMap(Map.of(
                "item", "/mnt/dictionary/content/dictionaries/fruit/i18n/apple",
                "key", "appel",
                "en", "New Apple",
                "nl_BE", "Nieuwe Appel"
        ));

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        Resource resource = context.resourceResolver().getResource("/content/dictionaries/fruit/i18n/en/appel");
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
