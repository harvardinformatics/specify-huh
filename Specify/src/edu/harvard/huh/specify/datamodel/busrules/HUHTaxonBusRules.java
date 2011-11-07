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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import org.hibernate.Hibernate;

import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.af.ui.forms.persist.AltViewIFace.CreationMode;
import edu.ku.brc.af.ui.forms.validation.ValComboBox;
import edu.ku.brc.af.ui.forms.validation.ValComboBoxFromQuery;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
import edu.ku.brc.specify.datamodel.TaxonTreeDefItem;
import edu.ku.brc.specify.datamodel.busrules.CollectionObjectBusRules;
import edu.ku.brc.specify.datamodel.busrules.TaxonBusRules;
import edu.ku.brc.specify.tasks.TreeTaskMgr;
import edu.ku.brc.ui.GetSetValueIFace;

/**
 * This alters the UI depending on the rank, parent, and hybrid status of the taxon.
 * It makes some configurations of authors illegal.
 * 
 * @author mkelly
 *
 * @code_status alpha
 *
 * Created Date: Feb 22, 2010
 *
 */
public class HUHTaxonBusRules extends TaxonBusRules
{
    protected final static String NAME = "name";

    // There are separate FormViewObj objects for view mode and edit mode.
    // The parent class sets the global var formViewObj to the one used during bus rules initialization.
    // However, the "current" form view changes when the mode changes. This makes label lookup by component
    // problematic because different form views may have created different components out of the same form
    // cell def.  So if you call this.formViewObj.getLabelFor(comp),you might be asking that formViewObj
    // about a component it knows nothing about.
    private FormViewObj editViewObj;
    private FormViewObj viewViewObj;

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
        
        if (CreationMode.EDIT.equals(formViewObj.getAltView().getMode()))
        {
            this.editViewObj = formViewObj;
        }
        else if (CreationMode.VIEW.equals(formViewObj.getAltView().getMode()))
        {
            this.viewViewObj = formViewObj;
        }
        String parentLabelText = getLabelText(formViewObj, PARENT);
        if (parentLabelText != null) this.parentLabelDefault = parentLabelText;
        
        String nameLabelText = getLabelText(formViewObj, NAME);
        if (nameLabelText != null) this.nameLabelDefault = nameLabelText;

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
        
        Taxon taxon = (Taxon) dataObj;

        fixParentLabel(taxon != null ? taxon.getParent() : null);
        fixNameLabel(taxon != null ? taxon.getDefinitionItem() : null);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.busrules.BaseBusRules#beforeSave(java.lang.Object, edu.ku.brc.dbsupport.DataProviderSessionIFace)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void beforeSave(Object dataObj, DataProviderSessionIFace session) {
        super.beforeSave(dataObj, session);

        if (!(dataObj instanceof Taxon)) {
            return;
        }

        fixHybridName((Taxon) dataObj);

        Taxon taxon = (Taxon) dataObj;

        String parAuthorName = null;
        String stdAuthorName = null;

        if (taxon.getStdAuthor() != null) { // StdAuthor is required
            stdAuthorName = taxon.getStdAuthor().getAuthorName();

            if (taxon.getStdExAuthor() != null) {
                String stdExAuthorName = taxon.getStdExAuthor().getAuthorName();

                if (stdAuthorName != null && stdExAuthorName != null) {
                    stdAuthorName = stdAuthorName + " ex " + stdExAuthorName;
                }
            }

            if (taxon.getParAuthor() != null) {
                parAuthorName = taxon.getParAuthor().getAuthorName();

                if (taxon.getParExAuthor() != null) {
                    String parExAuthorName = taxon.getParExAuthor()
                            .getAuthorName();

                    if (parAuthorName != null && parExAuthorName != null) {
                        parAuthorName = parAuthorName + " ex "
                                + parExAuthorName;
                    }
                }
                
                // Added by lchan
                if (taxon.getParSanctAuthor() != null) {
                    parAuthorName += ": " + taxon.getParSanctAuthor().getAuthorName();
                }
            }
            
            if (stdAuthorName != null && parAuthorName != null) {
                stdAuthorName = "(" + parAuthorName + ") " + stdAuthorName;
            }

            String sanctAuthorName = null;
            if (taxon.getSanctAuthor() != null) {
                sanctAuthorName = taxon.getSanctAuthor().getAuthorName();
                stdAuthorName = stdAuthorName + ": " + sanctAuthorName;
            }

            // set its author
            taxon.setAuthor(stdAuthorName);
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
                reasonList.add(getResourceString("TaxonBusRules.NO_AUTHOR_ABBREV_VAR"));
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

        JLabel label = getLabel(componentId);
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
        JLabel nameLabel = getLabel(NAME);
        if (nameLabel != null && defItem != null)
        {
            Integer rankId = defItem.getRankId();
            if (rankId != null)
            {
                nameLabel.setText(defItem.getRankId() > TaxonTreeDef.GENUS ? "Epithet:" : "Name:"); // I18N?
            }
        }
        else if (nameLabel != null)
        {
            String defaultText = getDefaultLabelText(NAME);
            if (defaultText != null) nameLabel.setText(defaultText);
        }
    }
    
