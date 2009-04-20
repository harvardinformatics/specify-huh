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
package edu.ku.brc.ui.tmanfe;

import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.ku.brc.af.core.UsageTracker;
import edu.ku.brc.ui.IconManager;
import edu.ku.brc.ui.SearchableJXTable;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.UIRegistry;
import edu.ku.brc.ui.UIHelper.OSTYPE;


/***************************************************************************************************
 * 
 * This class implements a basic spreadsheet using a JTable. It also provides a main() method to be
 * run as an application.
 * 
 * @version 1.0 July-2002
 * @author Thierry Manf, Rod Spears
 * 
 **************************************************************************************************/
public class SpreadSheet  extends SearchableJXTable implements ActionListener
{
    protected static final Logger log = Logger.getLogger(SpreadSheet.class);
    
    /**
     * Set this field to true and recompile to get debug traces
     */
    public static final boolean DEBUG = true;

    protected SpreadSheetModel   model;
    protected JScrollPane        scrollPane;
    protected JPopupMenu         popupMenu;
    protected Action             deleteAction        = null;
    
    protected boolean            useRowScrolling     = false;

    // Members needed for the RowHeader    
    protected int                rowLabelWidth       = 0;     // the width of the each row's label
    protected JPanel             rowHeaderPanel;
    protected RHCellMouseAdapter rhCellMouseAdapter;
    protected Border             cellBorder          = null;
    protected Font               cellFont;
    
    // Cell Selection
    protected boolean            mouseDown           = false;
    private boolean              rowSelectionStarted = false;

    protected SearchReplacePanel findPanel = null;
    
    // XXX Fix for Mac OS X Java 5 Bug
    protected int prevRowSelInx = -1;
    protected int prevColSelInx = -1;
    
    //no editing allowed when isReadOnly is true
    protected boolean isReadOnly = false;
    
    /**
     * Constructor for Spreadsheet from model
     * @param model
     */
    public SpreadSheet(final SpreadSheetModel model)
    {
        super(model);
        
        this.model = model;
        buildSpreadsheet();
    }
    
    /**
     * @return the Find Replace Panel.
     */
    public SearchReplacePanel getFindReplacePanel()
    {
        log.debug("Getting mySearchPanel");
        if (findPanel == null)
        {
            findPanel = new SearchReplacePanel(this);
        }
        return findPanel;
    }

