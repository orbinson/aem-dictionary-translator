package be.orbinson.aem.dictionarytranslator.servlets.action;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;

import static be.orbinson.aem.dictionarytranslator.servlets.action.ExportDictionaryServlet.KEY_HEADER;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.JCR_BASENAME;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.MIX_LANGUAGE;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGEENTRY;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGE;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_KEY;
import static com.day.cq.commons.jcr.JcrConstants.JCR_LANGUAGE;
import static com.day.cq.commons.jcr.JcrConstants.JCR_MIXINTYPES;
import static com.day.cq.commons.jcr.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.NT_SLING_FOLDER;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

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
        RequestParameter csvfile = request.getRequestParameter("csvfile");

        if (csvfile != null) {
            try {
                processCsvFile(request, response, path, csvfile.getInputStream());
            } catch (IOException e) {
                handleCsvProcessingError(response, e);
            }
        }
    }

    private void processCsvFile(SlingHttpServletRequest request, SlingHttpServletResponse response, String path, InputStream csvContent) throws IOException {
        List<String> languages = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        List<List<String>> translations = new ArrayList<>();

        ResourceResolver resourceResolver = request.getResourceResolver();

        try (InputStreamReader reader = new InputStreamReader(csvContent)) {
            String result = IOUtils.toString(csvContent, String.valueOf(StandardCharsets.UTF_8));
            csvContent.reset();

            CSVFormat format = determineCsvFormat(result, response);
            if (format == null) {
                return;
            }

            CSVParser csvParser = new CSVParser(reader, format);
            Map<String, Integer> headers = csvParser.getHeaderMap();

            validateCsvHeaders(response, headers);

            headers.remove(KEY_HEADER);
            Resource dictionary = resourceResolver.getResource(path);
            List<Resource> knownLanguages = Lists.newArrayList(dictionary.listChildren());
            initializeLanguageData(headers, languages, translations, knownLanguages, response);

            for (CSVRecord record : csvParser) {
                processCsvRecord(path, languages, resourceResolver, keys, translations, record);
            }

            resourceResolver.commit();
        } catch (IOException e) {
            handleCsvProcessingError(response, e);
        }
    }

    private CSVFormat determineCsvFormat(String csvContent, SlingHttpServletResponse response) {
        if (csvContent.contains(";")) {
            return CSVFormat.newFormat(';').withFirstRecordAsHeader();
        } else if (csvContent.contains(",")) {
            return CSVFormat.DEFAULT.withFirstRecordAsHeader();
        } else {
            String error = "Invalid CSV file. The Delimiter should be ',' or ';'.";
            LOG.error(error);
            response.setStatus(400, error);
            return null;
        }
    }

    private void validateCsvHeaders(SlingHttpServletResponse response, Map<String, Integer> headers) {
        if (!headers.containsKey(KEY_HEADER) || headers.get(KEY_HEADER) != 0) {
            String error = MessageFormat.format("Invalid CSV file. The first column must be {0}. The Delimiter should be ',' or ';'.", KEY_HEADER);
            LOG.error(error);
            response.setStatus(400, error);
        }
    }

    private void initializeLanguageData(Map<String, Integer> headers, List<String> languages, List<List<String>> translations, List<Resource> knownLanguages, SlingHttpServletResponse response) {
        for (String language : headers.keySet()) {
            boolean hasMatch = true;
            for (Resource knownLanguage : knownLanguages){
             hasMatch = false;
                if (knownLanguage.getName().equals(language)){
                    languages.add(language);
                    translations.add(new ArrayList<>());
                    hasMatch = true;
                    break;
                }
            }
            if (!hasMatch) {
                String error = "Incorrect CSV file, please only add languages that exist in the dictionary";
                LOG.warn(error);
                response.setStatus(400, error);
            }
        }
    }

    private void processCsvRecord(String path, List<String> languages, ResourceResolver resourceResolver, List<String> keys, List<List<String>> translations, CSVRecord record) throws PersistenceException {
        if (record.size() != languages.size() + 1) {
            LOG.warn("Ignoring row with incorrect number of translations: " + record);
            return;
        }

        String label = record.get(KEY_HEADER);
        keys.add(label);

        for (String language : languages) {
            int index = languages.indexOf(language);
            String translation = record.get(language);
            translations.get(index).add(translation);

            createOrUpdateResource(path, resourceResolver, language, label, translation);
        }
    }

    private void createOrUpdateResource(String path, ResourceResolver resourceResolver, String language, String label, String translation) throws PersistenceException {
        Resource languageResource = resourceResolver.getResource(path + "/" + language);
        if (languageResource == null) {
            languageResource = createLanguageResource(resourceResolver, path, language);
        }
        Resource labelResource = resourceResolver.getResource(path + "/" + language + "/" + label);
        if (labelResource == null) {
            createLabelResource(resourceResolver, languageResource, label, translation);
        } else {
            updateLabelResourceProperties(labelResource, label, translation);
        }
    }

    private Resource createLanguageResource(ResourceResolver resourceResolver, String path, String language) throws PersistenceException {
        return resourceResolver.create(resourceResolver.getResource(path), language, Map.of(
                JCR_PRIMARYTYPE, NT_SLING_FOLDER,
                JCR_LANGUAGE, language,
                JCR_BASENAME, language,
                SLING_RESOURCE_TYPE_PROPERTY, NT_SLING_FOLDER,
                JCR_MIXINTYPES, new String[]{MIX_LANGUAGE}
        ));
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

    private void handleCsvProcessingError(SlingHttpServletResponse response, IOException e) {
        String error = "Error while parsing CSV file or creating nodes";
        LOG.error(error, e);
        response.setStatus(500, error);
    }
}
