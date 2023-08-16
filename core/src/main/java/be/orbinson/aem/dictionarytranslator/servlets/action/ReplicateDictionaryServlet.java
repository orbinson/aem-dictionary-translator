package be.orbinson.aem.dictionarytranslator.servlets.action;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.Distributor;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/replicate-dictionary",
        methods = "POST"
)
public class ReplicateDictionaryServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ReplicateDictionaryServlet.class);

    @Reference
    private transient Distributor distributor;

    @Override
    protected void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String path = request.getParameter("path");

        if (StringUtils.isEmpty(path)) {
            LOG.warn("Invalid parameters to replicate dictionary");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            final ResourceResolver resourceResolver = request.getResourceResolver();
            Resource resource = resourceResolver.getResource(path);

            if (resource != null) {
                DistributionRequest distributionRequest = new SimpleDistributionRequest(DistributionRequestType.ADD, true, path);
                distributor.distribute("publish", resourceResolver, distributionRequest);

                if (LOG.isDebugEnabled()) {
                    // javasecurity:S5145
                    LOG.debug("Replicated dictionary, 'path={}'", path.replaceAll("[\n\r]", "_"));
                }
            } else {
                if (LOG.isWarnEnabled()) {
                    // javasecurity:S5145
                    LOG.warn("Unable to get resource for path '{}", path.replaceAll("[\n\r]", "_"));
                }
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }
}
