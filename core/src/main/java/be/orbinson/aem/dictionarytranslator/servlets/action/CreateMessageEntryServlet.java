package be.orbinson.aem.dictionarytranslator.servlets.action;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

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

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.Dictionary;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.servlets.datasource.CombiningMessageEntryDatasourceForDialog;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/create-message-entry",
        methods = "POST"
)
public class CreateMessageEntryServlet extends AbstractDictionaryServlet {

    public CreateMessageEntryServlet() {
        super("Unable to create message entry");
    }

    private static final Logger LOG = LoggerFactory.getLogger(CreateMessageEntryServlet.class);

    @Reference
    private transient DictionaryService dictionaryService;

    @Override
    protected void internalDoPost(SlingHttpServletRequest request, HtmlResponse htmlResponse) throws Throwable {
        String key = getMandatoryParameter(request, "key", false);
        String dictionary = getMandatoryParameter(request, "dictionary", false);
        htmlResponse.setCreateRequest(true);
        createMessageEntry(request, htmlResponse, dictionary, key);
    }

    private void createMessageEntry(SlingHttpServletRequest request, HtmlResponse htmlResponse, String path, String key) throws IOException, DictionaryException {
        ResourceResolver resourceResolver = request.getResourceResolver();
        Collection<Dictionary> dictionaries = dictionaryService.getDictionaries(resourceResolver, path);
        if (dictionaries.isEmpty()) {
            htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("No dictionaries found at path '%s'", path));
            return;
        }
        for (Dictionary languageDictionary : dictionaries) {
            if (!languageDictionary.getEntries().containsKey(key)) {
                String name = languageDictionary.getLanguage().toLanguageTag();
                String message = getOptionalParameter(request, name, true).orElse("");
                boolean useEmpty = getOptionalParameter(request, name + CombiningMessageEntryDatasourceForDialog.SUFFIX_USE_EMPTY, false, Boolean::parseBoolean).orElse(false);
                Optional<String> messageOptional;
                if (useEmpty || !message.isEmpty()) {
                    messageOptional = Optional.of(message);
                } else {
                    messageOptional = Optional.empty();
                }
                LOG.debug("Creating message entry for key '{}' in dictionary '{}'...", key, languageDictionary.getPath());
                languageDictionary.createEntry(resourceResolver, key, messageOptional);
            } else {
                htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Can not create message entry %s, key already exists", key));
                return;
            }
        }
        resourceResolver.commit();
    }
}
