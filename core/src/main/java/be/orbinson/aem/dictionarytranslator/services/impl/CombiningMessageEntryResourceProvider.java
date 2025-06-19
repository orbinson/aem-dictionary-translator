package be.orbinson.aem.dictionarytranslator.services.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.text.translate.AggregateTranslator;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.JavaUnicodeEscaper;
import org.apache.commons.lang3.text.translate.UnicodeUnescaper;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.ui.components.ds.ValueMapResource;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.Dictionary;
import be.orbinson.aem.dictionarytranslator.services.Dictionary.Message;

/**
 * Resource provider exposing all translations for one dictionary entry.
 * It expects the path to be of the form:
 * {@code /mnt/dictionary/<dictionaryPath>/<key>}
 * <p>
 * The key is unescaped with {@link UnicodeUnescaper} to also allow "/" in it.
 * This resource provider is used to combine message entries from different languages into a single resource.
 * It is used in the AEM UI to display the combined message entries for a given key.
 */
@Component(
        service = { ResourceProvider.class, CombiningMessageEntryResourceProvider.class },
        property = {
                ResourceProvider.PROPERTY_ROOT + "=" + CombiningMessageEntryResourceProvider.ROOT
        }
)
@Designate(ocd = CombiningMessageEntryResourceProvider.Config.class)
public class CombiningMessageEntryResourceProvider extends ResourceProvider<Object> {

    private static final Logger LOG = LoggerFactory.getLogger(CombiningMessageEntryResourceProvider.class);

    @ObjectClassDefinition(
            name = "Orbinson AEM Dictionary Translator - Combining Message Entry Resource Provider",
            description = "Configures aspects which affect the maintenance UI of individual dictionaries.")
    public @interface Config {
        @AttributeDefinition(
                name = "Enable validation",
                description = "Exposes information on conflicting items. Enabling this may have a negative impact on performance.")
        boolean enableValidation() default false;
    }

    enum OverlapType {
        NONE,
        PARTIAL,
        FULL,
        POTENTIAL
    }

    public static final class ValidationMessage implements Comparable<ValidationMessage> {
        public enum Severity {
            ERROR("error"), // most severe should have lowest ordinal
            WARNING("warning"),
            INFO("info");

            private final String label;

            Severity(String label) {
                this.label = label;
            }

            @Override
            public String toString() {
                return label;
            }
        }

        private final Locale language;
        private final String i18nKey;
        private final String[] arguments;
        private final Severity severity;

        public ValidationMessage(Severity severity, Locale language, String i18nKey, String... arguments) {
            this.severity = severity;
            this.language = language;
            this.i18nKey = i18nKey;
            this.arguments = arguments;
        }

