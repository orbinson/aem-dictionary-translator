package be.orbinson.aem.dictionarytranslator.servlets.datasource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.servlethelpers.MockRequestDispatcherFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.resourceresolver.MockFindQueryResources;
import org.apache.sling.testing.resourceresolver.MockFindResourcesHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.EmptyDataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider;
import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider.ValidationMessage;
import be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider.ValidationMessage.Severity;
import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class CombiningMessageEntriesDatasourceForTableTest {

    private final AemContext context = new AemContext(ResourceResolverType.RESOURCEPROVIDER_MOCK);
    CombiningMessageEntriesDatasourceForTable servlet;

    @Mock
    ExpressionResolver expressionResolver;

    private static final String KEY_SPECIAL_CHARACTERS = "key / with / special characters &&&";

    /** Contains the paths of all resources which are returned by the findResources() method, necessary because the RR mock does not support queries natively */
    List<String> dictionaryPaths;

    @BeforeEach
    void beforeEach() {
        context.registerService(Replicator.class, mock(Replicator.class));
        expressionResolver = context.registerService(ExpressionResolver.class, expressionResolver);
        DictionaryService dictionaryService = new DictionaryServiceImpl();
        context.registerInjectActivateService(dictionaryService);
        Converter converter = Converters.standardConverter();
        CombiningMessageEntryResourceProvider.Config config = converter.convert(Map.of("enableValidation", true)).to(CombiningMessageEntryResourceProvider.Config.class);
        CombiningMessageEntryResourceProvider provider = new CombiningMessageEntryResourceProvider(dictionaryService, config);
        context.registerInjectActivateService(provider);
        context.load().json("/content.json", "/content");
        dictionaryPaths = new ArrayList<>(List.of(
                "/content/dictionaries/fruit/i18n/en",
                "/content/dictionaries/fruit/i18n/nl_be",
                "/content/dictionaries/vegetables/i18n/en")
        );
        
        MockFindResourcesHandler handler = new MockFindResourcesHandler() {
            @Override
            public @Nullable Iterator<Resource> findResources(@NotNull String query, String language) {
                return dictionaryPaths.stream().map(p -> context.resourceResolver().getResource(p)).iterator();
            }
        };
        MockFindQueryResources.addFindResourceHandler(context.resourceResolver(), handler);
        // add additional message entry with key having special characters
        context.build().resource("/content/dictionaries/fruit/i18n/en/specialkey", "jcr:primaryType", "sling:MessageEntry",
                "sling:message", "Cherry",
                "sling:key", KEY_SPECIAL_CHARACTERS);
        servlet = context.registerService(new CombiningMessageEntriesDatasourceForTable(dictionaryService, provider, expressionResolver));
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
    void testListDataSource() throws ServletException, IOException {
        context.request().setResource(new SyntheticResource(context.resourceResolver(), "/some/path", "artificial test resource"));
        servlet.doGet(context.request(), context.response());

        DataSource dataSource = (DataSource) context.request().getAttribute(DataSource.class.getName());
        assertDataSourceEquals(dataSource,
                new SyntheticResource(context.resourceResolver(), "/mnt/dictionary/content/dictionaries/fruit/i18n/apple", CombiningMessageEntryResourceProvider.RESOURCE_TYPE),
                new SyntheticResource(context.resourceResolver(), "/mnt/dictionary/content/dictionaries/fruit/i18n/banana", CombiningMessageEntryResourceProvider.RESOURCE_TYPE),
                new SyntheticResource(context.resourceResolver(), "/mnt/dictionary/content/dictionaries/fruit/i18n/cherry", CombiningMessageEntryResourceProvider.RESOURCE_TYPE),
                new SyntheticResource(context.resourceResolver(), CombiningMessageEntryResourceProvider.createPath("/content/dictionaries/fruit/i18n", KEY_SPECIAL_CHARACTERS), CombiningMessageEntryResourceProvider.RESOURCE_TYPE),
                new SyntheticResource(context.resourceResolver(), "/mnt/dictionary/content/dictionaries/fruit/i18n/mango", CombiningMessageEntryResourceProvider.RESOURCE_TYPE),
                new SyntheticResource(context.resourceResolver(), "/mnt/dictionary/content/dictionaries/fruit/i18n/papaya", CombiningMessageEntryResourceProvider.RESOURCE_TYPE),
                new SyntheticResource(context.resourceResolver(), "/mnt/dictionary/content/dictionaries/fruit/i18n/pear", CombiningMessageEntryResourceProvider.RESOURCE_TYPE)
        );
    }

    @Test
    void testColumnDataSource() throws ServletException, IOException {
        context.request().setResource(new SyntheticResource(context.resourceResolver(), "/some/path/columnsdatasource", "artificial test resource"));
        servlet.doGet(context.request(), context.response());

        DataSource dataSource = (DataSource)context.request().getAttribute(DataSource.class.getName());

        assertDataSourceEquals(dataSource, CombiningMessageEntriesDatasourceForTable.getColumn(context.resourceResolver(), "select", true, Optional.empty()),
                CombiningMessageEntriesDatasourceForTable.getColumn(context.resourceResolver(), "jcr:title", "Key", Optional.of("Key")),
                CombiningMessageEntriesDatasourceForTable.getColumn(context.resourceResolver(), "jcr:title", "Validation", Optional.of("Validation")),
                CombiningMessageEntriesDatasourceForTable.getColumn(context.resourceResolver(), "jcr:title", "en", Optional.of("en")),
                CombiningMessageEntriesDatasourceForTable.getColumn(context.resourceResolver(), "jcr:title", "nl-BE", Optional.of("nl-BE")));
    }

    /**
     * As {@link DataSource} is not an iterable we cannot use {@link #assertIterableEquals(Iterable, Iterable)}.
     * Also most resources don't have a proper implementation of {@link Object#equals()}, therefore this
     * method compares path, resource type and (optionally) value map.
     * @param dataSource
     * @param expectedResources
     */
    void assertDataSourceEquals(DataSource dataSource, Resource... expectedResources) {
        Iterator<Resource> iterator = dataSource.iterator();
        int index = 0;
        for (Resource expectedResource : expectedResources) {
            assertTrue(iterator.hasNext(), "Iterator has less elements than expected, expected at least " + (expectedResources.length) + " but got only " + index);
            Resource resource = iterator.next();
            assertEquals(expectedResource.getPath(), resource.getPath(), "Path at index " + index + " does not match");
            assertEquals(expectedResource.getResourceType(), resource.getResourceType(), "Type at index " + index + " does not match");
            if (!expectedResource.getValueMap().isEmpty()) {
                assertEquals(expectedResource.getValueMap(), resource.getValueMap(), "ValueMap at index " + index + " does not match");
            }
            index++;
        }
        assertFalse(iterator.hasNext(), () -> "Iterator has at least one more element than expected: " + iterator.next());
    }

    @Test
    void testValidationMessageSortOrder() {
        SortedSet<ValidationMessage> messages = new TreeSet<>();
        messages.add(new ValidationMessage(Severity.WARNING, Locale.GERMAN, "warn"));
        messages.add(new ValidationMessage(Severity.ERROR, Locale.GERMAN, "error"));
        messages.add(new ValidationMessage(Severity.INFO, Locale.GERMAN, "info"));
        messages.add(new ValidationMessage(Severity.ERROR, Locale.GERMAN, "error2"));
        assertArrayEquals(new ValidationMessage.Severity[] { Severity.ERROR, Severity.ERROR, Severity.WARNING, Severity.INFO }, messages.stream().map(ValidationMessage::getSeverity).toArray());
    }
}