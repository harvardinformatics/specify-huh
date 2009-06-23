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

import static edu.ku.brc.ui.UIRegistry.enableActionAndMenu;

import java.awt.Color;
import java.awt.Frame;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import javax.swing.SwingUtilities;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.db.DBFieldInfo;
import edu.ku.brc.af.core.db.DBInfoBase;
import edu.ku.brc.af.core.db.DBRelationshipInfo;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.MultiView;
import edu.ku.brc.af.ui.forms.TableViewObj;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.af.ui.forms.FormViewObj.FVOFieldInfo;
import edu.ku.brc.af.ui.forms.persist.AltViewIFace;
import edu.ku.brc.af.ui.forms.persist.FormCellFieldIFace;
import edu.ku.brc.af.ui.forms.persist.FormCellIFace;
import edu.ku.brc.af.ui.forms.persist.FormCellLabel;
import edu.ku.brc.af.ui.forms.persist.FormCellSubView;
import edu.ku.brc.af.ui.forms.persist.FormCellSubViewIFace;
import edu.ku.brc.af.ui.forms.persist.FormRowIFace;
import edu.ku.brc.af.ui.forms.persist.FormViewDefIFace;
import edu.ku.brc.af.ui.forms.persist.ViewIFace;
import edu.ku.brc.af.ui.forms.validation.AutoNumberableIFace;
import edu.ku.brc.af.ui.forms.validation.FormValidator;
import edu.ku.brc.ui.ToggleButtonChooserDlg;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.UIRegistry;

public class FpFormViewObj extends FormViewObj implements FilteredPushListenerIFace
{
    private static final Logger log = Logger.getLogger(FpFormViewObj.class);
    
    private boolean isMagic;
    
    private FilteredPushInfo fpInfo;
    
    public FpFormViewObj(ViewIFace view, AltViewIFace altView, MultiView mvParent,
            FormValidator formValidator, int options, Color bgColor)
    {
        super(view, altView, mvParent, formValidator, options, bgColor);
        // TODO Auto-generated constructor stub
    }

    public FpFormViewObj(ViewIFace view, AltViewIFace altView, MultiView mvParent,
            FormValidator formValidator, int options, String cellName, Color bgColor)
    {
        super(view, altView, mvParent, formValidator, options, cellName, bgColor);
        // TODO Auto-generated constructor stub
    }

    /**
     * Adjust the Action and MenuItem for CarryForward.
     * @param isVisible whether is is visible
     */
    private void adjustActionsAndMenus(final boolean isVisible) // TODO: this is from the parent class FormViewObj
    {
        enableActionAndMenu("ConfigFilteredPush", isVisible, null); // see Specify.createMenus()
    }
    
    @Override
    public void notification(FilteredPushEvent e)
    {
        // TODO Auto-generated method stub

    }

    /**
     * Turns on/off Filtered Push magic for this form
     * @param isMagic true - on, false - off
     */
    public void setMagic(boolean isMagic)
    {
        this.isMagic = isMagic;
        adjustActionsAndMenus(true);
    }
    
