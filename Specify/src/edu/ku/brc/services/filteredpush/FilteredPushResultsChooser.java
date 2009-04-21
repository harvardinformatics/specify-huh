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

import java.awt.Frame;
import java.util.List;
import java.util.Vector;

import edu.ku.brc.services.filteredpush.ui.FilteredPushResultsDisplay;
import edu.ku.brc.specify.ui.HelpMgr;
import edu.ku.brc.ui.CustomDialog;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.UIRegistry;
import edu.ku.brc.util.Pair;

public class FilteredPushResultsChooser extends CustomDialog
{
    protected FilteredPushResultsDisplay resultsDisplayPanel = new FilteredPushResultsDisplay();
    List<Pair<FilteredPushRecordIFace, List<FilteredPushResult>>> rowsAndResults;
    protected List<FilteredPushResult> chosenResults;
    protected boolean            hasBeenShown;
    protected int                rowIndex;
    protected String             baseTitle;
    
    /**
     * @param parent
     * @param title
     * @param rowsAndResults
     */
    public FilteredPushResultsChooser(final Frame parent, 
                                   final String title, 
                                   final List<Pair<FilteredPushRecordIFace, List<FilteredPushResult>>> rowsAndResults)
    {
        super(parent, title, true, CustomDialog.OKCANCELAPPLYHELP, null);
        
        this.rowsAndResults = rowsAndResults;
        this.hasBeenShown   = false;
        this.baseTitle      = title;
        
        if (rowsAndResults.size() == 0)
        {
            throw new IllegalArgumentException("WorkbenchRow set must be non-empty"); //$NON-NLS-1$
        }
        
        // create a vector for all of the user choices
        chosenResults = new Vector<FilteredPushResult>(rowsAndResults.size());
        // make sure it's the same size as the incoming list of rows
        for (int i = 0; i < rowsAndResults.size(); ++i)
        {
            chosenResults.add(null);
        }
        
        setContentPanel(resultsDisplayPanel);
        
        this.cancelLabel = getResourceString("FilteredPushResultsChooser.SKIP");
        this.applyLabel  = getResourceString("FilteredPushResultsChooser.ACCEPT");
        this.okLabel     = getResourceString("FilteredPushResultsChooser.QUIT");
        
        rowIndex = -1;
    }
    
    public List<FilteredPushResult> getResultsChosen()
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
        FilteredPushResult result = resultsDisplayPanel.getSelectedResult();
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
        List<FilteredPushResult> resSet = rowsAndResults.get(rowIndex).second;
        if (resSet.size() == 0)
        {
            showNextRecord();
        }

        setTitle(baseTitle + ": " + (rowIndex+1) + " " + getResourceString("of") + " " + rowsAndResults.size()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //TODO
        
        try
        {
            FilteredPushRecordIFace item = rowsAndResults.get(rowIndex).first;
                        
            resultsDisplayPanel.setFilteredPushQueryAndResults(item, resSet);
            resultsDisplayPanel.setSelectedResult(0);
        }
        catch (Exception e)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(FilteredPushResultsChooser.class, e);
            UIRegistry.getStatusBar().setErrorMessage(getResourceString("FilteredPushResultsChooser.ERROR_DISPLAY_FP_RESULTS"), e);
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
