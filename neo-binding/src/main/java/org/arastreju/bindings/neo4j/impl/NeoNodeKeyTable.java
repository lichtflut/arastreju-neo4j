package org.arastreju.bindings.neo4j.impl;

import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.NodeKeyTable;
import org.arastreju.sge.spi.PhysicalNodeID;
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
    public NeoPhysicalNodeID lookup(QualifiedName qn) {
        Node node = lookupSingleNode(qn);
        if (node != null) {
            return new NeoPhysicalNodeID(node.getId());
        } else {
            return null;
        }
    }

    @Override
    public void put(QualifiedName qn, PhysicalNodeID physicalID) {
        NeoPhysicalNodeID neoNodeID = (NeoPhysicalNodeID) physicalID;
        Node node = gdbService.getNodeById(neoNodeID.getId());
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
