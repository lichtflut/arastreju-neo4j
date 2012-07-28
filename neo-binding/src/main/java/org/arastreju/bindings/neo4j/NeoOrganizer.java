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
import org.arastreju.bindings.neo4j.index.ResourceIndex;
import org.arastreju.sge.ModelingConversation;
import org.arastreju.sge.apriori.Aras;
import org.arastreju.sge.apriori.RDF;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.io.StatementContainer;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.naming.Namespace;
import org.arastreju.sge.query.Query;
import org.arastreju.sge.query.QueryResult;
import org.arastreju.sge.spi.abstracts.AbstractOrganizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(NeoOrganizer.class);

    private final GraphDataConnection connection;
    private final Neo4jGate gate;

    // -----------------------------------------------------
	
	/**
	 * Constructor.
	 * @param connection The connection.
	 */
    public NeoOrganizer(GraphDataConnection connection, Neo4jGate gate) {
        this.connection = connection;
        this.gate = gate;
    }

    // -----------------------------------------------------
	
	/** 
	 * {@inheritDoc}
	 */
	public Collection<Namespace> getNamespaces() {
		final List<Namespace> result = new ArrayList<Namespace>();
		final List<ResourceNode> nodes = index().lookup(RDF.TYPE, Aras.NAMESPACE).toList();
		for (ResourceNode node : nodes) {
			result.add(createNamespace(node));
		}
		return result;
	}
	

	
	// -----------------------------------------------------

	/** 
	 * {@inheritDoc}
	 */
	public Collection<Context> getContexts() {
		final List<Context> result = new ArrayList<Context>();
		final List<ResourceNode> nodes = index().lookup(RDF.TYPE, Aras.CONTEXT).toList();
		for (ResourceNode node : nodes) {
			result.add(createContext(node));
		}
		return result;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public StatementContainer getStatements(final Context ctx) {
        final NeoConversationContext conversationContext = new NeoConversationContext(connection);
        conversationContext.setReadContexts(ctx);
        conversationContext.setPrimaryContext(ctx);
        return new StatementContainer() {
            @Override
            public Collection<Namespace> getNamespaces() {
                return NeoOrganizer.this.getNamespaces();
            }

            @Override
            public Iterator<Statement> iterator() {
                final ResourceIndex index = new ResourceIndex(connection, conversationContext);
                final QueryResult queryResult = index.getAllResources();
                LOGGER.info("Resources found: {}.", queryResult.size());
                final Iterator<ResourceNode> nodeIterator = queryResult.iterator();
                return new Iterator<Statement>() {

                    private Iterator<Statement> stmtIterator;

                    @Override
                    public boolean hasNext() {
                        if (stmtIterator == null) {
                            nextNode();
                        }
                        while (stmtIterator != null) {
                            if (stmtIterator.hasNext()) {
                                return true;
                            } else {
                                nextNode();
                            }

                        }
                        return false;
                    }

                    @Override
                    public Statement next() {
                        Statement stmt = stmtIterator.next();
                        LOGGER.info("Next statement: {}.", stmt);
                        return stmt;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                    private void nextNode() {
                        if (nodeIterator.hasNext()) {
                            ResourceNode node = nodeIterator.next();
                            LOGGER.info("Next resource: {}.", node);
                            stmtIterator = node.getAssociations().iterator();
                        } else {
                            LOGGER.info("Iteration over nodes done.");
                            stmtIterator = null;
                        }
                    }
                };
            }
        } ;
    }

    // ----------------------------------------------------

    @Override
    protected ModelingConversation conversation() {
        return gate.startConversation();
    }

    protected Query query() {
		return conversation().createQuery();
	}

    protected ResourceIndex index() {
        return new ResourceIndex(connection, gate.newConversationContext());
    }
	
}
