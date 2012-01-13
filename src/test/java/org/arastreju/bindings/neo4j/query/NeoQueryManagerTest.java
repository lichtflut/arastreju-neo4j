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


import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.arastreju.bindings.neo4j.impl.GraphDataStore;
import org.arastreju.bindings.neo4j.impl.SemanticNetworkAccess;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.apriori.Aras;
import org.arastreju.sge.apriori.RDF;
import org.arastreju.sge.apriori.RDFS;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.model.DetachedStatement;
import org.arastreju.sge.model.SimpleResourceID;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.SNResource;
import org.arastreju.sge.model.nodes.views.SNEntity;
import org.arastreju.sge.model.nodes.views.SNText;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.query.FieldParam;
import org.arastreju.sge.query.Query;
import org.arastreju.sge.query.QueryExpression;
import org.arastreju.sge.query.QueryResult;
import org.arastreju.sge.query.UriParam;
import org.arastreju.sge.query.ValueParam;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * <p>
 *  Test cases for {@link NeoQueryManager}.
 * </p>
 *
 * <p>
 * 	Created Jan 6, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoQueryManagerTest {
	
	private final QualifiedName qnCar = new QualifiedName("http://q#", "Car");
	private final QualifiedName qnBike = new QualifiedName("http://q#", "Bike");
	
	private GraphDataStore store;
	private SemanticNetworkAccess sna;
	private NeoQueryManager qm;
	
	// -----------------------------------------------------

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		store = new GraphDataStore();
		sna = new SemanticNetworkAccess(store);
		qm = new NeoQueryManager(sna, sna.getIndex());
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		sna.close();
		store.close();
	}
	
	// -----------------------------------------------------
	
	@Test
	public void testQueryBuilder() {
		final NeoQueryBuilder query = qm.buildQuery();
			query.beginAnd()
				.add(new FieldParam("a", 1))
				.add(new FieldParam("b", 2))
				.add(new FieldParam("c", 3))
				.beginOr()
					.add(new FieldParam("d1", 1))
					.add(new FieldParam("d2", 2))
					.add(new FieldParam("d3", 3))
				.end();
		

		final QueryExpression root = query.getRoot();
		Assert.assertTrue(root != null);
		Assert.assertEquals(4, root.getChildren().size());
		Assert.assertEquals(3, root.getChildren().get(3).getChildren().size());
		
	}
	
	@Test
	public void testFindByTag(){
		final ResourceNode car = new SNResource(qnCar);
		SNOPS.associate(car, Aras.HAS_PROPER_NAME, new SNText("BMW"));
		SNOPS.associate(car, RDFS.LABEL, new SNText("Automobil"));
		sna.attach(car);
		
		final Query query = qm.buildQuery()
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
		
		Query query = qm.buildQuery()
				.addValue("Automobil")
				.and()
				.not().add(new ValueParam("BMW"));
		
		Assert.assertEquals(0, query.getResult().size());
		
		query = qm.buildQuery()
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
		
		final Query query = qm.buildQuery().add(new FieldParam(RDFS.LABEL, "Automobil"));
		final QueryResult result = query.getResult();
		final List<ResourceNode> list = result.toList();
		Assert.assertEquals(1, list.size());
		Assert.assertTrue(list.contains(car));
		
	}
	
	@Test
	public void testFindByQuery(){
		final Context ctx = null;
		final ResourceNode car = new SNResource(qnCar);
		SNOPS.associate(car, RDF.TYPE, RDFS.CLASS, ctx);
		sna.attach(car);
		
		final ResourceNode bike = new SNResource(qnBike);
		SNOPS.associate(bike, RDF.TYPE, RDFS.CLASS, ctx);
		sna.attach(bike);
		
		final SNEntity aCar = car.asClass().createInstance(ctx);
		sna.attach(aCar);
		
		final SNEntity aBike = bike.asClass().createInstance(ctx);
		sna.attach(aBike);
		
		final Query query = qm.buildQuery().add(new UriParam("*Car"));
		final QueryResult result = query.getResult();
		final List<ResourceNode> list = result.toList();
		Assert.assertEquals(1, list.size());
		Assert.assertEquals(new SimpleResourceID(qnCar), list.get(0));
		
	}
	
	@Test
	public void testFindByType(){
		final Context ctx = null;
		final ResourceNode car = new SNResource(qnCar);
		SNOPS.associate(car, RDF.TYPE, RDFS.CLASS, ctx);
		sna.attach(car);
		
		final ResourceNode bike = new SNResource(qnBike);
		SNOPS.associate(bike, RDF.TYPE, RDFS.CLASS, ctx);
		sna.attach(bike);
		
		final SNEntity aCar = car.asClass().createInstance(ctx);
		sna.attach(aCar);
		
		final SNEntity aBike = bike.asClass().createInstance(ctx);
		sna.attach(aBike);
		
		final List<ResourceNode> result = qm.findByType(new SimpleResourceID(qnCar));
		Assert.assertEquals(1, result.size());
		Assert.assertTrue(result.contains(aCar));
		
		final List<ResourceNode> result2 = qm.findByType(new SimpleResourceID(qnBike));
		Assert.assertEquals(1, result2.size());
		Assert.assertTrue("Expected aBike to be a Bike", result2.contains(aBike));
		
		final List<ResourceNode> result3 = qm.findByType(RDFS.CLASS);
		Assert.assertEquals(2, result3.size());
		Assert.assertTrue("Expected Bike to be a Class", result3.contains(bike));
		Assert.assertTrue("Expected Car to be a Class", result3.contains(car));
	}
	
	@Test
	public void testFindIncomingAssociations() {
		final Context ctx = null;
		final ResourceNode car = new SNResource(qnCar);
		SNOPS.associate(car, RDF.TYPE, RDFS.CLASS);
		sna.attach(car);
		
		final ResourceNode bike = new SNResource(qnBike);
		SNOPS.associate(bike, RDF.TYPE, RDFS.CLASS);
		sna.attach(bike);

		final SNEntity car1 = car.asClass().createInstance(ctx);
		sna.attach(car1);

		final SNEntity car2 = car.asClass().createInstance(ctx);
		sna.attach(car2);
		
		sna.detach(car1);
		sna.detach(car2);
		sna.detach(car);
		sna.detach(bike);

		final Set<Statement> result = qm.findIncomingStatements(RDFS.CLASS);
		Assert.assertNotNull(result);
		Assert.assertEquals(2, result.size());
		Assert.assertTrue(result.contains(new DetachedStatement(car, RDF.TYPE, RDFS.CLASS)));
		Assert.assertTrue(result.contains(new DetachedStatement(bike, RDF.TYPE, RDFS.CLASS)));
		
		final Set<Statement> result2 = qm.findIncomingStatements(bike);
		Assert.assertNotNull(result2);
		Assert.assertEquals(0, result2.size());
		
		final Set<Statement> result3 = qm.findIncomingStatements(car);
		Assert.assertNotNull(result3);
		Assert.assertEquals(2, result3.size());
		Assert.assertTrue(result3.contains(new DetachedStatement(car1, RDF.TYPE, car)));
		Assert.assertTrue(result3.contains(new DetachedStatement(car2, RDF.TYPE, car)));
	}
	

}
