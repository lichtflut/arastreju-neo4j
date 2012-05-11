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
			
			final Neo4jGate gate;
			
			final ArastrejuProfile profile = ctx.getProfile();
			final String domain = ctx.getDomain();
			final GraphDataStore store = getStore(profile, domain);
			
			if (store != null) {
				gate = new Neo4jGate(ctx, new GraphDataConnection(store));
			} else {
				final GraphDataConnection connection = new GraphDataConnection(initialize(profile, domain));
				gate = new Neo4jGate(ctx, connection);
			}
			getProfile().onOpen(gate);
			return gate;
			
		} catch (IOException e) {
			throw new GateInitializationException("Could not initialize gate", e);
		}
	}
	
	// ----------------------------------------------------

	/**
	 * Initialize the domain.
	 * @param profile The Arastreju Profile.
	 * @param domain The domain name.
	 * @return The {@link GraphDataStore}.
	 * @throws IOException
	 */
	private GraphDataStore initialize(ArastrejuProfile profile, String domain) throws IOException {
		final GraphDataStore store = createStore(profile, domain);
		profile.addListener(store);
		if (isStoreDirDefined(profile)) {
			final String key = KEY_GRAPH_DATA_STORE + ":" + domain;
			profile.setProfileObject(key, store);
		}
		return store;
	}
	
	private GraphDataStore createStore(final ArastrejuProfile profile, final String domain) throws IOException {
		if (isStoreDirDefined(profile)){
			String basedir = profile.getProperty(ArastrejuProfile.ARAS_STORE_DIRECTORY);
			return new GraphDataStore(basedir + "/" + domain);
		} else {
			return new GraphDataStore(GraphDataStore.prepareTempStore(domain));
		}
	}
	
	private GraphDataStore getStore(ArastrejuProfile profile, String domain) {
		final String key = KEY_GRAPH_DATA_STORE + ":" + domain;
		return (GraphDataStore) profile.getProfileObject(key);
	}
	
	// -----------------------------------------------------
	
	private boolean isStoreDirDefined(final ArastrejuProfile profile) {
		return profile.isPropertyDefined(ArastrejuProfile.ARAS_STORE_DIRECTORY);
	}

}
