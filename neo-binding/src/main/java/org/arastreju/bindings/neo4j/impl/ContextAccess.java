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

import org.arastreju.bindings.neo4j.NeoConstants;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.context.Context;
import org.arastreju.sge.context.SimpleContextID;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.spi.GraphDataStore;
import org.neo4j.graphdb.Relationship;

/**
 * <p>
 *  Accessor for context information of relationships.
 * </p>
 *
 * <p>
 * 	Created Jun 16, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class ContextAccess {
	
	public static final Context[] NO_CTX = new Context[0];
	
    private GraphDataStore store;

	// -----------------------------------------------------
	
	/**
	 * Constructor.
	 * @param store The underlying store.
	 */
	public ContextAccess(GraphDataStore store) {
		this.store = store;
	}
	
	// -----------------------------------------------------
	
	public Context[] getContextInfo(final Relationship rel) {
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
                ctxs[i] = new SimpleContextID(qn);
            }
		}
		return ctxs;
	}
	
	/**
	 * Assigns context information to a relationship.
	 * @param relationship The relationship to be assigned to the contexts.
	 * @param contexts The contexts.
	 */
	public void assignContext(final Relationship relationship, final Context[] contexts) {
		if (contexts != null && contexts.length > 0) {
			String[] uris = new String[contexts.length];
			for (int i = 0; i < contexts.length; i++) {
                assureExists(contexts[i].getQualifiedName());
				uris[i] = contexts[i].toURI();
			}
			relationship.setProperty(NeoConstants.CONTEXT_URI, uris);
		} 
	}

    // ----------------------------------------------------

    private boolean exists(QualifiedName qn) {
        return store.find(qn) != null;
    }

    private void assureExists(QualifiedName qn) {
        if (!exists(qn)) {
            store.create(qn);
        }
    }

}
