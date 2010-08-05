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

public class PublicationNumber
{
    // from st_lookup category 104
    public static enum TYPE  { BPH,  BPH2, HOLLIS, ISBN, ISSN, TL2 };
    
    public static TYPE parseType(String string) throws AsaException
    {
        for (TYPE type : TYPE.values())
        {
            if (type.name().equals(string)) return type;
        }
        throw new AsaException("Invalid botanist name type: " + string);
    }
    
    private Integer publicationId;
    private TYPE type;
    private String text;
    
    public Integer getPublicationId()
    {
        return publicationId;
    }
    public TYPE getType()
    {
        return type;
    }
    public String getText()
    {
        return text;
    }

    public void setPublicationId(Integer publicationId)
    {
        this.publicationId = publicationId;
    }
    public void setType(TYPE type)
    {
        this.type = type;
    }
    public void setText(String text)
    {
        this.text = text;
    }
    
    
}
