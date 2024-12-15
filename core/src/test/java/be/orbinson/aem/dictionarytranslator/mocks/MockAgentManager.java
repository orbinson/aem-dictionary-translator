package be.orbinson.aem.dictionarytranslator.mocks;

import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentManager;

import java.util.Map;

public class MockAgentManager implements AgentManager {
    @Override
    public Map<String, Agent> getAgents() {
        MockAgent publishAgent = new MockAgent("publish", true);
        MockAgent previewAgent = new MockAgent("preview", false);
        return Map.of("publish", publishAgent, "preview", previewAgent);
    }
}
