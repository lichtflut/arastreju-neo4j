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
package org.arastreju.bindings.neo4j.it;

import org.arastreju.bindings.neo4j.storage.NeoGraphDataStore;
import org.arastreju.sge.index.IndexProvider;
import org.arastreju.sge.spi.AbstractConversationTest;
import org.arastreju.sge.spi.impl.ConversationImpl;
import org.arastreju.sge.spi.impl.GraphDataConnectionImpl;
import org.arastreju.sge.spi.impl.WorkingContextImpl;
import org.junit.Before;

import static org.arastreju.sge.SNOPS.objects;
import static org.arastreju.sge.SNOPS.remove;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * <p>
 *  Black box tests against conversation using Neo4J backend.
 * </p>
 *
 * <p>
 * 	Created Sep 9, 2010
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoConversationTest extends AbstractConversationTest {

	@Override
    @Before
	public void setUp() throws Exception {
        NeoGraphDataStore neoStore = new NeoGraphDataStore();
        store = neoStore;
		connection = new GraphDataConnectionImpl(store, new IndexProvider(neoStore.getStorageDir()));
		conversation = new ConversationImpl(new WorkingContextImpl(connection));
	}

}
