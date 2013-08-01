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
