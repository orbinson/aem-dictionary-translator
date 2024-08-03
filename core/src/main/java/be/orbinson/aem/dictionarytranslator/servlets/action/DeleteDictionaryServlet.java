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
                        HtmlResponse htmlResponse = new HtmlResponse();
                        htmlResponse.setStatus(HttpServletResponse.SC_NOT_FOUND, "Dictionary not found");
                        htmlResponse.send(response, true);
                    }
                } catch (PersistenceException | ReplicationException e) {
                    HtmlResponse htmlResponse = new HtmlResponse();
                    htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Unable to delete dictionary '%s': %s", dictionary, e.getMessage()));
                    htmlResponse.send(response, true);
                }
            }
        } else {
            HtmlResponse htmlResponse = new HtmlResponse();
            htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, "Dictionary parameter can not be empty");
            htmlResponse.send(response, true);
        }
    }

    private void deactivateAndDelete(ResourceResolver resourceResolver, Resource resource) throws ReplicationException, PersistenceException {
        replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.DEACTIVATE, resource.getPath());
        resourceResolver.delete(resource);
    }
}
