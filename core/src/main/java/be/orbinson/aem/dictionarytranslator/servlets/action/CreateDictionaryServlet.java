package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/create-dictionary",
        methods = "POST"
)
public class CreateDictionaryServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(CreateDictionaryServlet.class);

    @Reference
    private transient DictionaryService dictionaryService;

    @Override
    protected void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String name = request.getParameter("name");
        String path = request.getParameter("path");
        String basename = request.getParameter("basename");

        String[] languages = request.getParameterValues("language");

        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(path) || languages == null) {
            HtmlResponse htmlResponse = new HtmlResponse();
            htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Invalid parameters to create dictionary, 'dictionary=%s', 'path=%s', 'languages=%s', 'basename=%s'", name, path, String.join(",", Arrays.asList(Optional.ofNullable(languages).orElse(new String[0]))), basename));
            htmlResponse.send(response, true);
        } else {
            final ResourceResolver resourceResolver = request.getResourceResolver();
            Resource resource = resourceResolver.getResource(path);

            if (resource != null) {
                try {
                    LOG.debug("Create dictionary '{}'", name);
                    dictionaryService.createDictionary(resource, name, languages, basename);
                } catch (PersistenceException e) {
                    HtmlResponse htmlResponse = new HtmlResponse();
                    htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Unable to create dictionary: %s", e.getMessage()));
                    htmlResponse.send(response, true);
                }
            } else {
                HtmlResponse htmlResponse = new HtmlResponse();
                htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Unable to get resource for path '%s", path));
                htmlResponse.send(response, true);
            }
        }
    }
}
