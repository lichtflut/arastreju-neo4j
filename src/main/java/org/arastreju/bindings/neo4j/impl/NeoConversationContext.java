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

import java.util.HashMap;
import java.util.Map;

import org.arastreju.bindings.neo4j.NeoConstants;
import org.arastreju.bindings.neo4j.extensions.NeoAssociationKeeper;
import org.arastreju.sge.ConversationContext;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.naming.QualifiedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class NeoConversationContext implements NeoConstants, ConversationContext {
	
	public static final Context[] NO_CTX = new Context[0];

	private static final Logger logger = LoggerFactory.getLogger(NeoConversationContext.class);

	private final Map<QualifiedName, NeoAssociationKeeper> register = new HashMap<QualifiedName, NeoAssociationKeeper>();
	
	private final AssociationHandler handler;
	
	private Context writeContext;
	
	private Context[] readContexts;
	
	private boolean active = true;

	// ----------------------------------------------------
	
	/**
	 * Creates a new Working Context.
	 * @param connection The connection.
	 */
	public NeoConversationContext(GraphDataConnection connection) {
		this.handler = new AssociationHandler(connection, this);
	}

	// ----------------------------------------------------
	
	/**
	 * @param qn The resource's qualified name.
	 * @return The association keeper or null;
	 */
	public NeoAssociationKeeper getAssociationKeeper(QualifiedName qn) {
		assertActive();
		return register.get(qn);
	}
	
	/**
	 * @param qn The resource's qualified name.
	 * @param keeper The keeper to be accessed.
	 */
	public void attach(QualifiedName qn, NeoAssociationKeeper keeper) {
		assertActive();
		register.put(qn, keeper);
		keeper.setWorkingContext(this);
	}
	
	/**
	 * @param qn The resource's qualified name.
	 */
	public void detach(QualifiedName qn) {
		assertActive();
		final NeoAssociationKeeper removed = register.remove(qn);
		if (removed != null) {
			removed.detach();
		}
	}
	
	/**
	 * Clear the cache.
	 */
	public void clear() {
		assertActive();
		for (NeoAssociationKeeper keeper : register.values()) {
			keeper.detach();
		}
		register.clear();
	}
	
	/**
	 * Close and invalidate this context.
	 */
	public void close() {
		if (active) {
			clear();
			active = false;
		}
	}
	
	// ----------------------------------------------------

	/**
	 * Resolve the associations of given association keeper.
	 * @param keeper The association keeper to be resolved.
	 */
	public void resolveAssociations(final NeoAssociationKeeper keeper) {
		assertActive();
		handler.resolveAssociations(keeper);
	}
	
	// ----------------------------------------------------
	
	/**
	 * Add a new Association to given Neo node, or rather create a corresponding Relation.
	 * @param subject The neo node, which shall be the subject in the new Relation.
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
	
	/** 
	 * {@inheritDoc}
	 */
	public Context[] getReadContexts() {
		assertActive();
		if (readContexts != null) {
			return readContexts;
		} else {
			return NO_CTX;
		}
	}
	
   /** 
	 * {@inheritDoc}
	 */
    public Context getWriteContext() {
    	return writeContext;
    }

	/**
	 * {@inheritDoc}
	 */
	public ConversationContext setWriteContext(Context ctx) {
		this.writeContext = ctx;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public ConversationContext setReadContexts(Context... ctxs) {
		this.readContexts = ctxs;
		return this;
	}
	
    // ----------------------------------------------------
	
	private void assertActive() {
		if (!active) {
			logger.warn("Conversation already closed.");
			//throw new IllegalStateException("Conversation already closed.");
		}
	}
}