    private void fixParentLabel(final Taxon parent)
    {       
        JLabel parentLabel = getLabel(PARENT);
        if (parentLabel != null && parent != null)
        {
            TaxonTreeDefItem parentDefItem = parent.getDefinitionItem();
            parentLabel.setText(parentDefItem.getName());
        }
        else if (parentLabel != null)
        {
            String defaultText = getDefaultLabelText(PARENT);
            if (defaultText != null) parentLabel.setText(defaultText);
        }
    }

    private JLabel getLabel(String componentId)
    {
        if (PARENT.equals(componentId))
        {
            JLabel label = getLabel(PARENT, CreationMode.EDIT);
            if (label != null) return label;
            else return getLabel(PARENT, CreationMode.VIEW);
        }
        else
        {
            return this.formViewObj.getLabelFor(this.formViewObj.getControlById(componentId));
        }
    }

    private JLabel getLabel(String componentId, CreationMode mode)
    {
        if (CreationMode.EDIT.equals(mode))
        {
            FormViewObj formViewObj = getEditViewObj();
            return getLabel(componentId, formViewObj);
        }
        else if (CreationMode.VIEW.equals(mode))
        {
            FormViewObj formViewObj = getViewViewObj();
            return getLabel(componentId, formViewObj);
        }
        return null;
    }

    private JLabel getLabel(String componentId, FormViewObj formViewObj)
    {
        if (formViewObj != null)
        {
            Component parentComponent = formViewObj.getControlById(componentId);
            if (parentComponent != null)
            {
                JLabel label = formViewObj.getLabelFor(parentComponent);
                if (label != null)
                {
                    return label;
                }
            }
        }
        return null;
    }

    private String getLabelText(FormViewObj formViewObj, String componentId)
    {
        JLabel parentLabel = getLabel(componentId, formViewObj);
        if (parentLabel != null)
        {
            return parentLabel.getText();
        }
        return null;
    }

    private String getDefaultLabelText(String componentId)
    {
        if (NAME.equals(componentId))
        {
            return this.nameLabelDefault;
        }
        else if (PARENT.equals(componentId))
        {
            return this.parentLabelDefault;
        }
        else
        {
            return null;
        }
    }

    private FormViewObj getViewViewObj()
    {
        if (this.viewViewObj == null)
        {
            if (this.formViewObj != null && CreationMode.VIEW.equals(this.formViewObj.getAltView().getMode()))
            {
                this.viewViewObj = this.formViewObj;
            }
        }
        return this.viewViewObj;
    }

    private FormViewObj getEditViewObj()
    {
        if (this.editViewObj == null)
        {
            if (this.formViewObj != null && CreationMode.VIEW.equals(this.formViewObj.getAltView().getMode()))
            {
                this.editViewObj = this.formViewObj;
            }
        }
        return this.editViewObj;
    }
    
    /**
     * Fixes the way Specify generates hybrid full names. Should go after the
     * superclass's beforeSave method is called. It replaces the full name for
     * genera and species. It appends "notho" to the rank names below species.
     * 
     * @author lchan
     * @param taxon
     */
    public void fixHybridName(Taxon taxon) {
        if (taxon.getIsHybrid()) {
            String fullName = taxon.getFullName();

            if (taxon.getName() != null && !taxon.getName().isEmpty()) {
                // Genus should be xGenus
                if (taxon.getRankId() == TaxonTreeDef.GENUS) {
                    fullName = "\u00D7" + taxon.getName();
                
                }
                // Species should be Genus xSpecies
                else if (taxon.getRankId() == TaxonTreeDef.SPECIES) {
                    fullName = taxon.getParent().getName() + " \u00D7"
                            + taxon.getName();
                }
                // Ranks below species should be replaced with nothorank.
                else if (taxon.getRankId() > TaxonTreeDef.SPECIES) {
                    fullName = fullName.replace(" f. ", " nothof. ");
                    fullName = fullName.replace(" lusus ", " notholusus ");
                    fullName = fullName.replace(" modif. ", " nothomodif. ");
                    fullName = fullName.replace(" prol. ", " nothoprol. ");
                    fullName = fullName.replace(" subsp. ", " nothosubsp. ");
                    fullName = fullName.replace(" subvar. ", " nothosubvar. ");
                    fullName = fullName.replace(" var. ", " nothovar. ");
                }

                taxon.setFullName(fullName);
            }
        }
    }

}
