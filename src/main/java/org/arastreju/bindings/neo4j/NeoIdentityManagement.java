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

import static org.arastreju.sge.SNOPS.associate;
import static org.arastreju.sge.SNOPS.assure;
import static org.arastreju.sge.SNOPS.remove;
import static org.arastreju.sge.SNOPS.singleObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.arastreju.bindings.neo4j.extensions.NeoResourceResolver;
import org.arastreju.bindings.neo4j.impl.GraphDataConnection;
import org.arastreju.bindings.neo4j.impl.SemanticNetworkAccess;
import org.arastreju.bindings.neo4j.index.ResourceIndex;
import org.arastreju.bindings.neo4j.query.NeoQueryBuilder;
import org.arastreju.sge.IdentityManagement;
import org.arastreju.sge.apriori.Aras;
import org.arastreju.sge.apriori.RDF;
import org.arastreju.sge.eh.ArastrejuException;
import org.arastreju.sge.eh.ArastrejuRuntimeException;
import org.arastreju.sge.eh.ErrorCodes;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.SNResource;
import org.arastreju.sge.model.nodes.views.SNEntity;
import org.arastreju.sge.model.nodes.views.SNText;
import org.arastreju.sge.query.Query;
import org.arastreju.sge.query.QueryResult;
import org.arastreju.sge.security.Credential;
import org.arastreju.sge.security.LoginException;
import org.arastreju.sge.security.Permission;
import org.arastreju.sge.security.Role;
import org.arastreju.sge.security.User;
import org.arastreju.sge.security.impl.SNPermission;
import org.arastreju.sge.security.impl.SNRole;
import org.arastreju.sge.security.impl.SNUser;
import org.arastreju.sge.spi.GateContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *  Neo4J specific Identity Management. 
 * </p>
 *
 * <p>
 * 	Created Apr 29, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoIdentityManagement implements IdentityManagement {
	
	private final Logger logger = LoggerFactory.getLogger(NeoIdentityManagement.class);
	
	private final ResourceIndex index;
	
	private final SemanticNetworkAccess sna;

	private final NeoResourceResolver resolver;

	private final GateContext ctx;

	// -----------------------------------------------------
	
	/**
	 * Constructor.
	 * @param connection The connection store.
	 * @param ctx The gate context.
	 */
	public NeoIdentityManagement(GraphDataConnection connection, GateContext ctx) {
		this.ctx = ctx;
		this.sna = connection.getSemanticNetworkAccess();
		this.resolver = connection.getResourceResolver();
		this.index = connection.getIndex();
	}
	
	// -----------------------------------------------------
	
	/**
	 * {@inheritDoc}
	 */
	public User findUser(final String identity) {
		final QueryResult result = index.lookup(Aras.IDENTIFIED_BY, identity);
		if (result.size() > 1) {
			logger.error("More than one user with name '" + identity + "' found.");
			throw new IllegalStateException("More than on user with name '" + identity + "' found.");
		} else if (result.isEmpty()) {
			return null;
		} else {
			return new SNUser(result.getSingleNode());
		}
	};

	/**
	 * {@inheritDoc}
	 */
	public User login(final String name, final Credential credential) throws LoginException {
		logger.debug("trying to login user '" + name + "'.");
		if (name == null) {
			throw new LoginException(ErrorCodes.LOGIN_INVALID_DATA, "No username given");	
		}
		final QueryResult found = index.lookup(Aras.IDENTIFIED_BY, name);
		if (found.size() > 1) {
			logger.error("More than on user with name '" + name + "' found.");
			throw new IllegalStateException("More than on user with name '" + name + "' found.");
		}
		if (found.isEmpty()){
			throw new LoginException(ErrorCodes.LOGIN_USER_NOT_FOUND, "User does not exist: " + name);	
		}
		
		final SNEntity user = found.getSingleNode().asEntity();
		if (!credential.applies(singleObject(user, Aras.HAS_CREDENTIAL))){
			throw new LoginException(ErrorCodes.LOGIN_USER_CREDENTIAL_NOT_MATCH, "Wrong credential");
		}
		
		return new SNUser(user);
	}

	/**
	 * {@inheritDoc}
	 */
	public User register(final String uniqueName, final Credential credential) throws ArastrejuException {
		return register(uniqueName, credential, new SNResource());
	}

	/**
	 * {@inheritDoc}
	 */
	public User register(final String name, final Credential credential, final ResourceNode corresponding) throws ArastrejuException {
		assertUniqueIdentity(name);
		assure(corresponding, RDF.TYPE, Aras.USER, Aras.IDENT);
		assure(corresponding, Aras.BELONGS_TO_DOMAIN, ctx.getDomain(), Aras.IDENT);
		assure(corresponding, Aras.HAS_CREDENTIAL, new SNText(credential.stringRepesentation()), Aras.IDENT);
		associate(corresponding, Aras.IDENTIFIED_BY, new SNText(name), Aras.IDENT);
		sna.attach(corresponding);
		return new SNUser(corresponding);
	}

	/** 
	* {@inheritDoc}
	*/
	public User registerAlternateID(User user, String uniqueName) throws ArastrejuException {
		assertUniqueIdentity(uniqueName);
		final ResourceNode node = resolver.resolve(user);
		associate(node, Aras.IDENTIFIED_BY, new SNText(uniqueName), Aras.IDENT);
		return user;
	}
	
	/** 
	 * {@inheritDoc}
	 */
	public boolean isIdentifierInUse(String identifier) {
		final QueryResult result = index.lookup(Aras.IDENTIFIED_BY, identifier);
		if (result.isEmpty()) {
			return false;
		} else {
			return true;
		}
	}
	
	// ----------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	public Role registerRole(final String name) {
		final ResourceNode existing = findItem(Aras.ROLE, name);
		if (existing != null) {
			return new SNRole(existing);
		}
		final SNResource role = new SNResource();
		associate(role, Aras.HAS_UNIQUE_NAME, new SNText(name), Aras.IDENT);
		associate(role, RDF.TYPE, Aras.ROLE, Aras.IDENT);
		sna.attach(role);
		return new SNRole(role);
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<Role> getRoles() {
		final List<ResourceNode> nodes = index.lookup(RDF.TYPE, Aras.ROLE).toList();
		final Set<Role> roles = new HashSet<Role>(nodes.size());
		for(ResourceNode current: nodes) {
			roles.add(new SNRole(current));
		}
		return roles;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addUserToRoles(final User user, final Role... roles) {
		sna.attach(user);
		for (Role role : roles) {
			associate(user, Aras.HAS_ROLE, role, Aras.IDENT);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void removeUserFromRoles(final User user, final Role... roles) {
		for (Role role : roles) {
			remove(user, Aras.HAS_ROLE, role);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void addPermissionsToRole(final Role role, final Permission... permissions) {
		sna.attach(role);
		for (Permission permission : permissions) {
			associate(role, Aras.CONTAINS, permission, Aras.IDENT);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Permission registerPermission(final String name) {
		final ResourceNode existing = findItem(Aras.PERMISSION, name);
		if (existing != null) {
			return new SNPermission(existing);
		}
		final SNResource permission = new SNResource();
		associate(permission, Aras.HAS_UNIQUE_NAME, new SNText(name), Aras.IDENT);
		associate(permission, RDF.TYPE, Aras.PERMISSION, Aras.IDENT);
		sna.attach(permission);
		return new SNPermission(permission);
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<Permission> getPermissions() {
		final List<ResourceNode> nodes = index.lookup(RDF.TYPE, Aras.PERMISSION).toList();
		final Set<Permission> permissions = new HashSet<Permission>(nodes.size());
		for(ResourceNode current: nodes) {
			permissions.add(new SNPermission(current));
		}
		return permissions;
	}
	
	// -----------------------------------------------------
	
	private ResourceNode findItem(final ResourceID type, final String name) {
		final Query query = new NeoQueryBuilder(index);
		query.addField(RDF.TYPE, type);
		query.and();
		query.addField(Aras.HAS_UNIQUE_NAME, name);
		final QueryResult result = query.getResult();
		if (result.size() > 1) {
			throw new ArastrejuRuntimeException(ErrorCodes.GENERAL_CONSISTENCY_FAILURE, 
					"Unique name is not unique for " + type + ": " + name);
		} else {
			return result.getSingleNode();
		}
	}
	
	protected void assertUniqueIdentity(final String name) throws ArastrejuException {
		final QueryResult found = index.lookup(Aras.IDENTIFIED_BY, name);
		if (found.size() > 0) {
			logger.error("More than on user with name '" + name + "' found.");
			throw new ArastrejuException(ErrorCodes.REGISTRATION_NAME_ALREADY_IN_USE, 
					"More than on user with name '" + name + "' found.");
		}
	}
	
}
