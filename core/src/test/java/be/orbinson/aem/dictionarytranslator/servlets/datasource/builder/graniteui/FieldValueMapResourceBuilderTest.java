package be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.graniteui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.Test;

import be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.ValueMapResourceBuilderFactory;

class FieldValueMapResourceBuilderTest {

    private final ValueMapResourceBuilderFactory factory = new ValueMapResourceBuilderFactory(mock(ResourceResolver.class), "/my/path");

    @Test
    void testTextFieldValueWithExpressionDelimitersIsEscaped() {
        Resource field = FieldValueMapResourceBuilder.forTextField(factory, "en", "English", "Resolution #{number} of ${total}").build();
        // Granite UI evaluates the configured value as an expression; escaped, it renders the text unchanged
        assertEquals("Resolution \\#{number} of \\${total}", field.getValueMap().get("value", String.class));
    }

    @Test
    void testHiddenFieldValueWithExpressionDelimitersIsEscaped() {
        Resource field = FieldValueMapResourceBuilder.forHiddenField(factory, "key", "#{key}").build();
        assertEquals("\\#{key}", field.getValueMap().get("value", String.class));
    }

    @Test
    void testTextWithoutExpressionDelimitersIsUnchanged() {
        Resource field = FieldValueMapResourceBuilder.forTextField(factory, "en", "English", "Resolution {index} # 1 $ 2").build();
        assertEquals("Resolution {index} # 1 $ 2", field.getValueMap().get("value", String.class));
    }

    @Test
    void testEscapeExpressionsAcceptsNull() {
        assertNull(FieldValueMapResourceBuilder.escapeExpressions(null));
    }
}
