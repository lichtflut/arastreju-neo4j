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

import org.arastreju.bindings.neo4j.impl.NeoConversationContext;
import org.arastreju.bindings.neo4j.impl.NeoGraphDataConnection;
import org.arastreju.bindings.neo4j.impl.NeoGraphDataStore;
import org.arastreju.bindings.neo4j.impl.NeoResourceResolver;
import org.arastreju.bindings.neo4j.impl.SemanticNetworkAccess;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.apriori.Aras;
import org.arastreju.sge.apriori.RDF;
import org.arastreju.sge.apriori.RDFS;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.context.SimpleContextID;
import org.arastreju.sge.index.ArasIndexerImpl;
import org.arastreju.sge.index.IndexProvider;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.SimpleResourceID;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.SNResource;
import org.arastreju.sge.model.nodes.ValueNode;
import org.arastreju.sge.model.nodes.views.SNClass;
import org.arastreju.sge.model.nodes.views.SNEntity;
import org.arastreju.sge.model.nodes.views.SNText;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.ResourceResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Matcher;

import static org.arastreju.sge.SNOPS.associate;
import static org.arastreju.sge.SNOPS.associations;
import static org.arastreju.sge.SNOPS.id;
import static org.arastreju.sge.SNOPS.qualify;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * <p>
 *  Test cases for {@link org.arastreju.bindings.neo4j.impl.SemanticNetworkAccess}.
 * </p>
 *
 * <p>
 * 	Created Dec 2, 2010
 * </p>
 *
 * @author Oliver Tigges
 */
public class SemanticNetworkAccessTest {
	
	private final QualifiedName qnVehicle = new QualifiedName("http://q#", "Verhicle");
	private final QualifiedName qnCar = new QualifiedName("http://q#", "Car");
	private final QualifiedName qnBike = new QualifiedName("http://q#", "Bike");
	
	private final QualifiedName qnEmployedBy = new QualifiedName("http://q#", "employedBy");
	private final QualifiedName qnHasEmployees = new QualifiedName("http://q#", "hasEmployees");
	private final QualifiedName qnKnows = new QualifiedName("http://q#", "knows");
	
	private SemanticNetworkAccess sna;
	private NeoGraphDataStore store;
	private NeoGraphDataConnection connection;
	private ResourceResolver resolver;
	private ArasIndexerImpl index;
	private NeoConversationContext ctx;
	
	// -----------------------------------------------------

	@Before
	public void setUp() throws Exception {
		store = new NeoGraphDataStore();
        IndexProvider indexProvider = new IndexProvider(store.getStorageDir());
        connection = new NeoGraphDataConnection(store, indexProvider);
		ctx = new NeoConversationContext(connection);
		index = new ArasIndexerImpl(ctx, indexProvider);
		sna = new SemanticNetworkAccess(ctx);
		resolver = new NeoResourceResolver(ctx);
		
	}

	@After
	public void tearDown() throws Exception {
		connection.close();
		ctx.close();
		store.close();
		index.clear();
		index.close();
	}
	
	// -----------------------------------------------------

	@Test
	public void testResolveAndFind() throws IOException {
		ResourceNode found = resolver.findResource(qnVehicle);
		assertNull(found);
		
		ResourceNode resolved = resolver.resolve(SNOPS.id(qnVehicle));
		assertNotNull(resolved);
		
		found = resolver.findResource(qnVehicle);
		assertNotNull(found);
		
	}
	
	@Test
	public void testValueIndexing() throws IOException {
		final ResourceNode car = new SNResource(qnCar);
		SNOPS.associate(car, Aras.HAS_PROPER_NAME, new SNText("BMW"));
		
		sna.attach(car);

		Iterable<QualifiedName> res = index.search(escapeColon(Aras.HAS_PROPER_NAME.toURI())+":BMW");
		assertNotNull(res);
		Iterator<QualifiedName> it = res.iterator();

		assertTrue(it.hasNext());

		QualifiedName qn = it.next();

		assertNotNull(qn);
		assertEquals(qnCar.toURI(), qn.toURI());
		assertFalse(it.hasNext());

	}
	
	@Test
	public void testDetaching() throws IOException{
		final ResourceNode car = new SNResource(qnCar);
		sna.attach(car);
		
		final ResourceNode car3 = resolver.findResource(qnCar);
		assertEquals(car, car3);
		
		sna.detach(car);

		final ResourceNode car4 = resolver.findResource(qnCar);
		assertNotSame(car, car4);
	}
	
