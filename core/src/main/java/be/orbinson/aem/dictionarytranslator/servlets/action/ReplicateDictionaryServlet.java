package be.orbinson.aem.dictionarytranslator.servlets.action;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.distribution.Distributor;
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
        resourceTypes = "aem-dictionary-translator/servlet/action/replicate-dictionary",
        methods = "POST"
)
public class ReplicateDictionaryServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ReplicateDictionaryServlet.class);

    @Reference
    private transient Distributor distributor;

    @Reference
    private transient Replicator replicator;

    private ResourceResolver resourceResolver;

    @Override
    protected void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String path = request.getParameter("path");

        if (StringUtils.isEmpty(path)) {
            LOG.warn("Invalid parameters to replicate dictionary");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            resourceResolver = request.getResourceResolver();
            Resource resource = resourceResolver.getResource(path);

            if (resource != null) {
                deepReplicate(resource);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Replicated dictionary, 'path={}'", path.replaceAll("[\n\r]", "_"));
                }
            } else {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Unable to get resource for path '{}", path.replaceAll("[\n\r]", "_"));
                }
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }

    private void deepReplicate(Resource parentResource) {
        String path = parentResource.getPath();
        try {
            replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.ACTIVATE, path);
        } catch (ReplicationException e) {
            LOG.error("ReplicationException occurred when trying to replicate dictionary in ReplicateDictionaryServlet");
        }

        if (parentResource.hasChildren()) {
            for (Resource childResource : parentResource.getChildren()) {
                deepReplicate(childResource);
            }
        }
    }
}
