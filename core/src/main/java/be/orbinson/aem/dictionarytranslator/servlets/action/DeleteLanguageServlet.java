package be.orbinson.aem.dictionarytranslator.servlets.action;

import java.util.Locale;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/delete-language",
        methods = "POST"
)
public class DeleteLanguageServlet extends AbstractDictionaryServlet {

    public DeleteLanguageServlet() {
        super("Unable to delete language");
    }

    public static final String LANGUAGE_PARAM = "language";
    public static final String DICTIONARY_PARAM = "dictionary";

    private static final Logger LOG = LoggerFactory.getLogger(DeleteLanguageServlet.class);

    @Reference
    private transient DictionaryService dictionaryService;

    @Reference
    private Replicator replicator;

    @Override
    protected void internalDoPost(SlingHttpServletRequest request, HtmlResponse htmlResponse) throws Throwable {
        final String language = getMandatoryParameter(request, LANGUAGE_PARAM, false);
        final String dictionary = getMandatoryParameter(request, DICTIONARY_PARAM, false);

        htmlResponse.setPath(dictionary + "/" + language);
        ResourceResolver resourceResolver = request.getResourceResolver();
        LOG.debug("Deleting language dictionary '{}' below '{}'", language, dictionary);
        try {
            dictionaryService.deleteDictionary(replicator, resourceResolver, dictionary, Locale.forLanguageTag(language));
        } catch (DictionaryException e) {
            htmlResponse.setStatus(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            return;
        }
        resourceResolver.commit();
    }

}
