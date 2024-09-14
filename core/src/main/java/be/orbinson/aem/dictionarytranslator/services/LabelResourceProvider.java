package be.orbinson.aem.dictionarytranslator.services;

import be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants;
import be.orbinson.aem.dictionarytranslator.utils.DictionaryUtil;
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

    public static final String RESOURCE_TYPE = "aem-dictionary-translator/components/label";
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
                    Resource labelResource = getLabelResource(language, resourceResolver, dictionaryPath, name);
                    if (labelResource != null) {
                        resourceResolver.delete(labelResource);
                    }
                }
                resourceResolver.commit();
            }
        }
    }

    private static @Nullable Resource getLabelResource(String language, ResourceResolver resourceResolver, String dictionaryPath, String name) {
        Resource dictionaryResource = resourceResolver.getResource(dictionaryPath);
        if (dictionaryResource != null) {
            Resource languageResource = DictionaryUtil.getLanguageResource(dictionaryResource, language);
            if (languageResource != null) {
                return languageResource.getChild(name);
            }
        }
        return null;
    }


    private Map<String, Object> getValuesAndLabelPaths(Resource dictionaryResource, String labelName, List<String> languages) {
        Map<String, Object> properties = new HashMap<>();
        List<String> labelPaths = new ArrayList<>();

        for (String language : languages) {
            Resource languageResource = DictionaryUtil.getLanguageResource(dictionaryResource, language);
            if (languageResource != null) {
                Resource labelResource = languageResource.getChild(labelName);
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

        String labelName = Text.getName(path);
        String dictionaryPath = Text.getRelativeParent(path, 1).replaceFirst(ROOT, "");
        Resource dictionaryResource = resourceResolver.getResource(dictionaryPath);

        if (dictionaryResource != null && isLabel(dictionaryResource, labelName)) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("name", labelName);
            properties.put("key", getKey(dictionaryResource, labelName));
            properties.put("path", path);
            properties.put("dictionaryPath", dictionaryPath);
            List<String> languages = dictionaryService.getLanguages(dictionaryResource);
            properties.put(LANGUAGES, languages);
            properties.putAll(getValuesAndLabelPaths(dictionaryResource, labelName, languages));
            return new ValueMapResource(resourceResolver, path, RESOURCE_TYPE, new ValueMapDecorator(properties));
        }
        return null;
    }

    private boolean isLabel(Resource dictionaryResource, String labelName) {
        List<String> languages = dictionaryService.getLanguages(dictionaryResource);
        if (!languages.isEmpty()) {
            for (String language : languages) {
                Resource languageResource = DictionaryUtil.getLanguageResource(dictionaryResource, language);
                if (languageResource != null) {
                    Resource labelResource = languageResource.getChild(labelName);
                    if (labelResource != null && labelResource.isResourceType(DictionaryConstants.SLING_MESSAGEENTRY)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns the first set {@code sling:key} of the {@code sling:MessageEntry} resource with the given labelName below any of the available languages.
     * Falls back to the {@code labelName} if no {@code sling:key} in any language's entry is found.
     *
     * @param dictionaryResource the parent resource containing the languages of a dictionary
     * @param labelName
     * @return the key of a label/entry in the dictionary
     */
    private String getKey(Resource dictionaryResource, String labelName) {
        List<String> languages = dictionaryService.getLanguages(dictionaryResource);
        // in order to speed things up always start with language "en" (if existing)
        String mostCompleteLanguage = Locale.ENGLISH.getLanguage();
        if (languages.contains(mostCompleteLanguage)) {
            languages.remove(mostCompleteLanguage);
            languages.add(0, mostCompleteLanguage);
        }
        return languages.stream()
                .map(language -> DictionaryUtil.getLanguageResource(dictionaryResource, language))
                .filter(Objects::nonNull)
                .map(languageResource -> languageResource.getChild(labelName))
                .filter(Objects::nonNull)
                .map(labelResource -> labelResource.getValueMap().get(DictionaryConstants.SLING_KEY, String.class))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(labelName);
    }

    @Override
    public @Nullable Iterator<Resource> listChildren(@NotNull ResolveContext<Object> ctx, @NotNull Resource parent) {
        return Collections.emptyIterator();
    }

}
