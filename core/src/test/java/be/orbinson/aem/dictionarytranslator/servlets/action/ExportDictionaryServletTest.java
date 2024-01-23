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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(AemContextExtension.class)
class ExportDictionaryServletTest {

    private final AemContext context = new AemContext();
    private ExportDictionaryServlet exportTranslation;

    @BeforeEach
    void setUp() throws Exception {
        exportTranslation = new ExportDictionaryServlet();

        context.request().setMethod("POST");
        context.response().setCharacterEncoding("UTF-8");
        context.create().resource("/test/path");
        context.registerAdapter(ResourceResolver.class, Session.class, new Function<ResourceResolver, Session>() {
            @Override
            public Session apply(ResourceResolver resolver) {
                return Mockito.mock(Session.class);
            }
        });

        createLanguageResource("en", "champion", "champion");
        createLanguageResource("nl", "champion", "kampioen");
        createLanguageResource("it", "champion", "campione");
        context.resourceResolver().commit();

    }

    @Test
    void doGet_ExportTranslationWithSemiColon_Success() throws ServletException, IOException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("dictionary", "/test/path");
        parameters.put("./delimiter", ";");
        context.request().setParameterMap(parameters);
        exportTranslation.doPost(context.request(), context.response());

        String csvContent = context.response().getOutputAsString();
        String expectedContent = "Labelname;en;nl;it\n" +
                "champion;champion;kampioen;campione\n";
        assertEquals(expectedContent, csvContent);
    }

    @Test
    void doGet_ExportTranslation_ResourceNotFound() throws ServletException, IOException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("dictionary", "/no/such/resource");
        parameters.put("./delimiter", ";");
        context.request().setParameterMap(parameters);
        exportTranslation.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_NOT_FOUND, context.response().getStatus());
    }

    @Test
    void doGet_ExportTranslationWithComma_Success() throws ServletException, IOException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("dictionary", "/test/path");
        parameters.put("./delimiter", ",");
        context.request().setParameterMap(parameters);
        exportTranslation.doPost(context.request(), context.response());

        String csvContent = context.response().getOutputAsString();
        String expectedContent = "Labelname,en,nl,it\n" +
                "champion,champion,kampioen,campione\n";
        assertEquals(expectedContent, csvContent);
    }

    private void createLanguageResource(String language, String label, String translation) throws Exception {
        ResourceResolver resourceResolver = context.resourceResolver();
        Session session = resourceResolver.adaptTo(Session.class);
        if (session != null) {
            String path = "/test/path";
            Map<String, Object> newFolderProperties = new HashMap<>();
            newFolderProperties.put(JcrConstants.JCR_PRIMARYTYPE, "sling:Folder");
            newFolderProperties.put("jcr:language", language);
            newFolderProperties.put("jcr:basename", language);
            newFolderProperties.put("sling:resourceType", "sling:Folder");
            newFolderProperties.put(JcrConstants.JCR_MIXINTYPES, new String[]{"mix:language"});
            Resource languageResource = resourceResolver.create(resourceResolver.getResource(path), language, newFolderProperties);
            Map<String, Object> mvm = new HashMap<>();
            mvm.put("jcr:primaryType", "sling:MessageEntry");
            mvm.put("sling:key", label);
            mvm.put("sling:message", translation);
            resourceResolver.create(languageResource, label, mvm);
        }
    }
}
