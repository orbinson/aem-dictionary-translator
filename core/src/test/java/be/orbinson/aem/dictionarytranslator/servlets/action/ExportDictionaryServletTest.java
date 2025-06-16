package be.orbinson.aem.dictionarytranslator.servlets.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.resourceresolver.MockFindQueryResources;
import org.apache.sling.testing.resourceresolver.MockFindResourcesHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class ExportDictionaryServletTest {

    private final AemContext context = new AemContext();
    private ExportDictionaryServlet servlet;
    private List<String> dictionaryPaths;

    @BeforeEach
    void setUp() {
        context.registerService(Replicator.class, mock(Replicator.class));
        context.registerInjectActivateService(new DictionaryServiceImpl());

        context.load().json("/content.json", "/content");
        dictionaryPaths = new ArrayList<>();
        dictionaryPaths.add("/content/dictionaries/fruit/i18n/en");
        dictionaryPaths.add("/content/dictionaries/fruit/i18n/nl_be");
        MockFindResourcesHandler handler = new MockFindResourcesHandler() {
            @Override
            public @Nullable Iterator<Resource> findResources(@NotNull String query, String language) {
                return dictionaryPaths.stream().map(p -> context.resourceResolver().getResource(p)).iterator();
            }
        };
        MockFindQueryResources.addFindResourceHandler(context.resourceResolver(), handler);

        servlet = context.registerInjectActivateService(new ExportDictionaryServlet());

        context.response().setCharacterEncoding("UTF-8");
        context.request().setMethod("POST");
    }

    @ParameterizedTest
    @ValueSource(strings = {";", ","})
    void doPostWithDifferentSeparators(String delimiter) throws Exception {
        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/fruit/i18n",
                "delimiter", delimiter
        ));

        servlet.service(context.request(), context.response());

        String csvContent = context.response().getOutputAsString().replaceAll("\r\n", "\n");
        String expectedContent = "KEY" + delimiter + "en" + delimiter + "nl-BE\n" +
                "apple" + delimiter + "Apple" + delimiter + "Appel\n" +
                "banana" + delimiter + "Banana" + delimiter + "Banaan\n" +
                "cherry" + delimiter + "Cherry" + delimiter + "Kers\n" +
                "mango" + delimiter + "Mango" + delimiter + "\n" +
                "papaya" + delimiter + "Papaya" + delimiter + "\n" +
                "pear" + delimiter + "<empty>" + delimiter + "\n";
        assertEquals(expectedContent, csvContent);
    }

    @Test
    void doPostResourceNotFound() throws Exception {
        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/non-existing/i18n",
                "delimiter", ";"
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }
}
