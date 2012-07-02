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

import org.arastreju.bindings.neo4j.impl.GraphDataConnection;
import org.arastreju.bindings.neo4j.impl.NeoConversationContext;
import org.arastreju.sge.ArastrejuGate;
import org.arastreju.sge.ModelingConversation;
import org.arastreju.sge.Organizer;
import org.arastreju.sge.spi.GateContext;
import org.arastreju.sge.spi.GateInitializationException;

/**
 * <p>
 * 	Neo4j specific implementation of {@link ArastrejuGate}.
 * </p>
 * 
 * <p>
 * 	Created Jan 4, 2011
 * </p>
 * 
 * @author Oliver Tigges
 */
public class Neo4jGate implements ArastrejuGate {
	
	private final GraphDataConnection connection;
	
	private final GateContext ctx;
	
	private boolean open;

	// -----------------------------------------------------

	/**
	 * Initialize default gate.
	 * @param ctx The gate context.
     * @param connection The connection to the graph datastore.
	 */
	public Neo4jGate(final GateContext ctx, GraphDataConnection connection) throws GateInitializationException {
		this.ctx = ctx;
		this.connection = connection;
		this.open = true;
	}
	
	// -----------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
    @Override
	public ModelingConversation startConversation() {
		return new Neo4jModellingConversation(connection);
	}

	/**
	 * {@inheritDoc}
	 */
    @Override
	public Organizer getOrganizer() {
		return new NeoOrganizer(connection, new NeoConversationContext(connection));
	}
	
	/**
	 * {@inheritDoc}
	 */
    @Override
	public void close() {
		connection.close();
		open = false;
	}
	
	/** 
	 * {@inheritDoc}
	 */
    @Override
	public GateContext getContext() {
		return ctx;
	}
	
}
