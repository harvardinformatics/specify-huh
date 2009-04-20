/* Copyright (C) 2009, University of Kansas Center for Research
 * 
 * Specify Software Project, specify@ku.edu, Biodiversity Institute,
 * 1345 Jayhawk Boulevard, Lawrence, Kansas, 66045, USA
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package edu.ku.brc.specify.tasks.subpane.qb;


/**
 * @author timbo
 *
 * @code_status Alpha
 *
 */
public class ERTICaptionInfoTreeLevel extends ERTICaptionInfoQB
{
    protected final ERTICaptionInfoTreeLevelGrp group;
    protected final int rank;
    protected int rankIdx;
    
    public ERTICaptionInfoTreeLevel(String  colName, 
                                    String  colLabel, 
                                    int     posIndex,
                                    String colStringId,
                                    final ERTICaptionInfoTreeLevelGrp group,
                                    final int rank)
    {
        super(colName, colLabel, true, null, posIndex, colStringId, null);
        this.group = group;
        this.rank = rank;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.tasks.subpane.qb.ERTICaptionInfoQB#processValue(java.lang.Object)
     */
    @Override
    public Object processValue(Object value)
    {
        // TODO Auto-generated method stub
        return group.processValue(value, rankIdx);
    }

    /**
     * @return the rank
     */
    public int getRank()
    {
        return rank;
    }

    /**
     * @return the rankIdx
     */
    public int getRankIdx()
    {
        return rankIdx;
    }
    
    /**
     * @param value the rankIdx to set
     */
    public void setRankIdx(int value)
    {
        rankIdx = value;
    }
    
}
