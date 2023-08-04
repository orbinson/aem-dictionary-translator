package be.orbinson.aem.dictionarytranslator.servlets;

import com.day.cq.commons.jcr.JcrConstants;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "dictionary-translator/endpoints/copy-language",
        methods = "POST")
public class CopyLanguage extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(CopyLanguage.class);
    private static final Collection<String> PARAMETERS = List.of("originalLanguage", "languageCode", "path");

    @Override
    public void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws IOException {
        final Map<String, String> parameterMap = getFirstValuesOfRequestParameters(request);

        // Check if all required request parameters are present
        if (!parameterMap.keySet().containsAll(PARAMETERS)) {
            final String missingParameters = PARAMETERS.stream()
                    .filter(parameter -> !parameterMap.containsKey(parameter))
                    .collect(Collectors.joining());
            LOG.warn("Missing parameters {}", missingParameters);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter(s) " + missingParameters);
            return;
        }

        // Define request parameters
        final String originalLanguage = parameterMap.get("originalLanguage");
        final String newLanguageCode = parameterMap.get("languageCode");
        final String parentPath = parameterMap.get("path");

        // Get parent and original language resource
        final ResourceResolver resourceResolver = request.getResourceResolver();
        final Resource parentResource = resourceResolver.getResource(parentPath);
        final Resource originalLanguageResource = resourceResolver.getResource(parentPath + "/" + originalLanguage);

        // Check if resources are not null
        if (parentResource == null || originalLanguageResource == null) {
            LOG.warn("Unable to find original language at {}/{}", parentPath, originalLanguage);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Original language can't be found.");
            return;
        }


        try {
            createNewLanguageResource(newLanguageCode, resourceResolver, parentResource, originalLanguageResource.getValueMap().get("sling:basename", String.class));

            // Copy all translated labels from the original language to the new language
            for (final Resource childResource : originalLanguageResource.getChildren()) {
                resourceResolver.copy(childResource.getPath(), parentPath + "/" + newLanguageCode);
                LOG.info("Successfully copied language {} to {} in dictionary {}", originalLanguage, newLanguageCode, parentPath);
            }
            resourceResolver.commit();
        } catch (final PersistenceException e) {
            final String errorMessage = "Unable to copy language form " + parentPath + "/" + originalLanguage + " to " + parentPath + "/" + newLanguageCode;
            LOG.error(errorMessage, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void createNewLanguageResource(final String newLanguageCode, final ResourceResolver resourceResolver, final Resource parentResource, final String basename) throws PersistenceException {
        final Map<String, Object> properties = new HashMap<>();
        properties.put(JcrConstants.JCR_LANGUAGE, newLanguageCode);
        properties.put(JcrConstants.JCR_MIXINTYPES, "mix:language");
        properties.put(JcrConstants.JCR_PRIMARYTYPE, JcrResourceConstants.NT_SLING_FOLDER);
        if (StringUtils.isNotEmpty(basename)) {
            properties.put("sling:basename", basename);
        }
        resourceResolver.create(parentResource, newLanguageCode, properties);
    }

    private Map<String, String> getFirstValuesOfRequestParameters(final SlingHttpServletRequest request) {
        return request.getParameterMap()
                .entrySet()
                .stream()
                .filter(entry -> ArrayUtils.isNotEmpty(entry.getValue()))
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()[0]))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
