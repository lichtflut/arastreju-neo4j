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

import java.util.Collections;
import java.util.Set;

import org.arastreju.bindings.neo4j.NeoConstants;
import org.arastreju.bindings.neo4j.impl.AssociationHandler;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.associations.AbstractAssociationKeeper;
import org.arastreju.sge.model.associations.AssociationKeeper;
import org.arastreju.sge.model.associations.DetachedAssociationKeeper;
import org.arastreju.sge.naming.QualifiedName;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	private final AssociationHandler handler;
	
	private final Logger logger = LoggerFactory.getLogger(NeoAssociationKeeper.class);
	
	// -----------------------------------------------------
	
	/**
	 * Create a new association keeper.
	 * @param id The node's ID.
	 * @param neoNode The neo node.
	 * @param handler A link to the association handler.
	 */
	public NeoAssociationKeeper(final ResourceID id, final Node neoNode, final AssociationHandler handler) {
		this.id = id;
		this.neoNode = neoNode;
		this.handler = handler;
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
		if (!getAssociations().contains(assoc)) {
			handler.addAssociation(this, assoc);
			logger.debug("Added Association: " + assoc);	
		}
	}
	
	/** 
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeAssociation(final Statement assoc) {
		super.removeAssociation(assoc);
		logger.debug("Removed Association: " + assoc);
		return handler.removeAssociation(this, assoc);
	}
	
	// ----------------------------------------------------
	
	/**
	 * {@inheritDoc}
	 */
	public Set<Statement> getAssociationsForRemoval() {
		return Collections.emptySet();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isAttached() {
		return true;
	}
	
	// ----------------------------------------------------
	
	/**
	 * Add the association directly to the associations.
	 */
	public void addAssociationDirectly(final Statement assoc) {
		super.addAssociation(assoc);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void resolveAssociations() {
		handler.resolveAssociations(this);
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
