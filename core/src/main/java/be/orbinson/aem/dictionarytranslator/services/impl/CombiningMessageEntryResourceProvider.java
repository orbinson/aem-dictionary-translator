package be.orbinson.aem.dictionarytranslator.services.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.granite.ui.components.ds.ValueMapResource;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService.Message;

/**
 * Resource provider exposing all translations for one dictionary entry.
 * It expects the path to be of the form:
 * {@code /mnt/dictionary/<dictionaryPath>/<key>}
 * <p>
 * The key is unescaped with {@link Text#unescapeIllegalJcrChars(String)} to also allow "/" in it.
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

        String key = extractKeyFromPath(path);
        String dictionaryPath = getDictionaryPath(path);
        Resource dictionaryResource = resourceResolver.getResource(dictionaryPath);

        if (dictionaryResource != null) {
            @NotNull Map<String, Message> messagePerLanguage = new HashMap<>();
            List<String> languages = dictionaryService.getLanguages(dictionaryResource);
            for (String language : languages) {
                Message message;
                try {
                    message = dictionaryService.getMessages(dictionaryResource, language).get(key);
                } catch (DictionaryException e) {
                    throw new ResourceNotFoundException("Unable to get message entries for language '" + language + "' in dictionary '" + dictionaryPath + "'", e);
                }
                messagePerLanguage.put(language, message);
            }
           return createResource(parent, resourceResolver, path, dictionaryService.isEditableDictionary(dictionaryResource), messagePerLanguage);
        } else {
            throw new ResourceNotFoundException(path, "Unable to get underlying dictionary resource for path '" + dictionaryPath + "'");
        }
    }

    static @NotNull Resource createResource(@Nullable Resource parent, @NotNull ResourceResolver resourceResolver, 
            @NotNull String path, boolean isEditable, @NotNull Map<String, Message> messagePerLanguage) {
        return new ValueMapResource(resourceResolver, path, RESOURCE_TYPE, new ValueMapDecorator(createResourceProperties(path, isEditable, messagePerLanguage)));
    }

    private static String extractKeyFromPath(@NotNull String path) {
        return Text.unescapeIllegalJcrChars(Text.getName(path));
    }

    public static @NotNull Map<String, Object> createResourceProperties(@NotNull String path, boolean isEditable, @NotNull Map<String, Message> messagePerLanguage) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(KEY, extractKeyFromPath(path));
        properties.put("path", path);
        properties.put("editable", isEditable);
        properties.put(DICTIONARY_PATH, getDictionaryPath(path));
        properties.put(LANGUAGES, messagePerLanguage.keySet());
        List<String> messageEntryPaths = new ArrayList<>();
        for (Entry<String, Message> messageEntryPerLanguageEntry : messagePerLanguage.entrySet()) {
            Message message = messageEntryPerLanguageEntry.getValue();
            final String text;
            if (message == null) {
                text = "";
            } else {
                text = message.getText();
                message.getResourcePath().ifPresent(messageEntryPaths::add);
            }
            properties.put(messageEntryPerLanguageEntry.getKey(), text);
        }
        // all paths to replicate
        properties.put(MESSAGE_ENTRY_PATHS, messageEntryPaths);
        return properties;
    }

    public static @NotNull String getDictionaryPath(@NotNull String path) {
        return Text.getRelativeParent(path, 1).replaceFirst(ROOT, "");
    }

    @Override
    public @Nullable Iterator<Resource> listChildren(@NotNull ResolveContext<Object> ctx, @NotNull Resource parent) {
        return Collections.emptyIterator();
    }
}
