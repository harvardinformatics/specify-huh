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
package edu.ku.brc.specify.ui.db;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;

import edu.ku.brc.af.prefs.AppPreferences;
import edu.ku.brc.af.prefs.AppPrefsCache;
import edu.ku.brc.af.ui.db.ERTICaptionInfo;
import edu.ku.brc.af.ui.db.QueryForIdResultsIFace;
import edu.ku.brc.af.ui.forms.DataObjectSettable;
import edu.ku.brc.af.ui.forms.DataObjectSettableFactory;
import edu.ku.brc.af.ui.forms.FormHelper;
import edu.ku.brc.af.ui.forms.formatters.DataObjFieldFormatMgr;
import edu.ku.brc.af.ui.forms.formatters.DataObjSwitchFormatter;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
import edu.ku.brc.dbsupport.CustomQueryIFace;
import edu.ku.brc.dbsupport.CustomQueryListener;
import edu.ku.brc.dbsupport.JPAQuery;
import edu.ku.brc.dbsupport.RecordSetIFace;
import edu.ku.brc.dbsupport.SQLExecutionListener;
import edu.ku.brc.dbsupport.SQLExecutionProcessor;
import edu.ku.brc.specify.datamodel.RecordSet;
import edu.ku.brc.specify.tasks.ExpressSearchTask;
import edu.ku.brc.specify.tasks.subpane.ESResultsTablePanelIFace;
import edu.ku.brc.ui.CommandAction;
import edu.ku.brc.ui.CommandDispatcher;
import edu.ku.brc.ui.DateWrapper;
import edu.ku.brc.ui.JStatusBar;
import edu.ku.brc.ui.UIRegistry;

/**
 * @code_status Alpha
 *
 * @author rods
 *
 */
@SuppressWarnings("serial")
public class ResultSetTableModel extends AbstractTableModel implements SQLExecutionListener, CustomQueryListener
{
    // Static Data Members
    private static final Logger log = Logger.getLogger(ResultSetTableModel.class);
    
    protected static DateWrapper scrDateFormat = AppPrefsCache.getDateWrapper("ui", "formatting", "scrdateformat");
    
    protected static int VISIBLE_ROWS = 10; // XXX Got get this from elsewhere
    
    // Data Members    
    protected ESResultsTablePanelIFace    parentERTP;
    protected Vector<Class<?>>            classNames  = new Vector<Class<?>>();
    protected Vector<String>              colNames    = new Vector<String>();
    protected int                         currentRow  = 0;
    protected QueryForIdResultsIFace      results;
    protected boolean                     doSequentially;
    protected int                         numColumns  = 0;
    protected Vector<Integer>             ids         = null;  // Must be initialized to null!
    protected Vector<Vector<Object>>      cache       = new Vector<Vector<Object>>();
    protected List<ERTICaptionInfo>       captionInfo = null;
    protected int[]                       columnIndexMapper = null;
    
    protected PropertyChangeListener      propertyListener  = null;
    
    protected JStatusBar                  statusBar         = UIRegistry.getStatusBar();
    protected boolean                     doDebug           = AppPreferences.getLocalPrefs().getBoolean("esdebug", false);
    
    // Frame buffer
    protected int startInx = 0;
    protected int endInx   = 0;
    
    protected boolean useColOffset = false;
    
    /**
     * Construct with a QueryForIdResultsIFace
     * @param parentERTP
     * @param results
     */
    public ResultSetTableModel(final ESResultsTablePanelIFace parentERTP,
                               final QueryForIdResultsIFace results)
    {
        this(parentERTP, results, false);
    }
    
    /**
     * @param parentERTP
     * @param results
     * @param doSequentially
     */
    public ResultSetTableModel(final ESResultsTablePanelIFace parentERTP,
                               final QueryForIdResultsIFace results,
                               final boolean doSequentially,
                               final boolean doSelfStart)
    {
        this.parentERTP = parentERTP;
        this.results = results;
        this.doSequentially = doSequentially;
        
        captionInfo = results.getVisibleCaptionInfo();
        
        if (!results.showProgress())
        {
            statusBar = null;
        }
        
        initialize();
      
        if (doSelfStart)
        {
        	startDataAquisition(doSequentially);
        }
    }
    