	@Test
	public void testDatatypes() throws IOException {
		final ResourceNode car = new SNResource(qnCar);
		
		sna.attach(car);
		
		SNOPS.associate(car, Aras.HAS_PROPER_NAME, new SNText("BMW"));
		sna.detach(car);
		
		final ResourceNode car2 = resolver.findResource(qnCar);
		assertNotSame(car, car2);
		final ValueNode value = SNOPS.singleObject(car2, Aras.HAS_PROPER_NAME).asValue();
		
		assertEquals(value.getStringValue(), "BMW");
	}
	
	@Test
	public void testPersisting() throws IOException {
		final ResourceNode vehicle = new SNResource(qnVehicle);
		final ResourceNode car = new SNResource(qnCar);
		
		SNOPS.associate(car, RDFS.SUB_CLASS_OF, vehicle);
		SNOPS.associate(car, Aras.HAS_PROPER_NAME, new SNText("BMW"));
		
		sna.attach(car);
		
		// detach and find again
		sna.detach(car);
		final ResourceNode car2 = resolver.findResource(qnCar);
		assertNotSame(car, car2);

		final ResourceNode res = SNOPS.singleObject(car2, RDFS.SUB_CLASS_OF).asResource();
		assertEquals(vehicle, res);
		
		final ValueNode value = SNOPS.singleObject(car2, Aras.HAS_PROPER_NAME).asValue();
		assertEquals(value.getStringValue(), "BMW");
	}
	
	@Test
	public void testMerging() throws IOException {
		final ResourceNode vehicle = new SNResource(qnVehicle);
		final ResourceNode car1 = new SNResource(qnCar);
		
		sna.attach(car1);
		
		SNOPS.associate(car1, Aras.HAS_BRAND_NAME, new SNText("BMW"));
		
		// detach 
		sna.detach(car1);
		sna.detach(vehicle);
		
		SNOPS.associate(car1, RDFS.SUB_CLASS_OF, vehicle);
		SNOPS.associate(car1, Aras.HAS_PROPER_NAME, new SNText("Knut"));

		// attach again
		sna.attach(car1);
		
		// detach and find again
		sna.detach(car1);
		final ResourceNode car2 = resolver.findResource(qnCar);
		assertNotNull(car2);
		assertNotSame(car1, car2);
		
		final ResourceNode subClasss = SNOPS.singleObject(car2, RDFS.SUB_CLASS_OF).asResource();
		assertEquals(vehicle, subClasss);
		
		final ValueNode brandname = SNOPS.singleObject(car2, Aras.HAS_BRAND_NAME).asValue();
		assertEquals(brandname.getStringValue(), "BMW");

		final ValueNode propername = SNOPS.singleObject(car2, Aras.HAS_PROPER_NAME).asValue();
		assertEquals(propername.getStringValue(), "Knut");
	}

	@Test
	public void testBidirectionalAssociations() {
		final ResourceNode vehicle = new SNResource(qnVehicle);
		final ResourceNode car = new SNResource(qnCar);
		
		final ResourceID pred1 = new SimpleResourceID("http://arastreju.org/stuff#", "P1");
		final ResourceID pred2 = new SimpleResourceID("http://arastreju.org/stuff#", "P2");
		
		SNOPS.associate(vehicle, pred1, car);
		SNOPS.associate(car, pred2, vehicle);
		sna.attach(vehicle);
		
		final ResourceNode vehicleLoaded = resolver.findResource(qnVehicle);
		final ResourceNode carLoaded = resolver.findResource(qnCar);
		
		assertFalse(vehicleLoaded.getAssociations().isEmpty());
		assertFalse(carLoaded.getAssociations().isEmpty());
		
		assertFalse(associations(vehicleLoaded, pred1).isEmpty());
		assertFalse(associations(carLoaded, pred2).isEmpty());
	}
	
