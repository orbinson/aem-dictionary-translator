package be.orbinson.aem.dictionarytranslator.servlets.action;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
import org.apache.tika.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static be.orbinson.aem.dictionarytranslator.servlets.action.ExportDictionaryServlet.KEY_HEADER;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.*;
import static com.day.cq.commons.jcr.JcrConstants.*;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.NT_SLING_FOLDER;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

@Component(service = Servlet.class)
@SlingServletResourceTypes(resourceSuperType = "granite/ui/components/coral/foundation/form", resourceTypes = "aem-dictionary-translator/servlet/action/import-dictionary", methods = "POST")
public class ImportDictionaryServlet extends SlingAllMethodsServlet {

    @Override
    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String path = request.getParameter("dictionary");
        RequestParameter csvfile = request.getRequestParameter("csvfile");

        if (csvfile != null) {
            try {
                processCsvFile(request, response, path, csvfile.getInputStream());
            } catch (IOException e) {
                HtmlResponse htmlResponse = new HtmlResponse();
                htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while importing CSV file: " + e.getMessage());
                htmlResponse.send(response, true);
            }
        }
    }

    private void processCsvFile(SlingHttpServletRequest request, SlingHttpServletResponse response, String path, InputStream csvContent) throws IOException {
        List<String> languages = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        List<List<String>> translations = new ArrayList<>();

        ResourceResolver resourceResolver = request.getResourceResolver();

        String result = IOUtils.toString(csvContent, String.valueOf(StandardCharsets.UTF_8));

        CSVFormat format = determineCsvFormat(result);
        if (format == null) {
            return;
        }

        try (CSVParser csvParser = CSVParser.parse(result, format)) {
            Map<String, Integer> headers = csvParser.getHeaderMap();

            validateCsvHeaders(response, headers);

            headers.remove(KEY_HEADER);
            Resource dictionary = resourceResolver.getResource(path);
            if (dictionary != null) {
                List<Resource> knownLanguages = Lists.newArrayList(dictionary.listChildren());
                initializeLanguageData(headers, languages, translations, knownLanguages, response);

                for (CSVRecord record : csvParser) {
                    processCsvRecord(path, languages, resourceResolver, keys, translations, record);
                }

                resourceResolver.commit();
            }
        }
    }

    private CSVFormat determineCsvFormat(String csvContent) throws IOException {
        if (csvContent.contains(";")) {
            return CSVFormat.newFormat(';').withFirstRecordAsHeader();
        } else if (csvContent.contains(",")) {
            return CSVFormat.DEFAULT.withFirstRecordAsHeader();
        } else {
            throw new IOException("Invalid CSV file. The Delimiter should be ',' or ';'.");
        }
    }

    private void validateCsvHeaders(SlingHttpServletResponse response, Map<String, Integer> headers) throws IOException {
        if (!headers.containsKey(KEY_HEADER) || headers.get(KEY_HEADER) != 0) {
            throw new IOException(MessageFormat.format("Invalid CSV file. The first column must be {0}. The Delimiter should be ',' or ';'.", KEY_HEADER));
        }
    }

    private void initializeLanguageData(Map<String, Integer> headers, List<String> languages, List<List<String>> translations, List<Resource> knownLanguages, SlingHttpServletResponse response) throws IOException {
        for (String language : headers.keySet()) {
            boolean hasMatch = true;
            for (Resource knownLanguage : knownLanguages) {
                hasMatch = false;
                if (knownLanguage.getName().equals(language)) {
                    languages.add(language);
                    translations.add(new ArrayList<>());
                    hasMatch = true;
                    break;
                }
            }
            if (!hasMatch) {
                throw new IOException("Incorrect CSV file, please only add languages that exist in the dictionary");
            }
        }
    }

    private void processCsvRecord(String path, List<String> languages, ResourceResolver resourceResolver, List<String> keys, List<List<String>> translations, CSVRecord record) throws IOException {
        if (record.size() != languages.size() + 1) {
            throw new IOException("Record has an incorrect number of translations: " + record);
        }

        String label = record.get(KEY_HEADER);
        keys.add(label);

        for (String language : languages) {
            int index = languages.indexOf(language);
            String translation = record.get(language);
            translations.get(index).add(translation);

            createOrUpdateLabelResource(path, resourceResolver, language, label, translation);
        }
    }

    private void createOrUpdateLabelResource(String path, ResourceResolver resourceResolver, String language, String label, String translation) throws PersistenceException {
        Resource languageResource = getLanguageResource(path, resourceResolver, language);

        Resource labelResource = resourceResolver.getResource(path + "/" + language + "/" + label);
        if (labelResource == null) {
            createLabelResource(resourceResolver, languageResource, label, translation);
        } else {
            updateLabelResourceProperties(labelResource, label, translation);
        }
    }

    private static @NotNull Resource getLanguageResource(String path, ResourceResolver resourceResolver, String language) throws PersistenceException {
        return ResourceUtil.getOrCreateResource(resourceResolver, path + "/" + language, Map.of(JCR_PRIMARYTYPE, NT_SLING_FOLDER, JCR_LANGUAGE, language, JCR_BASENAME, language, SLING_RESOURCE_TYPE_PROPERTY, NT_SLING_FOLDER, JCR_MIXINTYPES, new String[]{MIX_LANGUAGE}), null, false);
    }


    private Resource createLabelResource(ResourceResolver resourceResolver, Resource languageResource, String newNodeName, String translation) throws PersistenceException {
        Map<String, Object> properties = new HashMap<>();
        properties.put(JCR_PRIMARYTYPE, SLING_MESSAGEENTRY);
        properties.put(SLING_KEY, newNodeName);
        if (!translation.isBlank()) {
            properties.put(SLING_MESSAGE, translation);
        }
        return resourceResolver.create(languageResource, newNodeName, properties);
    }

    private void updateLabelResourceProperties(Resource labelResource, String label, String translation) {
        ModifiableValueMap modifiableValueMap = labelResource.adaptTo(ModifiableValueMap.class);
        if (modifiableValueMap != null) {
            modifiableValueMap.put(JCR_PRIMARYTYPE, SLING_MESSAGEENTRY);
            modifiableValueMap.put(SLING_KEY, label);
            if (!translation.isBlank()) {
                modifiableValueMap.put(SLING_MESSAGE, translation);
            }
        }
    }

}
