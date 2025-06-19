package be.orbinson.aem.dictionarytranslator.servlets.action;

import java.util.Collection;

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
        resourceTypes = "aem-dictionary-translator/servlet/action/delete-dictionary",
        methods = "POST"
)
public class DeleteDictionaryServlet extends AbstractDictionaryServlet {

    public DeleteDictionaryServlet() {
        super("Unable to delete dictionaries");
    }

    public static final String DICTIONARIES_PARAM = "item";

    private static final Logger LOG = LoggerFactory.getLogger(DeleteDictionaryServlet.class);

    @Reference
    private transient DictionaryService dictionaryService;

    @Reference
    private Replicator replicator;

    @Override
    protected void internalDoPost(SlingHttpServletRequest request, HtmlResponse htmlResponse) throws Throwable {
        Collection<String> dictionaries = getMandatoryParameters(request, DICTIONARIES_PARAM, false);
        final ResourceResolver resourceResolver = request.getResourceResolver();
        try {
            for (String dictionaryPath : dictionaries) {
                htmlResponse.setPath(dictionaryPath);
                LOG.debug("Delete dictionary at path '{}'", dictionaryPath);
                dictionaryService.deleteDictionaries(replicator, resourceResolver, dictionaryPath);
            }
        } catch (DictionaryException e) {
            htmlResponse.setStatus(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            return;
        }
        resourceResolver.commit();
    }

}
