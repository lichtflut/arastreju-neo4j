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
import org.arastreju.bindings.neo4j.index.ResourceIndex;
import org.arastreju.bindings.neo4j.tx.NeoTxProvider;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.eh.ArastrejuRuntimeException;
import org.arastreju.sge.eh.ErrorCodes;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.associations.AssociationKeeper;
import org.arastreju.sge.model.associations.DetachedAssociationKeeper;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.TxAction;
import org.neo4j.graphdb.Node;

import java.util.HashSet;
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
	
	private final GraphDataConnection connection;
	private final NeoConversationContext conversationContext;
	private final ResourceIndex index;

	// -----------------------------------------------------

	/**
	 * Constructor. Creates a store using given directory.
	 * @param connection The connection to the graph database.
	 * @param conversationContext The conversation context.
	 */
	public SemanticNetworkAccess(final GraphDataConnection connection, final NeoConversationContext conversationContext) {
		this.connection = connection;
		this.conversationContext = conversationContext;
		this.index = new ResourceIndex(connection, conversationContext);
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
			return;
		}
		tx().doTransacted(new TxAction() {
			public void execute() {
				// 2nd: check if node for qualified name exists and has to be merged
				final AssociationKeeper attachedKeeper = findAssociationKeeper(resource.getQualifiedName());
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
		NeoAssocKeeperAccess.setAssociationKeeper(node, new DetachedAssociationKeeper(node.getAssociations()));
		conversationContext.detach(node.getQualifiedName());
	}
	
	/**
	 * Reset the given node if it is detached.
	 * @param node The node to be reseted.
	 */
	public void reset(final ResourceNode node) {
		// 1st: check if node is detached.
		if (!node.isAttached()){
			return;
		}
		final AssociationKeeper keeper = findAssociationKeeper(node.getQualifiedName());
		if (keeper != null) {
			NeoAssocKeeperAccess.setAssociationKeeper(node, keeper);
		} else {
			throw new ArastrejuRuntimeException(ErrorCodes.GENERAL_CONSISTENCY_FAILURE, 
					"Can't find node/keeper for attached node " + node.getQualifiedName());
		}
	}
	
	/**
	 * Remove the node.
	 * @param id The ID.
	 */
	public void remove(final ResourceID id) {
        final NeoAssociationKeeper registered = conversationContext.getAssociationKeeper(id.getQualifiedName());
		registered.getAssociations().clear();
		tx().doTransacted(new TxAction() {
			public void execute() {
				new NodeRemover(connection, conversationContext).remove(registered.getNeoNode(), false);
			}
		});
	}
	
	// -----------------------------------------------------
	
	/**
	 * Find the keeper for a qualified name.
	 * @param qn The qualified name.
	 * @return The keeper or null.
	 */
	protected AssociationKeeper findAssociationKeeper(final QualifiedName qn) {
		final AssociationKeeper registered = conversationContext.getAssociationKeeper(qn);
		if (registered != null) {
			return registered;
		}
		final Node neoNode = index.findNeoNode(qn);
		if (neoNode != null) {
			return createKeeper(qn, neoNode);
		} else {
			return null;
		}
	}
	
	/**
	 * Create the given resource node in Neo4j DB.
	 * @param node A not yet persisted node.
	 * @return The persisted ResourceNode.
	 */
	protected ResourceNode persist(final ResourceNode node) {
		// 1st: create a corresponding Neo node.
		final Node neoNode = connection.getStore().getGdbService().createNode();
		neoNode.setProperty(PROPERTY_URI, node.getQualifiedName().toURI());
		
		// 2nd: retain copy of current associations and attach the Resource with this store.
		final Set<Statement> copy = node.getAssociations();
		final NeoAssociationKeeper keeper = createKeeper(node.getQualifiedName(), neoNode);
		NeoAssocKeeperAccess.setAssociationKeeper(node, keeper);
		
		// 3rd: index the Neo node.
		index.index(neoNode, node);
		
		// 4th: store all associations.
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
		final Set<Statement> currentAssocs = new HashSet<Statement>(attached.getAssociations());
		final AssociationKeeper detached = NeoAssocKeeperAccess.getAssociationKeeper(changed);
		for (Statement toBeRemoved : detached.getAssociationsForRemoval()) {
			attached.removeAssociation(toBeRemoved);
		}
		for(Statement assoc : detached.getAssociations()){
			if (!currentAssocs.contains(assoc)){
				attached.addAssociation(assoc);
			}
		}
		NeoAssocKeeperAccess.setAssociationKeeper(changed, attached);
	}
	
	protected NeoAssociationKeeper createKeeper(QualifiedName qn, Node neoNode) {
		final NeoAssociationKeeper keeper = new NeoAssociationKeeper(SNOPS.id(qn), neoNode);
		conversationContext.attach(qn, keeper);
		return keeper;
	}
	
	// ----------------------------------------------------
	
	private NeoTxProvider tx() {
		return conversationContext.getTxProvider();
	}
	
}
