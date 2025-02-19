package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import com.day.cq.replication.Replicator;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(AemContextExtension.class)
class ExportDictionaryServletTest {

    private final AemContext context = new AemContext();
    private ExportDictionaryServlet servlet;

    @BeforeEach
    void setUp() {
        context.registerService(Replicator.class, mock(Replicator.class));
        context.registerInjectActivateService(new DictionaryServiceImpl());

        context.load().json("/content.json", "/content");

        servlet = context.registerInjectActivateService(new ExportDictionaryServlet());

        context.response().setCharacterEncoding("UTF-8");
    }

    @ParameterizedTest
    @ValueSource(strings = {";", ","})
    void doPostWithSemiColon(String delimiter) throws Exception {
        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/fruit/i18n",
                "delimiter", delimiter
        ));

        servlet.doPost(context.request(), context.response());

        String csvContent = context.response().getOutputAsString().replaceAll("\r\n", "\n");
        String expectedContent = "KEY" + delimiter + "en" + delimiter + "nl_BE\n" +
                "apple" + delimiter + "Apple" + delimiter + "Appel\n" +
                "banana" + delimiter + "Banana" + delimiter + "Banaan\n" +
                "cherry" + delimiter + "Cherry" + delimiter + "Kers\n";
        assertEquals(expectedContent, csvContent);
    }

    @Test
    void doPostResourceNotFound() throws Exception {
        context.request().setParameterMap(Map.of(
                "dictionary", "/content/dictionaries/non-existing/i18n",
                "delimiter", ";"
        ));

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }
}
