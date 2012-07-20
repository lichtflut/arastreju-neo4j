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
package org.arastreju.bindings.neo4j.extensions;

import java.util.Locale;

import org.arastreju.bindings.neo4j.NeoConstants;
import org.arastreju.sge.model.ElementaryDataType;
import org.arastreju.sge.model.nodes.SNValue;
import org.neo4j.graphdb.Node;

/**
 * <p>
 *  Simple extension of {@link SNValue} for Neo data nodes.
 * </p>
 *
 * <p>
 * 	Created Jan 6, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class SNValueNeo extends SNValue implements NeoConstants {

	/**
	 * Creates a new {@link SNValue} based on a Neo4j {@link Node}.
	 * @param neoNode The Neo4j Node.
	 */
	public SNValueNeo(final Node neoNode){
		super(getDatatype(neoNode), neoNode.getProperty(PROPERTY_VALUE), getLocale(neoNode));
	}
	
	// -----------------------------------------------------
	
	/**
	 * @param neoNode The Neo Node.
	 * @return The corresponding datatype.
	 */
	private static ElementaryDataType getDatatype(final Node neoNode) {
		final String datatypeName = (String) neoNode.getProperty(PROPERTY_DATATYPE);
		return ElementaryDataType.valueOf(datatypeName);
	}
	
	/**
	 * @param neoNode The Neo Node.
	 * @return The corresponding datatype.
	 */
	private static Locale getLocale(final Node neoNode) {
		if (!neoNode.hasProperty(PROPERTY_LOCALE)) {
			return null;
		}
		final String localeName = (String) neoNode.getProperty(PROPERTY_LOCALE);
		String language = localeName.substring(0, 2);
		if (localeName.length() >= 5 ) {
			String country = localeName.substring(3, 5);
			return new Locale(language, country);
		} else {
			return new Locale(language);
		}
	}

}
