/**
 * 
 */
package edu.ku.brc.specify.utilapps.morphbank;

import java.util.List;
import java.util.Vector;

import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.CollectionObject;

/**
 * @author timo
 * 
 * Filenames are nothing but a specimen barcode number.
 * 
 *  Used to import images for the Troy database.
 *
 */
public class BarCodeFileNameParser implements FileNameParserIFace
{

	/* (non-Javadoc)
	 * @see edu.ku.brc.specify.plugins.morphbank.FileNameParserIFace#getRecordIds(java.lang.String)
	 */
	@Override
	public List<Integer> getRecordIds(String fileName)
	{
		List<Integer> result = new Vector<Integer>();
		String id = fileName.replace("_", "");
		id = id.substring(0, id.length() - 4);
		String sql = "select CollectionObjectID from collectionobject where AltCatalogNumber = '" + id + "'";
		//System.out.println(sql);
		//result.add(1);
		Vector<Object> idObjs = BasicSQLUtils.querySingleCol(DBConnection.getInstance().getConnection(), sql);
		if (idObjs != null)
		{
			for (Object idObj : idObjs)
			{
				result.add((Integer )idObj);
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see edu.ku.brc.specify.plugins.morphbank.FileNameParserIFace#getTableId()
	 */
	@Override
	public Integer getTableId()
	{
		return CollectionObject.getClassTableId();
	}

}
