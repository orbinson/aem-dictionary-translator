package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.*;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/update-label",
        methods = "POST"
)
public class UpdateLabelServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateLabelServlet.class);

    @Reference
    private transient DictionaryService dictionaryService;

    @Override
    protected void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String label = request.getParameter("item"); // only single items are supported

        if (StringUtils.isEmpty(label)) {
            HtmlResponse htmlResponse = new HtmlResponse();
            htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, "Label parameter is required");
            htmlResponse.send(response, true);
        } else {
            ResourceResolver resourceResolver = request.getResourceResolver();
            Resource labelResource = resourceResolver.getResource(label);
            try {
                if (labelResource != null) {
                    // javasecurity:S5145
                    LOG.debug("Update label on path '{}'", label);
                    updateLabel(request, resourceResolver, labelResource);
                } else {
                    // javasecurity:S5145
                    HtmlResponse htmlResponse = new HtmlResponse();
                    htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Label does not exist '%s'", label));
                    htmlResponse.send(response, true);
                }
            } catch (Exception e) {
                HtmlResponse htmlResponse = new HtmlResponse();
                htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Unable to update label '%s': %s", label, e.getMessage()));
                htmlResponse.send(response, true);
            }
        }
    }

    private void updateLabel(SlingHttpServletRequest request, ResourceResolver resourceResolver, Resource labelResource) throws DictionaryException, PersistenceException, RepositoryException {
        String key = request.getParameter("key");
        String dictionaryPath = labelResource.getValueMap().get("dictionaryPath", String.class);
        if (StringUtils.isNotBlank(dictionaryPath)) {
            Resource dictionaryResource = resourceResolver.getResource(dictionaryPath);
            String[] languages = labelResource.getValueMap().get("languages", new String[0]);
            for (String language : languages) {
                String message = request.getParameter(language);
                dictionaryService.updateLabel(resourceResolver, dictionaryResource, language, key, message);
            }
        } else {
            throw new DictionaryException("Could not find dictionary path");
        }
    }

}
