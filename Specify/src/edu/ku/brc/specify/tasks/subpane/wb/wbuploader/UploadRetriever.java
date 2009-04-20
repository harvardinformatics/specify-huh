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
package edu.ku.brc.specify.tasks.subpane.wb.wbuploader;

import java.awt.Frame;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import edu.ku.brc.af.core.Taskable;
import edu.ku.brc.af.ui.db.QueryForIdResultsIFace;
import edu.ku.brc.specify.tasks.subpane.ESResultsSubPane;
import edu.ku.brc.specify.tasks.subpane.ESResultsTablePanel;
import edu.ku.brc.specify.tasks.subpane.ExpressSearchResultsPaneIFace;
import edu.ku.brc.ui.CustomDialog;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author timbo
 *
 * @code_status Alpha
 * 
 * Displays uploaded (newly created as result of an upload) data.
 * 
 *Currently new records for each displayable table which was uploaded to are displayed in an 
 *ExpressSearchResultsPaneIFace with each table in a ESResultsSupPane.
 *
 *Currently no services are provided. The 'Show More' functionality is enabled but not working.
 *
 */
public class UploadRetriever //implements CommandListener, SQLExecutionListener, CustomQueryListener
{
    protected static final Logger log = Logger.getLogger(UploadRetriever.class);
    
    /**
     * @param uploadTables
     * @param task
     * @param title
     * 
     * Produces an simple-search results -style view of uploaded data with a sub-pane for each uploadTable with
     * columns present in the dataset for which new records were created in the db. 
     */
    public void viewUploads(final List<UploadTable> uploadTables, final Taskable task, final String title)
    {
        viewUploads2(uploadTables, new ESResultsSubPane(title, task, true) 
        {

            /* (non-Javadoc)
             * @see edu.ku.brc.specify.tasks.subpane.ESResultsSubPane#createResultsTable(edu.ku.brc.ui.db.QueryForIdResultsIFace)
             */
            @Override
            protected ESResultsTablePanel createResultsTable(QueryForIdResultsIFace results)
            {
                return new UploadResultsTablePanel(this, results, results.shouldInstallServices(), results.isExpanded()); 
            }
            
        });
    }
    
    /**
     * @param uploadTables
     * @param esrPane
     * 
     * Adds results for each uploadTable and (currently) displays them in a Dialog.
     */
    protected void viewUploads2(final List<UploadTable> uploadTables, final ExpressSearchResultsPaneIFace esrPane)
    {
        for (UploadTable ut : uploadTables)
        {
            esrPane.addSearchResults(new UploadResults(ut, Uploader.getCurrentUpload().uploadData));
        }
            CustomDialog cd = new CustomDialog((Frame )UIRegistry.getTopWindow(), 
                            "Uploaded Data", //XXX i18n
                            true,
                            CustomDialog.OK_BTN,
                            (ESResultsSubPane )esrPane);
            cd.setOkLabel(UIRegistry.getResourceString("CLOSE"));
            cd.setModal(false);
            UIHelper.centerAndShow(cd);
    }


    /**
     * @param flds
     * 
     * Sorts flds by getSequence(), getIndex(), getField()
    */
    
