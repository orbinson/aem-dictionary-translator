package be.orbinson.aem.dictionarytranslator.servlets.rendercondition;

import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;
import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentManager;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static junitx.framework.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class ReplicationAgentEnabledRenderConditionTest {

    final AemContext context = new AemContext();

    ReplicationAgentEnabledRenderCondition servlet;

    @Mock
    AgentManager agentManager;

    @BeforeEach
    void setUp() {
        agentManager = context.registerService(AgentManager.class, agentManager);
        servlet = context.registerInjectActivateService(new ReplicationAgentEnabledRenderCondition());
        context.load().json("/apps.json", "/apps");
    }

    @Test
    void checkIfRenderConditionIsTrueForEnabledAgent() {
        Agent agent = mock(Agent.class);
        when(agent.isEnabled()).thenReturn(true);
        when(agentManager.getAgents()).thenReturn(Map.of("publish", agent));

        context.currentResource("/apps/aem-dictionary-translator/content/granite/field/publish/granite:rendercondition");

        servlet.doGet(context.request(), context.response());

        SimpleRenderCondition renderCondition = (SimpleRenderCondition) context.request().getAttribute(RenderCondition.class.getName());
        assertTrue(renderCondition.check());
    }

    @Test
    void checkIfRenderConditionIsFalseForDisabledAgent() {
        Agent agent = mock(Agent.class);
        when(agent.isEnabled()).thenReturn(false);
        when(agentManager.getAgents()).thenReturn(Map.of("preview", agent));

        context.currentResource("/apps/aem-dictionary-translator/content/granite/field/publish-preview/granite:rendercondition");

        servlet.doGet(context.request(), context.response());

        SimpleRenderCondition renderCondition = (SimpleRenderCondition) context.request().getAttribute(RenderCondition.class.getName());
        assertFalse(renderCondition.check());
    }
}