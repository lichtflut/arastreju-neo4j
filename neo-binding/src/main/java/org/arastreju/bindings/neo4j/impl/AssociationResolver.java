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
package org.arastreju.bindings.neo4j.impl;

import org.arastreju.bindings.neo4j.ArasRelTypes;
import org.arastreju.bindings.neo4j.NeoConstants;
import org.arastreju.bindings.neo4j.extensions.SNValueNeo;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.model.DetachedStatement;
import org.arastreju.sge.model.SimpleResourceID;
import org.arastreju.sge.model.StatementMetaInfo;
import org.arastreju.sge.model.associations.AttachedAssociationKeeper;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.SemanticNode;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.ResourceResolver;
import org.arastreju.sge.spi.AttachedResourceNode;
import org.arastreju.sge.spi.uow.ResourceResolverImpl;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;

import static org.arastreju.sge.SNOPS.id;

/**
 * <p>
 *  Handler for resolving of a node's association.
 * </p>
 *
 * <p>
 * 	Created Dec 1, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class AssociationResolver implements NeoConstants {

	private static final Logger LOGGER = LoggerFactory.getLogger(AssociationResolver.class);

    // ----------------------------------------------------

    private final NeoConversationContext convContext;

	private final ResourceResolver resourceResolver;

	private final ContextAccess ctxAccess;

    private final NeoGraphDataStore store;

    // ----------------------------------------------------

	/**
	 * Creates a new association handler.
	 * @param conversationContext The current working context.
     * @param store The physical store.
	 */
	public AssociationResolver(NeoConversationContext conversationContext, NeoGraphDataStore store) {
        this.convContext = conversationContext;
		this.resourceResolver = new ResourceResolverImpl(conversationContext);
        this.store = store;
		this.ctxAccess = new ContextAccess(store);
	}

	// ----------------------------------------------------

	/**
	 * Resolve the associations of given association keeper.
	 * @param keeper The association keeper to be resolved.
	 */
	public void resolveAssociations(AttachedAssociationKeeper keeper) {
        final Node neoNode = store.getNeoNode(keeper.getQualifiedName());
        for(Relationship rel : neoNode.getRelationships(Direction.OUTGOING)){
			final Context[] ctx = ctxAccess.getContextInfo(rel);
			if (!regardContext(ctx, rel)) {
				continue;
			}
			SemanticNode object = null;
			if (rel.isType(ArasRelTypes.REFERENCE)){
				object = resolve(rel.getEndNode());
			} else if (rel.isType(ArasRelTypes.VALUE)){
				object = new SNValueNeo(rel.getEndNode());
			}
			final ResourceNode predicate = resourceResolver.resolve(new SimpleResourceID(rel.getProperty(PREDICATE_URI).toString()));
			final StatementMetaInfo mi = new StatementMetaInfo(ctx, new Date((Long)rel.getProperty(TIMESTAMP, 0L)));
			keeper.addAssociationDirectly(new DetachedStatement(id(keeper.getQualifiedName()), predicate, object, mi));
		}
	}

	// ----------------------------------------------------
	
	private boolean regardContext(Context[] stmtContexts, Relationship rel) {
		if (stmtContexts.length == 0) {
			LOGGER.debug("Statement has no context.");
			return true;
		}
		Context[] readContexts = convContext.getReadContexts();
        for (Context readContext : readContexts) {
            for (Context stmtContext : stmtContexts) {
                if (readContext.equals(stmtContext)) {
                    return true;
                }
            }
        }
        if (LOGGER.isDebugEnabled()) {
            final StringBuilder sb = new StringBuilder("Contexts of Statement ");
            sb.append(resolve(rel.getStartNode()));
            sb.append(" --> ");
            sb.append(rel.getProperty(PREDICATE_URI));
            sb.append(" --> ");
            sb.append(convert(rel, rel.getEndNode()));
            sb.append(" {} ");
            sb.append("not in read contexts");
            sb.append(" {}.");
            LOGGER.debug(sb.toString(), Arrays.toString(stmtContexts), Arrays.toString(readContexts));
        }
		return false;
	}

    private SemanticNode convert(Relationship rel, Node node) {
        if (rel.isType(ArasRelTypes.REFERENCE)){
            return resolve(node);
        } else if (rel.isType(ArasRelTypes.VALUE)){
            return new SNValueNeo(node);
        } else {
            return null;
        }
    }

    private ResourceNode resolve(final Node neoNode) {
        final Object uriProperty = neoNode.getProperty(NeoConstants.PROPERTY_URI, null);
        if (uriProperty == null) {
            return null;
        }
        final QualifiedName qn = QualifiedName.create(uriProperty.toString());
        AttachedAssociationKeeper keeper = convContext.lookup(qn);
        if (keeper == null){
            keeper = new AttachedAssociationKeeper(qn, new NeoPhysicalNodeID(neoNode));
            convContext.attach(qn, keeper);
        }
        return new AttachedResourceNode(qn, keeper);
    }

}
