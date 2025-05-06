package be.orbinson.aem.dictionarytranslator.models;

import java.util.List;
import java.util.Optional;

import org.apache.sling.api.resource.Resource;

public interface Dictionary {
    Resource getResource();

    String getLanguageList();

    String getBasename();

    List<String> getLanguages();

    List<String> getKeys();

    boolean isEditable();

    int getKeyCount();

    /**
     * Returns the dictionary resource having a lower or same ordinal, containing the given key and language and having at least one overlapping basename.
     * @param key
     * @param language
     * @return the dictionary resource having a higher or same precedence if it exists, otherwise an empty Optional
     */
    Optional<Resource> getConflictingDictionary(String key, String language);
}
