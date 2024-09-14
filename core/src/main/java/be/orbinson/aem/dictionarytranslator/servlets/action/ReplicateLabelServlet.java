package be.orbinson.aem.dictionarytranslator.servlets.action;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
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
        resourceTypes = "aem-dictionary-translator/servlet/action/publish-label",
        methods = "POST"
)
public class ReplicateLabelServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ReplicateLabelServlet.class);

    @Reference
    private transient Replicator replicator;

    @Override
    protected void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String[] labels = request.getParameterValues("item");

        if (labels == null) {
            LOG.warn("At least one item parameter is required");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            try {
                for (String label : labels) {
                    ResourceResolver resourceResolver = request.getResourceResolver();
                    Resource labelResource = resourceResolver.getResource(label);
                    if (labelResource != null) {
                        String[] labelPaths = labelResource.getValueMap().get("labelPaths", new String[0]);
                        for (String labelPath : labelPaths) {
                            replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.ACTIVATE, labelPath);
                            LOG.debug("Published label on path '{}'", labelPath);
                        }
                    } else {
                        HtmlResponse htmlResponse = new HtmlResponse();
                        htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Unable to get label '%s", label));
                        htmlResponse.send(response, true);
                    }

                }
            } catch (ReplicationException e) {
                HtmlResponse htmlResponse = new HtmlResponse();
                htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while replicating label: " + e);
                htmlResponse.send(response, true);
            }
        }
    }

}
