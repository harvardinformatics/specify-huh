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

public class BotanistName
{
    private Integer botanistId;
    private    TYPE type;
    private  String name;
    
    public static enum TYPE             {  FullName,    AuthorName,    Collector,   Variant,   AuthorAbbrev   };
    private static String[] TypeNames = { "full name", "author name", "collector name", "variant", "author abbrev" };

    public static TYPE parseType(String string) throws AsaException
    {
	    for (TYPE type : TYPE.values())
	    {
	        if (TypeNames[type.ordinal()].equals(string)) return type;
	    }
	    throw new AsaException("Invalid botanist name type: " + string);
    }
    
    public static String toString(TYPE type)
    {
    	return type.name();
    }

    public Integer getBotanistId() { return botanistId; }
    
    public TYPE getType() { return type; }
    
    public String getName() { return name; }
    
    public void setBotanistId(Integer botanistId) {
        this.botanistId = botanistId;
    }
    
    public void setType(TYPE type) {
        this.type = type;
    }
    
    public void setName(String name) {
        this.name = name;
    }

}
