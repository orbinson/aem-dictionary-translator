package be.orbinson.aem.dictionarytranslator.mocks;

import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentConfig;
import com.day.cq.replication.CompositeReplicationAction;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationContent;
import com.day.cq.replication.ReplicationContentFacade;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationLog;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.ReplicationQueue;
import com.day.cq.replication.ReverseReplication;

import javax.jcr.Session;
import java.util.Calendar;
import java.util.Map;

public class MockAgent implements Agent {
    private final String id;
    private final boolean enabled;

    public MockAgent(String id, boolean enabled) {
        this.id = id;
        this.enabled = enabled;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void checkValid() {
        // no-op
    }

    @Override
    public AgentConfig getConfiguration() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ReplicationLog getLog() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void replicate(ReplicationAction replicationAction, ReplicationContent replicationContent, ReplicationOptions replicationOptions) throws ReplicationException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void replicate(CompositeReplicationAction compositeReplicationAction, ReplicationContent replicationContent, ReplicationOptions replicationOptions) throws ReplicationException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ReverseReplication[] poll(ReplicationAction replicationAction) throws ReplicationException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ReplicationContent buildContent(Session session, ReplicationAction replicationAction) throws ReplicationException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ReplicationContent buildContent(Session session, ReplicationAction replicationAction, Map<String, Object> map) throws ReplicationException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ReplicationContent getContent(ReplicationContentFacade replicationContentFacade) throws ReplicationException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ReplicationQueue getQueue() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean isCacheInvalidator() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setNextPollTimeline(Calendar calendar) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Calendar getLastPollTimeline() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean isInMaintenanceMode() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean supportsBulkContentBuilding() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ReplicationContent buildBulkContent(Session session, CompositeReplicationAction compositeReplicationAction, Map<String, Object> map) throws ReplicationException {
        throw new UnsupportedOperationException("Not implemented");
    }
}
