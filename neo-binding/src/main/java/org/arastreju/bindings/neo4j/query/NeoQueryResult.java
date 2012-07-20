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
package org.arastreju.bindings.neo4j.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.arastreju.bindings.neo4j.impl.NeoNodeResolver;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.query.Query;
import org.arastreju.sge.query.QueryResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

/**
 * <p>
 *  Result for a {@link Query}.
 * </p>
 *
 * <p>
 * 	Created Nov 10, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoQueryResult implements QueryResult {

	private final IndexHits<Node> hits;
	private final NeoNodeResolver resolver;
	
	// -----------------------------------------------------
	
	/**
	 * Constructor.
	 * @param hits The index hits.
	 */
	public NeoQueryResult(final IndexHits<Node> hits, final NeoNodeResolver resolver) {
		this.hits = hits;
		this.resolver = resolver;
	}
	
	// -----------------------------------------------------
	
	/** 
	 * {@inheritDoc}
	 */
	public Iterator<ResourceNode> iterator() {
		return new ResolvingIterator();
	}

	/** 
	 * {@inheritDoc}
	 */
	public void close() {
		hits.close();
	}

	/** 
	 * {@inheritDoc}
	 */
	public int size() {
		return hits.size();
	}

	/** 
	 * {@inheritDoc}
	 */
	public List<ResourceNode> toList() {
		final List<ResourceNode> result = new ArrayList<ResourceNode>(size());
		for (Node node : hits) {
			result.add(resolver.resolve(node));
		}
		return result;
	}
	
	/** 
	* {@inheritDoc}
	*/
	public List<ResourceNode> toList(int max) {
		int absMax = Math.min(hits.size(), max);
		final List<ResourceNode> result = new ArrayList<ResourceNode>(absMax);
		for (int i=0; i < absMax; i++) {
			result.add(resolver.resolve(hits.next()));
		}
		hits.close();
		return result;
	}
	
	/** 
	 * {@inheritDoc}
	 */
	public List<ResourceNode> toList(int offset, int max) {
		int absMax = Math.min(hits.size(), offset + max);
		if (offset >= absMax) {
			hits.close();
			return Collections.emptyList();
		}
		final List<ResourceNode> result = new ArrayList<ResourceNode>(absMax - offset);
		for (int i=0; i < absMax; i++) {
			final Node next = hits.next();
			if (i >= offset) {
				result.add(resolver.resolve(next));
			}
		}
		hits.close();
		return result;
	}

	/** 
	 * {@inheritDoc}
	 */
	public boolean isEmpty() {
		return hits.size() <= 0;
	}
	
	/** 
	* {@inheritDoc}
	*/
	public ResourceNode getSingleNode() {
		try {
			if (isEmpty()) {
				return null;
			} else if (size() > 1) {
				throw new IllegalStateException("More than one result found.");
			} else {
				return toList().get(0);
			}
		} finally {
			close();
		}
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + size() + " results]";
	}
	
	// -----------------------------------------------------
	
	class ResolvingIterator implements Iterator<ResourceNode> {

		/** 
		 * {@inheritDoc}
		 */
		public boolean hasNext() {
			return hits.hasNext();
		}

		/** 
		 * {@inheritDoc}
		 */
		public ResourceNode next() {
			return resolver.resolve(hits.next());
		}

		/** 
		 * {@inheritDoc}
		 */
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

}
