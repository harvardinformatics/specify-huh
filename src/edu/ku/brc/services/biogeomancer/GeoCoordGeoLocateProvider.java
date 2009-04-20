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
package edu.ku.brc.services.biogeomancer;

import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
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
import edu.ku.brc.services.geolocate.client.GeoLocate;
import edu.ku.brc.services.geolocate.client.GeorefResult;
import edu.ku.brc.services.geolocate.client.GeorefResultSet;
import edu.ku.brc.ui.IconManager;
import edu.ku.brc.ui.JStatusBar;
import edu.ku.brc.ui.ProgressDialog;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.UIRegistry;
import edu.ku.brc.util.Pair;

/**
 * This class process (retrieves) GeoRef Latitude,Longitudes from GeoLocate. This class was moved and refactored
 * from the WorkBench to be generic.
 * 
 * @author rod
 *
 * @code_status Beta
 *
 * Jan 14, 2008
 *
 */
public class GeoCoordGeoLocateProvider implements GeoCoordServiceProviderIFace
{
    private static final Logger log = Logger.getLogger(GeoCoordGeoLocateProvider.class);
    
    //protected static final String GEOLOCATE_RESULTS_VIEW_CONFIRM = UIRegistry.getString("GeoCoordGeoLocateProvider.GEOLOCATE_RESULTS_VIEW_CONFIRM"); //$NON-NLS-1$
    
    protected GeoCoordProviderListenerIFace listener    = null;
    protected String                        helpContext = null;
    
