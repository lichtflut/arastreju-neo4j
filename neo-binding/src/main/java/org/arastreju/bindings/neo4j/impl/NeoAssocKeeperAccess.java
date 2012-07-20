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

import org.arastreju.bindings.neo4j.extensions.NeoAssociationKeeper;
import org.arastreju.sge.model.associations.AssociationKeeper;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.SNResource;
import org.arastreju.sge.spi.AssocKeeperAccess;
import org.neo4j.graphdb.Node;

/**
 * <p>
 *  Accessor for {@link AssociationKeeper} of a Resource Node. 
 * </p>
 *
 * <p>
 * 	Created Jan 14, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoAssocKeeperAccess {
	
	/**
	 * Get the association keeper of given node.
	 */
	public static AssociationKeeper getAssociationKeeper(final ResourceNode node){
        return AssocKeeperAccess.getInstance().getAssociationKeeper(node);
	}
	
	/**
	 * Get the neo association keeper of given node.
	 */
	public static NeoAssociationKeeper getNeoAssociationKeeper(final ResourceNode node){
		return (NeoAssociationKeeper) getAssociationKeeper(node);
	}
	
	/**
	 * Get the Neo4j node attached to the Arastreju node's association keeper.
	 */
	public static Node getNeoNode(final ResourceNode node){
		try {
			NeoAssociationKeeper keeper = (NeoAssociationKeeper) getAssociationKeeper(node);
			return keeper.getNeoNode();
		} catch (ClassCastException e){
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Set the given association keeper to the resource node.
	 * @param node Must be an instance of {@link SNResource}.
	 * @param ak The association keeper to be set. 
	 */
	public static void setAssociationKeeper(final ResourceNode node, final AssociationKeeper ak) {
        AssocKeeperAccess.getInstance().setAssociationKeeper(node, ak);
	}
	

}
