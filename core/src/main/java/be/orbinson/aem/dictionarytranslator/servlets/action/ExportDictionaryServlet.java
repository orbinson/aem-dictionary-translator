package be.orbinson.aem.dictionarytranslator.servlets.action;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
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

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/export-dictionary",
        methods = "POST")
public class ExportDictionaryServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ExportDictionaryServlet.class);

    @Override
    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String dictionary = request.getParameter("dictionary");

        response.setContentType("text/csv");
        response.setHeader("Content-disposition", "attachment; filename=\"dictionary_" + dictionary + ".csv");

        try (PrintWriter writer = response.getWriter()) {
            ResourceResolver resolver = request.getResourceResolver();
            Resource resource = resolver.getResource(dictionary);
            StringBuilder csvHeader = new StringBuilder("Labelname");
            List<Resource> languageResources = new ArrayList<>();
            if (resource != null) {
                Iterator<Resource> children = resource.listChildren();
                while (children.hasNext()) {
                    Resource child = children.next();
                    csvHeader.append(";");
                    csvHeader.append(child.getName());
                    languageResources.add(child);
                }
                writer.println(csvHeader);
                LOG.info("CSV header: " + csvHeader);

                if (!languageResources.isEmpty()) {
                    Resource firstLanguageResource = languageResources.get(0);
                    Iterator<Resource> labelChildren = firstLanguageResource.listChildren();
                    while (labelChildren.hasNext()) {
                        Resource labelResource = labelChildren.next();
                        StringBuilder csvRow = new StringBuilder(labelResource.getValueMap().get("sling:key", String.class));
                        for (Resource languageResource : languageResources) {
                            Resource correspondingLabelResource = languageResource.getChild(labelResource.getName());
                            csvRow.append(";");
                            String translation = correspondingLabelResource.getValueMap().get("sling:message", String.class);
                            csvRow.append(translation);
                        }
                        writer.println(csvRow);
                        LOG.info("CSV row: " + csvRow);
                    }
                }
            }
        } catch (IOException e) {
            String error = "Error while writing CSV file";
            LOG.error(error, e);
            response.setStatus(400, error);
        }
    }
}
