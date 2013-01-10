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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Assert;

import org.arastreju.bindings.neo4j.Neo4jGate;
import org.arastreju.bindings.neo4j.NeoOrganizer;
import org.arastreju.bindings.neo4j.impl.GraphDataConnection;
import org.arastreju.bindings.neo4j.impl.GraphDataStore;
import org.arastreju.bindings.neo4j.impl.NeoConversationContext;
import org.arastreju.bindings.neo4j.tx.NeoTxProvider;
import org.arastreju.sge.ModelingConversation;
import org.arastreju.sge.Organizer;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.context.PhysicalDomain;
import org.arastreju.sge.io.StatementContainer;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.SimpleResourceID;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.SNResource;
import org.arastreju.sge.naming.Namespace;
import org.arastreju.sge.naming.QualifiedName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * <p>
 *  Test case for {@link org.arastreju.bindings.neo4j.NeoOrganizer}.
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

    private final ResourceID knows = new SimpleResourceID("http://q#", "knows");
	
	private Organizer organizer;
	private GraphDataStore store;
	private GraphDataConnection connection;
    private Neo4jGate gate;

    // -----------------------------------------------------

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		store = new GraphDataStore();
		connection = new GraphDataConnection(store, new NeoTxProvider());
        gate = new Neo4jGate(new PhysicalDomain("test"), connection);
        organizer = new NeoOrganizer(connection, gate);
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		connection.close();
        gate.close();
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

    @Test
    public void testContextExport() {
        Context c1 = organizer.registerContext(ctx1);
        Context c2 = organizer.registerContext(ctx2);
        Context c3 = organizer.registerContext(ctx3);

        ModelingConversation conv = gate.startConversation();

        ResourceNode mike = new SNResource(new QualifiedName(ns1, "Mike"));

        conv.getConversationContext().setPrimaryContext(c1);
        conv.addStatement(mike.addAssociation(knows, new SNResource()));

        conv.getConversationContext().setPrimaryContext(c2);
        conv.addStatement(mike.addAssociation(knows, new SNResource()));
        conv.addStatement(mike.addAssociation(knows, new SNResource()));

        List<Statement> statements = toList(organizer.getStatements(c1));
        Assert.assertEquals(1, statements.size());

        statements = toList(organizer.getStatements(c2));
        Assert.assertEquals(2, statements.size());

        statements = toList(organizer.getStatements(c3));
        Assert.assertEquals(0, statements.size());

    }

    private List<Statement> toList(StatementContainer container) {
        List<Statement> result = new ArrayList<Statement>();
        for (Statement current : container) {
            result.add(current);
        }
        return result;
    }

}
