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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.arastreju.bindings.neo4j.Neo4jModellingConversation;
import org.arastreju.bindings.neo4j.impl.GraphDataConnection;
import org.arastreju.bindings.neo4j.impl.GraphDataStore;
import org.arastreju.bindings.neo4j.impl.NeoConversationContext;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.apriori.Aras;
import org.arastreju.sge.apriori.RDFS;
import org.arastreju.sge.io.RdfXmlBinding;
import org.arastreju.sge.io.SemanticGraphIO;
import org.arastreju.sge.io.SemanticIOException;
import org.arastreju.sge.model.SemanticGraph;
import org.arastreju.sge.model.SimpleResourceID;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.SNResource;
import org.arastreju.sge.model.nodes.views.SNClass;
import org.arastreju.sge.naming.QualifiedName;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * <p>
 *  Test case for the {@link org.arastreju.bindings.neo4j.Neo4jModellingConversation}.
 * </p>
 *
 * <p>
 * 	Created Sep 9, 2010
 * </p>
 *
 * @author Oliver Tigges
 */
public class Neo4jModellingConversationTest {
	
	private GraphDataStore store;
	private Neo4jModellingConversation mc;
	private GraphDataConnection connection;
	
	// -----------------------------------------------------

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		store = new GraphDataStore();
		connection = new GraphDataConnection(store);
		mc = new Neo4jModellingConversation(connection, new NeoConversationContext(connection));
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		mc.close();
		connection.close();
		store.close();
	}
	
	// ----------------------------------------------------
	
	@Test
	public void testInstantiation() throws IOException{
		ResourceNode node = new SNResource(new QualifiedName("http://q#", "N1"));
		mc.attach(node);
	}
	
	@Test
	public void testFind() throws IOException{
		QualifiedName qn = new QualifiedName("http://q#", "N1");
		ResourceNode node = new SNResource(qn);
		mc.attach(node);
		
		ResourceNode node2 = mc.findResource(qn);
		
		assertNotNull(node2);
	}
	
	@Test
	public void testMerge() throws IOException{
		QualifiedName qn = new QualifiedName("http://q#", "N1");
		ResourceNode node = new SNResource(qn);
		mc.attach(node);
		
		mc.attach(node);
		
		ResourceNode node2 = mc.findResource(qn);
		
		assertNotNull(node2);
	}
	
	
	@Test
	public void testSNViews() throws IOException {
		final QualifiedName qnVehicle = new QualifiedName("http://q#", "Verhicle");
		ResourceNode vehicle = new SNResource(qnVehicle);
		mc.attach(vehicle);
		
		final QualifiedName qnCar = new QualifiedName("http://q#", "Car");
		ResourceNode car = new SNResource(qnCar);
		mc.attach(car);
		
		SNOPS.associate(car, RDFS.SUB_CLASS_OF, vehicle);
		
		mc.getConversationContext().clear();
		
		car = mc.findResource(qnCar);
		vehicle = mc.findResource(qnVehicle);
		
		Assert.assertTrue(SNClass.from(car).isSpecializationOf(vehicle));
	}
	
	@Test
	public void testGraphImport() throws IOException, SemanticIOException{
		final SemanticGraphIO io = new RdfXmlBinding();
		final SemanticGraph graph = io.read(getClass().getClassLoader().getResourceAsStream("test-statements.rdf.xml"));
		
		mc.attach(graph);
		
		final QualifiedName qn = new QualifiedName("http://test.arastreju.org/common#Person");
		final ResourceNode node = mc.findResource(qn);
		assertNotNull(node);
		
		final ResourceNode hasChild = mc.findResource(SNOPS.qualify("http://test.arastreju.org/common#hasChild"));
		assertNotNull(hasChild);
		assertEquals(new SimpleResourceID("http://test.arastreju.org/common#hasParent"), SNOPS.objects(hasChild, Aras.INVERSE_OF).iterator().next());
		
		final ResourceNode marriedTo = mc.findResource(SNOPS.qualify("http://test.arastreju.org/common#isMarriedTo"));
		assertNotNull(marriedTo);
		assertEquals(marriedTo, SNOPS.objects(marriedTo, Aras.INVERSE_OF).iterator().next());
	}
	
	@Test
	public void testSerialization() throws IOException, SemanticIOException, ClassNotFoundException {
		final SemanticGraphIO io = new RdfXmlBinding();
		final SemanticGraph graph = io.read(getClass().getClassLoader().getResourceAsStream("test-statements.rdf.xml"));
		mc.attach(graph);
		
		final QualifiedName qn = new QualifiedName("http://test.arastreju.org/common#Person");
		final ResourceNode node = mc.findResource(qn);
		
		assertTrue(node.isAttached());
		
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		new ObjectOutputStream(out).writeObject(node);
		
		byte[] bytes = out.toByteArray();
		out.close();
		
		final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
		
		final ResourceNode read = (ResourceNode) in.readObject();
		assertFalse(read.isAttached());
	}
	
}
