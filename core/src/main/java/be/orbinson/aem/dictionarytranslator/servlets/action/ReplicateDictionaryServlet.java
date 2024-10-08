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
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
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
    private transient Replicator replicator;

    @Override
    protected void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String path = request.getParameter("path");

        if (StringUtils.isEmpty(path)) {
            LOG.warn("Invalid parameters to replicate dictionary");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            ResourceResolver resourceResolver = request.getResourceResolver();
            Resource resource = resourceResolver.getResource(path);

            try {
                if (resource != null) {
                    deepReplicate(resourceResolver, resource);
                    // javasecurity:S5145
                    LOG.debug("Replicated dictionary, 'path={}'", path);
                } else {
                    // javasecurity:S5145
                    HtmlResponse htmlResponse = new HtmlResponse();
                    htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Unable to get resource for path '%s", path));
                    htmlResponse.send(response, true);
                }
            } catch (ReplicationException e) {
                HtmlResponse htmlResponse = new HtmlResponse();
                htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while replicating dictionary: " + e);
                htmlResponse.send(response, true);
            }
        }
    }

    private void deepReplicate(ResourceResolver resourceResolver, Resource parentResource) throws ReplicationException {
        String path = parentResource.getPath();
        replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.ACTIVATE, path);

        if (parentResource.hasChildren()) {
            for (Resource childResource : parentResource.getChildren()) {
                deepReplicate(resourceResolver, childResource);
            }
        }
    }
}
