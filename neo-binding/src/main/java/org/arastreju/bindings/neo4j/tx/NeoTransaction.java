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

import org.arastreju.sge.spi.tx.AbstractTransactionControl;
import org.neo4j.graphdb.Transaction;

/**
 * <p>
 *  Direct wrapper of a Neo4j Transaction.
 * </p>
 *
 * <p>
 * 	Created Jun 7, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
class NeoTransaction extends AbstractTransactionControl {

	private Transaction tx;

	// -----------------------------------------------------

	/**
	 * Constructor.
	 * @param tx The transaction.
	 */
	public NeoTransaction(final Transaction tx) {
		this.tx = tx;
	}
	
	// -----------------------------------------------------
	
	/**
	 * @return true if the transaction is active.
	 */
    @Override
	public boolean isActive() {
		return tx != null;
	}
	
	// -----------------------------------------------------
	
	@Override
	public void success() {
		assertTxActive();
		tx.success();
	}

    @Override
	public void fail() {
		assertTxActive();
		tx.failure();
	}

    @Override
	public void finish() {
		assertTxActive();
		tx.finish();
		tx = null;
	}
	
	// ----------------------------------------------------

    @Override
	public void flush() {
	}

}