    /**
     * @param parentERTP
     * @param results
     * @param doSequentially
     */
    public ResultSetTableModel(final ESResultsTablePanelIFace parentERTP,
            final QueryForIdResultsIFace results,
            final boolean doSequentially)
    {
    	this(parentERTP, results, doSequentially, true);
    }
    
    /**
     * perform initializations which must be performed before startDataAcquisition() is called.
     */
    protected void initialize()
    {
    	//nothing to do here
    }
    
    public void startDataAcquisition()
    {
    	startDataAquisition(doSequentially);
    }
    /**
     * @param doSequentiallyArg
     */
    protected void startDataAquisition(final boolean doSequentiallyArg)
    {
        //System.out.println("\n"+results.getTitle()+" " +results.isHQL());
        if (statusBar != null)
        {
            statusBar.incrementRange(getClass().getSimpleName());
        }
        
        if (results.isHQL())
        {
            List<ERTICaptionInfo> captions = results.getVisibleCaptionInfo();
            numColumns = captions.size();
            for (ERTICaptionInfo caption : captions)
            {
                 classNames.addElement(caption.getColClass());
                 colNames.addElement(caption.getColName());
            }
            
            JPAQuery jpaQuery = null;
            String   sqlStr   = results.getSQL(results.getSearchTerm(), ids);
            if (sqlStr != null)
            {
                jpaQuery = new JPAQuery(sqlStr, this);
                jpaQuery.setParams(results.getParams());
                if (doSequentiallyArg)
                {
                    jpaQuery.execute();
                } else
                {
                    results.setQueryTask(jpaQuery.start());
                }
            }
            
        } else
        {
            SQLExecutionProcessor sqlProc = new SQLExecutionProcessor(this, results.getSQL(results.getSearchTerm(), ids));
            if (true) // mmk: was "if(doSequentially), but this causes problems with the ui components being accessed in unexpected ways
            {
                sqlProc.execute();
                
            } else
            {
                sqlProc.start();
            }
        }
    }
    
    /**
     * Cleans up internal data members.
     */
    public void cleanUp()
    {
        parentERTP = null;
        results    = null;
        propertyListener = null;
        
        for (Vector<Object> list : cache)
        {
            list.clear(); 
        }
        cache.clear();
        
        if (ids != null)
        {
            ids.clear();
        }
        
    }

    /**
     * Returns the number of columns
     * @return Number of columns
     */
    public int getColumnCount()
    {
        if (captionInfo != null)
        {
            return captionInfo.size();
        }
        return numColumns;
    }

    /**
     * Returns the Class object for a column
     * @param column the column in question
     * @return the Class of the column
     */
    @Override
    public Class<?> getColumnClass(int column)
    {
        /*
         * Modified this to fix a bug that (so far) had only shown itself for Timestamp columns.
         * 
         * ResultSetTableModel.getValueAt(row, column) returns a formatted object for the column.
         * 
         * Then in the JTable display code, JTable.getCellRenderer calls getDefaultRenderer(getColumnClass())
         * Which returns a formatter for columnClass and trie to format the already-formatted value
         * from getValueAt(). Which actually works out (except the "centered" property is sometimes overridden
         * for Numbers) for all classes (usually a default formatter Object is eventually returned) except
         * java.sql.Timestamp. 
         * 
         * Everything seems OK now.
         * 
         */
        return Object.class; //whatever getValueAt(?, column) returns.
    }
    
    protected Class<?> getColumnClass2(int column)
    {
        if (captionInfo != null)
        {
            if (captionInfo.get(column).getColClass() == null)
            {
                return String.class;
            }
            //log.debug(captionInfo.get(column).getColClass().getName());
            return captionInfo.get(column).getColClass();
        }
        
        if (classNames.size() > 0)
        {
            Class<?> classObj = classNames.elementAt(column);
            
            if (classObj == Calendar.class || classObj == java.sql.Date.class || classObj == Date.class || classObj == Timestamp.class)
            {
                return String.class;
            }
            return classObj;
        }
        return String.class;
    }

