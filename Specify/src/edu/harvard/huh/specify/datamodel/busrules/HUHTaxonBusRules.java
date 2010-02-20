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
package edu.harvard.huh.specify.datamodel.busrules;

import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.hibernate.Hibernate;

import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.af.ui.forms.validation.ValComboBox;
import edu.ku.brc.af.ui.forms.validation.ValComboBoxFromQuery;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.config.DisciplineType;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
import edu.ku.brc.specify.datamodel.TaxonTreeDefItem;
import edu.ku.brc.specify.datamodel.busrules.CollectionObjectBusRules;
import edu.ku.brc.specify.datamodel.busrules.TaxonBusRules;
import edu.ku.brc.specify.tasks.TreeTaskMgr;
import edu.ku.brc.ui.GetSetValueIFace;

/**
 * This alters the UI depending on which type of agent is set.
 * 
 * @author rods
 *
 * @code_status Complete
 *
 * Created Date: Jan 24, 2007
 *
 */
public class HUHTaxonBusRules extends TaxonBusRules
{
    protected final static String NAME = "name";
    
    private JLabel parentLabel;
    private JLabel nameLabel;
    
    private String parentLabelDefault;
    private String nameLabelDefault;
    
    /**
     * Constructor.
     */
    public HUHTaxonBusRules()
    {
        super();
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BaseBusRules#initialize(edu.ku.brc.ui.forms.Viewable)
     */
    @Override
    public void initialize(Viewable viewableArg)
    {
        super.initialize(viewableArg);
        
        // TODO: the form system MUST require the hybridParent1 and hybridParent2 widgets to be present if the isHybrid checkbox is present
        final JCheckBox        hybridCheckBox = (JCheckBox)formViewObj.getControlByName(IS_HYBRID);
        
        if (hybridCheckBox != null)
        {
            final Component hybrid1Component  = formViewObj.getControlByName(HYBRIDPARENT1);
            final Component hybrid2Component  = formViewObj.getControlByName(HYBRIDPARENT2);
            
            setVisible(HYBRIDPARENT1, hybridCheckBox.isSelected());
            setVisible(HYBRIDPARENT2, hybridCheckBox.isSelected());
            
            hybridCheckBox.addItemListener(new ItemListener()
            {
                public void itemStateChanged(ItemEvent e)
                {
                    boolean isHybrid = hybridCheckBox.isSelected();
                    setVisible(HYBRIDPARENT1, isHybrid);
                    setVisible(HYBRIDPARENT2, isHybrid);
                    
                    if (!hybridCheckBox.isSelected())
                    {
                        if (hybrid1Component != null) ((GetSetValueIFace)hybrid1Component).setValue(null, null);
                        if (hybrid2Component != null) ((GetSetValueIFace)hybrid2Component).setValue(null, null);
                    }
                }
            });
        }
        
        Component parentComp = formViewObj.getControlByName(PARENT);
        Component nameComp = formViewObj.getControlByName(NAME);

        parentLabel = formViewObj.getLabelFor(parentComp);
        nameLabel = formViewObj.getLabelFor(nameComp);

        if (parentLabel != null) parentLabelDefault = parentLabel.getText();
        if (nameLabel != null) nameLabelDefault = nameLabel.getText();

        TreeTaskMgr.checkLocks();
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BaseBusRules#afterFillForm(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void afterFillForm(final Object dataObj)
    {
        super.afterFillForm(dataObj);
        
        Taxon taxon = (Taxon) formViewObj.getDataObj();
        if (taxon != null)
        {
            fixParentLabel(taxon.getParent());
            fixNameLabel(taxon.getDefinitionItem());
        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.busrules.BaseBusRules#beforeSave(java.lang.Object, edu.ku.brc.dbsupport.DataProviderSessionIFace)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void beforeSave(Object dataObj, DataProviderSessionIFace session)
    {
        super.beforeSave(dataObj, session);
        
        if (dataObj instanceof Taxon)
        {
           Taxon taxon = (Taxon) dataObj;
           
           String parAuthorName   = null;
           String stdAuthorName   = null;

           if (taxon.getStdAuthor() != null)
           {
               stdAuthorName = taxon.getStdAuthor().getAuthorName();
               
               if (taxon.getStdExAuthor() != null)
               {
                   String stdExAuthorName = taxon.getStdExAuthor().getAuthorName();
                   
                   if (stdAuthorName != null && stdExAuthorName != null)
                   {
                       stdAuthorName = stdAuthorName + " ex " + stdExAuthorName;
                   }
               }
               
               if (taxon.getParAuthor() != null)
               {
                   parAuthorName = taxon.getParAuthor().getAuthorName();
                   
                   if (taxon.getParExAuthor() != null)
                   {
                       String parExAuthorName = taxon.getParExAuthor().getAuthorName();
                       
                       if (parAuthorName != null && parExAuthorName != null)
                       {
                           parAuthorName = parAuthorName + " ex " + parExAuthorName;
                       }
                   }
               }

               if (stdAuthorName != null && parAuthorName != null)
               {
                   stdAuthorName = "(" + parAuthorName + ") " + stdAuthorName;
               }

               // set its author
               taxon.setAuthor(stdAuthorName);
           }
        }
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#processBusinessRules(java.lang.Object)
     */
    @Override
    public STATUS processBusinessRules(final Object dataObj)
    {
        reasonList.clear();
        STATUS status =  super.processBusinessRules(dataObj);
        
        if (status == STATUS.OK)
        {
            status = checkAuthors((Taxon) dataObj);
        }
        return status;
    }
    
    /**
     * Process business rules specifically related to determinations.  Return OK status, or add error messages
     * to reasonList and return Error status.
     * @param dataObj
     * @return 
     */
    protected STATUS checkAuthors(final Taxon taxon)
    {
        // add authors as we check the rules
        List<Agent> authors = new ArrayList<Agent>();
        
        // check that if there is a par ex author, then there is a par author
        if (taxon.getParExAuthor() != null)
        {
            if (taxon.getParAuthor() == null)
            {
                reasonList.add(getResourceString("TaxonBusRules.NO_PAR_AUTHOR_FOR_EX"));
                return STATUS.Error;
            }
            authors.add(taxon.getParExAuthor());
        }

        // check that if there is a std ex author, then there is a std author
        if (taxon.getStdExAuthor() != null)
        {
            if (taxon.getStdAuthor() == null)
            {
                reasonList.add(getResourceString("TaxonBusRules.NO_STD_AUTHOR_FOR_EX"));
                return STATUS.Error;
            }
            authors.add(taxon.getStdExAuthor());
        }
        
        // check that if there is a par author, then there is a std author
        if (taxon.getParAuthor() != null)
        {
            if (taxon.getStdAuthor() == null)
            {
                reasonList.add(getResourceString("TaxonBusRules.NO_STD_AUTHOR_FOR_PAR"));
                return STATUS.Error;
            }
            authors.add(taxon.getParAuthor());
        }
        
        if (taxon.getStdAuthor() != null)
        {
            authors.add(taxon.getStdAuthor());
        }
        
        // check that all authors have auth abbrev variants
        STATUS authorStatus = STATUS.OK;
        
        DataProviderSessionIFace tmpSession = DataProviderFactory.getInstance().createSession();
        for (Agent author : authors)
        {
            try
            {
                tmpSession.attach(author);
                
            } catch (Exception ex)
            {
                ex.printStackTrace();
                edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(CollectionObjectBusRules.class, ex);
                break;
            }
            
            Hibernate.initialize(author);

            if (author.getAuthorName() == null)
            {
                reasonList.add(String.format(getResourceString("TaxonBusRules.NO_AUTHOR_ABBREV_VAR"), author.getLastName()));
                authorStatus = STATUS.Error;
            }
        }
        tmpSession.close();
        
        if (authorStatus != STATUS.OK)
        {
            return authorStatus;
        }

        // warn if no std author
        if (taxon.getStdAuthor() == null)
        {
            reasonList.add(getResourceString("TaxonBusRules.NO_STD_AUTHOR"));

            return STATUS.Warning;
        }
        
        return STATUS.OK;
    }
    
    // this is modified from HUHAgentBusRules, factor it out
    protected void setVisible(String componentId, boolean visible)
    {
        Component field  = formViewObj.getCompById(componentId);
        if (field != null) field.setVisible(visible);
        
        JLabel label = formViewObj.getLabelFor(field);
        if (label != null)
        {
            label.setVisible(visible);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void parentChanged(final FormViewObj form, 
                                 final ValComboBoxFromQuery parentComboBox, 
                                 final ValComboBox rankComboBox,
                                 final JCheckBox acceptedCheckBox,
                                 final ValComboBoxFromQuery acceptedParentWidget)
    {
        super.parentChanged(form, parentComboBox, rankComboBox, acceptedCheckBox, acceptedParentWidget);

        Taxon parentObj = (Taxon)parentComboBox.getValue();
        
        fixParentLabel(parentObj);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void rankChanged(final FormViewObj form,
            final ValComboBoxFromQuery parentComboBox, 
            final ValComboBox rankComboBox,
            final JCheckBox acceptedCheckBox,
            final ValComboBoxFromQuery acceptedParentWidget)
    {
        super.rankChanged(form, parentComboBox, rankComboBox, acceptedCheckBox, acceptedParentWidget);
        
        TaxonTreeDefItem rankObj = (TaxonTreeDefItem)rankComboBox.getValue();
        
        fixNameLabel(rankObj);
    }
    
    /**
     * Clears the values and hides some UI depending on what type is selected
     * @param cbx the type cbx
     */
    protected void fixNameLabel(final TaxonTreeDefItem defItem)
    {
        if (formViewObj != null && nameLabel != null && defItem != null)
        {
            Taxon taxon = (Taxon)formViewObj.getDataObj();

            if (taxon != null)
            {
                Integer rankId = defItem.getRankId();
                if (rankId != null)
                {
                    nameLabel.setText(defItem.getRankId() > TaxonTreeDef.GENUS ? "Epithet:" : "Name:"); // I18N?
                }
            }
        }
        else if (nameLabel != null)
        {
            nameLabel.setText(nameLabelDefault);
        }
    }
    
    /**
     * Clears the values and hides some UI depending on what type is selected
     * @param cbx the type cbx
     */
    protected void fixParentLabel(final Taxon parent)
    {
        if (formViewObj != null && parentLabel != null && parent != null)
        {
            Taxon taxon = (Taxon)formViewObj.getDataObj();

            if (taxon != null)
            {
                TaxonTreeDefItem parentDefItem = parent.getDefinitionItem();
                parentLabel.setText(parentDefItem.getName());
            }
        }
        else if (parentLabel != null)
        {
            parentLabel.setText(parentLabelDefault);
        }
    }
}
