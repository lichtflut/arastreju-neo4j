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
package org.arastreju.bindings.neo4j.index;

import static org.arastreju.sge.SNOPS.uri;

import java.util.Collection;

import org.arastreju.bindings.neo4j.NeoConstants;
import org.arastreju.bindings.neo4j.extensions.NeoResourceResolver;
import org.arastreju.bindings.neo4j.impl.GraphDataConnection;
import org.arastreju.bindings.neo4j.impl.NeoConversationContext;
import org.arastreju.bindings.neo4j.impl.NeoResourceResolverImpl;
import org.arastreju.bindings.neo4j.query.NeoQueryResult;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.ValueNode;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.query.QueryResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.lucene.QueryContext;

/**
 * <p>
 *  Wrapper around the Neo {@link IndexService} with convenience methods and a registry for caching.
 * </p>
 *
 * <p>
 * 	Created Apr 29, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class ResourceIndex implements NeoConstants {
	
	private final NeoIndex neoIndex;
	
	private final NeoResourceResolver resolver;
	
	// -----------------------------------------------------
	
	/**
	 * Constructor.
	 * @param connection The connection to the graph database.
	 * @param ctx The current conversation context.
	 */
	public ResourceIndex(GraphDataConnection connection, NeoConversationContext ctx) {
		this.resolver = new NeoResourceResolverImpl(connection, ctx);
		this.neoIndex = new NeoIndex(connection, ctx);
	}
	
	// -----------------------------------------------------
	
	/**
	 * Find Neo node by qualified name.
	 */
	public Node findNeoNode(final QualifiedName qn) {
		return neoIndex.lookup(qn);
	}
	
	// -----------------------------------------------------
	
	/**
	 * Find in Index by key and value.
	 */
	public QueryResult lookup(final ResourceID predicate, final ResourceID value) {
		final IndexHits<Node> hits = lookup(uri(predicate), uri(value));
		return new NeoQueryResult(hits, resolver);
	}
	
	/**
	 * Find in Index by key and value.
	 */
	public QueryResult lookup(final ResourceID predicate, final String value) {
		final IndexHits<Node> hits = lookup(uri(predicate), value);
		return new NeoQueryResult(hits, resolver);
	}
	
	// -- SEARCH ------------------------------------------
	
	public QueryResult search(final QueryContext query) {
		return new NeoQueryResult(neoIndex.search(query), resolver);
	}
	
	// -- ADD TO INDEX ------------------------------------
	
	public void index(final Node neoNode, final Statement stmt) {
		if (stmt.getObject().isValueNode()) {
			final ValueNode value = stmt.getObject().asValue();
			neoIndex.index(neoNode, stmt.getPredicate(), value);
		} else {
			neoIndex.index(neoNode, stmt.getPredicate(), stmt.getObject().asResource());
		}
	}
	
	public void index(final Node neoNode, final ResourceNode resourceNode) {
		neoIndex.index(neoNode, resourceNode.getQualifiedName());
	}
	
	/**
	 * Re-index a node.
	 * @param neoNode The Neo node.
	 * @param qn The corresponding Arastreju node.
	 * @param statements The statements to be indexed.
	 */
	public void reindex(final Node neoNode, final QualifiedName qn, final Collection<? extends Statement> statements) {
		removeFromIndex(neoNode);
		neoIndex.index(neoNode, qn);
		for (Statement stmt : statements) {
			index(neoNode, stmt);
		}
	}
	
	// --REMOVE FROM INDEX --------------------------------
	
	public void removeFromIndex(final Node node) {
		neoIndex.remove(node);
	}

	/**
	 * Remove relationship from index.
	 * @param rel The relationship to be removed.
	 */
	public void removeFromIndex(final Node neoNode, final Statement stmt) {
		final String key = stmt.getPredicate().getQualifiedName().toURI();
		if (stmt.getObject().isValueNode()) {
			final ValueNode value = stmt.getObject().asValue();
			// TODO: Remove general value without predicate.
			neoIndex.remove(neoNode, key, value.getStringValue());
		} else {
			neoIndex.remove(neoNode, key, stmt.getObject().asResource().getQualifiedName().toURI());
		}
	}
	
	// -----------------------------------------------------
	
	/**
	 * Find in Index by key and value.
	 */
	private IndexHits<Node> lookup(final String key, final String value) {
		return neoIndex.lookup(key, value);
	}
	
}
