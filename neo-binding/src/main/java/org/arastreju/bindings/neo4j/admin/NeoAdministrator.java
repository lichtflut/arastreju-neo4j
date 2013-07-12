package org.arastreju.bindings.neo4j.admin;

import org.arastreju.bindings.neo4j.storage.NeoConstants;
import org.arastreju.bindings.neo4j.storage.NeoGraphDataStore;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.spi.admin.AbstractAdministrator;
import org.neo4j.graphdb.Node;
import org.neo4j.tooling.GlobalGraphOperations;

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

    // ----------------------------------------------------

    public NeoAdministrator(NeoGraphDataStore store) {
        this.store = store;
    }

    // ----------------------------------------------------

    @Override
    public void reIndex() {
        GlobalGraphOperations ggo = GlobalGraphOperations.at(store.getGraphDatabaseService());
        for (Node node : ggo.getAllNodes()) {
            final Object uriProperty = node.getProperty(NeoConstants.PROPERTY_URI, null);
            if (uriProperty == null) {
                throw new IllegalStateException();
            }
            final QualifiedName qn = QualifiedName.from(uriProperty.toString());
        }
    }

    @Override
    public void reInference() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void backup() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void restore() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    // ----------------------------------------------------



}
