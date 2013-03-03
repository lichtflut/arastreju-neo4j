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
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.apriori.Aras;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.SNResource;
import org.arastreju.sge.model.nodes.views.SNText;
import org.arastreju.sge.persistence.TransactionControl;
import org.arastreju.sge.query.Query;
import org.arastreju.sge.query.QueryResult;
import org.arastreju.sge.spi.GraphDataStore;
import org.arastreju.tck.AbstractConversationTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

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
    protected GraphDataStore createStore() throws IOException {
        return new NeoGraphDataStore();
    }

    @Test
    public void testNonCommitIndexing() throws IOException {
        final ResourceNode car = new SNResource(qnCar);
        SNOPS.associate(car, Aras.HAS_PROPER_NAME, new SNText("BMW"));

        TransactionControl ctl = conversation.beginTransaction();
        conversation.attach(car);

        /* no commit here */

        Query query = conversation.createQuery().addField(Aras.HAS_PROPER_NAME.toURI(), "BMW");
        QueryResult result = query.getResult();
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(qnCar, result.getSingleNode().getQualifiedName());

//        conversation.remove(car);
//
//        query = conversation.createQuery().addField(Aras.HAS_PROPER_NAME.toURI(), "BMW");
//        result = query.getResult();
//        Assert.assertNotNull(result);
//        Assert.assertTrue(result.isEmpty());

        /* commit just for completeness */
        ctl.finish();

    }
}
