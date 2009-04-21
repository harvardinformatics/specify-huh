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

import java.io.IOException;
import java.util.List;

import com.csvreader.CsvWriter;

import edu.ku.brc.specify.rstools.ExportRow;

/**
 * @author timbo
 * 
 * @code_status Alpha
 * 
 */
public class CSVExport implements DataExport
{
    ConfigureCSV config;

    public CSVExport(final ConfigureExternalDataIFace config)
    {
        this.config = (ConfigureCSV) config;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.ku.brc.specify.tasks.subpane.wb.DataExport#getConfig()
     */
    public ConfigureExternalDataIFace getConfig()
    {
        return this.config;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.ku.brc.specify.tasks.subpane.wb.DataExport#setConfig(edu.ku.brc.specify.tasks.subpane.wb.ConfigureExternalDataIFace)
     */
    public void setConfig(ConfigureExternalDataIFace config)
    {
        this.config = (ConfigureCSV) config;
    }

    protected void writeHeaders(CsvWriter csv) throws IOException
    {
        try
        {
            csv.writeRecord(config.getHeaders(), true);
        }
        catch (IOException e)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(CSVExport.class, e);
            throw e;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.ku.brc.specify.tasks.subpane.wb.DataExport#writeData(java.util.List)
     */
    public void writeData(final List<?> data/*
                                             * , final DataProviderSessionIFace session, final
                                             * boolean closeSession
                                             */) throws Exception
    {
        String[] record;
        CsvWriter writer = new CsvWriter(config.getFileName());
        if (config.getFirstRowHasHeaders() && !config.getAppendData())
        {
            writeHeaders(writer);
        }
        for (int r = 0; r < data.size(); r++)
        {
            // MMK: since edu.ku.brc.specify.tasks.WorkbenchTask#exportWorkbench(final CommandAction cmdAction)
            // now sets the command action's data to List<ExportRow> rather than List<WorkbenchRow>,
            // I had to change this slightly.  ExportRow implements the same "interface" as the set of
            // methods that CSVExport calls on WorkbenchRow.

            ExportRow row = (ExportRow) data.get(r);
            record = new String[row.getExportFields().size()];
            for (int c = 0; c < row.getExportFields().size(); c++)
            {
                record[c] = row.getExportFields().get(c).getData();
            }
            try
            {
                writer.writeRecord(record);
            }
            catch (IOException e)
            {
                edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(CSVExport.class, e);
                throw (e);
            }
        }
        writer.flush();
    }

}
