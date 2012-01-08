/*
 * Copyright (C) 2010 lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.arastreju.bindings.neo4j.impl;

import org.arastreju.sge.apriori.RDF;
import org.arastreju.sge.inferencing.CompoundInferencer;
import org.arastreju.sge.inferencing.implicit.TypeInferencer;
import org.arastreju.sge.persistence.ResourceResolver;

/**
 * <p>
 *  Inferencer for Neo 4j for soft inferences, i.e. inferences that will only be put in the index,
 *  but not to the database.
 * </p>
 *
 * <p>
 * 	Created Dec 1, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoSoftInferencer extends CompoundInferencer {

	public NeoSoftInferencer(final ResourceResolver resolver) {
		addInferencer(new TypeInferencer(resolver), RDF.TYPE);
	}
	
}
