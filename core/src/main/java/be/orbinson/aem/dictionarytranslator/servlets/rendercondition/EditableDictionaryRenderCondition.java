package be.orbinson.aem.dictionarytranslator.servlets.rendercondition;

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.ExpressionHelper;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.Dictionary;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "aem-dictionary-translator/components/rendercondition/editable-dictionary"
)
public class EditableDictionaryRenderCondition extends SlingSafeMethodsServlet {

    @Reference
    private DictionaryService dictionaryService;

    @Reference
    private ExpressionResolver expressionResolver;

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) {
        ExpressionHelper expressionHelper = new ExpressionHelper(expressionResolver, request);
        String path = expressionHelper.getString(new Config(request.getResource()).get("path"));
        ResourceResolver resourceResolver = request.getResourceResolver();
        boolean editable = dictionaryService.getDictionaries(resourceResolver, path).stream().allMatch(d -> d.isEditable(resourceResolver));
        request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(editable));
    }
}
