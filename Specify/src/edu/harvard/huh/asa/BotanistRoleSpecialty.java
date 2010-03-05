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
    public static enum SPECIALTY             {  Algae,   Angiosperms,   Bryophytes,   Cryptogamic,                Diatoms,   Fossils,   Fungi,   FungiAndLichens,    Hepatics,   Lichens,  Mosses,   Phanerogams,   PreLinnean,    Pteridophytes,   Spermatophytes };
    private static String[] SpecialtyNames = { "Algae", "Angiosperms", "Bryophytes", "Cryptogamic, unspecified", "Diatoms", "Fossils", "Fungi", "Fungi and Lichens", "Hepatics", "Lichens", "Mosses", "Phanerogams", "Pre-Linnean", "Pteridophytes", "Spermatophytes" };

    public static SPECIALTY parseSpecialty(String string) throws AsaException
    {
        for (SPECIALTY specialty : SPECIALTY.values())
        {
            if (SpecialtyNames[specialty.ordinal()].equals(string)) return specialty;
        }
        throw new AsaException("Invalid specialty name: " + string);
    }
    
    public static String toString(SPECIALTY specialty)
    {
        return SpecialtyNames[specialty.ordinal()];
    }
    
    private Integer   botanistId;
    private String    role;
    private SPECIALTY specialty;
    private Integer   ordinal;
    
    public Integer getBotanistId() { return botanistId; }
    
    public String getRole() { return role; }
    
    public SPECIALTY getSpecialty() { return specialty; }
    
    public Integer getOrdinal() { return ordinal; }
    
    public void setBotanistId(Integer botanistId) { this.botanistId = botanistId; }
    
    public void setRole(String role) { this.role = role; }
    
    public void setSpecialty(SPECIALTY specialty) { this.specialty = specialty; }
    
    public void setOrdinal(Integer ordinal) { this.ordinal = ordinal; }
    
}
