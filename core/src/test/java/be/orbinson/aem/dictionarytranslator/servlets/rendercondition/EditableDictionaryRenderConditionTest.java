package be.orbinson.aem.dictionarytranslator.servlets.rendercondition;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class EditableDictionaryRenderConditionTest {
    final AemContext context = new AemContext();

    EditableDictionaryRenderCondition servlet;

    @Mock
    DictionaryService dictionaryService;

    @Mock
    ExpressionResolver expressionResolver;

    @BeforeEach
    void setUp() {
        dictionaryService = context.registerService(DictionaryService.class, dictionaryService);
        expressionResolver = context.registerService(ExpressionResolver.class, expressionResolver);

        servlet = context.registerInjectActivateService(new EditableDictionaryRenderCondition());
        context.load().json("/apps.json", "/apps");
    }

    @Test
    void renderConditionShouldBeTrueWhenDictionaryIsEditable() {
        context.currentResource("/apps/aem-dictionary-translator/content/dictionaries/message-entries/jcr:content/actions/primary/create-key/granite:rendercondition");
        doReturn(true).when(dictionaryService).isEditableDictionary(any());

        servlet.doGet(context.request(), context.response());

        SimpleRenderCondition renderCondition = (SimpleRenderCondition) context.request().getAttribute(RenderCondition.class.getName());
        assertTrue(renderCondition.check());
    }

    @Test
    void renderConditionShouldBeFalseWhenDictionaryIsNotEditable() {
        context.currentResource("/apps/aem-dictionary-translator/content/dictionaries/message-entries/jcr:content/actions/primary/create-key/granite:rendercondition");
        doReturn(false).when(dictionaryService).isEditableDictionary(any());

        servlet.doGet(context.request(), context.response());

        SimpleRenderCondition renderCondition = (SimpleRenderCondition) context.request().getAttribute(RenderCondition.class.getName());
        assertFalse(renderCondition.check());
    }
}