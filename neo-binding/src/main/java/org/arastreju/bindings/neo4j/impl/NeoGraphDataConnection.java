/*
 * Copyright 2012 by lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 */
package org.arastreju.bindings.neo4j.impl;

import org.arastreju.bindings.neo4j.tx.NeoTxProvider;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.TxProvider;
import org.arastreju.sge.spi.GraphDataConnection;
import org.arastreju.sge.spi.abstracts.AbstractGraphDataConnection;
import org.neo4j.graphdb.index.IndexManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
public class NeoGraphDataConnection extends AbstractGraphDataConnection<NeoConversationContext> {

	private final NeoGraphDataStore store;
	
	private final TxProvider txProvider;

	// ----------------------------------------------------

	/**
	 * Constructor.
	 * @param store The store.
	 */
	public NeoGraphDataConnection(NeoGraphDataStore store) {
		this.store = store;
		this.txProvider = new NeoTxProvider(store.getGdbService());
	}
	
	// ----------------------------------------------------

	/**
	 * @return the store
	 */
    @Override
	public NeoGraphDataStore getStore() {
		return store;
	}
	
	/**
	 * @return the txProvider
	 */
	public TxProvider getTxProvider() {
		return txProvider;
	}

    public IndexManager getIndexManager() {
        return store.getIndexManager();
    }

    // ----------------------------------------------------

    /**
     * Called when a resource has been modified by conversation context belonging to this graph data connection.
     * @param qn The qualified name of the modified resource.
     * @param context The context, where the modification occurred.
     */
    public void notifyModification(QualifiedName qn, NeoConversationContext context) {
        for (NeoConversationContext conversation : getOpenConversations()) {
            if (!conversation.equals(context)) {
                conversation.onModification(qn, context);
            }
        }
    }
	
}
