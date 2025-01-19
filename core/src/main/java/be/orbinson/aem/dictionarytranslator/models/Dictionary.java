package be.orbinson.aem.dictionarytranslator.models;

import org.apache.sling.api.resource.Resource;

import java.util.List;

public interface Dictionary {
    Resource getResource();

    String getLanguageList();

    String getBasename();

    List<String> getLanguages();

    List<String> getKeys();

    boolean isEditable();

    int getKeyCount();
}
