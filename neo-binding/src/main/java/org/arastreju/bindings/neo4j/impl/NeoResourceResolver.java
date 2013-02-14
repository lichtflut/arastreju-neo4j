/*
 * Copyright 2012 by lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 */
package org.arastreju.bindings.neo4j.impl;

import org.arastreju.bindings.neo4j.extensions.SNResourceNeo;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.associations.AssociationKeeper;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.ResourceResolver;
import org.arastreju.sge.spi.AssocKeeperAccess;

import java.util.Set;

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
                return persist(node);
			}
		}
	}

    // ----------------------------------------------------

    protected ResourceNode persist(final ResourceNode node) {
        // 1st: create a corresponding Neo node and attach the Resource with the current context.
        AssociationKeeper keeper = conversationContext.create(node.getQualifiedName());

        // 2nd: retain copy of current associations
        final Set<Statement> copy = node.getAssociations();
        AssocKeeperAccess.getInstance().setAssociationKeeper(node, keeper);

        // 3rd: store all associations.
        for (Statement assoc : copy) {
            keeper.addAssociation(assoc);
        }

        return node;
    }

}
