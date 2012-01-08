/*
 * Copyright (C) 2010 lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
class ArasNeoTransaction implements TransactionControl {

	private Transaction tx;
	
	// -----------------------------------------------------

	/**
	 * Constructor.
	 * @param tx The transaction.
	 */
	public ArasNeoTransaction(final Transaction tx) {
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
	public void success() {
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
	public void finish() {
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
