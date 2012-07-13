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

import java.util.ArrayList;
import java.util.List;

import org.arastreju.bindings.neo4j.NeoConstants;
import org.arastreju.bindings.neo4j.impl.GraphDataConnection;
import org.arastreju.bindings.neo4j.tx.TxProvider;
import org.arastreju.sge.ConversationContext;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.ValueNode;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.TxAction;
import org.arastreju.sge.persistence.TxResultAction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.index.lucene.QueryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *  Wrapper around the lucene indexes with convenience methods.
 * </p>
 *
 * <p>
 * 	Created Apr 29, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoIndex implements NeoConstants {

    /**
     * Index key representing a resource'id.
     */
    public static final String INDEX_KEY_RESOURCE_URI = "resource-uri";

    /**
     * Index key for a resource's values.
     */
    public static final String INDEX_KEY_RESOURCE_VALUE = "resource-value";

    /**
     * Index key for a resource's relations.
     */
    public static final String INDEX_KEY_RESOURCE_RELATION = "resource-relation";

    // ----------------------------------------------------


    /**
	 * Index for all resources in this graph data store.
	 */
	private static final String INDEX_RESOURCES = "resources";
	
	/**
	 * Index for all resources in this graph data store.
	 */
	private static final String CONTEXT_INDEX_PREFIX = "ctx-statements:";
	
	// -----------------------------------------------------
	

	private final GraphDataConnection connection;
	private final IndexManager manager;
	
	private final Logger logger = LoggerFactory.getLogger(NeoIndex.class);

	private final ConversationContext conversationContext;

	// -----------------------------------------------------
	
	/**
	 * Constructor.
	 * @param connection The connection to the graph database.
	 * @param ctx The current conversation context.
	 */
	public NeoIndex(GraphDataConnection connection, ConversationContext ctx) {
		this.connection = connection;
		this.conversationContext = ctx;
		this.manager = connection.getStore().getIndexManager();
	}
	
	// -- LOOKUP ------------------------------------------
	
	/**
	 * Find in Index by key and value.
	 */
	public Node lookup(final QualifiedName qn) {
		return manager.forNodes(INDEX_RESOURCES).get(INDEX_KEY_RESOURCE_URI, normalize(qn.toURI())).getSingle();
	}
	
	/**
	 * Find in Index by key and value.
	 */
	public IndexHits<Node> lookup(final ResourceID predicate, final ResourceID value) {
		return lookup(uri(predicate), uri(value));
	}
	
	/**
	 * Find in Index by key and value.
	 */
	public List<Node> lookupNodes(final ResourceID predicate, final ResourceID value) {
		return lookupNodes(uri(predicate), uri(value));
	}
	
	/**
	 * Find in Index by key and value.
	 */
	public IndexHits<Node> lookup(final ResourceID predicate, final String value) {
		return lookup(uri(predicate), value);
	}
	
	/**
	 * Find in Index by key and value.
	 */
	public List<Node> lookupNodes(final ResourceID predicate, final String value) {
		return lookupNodes(uri(predicate), value);
	}
	
	/**
	 * Find in Index by key and value.
	 */
	public IndexHits<Node> lookup(final String key, final String value) {
		return tx().doTransacted(new TxResultAction<IndexHits<Node>>() {
			public IndexHits<Node> execute() {
				return readIndex().get(key, normalize(value));
			}
		});
	}
	
	/**
	 * Find in Index by key and value.
	 */
	public List<Node> lookupNodes(final String key, final String value) {
		final List<Node> result = new ArrayList<Node>();
		tx().doTransacted(new TxAction() {
			public void execute() {
				toList(result, readIndex().get(key, normalize(value)));
			}
		});
		return result;
	}
	
	// -- SEARCH ------------------------------------------
	
	/**
	 * Search in value index by serach term.
	 */
	public List<Node> searchByValue(final String searchTerm) {
		return search(INDEX_KEY_RESOURCE_VALUE, searchTerm);
	}
	
	/**
	 * Execute the query.
	 * @param query The query.
	 * @return The resulting index hits.
	 */
	public IndexHits<Node> search(final String query) {
		return tx().doTransacted(new TxResultAction<IndexHits<Node>>() {
			public IndexHits<Node> execute() {
				return readIndex().query(query);
			}
		});
	}
	
	/**
	 * Execute the query.
	 * @param query The query.
	 * @return The resulting index hits.
	 */
	public IndexHits<Node> search(final QueryContext query) {
		return tx().doTransacted(new TxResultAction<IndexHits<Node>>() {
			public IndexHits<Node> execute() {
				return readIndex().query(query);
			}
		});
	}
	
	/**
	 * Find in Index by key and value.
	 */
	public List<Node> search(final String key, final String value) {
		final List<Node> result = new ArrayList<Node>();
		tx().doTransacted(new TxAction() {
			public void execute() {
				toList(result, readIndex().query(key, normalize(value)));
			}
		});
		return result;
	}
	
	// -- ADD TO INDEX ------------------------------------
	
	public void index(Node subject, ResourceID predicate, ValueNode value) {
		indexResource(subject, uri(predicate), value.getStringValue());
		indexResource(subject, INDEX_KEY_RESOURCE_VALUE, value.asValue().getStringValue());
	}
	
	public void index(Node subject, ResourceID predicate, ResourceNode relation) {
		indexResource(subject, uri(predicate), uri(relation));
		indexResource(subject, INDEX_KEY_RESOURCE_RELATION, relation.toURI());
	}
	
	public void index(Node subject, QualifiedName qn) {
		indexResource(subject, INDEX_KEY_RESOURCE_URI, qn.toURI());
	}
	
	// --REMOVE FROM INDEX --------------------------------
	
	public void remove(final Node node) {
	    writeIndex().remove(node);
	}

	/**
	 * Remove relationship from index.
	 * @param rel The relationship to be removed.
	 * TODO: Check - seems to fail
	 */
	public void remove(final Relationship rel) {
		final String value = (String) rel.getProperty(PREDICATE_URI);
		writeIndex().remove(rel.getStartNode(), normalize(value));
	}
	

	/**
	 * Remove relationship from index.
	 */
	public void remove(Node subject, String key, String value) {
	    writeIndex().remove(subject, key, normalize(value));
	}
	
	// -----------------------------------------------------
	
	private void indexResource(Node subject, String key, String value) {
	    writeIndex().add(subject, key, normalize(value));
	}
	
	/**
	 * @param result
	 * @param nodes
	 */
	private void toList(List<Node> result, IndexHits<Node> nodes) {
		for (Node node : nodes) {
			if (node.hasProperty(PROPERTY_URI)) {
				result.add(node);
			} else {
				logger.error("Invalid node in index, will be removed: " + node);
				remove(node);
				for(Relationship rel : node.getRelationships()) {
					remove(rel);
				}
			}
		}
	}
	
	private TxProvider tx() {
		return connection.getTxProvider();
	}
	
	private Index<Node> readIndex() {
        final Context writeContext = conversationContext.getWriteContext();
        if (writeContext != null) {
            //TODO: enable
            //return manager.forNodes(writeContext.toURI());
            return manager.forNodes(INDEX_RESOURCES);
        } else {
            return manager.forNodes(INDEX_RESOURCES);
        }
	}
	
	private Index<Node> writeIndex() {
	    final Context writeContext = conversationContext.getWriteContext();
	    if (writeContext != null) {
            //TODO: enable
            //return manager.forNodes(writeContext.toURI());
            return manager.forNodes(INDEX_RESOURCES);
	    } else {
	        return manager.forNodes(INDEX_RESOURCES);
	    }
    }
	
	// ----------------------------------------------------
	
	private String normalize(final String s) {
		return s.trim().toLowerCase();
	}

}
