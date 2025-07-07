package be.orbinson.aem.dictionarytranslator.servlets.action;

import java.util.function.Predicate;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.osgi.service.component.annotations.Component;

import com.adobe.granite.replication.treeactivation.NodeFilter;

import be.orbinson.aem.dictionarytranslator.services.impl.SlingMessageDictionaryImpl;

@Component(service = NodeFilter.class)
public class SlingMessageNodeFilter implements NodeFilter {
    public static final String NAME = "onlySlingMessages";
    
    private static final Predicate<Node> PREDICATE = SlingMessageDictionaryImpl.DEFAULT_NODE_FILTER;

    public boolean acceptNode(Node node) throws RepositoryException {
        return PREDICATE.test(node);
    }

    public String getName() {
        return NAME;
    }
}
