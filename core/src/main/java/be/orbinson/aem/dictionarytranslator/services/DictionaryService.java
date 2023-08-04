package be.orbinson.aem.dictionarytranslator.services;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;
import java.util.Map;

public interface DictionaryService {
    Map<String, String> getLanguagesForPath(ResourceResolver resourceResolver, String path);

    List<Resource> getDictionaries(ResourceResolver resourceResolver);

    List<String> getLanguages(Resource resource);

    String getBasename(Resource dictionaryResource);

    void createDictionary(Resource parent, String name, String[] languages, String basename) throws PersistenceException;

    void addLanguage(Resource dictionary, String language, String basename) throws PersistenceException;
}
