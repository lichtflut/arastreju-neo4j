/*
 * Copyright (C) 2013 lichtflut Forschungs- und Entwicklungsgesellschaft mbH
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

import org.arastreju.bindings.neo4j.storage.NeoGraphDataStore;
import org.arastreju.sge.ArastrejuGate;
import org.arastreju.sge.ArastrejuProfile;
import org.arastreju.sge.context.DomainIdentifier;
import org.arastreju.sge.index.IndexProvider;
import org.arastreju.sge.spi.ArastrejuGateFactory;
import org.arastreju.sge.spi.GateInitializationException;
import org.arastreju.sge.spi.GraphDataConnection;
import org.arastreju.sge.spi.impl.ArastrejuGateImpl;
import org.arastreju.sge.spi.impl.GraphDataConnectionImpl;

import java.io.IOException;

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
	
	@Override
	public synchronized ArastrejuGate create(final DomainIdentifier domainIdentifier) throws GateInitializationException {
		try {
            final GraphDataConnection connection = openConnection(domainIdentifier);
            final ArastrejuGate gate = new ArastrejuGateImpl(connection, domainIdentifier);
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
    private GraphDataConnection openConnection(DomainIdentifier ctx) throws IOException {
        final NeoGraphDataStore store = getOrCreateStore(ctx);
        IndexProvider indexProvider = new IndexProvider(store.getStorageDir());
        return new GraphDataConnectionImpl(store, indexProvider);
    }

    private NeoGraphDataStore getOrCreateStore(DomainIdentifier domainIdentifier) throws IOException {
        final NeoGraphDataStore store = getStore(domainIdentifier);
        if (store != null) {
            return store;
        } else {
            return createStore(domainIdentifier);
        }
    }

    private NeoGraphDataStore getStore(DomainIdentifier domainIdentifier) {
        final String key = KEY_GRAPH_DATA_STORE + ":" + domainIdentifier.getStorage();
        return (NeoGraphDataStore) getProfile().getProfileObject(key);
    }
	
    /**
     * Create and initialize the store.
     * @param domainIdentifier The identified of the data store.
     * @return The {@link org.arastreju.bindings.neo4j.storage.NeoGraphDataStore}.
     * @throws IOException
     */
    private NeoGraphDataStore createStore(DomainIdentifier domainIdentifier) throws IOException {
        final ArastrejuProfile profile = getProfile();
        final String storeName = domainIdentifier.getStorage();
        final NeoGraphDataStore store = createStore(storeName);
        profile.addListener(store);
        if (isStoreDirDefined(profile)) {
            final String key = KEY_GRAPH_DATA_STORE + ":" + storeName;
            profile.setProfileObject(key, store);
        }
        return store;
    }

    private NeoGraphDataStore createStore(String store) throws IOException {
        final ArastrejuProfile profile = getProfile();
        if (isStoreDirDefined(profile)){
            String basedir = profile.getProperty(ArastrejuProfile.ARAS_STORE_DIRECTORY);
            return new NeoGraphDataStore(basedir + "/" + store);
        } else {
            return new NeoGraphDataStore(NeoGraphDataStore.prepareTempStore(store));
        }
    }

	// -----------------------------------------------------
	
	private boolean isStoreDirDefined(final ArastrejuProfile profile) {
		return profile.isPropertyDefined(ArastrejuProfile.ARAS_STORE_DIRECTORY);
	}

}
