package be.orbinson.aem.dictionarytranslator.servlets.action;

import java.io.IOException;
import java.util.Collection;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.Dictionary;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/create-message-entry",
        methods = "POST"
)
public class CreateMessageEntryServlet extends AbstractDictionaryServlet {

    private static final Logger LOG = LoggerFactory.getLogger(CreateMessageEntryServlet.class);

    @Reference
    private transient DictionaryService dictionaryService;

    @Override
    protected void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String key = getMandatoryParameter(request, "key", false);
        String dictionary = getMandatoryParameter(request, "dictionary", false);

        createMessageEntry(request, response, dictionary, key);
        
    }

    private void createMessageEntry(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response, String path, String key) throws IOException {
        ResourceResolver resourceResolver = request.getResourceResolver();
        try {
            Collection<Dictionary> dictionaries = dictionaryService.getDictionaries(resourceResolver, path);
            if (dictionaries.isEmpty()) {
                HtmlResponse htmlResponse = new HtmlResponse();
                htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("No dictionaries found at path '%s'", path));
                htmlResponse.send(response, true);
                return;
            }
            for (Dictionary languageDictionary : dictionaries) {
                if (!languageDictionary.getEntries().containsKey(key)) {
                    String message = getOptionalParameter(request, languageDictionary.getLanguage().toLanguageTag(), true).orElse(null);
                    if (message != null) {
                        languageDictionary.createOrUpdateEntry(resourceResolver, key, message);
                    } else {
                        LOG.warn("No message provided for key '{}' in language '{}'", key, languageDictionary.getLanguage().toLanguageTag());
                    }
                } else {
                    HtmlResponse htmlResponse = new HtmlResponse();
                    htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Can not create message entry %s, key already exists", key));
                    htmlResponse.send(response, true);
                    return;
                }
            }
            resourceResolver.commit();
        } catch (PersistenceException|DictionaryException e) {
            HtmlResponse htmlResponse = new HtmlResponse();
            htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Unable to create key '%s' on dictionary '%s': %s", key, path, e.getMessage()));
            htmlResponse.send(response, true);
        }
    }

}
