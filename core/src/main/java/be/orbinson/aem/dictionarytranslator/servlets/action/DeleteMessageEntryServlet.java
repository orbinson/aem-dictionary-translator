package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import com.day.cq.replication.ReplicationException;
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

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/delete-message-entry",
        methods = "POST"
)
public class DeleteMessageEntryServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteMessageEntryServlet.class);
    public static final String ITEM_PARAM = "item";

    @Reference
    private transient DictionaryService dictionaryService;

    @Override
    protected void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String[] combiningMessageEntryPaths = request.getParameterValues(ITEM_PARAM);

        if (combiningMessageEntryPaths == null) {
            HtmlResponse htmlResponse = new HtmlResponse();
            htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, "item parameter is required");
            htmlResponse.send(response, true);
        } else {
            for (String combiningMessageEntryPath : combiningMessageEntryPaths) {
                ResourceResolver resourceResolver = request.getResourceResolver();
                Resource combiningMessageEntryResource = resourceResolver.getResource(combiningMessageEntryPath);
                try {
                    if (combiningMessageEntryResource != null) {
                        // javasecurity:S5145
                        LOG.debug("Delete message entry for path '{}'", combiningMessageEntryResource.getPath());
                        dictionaryService.deleteMessageEntry(resourceResolver, combiningMessageEntryResource);
                    } else {
                        // javasecurity:S5145
                        HtmlResponse htmlResponse = new HtmlResponse();
                        htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Unable to get message entry '%s'", combiningMessageEntryPath));
                        htmlResponse.send(response, true);
                    }
                } catch (PersistenceException | ReplicationException e) {
                    HtmlResponse htmlResponse = new HtmlResponse();
                    htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Unable to delete message entry '%s': %s", combiningMessageEntryPath, e.getMessage()));
                    htmlResponse.send(response, true);
                }
            }
        }
    }


}
