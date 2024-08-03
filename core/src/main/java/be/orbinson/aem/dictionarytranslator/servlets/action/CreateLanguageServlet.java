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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/create-language",
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
                    HtmlResponse htmlResponse = new HtmlResponse();
                    htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Unable to get dictionary for path '%s", dictionary));
                    htmlResponse.send(response, true);
                }
            } catch (PersistenceException e) {
                HtmlResponse htmlResponse = new HtmlResponse();
                htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Unable to add language '%s' to dictionary '%s': %s", language, dictionary, e.getMessage()));
                htmlResponse.send(response, true);
            }
        } else {
            HtmlResponse htmlResponse = new HtmlResponse();
            htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, "Dictionary and language parameter are required to add a language to a dictionary");
            htmlResponse.send(response, true);
        }
    }
}
