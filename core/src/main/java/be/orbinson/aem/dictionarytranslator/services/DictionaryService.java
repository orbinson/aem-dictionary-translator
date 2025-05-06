package be.orbinson.aem.dictionarytranslator.services;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import com.day.cq.replication.ReplicationException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.RepositoryException;
import java.util.List;

public interface DictionaryService {

    boolean isEditableDictionary(Resource resource);
    
    /**
     * Returns the ordinal of the dictionary resource. The lower the ordinal, the higher the precedence.
     * The ordinal is determined by the path of the dictionary resource and follows the logic from
     * <a href="https://sling.apache.org/documentation/bundles/internationalization-support-i18n.html#resourcebundle-hierarchies">ResourceBundle hierarchies</a>.
     * @param dictionaryResource the dictionary resource
     * @return the ordinal of the dictionary resource
     */
    int getOrdinal(Resource dictionaryResource);

    List<Resource> getDictionaries(ResourceResolver resourceResolver);

    void createDictionary(Resource parent, String name, String[] languages, String basename) throws PersistenceException;

    void deleteDictionary(ResourceResolver resourceResolver, String dictionaryPath) throws DictionaryException;

    List<String> getLanguages(Resource dictionaryResource);

    void deleteLanguage(ResourceResolver resourceResolver, Resource dictionaryResource, String language) throws DictionaryException;

    void addLanguage(Resource dictionaryResource, String language, String basename) throws PersistenceException;

    Resource getLanguageResource(Resource dictionaryResource, String language);

    String getBasename(Resource dictionaryResource);

    List<String> getKeys(Resource dictionaryResource);

    boolean keyExists(Resource dictionaryResource, String language, String key);

    Resource getMessageEntryResource(Resource languageResource, String key);

    void createMessageEntry(ResourceResolver resourceResolver, Resource dictionaryResource, String language, String key, String message) throws PersistenceException;

    void updateMessageEntry(ResourceResolver resourceResolver, Resource dictionaryResource, String language, String key, String message) throws PersistenceException, RepositoryException;

    void deleteMessageEntry(ResourceResolver resourceResolver, Resource combiningMessageEntryResource) throws PersistenceException, ReplicationException;
}
