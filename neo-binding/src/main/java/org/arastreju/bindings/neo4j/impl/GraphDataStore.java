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
package org.arastreju.bindings.neo4j.impl;

import java.io.File;
import java.io.IOException;

import org.arastreju.sge.ArastrejuProfile;
import org.arastreju.sge.spi.ProfileCloseListener;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *  Container for services, indexes and registries. 
 * </p>
 *
 * <p>
 * 	Created Jun 6, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class GraphDataStore implements ProfileCloseListener {
	
	private final Logger logger = LoggerFactory.getLogger(GraphDataStore.class);
	
	private final GraphDatabaseService gdbService;
	
	private final IndexManager indexManager;
	
	// -----------------------------------------------------

	/**
	 * Default constructor. Will use a <b>temporary</b> datastore!.
	 */
	public GraphDataStore() throws IOException {
		this(prepareTempStore());
	}
	
	/**
	 * Constructor. Creates a store using given directory.
	 * @param dir The directory for the store.
	 */
	public GraphDataStore(final String dir) {
        if (new File(dir).exists()) {
            logger.info("Using existing Neo4jDataStore in {}.", dir);
        } else {
            logger.info("New Neo4jDataStore created in {}.", dir);
        }
		gdbService = new EmbeddedGraphDatabase(dir); 
		indexManager = gdbService.index();
	}
	
	// -----------------------------------------------------

	/**
	 * @return the gdbService
	 */
	public GraphDatabaseService getGdbService() {
		return gdbService;
	}
	
	/**
	 * @return the indexManager
	 */
	public IndexManager getIndexManager() {
		return indexManager;
	}
	
	// -- ProfileCloseListener ----------------------------
	
	/**
	 * {@inheritDoc}
	 */
	public void onClosed(final ArastrejuProfile profile) {
		close();
	}
	
	public void close() {
		gdbService.shutdown();
	}
	
	// -----------------------------------------------------
	
	public static String prepareTempStore(String domain) throws IOException {
		final File temp = File.createTempFile(domain, Long.toString(System.nanoTime()));
		if (!temp.delete()) {
			throw new IOException("Could not delete temp file: "
					+ temp.getAbsolutePath());
		}
		if (!temp.mkdir()) {
			throw new IOException("Could not create temp directory: "
					+ temp.getAbsolutePath());
		}
		
		return temp.getAbsolutePath();
	}
	
	private static String prepareTempStore() throws IOException {
		return prepareTempStore("default");
	}

	
}
