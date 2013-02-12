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

import org.arastreju.bindings.neo4j.NeoConstants;
import org.arastreju.bindings.neo4j.extensions.NeoAssociationKeeper;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.associations.AssociationKeeper;
import org.arastreju.sge.model.associations.DetachedAssociationKeeper;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.persistence.TxAction;
import org.arastreju.sge.persistence.TxProvider;
import org.arastreju.sge.spi.AssocKeeperAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * <p>
 *  The Neo4jDataStore consists of three data containers:
 *  <ul>
 *  	<li>The Graph Database Service, containing the actual graph</li>
 *  	<li>An Index Service, mapping URLs and keywords to nodes</li>
 *  	<li>A Registry mapping QualifiedNames to Arastreju Resources</li>
 *  </ul>
 * </p>
 *
 * <p>
 * 	Created Sep 2, 2010
 * </p>
 *
 * @author Oliver Tigges
 */
public class SemanticNetworkAccess implements NeoConstants {

    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticNetworkAccess.class);
	
	private final NeoConversationContext conversationContext;

	// -----------------------------------------------------

	/**
	 * Constructor. Creates a store using given directory.
     * @param conversationContext The conversation context.
     */
	public SemanticNetworkAccess(final NeoConversationContext conversationContext) {
		this.conversationContext = conversationContext;
	}

	// -----------------------------------------------------

    public void create(final ResourceNode resource) {
        tx().doTransacted(new TxAction() {
            public void execute() {
                persist(resource);
            }
        });
    }

	/**
	 * Attach the given node if it is not already attached.
	 * @param resource The node to attach.
	 */
	public void attach(final ResourceNode resource) {
		// 1st: check if node is already attached.
		if (resource.isAttached()){
            verifySameContext(resource);
			return;
		}
		tx().doTransacted(new TxAction() {
			public void execute() {
				// 2nd: check if node for qualified name exists and has to be merged
                final AssociationKeeper attachedKeeper = conversationContext.find(resource.getQualifiedName());
				if (attachedKeeper != null){
					merge(attachedKeeper, resource);
				} else {
					// 3rd: if resource is really new, create a new Neo node.
					persist(resource);
				}
			}
		});
	}

    /**
	 * Unregister the node from the registry and detach the {@link AssociationKeeper}
	 * @param node The node to detach.
	 */
	public void detach(final ResourceNode node){
        AssocKeeperAccess.getInstance().setAssociationKeeper(
                node, new DetachedAssociationKeeper(node.getAssociations()));
		conversationContext.detach(node.getQualifiedName());
	}
	
	// -----------------------------------------------------
	
	/**
	 * Create the given resource node in Neo4j DB.
	 * @param node A not yet persisted node.
	 * @return The persisted ResourceNode.
	 */
	protected ResourceNode persist(final ResourceNode node) {
		// 1st: create a corresponding Neo node and attach the Resource with the current context.
        NeoAssociationKeeper keeper = conversationContext.create(node.getQualifiedName());

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
