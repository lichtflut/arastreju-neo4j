/*
 * Copyright (C) 2012 lichtflut Forschungs- und Entwicklungsgesellschaft mbH
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
package org.arastreju.bindings.neo4j.impl;

import org.arastreju.bindings.neo4j.ArasRelTypes;
import org.arastreju.bindings.neo4j.NeoConstants;
import org.arastreju.bindings.neo4j.extensions.NeoAssociationKeeper;
import org.arastreju.bindings.neo4j.extensions.SNValueNeo;
import org.arastreju.bindings.neo4j.index.ResourceIndex;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.index.ArasIndexerImpl;
import org.arastreju.sge.inferencing.Inferencer;
import org.arastreju.sge.model.DetachedStatement;
import org.arastreju.sge.model.SimpleResourceID;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.StatementMetaInfo;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.SemanticNode;
import org.arastreju.sge.model.nodes.ValueNode;
import org.arastreju.sge.persistence.ResourceResolver;
import org.arastreju.sge.persistence.TxAction;
import org.arastreju.sge.persistence.TxProvider;
import org.arastreju.sge.spi.AssocKeeperAccess;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * <p>
 *  Handler for resolving, adding and removing of a node's association.
 * </p>
 *
 * <p>
 * 	Created Dec 1, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class AssociationHandler implements NeoConstants {

	private static final Logger LOGGER = LoggerFactory.getLogger(AssociationHandler.class);

    // ----------------------------------------------------

    private final NeoGraphDataConnection connection;

    private final NeoConversationContext convContext;
	
	private final Inferencer hardInferencer;
	
	private final Inferencer softInferencer;
	
	private final ResourceResolver resourceResolver;

    private final NeoNodeResolver neoNodeResolver;

	private final ArasIndexerImpl index;

	private final ContextAccess ctxAccess;

	// ----------------------------------------------------
	
	/**
	 * Creates a new association handler.
	 * @param connection The connection.
	 * @param conversationContext The current working context.
	 */
	public AssociationHandler(NeoGraphDataConnection connection, NeoConversationContext conversationContext) {
        this.connection = connection;
        this.convContext = conversationContext;
		this.resourceResolver = new NeoResourceResolver(connection, conversationContext);
        this.neoNodeResolver = new NeoNodeResolver(conversationContext);
		this.index = new ArasIndexerImpl(conversationContext);
		this.ctxAccess = new ContextAccess(resourceResolver);
		this.softInferencer = new NeoSoftInferencer(resourceResolver);
		this.hardInferencer = new NeoHardInferencer(resourceResolver);
	}

	// ----------------------------------------------------

	/**
	 * Resolve the associations of given association keeper.
	 * @param keeper The association keeper to be resolved.
	 */
	public void resolveAssociations(NeoAssociationKeeper keeper) {
		for(Relationship rel : keeper.getNeoNode().getRelationships(Direction.OUTGOING)){
			final Context[] ctx = ctxAccess.getContextInfo(rel);
			if (!regardContext(ctx, rel)) {
				continue;
			}
			SemanticNode object = null;
			if (rel.isType(ArasRelTypes.REFERENCE)){
				object = neoNodeResolver.resolve(rel.getEndNode());
			} else if (rel.isType(ArasRelTypes.VALUE)){
				object = new SNValueNeo(rel.getEndNode());
			}
			final ResourceNode predicate = resourceResolver.resolve(new SimpleResourceID(rel.getProperty(PREDICATE_URI).toString()));
			final StatementMetaInfo mi = new StatementMetaInfo(ctx, new Date((Long)rel.getProperty(TIMESTAMP, 0L)));
			keeper.addAssociationDirectly(new DetachedStatement(keeper.getID(), predicate, object, mi));
		}
	}

    public Set<Statement> getIncomingStatements(NeoAssociationKeeper keeper) {
        final Set<Statement> result = new HashSet<Statement>();
        for(Relationship rel : keeper.getNeoNode().getRelationships(Direction.INCOMING)){
            final Context[] ctx = ctxAccess.getContextInfo(rel);
            if (!regardContext(ctx, rel)) {
                continue;
            }
            final ResourceNode subject = neoNodeResolver.resolve(rel.getStartNode());
            final ResourceNode predicate = resourceResolver.resolve(new SimpleResourceID(rel.getProperty(PREDICATE_URI).toString()));
            final StatementMetaInfo mi = new StatementMetaInfo(ctx, new Date((Long)rel.getProperty(TIMESTAMP, 0L)));
           result.add(new DetachedStatement(subject, predicate, keeper.getID(), mi));
        }
        return result;
    }
	
	// ----------------------------------------------------
	
	/**
	 * Add a new Association to given Neo node, or rather create a corresponding Relation.
	 * @param keeper The neo keeper, which shall be the subject in the new Relation.
	 * @param stmt The Association.
	 */
	public void addAssociation(final NeoAssociationKeeper keeper, final Statement stmt) {
        tx().doTransacted(new TxAction() {
            public void execute() {

                final ResourceNode predicate = resourceResolver.resolve(stmt.getPredicate());
                final SemanticNode object = resolve(stmt.getObject());
                final Statement assoc = new DetachedStatement(keeper.getID(), predicate, object, stmt.getMetaInfo());
                keeper.addAssociationDirectly(assoc);

                createRelationships(keeper.getNeoNode(), stmt);
                final List<Statement> stmtList = Collections.singletonList(stmt);
                addHardInferences(stmtList);
                addSoftInferences(keeper, stmtList);
                connection.notifyModification(keeper.getQualifiedName(), convContext);

            }
        });
		
	}

	/**
	 * Remove the given association.
	 * @param keeper The keeper.
	 * @param assoc The association.
	 * @return true if the association has been removed.
	 */
	public boolean removeAssociation(final NeoAssociationKeeper keeper, final Statement assoc) {
		final Relationship relationship = findCorresponding(keeper.getNeoNode(), assoc);
		if (relationship != null) {
            tx().doTransacted(new TxAction() {
                public void execute() {
                    LOGGER.debug("Deleting: " + assoc);
                    relationship.delete();
                    //index.removeFromIndex(keeper.getNeoNode(), assoc);
                    removeHardInferences(Collections.singleton(assoc));
                    ResourceNode sub = resourceResolver.resolve(assoc.getSubject());
                    index.index(sub);
//                    index.reindex(keeper.getNeoNode(), keeper.getQualifiedName(), keeper.getAssociations());
                    addSoftInferences(keeper, keeper.getAssociations());
                    connection.notifyModification(keeper.getQualifiedName(), convContext);
                }
            });
			return true;
		} else {
			LOGGER.warn("Didn't find corresponding relationship to delete: " + assoc);
			return false;	
		}
	}
	
	// ----------------------------------------------------
	
	private void addSoftInferences(final NeoAssociationKeeper keeper, final Collection<? extends Statement> originals) {
		final Set<Statement> inferenced = new HashSet<Statement>();
		for (Statement stmt : originals) {
			softInferencer.addInferenced(stmt, inferenced);
		}
		for (Statement stmt : inferenced) {
			if (stmt.getSubject().getQualifiedName().equals(keeper.getQualifiedName())) {
				index.index(stmt);
			} else {
				LOGGER.warn("Inferred statement can not be indexed: " + stmt);
			}
		}
	}
	
	private void addHardInferences(final Collection<? extends Statement> originals) {
		final Set<Statement> inferenced = new HashSet<Statement>();
		for (Statement stmt : originals) {
			hardInferencer.addInferenced(stmt, inferenced);
		}
		for (Statement stmt : inferenced) {
			addStatements(stmt);
		}
	}
	
	private void removeHardInferences(final Collection<? extends Statement> originals) {
		final Set<Statement> inferenced = new HashSet<Statement>();
		for (Statement stmt : originals) {
			hardInferencer.addInferenced(stmt, inferenced);
		}
		removeStatements(inferenced);
	}
	
	/**
	 * Add new statements.
	 * @param statements The statements.
	 */
	private void addStatements(final Statement... statements) {
		for (Statement stmt : statements) {
			ResourceNode subject = resourceResolver.resolve(stmt.getSubject());
			SNOPS.associate(subject, stmt.getPredicate(), stmt.getObject(), stmt.getContexts());
		}
	}
	
	/**
	 * Remove statements.
	 * @param statements The statements.
	 */
	private void removeStatements(final Collection<? extends Statement> statements) {
		for (Statement stmt : statements) {
			ResourceNode subject = resourceResolver.resolve(stmt.getSubject());
			SNOPS.remove(subject, stmt.getPredicate(), stmt.getObject());
		}
	}
	
	private void createRelationships(Node subject, Statement stmt) {
        LOGGER.debug("Created statement {}. ", stmt);
		if (stmt.getObject().isResourceNode()){
			final ResourceNode arasClient = resourceResolver.resolve(stmt.getObject().asResource());
			final Node neoClient = getNeoNode(arasClient);
			createRelationShip(subject, neoClient, stmt);
		} else {
			final Node neoClient = subject.getGraphDatabase().createNode();
			final ValueNode value = stmt.getObject().asValue();
			neoClient.setProperty(PROPERTY_DATATYPE, value.getDataType().name());
			neoClient.setProperty(PROPERTY_VALUE, value.getStringValue());
			addLocale(neoClient, value.getLocale());
			createRelationShip(subject, neoClient, stmt);
		}
		index.index(stmt);
	}
	
	private void createRelationShip(final Node subject, final Node object, final Statement stmt) {
		final RelationshipType type = stmt.getObject().isResourceNode() ? ArasRelTypes.REFERENCE : ArasRelTypes.VALUE;
        try {
		    final Relationship relationship = subject.createRelationshipTo(object, type);
            relationship.setProperty(PREDICATE_URI, stmt.getPredicate().toURI());
            relationship.setProperty(PREDICATE_URI, stmt.getPredicate().toURI());
            relationship.setProperty(TIMESTAMP, new Date().getTime());
            ctxAccess.assignContext(relationship, getCurrentContexts(stmt));
            LOGGER.debug("added relationship--> " + relationship + " to node " + subject);
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
	
	private SemanticNode resolve(final SemanticNode node) {
		if (node.isResourceNode()) {
			return resourceResolver.resolve(node.asResource());
		} else {
			return node;
		}
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
	
	private boolean regardContext(Context[] stmtContexts, Relationship rel) {
		if (stmtContexts.length == 0) {
			LOGGER.debug("Statement has no context.");
			return true;
		}
		Context[] readContexts = convContext.getReadContexts();
        for (Context readContext : readContexts) {
            for (Context stmtContext : stmtContexts) {
                if (readContext.equals(stmtContext)) {
                    return true;
                }
            }
        }
        if (LOGGER.isDebugEnabled()) {
            final StringBuilder sb = new StringBuilder("Contexts of Statement ");
            sb.append(neoNodeResolver.resolve(rel.getStartNode()));
            sb.append(" --> ");
            sb.append(rel.getProperty(PREDICATE_URI));
            sb.append(" --> ");
            sb.append(convert(rel, rel.getEndNode()));
            sb.append(" {} ");
            sb.append("not in read contexts");
            sb.append(" {}.");
            LOGGER.debug(sb.toString(), Arrays.toString(stmtContexts), Arrays.toString(readContexts));
        }
		return false;
	}

    private SemanticNode convert(Relationship rel, Node node) {
        if (rel.isType(ArasRelTypes.REFERENCE)){
            return neoNodeResolver.resolve(node);
        } else if (rel.isType(ArasRelTypes.VALUE)){
            return new SNValueNeo(node);
        } else {
            return null;
        }
    }

    private Node getNeoNode(final ResourceNode node){
        try {
            NeoAssociationKeeper keeper =
                    (NeoAssociationKeeper) AssocKeeperAccess.getInstance().getAssociationKeeper(node);
            return keeper.getNeoNode();
        } catch (ClassCastException e){
            throw new RuntimeException(e);
        }
    }

    private TxProvider tx() {
        return convContext.getTxProvider();
    }
}
