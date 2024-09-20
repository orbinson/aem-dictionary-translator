package be.orbinson.aem.dictionarytranslator.servlets.action;

import be.orbinson.aem.dictionarytranslator.services.DictionaryService;
import be.orbinson.aem.dictionarytranslator.services.impl.DictionaryServiceImpl;
import com.adobe.granite.translation.api.TranslationConfig;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class ReplicateMessageEntryServletTest {

    private final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @NotNull
    ReplicateMessageEntryServlet servlet;

    @NotNull
    CreateMessageEntryServlet create;

    DictionaryService dictionaryService;

    @Mock
    TranslationConfig translationConfig;

    @Mock
    Replicator replicator;

    @Mock
    ResourceResolver resourceResolver;


    @BeforeEach
    void beforeEach() {
        translationConfig = context.registerService(TranslationConfig.class, translationConfig);
        replicator = context.registerService(Replicator.class, replicator);
        dictionaryService = context.registerInjectActivateService(new DictionaryServiceImpl());
        resourceResolver = context.registerService(ResourceResolver.class, resourceResolver);
        servlet = context.registerInjectActivateService(new ReplicateMessageEntryServlet());
        create = context.registerInjectActivateService(new CreateMessageEntryServlet());
    }

    @Test
    void doPostWithoutParams() throws ServletException, IOException {
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of());

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void publishMessageEntryWithNonExistingKey() throws ServletException, IOException {
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "item", "/content/dictionaries/i18n/appel"
        ));

        servlet.service(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    @Disabled("Testing CI")
    void publishExistingMessageEntry() throws ServletException, IOException, ReplicationException {
        Resource test = context.create().resource("/content/dictionaries/i18n/en/appel");
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "item", new String[]{"/content/dictionaries/i18n/en/appel"}
        ));

        List<Resource> resources = new ArrayList<>();
        resources.add(test);
        Iterator<Resource> iterator = resources.iterator();

        SlingHttpServletRequest request = Mockito.spy(context.request());

        servlet.service(context.request(), context.response());

        verify(replicator, times(1)).replicate(any(), eq(ReplicationActionType.ACTIVATE), anyString());
        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    @Disabled("Testing CI")
    void publishMultipleMessageEntries() throws ServletException, IOException, ReplicationException {
        context.create().resource("/content/dictionaries/i18n/en/appel");
        context.create().resource("/content/dictionaries/i18n/en/peer");
        context.create().resource("/content/dictionaries/i18n/en/framboos");

        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "item", new String[]{"/content/dictionaries/i18n/appel,/content/dictionaries/i18n/peer"}
        ));

        servlet.service(context.request(), context.response());

        verify(replicator, times(2)).replicate(any(), eq(ReplicationActionType.ACTIVATE), anyString());
        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void publishNonExistingMessageEntry() throws ServletException, IOException, ReplicationException {
        context.create().resource("/content/dictionaries/i18n/en/appel");
        context.request().setMethod("POST");
        context.request().setParameterMap(Map.of(
                "item", new String[]{"/content/dictionaries/i18n/peer"}
        ));

        servlet.service(context.request(), context.response());

        verify(replicator, times(0)).replicate(any(), any(), anyString());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

}
