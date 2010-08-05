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

public class Optr
{
    private Integer id;
    private  String userName;
    private  String fullName;
    private  String note;

    public Integer getId() { return id; }
    
    public String getUserName() { return userName; }
    
    public String getFullName() { return fullName; }
    
    public String getNote() { return note; }
    
    public void setId(Integer id) { this.id = id; }
    
    public void setUserName(String userName) { this.userName = userName; }
    
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public void setNote(String remarks) { this.note = remarks; }
    
    public String getFirstName()
    {
        if (fullName.contains(","))
        {
            // If there's at least one comma, the first name is everything after the comma
            int commaIndex = fullName.indexOf(',');

            return fullName.substring(commaIndex + 1).trim();  
        }
        else if(fullName.contains(" "))
        {
            String[] nameParts = fullName.split(" ");
            if (nameParts.length == 1)
            {
                return null;
            }
            else if (nameParts.length < 4)
            {
                // If there's one or two spaces, the first name is everything before the last space
                int spaceIndex = fullName.lastIndexOf(' ');
                return fullName.substring(0, spaceIndex);
            }
            else if (nameParts.length == 4){
                // If there's three spaces, the first name is everything after the second space
                return nameParts[2] + " " + nameParts[3];
            }
            else {
                return null;
            }
        }
        else
        {
            return null;
        }
    }
    
    public String getLastName()
    {
        if (fullName.contains(","))
        {
            int commaIndex = fullName.indexOf(',');

            // If there's at least one comma, the last name is everything before the comma
            return fullName.substring(0, commaIndex).trim();
        }
        else if(fullName.contains(" "))
        {
            String[] nameParts = fullName.split(" ");
            if (nameParts.length == 1)
            {
                return fullName;
            }
            else if (nameParts.length < 4)
            {
                // If there's one or two spaces, the last name is everything after the last space
                return nameParts[nameParts.length -1];
            }
            else if (nameParts.length == 4){
                // If there's three spaces, the last name is everything before the second space
                return nameParts[0] + " " + nameParts[1];
            }
            else {
                return fullName;
            }
        }
        else
        {
            return fullName;
        }
    }
}
