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

import de.lichtflut.infra.exceptions.NotYetImplementedException;
import org.arastreju.bindings.neo4j.NeoConstants;
import org.arastreju.bindings.neo4j.extensions.NeoAssociationKeeper;
import org.arastreju.bindings.neo4j.index.NeoIndex;
import org.arastreju.bindings.neo4j.index.NeoNodeKeyTable;
import org.arastreju.sge.ArastrejuProfile;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.model.SimpleResourceID;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.NodeKeyTable;
import org.arastreju.sge.spi.GraphDataStore;
import org.arastreju.sge.spi.PhysicalNodeID;
import org.arastreju.sge.spi.ProfileCloseListener;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

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
public class NeoGraphDataStore implements GraphDataStore<NeoAssociationKeeper>, ProfileCloseListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(NeoGraphDataStore.class);

    private final NodeKeyTable keyTable;
	
	private final GraphDatabaseService gdbService;
	
	private final IndexManager indexManager;
	
	// -----------------------------------------------------

	/**
	 * Default constructor. Will use a <b>temporary</b> datastore!.
	 */
	public NeoGraphDataStore() throws IOException {
		this(prepareTempStore());
	}
	
	/**
	 * Constructor. Creates a store using given directory.
	 * @param dir The directory for the store.
	 */
	public NeoGraphDataStore(final String dir) {
        if (new File(dir).exists()) {
            LOGGER.info("Using existing Neo4jDataStore in {}.", dir);
        } else {
            LOGGER.info("New Neo4jDataStore created in {}.", dir);
        }
		gdbService = new EmbeddedGraphDatabase(dir); 
		indexManager = gdbService.index();
        keyTable = new NeoNodeKeyTable(indexManager);
	}
	
	// -- GraphDataStore ----------------------------------

    @Override
    public NeoAssociationKeeper find(QualifiedName qn) {
        Index<Node> index = indexManager.forNodes(NeoIndex.INDEX_RESOURCES);
        Node found = index.get(NeoIndex.INDEX_KEY_RESOURCE_URI, NeoIndex.normalize(qn.toURI())).getSingle();
        if (found != null) {
            return new NeoAssociationKeeper(new SimpleResourceID(qn), found);
        } else {
            return null;
        }
    }

    @Override
    public NeoAssociationKeeper create(QualifiedName qn) {
        Node node = gdbService.createNode();
        node.setProperty(NeoConstants.PROPERTY_URI, qn.toURI());
        return new NeoAssociationKeeper(SNOPS.id(qn), node);
    }

    @Override
    public void remove(QualifiedName qn) {
        keyTable.lookup(qn);
    }

    @Override
    public boolean addAssociation(PhysicalNodeID id, Statement assoc) {
        throw new NotYetImplementedException();
    }

    // -- Neo Services ------------------------------------

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

    @Override
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
