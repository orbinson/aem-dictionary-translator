package be.orbinson.aem.dictionarytranslator.servlets.action;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
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

import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/delete-dictionary",
        methods = "POST"
)
public class DeleteDictionaryServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteDictionaryServlet.class);
    public static final String DICTIONARIES_PARAM = "dictionaries";

    @Reference
    private transient Replicator replicator;

    @Override
    protected void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String dictionaries = request.getParameter(DICTIONARIES_PARAM);

        if (StringUtils.isNotEmpty(dictionaries)) {
            final ResourceResolver resourceResolver = request.getResourceResolver();

            for (String dictionary : dictionaries.split(",")) {
                try {
                    final Resource resource = resourceResolver.getResource(dictionary);
                    if (resource != null) {
                        LOG.debug("Delete dictionary '{}'", dictionary);
                        deactivateAndDelete(resourceResolver, resource);
                        resourceResolver.commit();
                    } else {
                        LOG.warn("Dictionary '{}' not found to delete", dictionary);
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    }
                } catch (PersistenceException | ReplicationException e) {
                    LOG.error("Error deleting item: {}", dictionary, e);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                }
            }
        } else {
            LOG.warn("Dictionary parameter can not be empty");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void deactivateAndDelete(ResourceResolver resourceResolver, Resource resource) throws ReplicationException, PersistenceException {
        replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.DEACTIVATE, resource.getPath());
        resourceResolver.delete(resource);
    }
}
