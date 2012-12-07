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

import java.util.Set;

import org.arastreju.bindings.neo4j.impl.GraphDataConnection;
import org.arastreju.bindings.neo4j.impl.NeoConversationContext;
import org.arastreju.bindings.neo4j.impl.NeoResourceResolver;
import org.arastreju.bindings.neo4j.impl.SemanticNetworkAccess;
import org.arastreju.bindings.neo4j.index.ResourceIndex;
import org.arastreju.bindings.neo4j.query.NeoQueryBuilder;
import org.arastreju.sge.ModelingConversation;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.ResourceResolver;
import org.arastreju.sge.query.Query;
import org.arastreju.sge.spi.abstracts.AbstractModelingConversation;
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
public class Neo4jModellingConversation extends AbstractModelingConversation implements ModelingConversation {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Neo4jModellingConversation.class);

    // ----------------------------------------------------
	
	private final NeoConversationContext conversationContext;
	
	private final SemanticNetworkAccess sna;
	
	private final ResourceResolver resolver;
	
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
        super(context);
        this.conversationContext = context;
        this.sna = new SemanticNetworkAccess(connection, context);
        this.resolver = new NeoResourceResolver(connection, context);
    }
	
    // ----------------------------------------------------
	
	@Override
	public Query createQuery() {
		assertActive();
		return new NeoQueryBuilder(new ResourceIndex(conversationContext));
	}

    @Override
    public Set<Statement> findIncomingStatements(ResourceID object) {
        assertActive();
        return conversationContext.getIncomingStatements(object);
    }

    // ----------------------------------------------------

    @Override
	public ResourceNode findResource(final QualifiedName qn) {
		assertActive();
		return resolver.findResource(qn);
	}
	
	@Override
    public ResourceNode resolve(final ResourceID resource) {
		assertActive();
		return resolver.resolve(resource);
	}

    // ----------------------------------------------------

    @Override
	public void attach(final ResourceNode node) {
		assertActive();
		sna.attach(node);
	}

    @Override
	public void reset(final ResourceNode node) {
		assertActive();
		sna.reset(node);
	}

    @Override
	public void detach(final ResourceNode node) {
		assertActive();
		sna.detach(node);
	}

    @Override
	public void remove(final ResourceID id) {
		assertActive();
		sna.remove(id);
	}
	
	// ----------------------------------------------------
	
	@Override
    protected void assertActive() {
		if (!conversationContext.isActive()) {
			LOGGER.warn("Conversation already closed.");
			//throw new IllegalStateException("Conversation already closed.");
		}
	}
	
}