    public void aboutToShow(final boolean show)
    {
        super.aboutToShow(show);
        
        if (mvParent != null && mvParent.isTopLevel() && isEditing)
        {
            if (show)
            {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run()
                    {
                        //log.error(hashCode()+"  "+show);
                        adjustActionsAndMenus(true);
                    }
                });
            } else
            {
                //log.error(hashCode()+"  "+show);
                adjustActionsAndMenus(false);
            }
        }
    }
    
    /**
     * Returns the CarryForwardInfo Object for the Form.
     * @return the CarryForwardInfo Object for the Form
     */
    private FilteredPushInfo getFilteredPushInfo()
    {
        if (fpInfo == null)
        {
            try
            {
                Class<?> classObj = Class.forName(formViewDef.getClassName());
                fpInfo = new FilteredPushInfo(classObj, this, formViewDef);

            } catch (ClassNotFoundException ex)
            {
                edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(FormViewObj.class, ex);
                throw new RuntimeException(ex);
            }
        }
        return fpInfo;
    }
    
    /** 
     * set the magic fields (see FormViewObj.configureCarryForward()
     */
    public void configureFilteredPush()
    {
        log.debug("FpFormViewObj.configureFilteredPush");
        
        FilteredPushInfo magicFieldsInfo = getFilteredPushInfo();
                
        Vector<FVOFieldInfo> itemLabels    = getMagicItemList();
        Vector<FVOFieldInfo> selectedItems = new Vector<FVOFieldInfo>(magicFieldsInfo.getFieldList());
        
        Collections.sort(itemLabels, new Comparator<FVOFieldInfo>() {
            @Override
            public int compare(FVOFieldInfo o1, FVOFieldInfo o2)
            {
                if (o1.getLabel() == null || o2.getLabel() == null)
                {
                    return 1;
                }
                return o1.getLabel().compareTo(o2.getLabel());
            }
        });
        
        ToggleButtonChooserDlg<FVOFieldInfo> dlg = new ToggleButtonChooserDlg<FVOFieldInfo>((Frame)UIRegistry.getTopWindow(),
                "CONFIG_FP_TITLE", itemLabels);
        dlg.setUseScrollPane(true);
        dlg.setAddSelectAll(true);
        dlg.setSelectedObjects(selectedItems);
        UIHelper.centerAndShow(dlg);
        
        if (!dlg.isCancelled())
        {
            fpInfo.add(dlg.getSelectedObjects());
        }
    }
    
    private Vector<FVOFieldInfo> getMagicItemList()
    {
        DBTableInfo ti = DBTableIdMgr.getInstance().getByClassName(view.getClassName());
        
        Vector<FVOFieldInfo> itemLabels = new Vector<FVOFieldInfo>();

        // This next section loops through all the UI components in the form that has an ID
        // It checks to make sure that it is a candidate for CF
        Vector<String> ids = new Vector<String>();
        getFieldIds(ids, true); // true means return all the UI components with ids not just the fields

        for (String id : ids)
        {
            FVOFieldInfo fieldInfo = getFieldInfoForId(id);
            String       fieldName = fieldInfo.getFormCell().getName();
            DBFieldInfo  fi        = ti != null ? ti.getFieldByName(fieldName) : null;
            
            fieldInfo.setFieldInfo(fi);

            // Start by assuming it is OK to be added
            boolean isOK = true;

            if (fieldInfo.getFormCell() instanceof FormCellFieldIFace)
            {
                // Only the ones that are editable.
                FormCellFieldIFace fcf = (FormCellFieldIFace)fieldInfo.getFormCell();
                if (fcf.isReadOnly())
                {
                    isOK = false; 
                } else 
                {
                    DBInfoBase infoBase = fieldInfo.getFieldInfo();
                    if (infoBase instanceof DBFieldInfo)
                    {
                        if (fi.isUnique())
                        {
                            isOK = false;
                            
                        } else if (fi.getFormatter() != null && fi.getFormatter().isIncrementer())
                        {
                            isOK = false;
                        }
                    } else
                    {
                        log.debug("Skipping "+infoBase);
                    }
                }
            }
            else if (fieldInfo.getFormCell() instanceof FormCellSubViewIFace)
            {
                String name = fieldInfo.getName();
                if (fieldInfo.getComp() instanceof MultiView)
                {
                    MultiView   mv = ((MultiView)fieldInfo.getComp());
                    FormViewObj fvo =  mv.getCurrentViewAsFormViewObj();
                    if (fvo != null)
                    {
                        itemLabels.addAll(((FpFormViewObj) fvo).getMagicItemList());
                    }
                    else {
                        Viewable view = mv.getCurrentView();
                        if (view instanceof TableViewObj)
                        {
                            // TODO: start here.  Need to get label and db info for columns of the table.
                        }
                    }
                }
                
            }
            else
            {
                log.debug("Skipping "+fieldInfo.getFormCell());
                continue;
            }
            
            // At this point we have weeded out any readonly/autoinc "fields" and we need to get a label for the field
            // And weed out any SubViews.
            if (isOK)
            {
                // Check to see if the field has a label
                String label = null;
                FVOFieldInfo labelInfo = getLabelInfoFor(id);
                if (labelInfo != null)
                {
                    if (!(fieldInfo.getFormCell() instanceof FormCellLabel))
                    {
                        label = ((FormCellLabel)labelInfo.getFormCell()).getLabel();
                    }
                }
                
                log.error("Couldn't find field ["+fieldName+"] in ["+ti.getTitle()+"]");
                
                // Now we go get the DBFieldInfo and DBRelationshipInfo and check to make
                // that the field or Relationship is still a candidate for FP
                DBInfoBase infoBase = null;
                if (ti != null)
                {
                    if (fi != null)
                    {
                        infoBase = fi;
                        
                        // Skip any fields that are AutoNumbered
                        if (fieldInfo.getComp() instanceof AutoNumberableIFace)
                        {
                            isOK = !((AutoNumberableIFace)fieldInfo.getComp()).isFormatterAutoNumber();
                        } else
                        {
                            isOK = true;
                        }
                        
                    } else
                    {
                        DBRelationshipInfo ri = ti.getRelationshipByName(fieldName);
                        if (ri != null)
                        {
                            infoBase = ri;
                            
                            // If the field is a OneToMany then it is a s Set
                            // and we need to make sure the items in the set are clonable
                            // if they are not clonable then we can't include this in 
                            // the Carry Forward list
                            Class<?> dataClass = ri.getDataClass();
                            if (ri.getType() == DBRelationshipInfo.RelationshipType.OneToMany)
                            {
                                try
                                {
                                    Method method = dataClass.getMethod("clone", new Class<?>[] {});
                                    // Pretty much every Object has a "clone" method but we need 
                                    // to check to make sure it is implemented by the same class of 
                                    // Object that is in the Set.
                                    isOK = method.getDeclaringClass() == dataClass;
                                    
                                } catch (Exception ex) 
                                {
                                    edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                                    edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(FormViewObj.class, ex);
                                    isOK = false; // this really shouldn't happen
                                }
                            }
                            
                        } else if (fieldInfo.getUiPlugin() != null)
                        {
                            if (StringUtils.isNotEmpty(label))
                            {
                                label = fieldInfo.getUiPlugin().getTitle();
                            }
                            isOK = fieldInfo.getUiPlugin().canCarryForward();
                        } else
                        {
                            log.error("Couldn't find field ["+fieldName+"] in ["+ti.getTitle()+"]");
                            isOK = false;  
                        }
                    }
                    
                    if (isOK)
                    {
                        if (infoBase != null && StringUtils.isEmpty(label))
                        {
                            label = infoBase.getTitle();
                        }
                        fieldInfo.setLabel(label);
                        itemLabels.add(fieldInfo);
                        fieldInfo.setFieldInfo(infoBase);
                    }
                }
            }
        }
        
        return itemLabels;
    }
}
