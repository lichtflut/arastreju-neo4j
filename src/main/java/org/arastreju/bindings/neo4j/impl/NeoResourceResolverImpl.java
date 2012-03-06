/*
 * Copyright 2012 by lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 */
package org.arastreju.bindings.neo4j.impl;

import org.arastreju.bindings.neo4j.NeoConstants;
import org.arastreju.bindings.neo4j.extensions.NeoAssociationKeeper;
import org.arastreju.bindings.neo4j.extensions.NeoResourceResolver;
import org.arastreju.bindings.neo4j.extensions.SNResourceNeo;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.associations.AssociationKeeper;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.naming.QualifiedName;
import org.neo4j.graphdb.Node;

/**
 * <p>
 *  Simple implementation of {@link NeoResourceResolver}.
 * </p>
 *
 * <p>
 * 	Created Feb 14, 2012
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoResourceResolverImpl implements NeoResourceResolver {
	
	private final GraphDataConnection connection;
	
	// ----------------------------------------------------
	
	/**
	 * Constructor.
	 * @param connection The connection.
	 */
	public NeoResourceResolverImpl(GraphDataConnection connection) {
		this.connection = connection;
	}
	
	// ----------------------------------------------------
	
	/**
	 * {@inheritDoc}
	 */
	public ResourceNode findResource(final QualifiedName qn) {
		final AssociationKeeper keeper = findAssociationKeeper(qn);
		if (keeper != null) {
			return new SNResourceNeo(qn, keeper);
		} else {
			return null;
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ResourceNode resolve(final ResourceID resource) {
		final ResourceNode node = resource.asResource();
		if (node.isAttached()){
			return node;
		} else {
			final ResourceNode attached = findResource(resource.getQualifiedName());
			if (attached != null) {
				return attached;
			} else {
				connection.getSemanticNetworkAccess().create(node);
				return node;
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ResourceNode resolve(final Node neoNode) {
		final QualifiedName qn = QualifiedName.create(neoNode.getProperty(NeoConstants.PROPERTY_URI).toString());
		NeoAssociationKeeper keeper = connection.getWorkingContext().getAssociationKeeper(qn);
		if (keeper == null){
			keeper = createKeeper(qn, neoNode);
		}
		return new SNResourceNeo(qn, keeper);	
	}
	
	// ----------------------------------------------------
	
	/**
	 * Find the keeper for a qualified name.
	 * @param qn The qualified name.
	 * @return The keeper or null.
	 */
	protected AssociationKeeper findAssociationKeeper(final QualifiedName qn) {
		final AssociationKeeper registered = connection.getWorkingContext().getAssociationKeeper(qn);
		if (registered != null) {
			return registered;
		}
		final Node neoNode = connection.getIndex().findNeoNode(qn);
		if (neoNode != null) {
			return createKeeper(qn, neoNode);
		} else {
			return null;
		}
	}
	
	// ----------------------------------------------------

	protected NeoAssociationKeeper createKeeper(QualifiedName qn, Node neoNode) {
		final NeoAssociationKeeper keeper = new NeoAssociationKeeper(SNOPS.id(qn), neoNode);
		connection.getWorkingContext().attach(qn, keeper);
		return keeper;
	}
}
