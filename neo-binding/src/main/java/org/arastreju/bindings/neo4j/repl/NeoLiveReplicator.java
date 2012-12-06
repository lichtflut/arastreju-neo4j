package org.arastreju.bindings.neo4j.repl;

import org.arastreju.bindings.neo4j.ArasRelTypes;
import org.arastreju.bindings.neo4j.NeoConstants;
import org.arastreju.bindings.neo4j.index.NeoIndex;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.nodes.ValueNode;
import org.arastreju.sge.repl.ArasLiveReplicator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *  Neo4j-specific part of the replication mechanism.
 *  See doc of parent for the general idea.
 *  
 *  TODO: this probably doesn't work yet. 
 * </p>
 *
 * Created: 30.11.2012
 *
 * @author Timo Buhrmester
 */
public class NeoLiveReplicator extends ArasLiveReplicator implements NeoConstants {
	private final Logger logger = LoggerFactory.getLogger(NeoLiveReplicator.class);

	private final GraphDatabaseService gdbService;
	
	private int CurrentTxSeq = -1;
	private Transaction CurTx = null;

	// ----------------------------------------------------
	
	public NeoLiveReplicator(GraphDatabaseService gdbService) {
		super();
		this.gdbService = gdbService;
		logger.debug("gdbservice is "+gdbService);
	}

	
	// ----------------------------------------------------

	
	@Override
	protected void onNodeOp(int txSeq, boolean added, ResourceID node) {
		logger.debug("onNodeOp(added=" + added + ", node=" + node);
		ensureTx(txSeq);
		if (added) {
			final Node neoNode = gdbService.createNode();
			neoNode.setProperty(PROPERTY_URI, node.getQualifiedName().toURI());
			gdbService.index().forNodes("resources").add(neoNode, NeoIndex.INDEX_KEY_RESOURCE_URI, node.toURI().trim().toLowerCase());
			// XXX: context-aware indexing is missing still
		} else {
			/* XXX */
			Node neoNode = gdbService.index().forNodes("resources")
					.get(NeoIndex.INDEX_KEY_RESOURCE_URI, node.toURI().trim().toLowerCase())
					.getSingle();
			gdbService.index().forNodes("resources").remove(neoNode);
			
			neoNode.delete();
		}
	}

	@Override
	protected void onRelOp(int txSeq, boolean added, Statement stmt) {
		logger.debug("onRelOp(added="+added+", stmt="+stmt);
		ensureTx(txSeq);
		boolean valObj = stmt.getObject().isValueNode();
		RelationshipType type = valObj ? ArasRelTypes.VALUE : ArasRelTypes.REFERENCE;

		Node sub = gdbService.index().forNodes("resources")
				.get(NeoIndex.INDEX_KEY_RESOURCE_URI, stmt.getSubject()
						.toURI().trim().toLowerCase()).getSingle();

		Node obj;

		if (added) {
			if (valObj) {
				obj = gdbService.createNode();
				final ValueNode value = stmt.getObject().asValue();
				obj.setProperty(PROPERTY_DATATYPE, value.getDataType().name());
				obj.setProperty(PROPERTY_VALUE, value.getStringValue());
				//				addLocale(obj, value.getLocale()); XXX

			} else {
				obj = gdbService.index().forNodes("resources")
						.get(NeoIndex.INDEX_KEY_RESOURCE_URI, stmt.getObject()
								.asResource().toURI().trim().toLowerCase()).getSingle();
			}

			Relationship rel = sub.createRelationshipTo(obj, type);
			rel.setProperty(PREDICATE_URI, stmt.getPredicate().toURI());
			//XXX timestamp, context
		} else {
			Relationship rel = findCorresponding(sub, stmt);

			if (rel != null) {
				rel.delete();
			} else {
				logger.warn("failed to find a relation for statement " + stmt);
			}
		}
	}

	@Override
	protected void onEndOfTx(int txSeq) {
		if (CurrentTxSeq != txSeq) {
			logger.warn("spurious EOT (expected: "+CurrentTxSeq+", got: "+txSeq+") - ignoring");
			return;
		}
		
		if (CurTx != null) {
			CurTx.success();
			CurTx.finish();
			CurTx = null;
		} else {
			logger.warn("no current transaction ("+txSeq+")");
		}
	}

	
	// ----------------------------------------------------
	
	private void ensureTx(int txSeq) {
		if (txSeq > CurrentTxSeq) {
			if (CurTx != null) {
				logger.warn("transaction "+txSeq+" not finished!");
			}
			CurTx = gdbService.beginTx();
			CurrentTxSeq = txSeq;
		}
	}
	
	// ----------------------------------------------------

	
	/* copied from AssociationHandler */
	private Relationship findCorresponding(final Node neoNode, final Statement stmt) {
		final String assocPredicate = stmt.getPredicate().getQualifiedName().toURI();
		final String assocValue = SNOPS.string(stmt.getObject());
		for (Relationship rel : neoNode.getRelationships(Direction.OUTGOING)) {
			final String predicate = (String) rel.getProperty(PREDICATE_URI);
			if (assocPredicate.equals(predicate)) {
				if (stmt.getObject().isResourceNode()) {
					final String uri = (String) rel.getEndNode().getProperty(PROPERTY_URI);
					if (assocValue.equals(uri)) {
						return rel;
					}
				} else {
					final String value = (String) rel.getEndNode().getProperty(PROPERTY_VALUE);
					if (assocValue.equals(value)) {
						return rel;
					}
				}
			}
		}
		return null;
	}
}
