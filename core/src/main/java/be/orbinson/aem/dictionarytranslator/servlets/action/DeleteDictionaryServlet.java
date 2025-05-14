package be.orbinson.aem.dictionarytranslator.servlets.action;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

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

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/delete-dictionary",
        methods = "POST"
)
public class DeleteDictionaryServlet extends SlingAllMethodsServlet {

    public static final String DICTIONARIES_PARAM = "item";

    @Reference
    private transient DictionaryService dictionaryService;

    @Override
    protected void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String[] dictionaries = request.getParameterValues(DICTIONARIES_PARAM);

        if (dictionaries == null) {
            HtmlResponse htmlResponse = new HtmlResponse();
            htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, "Dictionaries parameters are required");
            htmlResponse.send(response, true);
        } else {
            final ResourceResolver resourceResolver = request.getResourceResolver();
            for (String dictionaryPath : dictionaries) {
                try {
                    dictionaryService.deleteDictionary(resourceResolver, dictionaryPath);
                } catch (DictionaryException | ReplicationException | PersistenceException e) {
                    HtmlResponse htmlResponse = new HtmlResponse();
                    htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Unable to delete dictionary '%s': %s", dictionaryPath, e.getMessage()));
                    htmlResponse.send(response, true);
                }
            }
        }
    }

}
