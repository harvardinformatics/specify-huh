/* Copyright (C) 2009, University of Kansas Center for Research
 * 
 * Specify Software Project, specify@ku.edu, Biodiversity Institute,
 * 1345 Jayhawk Boulevard, Lawrence, Kansas, 66045, USA
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package edu.ku.brc.specify.tasks.subpane.wb;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hpsf.CustomProperties;
import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import edu.ku.brc.specify.datamodel.Workbench;
import edu.ku.brc.specify.datamodel.WorkbenchRow;
import edu.ku.brc.specify.datamodel.WorkbenchRowImage;
import edu.ku.brc.specify.datamodel.WorkbenchTemplate;
import edu.ku.brc.specify.datamodel.WorkbenchTemplateMappingItem;
import edu.ku.brc.specify.rstools.ExportRow;
import edu.ku.brc.specify.rstools.ExportField;
import edu.ku.brc.specify.tasks.WorkbenchTask;

/**
 * @author timbo
 * 
 * @code_status Alpha
 * 
 */
public class XLSExport implements DataExport
{
    ConfigureXLS config;
    
    public XLSExport(ConfigureExternalDataIFace config)
    {
        setConfig(config);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.ku.brc.specify.tasks.subpane.wb.DataExport#getConfig()
     */
    public ConfigureExternalDataIFace getConfig()
    {
        return config;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.ku.brc.specify.tasks.subpane.wb.DataExport#setConfig(edu.ku.brc.specify.tasks.subpane.wb.ConfigureExternalDataIFace)
     */
    public void setConfig(final ConfigureExternalDataIFace config)
    {
        this.config = (ConfigureXLS)config;
    }

    protected void writeHeaders(final HSSFSheet workSheet)
    {
        String[] headers = config.getHeaders();
        HSSFRow hssfRow = workSheet.createRow(0);
        short col = 0;
        for (String head : headers)
        {
            hssfRow.createCell(col++).setCellValue(new HSSFRichTextString(head));
        }
    }

    /**
     * @param workSheet
     * writes headers for imagePath and geocoord (bg) data columns
     */
    protected void writeExtraHeaders(final HSSFSheet workSheet, Vector<Short> imgCols, short geoDataCol)
    {
        HSSFRow hssfRow = workSheet.getRow(0);
        if (geoDataCol != -1)
        {
        	hssfRow.createCell(geoDataCol).setCellValue(new HSSFRichTextString(DataImport.GEO_DATA_HEADING));
        }
        for (Short c : imgCols)
        {
            hssfRow.createCell(c).setCellValue(new HSSFRichTextString(DataImport.IMAGE_PATH_HEADING));
        }
    }

    /**
     * @param row
     * @return HSSFCellTypes for each column in workbench.
     */
    protected int[] bldColTypes(final WorkbenchTemplate wbt)
    {
        int[] result = new int[wbt.getWorkbenchTemplateMappingItems().size()];
        for (WorkbenchTemplateMappingItem mapItem : wbt.getWorkbenchTemplateMappingItems())
        {
            result[mapItem.getViewOrder()] = getColType(mapItem); 
        }
        return result;
    }
    /**
     * @param colNum - index of a workbench column.
     * @return the excel cell type appropriate for the database field the workbench column maps to.
     */
    protected int getColType(final WorkbenchTemplateMappingItem mapItem)
    {
        Class<?> dataType = WorkbenchTask.getDataType(mapItem);
        // These are the classes currently returned by getDataType():
        // java.lang.Long
        // java.lang.String
        // java.util.Calendar
        // java.lang.Float
        // java.lang.Boolean
        // java.lang.Byte
        // java.lang.Integer
        // java.lang.Short
        // java.lang.Double
        // java.util.Date
        // java.lang.BigDecimal

        return getColType(dataType);
    }
    
    // MMK -- pulled this out of getColType(WorkbenchTemplateMappingItem), where it
    // translates the value returned from WorkbenchTask#getDataType(WorkbenchTemplateMappingItem),
    // so that I could use it in writeData(List<?> data) to translate the value from
    // ExportRow#getType() which is the same thing
    private int getColType(final Class<?> dataType)
    {
        if (dataType == java.lang.Long.class
                || dataType == java.lang.Float.class
                || dataType == java.lang.Byte.class
                || dataType == java.lang.Integer.class
                || dataType == java.lang.Short.class
                || dataType == java.lang.Double.class
                || dataType == java.math.BigDecimal.class)
        {
            return HSSFCell.CELL_TYPE_NUMERIC;
        }
        else if (dataType == java.lang.Boolean.class)
        {
            // XXX still need to test if this type allows "don't know"
            return HSSFCell.CELL_TYPE_BOOLEAN;
        }
        else
        {
            return HSSFCell.CELL_TYPE_STRING;
        }
    }
    
    /**
     * calls HSSFCell.setCellValue 
     * 
     * Since all data is treated as string data by the WB and is not validated until an upload is attempted,
     * Validation and type-checking is no longer performed here since it could lead to loss of data 
     * in the exported file.
     * 
     * @param cell
     * @param value
     */
    protected void setCellValue(final HSSFCell cell, final String value)
    {
    	cell.setCellValue(new HSSFRichTextString(value));
    }
    
    /**
     * @param wbt
     * @return DocumentSummaryInformation containing the mappings for wbt.
     * 
     * Each mapping is stored as a property, using the column heading as the key.
     */
    protected DocumentSummaryInformation writeMappings(final WorkbenchTemplate wbt)
    {
        DocumentSummaryInformation dsi = PropertySetFactory.newDocumentSummaryInformation();
        CustomProperties cps = new CustomProperties();
        for (WorkbenchTemplateMappingItem wbmi : wbt.getWorkbenchTemplateMappingItems())
        {
            cps.put(wbmi.getCaption(), wbmi.getTableName() + "\t" + wbmi.getFieldName());
        }
        dsi.setCustomProperties(cps);
        return dsi;
    }
    
    // MMK: this method is analogous to writeMappings(WorkbenchTemplate); the assumption here
    // is that all the rows in a data set have the same columns in the same order, but this
    // assumption is made throughout these exporter classes (and is currently true 2008-12-09)
    protected DocumentSummaryInformation writeMappings(final ExportRow row)
    {
        DocumentSummaryInformation dsi = PropertySetFactory.newDocumentSummaryInformation();
        CustomProperties cps = new CustomProperties();
        for (ExportField field : row.getExportFields())
        {
            cps.put(field.getCaption(), field.getTableName() + "\t" + field.getFieldName());
        }
        dsi.setCustomProperties(cps);
        return dsi;
    }
    /*
     * (non-Javadoc)
     * 
     * @see edu.ku.brc.specify.tasks.subpane.wb.DataExport#writeData(java.util.List)
     */
    public void writeData(final List<?> data) throws Exception
    {
        // MMK: since edu.ku.brc.specify.tasks.WorkbenchTask#exportWorkbench(final CommandAction cmdAction)
        // now sets the command action's data to List<ExportRow> rather than List<WorkbenchRow>,
        // I had to change this quite a bit.  Together, ExportRow and ExportField implement the same "interface"
        // as the set of methods that XLSExport calls on WorkbenchRow/WorkbenchTemplateMappingItem.
        
        HSSFWorkbook workBook  = new HSSFWorkbook();
        HSSFSheet    workSheet = workBook.createSheet();
        DocumentSummaryInformation mappings = null;
        
        int rowNum = 0;

        if (config.getFirstRowHasHeaders() && !config.getAppendData())
        {
            writeHeaders(workSheet);
            rowNum++;
            
            String[] headers = config.getHeaders();
            for (short i=0;i<headers.length;i++)
            {
                workSheet.setColumnWidth(i, (short)(StringUtils.isNotEmpty(headers[i]) ? (256 * headers[i].length()) : 2560));
            }
            
            if (data.get(0).getClass().equals(WorkbenchTemplate.class))
            {
                mappings = writeMappings((WorkbenchTemplate)data.get(0));
            }
            else
            {
                // MMK: the assumption previously was that if it isn't a WorkbenchTemplate it's a WorkbenchRow.
                // Now the assumption is that it's an ExportRow.
                mappings = writeMappings(((ExportRow)data.get(0)));
            }
        }
        
        if (data.size() > 0)
        {
            if (data.get(0).getClass().equals(WorkbenchTemplate.class))
            {
                int[] disciplinees = bldColTypes((WorkbenchTemplate) data.get(0));
                // now set up cell types and formats for a bunch of empty rows....
            }
            else
            {

                ExportRow row = (ExportRow) data.get(0);
                
                for (Object obj : data)
                {
                    row  = (ExportRow) obj;

                    HSSFRow hssfRow = workSheet.createRow(rowNum++);
                    
                    for (int i = 0; i < row.getExportFields().size(); i++)
                    {
                        ExportField field = row.getExportFields().get(i);
                        HSSFCell cell = hssfRow.createCell(i);
                        cell.setCellType(getColType(field.getType()));
                        setCellValue(cell, field.getData());
                    }

                    // MMK: previously, more fields were tacked on here (and their headers, too)
                    // in the case of biogeomancer results or images being present.  I am assuming
                    // that those columns appear as workbench columns just like any others, and
                    // can be handled as such, but maybe that needs to be checked.
                    // TODO: check that biogeomancer results and images are handled correctly

                }
            }
        }
        try
        {
            //write the workbook
            FileOutputStream fos = new FileOutputStream(getConfig().getFileName());
            workBook.write(fos);
            fos.close();
            
            //Now write the mappings.
            //NOT (hopefully) the best way to write the mappings, but (sadly) the easiest way. 
            //May need to do this another way if this slows performance for big wbs.
            if (mappings != null)
            {
                InputStream is = new FileInputStream(getConfig().getFileName());
                POIFSFileSystem poifs = new POIFSFileSystem(is);
                is.close();
                mappings.write(poifs.getRoot(), DocumentSummaryInformation.DEFAULT_STREAM_NAME);
                fos = new FileOutputStream(getConfig().getFileName());
                poifs.writeFilesystem(fos);
                fos.close();
            }
        } 
        catch (Exception e)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(XLSExport.class, e);
            throw(e);
        }
    }
    
}
