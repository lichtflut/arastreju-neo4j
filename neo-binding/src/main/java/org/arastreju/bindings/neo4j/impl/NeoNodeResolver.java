/*
 * Copyright 2012 by lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 */
package org.arastreju.bindings.neo4j.impl;

import org.arastreju.bindings.neo4j.NeoConstants;
import org.arastreju.bindings.neo4j.extensions.NeoAssociationKeeper;
import org.arastreju.bindings.neo4j.extensions.SNResourceNeo;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.naming.QualifiedName;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *  Resolves the ResourceNode for a Neo node.
 * </p>
 *
 * <p>
 * 	Created Feb 14, 2012
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoNodeResolver {

	private final NeoConversationContext conversationContext;

	// ----------------------------------------------------

	/**
	 * Constructor.
	 * @param conversationContext The conversation context.
	 */
	public NeoNodeResolver(NeoConversationContext conversationContext) {
		this.conversationContext = conversationContext;
	}
	
	// ----------------------------------------------------

	public ResourceNode resolve(final Node neoNode) {
        final Object uriProperty = neoNode.getProperty(NeoConstants.PROPERTY_URI, null);
        if (uriProperty == null) {
            return null;
        }
        final QualifiedName qn = QualifiedName.create(uriProperty.toString());
		NeoAssociationKeeper keeper = conversationContext.lookup(qn);
		if (keeper == null){
            keeper = new NeoAssociationKeeper(SNOPS.id(qn), neoNode);
            conversationContext.attach(qn, keeper);
		}
		return new SNResourceNeo(qn, keeper);	
	}

}