        public Locale getLanguage() {
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
            // must be consistent with equals
            // lowest ordinal should be first
            int result = this.severity.compareTo(o.severity);
            if (result == 0) {
                result = 1;
            }
            return result;
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

    /* Properties exposed in the combining message entry resource */
    public static final String KEY = "key";
    public static final String ESCAPED_KEY = "escapedKey"; // exposed in UI which does not deal well with newlines
    public static final String DICTIONARY_PATH = "dictionaryPath";
    public static final String LANGUAGES = "languages";
    public static final String MESSAGE_ENTRY_PATHS = "messageEntryPaths";
    public static final String VALIDATION_MESSAGES = "validationMessages";

    private final DictionaryService dictionaryService;
    private final Config config;

    @Activate
    public CombiningMessageEntryResourceProvider(@Reference DictionaryService dictionaryService, Config config) {
        this.dictionaryService = dictionaryService;
        this.config = config;
    }

    public boolean isValidationEnabled() {
        return config.enableValidation();
    }

    static OverlapType checkBasenameOverlap(Set<String> baseNames, Set<String> conflictingDictionaryBaseNames) {
        if (conflictingDictionaryBaseNames == null && baseNames == null) {
            return OverlapType.FULL;
        } else if (Objects.equals(conflictingDictionaryBaseNames, baseNames)) {
            return OverlapType.FULL;
        } else if (conflictingDictionaryBaseNames == null || baseNames == null) {
            return OverlapType.POTENTIAL;
        } else if (baseNames.stream().anyMatch(conflictingDictionaryBaseNames::contains)) {
            return OverlapType.PARTIAL;
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
        Map<Locale, Message> messagePerLanguage = new LinkedHashMap<>();
        SortedSet<ValidationMessage> validationMessages = new TreeSet<>();
        Collection<Dictionary> dictionaries = dictionaryService.getDictionaries(resourceResolver, dictionaryPath);
        // only calculate editable flag once, as it is most likely the same for all sibling dictionaries and calling it is time consuming
        boolean isEditable = dictionaries.stream().findFirst().map(dictionary -> dictionary.isEditable(resourceResolver)).orElse(false);
        for (Dictionary dictionary : dictionaries) {
            try {
                Message message = dictionary.getEntries().get(key);
                if (message != null && config.enableValidation()) {
                    validateItem(resourceResolver, dictionary, key, message.getText()).ifPresent(validationMessages::add);
                }
                messagePerLanguage.put(dictionary.getLanguage(), message);
            } catch (DictionaryException e) {
                LOG.warn("Error retrieving message entries for dictionary at '" + dictionaryPath + "', skipping it", e);
            }
        }
        if (messagePerLanguage.isEmpty()) {
            return null;
        }
        return new ValueMapResource(resourceResolver, path, RESOURCE_TYPE, new ValueMapDecorator(createResourceProperties(path, isEditable, messagePerLanguage, config.enableValidation() ? Optional.of(validationMessages) : Optional.empty())));
    }

    private Optional<ValidationMessage> validateItem(ResourceResolver resourceResolver, Dictionary dictionary, String key, String message) {
        Dictionary conflictingDictionary = dictionaryService.getConflictingDictionary(resourceResolver, dictionary, key).orElse(null);
        final ValidationMessage validationMessage;
        if (conflictingDictionary != null) {
            // check if message is different from the one in the dictionary
            String otherMessage;
            try {
                otherMessage = conflictingDictionary.getEntries().get(key).getText();
            } catch (DictionaryException e) {
                throw new IllegalStateException("Unable to get message entries for in dictionary '" + conflictingDictionary.getPath() + "'", e);
            }
            if (Objects.equals(otherMessage, message)) {
                validationMessage = new ValidationMessage(ValidationMessage.Severity.INFO, dictionary.getLanguage(), "Conflicting dictionary at \"{0}\" for language {1}, it has the same message though.", conflictingDictionary.getPath(), dictionary.getLanguage().toLanguageTag());
            } else {
                Set<String> conflictingDictionaryBasenames = conflictingDictionary.getBaseNames();
                switch (checkBasenameOverlap(dictionary.getBaseNames(), conflictingDictionaryBasenames)) {
                    case PARTIAL:
                        validationMessage = new ValidationMessage(ValidationMessage.Severity.WARNING, dictionary.getLanguage(), "Conflicting dictionary at \"{0}\" for language {1} with another translation and partially overlapping basenames {2}.", conflictingDictionary.getPath(), dictionary.getLanguage().toLanguageTag(), String.join(", ", conflictingDictionaryBasenames));
                        break;
                    case FULL:
                        validationMessage = new ValidationMessage(ValidationMessage.Severity.ERROR, dictionary.getLanguage(), "Conflicting dictionary at \"{0}\" for language {1} with another translation for same basenames.", conflictingDictionary.getPath(), dictionary.getLanguage().toLanguageTag());
                        break;
                    case POTENTIAL:
                        validationMessage = new ValidationMessage(ValidationMessage.Severity.WARNING, dictionary.getLanguage(), "Potential conflicting dictionary at \"{0}\" for language {1} with another translation and potentially overlapping basenames (one side is null).", conflictingDictionary.getPath(), dictionary.getLanguage().toLanguageTag());
                        break;
                    default:
                        validationMessage = null;
                }
            }
        } else {
            // TODO: check super languages
            validationMessage = null;
        }
        return Optional.ofNullable(validationMessage);
    }

    static String extractKeyFromPath(@NotNull String path) {
        return new UnicodeUnescaper().translate(Text.getName(path));
    }

    /**
     * Creates a resource path for the given dictionary path and key.
     * The key is escaped with the help of unicode escape sequences for {@code /}, {@code %}, {@code .}, {@code \n} and {@code \r} potentially in order to
     * <ul>
     * <li>allow to reconstruct the dictionary path from the combined message entry path</li>
     * <li>prevent clashes with the relative path selectors {@code .} and {@code ..} which are used in the Sling API to refer to the current and parent resource</li>
     * <li>prevent the URITemplate heuristic in {@code /libs/clientlibs/granite/uritemplate/URITemplate.js#isEncoded(String)} from not applying URL encoding to such values if used in request parameters</li>
     * <li>prevent emitting of characters in HTML which may be modified through the browser (new lines in data attributes)</li>
     * </ul>
     *
     * @param dictionaryPath the dictionary path
     * @param key            the key
     * @return the path
     */
    public static String createPath(String dictionaryPath, String key) {
        if (!dictionaryPath.startsWith("/")) {
            throw new IllegalArgumentException("dictionaryPath must start with a slash (i.e. must be absolute)");
        }
        // does the key conflict with a relative path selector "." or ".."?
        final CharSequenceTranslator translator;
        if (key.chars().allMatch(c -> c == '.')) {
            translator = JavaUnicodeEscaper.between('.', '.');
        } else {
            translator = new AggregateTranslator(JavaUnicodeEscaper.between('%', '%'), JavaUnicodeEscaper.between('/', '/'), JavaUnicodeEscaper.between('\n', '\n'), JavaUnicodeEscaper.between('\r', '\r'));
        }
        return ROOT + dictionaryPath + "/" + translator.translate(key);
    }

    public static String escapeKey(String key) {
        // just expose the key in a form which makes whitespace characters visible
        return key.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t").replace(" ", "\u00B7");
    }

    public static @NotNull Map<String, Object> createResourceProperties(@NotNull String path, boolean isEditable, @NotNull Map<Locale, Message> messagePerLanguage, Optional<SortedSet<ValidationMessage>> validationMessages) {
        Map<String, Object> properties = new HashMap<>();
        String key = extractKeyFromPath(path);
        properties.put(KEY, key);
        properties.put(ESCAPED_KEY, escapeKey(key));
        properties.put("editable", isEditable);
        properties.put(DICTIONARY_PATH, getDictionaryPath(path));
        properties.put(LANGUAGES, messagePerLanguage.keySet().toArray(Locale[]::new));
        List<String> messageEntryPaths = new ArrayList<>();
        for (Entry<Locale, Message> messageEntryPerLanguageEntry : messagePerLanguage.entrySet()) {
            Message message = messageEntryPerLanguageEntry.getValue();
            final String text;
            if (message != null) {
                text = message.getText();
                message.getResourcePath().ifPresent(messageEntryPaths::add);
                if (text != null) {
                    // if no text is maintained, the message entry is there but contains no translation text -> treat visually the same as not existing
                    properties.put(messageEntryPerLanguageEntry.getKey().toLanguageTag(), text);
                }
            }
        }
        // all paths to replicate
        properties.put(MESSAGE_ENTRY_PATHS, messageEntryPaths);
        validationMessages.ifPresent(messages ->
            properties.put(VALIDATION_MESSAGES, messages)
        );
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
