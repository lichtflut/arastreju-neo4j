/*
 * Copyright 2012 by lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 */
package org.arastreju.bindings.neo4j.impl;

import java.util.AbstractSet;
import java.util.Iterator;

import org.arastreju.sge.model.Statement;

/**
 * <p>
 *  The set of attached associations of a resource.
 *  
 *  <strong>For future use!</strong>
 * </p>
 *
 * <p>
 * 	Created Feb 14, 2012
 * </p>
 *
 * @author Oliver Tigges
 */
public class AssociationSet extends AbstractSet<Statement> {

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<Statement> iterator() {
		return null;
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return 0;
	}

}