    /**
     * Get the column name
     * @param column the column of the cell to be gotten
     */
    @Override
    public String getColumnName(int column)
    {
        if (captionInfo != null)
        {
            return captionInfo.get(column).getColLabel();
        }

        if (column > -1 && column < colNames.size())
        {
            return colNames.get(column);
        }

        return "N/A";
    }

    
    /**
     * Gets the 'raw' value of the row col.
     * @param rowArg the row of the cell to be gotten
     * @param colArg the column of the cell to be gotten
     */
    public Object getCacheValueAt(final int row, final int column)
    {
        if (row > -1 && row < cache.size())
        {
            Vector<Object> rowArray = cache.get(row);
            if (column > -1 && column < rowArray.size())
            {
                return rowArray.get(column);
            }
        }
        return "No Data";
    }
    /**
     * Gets the value of the row col.
     * @param rowArg the row of the cell to be gotten
     * @param colArg the column of the cell to be gotten
     */
    public Object getValueAt(final int row, final int column)
    {
        if (row > -1 && row < cache.size())
        {
            Vector<Object> rowArray = cache.get(row);
            if (column > -1 && column < rowArray.size())
            {
                Object obj = rowArray.get(column);
                
                Class<?> dataClassObj = getColumnClass2(column); //see comment for getColumnClass().
                if (obj == null && (dataClassObj == null || dataClassObj == String.class))
                {
                    return "";
                }
                
                if (obj instanceof Calendar)
                {
                    return scrDateFormat.format((Calendar)obj);
                    
                } else if (obj instanceof Timestamp )
                {
                    return scrDateFormat.format((Date)obj);
                } else if (obj instanceof java.sql.Date || obj instanceof Date )
                {
                    return scrDateFormat.format((Date)obj);
                    
                } else if (obj instanceof Boolean )
                {
                    return UIRegistry.getResourceString(obj.toString());
                }
                
                //System.out.println(row+" "+column+" ["+obj+"] "+getColumnClass(column).getSimpleName() + " " + useColOffset);
                
                UIFieldFormatterIFace formatter = captionInfo != null ? captionInfo.get(column).getUiFieldFormatter() : null;
                if (formatter != null && formatter.isInBoundFormatter())
                {
                    return formatter.formatToUI(obj);
                }
                
                return obj;
            }
        }

        return "No Data";
    }

    /**
     * Sets a new value into the Model
     * @param aValue the value to be set
     * @param row the row of the cell to be set
     * @param column the column of the cell to be set
     */
    @Override
    public void setValueAt(Object aValue, int row, int column)
    {
        // empty
    }

    /**
     * Returns the number of rows
     * @return Number of rows
     */
    public int getRowCount()
    {
        return cache.size();
    }
    
    /**
     * @param index
     * @return
     */
    public Integer getRowId(final int index)
    {
        return ids.get(index);
    }
    
    /**
     * Removes a row from the model.
     * @param index the index to be removed.
     */
    public void removeRow(final int index)
    {
        cache.remove(index);
        ids.remove(index);
        fireTableRowsDeleted(index, index);
    }
    
    /**
     * @return the results
     */
    public QueryForIdResultsIFace getQueryForIdResults()
    {
        return results;
    }

    /**
     * Returns a RecordSet object from the table
     * @param rows the selected rows
     * @param returnAll indicates whether all the records should be returned if nothing was selected
     * @return Returns a RecordSet object from the table
     */
    public RecordSetIFace getRecordSet(final int[] rows, final boolean returnAll)
    {
        RecordSet rs = new RecordSet();
        rs.setType(RecordSet.GLOBAL);
        rs.initialize();

        // return if now rows are selected
        if (!returnAll && (rows == null || rows.length == 0))
        {
            return rs;
        }

        if (rows == null)
        {
            for (Integer id : ids)
            {
                rs.addItem(id);
            }
        }
        else
        {
            for (int inx : rows)
            {
                rs.addItem(ids.get(inx));
            }
        }
        
        return rs;
    }

    /**
     * Clears all the data from the model
     *
     */
    public void clear()
    {
        if (ids != null)
        {
            ids.clear();
        }
        
        if (cache != null)
        {
            for (Vector<Object> row : cache)
            {
                row.clear();
            }
            cache.clear();
        }
        classNames.clear();
        colNames.clear();

        currentRow = 0;
    }

