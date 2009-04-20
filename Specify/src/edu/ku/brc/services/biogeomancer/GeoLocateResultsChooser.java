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

import static edu.ku.brc.ui.UIRegistry.getLocalizedMessage;
import static edu.ku.brc.ui.UIRegistry.getResourceString;
import static edu.ku.brc.ui.UIRegistry.getStatusBar;

import java.awt.Frame;
import java.util.List;
import java.util.Vector;

import edu.ku.brc.services.geolocate.client.GeorefResult;
import edu.ku.brc.services.geolocate.client.GeorefResultSet;
import edu.ku.brc.services.geolocate.ui.GeoLocateResultsDisplay;
import edu.ku.brc.specify.ui.HelpMgr;
import edu.ku.brc.ui.CustomDialog;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.util.Pair;

public class GeoLocateResultsChooser extends CustomDialog
{
    protected GeoLocateResultsDisplay resultsDisplayPanel = new GeoLocateResultsDisplay();
    protected List<Pair<GeoCoordDataIFace,GeorefResultSet>> rowsAndResults;
    protected List<GeorefResult> chosenResults;
    protected boolean            hasBeenShown;
    protected int                rowIndex;
    
    /**
     * @param parent
     * @param title
     * @param rowsAndResults
     */
    public GeoLocateResultsChooser(final Frame parent, 
                                   final List<Pair<GeoCoordDataIFace, GeorefResultSet>> rowsAndResults)
    {
        super(parent, "", true, CustomDialog.OKCANCELAPPLYHELP, null);
        
        this.rowsAndResults = rowsAndResults;
        this.hasBeenShown   = false;
        
        if (rowsAndResults.size() == 0)
        {
            throw new IllegalArgumentException("WorkbenchRow set must be non-empty"); //$NON-NLS-1$
        }
        
        // create a vector for all of the user choices
        chosenResults = new Vector<GeorefResult>(rowsAndResults.size());
        // make sure it's the same size as the incoming list of rows
        for (int i = 0; i < rowsAndResults.size(); ++i)
        {
            chosenResults.add(null);
        }
        
        setContentPanel(resultsDisplayPanel);
        
        this.cancelLabel = getResourceString("GeoLocateResultsChooser.SKIP"); //$NON-NLS-1$
        this.applyLabel  = getResourceString("GeoLocateResultsChooser.ACCEPT"); //$NON-NLS-1$
        this.okLabel     = getResourceString("GeoLocateResultsChooser.QUIT"); //$NON-NLS-1$
        
        rowIndex = -1;
    }
    
    public List<GeorefResult> getResultsChosen()
    {
        if (!hasBeenShown)
        {
            pack();
            setVisible(true);
        }
        
        return chosenResults;
    }
    
    @Override
    public void setVisible(boolean visible)
    {
        if (hasBeenShown == false && visible)
        {
            hasBeenShown = true;
            createUI();

            HelpMgr.registerComponent(this.helpBtn, "WorkbenchSpecialTools"); //$NON-NLS-1$

            showNextRecord();

            UIHelper.centerWindow(this);
            pack();
        }

        super.setVisible(visible);
    }

    @Override
    protected void applyButtonPressed()
    {
        // remember, we're using the 'Apply' button for "accept" to progress
        // to the next record in the list and accept the currently selected result
        
        super.applyButtonPressed();
        
        // store the user selection into the chosen results list
        GeorefResult result = resultsDisplayPanel.getSelectedResult();
        chosenResults.set(rowIndex, result);
        
        // if this was the last record, close the window
        // otherwise, move on to the next record
        if (onLastRecord())
        {
            super.okButtonPressed();
        }
        else
        {
            showNextRecord();
        }
    }

    @Override
    protected void okButtonPressed()
    {
        // remember, we're using the 'OK' button for "Dismiss" to accept the
        // currently selected result and hide the dialog

        // right now we're NOT storing the user selection when "Dismiss" is pressed
        // to enable storing of the user selection, just uncomment the following lines...
        //----------------------------------
        // store the user selection into the chosen results list
        // BioGeomancerResultStruct result = resultsDisplayPanel.getSelectedResult();
        // chosenResults.set(rowIndex, result);
        //----------------------------------
        
        super.okButtonPressed();
    }
    
    @Override
    protected void cancelButtonPressed()
    {
        // remember, we're using the 'Cancel' button for "skip" to skip the
        // currently selected result and move onto the next one

        // if this was the last record, close the window
        // otherwise, move on to the next record
        if (onLastRecord())
        {
            super.okButtonPressed();
        }
        else
        {
            showNextRecord();
        }
    }

    /**
     * 
     */
    protected void showNextRecord()
    {
        rowIndex++;
        
        // skip any records with no results
        GeorefResultSet resSet = rowsAndResults.get(rowIndex).second;
        if (resSet.getNumResults() == 0)
        {
            showNextRecord();
        }

        setTitle(getLocalizedMessage("GeoLocateResultsChooser.TITLE", (rowIndex+1), rowsAndResults.size())); //$NON-NLS-1$
        
        try
        {
            GeoCoordDataIFace item = rowsAndResults.get(rowIndex).first;
            
            resultsDisplayPanel.setGeoLocateQueryAndResults(item.getLocalityString(), 
                                                            item.getCounty(), 
                                                            item.getState(), 
                                                            item.getCountry(), 
                                                            resSet);
            resultsDisplayPanel.setSelectedResult(0);
        }
        catch (Exception e)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(GeoLocateResultsChooser.class, e);
            getStatusBar().setErrorMessage(getResourceString("GeoLocateResultsChooser.ERROR_DISPLAY_GL_RESULTS"), e);//$NON-NLS-1$
            super.setVisible(false);
        }
    }
    
    /**
     * @return
     */
    protected boolean onLastRecord()
    {
        return (rowIndex == rowsAndResults.size()-1) ? true : false;
    }
}
