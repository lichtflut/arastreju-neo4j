/*
 * Copyright 2012 by lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 */
package org.arastreju.bindings.neo4j.impl;

import org.arastreju.bindings.neo4j.tx.NeoTxProvider;
import org.arastreju.sge.index.IndexProvider;
import org.arastreju.sge.persistence.TxProvider;
import org.arastreju.sge.spi.abstracts.AbstractGraphDataConnection;

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
public class NeoGraphDataConnection extends AbstractGraphDataConnection {

    private final NeoTxProvider txProvider;

    // ----------------------------------------------------

    /**
	 * Constructor.
	 * @param store The store.
     * @param indexProvider The provider for the search index.
	 */
	public NeoGraphDataConnection(NeoGraphDataStore store, IndexProvider indexProvider) {
        super(store, indexProvider);
        txProvider = new NeoTxProvider(store.getGdbService());
    }

    // ----------------------------------------------------

    @Override
    public TxProvider getTxProvider() {
        return txProvider;
    }

    @Override
    public NeoGraphDataStore getStore() {
        return (NeoGraphDataStore) super.getStore();
    }
}
