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

import edu.harvard.huh.asa.BotanistName;
import edu.harvard.huh.asa.BotanistName.TYPE;
import edu.ku.brc.specify.datamodel.AgentVariant;

public class BotanistNameConverter
{
    private static final Logger log  = Logger.getLogger( BotanistNameConverter.class );

    private static BotanistNameConverter instance = new BotanistNameConverter();
    
    private BotanistNameConverter() {
        ;
    }
    
    public static BotanistNameConverter getInstance() {
        return instance;
    }
    
    public AgentVariant convert(BotanistName botanistName) {

        AgentVariant variant = new AgentVariant();
        
        String name = botanistName.getName();
        if (name.length() > 255) {
            log.warn("Truncating botanist name variant: " + name);
            name = name.substring(0, 255);
        }
        variant.setName(botanistName.getName());
        
        Byte varType;
        TYPE nameType = botanistName.getType();
        
        if      (nameType == TYPE.Author      ) varType = AgentVariant.AUTHOR;
        else if (nameType == TYPE.AuthorAbbrev) varType = AgentVariant.AUTHOR_ABBREV;
        else if (nameType == TYPE.Collector   ) varType = AgentVariant.LABLELNAME;
        else if (nameType == TYPE.Variant     ) varType = AgentVariant.VARIANT;
        
        else throw new IllegalArgumentException("Unrecognized BotanistName type");

        variant.setVarType(varType);
        
        return variant;
    }
}
