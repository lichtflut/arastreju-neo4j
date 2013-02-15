package org.arastreju.bindings.neo4j.impl;

import org.arastreju.bindings.neo4j.ArasRelTypes;
import org.arastreju.bindings.neo4j.NeoConstants;
import org.arastreju.sge.ConversationContext;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.ValueNode;
import org.arastreju.sge.spi.AssociationListener;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * <p>
 *  Manager for Neo4J relationships.
 * </p>
 *
 * <p>
 *  Created Feb. 14, 2013
 * </p>
 *
 * @author Oliver Tigges
 */
public class RelationshipManager implements AssociationListener, NeoConstants {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelationshipManager.class);

    private final ContextAccess ctxAccess;

    private final ConversationContext convContext;

    private final NeoGraphDataStore store;

    // ----------------------------------------------------

    public RelationshipManager(ConversationContext convContext, NeoGraphDataStore store) {
        this.convContext = convContext;
        this.store = store;
        this.ctxAccess = new ContextAccess(store);
    }

    // ----------------------------------------------------

    @Override
    public void onCreate(Statement stmt) {
        Node node = store.getNeoNode(stmt.getSubject().getQualifiedName());
        createRelationship(node, stmt);
    }

    @Override
    public void onRemove(final Statement stmt) {
        Node node = store.getNeoNode(stmt.getSubject().getQualifiedName());
        final Relationship relationship = findCorresponding(node, stmt);
        if (relationship != null) {
            LOGGER.debug("Deleting physical relationship: {}", stmt);
            relationship.delete();
        } else {
            LOGGER.warn("Didn't find corresponding relationship to delete: {}", stmt);
        }
    }

    // ----------------------------------------------------

    private void createRelationship(Node subject, Statement stmt) {
        if (stmt.getObject().isResourceNode()){
            final ResourceNode arasObject = stmt.getObject().asResource();
            final Node neoObject = store.getNeoNode(arasObject.getQualifiedName());
            createRelationship(subject, neoObject, stmt);
        } else {
            final Node neoValue = subject.getGraphDatabase().createNode();
            final ValueNode value = stmt.getObject().asValue();
            neoValue.setProperty(PROPERTY_DATATYPE, value.getDataType().name());
            neoValue.setProperty(PROPERTY_VALUE, value.getStringValue());
            addLocale(neoValue, value.getLocale());
            createRelationship(subject, neoValue, stmt);
        }
        LOGGER.debug("Created physical relation for statement: {} ", stmt);
    }

    private void createRelationship(final Node subject, final Node object, final Statement stmt) {
        final RelationshipType type = stmt.getObject().isResourceNode() ? ArasRelTypes.REFERENCE : ArasRelTypes.VALUE;
        try {
            final Relationship relationship = subject.createRelationshipTo(object, type);
            relationship.setProperty(PREDICATE_URI, stmt.getPredicate().toURI());
            relationship.setProperty(PREDICATE_URI, stmt.getPredicate().toURI());
            relationship.setProperty(TIMESTAMP, new Date().getTime());
            ctxAccess.assignContext(relationship, getCurrentContexts(stmt));
        } catch (Exception e) {
            LOGGER.error("Failed to add relationship--> " + stmt + " to node " + subject, e);
        }
    }

    private Relationship findCorresponding(final Node neoNode, final Statement stmt) {
        final String assocPredicate = stmt.getPredicate().getQualifiedName().toURI();
        final String assocValue = SNOPS.string(stmt.getObject());
        for(Relationship rel : neoNode.getRelationships(Direction.OUTGOING)) {
            final String predicate = (String) rel.getProperty(PREDICATE_URI);
            if (assocPredicate.equals(predicate)) {
                if (stmt.getObject().isResourceNode()) {
                    final String uri = (String) rel.getEndNode().getProperty(PROPERTY_URI);
                    if (assocValue.equals(uri)) {
                        return rel;
                    }
                } else {
                    final String value = (String) rel.getEndNode().getProperty(PROPERTY_VALUE);
                    if (assocValue.equals(value)) {
                        return rel;
                    }
                }
            }
        }
        return null;
    }

    private void addLocale(Node node, Locale locale) {
        if (locale == null || locale.getLanguage() == null) {
            return;
        }
        final StringBuilder sb = new StringBuilder(5);
        sb.append(locale.getLanguage());
        if (locale.getCountry() != null) {
            sb.append("_").append(locale.getCountry());
        }
        node.setProperty(PROPERTY_LOCALE, sb.toString());
    }

    private Context[] getCurrentContexts(Statement stmt) {
        if (stmt.getContexts().length == 0) {
            if (convContext.getPrimaryContext() == null) {
                return ContextAccess.NO_CTX;
            } else {
                return new Context[] { convContext.getPrimaryContext() };
            }
        } else if (convContext.getPrimaryContext() == null) {
            return stmt.getContexts();
        } else {
            Set<Context> joined = new HashSet<Context>();
            joined.add(convContext.getPrimaryContext());
            Collections.addAll(joined, stmt.getContexts());
            return joined.toArray(new Context[joined.size()]);
        }
    }

}
