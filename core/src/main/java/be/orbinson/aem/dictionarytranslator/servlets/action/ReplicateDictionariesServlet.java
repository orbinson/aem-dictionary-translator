package be.orbinson.aem.dictionarytranslator.servlets.action;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.jcr.ItemVisitor;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.replication.ChunkedReplicator;
import be.orbinson.aem.dictionarytranslator.replication.TraversingNodeReplicatorVisitor;
import be.orbinson.aem.dictionarytranslator.services.Dictionary;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;

/**
 * Similar to the {@code com.day.cq.replication.impl.servlets.CommandServlet} but for dictionaries.
 * All dictionaries below the specified parent path are replicated.
 * In contrast to the default replication servlet this servlet makes sure that all dictionary items are contained in the replication
 * (irrespective of the dictionary format/node types).
 * It replicates the logic of `Publish Content Tree` workflow process (`com.day.cq.wcm.workflow.process.impl.treeactivation.TreeActivationWorkflowProcess`) as there is no other API provided by AEM for doing
 * this chunked replication.
 * This servlet is blocking, i.e. it waits for the replication to finish before returning a response.
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(resourceTypes = "aem-dictionary-translator/servlet/action/replicate-dictionary", methods = "POST")
public class ReplicateDictionariesServlet extends AbstractDictionaryServlet {

    private static final long serialVersionUID = 1L;

    public ReplicateDictionariesServlet() {
        super("Unable to replicate dictionaries");
    }

    private static final Logger LOG = LoggerFactory.getLogger(ReplicateDictionariesServlet.class);
    private static final String ACTION_PARAM = "action";
    private static final String AGENT_ID_PARAM = "agentId";
    private static final Integer DEFAULT_CHUNK_SIZE = 100; // Default chunk size for replication (in # of nodes)
    private static final String PARAM_CHUNK_SIZE = "chunkSize";

    @Reference
    private transient DictionaryService dictionaryService;

    @Reference
    private transient Replicator replicator;

    @Reference
    private transient EventAdmin eventAdmin;

    @Override
    protected void internalDoPost(@NotNull SlingHttpServletRequest request, @NotNull HtmlResponse htmlResponse) throws Throwable {
        final ReplicationActionType action = getMandatoryParameter(request, ACTION_PARAM, false, ReplicationActionType::fromName);
        if (action != ReplicationActionType.ACTIVATE) {
            throw new IllegalArgumentException("Unsupported replication action: " + action);
        }
        final String parentPath = getMandatoryParameter(request, PARENT_PATH_PARAM, false);
        Collection<String> agentIds = getOptionalParameters(request, AGENT_ID_PARAM, false);
        if (agentIds.isEmpty()) {
            LOG.debug("No agent specified, using default agent");
        }
        final Collection<String> messages = new LinkedList<>();
        ResourceResolver resourceResolver = request.getResourceResolver();
        int overallStatus = HttpServletResponse.SC_OK;
        // 
        Optional<Integer> chunkSize = getOptionalParameter(request, PARAM_CHUNK_SIZE, false, Integer::parseInt);
        Session session = resourceResolver.adaptTo(Session.class);
        try (ChunkedReplicator chunkedReplicator = new ChunkedReplicator(session, this.replicator, eventAdmin, agentIds, chunkSize.orElse(DEFAULT_CHUNK_SIZE), "replicate-dictionary")) {
            for (Dictionary dictionary : dictionaryService.getDictionaries(resourceResolver, parentPath)) {
                int status = replicate(htmlResponse, dictionary, messages, session, chunkedReplicator);
                if (overallStatus == HttpServletResponse.SC_OK) {
                    overallStatus = status;
                }
            }
        }
        htmlResponse.setStatus(overallStatus, messages.stream().collect(Collectors.joining("\n")));
    }

    private int replicate(HtmlResponse htmlResponse, Dictionary dictionary, final Collection<String> messages,
            Session session, ChunkedReplicator chunkedReplicator) {
        String path = dictionary.getPath();
        LOG.debug("Replicating language dictionary '{}'", path);
        htmlResponse.setPath(path);
        ItemVisitor replicatorVisitor = new TraversingNodeReplicatorVisitor(chunkedReplicator, dictionary.getNodeFilter(), 3);
        try {
            replicatorVisitor.visit(session.getNode(path));
        } catch (RepositoryException e) {
            messages.add("Replication failed for " + path + ": " + e.getMessage());
            LOG.error("Error while replicating dictionary at path '{}'", path, e);
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        messages.add("Replication started for dictionary " + path);
        return HttpServletResponse.SC_OK;
    }
}