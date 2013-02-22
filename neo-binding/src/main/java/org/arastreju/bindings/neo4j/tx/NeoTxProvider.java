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

import org.arastreju.sge.persistence.SubTransaction;
import org.arastreju.sge.persistence.TransactionControl;
import org.arastreju.sge.persistence.TxProvider;
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
	
	private final GraphDatabaseService gdbService;
	
	// -----------------------------------------------------
	
	/**
	 * Constructor.
	 * @param gdbService The service for this TX Control.
	 */
	public NeoTxProvider(final GraphDatabaseService gdbService) {
		this.gdbService = gdbService;
	}
	
	// -----------------------------------------------------

    @Override
    protected TransactionControl newTx() {
        return new NeoTransaction(gdbService.beginTx());
    }

    @Override
    protected TransactionControl newSubTx(TransactionControl tx) {
        return new SubTransaction(tx);
    }
}
