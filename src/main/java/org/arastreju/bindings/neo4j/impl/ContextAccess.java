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
import org.arastreju.sge.model.AbstractStatement;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.views.SNContext;
import org.neo4j.graphdb.Relationship;

import scala.actors.threadpool.Arrays;

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
public class ContextAccess implements NeoConstants {
	
	private final NeoResourceResolver resolver;
	
	// -----------------------------------------------------
	
	/**
	 * Constructor.
	 * @param resolver The resource resolver.
	 */
	public ContextAccess(final NeoResourceResolver resolver) {
		this.resolver = resolver;
	}
	
	// -----------------------------------------------------
	
	public Context[] getContextInfo(final Relationship rel) {
		if (!rel.hasProperty(CONTEXT_URI)) {
			return AbstractStatement.NO_CTX;
		} 
		final String[] ctxUris = (String[]) rel.getProperty(CONTEXT_URI);
		final Context[] ctxs = new Context[ctxUris.length];
		for (int i = 0; i < ctxUris.length; i++) {
			final String uri = ctxUris[i];
			final ResourceNode node = resolver.findResource(SNOPS.qualify(uri));
			if (node instanceof Context){
				ctxs[i] = (Context) node;
			} else if (node != null) {
				ctxs[i] = new SNContext(node);
			} else {
				throw new IllegalStateException("Could not find context(s): " + Arrays.toString(ctxUris));
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
			String[] resolved = new String[contexts.length];
			for (int i = 0; i < contexts.length; i++) {
				final ResourceID ctx = resolver.resolve(contexts[i]);
				resolved[i] = ctx.getQualifiedName().toURI();
			}
			relationship.setProperty(CONTEXT_URI, resolved);
		} 
	}

}
