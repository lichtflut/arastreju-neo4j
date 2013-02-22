package org.arastreju.bindings.neo4j;/*
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

import de.lichtflut.infra.logging.StopWatch;
import org.arastreju.sge.Arastreju;
import org.arastreju.sge.ArastrejuGate;
import org.arastreju.sge.Conversation;
import org.arastreju.sge.apriori.RDF;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.views.SNClass;
import org.arastreju.sge.persistence.TransactionControl;
import org.arastreju.sge.query.FieldParam;
import org.arastreju.sge.query.Query;

import java.util.List;

/**
 * <p>
 *  Test case for multi threading.
 * </p>
 *
 * <p>
 * 	Created Jun 6, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class MultiThreadingTest {
	
	public MultiThreadingTest() {}

    // ----------------------------------------------------

    public void start(int numberOfThreads) {
        for(int i = 0; i < numberOfThreads; i++) {
            final Thread t = new Thread(new Worker(Arastreju.getInstance().openMasterGate()));
            t.start();
            System.out.println("Startet Thread: " + t.getId());
        }
    }
	
	// -----------------------------------------------------
	
	public static void main(String[] args) {
		new MultiThreadingTest().start(10);
	}
	
	// -----------------------------------------------------
	
	static class Worker implements Runnable {
		
		private final ArastrejuGate gate;

		public Worker(final ArastrejuGate gate) {
			this.gate = gate;
		}
		
		public void run() {
			final StopWatch sw = new StopWatch();
			final Conversation mc = gate.startConversation();
			
			final TransactionControl txc = mc.beginTransaction();
			
			final SNClass clazz = new SNClass();
			mc.attach(clazz);
			
			for (int i = 1; i <= (1000); i++) {
				mc.attach(clazz.createInstance());
			}
			
			sw.displayTime("created instances of " + clazz);
			
			final Query query = gate.startConversation().createQuery().add(new FieldParam(RDF.TYPE, clazz.toURI()));
			final List<ResourceNode> instances = query.getResult().toList();
			sw.displayTime("found "+ instances.size() + " instances of " + clazz);
			
			txc.commit();
			
			System.out.println("Thread '" + Thread.currentThread().getId() + "' finished.");
		}
		
	}
	

}
