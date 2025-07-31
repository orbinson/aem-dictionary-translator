package be.orbinson.aem.dictionarytranslator.servlets.action;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.replication.treeactivation.ActivationParameters;
import com.adobe.granite.replication.treeactivation.TreeActivationException;
import com.adobe.granite.replication.treeactivation.TreeActivationService;
import com.day.cq.replication.ReplicationActionType;

import be.orbinson.aem.dictionarytranslator.services.Dictionary;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;

/**
 * Similar to the {@code com.day.cq.replication.impl.servlets.CommandServlet} but for dictionaries.
 * All dictionaries below the specified parent path are replicated.
 * In contrast to the default replication servlet this servlet makes sure that all dictionary items are contained in the replication
 * (irrespective of the dictionary format/node types).
 * This servlet uses the <a href="https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/operations/replication#tree-activation">Tree Activation API</a>
 * to perform the replication, which is more suitable for AEMaaCS environments.
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(resourceTypes = "aem-dictionary-translator/servlet/action/replicate-dictionary", methods = "POST")
@ServiceRanking(1000) // Ensure this servlet is preferred over the default replication servlet
public class ReplicateDictionariesWithTreeActivationServlet extends AbstractDictionaryServlet {

    private static final long serialVersionUID = 1L;

    public ReplicateDictionariesWithTreeActivationServlet() {
        super("Unable to replicate dictionaries with tree activation API");
    }

    private static final Logger LOG = LoggerFactory.getLogger(ReplicateDictionariesWithTreeActivationServlet.class);

    private static final String ACTION_PARAM = "action";
    private static final String AGENT_ID_PARAM = "agentId";

    @Reference
    private transient DictionaryService dictionaryService;

    @Reference
    private transient TreeActivationService treeActivationService;

    @Override
    protected void internalDoPost(@NotNull SlingHttpServletRequest request, @NotNull HtmlResponse htmlResponse) throws Throwable {
        final ReplicationActionType action = getMandatoryParameter(request, ACTION_PARAM, false, ReplicationActionType::fromName);
        if (action != ReplicationActionType.ACTIVATE) {
            htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, "Only 'activate' action is supported in AEMaaCS.");
            return;
        }
        final String parentPath = getMandatoryParameter(request, PARENT_PATH_PARAM, false);
        Collection<String> agentIds = getOptionalParameters(request, AGENT_ID_PARAM, false);
        if (agentIds.isEmpty()) {
            LOG.debug("No agent specified, using default agent");
            agentIds.add(ActivationParameters.DEFAULT_AGENT_ID);
        }
        final Collection<String> messages = new LinkedList<>();
        ResourceResolver resourceResolver = request.getResourceResolver();
        int overallStatus = HttpServletResponse.SC_OK;
        for (Dictionary dictionary : dictionaryService.getDictionaries(resourceResolver, parentPath)) {
            int status = replicate(htmlResponse, dictionary, messages, resourceResolver, agentIds);
            if (overallStatus == HttpServletResponse.SC_OK) {
                overallStatus = status;
            }
        }
        htmlResponse.setStatus(overallStatus, messages.stream().collect(Collectors.joining("\n")));
    }

    private int replicate(HtmlResponse htmlResponse, Dictionary dictionary, final Collection<String> messages,
            ResourceResolver resourceResolver, Collection<String> agentIds) {
        String path = dictionary.getPath();
        LOG.debug("Replicating language dictionary '{}'", path);
        htmlResponse.setPath(path);
        for (String agentId : agentIds) {
            LOG.debug("Using agent ID: {}", agentId);
            try {
                ActivationParameters activationParameters = createActivationParameters(resourceResolver.getUserID(), path, agentId);
                String activationId = treeActivationService.start(activationParameters);
                messages.add("Replication started for dictionary at " + path + " with activation ID: " + activationId);
            } catch (TreeActivationException e) {
                LOG.error("Error while replicating dictionary at {}", path, e);
                messages.add("Error while replicating dictionary at " + path + ": " + e.getMessage());
                return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            } catch (Exception e) {
                LOG.error("Unknown exception while replicating dictionary at {}", path, e);
                messages.add("Error while replicating dictionary at " + path + ": " + e.getMessage());
                return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }
        }
        return HttpServletResponse.SC_OK;
    }

    static ActivationParameters createActivationParameters(String userId, String path, String agentId) {
        return ActivationParameters.builder()
                .path(path)
                .rootPath(path)  // is not evaluated in the TreeActivationJob but still is serialized (i.e. must not contain a null value)
                .agentId(agentId)
                // for now just hardcode the filter name, as only the SlingMessageNodeFilter is available
                .filters(List.of(SlingMessageNodeFilter.NAME))
                .activationid(createActivationId())
                // the following should not have any effect (it is not evaluated in the TreeActivationJob)
                //.includeChildren(IncludeChildren.ALL_CHILDREN)
                .enableVersion(false)
                .userId(userId)
                .build();
    }

    static String createActivationId() {
        return UUID.randomUUID().toString();
    }
}