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

import org.arastreju.bindings.neo4j.extensions.NeoAssociationKeeper;
import org.arastreju.bindings.neo4j.extensions.SNResourceNeo;
import org.arastreju.bindings.neo4j.impl.NeoConversationContext;
import org.arastreju.bindings.neo4j.impl.NeoGraphDataConnection;
import org.arastreju.bindings.neo4j.impl.NeoResourceResolver;
import org.arastreju.bindings.neo4j.impl.SemanticNetworkAccess;
import org.arastreju.bindings.neo4j.index.ResourceIndex;
import org.arastreju.bindings.neo4j.query.NeoQueryBuilder;
import org.arastreju.sge.Conversation;
import org.arastreju.sge.index.QNResolver;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.TxResultAction;
import org.arastreju.sge.query.Query;
import org.arastreju.sge.spi.AssocKeeperAccess;
import org.arastreju.sge.spi.abstracts.AbstractConversation;

import java.util.Set;

/**
 * <p>
 *  Implementation of {@link org.arastreju.sge.Conversation} for Neo4j.
 * </p>
 *
 * <p>
 * 	Created Sep 2, 2010
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoConversation extends AbstractConversation implements Conversation {
	
	private final NeoConversationContext conversationContext;
	
	private final SemanticNetworkAccess sna;
	
	// -----------------------------------------------------

    /**
     * Create a new Modelling Conversation instance using a given data store.
     */
    public NeoConversation(final NeoGraphDataConnection connection, final NeoConversationContext context) {
        super(context);
        this.conversationContext = context;
        this.sna = new SemanticNetworkAccess(connection, context);
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

	protected QNResolver getQNResolver() {
		return new NeoResourceResolver((NeoGraphDataConnection) conversationContext.getConnection(), conversationContext);
	}

    @Override
	public ResourceNode findResource(final QualifiedName qn) {
        NeoAssociationKeeper existing = getConversationContext().find(qn);
        if (existing != null) {
            return new SNResourceNeo(qn, existing);
        }
        return null;
	}
	
	@Override
    public ResourceNode resolve(final ResourceID resource) {
        NeoAssociationKeeper existing = getConversationContext().find(resource.getQualifiedName());
        if (existing != null) {
            return new SNResourceNeo(resource.getQualifiedName(), existing);
        } else {
            return create(resource.getQualifiedName());
        }
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
        if (isAttached(node)) {
            return;
        }
        NeoAssociationKeeper existing = getConversationContext().find(node.getQualifiedName());
        if (existing != null) {
            AssocKeeperAccess.getInstance().setAssociationKeeper(node, existing);
        } else {
            throw new IllegalStateException("Detached node cannot be reset.");
        }
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

    public NeoConversationContext getConversationContext() {
        return (NeoConversationContext) super.getConversationContext();
    }

    // ----------------------------------------------------

    protected ResourceNode create(final QualifiedName qn) {
        return conversationContext.getTxProvider().doTransacted(new TxResultAction<ResourceNode>() {
            @Override
            public ResourceNode execute() {
                NeoAssociationKeeper created = getConversationContext().create(qn);
                SNResourceNeo createdResource = new SNResourceNeo(qn, created);
                new ResourceIndex(getConversationContext()).index(created.getNeoNode(), createdResource);
                return createdResource;
            }
        });
    }
	
}
