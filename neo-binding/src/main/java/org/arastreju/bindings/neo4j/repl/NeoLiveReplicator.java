package org.arastreju.bindings.neo4j.repl;

import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.repl.ArasLiveReplicator;

public class NeoLiveReplicator extends ArasLiveReplicator {
	@Override
	protected void onNodeOp(boolean added, ResourceID node) {
	}

	@Override
	protected void onRelOp(boolean added, Statement stmt) {
	}

}
