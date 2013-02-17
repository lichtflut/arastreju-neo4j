/*
 * Copyright (C) 2013 lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 *
 * The Arastreju-Neo4j binding is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.arastreju.bindings.neo4j.storage;

import org.arastreju.sge.ConversationContext;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.ValueNode;
import org.arastreju.sge.naming.QualifiedName;
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

    public static final Context[] NO_CTX = new Context[0];

    private final ConversationContext convContext;

    private final NeoGraphDataStore store;

    // ----------------------------------------------------

    public RelationshipManager(ConversationContext convContext, NeoGraphDataStore store) {
        this.convContext = convContext;
        this.store = store;
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
            assignContext(relationship, getCurrentContexts(stmt));
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
                return NO_CTX;
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

    /**
     * Assigns context information to a relationship.
     * @param relationship The relationship to be assigned to the contexts.
     * @param contexts The contexts.
     */
    private void assignContext(final Relationship relationship, final Context[] contexts) {
        if (contexts != null && contexts.length > 0) {
            String[] uris = new String[contexts.length];
            for (int i = 0; i < contexts.length; i++) {
                assureExists(contexts[i].getQualifiedName());
                uris[i] = contexts[i].toURI();
            }
            relationship.setProperty(NeoConstants.CONTEXT_URI, uris);
        }
    }

    // ----------------------------------------------------

    private boolean exists(QualifiedName qn) {
        return store.find(qn) != null;
    }

    private void assureExists(QualifiedName qn) {
        if (!exists(qn)) {
            store.create(qn);
        }
    }

}
