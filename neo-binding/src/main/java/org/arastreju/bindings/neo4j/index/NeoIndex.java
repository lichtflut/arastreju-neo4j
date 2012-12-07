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

import org.apache.commons.codec.binary.Base64;
import org.arastreju.bindings.neo4j.NeoConstants;
import org.arastreju.sge.ConversationContext;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.ValueNode;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.TxAction;
import org.arastreju.sge.persistence.TxProvider;
import org.arastreju.sge.persistence.TxResultAction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.index.lucene.QueryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static org.arastreju.sge.SNOPS.uri;

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

    public static final Logger LOGGER = LoggerFactory.getLogger(NeoIndex.class);

    // ----------------------------------------------------

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
	 * Index for all resources by their qualified name.
	 */
	private static final String INDEX_RESOURCES = "resources";

    /**
     * Index for statements in this domain: "local public"
     */
    private static final String INDEX_LOCAL = "local";

    /**
     * Mirror index for public statements: "global public"
     */
    private static final String INDEX_PUBLIC = "public";

    /**
     * Index prefix for context specific statements
     */
    private static final String INDEX_CONTEXT_PREFIX = "context-";
	
	// -----------------------------------------------------

    private final ConversationContext conversationContext;
	
	private final IndexManager manager;

    // -----------------------------------------------------
	
    /**
     * Constructor.
     * @param ctx The current conversation context.
     */
    public NeoIndex(ConversationContext ctx, IndexManager idxManager) {
        this.conversationContext = ctx;
        this.manager = idxManager;
    }
	
	// -- LOOKUP ------------------------------------------
	
	/**
	 * Find in Index by key and value.
	 */
	public Node lookup(final QualifiedName qn) {
		return resourceIndex().get(INDEX_KEY_RESOURCE_URI, normalize(qn.toURI())).getSingle();
	}
	
	/**
	 * Find in Index by key and value.
	 */
	public IndexHits<Node> lookup(final String key, final String value) {
		return tx().doTransacted(new TxResultAction<IndexHits<Node>>() {
            @Override
            public IndexHits<Node> execute() {
                return contextIndex().get(key, normalize(value));
            }
        });
	}

    /**
     * Find in Index by key and value.
     */
    public IndexHits<Node>  allNodes() {
        return tx().doTransacted(new TxResultAction<IndexHits<Node>>() {
            @Override
            public IndexHits<Node> execute() {
                return resourceIndex().query(INDEX_KEY_RESOURCE_URI, "*");
            }
        });
    }

	// -- SEARCH ------------------------------------------

	/**
	 * Execute the query.
	 * @param query The query.
	 * @return The resulting index hits.
	 */
	public IndexHits<Node> search(final String query) {
		return tx().doTransacted(new TxResultAction<IndexHits<Node>>() {
            @Override
            public IndexHits<Node> execute() {
                return contextIndex().query(query);
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
            @Override
            public IndexHits<Node> execute() {
                return contextIndex().query(query);
            }
        });
	}

	/**
	 * Find in Index by key and value.
	 */
	public List<Node> search(final String key, final String value) {
		final List<Node> result = new ArrayList<Node>();
		tx().doTransacted(new TxAction() {
            @Override
            public void execute() {
                toList(result, contextIndex().query(key, normalize(value)));
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
        resourceIndex().add(subject, INDEX_KEY_RESOURCE_URI, normalize(qn.toURI()));
		indexResource(subject, INDEX_KEY_RESOURCE_URI, qn.toURI());
	}
	
	// --REMOVE FROM INDEX --------------------------------
	
	public void remove(final Node node) {
	    contextIndex().remove(node);
	}

	/**
	 * Remove relationship from index.
	 * @param rel The relationship to be removed.
	 * TODO: Check - seems to fail
	 */
	public void remove(final Relationship rel) {
		final String value = (String) rel.getProperty(PREDICATE_URI);
		contextIndex().remove(rel.getStartNode(), normalize(value));
	}
	

	/**
	 * Remove relationship from index.
	 */
	public void remove(Node subject, String key, String value) {
	    contextIndex().remove(subject, key, normalize(value));
	}
	
	// -----------------------------------------------------
	
	private void indexResource(Node subject, String key, String value) {
	    contextIndex().add(subject, key, normalize(value));
	}
	
	private void toList(List<Node> result, IndexHits<Node> nodes) {
		for (Node node : nodes) {
			if (node.hasProperty(PROPERTY_URI)) {
				result.add(node);
			} else {
				LOGGER.error("Invalid node in index, will be removed: " + node);
				remove(node);
				for(Relationship rel : node.getRelationships()) {
					remove(rel);
				}
			}
		}
	}

	private TxProvider tx() {
		return conversationContext.getTxProvider();
	}

    // ----------------------------------------------------
	
	private Index<Node> contextIndex() {
	    final Context context = conversationContext.getPrimaryContext();
	    if (context != null) {
            return manager.forNodes(indexForContext(context));
	    } else {
            return manager.forNodes(INDEX_LOCAL);
	    }
    }

    private Index<Node> resourceIndex() {
        return manager.forNodes(INDEX_RESOURCES);
    }

	// ----------------------------------------------------
	
	private String normalize(final String s) {
		return s.trim().toLowerCase();
	}

    private String indexForContext(Context ctx) {
        String value = null;
        try {
            byte[] bytes =  Base64.encodeBase64(ctx.toURI().getBytes("UTF-8"));
            value = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Could not encode string: " + value + " to Base64.");
            throw new RuntimeException(e);
        }
        return INDEX_CONTEXT_PREFIX + value;
    }


}
