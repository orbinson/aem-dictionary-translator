package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
import org.apache.tika.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static be.orbinson.aem.dictionarytranslator.servlets.action.ExportDictionaryServlet.KEY_HEADER;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/import-dictionary",
        methods = "POST"
)
public class ImportDictionaryServlet extends SlingAllMethodsServlet {

    @Reference
    private DictionaryService dictionaryService;

    @Override
    public void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String dictionaryPath = request.getParameter("dictionary");
        RequestParameter csvfile = request.getRequestParameter("csvfile");

        if (csvfile != null) {
            try {
                processCsvFile(request, dictionaryPath, csvfile.getInputStream());
            } catch (IOException | RepositoryException e) {
                HtmlResponse htmlResponse = new HtmlResponse();
                htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while importing CSV file: " + e.getMessage());
                htmlResponse.send(response, true);
            }
        }
    }

    private void processCsvFile(SlingHttpServletRequest request, String dictionaryPath, InputStream csvContent) throws IOException, RepositoryException {
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
            validateCsvHeaders(headers);
            headers.remove(KEY_HEADER);

            Resource dictionaryResource = resourceResolver.getResource(dictionaryPath);
            if (dictionaryResource != null) {
                List<String> knownLanguages = dictionaryService.getLanguages(dictionaryResource);
                initializeLanguageData(headers, languages, translations, knownLanguages);

                for (CSVRecord csvRecord : csvParser) {
                    processCsvRecord(dictionaryResource, languages, resourceResolver, keys, translations, csvRecord);
                }

                resourceResolver.commit();
            }
        }
    }

    private CSVFormat determineCsvFormat(String csvContent) throws IOException {
        CSVFormat.Builder builder = CSVFormat.Builder.create()
                .setSkipHeaderRecord(true);
        if (csvContent.contains(";")) {
            builder
                    .setDelimiter(';')
                    .setHeader(csvContent.split("\n")[0].split(";"));
        } else if (csvContent.contains(",")) {
            builder
                    .setDelimiter(',')
                    .setHeader(csvContent.split("\n")[0].split(","));
        } else {
            throw new IOException("Invalid CSV file. The Delimiter should be ',' or ';'.");
        }

        return builder.build();
    }

    private void validateCsvHeaders(Map<String, Integer> headers) throws IOException {
        if (!headers.containsKey(KEY_HEADER) || headers.get(KEY_HEADER) != 0) {
            throw new IOException(MessageFormat.format("Invalid CSV file. The first column must be {0}. The Delimiter should be ',' or ';'.", KEY_HEADER));
        }
    }

    private void initializeLanguageData(Map<String, Integer> headers, List<String> languages, List<List<String>> translations, List<String> knownLanguages) throws IOException {
        for (String language : headers.keySet()) {
            boolean hasMatch = true;
            for (String knownLanguage : knownLanguages) {
                hasMatch = false;
                if (knownLanguage.equals(language)) {
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

    private void processCsvRecord(Resource dictionaryResource, List<String> languages, ResourceResolver resourceResolver, List<String> keys, List<List<String>> translations, CSVRecord csvRecord) throws IOException, RepositoryException {
        if (csvRecord.size() != languages.size() + 1) {
            throw new IOException("Record has an incorrect number of translations: " + csvRecord);
        }

        String key = csvRecord.get(KEY_HEADER);
        keys.add(key);

        for (String language : languages) {
            int index = languages.indexOf(language);
            String translation = csvRecord.get(language);
            translations.get(index).add(translation);

            createOrUpdateMessageEntryResource(dictionaryResource, resourceResolver, language, key, translation);
        }
    }

    private void createOrUpdateMessageEntryResource(Resource dictionaryResource, ResourceResolver resourceResolver, String language, String key, String translation) throws PersistenceException, RepositoryException {
        Resource languageResource = dictionaryService.getLanguageResource(dictionaryResource, language);
        Resource messageEntryResource = dictionaryService.getMessageEntryResource(languageResource, key);
        if (messageEntryResource == null) {
            dictionaryService.createMessageEntry(resourceResolver, dictionaryResource, language, key, translation);
        } else {
            dictionaryService.updateMessageEntry(resourceResolver, dictionaryResource, language, key, translation);
        }
    }

}
