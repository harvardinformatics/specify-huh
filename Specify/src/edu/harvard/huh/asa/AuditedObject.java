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
package edu.harvard.huh.asa;

import java.util.Date;

public class AuditedObject
{
    private Integer id;
    private Integer createdById;
    private Date createDate;
    private Integer updatedById;
    private Date updateDate;
    
    public Integer getId() { return id; }
    
    public Integer getCreatedById() { return createdById; }
    
    public Date getCreateDate() { return createDate; }
    
    public Integer getUpdatedById() { return updatedById; }
    
    public Date getUpdateDate() { return updateDate; }
    
    public void setId(Integer id) { this.id = id; }
    
    public void setCreatedById(Integer createdById) { this.createdById = createdById; }
    
    public void setCreateDate(Date createDate) { this.createDate = createDate; }
    
    public void setUpdatedById(Integer updatedById) { this.updatedById = updatedById; }
    
    public void setUpdateDate(Date updateDate) { this.updateDate = updateDate; }
}