	@Test
	public void testDirectRemoval() {
		final ResourceNode vehicle = new SNResource(qnVehicle);
		final ResourceNode car1 = new SNResource(qnCar);
		
		final Statement association = SNOPS.associate(car1, Aras.HAS_BRAND_NAME, new SNText("BMW"));
		SNOPS.associate(car1, RDFS.SUB_CLASS_OF, vehicle);
		SNOPS.associate(car1, Aras.HAS_PROPER_NAME, new SNText("Knut"));
		
		sna.attach(car1);
		
		final Statement stored = SNOPS.singleAssociation(car1, Aras.HAS_BRAND_NAME);
		assertEquals(association.hashCode(), stored.hashCode());
		
		assertEquals(3, car1.getAssociations().size());
		assertFalse(associations(car1, Aras.HAS_BRAND_NAME).isEmpty());
		assertTrue("Association not present", car1.getAssociations().contains(association));
		
		final boolean removedFlag = car1.removeAssociation(association);
		assertTrue(removedFlag);
		
		assertEquals(2, car1.getAssociations().size());
		assertTrue(associations(car1, Aras.HAS_BRAND_NAME).isEmpty());
		
	}
	
	@Test
	public void testAttachingRemoval() {
		final ResourceNode vehicle = new SNResource(qnVehicle);
		final ResourceNode car1 = new SNResource(qnCar);
		
		sna.attach(car1);
		
		final Statement association = SNOPS.associate(car1, Aras.HAS_BRAND_NAME, new SNText("BMW"));
		SNOPS.associate(car1, RDFS.SUB_CLASS_OF, vehicle);
		SNOPS.associate(car1, Aras.HAS_PROPER_NAME, new SNText("Knut"));
		
		// detach 
		sna.detach(car1);
		
		assertEquals(3, car1.getAssociations().size());
		assertFalse(associations(car1, Aras.HAS_BRAND_NAME).isEmpty());
		assertTrue("Association not present", car1.getAssociations().contains(association));
		
		final boolean removedFlag = car1.removeAssociation(association);
		assertTrue(removedFlag);
		
		sna.attach(car1);
		
		final ResourceNode car2 = resolver.findResource(qnCar);
		assertNotSame(car1, car2);
		
		assertEquals(2, car2.getAssociations().size());
		assertTrue(associations(car2, Aras.HAS_BRAND_NAME).isEmpty());
		
	}
	
	@Test
	public void testRemove() {
		final SNClass vehicle = SNClass.from(new SNResource(qnVehicle));
		final SNClass car = SNClass.from(new SNResource(qnCar));
		final SNClass bike = SNClass.from(new SNResource(qnBike));
		
		final ResourceNode car1 = car.createInstance();
		
		SNOPS.associate(vehicle, RDFS.SUB_CLASS_OF, RDF.TYPE);
		SNOPS.associate(car, RDFS.SUB_CLASS_OF, vehicle);
		SNOPS.associate(bike, RDFS.SUB_CLASS_OF, vehicle);
		
		sna.attach(vehicle);
		sna.attach(bike);
		
		SNOPS.associate(car1, Aras.HAS_BRAND_NAME, new SNText("BMW"));
		SNOPS.associate(car1, Aras.HAS_PROPER_NAME, new SNText("Knut"));
		
		sna.attach(car1);

		sna.remove(car);
		assertNull(resolver.findResource(qnCar));
		sna.remove(car1);
		
		assertFalse(car1.isAttached());
		assertFalse(car.isAttached());
		
		assertTrue(vehicle.isAttached());
		
		sna.detach(vehicle);
		
		ResourceNode found = resolver.findResource(qnVehicle);
		assertNotNull(found);
		assertEquals(RDF.TYPE, SNOPS.singleObject(found, RDFS.SUB_CLASS_OF));
	}
	
	@Test
	public void testInferencingInverseOfBidirectional() {
		final ResourceNode knows = new SNResource(qnKnows);
		
		associate(knows, Aras.INVERSE_OF, knows);
		
		sna.attach(knows);
		
		assertTrue(SNOPS.objects(knows, Aras.INVERSE_OF).contains(knows));
		
		sna.detach(knows);
		
		// preparation done.
		
		ResourceNode mike = new SNResource(qualify("http://q#Mike"));
		ResourceNode kent = new SNResource(qualify("http://q#Kent"));
		
		sna.attach(mike);
		
		associate(mike, knows, kent);
		
		sna.detach(kent);
		
		kent = resolver.findResource(kent.getQualifiedName());
		
		assertTrue(SNOPS.objects(kent, knows).contains(mike));
		
	}
	
