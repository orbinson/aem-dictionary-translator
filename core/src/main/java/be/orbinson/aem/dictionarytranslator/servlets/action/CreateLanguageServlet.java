package be.orbinson.aem.dictionarytranslator.servlets.action;

import java.io.IOException;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/create-language",
        methods = "POST"
)
public class CreateLanguageServlet extends AbstractDictionaryServlet {
    private static final Logger LOG = LoggerFactory.getLogger(CreateLanguageServlet.class);

    @Reference
    private transient DictionaryService dictionaryService;

    @Override
    protected void doPost(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        String dictionary = getMandatoryParameter(request, "dictionary", false);
        Locale language = getMandatoryParameter(request, "language", false, Locale::forLanguageTag);
        Collection<String> basenames = getOptionalParameters(request, "basename", true);

        ResourceResolver resourceResolver = request.getResourceResolver();
        try {
            LOG.debug("Adding language '{}' to dictionary '{}'", language, dictionary);
            dictionaryService.createDictionary(resourceResolver, dictionary, language, basenames);
            resourceResolver.commit();
        } catch (PersistenceException|DictionaryException e) {
            HtmlResponse htmlResponse = new HtmlResponse();
            htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Unable to add language '%s' to dictionary '%s': %s", language, dictionary, e.getMessage()));
            htmlResponse.send(response, true);
        }
    }
}
