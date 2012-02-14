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

import java.util.Set;

import junit.framework.Assert;

import org.arastreju.bindings.neo4j.impl.GraphDataConnection;
import org.arastreju.bindings.neo4j.impl.GraphDataStore;
import org.arastreju.bindings.neo4j.impl.SemanticNetworkAccess;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.apriori.Aras;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.eh.ArastrejuException;
import org.arastreju.sge.eh.ErrorCodes;
import org.arastreju.sge.model.nodes.views.SNClass;
import org.arastreju.sge.model.nodes.views.SNEntity;
import org.arastreju.sge.model.nodes.views.SNText;
import org.arastreju.sge.security.LoginException;
import org.arastreju.sge.security.PasswordCredential;
import org.arastreju.sge.security.Permission;
import org.arastreju.sge.security.Role;
import org.arastreju.sge.security.User;
import org.arastreju.sge.spi.GateContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * <p>
 *  Testcase for {@link NeoIdentityManagement}.
 * </p>
 *
 * <p>
 * 	Created Apr 29, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoIdentityManagementTest {

	private SemanticNetworkAccess sna;
	private NeoIdentityManagement im;
	private GraphDataStore store;
	private GraphDataConnection connection;
	
	// -----------------------------------------------------

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		store = new GraphDataStore();
		connection = new GraphDataConnection(store);
		sna = connection.getSemanticNetworkAccess();
		im = new NeoIdentityManagement(connection, new GateContext(null));
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		connection.close();
		store.close();
	}
	
	// -----------------------------------------------------
	
	@Test
	public void testLogin() {
		final Context ctx = Aras.IDENT;
		final SNClass identity = connection.getResourceResolver().resolve(Aras.IDENTITY).asClass();
		final SNEntity user = identity.createInstance(ctx);
		
		SNOPS.associate(user, Aras.IDENTIFIED_BY, new SNText("Bud Spencer"), ctx);
		SNOPS.associate(user, Aras.HAS_UNIQUE_NAME, new SNText("Bud Spencer"), ctx);
		SNOPS.associate(user, Aras.HAS_CREDENTIAL, new SNText("bud"), ctx);
		SNOPS.associate(user, Aras.HAS_EMAIL, new SNText("BudSpencer@lichtflut.de"), ctx);
		sna.attach(user);
		sna.detach(user);
		
		try {
			im.login("Bud Spencer", new PasswordCredential("wrong password"));
		} catch (LoginException e) {
			Assert.assertEquals(ErrorCodes.LOGIN_USER_CREDENTIAL_NOT_MATCH, e.getErrCode());
		}
		
		try {
			im.login("Terence Hill", new PasswordCredential(null));
		} catch (LoginException e) {
			Assert.assertEquals(ErrorCodes.LOGIN_USER_NOT_FOUND, e.getErrCode());
		}
		
		try {
			final User loggedIn = im.login("Bud Spencer", new PasswordCredential("bud"));
			Assert.assertEquals("Bud Spencer", loggedIn.getName());
			Assert.assertEquals("BudSpencer@lichtflut.de", loggedIn.getEmail());
		} catch (LoginException e) {
			Assert.fail("User should have logged in.");
		}
		
	}
	
	@Test
	public void testRegistration() throws LoginException, ArastrejuException {
		final User user = im.register("bud", new PasswordCredential("spencer"));
		Assert.assertEquals("bud", user.getName());
		Assert.assertTrue(user.isAttached());
		
		User loggedIn = im.login("bud", new PasswordCredential("spencer"));
		Assert.assertNotNull(loggedIn);
		Assert.assertEquals(user, loggedIn);
		
		final SNEntity node = new SNEntity();
		final User user2 = im.register("terrence", new PasswordCredential("hill"), node);
		Assert.assertEquals("terrence", user2.getName());
		Assert.assertTrue(user2.isAttached());
		
		User loggedIn2 = im.login("terrence", new PasswordCredential("hill"));
		Assert.assertNotNull(loggedIn2);
		Assert.assertEquals(user2, loggedIn2);
		Assert.assertEquals(node, loggedIn2);
	}
	
	@Test
	public void testRoles() {
		Role anything = im.registerRole("anything");
		Role nothing = im.registerRole("nothing");
		
		Assert.assertEquals("anything", anything.getName());
		
		Assert.assertEquals("nothing", nothing.getName());
		
		Set<Role> roles = im.getRoles();
		Assert.assertEquals(2, roles.size());
		Assert.assertTrue(roles.contains(anything));
		Assert.assertTrue(roles.contains(nothing));
		
		im.registerPermission("anything");
		im.registerRole("anything");	
	}
	
	@Test
	public void testPermissions() {
		Permission anything = im.registerPermission("anything");
		Permission nothing = im.registerPermission("nothing");
		
		Assert.assertEquals("anything", anything.getName());
		
		Assert.assertEquals("nothing", nothing.getName());
		
		Set<Permission> permissions = im.getPermissions();
		Assert.assertEquals(2, permissions.size());
		Assert.assertTrue(permissions.contains(anything));
		Assert.assertTrue(permissions.contains(nothing));
		
		im.registerRole("anything");
		im.registerPermission("anything");	
	}
	
}
