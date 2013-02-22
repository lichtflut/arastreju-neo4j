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
package org.arastreju.bindings.neo4j.storage;

import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.NodeKeyTable;
import org.arastreju.sge.spi.PhysicalNodeID;
import org.arastreju.sge.spi.impl.NumericPhysicalNodeID;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;

/**
 * <p>
 *  Neo specific node key table mapping qualified names to Neo node IDs.
 *  Will be removed by a lucene based implementation soon.
 * </p>
 *
 * <p>
 *  Created 27.01.13
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoNodeKeyTable implements NodeKeyTable {

    private static final String INDEX_NAME = "ARAS-NEO-QN";

    private static final String KEY_QN = "qn";

    private GraphDatabaseService gdbService;
    private IndexManager manager;

    // ----------------------------------------------------

    public NeoNodeKeyTable(GraphDatabaseService gdbService, IndexManager indexManager) {
        this.gdbService = gdbService;
        this.manager = indexManager;
    }

    // ----------------------------------------------------

    @Override
    public NumericPhysicalNodeID lookup(QualifiedName qn) {
        Node node = lookupSingleNode(qn);
        if (node != null) {
            return new NumericPhysicalNodeID(node.getId());
        } else {
            return null;
        }
    }

    @Override
    public void put(QualifiedName qn, PhysicalNodeID physicalID) {
        NumericPhysicalNodeID neoNodeID = (NumericPhysicalNodeID) physicalID;
        Node node = gdbService.getNodeById(neoNodeID.asLong());
        put(qn, node);
    }

    @Override
    public void remove(QualifiedName qn) {
        Node node = lookupSingleNode(qn);
        if (node != null) {
            qnIndex().remove(node);
        }
    }

    // ----------------------------------------------------

    public void put(QualifiedName qn, Node node) {
        Node existing = lookupSingleNode(qn);
        if (existing != null) {
            throw new IllegalStateException("There is already a node registered for qn: " + qn);
        }
        qnIndex().add(node, KEY_QN, normalize(qn.toURI()));
    }

    // ----------------------------------------------------

    private Node lookupSingleNode(QualifiedName qn) {
        IndexHits<Node> hits = qnIndex().get(KEY_QN, normalize(qn.toURI()));
        if (hits.size() > 1) {
            throw new IllegalStateException("Found more than one node with QN " + qn);
        } else if (hits.size() == 0) {
            return null;
        } else {
            return hits.getSingle();
        }
    }

    private Index<Node> qnIndex() {
        return manager.forNodes(INDEX_NAME);
    }

    private String normalize(final String s) {
        return s.trim().toLowerCase();
    }

}
