package be.orbinson.aem.dictionarytranslator.servlets;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "aem-dictionary-translator/endpoints/import-translations",
        methods = "POST")
public class ImportTranslation extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ImportTranslation.class);

    @Override
    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String path = request.getParameter("resourcePath");
        RequestParameter csvfile = request.getRequestParameter("csvfile");

        if (csvfile != null) {
            InputStream csvContent = csvfile.getInputStream();

            List<String> languages = new ArrayList<>();
            List<String> labelNames = new ArrayList<>();
            List<List<String>> translations = new ArrayList<>();

            ResourceResolver resourceResolver = request.getResourceResolver();

            try (InputStreamReader reader = new InputStreamReader(csvContent);
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

                Map<String, Integer> headers = csvParser.getHeaderMap();

                if (!headers.containsKey("Labelname") || headers.get("Labelname") != 0) {
                    LOG.error("Invalid CSV file. The first column must be 'Labelname'");
                    return;
                }

                headers.remove("Labelname");
                for (String language : headers.keySet()) {
                    languages.add(language);
                    translations.add(new ArrayList<>());
                }

                for (CSVRecord record : csvParser) {
                    if (record.size() != languages.size() + 1) {  // +1 for the 'Labelname' column.
                        LOG.warn("Ignoring row with incorrect number of translations: " + record);
                        continue;
                    }

                    String label = record.get("Labelname");
                    labelNames.add(label);

                    for (String language : languages) {
                        int index = languages.indexOf(language);
                        String translation = record.get(language);
                        translations.get(index).add(translation);

                        Resource languageResource = resourceResolver.getResource(path + "/" + language);
                        if (languageResource == null) {
                            Map<String, Object> newFolderProperties = new HashMap<>();
                            newFolderProperties.put("jcr:primaryType", "sling:Folder");
                            newFolderProperties.put("jcr:language", language);
                            newFolderProperties.put("jcr:basename", language);
                            newFolderProperties.put("sling:resourceType", "sling:Folder");
                            newFolderProperties.put("jcr:mixinTypes", new String[]{"mix:language"});
                            languageResource = resourceResolver.create(resourceResolver.getResource(path), language, newFolderProperties);
                        }

                        Map<String, Object> newNodeProperties = new HashMap<>();
                        newNodeProperties.put("jcr:primaryType", "nt:unstructured");
                        String newNodeName = label;
                        Resource newResource = resourceResolver.create(languageResource, newNodeName, newNodeProperties);
                        ModifiableValueMap mvm = newResource.adaptTo(ModifiableValueMap.class);
                        if (mvm != null) {
                            mvm.put("jcr:primaryType", "sling:MessageEntry");
                            mvm.put("sling:key", label);
                            mvm.put("sling:message", translation);
                        }
                    }
                }
                resourceResolver.commit();
            } catch (IOException e) {
                LOG.error("Error while parsing CSV file or creating nodes", e);
            }
        }
    }
}
