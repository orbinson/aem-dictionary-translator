package be.orbinson.aem.dictionarytranslator.models.impl;

import be.orbinson.aem.dictionarytranslator.models.Dictionary;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Named;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Model(
        adaptables = SlingHttpServletRequest.class,
        adapters = be.orbinson.aem.dictionarytranslator.models.Dictionary.class
)
public class DictionaryImpl implements Dictionary {

    @SlingObject
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    @Named("jcr:created")
    private Calendar created;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    @Named("jcr:lastModified")
    private Calendar lastModified;

    @OSGiService
    private DictionaryService dictionaryService;

    @Override
    public String getLanguageList() {
        return String.join(", ", this.getLanguages());
    }

    private String getDateString(Date date) {
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, request.getLocale());
        return formatter.format(date);
    }

    @Override
    public String getLastModifiedFormatted() {
        return getDateString(getLastModified().getTime());
    }

    @Override
    public Calendar getLastModified() {
        return lastModified == null ? getCreated() : lastModified;
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
    public @Nullable Calendar getCreated() {
        return created;
    }

    @Override
    public String getBasename() {
        return dictionaryService.getBasename(resource);
    }

    @Override
    public int getKeyCount() {
        return getKeys().size();
    }

    @Override
    public List<String> getKeys() {
        return dictionaryService.getKeys(resource);
    }
}
