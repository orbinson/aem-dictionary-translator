package be.orbinson.aem.dictionarytranslator.servlets.action;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "dictionary-translator/servlet/action/delete-dictionary",
        methods = "POST"
)
public class DeleteDictionaryServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteDictionaryServlet.class);

    @Override
    protected void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String dictionaries = request.getParameter("dictionaries");

        if (StringUtils.isNotEmpty(dictionaries)) {
            final ResourceResolver resourceResolver = request.getResourceResolver();

            for (String dictionary : dictionaries.split(",")) {
                final Resource resource = resourceResolver.getResource(dictionary);
                if (resource != null) {
                    LOG.debug("Delete dictionary '{}'", dictionary);
                    resourceResolver.delete(resource);
                } else {
                    LOG.warn("Dictionary '{}' not found to delete", dictionary);
                }

                try {
                    resourceResolver.commit();
                } catch (PersistenceException e) {
                    LOG.error("Error deleting item: {}", dictionary, e);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                }
            }
        } else {
            LOG.warn("Dictionary parameter can not be empty");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
