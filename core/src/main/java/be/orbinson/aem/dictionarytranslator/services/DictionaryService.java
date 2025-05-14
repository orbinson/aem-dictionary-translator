package be.orbinson.aem.dictionarytranslator.services;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ProviderType;

import com.day.cq.replication.ReplicationException;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;

/**
 * Low-level service interface for reading and creating/updating/deleting dictionaries in AEM.
 * <p>
 * A high-level read-only API is provided by the {@link be.orbinson.aem.dictionarytranslator.models.Dictionary} model and by the {@link be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider}.
 *
 */
@ProviderType
public interface DictionaryService {

    boolean isEditableDictionary(Resource dictionaryResource);

    List<Resource> getDictionaries(ResourceResolver resourceResolver);

    void createDictionary(Resource parent, String name, String[] languages, String basename) throws PersistenceException;

    void deleteDictionary(ResourceResolver resourceResolver, String dictionaryPath) throws DictionaryException, ReplicationException, PersistenceException;

    List<String> getLanguages(Resource dictionaryResource);

    void deleteLanguage(Resource dictionaryResource, String language) throws DictionaryException, ReplicationException, PersistenceException;

    void addLanguage(Resource dictionaryResource, String language, String basename) throws PersistenceException;

    String getBasename(Resource dictionaryResource);

    List<String> getKeys(Resource dictionaryResource) throws DictionaryException;

    boolean keyExists(Resource dictionaryResource, String language, String key) throws DictionaryException;

    /**
     * Either creates a new message entry or updates an existing one. The changes are not persisted until the resource resolver is committed.
     * @param dictionaryResource
     * @param language
     * @param key
     * @param message
     * @throws PersistenceException
     * @throws DictionaryException 
     */
    void createOrUpdateMessageEntry(Resource dictionaryResource, String language, String key, String message) throws PersistenceException, DictionaryException;

    /**
     * Deletes the message entry for the given language and key. The changes are not persisted until the resource resolver is committed.
     * However the message entry is immediately scheduled for deactivation.
     * @param dictionaryResource
     * @param language
     * @param key
     * @throws PersistenceException
     * @throws ReplicationException
     * @throws DictionaryException 
     */
    void deleteMessageEntry(Resource dictionaryResource, String language, String key)  throws PersistenceException, ReplicationException, DictionaryException;

    Map<String, Message> getMessages(Resource dictionaryResource, String language) throws DictionaryException;

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

    enum DictionaryType {
        SLING_MESSAGE_ENTRY("sling:MessageEntry"),
        JSON_FILE("JSON file"),
        /** the languages below this dictionary have different types, only applicable on dictionary level, not on language level */
        MIXED("Mixed");

        private final String label;

        DictionaryType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Returns the type of the dictionary for the given language.
     * @param dictionaryResource
     * @param language
     * @return the type of the dictionary for the given language
     * @throws DictionaryException if the dictionary for the given language does not exist
     */
    DictionaryType getType(Resource dictionaryResource, String language) throws DictionaryException;

    /**
     * Returns the type of the dictionary (actually the derived type from the type of all languages).
     * <p>
     * This method will return {@link DictionaryType#MIXED} if the dictionary contains languages of different types and 
     * {@link DictionaryType#SLING_MESSAGE_ENTRY} if the dictionary doesn't contain any languages yet.
     *
     * @param dictionaryResource the dictionary resource
     * @return the derived type of the dictionary
     */
    DictionaryType getType(Resource dictionaryResource);

}