    /**
     * @param propertyListener the propertyListener to set
     */
    public void setPropertyListener(PropertyChangeListener propertyListener)
    {
        this.propertyListener = propertyListener;
    }
    
    /**
     * @param setter
     * @param parent
     * @param fieldName
     * @param fieldClass
     * @param resultSet
     * @param colIndex
     * @throws SQLException
     */
    protected void setField(final DataObjectSettable setter, 
                            final Object             parent, 
                            final String             fieldName, 
                            final Class<?>           fieldClass,
                            final ResultSet          resultSet, 
                            final int                colIndex) throws SQLException
    {
        Object fieldDataObj = resultSet.getObject(colIndex + 1);
        //log.debug("fieldName ["+fieldName+"] fieldClass ["+fieldClass.getSimpleName()+"] colIndex [" +  colIndex + "] fieldDataObj [" + fieldDataObj+"]");
        if (fieldDataObj != null)
        {
            if (fieldClass == String.class)
            {
                setter.setFieldValue(parent, fieldName, fieldDataObj);    
                
            } else if (fieldClass == Byte.class)
            {
                setter.setFieldValue(parent, fieldName, resultSet.getByte(colIndex + 1));
                
            } else if (fieldClass == Short.class)
            {
                setter.setFieldValue(parent, fieldName, resultSet.getShort(colIndex + 1));
                
            } else
            {
                setter.setFieldValue(parent, fieldName, fieldDataObj);
            }
        } 
    }
    
