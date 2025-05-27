package be.orbinson.aem.dictionarytranslator.servlets.datasource;

import java.io.IOException;
import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.ExpressionHelper;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.EmptyDataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;

public abstract class SortedAndPaginatedDataSource extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(SortedAndPaginatedDataSource.class);
    private final ExpressionResolver expressionResolver;

    // https://developer.adobe.com/experience-manager/reference-materials/6-5/granite-ui/api/jcr_root/libs/granite/ui/components/coral/foundation/table/index.html
    private static final String PARAMETER_SORT_NAME = "sortName";
    private static final String PARAMETER_SORT_DIRECTION = "sortDir";

    SortedAndPaginatedDataSource(ExpressionResolver expressionResolver) {
        super();
        this.expressionResolver = expressionResolver;
    }

    private static final class OffsetAndLimit {
        private final int offset;
        private final int limit;

        public OffsetAndLimit(int offset, int limit) {
            this.offset = offset;
            this.limit = limit;
        }

        public int getOffset() {
            return offset;
        }

        public int getLimit() {
            return limit;
        }
    }

    protected OffsetAndLimit getOffsetAndLimit(SlingHttpServletRequest request) {
        Config dsCfg = new Config(request.getResource().getChild("datasource"));
        ExpressionHelper expressionHelper = new ExpressionHelper(expressionResolver, request);
        if (dsCfg == null) {
            LOG.warn("No datasource configuration found for resource: {}", request.getResource().getPath());
            return new OffsetAndLimit(0, -1); // Default to no limit and offset
        }
        if (dsCfg.get("limit").isBlank() || dsCfg.get("offset").isBlank()) {
            LOG.warn("No limit or offset configured for datasource: {}", request.getResource().getPath());
            return new OffsetAndLimit(0, -1); // Default to no limit and offset
        }
        Integer limit = expressionHelper.get(dsCfg.get("limit"), Integer.class);
        Integer offset = expressionHelper.get(dsCfg.get("offset"), Integer.class);
        if (limit == null || offset == null) {
            offset = 0;
            limit = -1;
        }
        return new OffsetAndLimit(offset, limit);
    }

    protected Optional<Comparator<Resource>> getComparator(SlingHttpServletRequest request) {
        // TODO: externalize via expression helper
        String sortName = request.getParameter(PARAMETER_SORT_NAME);
        boolean isAscending = "asc".equals(request.getParameter(PARAMETER_SORT_DIRECTION));
        if (sortName == null || sortName.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(getComparator(request.getLocale(), sortName, isAscending));
    }

    protected Comparator<Resource> getComparator(Locale locale, String sortName, boolean isAscending) {
        return getComparator(
                getResourceValueForSortName(sortName),
                locale,
                isAscending
        );
    }

    public abstract Function<Resource, Comparable<?>> getResourceValueForSortName(String sortName);

    private static Comparator<Resource> getComparator(Function<Resource, Comparable<?>> resourceValueExtractor, Locale locale, boolean isAscending) {
        Collator collator = Collator.getInstance(locale);
        return new Comparator<Resource>() {
            @Override
            public int compare(Resource o1, Resource o2) {
                Comparable value1 = resourceValueExtractor.apply(o1);
                Comparable value2 = resourceValueExtractor.apply(o2);
                int result;
                // Handle null values first
                if (value1 == null) {
                    result = (value2 == null) ? 0 : -1;
                } else if (value2 == null) {
                    result = 1;
                } else {
                    if (value1 instanceof String && value2 instanceof String) {
                        // If both values are strings, use collator for comparison
                        result = collator.compare((String) value1, (String) value2);
                    } else {
                        result = value1.compareTo(value2);
                    }
                }
                if (!isAscending) {
                    result = -result; // reverse order if not ascending
                }
                return result;
            }
        };
    }

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        OffsetAndLimit offsetAndLimit = getOffsetAndLimit(request);
        Optional<Comparator<Resource>> comparator = getComparator(request);
        DataSource dataSource;
        try {
            Collection<Resource> resources = populateResources(request, response, request.getResourceResolver(), offsetAndLimit.getOffset(), offsetAndLimit.getLimit(), comparator);
            dataSource = new SimpleDataSource(resources.iterator());
        } catch (Exception e) {
            LOG.error("Error creating dictionary data source", e);
            dataSource = EmptyDataSource.instance();
        }
        request.setAttribute(DataSource.class.getName(), dataSource);
    }

    protected Collection<Resource> populateResources(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response,
            ResourceResolver resolver, int offset, int limit, Optional<Comparator<Resource>> comparator) throws DictionaryException, ServletException, IOException {
        List<Resource> resourceList = new LinkedList<>();
        populateResources(request, response, resolver, resourceList);
        if (comparator.isPresent()) {
            resourceList.sort(comparator.get());
        }
        if (offset >= resourceList.size()) {
            // If offset is greater than the size of the list, return an empty list
            return new LinkedList<>();
        }
        if (limit > 0) {
            resourceList = resourceList.subList(offset, Math.min(offset + limit, resourceList.size()));
        }
        return resourceList;
    }
    
    protected abstract void populateResources(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response,
            ResourceResolver resolver, Collection<Resource> resources) throws DictionaryException, ServletException, IOException;
}
