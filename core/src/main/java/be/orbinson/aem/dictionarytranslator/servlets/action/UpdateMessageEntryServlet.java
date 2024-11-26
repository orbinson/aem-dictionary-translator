package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/update-message-entry",
        methods = "POST"
)
public class UpdateMessageEntryServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateMessageEntryServlet.class);

    @Reference
    private transient DictionaryService dictionaryService;

    @Override
    protected void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String combiningMessageEntryPath = request.getParameter("item"); // only single items are supported

        if (StringUtils.isEmpty(combiningMessageEntryPath)) {
            HtmlResponse htmlResponse = new HtmlResponse();
            htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, "item parameter is required");
            htmlResponse.send(response, true);
        } else {
            ResourceResolver resourceResolver = request.getResourceResolver();
            Resource combiningMessageEntryResource = resourceResolver.getResource(combiningMessageEntryPath);
            try {
                if (combiningMessageEntryResource != null) {
                    // javasecurity:S5145
                    LOG.debug("Update message entry for path '{}'", combiningMessageEntryPath);
                    updateCombiningMessageEntry(request, resourceResolver, combiningMessageEntryResource);
                } else {
                    // javasecurity:S5145
                    HtmlResponse htmlResponse = new HtmlResponse();
                    htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Message entry does not exist '%s'", combiningMessageEntryPath));
                    htmlResponse.send(response, true);
                }
            } catch (Exception e) {
                HtmlResponse htmlResponse = new HtmlResponse();
                htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Unable to update message entry '%s': %s", combiningMessageEntryPath, e.getMessage()));
                htmlResponse.send(response, true);
            }
        }
    }

    private void updateCombiningMessageEntry(SlingHttpServletRequest request, ResourceResolver resourceResolver, Resource combiningMessageEntryResource) throws DictionaryException, PersistenceException, RepositoryException {
        String key = request.getParameter("key");
        String dictionaryPath = combiningMessageEntryResource.getValueMap().get(CombiningMessageEntryResourceProvider.DICTIONARY_PATH, String.class);
        if (StringUtils.isNotBlank(dictionaryPath)) {
            Resource dictionaryResource = resourceResolver.getResource(dictionaryPath);
            String[] languages = combiningMessageEntryResource.getValueMap().get(CombiningMessageEntryResourceProvider.LANGUAGES, new String[0]);
            for (String language : languages) {
                String message = request.getParameter(language);
                if (message == null) {
                    throw new DictionaryException("Unable to get message for language '" + language + "'");
                }
                dictionaryService.updateMessageEntry(resourceResolver, dictionaryResource, language, key, message);
            }
        } else {
            throw new DictionaryException("Could not find dictionary path");
        }
    }

}
