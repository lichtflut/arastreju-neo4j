/*
 * Copyright 2012 by lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 */
package org.arastreju.bindings.neo4j.impl;

import org.arastreju.bindings.neo4j.extensions.SNResourceNeo;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.associations.AssociationKeeper;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.ResourceResolver;

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
public class NeoResourceResolver implements ResourceResolver {

	private final NeoConversationContext conversationContext;
	
	// ----------------------------------------------------
	
	/**
	 * Constructor.
     */
	public NeoResourceResolver(NeoConversationContext conversationContext) {
		this.conversationContext = conversationContext;
	}
	
	// ----------------------------------------------------

    @Override
	public ResourceNode findResource(final QualifiedName qn) {
		final AssociationKeeper keeper =  conversationContext.find(qn);
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
            final QualifiedName qn = resource.getQualifiedName();
            final ResourceNode attached = findResource(qn);
			if (attached != null) {
				return attached;
			} else {
                new SemanticNetworkAccess(conversationContext).create(node);
                return node;
                //NeoAssociationKeeper created = conversationContext.create(qn);
                //return new SNResourceNeo(qn, created);
			}
		}
	}

}
