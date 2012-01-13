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
package edu.ku.brc.specify.tasks;

import static edu.ku.brc.ui.UIRegistry.getLocalizedMessage;
import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.JOptionPane;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.harvard.huh.specify.datamodel.busrules.HUHLoanPreparationBusRules;
import edu.harvard.huh.specify.plugins.ItemCountsLabel;
import edu.ku.brc.af.core.SubPaneIFace;
import edu.ku.brc.af.core.SubPaneMgr;
import edu.ku.brc.af.core.TaskMgr;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.core.expresssearch.QueryAdjusterForDomain;
import edu.ku.brc.af.tasks.BaseTask.ASK_TYPE;
import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.MultiView;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.af.ui.forms.validation.FormValidator;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.dbsupport.RecordSetIFace;
import edu.ku.brc.dbsupport.RecordSetItemIFace;
import edu.ku.brc.helpers.SwingWorker;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.InfoRequest;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.PreparationsProviderIFace;
import edu.ku.brc.specify.ui.ColObjInfo;
import edu.ku.brc.specify.ui.PrepInfo;
import edu.ku.brc.ui.JStatusBar;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author rod
 * @author david lowery
 *
 * @code_status Alpha
 *
 * Oct 9, 2008
 *
 */
public class InteractionsProcessor<T extends PreparationsProviderIFace>
{
    private static final Logger log = Logger.getLogger(InteractionsProcessor.class);
    private static final String LOAN_LOADR = "LoanLoader";
    
    protected InteractionsTask task;
    protected boolean          isLoan;
    protected int              tableId;
    protected Viewable         viewable = null;
    
