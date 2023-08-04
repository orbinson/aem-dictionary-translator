package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.*;
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

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "dictionary-translator/servlet/action/update-label",
        methods = "POST"
)
public class UpdateLabelServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateLabelServlet.class);

    @Reference
    private transient DictionaryService dictionaryService;

    @Override
    protected void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String label = request.getParameter("label");

        if (StringUtils.isEmpty(label)) {
            LOG.warn("Label parameter is required");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            ResourceResolver resourceResolver = request.getResourceResolver();
            Resource resource = resourceResolver.getResource(label);
            try {
                if (resource != null) {
                    updateLabel(request, resourceResolver, resource);
                } else {
                    LOG.warn("Unable to get label '{}'", label);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                }
            } catch (Exception e) {
                LOG.error("Unable to update label '{}'", label);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    private void updateLabel(SlingHttpServletRequest request, ResourceResolver resourceResolver, Resource resource) throws Exception {
        String dictionaryPath = resource.getValueMap().get("dictionaryPath", String.class);
        if (StringUtils.isNotBlank(dictionaryPath)) {
            Resource dictionaryResource = resourceResolver.getResource(dictionaryPath);
            String name = resource.getName();
            String[] languages = resource.getValueMap().get("languages", new String[0]);
            for (String language : languages) {
                String message = request.getParameter(language);
                addMessage(resourceResolver, dictionaryResource, language, name, message);
            }
        } else {
            throw new Exception("Could not find dictionary path");
        }
    }

    private void addMessage(ResourceResolver resourceResolver, Resource dictionary, String language, String name, String message) {
        Resource languageResource = dictionary.getChild(language);
        if (languageResource != null) {
            try {
                Resource labelResource = languageResource.getChild(name);
                if (labelResource != null) {
                    ValueMap valueMap = labelResource.adaptTo(ModifiableValueMap.class);
                    valueMap.put("sling:message", message);
                    LOG.trace("Updated label with name '{}' and message '{}' on path '{}'", name, message, labelResource.getPath());
                }
                resourceResolver.commit();
            } catch (PersistenceException e) {
                LOG.error("Unable to update label for name '{}'", name);
            }
        }
    }
}
