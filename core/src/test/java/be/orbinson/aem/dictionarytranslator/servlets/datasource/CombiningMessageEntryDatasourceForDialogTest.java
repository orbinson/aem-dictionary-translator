package be.orbinson.aem.dictionarytranslator.servlets.datasource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.jackrabbit.vault.util.Text;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.servlethelpers.MockRequestDispatcherFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.EmptyDataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider;
import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider.ValidationMessage;
import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider.ValidationMessage.Severity;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
public class CombiningMessageEntryDatasourceForDialogTest {

    private final AemContext context = new AemContext(ResourceResolverType.RESOURCEPROVIDER_MOCK);
    CombiningMessageEntryDatasourceForDialog servlet;

    @Mock
    ExpressionResolver expressionResolver;

    private static final String KEY_SPECIAL_CHARACTERS = "key / with / special characters &&&";

    @BeforeEach
    void beforeEach() {
        context.registerService(Replicator.class, mock(Replicator.class));
        expressionResolver = context.registerService(ExpressionResolver.class, expressionResolver);
        context.registerInjectActivateService(new DictionaryServiceImpl());
        context.registerInjectActivateService(new CombiningMessageEntryResourceProvider());
        context.load().json("/content.json", "/content");
        // add additional message entry with key havgin special characters
        context.build().resource("/content/dictionaries/fruit/i18n/en/specialkey", "jcr:primaryType", "sling:MessageEntry",
                "sling:message", "Cherry",
                "sling:key", KEY_SPECIAL_CHARACTERS);
        servlet = context.registerInjectActivateService(new CombiningMessageEntryDatasourceForDialog());
        context.request().setRequestDispatcherFactory(new MockRequestDispatcherFactory() {

            @Override
            public RequestDispatcher getRequestDispatcher(String path, RequestDispatcherOptions options) {
                throw new UnsupportedOperationException("Not implemented");
            }

            @Override
            public RequestDispatcher getRequestDispatcher(Resource resource, RequestDispatcherOptions options) {
                return new RequestDispatcher() {

                    @Override
                    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
                        throw new UnsupportedOperationException("Not implemented");
                    }

                    @Override
                    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
                        // don't replicate logic from /libs/cq/gui/components/common/datasources/languages/languages.jsp
                        // but just return empty data source (i.e. no language labels available)
                        DataSource ds = EmptyDataSource.instance();
                        request.setAttribute(DataSource.class.getName(), ds);
                    }
                };
            }
        });
        context.requestPathInfo().setSuffix("/content/dictionaries/fruit/i18n");
    }

    @Test
    void testItemDataSource() throws ServletException, IOException {
        context.request().setParameterMap(Map.of("item", "/mnt/dictionary/content/dictionaries/fruit/i18n/apple"));
        context.request().setResource(new SyntheticResource(context.resourceResolver(), "/some/path", "artificial test resource"));
        servlet.doGet(context.request(), context.response());

        DataSource dataSource = (DataSource)context.request().getAttribute(DataSource.class.getName());

        // this should only have the 3 textfields for the dialog and a hidden field for the path
        assertEquals(4, IteratorUtils.size(dataSource.iterator()));
    }

}
