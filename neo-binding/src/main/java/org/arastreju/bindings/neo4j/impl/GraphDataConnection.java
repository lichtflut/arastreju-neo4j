/*
 * Copyright 2012 by lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 */
package org.arastreju.bindings.neo4j.impl;

import org.arastreju.bindings.neo4j.tx.NeoTxProvider;
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
public class GraphDataConnection {
	
	private final GraphDataStore store;
	
	private final NeoTxProvider txProvider;
	
	// ----------------------------------------------------

	/**
	 * Constructor.
	 * @param store The store.
	 */
	public GraphDataConnection(GraphDataStore store, NeoTxProvider txProv) {
		this.store = store;
		txProv.init(store.getGdbService());
		this.txProvider = txProv;
	}
	
	// ----------------------------------------------------

	/**
	 * @return the store
	 */
	public GraphDataStore getStore() {
		return store;
	}
	
	/**
	 * @return the txProvider
	 */
	public NeoTxProvider getTxProvider() {
		return txProvider;
	}

    public IndexManager getIndexManager() {
        return store.getIndexManager();
    }
	
	// ----------------------------------------------------
	
	/**
	 * (re-)open the connection.
	 */
	public void open() {
    }
	
	/**
	 * Close the connection and free all resources.
	 */
	public void close() {
	}

}
