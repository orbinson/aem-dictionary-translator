package be.orbinson.aem.dictionarytranslator.services;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import com.day.cq.replication.ReplicationException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Map;

public interface DictionaryService {

    boolean isEditableDictionary(Resource resource);

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
