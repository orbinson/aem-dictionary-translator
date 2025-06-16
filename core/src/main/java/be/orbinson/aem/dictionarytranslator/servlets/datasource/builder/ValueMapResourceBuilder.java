package be.orbinson.aem.dictionarytranslator.servlets.datasource.builder;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.jetbrains.annotations.NotNull;

import com.adobe.granite.ui.components.ds.ValueMapResource;

public class ValueMapResourceBuilder<T extends ValueMapResourceBuilder<T>> {

    protected final ValueMapResourceBuilderFactory factory;
    protected final String name;
    protected final Map<String, Object> properties;
    private final List<ValueMapResourceBuilder<?>> children;
    protected final String resourceType;

    /**
     * Creates a builder for a resource of type "nt:unstructured" with the given name.
     * @param name the name of the resource to create
     * @return a new {@link ValueMapResourceBuilder} for the specified resource type
     */
    public static <T extends ValueMapResourceBuilder<T>> @NotNull ValueMapResourceBuilder<T> forNtUnstructured(ValueMapResourceBuilderFactory factory, String name) {
        return new ValueMapResourceBuilder<>(factory, name, "nt:unstructured");
    }

    protected ValueMapResourceBuilder(ValueMapResourceBuilderFactory factory, String name, String resourceType) {
        this.factory = factory;
        this.name = name;
        properties = new HashMap<>();
        children = new LinkedList<>();
        this.resourceType = resourceType;
    }

    /**
     * 
     * @return the factory used to create this resource builder
     */
    public ValueMapResourceBuilderFactory getFactory() {
        return factory;
    }

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    public T withProperty(String name, Object value) {
        properties.put(name, value);
        return self();
    }

    /** Returns a new child factory for creating resources directly under this resource */
    public ValueMapResourceBuilderFactory getChildFactory() {
        return new ValueMapResourceBuilderFactory(factory.getResourceResolver(), factory.getPath() + "/" + name);
    }

    public void withChild(ValueMapResourceBuilder<?> child) {
        children.add(child);
    }

    public ValueMapResource build() {
        if (resourceType == null) {
            throw new IllegalStateException("Resource type must be set before building the resource.");
        }
        return new ValueMapResource(factory.getResourceResolver(), factory.getPath() + "/" + name, resourceType, new ValueMapDecorator(properties), children.stream().map(ValueMapResourceBuilder::build).collect(Collectors.toList()));
    }
}
