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
package org.arastreju.bindings.neo4j.tx;

import org.arastreju.sge.spi.tx.AbstractTxProvider;
import org.arastreju.sge.spi.tx.BoundTransactionControl;
import org.arastreju.sge.spi.tx.TxListener;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
public class NeoTxProvider extends AbstractTxProvider {
	
	private final GraphDatabaseService gdbService;

    private List<TxListener> listeners = new ArrayList<TxListener>();

    // -----------------------------------------------------
	
	/**
	 * Constructor.
     * @param gdbService The service for this TX Control.
     */
	public NeoTxProvider(GraphDatabaseService gdbService) {
		this.gdbService = gdbService;
    }

    // ----------------------------------------------------

    public NeoTxProvider register(TxListener... listeners) {
        Collections.addAll(this.listeners, listeners);
        return this;
    }
	
	// -----------------------------------------------------

    @Override
    protected BoundTransactionControl newTx() {
        return new NeoTransaction(gdbService.beginTx())
                .register(listeners());
    }

    private TxListener[] listeners() {
        return this.listeners.toArray(new TxListener[listeners.size()]);

    }

}