    /**
     * 
     */
    protected void buildSpreadsheet()
    {
       
        this.setShowGrid(true);

        int numRows = model.getRowCount();
        
        scrollPane = new JScrollPane(this, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        final SpreadSheet ss = this;
        JButton cornerBtn = UIHelper.createIconBtn("Blank", IconManager.IconSize.Std16, 
                                                    "SelectAll", 
                                                    new ActionListener() {
            public void actionPerformed(ActionEvent ae)
            {
                ss.selectAll();
            }
        });
        cornerBtn.setEnabled(true);
        scrollPane.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, cornerBtn);
        
        // Allows row and collumn selections to exit at the same time
        setCellSelectionEnabled(true);

        setRowSelectionAllowed(true);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        addMouseListener(new MouseAdapter() {
            /* (non-Javadoc)
             * @see java.awt.event.MouseAdapter#mousePressed(java.awt.event.MouseEvent)
             */
            @SuppressWarnings("synthetic-access")
            @Override
            public void mouseReleased(MouseEvent e) 
            {
                // XXX For Java 5 Bug
                prevRowSelInx = getSelectedRow();
                prevColSelInx = getSelectedColumn();
                
                if (e.getClickCount() == 2)
                {
                    int rowIndexStart = getSelectedRow();
                    int colIndexStart = getSelectedColumn();
                    
                    ss.editCellAt(rowIndexStart, colIndexStart);
                    if (ss.getEditorComponent() != null && ss.getEditorComponent() instanceof JTextComponent)
                    {
                        ss.getEditorComponent().requestFocus();
                        
                        final JTextComponent txtComp = (JTextComponent)ss.getEditorComponent();
                        String         txt       = txtComp.getText();
                        FontMetrics    fm        = txtComp.getFontMetrics(txtComp.getFont());
                        int            x         = e.getPoint().x - ss.getEditorComponent().getBounds().x - 1;
                        int            prevWidth = 0;
                        for (int i=0;i<txt.length();i++)
                        {
                            
                            int width        = fm.stringWidth(txt.substring(0, i));
                            int basePlusHalf = prevWidth + (int)(((width - prevWidth) / 2) + 0.5);
                            //System.out.println(prevWidth + " X[" + x + "] " + width+" ["+txt.substring(0, i)+"] " + i + " " + basePlusHalf);
                            //System.out.println(" X[" + x + "] " + prevWidth + " - "+ basePlusHalf+" - " + width+" ["+txt.substring(0, i)+"] " + i);
                            if (x < width)
                            {
                                // Clearing the selection is needed for Window for some reason
                                final int inx = i + (x <= basePlusHalf ? -1 : 0);
                                SwingUtilities.invokeLater(new Runnable() {
                                    @SuppressWarnings("synthetic-access")
                                    public void run()
                                    {
                                        txtComp.setSelectionStart(0);
                                        txtComp.setSelectionEnd(0);
                                        txtComp.setCaretPosition(inx > 0 ? inx : 0);
                                    }
                                });
                                break;
                            }
                            prevWidth = width;
                        }
                    }
                }
            }
        });

        // Create a row-header to display row numbers.
        // This row-header is made of labels whose Borders,
        // Foregrounds, Backgrounds, and Fonts must be
        // the one used for the table column headers.
        // Also ensure that the row-header labels and the table
        // rows have the same height.
        
        //i have no idea WHY this has to be called.  i rearranged
        //the table and find replace panel, 
        // i started getting an array index out of
        //bounds on teh column header ON MAC ONLY.  
        //tried firing this off, first and it fixed the problem.//meg
        this.getModel().fireTableStructureChanged();
        
        TableColumn       column   = getColumnModel().getColumn(0);
        TableCellRenderer renderer = getTableHeader().getDefaultRenderer();
        if (renderer == null)
        {
            column = getColumnModel().getColumn(0);
            renderer = column.getHeaderRenderer();
        }
        
        // Calculate Row Height
        Component   cellRenderComp = renderer.getTableCellRendererComponent(this, column.getHeaderValue(), false, false, -1, 0);
        cellFont                   = cellRenderComp.getFont();
        cellBorder                 = (Border)UIManager.getDefaults().get("TableHeader.cellBorder");
        Insets      insets         = cellBorder.getBorderInsets(tableHeader);
        FontMetrics metrics        = getFontMetrics(cellFont);

        rowHeight = insets.bottom + metrics.getHeight() + insets.top;

        /*
         * Create the Row Header Panel
         */
        rowHeaderPanel = new JPanel((LayoutManager)null);
        rowLabelWidth  = metrics.stringWidth("9999") + insets.right + insets.left;
        
        Dimension dim  = new Dimension(rowLabelWidth, rowHeight * numRows);
        rowHeaderPanel.setPreferredSize(dim); // need to call this when no layout manager is used.

        rhCellMouseAdapter = new RHCellMouseAdapter(this);
        
        // Adding the row header labels
        for (int ii = 0; ii < numRows; ii++)
        {
            addRow(ii, ii+1, false);
        }

        JViewport viewPort = new JViewport();
        dim.height = rowHeight * numRows;
        viewPort.setViewSize(dim);
        viewPort.setView(rowHeaderPanel);
        scrollPane.setRowHeader(viewPort);
        
        // Experimental from the web, but I think it does the trick.
        addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (!ss.isEditing() && !e.isActionKey() && !e.isControlDown() && !e.isMetaDown() &&
                    !e.isAltDown() && 
                    e.getKeyCode() != KeyEvent.VK_SHIFT &&
                    e.getKeyCode() != KeyEvent.VK_TAB &&
                    e.getKeyCode() != KeyEvent.VK_ENTER     
                    )
                {
                    log.error("Grabbed the event as input");
                    
                    int rowIndexStart = getSelectedRow();
                    int colIndexStart = getSelectedColumn();

                    if (rowIndexStart == -1 || colIndexStart == -1)
                        return;

                    ss.editCellAt(rowIndexStart, colIndexStart);
                    Component c = ss.getEditorComponent();
                    if (c instanceof JTextComponent)
                        ((JTextComponent) c).setText("");
                }
            }
        });

        resizeAndRepaint();
        
        // Taken from a JavaWorld Example (But it works)
        KeyStroke cut   = KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false);
        KeyStroke copy  = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false);
        KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false);
        
        Action ssAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                SpreadSheet.this.actionPerformed(e);
            }
        };
        
        getInputMap().put(cut, "Cut");
        getActionMap().put("Cut", ssAction);
        
        getInputMap().put(copy, "Copy");
        getActionMap().put("Copy", ssAction);

        getInputMap().put(paste, "Paste");
        getActionMap().put("Paste", ssAction);
        
        ((JMenuItem)UIRegistry.get(UIRegistry.COPY)).addActionListener(this);
        ((JMenuItem)UIRegistry.get(UIRegistry.CUT)).addActionListener(this);
        ((JMenuItem)UIRegistry.get(UIRegistry.PASTE)).addActionListener(this);
    }
    
    /**
     * Appends a new Row onto the spreadsheet.
     * @param rowInx the last index
     * @param adjustPanelSize whether to resize the header panel
     */
    protected void addRow(final int rowInx, final int rowNum, final boolean adjustPanelSize)
    {
        RowHeaderLabel lbl = new RowHeaderLabel(rowNum, cellFont);
        lbl.setBounds(0, rowInx * rowHeight, rowLabelWidth, rowHeight);
        //System.out.println(rowNum+"  "+lbl.getBounds());
        if (UIHelper.getOSType() != UIHelper.OSTYPE.MacOSX)
        {
            lbl.setBorder(cellBorder);
        }
        lbl.addMouseListener(rhCellMouseAdapter);
        //lbl.addMouseMotionListener(rhCellMouseAdapter);
        rowHeaderPanel.add(lbl);
        
        if (adjustPanelSize)
        {
            Dimension dim = new Dimension(rowLabelWidth, rowHeight * (rowInx+1));
            rowHeaderPanel.setPreferredSize(dim);
            rowHeaderPanel.setSize(dim);
            resizeAndRepaint();
        }
    }
    
    /**
     * Appends a new row onto the Spreadsheet. 
     */
    public void addRow()
    {
        addRow(getModel().getRowCount()-1, getModel().getRowCount(), true);
        
    }
    
    /**
     * @param deleteAction the deleteAction to set
     */
    public void setDeleteAction(Action deleteAction)
    {
        this.deleteAction = deleteAction;
    }

    /**
     * Must be called AFTER the model has been adjusted.
     * @param rowInx the row index that was removed
     */
    public void removeRow(final int rowInx, final boolean doSelection)
    {
        int rowCount = getModel().getRowCount();
        
        Component comp = rowHeaderPanel.getComponent(rowCount);
        rowHeaderPanel.remove(comp);

        Dimension dim = new Dimension(rowLabelWidth, rowHeight * (rowCount));
        rowHeaderPanel.setPreferredSize(dim);
        rowHeaderPanel.setSize(dim);
        rowHeaderPanel.validate();
 
        resizeAndRepaint();
        
        if (doSelection && rowCount > 0)
        {
            if (rowInx >= rowCount)
            {
                setRowSelectionInterval(rowCount-1, rowCount-1);
            } else
            {
                setRowSelectionInterval(rowInx, rowInx);
            }
            setColumnSelectionInterval(0, getColumnCount()-1);
        }
    }

    /**
     * Scrolls to a specified row.
     * @param row the row to scroll to (zero-based)
     */
    public void scrollToRow(final int row)
    {
        Rectangle r = getCellRect(row, 0, true);
        scrollRectToVisible( r );
    }
    
    /**
     * @return an array of indexes INTO THE MODEL of the currently selected rows
     */
    public int[] getSelectedRowModelIndexes()
    {
        int[] selected = getSelectedRows();
        for (int j = 0; j < selected.length; ++j)
        {
            int modelIndex = convertRowIndexToModel(selected[j]);
            selected[j] = modelIndex;
        }
        return selected;
    }
    
    /**
     * @return an array of indexes INTO THE MODEL of the currently selected columns
     */
    public int[] getSelectedColumnModelIndexes()
    {
        int[] selected = getSelectedColumns();
        for (int j = 0; j < selected.length; ++j)
        {
            int modelIndex = convertColumnIndexToModel(selected[j]);
            selected[j] = modelIndex;
        }
        return selected;
    }
    
    
    
    /**
     * Invoked when a cell edition starts. This method overrides and calls that of its super class.
     * 
     * @param int The row to be edited
     * @param int The column to be edited
     * @param EventObject The firing event
     * @return boolean false if for any reason the cell cannot be edited.
     */
    @Override
    public boolean editCellAt(int row, int column, EventObject ev)
    {
        return mouseDown ? false : isReadOnly ? false : super.editCellAt(row, column, ev);
    }

    /**
     * Invoked by the cell editor when a cell edition stops. This method override and calls that of
     * its super class.
     * 
     */
    @Override
    public void editingStopped(ChangeEvent ev)
    {
        //_model.setDisplayMode(_editedModelRow, _editedModelCol);
        super.editingStopped(ev);
    }

    /**
     * Invoked by the cell editor when a cell edition is cancelled. This method override and calls
     * that of its super class.
     * 
     */
    @Override
    public void editingCanceled(ChangeEvent ev)
    {
        //_model.setDisplayMode(_editedModelRow, _editedModelCol);
        super.editingCanceled(ev);
    }

    /**
     * @return the scroll pane.
     */
    public JScrollPane getScrollPane()
    {
        return scrollPane;
    }
    
    /**
     * CReates the popup menu for a cell. (THis really needs to be moved outside of this class).
     * @param pnt the point to pop it up
     * @return the popup menu
     */
    protected JPopupMenu createMenuForSelection(final Point pnt)
    {
        //final int row = rowAtPoint(pnt);
        
        Class<?> cellClass = getModel().getColumnClass(convertColumnIndexToModel(columnAtPoint(pnt)));
        boolean isImage =  cellClass == ImageIcon.class || cellClass == Image.class;
        
        JPopupMenu pMenu = new JPopupMenu();
        UsageTracker.incrUsageCount("WB.SpreadsheetContextMenu");
        if (getSelectedColumnCount() == 1)
        {
            final int[] rows = getSelectedRowModelIndexes();
            if (rows.length > 1)
            {
                //if (row == rows[0])
                //{
                    if (!isImage)
                    {
                        JMenuItem mi = pMenu.add(new JMenuItem("Fill Down")); // I18N
                        mi.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent ae)
                            {
                                int selectedUICol = getSelectedColumn();
                                int selectedModelCol = convertColumnIndexToModel(selectedUICol);
                                model.fill(selectedModelCol, rows[0], rows);
                                popupMenu.setVisible(false);
                            }
                        });
                    }
                //} else if (row == rows[rows.length-1])
                //{
                    if (!isImage)
                    {
                        JMenuItem mi = pMenu.add(new JMenuItem("Fill Up")); // I18N
                        mi.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent ae)
                            {
                                int selectedUICol = getSelectedColumn();
                                int selectedModelCol = convertColumnIndexToModel(selectedUICol);
                                model.fill(selectedModelCol, rows[rows.length-1], rows);
                                popupMenu.setVisible(false);
                            }
                        });
                    }
                //}
            }
        }
        
        if (!isImage)
        {        
            JMenuItem mi = pMenu.add(new JMenuItem("Clear Cell(s)"));// I18N
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae)
                {
                    int[] rows = getSelectedRowModelIndexes();
                    int[] cols = getSelectedColumnModelIndexes();

                    model.clearCells(rows, cols);
                    popupMenu.setVisible(false);
                }
            });
        }
        
        if (deleteAction != null)
        {
            JMenuItem mi = pMenu.add(new JMenuItem("Delete Row(s)")); // I18N
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae)
                {
                    deleteAction.actionPerformed(ae);
                    popupMenu.setVisible(false);
                }
            });
        }
        pMenu.setInvoker(this);
        return pMenu;
    }

    /* (non-Javadoc)
     * @see javax.swing.JComponent#processMouseEvent(java.awt.event.MouseEvent)
     */
    @Override
    public void processMouseEvent(MouseEvent ev)
    {
        int type = ev.getID();
        //int modifiers = ev.getModifiers();
        
        mouseDown = type == MouseEvent.MOUSE_PRESSED;
        
        // XXX For Java 5 Bug
        // I am not sure if we still need this
        if (mouseDown && UIHelper.getOSType() == UIHelper.OSTYPE.MacOSX)
        {
            int rowIndexStart = rowAtPoint(ev.getPoint());
            int colIndexStart = columnAtPoint(ev.getPoint());
            
            //System.out.println(isEditing()+"  "+rowIndexStart+" "+colIndexStart+" "+prevRowSelInx+" "+prevColSelInx+" ");
            if (isEditing() && (prevRowSelInx != rowIndexStart || prevColSelInx != colIndexStart))
            {
                getCellEditor().stopCellEditing();
            }
        }
        // Done - For Java 5 Bug

        if (ev.isPopupTrigger())
        {
            // No matter what, stop editing if we are editing
            if (isEditing())
            {
                getCellEditor().stopCellEditing();
            }
            
            // Now check to see if we right clicked on a different rowor column
            boolean isOnSelectedCell = false;
            int     rowIndexStart = rowAtPoint(ev.getPoint());
            int     colIndexStart = columnAtPoint(ev.getPoint());
            
            // Check to see if we are in the selection of mulitple rows and cols
            if (getSelectedRowCount() > 0)
            {
                int[] cols = getSelectedColumns();
                for (int r : getSelectedRows())
                {
                    if (r == rowIndexStart)
                    {
                        for (int c : cols)
                        {
                            if (c == colIndexStart)
                            {
                                isOnSelectedCell = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (!isOnSelectedCell)
            {
                setRowSelectionInterval(rowIndexStart, rowIndexStart);
                setColumnSelectionInterval(colIndexStart, colIndexStart);
            }
            
            if (popupMenu != null)
            {
                popupMenu.setVisible(false);
            }

            popupMenu = createMenuForSelection(ev.getPoint());

            if (popupMenu.isVisible())
            {
                popupMenu.setVisible(false);
                
            } else
            {
                //popupMenu.setTargetCells(_selection);
                Point p = getLocationOnScreen();
                popupMenu.setLocation(p.x + ev.getX() + 1, p.y + ev.getY() + 1);
                popupMenu.setVisible(true);
            }
        }
        super.processMouseEvent(ev);
    }
    
    /* (non-Javadoc)
     * @see javax.swing.JTable#processKeyBinding(javax.swing.KeyStroke, java.awt.event.KeyEvent, int, boolean)
     */
    @Override
    protected boolean processKeyBinding(KeyStroke ks, 
                                        KeyEvent  e,
                                        int       condition, 
                                        boolean   pressed)
    {
        if (e.getKeyCode() == KeyEvent.VK_META)
        {
            return false;
        }
        return super.processKeyBinding(ks, e, condition, pressed);
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(final ActionEvent e)
    {
        if (UIRegistry.getPermanentFocusOwner() != this) // bail
        {
            return;
        }
        
        //
        // The code in this method was tken from a JavaWorld Example
        //
        boolean isCut = e.getActionCommand().compareTo("Cut") == 0 || e.getActionCommand().equals("x");
        if (e.getActionCommand().compareTo("Copy") == 0 ||
            e.getActionCommand().equals("c")|| 
            isCut)
        {
            StringBuffer sbf = new StringBuffer();
            // Check to ensure we have selected only a contiguous block of
            // cells
            int   numcols      = getSelectedColumnCount();
            int   numrows      = getSelectedRowCount();
            int[] rowsselected = getSelectedRows();
            int[] colsselected = getSelectedColumns();
            if (!((numrows - 1 == rowsselected[rowsselected.length - 1] - rowsselected[0] && numrows == rowsselected.length) && 
                  (numcols - 1 == colsselected[colsselected.length - 1] - colsselected[0] && numcols == colsselected.length)))
            {
                //JOptionPane.showMessageDialog(null, "Invalid Copy Selection",
                //        "Invalid Copy Selection", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            for (int i = 0; i < numrows; i++)
            {
                for (int j = 0; j < numcols; j++)
                {
                    sbf.append(getValueAt(rowsselected[i], colsselected[j]));
                    if (j < numcols - 1)
                    {
                        sbf.append("\t");
                    }
                    if (isCut)
                    {
                        setValueAt("", rowsselected[i], colsselected[j]);
                    }
                }
                if (numrows > 1)
                {
                    sbf.append("\n");
                }
            }
            StringSelection stsel  = new StringSelection(sbf.toString());
            Clipboard       sysClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            sysClipboard.setContents(stsel, stsel);
            
        } else if (e.getActionCommand().compareTo("Paste") == 0 ||
                   e.getActionCommand().equals("v"))
        {
            //System.out.println("Trying to Paste");
            int[] rows = getSelectedRows();
            int[] cols = getSelectedColumns();
            if (rows != null && cols != null && rows.length > 0 && cols.length > 0)
            {
                int startRow = rows[0];
                int startCol = cols[0];
                try
                {
                    Clipboard sysClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    Transferable contents = sysClipboard.getContents(null);
                    String trstring = (String)contents.getTransferData(DataFlavor.stringFlavor);
                    //String trstring = (String) (sysClipboard.getContents(this).getTransferData(DataFlavor.stringFlavor));
                    //System.out.println("String is: [" + trstring+"]");
                    StringTokenizer st1 = new StringTokenizer(trstring, "\n\r");
                    for (int i = 0; st1.hasMoreTokens(); i++)
                    {
                        String   rowstring = st1.nextToken();
                        //System.out.println("Row [" + rowstring+"]");
                        String[] tokens    = StringUtils.splitPreserveAllTokens(rowstring, '\t');
                        for (int j = 0; j < tokens.length; j++)
                        {
                            if (startRow + i < getRowCount() && startCol + j < getColumnCount())
                            {
                                int colInx = startCol + j;
                                if (tokens[j].length() <= model.getColDataLen(colInx))
                                {
                                    setValueAt(tokens[j], startRow + i, colInx);
                                } else
                                {
                                    String msg = String.format(getResourceString("UI_NEWDATA_TOO_LONG"), new Object[] { model.getColumnName(startCol + j), model.getColDataLen(colInx) } );
                                    UIRegistry.getStatusBar().setErrorMessage(msg);
                                    Toolkit.getDefaultToolkit().beep();
                                }
                            }
                            //System.out.println("Putting [" + tokens[j] + "] at row=" + startRow + i + "column=" + startCol + j);
                        }
                    }
                } catch (Exception ex)
                {
                    edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                    edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(SpreadSheet.class, ex);
                    ex.printStackTrace();
                }
            }
        }
    }
    
    /* (non-Javadoc)
     * @see javax.swing.JComponent#setVisible(boolean)
     */
    @Override
    public void setVisible(boolean flag)
    {
        scrollPane.setVisible(flag);
    }
    
    /**
     * @return
     */
    public int getMaxUnitIncrement()
    {
        if (getModel() == null)
        {
            return 0;
        }
        int cols = getModel().getColumnCount();
        if (cols > 0)
        {
            cols--;
        }
        double unit = getPreferredSize().width / cols;
        return (int)unit;
    }

    /* (non-Javadoc)
     * @see javax.swing.JTable#getScrollableBlockIncrement(java.awt.Rectangle, int, int)
     */
    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
    {
        if (useRowScrolling)
        {
            if (orientation == SwingConstants.HORIZONTAL)
            {
                return visibleRect.width - getMaxUnitIncrement();
            }
            return visibleRect.height - getMaxUnitIncrement();
        }
        
        return super.getScrollableBlockIncrement(visibleRect, orientation, direction);

    }

    /* (non-Javadoc)
     * @see javax.swing.JTable#getScrollableTracksViewportHeight()
     */
    @Override
    public boolean getScrollableTracksViewportHeight()
    {
        return useRowScrolling ? false : super.getScrollableTracksViewportHeight();
    }

    /* (non-Javadoc)
     * @see javax.swing.JTable#getScrollableTracksViewportWidth()
     */
    @Override
    public boolean getScrollableTracksViewportWidth()
    {
        return useRowScrolling ? false : super.getScrollableTracksViewportWidth();
    }

    /* (non-Javadoc)
     * @see javax.swing.JTable#getScrollableUnitIncrement(java.awt.Rectangle, int, int)
     */
    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
    {
        if (useRowScrolling)
        {
            // Get the current position.
            int currentPosition = 0;
            if (orientation == SwingConstants.HORIZONTAL)
            {
                currentPosition = visibleRect.x;
            }
            else 
            {
                currentPosition = visibleRect.y;
            }

            // Return the number of pixels between currentPosition
            // and the nearest tick mark in the indicated direction.
            if (direction < 0)
            {
                int newPosition = currentPosition - (currentPosition / getMaxUnitIncrement()) * getMaxUnitIncrement();
                return (newPosition == 0) ? getMaxUnitIncrement() : newPosition;
            }
            // else
            return ((currentPosition / getMaxUnitIncrement()) + 1) * getMaxUnitIncrement() - currentPosition;
        }
        return super.getScrollableUnitIncrement(visibleRect, orientation, direction);
    }

    /*
     * This class is used to customize the cells rendering.
     */
    public class CellRenderer extends JLabel implements TableCellRenderer
    {

        private Border      _selectBorder;
        private EmptyBorder _emptyBorder;
        //private Dimension   _dim;

        public CellRenderer()
        {
            super();
            _emptyBorder  = new EmptyBorder(2, 2, 2, 2);
            _selectBorder = new LineBorder(Color.BLUE);
            //_selectBorder = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
            //_dim = new Dimension();
            //_dim.height = 22;
            //_dim.width = 100;
            //setSize(_dim);
        }

        /**
         *
         * Method defining the renderer to be used 
         * when drawing the cells.
         *
         */
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column)
        {
            setBorder(isSelected ? _selectBorder : _emptyBorder);
            setText(value.toString());

            return this;

        }

    }

    
    //------------------------------------------------------------------------------
    //-- Inner Classes
    //------------------------------------------------------------------------------
    class RowHeaderLabel extends JComponent
    {
        protected String rowNumStr;
        protected int    rowNum;
        protected Font   font;
   
        protected int    labelWidth  = Integer.MAX_VALUE;     
        protected int    labelheight = Integer.MAX_VALUE;     
        
        public RowHeaderLabel(int rowNum, final Font font)
        {
            this.rowNum    = rowNum;
            this.rowNumStr = Integer.toString(rowNum);
            this.font      = font;
        }

        public int getRowNum()
        {
            return rowNum;
        }

        /* (non-Javadoc)
         * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
         */
        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            
            g.setFont(font);
            
            if (labelWidth == Integer.MAX_VALUE)
            {
                FontMetrics fm = getFontMetrics(font);
                labelheight = fm.getAscent();
                labelWidth  = fm.stringWidth(rowNumStr);
            }
            
            Insets    ins  = getInsets();
            Dimension size = this.getSize();
            int y = size.height - ((size.height - labelheight) / 2) - ins.bottom;
            
            g.drawString(rowNumStr, (size.width - labelWidth) / 2, y);
        }
    }
    

    /* (non-Javadoc)
     * @see javax.swing.JTable#getModel()
     */
    @Override
    public SpreadSheetModel getModel()
    {
        return (SpreadSheetModel )super.getModel();
    }
    
    /**
     * Cleans up references.
     */
    public void cleanUp()
    {
        UIHelper.removeMouseListeners(this);
        
        ((JMenuItem)UIRegistry.get(UIRegistry.COPY)).removeActionListener(this);
        ((JMenuItem)UIRegistry.get(UIRegistry.CUT)).removeActionListener(this);
        ((JMenuItem)UIRegistry.get(UIRegistry.PASTE)).removeActionListener(this);

        if (findPanel != null)
        {
            findPanel.cleanUp();
            findPanel = null;
        }
        
        if (scrollPane != null)
        {
            scrollPane.removeAll();
            scrollPane.setRowHeader(null);
        }
        
        if (rowHeaderPanel != null)
        {
            rowHeaderPanel.removeAll();
            rowHeaderPanel = null;
        }
        if (model != null)
        {
            model.cleanUp();
            /* Nulling out model (somehow) causes Null Pointer Exceptions 
             * after a WorkBench has been closed.
             * (see bugzilla number 5204
            model      = null;
            */
        }
        scrollPane = null;
        if (popupMenu != null)
        {
            popupMenu.setVisible(false);
            popupMenu = null;
        }
            
        if (rhCellMouseAdapter != null)
        {
            rhCellMouseAdapter.cleanUp();
            rhCellMouseAdapter = null;
        }
    }
    
    /**
     * MouseAdapter for selecting rows by clicking and dragging on the Row Headers.
     */
    class RHCellMouseAdapter extends MouseAdapter
    {
        protected JTable table;
        protected Hashtable<Integer, Boolean> selectionHash = new Hashtable<Integer, Boolean>();
        protected Hashtable<Integer, Boolean> doubleSelected = new Hashtable<Integer, Boolean>();
        protected int selAnchor = -1;
        protected int selLead   = -1;
        
        // these fields are important when a user ctrl-clicks a row and then drags
        protected boolean ctrlWasDown = false;
        protected boolean dragIsDeselecting = false;
        
        public RHCellMouseAdapter(final JTable table)
        {
            this.table = table;
        }

        /* (non-Javadoc)
         * @see java.awt.event.MouseAdapter#mousePressed(java.awt.event.MouseEvent)
         */
        @SuppressWarnings("synthetic-access")
        @Override
        public void mousePressed(MouseEvent e) 
        {
            log.debug("mousePressed entered");
            log.debug("anchor: " + selAnchor);
            log.debug("lead   :" + selLead);
            
            if (isEditing())
            {
                getCellEditor().stopCellEditing();
            }
            
            RowHeaderLabel lbl = (RowHeaderLabel)e.getSource();
            int            row = lbl.getRowNum()-1;
            
            // toggle the selection state of the clicked row
            // and set the current row as the new anchor
            boolean ctrlDown = false;
            if (UIHelper.getOSType() == OSTYPE.MacOSX)
            {
            	ctrlDown = e.isMetaDown();
            }
            else
            {
            	ctrlDown = e.isControlDown();
            }
            if (ctrlDown)
            {
                ListSelectionModel selModel = table.getSelectionModel();
                
                // figure out the selection state of this row
                boolean wasSelected = table.getSelectionModel().isSelectedIndex(row);
                
                // toggle the selection state of this row
                if (wasSelected)
                {
                    // deselect it
                    selModel.removeSelectionInterval(row, row);
                    dragIsDeselecting = true;
                }
                else
                {
                    // select it and make it the new anchor
                    selModel.addSelectionInterval(row, row);
                    dragIsDeselecting = false;
                }
                selAnchor = row;
                selLead   = row;
                ctrlWasDown = true;
            }
            else if (e.isShiftDown())
            {
                ListSelectionModel selModel = table.getSelectionModel();

                selModel.removeSelectionInterval(selAnchor, selLead);
                selModel.addSelectionInterval(selAnchor, row);
                selLead = row;
            }
            else // no modifier keys are down
            {
                // just select the current row
                // and set it as the new anchor
                table.setRowSelectionInterval(row, row);

                table.setColumnSelectionInterval(0, table.getColumnCount()-1);
                selAnchor = selLead = row;
                
                prevRowSelInx = getSelectedRow();
                prevColSelInx = 0;
            }
            
            rowSelectionStarted = true;
            table.getSelectionModel().setValueIsAdjusting(true);
            
            log.debug("anchor: " + selAnchor);
            log.debug("lead   :" + selLead);
            log.debug("mousePressed exited");
        }

        /* (non-Javadoc)
         * @see java.awt.event.MouseAdapter#mouseReleased(java.awt.event.MouseEvent)
         */
        @SuppressWarnings("synthetic-access")
        @Override
        public void mouseReleased(MouseEvent e) 
        {
            log.debug("mouseReleased entered");
            log.debug("anchor: " + selAnchor);
            log.debug("lead   :" + selLead);
            
            // the user has released the mouse button, so we're done selecting rows
            //RowHeaderLabel lbl = (RowHeaderLabel)e.getSource();
            //int            row = lbl.getRowNum()-1;

            rowSelectionStarted = false;
            table.getSelectionModel().setValueIsAdjusting(false);
            ctrlWasDown = false;
            dragIsDeselecting = false;
            
            log.debug("anchor: " + selAnchor);
            log.debug("lead   :" + selLead);
            log.debug("mouseReleased exited");
        }

        /* (non-Javadoc)
         * @see java.awt.event.MouseAdapter#mouseEntered(java.awt.event.MouseEvent)
         */
        @SuppressWarnings("synthetic-access")
        @Override
        public void mouseEntered(MouseEvent e) 
        {
            // the user has clicked and is dragging, we are (de)selecting multiple rows...
            if (rowSelectionStarted)
            {
                log.debug("mouseEntered entered");
                log.debug("anchor: " + selAnchor);
                log.debug("lead   :" + selLead);
                
                RowHeaderLabel lbl = (RowHeaderLabel)e.getSource();
                int row    = lbl.getRowNum()-1;
                selLead = row;
                if (ctrlWasDown)
                {
                    if (dragIsDeselecting)
                    {
                        table.removeRowSelectionInterval(selAnchor, row);
                    }
                    else
                    {
                        table.addRowSelectionInterval(selAnchor, row);
                    }
                }
                else
                {
                    table.setRowSelectionInterval(selAnchor, row);
                }
                table.setColumnSelectionInterval(0, table.getColumnCount()-1);
                log.debug("anchor: " + selAnchor);
                log.debug("lead   :" + selLead);
                log.debug("mouseEntered exited");
            }
        }
        
        /**
         * Cleans up references.
         */
        public void cleanUp()
        {
            this.table = null;
        }
    }


    /**
     * @return the isReadOnly
     */
    public boolean isReadOnly()
    {
        return isReadOnly;
    }

    /**
     * @param isReadOnly the isReadOnly to set
     */
    public void setReadOnly(boolean isReadOnly)
    {
        this.isReadOnly = isReadOnly;
    }

}
