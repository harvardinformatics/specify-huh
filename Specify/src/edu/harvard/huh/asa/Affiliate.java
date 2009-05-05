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

public class Affiliate
{
    private Integer id;
    private String surname;
    private String givenName;
    private String position;
    private String phone;
    private String email;
    private String address;
    private String remarks;
    
    public Integer getId() { return id; }
    
    public String getSurname() { return surname; }
    
    public String getGivenName() { return givenName; }
    
    public String getPosition() { return position; }
    
    public String getPhone() { return phone; }
    
    public String getEmail() { return email; }
    
    public String getAddress() { return address; }
    
    public String getRemarks() { return remarks; }
    
    public String getGuid() { return id + " affiliate"; }
    
    public void setId(Integer id) { this.id = id; }
    
    public void setSurname(String surname) { this.surname = surname; }
    
    public void setGivenName(String givenName) { this.givenName = givenName; }
    
    public void setPosition(String position) { this.position = position; }
    
    public void setPhone(String phone) { this.phone = phone; }
    
    public void setEmail(String email) { this.email = email; }
    
    public void setAddress(String address) { this.address = address; }
    
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
