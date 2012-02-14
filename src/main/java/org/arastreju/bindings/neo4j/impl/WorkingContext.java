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

import java.util.HashMap;
import java.util.Map;

import org.arastreju.bindings.neo4j.NeoConstants;
import org.arastreju.bindings.neo4j.extensions.NeoAssociationKeeper;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.naming.QualifiedName;

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
public class WorkingContext implements NeoConstants {

	private final Map<QualifiedName, NeoAssociationKeeper> register = new HashMap<QualifiedName, NeoAssociationKeeper>();
	
	private final AssociationHandler handler;
	
	// ----------------------------------------------------
	
	/**
	 * Creates a new Working Context.
	 * @param connection The connection.
	 */
	public WorkingContext(final GraphDataConnection connection) {
		this.handler = new AssociationHandler(connection);
	}

	// ----------------------------------------------------
	
	/**
	 * @param qn The resource's qualified name.
	 * @return The association keeper or null;
	 */
	public NeoAssociationKeeper getAssociationKeeper(QualifiedName qn) {
		return register.get(qn);
	}
	
	/**
	 * @param qn The resource's qualified name.
	 * @param resource
	 */
	public void attach(QualifiedName qn, NeoAssociationKeeper keeper) {
		register.put(qn, keeper);
		keeper.setWorkingContext(this);
	}
	
	/**
	 * @param qn The resource's qualified name.
	 */
	public void detach(QualifiedName qn) {
		final NeoAssociationKeeper removed = register.remove(qn);
		if (removed != null) {
			removed.setWorkingContext(null);
		}
	}
	
	/**
	 * Clear the cache.
	 */
	public void clear() {
		for (NeoAssociationKeeper keeper : register.values()) {
			keeper.setWorkingContext(null);
		}
		register.clear();
	}
	
	// ----------------------------------------------------

	/**
	 * Resolve the associations of given association keeper.
	 * @param keeper The association keeper to be resolved.
	 */
	public void resolveAssociations(final NeoAssociationKeeper keeper) {
		handler.resolveAssociations(keeper);
	}
	
	// ----------------------------------------------------
	
	/**
	 * Add a new Association to given Neo node, or rather create a corresponding Relation.
	 * @param subject The neo node, which shall be the subject in the new Relation.
	 * @param stmt The Association.
	 */
	public void addAssociation(final NeoAssociationKeeper keeper, final Statement stmt) {
		handler.addAssociation(keeper, stmt);
	}

	/**
	 * Remove the given association.
	 * @param keeper The keeper.
	 * @param assoc The association.
	 * @return true if the association has been removed.
	 */
	public boolean removeAssociation(final NeoAssociationKeeper keeper, final Statement assoc) {
		return handler.removeAssociation(keeper, assoc);
	}
	
}
