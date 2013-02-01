/*
 * Copyright 2012 by lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 */
package org.arastreju.bindings.neo4j.impl;

import org.arastreju.bindings.neo4j.extensions.NeoAssociationKeeper;
import org.arastreju.bindings.neo4j.tx.NeoTxProvider;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.spi.abstracts.AbstractConversationContext;
import org.arastreju.sge.spi.abstracts.AbstractGraphDataConnection;
import org.arastreju.sge.spi.abstracts.WorkingContext;
import org.neo4j.graphdb.index.IndexManager;

/**
 * <p>
 *  Connection to a graph data store.
 * </p>
 *
 * <p>
 * 	Created Feb 13, 2012
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoGraphDataConnection extends AbstractGraphDataConnection<NeoAssociationKeeper> {

	private final NeoGraphDataStore store;
	
	// ----------------------------------------------------

	/**
	 * Constructor.
	 * @param store The store.
	 */
	public NeoGraphDataConnection(NeoGraphDataStore store) {
        super(store, new NeoTxProvider(store.getGdbService()));
        this.store = store;
	}
	
	// ----------------------------------------------------

    public IndexManager getIndexManager() {
        return store.getIndexManager();
    }

}
