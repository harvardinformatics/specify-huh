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

import java.util.Vector;

import edu.ku.brc.specify.datamodel.Workbench;

/**
 * @author timbo
 *
 * @code_status Alpha
 *
 *interface for workbench data import
 */
public interface DataImportIFace
{
    public enum Status {None, Valid, Error, Modified}
    
    /**
     * @param config
     */
    public void setConfig(final ConfigureExternalDataIFace config);

    /**
     * @return
     */
    public ConfigureExternalDataIFace getConfig();

    /**
     * @param workbench
     */
    public DataImportIFace.Status getData(final Workbench workbench);
    
    /**
     * @return
     */
    public Status getStatus();
    
    /**
     * @return info on cells truncated during last import
     */
    public Vector<DataImportTruncation> getTruncations();
    
    /**
     * @return messages generated during last import
     */
    public Vector<String> getMessages();
}
