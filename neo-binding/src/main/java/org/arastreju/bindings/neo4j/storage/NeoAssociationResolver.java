/*
 * Copyright (C) 2013 lichtflut Forschungs- und Entwicklungsgesellschaft mbH
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
package org.arastreju.bindings.neo4j.storage;

import org.arastreju.sge.SNOPS;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.context.ContextID;
import org.arastreju.sge.model.Assertor;
import org.arastreju.sge.model.ElementaryDataType;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.associations.AttachedAssociationKeeper;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.SNValue;
import org.arastreju.sge.model.nodes.SemanticNode;
import org.arastreju.sge.model.nodes.ValueNode;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.spi.AttachedResourceNode;
import org.arastreju.sge.spi.ConversationController;
import org.arastreju.sge.spi.impl.AbstractAssociationResolver;
import org.arastreju.sge.spi.impl.NumericPhysicalNodeID;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Locale;

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
public class NeoAssociationResolver extends AbstractAssociationResolver implements NeoConstants {

	private static final Logger LOGGER = LoggerFactory.getLogger(NeoAssociationResolver.class);

    // ----------------------------------------------------

    private final NeoGraphDataStore store;

    // ----------------------------------------------------

	/**
	 * Creates a new association handler.
	 * @param controller The conversation controller.
     * @param store The physical store.
	 */
	public NeoAssociationResolver(ConversationController controller, NeoGraphDataStore store) {
        super(controller);
        this.store = store;
	}

	// ----------------------------------------------------

	/**
	 * Resolve the associations of given association keeper.
	 * @param keeper The association keeper to be resolved.
	 */
	@Override
    public void resolveAssociations(AttachedAssociationKeeper keeper) {
        final Node neoNode = store.getNeoNode(keeper.getQualifiedName());
        if (neoNode == null) {
            LOGGER.warn("Found no neo node in data store for attached node {}", keeper.getQualifiedName());
            return;
        }
        for(Relationship rel : neoNode.getRelationships(Direction.OUTGOING)){
			final Context[] stmtContexts = getContextInfo(rel);
			if (!regardContext(stmtContexts, rel)) {
				continue;
			}
			SemanticNode object = null;
			if (rel.isType(ArasRelationshipType.REFERENCE)){
				object = resolve(rel.getEndNode());
			} else if (rel.isType(ArasRelationshipType.VALUE)){
				object = toValueNode(rel.getEndNode());
			}

            final ResourceNode predicate = resolve(rel.getProperty(PREDICATE_URI).toString());
            Statement stmt = Assertor.start(id(keeper.getQualifiedName()), predicate, object)
                .context(stmtContexts)
                .timestamp((Long) rel.getProperty(TIMESTAMP, 0L))
                .validFrom((Long) rel.getProperty(VALID_FROM, 0L))
                .validUntil((Long) rel.getProperty(VALID_UNTIL, 0L))
                .build();
			keeper.addAssociationDirectly(stmt);
		}
	}

    // ----------------------------------------------------

    private Context[] getContextInfo(final Relationship rel) {
        if (!rel.hasProperty(NeoConstants.CONTEXT_URI)) {
            return NO_CTX;
        }
        final String[] ctxUris = (String[]) rel.getProperty(NeoConstants.CONTEXT_URI);
        final Context[] ctxs = new Context[ctxUris.length];
        for (int i = 0; i < ctxUris.length; i++) {
            final String uri = ctxUris[i];
            final QualifiedName qn = SNOPS.qualify(uri);
            if (!exists(qn)) {
                throw new IllegalStateException("Could not find context: " + qn);
            } else {
                ctxs[i] = ContextID.forContext(qn);
            }
        }
        return ctxs;
    }
	
	private boolean regardContext(Context[] stmtContexts, Relationship rel) {
        boolean match = super.regardContext(stmtContexts);
        if (LOGGER.isDebugEnabled()) {
            logRelContexts(stmtContexts, readContexts(), rel, match);
        }
        return match;
    }

    private SemanticNode convert(Relationship rel, Node node) {
        if (rel.isType(ArasRelationshipType.REFERENCE)){
            return resolve(node);
        } else if (rel.isType(ArasRelationshipType.VALUE)){
            return toValueNode(node);
        } else {
            return null;
        }
    }

    private ResourceNode resolve(final Node neoNode) {
        final Object uriProperty = neoNode.getProperty(NeoConstants.PROPERTY_URI, null);
        if (uriProperty == null) {
            return null;
        }
        final QualifiedName qn = QualifiedName.from(uriProperty.toString());
        AttachedAssociationKeeper keeper = controller().lookup(qn);
        if (keeper == null){
            keeper = new AttachedAssociationKeeper(qn, new NumericPhysicalNodeID(neoNode.getId()));
            controller().attach(qn, keeper);
        }
        return new AttachedResourceNode(qn, keeper);
    }

    private boolean exists(QualifiedName qn) {
        return store.find(qn) != null;
    }

    private ValueNode toValueNode(Node neoNode) {
        return new SNValue(getDatatype(neoNode), neoNode.getProperty(PROPERTY_VALUE), getLocale(neoNode));
    }

    /**
     * @param neoNode The Neo Node.
     * @return The corresponding datatype.
     */
    private ElementaryDataType getDatatype(final Node neoNode) {
        final String datatypeName = (String) neoNode.getProperty(PROPERTY_DATATYPE);
        return ElementaryDataType.valueOf(datatypeName);
    }

    /**
     * @param neoNode The Neo Node.
     * @return The corresponding datatype.
     */
    private Locale getLocale(final Node neoNode) {
        if (!neoNode.hasProperty(PROPERTY_LOCALE)) {
            return null;
        }
        final String localeName = (String) neoNode.getProperty(PROPERTY_LOCALE);
        String language = localeName.substring(0, 2);
        if (localeName.length() >= 5 ) {
            String country = localeName.substring(3, 5);
            return new Locale(language, country);
        } else {
            return new Locale(language);
        }
    }

    private void logRelContexts(Context[] stmtContexts, Context[] readContexts, Relationship rel, boolean match) {
        final StringBuilder sb = new StringBuilder("Contexts of Statement ");
        sb.append(resolve(rel.getStartNode()));
        sb.append(" --> ");
        sb.append(rel.getProperty(PREDICATE_URI));
        sb.append(" --> ");
        sb.append(convert(rel, rel.getEndNode()));
        sb.append(" {} ");
        if (match) {
            sb.append("are in read contexts");
        } else {
            sb.append("are NOT in read contexts");
        }
        sb.append(" {}.");
        LOGGER.debug(sb.toString(), Arrays.toString(stmtContexts), Arrays.toString(readContexts));
    }

}
