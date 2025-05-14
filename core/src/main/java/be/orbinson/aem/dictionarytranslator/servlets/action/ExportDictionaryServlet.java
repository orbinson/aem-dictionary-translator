package be.orbinson.aem.dictionarytranslator.servlets.action;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;

// TODO should add quoting / escaping for CSV's
@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/export-dictionary",
        methods = "POST")
public class ExportDictionaryServlet extends SlingAllMethodsServlet {

    public static final String KEY_HEADER = "KEY";

    private static final Logger LOG = LoggerFactory.getLogger(ExportDictionaryServlet.class);

    @Reference
    private DictionaryService dictionaryService;

    @Override
    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String dictionaryPath = request.getParameter("dictionary");
        RequestParameter delimiter = request.getRequestParameter("delimiter");

        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"dictionary_" + dictionaryPath + ".csv");

        try (PrintWriter writer = response.getWriter()) {
            ResourceResolver resolver = request.getResourceResolver();
            Resource dictionaryResource = resolver.getResource(dictionaryPath);

            if (dictionaryResource != null) {
                List<String> languages = dictionaryService.getLanguages(dictionaryResource);
                writeCsvHeader(writer, delimiter, languages);
                writeCsvRows(writer, delimiter, dictionaryResource, languages);
            } else {
                HtmlResponse htmlResponse = new HtmlResponse();
                htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, "Dictionary resource not found.");
                htmlResponse.send(response, true);
            }
        } catch (IOException | DictionaryException e) {
            HtmlResponse htmlResponse = new HtmlResponse();
            htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while writing CSV file: " + e.getMessage());
            htmlResponse.send(response, true);
        }
    }


    private void writeCsvHeader(PrintWriter writer, RequestParameter delimiter, List<String> languages) {
        StringBuilder csvHeader = new StringBuilder(KEY_HEADER);

        for (String language : languages) {
            csvHeader.append(delimiter);
            csvHeader.append(language);
        }

        writer.println(csvHeader);
        LOG.debug("CSV header: {}", csvHeader);
    }

    private void writeCsvRows(PrintWriter writer, RequestParameter delimiter, Resource dictionaryResource, List<String> languages) throws DictionaryException {
        List<String> keys = dictionaryService.getKeys(dictionaryResource);
        Map<String, Map<String, String>> messageMapPerKey = new HashMap<>(); // the key is the message key
        for (String language : languages) {
            dictionaryService.getMessages(dictionaryResource, language).entrySet().stream()
                    .forEach(entry -> {
                        String key = entry.getKey();
                        String message = entry.getValue().getText();
                        messageMapPerKey.computeIfAbsent(key, k -> new HashMap<>()).put(language, message);
                    });
        }
        for (String key : keys) {
            StringBuilder csvRow = new StringBuilder();
            csvRow.append(key);
            csvRow.append(delimiter);
            Map<String, String> messagesPerLanguage = messageMapPerKey.get(key);
            for (int i = 0; i < languages.size(); i++) {
                String message = messagesPerLanguage.get(languages.get(i));
                if (message != null) {
                    csvRow.append(message);
                } else {
                    csvRow.append(" ");
                }
                if (i + 1 < languages.size()) {
                    csvRow.append(delimiter);
                }
            }
            LOG.debug("CSV row: {}", csvRow);
            writer.println(csvRow);
        }
    }

}


