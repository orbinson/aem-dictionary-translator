package be.orbinson.aem.dictionarytranslator.services.impl;

import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.jackrabbit.commons.json.JsonHandler;
import org.apache.jackrabbit.commons.json.JsonParser;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;

public class JsonFileLanguageDictionary extends LanguageDictionaryImpl {

    protected JsonFileLanguageDictionary(Resource dictionaryResource, Supplier<ResourceResolver> resourceResolverSupplier) {
        super(dictionaryResource, resourceResolverSupplier);
    }

    private static final Logger LOG = LoggerFactory.getLogger(JsonFileLanguageDictionary.class);
    
    @Override
    public Type getType() {
        return Type.JSON_FILE;
    }

    @Override
    public boolean isEditable(ResourceResolver resourceResolver) {
        return false;
    }

    @Override
    public Map<String, Message> loadMessages(Resource dictionaryResource) {
        Map<String, Object> messages = new HashMap<>();
        loadJsonDictionary(dictionaryResource.getChild(JCR_CONTENT), messages);
        return messages.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, e -> new Message(e.getValue().toString(), null)));
    }

    @Override
    public void createOrUpdateEntry(ResourceResolver resourceResolver, String key, String message)
            throws PersistenceException, DictionaryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void deleteEntry(Replicator replicator, ResourceResolver resourceResolver, String key)
            throws PersistenceException, ReplicationException, DictionaryException {
        throw new UnsupportedOperationException("Not supported yet.");

    }

    static boolean isCompliant(Resource languageResource) {
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
}
