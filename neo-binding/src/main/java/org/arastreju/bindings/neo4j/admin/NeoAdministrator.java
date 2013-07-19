package org.arastreju.bindings.neo4j.admin;

import org.arastreju.bindings.neo4j.storage.NeoConstants;
import org.arastreju.bindings.neo4j.storage.NeoGraphDataStore;
import org.arastreju.sge.ArastrejuGate;
import org.arastreju.sge.eh.meta.NotYetImplementedException;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.spi.admin.AbstractAdministrator;
import org.neo4j.graphdb.Node;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.Iterator;

/**
 * <p>
 *  Neo specific implementation of Administrator.
 * </p>
 *
 * <p>
 *  Created 10.05.13
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoAdministrator extends AbstractAdministrator {

    private final NeoGraphDataStore store;

    private final ArastrejuGate gate;

    // ----------------------------------------------------

    public NeoAdministrator(NeoGraphDataStore store, ArastrejuGate gate) {
        this.store = store;
        this.gate = gate;
    }

    // ----------------------------------------------------

    public Iterator<QualifiedName> allNodes() {
        GlobalGraphOperations ggo = GlobalGraphOperations.at(store.getGraphDatabaseService());
        final Iterator<Node> neoNodesIterator = ggo.getAllNodes().iterator();
        return new Iterator<QualifiedName>() {

            private QualifiedName prefetch;

            @Override
            public boolean hasNext() {
                return prefetch();
            }

            @Override
            public QualifiedName next() {
                prefetch();
                return prefetch;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            private boolean prefetch() {
                if (prefetch != null) {
                    return true;
                } else {
                    while (neoNodesIterator.hasNext()) {
                        Node node = neoNodesIterator.next();
                        final Object uriProperty = node.getProperty(NeoConstants.PROPERTY_URI, null);
                        if (uriProperty != null) {
                            prefetch = QualifiedName.from(uriProperty.toString());
                            return true;
                        }
                    }
                }
                return false;
            }

        };

    }

    // ----------------------------------------------------

    @Override
    public void reIndex() {
        throw new NotYetImplementedException();
    }

    @Override
    public void reInference() {
        throw new NotYetImplementedException();
    }

    @Override
    public void backup() {
        throw new NotYetImplementedException();
    }

    @Override
    public void restore() {
        throw new NotYetImplementedException();
    }

}
