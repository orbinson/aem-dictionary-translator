package be.orbinson.aem.dictionarytranslator.servlets.action;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.adobe.granite.replication.treeactivation.ActivationParameters;
import com.adobe.granite.replication.treeactivation.IncludeChildren;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class ReplicateDictionariesWithTreeActivationServletTest {

    // make sure to test against JCR mock to mimic behavior of dealing with null property values
    private final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Test
    void checkSerializationOfJobParameters() throws PersistenceException {
        ActivationParameters parameters = ReplicateDictionariesWithTreeActivationServlet.createActivationParameters("user1", "/content/my/path", "publish");
        // imitate what the com.adobe.granite.replication.treeactivation.service.impl.TreeActivationServiceImpl.start is doing (i.e. writing the job parameters into the JCR)
        Map<String, Object> jobParams = new JobParamWriter(parameters).getJobArgs(parameters.getPath());
        assertNotNull(jobParams);
        ResourceUtil.getOrCreateResource(context.resourceResolver(), "/tmp/jobParams", jobParams, "sling:Folder", true);
    }

    /** 
     * Copy of com.adobe.granite.replication.treeactivation.service.impl.JobParameterWriter from bundle com.adobe.granite.replication.treeactivation 1.1.2
     */
    private static final class JobParamWriter {
        private ActivationParameters params;

        public JobParamWriter(final ActivationParameters params) {
            this.params = params;
        }

        public Map<String, Object> getJobArgs(final String path) {
            final HashMap<String, Object> args = new HashMap<String, Object>();
            args.put("path", path);
            args.put("activationId", this.params.getActivationid());
            args.put("agentId", this.params.getAgentId());
            args.put("chunkSize", this.params.getChunkSize());
            args.put("dryRun", this.params.isDryRun());
            args.put("enableVersion", this.params.isEnableVersion());
            args.put("includeChildren", this.params.getIncludeChildren() == IncludeChildren.ALL_CHILDREN
                    || this.params.getIncludeChildren() == IncludeChildren.DIRECT_CHILDREN);
            args.put("onlyChildren", this.params.getIncludeChildren() == IncludeChildren.DIRECT_CHILDREN);
            args.put("maxQueueSize", this.params.getMaxQueueSize());
            args.put("maxTreeSize", this.params.getMaxTreeSize());
            args.put("maxLevel", this.params.getMaxLevel());
            args.put("filters", this.getFilterNames(this.params.getFilters()));
            args.put("rootPath", this.params.getRootPath());
            args.put("userId", this.params.getUserId());
            args.put("operationName", "Treeactivation");
            args.put("operationTitle", "Tree activation");
            args.put("description", "Tree activation " + this.params.getActivationid() + " for path " + path);
            return args;
        }

        private String getFilterNames(final List<String> filters) {
            return filters.stream().collect(Collectors.joining(";"));
        }
    }
}
