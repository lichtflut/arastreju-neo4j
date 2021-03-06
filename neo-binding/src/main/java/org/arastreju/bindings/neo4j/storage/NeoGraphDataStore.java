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
package org.arastreju.bindings.neo4j.storage;

import org.arastreju.bindings.neo4j.tx.NeoTxProvider;
import org.arastreju.sge.ArastrejuProfile;
import org.arastreju.sge.index.IndexProvider;
import org.arastreju.sge.model.associations.AttachedAssociationKeeper;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.persistence.NodeKeyTable;
import org.arastreju.sge.spi.AssociationResolver;
import org.arastreju.sge.spi.AssociationWriter;
import org.arastreju.sge.spi.GraphDataStore;
import org.arastreju.sge.spi.ProfileCloseListener;
import org.arastreju.sge.spi.WorkingContext;
import org.arastreju.sge.spi.impl.LuceneBasedNodeKeyTable;
import org.arastreju.sge.spi.impl.NumericPhysicalNodeID;
import org.arastreju.sge.spi.tx.TxProvider;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
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
public class NeoGraphDataStore implements GraphDataStore, ProfileCloseListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(NeoGraphDataStore.class);

	private final GraphDatabaseService gdbService;

    private final IndexProvider indexProvider;

    private final NodeKeyTable<NumericPhysicalNodeID> keyTable;

    // -----------------------------------------------------

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
        indexProvider = new IndexProvider(dir);

        try {
            keyTable = LuceneBasedNodeKeyTable.forNumericIDs(dir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
	
	// -- GraphDataStore ----------------------------------

    @Override
    public AttachedAssociationKeeper find(QualifiedName qn) {
        NumericPhysicalNodeID id = keyTable.lookup(qn);

        if (id != null) {
            try {
                Node node = gdbService.getNodeById(id.asLong());
                return new AttachedAssociationKeeper(qn, new NumericPhysicalNodeID(node.getId()));
            } catch (NotFoundException e) {
                keyTable.remove(qn);
                LOGGER.warn("No more neo node found with id {} and qn {}.", id, qn);
            }
        }
        return null;
    }

    @Override
    public AttachedAssociationKeeper create(QualifiedName qn) {
        Node node = gdbService.createNode();
        node.setProperty(NeoConstants.PROPERTY_URI, qn.toURI());
        NumericPhysicalNodeID nodeID = new NumericPhysicalNodeID(node.getId());
        keyTable.put(qn, nodeID);
        return new AttachedAssociationKeeper(qn, nodeID);
    }

    @Override
    public void remove(QualifiedName qn) {
        NumericPhysicalNodeID existing = keyTable.lookup(qn);
        if (existing != null) {
            Node node = gdbService.getNodeById(existing.asLong());
            for (Relationship rel : node.getRelationships()) {
                rel.delete();
            }
            node.delete();
            keyTable.remove(qn);
        }
    }

    // ----------------------------------------------------

    @Override
    public AssociationResolver createAssociationResolver(WorkingContext ctx) {
        return new NeoAssociationResolver(ctx, this);
    }

    @Override
    public AssociationWriter crateAssociationWriter(WorkingContext ctx) {
        return new NeoAssociationWriter(ctx, this);
    }

    @Override
    public TxProvider createTxProvider(WorkingContext ctx) {
        return new NeoTxProvider(gdbService);
    }

    @Override
    public IndexProvider getIndexProvider() {
        return indexProvider;
    }

    @Override
    public void close() {
        gdbService.shutdown();
        try {
            indexProvider.shutdown();
            keyTable.shutdown();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // -- ProfileCloseListener ----------------------------

    @Override
    public void onClosed(final ArastrejuProfile profile) {
        close();
    }

    // -- Neo Specifics -----------------------------------

    public Node getNeoNode(QualifiedName qn) {
        NumericPhysicalNodeID id = keyTable.lookup(qn);
        if (id != null) {
            return gdbService.getNodeById(id.asLong());
        } else {
            return null;
        }
    }

}
