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
package org.arastreju.bindings.neo4j;

import org.neo4j.graphdb.Relationship;

/**
 * <p>
 *  Constants for Arastreju Neo binding.
 * </p>
 *
 * <p>
 * 	Created Oct 11, 2010
 * </p>
 *
 * @author Oliver Tigges
 */
public interface NeoConstants {
	
	/**
	 * The URI of a resource node.. 
	 */
	String PROPERTY_URI = "resource-uri";
	
	/**
	 * Value string of a value node. 
	 */
	String PROPERTY_VALUE = "value";
	
	String PROPERTY_LOCALE = "locale";
	
	String PROPERTY_DATATYPE = "datatype";
	
	// ----------------------------------------------------
	
	/**
	 * Attribute of a {@link Relationship}.
	 */
	String CONTEXT_URI = "context-uri";
	
	/**
	 * Attribute of a {@link Relationship}.
	 */
	String PREDICATE_URI = "predicate-uri";
	
	/**
	 * Attribute of a {@link Relationship}.
	 */
	String TIMESTAMP = "timestamp";
	
}
