package be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.graniteui;

import be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.ValueMapResourceBuilderFactory;

/** 
 * Builder for <a href="https://developer.adobe.com/experience-manager/reference-materials/6-5/granite-ui/api/jcr_root/libs/granite/ui/components/coral/foundation/form/field/index.html#/libs/granite/ui/components/coral/foundation/form/field">Granite UI field components</a>.
 */
public class FieldValueMapResourceBuilder<T extends FieldValueMapResourceBuilder<T>> extends ComponentValueMapResourceBuilder<T> {

    public static <T extends FieldValueMapResourceBuilder<T>> FieldValueMapResourceBuilder<T> forCheckboxField(ValueMapResourceBuilderFactory factory, String name, String text, boolean value) {
        FieldValueMapResourceBuilder<T> builder = new FieldValueMapResourceBuilder<>(factory, name, "granite/ui/components/coral/foundation/form/checkbox");
        builder.withProperty("text", text);
        builder.withProperty("checked", value);
        // don't use value "on" but "true" to ease conversion to boolean
        builder.withProperty("value", Boolean.toString(true));
        return builder;
    }

    public static <T extends FieldValueMapResourceBuilder<T>> FieldValueMapResourceBuilder<T> forTextField(ValueMapResourceBuilderFactory factory, String name, String label, String value) {
        FieldValueMapResourceBuilder<T> builder = new FieldValueMapResourceBuilder<>(factory, name, "granite/ui/components/coral/foundation/form/textfield");
        builder.withFieldLabel(label);
        builder.withProperty("value", escapeExpressions(value));
        return builder;
    }


    public static <T extends FieldValueMapResourceBuilder<T>> FieldValueMapResourceBuilder<T> forHiddenField(ValueMapResourceBuilderFactory factory, String name, String value) {
        FieldValueMapResourceBuilder<T> builder = new FieldValueMapResourceBuilder<>(factory, name, "granite/ui/components/coral/foundation/form/hidden");
        builder.withProperty("value", escapeExpressions(value));
        return builder;
    }


    /**
     * Granite UI evaluates the configured {@code value} of a field as an expression (see
     * {@code /libs/granite/ui/components/coral/foundation/form/field/init.jsp}), so a translation like
     * {@code "Resolution #{number}"} would be resolved instead of shown, and the dialog fails. Escaping the
     * expression delimiters makes the field show the text unchanged.
     *
     * @param value the text to show in the field, may be {@code null}
     * @return the text with each expression start ("$" or "#" followed by an opening brace) escaped with a backslash
     * @see <a href="https://experienceleague.adobe.com/en/tools/aem-api-documentation/6-5/granite-ui/api/jcr_root/libs/granite/ui/docs/server/el.html">Granite UI Expression Language</a>
     * @see <a href="https://docs.oracle.com/javaee/6/tutorial/doc/bnaid.html">Expression Language: Literal Expressions</a>
     */
    static String escapeExpressions(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("${", "\\${").replace("#{", "\\#{");
    }

    private FieldValueMapResourceBuilder(ValueMapResourceBuilderFactory factory, String name, String resourceType) {
        super(factory, name, resourceType);
        properties.put("name", name);
    }

    T required() {
        properties.put("required", true);
        return self();
    }

    public T disabled() {
        properties.put("disabled", true);
        return self();
    }

    public T hidden() {
        // although checkbox is supposed to be a field, it is not using the fields component rendering script (E-001703496)
        if (resourceType.equals("granite/ui/components/coral/foundation/form/checkbox")) {
            throw new IllegalStateException("Checkbox fields cannot be hidden using this method.");
        } else {
            properties.put("hidden", "true");
        }
        return self();
    }

    public T withDescription(String description) {
        properties.put("fieldDescription", description);
        return self();
    }
    
    public T withFieldLabel(String label) {
        properties.put("fieldLabel", label);
        return self();
    }
}