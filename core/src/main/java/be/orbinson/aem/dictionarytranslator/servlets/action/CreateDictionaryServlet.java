package be.orbinson.aem.dictionarytranslator.servlets.action;

import java.util.Collection;
import java.util.Locale;

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrUtil;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/create-dictionary",
        methods = "POST"
)
public class CreateDictionaryServlet extends AbstractDictionaryServlet {

    public CreateDictionaryServlet() {
        super("Unable to create dictionary");
    }

    private static final Logger LOG = LoggerFactory.getLogger(CreateDictionaryServlet.class);

    @Reference
    private transient DictionaryService dictionaryService;

    @Override
    protected void internalDoPost(SlingHttpServletRequest request, HtmlResponse htmlResponse) throws Throwable {
        String path = getMandatoryParameter(request, "path", false);
        String name = getMandatoryParameter(request, "name", false);
        path = path + "/" + JcrUtil.escapeIllegalJcrChars(name);
        htmlResponse.setPath(path);
        htmlResponse.setCreateRequest(true);
        Collection<Locale> languages = getMandatoryParameters(request, "language", false, Locale::forLanguageTag);
        Collection<String> basenames = getOptionalParameters(request, "basename", true);

        final ResourceResolver resourceResolver = request.getResourceResolver();
        LOG.debug("Creating dictionary '{}'...", name);
        dictionaryService.createDictionaries(resourceResolver, path, languages, basenames);
        resourceResolver.commit();
    }

}
