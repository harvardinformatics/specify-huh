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
package edu.ku.brc.af.ui.forms.validation;

import java.util.Hashtable;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Element;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.ui.db.TextFieldWithInfo;
import edu.ku.brc.af.ui.forms.DataObjectGettable;
import edu.ku.brc.helpers.XMLHelper;
import edu.ku.brc.ui.UIRegistry;

/**
 * This factory knows how to create AutoComplete Comboboxes that get their data from a query.
 
 * @code_status Beta
 **
 * @author rods
 *
 */
public class TypeSearchForQueryFactory
{
    // Static Data Members
    private static final Logger log = Logger.getLogger(TypeSearchForQueryFactory.class);
    
    protected static TypeSearchForQueryFactory instance   = new TypeSearchForQueryFactory();
    protected static boolean                   doingLocal = false;
    
    // Data Members
    protected Hashtable<String, TypeSearchInfo> hash = new Hashtable<String, TypeSearchInfo>();
    
    /**
     * Protected Constructor.
     */
    protected TypeSearchForQueryFactory()
    {
        
    }
    
    /**
     * @return the DOM
     */
    protected Element getDOM()
    {
        if (doingLocal)
        {
            return XMLHelper.readDOMFromConfigDir("backstop/typesearch_def.xml");
        }
        return AppContextMgr.getInstance().getResourceAsDOM("TypeSearches");
    }
    
    /**
     * Loads the formats from the config file.
     *
     */
    public void load()
    {

        if (hash.size() == 0)
        {
            try
            {
                Element root = getDOM();
                if (root != null)
                {
                    List<?> typeSearches = root.selectNodes("/typesearches/typesearch");
                    for (Object fObj : typeSearches)
                    {
                        Element tsElement = (Element)fObj;
                        String name = tsElement.attributeValue("name");
                        if (StringUtils.isNotBlank(name))
                        {
                            TypeSearchInfo tsi = new TypeSearchInfo(XMLHelper.getAttr(tsElement, "tableid", -1),
                                                                    tsElement.attributeValue("displaycols"),
                                                                    tsElement.attributeValue("searchfield"),
                                                                    XMLHelper.getAttr(tsElement, "format", null),
                                                                    XMLHelper.getAttr(tsElement, "uifieldformatter", null),
                                                                    tsElement.attributeValue("dataobjformatter"));
                            hash.put(name, tsi);
                            
                            String sqlTemplate = tsElement.getTextTrim();
                            if (StringUtils.isNotEmpty(sqlTemplate))
                            {
                                tsi.setSqlTemplate(sqlTemplate);
                            }
                        } else
                        {
                            log.error("TypeSearchInfo element is missing or has a blank name!");
                        }
                    }
                } else
                {
                    log.debug("Couldn't open typesearch_def.xml");
                }
            } catch (Exception ex)
            {
                edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(TypeSearchForQueryFactory.class, ex);
                ex.printStackTrace();
                log.error(ex);
            }
        }
    }


    /**
     * Creates a new ValComboBoxFromQuery by name.
     * @param name the name of the ValComboBoxFromQuery to return
     * @return a ValComboBoxFromQuery by name
     */
    public static TextFieldWithInfo getTextFieldWithInfo(final String name,
                                                         final String dataObjFormatterNameArg)
    {
        instance.load();
        
        TypeSearchInfo typeSearchInfo = instance.hash.get(name);
        if (typeSearchInfo != null)
        {
            DBTableInfo tblInfo = DBTableIdMgr.getInstance().getInfoById(typeSearchInfo.getTableId());
            if (tblInfo != null)
            {
                return new TextFieldWithInfo(tblInfo.getClassName(),
                                             tblInfo.getIdFieldName(),
                                             typeSearchInfo.getSearchFieldName(),
                                             typeSearchInfo.getFormat(),
                                             typeSearchInfo.getUiFieldFormatterName(),
                                             dataObjFormatterNameArg,
                                             tblInfo.getNewObjDialog(),
                                             tblInfo.getTitle());
    
            }
            // else
            log.error("Table with ID["+typeSearchInfo.getTableId()+"] not found.");
        } else
        {
            log.error("Object Type Search Name ["+name+"] not found.");
        }
        return null;
    }
    
