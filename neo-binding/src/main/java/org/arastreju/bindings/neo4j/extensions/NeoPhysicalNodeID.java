/*
 * Copyright (C) 2013 lichtflut Forschungs- und Entwicklungsgesellschaft mbH
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

import org.arastreju.sge.spi.PhysicalNodeID;
import org.neo4j.graphdb.Node;

/**
 * <p>
 *  In Neo4j the internal technical ID of a node is a Long value.
 * </p>
 *
 * <p>
 *  Created Feb. 12, 2013
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoPhysicalNodeID implements PhysicalNodeID {

    private final long id;

    // ----------------------------------------------------

    public NeoPhysicalNodeID(long id) {
        this.id = id;
    }

    public NeoPhysicalNodeID(Node node) {
        id = node.getId();
    }

    // ----------------------------------------------------+

    public long getId() {
        return id;
    }

}
