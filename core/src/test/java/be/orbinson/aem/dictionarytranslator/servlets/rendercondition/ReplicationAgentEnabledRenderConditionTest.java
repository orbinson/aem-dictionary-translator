package be.orbinson.aem.dictionarytranslator.servlets.rendercondition;

import be.orbinson.aem.dictionarytranslator.mocks.MockAgentManager;
import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;
import com.day.cq.replication.AgentManager;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static junitx.framework.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class ReplicationAgentEnabledRenderConditionTest {

    final AemContext context = new AemContext();

    ReplicationAgentEnabledRenderCondition servlet;

    AgentManager agentManager;

    @BeforeEach
    void setUp() {
        agentManager = context.registerService(AgentManager.class, new MockAgentManager());
        servlet = context.registerInjectActivateService(new ReplicationAgentEnabledRenderCondition());
        context.load().json("/apps.json", "/apps");
    }

    @Test
    void checkIfRenderConditionIsTrueForEnabledAgent() {
        context.currentResource("/apps/aem-dictionary-translator/content/granite/field/publish/granite:rendercondition");

        servlet.doGet(context.request(), context.response());

        SimpleRenderCondition renderCondition = (SimpleRenderCondition) context.request().getAttribute(RenderCondition.class.getName());
        assertTrue(renderCondition.check());
    }

    @Test
    void checkIfRenderConditionIsFalseForDisabledAgent() {
        context.currentResource("/apps/aem-dictionary-translator/content/granite/field/publish-preview/granite:rendercondition");

        servlet.doGet(context.request(), context.response());

        SimpleRenderCondition renderCondition = (SimpleRenderCondition) context.request().getAttribute(RenderCondition.class.getName());
        assertFalse(renderCondition.check());
    }
}