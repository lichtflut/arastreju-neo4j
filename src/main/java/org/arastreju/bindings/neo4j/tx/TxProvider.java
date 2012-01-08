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
import org.arastreju.sge.persistence.TxAction;
import org.arastreju.sge.persistence.TxResultAction;
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
public class TxProvider {
	
	private final GraphDatabaseService gdbService;
	
	private ArasNeoTransaction tx;
	
	// -----------------------------------------------------
	
	/**
	 * Constructor.
	 * @param gdbService The service for this TX Control.
	 */
	public TxProvider(final GraphDatabaseService gdbService) {
		this.gdbService = gdbService;
	}
	
	// -----------------------------------------------------
	
	/**
	 * Begin a new transaction if not already one open.
	 * @return An active transaction.
	 */
	public TransactionControl begin() {
		if (!inTransaction()) {
			tx = new ArasNeoTransaction(gdbService.beginTx());
			return tx;
		} else {
			return new ArasNeoSubTransaction(tx);
		}
	}
	
	/**
	 * Check if there is a transaction running.
	 * @return true if there is a transaction.
	 */
	public boolean inTransaction() {
		return tx != null && tx.isActive();
	}
	
	// -----------------------------------------------------
	
	public void doTransacted(final TxAction action){
		final TransactionControl tx = begin();
		try {
			action.execute();
			tx.success();
		} catch (RuntimeException e) {
			e.printStackTrace();
			tx.fail();
			throw e;
		} finally {
			tx.finish();
		}
	}
	
	public <T> T doTransacted(final TxResultAction<T> action){
		final TransactionControl tx = begin();
		try {
			T result = action.execute();
			tx.success();
			return result;
		} catch (RuntimeException e) {
			e.printStackTrace();
			tx.fail();
			throw e;
		} finally {
			tx.finish();
		}
	}
	
}
