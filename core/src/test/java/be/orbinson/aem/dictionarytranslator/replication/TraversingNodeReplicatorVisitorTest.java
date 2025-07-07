package be.orbinson.aem.dictionarytranslator.replication;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.day.cq.replication.ReplicationException;

import be.orbinson.aem.dictionarytranslator.services.impl.SlingMessageDictionaryImpl;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class TraversingNodeReplicatorVisitorTest {

    private final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

    @Mock
    private ChunkedReplicator replicator;

    @Test
    void testSlingMessageDictionary() throws RepositoryException, ReplicationException {
        context.load().json("/content.json", "/content");
        context.currentResource("/content/dictionaries/fruit/i18n/en");
        ItemVisitor visitor = new TraversingNodeReplicatorVisitor(replicator, SlingMessageDictionaryImpl.DEFAULT_NODE_FILTER, 1);
        visitor.visit(context.currentResource().adaptTo(Node.class));
        Mockito.verify(replicator).submitPath("/content/dictionaries/fruit/i18n/en");
        Mockito.verify(replicator).submitPath("/content/dictionaries/fruit/i18n/en/apple");
        Mockito.verify(replicator).submitPath("/content/dictionaries/fruit/i18n/en/banana");
        Mockito.verify(replicator).submitPath("/content/dictionaries/fruit/i18n/en/cherry");
        Mockito.verify(replicator).submitPath("/content/dictionaries/fruit/i18n/en/papaya");
        Mockito.verify(replicator).submitPath("/content/dictionaries/fruit/i18n/en/pear");
        Mockito.verify(replicator).submitPath("/content/dictionaries/fruit/i18n/en/mango");
        Mockito.verifyNoMoreInteractions(replicator);
    }

    @Test
    void testEntriesBelowMaxDepth() throws RepositoryException, ReplicationException {
        context.load().json("/content.json", "/content");
        context.currentResource("/content/dictionaries/fruit/i18n/en");
        ItemVisitor visitor = new TraversingNodeReplicatorVisitor(replicator, SlingMessageDictionaryImpl.DEFAULT_NODE_FILTER, 0);
        visitor.visit(context.currentResource().adaptTo(Node.class));
        Mockito.verify(replicator).submitPath("/content/dictionaries/fruit/i18n/en");
        Mockito.verifyNoMoreInteractions(replicator);
    }
}
