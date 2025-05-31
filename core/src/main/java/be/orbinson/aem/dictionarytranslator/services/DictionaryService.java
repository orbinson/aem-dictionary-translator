package be.orbinson.aem.dictionarytranslator.services;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ProviderType;

import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;

/**
 * Low-level service interface for reading and creating/updating/deleting dictionaries in AEM.
 * <p>
 * A high-level read-only API is provided  by the {@link be.orbinson.aem.dictionarytranslator.services.impl.CombiningMessageEntryResourceProvider}.
 *
 */
@ProviderType
public interface DictionaryService {

    /** 
     * Returns all dictionaries present in the system. Leverages a query to find all dictionaries.
     * @param resourceResolver
     * @return all dictionaries present in the system, sorted by their language (i.e. the locale's language tag)
     */
    Collection<Dictionary> getAllDictionaries(ResourceResolver resourceResolver);

    /** 
     * Returns all dictionaries present in the system in a map where the key is the parent path of the dictionary and the value is a sorted map of dictionaries by locale.
     * Internally calls {@link #getAllDictionaries(ResourceResolver)}.
     * @param resourceResolver
     * @return a map where the key is the parent path of the dictionary and the value is a sorted map of dictionaries by locale.
     */
    Map<String, SortedMap<Locale, Dictionary>> getAllDictionariesByParentPath(ResourceResolver resourceResolver);

    /**
     * Returns the dictionary resource for the given parent path and language. 
     * May internally call {@link #getAllDictionaries(ResourceResolver)} in case no dictionaries with the given parent path and language are found in the cache.
     * @param resourceResolver
     * @param parentPath
     * @param language
     * @return the dictionary resource or an empty Optional if no dictionary was found
     */
    Optional<Dictionary> getDictionary(ResourceResolver resourceResolver, String parentPath, Locale language);

    /**
     * Returns all dictionaries present in the system which are direct children of the given parent path.
     * May internally call {@link #getAllDictionaries(ResourceResolver)} in case no dictionaries with the given parent path are found in the cache.
     * @param resourceResolver
     * @param parentPath
     * @return the dictionaries sorted by their language (i.e. the locale's language tag)
     */
    Collection<Dictionary> getDictionaries(ResourceResolver resourceResolver, String parentPath);

    /**
     * Returns all dictionaries present in the system which are direct children of the given parent path.
     * May internally call {@link #getAllDictionaries(ResourceResolver)} in case no dictionaries with the given parent path are found in the cache.
     * @param resourceResolver
     * @param parentPath
     * @return a map where the key is the locale of the dictionary and the value is the dictionary itself.
     */
   SortedMap<Locale, Dictionary> getDictionariesByLanguage(ResourceResolver resourceResolver, String parentPath);

    /**
     * Returns the dictionary having a lower or same ordinal, containing the given key in the given language. It does not evaluate the base name(s), though.
     * @param resourceResolver the resource resolver to use
     * @param dictionary the dictionary to check, must not be null
     * @param key the key to check
     * @return the dictionary resource having a higher or same precedence if it exists, otherwise an empty Optional
     */
    Optional<Dictionary> getConflictingDictionary(ResourceResolver resourceResolver, Dictionary dictionary, String key);

    // write operations
    /**
     * Creates a new dictionary parent resource for the given path containing one empty dictionary for each of the given languages.
     * Each dictionary is created with the given base names, if empty, the parent path is used as base name.
     * The changes are not persisted, so you need to call {@link ResourceResolver#commit()} afterwards.
     * @param resourceResolver
     * @param parentPath
     * @param languages
     * @throws PersistenceException
     * @throws DictionaryException in case at least one of the languages already exists below the parent path
     */
    void createDictionaries(ResourceResolver resourceResolver, String parentPath, Collection<Locale> languages, Collection<String> baseNames) throws PersistenceException, DictionaryException;

    /**
     * Deletes the resource at the given path (including all its children). This affects both dictionary as well as arbitrary other child resources.
     * The changes are not persisted, so you need to call {@link ResourceResolver#commit()} afterwards.
     * @param replicator
     * @param resourceResolver
     * @param path
     * @throws DictionaryException in case the path does not exist
     * @throws ReplicationException
     * @throws PersistenceException
     */
    void deleteDictionaries(Replicator replicator, ResourceResolver resourceResolver, String path) throws DictionaryException, ReplicationException, PersistenceException;

    /**
     * Deletes a dictionary for the given language b the given parent path. The changes are not persisted, so you need to call {@link ResourceResolver#commit()} afterwards.
     * @param replicator
     * @param resourceResolver
     * @param parentPath
     * @param language
     * @throws DictionaryException in case the container path does not exist or the language dictionary does not exist
     * @throws ReplicationException
     * @throws PersistenceException 
     */
    void deleteDictionary(Replicator replicator, ResourceResolver resourceResolver, String parentPath, Locale language) throws DictionaryException, ReplicationException, PersistenceException;

    /**
     * Creates a new dictionary for the given language below the given parent path. The changes are not persisted, so you need to call {@link ResourceResolver#commit()} afterwards.
     * @param resourceResolver the resource resolver to use, must not be null
     * @param parentPath the resource given through the parent path must exist, otherwise a {@link DictionaryException} is thrown. Must not be null.
     * @param language the language for which the dictionary is created, must not be null
     * @param baseNames the base names to use for the dictionary, if empty, the parent path is used as base name
     * @throws PersistenceException 
     * @throws DictionaryException in case the container path does not exist or the language dictionary already exists
     */
    void createDictionary(ResourceResolver resourceResolver, String parentPath, Locale language, Collection<String> baseNames) throws PersistenceException, DictionaryException;


}
