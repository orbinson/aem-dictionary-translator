package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGE;

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
        } catch (IOException e) {
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

    private void writeCsvRows(PrintWriter writer, RequestParameter delimiter, Resource dictionaryResource, List<String> languages) {
        List<String> keys = dictionaryService.getKeys(dictionaryResource);
        Map<String, Resource> languageResources = getLanguageResourceMap(dictionaryResource, languages);
        for (String key : keys) {
            StringBuilder csvRow = new StringBuilder();
            csvRow.append(key);
            csvRow.append(delimiter);
            for (int i = 0; i < languages.size(); i++) {
                String language = languages.get(i);
                Resource messageEntryResource = dictionaryService.getMessageEntryResource(languageResources.get(language), key);
                if (messageEntryResource != null) {
                    csvRow.append(messageEntryResource.getValueMap().get(SLING_MESSAGE));
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

    private @NotNull Map<String, Resource> getLanguageResourceMap(Resource dictionaryResource, List<String> languages) {
        Map<String, Resource> languageResources = new HashMap<>();
        for (String lang : languages) {
            Resource languageResource = dictionaryService.getLanguageResource(dictionaryResource, lang);
            if (languageResource != null) {
                languageResources.put(lang, languageResource);
            }
        }
        return languageResources;
    }

}


