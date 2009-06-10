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
package edu.ku.brc.specify.datamodel;

import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true, dynamicUpdate=true)
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "fpmessage")
public class FilteredPushMessage 
{
    // Fields    

    protected Integer  fpMessageId;
    protected String   name;
    protected String   uri;
    protected Calendar acknowledgedDate;
    protected Calendar receivedDate;
    
    // Constructors

    /** default constructor */
    public FilteredPushMessage()
    {
        // do nothing
    }

    /** constructor with id */
    public FilteredPushMessage(Integer fpMessageId) 
    {
        this.fpMessageId = fpMessageId;
    }

    @Id
    @GeneratedValue
    @Column(name = "FpMessageID", unique = false, nullable = false, insertable = true, updatable = true)
    public Integer getFpMessageId()
    {
        return fpMessageId;
    }

    public void setFpMessageId(Integer fpMessageId)
    {
        this.fpMessageId = fpMessageId;
    }

    @Column(name = "Name", unique = false, nullable = false, insertable = true, updatable = true, length = 50)
    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }

    @Temporal(TemporalType.DATE)
    @Column(name = "AcknowledgedDate", unique = false, nullable = true, insertable = true, updatable = true)
    public Calendar getAcknowledgedDate()
    {
        return acknowledgedDate;
    }
    
    public void setAcknowledgedDate(Calendar acknowledgedDate)
    {
        this.acknowledgedDate = acknowledgedDate;
    }
    
    @Temporal(TemporalType.DATE)
    @Column(name = "ReceivedDate", unique = false, nullable = false, insertable = true, updatable = true)
    public Calendar getReceivedDate()
    {
        return receivedDate;
    }
    
    public void setReceivedDate(Calendar receivedDate)
    {
        this.receivedDate = receivedDate;
    }
    
    @Lob
    @Column(name = "Uri", unique = false, nullable = false, insertable = true, updatable = false, length = 4096)
    public String getUri()
    {
        return uri;
    }
    
    public void setUri(String uri)
    {
        this.uri = uri;
    }
    
    public int compareTo(FilteredPushMessage o)
    {
        return getReceivedDate().compareTo(o.getReceivedDate());
    }
}