    /* This gets called for the "related" express searches defined in search.config.xml
     * @see edu.ku.brc.dbsupport.SQLExecutionListener#exectionDone(edu.ku.brc.dbsupport.SQLExecutionProcessor, java.sql.ResultSet)
     */
    @Override
    //@SuppressWarnings("null")
    public synchronized void exectionDone(final SQLExecutionProcessor process, final ResultSet resultSet)
    {
        if (statusBar != null)
        {
            statusBar.incrementValue(getClass().getSimpleName());
        }
        
        if (resultSet == null || results == null)
        {
            log.error("The " + (resultSet == null ? "resultSet" : "results") + " is null.");
            if (propertyListener != null)
            {
                propertyListener.propertyChange(new PropertyChangeEvent(this, "rowCount", null, 0));
            }
            return;
        }
        
        List<ERTICaptionInfo> captions = results.getVisibleCaptionInfo();

        try
        {
            if (resultSet.next())
            {
                // This is done once for the result set: add col labels to vector colNames,
                // data class to vector classnames; set each caption's colClass; set hasAggregatorCaption
                ResultSetMetaData metaData = resultSet.getMetaData();
                int numCols = resultSet.getMetaData().getColumnCount();

                numColumns = captions.size();
                
                boolean hasAggregatorCaption = false;
                
                for (ERTICaptionInfo caption : captions)
                {
                     colNames.addElement(caption.getColLabel());

                     int metadataIndex = caption.getPosIndex() + 1;

                     Class<?> cls = Class.forName(metaData.getColumnClassName(metadataIndex));
                     
                     // why aren't we letting dates be dates?
                     if (cls == Calendar.class ||  cls == java.sql.Date.class || cls == Date.class)
                     {
                         cls = String.class;
                     }
                     classNames.addElement(cls);
                     
                     // tell the caption what class its data is
                     caption.setColClass(cls);
                     
                     // we found an aggregator (this doesn't seem correct to me but it's all we've got)
                     if (caption.getAggregatorName() != null) hasAggregatorCaption = true;
                }
                
                if (ids == null)
                {
                    ids = new Vector<Integer>();
                } else
                {
                    ids.clear();
                }
                
                DataObjFieldFormatMgr dataObjMgr = DataObjFieldFormatMgr.getInstance();

                Vector<Object> displayRow  = null;
                Vector<Object> previousRow = null;  // the previous displayRow
                
                int previousId = -1;
                int id;

                do 
                {
                    id = resultSet.getInt(1);
                    
                    // If we are aggregating, only create new display rows for new id values.  Otherwise,
                    // create a new display row for each results row
                    if ((id != previousId && hasAggregatorCaption) || !hasAggregatorCaption)
                    {
                        ids.add(id);
                        previousRow = displayRow;
                        if (previousRow != null) cache.add(previousRow);
                        displayRow  = new Vector<Object>();
                    }
                    
                    // Iterate over the captions.  For each caption there will be a field in the display row.
                    // If we are aggregating (combining multiple rows with the same id into one), then we will
                    // create a new display row only the first time we see the id.  Thereafter we will update
                    // the display row.  For new rows, each caption appends its field to the display as it is
                    // processed.  For old rows, each caption updates its field in the display row.
                    
                    // Captions without aggregators will overwrite their display fields on each update.
                    // Captions with aggregators will add a new object to the list of objects to aggregate
                    // on each update.  When the next new row is created, we will get the old row's list
                    // and perform the aggregation and update the old row.

                    // displayIndex is where we are in the display values row; it takes into account the 
                    // compression of the values list caused by composite and aggregate captions
                    int displayIndex = 0;

                    for (ERTICaptionInfo caption :  captions)
                    {
                        // this is the index of the caption in the list of captions we're iterating over
                        int captionIndex  = caption.getPosIndex();

                        // the result set metadata has an id field at index 0 without a corresponding caption,
                        // add 1 to take this into account
                        int metadataIndex = captionIndex + 1;
                        
                        boolean isAggregate = caption.getAggregatorName() != null;
                        boolean isComposite = caption.getColInfoList() != null && caption.getColInfoList().size() > 0;

                        if (isComposite && !isAggregate) // aggregate captions may have multiple columns too, but they do their own thing
                        {
                            DataObjectSettable aggSetter = null;
                            DataObjSwitchFormatter dataObjFormatter = caption.getDataObjFormatter();
                            UIFieldFormatterIFace uiFieldFormatter = caption.getUiFieldFormatter();
                            
                            if (dataObjFormatter != null)
                            {
                                aggSetter = DataObjectSettableFactory.get(dataObjFormatter.getDataClass().getName(), "edu.ku.brc.af.ui.forms.DataSetterForObj");
                            }
                            
                            Object formattedObject = null;

                            if (aggSetter != null && dataObjFormatter != null)
                            {
                                Object compositeObject = caption.getAggClass().newInstance();

                                for (ERTICaptionInfo.ColInfo colInfo : caption.getColInfoList())
                                {
                                    setField(aggSetter, compositeObject, colInfo.getFieldName(), colInfo.getFieldClass(), resultSet, colInfo.getPosition());
                                }
                                formattedObject = dataObjFormatter.format(compositeObject);

                            } else if (uiFieldFormatter != null)
                            {
                                int      len = caption.getColInfoList().size();
                                Object[] val = new Object[len];
                                int      i   = 0;
                                for (ERTICaptionInfo.ColInfo colInfo : caption.getColInfoList())
                                {
                                    int columnMetadataIndex = colInfo.getPosition() + metadataIndex;
                                    if (columnMetadataIndex < numCols)
                                    {
                                        val[i++] = resultSet.getObject(columnMetadataIndex);
                                    } else
                                    {
                                        val[i++] = "(Missing Data)";
                                    }
                                }
                                Object formattedValue = uiFieldFormatter.formatToUI(val);
                                formattedObject = formattedValue != null ? formattedValue : "";

                            } else
                            {  
                                log.error("Aggregator is null! ["+caption.getAggregatorName()+"]");
                            }

                            if (displayRow.size() <= displayIndex) displayRow.add(formattedObject);
                            else displayRow.set(displayIndex, formattedObject);
                        }

                        else if (isAggregate) // Doing an Aggregation
                        {
                            Object dataObj = null;
                            
                            Object resultSetObj = resultSet.getObject(metadataIndex);
                            
                            if (resultSetObj != null) // don't bother creating an aggregation object if the result set obj is null
                            {
                                DataObjectSettable aggSetter = DataObjectSettableFactory.get(caption.getAggClass().getName(), FormHelper.DATA_OBJ_SETTER);
                                DataObjectSettable dataSetter = aggSetter;

                                if (caption.getSubClass() != null)
                                {
                                    dataSetter = DataObjectSettableFactory.get(caption.getSubClass().getName(), FormHelper.DATA_OBJ_SETTER);
                                }

                                Object aggObj = caption.getAggClass().newInstance();
                                Object aggSubObj = caption.getSubClass() != null ? caption.getSubClass().newInstance() : null;


                                if (aggSubObj != null)
                                {
                                    aggSetter.setFieldValue(aggObj, caption.getSubClassFieldName(), aggSubObj);
                                    dataObj = aggSubObj;
                                } else
                                {
                                    dataObj = aggObj;
                                }

                                for (ERTICaptionInfo.ColInfo colInfo : caption.getColInfoList())
                                {
                                    setField(dataSetter, dataObj, colInfo.getFieldName(), colInfo.getFieldClass(), resultSet, captionIndex + colInfo.getPosition());
                                }
                            }
                            
                            if (displayRow.size() <= displayIndex)  // this is a new row; add a new value and update previous row
                            {
                                Vector newList = new Vector();
                                if (dataObj != null) newList.add(dataObj);
                                displayRow.add(newList);

                                if (previousRow != null)
                                {
                                    Object previousValue = previousRow.get(displayIndex);
                                    if (previousValue instanceof Vector) // otherwise we've already updated it and it's a String
                                    {
                                        Vector oldList = (Vector) previousRow.get(displayIndex);
                                        previousRow.set(displayIndex, dataObjMgr.aggregate(oldList, caption.getAggClass()));
                                    }
                                }
                            }
                            else // this is an old row; update it
                            {
                                if (dataObj != null) ((Vector) displayRow.get(displayIndex)).add(dataObj);
                            }
                        }
                        else // neither a composite nor an aggregate caption
                        {
                            Object fieldObject = caption.processValue(resultSet.getObject(metadataIndex));

                            if (displayRow.size() <= displayIndex) displayRow.add(fieldObject);
                            else displayRow.set(displayIndex, fieldObject);
                        }
                        
                        previousId = id;
                        displayIndex++;
                    }
                    
                } while (resultSet.next());
                
                // We were always setting the rolled up data when the ID changed
                // but on the last row we need to do it here manually (so to speak)
                if (hasAggregatorCaption)
                {
                    int rowIndex = 0;
                    for (ERTICaptionInfo caption : captions)
                    {
                        boolean isAggregate = caption.getAggregatorName() != null;
                        
                        if (isAggregate)
                        {
                            Object value = displayRow.get(rowIndex);
                            if (value instanceof Vector) // otherwise we've already updated it and it's a String
                            {
                                Vector oldList = (Vector) value;
                                displayRow.set(rowIndex, dataObjMgr.aggregate(oldList, caption.getAggClass()));
                            }
                        }
                        rowIndex++;
                    }
                }
                if (previousId != id) ids.add(id);
                if (displayRow != null) cache.add(displayRow);
                
                fireTableStructureChanged();
                fireTableDataChanged();
            }
            
        } catch(Exception ex)
        {
            ex.printStackTrace();
        }
        
        if (propertyListener != null)
        {
            propertyListener.propertyChange(new PropertyChangeEvent(this, "rowCount", null, new Integer(cache.size())));
        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.dbsupport.SQLExecutionListener#executionError(edu.ku.brc.dbsupport.SQLExecutionProcessor, java.lang.Exception)
     */
    @Override
    public synchronized void executionError(SQLExecutionProcessor process, Exception ex)
    {
        if (statusBar != null)
        {
            statusBar.incrementValue(getClass().getSimpleName());
        }
    }

    /* (non-Javadoc) This gets called for the express searches not defined in search.config.xml
     * @see edu.ku.brc.dbsupport.CustomQueryListener#exectionDone(edu.ku.brc.dbsupport.CustomQuery)
     */
    @Override
    public void exectionDone(final CustomQueryIFace customQuery)
    {
        if (statusBar != null)
        {
            statusBar.incrementValue(getClass().getSimpleName());
        }
        
        results.queryTaskDone(customQuery);
        List<?> list      = customQuery.getDataObjects();
        
        List<ERTICaptionInfo> captions = results.getVisibleCaptionInfo();
        
        //log.debug("Results size: "+list.size());
        
        if (ids == null)
        {
            ids = new Vector<Integer>();
        } else
        {
            ids.clear();
        }
        
        if (!customQuery.isInError() && !customQuery.isCancelled() && list != null && list.size() > 0)
        {
            /*if (numColumns == 1)
            {
                for (Object rowObj : list)
                {
                    Vector<Object> row = new Vector<Object>(list.size());
                    row.add(rowObj);
                    cache.add(row);
                }
                
            } else*/
            {
                
                int maxTableRows = results.getMaxTableRows();
                int rowNum = 0;
                for (Object rowObj : list)
                {
                    if (rowNum == maxTableRows)
                    {
                        break;
                    }
                    if (customQuery.isCancelled())
                    {
                        break;
                    }
                    //Vector<Object> row = new Vector<Object>(list.size()); //list.size()?????
                    Vector<Object> row = new Vector<Object>(rowObj.getClass().isArray() ? ((Object[])rowObj).length : 1);
                    if (rowObj != null && rowObj.getClass().isArray())
                    {
                        Object[] rowCols = (Object[])rowObj;
                        int capInx = 0;
                        for (int col=0;col<rowCols.length;col++)
                        {
                            Object          colObj  = rowCols[col];
                            ERTICaptionInfo capInfo = captions.get(capInx);
                            UIFieldFormatterIFace uiFieldFormatter = capInfo.getUiFieldFormatter();
                            
                            if (col == 0)
                            {
                                if (colObj instanceof Integer)
                                {
                                    ids.add((Integer)colObj);
                                    if (doDebug) log.debug("*** 1 Adding id["+colObj+"]");
                                } else
                                {
                                    //log.error("First Column must be Integer id! ["+colObj+"]");
                                    row.add(capInfo.processValue(colObj));
                                    capInx++;
                                }
                                
                            } else if (capInfo.getColName() == null && 
                                       capInfo.getColInfoList().size() > 0)
                            {
                                int      len = capInfo.getColInfoList().size();
                                Object[] val = new Object[len];
                                for (int i=0;i<capInfo.getColInfoList().size();i++)
                                {
                                    val[i] = capInfo.processValue(rowCols[col+i]);
                                }
                                row.add(uiFieldFormatter.formatToUI(val));
                                col += capInfo.getColInfoList().size() - 1;
                                capInx++;
                                
                            } else
                            {
                                
                                Object obj = capInfo.processValue(colObj);
                                Object val = uiFieldFormatter == null ? obj : uiFieldFormatter.formatToUI(obj);
                                
                                row.add(val);
                                if (doDebug) log.debug("*** 2 Adding id["+obj+"]");
                                capInx++;
                            }
                            
                        } // for
                    } else
                    {
                        row.add(rowObj);
                    }
                    cache.add(row);
                    rowNum++;
                }                
            }
            
            results.cacheFilled(cache);
            
            fireTableDataChanged();
        }
        
        if (propertyListener != null)
        {
            propertyListener.propertyChange(new PropertyChangeEvent(this, "rowCount", null, new Integer(cache.size())));
        }
        
        if (parentERTP != null)
        {
            CommandAction cmdAction = new CommandAction(ExpressSearchTask.EXPRESSSEARCH, "SearchComplete", customQuery);
            cmdAction.setProperty("QueryForIdResultsIFace", results);
            cmdAction.setProperty("ESResultsTablePanelIFace", parentERTP);
            CommandDispatcher.dispatch(cmdAction);
        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.dbsupport.CustomQueryListener#executionError(edu.ku.brc.dbsupport.CustomQuery)
     */
    @Override
    public void executionError(CustomQueryIFace customQuery)
    {
        UIRegistry.getStatusBar().incrementValue(getClass().getSimpleName());
    }

    /**
     * @return the parentERTP
     */
    public ESResultsTablePanelIFace getParentERTP()
    {
        return parentERTP;
    }
    
    /**
     * @return true if results are still being loaded or created.
     */
    public boolean isLoadingCells()
    {
    	return false;
    }

	public QueryForIdResultsIFace getResults()
	{
		return results;
	}
    
    
}
