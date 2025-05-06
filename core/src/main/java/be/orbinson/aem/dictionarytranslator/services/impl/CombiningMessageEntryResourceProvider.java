package be.orbinson.aem.dictionarytranslator.services.impl;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService.Message;

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

/**
 * Resource provider exposing all translations for one dictionary entry.
 * It expects the path to be of the form:
 * {@code /mnt/dictionary/<dictionaryPath>/<key>}
 * <p>
 * This resource provider is used to combine message entries from different languages into a single resource.
 * It is used in the AEM UI to display the combined message entries for a given key.
 */
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

        if (dictionaryResource != null /* && isMessageEntry(dictionaryResource, key) */) {
            Map<String, Object> properties = new HashMap<>();
            properties.put(KEY, key);
            properties.put("path", path);
            properties.put("editable", dictionaryService.isEditableDictionary(dictionaryResource));
            properties.put(DICTIONARY_PATH, dictionaryPath);
            List<String> languages = dictionaryService.getLanguages(dictionaryResource);
            properties.put(LANGUAGES, languages);
            List<String> messageEntryPaths = new ArrayList<>();
            for (String language : languages) {
                Message message = dictionaryService.getMessages(dictionaryResource, language).get(key);
                final String text;
                if (message == null) {
                    text = "";
                } else {
                    text = message.getText();
                    message.getResourcePath().ifPresent(messageEntryPaths::add);
                }
                properties.put(language, text);
            }
            // all paths to replicate
            properties.put(MESSAGE_ENTRY_PATHS, messageEntryPaths);

            return new ValueMapResource(resourceResolver, path, RESOURCE_TYPE, new ValueMapDecorator(properties));
        }
        return null;
    }

    @Override
    public @Nullable Iterator<Resource> listChildren(@NotNull ResolveContext<Object> ctx, @NotNull Resource parent) {
        return Collections.emptyIterator();
    }
}
