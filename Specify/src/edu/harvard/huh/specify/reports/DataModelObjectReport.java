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

package edu.harvard.huh.specify.reports;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JOptionPane;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;

import edu.ku.brc.ui.UIRegistry;

/** Class that takes XSL-FO, generates PDF and displays it to the user.
 *  
 * @author lowery
 *
 */
public class DataModelObjectReport {
	private StreamSource stream;
	private static File reportsDir = new File(UIRegistry.getDefaultWorkingPath() + File.separator + ReportXslFiles.REPORTS_DIR);

	/** The constructor takes XSL-FO as a StreamSource argument
	 * 
	 * @param stream
	 */
	public DataModelObjectReport(StreamSource stream) {
		this.stream = stream;
	}
	
	/** Generates a PDF file and displays it from the stream supplied in the
	 * constructor.
	 */
	public void generatePDF() {
		FopFactory fopFactory = FopFactory.newInstance();
		OutputStream out = null;

		try {
			out = new BufferedOutputStream(new FileOutputStream(new File(reportsDir.getAbsoluteFile() + File.separator + "out.pdf")));
			Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);

			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer(); // identity
																// transformer

			Source src = stream;
			Result res = new SAXResult(fop.getDefaultHandler());

			transformer.transform(src, res);

		} catch (Exception e) {
			e.printStackTrace();
			displayErrorDlg();
		}

		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
				displayErrorDlg();
			}
		}

		PDFReader viewer = new PDFReader();
		try {
			PDFReader.display(reportsDir.getAbsoluteFile() + File.separator + "out.pdf");
		} catch (Exception e) {
			e.printStackTrace();
			displayErrorDlg();
		}

		//set on close operation to delete file
		try {
			new File(reportsDir.getAbsoluteFile() + File.separator + "out.pdf").delete();
		} catch (Exception e) {
			e.printStackTrace();
			displayErrorDlg();
		}
	}
	void displayErrorDlg() {
		JOptionPane.showMessageDialog(null, "Failed to generate invoice pdf!", "Error", JOptionPane.ERROR_MESSAGE);
	}
}
