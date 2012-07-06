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
import java.util.List;

import org.arastreju.sge.Arastreju;
import org.arastreju.sge.ArastrejuGate;
import org.arastreju.sge.ModelingConversation;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.apriori.RDF;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.views.SNClass;
import org.arastreju.sge.persistence.TransactionControl;
import org.arastreju.sge.query.FieldParam;
import org.arastreju.sge.query.Query;

import de.lichtflut.infra.logging.StopWatch;

/**
 * <p>
 *  Test case for multithreading.
 * </p>
 *
 * <p>
 * 	Created Jun 6, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class MultiThreadingTest {
	
	/**
	 * Constructor.
	 */
	public MultiThreadingTest(final int numberOfThreads) {
		
		for(int i = 0; i < numberOfThreads; i++) {
			final Thread t = new Thread(new Worker(Arastreju.getInstance().openMasterGate()));
			t.start();
			System.out.println("Startet Thread: " + t.getId());
		}
	}
	
	// -----------------------------------------------------
	
	public static void main(String[] args) {
		new MultiThreadingTest(10);
	}
	
	// -----------------------------------------------------
	
	static class Worker implements Runnable {
		
		private final ArastrejuGate gate;

		public Worker(final ArastrejuGate gate) {
			this.gate = gate;
		}
		
		public void run() {
			final StopWatch sw = new StopWatch();
			final ModelingConversation mc = gate.startConversation();
			
			final TransactionControl txc = mc.beginTransaction();
			
			final SNClass clazz = createClass();
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
		
		public SNClass createClass() {
			SNClass clazz = new SNClass();
			return clazz;
		}
		
	}
	

}
