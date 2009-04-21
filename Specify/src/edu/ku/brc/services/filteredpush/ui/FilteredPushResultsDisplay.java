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
package edu.ku.brc.services.filteredpush.ui;

import static edu.ku.brc.ui.UIHelper.createI18NFormLabel;
import static edu.ku.brc.ui.UIHelper.createLabel;
import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.ku.brc.services.filteredpush.FilteredPushRecordIFace;
import edu.ku.brc.services.filteredpush.FilteredPushResult;
import edu.ku.brc.ui.JStatusBar;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.UIRegistry;

public class FilteredPushResultsDisplay extends JPanel
{
    protected static final int MAP_WIDTH  = 400;
    protected static final int MAP_HEIGHT = 250;

    protected ResultsTableModel tableModel;
    protected JTable resultsTable;
    
    protected JLabel mapLabel;
    
    protected JTextField idField;
    protected JTextField barcodeField;
    protected JTextField collectorNumberField;
    protected JTextField collectorField;
    protected JTextField taxonField;
    protected JTextField localityField;
    protected JTextField latitudeField;
    protected JTextField longitudeField;
    protected JTextField serverName;

    protected JScrollPane scrollPane;
    
    /**
     * Constructor.
     */
    public FilteredPushResultsDisplay()
    {
        super();
        
        setLayout(new FormLayout("p,10px,400px,10px,C:p:g", "p,2px,p,2px,p,2px,p,2px,p,2px,p,2px,p,2px,p,2px,p,10px,p")); //$NON-NLS-1$ //$NON-NLS-2$
        
        CellConstraints cc = new CellConstraints();
        
        // add the query fields to the display
        int rowIndex = 1;

        idField               = addRow(cc, getResourceString("FilteredPushResultsDisplay.ID"),        1, rowIndex);
        rowIndex +=2;
        barcodeField          = addRow(cc, getResourceString("FilteredPushResultsDisplay.BARCODE"),   1, rowIndex);
        rowIndex += 2;
        collectorField        = addRow(cc, getResourceString("FilteredPushResultsDisplay.COLLECTOR"), 1, rowIndex);
        rowIndex += 2;
        collectorNumberField  = addRow(cc, getResourceString("FilteredPushResultsDisplay.COLL_NUM"),  1, rowIndex);
        rowIndex += 2;
        taxonField            = addRow(cc, getResourceString("FilteredPushResultsDisplay.TAXON"),     1, rowIndex);
        rowIndex += 2;
        localityField         = addRow(cc, getResourceString("FilteredPushResultsDisplay.LOCALITY"),  1, rowIndex);
        rowIndex += 2;
        latitudeField         = addRow(cc, getResourceString("FilteredPushResultsDisplay.LATITUDE"),  1, rowIndex);
        rowIndex += 2;
        longitudeField        = addRow(cc, getResourceString("FilteredPushResultsDisplay.LONGITUDE"), 1, rowIndex);
        rowIndex += 2;
        serverName            = addRow(cc, getResourceString("FilteredPushResultsDisplay.SERVER"),    1, rowIndex);
        rowIndex += 2;
        
        // add the JLabel to show the map
        mapLabel = createLabel(getResourceString("FilteredPushResultsDisplay.LOADING_MAP"));
        mapLabel.setPreferredSize(new Dimension(MAP_WIDTH, MAP_HEIGHT));
        add(mapLabel, cc.xywh(5,1,1,9));

        // add the results table
        tableModel   = new ResultsTableModel();
        resultsTable = new JTable(tableModel);
        resultsTable.setShowVerticalLines(false);
        resultsTable.setShowHorizontalLines(false);
        resultsTable.setRowSelectionAllowed(true);
        
        scrollPane = new JScrollPane(resultsTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(scrollPane, cc.xywh(1,rowIndex, 5, 1));
        rowIndex+=2;
    }
    
    public void setFilteredPushQueryAndResults(FilteredPushRecordIFace item, List<FilteredPushResult> fpResults)
    {
        String genus = item.getGenus();
        String species = item.getSpecies();
        String taxon = genus;
        if (genus != null && species != null ) taxon = genus + " " + species;
        
        idField.setText(item.getId());
        idField.setCaretPosition(0);
        collectorField.setText(item.getCollector());
        collectorField.setCaretPosition(0);
        collectorNumberField.setText(item.getCollectorNumber());
        collectorNumberField.setCaretPosition(0);
        taxonField.setText(taxon);
        taxonField.setCaretPosition(0);
        localityField.setText(item.getLocality());
        localityField.setCaretPosition(0);
        latitudeField.setText(item.getLatitude());
        latitudeField.setCaretPosition(0);
        longitudeField.setText(item.getLongitude());
        longitudeField.setCaretPosition(0);
        serverName.setText(item.getServerName());
        serverName.setCaretPosition(0);
        
        tableModel.setResultSet(fpResults);
        
        mapLabel.setText(getResourceString("FilteredPushResultsDisplay.LOADING_MAP"));
        //GeoLocate.getMapOfGeographicPoints(fpResults, FilteredPushResultsDisplay.this);

        // set the table height to at most 10 rows
        //UIHelper.calcColumnWidths(resultsTable);
        Dimension size = resultsTable.getPreferredScrollableViewportSize();
        size.height = Math.min(size.height, resultsTable.getRowHeight()*10);
        size.width  = Math.min((int) this.getBounds().getWidth(), (int) size.getWidth());
        //resultsTable.setPreferredScrollableViewportSize(size);
        resultsTable.revalidate();
    }
    
    /**
     * Returns the selected result.
     * 
     * @return the selected result
     */
    public FilteredPushResult getSelectedResult()
    {
        int rowIndex = resultsTable.getSelectedRow();
        if (rowIndex < 0 || rowIndex > tableModel.getRowCount())
        {
            return null;
        }
        
        return tableModel.getResult(rowIndex);
    }
    
    /**
     * Selects the result with the given index in the results list.
     * 
     * @param index the index of the result to select
     */
    public void setSelectedResult(int index)
    {
        if (index < 0 || index > resultsTable.getRowCount()-1)
        {
            resultsTable.clearSelection();
        }
        else
        {
            resultsTable.setRowSelectionInterval(index, index);
            int colCount = resultsTable.getColumnCount();
            resultsTable.setColumnSelectionInterval(0, colCount-1);
        }
    }


    /* (non-Javadoc)
     * @see edu.ku.brc.specify.tasks.services.LocalityMapper.MapperListener#exceptionOccurred(java.lang.Exception)
     */
    public void exceptionOccurred(Exception e)
    {
        mapLabel.setText(getResourceString("FilteredPushResultsDisplay.ERROR_GETTING_MAP"));
        JStatusBar statusBar = UIRegistry.getStatusBar();
        statusBar.setErrorMessage(getResourceString("FilteredPushResultsDisplay.ERROR_GETTING_MAP"), e);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.tasks.services.LocalityMapper.MapperListener#mapReceived(javax.swing.Icon)
     */
    public void mapReceived(Icon map)
    {
        JStatusBar statusBar = UIRegistry.getStatusBar();
        statusBar.setText(""); //$NON-NLS-1$
        mapLabel.setText(null);
        mapLabel.setIcon(map);
        repaint();
    }
    
    /**
     * Adds a new row to this object's content area.
     * 
     * @param cc the cell constraints of the new row
     * @param labelStr the text label for the new row
     * @param column the starting column number for the new row's UI
     * @param row the row number of the new row
     * @return the {@link JTextField} added to the new row
     */
    protected JTextField addRow(final CellConstraints cc,
                                final String labelStr,
                                final int column,
                                final int row)
    {
        add(createI18NFormLabel(labelStr), cc.xy(column,row)); //$NON-NLS-1$
        JTextField tf = createTextField();
        tf.setEditable(false);
        add(tf, cc.xy(column+2,row));
        return tf;
    }

    /**
     * Creates a {@link JTextField} customized for use in this UI widget.
     * 
     * @return a {@link JTextField}
     */
    protected JTextField createTextField()
    {
        JTextField tf     = UIHelper.createTextField();
        Insets     insets = tf.getBorder().getBorderInsets(tf);
        tf.setBorder(BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.bottom));
        tf.setForeground(Color.BLACK);
        tf.setBackground(Color.WHITE);
        tf.setEditable(false);
        return tf;
    }
    
    protected class ResultsTableModel extends AbstractTableModel
    {
        protected List<FilteredPushResult> results;
        
        public void setResultSet(List<FilteredPushResult> results)
        {
            this.results = results;
            fireTableDataChanged();
        }
        
        public FilteredPushResult getResult(int index)
        {
            return results.get(index);
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return String.class;
        }

        @Override
        public String getColumnName(int column)
        {
            switch (column)
            {
                case 0:
                {
                    return getResourceString("FilteredPushResultsDisplay.SERVER");
                }
                case 1:
                {
                    return getResourceString("FilteredPushResultsDisplay.ID");
                }
                case 2:
                {
                    return getResourceString("FilteredPushResultsDisplay.BARCODE");
                }
                case 3:
                {
                    return getResourceString("FilteredPushResultsDisplay.COLLECTOR");
                }
                case 4:
                {
                    return getResourceString("FilteredPushResultsDisplay.COLL_NUM");
                }
                case 5:
                {
                    return getResourceString("FilteredPushResultsDisplay.TAXON");
                }
                case 6:
                {
                    return getResourceString("FilteredPushResultsDisplay.LOCALITY");
                }
                case 7:
                {
                    return getResourceString("FilteredPushResultsDisplay.LATITUDE");
                }
                case 8:
                {
                    return getResourceString("FilteredPushResultsDisplay.LONGITUDE");
                }

            }
            return null;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return false;
        }

        public int getColumnCount()
        {
            return 9;
        }

        public int getRowCount()
        {
            return (results == null) ? 0 : results.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex)
        {
            FilteredPushResult res = results.get(rowIndex);

            switch (columnIndex)
            {
                case 0:
                {
                    return res.getServerName();
                }
                case 1:
                {
                    return rowIndex+1;
                }
                case 2:
                {
                    return res.getBarcode();
                }
                case 3:
                {
                    return res.getCollector();
                }
                case 4:
                {
                    return res.getCollectorNumber();
                }
                case 5:
                {
                    String genus = res.getGenus();
                    String species = res.getSpecies();
                    
                    if (genus != null) {
                        if (species != null) {
                            return genus + " " + species;
                        }
                        else {
                            return genus;
                        }
                    }
                    else {
                        return null;
                    }
                }
                case 6:
                {
                    return res.getLocality();
                }
                case 7:
                {
                    return res.getLatitude();
                }
                case 8:
                {
                    return res.getLongitude();
                }

            }
            return null;
        }
        
    }
}
