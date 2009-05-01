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
package edu.harvard.huh.asa2specify;

import org.apache.log4j.Logger;

import edu.harvard.huh.asa.PublAuthor;
import edu.ku.brc.specify.datamodel.Author;

public class PublAuthorConverter
{
    private static final Logger log  = Logger.getLogger( PublAuthorConverter.class );

    private static PublAuthorConverter instance = new PublAuthorConverter();
    
    private PublAuthorConverter() {
        ;
    }
    
    public static PublAuthorConverter getInstance() {
        return instance;
    }
    
    public Author convert(PublAuthor publAuthor) {

        Author author = new Author();
        
        return author;
    }
}
