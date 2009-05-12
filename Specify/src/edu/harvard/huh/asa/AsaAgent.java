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

public class AsaAgent
{
    private Integer id;
    private Integer organizationId;
    private Boolean isActive;
    private String  prefix;
    private String  name;
    private String  title;
    private String  specialty;
    private String  correspAddress;
    private String  shippingAddress;
    private String  email;
    private String  phone;
    private String  fax;
    private String  uri;
    private String  remarks;
    
    public Integer getId() { return id; }
    
    public Integer getOrganizationId() { return organizationId; }
    
    public Boolean isActive() { return isActive; }
    
    public String getPrefix() { return prefix; }
    
    public String getName() { return name; }
    
    public String getTitle() { return title; }
    
    public String getSpecialty() { return specialty; }
    
    public String getCorrespAddress() { return correspAddress; }
    
    public String getShippingAddress() { return shippingAddress; }
    
    public String getEmail() { return email; }
    
    public String getPhone() { return phone; }
    
    public String getFax() { return fax; }
    
    public String getUri() { return uri; }

    public String getRemarks() { return remarks; }

    public String getGuid() { return id + " agent"; }
    
    public void setId(Integer id) { this.id = id; }
    
    public void setOrganizationId(Integer organizationId) { this.organizationId = organizationId; }
    
    public void setActive(Boolean isActive) { this.isActive = isActive; }
    
    public void setPrefix(String prefix) { this.prefix = prefix; }
    
    public void setName(String name) { this.name = name; }
    
    public void setTitle(String title) { this.title = title; }
    
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    
    public void setCorrespAddress(String correspAddress) { this.correspAddress = correspAddress; }
    
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    
    public void setEmail(String email) { this.email = email; }
    
    public void setPhone(String phone) { this.phone = phone; }
    
    public void setFax(String fax) { this.fax = fax; }
    
    public void setUri(String uri) { this.uri = uri; }
    
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
