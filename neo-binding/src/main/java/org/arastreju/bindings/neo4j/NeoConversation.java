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
import org.arastreju.sge.Conversation;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.index.QNResolver;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.associations.AssociationKeeper;
import org.arastreju.sge.model.associations.DetachedAssociationKeeper;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.TxAction;
import org.arastreju.sge.persistence.TxProvider;
import org.arastreju.sge.persistence.TxResultAction;
import org.arastreju.sge.spi.AssocKeeperAccess;
import org.arastreju.sge.spi.abstracts.AbstractConversation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(NeoConversation.class);
	
	private final NeoConversationContext conversationContext;
	
	// -----------------------------------------------------

    /**
     * Create a new Modelling Conversation instance using a given data store.
     */
    public NeoConversation(final NeoConversationContext context) {
        super(context);
        this.conversationContext = context;
    }
	
    // ----------------------------------------------------

    @Override
    public Set<Statement> findIncomingStatements(ResourceID object) {
        assertActive();
        return conversationContext.getIncomingStatements(object);
    }

    // ----------------------------------------------------

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
	public void attach(final ResourceNode resource) {
		assertActive();
        // 1st: check if node is already attached.
        if (resource.isAttached()){
            verifySameContext(resource);
            return;
        }
        tx().doTransacted(new TxAction() {
            public void execute() {
                // 2nd: check if node for qualified name exists and has to be merged
                final AssociationKeeper attachedKeeper = conversationContext.find(resource.getQualifiedName());
                if (attachedKeeper != null) {
                    merge(attachedKeeper, resource);
                } else {
                    // 3rd: if resource is really new, create a new Neo node.
                    persist(resource);
                }
            }
        });
	}

    @Override
	public void detach(final ResourceNode node) {
		assertActive();
        AssocKeeperAccess.getInstance().setAssociationKeeper(
                node, new DetachedAssociationKeeper(node.getAssociations()));
        conversationContext.detach(node.getQualifiedName());
	}

    @Override
	public void remove(final ResourceID id) {
		assertActive();
        conversationContext.remove(id.getQualifiedName());
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
	
	// ----------------------------------------------------

    public NeoConversationContext getConversationContext() {
        return (NeoConversationContext) super.getConversationContext();
    }

    // ----------------------------------------------------

    @Override
    protected QNResolver getQNResolver() {
        return new QNResolver() {
            @Override
            public ResourceNode resolve(QualifiedName qn) {
                return NeoConversation.this.resolve(SNOPS.id(qn));
            }
        };
    }

    protected ResourceNode create(final QualifiedName qn) {
        return conversationContext.getTxProvider().doTransacted(new TxResultAction<ResourceNode>() {
            @Override
            public ResourceNode execute() {
                NeoAssociationKeeper created = getConversationContext().create(qn);
                return new SNResourceNeo(qn, created);
            }
        });
    }

    /**
     * Create the given resource node in Neo4j DB.
     * @param node A not yet persisted node.
     * @return The persisted ResourceNode.
     */
    protected ResourceNode persist(final ResourceNode node) {
        // 1st: create a corresponding Neo node and attach the Resource with the current context.
        AssociationKeeper keeper = conversationContext.create(node.getQualifiedName());

        // 2nd: retain copy of current associations
        final Set<Statement> copy = node.getAssociations();
        AssocKeeperAccess.getInstance().setAssociationKeeper(node, keeper);

        // 3rd: store all associations.
        for (Statement assoc : copy) {
            keeper.addAssociation(assoc);
        }

        return node;
    }

    /**
     * Merges all associations from the 'changed' node to the 'attached' keeper and put's keeper in 'changed'.
     * @param attached The currently attached keeper for this resource.
     * @param changed An unattached node referencing the same resource.
     */
    protected void merge(final AssociationKeeper attached, final ResourceNode changed) {
        final AssociationKeeper detached = AssocKeeperAccess.getInstance().getAssociationKeeper(changed);
        AssocKeeperAccess.getInstance().merge(attached, detached);
        AssocKeeperAccess.getInstance().setAssociationKeeper(changed, attached);
        conversationContext.attach(changed.getQualifiedName(), (NeoAssociationKeeper) attached);
    }

    // ----------------------------------------------------

    private void verifySameContext(ResourceNode resource) {
        AssociationKeeper given = AssocKeeperAccess.getInstance().getAssociationKeeper(resource);
        if (!given.getConversationContext().equals(conversationContext)) {
            LOGGER.warn("Resource {} is not in current conversation context {}: ", resource, conversationContext);
        }
    }

    private TxProvider tx() {
        return conversationContext.getTxProvider();
    }
	
}