	@Test
	public void testInferencingInverseOf() {
		final ResourceNode hasEmployees = new SNResource(qnHasEmployees);
		final ResourceNode isEmployedBy = new SNResource(qnEmployedBy);
		
		associate(hasEmployees, Aras.INVERSE_OF, isEmployedBy);
		associate(isEmployedBy, Aras.INVERSE_OF, hasEmployees);
		
		sna.attach(hasEmployees);
		
		// preparation done.
		
		ResourceNode mike = new SNResource(qualify("http://q#Mike"));
		ResourceNode corp = new SNResource(qualify("http://q#Corp"));
		
		sna.attach(mike);
		
		associate(mike, isEmployedBy, corp);
		
		sna.detach(corp);
		
		corp = resolver.findResource(corp.getQualifiedName());
		
		assertTrue(SNOPS.objects(corp, hasEmployees).contains(mike));
		assertTrue(SNOPS.objects(mike, isEmployedBy).contains(corp));
		
		SNOPS.remove(corp, hasEmployees);
		
		assertFalse(SNOPS.objects(corp, hasEmployees).contains(mike));
		
		mike = resolver.findResource(mike.getQualifiedName());
		assertFalse(SNOPS.objects(mike, isEmployedBy).contains(corp));
		
	}
	
	@Test
	public void testInferencingSubClasses() {
		final SNClass vehicleClass = SNClass.from(new SNResource(qnVehicle));
		final SNClass carClass = SNClass.from(new SNResource(qnCar));
		SNOPS.associate(carClass, RDFS.SUB_CLASS_OF, vehicleClass);
		
		final SNEntity car = carClass.createInstance();
		final SNEntity vehicle = vehicleClass.createInstance();
		
		sna.attach(vehicle);
		sna.attach(car);

//		QueryResult hits = index.lookup(RDF.TYPE, id(qnVehicle));
		Iterable<QualifiedName> res = index.search(escapeColon(RDF.TYPE.toURI())+":"+escapeColon(id(qnVehicle).toURI()));
		assertNotNull(res);

		Iterator<QualifiedName> it = res.iterator();

		assertTrue(it.hasNext());
		it.next();
		assertTrue(it.hasNext());
		it.next();
		assertFalse(it.hasNext());

		SNOPS.remove(car, RDF.TYPE);

		res = index.search(escapeColon(RDF.TYPE.toURI())+":"+escapeColon(id(qnVehicle).toURI()));
		assertNotNull(res);

		it = res.iterator();

		assertTrue(it.hasNext());
		it.next();
		assertFalse(it.hasNext());

	}
	
	@Test
	public void testMultipleContexts() {
		final ResourceNode vehicle = new SNResource(qnVehicle);
		final ResourceNode car1 = new SNResource(qnCar);
		
		final String ctxNamepsace = "http://lf.de/ctx#";
		final SimpleContextID ctx1 = new SimpleContextID(ctxNamepsace, "ctx1");
		final SimpleContextID ctx2 = new SimpleContextID(ctxNamepsace, "ctx2");
		final SimpleContextID ctx3 = new SimpleContextID(ctxNamepsace, "ctx3");
		
		final SimpleContextID convCtx1 = new SimpleContextID(ctxNamepsace, "convCtx1");
		final SimpleContextID convCtx2 = new SimpleContextID(ctxNamepsace, "convCtx1");
		
		sna.attach(car1);

		ctx.setReadContexts(ctx1, ctx2, ctx3, convCtx1, convCtx2);
		
		associate(car1, Aras.HAS_BRAND_NAME, new SNText("BMW"), ctx1);
		ctx.setPrimaryContext(convCtx1);
		associate(car1, RDFS.SUB_CLASS_OF, vehicle, ctx1, ctx2);
		ctx.setPrimaryContext(convCtx2);
		associate(car1, Aras.HAS_PROPER_NAME, new SNText("Knut"), ctx1, ctx2, ctx3);
		
		// detach 
		sna.detach(car1);
		
		final ResourceNode car2 = resolver.findResource(qnCar);
		assertNotSame(car1, car2);
		
		final Context[] cl1 = SNOPS.singleAssociation(car2, Aras.HAS_BRAND_NAME).getContexts();
		final Context[] cl2 = SNOPS.singleAssociation(car2, RDFS.SUB_CLASS_OF).getContexts();
		final Context[] cl3 = SNOPS.singleAssociation(car2, Aras.HAS_PROPER_NAME).getContexts();
		
		assertArrayEquals(new Context[] {ctx1}, cl1);
		assertArrayEquals(new Context[] {convCtx1, ctx1, ctx2}, cl2);
		assertArrayEquals(new Context[] {convCtx2, ctx1, ctx2, ctx3}, cl3);
	}

	private static String escapeColon(String s) {
		return s.replaceAll(":", Matcher.quoteReplacement("\\:"));
	}
}
