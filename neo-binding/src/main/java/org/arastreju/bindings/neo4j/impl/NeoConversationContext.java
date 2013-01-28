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

import org.arastreju.bindings.neo4j.NeoConstants;
import org.arastreju.bindings.neo4j.extensions.NeoAssociationKeeper;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.spi.abstracts.AbstractConversationContext;
import org.arastreju.sge.spi.abstracts.WorkingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

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
public class NeoConversationContext extends AbstractConversationContext<NeoAssociationKeeper> implements NeoConstants {

    private static final Logger LOGGER = LoggerFactory.getLogger(NeoConversationContext.class);

	private final AssociationHandler handler;

    // ----------------------------------------------------
	
	/**
	 * Creates a new Working Context.
	 * @param connection The connection.
	 */
	public NeoConversationContext(NeoGraphDataConnection connection) {
        super(connection);
        this.handler = new AssociationHandler(connection, this);
	}

	// ----------------------------------------------------
	
	/**
	 * @param qn The resource's qualified name.
	 * @return The association keeper or null;
	 */
	public NeoAssociationKeeper getAssociationKeeper(QualifiedName qn) {
		assertActive();
        NeoAssociationKeeper registered = lookup(qn);
        if (registered != null && !registered.isAttached()) {
            LOGGER.warn("There is a detached NeoAssociationKeeper in the conversation register: {}.", qn);
        }
        return registered;
	}
	
	// ----------------------------------------------------

	/**
	 * Resolve the associations of given association keeper.
	 * @param keeper The association keeper to be resolved.
	 */
	public void resolveAssociations(NeoAssociationKeeper keeper) {
		assertActive();
		handler.resolveAssociations(keeper);
	}

    /**
     * Get the incoming statements of the given node.
     * @param object The node which is the object of the searched statements.
     * @return The statments.
     */
    public Set<Statement> getIncomingStatements(ResourceID object) {
        assertActive();
        NeoAssociationKeeper keeper = lookup(object.getQualifiedName());
        if (keeper == null) {
            return Collections.emptySet();
        }
        return handler.getIncomingStatements(keeper);
    }
	
	// ----------------------------------------------------
	
	/**
	 * Add a new Association to given Neo node, or rather create a corresponding Relation.
	 * @param keeper The neo node, which shall be the subject in the new Relation.
	 * @param stmt The Association.
	 */
	public void addAssociation(final NeoAssociationKeeper keeper, final Statement stmt) {
		assertActive();
		handler.addAssociation(keeper, stmt);
	}

	/**
	 * Remove the given association.
	 * @param keeper The keeper.
	 * @param assoc The association.
	 * @return true if the association has been removed.
	 */
	public boolean removeAssociation(final NeoAssociationKeeper keeper, final Statement assoc) {
		assertActive();
		return handler.removeAssociation(keeper, assoc);
	}

    // ----------------------------------------------------

    @Override
    public void onModification(QualifiedName qualifiedName, WorkingContext otherContext) {
        NeoAssociationKeeper existing = lookup(qualifiedName);
        if (existing != null) {
            LOGGER.info("Concurrent change on node {} in other context {}.", qualifiedName, otherContext);
            existing.notifyChanged();
        }
    }

}
