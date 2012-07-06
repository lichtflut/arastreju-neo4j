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
package org.arastreju.bindings.neo4j;

import org.arastreju.bindings.neo4j.extensions.NeoResourceResolver;
import org.arastreju.bindings.neo4j.impl.GraphDataConnection;
import org.arastreju.bindings.neo4j.impl.NeoConversationContext;
import org.arastreju.bindings.neo4j.impl.NeoResourceResolverImpl;
import org.arastreju.bindings.neo4j.impl.SemanticNetworkAccess;
import org.arastreju.bindings.neo4j.index.ResourceIndex;
import org.arastreju.bindings.neo4j.query.NeoQueryBuilder;
import org.arastreju.sge.ConversationContext;
import org.arastreju.sge.ModelingConversation;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.SemanticGraph;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.SemanticNode;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.TransactionControl;
import org.arastreju.sge.persistence.TxResultAction;
import org.arastreju.sge.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *  Implementation of {@link ModelingConversation} for Neo4j.
 * </p>
 *
 * <p>
 * 	Created Sep 2, 2010
 * </p>
 *
 * @author Oliver Tigges
 */
public class Neo4jModellingConversation implements ModelingConversation {
	
	private static final Logger logger = LoggerFactory.getLogger(Neo4jModellingConversation.class);
	
	private final GraphDataConnection connection;
	
	private final NeoConversationContext conversationContext;
	
	private final SemanticNetworkAccess sna;
	
	private final NeoResourceResolver resolver;
	
	// -----------------------------------------------------

	/**
	 * Create a new Modelling Conversation instance using a given data store.
	 */
	public Neo4jModellingConversation(final GraphDataConnection connection) {
		this(connection, new NeoConversationContext(connection));
	}

    /**
     * Create a new Modelling Conversation instance using a given data store.
     */
    public Neo4jModellingConversation(final GraphDataConnection connection, final NeoConversationContext context) {
        this.connection = connection;
        this.conversationContext = context;
        this.sna = new SemanticNetworkAccess(connection, context);
        this.resolver = new NeoResourceResolverImpl(connection, context);
    }
	
	// -----------------------------------------------------
	
	/**
	 * {@inheritDoc}
	 */
	public void addStatement(final Statement stmt) {
		assertActive();
		final ResourceNode subject = resolve(stmt.getSubject());
		SNOPS.associate(subject, stmt.getPredicate(), stmt.getObject(), stmt.getContexts());
	}
	
	/** 
	* {@inheritDoc}
	*/
	public boolean removeStatement(final Statement stmt) {
		assertActive();
		final ResourceNode subject = resolve(stmt.getSubject());
		return SNOPS.remove(subject, stmt.getPredicate(), stmt.getObject());
	}
	
	// ----------------------------------------------------
	
	/** 
	 * {@inheritDoc}
	 */
	public Query createQuery() {
		assertActive();
		return new NeoQueryBuilder(new ResourceIndex(connection, conversationContext));
	}
	
	// ----------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	public ResourceNode findResource(final QualifiedName qn) {
		assertActive();
		return resolver.findResource(qn);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ResourceNode resolve(final ResourceID resource) {
		assertActive();
		return resolver.resolve(resource);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void attach(final ResourceNode node) {
		assertActive();
		sna.attach(node);
	}
	
	/** 
	 * {@inheritDoc}
	 */
	public void reset(final ResourceNode node) {
		assertActive();
		sna.reset(node);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void detach(final ResourceNode node) {
		assertActive();
		sna.detach(node);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void remove(final ResourceID id) {
		assertActive();
		sna.remove(id);
	}
	
	// -----------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	public void attach(final SemanticGraph graph) {
		assertActive();
		connection.getTxProvider().doTransacted(new TxResultAction<SemanticGraph>() {
			public SemanticGraph execute() {
				for(Statement stmt : graph.getStatements()) {
					final ResourceNode subject = resolve(stmt.getSubject());
					SNOPS.associate(subject, stmt.getPredicate(), stmt.getObject(), stmt.getContexts());
				}
				return graph;
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	public void detach(final SemanticGraph graph) {
		assertActive();
		for(SemanticNode node : graph.getNodes()){
			if (node.isResourceNode() && node.asResource().isAttached()){
				sna.detach(node.asResource());
			}
		}
	}
	
	// -----------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	public ConversationContext getConversationContext() {
		assertActive();
		return conversationContext;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public TransactionControl beginTransaction() {
		assertActive();
		return connection.getTxProvider().begin();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void close() {
		conversationContext.close();
	}
	
	// ----------------------------------------------------
	
	private void assertActive() {
		if (!conversationContext.isActive()) {
			logger.warn("Conversation already closed.");
			//throw new IllegalStateException("Conversation already closed.");
		}
	}
	
}
