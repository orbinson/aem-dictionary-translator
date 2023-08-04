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
import java.util.*;

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
        return resource.getPath().startsWith("/content");
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
    public int getLabelCount() {
        return getKeys().size();
    }

    @Override
    public List<String> getKeys() {
        Set<String> keys = new TreeSet<>();
        for (String language : dictionaryService.getLanguages(resource)) {
            Resource child = resource.getChild(language);
            if (child != null) {
                child.listChildren().forEachRemaining(item -> addKey(keys, item));
            }
        }
        return List.copyOf(keys);
    }

    private static void addKey(Set<String> keys, Resource item) {
        if (item.isResourceType("sling:MessageEntry")) {
            keys.add(item.getName());
        }
    }
}
