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
package edu.ku.brc.services.filteredpush;

import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import edu.ku.brc.af.core.UsageTracker;
import edu.ku.brc.services.biogeomancer.GeoCoordDataIFace;
import edu.ku.brc.services.biogeomancer.GeoCoordGeoLocateProvider;
import edu.ku.brc.services.biogeomancer.GeoLocateResultsChooser;
import edu.ku.brc.services.geolocate.client.GeorefResult;
import edu.ku.brc.services.geolocate.client.GeorefResultSet;
import edu.ku.brc.specify.datamodel.WorkbenchDataItem;
import edu.ku.brc.specify.datamodel.WorkbenchRow;
import edu.ku.brc.specify.datamodel.WorkbenchTemplateMappingItem;
import edu.ku.brc.ui.JStatusBar;
import edu.ku.brc.ui.ProgressDialog;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.UIRegistry;
import edu.ku.brc.util.Pair;


public class FilteredPushServiceProvider implements FilteredPushServiceProviderIFace
{
    private static final Logger log = Logger.getLogger(GeoCoordGeoLocateProvider.class);
    
    //protected static final String GEOLOCATE_RESULTS_VIEW_CONFIRM = UIRegistry.getString("GeoCoordGeoLocateProvider.GEOLOCATE_RESULTS_VIEW_CONFIRM"); //$NON-NLS-1$
    
    protected FilteredPushProviderListenerIFace listener = null;
    protected String                         helpContext = null;
    
