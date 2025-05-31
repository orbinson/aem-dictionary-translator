package be.orbinson.aem.dictionarytranslator.servlets.action;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

import com.day.cq.commons.jcr.JcrUtil;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/create-dictionary",
        methods = "POST"
)
public class CreateDictionaryServlet extends AbstractDictionaryServlet {

    private static final Logger LOG = LoggerFactory.getLogger(CreateDictionaryServlet.class);

    @Reference
    private transient DictionaryService dictionaryService;

    @Override
    protected void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String path = getMandatoryParameter(request, "path", false);
        String name = getMandatoryParameter(request, "name", false);
        path = path + "/" + JcrUtil.escapeIllegalJcrChars(name);
        Collection<Locale> languages = getMandatoryParameters(request, "language", false, Locale::forLanguageTag);
        Collection<String> basenames = getOptionalParameters(request, "basename", true);

        final ResourceResolver resourceResolver = request.getResourceResolver();
        try {
            LOG.debug("Create dictionary '{}'", name);
            dictionaryService.createDictionaries(resourceResolver, path, languages, basenames);
            resourceResolver.commit();
        } catch (PersistenceException | DictionaryException e) {
            HtmlResponse htmlResponse = new HtmlResponse();
            htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Unable to create dictionary: %s", e.getMessage()));
            htmlResponse.send(response, true);
        }
    }
}
