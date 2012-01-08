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

import java.util.Arrays;

import org.arastreju.bindings.neo4j.index.NeoIndex;
import org.arastreju.bindings.neo4j.index.ResourceIndex;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.query.QueryBuilder;
import org.arastreju.sge.query.QueryExpression;
import org.arastreju.sge.query.QueryParam;
import org.arastreju.sge.query.QueryResult;
import org.neo4j.index.lucene.QueryContext;

import de.lichtflut.infra.exceptions.NotYetSupportedException;

/**
 * <p>
 *  Query Builder specific for Neo4j and Lucene.
 * </p>
 *
 * <p>
 * 	Created Nov 7, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoQueryBuilder extends QueryBuilder {
	
	private final ResourceIndex index;
	
	// -----------------------------------------------------
	
	/**
	 * @param index
	 */
	public NeoQueryBuilder(final ResourceIndex index) {
		this.index = index;
	}
	
	// -----------------------------------------------------

	/** 
	 * {@inheritDoc}
	 */
	public QueryResult getResult() {
		return index.search(toQueryContext());
	}

	/** 
	 * {@inheritDoc}
	 */
	public ResourceNode getSingleNode() {
		final QueryResult result = index.search(toQueryContext());
		return result.getSingleNode();
	}
	
	// -----------------------------------------------------
	
	protected QueryContext toQueryContext() {
		final QueryContext qctx = new QueryContext(toQueryString());
		qctx.tradeCorrectnessForSpeed();
		if (getSortCriteria() != null) {
			String[] columns = getSortCriteria().getColumns();
			if (columns.length > 1) {
				qctx.sort(columns[0], Arrays.copyOfRange(columns, 1, columns.length -1));
			} else if (columns.length > 0) {
				qctx.sort(columns[0]);
			}
		}
		return qctx;
	}
	
	protected String toQueryString() {
		final StringBuilder sb = new StringBuilder();
		append(getRoot(), sb);
		return sb.toString();
	}
	
	/** 
	 * {@inheritDoc}
	 */
	@Override
	protected QueryExpression getRoot() {
		return super.getRoot();
	}
	
	// -----------------------------------------------------

	private void append(final QueryExpression exp, final StringBuilder sb) {
		if (exp.isLeaf()) {
			appendLeaf(exp.getQueryParam(), sb);
		} else {
			sb.append("(");
			boolean first = true;
			for (QueryExpression child : exp.getChildren()) {
				if (first) {
					first = false;
				} else {
					sb.append(" " + exp.getOperator().name() + " ");
				}
				append(child, sb);
			}
			sb.append(")");
		}
	}
	
	private void appendLeaf(final QueryParam param, final StringBuilder sb) {
		switch(param.getOperator()) {
		case EQUALS:
			sb.append(normalizeKey(param.getName()) + ":");
			break;
		case HAS_URI:
			sb.append(NeoIndex.INDEX_KEY_RESOURCE_URI + ":");
			break;
		case HAS_VALUE:
			sb.append(NeoIndex.INDEX_KEY_RESOURCE_VALUE + ":");
			break;
		default:
			throw new NotYetSupportedException(param.getOperator());
		}
		sb.append(normalizeValue(param.getValue().toString()));
	}
	
	private String normalizeKey(final String key) {
		return key.replaceAll(":", "\\\\:");
	}
	
	private String normalizeValue(final String key) {
		return key.trim().toLowerCase().replaceAll(":", "\\\\:");
	}
	
}
