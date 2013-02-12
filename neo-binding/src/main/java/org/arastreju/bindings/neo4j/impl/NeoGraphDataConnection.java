/*
 * Copyright 2012 by lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 */
package org.arastreju.bindings.neo4j.impl;

import org.arastreju.bindings.neo4j.extensions.NeoAssociationKeeper;
import org.arastreju.bindings.neo4j.tx.NeoTxProvider;
import org.arastreju.sge.index.IndexProvider;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.TxProvider;
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
}
