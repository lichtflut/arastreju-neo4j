/*
 * Copyright 2012 by lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 */
package org.arastreju.bindings.neo4j.impl;

import org.arastreju.bindings.neo4j.tx.NeoTxProvider;
import org.arastreju.sge.naming.QualifiedName;
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
public class GraphDataConnection {

    private final Set<NeoConversationContext> openConversations = new HashSet<NeoConversationContext>();
	
	private final GraphDataStore store;
	
	private final NeoTxProvider txProvider;

	// ----------------------------------------------------

	/**
	 * Constructor.
	 * @param store The store.
	 */
	public GraphDataConnection(GraphDataStore store) {
		this.store = store;
		this.txProvider = new NeoTxProvider(store.getGdbService());
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

    public void register(NeoConversationContext conversationContext) {
        openConversations.add(conversationContext);
    }


    public void unregister(NeoConversationContext conversationContext) {
        openConversations.remove(conversationContext);
    }

    // ----------------------------------------------------

    /**
     * Called when a resource has been modified by conversation context belonging to this graph data connection.
     * @param qn The qualified name of the modified resource.
     * @param context The context, where the modification occurred.
     */
    public void notifyModification(QualifiedName qn, NeoConversationContext context) {
        for (NeoConversationContext conversation : openConversations) {
            if (!conversation.equals(context)) {
                conversation.onModification(qn, context);
            }
        }
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
        List<NeoConversationContext> copy = new ArrayList<NeoConversationContext>(openConversations);
        // iterating over copy because original will be remove itself while closing.
        for (NeoConversationContext cc : copy) {
            cc.close();
        }
	}

}
