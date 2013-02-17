package org.arastreju.bindings.neo4j.extensions;

import org.arastreju.sge.spi.PhysicalNodeID;
import org.neo4j.graphdb.Node;

/**
 * <p>
 *  In Neo4j the internal technical ID of a node is a Long value.
 * </p>
 *
 * <p>
 *  Created 12.02.13
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
