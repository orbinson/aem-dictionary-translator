package be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.graniteui;

import be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.ValueMapResourceBuilder;
import be.orbinson.aem.dictionarytranslator.servlets.datasource.builder.ValueMapResourceBuilderFactory;

/**
 * Builder for <a href="https://developer.adobe.com/experience-manager/reference-materials/6-5/granite-ui/api/jcr_root/libs/granite/ui/docs/server/datasource.html">Granite UI container components having a datasource</a>.
 * This builder is used to create containers that can hold multiple items, such as fieldsets or wells.
 */
public class ContainerValueMapResourceBuilder<T extends ContainerValueMapResourceBuilder<T>> extends ComponentValueMapResourceBuilder<T> {

    public static <T extends ContainerValueMapResourceBuilder<T>> ContainerValueMapResourceBuilder<T> forFieldSet(ValueMapResourceBuilderFactory factory, String name) {
        return new ContainerValueMapResourceBuilder<>(factory, name, "granite/ui/components/coral/foundation/form/fieldset");
    }
    
    public static <T extends ContainerValueMapResourceBuilder<T>> ContainerValueMapResourceBuilder<T> forWell(ValueMapResourceBuilderFactory factory, String name) {
        return new ContainerValueMapResourceBuilder<>(factory, name, "granite/ui/components/coral/foundation/well");
    }

    private ValueMapResourceBuilder<T> itemsBuilder;

    public ContainerValueMapResourceBuilder(ValueMapResourceBuilderFactory factory, String name, String resourceType) {
        super(factory, name, resourceType);
    }

    /**
     * Returns a factory for creating a <a href="https://developer.adobe.com/experience-manager/reference-materials/6-5/granite-ui/api/jcr_root/libs/granite/ui/docs/server/datasource.html">item data source</a>.
     * Any {@link ValueMapResourceBuilder}s created with this factory need to be added to this container via {@link #withItem(ValueMapResourceBuilder)}.
     * @return the factory for creating item builders
     */
    public ValueMapResourceBuilderFactory getItemFactory() {
        return getOrCreateItemsBuilder().getChildFactory();
    }

    private ValueMapResourceBuilder<?> getOrCreateItemsBuilder() {
        if (itemsBuilder == null) {
            itemsBuilder = ValueMapResourceBuilder.forNtUnstructured(getChildFactory(), "items");
            super.withChild(itemsBuilder);
        }
        return itemsBuilder;
    }

    /**
     * Adds an item {@link ValueMapResourceBuilder} to this container.
     * The child will be added below the {@code items} node of this container.
     * Use it only with a {@link ValueMapResourceBuilder} created from the factory returned by {@link #getItemFactory()}.
     * @param child the child builder to add
     */
    public void withItem(ValueMapResourceBuilder<?> child) {
        if (itemsBuilder == null || !child.getFactory().equals(itemsBuilder.getChildFactory())) {
            throw new IllegalStateException("Only children created from this containers item factory are supported.");
        }
        itemsBuilder.withChild(child);
    }

}