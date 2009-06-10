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
package edu.ku.brc.specify.datamodel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * A data class to hold fp messages corresponding to WorkbenchRows.
 * 
 * Note: this class has a natural ordering that is inconsistent with equals (as described be {@link Comparable#compareTo(Object)}.
 * 
 * @author mkelly
 * @code_status Alpha
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "workbenchrowfpmsg")
public class WorkbenchRowFpMsg implements java.io.Serializable, Comparable<WorkbenchRowFpMsg>
{
    protected Integer             workbenchRowFpMsgId;
    protected WorkbenchRow        workbenchRow;
    protected FilteredPushMessage fpMessage;
    /**
     * Constructor (for JPA compliance).
     */
    public WorkbenchRowFpMsg()
    {
        //
    }
    
    public WorkbenchRowFpMsg(final WorkbenchRow workbenchRow, final FilteredPushMessage fpMessage)
    {
        initialize();
        this.workbenchRow = workbenchRow;
        this.fpMessage = fpMessage;
        workbenchRow.getWorkbenchRowFpMsgs().add(this);
    }

    public void initialize()
    {
        workbenchRowFpMsgId = null;
        workbenchRow        = null;
        fpMessage           = null;
    }

    @Id
    @GeneratedValue
    @Column(name = "WorkbenchRowFpMsgID", nullable = false)
    public Integer getWorkbenchRowFpMsgId()
    {
        return workbenchRowFpMsgId;
    }

    public void setWorkbenchRowFpMsgId(Integer workbenchRowFpMsgId)
    {
        this.workbenchRowFpMsgId = workbenchRowFpMsgId;
    }
    
    /**
     * Generic Getter for the ID Property.
     * @returns ID Property.
     */
    @Transient
    public Integer getId()
    {
        return this.workbenchRowFpMsgId;
    }

    @ManyToOne
    @JoinColumn(name = "WorkbenchRowID", nullable = false)
    public WorkbenchRow getWorkbenchRow()
    {
        return workbenchRow;
    }

    public void setWorkbenchRow(WorkbenchRow workbenchRow)
    {
        this.workbenchRow = workbenchRow;
    }
    
    @ManyToOne
    @JoinColumn(name = "FpMessageID", nullable = false)
    public FilteredPushMessage getFpMessage()
    {
        return this.fpMessage;
    }

    public void setFpMessage(FilteredPushMessage fpMessage)
    {
        this.fpMessage = fpMessage;
    }

    @Override
    public String toString()
    {
        return workbenchRow + ": " + fpMessage;
    }
    

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(WorkbenchRowFpMsg o)
    {        
        return this.getFpMessage().getReceivedDate().compareTo(o.getFpMessage().getReceivedDate());
    }
}
