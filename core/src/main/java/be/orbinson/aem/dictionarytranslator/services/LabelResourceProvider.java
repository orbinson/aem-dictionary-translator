package be.orbinson.aem.dictionarytranslator.services;

import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.text.Text;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.*;

@Component(
        service = ResourceProvider.class,
        property = {
                ResourceProvider.PROPERTY_ROOT + "=" + LabelResourceProvider.ROOT,
                ResourceProvider.PROPERTY_MODIFIABLE + "=" + true
        }
)
public class LabelResourceProvider extends ResourceProvider<Object> {
    public static final String ROOT = "/mnt/dictionary";

    public static final String RESOURCE_TYPE = "dictionary-translator/components/label";
    public static final String LANGUAGES = "languages";

    @Reference
    private DictionaryService dictionaryService;

    @Override
    public void delete(@NotNull ResolveContext<Object> ctx, @NotNull Resource resource) throws PersistenceException {
        ValueMap properties = resource.getValueMap();

        if (properties.containsKey(LANGUAGES) && properties.containsKey("name")) {
            ResourceResolver resourceResolver = ctx.getResourceResolver();

            String[] languages = properties.get(LANGUAGES, String[].class);
            String dictionaryPath = properties.get("dictionaryPath", String.class);
            String name = properties.get("name", String.class);

            if (StringUtils.isNotEmpty(name) && languages != null) {
                for (String language : languages) {
                    Resource labelResource = resourceResolver.getResource(dictionaryPath + "/" + language + "/" + name);
                    if (labelResource != null) {
                        resourceResolver.delete(labelResource);
                    }
                }
                resourceResolver.commit();
            }
        }
    }

    private Map<String, Object> getValues(Resource dictionaryResource, String labelName) {
        Map<String, Object> keys = new HashMap<>();

        for (String language : dictionaryService.getLanguages(dictionaryResource)) {
            Resource languageResource = dictionaryResource.getChild(language);
            if (languageResource != null) {
                Resource labelResource = languageResource.getChild(labelName);
                if (labelResource != null && (labelResource.getValueMap().containsKey("sling:message"))) {
                    keys.put(language, labelResource.getValueMap().get("sling:message", String.class));
                }
            }
        }

        return keys;
    }

    @Override
    public @Nullable Resource getResource(@NotNull ResolveContext<Object> ctx, @NotNull String path, @NotNull ResourceContext resourceContext, @Nullable Resource parent) {
        ResourceResolver resourceResolver = ctx.getResourceResolver();
        if (!path.startsWith(ROOT) || ROOT.equals(path)) {
            // Not applying label resource provider
            return null;
        }
        String labelName = Text.getName(path);
        String dictionaryPath = Text.getRelativeParent(path, 1).replaceFirst(ROOT, "");
        Resource dictionaryResource = resourceResolver.getResource(dictionaryPath);

        if (dictionaryResource != null && isLabel(dictionaryResource, labelName)) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("name", labelName);
            properties.put("key", getKey(dictionaryResource, labelName));
            properties.put("path", path);
            properties.put("dictionaryPath", dictionaryPath);
            properties.put(LANGUAGES, dictionaryService.getLanguages(dictionaryResource));
            properties.putAll(getValues(dictionaryResource, labelName));
            return new ValueMapResource(resourceResolver, path, RESOURCE_TYPE, new ValueMapDecorator(properties));
        }
        return null;
    }

    private boolean isLabel(Resource dictionaryResource, String labelName) {
        List<String> languages = dictionaryService.getLanguages(dictionaryResource);
        if (!languages.isEmpty()) {
            String firstLanguage = languages.get(0);
            Resource languageResource = dictionaryResource.getChild(firstLanguage);
            if (languageResource != null) {
                Resource labelResource = languageResource.getChild(labelName);
                return labelResource != null && labelResource.isResourceType("sling:MessageEntry");
            }
        }
        return false;
    }

    private String getKey(Resource dictionaryResource, String labelName) {
        List<String> languages = dictionaryService.getLanguages(dictionaryResource);
        if (!languages.isEmpty()) {
            String firstLanguage = languages.get(0);
            Resource languageResource = dictionaryResource.getChild(firstLanguage);
            if (languageResource != null) {
                Resource labelResource = languageResource.getChild(labelName);
                if (labelResource != null && labelResource.getValueMap().containsKey("sling:key")) {
                    return labelResource.getValueMap().get("sling:key", String.class);
                }
            }
        }
        return labelName;
    }

    @Override
    public @Nullable Iterator<Resource> listChildren(@NotNull ResolveContext<Object> ctx, @NotNull Resource parent) {
        return Collections.emptyIterator();
    }

}
