package be.orbinson.aem.dictionarytranslator.services;

import be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.text.Text;
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

    public static final String RESOURCE_TYPE = "aem-dictionary-translator/components/label";

    public static final String LANGUAGES = "languages";
    public static final String LABEL_PATHS = "labelPaths";

    @Reference
    private DictionaryService dictionaryService;

    @Override
    public void delete(@NotNull ResolveContext<Object> ctx, @NotNull Resource resource) throws PersistenceException {
        ValueMap properties = resource.getValueMap();

        if (properties.containsKey(LABEL_PATHS)) {
            ResourceResolver resourceResolver = ctx.getResourceResolver();
            for (String labelPath : properties.get(LABEL_PATHS, new String[0])) {
                Resource labelResource = resourceResolver.getResource(labelPath);
                if (labelResource != null) {
                    resourceResolver.delete(labelResource);
                }
            }
             resourceResolver.commit();
        }
    }

    private Map<String, Object> getValuesAndLabelPaths(Resource dictionaryResource, String key, List<String> languages) {
        Map<String, Object> properties = new HashMap<>();
        List<String> labelPaths = new ArrayList<>();

        for (String language : languages) {
            Resource languageResource = dictionaryService.getLanguageResource(dictionaryResource, language);
            if (languageResource != null) {
                Resource labelResource = dictionaryService.getLabelResource(languageResource, key);
                if (labelResource != null && (labelResource.getValueMap().containsKey(DictionaryConstants.SLING_MESSAGE))) {
                    properties.put(language, labelResource.getValueMap().get(DictionaryConstants.SLING_MESSAGE, String.class));
                    labelPaths.add(labelResource.getPath());
                }
            }
        }

        properties.put("labelPaths", labelPaths);

        return properties;
    }

    @Override
    public @Nullable Resource getResource(@NotNull ResolveContext<Object> ctx, @NotNull String path, @NotNull ResourceContext resourceContext, @Nullable Resource parent) {
        ResourceResolver resourceResolver = ctx.getResourceResolver();
        if (!path.startsWith(ROOT) || ROOT.equals(path)) {
            // Not applying label resource provider
            return null;
        }

        String key = Text.getName(path);
        String dictionaryPath = Text.getRelativeParent(path, 1).replaceFirst(ROOT, "");
        Resource dictionaryResource = resourceResolver.getResource(dictionaryPath);

        if (dictionaryResource != null && isLabel(dictionaryResource, key)) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("key", key);
            properties.put("path", path);
            properties.put("dictionaryPath", dictionaryPath);
            List<String> languages = dictionaryService.getLanguages(dictionaryResource);
            properties.put(LANGUAGES, languages);
            properties.putAll(getValuesAndLabelPaths(dictionaryResource, key, languages));
            return new ValueMapResource(resourceResolver, path, RESOURCE_TYPE, new ValueMapDecorator(properties));
        }
        return null;
    }

    private boolean isLabel(Resource dictionaryResource, String key) {
        List<String> languages = dictionaryService.getLanguages(dictionaryResource);
        if (!languages.isEmpty()) {
            for (String language : languages) {
                Resource languageResource = dictionaryService.getLanguageResource(dictionaryResource, language);
                if (languageResource != null) {
                    Resource labelResource = dictionaryService.getLabelResource(languageResource, key);
                    if (labelResource != null && labelResource.isResourceType(DictionaryConstants.SLING_MESSAGEENTRY)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public @Nullable Iterator<Resource> listChildren(@NotNull ResolveContext<Object> ctx, @NotNull Resource parent) {
        return Collections.emptyIterator();
    }

}
