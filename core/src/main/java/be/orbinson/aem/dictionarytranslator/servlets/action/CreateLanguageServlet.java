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
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "dictionary-translator/servlet/action/create-language",
        methods = "POST"
)
public class CreateLanguageServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(CreateLanguageServlet.class);

    @Reference
    private transient DictionaryService dictionaryService;

    @Override
    protected void doPost(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        String dictionary = request.getParameter("dictionary");
        String language = request.getParameter("language");
        String basename = request.getParameter("basename");

        if (StringUtils.isNotEmpty(dictionary) && StringUtils.isNotEmpty(language)) {
            ResourceResolver resourceResolver = request.getResourceResolver();
            Resource resource = resourceResolver.getResource(dictionary);
            try {
                if (resource != null) {
                    LOG.debug("Adding language '{}' to dictionary '{}'", language, dictionary);
                    dictionaryService.addLanguage(resource, language, basename);
                } else {
                    LOG.warn("Unable to get dictionary '{}'", dictionary);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                }
            } catch (PersistenceException e) {
                LOG.error("Unable to add language '{}' to dictionary '{}'", language, dictionary);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            LOG.warn("Dictionary and language parameter are required to add a language to a dictionary");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
