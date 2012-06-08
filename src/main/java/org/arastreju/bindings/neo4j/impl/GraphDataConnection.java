/*
 * Copyright 2012 by lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 */
package org.arastreju.bindings.neo4j.impl;

import org.arastreju.bindings.neo4j.tx.TxProvider;

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
	
	private final TxProvider txProvider;
	
	private NeoConversationContext workingContext;

	// ----------------------------------------------------

	/**
	 * Constructor.
	 * @param store The store.
	 */
	public GraphDataConnection(GraphDataStore store) {
		this.store = store;
		this.txProvider = new TxProvider(store.getGdbService());
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
	public TxProvider getTxProvider() {
		return txProvider;
	}
	
	/**
	 * @return the working context.
	 */
	public NeoConversationContext getWorkingContext() {
		if (workingContext == null) {
			workingContext = new NeoConversationContext(this);
		}
		return workingContext;
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
		getWorkingContext().close();
	}

}
