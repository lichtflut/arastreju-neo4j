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

import org.arastreju.sge.persistence.TransactionControl;
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
class ArasNeoTransaction extends TransactionControl {

	private Transaction tx;
	
	// -----------------------------------------------------

	/**
	 * Constructor.
	 * @param tx The transaction.
	 */
	public ArasNeoTransaction(final Transaction tx, NeoTxProvider txProv) {
		super(txProv);
		this.tx = tx;
	}
	
	// -----------------------------------------------------
	
	/**
	 * @return true if the transaction is active.
	 */
	public boolean isActive() {
		return tx != null;
	}
	
	// -----------------------------------------------------
	
	/** 
	 * {@inheritDoc}
	 */
	public void onSuccess() {
		assertTxActive();
		tx.success();
	}

	/** 
	 * {@inheritDoc}
	 */
	public void fail() {
		assertTxActive();
		tx.failure();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void onFinish() {
		assertTxActive();
		tx.finish();
		tx = null;
	}
	
	// ----------------------------------------------------
	
	/** 
	 * {@inheritDoc}
	 */
	public void commit() {
		success();
		finish();
	}

	/** 
	 * {@inheritDoc}
	 */
	public void rollback() {
		fail();
		finish();
	}
	
	// ----------------------------------------------------

	/** 
	 * {@inheritDoc}
	 */
	public void flush() {
	}
	
	// ----------------------------------------------------
	
	protected void assertTxActive() {
		if (!isActive()) {
			throw new IllegalStateException("Transaction has already been closed.");
		}
	}
}
