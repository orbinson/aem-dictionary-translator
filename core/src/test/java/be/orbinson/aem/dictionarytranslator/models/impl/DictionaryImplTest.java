package be.orbinson.aem.dictionarytranslator.models.impl;

import be.orbinson.aem.dictionarytranslator.models.Dictionary;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junit.framework.Assert;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
@Disabled("Temporary disabled to test CI")
class DictionaryImplTest {
    private final AemContext context = new AemContext();


    @BeforeEach
    public void setUp() {
        context.addModelsForClasses(DictionaryImpl.class);
        context.load().json("/i18nTestDictionaries.json", "/content/dictionaries");
    }

    @Test
    void testGetLanguages() {
        final List<String> expectedLanguages = new ArrayList<>();
        expectedLanguages.add("en");
        expectedLanguages.add("fr");
        expectedLanguages.add("de");

        final Resource testResource = context.currentResource("/content/dictionaries/languages");
        if (testResource != null) {
            final Dictionary dictionary = testResource.adaptTo(Dictionary.class);
            if (dictionary != null) {
                Assert.assertNotNull(dictionary);
                final List<String> actualLanguages = new ArrayList<>(dictionary.getLanguages());
                Assert.assertEquals(expectedLanguages, actualLanguages);
            } else {
                Assert.fail("could not adapt resource to dictionary");
            }
        } else {
            Assert.fail("No resource available");
        }
    }

    @Test
    void testGetLanguagesString() {
        final String expectedLanguages = "en, fr, de";

        final Resource testResource = context.currentResource("/content/dictionaries/languages");
        if (testResource != null) {
            final Dictionary dictionary = testResource.adaptTo(Dictionary.class);
            if (dictionary != null) {
                Assert.assertNotNull(dictionary);
                final String actualLanguages = dictionary.getLanguageList();
                Assert.assertEquals("message", expectedLanguages, actualLanguages);
            } else {
                Assert.fail("could not adapt resource to dictionary");
            }
        } else {
            Assert.fail("No resource available");
        }
    }

    @Test
    void testContentIsEditable() {
        final Resource testResource = context.currentResource("/content/dictionaries/languages");
        if (testResource != null) {
            final Dictionary dictionary = testResource.adaptTo(Dictionary.class);
            if (dictionary != null) {
                Assert.assertTrue(dictionary.isEditable());
            } else {
                Assert.fail("could not adapt resource to dictionary");
            }
        } else {
            Assert.fail("No resource available");
        }
    }

    @Test
    void testAppsIsNotEditable() {
        context.load().json("/i18nTestDictionaries.json", "/apps/dictionaries");
        final Resource testResource = context.currentResource("/apps/dictionaries/languages");
        if (testResource != null) {
            final Dictionary dictionary = testResource.adaptTo(Dictionary.class);
            if (dictionary != null) {
                Assert.assertFalse(dictionary.isEditable());
            } else {
                Assert.fail("could not adapt resource to dictionary");
            }
        } else {
            Assert.fail("No resource available");
        }
    }

    @Test
    void testLibsIsNotEditable() {
        context.load().json("/i18nTestDictionaries.json", "/libs/dictionaries");
        final Resource testResource = context.currentResource("/libs/dictionaries/languages");
        if (testResource != null) {
            final Dictionary dictionary = testResource.adaptTo(Dictionary.class);
            if (dictionary != null) {
                Assert.assertFalse(dictionary.isEditable());
            } else {
                Assert.fail("could not adapt resource to dictionary");
            }
        } else {
            Assert.fail("No resource available");
        }
    }

    @Test
    void getCreated() throws ParseException {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSXXX");
        final String dateInString = "2022-08-23T10:42:50.469+02:00";
        final Date date = simpleDateFormat.parse(dateInString);
        final Calendar expectedCreated = Calendar.getInstance();
        expectedCreated.setTime(date);

        final Resource testResource = context.currentResource("/content/dictionaries/languages");
        if (testResource != null) {
            final Dictionary dictionary = testResource.adaptTo(Dictionary.class);
            if (dictionary != null) {
                final Calendar actualCreated = dictionary.getCreated();
                Assert.assertTrue(DateUtils.isSameInstant(expectedCreated, actualCreated));
            } else {
                Assert.fail("could not adapt resource to dictionary");
            }
        } else {
            Assert.fail("No resource available");
        }
    }

    @Test
    void testGetLastModifiedWhenNotModified() throws ParseException {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSXXX");
        final String dateInString = "2022-08-23T10:42:50.469+02:00";
        final Date date = simpleDateFormat.parse(dateInString);
        final Calendar expectedCreated = Calendar.getInstance();
        expectedCreated.setTime(date);

        final Resource testResource = context.currentResource("/content/dictionaries/languages");
        if (testResource != null) {
            final Dictionary dictionary = testResource.adaptTo(Dictionary.class);
            if (dictionary != null) {
                final Calendar actualCreated = dictionary.getLastModified();
                Assert.assertTrue(DateUtils.isSameInstant(expectedCreated, actualCreated));
            } else {
                Assert.fail("could not adapt resource to dictionary");
            }
        } else {
            Assert.fail("No resource available");
        }
    }

    @Test
    void testGetLastModified() throws ParseException {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSXXX");
        final String dateInString = "2022-08-24T10:42:50.469+02:00";
        final Date date = simpleDateFormat.parse(dateInString);
        final Calendar expectedCreated = Calendar.getInstance();
        expectedCreated.setTime(date);

        final Resource testResource = context.currentResource("/content/dictionaries/modified");
        if (testResource != null) {
            final Dictionary dictionary = testResource.adaptTo(Dictionary.class);
            if (dictionary != null) {
                final Calendar actualModified = dictionary.getLastModified();
                final Calendar actualCreated = dictionary.getCreated();
                Assert.assertTrue(DateUtils.isSameInstant(expectedCreated, actualModified));
                Assert.assertFalse(DateUtils.isSameInstant(expectedCreated, actualCreated));
            } else {
                Assert.fail("could not adapt resource to dictionary");
            }
        } else {
            Assert.fail("No resource available");
        }
    }

    @Test
    void testGetFormattedLastModified() throws ParseException {
        final String expectedFormattedLastModified = "30-08-2022";
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSXXX");
        final String dateInString = "2022-08-24T10:42:50.469+02:00";
        final Date date = simpleDateFormat.parse(dateInString);
        final Calendar expectedCreated = Calendar.getInstance();
        expectedCreated.setTime(date);

        final Resource testResource = context.currentResource("/content/dictionaries/modified");
        if (testResource != null) {
            final Dictionary dictionary = testResource.adaptTo(Dictionary.class);
            if (dictionary != null) {
                final String actualFormattedLastModified = dictionary.getLastModifiedFormatted();
                Assert.assertEquals(expectedFormattedLastModified, actualFormattedLastModified);
            } else {
                Assert.fail("could not adapt resource to dictionary");
            }
        } else {
            Assert.fail("No resource available");
        }
    }
}
