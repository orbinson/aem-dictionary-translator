package be.orbinson.aem.dictionarytranslator.replication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.AccessDeniedException;
import com.day.cq.replication.AgentIdFilter;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.workflow.api.WcmWorkflowService;

/** Replicates given paths in chunks. Mostly a copy of com.day.cq.wcm.workflow.process.impl.ChunkedReplicated which is unfortunately not public API. */
public class ChunkedReplicator implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ChunkedReplicator.class);

    private final Replicator replicator;
    private final EventAdmin eventAdmin;

    private final int chunkSize;

    private final Session replicationSession;

    private final ReplicationOptions options;

    private final String chunkReplicatorId;

    private final List<String> paths;

    private int totalPathsSubmitted = 0;

    private Collection<String> agentIds;

    public ChunkedReplicator(Session session, Replicator replicator, EventAdmin eventAdmin, Collection<String> agentIds, int chunkSize, String id) {
        this.replicationSession = session;
        this.replicator = replicator;
        this.eventAdmin = eventAdmin;
        this.options = new ReplicationOptions();
        if (!agentIds.isEmpty()) {
            options.setFilter(new AgentIdFilter(agentIds.toArray(new String[0])));
        }
        this.agentIds = agentIds;
        this.chunkSize = chunkSize;
        this.chunkReplicatorId = "chunkedReplication-" + id;
        this.paths = new ArrayList<>(chunkSize);
    }

    public boolean submitPath(String path) throws ReplicationException {
        try {
            replicator.checkPermission(replicationSession, ReplicationActionType.ACTIVATE, path);
            this.paths.add(path);
            LOG.trace("submitting {} to {}", path, this.chunkReplicatorId);
            this.totalPathsSubmitted++;
            if (this.paths.size() == this.chunkSize) {
                runReplication();
                LOG.info("{} replicated chunk ({} paths already replicated)", this.chunkReplicatorId,
                        Integer.valueOf(this.totalPathsSubmitted));
                return true;
            }
        } catch (AccessDeniedException e) {
            LOG.warn("{} is not allowed to replicate the path {}. Sending request for activation instead.", this.replicationSession.getUserID(),
                    path);
            sendRequestForActivationEvent(path);
        }
        return false;
    }

    public void close() throws ReplicationException {
        runReplication();
        LOG.info("closed {}, {} paths replicated", this.chunkReplicatorId,
                this.totalPathsSubmitted);
    }

    void runReplication() throws ReplicationException {
        if (!this.paths.isEmpty()) {
            String[] pathsArray = this.paths.toArray(new String[0]);
            replicate(pathsArray);
            this.paths.clear();
        }
    }

    void replicate(String[] paths) throws ReplicationException {
        this.replicator.replicate(this.replicationSession, ReplicationActionType.ACTIVATE, paths, this.options);
    }

    public int getReplicatedPaths() {
        return this.totalPathsSubmitted;
    }

    private void sendRequestForActivationEvent(String path) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("path", path);
        properties.put("replicationType", ReplicationActionType.ACTIVATE);
        properties.put("userId", replicationSession.getUserID());
        if (!agentIds.isEmpty()) {
            // just use the first agentId for the event
            properties.put("agentId", agentIds.iterator().next());
        }
        Event event = new Event(WcmWorkflowService.EVENT_TOPIC, properties);
        this.eventAdmin.sendEvent(event);
    }
}
