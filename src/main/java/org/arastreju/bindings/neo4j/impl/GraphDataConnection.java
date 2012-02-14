/*
 * Copyright 2012 by lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 */
package org.arastreju.bindings.neo4j.impl;

import org.arastreju.bindings.neo4j.extensions.NeoResourceResolver;
import org.arastreju.bindings.neo4j.index.ResourceIndex;
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

	private final SemanticNetworkAccess sna;
	
	private final ResourceIndex index;
	
	private final NeoResourceResolver resolver;
	
	private WorkingContext workingContext;
	
	// ----------------------------------------------------

	/**
	 * Constructor.
	 * @param store The store.
	 */
	public GraphDataConnection(final GraphDataStore store) {
		this.store = store;
		this.txProvider = new TxProvider(store.getGdbService());
		this.resolver = new NeoResourceResolverImpl(this);
		this.sna = new SemanticNetworkAccess(this);
		this.index = new ResourceIndex(this);
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
	public WorkingContext getWorkingContext() {
		if (workingContext == null) {
			workingContext = new WorkingContext(this);
		}
		return workingContext;
	}
	
	/**
	 * @return The semantic network access.
	 */
	public SemanticNetworkAccess getSemanticNetworkAccess() {
		return sna;
	}
	
	/**
	 * @return The semantic network access.
	 */
	public NeoResourceResolver getResourceResolver() {
		return resolver;
	}
	
	/**
	 * @return The index.
	 */
	public ResourceIndex getIndex() {
		return index;
	}

	// ----------------------------------------------------
	
	/**
	 * Close the connection and free all resources.
	 */
	public void close() {
		getWorkingContext().clear();
	}
	
}
