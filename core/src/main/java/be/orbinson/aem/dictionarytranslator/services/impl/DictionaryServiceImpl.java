package be.orbinson.aem.dictionarytranslator.services.impl;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_KEY;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGE;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGEENTRY;
import static org.apache.jackrabbit.JcrConstants.JCR_LANGUAGE;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

@Component
public class DictionaryServiceImpl implements DictionaryService {

    private static final Logger LOG = LoggerFactory.getLogger(DictionaryServiceImpl.class);
    private static final String SLING_BASENAME = "sling:basename";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Replicator replicator;

    public boolean isEditableDictionary(Resource resource) {
        String path = resource.getPath();
        Session session = resource.getResourceResolver().adaptTo(Session.class);
        if (session != null) {
            try {
                AccessControlManager accessControlManager = session.getAccessControlManager();
                Privilege[] privileges = new Privilege[]{
                        accessControlManager.privilegeFromName(Privilege.JCR_ADD_CHILD_NODES),
                        accessControlManager.privilegeFromName(Privilege.JCR_REMOVE_NODE),
                        accessControlManager.privilegeFromName("crx:replicate")
                };
                return accessControlManager.hasPrivileges(path, privileges);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public void addLanguage(Resource dictionary, String language, String basename) throws PersistenceException {
        Map<String, Object> properties = new HashMap<>();

        ResourceResolver resourceResolver = dictionary.getResourceResolver();

        properties.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, JcrResourceConstants.NT_SLING_FOLDER);
        properties.put("jcr:language", language);
        properties.put("jcr:mixinTypes", "mix:language");

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
                .findResources("//element(*, mix:language)[@jcr:language and (@jcr:primaryType='sling:Folder' or @jcr:primaryType='nt:folder')]/..", "xpath")
                .forEachRemaining(resource -> result.put(resource.getPath(), resource));

        return new ArrayList<>(result.values());
    }

    public void createDictionary(Resource parent, String name, String[] languages, String basename) throws PersistenceException {
        LOG.debug("Create dictionary '{}'", name);
        ResourceResolver resourceResolver = parent.getResourceResolver();
        String dictionaryPath = String.format("%s/%s/i18n", parent.getPath(), JcrUtil.createValidName(name));
        Resource dictionaryResource = ResourceUtil.getOrCreateResource(resourceResolver, dictionaryPath, "sling:Folder", "sling:Folder", true);

        for (String language : languages) {
            addLanguage(dictionaryResource, language, basename);
        }
    }

    @Override
    public void deleteDictionary(ResourceResolver resourceResolver, String dictionaryPath) throws DictionaryException {
        LOG.debug("Delete dictionary '{}'", dictionaryPath);
        try {
            final Resource dictionaryResource = resourceResolver.getResource(dictionaryPath);
            if (dictionaryResource != null) {
                replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.DEACTIVATE, dictionaryResource.getPath());
                resourceResolver.delete(dictionaryResource);
                resourceResolver.commit();
            } else {
                throw new DictionaryException("Dictionary '" + dictionaryPath + "' not found");
            }
        } catch (PersistenceException | ReplicationException e) {
            throw new DictionaryException("Could not delete dictionary: " + e.getMessage(), e);
        }
    }


    public List<String> getLanguages(Resource dictionaryResource) {
        Set<String> result = new TreeSet<>();

        dictionaryResource.listChildren().forEachRemaining(child -> {
            ValueMap properties = child.getValueMap();
            if (properties.containsKey(JCR_LANGUAGE)) {
                LOG.trace("Found language with path '{}'", child.getPath());
                result.add(properties.get(JCR_LANGUAGE, String.class));
            }
        });

        return new ArrayList<>(result);
    }

    @Override
    public void deleteLanguage(ResourceResolver resourceResolver, Resource dictionaryResource, String language) throws DictionaryException {
        Resource languageResource = getLanguageResource(dictionaryResource, language);
        if (languageResource != null) {
            try {
                LOG.debug("Delete language '{}' from '{}'", language, dictionaryResource.getPath());
                replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.DEACTIVATE, languageResource.getPath());
                resourceResolver.delete(languageResource);
                resourceResolver.commit();
            } catch (PersistenceException | ReplicationException e) {
                throw new DictionaryException("Could not delete language: " + e.getMessage(), e);
            }
        } else {
            throw new DictionaryException("Language does not exist: " + language);
        }
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
    @Override
    public @Nullable Resource getLanguageResource(Resource dictionaryResource, String language) {
        if (dictionaryResource != null) {
            for (Resource languageResource : dictionaryResource.getChildren()) {
                if (language.equals(languageResource.getValueMap().get(JcrConstants.JCR_LANGUAGE))) {
                    return languageResource;
                }
            }
        }
        return null;
    }

    @Override
    public List<String> getKeys(Resource dictionaryResource) {
        Set<String> keys = new TreeSet<>();
        for (String language : getLanguages(dictionaryResource)) {
            Resource languageResource = getLanguageResource(dictionaryResource, language);
            if (languageResource != null) {
                for (Resource messageEntryResource : languageResource.getChildren()) {
                    if (messageEntryResource.isResourceType(SLING_MESSAGEENTRY)) {
                        String key = Optional.ofNullable(messageEntryResource.getValueMap().get(SLING_KEY, String.class))
                                .orElse(messageEntryResource.getName());
                        keys.add(key);
                    }
                }
            }
        }
        return List.copyOf(keys);
    }


    @Override
    public boolean keyExists(Resource dictionaryResource, String language, String key) {
        Resource languageResource = getLanguageResource(dictionaryResource, language);
        return languageResource != null && getMessageEntryResource(languageResource, key) != null;
    }

    @Override
    public void createMessageEntry(ResourceResolver resourceResolver, Resource dictionaryResource, String language, String key, String message) throws PersistenceException {
        Resource languageResource = getLanguageResource(dictionaryResource, language);

        if (languageResource != null) {
            String path = languageResource.getPath();
            Map<String, Object> properties = new HashMap<>();
            properties.put(JCR_PRIMARYTYPE, SLING_MESSAGEENTRY);
            properties.put(SLING_KEY, key);
            if (!message.isBlank()) {
                properties.put(SLING_MESSAGE, message);
            }
            resourceResolver.create(languageResource, Text.escapeIllegalJcrChars(key), properties);
            resourceResolver.commit();
            LOG.trace("Created message entry with key '{}' and message '{}' on path '{}'", key, message, path);
        }
    }

    @Override
    public void updateMessageEntry(ResourceResolver resourceResolver, Resource dictionaryResource, String language, String key, String message) throws PersistenceException {
        Resource languageResource = getLanguageResource(dictionaryResource, language);
        if (languageResource != null) {
            Resource messageEntryResource = getOrCreateMessageEntryResource(resourceResolver, languageResource, key);
            if (messageEntryResource != null) {
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
                }
            }
            resourceResolver.commit();
        }
    }

