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

import org.arastreju.bindings.neo4j.extensions.NeoConversationContext;
import org.arastreju.bindings.neo4j.extensions.NeoGraphDataConnection;
import org.arastreju.sge.Conversation;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.io.StatementContainer;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.naming.Namespace;
import org.arastreju.sge.organize.AbstractOrganizer;
import org.arastreju.sge.query.QueryResult;

import java.util.Collection;
import java.util.Iterator;

/**
 * <p>
 *  Neo implementation of Organizer.
 * </p>
 *
 * <p>
 * 	Created Sep 22, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoOrganizer extends AbstractOrganizer {

    private final NeoGraphDataConnection connection;
    private final Neo4jGate gate;

    // -----------------------------------------------------
	
	/**
	 * Constructor.
	 * @param connection The connection.
	 */
    public NeoOrganizer(NeoGraphDataConnection connection, Neo4jGate gate) {
        this.connection = connection;
        this.gate = gate;
    }

    // -----------------------------------------------------

    @Override
    public StatementContainer getStatements(final Context... ctx) {
        final NeoConversationContext conversationContext = new NeoConversationContext(connection);
        conversationContext.setReadContexts(ctx);
        return new StatementContainer() {
            @Override
            public Collection<Namespace> getNamespaces() {
                return NeoOrganizer.this.getNamespaces();
            }

            @Override
            public Iterator<Statement> iterator() {
                //final ResourceIndex index = new ResourceIndex(connection, conversationContext);
                //final QueryResult queryResult = index.getAllResources();
                final QueryResult queryResult = null;
                final Iterator<ResourceNode> nodeIterator = queryResult.iterator();
                return newStatementIterator(nodeIterator);
            }
        } ;
    }

    // ----------------------------------------------------

    @Override
    protected Conversation conversation() {
        return gate.startConversation();
    }

}
