package be.orbinson.aem.dictionarytranslator.services.impl;

import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.MIX_LANGUAGE;
import static org.apache.jackrabbit.JcrConstants.JCR_LANGUAGE;
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.jcr.Session;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.Dictionary;

@Component(property = { ResourceChangeListener.PATHS + "=/",
ResourceChangeListener.CHANGES + "=ADDED" ,
ResourceChangeListener.CHANGES + "=REMOVED",
ResourceChangeListener.CHANGES + "=CHANGED"})
public class DictionaryServiceImpl implements DictionaryService, ResourceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(DictionaryServiceImpl.class);
    // same as https://github.com/apache/sling-org-apache-sling-i18n/blob/3f98ebf430e416226500c2975086423edc29dcb3/src/main/java/org/apache/sling/i18n/impl/JcrResourceBundle.java#L69
    static final String QUERY_LANGUAGE_ROOTS = "//element(*,mix:language)[@jcr:language]";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private final boolean allowEmptyQueryResults;

    /**
     * Cache of dictionaries, the top level key the direct parent path of the dictionary resource.
     * The value is a map of locales to dictionaries.
     * The cache for all languages is automatically invalidated when a resource below the dictionary path was changed.
     */
    private final Map<String, SortedMap<Locale, Dictionary>> dictionaryMap = new HashMap<>();

    public DictionaryServiceImpl() {
        this(false);
    }

    public DictionaryServiceImpl(boolean allowEmptyQueryResults) {
        this.allowEmptyQueryResults = allowEmptyQueryResults;
    }
    private static final class LocaleComparator implements Comparator<Locale> {
        @Override
        public int compare(Locale o1, Locale o2) {
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            return o1.toLanguageTag().compareTo(o2.toLanguageTag());
        }
    }

    @Override
    public Collection<Dictionary> getAllDictionaries(ResourceResolver resourceResolver) {
        Iterator<Resource> iterator = resourceResolver
                .findResources(QUERY_LANGUAGE_ROOTS, "xpath");
        if (!allowEmptyQueryResults && !iterator.hasNext()) {
            throw new IllegalStateException("The query '" + QUERY_LANGUAGE_ROOTS + "' returned no results. This either means the according search index is corrupt/non-existing or the underlying resource resolver does not support queries  (when executed from tests)!");
        }
        iterator.forEachRemaining(resource -> {
            if (resource == null) {
                throw new IllegalStateException("Resource is null, this should not happen when using the query '" + QUERY_LANGUAGE_ROOTS + "'.");
            }
            // only create new dictionary instance if it is not already in the cache
            SortedMap<Locale, Dictionary> dictionaries = dictionaryMap.computeIfAbsent(resource.getParent().getPath(), (k) -> new TreeMap<>(new LocaleComparator()));
            Dictionary dictionary = getLanguageDictionary(resource);
            dictionaries.computeIfAbsent(dictionary.getLanguage(), (k) -> {
                // create dictionary
                LOG.debug("Creating dictionary for path '{}'", resource.getPath());
                return dictionary;
            });
        });
        return dictionaryMap.values().stream().flatMap(x -> x.values().stream()).collect(Collectors.toList());
    }

    @Override
    public Map<String, SortedMap<Locale, Dictionary>> getAllDictionariesByParentPath(ResourceResolver resourceResolver) {
        getAllDictionaries(resourceResolver);
        return dictionaryMap;
    }

    @Override
    public Collection<Dictionary> getDictionaries(ResourceResolver resourceResolver, String parentPath) {
        return getDictionariesByLanguage(resourceResolver, parentPath).values();
    }

    
    @Override
    public SortedMap<Locale, Dictionary> getDictionariesByLanguage(ResourceResolver resourceResolver, String parentPath) {
        SortedMap<Locale, Dictionary> dictionaries = internalGetDictionaries(parentPath);
        if (dictionaries.isEmpty()) {
            // always rely on search instead of traversal as there might be lots of child resources to traverse otherwise
            getAllDictionaries(resourceResolver);
            dictionaries = internalGetDictionaries(parentPath);
        }
        return dictionaries;
    }

    @Override
    public Optional<Dictionary> getDictionary(ResourceResolver resourceResolver, String parentPath, Locale language) {
        Optional<Dictionary> dictionary = internalGetDictionary(parentPath, language);
        if (dictionary.isEmpty()) {
            // always rely on search instead of traversal as reconstructing the path from just the parent and the locale is not possible
            getAllDictionaries(resourceResolver);
            dictionary = internalGetDictionary(parentPath, language);
            if (dictionary.isEmpty()) {
                // cache also the miss, so we don't have to search again
                SortedMap<Locale, Dictionary> dictionaries = dictionaryMap.computeIfAbsent(parentPath, (k) -> new TreeMap<>(new LocaleComparator()));
                dictionaries.put(language, null);
            }
        }
        return dictionary;
    }

    private Optional<Dictionary> internalGetDictionary(String parentPath, Locale language) {
        SortedMap<Locale, Dictionary> dictionaries = dictionaryMap.get(parentPath);
        if (dictionaries == null) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(dictionaries.get(language));
        }
    }

    private SortedMap<Locale, Dictionary> internalGetDictionaries(String parentPath) {
        // filter out null values, which are used to cache missing dictionaries
        return dictionaryMap.getOrDefault(parentPath, Collections.emptySortedMap()).entrySet().stream()
                .filter(e -> e.getValue() != null) // filter out null values
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (v1, v2) -> v1, () -> new TreeMap<>(new LocaleComparator())));
    }

    protected ResourceResolver createSystemReadResolver() {
        try {
            return resourceResolverFactory.getServiceResourceResolver(Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, "dictionary-service"));
        } catch (LoginException e) {
            // TODO: better exception
            throw new RuntimeException("Unable to create system read resolver", e);
        }
    }

    private Dictionary getLanguageDictionary(Resource resource) {
        if (JsonFileDictionary.isCompliant(resource)) {
            return new JsonFileDictionary(resource, this::createSystemReadResolver);
        } else {
            return new SlingMessageDictionaryImpl(resource, this::createSystemReadResolver);
        }
    }

    @Override
    public void createDictionary(ResourceResolver resourceResolver, String parentPath, Locale language, Collection<String> baseNames) throws PersistenceException, DictionaryException {
        if (parentPath == null || parentPath.isEmpty()) {
            throw new IllegalArgumentException("Parent path must not be null or empty");
        }
        Resource parentResource = resourceResolver.getResource(parentPath);
        if (parentResource == null) {
            throw new DictionaryException("Parent resource not found at path: " + parentPath);
        }
        // check for existing dictionary with same language below the parent path
        Optional<Dictionary> oldDictionary = getDictionary(resourceResolver, parentPath, language);
        if (oldDictionary.isPresent()) {
            throw new DictionaryException("Dictionary for language '" + language.toLanguageTag() + "' already exists at path: " + oldDictionary.get().getPath());
        }
        Map<String, Object> properties = new HashMap<>();

        properties.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, JcrResourceConstants.NT_SLING_FOLDER);
        properties.put(JCR_LANGUAGE, language.toLanguageTag().toLowerCase(Locale.ROOT));
        properties.put(JCR_MIXINTYPES, MIX_LANGUAGE);

        if (!baseNames.isEmpty()) {
            properties.put(DictionaryImpl.SLING_BASENAME, baseNames.toArray(new String[0]));
        } else {
            properties.put(DictionaryImpl.SLING_BASENAME, parentResource.getPath());
        }

        LOG.debug("Add dictionary with with properties '{}' to parent resource {}", properties, parentResource.getPath());
        resourceResolver.create(parentResource, language.toLanguageTag().toLowerCase(Locale.ROOT), properties);
    }

    @Override
    public void createDictionaries(ResourceResolver resourceResolver, String parentPath, Collection<Locale> languages, Collection<String> basenames) throws PersistenceException, DictionaryException {
        LOG.debug("Create dictionaries below '{}'", parentPath);
        Resource containerResource = ResourceUtil.getOrCreateResource(resourceResolver, parentPath, JcrResourceConstants.NT_SLING_FOLDER, JcrResourceConstants.NT_SLING_FOLDER, false);

        for (Locale language : languages) {
            createDictionary(resourceResolver, containerResource.getPath(), language, basenames);
        }
    }

    @Override
    public void deleteDictionaries(Replicator replicator, ResourceResolver resourceResolver, String parentPath) throws DictionaryException, ReplicationException, PersistenceException {
        LOG.debug("Delete dictionaries below '{}'", parentPath);
        final Resource dictionaryResource = resourceResolver.getResource(parentPath);
        if (dictionaryResource != null) {
            replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.DEACTIVATE, dictionaryResource.getPath());
            resourceResolver.delete(dictionaryResource);
        } else {
            throw new DictionaryException("Resource at '" + parentPath + "' not found");
        }
    }

    @Override
    public void deleteDictionary(Replicator replicator, ResourceResolver resourceResolver, String parentPath, Locale language) throws DictionaryException, ReplicationException, PersistenceException {
        Dictionary dictionary = getDictionary(resourceResolver, parentPath, language).orElseThrow(() -> new DictionaryException("Dictionary not found for parent path '" + parentPath + "' and language '" + language + "'"));
        Resource dictionaryResource = resourceResolver.getResource(dictionary.getPath());
        LOG.debug("Delete language '{}' from '{}'", language, dictionaryResource.getPath());
        replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.DEACTIVATE, dictionaryResource.getPath());
        resourceResolver.delete(dictionaryResource);
    }

    @Override
    public void onChange(final @NotNull List<ResourceChange> changes) {
        for (final ResourceChange change : changes) {
            // always invalidate all languages of a dictionary
            synchronized(this) {
                dictionaryMap.keySet().removeIf(
                    path -> {
                        if (change.getPath().startsWith(path)) {
                            LOG.debug("Invalidating dictionary cache below path '{}'", path);
                            return true;
                        }
                        return false;
                    });
            }
        }
    }

    @Override
    public Optional<Dictionary> getConflictingDictionary(ResourceResolver resourceResolver, Dictionary dictionary, String key) {
        return getAllDictionaries(resourceResolver).stream()
                .filter(d-> !d.getPath().equals(dictionary.getPath()))
                .filter(d -> d.getLanguage().equals(dictionary.getLanguage()))
                .filter(d -> d.getOrdinal() <= dictionary.getOrdinal())
                .filter(d -> {
                    try {
                        return d.getEntries().containsKey(key);
                    } catch (DictionaryException e) {
                        return false; // if we cannot get the entries, we assume it is not a conflicting dictionary
                    }
                })
                .findFirst();
    }

}
