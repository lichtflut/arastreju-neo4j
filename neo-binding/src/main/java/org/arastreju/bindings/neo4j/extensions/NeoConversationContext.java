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
package org.arastreju.bindings.neo4j.extensions;

import org.arastreju.sge.spi.AssociationResolver;
import org.arastreju.bindings.neo4j.storage.NeoAssociationResolver;
import org.arastreju.bindings.neo4j.storage.NeoAssociationWriter;
import org.arastreju.bindings.neo4j.storage.NeoGraphDataStore;
import org.arastreju.sge.inferencing.implicit.InverseOfInferencer;
import org.arastreju.sge.model.associations.AttachedAssociationKeeper;
import org.arastreju.sge.persistence.ResourceResolver;
import org.arastreju.sge.spi.GraphDataConnection;
import org.arastreju.sge.spi.abstracts.AbstractConversationContext;
import org.arastreju.sge.spi.abstracts.AssociationManager;
import org.arastreju.sge.spi.uow.IndexUpdateUOW;
import org.arastreju.sge.spi.uow.InferencingInterceptor;
import org.arastreju.sge.spi.uow.OpenConversationNotifier;
import org.arastreju.sge.spi.uow.ResourceResolverImpl;

/**
 * <p>
 *  Handler for resolving, adding and removing of a node's association.
 * </p>
 *
 * <p>
 * 	Created Dec 1, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoConversationContext extends AbstractConversationContext {

    private final AssociationResolver resolver;
    private final AssociationManager manager;

    // ----------------------------------------------------
	
	/**
	 * Creates a new Working Context.
	 * @param connection The connection.
	 */
	public NeoConversationContext(GraphDataConnection connection) {
        super(connection);
        NeoGraphDataStore store = (NeoGraphDataStore) getConnection().getStore();
        this.resolver = new NeoAssociationResolver(this, store);
        this.manager = createAssociationManager();
	}

	// ----------------------------------------------------

	/**
	 * Resolve the associations of given association keeper.
	 * @param keeper The association keeper to be resolved.
	 */
    @Override
	public void resolveAssociations(AttachedAssociationKeeper keeper) {
		assertActive();
		resolver.resolveAssociations(keeper);
	}

    // ----------------------------------------------------

    @Override
    protected AssociationManager getAssociationManager() {
        return manager;
    }

    // ----------------------------------------------------

    private AssociationManager createAssociationManager() {
        ResourceResolver resolver = new ResourceResolverImpl(this);
        NeoGraphDataStore store = (NeoGraphDataStore) getConnection().getStore();
        AssociationManager am = new AssociationManager(resolver);
        am.register(new NeoAssociationWriter(this, store));
        am.register(new IndexUpdateUOW(getIndexUpdator()));
        am.register(new InferencingInterceptor(am).add(new InverseOfInferencer(resolver)));
        am.register(new OpenConversationNotifier(getConnection(), this));
        return am;
    }

}
