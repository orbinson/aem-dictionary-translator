package be.orbinson.aem.dictionarytranslator.servlets.action;

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
import org.apache.tika.io.IOUtils;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/import-dictionary",
        methods = "POST")
public class ImportDictionaryServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ImportDictionaryServlet.class);

    @Override
    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String path = request.getParameter("dictionary");
        RequestParameter csvfile = request.getRequestParameter("./csvfile");

        if (csvfile != null) {
            InputStream csvContent = csvfile.getInputStream();

            List<String> languages = new ArrayList<>();
            List<String> labelNames = new ArrayList<>();
            List<List<String>> translations = new ArrayList<>();

            ResourceResolver resourceResolver = request.getResourceResolver();

            try (InputStreamReader reader = new InputStreamReader(csvContent)) {
                String result = IOUtils.toString(csvContent, String.valueOf(StandardCharsets.UTF_8));
                csvContent.reset();
                CSVFormat format = null;
                if (result.contains(";")) {
                    format = CSVFormat.newFormat(';').withFirstRecordAsHeader();
                } else {
                    format = CSVFormat.DEFAULT.withFirstRecordAsHeader();
                }
                CSVParser csvParser = new CSVParser(reader, format);
                Map<String, Integer> headers = csvParser.getHeaderMap();

                if (!headers.containsKey("Labelname") || headers.get("Labelname") != 0) {
                    String error = "Invalid CSV file. The first column must be 'Labelname'. The Delimiter should be ',' or ';'.";
                    LOG.error(error);
                    response.setStatus(400, error);
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
                        Resource labelResource = resourceResolver.getResource(path + "/" + language + "/" + newNodeName);
                        if (labelResource == null) {
                            labelResource = resourceResolver.create(languageResource, newNodeName, newNodeProperties);
                        }
                        ModifiableValueMap mvm = labelResource.adaptTo(ModifiableValueMap.class);
                        if (mvm != null) {
                            mvm.put("jcr:primaryType", "sling:MessageEntry");
                            mvm.put("sling:key", label);
                            mvm.put("sling:message", translation);
                        }
                    }
                }
                resourceResolver.commit();
            } catch (IOException e) {
                String error = "Error while parsing CSV file or creating nodes";
                LOG.error(error, e);
                response.setStatus(400, "Error while parsing CSV file or creating nodes");
            }
        }
    }
}