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
package edu.ku.brc.util;

import java.io.File;

import edu.ku.brc.helpers.XMLHelper;

/**
 * Class that finds the path to hbm files
 * 
 * @code_status Unknown
 * 
 * @author megkumin
 * 
 */
public class DatamodelHelper 
{
	// Static Data Members
	//private static final Logger log = Logger.getLogger(DatamodelHelper.class);

    private static String outputFileName = "specify_datamodel.xml";
    
	/**
	 * 
	 */
	public DatamodelHelper() 
	{
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Returns full path to file in hbm directory
	 * 
	 * @param fileName
	 *            the name of the file to be read
	 * @return the path to the file
	 */
	public static String getDataModelSrcDirPath() 
	{
        //return ClassLoader.getSystemResource("Accession.hbm.xml").getFile();
        /*String fileName = "Accession.hbm.xml";
		String path = new File(".").getAbsolutePath();
		if (path.endsWith(".")) {
			path = path.substring(0, path.length() - 2);
		}*/
		return"src" + File.separator
				+ "edu" + File.separator + "ku" + File.separator + "brc"
				+ File.separator + "specify" + File.separator + "datamodel";
                
        //return "OldHBMs";
	}

	/**
	 * Gets the path to UI based hbm files
	 * 
	 * @return - path to ui based hbm files String -
	 */
	public static String getUiHbmDirPath() 
	{
	    // return ClassLoader.getSystemResource("PickList.hbm.xml").getFile();
		String fileName = "PickList.hbm.xml";
		String path = new File(".").getAbsolutePath();
		if (path.endsWith(".")) {
			path = path.substring(0, path.length() - 2);
		}
		return path + File.separator + File.separator + "src" + File.separator
				+ "edu" + File.separator + "ku" + File.separator + "brc"
				+ File.separator + "ui" + File.separator + "db"
				+ File.separator + fileName;
	}

	/**
	 * Returns full path to file in datamodel File
	 * 
	 * @return the path to the file
	 */
	public static String getDatamodelFilePath() 
	{
		return XMLHelper.getConfigDirPath(outputFileName);
	}

	/**
	 * Returns full path to file in tableId listing file
	 * 
	 * @return the path to the file
	 */
	public static String getTableIdFilePath() 
	{
		return XMLHelper.getConfigDirPath("specify_tableid_listing.xml");
	}
    
    /**
     * Returns full path to file workbench upload def file
     * 
     * @return the path to the file
     */
    public static String getWorkbenchUploadDefFilePath()
    {
        return XMLHelper.getConfigDirPath("specify_workbench_upload_def.xml");
    }

    /**
     * @return the outputFileName
     */
    public static String getOutputFileName()
    {
        return outputFileName;
    }

    /**
     * @param outputFileName the outputFileName to set
     */
    public static void setOutputFileName(String outputFileName)
    {
        DatamodelHelper.outputFileName = outputFileName;
    }
}