    @Override
    public Resource getMessageEntryResource(Resource languageResource, String key) {
        // In order to speed up the search, we go for the default check where it is the escaped key as node name
        Resource messageEntryResource = languageResource.getChild(Text.escapeIllegalJcrChars(key));
        if (messageEntryResource != null) {
            return messageEntryResource;
        }

        // Fall back to searching for the resource with sling:key as correct property
        for (Resource resource : languageResource.getChildren()) {
            if (key.equals(resource.getValueMap().get(DictionaryConstants.SLING_KEY))) {
                return resource;
            }
        }

        return null;
    }

    @Override
    public void deleteMessageEntry(ResourceResolver resourceResolver, Resource combiningMessageEntryResource) throws PersistenceException, ReplicationException {
        ValueMap properties = combiningMessageEntryResource.getValueMap();
        if (properties.containsKey(CombiningMessageEntryResourceProvider.MESSAGE_ENTRY_PATHS)) {
            for (String messageEntryPath : properties.get(CombiningMessageEntryResourceProvider.MESSAGE_ENTRY_PATHS, new String[0])) {
                Resource messageEntryResource = resourceResolver.getResource(messageEntryPath);
                if (messageEntryResource != null) {
                    replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.DEACTIVATE, messageEntryPath);
                    resourceResolver.delete(messageEntryResource);
                }
            }
            resourceResolver.commit();
        }
    }

    private Resource getOrCreateMessageEntryResource(ResourceResolver resourceResolver, Resource languageResource, String key) throws PersistenceException {
        Resource messageEntryResource = getMessageEntryResource(languageResource, key);
        if (messageEntryResource != null) {
            return messageEntryResource;
        }
        resourceResolver.create(languageResource, Text.escapeIllegalJcrChars(key), Map.of("jcr:primaryType", SLING_MESSAGEENTRY));
        resourceResolver.commit();
        return languageResource.getChild(Text.escapeIllegalJcrChars(key));
    }

}