    /**
     * Constructor.
     */
    public GeoCoordGeoLocateProvider()
    {
        //This block is empty.
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.services.biogeomancer.GeoCoordServiceProviderIFace#processGeoRefData(java.util.List)
     */
    public void processGeoRefData(final List<GeoCoordDataIFace>       items, 
                                  final GeoCoordProviderListenerIFace listenerArg,
                                  final String                        helpContextArg)
    {
        this.listener    = listenerArg;
        this.helpContext = helpContextArg;
        
        UsageTracker.incrUsageCount("WB.GeoLocateRows"); //$NON-NLS-1$
        
        log.info("Performing GeoLocate lookup of selected records"); //$NON-NLS-1$
        
        // create a progress bar dialog to show the network progress
        final ProgressDialog progressDialog = new ProgressDialog(getResourceString("GeoCoordGeoLocateProvider.GEOLOC_PROGRESS"), false, true); //$NON-NLS-1$
        progressDialog.getCloseBtn().setText(getResourceString("GeoCoordGeoLocateProvider.CANCEL")); //$NON-NLS-1$
        progressDialog.setModal(true);
        progressDialog.setProcess(0, items.size());

        progressDialog.setIconImage( IconManager.getImage("AppIcon").getImage());

        // create the thread pool for doing the GEOLocate web service requests
        final ExecutorService glExecServ = Executors.newFixedThreadPool(10);
        
        // NOTE:
        // You might think to use a CompletionService to get the completed tasks, as they finish.
        // However, since we want to display the results to the user in the order they appear in the table
        // we don't want a CompletionService.  We can simply wait for each result in order.
        // See "Java Concurrency in Practice" by Brian Goetz, page 129
        // So, instead we keep a List of the Future objects as we schedule the Callable workers.
        final List<Future<Pair<GeoCoordDataIFace, GeorefResultSet>>> runningQueries = new Vector<Future<Pair<GeoCoordDataIFace, GeorefResultSet>>>();
        
        // create the thread pool for pre-caching maps
        final ExecutorService mapGrabExecServ = Executors.newFixedThreadPool(10);
        
        // create individual worker threads to do the GL queries for the rows
        for (GeoCoordDataIFace grItem: items)
        {
            final GeoCoordDataIFace item = grItem;
            
            // create a background thread to do the web service work
            Callable<Pair<GeoCoordDataIFace, GeorefResultSet>> wsClientWorker = new Callable<Pair<GeoCoordDataIFace, GeorefResultSet>>()
            {
                @SuppressWarnings("synthetic-access") //$NON-NLS-1$
                public Pair<GeoCoordDataIFace, GeorefResultSet> call() throws Exception
                {
                    // get the locality data
                    String localityNameStr = item.getLocalityString();
                            
                    // get the geography data
                    String country = item.getCountry();
                    String state   = item.getState();
                    String county  = item.getCounty();
                    
                    country = country == null ? "" : country;
                    state   = state   == null ? "" : state;
                    county  = county  == null ? "" : county;
                    
                    // make the web service request
                    log.info("Making call to GEOLocate web service: " + localityNameStr); //$NON-NLS-1$
                    final GeorefResultSet glResults = GeoLocate.getGeoLocateResults(country, state, county, localityNameStr);

                    // update the progress bar
                    SwingUtilities.invokeLater(new Runnable()
                    {
                       public void run()
                       {
                           int progress = progressDialog.getProcess();
                           progressDialog.setProcess(++progress);
                       }
                    });

                    // if there was at least one result, pre-cache a map for that result
                    if (glResults != null && glResults.getNumResults() > 0)
                    {
                        Runnable mapPreCacheTask = new Runnable()
                        {
                            public void run()
                            {
                                try
                                {
                                    int rowNumber = item.getGeoCoordId();
                                    log.info("Requesting map of GEOLocate results for workbench row " + rowNumber); //$NON-NLS-1$
                                    GeoLocate.getMapOfGeographicPoints(glResults.getResultSet(), null);
                                }
                                catch (Exception e)
                                {
                                    UsageTracker.incrHandledUsageCount();
                                    edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(GeoCoordGeoLocateProvider.class, e);
                                    log.warn("Failed to pre-cache GEOLocate results map",e); //$NON-NLS-1$
                                }
                            }
                        };
                        mapGrabExecServ.execute(mapPreCacheTask);
                    }

                    return new Pair<GeoCoordDataIFace, GeorefResultSet>(item, glResults);
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
                final List<Pair<GeoCoordDataIFace, GeorefResultSet>> glResults = new Vector<Pair<GeoCoordDataIFace, GeorefResultSet>>();
                
                // iterrate over the set of queries, asking for the result
                // this will basically block us right here until all of the queries are completed
                for (Future<Pair<GeoCoordDataIFace, GeorefResultSet>> completedQuery: runningQueries)
                {
                    try
                    {
                        glResults.add(completedQuery.get());
                    }
                    catch (InterruptedException e)
                    {
                        // ignore this query since results were not available
                        log.warn("Process cancelled by user",e); //$NON-NLS-1$
                        mapGrabExecServ.shutdown();
                        return;
                    }
                    catch (ExecutionException e)
                    {
                        // ignore this query since results were not available
                        log.error(completedQuery.toString() + " had an execution error",e); //$NON-NLS-1$
                    }
                }
                
                // do the UI work to show the results
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        progressDialog.setVisible(false);
                        displayGeoLocateResults(glResults, items);
                        mapGrabExecServ.shutdown();
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
                mapGrabExecServ.shutdownNow();
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
    protected void displayGeoLocateResults(final List<Pair<GeoCoordDataIFace, GeorefResultSet>> glResults,
                                           final List<GeoCoordDataIFace> items)
    {
        final JStatusBar statusBar = UIRegistry.getStatusBar();
        
        statusBar.setText(getResourceString("GeoCoordGeoLocateProvider.GEOLOC_COMPLETED")); //$NON-NLS-1$
        
        List<Pair<GeoCoordDataIFace, GeorefResultSet>> withResults = new Vector<Pair<GeoCoordDataIFace, GeorefResultSet>>();

        for (Pair<GeoCoordDataIFace, GeorefResultSet> result: glResults)
        {
            if (result.second.getNumResults() > 0)
            {
                withResults.add(result);
            }
        }
        
        if (withResults.size() == 0)
        {
            statusBar.setText(getResourceString("GeoCoordGeoLocateProvider.NO_GL_RESULTS")); //$NON-NLS-1$
            JOptionPane.showMessageDialog(UIRegistry.getTopWindow(),
                    getResourceString("GeoCoordGeoLocateProvider.NO_GL_RESULTS"), //$NON-NLS-1$
                    getResourceString("NO_RESULTS"), JOptionPane.INFORMATION_MESSAGE); //$NON-NLS-1$

            return;
        }
        
        if (listener != null)
        {
            listener.aboutToDisplayResults();
        }
        
        // ask the user if they want to review the results
        String message = String.format(getResourceString("GeoCoordGeoLocateProvider.GEOLOCATE_RESULTS_VIEW_CONFIRM"), String.valueOf(withResults.size())); //$NON-NLS-1$
        int userChoice = JOptionPane.showConfirmDialog(UIRegistry.getTopWindow(), message,
                getResourceString("GeoCoordGeoLocateProvider.GEO_CONTINUE"), JOptionPane.YES_NO_OPTION); //$NON-NLS-1$
        
        if (userChoice != JOptionPane.YES_OPTION)
        {
            statusBar.setText(getResourceString("GeoCoordGeoLocateProvider.USER_TERMINATED")); //$NON-NLS-1$
            return;
        }

        // create the UI for displaying the BG results
        JFrame topFrame = (JFrame)UIRegistry.getTopWindow();
        GeoLocateResultsChooser bgResChooser = new GeoLocateResultsChooser(topFrame, withResults); //$NON-NLS-1$
        
        List<GeorefResult> results = bgResChooser.getResultsChosen();
        
        int itemsUpdated = 0;
        
        for (int i = 0; i < results.size(); ++i)
        {
            GeoCoordDataIFace item = withResults.get(i).first;
            GeorefResult chosenResult = results.get(i);
            
            if (chosenResult != null)
            {
                Double latitude = chosenResult.getWGS84Coordinate().getLatitude();
                Double longitude = chosenResult.getWGS84Coordinate().getLongitude();
                item.set(String.format("%7.5f", latitude), String.format("%7.5f", longitude)); //$NON-NLS-1$ //$NON-NLS-2$
                
                itemsUpdated++;
            }
        }
        
        if (listener != null)
        {
            listener.complete(items, itemsUpdated);
        }
    }
}
