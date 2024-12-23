package be.orbinson.aem.dictionarytranslator.servlets.action;

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

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/create-message-entry",
        methods = "POST"
)
public class CreateMessageEntryServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(CreateMessageEntryServlet.class);

    @Reference
    private transient DictionaryService dictionaryService;

    @Override
    protected void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String key = request.getParameter("key");
        String dictionary = request.getParameter("dictionary");

        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(dictionary)) {
            HtmlResponse htmlResponse = new HtmlResponse();
            htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Invalid parameters to create language, 'key=%s', 'dictionary=%s', ", key, dictionary));
            htmlResponse.send(response, true);
        } else {
            createMessageEntry(request, response, dictionary, key);
        }
    }

    private void createMessageEntry(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response, String dictionary, String key) throws IOException {
        ResourceResolver resourceResolver = request.getResourceResolver();
        Resource dictionaryResource = resourceResolver.getResource(dictionary);
        try {
            if (dictionaryResource != null) {
                for (String language : dictionaryService.getLanguages(dictionaryResource)) {
                    // javasecurity:S5145
                    LOG.debug("Create message entry on path '{}/{}'", dictionary, key);
                    String message = request.getParameter(language);
                    if (!dictionaryService.keyExists(dictionaryResource, language, key)) {
                        dictionaryService.createMessageEntry(resourceResolver, dictionaryResource, language, key, message);
                    } else {
                        HtmlResponse htmlResponse = new HtmlResponse();
                        htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Can not create message entry %s, key already exists", key));
                        htmlResponse.send(response, true);
                    }
                }
            } else {
                HtmlResponse htmlResponse = new HtmlResponse();
                htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Unable to get dictionary '%s'", dictionary));
                htmlResponse.send(response, true);
            }
        } catch (PersistenceException e) {
            HtmlResponse htmlResponse = new HtmlResponse();
            htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Unable to create key '%s' on dictionary '%s': %s", key, dictionary, e.getMessage()));
            htmlResponse.send(response, true);
        }
    }

}