    /**
     * Creates a new ValComboBoxFromQuery by name.
     * @param name the name of the ValComboBoxFromQuery to return
     * @return a ValComboBoxFromQuery by name
     */
    public static TextFieldWithInfo getTextFieldWithInfo(final String name,
                                                         final String dataObjFormatterNameArg,
                                                         final DataObjectGettable getter)
    {
        instance.load();
        
        TypeSearchInfo typeSearchInfo = instance.hash.get(name);
        if (typeSearchInfo != null)
        {
            DBTableInfo tblInfo = DBTableIdMgr.getInstance().getInfoById(typeSearchInfo.getTableId());
            if (tblInfo != null)
            {
                return new TextFieldWithInfo(tblInfo.getClassName(),
                                             tblInfo.getIdFieldName(),
                                             typeSearchInfo.getSearchFieldName(),
                                             typeSearchInfo.getFormat(),
                                             typeSearchInfo.getUiFieldFormatterName(),
                                             dataObjFormatterNameArg,
                                             getter,
                                             tblInfo.getNewObjDialog(),
                                             tblInfo.getTitle());
    
            }
            // else
            log.error("Table with ID["+typeSearchInfo.getTableId()+"] not found.");
        } else
        {
            log.error("Object Type Search Name ["+name+"] not found.");
        }
        return null;
    }
    
    /**
     * For a given Formatter it returns the formatName.
     * @param name the name of the formatter to use
     * @return the name of the formatter
     */
    public static String getDataObjFormatterName(final String name)
    {
        instance.load();
        
        TypeSearchInfo typeSearchInfo = instance.hash.get(name);
        if (typeSearchInfo != null)
        {
            return typeSearchInfo.getDataObjFormatterName();
        }            
        // else
        log.error("Object Type Search Name ["+name+"] not found.");
        
        UIRegistry.showError("Couldn't create ValComboBoxFromQuery because the entry ["+name+"] is not in the typesearch_def.xml");
        return null;
    }

    /**
     * Creates a new ValComboBoxFromQuery by name.
     * @param name the name of the ValComboBoxFromQuery to return
     * @return a ValComboBoxFromQuery by name
     */
    public static ValComboBoxFromQuery createValComboBoxFromQuery(final String name, 
                                                                  final int btnOpts,
                                                                  final String dataObjFormatterNameArg,
                                                                  final String helpContextArg)
    {
        instance.load();
        
        TypeSearchInfo typeSearchInfo = instance.hash.get(name);
        if (typeSearchInfo != null)
        {
            DBTableInfo tblInfo = DBTableIdMgr.getInstance().getInfoById(typeSearchInfo.getTableId());
            if (tblInfo != null)
            {
                return new ValComboBoxFromQuery(tblInfo,
                                                typeSearchInfo.getSearchFieldName(),
                                                typeSearchInfo.getDisplayColumns(),
                                                typeSearchInfo.getSearchFieldName(),
                                                typeSearchInfo.getFormat(),
                                                typeSearchInfo.getUiFieldFormatterName(),
                                                dataObjFormatterNameArg,
                                                typeSearchInfo.getSqlTemplate(),
                                                helpContextArg,
                                                btnOpts);

    
            }
            // else
            log.error("Table with ID["+typeSearchInfo.getTableId()+"] not found.");
        } else
        {
            log.error("Object Type Search Name ["+name+"] not found.");
        }

        UIRegistry.showError("Couldn't create ValComboBoxFromQuery because the entry ["+name+"] is not in the typesearch_def.xml");
        return null;
        //throw new RuntimeException("Couldn't create ValComboBoxFromQuery by name["+name+"]");
    }

    /**
     * @param doingLocal the doingLocal to set
     */
    public static void setDoingLocal(boolean doingLocal)
    {
        TypeSearchForQueryFactory.doingLocal = doingLocal;
    }
    
    //-----------------------------------------------------
    //-- Inner Classes
    //-----------------------------------------------------
    class TypeSearchInfo
    {
        protected int    tableId;
        protected String displayColumns;
        protected String searchFieldName;
        protected String format;
        protected String uiFieldFormatterName;
        protected String dataObjFormatterName;
        protected String sqlTemplate = null;

        public TypeSearchInfo(int    tableId,
                              String displayColumns,
                              String searchFieldName,
                              String format,
                              String uiFieldFormatterName,
                              String dataObjFormatterName)
        {
            this.tableId         = tableId;
            this.displayColumns  = displayColumns;

            this.searchFieldName = searchFieldName;
            this.format          = format;
            this.uiFieldFormatterName = uiFieldFormatterName;
            this.dataObjFormatterName = dataObjFormatterName;
        }

        public int getTableId()
        {
            return tableId;
        }

        public String getDisplayColumns()
        {
            return displayColumns;
        }

        public String getFormat()
        {
            return format;
        }

        /**
         * @return the uiFieldFormatterName
         */
        public String getUiFieldFormatterName()
        {
            return uiFieldFormatterName;
        }

        /**
         * @return the dataObjFormatterName
         */
        public String getDataObjFormatterName()
        {
            return dataObjFormatterName;
        }

        public String getSearchFieldName()
        {
            return searchFieldName;
        }

        /**
         * @return the sqlTemplate
         */
        public String getSqlTemplate()
        {
            return sqlTemplate;
        }

        /**
         * @param sqlTemplate the sqlTemplate to set
         */
        public void setSqlTemplate(String sqlTemplate)
        {
            this.sqlTemplate = sqlTemplate;
        }
        
    }
}