    /**
     * 
     */
    public InteractionsProcessor(final InteractionsTask task, 
                                 final boolean          isLoan,
                                 final int              tableId)
    {
        this.task    = task;
        this.isLoan  = isLoan;
        this.tableId = tableId;
    }
    
    
    /**
     * Asks where the source of the Loan Preps should come from. (ResultSet, InfoRequest, or dialog for catalog numbers)
     * @return the source enum
     */
    protected ASK_TYPE askSourceOfPreps(final boolean hasInfoReqs, final boolean hasFragmentRS)
    {
        String label;
        if (hasInfoReqs && hasFragmentRS)
        {
            label = getResourceString("NEW_INTER_USE_RS_IR");  // RecordSet
            
        } else if (hasInfoReqs)
        {
            label = getResourceString("NEW_INTER_USE_IR");
        } else
        {
            label = getResourceString("NEW_INTER_USE_RS");
        }
        
        Object[] options = { 
                label, 
                getResourceString("NEW_INTER_ENTER_CATNUM") 
              };
        int userChoice = JOptionPane.showOptionDialog(UIRegistry.getTopWindow(), 
                                                     getResourceString("NEW_INTER_CHOOSE_RSOPT"), 
                                                     getResourceString("NEW_INTER_CHOOSE_RSOPT_TITLE"), 
                                                     JOptionPane.YES_NO_CANCEL_OPTION,
                                                     JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (userChoice == JOptionPane.NO_OPTION)
        {
            return ASK_TYPE.EnterCats;
            
        } else if (userChoice == JOptionPane.YES_OPTION)
        {
            return ASK_TYPE.ChooseRS;
        }
        return ASK_TYPE.Cancel;
    }
    
    /**
     * Creates a new loan/gift.
     */
    public void createOrAdd()
    {
        this.viewable = null;
        createOrAdd(null, null, null);
    }
    
    /**
     * Creates a new loan/gift and will set the new data object back into the Viewable.
     * @param viewableArg
     */
    public void createOrAdd(final Viewable viewableArg)
    {
        this.viewable = viewableArg;

        createOrAdd(null, null, null);
    }
    
    /**
     * @param recordSetArg
     */
    public void createOrAdd(final RecordSetIFace recordSetArg)
    {
        this.viewable = null;
        createOrAdd(null, null, recordSetArg);
    }
    
    /**
     * @param currPrepProvider
     */
    public void createOrAdd(final T currPrepProvider)
    {
        this.viewable = null;
        createOrAdd(currPrepProvider, null, null);
    }
    
    public void createOrAdd(final T              currPrepProvider, 
            final InfoRequest    infoRequest, 
            final RecordSetIFace recordSetArg, final Viewable viewable) {
    	this.viewable = viewable;
    	createOrAdd(currPrepProvider, infoRequest, recordSetArg);
    }
    
    /**
     * Creates a new loan from a RecordSet.
     * dl: this is used when you create a new loan and is also used by the batchaddpreps plugin.
     * @param currPrepProvider an existing loan that needs additional Preps
     * @param infoRequest a info request
     * @param recordSetArg the recordset to use to create the loan
     */
    public void createOrAdd(final T              currPrepProvider, 
                            final InfoRequest    infoRequest, 
                            final RecordSetIFace recordSetArg)
    {
        RecordSetIFace recordSet = recordSetArg;
        if (infoRequest == null && recordSet == null)
        {	
            // Get a List of InfoRequest RecordSets
            Vector<RecordSetIFace> rsList         = task.getInfoReqRecordSetsFromSideBar();
            RecordSetTask          rsTask         = (RecordSetTask)TaskMgr.getTask(RecordSetTask.RECORD_SET);
            List<RecordSetIFace>   fragmentRSList = rsTask.getRecordSets(Fragment.getClassTableId());
            
            // If the List is empty then
            if (rsList.size() == 0 && fragmentRSList.size() == 0)
            {
                recordSet = task.askForCatNumbersRecordSet(); // Fragments
                
            } else 
            {
                ASK_TYPE rv = askSourceOfPreps(rsList.size() > 0, fragmentRSList.size() > 0);
                if (rv == ASK_TYPE.ChooseRS)
                {
                    recordSet = RecordSetTask.askForRecordSet(Fragment.getClassTableId(), rsList);
                    
                } else if (rv == ASK_TYPE.EnterCats)
                {
                    recordSet = task.askForCatNumbersRecordSet(); // Fragments
                    
                } else if (rv == ASK_TYPE.Cancel)
                {
                    viewable.setNewObject(null);
                    return;
                }
            }
        }
        
        if (recordSet == null)
        {
            return;
        }
        
        
        // dl: get current multiview object from the subpane and setHasNewData in the formviewobj as true.
        if (currPrepProvider != null) {
        	SubPaneIFace subPane = SubPaneMgr.getInstance().getCurrentSubPane();
        	//MultiView mv = subPane.getMultiView();
        	
 			//viewable = mv.getCurrentViewAsFormViewObj();
            viewable.setHasNewData(true);
        }
        
        DBTableIdMgr.getInstance().getInClause(recordSet);

        DBTableInfo tableInfo = DBTableIdMgr.getInstance().getInfoById(recordSet.getDbTableId());
        
        DataProviderFactory.getInstance().evict(tableInfo.getClassObj()); // XXX Not sure if this is really needed
        
        DataProviderSessionIFace session = null;
        try
        {
            session = DataProviderFactory.getInstance().createSession();
            
            // OK, it COULD be a RecordSet contain one or more InfoRequest, 
            // we will only accept an RS with one InfoRequest
            if (infoRequest == null && recordSet.getDbTableId() == InfoRequest.getClassTableId())
            {
                if (recordSet.getNumItems() == 1)
                {
                    RecordSetItemIFace item = recordSet.getOnlyItem();
                    if (item != null)
                    {
                        InfoRequest infoReq = session.get(InfoRequest.class, item.getRecordId().intValue());
                        if (infoReq != null)
                        {
                            createOrAdd(null, infoReq, infoReq.getRecordSets().iterator().next());
                            
                        } else
                        {
                            // error about missing info request
                            // Error Dialog
                        }
                    } else
                    {
                        // error about item being null for some unbelievable reason 
                     // Error Dialog
                    }
                } else 
                {
                    // error about item having more than one or none
                    // Error Dialog
                }
                return;
            }
            
            // OK, here we have a recordset of Fragments
            // First we process all the Fragments in the RecordSet
            // and create a list of Preparations that can be loaned
            String sqlStr = DBTableIdMgr.getInstance().getQueryForTable(recordSet);
            if (StringUtils.isNotBlank(sqlStr))
            {
                final JStatusBar statusBar = UIRegistry.getStatusBar();
                statusBar.setIndeterminate(LOAN_LOADR, true);
                
                if (recordSet != null) //&& recordSet.getNumItems() > 2) dl: this is commented so that the simple glass pane msg is displayed every time
                {
                    UIRegistry.writeSimpleGlassPaneMsg(getResourceString("NEW_INTER_LOADING_PREP"), 24);
                }
                
                PrepLoaderSQL prepLoaderSQL = new PrepLoaderSQL(currPrepProvider, recordSet, infoRequest, isLoan);
                prepLoaderSQL.addPropertyChangeListener(
                        new PropertyChangeListener() {
                            public  void propertyChange(PropertyChangeEvent evt) {
                                log.debug(evt.getNewValue());
                                if ("progress".equals(evt.getPropertyName())) 
                                {
                                    statusBar.setValue(LOAN_LOADR, (Integer)evt.getNewValue());
                                }
                            }
                        });
                prepLoaderSQL.execute();
                
            } else
            {
                log.error("Query String empty for RecordSet tableId["+recordSet.getDbTableId()+"]");
            }
            
        } catch (Exception ex)
        {
            ex.printStackTrace();
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(InteractionsProcessor.class, ex);
        }
    }

    /**
     * After loading the prepas from the db, run this method to stat the swing worker with calls to addPrepsToGift and addPrepsToLoan
     * @param frToPrepHash
     * @param prepTypeHash
     * @param prepProvider
     * @param infoRequest
     * @param session
     */
    protected void prepsLoaded (final Hashtable<Integer, ColObjInfo> frToPrepHash,
                               final Hashtable<Integer, String>     prepTypeHash,
                               final T                              prepProvider,
                               final InfoRequest                    infoRequest) throws Exception
    {
        
        if (frToPrepHash.size() == 0 || prepTypeHash.size() == 0)
        {
            UIRegistry.showLocalizedMsg("NEW_INTER_NO_PREPS_TITLE", "NEW_INTER_NO_PREPS");
            return;
        }
        
        final DBTableInfo ti = DBTableIdMgr.getInstance().getInfoById(tableId);  // gift or loan table id

        /*final SelectPrepsDlg loanSelectPrepsDlg = new SelectPrepsDlg(frToPrepHash, prepTypeHash, ti.getTitle());
        loanSelectPrepsDlg.createUI();
        loanSelectPrepsDlg.setModal(true);
        
        UIHelper.centerAndShow(loanSelectPrepsDlg);
        
        if (loanSelectPrepsDlg.isCancelled())
        {
            if (viewable != null)
            {
                viewable.setNewObject(null);
            }
            return;
        }*/

        final Hashtable<Integer, Integer> prepsHash = getPreparationCounts(frToPrepHash); //loanSelectPrepsDlg.getPreparationCounts();
        if (prepsHash.size() > 0)
        {
            final SwingWorker worker = new SwingWorker() 
            {
                @Override
                public Object construct()
                {
                    JStatusBar statusBar = UIRegistry.getStatusBar();
                    statusBar.setIndeterminate("INTERACTIONS", true);
                    statusBar.setText(getLocalizedMessage("CREATING_INTERACTION", ti.getTitle()));

                    if (isLoan)
                    {
                        task.addPrepsToLoan(prepProvider, infoRequest, prepsHash, viewable);
                    } else
                    {
                        task.addPrepsToGift(prepProvider, infoRequest, prepsHash, viewable);
                    }
                    
                    return null;
                }

                //Runs on the event-dispatching thread.
                @Override
                public void finished()
                {
                    JStatusBar statusBar = UIRegistry.getStatusBar();
                    statusBar.setProgressDone("INTERACTIONS");
                    statusBar.setText("");
                    UIRegistry.clearSimpleGlassPaneMsg();
                    
                    if (viewable != null) {
	                	MultiView mv = (MultiView)((FormViewObj)viewable).getControlByName("loanPreparations");
	                	FormViewObj formViewObj = mv.getCurrentViewAsFormViewObj();
	              
	                	//dl: update the itemcountslabel plugin when batch add preps is performed
	                    ItemCountsLabel itemCountsLabel = (ItemCountsLabel)formViewObj.getControlById("itemcountslabel");
	                    //HUHLoanPreparationBusRules.doAccounting(itemCountsLabel, (Loan)formViewObj.getParentDataObj());
                    }
                }
            };
            worker.start();
        }
    }
    
    private Hashtable<Integer, Integer> getPreparationCounts(Hashtable<Integer, ColObjInfo> frToPrepHash)
    {
        Hashtable<Integer, Integer> result = new Hashtable<Integer, Integer>();
        
        for (ColObjInfo fragmentInfo : frToPrepHash.values())
        {
            for (PrepInfo prepInfo : fragmentInfo.getPreps().values())
            {
                Integer prepId = prepInfo.getPrepId();
                int countAmt = prepInfo.getQtyPrep();

                result.put(prepId, countAmt);
            }
        }
        return result;
    }
    
    /**
     * Creates a new loan from a InfoRequest.
     * @param infoRequest the infoRequest to use to create the loan
     */
    public void createFromInfoRequest(final InfoRequest infoRequest)
    {   
        RecordSetIFace rs = null;
        DataProviderSessionIFace session = DataProviderFactory.getInstance().createSession();
        try
        {
            session.attach(infoRequest);
            rs = infoRequest.getRecordSets().iterator().next();
            
        } catch (Exception ex)
        {
            ex.printStackTrace();
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(InteractionsProcessor.class, ex);
            // Error Dialog
            
        } finally
        {
            if (session != null)
            {
                session.close();
            }
        }
        
        if (rs != null)
        {
            createOrAdd(null, infoRequest, rs);
        }
    }
    
    //--------------------------------------------------------------
    // Background loader class for loading a large number of loan preparations
    //--------------------------------------------------------------
    class PrepLoaderSQL extends javax.swing.SwingWorker<Integer, Integer>
    {
        private final String PROGRESS = "progress";
        
        private RecordSetIFace recordSet;
        private T              prepsProvider;
        private InfoRequest    infoRequest;
        private boolean        isForLoan;

        private Hashtable<Integer, String>     prepTypeHash = new Hashtable<Integer, String>();
        private Hashtable<Integer, ColObjInfo> frToPrepHash = new Hashtable<Integer, ColObjInfo>();
        
        /**
         * @param prepsProvider
         * @param recordSet
         * @param infoRequest
         */
        public PrepLoaderSQL(final T              prepsProvider,
                             final RecordSetIFace recordSet,
                             final InfoRequest    infoRequest,
                             final boolean        isForLoan)
        {
            this.recordSet     = recordSet;
            this.prepsProvider = prepsProvider;
            this.infoRequest   = infoRequest;
            this.isForLoan     = isForLoan;
        }

        /**
         * @param val
         * @return
         */
        private Integer getInt(final Object val)
        {
            return val == null ? 0 : (Integer)val;
        }
        
        /**
         * @return a List of rows that have the Fragment info from the recordset
         */
        protected Vector<Object[]> getFragmentsFromRecordSet()
        {
            String sql = "SELECT f.FragmentID, ifnull(f.Identifier, p.Identifier), tx.FullName " +
            		     "FROM fragment f INNER JOIN " +
            		     "preparation p ON f.PreparationID=p.PreparationID LEFT JOIN " +
            		     "determination dt ON f.FragmentID=dt.FragmentID LEFT JOIN " +
                         "taxon tx ON (dt.TaxonID=tx.TaxonID AND dt.isCurrent <> 0) " + 
                         "WHERE f.CollectionMemberID = COLMEMID AND f.FragmentID " + DBTableIdMgr.getInstance().getInClause(recordSet);
            sql = QueryAdjusterForDomain.getInstance().adjustSQL(sql);
            log.debug(sql);
            
            Vector<Object[]> fullItems = BasicSQLUtils.query(sql);
            if (fullItems.size() != recordSet.getNumItems())
            {
                sql = "SELECT FragmentID, Identifier FROM fragment WHERE CollectionMemberID = COLMEMID " + 
                      "AND FragmentID " + DBTableIdMgr.getInstance().getInClause(recordSet);
                      
                Vector<Object[]> partialItems = BasicSQLUtils.query(QueryAdjusterForDomain.getInstance().adjustSQL(sql));
                partialItems.addAll(fullItems);
                return partialItems;
            }
            return fullItems;
        }
        
        /**
         * @return
         */
        protected int collectForLoan()
        {
            int total = 0;
            int count = 0;
            try
            {
                Vector<Object[]> frIdRows = getFragmentsFromRecordSet();
                total = frIdRows.size() * 2;
                if (frIdRows.size() != 0)
                {
                    UIRegistry.getStatusBar().setProgressRange(LOAN_LOADR, 0, Math.min(count, total));
    
                    // Get Preps with Loans
                    StringBuilder sb = new StringBuilder();
                    sb.append("SELECT f.FragmentID, p.PreparationID, (ifnull(lp.ItemCount, 0) + ifnull(lp.TypeCount, 0) + ifnull(lp.NonSpecimenCount, 0)) " +
                             "FROM preparation p INNER JOIN fragment f ON p.PreparationID = f.PreparationID " +
                             "INNER JOIN loanpreparation lp ON p.PreparationID = lp.PreparationID " +
                             "WHERE f.CollectionMemberID = COLMEMID AND f.FragmentID in (");
                   for (Object[] row : frIdRows)
                   {
                       count++;
                       if ((count % 10) == 0) firePropertyChange(PROGRESS, 0, count);
                       
                       Integer frId = (Integer)row[0];
                       sb.append(frId);
                       sb.append(',');
                       
                       if (row.length > 1 && row[1] != null)
                       {
                           String identifier = row.length > 1  && row[1] != null ? row[1].toString() : null;
                           String taxonName  = row.length > 2  && row[2] != null ? row[2].toString() : null;
                           frToPrepHash.put(frId, new ColObjInfo(frId, identifier, taxonName));
                       }
                   }
                   sb.setLength(sb.length()-1); // chomp last comma
                   sb.append(')');
                   
                   // Get a hash contain a mapping from PrepId to Loan Quantity
                   Hashtable<Integer, Integer> prepIdToLoanQnt = new Hashtable<Integer, Integer>();
                   
                   String sql = QueryAdjusterForDomain.getInstance().adjustSQL(sb.toString());
                   log.debug(sql);
                   
                   Vector<Object[]> rows = BasicSQLUtils.query(sql);
                   if (rows.size() > 0)
                   {
                       for (Object[] row : rows)
                       {
                           prepIdToLoanQnt.put((Integer)row[1], ((Long)row[2]).intValue());
                       }
                   }
                   
                   // Now get the Preps With Loans
                   sb = new StringBuilder();
                   sb.append("SELECT p.PreparationID, p.CountAmt, lp.ItemCount, lp.QuantityResolved, " +
                             "f.FragmentID, pt.PrepTypeID, pt.Name " +
                             "FROM preparation AS p INNER JOIN fragment AS f ON p.PreparationID = f.PreparationID " +
                             "INNER JOIN preptype AS pt ON p.PrepTypeID = pt.PrepTypeID " +
                             "LEFT OUTER JOIN loanpreparation AS lp ON p.PreparationID = lp.PreparationID " +
                             "WHERE pt.IsLoanable <> 0 AND f.FragmentID in (");
                   for (Object[] row : frIdRows)
                   {
                       sb.append(row[0]);
                       sb.append(',');
                   }
                   sb.setLength(sb.length()-1); // chomp last comma
                   sb.append(") ORDER BY f.Identifier ASC");
                   
                   // Get the Preps and Qty
                   sql = QueryAdjusterForDomain.getInstance().adjustSQL(sb.toString());
                   log.debug(sql);
                   
                   rows = BasicSQLUtils.query(sql);
                   if (rows.size() > 0)
                   {
                       for (Object[] row : rows)
                       {
                           count++;
                           if ((count % 10) == 0) firePropertyChange(PROGRESS, 0, Math.min(count, total));
                           
                           int prepId = getInt(row[0]);
                           int pQty   = getInt(row[1]);
                           int qty    = getInt(row[2]);
                           int qtyRes = getInt(row[3]);
                           int frId   = getInt(row[4]);
                           
                           prepTypeHash.put((Integer)row[5], row[6].toString());
                           
                           pQty -= getInt(prepIdToLoanQnt.get(prepId));
                           
                           ColObjInfo frObjInfo = frToPrepHash.get(frId);
                           if (frObjInfo == null)
                           {
                               // error
                           }
                           
                           PrepInfo prepInfo = frObjInfo.get(prepId);
                           if (prepInfo != null)
                           {
                               prepInfo.add(qty, qtyRes);
                           } else
                           {
                               frObjInfo.add(new PrepInfo(prepId, (Integer)row[5], pQty, qty, qtyRes));    
                           }
                       }
                   }                   
                }
            } catch (Exception ex)
            {
                ex.printStackTrace();
                edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(InteractionsProcessor.class, ex);
    
            }
            firePropertyChange(PROGRESS, 0, total);
            UIRegistry.getStatusBar().setIndeterminate(LOAN_LOADR, true);
            return 0;
        }
        
        /**
         * @return
         */
        protected int collectForGift()
        {
            int total = 0;
            int count = 0;
            try
            {
                Vector<Object[]> frIdRows = getFragmentsFromRecordSet();
                if (frIdRows.size() != 0)
                {
                    UIRegistry.getStatusBar().setProgressRange(LOAN_LOADR, 0, total);
                    
                    // Get Preps with Loans
                    StringBuilder sb = new StringBuilder();
                    sb.append("SELECT p.PreparationID, lp.ItemCount, lp.QuantityResolved " +
                             "FROM preparation AS p INNER JOIN fragment AS f ON p.PreparationID = f.PreparationID " +
                             "INNER JOIN loanpreparation AS lp ON p.PreparationID = lp.PreparationID " +
                             "WHERE f.CollectionMemberID = COLMEMID AND f.FragmentID in (");
                   for (Object[] row : frIdRows)
                   {
                       count++;
                       if ((count % 10) == 0) firePropertyChange(PROGRESS, 0, Math.min(count, total));
                       
                       Integer frId = (Integer)row[0];
                       sb.append(frId);
                       sb.append(',');
                       
                       if (row[1] != null)
                       {
                           frToPrepHash.put(frId, new ColObjInfo(frId, row[1].toString(), row.length == 3 ? row[2].toString() : null));
                       }
                   }
                   sb.setLength(sb.length()-1); // chomp last comma
                   sb.append(')');
                   
                   // Get a hash contain a mapping from PrepId to Gift Quantity
                   Hashtable<Integer, Integer> prepIdToLoanQnt = new Hashtable<Integer, Integer>();
                   
                   String sql = QueryAdjusterForDomain.getInstance().adjustSQL(sb.toString());
                   log.debug(sql);
                   
                   Vector<Object[]> rows = BasicSQLUtils.query(sql);
                   if (rows.size() > 0)
                   {
                       for (Object[] row : rows)
                       {
                           int qty    = getInt(row[1]);
                           int qtyRes = getInt(row[2]);
                           prepIdToLoanQnt.put((Integer)row[0], qty-qtyRes);
                       }
                   }
                   
                   // Now get the Preps With Gift
                   sb = new StringBuilder();
                   sb.append("SELECT p.PreparationID, p.CountAmt, gp.Quantity, " +
                             "f.FragmentID, pt.PrepTypeID, pt.Name " +
                             "FROM preparation AS p INNER JOIN fragment AS f ON p.PreparationID = f.PreparationID " +
                             "INNER JOIN preptype AS pt ON p.PrepTypeID = pt.PrepTypeID " +
                             "LEFT OUTER JOIN giftpreparation AS gp ON p.PreparationID = gp.PreparationID " +
                             "WHERE pt.IsLoanable <> 0 AND f.FragmentID in (");
                   for (Object[] row : frIdRows)
                   {
                       sb.append(row[0]);
                       sb.append(',');
                   }
                   sb.setLength(sb.length()-1); // chomp last comma
                   sb.append(") ORDER BY f.Identifier ASC");
                   
                   // Get the Preps and Qty
                   sql = QueryAdjusterForDomain.getInstance().adjustSQL(sb.toString());
                   log.debug(sql);
                   
                   rows = BasicSQLUtils.query(sql);
                   if (rows.size() > 0)
                   {
                       for (Object[] row : rows)
                       {
                           int prepId = getInt(row[0]);
                           
                           count++;
                           if ((count % 10) == 0) firePropertyChange(PROGRESS, 0, Math.min(count, total));
                           
                           int pQty   = getInt(row[1]);
                           int qty    = getInt(row[2]);
                           int coId   = getInt(row[3]);
                           
                           prepTypeHash.put((Integer)row[4], row[5].toString());
                           
                           pQty -= getInt(prepIdToLoanQnt.get(prepId));
                           
                           ColObjInfo frObjInfo = frToPrepHash.get(coId);
                           if (frObjInfo == null)
                           {
                               // error
                           }
                           
                           PrepInfo prepInfo = frObjInfo.get(prepId);
                           if (prepInfo != null)
                           {
                               prepInfo.add(qty, qty);
                           } else
                           {
                               frObjInfo.add(new PrepInfo(prepId, (Integer)row[4], pQty, qty, 0));    
                           }
                       }
                   }                   
                }
            } catch (Exception ex)
            {
                ex.printStackTrace();
                edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(InteractionsProcessor.class, ex);

            }
            firePropertyChange(PROGRESS, 0, total);
            UIRegistry.getStatusBar().setIndeterminate(LOAN_LOADR, true);
            return 0;
        }

        
        /* (non-Javadoc)
         * @see javax.swing.SwingWorker#doInBackground()
         */
        @Override
        protected Integer doInBackground() throws Exception
        {
            frToPrepHash = new Hashtable<Integer, ColObjInfo>();
            
            return isForLoan ? collectForLoan() : collectForGift();
        }

        /* (non-Javadoc)
         * @see javax.swing.SwingWorker#done()
         */
        @Override
        protected void done()
        {
            super.done();
            
            UIRegistry.getStatusBar().setProgressDone(LOAN_LOADR);
            
            if (recordSet != null) //&& recordSet.getNumItems() > 2) dl: this is commented so that the simple glass pane msg is displayed every time
            {
                UIRegistry.clearSimpleGlassPaneMsg();
            }
            
            try {
            	prepsLoaded(frToPrepHash, prepTypeHash, prepsProvider, infoRequest);
            } catch (Exception e) {
            	JOptionPane.showMessageDialog(null, "An error has occured trying to load preparations into the loan!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        
    }
    

}
