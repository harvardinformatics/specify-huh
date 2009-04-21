/* This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.ku.brc.specify.tasks.subpane.wb;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.ku.brc.specify.rstools.ExportDataSet;
import edu.ku.brc.specify.rstools.ExportField;
import edu.ku.brc.specify.rstools.ExportRow;

public class XMLExport implements DataExport
{
    private ConfigureXML config;
 
    final static String DATASET_ELT_NAME  = "recordset";
    final static String DATASET_NAME_ATTR = "name";
    final static String DATASET_REL_ATTR  = "relation";
    final static String ROW_ELT_NAME      = "record";
    final static String FIELD_ELT_NAME    = "field";
    final static String FIELD_NAME_ATTR   = "name";

    private Document             document = null;
    private Transformer       transformer = null;
    
    public XMLExport(ConfigureExternalDataIFace config) throws Exception
    {
        setConfig(config);

        try
        {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            transformer = tFactory.newTransformer();
            
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            //factory.setNamespaceAware(true);
            //factory.setValidating(true); 
            document = dbFactory.newDocumentBuilder().newDocument();
        }
        catch (TransformerConfigurationException e)
        {
            throw e; // TODO: handle this exception?
        }
        catch (ParserConfigurationException e)
        {
            throw e; // TODO: handle this exception?
        }
    }

    public ConfigureExternalDataIFace getConfig()
    {
        return config;
    }

    public void setConfig(ConfigureExternalDataIFace config)
    {
        this.config = (ConfigureXML)config;
    }

    public void writeData(List<?> data) throws Exception
    {
        ExportDataSet dataSet = new ExportDataSet();

        if (! data.isEmpty())
        {
            if (((ExportRow) data.get(0)).getDataSet() != null)
            {
                dataSet.setName(((ExportRow) data.get(0)).getDataSet().getName());
            }
            for (Object obj : data)
            {
                dataSet.addExportRow((ExportRow) obj);
            }
        }

        writeData(dataSet);
    }
 
    /**
     * Create an xml org.w3c.dom.Document and print it to a file
     * specified in {@link #config}
     * @param dataSet
     * @throws Exception
     */
    public void writeData(ExportDataSet dataSet) throws Exception
    {
        // create the <dataset> root; assume that all the rows are from the same workbench
        Element dataSetElement = createElement(dataSet);
        getDocument().appendChild(dataSetElement);

        // for each workbench row selected for export...
        for (ExportRow row : dataSet.getExportRows())
        {
            // create a <row>
            Element rowElement = createElement(row);
            dataSetElement.appendChild(rowElement);

            // for each field in the workbench row...
            for (ExportField field : row.getExportFields())
            {
                // create a <field>
                Element fieldElement = createElement(field);
                rowElement.appendChild(fieldElement);
            }

            // create <relatedset>s for rows in other workbenches related to this <row>                
            for (ExportDataSet relatedSet : row.getRelatedSets())
            {
                // create a <relatedset name="other wb name"/>
                Element relatedSetElement =
                    createElement(relatedSet);
                rowElement.appendChild(relatedSetElement);

                // for each related row...
                for (ExportRow relatedRow : relatedSet.getExportRows())
                {
                    // create a <row>
                    Element relatedRowElement = createElement(relatedRow);
                    relatedSetElement.appendChild(relatedRowElement);

                    // for each field in the related workbench row
                    for (ExportField field : relatedRow.getExportFields())
                    {
                        // create a <field>
                        Element fieldElement = createElement(field);
                        relatedRowElement.appendChild(fieldElement);
                    }
                }
            }
        }
        
        try
        {
            DOMSource     source = new DOMSource(getDocument());            
            FileOutputStream fos = new FileOutputStream(getConfig().getFileName());
            StreamResult  result = new StreamResult(fos);
            getTransformer().transform(source, result);
            fos.close();
        }
        catch (FileNotFoundException e)
        {
            throw e; // TODO: handle this exception?
        }
        catch (IOException e)
        {
            throw e; // TODO: handle this exception?
        }
        catch (TransformerException e)
        {
            throw e; // TODO: handle this exception?
        }
    }
    
    private Document getDocument()
    {
        return document;
    }
    
    private Transformer getTransformer()
    {
        return transformer;
    }
    
    private Element createElement(ExportDataSet dataSet) {
        
        Element element = getDocument().createElement(DATASET_ELT_NAME);

        if (dataSet.getName() != null)
        {
            element.setAttribute(DATASET_NAME_ATTR, dataSet.getName());
        }

        return element;
    }
    
    private Element createElement(ExportRow row) {
        Element element = getDocument().createElement(ROW_ELT_NAME);

        return element;
    }
    
    private Element createElement(ExportField field) {
        Element element = getDocument().createElement(FIELD_ELT_NAME);
        element.setAttribute(FIELD_NAME_ATTR, field.getCaption());
        
        if (field.getData() != null)
        {
            element.appendChild(getDocument().createTextNode(field.getData()));
        }
        
        return element;
    }
 
}
