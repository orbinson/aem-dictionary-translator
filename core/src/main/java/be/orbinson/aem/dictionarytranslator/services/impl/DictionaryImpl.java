package be.orbinson.aem.dictionarytranslator.services.impl;

import java.util.Arrays;
import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;

import be.orbinson.aem.dictionarytranslator.exception.DictionaryException;
import be.orbinson.aem.dictionarytranslator.services.Dictionary;

/**
 * Each dictionary is backed by only a singleton instance of this class.
 * Therefore this class is thread-safe.
 * It is an immutable wrapper around a dictionary resource.
 * It supports lazy loading of the underlying entries leveraging a system resource resolver (which needs read-access to all dictionaries).
 * All write operations take a resource resolver as argument which is used to persist the changes in the repository. However as this class is immutable
 * this object itself becomes invalid after a write operation (managed by the cache in the {@link DictionaryServiceImpl}).
 * 
 * Reading is done initially with the given resource resolver, lazy loading is achieved with a system resolver 
 */
public abstract class DictionaryImpl implements Dictionary {

    private static final Logger LOG = LoggerFactory.getLogger(DictionaryImpl.class);
    static final String SLING_BASENAME = "sling:basename";

    protected final String path;
    private final int ordinal;
    private final Locale language;
    private final Set<String> baseNames;
    private Map<String, Message> messages;
    private final Supplier<ResourceResolver> resourceResolverSupplier;

    protected DictionaryImpl(Resource dictionaryResource, Supplier<ResourceResolver> resourceResolverSupplier) {
        this.path = dictionaryResource.getPath();
        this.ordinal = getOrdinal(path, dictionaryResource.getResourceResolver().getSearchPath());
        ValueMap properties = dictionaryResource.getValueMap();
        String languageString = properties.get(JcrConstants.JCR_LANGUAGE, String.class);
        if (languageString == null || languageString.isEmpty()) {
            throw new IllegalStateException("Language is not set for dictionary: " + path);
        }
        this.language = toLocale(properties.get(JcrConstants.JCR_LANGUAGE, String.class));
        String[] baseNames = properties.get(SLING_BASENAME, String[].class);
        this.baseNames = baseNames != null ? Set.of(baseNames) : Set.of();
        this.resourceResolverSupplier = resourceResolverSupplier;
    }

    private static int getOrdinal(String path, String[] searchPaths) {
        int i = 0;
        for (; i < searchPaths.length; i++) {
            if (path.startsWith(searchPaths[i])) {
                return i;
            }
        }
        return i;
    }

    @Override
    public boolean isEditable(ResourceResolver resourceResolver) {
        Session session = resourceResolver.adaptTo(Session.class);
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
        } else {
            LOG.debug("ResourceResolver is not adaptable to Session, cannot check if dictionary is editable at path: {}", path);
            return true;
        }
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public Locale getLanguage() {
        return language;
    }

    @Override
    public Set<String> getBaseNames() {
        return baseNames;
    }

    @Override
    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public synchronized Map<String, Message> getEntries() throws DictionaryException {
        if (messages == null) {
            try (ResourceResolver resolver = resourceResolverSupplier.get()) {
                Resource dictionaryResource = resolver.getResource(path);
                if (dictionaryResource == null) {
                    throw new DictionaryException("Dictionary resource not found: " + path);
                }
                messages = loadMessages(dictionaryResource);
            }
        } 
        return messages;
    }

    public abstract Map<String, Message> loadMessages(Resource dictionaryResource) throws DictionaryException;

    
    @Override
    public void createOrUpdateEntry(ResourceResolver resourceResolver, String key, String message)
            throws PersistenceException, DictionaryException {
        updateEntry(resourceResolver, key, Optional.ofNullable(message));
    }

    /**
     * Returns the resource for this dictionary using the given resource resolver.
     * If the resource is not found, a {@link DictionaryException} is thrown.
     *
     * @param resourceResolver the resource resolver to use
     * @return the resource for this dictionary
     * @throws DictionaryException if the resource is not found
     */
    protected Resource getResource(ResourceResolver resourceResolver) throws DictionaryException {
        Resource resource = resourceResolver.getResource(path);
        if (resource == null) {
            throw new DictionaryException("Dictionary resource not found: " + path);
        }
        return resource;
    }

    /**
     * ---------------------------------------------------
     * Copied from https://github.com/apache/sling-org-apache-sling-i18n/blob/3f98ebf430e416226500c2975086423edc29dcb3/src/main/java/org/apache/sling/i18n/impl/JcrResourceBundleProvider.java#L668C5-L708C6
     */
    
    /**
     * A regular expression pattern matching all custom country codes.
     * @see <a href="https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2#User-assigned_code_elements">User-assigned code elements</a>
     */
    private static final Pattern USER_ASSIGNED_COUNTRY_CODES_PATTERN = Pattern.compile("aa|q[m-z]|x[a-z]|zz");
    
