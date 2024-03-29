package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
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

import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_KEY;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGE;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGEENTRY;
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
            LOG.warn("Key and dictionary parameters are required");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            ResourceResolver resourceResolver = request.getResourceResolver();
            Resource resource = resourceResolver.getResource(dictionary);
            try {
                if (resource != null) {
                    for (String language : dictionaryService.getLanguages(resource)) {
                        // javasecurity:S5145
                        LOG.debug("Create label on path '{}/{}'", dictionary, key);
                        String message = request.getParameter(language);
                        addMessage(resourceResolver, resource, language, key, message);
                    }
                } else {
                    // javasecurity:S5145
                    LOG.warn("Unable to get dictionary '{}'", dictionary);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                }
            } catch (PersistenceException e) {
                LOG.error("Unable to create key '{}' on dictionary '{}'", key, dictionary);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    private void addMessage(ResourceResolver resourceResolver, Resource dictionary, String language, String key, String message) throws PersistenceException {
        Resource resource = dictionary.getChild(language);

        if (resource != null) {
            String path = resource.getPath();
            Map<String, Object> properties = new HashMap<>();
            properties.put(JCR_PRIMARYTYPE, SLING_MESSAGEENTRY);
            properties.put(SLING_KEY, key);
            if (!message.isBlank()){
                properties.put(SLING_MESSAGE, message);
            }
            resourceResolver.create(resource, JcrUtil.createValidName(key), properties);
            LOG.trace("Create label with key '{}' and message '{}' on path '{}'", key, message, path);
            resourceResolver.commit();
        }
    }
}
