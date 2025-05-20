package be.orbinson.aem.dictionarytranslator.models;

import java.util.List;

import org.apache.sling.api.resource.Resource;

import org.osgi.annotation.versioning.ProviderType;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;

@ProviderType
public interface Dictionary {
    Resource getResource();

    String getLanguageList();

    String getBasename();

    List<String> getLanguages();

    List<String> getKeys() throws DictionaryException;

    boolean isEditable();

    int getKeyCount() throws DictionaryException;
    
    DictionaryService.DictionaryType getType();

}
