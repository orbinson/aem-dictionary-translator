package be.orbinson.aem.dictionarytranslator.services.impl;

import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_KEY;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGE;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGEENTRY;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGE_MIXIN;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.MIX_LANGUAGE;
import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;
import static org.apache.jackrabbit.JcrConstants.JCR_LANGUAGE;
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.commons.json.JsonHandler;
import org.apache.jackrabbit.commons.json.JsonParser;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants;

@Component(property = { ResourceChangeListener.PATHS + "=/",
ResourceChangeListener.CHANGES + "=ADDED" ,
ResourceChangeListener.CHANGES + "=REMOVED",
ResourceChangeListener.CHANGES + "=CHANGED"})
public class DictionaryServiceImpl implements DictionaryService, ResourceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(DictionaryServiceImpl.class);
    private static final String SLING_BASENAME = "sling:basename";
    // same as https://github.com/apache/sling-org-apache-sling-i18n/blob/3f98ebf430e416226500c2975086423edc29dcb3/src/main/java/org/apache/sling/i18n/impl/JcrResourceBundle.java#L69
    static final String QUERY_LANGUAGE_ROOTS = "//element(*,mix:language)[@jcr:language]";

    @Reference
    private Replicator replicator;

    /**
     * Cache of language dictionaries, the key is an artifical path in the format {@code <dictionaryPath>/<language>}.
     * The cache for all languages is automatically invalidated when a resource below the dictionary path was changed.
     */
    private final Map<String, Map<String, Message>> messagesPerLanguageDictionary = new HashMap<>();

    public boolean isEditableDictionary(Resource resource) {
        String path = resource.getPath();
        if (getType(resource) != DictionaryType.SLING_MESSAGE_ENTRY) {
            return false;
        }
        Session session = resource.getResourceResolver().adaptTo(Session.class);
        if (session != null) {
            try {
                AccessControlManager accessControlManager = session.getAccessControlManager();
                Privilege[] privileges = new Privilege[]{
                        accessControlManager.privilegeFromName(Privilege.JCR_ADD_CHILD_NODES),
                        accessControlManager.privilegeFromName(Privilege.JCR_REMOVE_NODE),
                        accessControlManager.privilegeFromName("crx:replicate")
                };
                // https://jackrabbit.apache.org/oak/docs/nodestore/compositens.html#checking-for-read-only-access
                boolean isPathWritable= session.hasCapability("addNode", path, new Object[] { "nt:folder" });
                return isPathWritable && accessControlManager.hasPrivileges(path, privileges);
            } catch (RepositoryException e) {
                LOG.debug("Could not check if dictionary is editable, therefore assume it is not!", e);
                return false;
            }
        }
        return false;
    }

    @Override
    public int getOrdinal(Resource dictionaryResource) {
        final String[] searchPaths = dictionaryResource.getResourceResolver().getSearchPath();
        int i = 0;
        for (; i < searchPaths.length; i++) {
            if (dictionaryResource.getPath().startsWith(searchPaths[i])) {
                return i;
            }
        }
        return i;
    }

    public void addLanguage(Resource dictionary, String language, String basename) throws PersistenceException {
        Map<String, Object> properties = new HashMap<>();

        ResourceResolver resourceResolver = dictionary.getResourceResolver();

        properties.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, JcrResourceConstants.NT_SLING_FOLDER);
        properties.put(JCR_LANGUAGE, language);
        properties.put(JCR_MIXINTYPES, MIX_LANGUAGE);

        if (StringUtils.isNotEmpty(basename)) {
            properties.put(SLING_BASENAME, basename);
        } else {
            properties.put(SLING_BASENAME, dictionary.getPath());
        }

        LOG.debug("Add language '{}' to dictionary '{}' with properties '{}'", language, dictionary, properties);
        resourceResolver.create(dictionary, language, properties);
        resourceResolver.commit();
    }

    public @NotNull List<Resource> getDictionaries(ResourceResolver resourceResolver) {
        Map<String, Resource> result = new TreeMap<>();

        resourceResolver
                .findResources(QUERY_LANGUAGE_ROOTS + "/..", "xpath")
                .forEachRemaining(resource -> result.put(resource.getPath(), resource));

        return new ArrayList<>(result.values());
    }

    public void createDictionary(Resource parent, String name, String[] languages, String basename) throws PersistenceException {
        LOG.debug("Create dictionary '{}'", name);
        ResourceResolver resourceResolver = parent.getResourceResolver();
        String dictionaryPath = String.format("%s/%s/i18n", parent.getPath(), JcrUtil.createValidName(name));
        Resource dictionaryResource = ResourceUtil.getOrCreateResource(resourceResolver, dictionaryPath, JcrResourceConstants.NT_SLING_FOLDER, JcrResourceConstants.NT_SLING_FOLDER, true);

        for (String language : languages) {
            addLanguage(dictionaryResource, language, basename);
        }
    }

    @Override
    public void deleteDictionary(ResourceResolver resourceResolver, String dictionaryPath) throws DictionaryException, ReplicationException, PersistenceException {
        LOG.debug("Delete dictionary '{}'", dictionaryPath);
        final Resource dictionaryResource = resourceResolver.getResource(dictionaryPath);
        if (dictionaryResource != null) {
            replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.DEACTIVATE, dictionaryResource.getPath());
            resourceResolver.delete(dictionaryResource);
            resourceResolver.commit();
        } else {
            throw new DictionaryException("Dictionary '" + dictionaryPath + "' not found");
        }
    }

    public List<String> getLanguages(Resource dictionaryResource) {
        Set<String> result = new TreeSet<>();

        dictionaryResource.listChildren().forEachRemaining(child -> {
            ValueMap properties = child.getValueMap();
            // mixin check not implemented due to https://issues.apache.org/jira/browse/SLING-12779
            if (properties.containsKey(JCR_LANGUAGE) /* && hasMixinType(child, MIX_LANGUAGE)*/) {
                LOG.trace("Found language with path '{}'", child.getPath());
                result.add(properties.get(JCR_LANGUAGE, String.class));
            }
        });

        return new ArrayList<>(result);
    }

    @Override
    public void deleteLanguage(Resource dictionaryResource, String language) throws DictionaryException, ReplicationException, PersistenceException {
        Resource languageResource = getLanguageResource(dictionaryResource, language).orElseThrow(() -> new DictionaryException("Language resource not found for language \"" + language + "\" of dictionary \"" + dictionaryResource.getPath() + "\""));
        LOG.debug("Delete language '{}' from '{}'", language, dictionaryResource.getPath());
        ResourceResolver resourceResolver = dictionaryResource.getResourceResolver();
        replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.DEACTIVATE, languageResource.getPath());
        resourceResolver.delete(languageResource);
        resourceResolver.commit();
    }

    @Override
    public String getBasename(Resource dictionaryResource) {
        AtomicReference<String> basename = new AtomicReference<>();
        dictionaryResource.listChildren().forEachRemaining(child -> {
            ValueMap properties = child.getValueMap();
            if (properties.containsKey(JCR_LANGUAGE) && basename.get() == null) {
                LOG.trace("Found language with path '{}'", child.getPath());
                basename.set(properties.get(SLING_BASENAME, String.class));
            }
        });
        return basename.get();
    }

    /**
     * Gets the language resource based on the jcr:language property
     *
     * @param dictionaryResource The dictionary resource
     * @param language           The language
     * @return the language resource if it exists
     */
    private Optional<Resource> getLanguageResource(Resource dictionaryResource, String language) {
        Objects.requireNonNull(dictionaryResource, "dictionaryResource must not be null");
        for (Resource languageResource : dictionaryResource.getChildren()) {
            if (language.equals(languageResource.getValueMap().get(JcrConstants.JCR_LANGUAGE))) {
                return Optional.of(languageResource);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<String> getKeys(Resource dictionaryResource) throws DictionaryException {
        Set<String> keys = new TreeSet<>();
        // collect keys from all languages
        for (String language : getLanguages(dictionaryResource)) {
            keys.addAll(getMessages(dictionaryResource, language).keySet());
        }
        return List.copyOf(keys);
    }

    @Override
    public boolean keyExists(Resource dictionaryResource, String language, String key) {
        return internalGetMessages(dictionaryResource, language).map(m -> m.containsKey(key)).orElse(false);
    }

    @Override
    public void createOrUpdateMessageEntry(Resource dictionaryResource, String language, String key, String message) throws PersistenceException, DictionaryException {
        Resource languageResource = getLanguageResource(dictionaryResource, language).orElseThrow(() -> new DictionaryException("Language resource not found for language \"" + language + "\" of dictionary \"" + dictionaryResource.getPath() + "\""));
        Resource messageEntryResource = getOrCreateMessageEntryResource(languageResource, key);
        updateMessage(key, message, messageEntryResource);
    }

    private static void updateMessage(String key, String message, Resource messageEntryResource) throws PersistenceException {
        ValueMap valueMap = messageEntryResource.adaptTo(ModifiableValueMap.class);
        if (valueMap != null) {
            if (message.isBlank()) {
                valueMap.remove(SLING_MESSAGE);
            } else {
                valueMap.put(SLING_MESSAGE, message);
                if (StringUtils.isNotBlank(key)) {
                    valueMap.putIfAbsent(SLING_KEY, key);
                }
                LOG.trace("Updated message entry with name '{}' and message '{}' on path '{}'", messageEntryResource.getName(), message, messageEntryResource.getPath());
            }
        } else {
            throw new PersistenceException("Could not update message entry, resource at \"" + messageEntryResource.getPath() + "\" not adaptable to ModifiableValueMap");
        }
    }

    private Optional<Resource> getMessageEntryResource(Resource languageResource, String key) {
        Objects.requireNonNull(languageResource, "languageResource must not be null");
        Objects.requireNonNull(key, "key must not be null");
        // In order to speed up the search, we go for the default check where it is the escaped key as node name
        Resource messageEntryResource = languageResource.getChild(Text.escapeIllegalJcrChars(key));
        if (isMessageEntryResource(messageEntryResource)) {
            return Optional.of(messageEntryResource);
        }

        // Fall back to searching for the resource with sling:key as correct property
        for (Resource resource : languageResource.getChildren()) {
            if (key.equals(resource.getValueMap().get(DictionaryConstants.SLING_KEY)) && isMessageEntryResource(messageEntryResource)) {
                return Optional.of(resource);
            }
        }

        return Optional.empty();
    }

    @Override
    public DictionaryType getType(Resource dictionaryResource, String language) throws DictionaryException {
        Resource languageResource = getLanguageResource(dictionaryResource, language).orElseThrow(() -> new DictionaryException("Language resource not found for language \"" + language + "\" in dictionary \"" + dictionaryResource.getPath() + "\""));
        if (isJsonFileBasedDictionary(languageResource)) {
            return DictionaryType.JSON_FILE;
        } else {
            return DictionaryType.SLING_MESSAGE_ENTRY;
        }
    }

    @Override
    public DictionaryType getType(Resource dictionaryResource) {
        DictionaryType type = null;
        for (String language : getLanguages(dictionaryResource)) {
            try {
                DictionaryType newType = getType(dictionaryResource, language);
                if (type != null && type != newType) {
                    return DictionaryType.MIXED;
                } else {
                    type = newType;
                }
            } catch (DictionaryException e) {
                LOG.warn("Could not get dictionary type for language '{}' below '{}, skipping language", language, dictionaryResource.getPath(), e);
            }
        }
        if (type == null) {
            // for empty dictionaries, we return the default type
            return DictionaryType.SLING_MESSAGE_ENTRY;
        }
        return type;
    }

    @Override
    public void onChange(final @NotNull List<ResourceChange> changes) {
        for (final ResourceChange change : changes) {
            // always invalidate all languages of a dictionary
            synchronized(this) {
                messagesPerLanguageDictionary.keySet().removeIf(
                    key -> {
                        String dictionaryPath = Text.getRelativeParent(key, 1);
                        if (change.getPath().startsWith(dictionaryPath)) {
                            LOG.debug("Invalidating dictionary cache for path '{}'", dictionaryPath);
                            return true;
                        }
                        return false;
                    });
            }
        }
    }

    @Override
    public synchronized Map<String, Message> getMessages(Resource dictionaryResource, String language) throws DictionaryException {
        return internalGetMessages(dictionaryResource, language).orElseThrow(() -> new DictionaryException("No messages found for language \"" + language + "\" in dictionary \"" + dictionaryResource.getPath() + "\""));
    }

    private synchronized Optional<Map<String, Message>> internalGetMessages(Resource dictionaryResource, String language) {
        String cacheKey = dictionaryResource.getPath() + "/" + language;
        if (!messagesPerLanguageDictionary.containsKey(cacheKey)) {
            Map<String, Message> messages = loadMessages(dictionaryResource, language).orElse(null);
            // also cache non-existing languages to speed up the conflict check
            messagesPerLanguageDictionary.put(cacheKey, messages);
            return Optional.ofNullable(messages);
        } else {
            return Optional.ofNullable(messagesPerLanguageDictionary.get(cacheKey));
        }
    }

    private Optional<Map<String, Message>> loadMessages(Resource dictionaryResource, String language) {
        Resource resource = getLanguageResource(dictionaryResource, language).orElse(null);
        if (resource == null) {
            return Optional.empty();
        }
        if (isJsonFileBasedDictionary(resource)) {
            Map<String, Object> messages = new HashMap<>();
            loadJsonDictionary(resource.getChild(JCR_CONTENT), messages);
            return Optional.of(messages.entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, e -> new Message(e.getValue().toString(), null))));
        } else {
            Map<String, Message> messages = new HashMap<>();
            resource.listChildren().forEachRemaining(messageEntryResource -> {
                if (isMessageEntryResource(messageEntryResource)) {
                    String key = Optional.ofNullable(messageEntryResource.getValueMap().get(SLING_KEY, String.class))
                            .orElse(Text.unescapeIllegalJcrChars(messageEntryResource.getName()));
                    messages.put(key, new Message(messageEntryResource.getValueMap().get(SLING_MESSAGE, ""), messageEntryResource.getPath()));
                }
            });
            return Optional.of(messages);
        }
    }

    private boolean isJsonFileBasedDictionary(Resource languageResource) {
        return (languageResource.getName().endsWith(".json") && languageResource.getChild(JCR_CONTENT) != null);
    }

    // start copy of https://github.com/apache/sling-org-apache-sling-i18n/blob/3f98ebf430e416226500c2975086423edc29dcb3/src/main/java/org/apache/sling/i18n/impl/JcrResourceBundle.java#L245
    private void loadJsonDictionary(Resource resource, final Map<String, Object> targetDictionary) {
        LOG.info("Loading json dictionary: {}", resource.getPath());

        // use streaming parser (we don't need the dict in memory twice)
        JsonParser parser = new JsonParser(new JsonHandler() {

            private String key;

            @Override
            public void key(String key) throws IOException {
                this.key = key;
            }

            @Override
            public void value(String value) throws IOException {
                targetDictionary.put(key, value);
            }

            @Override
            public void object() throws IOException {}

            @Override
            public void endObject() throws IOException {}

            @Override
            public void array() throws IOException {}

            @Override
            public void endArray() throws IOException {}

            @Override
            public void value(boolean value) throws IOException {}

            @Override
            public void value(long value) throws IOException {}

            @Override
            public void value(double value) throws IOException {}
        });

        final InputStream stream = resource.adaptTo(InputStream.class);
        if (stream != null) {
            String encoding = "utf-8";
            final ResourceMetadata metadata = resource.getResourceMetadata();
            if (metadata.getCharacterEncoding() != null) {
                encoding = metadata.getCharacterEncoding();
            }

            try {

                parser.parse(stream, encoding);

            } catch (IOException e) {
                LOG.warn("Could not parse i18n json dictionary {}: {}", resource.getPath(), e.getMessage());
            } finally {
                try {
                    stream.close();
                } catch (IOException ignore) {
                }
            }
        } else {
            LOG.warn("Not a json file: {}", resource.getPath());
        }
    }

    private static boolean isMessageEntryResource(Resource messageEntryResource) {
        return messageEntryResource != null && (messageEntryResource.isResourceType(SLING_MESSAGEENTRY) ||
                hasMixinType(messageEntryResource, SLING_MESSAGE_MIXIN));
    }

    private static boolean hasMixinType(Resource resource, String mixinType) {
        return Arrays.asList(resource.getValueMap().get(JCR_MIXINTYPES, new String[0])).contains(mixinType);
    }

    @Override
    public void deleteMessageEntry(Resource dictionaryResource, String language, String key) throws PersistenceException, ReplicationException, DictionaryException {
        Resource languageResource = getLanguageResource(dictionaryResource, language).orElseThrow(() -> new DictionaryException("Language resource not found for language \"" + language + "\" of dictionary \"" + dictionaryResource.getPath() + "\""));
        Resource messageEntryResource = getMessageEntryResource(languageResource, key).orElseThrow(() -> new DictionaryException("Message entry resource not found for key \"" + key + "\" in language \"" + language + "\" of dictionary \"" + dictionaryResource.getPath() + "\""));
        replicator.replicate(dictionaryResource.getResourceResolver().adaptTo(Session.class), ReplicationActionType.DEACTIVATE, messageEntryResource.getPath());
        dictionaryResource.getResourceResolver().delete(messageEntryResource);
    }

    private @NotNull Resource getOrCreateMessageEntryResource(Resource languageResource, String key) throws PersistenceException {
        Optional<Resource> messageEntryResource = getMessageEntryResource(languageResource, key);
        if (messageEntryResource.isPresent()) {
            return messageEntryResource.get();
        } else {
            Resource newResource = languageResource.getResourceResolver().create(languageResource, Text.escapeIllegalJcrChars(key), Map.of(JCR_PRIMARYTYPE, SLING_MESSAGEENTRY));
            LOG.trace("Created message entry with key '{}' on path '{}'", key, newResource.getPath());
            return newResource;
        }
    }

    @Override
    public Optional<Resource> getConflictingDictionary(Resource dictionaryResource, String language, String key) {
        return getDictionaries(dictionaryResource.getResourceResolver()).stream()
                .filter(r -> !r.getPath().equals(dictionaryResource.getPath()))
                .filter(r -> getOrdinal(r) <= getOrdinal(dictionaryResource))
                .filter(r -> keyExists(r, language, key))
                .findFirst();
    }

}
