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
package org.arastreju.bindings.neo4j.tx;

import org.arastreju.bindings.neo4j.repl.NeoLiveReplicator;
import org.arastreju.sge.persistence.SubTransaction;
import org.arastreju.sge.persistence.TransactionControl;
import org.arastreju.sge.persistence.TxProvider;
import org.arastreju.sge.repl.ArasLiveReplicator;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * <p>
 *  Transaction provider based on neo4j {@link GraphDatabaseService} transactions.
 * </p>
 *
 * <p>
 * 	Created Jun 6, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoTxProvider extends TxProvider {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NeoTxProvider.class);
	
	private GraphDatabaseService gdbService;
	
	// -----------------------------------------------------
	
	public void init(GraphDatabaseService gdbService) {
		this.gdbService = gdbService;
	}
	
	// -----------------------------------------------------

    @Override
    protected TransactionControl newTx() {
	if (gdbService == null) {
		logger.warn("not initialized");
		return null;
	}
        return new ArasNeoTransaction(gdbService.beginTx(), this);
    }

    @Override
    protected TransactionControl newSubTx(TransactionControl tx) {
        return new SubTransaction(tx);
    }

	@Override
	protected ArasLiveReplicator createReplicator() {
		return new NeoLiveReplicator(gdbService);
	}
}
