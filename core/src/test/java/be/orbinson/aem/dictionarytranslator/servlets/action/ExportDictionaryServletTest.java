package be.orbinson.aem.dictionarytranslator.servlets.action;

import com.day.cq.commons.jcr.JcrConstants;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(AemContextExtension.class)
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
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("dictionary", "/test/path");
        parameters.put("delimiter", ";");
        context.request().setParameterMap(parameters);
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
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("dictionary", "/test/path");
        parameters.put("delimiter", ";");
        context.request().setParameterMap(parameters);
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
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("dictionary", "/no/such/resource");
        parameters.put("delimiter", ";");
        context.request().setParameterMap(parameters);
        exportDictionaryServlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_NOT_FOUND, context.response().getStatus());
    }

    @Test
    void doPostWithComma() throws Exception {
        createLanguageResource("en", "champion", "champion");
        createLanguageResource("nl", "champion", "kampioen");
        createLanguageResource("it", "champion", "campione");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("dictionary", "/test/path");
        parameters.put("delimiter", ",");
        context.request().setParameterMap(parameters);
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
            if (resourceResolver.getResource(path + "/" + language) == null){
                languageResource = resourceResolver.create(resourceResolver.getResource(path), language, Map.of(
                        JcrConstants.JCR_PRIMARYTYPE, "sling:Folder",
                        "jcr:language", language,
                        "jcr:basename", language,
                        "sling:resourceType", "sling:Folder",
                        JcrConstants.JCR_MIXINTYPES, new String[]{"mix:language"}
                ));
            } else {
                languageResource = resourceResolver.getResource(path + "/" + language);
            }
            resourceResolver.create(languageResource, label, Map.of(
                    "jcr:primaryType", "sling:MessageEntry",
                    "sling:key", label,
                    "sling:message", translation
            ));
        }
        context.resourceResolver().commit();
    }
}
