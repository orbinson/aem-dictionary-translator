package be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.graniteui;

import static org.apache.sling.hamcrest.ResourceMatchers.hasChildren;
import static org.apache.sling.hamcrest.ResourceMatchers.name;
import static org.apache.sling.hamcrest.ResourceMatchers.nameAndProps;
import static org.apache.sling.hamcrest.ResourceMatchers.path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.ValueMapResourceBuilder;
import be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.ValueMapResourceBuilderFactory;

class ContainerValueMapResourceBuilderTest {

    @Test
    void testFieldSet() {
        ValueMapResourceBuilderFactory factory = new ValueMapResourceBuilderFactory(mock(ResourceResolver.class), "/my/path");
        
        ContainerValueMapResourceBuilder<?> rootBuilder = ContainerValueMapResourceBuilder.forFieldSet(factory, "fieldSet");
        rootBuilder.withItem(FieldValueMapResourceBuilder.forTextField(rootBuilder.getItemFactory(), "field1", "Field 1", "my text"));
        rootBuilder.withItem(FieldValueMapResourceBuilder.forTextField(rootBuilder.getItemFactory(), "field2", "Field 2", "my text"));
        
        Resource rootResource =  rootBuilder.build();
        MatcherAssert.assertThat(rootResource, name("fieldSet"));
        MatcherAssert.assertThat(rootResource, path("/my/path/fieldSet"));
        // https://issues.apache.org/jira/browse/SLING-12831
        //MatcherAssert.assertThat(rootResource, resourceType("nt:unstructured"));
        MatcherAssert.assertThat(rootResource, hasChildren("items"));
        Iterator<Resource> children = rootResource.getChild("items").listChildren();
        assertTrue(children.hasNext());
        Resource child1 = children.next();
        assertTrue(children.hasNext());
        Resource child2 = children.next();
        assertFalse(children.hasNext());
        MatcherAssert.assertThat(child1, nameAndProps("field1", "name", "field1"));
        MatcherAssert.assertThat(child1, path("/my/path/fieldSet/items/field1"));
        MatcherAssert.assertThat(child2, nameAndProps("field2", "name", "field2"));
        MatcherAssert.assertThat(child2, path("/my/path/fieldSet/items/field2"));
    }

    @Test
    void testWithItemAndWrongFactory() {
        ValueMapResourceBuilderFactory factory = new ValueMapResourceBuilderFactory(mock(ResourceResolver.class), "/my/path");
        
        ContainerValueMapResourceBuilder<?> rootBuilder = ContainerValueMapResourceBuilder.forFieldSet(factory, "fieldSet");
        assertThrows(IllegalStateException.class, () -> {
            rootBuilder.withItem(ValueMapResourceBuilder.forNtUnstructured(rootBuilder.getChildFactory(), "field1"));
        });
    }
}