    public static void columnOrder(List<UploadField> flds)
    {
        Collections.sort(flds, new Comparator<UploadField>()
        {

            /*
             * (non-Javadoc)
             * 
             * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
             */
            //@Override
            public int compare(UploadField o1, UploadField o2)
            {
                int result = o1.getSequenceInt() < o2.getSequenceInt() ? -1 : (o1.getSequenceInt() == o2
                        .getSequenceInt() ? 0 : 1);
                if (result != 0) { return result; }

                // else
                result = o1.getIndex() < o2.getIndex() ? -1 : (o1.getIndex() == o2.getIndex() ? 0
                        : 1);
                if (result != 0) { return result; }

                // else
                return o1.getField().compareTo(o2.getField());
            }
        });
    }
    
// /**
// * @param esrPane
// * @param ut
// * @return
// */
// protected JPAQuery startUploadViewJPA(final ExpressSearchResultsPaneIFace esrPane,
// final UploadTable ut)
// {
// JPAQuery jpaQuery = null;
// String sqlStr = getSQL(ut);
//        log.debug(sqlStr);
//        if (sqlStr != null)
//        {
//            jpaQuery = new JPAQuery(sqlStr, this);
//            jpaQuery.setData(new Object[] { ut, esrPane });
//            jpaQuery.start();
//        }
//        return jpaQuery;
//    }

//    protected String getSQL(final UploadTable ut)
//    {
//        //select fields present in the uploaded dataSet
//        String fldsClause = "";
//        for (Vector<UploadField> flds : ut.getUploadFields())
//        {
//            for (UploadField fld : flds)
//            {
//                if (fld.getIndex() != -1 && !(fld.getSequence() > 0))
//                {
//                    fldsClause += (StringUtils.isNotBlank(fldsClause) ? ", " : "") + "tbl." + fld.getField().getName();
//                }
//            }
//        }
//
//        if (StringUtils.isBlank(fldsClause))
//        {
//            return null;
//        }
//        
//        //Make (potentially giant) IN expression for uploaded record keys
//        //XXX Will this work???
//        String whereClause = "";
//        for (UploadedRecordInfo info : ut.getUploadedRecs())
//        {
//            whereClause += (StringUtils.isNotBlank(whereClause) ? ", " : "") + info.getKey().toString();
//        }
//        if (StringUtils.isNotBlank(whereClause))
//        {
//            whereClause = " where " + ut.getTable().getKey().getName() + " in(" + whereClause + ")";
//        }
//        
//        if (StringUtils.isBlank(whereClause))
//        {
//            return null;
//        }
//        
//        return "select " + fldsClause + " from " + ut.getWriteTable().getName() + " tbl " + whereClause;
//    }
    
//    /* (non-Javadoc)
//     * @see edu.ku.brc.dbsupport.CustomQueryListener#exectionDone(edu.ku.brc.dbsupport.CustomQueryIFace)
//     */
//    //@Override
//    public void exectionDone(CustomQueryIFace customQuery)
//    {
//        // TODO Auto-generated method stub
//        Object data = ((JPAQuery)customQuery).getData();
//        UploadTable ut = (UploadTable )((Object[])data)[0];
//        String name = ut.toString();
//        log.debug("Upload results done for " + name + ": " + customQuery.getDataObjects().size());
//    }

//    /* (non-Javadoc)
//     * @see edu.ku.brc.dbsupport.CustomQueryListener#executionError(edu.ku.brc.dbsupport.CustomQueryIFace)
//     */
//    //@Override
//    public void executionError(CustomQueryIFace customQuery)
//    {
//        // TODO Auto-generated method stub
//        log.debug("Upload results error: " + customQuery.getName());
//    }

//    /* (non-Javadoc)
//     * @see edu.ku.brc.ui.CommandListener#doCommand(edu.ku.brc.ui.CommandAction)
//     */
//    //@Override
//    public void doCommand(CommandAction cmdAction)
//    {
//        // TODO Auto-generated method stub
//        
//    }

//    /* (non-Javadoc)
//     * @see edu.ku.brc.dbsupport.SQLExecutionListener#exectionDone(edu.ku.brc.dbsupport.SQLExecutionProcessor, java.sql.ResultSet)
//     */
//    //@Override
//    public void exectionDone(SQLExecutionProcessor process, ResultSet resultSet)
//    {
//        // TODO Auto-generated method stub
//        
//    }

//    /* (non-Javadoc)
//     * @see edu.ku.brc.dbsupport.SQLExecutionListener#executionError(edu.ku.brc.dbsupport.SQLExecutionProcessor, java.lang.Exception)
//     */
//    //@Override
//    public void executionError(SQLExecutionProcessor process, Exception ex)
//    {
//        // TODO Auto-generated method stub
//        
//    }

}
