package be.orbinson.aem.dictionarytranslator.servlets.rendercondition;

import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;
import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

@Component(service = Servlet.class)
@SlingServletResourceTypes(resourceTypes = "aem-dictionary-translator/components/rendercondition/replication-agent-enabled")
public class ReplicationAgentEnabledRenderCondition extends SlingSafeMethodsServlet {

    @Reference
    private AgentManager agentManager;

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        String agentId = new Config(request.getResource()).get("agentId");
        boolean isEnabled = false;
        Agent agent = agentManager.getAgents().get(agentId);
        if (agent != null) {
            isEnabled = agent.isEnabled();
        }
        SimpleRenderCondition simpleRenderCondition = new SimpleRenderCondition(isEnabled);
        request.setAttribute(RenderCondition.class.getName(), simpleRenderCondition);
    }
}
