package be.orbinson.aem.dictionarytranslator.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "aem-dictionary-translator/endpoints/export-translations",
        methods = "GET")
public class ExportTranslation extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ExportTranslation.class);

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String path = request.getParameter("resourcePath");

        response.setContentType("text/csv");
        response.setHeader("Content-disposition", "attachment; filename=\"dictionary.csv");

        try (PrintWriter writer = response.getWriter()) {
            ResourceResolver resolver = request.getResourceResolver();
            Resource resource = resolver.getResource(path);
            StringBuilder csvHeader = new StringBuilder("Labelname");
            List<Resource> languageResources = new ArrayList<>();
            if (resource != null) {
                Iterator<Resource> children = resource.listChildren();
                while (children.hasNext()) {
                    Resource child = children.next();
                    csvHeader.append(",");
                    csvHeader.append(child.getName());
                    languageResources.add(child);
                }
                writer.println(csvHeader.toString());
                LOG.error("CSV header: " + csvHeader.toString());

                if (!languageResources.isEmpty()) {
                    Resource firstLanguageResource = languageResources.get(0);
                    Iterator<Resource> labelChildren = firstLanguageResource.listChildren();
                    while (labelChildren.hasNext()) {
                        Resource labelResource = labelChildren.next();
                        StringBuilder csvRow = new StringBuilder(labelResource.getValueMap().get("sling:key", String.class));
                        for (Resource languageResource : languageResources) {
                            Resource correspondingLabelResource = languageResource.getChild(labelResource.getName());
                            csvRow.append(",");
                            String translation = correspondingLabelResource.getValueMap().get("sling:message", String.class);
                            csvRow.append(translation);
                        }
                        writer.println(csvRow.toString());
                        LOG.error("CSV row: " + csvRow.toString());
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Error while writing CSV file", e);
        }
    }
}
