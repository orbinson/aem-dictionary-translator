package be.orbinson.aem.dictionarytranslator.servlets.action;

import java.util.Collection;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.services.Dictionary;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/delete-message-entry",
        methods = "POST"
)
public class DeleteMessageEntryServlet extends AbstractDictionaryServlet {
    public DeleteMessageEntryServlet() {
        super("Unable to delete message entry");
    }

    private static final Logger LOG = LoggerFactory.getLogger(DeleteMessageEntryServlet.class);
    public static final String ITEM_PARAM = "item";

    @Reference
    private transient DictionaryService dictionaryService;

    @Reference
    private Replicator replicator;
    
    @Override
    protected void internalDoPost(SlingHttpServletRequest request, HtmlResponse htmlResponse) throws Throwable {
        Collection<String> combiningMessageEntryPaths  = getMandatoryParameters(request, ITEM_PARAM, false);
        for (String combiningMessageEntryPath : combiningMessageEntryPaths) {
            htmlResponse.setPath(combiningMessageEntryPath);
            ResourceResolver resourceResolver = request.getResourceResolver();
            Resource combiningMessageEntryResource = resourceResolver.getResource(combiningMessageEntryPath);
            if (combiningMessageEntryResource == null) {
                htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Unable to get message entry '%s'", combiningMessageEntryPath));
                return;
            }
            try {
                ValueMap properties = combiningMessageEntryResource.getValueMap();
                String key = properties.get(CombiningMessageEntryResourceProvider.KEY, String.class);
                if (key == null) {
                    throw new IllegalArgumentException("" + CombiningMessageEntryResourceProvider.KEY + " is required");
                }
                String dictionaryPath = properties.get(CombiningMessageEntryResourceProvider.DICTIONARY_PATH, String.class);
                if (dictionaryPath == null) {
                    throw new IllegalArgumentException("" + CombiningMessageEntryResourceProvider.DICTIONARY_PATH + " is required");
                }
                for (Dictionary dictionary : dictionaryService.getDictionaries(resourceResolver, dictionaryPath)) {
                    if (dictionary.getEntries().containsKey(key)) {
                        LOG.debug("Deleting message entry for key '{}' in dictionary '{}'...", key, dictionary.getPath());
                        dictionary.deleteEntry(replicator, resourceResolver, key);
                    } else {
                        LOG.debug("No message entry found for key '{}' in dictionary '{}'", key, dictionary.getPath());
                    }
                }
                resourceResolver.commit();
                // javasecurity:S5145
                LOG.debug("Deleted message entry for key '{}' from all dictionaries below '{}'", key, dictionaryPath);
            
            } catch (IllegalArgumentException e) {
                htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Missing mandatory property from resource: %s", combiningMessageEntryPath, e.getMessage()));
            } 
        }
    }

}
