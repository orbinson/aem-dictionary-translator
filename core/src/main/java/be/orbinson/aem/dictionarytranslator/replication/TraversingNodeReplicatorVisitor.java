package be.orbinson.aem.dictionarytranslator.replication;

import java.util.function.Predicate;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.util.TraversingItemVisitor;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.vault.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationException;

public class TraversingNodeReplicatorVisitor extends TraversingItemVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(TraversingNodeReplicatorVisitor.class);
    private final ChunkedReplicator replicator;
    private final Predicate<Node> nodeFilter;

    public TraversingNodeReplicatorVisitor(ChunkedReplicator replicator, Predicate<Node> nodeFilter, int maxDepth) {
        super(false, maxDepth);
        this.replicator = replicator;
        this.nodeFilter = nodeFilter;
    }

    @Override
    protected void entering(Node node, int depth) throws RepositoryException {
        // only replicate nodes that are on the root level or are nt:hierarchy nodes (other hierarchies are already considered by the replicator
        if (depth == 0 || isHierarchyNode(node)) {
            if (nodeFilter.test(node)) {
                try {
                    replicator.submitPath(node.getPath());
                } catch (ReplicationException e) {
                    throw new RepositoryException("Error replicating node: " + node.getPath(), e);
                }
            } else {
                LOG.debug("Skipping node {} as it does not match the filter", node.getPath());
            }
        } else {
            LOG.debug("Skipping node {} as should be contained in replicated parent node {} already", node.getPath(), Text.getRelativeParent(node.getPath(), depth));
        }
    }

    private static boolean isHierarchyNode(Node node) throws RepositoryException {
        return node.isNodeType(JcrConstants.NT_HIERARCHYNODE);
    }

    @Override
    protected void leaving(Node node, int depth) throws RepositoryException {
        // No action needed on leaving nodes
    }

    @Override
    protected void entering(Property property, int depth) throws RepositoryException {
        // No action needed on entering properties
    }


    @Override
    protected void leaving(Property property, int depth) throws RepositoryException {
        // No action needed on leaving properties
    }


}
