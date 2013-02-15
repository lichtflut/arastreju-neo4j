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
package org.arastreju.bindings.neo4j.impl;

import org.arastreju.sge.index.ArasIndexerImpl;
import org.arastreju.sge.inferencing.implicit.InverseOfInferencer;
import org.arastreju.sge.inferencing.implicit.SubClassOfInferencer;
import org.arastreju.sge.inferencing.implicit.TypeInferencer;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.associations.AttachedAssociationKeeper;
import org.arastreju.sge.persistence.ResourceResolver;
import org.arastreju.sge.persistence.TxAction;
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
	public NeoConversationContext(NeoGraphDataConnection connection) {
        super(connection);
        this.resolver = new AssociationResolver(this, connection.getStore());
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

    @Override
	public void addAssociation(final AttachedAssociationKeeper keeper, final Statement stmt) {
		assertActive();
        getTxProvider().doTransacted(new TxAction() {
            @Override
            public void execute() {
                manager.addAssociation(keeper, stmt);
            }
        });

	}

	@Override
	public boolean removeAssociation(final AttachedAssociationKeeper keeper, final Statement stmt) {
		assertActive();
        getTxProvider().doTransacted(new TxAction() {
            @Override
            public void execute() {
                manager.removeAssociation(keeper, stmt);
            }
        });
        return true;
	}

    // ----------------------------------------------------

    private AssociationManager createAssociationManager() {
        ResourceResolver resolver = new ResourceResolverImpl(this);
        NeoGraphDataStore store = (NeoGraphDataStore) getConnection().getStore();

        ArasIndexerImpl index = new ArasIndexerImpl(this, getIndexProvider());
        index.add(new TypeInferencer(resolver));
        index.add(new SubClassOfInferencer(resolver));

        AssociationManager am = new AssociationManager(resolver);
        am.register(new RelationshipManager(this, store));
        am.register(new IndexUpdateUOW(index));
        am.register(new InferencingInterceptor(am).add(new InverseOfInferencer(resolver)));
        am.register(new OpenConversationNotifier(getConnection(), this));
        return am;
    }

}
