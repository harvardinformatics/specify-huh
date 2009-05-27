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
import java.sql.SQLException;
import java.sql.Statement;

import edu.harvard.huh.asa2specify.LocalException;

public abstract class TreeLoader extends AuditedObjectLoader
{

    public TreeLoader(File csvFile, Statement sqlStatement) throws LocalException
    {
        super(csvFile, sqlStatement);
    }

    protected void numberNodes(String tableName, String idField) throws LocalException
    {
        String tempTableName = "tempTable";
        try {
            String resetHighestChildNodeNumber = "update " + tableName + " set HighestChildNodeNumber=null";
            getStatement().executeUpdate(resetHighestChildNodeNumber);

            String createTableLikeTaxon = "create table " + tempTableName + " like " + tableName;
            getStatement().execute(createTableLikeTaxon);

            String copyTaxonTable = "insert into " + tempTableName + " select * from " + tableName;
            getStatement().executeUpdate(copyTaxonTable);

            String updateLeafNodes = "update " + tableName + " a " +
            "set a.HighestChildNodeNumber=a.NodeNumber " +
            "where not exists " +
            "(select null from " + tempTableName + " b where a." + idField + "=b.ParentID)";

            int updatedNodes = getStatement().executeUpdate(updateLeafNodes);

            String updateReftaxon = "update " + tempTableName + " a " +
            "set a.HighestChildNodeNumber=" +
            "(select b.HighestChildNodeNumber from " + tableName + " b " +
            "where b." + idField + "=a." + idField + ")";

            getStatement().executeUpdate(updateReftaxon);

            String updateNextLevel = "update " + tableName + " a " +
            "set a.HighestChildNodeNumber=" +
            "(select max(b.HighestChildNodeNumber) from " + tempTableName + " b " +
            "where b.ParentID=a." + idField + ") " +
            "where  a." + idField + " in (select c.ParentID from " + tempTableName + " c " +
            "where c.HighestChildNodeNumber is not null) order by a." + idField + " desc";

            while (updatedNodes > 0) {               
                updatedNodes = getStatement().executeUpdate(updateNextLevel);
                getStatement().executeUpdate(updateReftaxon);
            }
            
            String dropTempTable = "drop table " + tempTableName;
            getStatement().execute(dropTempTable);
            
        }
        catch (SQLException e)
        {
            throw new LocalException("Problem numbering nodes", e);
        }
    }

}
