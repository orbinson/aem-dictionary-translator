package be.orbinson.aem.dictionarytranslator.utils;

import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.Nullable;

import static com.day.cq.commons.jcr.JcrConstants.JCR_LANGUAGE;

public class DictionaryUtil {

    private DictionaryUtil() {
    }

    /**
     * Gets the language resource based on the jcr:language property
     *
     * @param dictionaryResource The dictionary resource
     * @param language           The language
     * @return the language resource if it exists
     */
    public static @Nullable Resource getLanguageResource(Resource dictionaryResource, String language) {
        if (dictionaryResource != null) {
            for (Resource languageResource : dictionaryResource.getChildren()) {
                if (language.equals(languageResource.getValueMap().get(JCR_LANGUAGE))) {
                    return languageResource;
                }
            }
        }
        return null;
    }
}
