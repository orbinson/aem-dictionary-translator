package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.utils.DictionaryUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.util.Text;
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
import java.util.HashMap;
import java.util.Map;

import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.*;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/create-label",
        methods = "POST"
)
public class CreateLabelServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(CreateLabelServlet.class);

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
            ResourceResolver resourceResolver = request.getResourceResolver();
            Resource dictionaryResource = resourceResolver.getResource(dictionary);
            try {
                if (dictionaryResource != null) {
                    for (String language : dictionaryService.getLanguages(dictionaryResource)) {
                        // javasecurity:S5145
                        LOG.debug("Create label on path '{}/{}'", dictionary, key);
                        String message = request.getParameter(language);
                        if (!labelExists(dictionaryResource, language, key)) {
                            addMessage(resourceResolver, dictionaryResource, language, key, message);
                        } else {
                            HtmlResponse htmlResponse = new HtmlResponse();
                            htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Can not create label %s, label already exists", key));
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

    private boolean labelExists(Resource dictionaryResource, String language, String key) {
        Resource languageResource = DictionaryUtil.getLanguageResource(dictionaryResource, language);
        return languageResource != null && languageResource.getChild(Text.escapeIllegalJcrChars(key)) != null;
    }

    private void addMessage(ResourceResolver resourceResolver, Resource dictionaryResource, String language, String key, String message) throws PersistenceException {
        Resource resource = DictionaryUtil.getLanguageResource(dictionaryResource, language);

        if (resource != null) {
            String path = resource.getPath();
            Map<String, Object> properties = new HashMap<>();
            properties.put(JCR_PRIMARYTYPE, SLING_MESSAGEENTRY);
            properties.put(SLING_KEY, key);
            if (!message.isBlank()) {
                properties.put(SLING_MESSAGE, message);
            }
            resourceResolver.create(resource, Text.escapeIllegalJcrChars(key), properties);
            LOG.trace("Create label with key '{}' and message '{}' on path '{}'", key, message, path);
            resourceResolver.commit();
        }
    }


}
