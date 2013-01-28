package org.arastreju.bindings.neo4j.index;

import de.lichtflut.infra.exceptions.NotYetSupportedException;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.NodeKeyTable;
import org.arastreju.sge.spi.PhysicalNodeID;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
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

    private IndexManager manager;

    // ----------------------------------------------------

    public NeoNodeKeyTable(IndexManager indexManager) {
        this.manager = indexManager;
    }

    // ----------------------------------------------------

    @Override
    public PhysicalNodeID lookup(QualifiedName qn) {
        Node single = qnIndex().get(NeoIndex.INDEX_KEY_RESOURCE_URI, NeoIndex.normalize(qn.toURI())).getSingle();
        single.getId();
        return null;
    }

    @Override
    public void put(QualifiedName qn, PhysicalNodeID physicalID) {
        throw new NotYetSupportedException();
    }

    // ----------------------------------------------------

    private Index<Node> qnIndex() {
        return manager.forNodes(NeoIndex.INDEX_RESOURCES);
    }

}
