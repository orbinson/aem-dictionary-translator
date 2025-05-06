package be.orbinson.aem.dictionarytranslator.services.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

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

    enum OverlapType {
        NONE,
        PARTIAL,
        FULL,
        POTENTIAL
    }

    public static final class ValidationMessage implements Comparable<ValidationMessage> {
        enum Severity {
            INFO("info"),
            WARNING("warning"),
            ERROR("error");

            private final String label;

            Severity(String label) {
                this.label = label;
            }

            @Override
            public String toString() {
                return label;
            }
        }

        private final String language;
        private final String i18nKey;
        private final String[] arguments;
        private final Severity severity;

        public ValidationMessage(Severity severity, String language, String i18nKey, String... arguments) {
            this.severity = severity;
            this.language = language;
            this.i18nKey = i18nKey;
            this.arguments = arguments;
        }

        public String getLanguage() {
            return language;
        }

        public String getI18nKey() {
            return i18nKey;
        }
        /**
         * @return the arguments to be used in the i18n key
         */
        public String[] getArguments() {
            return arguments;
        }

        public Severity getSeverity() {
            return severity;
        }

        @Override
        public int compareTo(ValidationMessage o) {
            return o.severity.compareTo(this.severity);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(arguments);
            result = prime * result + Objects.hash(i18nKey, language, severity);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ValidationMessage other = (ValidationMessage) obj;
            return Arrays.equals(arguments, other.arguments) && Objects.equals(i18nKey, other.i18nKey)
                    && Objects.equals(language, other.language) && severity == other.severity;
        }

        
    }


    public static final String ROOT = "/mnt/dictionary";

    public static final String RESOURCE_TYPE = "aem-dictionary-translator/components/combining-message-entry";

    public static final String KEY = "key";
    public static final String DICTIONARY_PATH = "dictionaryPath";
    public static final String LANGUAGES = "languages";
    public static final String MESSAGE_ENTRY_PATHS = "messageEntryPaths";
    public static final String VALIDATION_MESSAGES = "validationMessages";

    @Reference
    private DictionaryService dictionaryService;


    static OverlapType checkBasenameOverlap(String currentDictionaryBasename, String conflictingDictionaryBasename) {
        if (conflictingDictionaryBasename == null && currentDictionaryBasename == null) {
            return OverlapType.FULL;
        } else if (Objects.equals(conflictingDictionaryBasename, currentDictionaryBasename)) {
            return OverlapType.FULL;
        } else if (conflictingDictionaryBasename == null || currentDictionaryBasename == null) {
            return OverlapType.POTENTIAL;
        } else {
            return OverlapType.NONE;
        }
    }

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
            @NotNull Map<String, Message> messagePerLanguage = new LinkedHashMap<>(); // preserve the order of languages
            List<String> languages = dictionaryService.getLanguages(dictionaryResource);
            SortedSet<ValidationMessage> validationMessages = new TreeSet<>();
            for (String language : languages) {
                Message message;
                try {
                    message = dictionaryService.getMessages(dictionaryResource, language).get(key);
                } catch (DictionaryException e) {
                    throw new ResourceNotFoundException("Unable to get message entries for language '" + language + "' in dictionary '" + dictionaryPath + "'", e);
                }
                messagePerLanguage.put(language, message);
                if (message != null) {
                    validateItem(dictionaryResource, language, key, message.getText()).ifPresent(validationMessages::add);
                }
            }
            return new ValueMapResource(resourceResolver, path, RESOURCE_TYPE, new ValueMapDecorator(createResourceProperties(path, dictionaryService.isEditableDictionary(dictionaryResource), messagePerLanguage, validationMessages)));
        } else {
            throw new ResourceNotFoundException(path, "Unable to get underlying dictionary resource for path '" + dictionaryPath + "'");
        }
    }

    private Optional<ValidationMessage> validateItem(Resource dictionaryResource, String language, String key, String message) {
        Optional<Resource> conflictingDictionaryResource = dictionaryService.getConflictingDictionary(dictionaryResource, language, key);
        final ValidationMessage validationMessage;
        // check severity as well
        if (conflictingDictionaryResource.isPresent()) {
            // check if message is different from the one in the dictionary
            String otherMessage;
            try {
                otherMessage = dictionaryService.getMessages(conflictingDictionaryResource.get(), language).get(key).getText();
            } catch (DictionaryException e) {
                throw new IllegalStateException("Unable to get message entries for language '" + language + "' in dictionary '" + conflictingDictionaryResource.get().getPath() + "'", e);
            }
            if (Objects.equals(otherMessage, message)) {
                validationMessage = new ValidationMessage(ValidationMessage.Severity.INFO, language, "Conflicting dictionary at {0} for language {1}, it has the same message though", conflictingDictionaryResource.get().getPath(), language);
            } else {
                String conflictingDictionaryBasename = dictionaryService.getBasename(conflictingDictionaryResource.get());
                switch (checkBasenameOverlap(dictionaryService.getBasename(dictionaryResource), conflictingDictionaryBasename)) {
                    case PARTIAL:
                        validationMessage = new ValidationMessage(ValidationMessage.Severity.WARNING, language, "Conflicting dictionary at {0} for language {1} with another translation and partially overlapping basenames {2}", conflictingDictionaryResource.get().getPath(), language);
                        break;
                    case FULL:
                        validationMessage = new ValidationMessage(ValidationMessage.Severity.ERROR, language, "Conflicting dictionary at {0} for language {1} with another translation for same basenames {2}", conflictingDictionaryResource.get().getPath(), language, conflictingDictionaryBasename);
                        break;
                    case POTENTIAL:
                        validationMessage = new ValidationMessage(ValidationMessage.Severity.INFO, language, "Potential conflicting dictionary at {0} for language {1}, with another translation and potentially overlapping basenames (one side is null)", conflictingDictionaryResource.get().getPath(), language);
                        break;
                    default:
                        validationMessage = null;
                }
            }
        } else {
            validationMessage = null;
        }
        return Optional.ofNullable(validationMessage);
    }

    

    private static String extractKeyFromPath(@NotNull String path) {
        return Text.unescapeIllegalJcrChars(Text.getName(path));
    }

    public static @NotNull Map<String, Object> createResourceProperties(@NotNull String path, boolean isEditable, @NotNull Map<String, Message> messagePerLanguage, SortedSet<ValidationMessage> validationMessages) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(KEY, extractKeyFromPath(path));
        properties.put("path", path); // TODO: remove as it duplicates the resource path which is always available
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
        if (!validationMessages.isEmpty()) {
            properties.put(VALIDATION_MESSAGES, validationMessages);
        }
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
