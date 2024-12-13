package be.orbinson.aem.dictionarytranslator.models.impl;

import be.orbinson.aem.dictionarytranslator.models.Dictionary;
import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import com.day.cq.replication.Replicator;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(AemContextExtension.class)
class DictionaryImplTest {

    private final AemContext context = new AemContext();
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSXXX");

    @BeforeEach
    public void setUp() {
        context.addModelsForClasses(DictionaryImpl.class);

        context.registerService(Replicator.class, mock(Replicator.class));
        context.registerInjectActivateService(new DictionaryServiceImpl());

        context.load().json("/i18nTestDictionaries.json", "/content/dictionaries");
    }

    @Test
    void dictionaryShouldReturnCorrectLanguages() {
        context.currentResource("/content/dictionaries/languages");

        Dictionary dictionary = context.request().adaptTo(Dictionary.class);

        assertEquals(List.of("de", "en", "fr"), dictionary.getLanguages());
    }

    @Test
    void dictionaryShouldReturnCorrectKeyCount() {
        context.currentResource("/content/dictionaries/languages");

        Dictionary dictionary = context.request().adaptTo(Dictionary.class);

        assertEquals(1, dictionary.getKeyCount());
    }

    @Test
    void dictionaryShouldReturnCorrectBasename() {
        context.currentResource("/content/dictionaries/languages");

        Dictionary dictionary = context.request().adaptTo(Dictionary.class);

        assertEquals("/content/dictionaries/basename", dictionary.getBasename());
    }

    @Test
    void dictionaryShouldReturnCorrectLanguageList() {
        context.currentResource("/content/dictionaries/languages");

        Dictionary dictionary = context.request().adaptTo(Dictionary.class);

        assertEquals("de, en, fr", dictionary.getLanguageList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/apps/dictionaries", "/libs/dictionaries"})
    void unreadableOrImmutablePathsShouldNotBeEditable(String path) {
        context.load().json("/i18nTestDictionaries.json", path);
        context.currentResource(path + "/languages");

        Dictionary dictionary = context.request().adaptTo(Dictionary.class);

        assertFalse(dictionary.isEditable());
    }

    @Test
    void dictionaryCreationTimeShouldBeCorrect() throws ParseException {
        Date date = simpleDateFormat.parse("2022-08-23T10:42:50.469+02:00");
        Calendar expectedCreated = Calendar.getInstance();
        expectedCreated.setTime(date);
        context.currentResource("/content/dictionaries/languages");

        Dictionary dictionary = context.request().adaptTo(Dictionary.class);

        assertTrue(DateUtils.isSameInstant(expectedCreated, dictionary.getCreated()));
    }

    @Test
    void lastModifiedShouldBeCorrectWhenNotModified() throws ParseException {
        Date date = simpleDateFormat.parse("2022-08-23T10:42:50.469+02:00");
        Calendar expectedCreated = Calendar.getInstance();
        expectedCreated.setTime(date);
        context.currentResource("/content/dictionaries/languages");

        Dictionary dictionary = context.request().adaptTo(Dictionary.class);

        assertTrue(DateUtils.isSameInstant(expectedCreated, dictionary.getLastModified()));
    }

    @Test
    void dictionaryShouldReturnCorrectCreatedTime() throws ParseException {
        Date date = simpleDateFormat.parse("2022-08-24T10:42:50.469+02:00");
        Calendar expectedCreated = Calendar.getInstance();
        expectedCreated.setTime(date);
        context.currentResource("/content/dictionaries/modified");

        Dictionary dictionary = context.request().adaptTo(Dictionary.class);

        assertFalse(DateUtils.isSameInstant(expectedCreated, dictionary.getCreated()));
    }

}
