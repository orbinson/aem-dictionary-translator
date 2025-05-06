package be.orbinson.aem.dictionarytranslator.servlets.action;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

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

import com.day.cq.replication.ReplicationException;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/delete-language",
        methods = "POST"
)
public class DeleteLanguageServlet extends SlingAllMethodsServlet {

    public static final String LANGUAGE_PARAM = "language";
    public static final String DICTIONARY_PARAM = "dictionary";

    @Reference
    private transient DictionaryService dictionaryService;

    @Override
    @SuppressWarnings("java:S1075")
    public void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        final String language = request.getParameter(LANGUAGE_PARAM);
        final String dictionary = request.getParameter(DICTIONARY_PARAM);

        if (StringUtils.isNotEmpty(dictionary) && StringUtils.isNotEmpty(language)) {
            ResourceResolver resourceResolver = request.getResourceResolver();
            Resource dictionaryResource = resourceResolver.getResource(dictionary);
            try {
                if (dictionaryResource != null) {
                    dictionaryService.deleteLanguage(dictionaryResource, language);
                } else {
                    HtmlResponse htmlResponse = new HtmlResponse();
                    htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Unable to get dictionary '%s'", dictionary));
                    htmlResponse.send(response, true);
                }
            } catch (DictionaryException | PersistenceException | ReplicationException e) {
                HtmlResponse htmlResponse = new HtmlResponse();
                htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Unable to delete language '%s': %s", language, e.getMessage()));
                htmlResponse.send(response, true);
            }
        } else {
            HtmlResponse htmlResponse = new HtmlResponse();
            htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, "Language and dictionary parameter can not be empty");
            htmlResponse.send(response, true);
        }
    }

}
