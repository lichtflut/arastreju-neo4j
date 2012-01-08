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

import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.ResourceResolver;
import org.neo4j.graphdb.Node;

/**
 * <p>
 *  Interface for resource resolving.
 * </p>
 *
 * <p>
 * 	Created Jan 14, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public interface NeoResourceResolver extends ResourceResolver {

	/**
	 * Find a resource by it's qualified name.
	 * @param qn The qualified name.
	 * @return The resource node or null.
	 */
	ResourceNode findResource(QualifiedName qn);

	/**
	 * Find a resource by it's corresponding neo node.
	 * @param neoNode The neo node.
	 * @return The resource node.
	 */
	ResourceNode resolve(Node neoNode);
	
}