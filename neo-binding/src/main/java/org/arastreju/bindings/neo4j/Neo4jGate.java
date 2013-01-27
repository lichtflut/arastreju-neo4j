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

import org.arastreju.bindings.neo4j.impl.NeoConversationContext;
import org.arastreju.bindings.neo4j.impl.NeoGraphDataConnection;
import org.arastreju.sge.ArastrejuGate;
import org.arastreju.sge.Conversation;
import org.arastreju.sge.Organizer;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.context.DomainIdentifier;
import org.arastreju.sge.spi.GateInitializationException;
import org.arastreju.sge.spi.abstracts.AbstractArastrejuGate;

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
public class Neo4jGate extends AbstractArastrejuGate {

	private final NeoGraphDataConnection connection;
	
	private boolean open;

	// -----------------------------------------------------

	/**
	 * Initialize default gate.
	 * @param domainIdentifier The gate context.
     * @param connection The connection to the graph datastore.
	 */
	public Neo4jGate(DomainIdentifier domainIdentifier, NeoGraphDataConnection connection) throws GateInitializationException {
        super(domainIdentifier);
		this.connection = connection;
		this.open = true;
	}
	
	// -----------------------------------------------------

    @Override
	public Conversation startConversation() {
        return new NeoConversation(connection, newConversationContext());
	}

    @Override
    public Conversation startConversation(Context primary, Context... readContexts) {
        NeoConversationContext cc = new NeoConversationContext(connection);
        cc.setPrimaryContext(primary);
        cc.setReadContexts(readContexts);
        return new NeoConversation(connection, cc);
    }

    @Override
	public Organizer getOrganizer() {
		return new NeoOrganizer(connection, this);
	}
	
    @Override
	public void close() {
		connection.close();
		open = false;
	}

    // ----------------------------------------------------

    public NeoConversationContext newConversationContext() {
        NeoConversationContext cc = new NeoConversationContext(connection);
        super.initContext(cc);
        return cc;
    }

}
