package be.orbinson.aem.dictionarytranslator.models.impl;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.models.Dictionary;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService.DictionaryType;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Model(
        adaptables = SlingHttpServletRequest.class,
        adapters = be.orbinson.aem.dictionarytranslator.models.Dictionary.class
)
public class DictionaryImpl implements Dictionary {
    @SlingObject
    private Resource resource;

    @OSGiService
    private DictionaryService dictionaryService;

    @Override
    public String getLanguageList() {
        return String.join(", ", this.getLanguages());
    }

    @Override
    public List<String> getLanguages() {
        return dictionaryService.getLanguages(resource);
    }

    @Override
    public boolean isEditable() {
        return dictionaryService.isEditableDictionary(resource);
    }

    @Override
    public @NotNull Resource getResource() {
        return resource;
    }

    @Override
    public String getBasename() {
        return dictionaryService.getBasename(resource);
    }

    @Override
    public int getKeyCount() throws DictionaryException {
        return getKeys().size();
    }

    @Override
    public List<String> getKeys() throws DictionaryException {
        return dictionaryService.getKeys(resource);
    }

    @Override
    public DictionaryType getType() {
        return dictionaryService.getType(resource);
    }

}
