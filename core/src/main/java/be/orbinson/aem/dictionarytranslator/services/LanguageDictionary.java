package be.orbinson.aem.dictionarytranslator.services;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;

import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;

public interface LanguageDictionary {

    /**
     * Returns the path of the underlying dictionary resource.
     * @return the path of the underlying dictionary resource
     */
    String getPath();

    /**
     * Returns the language of this dictionary.
     * @return the language of this dictionary
     */
    Locale getLanguage();

    /**
     * Returns if the entries of this dictionary can be written as well.
     * @return true if the entries can be written with the given {@link ResourceResolver}, false otherwise
     * @see #createOrUpdateEntry(ResourceResolver, String, String)
     * @see #deleteEntry(Replicator, ResourceResolver, String)
     */
    boolean isEditable(ResourceResolver resourceResolver);

    /**
     * Returns the base name(s) of this dictionary.
     * @return the base names
     */
    Set<String> getBaseNames();

    enum Type {
        SLING_MESSAGE_ENTRY("sling:MessageEntry"),
        JSON_FILE("JSON file"),
        /** the languages below this dictionary have different types, only applicable on container level, not on language level */
        MIXED("Mixed");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** 
     * Returns the type of the dictionary resource.
     * @return the type of the dictionary resource
     */
    Type getType();

    /**
     * Returns the ordinal of the dictionary resource. The lower the ordinal, the higher the precedence.
     * The ordinal is determined by the path of the dictionary resource and follows the logic from
     * <a href="https://sling.apache.org/documentation/bundles/internationalization-support-i18n.html#resourcebundle-hierarchies">ResourceBundle hierarchies</a>.
     * The lowest ordinal is 0 and the highest depends on the number of search paths configured for the resource resolver.
     * @return the ordinal of the dictionary resource
     */
    int getOrdinal();

    /** 
     * Encapsulating a single message entry (for one language).
     * Exposes both the actual text as well as the resource path of the source message entry.
     * The latter is only available if the message is based on a single resource (and not based on a JSON file).
     */
    public final class Message {
        private final String text;
        private final String resourcePath;

        public Message(String message, String resourcePath) {
            this.text = message;
            this.resourcePath = resourcePath;
        }

        public String getText() {
            return text;
        }

        public Optional<String> getResourcePath() {
            return Optional.ofNullable(resourcePath);
        }

        @Override
        public String toString() {
            return "Message [text=" + text + ", resourcePath=" + resourcePath + "]";
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, resourcePath);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Message other = (Message) obj;
            return Objects.equals(text, other.text) && Objects.equals(resourcePath, other.resourcePath);
        }
    }

    /**
     * Returns all message entries for the given language below the given dictionary resource.
     * @param dictionaryResource
     * @param language
     * @return all message entries for the given language. The key of the map is the key of the message entry and the value is a {@link Message} object containing the actual text and other metadata.
     * @throws DictionaryException if the dictionary for the given language does not exist
     */
    Map<String, Message> getEntries() throws DictionaryException;

    /**
     * Either creates a new message entry or updates an existing one. The changes are not persisted until the resource resolver is committed.
     * @param resourceResolver
     * @param key
     * @param message
     * @throws PersistenceException in case creating a new resource failed
     * @throws DictionaryException in case the language does not exist below the given dictionary resource
     * @throws UnsupportedOperationException in case the dictionary is not editable
     * @see #isEditable()
     */
    void createOrUpdateEntry(ResourceResolver resourceResolver, String key, String message) throws PersistenceException, DictionaryException;

    /**
     * Deletes the message entry for the given language and key. The changes are not persisted until the resource resolver is committed.
     * However the message entry is immediately scheduled for deactivation.
     * @param replicator
     * @param resourceResolver
     * @param key
     * @throws PersistenceException in case deleting the resource failed
     * @throws ReplicationException in case deactivation failed
     * @throws DictionaryException if either language or key does not exist below the given dictionary resource
     * @throws UnsupportedOperationException in case the dictionary is not editable
     * @see #isEditable()
     */
    void deleteEntry(Replicator replicator, ResourceResolver resourceResolver, String key)  throws PersistenceException, ReplicationException, DictionaryException;

}
