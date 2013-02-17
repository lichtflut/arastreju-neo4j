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

import de.lichtflut.infra.exceptions.NotYetImplementedException;
import org.arastreju.sge.Conversation;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.index.QNResolver;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.spi.abstracts.AbstractConversation;
import org.arastreju.sge.spi.abstracts.WorkingContext;

import java.util.Set;

/**
 * <p>
 *  Implementation of {@link org.arastreju.sge.Conversation} for Neo4j.
 * </p>
 *
 * <p>
 * 	Created Sep 2, 2010
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoConversation extends AbstractConversation implements Conversation {

    /**
     * Create a new Modelling Conversation instance using a given data store.
     */
    public NeoConversation(final WorkingContext context) {
        super(context);
    }
	
    // ----------------------------------------------------

    @Override
    public Set<Statement> findIncomingStatements(ResourceID id) {
        throw new NotYetImplementedException();
    }
	
	// ----------------------------------------------------

    @Override
    protected QNResolver getQNResolver() {
        return new QNResolver() {
            @Override
            public ResourceNode resolve(QualifiedName qn) {
                return NeoConversation.this.resolve(SNOPS.id(qn));
            }
        };
    }

}
