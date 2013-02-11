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
package org.arastreju.bindings.neo4j.it;


import junit.framework.Assert;
import org.arastreju.bindings.neo4j.impl.NeoGraphDataConnection;
import org.arastreju.bindings.neo4j.impl.NeoGraphDataStore;
import org.arastreju.bindings.neo4j.impl.NeoConversationContext;
import org.arastreju.bindings.neo4j.impl.NeoResourceResolver;
import org.arastreju.bindings.neo4j.impl.SemanticNetworkAccess;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.apriori.Aras;
import org.arastreju.sge.apriori.RDF;
import org.arastreju.sge.apriori.RDFS;
import org.arastreju.sge.index.ArasIndexerImpl;
import org.arastreju.sge.index.LuceneQueryBuilder;
import org.arastreju.sge.model.SimpleResourceID;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.SNResource;
import org.arastreju.sge.model.nodes.views.SNClass;
import org.arastreju.sge.model.nodes.views.SNEntity;
import org.arastreju.sge.model.nodes.views.SNText;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.query.FieldParam;
import org.arastreju.sge.query.Query;
import org.arastreju.sge.query.QueryResult;
import org.arastreju.sge.query.UriParam;
import org.arastreju.sge.query.ValueParam;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * <p>
 *  Test cases for queries.
 * </p>
 *
 * <p>
 * 	Created Jan 6, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoQueryTest {

	private final QualifiedName qnCar = new QualifiedName("http://q#", "Car");
	private final QualifiedName qnBike = new QualifiedName("http://q#", "Bike");

	private NeoGraphDataStore store;
	private NeoGraphDataConnection connection;
	private SemanticNetworkAccess sna;
	private NeoConversationContext convCtx;
	private ArasIndexerImpl index;

	// -----------------------------------------------------

	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		store = new NeoGraphDataStore();
		connection = new NeoGraphDataConnection(store);
		convCtx = new NeoConversationContext(connection);
		sna = new SemanticNetworkAccess(connection, convCtx);
		index = new ArasIndexerImpl(convCtx);
	}

	/**
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		convCtx.close();
		connection.close();
		store.close();
	}

	// -----------------------------------------------------

	@Test
	public void testFindByTag(){
		final ResourceNode car = new SNResource(qnCar);
		SNOPS.associate(car, Aras.HAS_PROPER_NAME, new SNText("BMW"));
		SNOPS.associate(car, RDFS.LABEL, new SNText("Automobil"));
		sna.attach(car);

		Query query = new LuceneQueryBuilder(index, new NeoResourceResolver(connection,  convCtx));
		query
				.addValue("Automobil")
				.and()
				.add(new ValueParam("BMW"));
		final QueryResult result = query.getResult();
		final List<ResourceNode> list = result.toList();
		Assert.assertEquals(1, list.size());
		Assert.assertTrue(list.contains(car));
		
	}
	
	@Test
	public void testFindByInvertedTag(){
		final ResourceNode car = new SNResource(qnCar);
		SNOPS.associate(car, Aras.HAS_PROPER_NAME, new SNText("BMW"));
		SNOPS.associate(car, RDFS.LABEL, new SNText("Automobil"));
		sna.attach(car);

		Query query = new LuceneQueryBuilder(index, new NeoResourceResolver(connection,  convCtx));
		query.addValue("Automobil")
				.and()
				.not().add(new ValueParam("BMW"));
		
		Assert.assertEquals(0, query.getResult().size());

		query = new LuceneQueryBuilder(index, new NeoResourceResolver(connection,  convCtx));
		query
				.addValue("Automobil")
				.and()
				.not().add(new ValueParam("VW"));
		
		Assert.assertEquals(1, query.getResult().size());
		Assert.assertTrue(query.getResult().toList().contains(car));
		
	}
	
	
	@Test
	public void testFindByPredicateAndTag(){
		final ResourceNode car = new SNResource(qnCar);
		SNOPS.associate(car, Aras.HAS_PROPER_NAME, new SNText("BMW"));
		SNOPS.associate(car, RDFS.LABEL, new SNText("Automobil"));
		sna.attach(car);

		Query query = new LuceneQueryBuilder(index, new NeoResourceResolver(connection,  convCtx));
		query.add(new FieldParam(RDFS.LABEL, "Automobil"));
		final QueryResult result = query.getResult();
		final List<ResourceNode> list = result.toList();
		Assert.assertEquals(1, list.size());
		Assert.assertTrue(list.contains(car));
		
	}
	
	@Test
	public void testFindByQuery(){
		final ResourceNode car = new SNResource(qnCar);
		SNOPS.associate(car, RDF.TYPE, RDFS.CLASS);
		sna.attach(car);
		
		final ResourceNode bike = new SNResource(qnBike);
		SNOPS.associate(bike, RDF.TYPE, RDFS.CLASS);
		sna.attach(bike);
		
		final SNEntity aCar = SNClass.from(car).createInstance();
		sna.attach(aCar);
		
		final SNEntity aBike = SNClass.from(bike).createInstance();
		sna.attach(aBike);

		Query query = new LuceneQueryBuilder(index, new NeoResourceResolver(connection,  convCtx));
		query.add(new UriParam("*Car"));
		
		final QueryResult result = query.getResult();
		final List<ResourceNode> list = result.toList();
		Assert.assertEquals(1, list.size());
		Assert.assertEquals(new SimpleResourceID(qnCar), list.get(0));
		
	}
	
}
