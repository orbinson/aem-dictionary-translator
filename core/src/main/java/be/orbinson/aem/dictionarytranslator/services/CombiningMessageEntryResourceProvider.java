package be.orbinson.aem.dictionarytranslator.services;

import be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.text.Text;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
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
                ResourceProvider.PROPERTY_ROOT + "=" + CombiningMessageEntryResourceProvider.ROOT
        }
)
public class CombiningMessageEntryResourceProvider extends ResourceProvider<Object> {

    public static final String ROOT = "/mnt/dictionary";

    public static final String RESOURCE_TYPE = "aem-dictionary-translator/components/combining-message-entry";

    public static final String KEY = "key";
    public static final String DICTIONARY_PATH = "dictionaryPath";
    public static final String LANGUAGES = "languages";
    public static final String MESSAGE_ENTRY_PATHS = "messageEntryPaths";

    @Reference
    private DictionaryService dictionaryService;

    private Map<String, Object> getValuesAndMessageEntryPaths(Resource dictionaryResource, String key, List<String> languages) {
        Map<String, Object> properties = new HashMap<>();
        List<String> messageEntryPaths = new ArrayList<>();

        for (String language : languages) {
            Resource languageResource = dictionaryService.getLanguageResource(dictionaryResource, language);
            if (languageResource != null) {
                Resource messageEntryResource = dictionaryService.getMessageEntryResource(languageResource, key);
                if (messageEntryResource != null && (messageEntryResource.getValueMap().containsKey(DictionaryConstants.SLING_MESSAGE))) {
                    properties.put(language, messageEntryResource.getValueMap().get(DictionaryConstants.SLING_MESSAGE, String.class));
                    messageEntryPaths.add(messageEntryResource.getPath());
                }
            }
        }

        properties.put(MESSAGE_ENTRY_PATHS, messageEntryPaths);

        return properties;
    }

    @Override
    public @Nullable Resource getResource(@NotNull ResolveContext<Object> ctx, @NotNull String path, @NotNull ResourceContext resourceContext, @Nullable Resource parent) {
        ResourceResolver resourceResolver = ctx.getResourceResolver();
        if (!path.startsWith(ROOT) || ROOT.equals(path)) {
            // Not applying combining message entry resource provider
            return null;
        }

        String key = Text.getName(path);
        String dictionaryPath = Text.getRelativeParent(path, 1).replaceFirst(ROOT, "");
        Resource dictionaryResource = resourceResolver.getResource(dictionaryPath);

        if (dictionaryResource != null && isMessageEntry(dictionaryResource, key)) {
            Map<String, Object> properties = new HashMap<>();
            properties.put(KEY, key);
            properties.put("path", path);
            properties.put(DICTIONARY_PATH, dictionaryPath);
            List<String> languages = dictionaryService.getLanguages(dictionaryResource);
            properties.put(LANGUAGES, languages);
            properties.putAll(getValuesAndMessageEntryPaths(dictionaryResource, key, languages));
            return new ValueMapResource(resourceResolver, path, RESOURCE_TYPE, new ValueMapDecorator(properties));
        }
        return null;
    }

    private boolean isMessageEntry(Resource dictionaryResource, String key) {
        List<String> languages = dictionaryService.getLanguages(dictionaryResource);
        // in order to speed things up always start with language "en" (if existing)
        String mostCompleteLanguage = Locale.ENGLISH.getLanguage();
        if (languages.contains(mostCompleteLanguage)) {
            languages.remove(mostCompleteLanguage);
            languages.add(0, mostCompleteLanguage);
        }

        if (!languages.isEmpty()) {
            for (String language : languages) {
                Resource languageResource = dictionaryService.getLanguageResource(dictionaryResource, language);
                if (languageResource != null) {
                    Resource messageEntryResource = dictionaryService.getMessageEntryResource(languageResource, key);
                    if (messageEntryResource != null && messageEntryResource.isResourceType(DictionaryConstants.SLING_MESSAGEENTRY)) {
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
