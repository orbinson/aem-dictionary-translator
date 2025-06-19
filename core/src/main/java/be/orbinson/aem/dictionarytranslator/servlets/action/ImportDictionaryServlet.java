package be.orbinson.aem.dictionarytranslator.servlets.action;

import static be.orbinson.aem.dictionarytranslator.servlets.action.ExportDictionaryServlet.KEY_HEADER;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
import org.apache.tika.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.Dictionary;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryImpl;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/import-dictionary",
        methods = "POST"
)
public class ImportDictionaryServlet extends AbstractDictionaryServlet {

    @Reference
    private DictionaryService dictionaryService;

    @Reference
    private Replicator replicator;

    @Override
    public void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String dictionaryPath = getMandatoryParameter(request, "dictionary", false);
        RequestParameter csvfile = request.getRequestParameter("csvfile");

        if (csvfile != null) {
            try {
                processCsvFile(request, dictionaryPath, csvfile.getInputStream());
            } catch (IOException | RepositoryException | DictionaryException | ReplicationException e) {
                HtmlResponse htmlResponse = new HtmlResponse();
                htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while importing CSV file: " + e.getMessage());
                htmlResponse.send(response, true);
            }
        }
    }

    private void processCsvFile(SlingHttpServletRequest request, String dictionaryPath, InputStream csvContent) throws IOException, RepositoryException, DictionaryException, ReplicationException {
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

            Map<Locale, Dictionary> dictionaries = dictionaryService.getDictionariesByLanguage(resourceResolver, dictionaryPath);
            if (!dictionaries.isEmpty()) {
                List<Locale> knownLanguages = dictionaries.keySet()
                        .stream()
                        .collect(Collectors.toList());
                Map<Locale, String> localeToHeaderMap = getLocalesToCsvHeadersMap(headers.keySet(), knownLanguages);

                for (CSVRecord csvRecord : csvParser) {
                    processCsvRecord(resourceResolver, dictionaries, localeToHeaderMap, csvRecord);
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

    private Map<Locale, String> getLocalesToCsvHeadersMap(Collection<String> columnHeaders, List<Locale> knownLanguages) throws IOException {
        Map<Locale, String> map = new HashMap<>();
        for (String language : columnHeaders) {
            Locale languageInCsv = DictionaryImpl.toLocale(language);
            if (knownLanguages.contains(languageInCsv)) {
                map.put(languageInCsv, language);
            } else {
                throw new IOException("Incorrect CSV file, please only add languages that already have existing dictionaries. No dictionary found for language: " + language);
            }
        }
        return map;
    }

    private void processCsvRecord(ResourceResolver resourceResolver, Map<Locale, Dictionary> dictionaries, Map<Locale, String> localeToHeaderMap, CSVRecord csvRecord) throws IOException, RepositoryException, DictionaryException, ReplicationException {
        if (csvRecord.size() != localeToHeaderMap.size() + 1) {
            throw new IOException("Record has an incorrect number of translations: " + csvRecord);
        }

        String key = csvRecord.get(KEY_HEADER);

        for (Map.Entry<Locale, String> localeWithHeader : localeToHeaderMap.entrySet()) {
            Dictionary dictionary = dictionaries.get(localeWithHeader.getKey());
            if (dictionary == null) {
                throw new IOException("No dictionary found for language: " + localeWithHeader.getValue());
            }
            if (!dictionary.isEditable(resourceResolver)) {
                throw new IOException("Dictionary for language '" + localeWithHeader.getValue() + "' is not editable: " + dictionary.getPath());
            }
            String message = csvRecord.get(localeWithHeader.getValue());
            final Optional<String> messageOptional;
            if (message.isEmpty()) {
                messageOptional = Optional.empty();
            } else {
                if (message.equals(ExportDictionaryServlet.VALUE_EMPTY)) {
                    message = ""; // convert the special empty value back to an empty string
                }
                messageOptional = Optional.of(message);
            }
            dictionary.updateEntry(resourceResolver, key, messageOptional);
        }
    }

}
