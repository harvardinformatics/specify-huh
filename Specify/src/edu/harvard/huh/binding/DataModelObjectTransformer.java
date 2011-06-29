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

import java.io.OutputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/** Class for applying a transformation to xml. Can be used as an alternative
 * to DataModelObjectMarshaller if marshaling is not required.
 * 
 * @author lowery
 *
 */
public class DataModelObjectTransformer {
	private StreamSource xsl;
	
	/** The constructor takes the xsl as a StreamSource argument
	 * 
	 * @param xsl
	 */
	public DataModelObjectTransformer(StreamSource xsl) {
		this.xsl = xsl;
	}
	
	/** This method performs a transformation using the xsl supplied in the constructor
	 * on the StreamSource xml argument and sends the output to the OutputStream argument
	 * 
	 * @param xml
	 * @param stream
	 */
	public void transform(StreamSource xml, OutputStream stream) {
		TransformerFactory tFactory = TransformerFactory.newInstance();
		try {
			Transformer transformer = tFactory.newTransformer(xsl);
			transformer.transform(xml, new StreamResult(stream));
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
}
