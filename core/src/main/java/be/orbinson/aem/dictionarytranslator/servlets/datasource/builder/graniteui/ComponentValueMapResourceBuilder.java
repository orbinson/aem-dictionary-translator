package be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.graniteui;

import be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.ValueMapResourceBuilder;
import be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.ValueMapResourceBuilderFactory;
/**
 * This builder is used to create arbitrary Granite UI components.
 * @param <T>
 */
public class ComponentValueMapResourceBuilder<T extends ComponentValueMapResourceBuilder<T>> extends ValueMapResourceBuilder<T> {

    public enum AlertVariant {
        INFO, WARNING, ERROR, SUCCESS
    }

    public static <T extends ComponentValueMapResourceBuilder<T>> ComponentValueMapResourceBuilder<T> forAlert(ValueMapResourceBuilderFactory factory, String name, String title, String text, AlertVariant variant) {
        ComponentValueMapResourceBuilder<T> builder = new ComponentValueMapResourceBuilder<>(factory, name, "granite/ui/components/coral/foundation/alert");
        builder.withProperty("jcr:title", title);
        builder.withProperty("text", text);
        builder.withProperty("variant", variant.name().toLowerCase());
        return builder;
    }

    /**
     * Creates a builder for a <a href="https://developer.adobe.com/experience-manager/reference-materials/6-5/granite-ui/api/jcr_root/libs/granite/ui/components/coral/foundation/form/select/index.html">Granite UI granite:FormSelectItem</a>.
     * @param factory the factory to use for creating the resource
     * @param text the text to display for the select item
     * @param value the value of the select item
     * @return a new {@link ComponentValueMapResourceBuilder} for the select item
     */
    public static <T extends ComponentValueMapResourceBuilder<T>> ComponentValueMapResourceBuilder<T> forFormSelectItem(ValueMapResourceBuilderFactory factory, String text, String value) {
        ComponentValueMapResourceBuilder<T> builder = new ComponentValueMapResourceBuilder<>(factory, value, "nt:unstructured");
        builder.withProperty("text", text);
        builder.withProperty("value", value);
        return builder;
    }

    private ValueMapResourceBuilder<?> dataBuilder;

    protected ComponentValueMapResourceBuilder(ValueMapResourceBuilderFactory factory, String name, String resourceType) {
        super(factory, name, resourceType);
    }

    /**
     * https://developer.adobe.com/experience-manager/reference-materials/6-5/granite-ui/api/jcr_root/libs/granite/ui/docs/server/commonattrs.html
     * @param name the attribute name without the "granite:" prefix
     * @param value the value of the attribute
     */
    public T withCommonAttribute(String name, String value) {
        properties.put("granite:"+name, value);
        return self();
    }

    public T withDataAttribute(String name, String value) {
        getOrCreateDataBuilder().withProperty(name, value);
        return self();
    }

    private ValueMapResourceBuilder<?> getOrCreateDataBuilder() {
        if (dataBuilder == null) {
            dataBuilder = ValueMapResourceBuilder.forNtUnstructured(getChildFactory(), "granite:data");
            this.withChild(dataBuilder);
        }
        return dataBuilder;
    }
}