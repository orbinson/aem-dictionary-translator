package be.orbinson.aem.dictionarytranslator.servlets.action;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import org.apache.sling.testing.resourceresolver.MockFindQueryResources;
import org.apache.sling.testing.resourceresolver.MockFindResourcesHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class CreateDictionaryServletTest {
    private final AemContext context = new AemContext();

    CreateDictionaryServlet servlet;

    @Spy
    DictionaryService dictionaryService;

    private List<String> dictionaryPaths;

    @BeforeEach
    void beforeEach() {
        context.registerService(Replicator.class, mock(Replicator.class));
        dictionaryService = context.registerInjectActivateService(new DictionaryServiceImpl(true));

        servlet = context.registerInjectActivateService(new CreateDictionaryServlet());
        context.request().setMethod("POST");
        
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
    void doPostInvalidParams() throws IOException, ServletException {
        context.request().setParameterMap(Map.of(
                "name", "dictionaries",
                "path", "/some/path"
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void doPostValidParams() throws IOException, ServletException {
        String[] languages = new String[]{"en", "fr"};
        context.create().resource("/content/dictionaries");
        context.request().setParameterMap(Map.of(
                "name", "fruit",
                "path", "/content/dictionaries",
                "language", languages,
                "basename", "/content/dictionaries/fruit"
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_CREATED, context.response().getStatus());
        for (String language : languages) {
            Resource resource = context.resourceResolver().getResource("/content/dictionaries/fruit/" + language);
            assertNotNull(resource);
            ValueMap properties = resource.getValueMap();
            assertEquals(language, properties.get("jcr:language"));
            assertEquals("mix:language", properties.get("jcr:mixinTypes"));
            assertEquals(language, properties.get("jcr:language"));
            assertArrayEquals(new String[]{"/content/dictionaries/fruit"}, properties.get("sling:basename", String[].class));
        }
    }
}
