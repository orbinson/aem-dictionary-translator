package be.orbinson.aem.dictionarytranslator.servlets.action;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVFormat.Builder;
import org.apache.commons.csv.CSVPrinter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.Dictionary;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/export-dictionary",
        methods = "POST")
public class ExportDictionaryServlet extends AbstractDictionaryServlet {

    public static final String KEY_HEADER = "KEY";

    private static final Logger LOG = LoggerFactory.getLogger(ExportDictionaryServlet.class);

    @Reference
    private DictionaryService dictionaryService;

    @Override
    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String dictionaryPath = getMandatoryParameter(request, "dictionary", false);
        Optional<String> delimiter = getOptionalParameter(request, "delimiter", false);

        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"dictionary_" + dictionaryPath + ".csv");

        ResourceResolver resolver = request.getResourceResolver();
        try {
            Collection<Dictionary> dictionaries = dictionaryService.getDictionaries(resolver, dictionaryPath);
            if (dictionaries.isEmpty()) {
                HtmlResponse htmlResponse = new HtmlResponse();
                htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, "No dictionaries found below path: " + dictionaryPath);
                htmlResponse.send(response, true);
                return;
            }
            try (CSVPrinter printer = createCsvPrinter(response.getWriter(), delimiter, dictionaries.stream().map(Dictionary::getLanguage).collect(Collectors.toList()))) {
                Collection<String> keys = dictionaries.stream().flatMap(d -> {
                    try {
                        return d.getEntries().keySet().stream();
                    } catch (DictionaryException e) {
                        LOG.warn("Error while retrieving keys from dictionary: " + d.getPath(), e);
                        return Stream.empty();
                    }
                }).distinct().sorted().collect(Collectors.toList());
                for (String key : keys) {
                    writeCsvRow(printer, dictionaries, key);
                }
            }
        } catch (IOException | DictionaryException e) {
            HtmlResponse htmlResponse = new HtmlResponse();
            htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while writing CSV file: " + e.getMessage());
            htmlResponse.send(response, true);
        }
    }


    private CSVPrinter createCsvPrinter(PrintWriter writer, Optional<String> delimiter, List<Locale> languages) throws IOException {
        List<String> csvHeader = new LinkedList<>();
        csvHeader.add(KEY_HEADER);

        for (Locale language : languages) {
            csvHeader.add(language.toLanguageTag());
        }

        Builder builder = CSVFormat.Builder.create(CSVFormat.DEFAULT).setHeader(csvHeader.toArray(new String[0]));
        if (delimiter.isPresent()) {
            builder.setDelimiter(delimiter.get());
        }
        return builder.build().print(writer);
    }

    private void writeCsvRow(CSVPrinter csvPrinter, Collection<Dictionary> dictionaries, String key) throws DictionaryException, IOException {
        // use the combined message entries from all dictionaries
        Collection<String> columns = new LinkedList<>();
        columns.add(key);
        dictionaries.stream()
            .map(dict -> {
                try {
                    return dict.getEntries().get(key);
                } catch (DictionaryException e) {
                    LOG.debug("Error while retrieving entries from dictionary: " + dict.getPath(), e);
                    return null;
                }
            })
            .map(entry -> entry != null ? entry.getText() : " ")
            .forEach(columns::add);
        csvPrinter.printRecord(columns);
    }

}


