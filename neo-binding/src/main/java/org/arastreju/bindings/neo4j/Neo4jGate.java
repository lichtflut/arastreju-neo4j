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

import org.arastreju.bindings.neo4j.extensions.NeoConversationContext;
import org.arastreju.sge.ArastrejuGate;
import org.arastreju.sge.Conversation;
import org.arastreju.sge.context.DomainIdentifier;
import org.arastreju.sge.spi.GateInitializationException;
import org.arastreju.sge.spi.GraphDataConnection;
import org.arastreju.sge.spi.abstracts.AbstractArastrejuGate;
import org.arastreju.sge.spi.abstracts.WorkingContext;

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

	/**
	 * Initialize default gate.
	 * @param domainIdentifier The gate context.
     * @param connection The connection to the graph data store.
	 */
	public Neo4jGate(DomainIdentifier domainIdentifier, GraphDataConnection connection)
            throws GateInitializationException {
        super(connection, domainIdentifier);
	}
	
	// -----------------------------------------------------

    @Override
    protected WorkingContext newWorkingContext(GraphDataConnection connection) {
        return new NeoConversationContext(connection);
    }

    @Override
    protected Conversation newConversation(WorkingContext ctx) {
        return new NeoConversation(ctx);
    }
}
