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
        resourceTypes = "dictionary-translator/servlet/action/delete-label",
        methods = "POST"
)
public class DeleteLabelServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteLabelServlet.class);

    @Override
    protected void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String labels = request.getParameter("labels");

        if (StringUtils.isEmpty(labels)) {
            LOG.warn("Labels parameters are required");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            for (String label : labels.split(",")) {
                ResourceResolver resourceResolver = request.getResourceResolver();
                Resource resource = resourceResolver.getResource(label);
                try {
                    if (resource != null) {
                        LOG.debug("Delete label on path '{}'", label);
                        resourceResolver.delete(resource);
                    } else {
                        LOG.warn("Unable to get label '{}'", labels);
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    }
                } catch (PersistenceException e) {
                    LOG.error("Unable to delete labels '{}'", labels);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
        }
    }
}
