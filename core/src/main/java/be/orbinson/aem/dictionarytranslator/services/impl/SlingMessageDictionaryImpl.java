package be.orbinson.aem.dictionarytranslator.services.impl;

import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_KEY;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGE;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGEENTRY;
import static be.orbinson.aem.dictionarytranslator.utils.DictionaryConstants.SLING_MESSAGE_MIXIN;
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;

public class SlingMessageDictionaryImpl extends DictionaryImpl {

    private static final Logger LOG = LoggerFactory.getLogger(SlingMessageDictionaryImpl.class);

    public SlingMessageDictionaryImpl(Resource dictionaryResource, Supplier<ResourceResolver> resourceResolverSupplier) {
        super(dictionaryResource, resourceResolverSupplier);
    }

    @Override
    public Type getType() {
        return Type.SLING_MESSAGE_ENTRY;
    }

    private static boolean isMessageEntryResource(Resource messageEntryResource) {
        return messageEntryResource != null && (messageEntryResource.isResourceType(SLING_MESSAGEENTRY) ||
                hasMixinType(messageEntryResource, SLING_MESSAGE_MIXIN));
    }

    private static boolean hasMixinType(Resource resource, String mixinType) {
        return Arrays.asList(resource.getValueMap().get(JCR_MIXINTYPES, new String[0])).contains(mixinType);
    }

    @Override
    public Map<String, Message> loadMessages(Resource dictionaryResource) {
        Map<String, Message> messages = new HashMap<>();
        // TODO: use https://sling.apache.org/documentation/bundles/resource-filter.html to optimize filtering out non-relevant children
        dictionaryResource.listChildren().forEachRemaining(messageEntryResource -> {
            if (isMessageEntryResource(messageEntryResource)) {
                String key = Optional.ofNullable(messageEntryResource.getValueMap().get(SLING_KEY, String.class))
                        .orElse(Text.unescapeIllegalJcrChars(messageEntryResource.getName()));
                messages.put(key, new Message(messageEntryResource.getValueMap().get(SLING_MESSAGE, null), messageEntryResource.getPath()));
            }
        });
        return messages;
    }

    @Override
    public void createEntry(ResourceResolver resourceResolver, String key, Optional<String> message)
            throws PersistenceException, DictionaryException {
        final Resource messageEntryResource = createMessageEntryResource(resourceResolver, key);
        updateMessage(key, message, messageEntryResource);
    }

    @Override
    public void updateEntry(ResourceResolver resourceResolver, String key, Optional<String> message)
            throws PersistenceException, DictionaryException {
        final Resource messageEntryResource;
        if (message.isPresent()) {
            messageEntryResource = getOrCreateMessageEntryResource(resourceResolver, key);
        } else {
            messageEntryResource = getMessageEntryResource(resourceResolver, key).orElse(null);
            if (messageEntryResource == null) {
                LOG.trace("No message entry found for key '{}', nothing to update, as message was not set", key);
                return; // Nothing to update if the message is not present and no entry exists
            }
        }
        updateMessage(key, message, messageEntryResource);
    }

    private static void updateMessage(String key, Optional<String> message, Resource messageEntryResource) throws PersistenceException {
        ValueMap valueMap = messageEntryResource.adaptTo(ModifiableValueMap.class);
        if (valueMap != null) {
            if (message.isPresent()) {
                valueMap.put(SLING_MESSAGE, message.get());
                LOG.trace("Updated message entry for key '{}' to message '{}' on path '{}'", key, message.get(), messageEntryResource.getPath());
            } else {
                valueMap.remove(SLING_MESSAGE);
                LOG.trace("Removed message for key '{}' on path '{}'", key, messageEntryResource.getPath());
            }
          
        } else {
            throw new PersistenceException("Could not update message entry, resource at \"" + messageEntryResource.getPath() + "\" not adaptable to ModifiableValueMap");
        }
    }

    private Optional<Resource> getMessageEntryResource(ResourceResolver resourceResolver, String key) throws DictionaryException {
        Objects.requireNonNull(resourceResolver, "resourceResolver must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Message message = getEntries().get(key); // Ensure the key is present in the entries map, if not it will throw an exception
        if (message == null) {
            return Optional.empty();
        }
        Resource messageEntryResource = resourceResolver.getResource(message.getResourcePath().orElseThrow(() -> new DictionaryException("Message entry resource not found for key \"" + key + "\"")));
        return Optional.of(messageEntryResource);
    }

    private @NotNull Resource getOrCreateMessageEntryResource(ResourceResolver resourceResolver, String key) throws DictionaryException, PersistenceException {
        Optional<Resource> messageEntryResource = getMessageEntryResource(resourceResolver, key);
        if (messageEntryResource.isPresent()) {
            return messageEntryResource.get();
        } else {
            return createMessageEntryResource(resourceResolver, key);
        }
    }

    private Resource createMessageEntryResource(ResourceResolver resourceResolver, String key) throws PersistenceException, DictionaryException {
        Resource newResource = resourceResolver.create(getResource(resourceResolver), Text.escapeIllegalJcrChars(key), Map.of(JCR_PRIMARYTYPE, SLING_MESSAGEENTRY));
        LOG.trace("Created message entry with key '{}' on path '{}'", key, newResource.getPath());
        return newResource;
    }

    @Override
    public void deleteEntry(Replicator replicator, ResourceResolver resourceResolver, String key)
            throws PersistenceException, ReplicationException, DictionaryException {
        Resource messageEntryResource = getMessageEntryResource(resourceResolver, key).orElseThrow(() -> new DictionaryException("Message entry resource not found for key \"" + key + "\" of dictionary \"" + getPath() + "\""));
        replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.DEACTIVATE, messageEntryResource.getPath());
        resourceResolver.delete(messageEntryResource);
    }

}
