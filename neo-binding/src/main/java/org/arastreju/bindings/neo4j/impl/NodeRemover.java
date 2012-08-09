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

import org.arastreju.bindings.neo4j.index.ResourceIndex;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 *  Remover of Neo Nodes from datastore.
 *  TODO: always remove value nodes
 * </p>
 *
 * <p>
 * 	Created Sep 21, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class NodeRemover {

	private final ResourceIndex index;
    private final NeoConversationContext context;

    // -----------------------------------------------------

	/**
	 * Constructor.
	 * @param context The context.
	 */
	public NodeRemover(NeoConversationContext context) {
        this.context = context;
        this.index = new ResourceIndex(context.getConnection(), context);
	}
	
	// -----------------------------------------------------

    /**
     * Remove the given node.
     * @param neoNode The node.
     * @param cascade Flag if removing shall be cascaded.
     * @return The set of removed nodes.
     */
    public Set<Node> remove(final Node neoNode, final boolean cascade) {
        final Set<Node> deleted = new HashSet<Node>();
        remove(neoNode, deleted, cascade);
        return deleted;
    }

	// -----------------------------------------------------

	private void remove(final Node neoNode, final Set<Node> deleted, final boolean cascade) {
		// 2nd: delete relations
		final List<Node> cascading = new ArrayList<Node>();
		for (Relationship rel : neoNode.getRelationships()) {
			cascading.add(rel.getEndNode());
			rel.delete();
		}
		
		// 3rd: delete neo node
		index.removeFromIndex(neoNode);
		neoNode.delete();
		deleted.add(neoNode);

		// 4th: cascade
		if (cascade) {
			for(Node c : cascading) {
				if (!deleted.contains(c) && !c.hasRelationship(Direction.INCOMING)) {
					remove(c, deleted, cascade);
				}
			}
		}
	}

}
