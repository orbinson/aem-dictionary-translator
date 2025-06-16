package be.orbinson.aem.dictionarytranslator.servlets.datasource.builder;

import static org.mockito.Mockito.mock;
import static org.apache.sling.hamcrest.ResourceMatchers.*;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

class ValueMapResourceBuilderTest {

    @Test
    void testNtUnstructured() {
        ValueMapResourceBuilderFactory factory = new ValueMapResourceBuilderFactory(mock(ResourceResolver.class), "/my/path");
        ValueMapResourceBuilder<?> rootBuilder = ValueMapResourceBuilder.forNtUnstructured(factory, "a").withProperty("prop1", "a").withProperty("prop2", 1);
        rootBuilder.withChild(
            ValueMapResourceBuilder.forNtUnstructured(rootBuilder.getChildFactory(), "a1")
                .withProperty("prop1", "a1")
                .withProperty("prop2", 2)
            );
        rootBuilder.withChild(
                ValueMapResourceBuilder.forNtUnstructured(rootBuilder.getChildFactory(), "a2")
                    .withProperty("prop1", "a2")
                    .withProperty("prop2", 2)
                );
        Resource rootResource = rootBuilder.build();
        MatcherAssert.assertThat(rootResource, nameAndProps("a", "prop1", "a", "prop2", 1));
        MatcherAssert.assertThat(rootResource, path("/my/path/a"));
        // https://issues.apache.org/jira/browse/SLING-12831
        //MatcherAssert.assertThat(rootResource, resourceType("nt:unstructured"));
        MatcherAssert.assertThat(rootResource, hasChildren("a1", "a2"));
       
    }

}
