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
package org.arastreju.bindings.neo4j.util.test;

import org.arastreju.bindings.neo4j.NeoConstants;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 * <p>
 *  Dumper for Nodes and Relationships.
 * </p>
 *
 * <p>
 * 	Created Dec 16, 2010
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoGraphDumper implements NeoConstants {
	
	/**
	 * Dumps just the given Neo node.
	 * @param neoNode The node to be dumped. 
	 * @return The corresponding string.
	 */
	public static String dump(final Node neoNode){
		final StringBuilder sb = new StringBuilder("node[");
		if (neoNode.hasProperty(PROPERTY_URI)){
			sb.append(neoNode.getProperty(PROPERTY_URI));
		} else if (neoNode.hasProperty(PROPERTY_VALUE)) {
			sb.append(neoNode.getProperty(PROPERTY_VALUE));
		} else {
			sb.append(neoNode.getId() + "|" + System.identityHashCode(neoNode));
		}
		sb.append("]");
		return sb.toString();
	}
	
	/**
	 * Dumps the given Neo node with all relations.
	 * @param neoNode The node to be dumped. 
	 * @return The corresponding string.
	 */
	public static String dumpDeep(final Node neoNode){
		final StringBuilder sb = new StringBuilder("node[" + neoNode.getId() + "|" + 
				System.identityHashCode(neoNode) + "]");
		for (Relationship rel : neoNode.getRelationships()) {
			sb.append("\n * " + dump(rel));
		}
		return sb.toString();
	}
	
	/**
	 * Dumps the relation.
	 * @param rel The relation.
	 * @return The corresponding string.
	 */
	public static String dump(final Relationship rel){
		final StringBuilder sb = new StringBuilder("rel[");
		sb.append(dump(rel.getStartNode()));
		if (rel.hasProperty(PREDICATE_URI)){
			sb.append(rel.getProperty(PREDICATE_URI));
		} else {
			sb.append(rel.getId() + "|" + System.identityHashCode(rel));
		}
		sb.append(dump(rel.getEndNode()));
		return sb.toString();
	}

}
