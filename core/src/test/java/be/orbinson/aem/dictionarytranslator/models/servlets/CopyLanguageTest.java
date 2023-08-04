package be.orbinson.aem.dictionarytranslator.models.servlets;

import be.orbinson.aem.dictionarytranslator.servlets.CopyLanguage;
import com.day.cq.commons.jcr.JcrConstants;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(AemContextExtension.class)
public class CopyLanguageTest {
    private final AemContext context = new AemContext();
    private CopyLanguage servlet;

    @BeforeEach
    void setUp() {
        servlet = new CopyLanguage();
        context.request().setMethod(HttpConstants.METHOD_POST);
    }

    @Test
    void testDoPostMissingParams() throws ServletException, IOException {
        Map<String, Object> params = new HashMap<>();
        context.request().setParameterMap(params);
        servlet.doPost(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }


    @Test
    void testDoPostValidParams() throws ServletException, IOException {
        context.create().resource("/content/myPage", JcrConstants.JCR_PRIMARYTYPE, JcrResourceConstants.NT_SLING_FOLDER);
        context.create().resource("/content/myPage/en", JcrConstants.JCR_PRIMARYTYPE, JcrResourceConstants.NT_SLING_FOLDER);
        Map<String, Object> paramMap = Map.of(
                "originalLanguage", new String[]{"en"},
                "languageCode", new String[]{"fr"},
                "path", new String[]{"/content/myPage"}
        );
        context.request().setParameterMap(paramMap);
        servlet.doPost(context.request(), context.response());
        assertNotNull(context.resourceResolver().getResource("/content/myPage/fr"));
    }

    @Test
    void testDoPostInvalidParams() throws ServletException, IOException {
        context.create().resource("/content/myPage", JcrConstants.JCR_PRIMARYTYPE, JcrResourceConstants.NT_SLING_FOLDER);
        context.create().resource("/content/myPage/en", JcrConstants.JCR_PRIMARYTYPE, JcrResourceConstants.NT_SLING_FOLDER);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("originalLanguage", new String[]{"nonexistent"});
        paramMap.put("languageCode", new String[]{"fr"});
        paramMap.put("path", new String[]{"/content/myPage"});
        context.request().setParameterMap(paramMap);
        servlet.doPost(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

}
