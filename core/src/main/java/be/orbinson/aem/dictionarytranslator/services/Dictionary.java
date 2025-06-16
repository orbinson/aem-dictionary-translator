package be.orbinson.aem.dictionarytranslator.services;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ProviderType;

import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;

/**
 * Represents a dictionary resource subtree in AEM.
 * A dictionary is a resource subtree that contains message entries for a specific language.
 * @see <a href="https://sling.apache.org/documentation/bundles/internationalization-support-i18n.html">Sling i18n</a>
 */
@ProviderType
public interface Dictionary {

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
     * As the return value is not cached, and this method takes some time, this should be called only when really necessary.
     * @return true if the entries can be written with the given {@link ResourceResolver}, false otherwise
     * @see #createEntry(ResourceResolver, String, Optional)
     * @see #updateEntry(ResourceResolver, String, Optional)
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

        public Message(String text, String resourcePath) {
            this.text = text;
            this.resourcePath = resourcePath;
        }

        /**
         * 
         * @return the text of the message entry, or null if no message is set (in this case the key was still found, but no message was set)
         */
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
     * @return all message entries for the given language. The key of the map is the key of the message entry and the value is a {@link Message} object containing the actual text and other metadata.
     * @throws DictionaryException if the dictionary for the given language does not exist
     */
    Map<String, Message> getEntries() throws DictionaryException;

    /**
     * Creates a new message entry. The changes are not persisted until the resource resolver is committed.
     * @param resourceResolver
     * @param key
     * @param message the optional message. In case this is empty the according property will not be set on the message entry resource.
     * @throws PersistenceException in case creating a new resource failed
     * @throws DictionaryException in case the language does not exist below the given dictionary resource
     * @throws UnsupportedOperationException in case the dictionary is not editable
     * @see #isEditable(ResourceResolver)
     */
    void createEntry(ResourceResolver resourceResolver, String key, Optional<String> message) throws PersistenceException, DictionaryException;
    
    /**
     * Either creates a new message entry or updates an existing one. The changes are not persisted until the resource resolver is committed.
     * In general this operation will try to modify resources as less as possible in the dictionary resource subtree.
     * In case you always want to create a new message entry, use {@link #createEntry(ResourceResolver, String, Optional)}.
     * @param resourceResolver
     * @param key
     * @param message the optional message. If present a message entry will be created or updated, i.e. parent dictionaries will no longer be considered for this key. In case not present this is either a noop (in case the entry was not yet there) or just removes the translation from the entry.
     * @throws PersistenceException in case creating a new resource failed
     * @throws DictionaryException in case the language does not exist below the given dictionary resource
     * @throws UnsupportedOperationException in case the dictionary is not editable
     * @see #isEditable(ResourceResolver)
     */
    void updateEntry(ResourceResolver resourceResolver, String key, Optional<String> message) throws PersistenceException, DictionaryException;

    /**
     * Shortcut for {@link #updateEntry(ResourceResolver, String, Optional)} with the given message.
     * @param resourceResolver
     * @param key
     * @param message the message. Even if the message is empty, a message entry will be created or updated, i.e. parent dictionaries will no longer be considered for this key.
     * @throws PersistenceException in case creating a new resource failed
     * @throws DictionaryException in case the language does not exist below the given dictionary resource
     * @throws UnsupportedOperationException in case the dictionary is not editable
     * @see #isEditable(ResourceResolver)
     * @deprecated use {@link #updateEntry(ResourceResolver, String, Optional)} instead
     */
    @Deprecated
    void createOrUpdateEntry(ResourceResolver resourceResolver, String key, String message) throws PersistenceException, DictionaryException;

    /**
     * Deletes the message entry for the given language and key. The changes are not persisted until the resource resolver is committed.
     * However the message entry is immediately scheduled for deactivation.
     * This method will completely remove the message entry (not only the sling:message property).
     * @param replicator
     * @param resourceResolver
     * @param key
     * @throws PersistenceException in case deleting the resource failed
     * @throws ReplicationException in case deactivation failed
     * @throws DictionaryException if either language or key does not exist below the given dictionary resource
     * @throws UnsupportedOperationException in case the dictionary is not editable
     * @see #isEditable(ResourceResolver)
     */
    void deleteEntry(Replicator replicator, ResourceResolver resourceResolver, String key)  throws PersistenceException, ReplicationException, DictionaryException;

}
