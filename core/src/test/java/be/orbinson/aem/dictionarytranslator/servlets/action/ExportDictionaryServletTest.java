package be.orbinson.aem.dictionarytranslator.servlets.action;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.function.Function;

import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.*;
import static org.apache.jackrabbit.JcrConstants.*;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.NT_SLING_FOLDER;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;

// TODO: should be recreated with the dictionary service in mind
@ExtendWith(AemContextExtension.class)
@Disabled("TODO: should be recreated with the dictionary service in mind")
class ExportDictionaryServletTest {

    private final AemContext context = new AemContext();
    private ExportDictionaryServlet exportDictionaryServlet;

    @BeforeEach
    void setUp() {
        exportDictionaryServlet = new ExportDictionaryServlet();

        context.request().setMethod("POST");
        context.response().setCharacterEncoding("UTF-8");
        context.create().resource("/test/path");
        context.registerAdapter(ResourceResolver.class, Session.class, new Function<ResourceResolver, Session>() {
            @Override
            public Session apply(ResourceResolver resolver) {
                return Mockito.mock(Session.class);
            }
        });
    }

    @Test
    void doPostWithSemiColon() throws Exception {
        createLanguageResource("en", "champion", "champion");
        createLanguageResource("nl", "champion", "kampioen");
        createLanguageResource("it", "champion", "campione");
        context.request().setParameterMap(Map.of(
                "dictionary", "/test/path",
                "delimiter", ";"
        ));
        exportDictionaryServlet.doPost(context.request(), context.response());

        String csvContent = context.response().getOutputAsString();
        String expectedContent = "KEY;en;nl;it\n" +
                "champion;champion;kampioen;campione\n";
        assertEquals(expectedContent, csvContent);
    }

    @Test
    void doPostWithMultipleLabels() throws Exception {
        createLanguageResource("en", "champion", "champion");
        createLanguageResource("nl", "champion", "kampioen");
        createLanguageResource("it", "champion", "campione");
        createLanguageResource("en", "apple", "apple");
        createLanguageResource("nl", "apple", "appel");
        createLanguageResource("it", "apple", "pomme");
        context.request().setParameterMap(Map.of(
                "dictionary", "/test/path",
                "delimiter", ";"
        ));
        exportDictionaryServlet.doPost(context.request(), context.response());

        String csvContent = context.response().getOutputAsString();
        String expectedContent = "KEY;en;nl;it\n" +
                "champion;champion;kampioen;campione\n" +
                "apple;apple;appel;pomme\n";
        assertEquals(expectedContent, csvContent);
    }

    @Test
    void doPostNoLabels() throws Exception {
        createLanguageResource("en", "", "");
        createLanguageResource("nl", "", "");
        createLanguageResource("it", "", "");
        context.request().setParameterMap(Map.of(
                "dictionary", "/test/path",
                "delimiter", ";"
        ));
        exportDictionaryServlet.doPost(context.request(), context.response());

        String csvContent = context.response().getOutputAsString();
        String expectedContent = "KEY;en;nl;it\n";
        assertEquals(expectedContent, csvContent);
    }

    @Test
    void doPostResourceNotFound() throws Exception {
        createLanguageResource("en", "champion", "champion");
        createLanguageResource("nl", "champion", "kampioen");
        createLanguageResource("it", "champion", "campione");
        context.request().setParameterMap(Map.of(
                "dictionary", "/no/such/resource",
                "delimiter", ";"
        ));
        exportDictionaryServlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void doPostWithComma() throws Exception {
        createLanguageResource("en", "champion", "champion");
        createLanguageResource("nl", "champion", "kampioen");
        createLanguageResource("it", "champion", "campione");
        context.request().setParameterMap(Map.of(
                "dictionary", "/test/path",
                "delimiter", ","
        ));
        exportDictionaryServlet.doPost(context.request(), context.response());

        String csvContent = context.response().getOutputAsString();
        String expectedContent = "KEY,en,nl,it\n" +
                "champion,champion,kampioen,campione\n";
        assertEquals(expectedContent, csvContent);
    }

    private void createLanguageResource(String language, String label, String translation) throws Exception {
        ResourceResolver resourceResolver = context.resourceResolver();
        Session session = resourceResolver.adaptTo(Session.class);
        if (session != null) {
            String path = "/test/path";
            Resource languageResource;
            if (resourceResolver.getResource(path + "/" + language) == null) {
                languageResource = resourceResolver.create(resourceResolver.getResource(path), language, Map.of(
                        JCR_PRIMARYTYPE, NT_SLING_FOLDER,
                        JCR_LANGUAGE, language,
                        JCR_BASENAME, language,
                        SLING_RESOURCE_TYPE_PROPERTY, NT_SLING_FOLDER,
                        JCR_MIXINTYPES, new String[]{MIX_LANGUAGE}
                ));
            } else {
                languageResource = resourceResolver.getResource(path + "/" + language);
            }
            resourceResolver.create(languageResource, label, Map.of(
                    JCR_PRIMARYTYPE, SLING_MESSAGEENTRY,
                    SLING_KEY, label,
                    SLING_MESSAGE, translation
            ));
        }
        context.resourceResolver().commit();
    }
}
