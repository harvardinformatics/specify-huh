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

public class BotanistRoleSpecialty
{
    private Integer botanistId;
    private  String role;
    private  String specialty;
    private Integer ordinal;
    
    public BotanistRoleSpecialty()
    {
        ;
    }
    
    public Integer getBotanistId() { return botanistId; }
    
    public String getRole() { return role; }
    
    public String getSpecialty() { return specialty; }
    
    public Integer getOrdinal() { return ordinal; }
    
    public void setBotanistId(Integer botanistId) { this.botanistId = botanistId; }
    
    public void setRole(String role) { this.role = role; }
    
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    
    public void setOrdinal(Integer ordinal) { this.ordinal = ordinal; }
    
}
