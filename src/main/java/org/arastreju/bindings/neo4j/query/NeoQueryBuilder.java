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
import org.arastreju.sge.query.QueryException;
import org.arastreju.sge.query.QueryExpression;
import org.arastreju.sge.query.QueryOperator;
import org.arastreju.sge.query.QueryParam;
import org.arastreju.sge.query.QueryResult;
import org.neo4j.index.lucene.QueryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	private final Logger logger = LoggerFactory.getLogger(NeoQueryBuilder.class);
	
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
		final String queryString = toQueryString();
		logger.debug("Query string: " + queryString);
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
			if (QueryOperator.NOT.equals(exp.getOperator())) {
				sb.append(" " + exp.getOperator().name() + " ");
			}
			sb.append("(");
			boolean first = true;
			for (QueryExpression child : exp.getChildren()) {
				if (first) {
					first = false;
				} else if (!QueryOperator.NOT.equals(exp.getOperator())) { 
					sb.append(" " + exp.getOperator().name() + " ");
				}
				append(child, sb);
			}
			sb.append(")");
		}
	}
	
	private void appendLeaf(final QueryParam param, final StringBuilder sb) {
		String value = normalizeValue(param.getValue());
		if (value == null || value.length() == 0) {
			throw new QueryException("Invalid query value: " + param);
		}
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
		case HAS_RELATION:
			sb.append(NeoIndex.INDEX_KEY_RESOURCE_RELATION + ":");
			break;
		case SUB_QUERY:
			sb.append(param.getValue());
			// abort here!
			return;
		default:
			throw new NotYetSupportedException(param.getOperator());
		}
		sb.append(value);
	}
	
	private String normalizeKey(final String key) {
		return key.replaceAll(":", "\\\\:");
	}
	
	private String normalizeValue(final Object value) {
		if (value == null) {
			return null;
		}
		String normalized = value.toString().trim().toLowerCase();
		if (normalized.indexOf(" ") > -1) {
			normalized = "\"" + normalized + "\"";
		}
		return normalized.replaceAll(":", "\\\\:");
	}
	
}
