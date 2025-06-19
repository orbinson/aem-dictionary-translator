package be.orbinson.aem.dictionarytranslator.servlets.datasource.builder;

import java.util.Objects;

import org.apache.sling.api.resource.ResourceResolver;

/**
 * Factory class for creating instances of {@link ValueMapResourceBuilder}.
 * Manages dealing with paths and resource resolver.
 */
public class ValueMapResourceBuilderFactory {
    /** the parent path below which all resources are created through builders from this factory */
    private final String path;
    private final ResourceResolver resourceResolver;

    public ValueMapResourceBuilderFactory(ResourceResolver resourceResolver, String path) {
        this.resourceResolver = resourceResolver;
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, resourceResolver);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ValueMapResourceBuilderFactory other = (ValueMapResourceBuilderFactory) obj;
        return Objects.equals(path, other.path) && resourceResolver==other.resourceResolver;
    }
}
