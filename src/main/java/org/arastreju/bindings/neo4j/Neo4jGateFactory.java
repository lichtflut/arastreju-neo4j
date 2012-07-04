/*
 * Copyright (C) 2012 lichtflut Forschungs- und Entwicklungsgesellschaft mbH
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
package org.arastreju.bindings.neo4j;

import java.io.IOException;

import org.arastreju.bindings.neo4j.impl.GraphDataConnection;
import org.arastreju.bindings.neo4j.impl.GraphDataStore;
import org.arastreju.sge.ArastrejuGate;
import org.arastreju.sge.ArastrejuProfile;
import org.arastreju.sge.config.StoreIdentifier;
import org.arastreju.sge.spi.ArastrejuGateFactory;
import org.arastreju.sge.spi.GateContext;
import org.arastreju.sge.spi.GateInitializationException;

/**
 * <p>
 *  Neo4j specific Gate Factory.
 * </p>
 *
 * <p>
 * 	Created Jan 4, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class Neo4jGateFactory extends ArastrejuGateFactory {
	
	private static final String KEY_GRAPH_DATA_STORE = "aras:neo4j:profile-object:graph-data-store";
	
	// ----------------------------------------------------

	/**
	 * Constructor.
	 */
	public Neo4jGateFactory(final ArastrejuProfile profile) {
		super(profile);
	}
	
	// -----------------------------------------------------
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized ArastrejuGate create(final GateContext ctx) throws GateInitializationException {
		try {
            final GraphDataConnection connection = openConnection(ctx);
            final Neo4jGate gate = new Neo4jGate(ctx, connection);
			getProfile().onOpen(gate);
			return gate;
		} catch (IOException e) {
			throw new GateInitializationException("Could not createStore gate.", e);
		}
	}
	
	// ----------------------------------------------------

    /**
     * Open a new connection to the store corresponding to the context.
     * @param ctx The context.
     * @return The new connection.
     * @throws IOException
     */
    private GraphDataConnection openConnection(GateContext ctx) throws IOException {
        final GraphDataStore store = getOrCreateStore(ctx);
        return new GraphDataConnection(store);
    }

    private GraphDataStore getOrCreateStore(GateContext ctx) throws IOException {
        final GraphDataStore store = getStore(getProfile(), ctx.getStoreIdentifier());
        if (store != null) {
            return store;
        } else {
            return createStore(ctx.getStoreIdentifier());
        }
    }

    private GraphDataStore getStore(ArastrejuProfile profile, StoreIdentifier storeIdentifier) {
        final String key = KEY_GRAPH_DATA_STORE + ":" + storeIdentifier.getStorageName();
        return (GraphDataStore) profile.getProfileObject(key);
    }
	
    /**
     * Create and initialize the store.
     * @param storeIdentifier The identified of the data store.
     * @return The {@link GraphDataStore}.
     * @throws IOException
     */
    private GraphDataStore createStore(StoreIdentifier storeIdentifier) throws IOException {
        final ArastrejuProfile profile = getProfile();
        final String storeName = storeIdentifier.getStorageName();
        final GraphDataStore store = createStore(profile, storeName);
        profile.addListener(store);
        if (isStoreDirDefined(profile)) {
            final String key = KEY_GRAPH_DATA_STORE + ":" + storeName;
            profile.setProfileObject(key, store);
        }
        return store;
    }

    private GraphDataStore createStore(ArastrejuProfile profile, String store) throws IOException {
        if (isStoreDirDefined(profile)){
            String basedir = profile.getProperty(ArastrejuProfile.ARAS_STORE_DIRECTORY);
            return new GraphDataStore(basedir + "/" + store);
        } else {
            return new GraphDataStore(GraphDataStore.prepareTempStore(store));
        }
    }

	// -----------------------------------------------------
	
	private boolean isStoreDirDefined(final ArastrejuProfile profile) {
		return profile.isPropertyDefined(ArastrejuProfile.ARAS_STORE_DIRECTORY);
	}

}
