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
    public CreateLanguageServlet() {
        super("Unable to add language");
    }

    private static final Logger LOG = LoggerFactory.getLogger(CreateLanguageServlet.class);

    @Reference
    private transient DictionaryService dictionaryService;

    @Override
    protected void internalDoPost(SlingHttpServletRequest request, HtmlResponse htmlResponse) throws Throwable {
        String dictionary = getMandatoryParameter(request, "dictionary", false);
        Locale language = getMandatoryParameter(request, "language", false, Locale::forLanguageTag);
        Collection<String> basenames = getOptionalParameters(request, "basename", true);
        htmlResponse.setCreateRequest(true);
        htmlResponse.setPath(dictionary + "/" + language.toLanguageTag()); // close enough to the actual path
        ResourceResolver resourceResolver = request.getResourceResolver();
        LOG.debug("Adding language '{}' to dictionary '{}'...", language, dictionary);
        dictionaryService.createDictionary(resourceResolver, dictionary, language, basenames);
        resourceResolver.commit();
        
    }
}
