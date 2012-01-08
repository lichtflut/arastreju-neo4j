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

import static org.arastreju.sge.SNOPS.uri;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.arastreju.bindings.neo4j.ArasRelTypes;
import org.arastreju.bindings.neo4j.NeoConstants;
import org.arastreju.bindings.neo4j.extensions.SNValueNeo;
import org.arastreju.bindings.neo4j.impl.ContextAccess;
import org.arastreju.bindings.neo4j.impl.NeoResourceResolver;
import org.arastreju.bindings.neo4j.index.ResourceIndex;
import org.arastreju.sge.apriori.RDF;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.model.DetachedStatement;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.SemanticNode;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.query.QueryManager;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *  Neo specific implementation of {@link QueryManager}.
 * </p>
 *
 * <p>
 * 	Created Jan 6, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoQueryManager implements QueryManager, NeoConstants {

	private final Logger logger = LoggerFactory.getLogger(NeoQueryManager.class);
	
	private final ResourceIndex index;
	
	private final NeoResourceResolver resolver;

	// -----------------------------------------------------
	
	/**
	 * Constructor.
	 */
	public NeoQueryManager(final NeoResourceResolver resolver, final ResourceIndex index) {
		this.resolver = resolver;
		this.index = index;
	}
	
	// -----------------------------------------------------
	
	/** 
	 * {@inheritDoc}
	 */
	public NeoQueryBuilder buildQuery() {
		return new NeoQueryBuilder(index);
	}
	
	// -----------------------------------------------------
	
	/**
	 * {@inheritDoc}
	 */
	public Set<Statement> findIncomingStatements(final ResourceID resource) {
		final Set<Statement> result = new HashSet<Statement>();
		final Node node = index.findNeoNode(resource.getQualifiedName());
		if (node != null){
			for (Relationship rel : node.getRelationships(Direction.INCOMING)) {
				result.add(toArasStatement(rel));
			}
		}
		return result;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<ResourceNode> findByType(final ResourceID type) {
		final String typeURI = uri(type);
		final List<ResourceNode> result = index.lookup(RDF.TYPE, typeURI).toList();
		logger.debug("found with rdf:type '" + typeURI + "': " + result);
		return result;
	}
	
	// ----------------------------------------------------
	
	/**
	 * Converts a Neo4j relationship to an Arastreju Statement.
	 */
	public Statement toArasStatement(final Relationship rel){
		SemanticNode object = null;
		if (rel.isType(ArasRelTypes.REFERENCE)){
			object = resolver.resolve(rel.getEndNode());	
		} else if (rel.isType(ArasRelTypes.VALUE)){
			object = new SNValueNeo(rel.getEndNode());
		}
		
		final ResourceNode subject =  resolver.resolve(rel.getStartNode());	
		final ResourceNode predicate = resolver.findResource(new QualifiedName(rel.getProperty(PREDICATE_URI).toString()));
		final Context[] ctx = new ContextAccess(resolver).getContextInfo(rel);
		
		return new DetachedStatement(subject, predicate, object, ctx);
	}

}
