package be.orbinson.aem.dictionarytranslator.services.impl;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import com.adobe.granite.translation.api.TranslationConfig;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.*;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.jackrabbit.JcrConstants.JCR_LANGUAGE;

@Component
public class DictionaryServiceImpl implements DictionaryService {
    private static final Logger LOG = LoggerFactory.getLogger(DictionaryServiceImpl.class);
    private static final String SLING_BASENAME = "sling:basename";

    @Reference
    private TranslationConfig translationConfig;
    @Reference
    private ResourceResolverFactory resourceResolverFactory;

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

    private ResourceResolver getServiceResourceResolver() throws LoginException {
        Map<String, Object> authenticationInfo = Map.of(ResourceResolverFactory.SUBSERVICE, "dictionary-service");
        return resourceResolverFactory.getServiceResourceResolver(authenticationInfo);
    }

    public @NotNull Map<String, String> getLanguagesForPath(ResourceResolver resourceResolver, String path) {
        Map<String, String> result = new HashMap<>();
        Resource resource = resourceResolver.getResource(path);

        if (resource != null && translationConfig != null) {
            try (ResourceResolver serviceResourceResolver = getServiceResourceResolver()) {
                Map<String, String> languages = translationConfig.getLanguages(serviceResourceResolver);

                resource.getChildren().forEach(child -> {
                    if (child.getValueMap().containsKey(JcrConstants.JCR_LANGUAGE)) {
                        String language = child.getValueMap().get(JcrConstants.JCR_LANGUAGE, String.class);
                        if (language != null) {
                            String label = languages.get(language);
                            LOG.trace("Add language '{}' with label '{}'", language, label);
                            result.put(language, label);
                        }
                    }
                });
            } catch (LoginException e) {
                LOG.error("Unable to get service resource resolver to get languages", e);
            }
        }

        return result;
    }

    public @NotNull List<Resource> getDictionaries(ResourceResolver resourceResolver) {
        Map<String, Resource> result = new TreeMap<>();

        resourceResolver
                .findResources("//element(*, mix:language)[@jcr:language and (@jcr:primaryType='sling:Folder' or @jcr:primaryType='nt:folder')]/..", "xpath")
                .forEachRemaining(resource -> result.put(resource.getPath(), resource));

        return new ArrayList<>(result.values());
    }

    public List<String> getLanguages(Resource resource) {
        List<String> result = new ArrayList<>();

        resource.listChildren().forEachRemaining(child -> {
            ValueMap properties = child.getValueMap();
            if (properties.containsKey(JCR_LANGUAGE)) {
                LOG.trace("Found language with path '{}'", child.getPath());
                result.add(properties.get(JCR_LANGUAGE, String.class));
            }
        });

        return result;
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

    public void createDictionary(Resource parent, String name, String[] languages, String basename) throws PersistenceException {
        LOG.debug("Create dictionary '{}'", name);
        ResourceResolver resourceResolver = parent.getResourceResolver();
        String dictionaryPath = String.format("%s/%s/i18n", parent.getPath(), JcrUtil.createValidName(name));
        Resource dictionaryResource = ResourceUtil.getOrCreateResource(resourceResolver, dictionaryPath, "sling:Folder", "sling:Folder", true);

        for (String language : languages) {
            addLanguage(dictionaryResource, language, basename);
        }
    }
}
