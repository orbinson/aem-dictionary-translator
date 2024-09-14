package be.orbinson.aem.dictionarytranslator.services;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.jetbrains.annotations.Nullable;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Map;

public interface DictionaryService {

    List<Resource> getDictionaries(ResourceResolver resourceResolver);

    void createDictionary(Resource parent, String name, String[] languages, String basename) throws PersistenceException;

    void deleteDictionary(ResourceResolver resourceResolver, String dictionaryPath) throws DictionaryException;

    List<String> getLanguages(Resource dictionaryResource);

    void deleteLanguage(ResourceResolver resourceResolver, Resource dictionaryResource, String language) throws DictionaryException;

    void addLanguage(Resource dictionary, String language, String basename) throws PersistenceException;

    @Nullable
    Resource getLanguageResource(Resource dictionaryResource, String language);

    Map<String, String> getLanguagesForPath(ResourceResolver resourceResolver, String dictionaryPath);

    String getBasename(Resource dictionaryResource);

    List<String> getLabelKeys(Resource dictionaryResource);

    boolean labelExists(Resource dictionaryResource, String language, String key);

    Resource getLabelResource(Resource languageResource, String key);

    void createLabel(ResourceResolver resourceResolver, Resource dictionaryResource, String language, String key, String message) throws PersistenceException;

    void updateLabel(ResourceResolver resourceResolver, Resource dictionaryResource, String language, String key, String message) throws PersistenceException, RepositoryException;

}
