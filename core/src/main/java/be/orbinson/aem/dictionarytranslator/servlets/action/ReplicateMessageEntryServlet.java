package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.CombiningMessageEntryResourceProvider;
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
        resourceTypes = "aem-dictionary-translator/servlet/action/publish-message-entry",
        methods = "POST"
)
public class ReplicateMessageEntryServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ReplicateMessageEntryServlet.class);

    @Reference
    private transient Replicator replicator;

    @Override
    protected void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String[] combiningMessageEntryPaths = request.getParameterValues("item");

        if (combiningMessageEntryPaths == null) {
            LOG.warn("At least one item parameter is required");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            try {
                for (String combiningMessageEntryPath : combiningMessageEntryPaths) {
                    ResourceResolver resourceResolver = request.getResourceResolver();
                    Resource combiningMessageEntry = resourceResolver.getResource(combiningMessageEntryPath);
                    if (combiningMessageEntry != null) {
                        String[] messageEntryPaths = combiningMessageEntry.getValueMap().get(CombiningMessageEntryResourceProvider.MESSAGE_ENTRY_PATHS, new String[0]);
                        for (String messageEntryPath : messageEntryPaths) {
                            replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.ACTIVATE, messageEntryPath);
                            LOG.debug("Published message entry for path '{}'", messageEntryPath);
                        }
                    } else {
                        HtmlResponse htmlResponse = new HtmlResponse();
                        htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Unable to get message entry '%s", combiningMessageEntryPath));
                        htmlResponse.send(response, true);
                    }

                }
            } catch (ReplicationException e) {
                HtmlResponse htmlResponse = new HtmlResponse();
                htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while replicating message entry: " + e);
                htmlResponse.send(response, true);
            }
        }
    }

}
