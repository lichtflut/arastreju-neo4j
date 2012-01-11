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

import org.arastreju.bindings.neo4j.impl.SemanticNetworkAccess;
import org.arastreju.bindings.neo4j.query.NeoQueryManager;
import org.arastreju.sge.ArastrejuGate;
import org.arastreju.sge.IdentityManagement;
import org.arastreju.sge.ModelingConversation;
import org.arastreju.sge.Organizer;
import org.arastreju.sge.query.QueryManager;
import org.arastreju.sge.security.LoginException;
import org.arastreju.sge.security.User;
import org.arastreju.sge.spi.GateContext;
import org.arastreju.sge.spi.GateInitializationException;
import org.arastreju.sge.spi.LoginContext;

/**
 * <p>
 * 	Neo4j specific implementation of {@link ArastrejuGate}.
 * </p>
 * 
 * <p>
 * 	Created Jan 4, 2011
 * </p>
 * 
 * @author Oliver Tigges
 */
public class Neo4jGate implements ArastrejuGate {
	
	private final SemanticNetworkAccess sna;
	private final User user;

	// -----------------------------------------------------

	/**
	 * Initialize default gate.
	 * @param ctx The gate context.
	 */
	public Neo4jGate(final GateContext ctx, SemanticNetworkAccess sna) throws GateInitializationException {
		this.sna = sna;
		this.user = null;
	}
	
	/**
	 * Initialize default gate and perform login.
	 * @param ctx The gate context.
	 * @param sna The semantic network access object.
	 */
	public Neo4jGate(final LoginContext ctx, SemanticNetworkAccess sna) throws GateInitializationException {
		this.sna = sna;
		try {
			user = getIdentityManagement().login(ctx.getUsername(), ctx.getCredential());
		} catch (LoginException e) {
			throw new GateInitializationException(e);
		}
	}

	// -----------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	public ModelingConversation startConversation() {
		return new Neo4jModellingConversation(sna);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public QueryManager createQueryManager() {
		return new NeoQueryManager(sna, sna.getIndex());
	}

	/**
	 * {@inheritDoc}
	 */
	public Organizer getOrganizer() {
		return new NeoOrganizer(sna);
	}

	/**
	 * {@inheritDoc}
	 */

	public IdentityManagement getIdentityManagement() {
		return new NeoIdentityManagement(sna);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void close() {
	}
	
	// ----------------------------------------------------
	
	/**
	 * The user associated with this gate.
	 * @return The user
	 */
	public User getUser() {
		return user;
	}
	
}
