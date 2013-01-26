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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.arastreju.bindings.neo4j;

import org.arastreju.bindings.neo4j.impl.NeoGraphDataConnection;
import org.arastreju.bindings.neo4j.impl.TestNeoGraphDataStore;
import org.arastreju.sge.ArastrejuGate;
import org.arastreju.sge.ArastrejuProfile;
import org.arastreju.sge.context.DomainIdentifier;
import org.arastreju.sge.spi.ArastrejuGateFactory;
import org.arastreju.sge.spi.GateInitializationException;

/**
 * <p>
 * Neo4j specific Gate Factory for testing purposes.
 * </p>
 * 
 * <p>
 * Created Jan 4, 2011
 * </p>
 * 
 * @author Oliver Tigges
 */
public class Neo4jTestGateFactory extends ArastrejuGateFactory {

    private NeoGraphDataConnection conn;

    public Neo4jTestGateFactory(ArastrejuProfile profile) {
        super(profile);
        try {
            conn = new NeoGraphDataConnection(new TestNeoGraphDataStore());
        }
        catch (Exception any) {
            throw new RuntimeException(any);
        }
    }

    @Override
    public ArastrejuGate create(DomainIdentifier identifier) throws GateInitializationException {
        final Neo4jGate gate = new Neo4jGate(identifier, conn);
        getProfile().onOpen(gate);
        return gate;
    }

}
