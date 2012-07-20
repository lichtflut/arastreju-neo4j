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
package org.arastreju.bindings.neo4j.extensions;

import org.arastreju.bindings.neo4j.NeoConstants;
import org.arastreju.bindings.neo4j.impl.NeoConversationContext;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.associations.AbstractAssociationKeeper;
import org.arastreju.sge.model.associations.AssociationKeeper;
import org.arastreju.sge.model.associations.DetachedAssociationKeeper;
import org.arastreju.sge.naming.QualifiedName;
import org.neo4j.graphdb.Node;

/**
 * <p>
 *  Special {@link AssociationKeeper} for Neo4J.
 * </p>
 *
 * <p>
 * 	Created Oct 11, 2010
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoAssociationKeeper extends AbstractAssociationKeeper implements NeoConstants {

	private final ResourceID id;
	
	private final Node neoNode;
	
	private NeoConversationContext context;
	
	// -----------------------------------------------------
	
	/**
	 * Create a new association keeper.
	 * @param id The node's ID.
	 * @param neoNode The neo node.
	 */
	public NeoAssociationKeeper(final ResourceID id, final Node neoNode) {
		this.id = id;
		this.neoNode = neoNode;
	}
	
	// -----------------------------------------------------
	
	public Node getNeoNode() {
		return neoNode;
	}

	public QualifiedName getQualifiedName() {
		return id.getQualifiedName();
	}
	
	public ResourceID getID() {
		return id;
	}
	
	// -----------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addAssociation(final Statement assoc) {
		if (getAssociations().contains(assoc)) {
			return;
		}
		if (isAttached()) {
			context.addAssociation(this, assoc);
		} else {
			super.addAssociation(assoc);
		}
	}
	
	/** 
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeAssociation(final Statement assoc) {
		if (isAttached()) {
			getAssociations().remove(assoc);
			return context.removeAssociation(this, assoc);
		} else {
			return super.removeAssociation(assoc);
		}
	}
	
	// ----------------------------------------------------
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isAttached() {
		return context != null && context.isActive();
	}
	

	/**
	 * Detaches this node from the working context.
	 */
	public void detach() {
		markResolved();
		this.context = null;
	}
	
	/**
	 * Set the working context.
	 * @param context the context to set
	 */
	public void setWorkingContext(NeoConversationContext context) {
		this.context = context;
	}
	
	// ----------------------------------------------------

	/**
	 * Add an association directly to the set, without resolving.
	 * @param assoc The association to add.
	 */
	public void addAssociationDirectly(final Statement assoc) {
		getAssociationsDirectly().add(assoc);
	}
	
	// ----------------------------------------------------
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void resolveAssociations() {
		if (isAttached()) {
			context.resolveAssociations(this);
		} else {
			throw new IllegalStateException("This node is no longer attached. Cannot resolve associations.");
		}
	}
	
	// ----------------------------------------------------
	
	/**
	 * Called when being serialized --> Replace by detached association keeper.
	 * @return A Detached Association Keeper.
	 */
	private Object writeReplace() {
		return new DetachedAssociationKeeper(getAssociationsDirectly());
	}

}
