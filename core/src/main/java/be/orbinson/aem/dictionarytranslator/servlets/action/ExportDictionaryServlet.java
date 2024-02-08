package be.orbinson.aem.dictionarytranslator.servlets.action;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.Servlet;

import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_KEY;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGE;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/export-dictionary",
        methods = "POST")
public class ExportDictionaryServlet extends SlingAllMethodsServlet {

    public static final String KEY_HEADER = "KEY";
    private static final Logger LOG = LoggerFactory.getLogger(ExportDictionaryServlet.class);
    @Override
    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String dictionary = request.getParameter("dictionary");
        RequestParameter delimiter = request.getRequestParameter("delimiter");

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"dictionary_" + dictionary + ".csv");

        try (PrintWriter writer = response.getWriter()) {
            ResourceResolver resolver = request.getResourceResolver();
            Resource resource = resolver.getResource(dictionary);

            if (resource != null) {
                List<Resource> languageResources = collectLanguageResources(resource);
                writeCsvHeader(writer, delimiter, languageResources);

                if (!languageResources.isEmpty()) {
                    writeCsvRows(writer, delimiter, languageResources);
                }
            } else {
                handleResourceNotFound(response);
            }
        } catch (IOException e) {
            handleIOException(response, e);
        }
    }

    private List<Resource> collectLanguageResources(Resource resource) {
        Iterator<Resource> children = resource.listChildren();
        List<Resource> languageResources = new ArrayList<>();

        while (children.hasNext()) {
            Resource child = children.next();
            languageResources.add(child);
        }

        return languageResources;
    }

    private void writeCsvHeader(PrintWriter writer, RequestParameter delimiter, List<Resource> languageResources) {
        StringBuilder csvHeader = new StringBuilder(KEY_HEADER);

        for (Resource languageResource : languageResources) {
            csvHeader.append(delimiter);
            csvHeader.append(languageResource.getName());
        }

        writer.println(csvHeader);
        LOG.debug("CSV header: " + csvHeader);
    }

    private void writeCsvRows(PrintWriter writer, RequestParameter delimiter, List<Resource> languageResources) {
        List<String> labels = new ArrayList<>();
        Resource currentLanguageResource = null;
        if (!languageResources.isEmpty()) {
            for (int i = 0; i < languageResources.size(); i++) {
                currentLanguageResource = languageResources.get(i);
                Iterator<Resource> labelChildren = currentLanguageResource.listChildren();
                while (labelChildren.hasNext()) {
                    Resource labelResource = labelChildren.next();
                    if (!labels.contains(labelResource.getName())){
                        StringBuilder csvRow = buildCsvRow(labelResource, delimiter, languageResources);
                        writer.println(csvRow);
                        labels.add(labelResource.getName());
                        LOG.debug("CSV row: " + csvRow);
                    }
                }
            }
        }
    }

    private StringBuilder buildCsvRow(Resource labelResource, RequestParameter delimiter, List<Resource> languageResources) {
        StringBuilder csvRow;
        if (labelResource.getValueMap().containsKey(SLING_KEY)) {
            csvRow = new StringBuilder(labelResource.getValueMap().get(SLING_KEY, String.class));
        } else {
            csvRow = new StringBuilder(labelResource.getName());
        }

        for (Resource languageResource : languageResources) {
            Resource correspondingLabelResource = languageResource.getChild(labelResource.getName());
            csvRow.append(delimiter);
            String translation = " ";
            if (correspondingLabelResource != null) {
                translation = correspondingLabelResource.getValueMap().get(SLING_MESSAGE, String.class);
            }
            if (translation == null) {
                translation = " "; // This should be a space because appending an empty string will delete the whole string
            }
            csvRow.append(translation);
        }

        return csvRow;
    }

    private void handleResourceNotFound(SlingHttpServletResponse response) {
        String error = "Dictionary resource not found.";
        LOG.error(error);
        response.setStatus(404, error);
    }

    private void handleIOException(SlingHttpServletResponse response, IOException e) {
        String error = "Error while writing CSV file";
        LOG.error(error, e);
        response.setStatus(500, error);
    }
}


