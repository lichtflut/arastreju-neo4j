/*
 * Copyright 2011 by lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 */
package org.arastreju.bindings.neo4j.index;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import org.arastreju.bindings.neo4j.extensions.NeoAssociationKeeper;
import org.arastreju.bindings.neo4j.impl.AssocKeeperAccess;
import org.arastreju.sge.model.associations.AssociationKeeper;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.naming.QualifiedName;

/**
 * <p>
 *  Cache for attached resources.
 * </p>
 *
 * <p>
 * 	Created Dec 23, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class AssociationKeeperCache {
	
	private SoftReference<Map<QualifiedName, NeoAssociationKeeper>> keeperRegisterReference;
	
	// ----------------------------------------------------
	
	/**
	 * @param qn The resource's qualified name.
	 * @return The association keeper or null;
	 */
	public AssociationKeeper getAssociationKeeper(QualifiedName qn) {
		return getKeeperMap().get(qn);
	}
	
	/**
	 * @param qn The resource's qualified name.
	 * @param resource
	 */
	public void put(QualifiedName qn, ResourceNode resource) {
		getKeeperMap().put(qn, AssocKeeperAccess.getNeoAssociationKeeper(resource));
	}
	
	/**
	 * @param qn The resource's qualified name.
	 */
	public void remove(QualifiedName qn) {
		getKeeperMap().remove(qn);
	}
	
	/**
	 * Clear the cache.
	 */
	public void clear() {
		getKeeperMap().clear();
	}
	
	// ----------------------------------------------------
	
	private synchronized Map<QualifiedName, NeoAssociationKeeper> getKeeperMap() {
		if (keeperRegisterReference == null || keeperRegisterReference.get() == null) {
			final Map<QualifiedName, NeoAssociationKeeper> map = new HashMap<QualifiedName, NeoAssociationKeeper>(1000);
			keeperRegisterReference = new SoftReference<Map<QualifiedName, NeoAssociationKeeper>>(map);
		}
		return keeperRegisterReference.get();
	}

}
