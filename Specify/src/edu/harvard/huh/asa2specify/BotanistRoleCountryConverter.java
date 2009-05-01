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

import edu.harvard.huh.asa.BotanistRoleCountry;
import edu.ku.brc.specify.datamodel.AgentGeography;

public class BotanistRoleCountryConverter
{
    private static final Logger log  = Logger.getLogger( BotanistRoleCountryConverter.class );

    private static BotanistRoleCountryConverter instance = new BotanistRoleCountryConverter();
    
    private BotanistRoleCountryConverter() {
        ;
    }
    
    public static BotanistRoleCountryConverter getInstance() {
        return instance;
    }
    
    public AgentGeography convert(BotanistRoleCountry botanistRoleCountry) {

        AgentGeography agentGeography = new AgentGeography();
        
        String role = botanistRoleCountry.getRole();
        if (role.length() > 64) {
            log.warn("Truncating botanist role: " + role);
            role = role.substring(0, 64);
        }
        agentGeography.setRole(role);

        Integer ordinal = botanistRoleCountry.getOrdinal();
        agentGeography.setRemarks(String.valueOf(ordinal));

        return agentGeography;
    }
}
