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

import static org.arastreju.sge.SNOPS.assure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.arastreju.bindings.neo4j.extensions.NeoResourceResolver;
import org.arastreju.bindings.neo4j.impl.GraphDataConnection;
import org.arastreju.bindings.neo4j.impl.SemanticNetworkAccess;
import org.arastreju.bindings.neo4j.query.NeoQueryBuilder;
import org.arastreju.sge.apriori.Aras;
import org.arastreju.sge.apriori.RDF;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.context.SimpleContextID;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.views.SNText;
import org.arastreju.sge.naming.Namespace;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.naming.SimpleNamespace;
import org.arastreju.sge.query.Query;
import org.arastreju.sge.query.QueryResult;
import org.arastreju.sge.security.Domain;
import org.arastreju.sge.security.impl.SNDomain;
import org.arastreju.sge.spi.abstracts.AbstractOrganizer;

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

	private final GraphDataConnection connection;
	private final NeoResourceResolver resolver;
	private final SemanticNetworkAccess sna;
	
	// -----------------------------------------------------
	
	/**
	 * Constructor.
	 * @param connection The connection.
	 */
	public NeoOrganizer(final GraphDataConnection connection) {
		this.connection = connection;
		this.resolver = connection.getResourceResolver();
		this.sna = connection.getSemanticNetworkAccess();
	}
	
	// -----------------------------------------------------
	
	/** 
	 * {@inheritDoc}
	 */
	public Collection<Namespace> getNamespaces() {
		final List<Namespace> result = new ArrayList<Namespace>();
		final List<ResourceNode> nodes = connection.getIndex().lookup(RDF.TYPE, Aras.NAMESPACE).toList();
		for (ResourceNode node : nodes) {
			result.add(createNamespace(node));
		}
		return result;
	}
	
	/** 
	 * {@inheritDoc}
	 */
	public Namespace registerNamespace(final String uri, final String prefix) {
		final Query query = query()
				.addField(RDF.TYPE, Aras.NAMESPACE)
				.and()
				.addField(Aras.HAS_URI, uri);
		final QueryResult result = query.getResult();
		if (!result.isEmpty()) {
			final ResourceNode node = resolver.resolve(result.iterator().next());
			assure(node,  Aras.HAS_PREFIX, new SNText(prefix));
			return new SimpleNamespace(uri, prefix);
		} else {
			final Namespace ns = new SimpleNamespace(uri, prefix);
			final ResourceNode node = createNamespaceNode(ns);
			sna.attach(node);
			return ns;
		}
	}
	
	// -----------------------------------------------------

	/** 
	 * {@inheritDoc}
	 */
	public Collection<Context> getContexts() {
		final List<Context> result = new ArrayList<Context>();
		final List<ResourceNode> nodes = connection.getIndex().lookup(RDF.TYPE, Aras.CONTEXT).toList();
		for (ResourceNode node : nodes) {
			result.add(createContext(node));
		}
		return result;
	}

	/** 
	 * {@inheritDoc}
	 */
	public Context registerContext(final QualifiedName qn) {
		final ResourceNode node = createContextNode(qn);
		sna.attach(node);
		return new SimpleContextID(qn);
	}
	
	// ----------------------------------------------------
	
	/** 
	 * {@inheritDoc}
	 */
	public Domain findDomain(String name) {
		final Query query = query()
				.addField(RDF.TYPE, Aras.DOMAIN)
				.and()
				.addField(Aras.HAS_UNIQUE_NAME, name);
		ResourceNode node = query.getResult().getSingleNode();
		if (node != null) {
			return createDomain(node);
		} else {
			return null;
		}
	};
	
	/** 
	 * {@inheritDoc}
	 */
	public Collection<Domain> getDomains() {
		final List<Domain> result = new ArrayList<Domain>();
		final List<ResourceNode> nodes = connection.getIndex().lookup(RDF.TYPE, Aras.DOMAIN).toList();
		for (ResourceNode node : nodes) {
			result.add(createDomain(node));
		}
		return result;
	}
	
	/** 
	 * {@inheritDoc}
	 */
	public Domain getDomesticDomain() {
		final Query query = query()
				.addField(RDF.TYPE, Aras.DOMAIN)
				.and()
				.addField(Aras.IS_DOMESTIC_DOMAIN, Boolean.TRUE);
		ResourceNode node = query.getResult().getSingleNode();
		if (node != null) {
			return createDomain(node);
		} else {
			return null;
		}
	}
	
	/** 
	 * {@inheritDoc}
	 */
	public Domain initDomesticDomain(String name) {
		final ResourceNode node = createDomesticDomainNode(name);
		sna.attach(node);
		return new SNDomain(node); 
	}
	
	/** 
	 * {@inheritDoc}
	 */
	public Domain registerDomain(String name, String title, String description) {
		final ResourceNode node = createDomainNode(name, title, description);
		sna.attach(node);
		return new SNDomain(node); 
	}
	
	/** 
	 * {@inheritDoc}
	 */
	public void updateDomain(Domain domain) {
		sna.attach(domain);
	}
	
	// ----------------------------------------------------
	
	private Query query() {
		return new NeoQueryBuilder(connection.getIndex());
	}
	
}