    /**
     * Constructor.
     */
    public FilteredPushServiceProvider()
    {
        // empty block
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.services.biogeomancer.GeoCoordServiceProviderIFace#processGeoRefData(java.util.List)
     */
    public void processFilteredPushData(final List<WorkbenchRow>       items, 
                                  final FilteredPushProviderListenerIFace listenerArg,
                                  final String                        helpContextArg)
    {
        this.listener    = listenerArg;
        this.helpContext = helpContextArg;
        
        UsageTracker.incrUsageCount("WB.GeoLocateRows"); //$NON-NLS-1$ // TODO
        
        log.info("Performing Filtered Push query of selected records"); //$NON-NLS-1$
        
        // create a progress bar dialog to show the network progress
        final ProgressDialog progressDialog = new ProgressDialog(getResourceString("FilteredPushProvider.QUERY_PROGRESS"), false, true);
        progressDialog.getCloseBtn().setText(getResourceString("FilteredPushProvider.CANCEL"));
        progressDialog.setModal(true);
        progressDialog.setProcess(0, items.size());

        // XXX Java 6
        //progressDialog.setIconImage( IconManager.getImage("AppIcon").getImage());

        // create the thread pool for doing the GEOLocate web service requests
        final ExecutorService glExecServ = Executors.newFixedThreadPool(10);
        
        // NOTE:
        // You might think to use a CompletionService to get the completed tasks, as they finish.
        // However, since we want to display the results to the user in the order they appear in the table
        // we don't want a CompletionService.  We can simply wait for each result in order.
        // See "Java Concurrency in Practice" by Brian Goetz, page 129
        // So, instead we keep a List of the Future objects as we schedule the Callable workers.
        final List<Future<Pair<FilteredPushRecordIFace, List<FilteredPushResult>>>> runningQueries = new Vector<Future<Pair<FilteredPushRecordIFace, List<FilteredPushResult>>>>();
        
        // create the thread pool for pre-caching maps
        final ExecutorService fpQueryExecServ = Executors.newFixedThreadPool(10);
        
        // create individual worker threads to do the GL queries for the rows
        for (WorkbenchRow grItem: items)
        {
            final WorkbenchRow item = grItem;
            final SpecifyFPRecord fpItem = new SpecifyFPRecord(grItem);

            final List<Pair<String, String>> conditions = new ArrayList<Pair<String, String>>();

            String collector = fpItem.getCollector();
            if (collector != null) {
                Pair<String, String> collectorCondition = new Pair<String, String>();
                collectorCondition.setFirst(FilteredPush.fpCollectorFieldName);
                collectorCondition.setSecond(collector);
                conditions.add(collectorCondition);
            }

            String collectorNumber = fpItem.getCollectorNumber();
            if (collectorNumber != null) {
                Pair<String, String> collNumCondition = new Pair<String, String>();
                collNumCondition.setFirst(FilteredPush.fpCollectorNumberFieldName);
                collNumCondition.setSecond(collectorNumber);
                conditions.add(collNumCondition);
            }
            
            // create a background thread to do the web service work
            Callable<Pair<FilteredPushRecordIFace, List<FilteredPushResult>>> wsClientWorker = new Callable<Pair<FilteredPushRecordIFace, List<FilteredPushResult>>>()
            {
                @SuppressWarnings("synthetic-access") //$NON-NLS-1$
                public Pair<FilteredPushRecordIFace, List<FilteredPushResult>> call() throws Exception
                {
                    // make the web service request
                    log.info("Making call to Filtered Push service"); //$NON-NLS-1$
                    String response = FilteredPush.getFilteredPushResponse(conditions);
                    List<FilteredPushResult> fpResult = FilteredPush.parse(response);

                    System.err.println("fpResults: " + fpResult.size());
                    for (FilteredPushResult fpr : fpResult)
                    {
                        System.err.print("id=" + fpr.getId() + "\t" + "barcode=" + fpr.getBarcode() + "\t" + "coll=" + fpr.getCollector() + "\t");
                        System.err.print("collNo=" + fpr.getCollectorNumber() + "\t" + "sp=" + fpr.getSpecies() + "\t" + "gen=" + fpr.getGenus() + "\n"); 
                    }
                    // update the progress bar
                    SwingUtilities.invokeLater(new Runnable()
                    {
                       public void run()
                       {
                           int progress = progressDialog.getProcess();
                           progressDialog.setProcess(++progress);
                       }
                    });

                    return new Pair<FilteredPushRecordIFace, List<FilteredPushResult>>(fpItem, fpResult); // TODO
                }
            };
            
            runningQueries.add(glExecServ.submit(wsClientWorker));
        }
        
        // shut down the ExecutorService
        // this will run all of the task that have already been submitted
        glExecServ.shutdown();
        
        // this thread simply gets the 'waiting for all results' part off of the Swing thread
        final Thread waitingForExecutors = new Thread(new Runnable()
        {
            @SuppressWarnings("synthetic-access") //$NON-NLS-1$
            public void run()
            {
                // a big list of the query results
                final List<Pair<FilteredPushRecordIFace, List<FilteredPushResult>>> fpResults = new Vector<Pair<FilteredPushRecordIFace, List<FilteredPushResult>>>();
                
                // iterrate over the set of queries, asking for the result
                // this will basically block us right here until all of the queries are completed
                for (Future<Pair<FilteredPushRecordIFace, List<FilteredPushResult>>> completedQuery: runningQueries)
                {
                    try
                    {
                        fpResults.add(completedQuery.get());
                    }
                    catch (InterruptedException e)
                    {
                        UsageTracker.incrHandledUsageCount();
                        edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(GeoCoordGeoLocateProvider.class, e);
                        // ignore this query since results were not available
                        log.warn("Process cancelled by user",e); //$NON-NLS-1$
                        fpQueryExecServ.shutdown();
                        return;
                    }
                    catch (ExecutionException e)
                    {
                        UsageTracker.incrHandledUsageCount();
                        edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(GeoCoordGeoLocateProvider.class, e);
                        // ignore this query since results were not available
                        log.error(completedQuery.toString() + " had an execution error", e); //$NON-NLS-1$
                    }
                }
                
                // do the UI work to show the results
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        progressDialog.setVisible(false);
                        displayFilteredPushResults(fpResults); 
                        fpQueryExecServ.shutdown();
                    }
                });
            }
        });
        waitingForExecutors.setName("GEOLocate UI update thread"); //$NON-NLS-1$
        waitingForExecutors.start();
        
        // if the user hits close, stop the worker thread
        progressDialog.getCloseBtn().addActionListener(new ActionListener()
        {
            @SuppressWarnings("synthetic-access") //$NON-NLS-1$
            public void actionPerformed(ActionEvent ae)
            {
                log.debug("Stopping the GEOLocate service worker threads"); //$NON-NLS-1$
                glExecServ.shutdownNow();
                fpQueryExecServ.shutdownNow();
                waitingForExecutors.interrupt();
            }
        });

        // popup the progress dialog
        UIHelper.centerAndShow(progressDialog);
    }
    
    /**
     * Create a dialog to display the set of rows that had at least one result option
     * returned by GEOLocate.  The dialog allows the user to iterate through the
     * records supplied, choosing a result (or not) for each one.
     * 
     * @param rows the set of records containing valid GEOLocate responses with at least one result
     */
    protected void displayFilteredPushResults(final List<Pair<FilteredPushRecordIFace, List<FilteredPushResult>>> fpResults)
    {
        final JStatusBar statusBar = UIRegistry.getStatusBar();
        
        statusBar.setText(getResourceString("FilteredPushResultsProvider.FP_QUERY_COMPLETED"));
        
        List<Pair<FilteredPushRecordIFace, List<FilteredPushResult>>> withResults = new Vector<Pair<FilteredPushRecordIFace, List<FilteredPushResult>>>();

        for (Pair<FilteredPushRecordIFace, List<FilteredPushResult>> result: fpResults)
        {
            if (result.second.size() > 0)
            {
                withResults.add(result);
            }
        }
        
        if (withResults.size() == 0)
        {
            statusBar.setText(getResourceString("FilteredPushProvider.NO_FP_RESULTS"));
            JOptionPane.showMessageDialog(UIRegistry.getTopWindow(),
                    getResourceString("FilteredPushProvider.NO_FP_RESULTS"),
                    getResourceString("NO_RESULTS"), JOptionPane.INFORMATION_MESSAGE);

            return;
        }
        
        if (listener != null)
        {
            listener.aboutToDisplayResults();
        }
        
        // ask the user if they want to review the results
        String message = String.format(getResourceString("FilteredPushProvider.FP_RESULTS_VIEW_CONFIRM"), String.valueOf(withResults.size()));
        int userChoice = JOptionPane.showConfirmDialog(UIRegistry.getTopWindow(), message,
                getResourceString("FilteredPushProvider.FP_CONTINUE"), JOptionPane.YES_NO_OPTION);
        
        if (userChoice != JOptionPane.YES_OPTION)
        {
            statusBar.setText(getResourceString("FilteredPushProvider.USER_TERMINATED"));
            return;
        }

        // create the UI for displaying the BG results
        JFrame topFrame = (JFrame)UIRegistry.getTopWindow();
        FilteredPushResultsChooser fpResChooser = new FilteredPushResultsChooser(topFrame, 
                getResourceString("FilteredPushResultsProvider.FP_RES_CHOOSER_TITLE"), withResults);

        // TODO: here down
        List<FilteredPushResult> results = fpResChooser.getResultsChosen();

        int itemsUpdated = 0;
        
        for (int i = 0; i < results.size(); ++i)
        {
            FilteredPushRecordIFace item = withResults.get(i).first;  // here is where we need a connection to the workbench object
            
            FilteredPushResult chosenResult = results.get(i);
            
            if (chosenResult != null)
            {
                item.setCollector(chosenResult.getCollector());
                item.setCollectorNumber(chosenResult.getCollectorNumber());
                item.setGenus(chosenResult.getGenus());
                item.setSpecies(chosenResult.getSpecies());
                item.setLocality(chosenResult.getLocality());
                item.setLatitude(chosenResult.getLatitude());
                item.setLongitude(chosenResult.getLongitude());

                itemsUpdated++;
            }
        }
        
        if (listener != null)
        {
            listener.complete(itemsUpdated);
        }
    }
}
