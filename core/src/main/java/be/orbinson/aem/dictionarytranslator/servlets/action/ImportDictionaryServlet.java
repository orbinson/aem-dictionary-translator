package be.orbinson.aem.dictionarytranslator.servlets.action;

import static be.orbinson.aem.dictionarytranslator.servlets.action.ExportDictionaryServlet.KEY_HEADER;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
import org.apache.tika.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;

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
            } catch (IOException | RepositoryException | DictionaryException e) {
                HtmlResponse htmlResponse = new HtmlResponse();
                htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while importing CSV file: " + e.getMessage());
                htmlResponse.send(response, true);
            }
        }
    }

    private void processCsvFile(SlingHttpServletRequest request, String dictionaryPath, InputStream csvContent) throws IOException, RepositoryException, DictionaryException {
        List<String> languages = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        List<List<String>> translations = new ArrayList<>();

        ResourceResolver resourceResolver = request.getResourceResolver();

        List<String> lines = IOUtils.readLines(csvContent, String.valueOf(StandardCharsets.UTF_8));
        String header = !lines.isEmpty() ? lines.get(0) : StringUtils.EMPTY;
        String result = String.join("\n", lines);

        CSVFormat format = determineCsvFormat(header);
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
                    processCsvRecord(dictionaryResource, languages, keys, translations, csvRecord);
                }

                resourceResolver.commit();
            }
        }
    }

    private CSVFormat determineCsvFormat(String csvHeader) throws IOException {
        CSVFormat.Builder builder = CSVFormat.Builder.create()
                .setSkipHeaderRecord(true);
        if (csvHeader.contains(";")) {
            builder
                    .setDelimiter(';')
                    .setHeader(csvHeader.split("\n")[0].split(";"));
        } else if (csvHeader.contains(",")) {
            builder
                    .setDelimiter(',')
                    .setHeader(csvHeader.split("\n")[0].split(","));
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

    private void processCsvRecord(Resource dictionaryResource, List<String> languages, List<String> keys, List<List<String>> translations, CSVRecord csvRecord) throws IOException, RepositoryException, DictionaryException {
        if (csvRecord.size() != languages.size() + 1) {
            throw new IOException("Record has an incorrect number of translations: " + csvRecord);
        }

        String key = csvRecord.get(KEY_HEADER);
        keys.add(key);

        for (String language : languages) {
            try {
                if (dictionaryService.getType(dictionaryResource, language) != DictionaryService.DictionaryType.SLING_MESSAGE_ENTRY) {
                    throw new IOException("Can only import CSV files for dictionaries of type SLING_MESSAGE_ENTRY");
                }
            } catch (DictionaryException e) {
                throw new IOException("Could not determine type of language '" + language + "' from dictionary '" + dictionaryResource.getPath() + "'", e);
            }
            int index = languages.indexOf(language);
            String translation = csvRecord.get(language);
            translations.get(index).add(translation);

            dictionaryService.createOrUpdateMessageEntry(dictionaryResource, language, key, translation);
        }
    }

}
