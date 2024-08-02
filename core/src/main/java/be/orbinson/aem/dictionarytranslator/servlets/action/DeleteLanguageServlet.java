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
        resourceTypes = "aem-dictionary-translator/servlet/action/delete-language",
        methods = "POST"
)
public class DeleteLanguageServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteLanguageServlet.class);
    public static final String LANGUAGE_PARAM = "language";
    public static final String DICTIONARY_PARAM = "dictionary";

    @Reference
    private transient Replicator replicator;

    @Override
    @SuppressWarnings("java:S1075")
    public void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        final String language = request.getParameter(LANGUAGE_PARAM);
        final String dictionary = request.getParameter(DICTIONARY_PARAM);

        if (StringUtils.isNotEmpty(dictionary) && StringUtils.isNotEmpty(language)) {
            ResourceResolver resourceResolver = request.getResourceResolver();
            String path = dictionary + "/" + language;
            Resource resource = resourceResolver.getResource(path);

            try {
                if (resource != null) {
                    LOG.debug("Delete language '{}' from '{}'", language, dictionary);
                    deactivateAndDelete(resourceResolver, resource);
                    resourceResolver.commit();
                } else {
                    LOG.warn("Unable to find dictionary '{}'", path);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                }
            } catch (PersistenceException | ReplicationException e) {
                LOG.error("Unable to delete language '{}' from dictionary '{}'", language, dictionary);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            LOG.warn("Language and dictionary parameter are required");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void deactivateAndDelete(ResourceResolver resourceResolver, Resource resource) throws ReplicationException, PersistenceException {
        replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.DEACTIVATE, resource.getPath());
        resourceResolver.delete(resource);
    }
}
