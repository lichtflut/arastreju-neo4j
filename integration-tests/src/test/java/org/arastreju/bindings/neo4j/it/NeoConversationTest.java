/*
 * Copyright (C) 2013 lichtflut Forschungs- und Entwicklungsgesellschaft mbH
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

import org.arastreju.bindings.neo4j.NeoConversation;
import org.arastreju.bindings.neo4j.impl.NeoConversationContext;
import org.arastreju.bindings.neo4j.impl.NeoGraphDataConnection;
import org.arastreju.bindings.neo4j.impl.NeoGraphDataStore;
import org.arastreju.sge.Conversation;
import org.arastreju.sge.ConversationContext;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.apriori.Aras;
import org.arastreju.sge.apriori.RDF;
import org.arastreju.sge.apriori.RDFS;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.context.SimpleContextID;
import org.arastreju.sge.index.IndexProvider;
import org.arastreju.sge.io.RdfXmlBinding;
import org.arastreju.sge.io.SemanticGraphIO;
import org.arastreju.sge.io.SemanticIOException;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.SemanticGraph;
import org.arastreju.sge.model.SimpleResourceID;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.SNResource;
import org.arastreju.sge.model.nodes.ValueNode;
import org.arastreju.sge.model.nodes.views.SNClass;
import org.arastreju.sge.model.nodes.views.SNEntity;
import org.arastreju.sge.model.nodes.views.SNText;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.query.Query;
import org.arastreju.sge.query.QueryResult;
import org.arastreju.sge.spi.GraphDataConnection;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.arastreju.sge.SNOPS.associate;
import static org.arastreju.sge.SNOPS.associations;
import static org.arastreju.sge.SNOPS.objects;
import static org.arastreju.sge.SNOPS.qualify;
import static org.arastreju.sge.SNOPS.remove;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * <p>
 *  Test case for the {@link org.arastreju.bindings.neo4j.NeoConversation}.
 * </p>
 *
 * <p>
 * 	Created Sep 9, 2010
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoConversationTest {

    private final QualifiedName qnVehicle = new QualifiedName("http://q#", "Verhicle");
    private final QualifiedName qnCar = new QualifiedName("http://q#", "Car");
    private final QualifiedName qnBike = new QualifiedName("http://q#", "Bike");

    private final QualifiedName qnEmployedBy = new QualifiedName("http://q#", "employedBy");
    private final QualifiedName qnHasEmployees = new QualifiedName("http://q#", "hasEmployees");
    private final QualifiedName qnKnows = new QualifiedName("http://q#", "knows");

	private NeoGraphDataStore store;
    private GraphDataConnection connection;
	private Conversation conversation;

	// -----------------------------------------------------

	@Before
	public void setUp() throws Exception {
		store = new NeoGraphDataStore();
		connection = new NeoGraphDataConnection(store, new IndexProvider(store.getStorageDir()));
		conversation = new NeoConversation(new NeoConversationContext(connection));
	}

	@After
	public void tearDown() throws Exception {
		conversation.close();
		connection.close();
		store.close();
	}
	
	// ----------------------------------------------------
	
	@Test
	public void testInstantiation() throws IOException{
		ResourceNode node = new SNResource(new QualifiedName("http://q#", "N1"));
		conversation.attach(node);
	}
	
	@Test
	public void testFind() throws IOException{
		QualifiedName qn = new QualifiedName("http://q#", "N1");
		ResourceNode node = new SNResource(qn);
		conversation.attach(node);
		
		ResourceNode node2 = conversation.findResource(qn);
		
		assertNotNull(node2);
	}

    @Test
    public void testResolveAndFind() throws IOException {
        ResourceNode found = conversation.findResource(qnVehicle);
        assertNull(found);

        ResourceNode resolved = conversation.resolve(SNOPS.id(qnVehicle));
        assertNotNull(resolved);

        found = conversation.findResource(qnVehicle);
        assertNotNull(found);
    }
	
	@Test
	public void testMerge() throws IOException{
		QualifiedName qn = new QualifiedName("http://q#", "N1");
		ResourceNode node = new SNResource(qn);
		conversation.attach(node);
		
		conversation.attach(node);
		
		ResourceNode node2 = conversation.findResource(qn);
		
		assertNotNull(node2);
	}

    @Test
    public void testDetaching() throws IOException{
        final ResourceNode car = new SNResource(qnCar);
        conversation.attach(car);

        final ResourceNode car3 = conversation.findResource(qnCar);
        assertEquals(car, car3);

        conversation.detach(car);

        final ResourceNode car4 = conversation.findResource(qnCar);
        assertNotSame(car, car4);
    }

    @Test
    public void testMerging() throws IOException {
        final ResourceNode vehicle = new SNResource(qnVehicle);
        final ResourceNode car1 = new SNResource(qnCar);

        conversation.attach(car1);

        SNOPS.associate(car1, Aras.HAS_BRAND_NAME, new SNText("BMW"));

        // detach
        conversation.detach(car1);
        conversation.detach(vehicle);

        SNOPS.associate(car1, RDFS.SUB_CLASS_OF, vehicle);
        SNOPS.associate(car1, Aras.HAS_PROPER_NAME, new SNText("Knut"));

        // attach again
        conversation.attach(car1);

        // detach and find again
        conversation.detach(car1);
        final ResourceNode car2 = conversation.findResource(qnCar);
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
    public void testPersisting() throws IOException {
        final ResourceNode vehicle = new SNResource(qnVehicle);
        final ResourceNode car = new SNResource(qnCar);

        SNOPS.associate(car, RDFS.SUB_CLASS_OF, vehicle);
        SNOPS.associate(car, Aras.HAS_PROPER_NAME, new SNText("BMW"));

        conversation.attach(car);

        // detach and find again
        conversation.detach(car);
        final ResourceNode car2 = conversation.findResource(qnCar);
        assertNotSame(car, car2);

        final ResourceNode res = SNOPS.singleObject(car2, RDFS.SUB_CLASS_OF).asResource();
        assertEquals(vehicle, res);

        final ValueNode value = SNOPS.singleObject(car2, Aras.HAS_PROPER_NAME).asValue();
        assertEquals(value.getStringValue(), "BMW");
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

        conversation.attach(vehicle);
        conversation.attach(bike);

        SNOPS.associate(car1, Aras.HAS_BRAND_NAME, new SNText("BMW"));
        SNOPS.associate(car1, Aras.HAS_PROPER_NAME, new SNText("Knut"));

        conversation.attach(car1);

        conversation.remove(car);
        assertNull(conversation.findResource(qnCar));
        conversation.remove(car1);

        assertFalse(car1.isAttached());
        assertFalse(car.isAttached());

        assertTrue(vehicle.isAttached());

        conversation.detach(vehicle);

        ResourceNode found = conversation.findResource(qnVehicle);
        assertNotNull(found);
        assertEquals(RDF.TYPE, SNOPS.singleObject(found, RDFS.SUB_CLASS_OF));
    }

    @Test
    public void testDirectRemoval() {
        final ResourceNode vehicle = new SNResource(qnVehicle);
        final ResourceNode car1 = new SNResource(qnCar);

        final Statement association = SNOPS.associate(car1, Aras.HAS_BRAND_NAME, new SNText("BMW"));
        SNOPS.associate(car1, RDFS.SUB_CLASS_OF, vehicle);
        SNOPS.associate(car1, Aras.HAS_PROPER_NAME, new SNText("Knut"));

        conversation.attach(car1);

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

        conversation.attach(car1);

        final Statement association = SNOPS.associate(car1, Aras.HAS_BRAND_NAME, new SNText("BMW"));
        SNOPS.associate(car1, RDFS.SUB_CLASS_OF, vehicle);
        SNOPS.associate(car1, Aras.HAS_PROPER_NAME, new SNText("Knut"));

        // detach
        conversation.detach(car1);

        assertEquals(3, car1.getAssociations().size());
        assertFalse(associations(car1, Aras.HAS_BRAND_NAME).isEmpty());
        assertTrue("Association not present", car1.getAssociations().contains(association));

        final boolean removedFlag = car1.removeAssociation(association);
        assertTrue(removedFlag);

        conversation.attach(car1);

        final ResourceNode car2 = conversation.findResource(qnCar);
        assertNotSame(car1, car2);

        assertEquals(2, car2.getAssociations().size());
        assertTrue(associations(car2, Aras.HAS_BRAND_NAME).isEmpty());

    }

    @Test
    public void testBidirectionalAssociations() {
        final ResourceNode vehicle = new SNResource(qnVehicle);
        final ResourceNode car = new SNResource(qnCar);

        final ResourceID pred1 = new SimpleResourceID("http://arastreju.org/stuff#", "P1");
        final ResourceID pred2 = new SimpleResourceID("http://arastreju.org/stuff#", "P2");

        SNOPS.associate(vehicle, pred1, car);
        SNOPS.associate(car, pred2, vehicle);
        conversation.attach(vehicle);

        final ResourceNode vehicleLoaded = conversation.findResource(qnVehicle);
        final ResourceNode carLoaded = conversation.findResource(qnCar);

        assertFalse(vehicleLoaded.getAssociations().isEmpty());
        assertFalse(carLoaded.getAssociations().isEmpty());

        assertFalse(associations(vehicleLoaded, pred1).isEmpty());
        assertFalse(associations(carLoaded, pred2).isEmpty());
    }

    @Test
    public void testValueIndexing() throws IOException {
        final ResourceNode car = new SNResource(qnCar);
        SNOPS.associate(car, Aras.HAS_PROPER_NAME, new SNText("BMW"));

        conversation.attach(car);

        Query query = conversation.createQuery().addField(Aras.HAS_PROPER_NAME.toURI(), "BMW");
        QueryResult result = query.getResult();

        assertNotNull(result);

        assertFalse(result.isEmpty());

        assertEquals(qnCar, result.getSingleNode().getQualifiedName());
    }

    @Test
    public void testDataTypes() throws IOException {
        final ResourceNode car = new SNResource(qnCar);

        conversation.attach(car);

        SNOPS.associate(car, Aras.HAS_PROPER_NAME, new SNText("BMW"));
        conversation.detach(car);

        final ResourceNode car2 = conversation.findResource(qnCar);
        assertNotSame(car, car2);
        final ValueNode value = SNOPS.singleObject(car2, Aras.HAS_PROPER_NAME).asValue();

        assertEquals(value.getStringValue(), "BMW");
    }
	
	
	@Test
	public void testSNViews() throws IOException {
		final QualifiedName qnVehicle = new QualifiedName("http://q#", "Verhicle");
		ResourceNode vehicle = new SNResource(qnVehicle);
		conversation.attach(vehicle);
		
		final QualifiedName qnCar = new QualifiedName("http://q#", "Car");
		ResourceNode car = new SNResource(qnCar);
		conversation.attach(car);
		
		SNOPS.associate(car, RDFS.SUB_CLASS_OF, vehicle);
		
		conversation.getConversationContext().clear();
		
		car = conversation.findResource(qnCar);
		vehicle = conversation.findResource(qnVehicle);
		
		Assert.assertTrue(SNClass.from(car).isSpecializationOf(vehicle));
	}
	
	@Test
	public void testGraphImport() throws IOException, SemanticIOException{
		final SemanticGraphIO io = new RdfXmlBinding();
		final SemanticGraph graph = io.read(getClass().getClassLoader().getResourceAsStream("test-statements.rdf.xml"));
		
		conversation.attach(graph);
		
		final QualifiedName qn = new QualifiedName("http://test.arastreju.org/common#Person");
		final ResourceNode node = conversation.findResource(qn);
		assertNotNull(node);
		
		final ResourceNode hasChild = conversation.findResource(SNOPS.qualify("http://test.arastreju.org/common#hasChild"));
		assertNotNull(hasChild);
		assertEquals(new SimpleResourceID("http://test.arastreju.org/common#hasParent"), objects(hasChild, Aras.INVERSE_OF).iterator().next());
		
		final ResourceNode marriedTo = conversation.findResource(SNOPS.qualify("http://test.arastreju.org/common#isMarriedTo"));
		assertNotNull(marriedTo);
		assertEquals(marriedTo, objects(marriedTo, Aras.INVERSE_OF).iterator().next());
	}
	
	@Test
	public void testSerialization() throws IOException, SemanticIOException, ClassNotFoundException {
		final SemanticGraphIO io = new RdfXmlBinding();
		final SemanticGraph graph = io.read(getClass().getClassLoader().getResourceAsStream("test-statements.rdf.xml"));
		conversation.attach(graph);
		
		final QualifiedName qn = new QualifiedName("http://test.arastreju.org/common#Person");
		final ResourceNode node = conversation.findResource(qn);
		
		assertTrue(node.isAttached());
		
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		new ObjectOutputStream(out).writeObject(node);
		
		byte[] bytes = out.toByteArray();
		out.close();
		
		final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
		
		final ResourceNode read = (ResourceNode) in.readObject();
		assertFalse(read.isAttached());
	}

    @Test
    public void testInferencingInverseOfBidirectional() {
        final ResourceNode knows = new SNResource(qnKnows);

        associate(knows, Aras.INVERSE_OF, knows);

        conversation.attach(knows);

        assertTrue(objects(knows, Aras.INVERSE_OF).contains(knows));

        conversation.detach(knows);

        // preparation done.

        ResourceNode mike = new SNResource(qualify("http://q#Mike"));
        ResourceNode kent = new SNResource(qualify("http://q#Kent"));

        conversation.attach(mike);

        associate(mike, knows, kent);

        conversation.detach(kent);

        kent = conversation.findResource(kent.getQualifiedName());

        assertTrue(objects(kent, knows).contains(mike));

    }

    @Test
    public void testInferencingInverseOf() {
        final ResourceNode hasEmployees = new SNResource(qnHasEmployees);
        final ResourceNode isEmployedBy = new SNResource(qnEmployedBy);

        associate(hasEmployees, Aras.INVERSE_OF, isEmployedBy);
        associate(isEmployedBy, Aras.INVERSE_OF, hasEmployees);

        conversation.attach(hasEmployees);

        Assert.assertTrue(hasEmployees.isAttached());
        Assert.assertTrue(isEmployedBy.isAttached());

        // preparation done.

        ResourceNode mike = new SNResource(qualify("http://q#Mike"));
        ResourceNode corp = new SNResource(qualify("http://q#Corp"));

        conversation.attach(mike);
        Assert.assertTrue(mike.isAttached());

        associate(mike, isEmployedBy, corp);
        Assert.assertTrue(corp.isAttached());

        conversation.detach(corp);

        corp = conversation.findResource(corp.getQualifiedName());

        assertTrue(objects(corp, hasEmployees).contains(mike));
        assertTrue(objects(mike, isEmployedBy).contains(corp));

        remove(corp, hasEmployees);
        assertFalse(objects(corp, hasEmployees).contains(mike));

        mike = conversation.findResource(mike.getQualifiedName());
        assertFalse(objects(mike, isEmployedBy).contains(corp));

    }

    @Test
    public void testInferencingSubClasses() {
        final SNClass vehicleClass = SNClass.from(new SNResource(qnVehicle));
        final SNClass carClass = SNClass.from(new SNResource(qnCar));
        SNOPS.associate(carClass, RDFS.SUB_CLASS_OF, vehicleClass);

        final SNEntity car = carClass.createInstance();
        final SNEntity vehicle = vehicleClass.createInstance();

        conversation.attach(vehicle);
        conversation.attach(car);

		QueryResult res = conversation.createQuery().addField(RDF.TYPE.toURI(), qnVehicle).getResult();
        assertNotNull(res);

        assertEquals(2, res.size());

        remove(car, RDF.TYPE);

        res = conversation.createQuery().addField(RDF.TYPE.toURI(), qnVehicle).getResult();
        assertNotNull(res);

        assertEquals(1, res.size());

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

        conversation.attach(car1);

        ConversationContext ctx = conversation.getConversationContext();

        ctx.setReadContexts(ctx1, ctx2, ctx3, convCtx1, convCtx2);

        associate(car1, Aras.HAS_BRAND_NAME, new SNText("BMW"), ctx1);
        ctx.setPrimaryContext(convCtx1);
        associate(car1, RDFS.SUB_CLASS_OF, vehicle, ctx1, ctx2);
        ctx.setPrimaryContext(convCtx2);
        associate(car1, Aras.HAS_PROPER_NAME, new SNText("Knut"), ctx1, ctx2, ctx3);

        // detach 
        conversation.detach(car1);

        final ResourceNode car2 = conversation.findResource(qnCar);
        assertNotSame(car1, car2);

        final Context[] cl1 = SNOPS.singleAssociation(car2, Aras.HAS_BRAND_NAME).getContexts();
        final Context[] cl2 = SNOPS.singleAssociation(car2, RDFS.SUB_CLASS_OF).getContexts();
        final Context[] cl3 = SNOPS.singleAssociation(car2, Aras.HAS_PROPER_NAME).getContexts();

        assertArrayEquals(new Context[] {ctx1}, cl1);
        assertArrayEquals(new Context[] {convCtx1, ctx1, ctx2}, cl2);
        assertArrayEquals(new Context[] {convCtx2, ctx1, ctx2, ctx3}, cl3);
    }
	
}
