/*
 * Copyright 2012 by lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 */
package org.arastreju.bindings.neo4j.impl;

import org.arastreju.bindings.neo4j.extensions.NeoAssociationKeeper;
import org.arastreju.bindings.neo4j.extensions.SNResourceNeo;
import org.arastreju.bindings.neo4j.index.ResourceIndex;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.index.QNResolver;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.SimpleResourceID;
import org.arastreju.sge.model.associations.AssociationKeeper;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.ResourceResolver;
import org.neo4j.graphdb.Node;

/**
 * <p>
 *  Simple implementation of a resource resolver.
 * </p>
 *
 * <p>
 * 	Created Feb 14, 2012
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoResourceResolver implements ResourceResolver, QNResolver {

	private final NeoGraphDataConnection connection;
	private final NeoConversationContext conversationContext;
	
	// ----------------------------------------------------
	
	/**
	 * Constructor.
	 * @param connection The connection.
	 */
	public NeoResourceResolver(NeoGraphDataConnection connection, NeoConversationContext conversationContext) {
		this.connection = connection;
		this.conversationContext = conversationContext;
	}
	
	// ----------------------------------------------------

    @Override
	public ResourceNode findResource(final QualifiedName qn) {
		final AssociationKeeper keeper = findAssociationKeeper(qn);
		if (keeper != null) {
			return new SNResourceNeo(qn, keeper);
		} else {
			return null;
		}
	}
	
	@Override
	public ResourceNode resolve(final ResourceID resource) {
		final ResourceNode node = resource.asResource();
		if (node.isAttached()){
			return node;
		} else {
			final ResourceNode attached = findResource(resource.getQualifiedName());
			if (attached != null) {
				return attached;
			} else {
				new SemanticNetworkAccess(connection, conversationContext).create(node);
				return node;
			}
		}
	}

	@Override
	public ResourceNode resolve(QualifiedName qn) {
		return resolve(new SimpleResourceID(qn));
	}
	// ----------------------------------------------------
	
	/**
	 * Find the keeper for a qualified name.
	 * @param qn The qualified name.
	 * @return The keeper or null.
	 */
	protected AssociationKeeper findAssociationKeeper(final QualifiedName qn) {
		final AssociationKeeper registered = conversationContext.getAssociationKeeper(qn);
		if (registered != null && registered.isAttached()) {
			return registered;
		}
		final Node neoNode = new ResourceIndex(conversationContext).findNeoNode(qn);
		if (neoNode != null) {
			return createKeeper(qn, neoNode);
		} else {
			return null;
		}
	}
	
	// ----------------------------------------------------

	protected NeoAssociationKeeper createKeeper(QualifiedName qn, Node neoNode) {
		final NeoAssociationKeeper keeper = new NeoAssociationKeeper(SNOPS.id(qn), neoNode);
		conversationContext.attach(qn, keeper);
		return keeper;
	}
}
