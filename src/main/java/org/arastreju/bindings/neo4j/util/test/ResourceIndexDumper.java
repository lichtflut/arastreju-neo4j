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
package org.arastreju.bindings.neo4j.util.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

/**
 * <p>
 *  Simple IndexDumper to dump out a lucene index into a given {@PrintStream}
 * </p>
 *
 * <p>
 * 	Created May 25, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class ResourceIndexDumper {

	private final String dir;
	
	// --CONSTRUCTIR----------------------------------------
	
	/**
	 * @param dir, the specified index directory
	 */
	public ResourceIndexDumper(String dir){
		this.dir = dir;
	}
	
	// -----------------------------------------------------
	
	/**
	 * Dumps out the lucene indices into the given {@PrintStream}
	 */
	public void writeOut(final PrintStream out) throws CorruptIndexException, IOException{
		IndexReader reader = IndexReader.open(FSDirectory.open(new File(dir)),true);
		for (int i = 0; i < reader.numDocs(); i++){
            Document document = reader.document(i);
            List<Fieldable> fields = document.getFields();
            for (Fieldable field : fields) {
            	out.println(field.name() + " -> " + field.stringValue());
			}
		}
		out.flush();
	}
}
