package be.orbinson.aem.dictionarytranslator.models;

import org.apache.sling.api.resource.Resource;

import java.util.Calendar;
import java.util.List;

public interface Dictionary {
    Resource getResource();

    Calendar getCreated();

    String getLanguageList();

    String getLastModifiedFormatted();

    String getBasename();

    Calendar getLastModified();

    List<String> getLanguages();

    List<String> getKeys();

    boolean isEditable();

    int getKeyCount();
}
