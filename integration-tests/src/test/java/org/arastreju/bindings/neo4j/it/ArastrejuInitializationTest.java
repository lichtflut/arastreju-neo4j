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
package org.arastreju.bindings.neo4j.it;

import junit.framework.Assert;
import org.arastreju.bindings.neo4j.Neo4jGateFactory;
import org.arastreju.sge.Arastreju;
import org.arastreju.sge.ArastrejuGate;
import org.arastreju.sge.ArastrejuProfile;
import org.arastreju.sge.Conversation;
import org.arastreju.sge.ConversationContext;
import org.arastreju.sge.apriori.Aras;
import org.arastreju.sge.model.DetachedStatement;
import org.arastreju.sge.model.Statement;
import org.arastreju.sge.model.nodes.SNResource;
import org.arastreju.sge.model.nodes.views.SNScalar;
import org.arastreju.sge.naming.Namespace;
import org.arastreju.sge.naming.QualifiedName;
import org.arastreju.sge.spi.util.FileStoreUtil;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;


/**
 * <p>
 *  Test cases for initialization of Arastreju.
 * </p>
 *
 * <p>
 * 	Created Jan 4, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class ArastrejuInitializationTest {
	
	@Test
	public void testRootGate() throws IOException {
		final ArastrejuProfile profile = new ArastrejuProfile("any profile");
		profile.setProperty(ArastrejuProfile.GATE_FACTORY, Neo4jGateFactory.class.getCanonicalName());
		profile.setProperty(ArastrejuProfile.ARAS_STORE_DIRECTORY, FileStoreUtil.prepareTempStore());
		
		final Arastreju aras = Arastreju.getInstance(profile);
		Assert.assertNotNull(aras);
		
		final ArastrejuGate root = aras.openMasterGate();
		Assert.assertNotNull(root);
		
		final ArastrejuGate myDomain = aras.openGate("mydomain");
		Assert.assertNotNull(myDomain);
	}
	
	@Test
	public void testTempRootGate(){
		final ArastrejuGate rootGate = Arastreju.getInstance().openMasterGate();
		
		Assert.assertNotNull(rootGate);
		
		Assert.assertNotNull(rootGate.startConversation());
		
		rootGate.close();
		
	}
	
	@Test
	public void testTempRootGateForDomain(){
		final ArastrejuGate mydomain = Arastreju.getInstance().openGate("mydomain");
		
		Assert.assertNotNull(mydomain);
		
		Assert.assertNotNull(mydomain.startConversation());

        mydomain.close();
	}

    @Test
    public void shouldPropagateContextsOfVirtualDomains() throws IOException {
        final ArastrejuProfile profile = new ArastrejuProfile("virtual profile");
        profile.setProperty(ArastrejuProfile.GATE_FACTORY, Neo4jGateFactory.class.getCanonicalName());
        profile.setProperty(ArastrejuProfile.ARAS_STORE_DIRECTORY, FileStoreUtil.prepareTempStore());
        profile.setProperty(ArastrejuProfile.ENABLE_VIRTUAL_DOMAINS, "true");

        final Arastreju aras = Arastreju.getInstance(profile);
        Assert.assertNotNull(aras);

        final ArastrejuGate gate = aras.openGate("mydomain");
        Assert.assertNotNull(gate);

        ConversationContext cc = gate.startConversation().getConversationContext();
        Assert.assertNotNull(cc);
        Assert.assertNotNull(cc.getPrimaryContext());
        Assert.assertEquals(QualifiedName.from(Namespace.LOCAL_CONTEXTS, "mydomain"), cc.getPrimaryContext().getQualifiedName());

    }

    @Test
    @Ignore
    public void testSequentialIndexCreation() throws IOException {
        final ArastrejuProfile profile = new ArastrejuProfile("any profile");
        profile.setProperty(ArastrejuProfile.GATE_FACTORY, Neo4jGateFactory.class.getCanonicalName());
        profile.setProperty(ArastrejuProfile.ARAS_STORE_DIRECTORY, FileStoreUtil.prepareTempStore());
        Arastreju instance = Arastreju.getInstance(profile);

        for (int i = 0; i < 5; i++) {
            ArastrejuGate gate = instance.openGate("idx-test");
            Conversation conversation = gate.startConversation();

            Statement stmt = new DetachedStatement(new SNResource(), Aras.HAS_SERIAL_NUMBER, new SNScalar(1));
            conversation.addStatement(stmt);
            conversation.close();
            gate.close();
        }

        profile.close();
    }
	
}
