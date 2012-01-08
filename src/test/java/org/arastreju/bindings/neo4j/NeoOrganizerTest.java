/*
 * Copyright (C) 2010 lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.arastreju.bindings.neo4j;

import java.util.Collection;

import junit.framework.Assert;

import org.arastreju.bindings.neo4j.impl.GraphDataStore;
import org.arastreju.bindings.neo4j.impl.SemanticNetworkAccess;
import org.arastreju.sge.Organizer;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.naming.Namespace;
import org.arastreju.sge.naming.QualifiedName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * <p>
 *  Test case for {@link NeoOrganizer}.
 * </p>
 *
 * <p>
 * 	Created Nov 29, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoOrganizerTest {
	
	private static final String ns1 = "http://test.lf.de/ns1";
	private static final String ns2 = "http://test.lf.de/ns2";
	private static final String ns3 = "http://test.lf.de/ns3";
	
	private static final QualifiedName ctx1 = new QualifiedName("http://test.lf.de#", "Ctx1");
	private static final QualifiedName ctx2 = new QualifiedName("http://test.lf.de#", "Ctx2");
	private static final QualifiedName ctx3 = new QualifiedName("http://test.lf.de#", "Ctx3");
	
	private Organizer organizer;
	private SemanticNetworkAccess sna;
	private GraphDataStore store;
	
	// -----------------------------------------------------

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		store = new GraphDataStore();
		sna = new SemanticNetworkAccess(store);
		organizer = new NeoOrganizer(sna);
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		sna.close();
		store.close();
	}
	
	// ----------------------------------------------------
	
	@Test
	public void testNamespaceRegistration() {
		organizer.registerNamespace(ns1, "a");
		organizer.registerNamespace(ns2, "b");
		organizer.registerNamespace(ns2, "c");
		organizer.registerNamespace(ns3, "d");
		
		Collection<Namespace> result = organizer.getNamespaces();
		Assert.assertEquals(3, result.size());
	}
	
	@Test
	public void testContextRegistration() {
		organizer.registerContext(ctx1);
		organizer.registerContext(ctx2);
		organizer.registerContext(ctx3);
		
		organizer.registerContext(ctx3);
		organizer.registerContext(ctx3);
		organizer.registerContext(ctx3);
		
		Collection<Context> result = organizer.getContexts();
		Assert.assertEquals(3, result.size());
	}

}