    /**
     * Converts the given <code>localeString</code> to a valid
     * <code>java.util.Locale</code>. It must either be in the format specified by
     * {@link Locale#toString()} or in <a href="https://tools.ietf.org/html/bcp47">BCP 47 format</a>
     * If the locale string is <code>null</code> or empty, the platform default locale is assumed. If
     * the localeString matches any locale available per default on the
     * platform, that platform locale is returned. Otherwise the localeString is
     * parsed and the language and country parts are compared against the
     * languages and countries provided by the platform. Any unsupported
     * language or country is replaced by the platform default language and
     * country.
     * Locale string is also parsed for script tag. Any unsupported script is ignored.
     * @param localeString the locale as string
     * @return the {@link Locale} being generated from the {@code localeString}
     */
    public static Locale toLocale(String localeString) {
        if (localeString == null || localeString.length() == 0) {
            return Locale.getDefault();
        }

        // support BCP 47 compliant strings as well (using a different separator "-" instead of "_")
        localeString = localeString.replaceAll("-", "_");
        // check language and country
        final String[] parts = localeString.split("_");
        if (parts.length == 0) {
            return Locale.getDefault();
        }

        // at least language is available
        String lang = getValidLanguage(parts[0]);
        if (parts.length == 1) {
            return new Locale(lang);
        }

        Locale localeWithBuilder = createLocaleWithBuilder(parts, lang);
        if (localeWithBuilder != null) {
            return localeWithBuilder;
        }

        return createLocaleWithConstructor(lang, parts);
    }

    /**
     * Create locale with Locale.Builder
     * @param parts parts of Locale string
     * @param lang language part of Locale string
     * @return Locale created with Locale.Builder or null if it fails or when parts length is less than 2
     */
    private static Locale createLocaleWithBuilder(String[] parts, String lang) {
        if (parts.length >= 2) {
            if (isScript(parts[1])) {
                try {
                    switch (parts.length) {
                        case 2:
                            return new Locale.Builder()
                                    .setLanguage(lang)
                                    .setScript(parts[1])
                                    .build();
                        case 3:
                            return new Locale.Builder()
                                    .setLanguage(lang)
                                    .setScript(parts[1])
                                    .setRegion(getValidCountry(parts[2]))
                                    .build();
                        default:
                            return processMultipleParts(parts, lang);
                    }
                } catch (IllformedLocaleException e) {
                    LOG.warn(
                                    "Failed to create locale with LocaleBuilder having parts: {}",
                                    Arrays.toString(parts),
                                    e);
                }
            }
        }
        return null;
    }

    /**
     * Process parts of Locale string when its length is greater than or equals 4
     * @param parts parts of Locale string
     * @param lang language part of Locale string
     * @return Locale created with Locale.Builder or null when parts length is less than 4
     */
    private static Locale processMultipleParts(String[] parts, String lang) {
        if (parts.length >= 4) {
            Locale.Builder localeBuilder =
                    new Locale.Builder().setLanguage(lang).setScript(parts[1]).setRegion(getValidCountry(parts[2]));
            try {
                localeBuilder.setVariant(parts[3]);
                return localeBuilder.build();
            } catch (IllformedLocaleException e) {
                // creating locale with language, script and country
                return localeBuilder.build();
            }
        }
        return null;
    }

    private static String getValidLanguage(String lang) {
        for (String validLang : Locale.getISOLanguages()) {
            if (validLang.equalsIgnoreCase(lang)) {
                return lang;
            }
        }
        return Locale.getDefault().getLanguage();
    }

    private static String getValidCountry(String country) {
        return isValidCountryCode(country) ? country : Locale.getDefault().getCountry();
    }

    private static Locale createLocaleWithConstructor(String lang, String[] parts) {
        String country = parts.length > 1 ? getValidCountry(parts[1]) : "";
        String variant = parts.length > 2 ? parts[2] : "";
        return new Locale(lang, country, variant);
    }

    private static boolean isValidCountryCode(String country) {
        boolean isValidCountryCode = false;
        // allow user-assigned codes (https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2#User-assigned_code_elements)
        if (USER_ASSIGNED_COUNTRY_CODES_PATTERN.matcher(country.toLowerCase()).matches()) {
            isValidCountryCode = true;
        } else {
            String[] countries = Locale.getISOCountries();
            for (int i = 0; i < countries.length; i++) {
                if (countries[i].equalsIgnoreCase(country)) {
                    isValidCountryCode = true; // signal ok
                    break;
                }
            }
        }
        return isValidCountryCode;
    }

    private static boolean isAlpha(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private static boolean isAlphaString(String s) {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (!isAlpha(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isScript(String s) {
        // script        = 4ALPHA              ; ISO 15924 code
        return (s.length() == 4) && isAlphaString(s);
    }

}
