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
package edu.harvard.huh.asa2specify.loader;

import java.io.File;
import java.sql.Statement;

import edu.harvard.huh.asa.AsaDetermination;
import edu.ku.brc.specify.datamodel.Determination;

public class DeterminationLoader extends CsvToSqlLoader
{

    public DeterminationLoader(File csvFile, Statement sqlStatement)
    {
        super(csvFile, sqlStatement);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void loadRecord(String[] columns) throws LocalException
    {
        AsaDetermination asaDetermination = parseDeterminationRecord(columns);

        Determination determination = convert(asaDetermination);
        
        String sql = getInsertSql(determination);
        insert(sql);
    }

    private AsaDetermination parseDeterminationRecord(String[] columns)
    {
        AsaDetermination determination = new AsaDetermination();
        
        return determination;
    }
    
    private Determination convert(AsaDetermination asaDetermination)
    {
        Determination determination = new Determination();
        
        return determination;
    }
    
    private String getInsertSql(Determination determination)
    {
        String fieldNames = "CollectionMemberID, CollectionObjectID, TimestampCreated";

        String[] values = new String[3];
        
        values[0] = String.valueOf( determination.getCollectionMemberId() );
        values[1] = String.valueOf( determination.getCollectionObject().getId() );
        values[2] ="now";

        return SqlUtils.getInsertSql("determination", fieldNames, values);
    }
}
