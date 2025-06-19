package be.orbinson.aem.dictionarytranslator.servlets.action;

import java.util.Locale;
import java.util.Optional;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.Dictionary;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider;
import be.orbinson.aem.dictionarytranslator.servlets.datasource.CombiningMessageEntryDatasourceForDialog;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/update-message-entry",
        methods = "POST"
)
public class UpdateMessageEntryServlet extends AbstractDictionaryServlet {

    public UpdateMessageEntryServlet() {
        super("Unable to update message entry");
    }

    private static final Logger LOG = LoggerFactory.getLogger(UpdateMessageEntryServlet.class);

    @Reference
    private transient DictionaryService dictionaryService;

    @Reference
    private Replicator replicator;

    @Override
    protected void internalDoPost(SlingHttpServletRequest request, HtmlResponse htmlResponse) throws Throwable {
        String combiningMessageEntryPath = getMandatoryParameter(request, "item", false); // only single items are supported
        htmlResponse.setPath(combiningMessageEntryPath);
        ResourceResolver resourceResolver = request.getResourceResolver();
        Resource combiningMessageEntryResource = resourceResolver.getResource(combiningMessageEntryPath);
        if (combiningMessageEntryResource != null) {
            // javasecurity:S5145
            LOG.debug("Updating message entry for path '{}'...", combiningMessageEntryPath);
            updateCombiningMessageEntry(request, resourceResolver, combiningMessageEntryResource);
        } else {
            // javasecurity:S5145
            htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("No dictionaries to update exist for '%s'", combiningMessageEntryPath));
        }
    }

    private void updateCombiningMessageEntry(SlingHttpServletRequest request, ResourceResolver resourceResolver, Resource combiningMessageEntryResource) throws DictionaryException, PersistenceException, RepositoryException, ReplicationException {
        String dictionaryPath = combiningMessageEntryResource.getValueMap().get(CombiningMessageEntryResourceProvider.DICTIONARY_PATH, String.class);
        String key = combiningMessageEntryResource.getValueMap().get(CombiningMessageEntryResourceProvider.KEY, String.class);
        if (StringUtils.isNotBlank(dictionaryPath)) {
            // check languages
            Locale[] languages = combiningMessageEntryResource.getValueMap().get(CombiningMessageEntryResourceProvider.LANGUAGES, new Locale[0]);
            for (Locale language : languages) {
                Dictionary dictionary = dictionaryService.getDictionary(resourceResolver, dictionaryPath, language)
                        .orElseThrow(() -> new DictionaryException("Could not find dictionary for language '" + language + "' below path: " + dictionaryPath));
                String paramNameMessage = language.toLanguageTag();
                String message = getOptionalParameter(request, paramNameMessage, true).orElse("");
                boolean useEmpty = getOptionalParameter(request, paramNameMessage + CombiningMessageEntryDatasourceForDialog.SUFFIX_USE_EMPTY, false, Boolean::parseBoolean).orElse(false);
                final Optional<String> messageOptional;
                if (!useEmpty && message.isEmpty()) {
                    messageOptional = Optional.empty();
                } else {
                    messageOptional = Optional.of(message);
                }
                dictionary.updateEntry(resourceResolver, key, messageOptional);
            }
            resourceResolver.commit();
        } else {
            throw new DictionaryException("Could not find dictionary path in resource: " + combiningMessageEntryResource.getPath());
        }
    }

}
