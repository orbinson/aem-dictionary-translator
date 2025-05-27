package be.orbinson.aem.dictionarytranslator.servlets.action;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.day.cq.replication.ReplicationException;
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

    public static final String LANGUAGE_PARAM = "language";
    public static final String DICTIONARY_PARAM = "dictionary";

    @Reference
    private transient DictionaryService dictionaryService;

    @Reference
    private Replicator replicator;

    @Override
    @SuppressWarnings("java:S1075")
    public void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        final String language = getMandatoryParameter(request, LANGUAGE_PARAM, false);
        final String dictionary = getMandatoryParameter(request, DICTIONARY_PARAM, false);

        ResourceResolver resourceResolver = request.getResourceResolver();
        try {
            dictionaryService.deleteDictionary(replicator, resourceResolver, dictionary, Locale.forLanguageTag(language));
            resourceResolver.commit();
        } catch (DictionaryException | PersistenceException | ReplicationException e) {
            HtmlResponse htmlResponse = new HtmlResponse();
            htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Unable to delete language '%s': %s", language, e.getMessage()));
            htmlResponse.send(response, true);
        } 
    }

}
