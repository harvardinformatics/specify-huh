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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import edu.ku.brc.util.Orderable;

/**
 * @author jstewart
 * @code_status Alpha
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true, dynamicUpdate=true)
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "repositoryagreementattachment")
public class RepositoryAgreementAttachment extends DataModelObjBase implements ObjectAttachmentIFace<RepositoryAgreement>, 
                                                                               Orderable,
                                                                               Serializable,
                                                                               Comparable<RepositoryAgreementAttachment>
{
    protected Integer    repositoryAgreementAttachmentId;
    protected RepositoryAgreement     repositoryAgreement;
    protected Attachment attachment;
    protected Integer    ordinal;
    protected String     remarks;
    
    public RepositoryAgreementAttachment()
    {
        // do nothing
    }
    
    public RepositoryAgreementAttachment(Integer id)
    {
        this.repositoryAgreementAttachmentId = id;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#initialize()
     */
    @Override
    public void initialize()
    {
        super.init();
        repositoryAgreementAttachmentId = null;
        repositoryAgreement             = null;
        attachment         = new Attachment();
        attachment.initialize();
        ordinal            = null;
    }

    @Id
    @GeneratedValue
    @Column(name = "RepositoryAgreementAttachmentID")
    public Integer getRepositoryAgreementAttachmentId()
    {
        return repositoryAgreementAttachmentId;
    }

    public void setRepositoryAgreementAttachmentId(Integer repositoryAgreementAttachmentId)
    {
        this.repositoryAgreementAttachmentId = repositoryAgreementAttachmentId;
    }

    @ManyToOne
    @JoinColumn(name = "RepositoryAgreementID", nullable = false)
    public RepositoryAgreement getRepositoryAgreement()
    {
        return repositoryAgreement;
    }

    public void setRepositoryAgreement(RepositoryAgreement repositoryAgreement)
    {
        this.repositoryAgreement = repositoryAgreement;
    }

    @ManyToOne()
    @Cascade( {CascadeType.SAVE_UPDATE, CascadeType.MERGE, CascadeType.LOCK} )
    @JoinColumn(name = "AttachmentID", nullable = false)
    @OrderBy("ordinal ASC")
    public Attachment getAttachment()
    {
        return attachment;
    }

    public void setAttachment(Attachment attachment)
    {
        this.attachment = attachment;
    }

    @Column(name = "Ordinal")
    public Integer getOrdinal()
    {
        return ordinal;
    }

    public void setOrdinal(Integer ordinal)
    {
        this.ordinal = ordinal;
    }

    @Lob
    @Column(name = "Remarks", length = 4096)
    public String getRemarks()
    {
        return remarks;
    }

    public void setRemarks(String remarks)
    {
        this.remarks = remarks;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.OrderableFormDataObj#getOrderIndex()
     */
    @Transient
    public int getOrderIndex()
    {
        return (this.ordinal != null) ? this.ordinal : 0;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.OrderableFormDataObj#setOrderIndex(int)
     */
    public void setOrderIndex(int ordinal)
    {
        this.ordinal = ordinal;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getDataClass()
     */
    @Override
    @Transient
    public Class<?> getDataClass()
    {
        return RepositoryAgreementAttachment.class;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getId()
     */
    @Override
    @Transient
    public Integer getId()
    {
        return repositoryAgreementAttachmentId;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getTableId()
     */
    @Override
    @Transient
    public int getTableId()
    {
        return 118;
    }

    @Transient
    public RepositoryAgreement getObject()
    {
        return getRepositoryAgreement();
    }

    public void setObject(RepositoryAgreement object)
    {
        setRepositoryAgreement(object);
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getParentTableId()
     */
    @Override
    @Transient
    public Short getParentTableId()
    {
        return (short)RepositoryAgreement.getClassTableId();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getParentId()
     */
    @Override
    @Transient
    public Integer getParentId()
    {
        return repositoryAgreement != null ? repositoryAgreement.getId() : null;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getIdentityTitle()
     */
    @Override
    @Transient
    public String getIdentityTitle()
    {
         return Attachment.getIdentityTitle(this);
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#toString()
     */
    @Override
    public String toString()
    {
        String aString = (attachment != null) ? attachment.getIdentityTitle() : "NULL Attachment";
        String oString = (getObject() != null) ? getObject().getIdentityTitle() : "NULL Object Reference";
        return aString + " : " + oString;
    }
    
    
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(RepositoryAgreementAttachment obj)
    {
        return ordinal.compareTo(obj.ordinal);
    }

}
