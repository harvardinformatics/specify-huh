/*
 * Created on 2011 June 29th
 *
 * Copyright © 2011 President and Fellows of Harvard College
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * @Author: David B. Lowery  lowery@cs.umb.edu
 */

package edu.harvard.huh.binding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;

/** This class uses JibX bindings to marshal objects to xml. Also contains a method 
 * for applying xslt to the xml result.
 * 
 * @author lowery
 *
 * @param <T>
 */
public class DataModelObjectMarshaller<T> {
	private T t;
	
	/** Constructor takes an object T as the object to perform marshaling and transform
	 * operations on
	 * @param t
	 */
	public DataModelObjectMarshaller(T t) {
		this.t = t;
	}
	
	/** Takes an OutputStream for xml output as an argument and marshals the
	 * object class supplied in the constructor to xml.
	 * 
	 * @param stream
	 */
	public void marshal(OutputStream stream) {
		try {

			IBindingFactory bfact = 
				BindingDirectory.getFactory(t.getClass());
			IMarshallingContext mctx = bfact.createMarshallingContext();
			mctx.marshalDocument(t, "UTF-8", null,
					stream);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** Alternative to marshal method, takes xsl as a StreamSource, marshals the object 
	 * to xml and then applies the xsl to the resulting xml before returning the final
	 * output in the OutputStream supplied as an argument. There is no need to call marshal
	 * first as this method does the marshaling before applying the transformation.
	 * 
	 * @param xsl
	 * @param stream
	 */
	public void transform(StreamSource xsl, OutputStream stream) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		this.marshal(out);
		
		TransformerFactory tFactory = TransformerFactory.newInstance();
		try {
			Transformer transformer = tFactory.newTransformer(xsl);
			transformer.transform(new StreamSource(new ByteArrayInputStream(out.toByteArray())), new StreamResult(stream));
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
}
