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

import java.util.Hashtable;

public class Optr
{
    public final static int BFRANZONE  = 3018;
    public final static int BRACH      = 2123;
    public final static int BTAN       = 2135;
    public final static int CBEANS     = 3009;
    public final static int DBOUFFORD  = 2646;
    public final static int DPFISTER   = 2739;
    public final static int EPFISTER   = 2763;
    public final static int ESHAW      = 2820;
    public final static int EWOOD      = 2141;
    public final static int EZACHARIAS = 3010;
    public final static int GLEWISG    = 2711;
    public final static int HALLING    = 2428;
    public final static int HKESNER    = 2705;
    public final static int IHAY       = 2388;
    public final static int JCACAVIO   = 2157;
    public final static int JDOLAN     = 2722;
    public final static int JMACKLIN   = 3003;
    public final static int KGANDHI    = 2798;
    public final static int KITTREDGE  = 2149;
    public final static int LLUKAS     = 3001;
    public final static int MACKLILN   = 2395;
    public final static int MPETERS    = 3002;
    public final static int MSCHMULL   = 3005;
    public final static int PWHITE     = 2316;
    public final static int ROMERO     = 2307;
    public final static int SDAVIES    = 2658;
    public final static int SHULTZ     = 2383;
    public final static int SKELLEY    = 2776;
    public final static int SLAGRECA   = 2363;    
    public final static int SLINDSAY   = 2324;
    public final static int SZABEL     = 3006;
    public final static int THIERS     = 2670;
    public final static int ZANONI     = 2857;
    
    private Integer id;
    private String userName;
    private String fullName;
    private String remarks;
    
    private static Hashtable<Integer, Integer> BotanistIdsByOptrId = new Hashtable<Integer, Integer>();
    
    static {
        BotanistIdsByOptrId.put(BFRANZONE,  Botanist.BFRANZONE);
        BotanistIdsByOptrId.put(BRACH,      Botanist.BRACH);
        BotanistIdsByOptrId.put(BTAN,       Botanist.BTAN);
        BotanistIdsByOptrId.put(CBEANS,     Botanist.CBEANS);
        BotanistIdsByOptrId.put(DBOUFFORD,  Botanist.DBOUFFORD);
        BotanistIdsByOptrId.put(DPFISTER,   Botanist.DPFISTER);
        BotanistIdsByOptrId.put(EPFISTER,   Botanist.EPFISTER);
        BotanistIdsByOptrId.put(ESHAW,      Botanist.ESHAW1);
        BotanistIdsByOptrId.put(EWOOD,      Botanist.EWOOD);
        BotanistIdsByOptrId.put(EZACHARIAS, Botanist.EZACHARIAS);
        BotanistIdsByOptrId.put(GLEWISG,    Botanist.GLEWISG);
        BotanistIdsByOptrId.put(HALLING,    Botanist.HALLING);
        BotanistIdsByOptrId.put(HKESNER,    Botanist.HKESNER);
        BotanistIdsByOptrId.put(IHAY,       Botanist.IHAY);
        BotanistIdsByOptrId.put(JCACAVIO,   Botanist.JCACAVIO);
        BotanistIdsByOptrId.put(JDOLAN,     Botanist.JDOLAN);
        BotanistIdsByOptrId.put(JMACKLIN,   Botanist.JMACKLIN);
        BotanistIdsByOptrId.put(KGANDHI,    Botanist.KGANDHI);
        BotanistIdsByOptrId.put(KITTREDGE,  Botanist.KITTREDGE);
        BotanistIdsByOptrId.put(LLUKAS,     Botanist.LLUKAS);
        BotanistIdsByOptrId.put(MACKLILN,   Botanist.MACKLILN);
        BotanistIdsByOptrId.put(MPETERS,    Botanist.MPETERS);
        BotanistIdsByOptrId.put(MSCHMULL,   Botanist.MSCHMULL);
        BotanistIdsByOptrId.put(PWHITE,     Botanist.PWHITE);
        BotanistIdsByOptrId.put(ROMERO,     Botanist.ROMERO);
        BotanistIdsByOptrId.put(SDAVIES,    Botanist.SDAVIES);
        BotanistIdsByOptrId.put(SHULTZ,     Botanist.SHULTZ1);
        BotanistIdsByOptrId.put(SKELLEY,    Botanist.SKELLEY);
        BotanistIdsByOptrId.put(SLAGRECA,   Botanist.SLAGRECA); 
        BotanistIdsByOptrId.put(SLINDSAY,   Botanist.SLINDSAY);
        BotanistIdsByOptrId.put(SZABEL,     Botanist.SZABEL);
        BotanistIdsByOptrId.put(THIERS,     Botanist.THIERS);
        BotanistIdsByOptrId.put(ZANONI,     Botanist.ZANONI);
    }

    public Integer getId() { return id; }
    
    public String getUserName() { return userName; }
    
    public String getFullName() { return fullName; }
    
    public String getRemarks() { return remarks; }
    
    public String getGuid()
    {
        Integer id = BotanistIdsByOptrId.get(this.id);

        if (id == null)
        {
            return this.id + " optr";
        }
        else
        {
            return id + " botanist";
        }
    }
    
    public void setId(Integer id) { this.id = id; }
    
    public void setUserName(String userName) { this.userName = userName; }
    
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public void setRemarks(String remarks) { this.remarks = remarks; }
    
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
