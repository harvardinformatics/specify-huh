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
package edu.ku.brc.specify.ui;

import static edu.ku.brc.ui.UIHelper.createButton;
import static edu.ku.brc.ui.UIHelper.createCheckBox;
import static edu.ku.brc.ui.UIHelper.createI18NLabel;
import static edu.ku.brc.ui.UIHelper.createLabel;
import static edu.ku.brc.ui.UIHelper.setControlSize;
import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.prefs.AppPrefsCache;
import edu.ku.brc.af.ui.db.ViewBasedDisplayDialog;
import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.MultiView;
import edu.ku.brc.af.ui.forms.ViewFactory;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.af.ui.forms.persist.FormCellField;
import edu.ku.brc.af.ui.forms.persist.FormCellFieldIFace;
import edu.ku.brc.af.ui.forms.persist.FormCellIFace;
import edu.ku.brc.af.ui.forms.persist.ViewIFace;
import edu.ku.brc.af.ui.forms.validation.FormValidator;
import edu.ku.brc.af.ui.forms.validation.UIValidatable;
import edu.ku.brc.af.ui.forms.validation.UIValidator;
import edu.ku.brc.af.ui.forms.validation.ValComboBoxFromQuery;
import edu.ku.brc.af.ui.forms.validation.ValFormattedTextFieldSingle;
import edu.ku.brc.af.ui.forms.validation.ValidationListener;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.LoanReturnPreparation;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.ui.ColorWrapper;
import edu.ku.brc.ui.DateWrapper;
import edu.ku.brc.ui.IconManager;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.VerticalSeparator;

/**
 * Creates a dialog representing all the Preparation objects being returned for a loan.
 * TODO: Convert to use CustomDialog
 * 
 * @author rods
 * @author dl
 *
 * @code_status Beta
 *
 * Created Date: Dec 15, 2006
 *
 */
public class LoanReturnDlg extends JDialog
{
    protected ColorWrapper           requiredfieldcolor = AppPrefsCache.getColorWrapper("ui", "formatting", "requiredfieldcolor");
    protected DateWrapper            scrDateFormat = AppPrefsCache.getDateWrapper("ui", "formatting", "scrdateformat");
    protected Loan                   loan;
    protected List<ColObjPanel>      colObjPanels = new Vector<ColObjPanel>();
    protected JButton                okBtn;
    protected JLabel                 summaryLabel;
    protected FormValidator          validator = new FormValidator(null);
    protected ValComboBoxFromQuery   agentCBX;
    protected boolean                isCancelled = true;
    protected ValFormattedTextFieldSingle dateClosed;
    
    /**
     * Constructor for the loan return dialog
     * 
     * dl: I added components for Type and Non-specimen counts. Took out the unused "resolved" fields.
     * @param loan the loan
     */
    public LoanReturnDlg(final Loan loan)
    {
        this.loan = loan;
        
        ImageIcon appIcon = IconManager.getIcon("AppIcon"); //$NON-NLS-1$
        if (appIcon != null)
        {
            setIconImage(appIcon.getImage());
        }

        
    }
    
    /**
     * Creates the UI for the dialog.
     * 
     * @return
     */
    public boolean createUI()
    {
        DataProviderSessionIFace session = null;
        try
        {
            session = DataProviderFactory.getInstance().createSession();
            
            loan = session.merge(loan);
        
            setTitle(getResourceString("LOANRET_TITLE"));
            
            validator.addValidationListener(new ValidationListener() {
                public void wasValidated(UIValidator val)
                {
                    doEnableOKBtn();
                }
            });
            
             
            JPanel contentPanel = new JPanel(new BorderLayout());
            
            JPanel mainPanel = new JPanel();
            
            Hashtable<CollectionObject, Vector<LoanPreparation>> colObjHash = new Hashtable<CollectionObject, Vector<LoanPreparation>>();
            for (LoanPreparation loanPrep : loan.getLoanPreparations())
            {
                Set<Fragment> fragments = loanPrep.getPreparation().getFragments();
                for (Fragment fragment : fragments)
                {
                    CollectionObject colObj = fragment.getCollectionObject();
                    
                    Vector<LoanPreparation> list = colObjHash.get(colObj);
                    if (list == null)
                    {
                        list = new Vector<LoanPreparation>();
                        colObjHash.put(colObj, list);
                    }
                    list.add(loanPrep);
                }
            }
            int colObjCnt = colObjHash.size();
    
            String          rowDef   = UIHelper.createDuplicateJGoodiesDef("p", "1px,p,4px", (colObjCnt*2)-1);
            PanelBuilder    pbuilder = new PanelBuilder(new FormLayout("f:p:g", rowDef), mainPanel);
            CellConstraints cc       = new CellConstraints();
            
            ActionListener al = new ActionListener()
            {
                public void actionPerformed(ActionEvent ae)
                {
                    doEnableOKBtn();
                }
            };
     
            ChangeListener cl = new ChangeListener()
            {
                public void stateChanged(ChangeEvent ae)
                {
                    doEnableOKBtn();
                }
            };
     
            int i = 0;
            int y = 1;
    
            Vector<CollectionObject> keysList = new Vector<CollectionObject>(colObjHash.keySet());
            Collections.sort(keysList);
            for (CollectionObject co : keysList)
            {
                if (i > 0)
                {
                    pbuilder.addSeparator("", cc.xy(1,y));
                    y += 2;
                }
                
                ColObjPanel panel = new ColObjPanel(session, this, co, colObjHash.get(co));
                colObjPanels.add(panel);
                panel.addActionListener(al, cl);
                pbuilder.add(panel, cc.xy(1,y));
                y += 2;
                i++;
            }
            
            JButton selectAllBtn = createButton(getResourceString("SELECTALL"));
            okBtn = createButton(getResourceString("SAVE"));
            JButton cancel = createButton(getResourceString("CANCEL"));
            
            PanelBuilder pb = new PanelBuilder(new FormLayout("p,2px,p,2px,p,2px,p,2px,p,2px,p", "p"));
            
            dateClosed = new ValFormattedTextFieldSingle("Date", false, false, 10);
            dateClosed.setNew(true);
            dateClosed.setValue(null, "");
            dateClosed.setRequired(true);
            validator.hookupTextField(dateClosed,
                    "2",
                    true,
                    UIValidator.Type.Changed,  
                    "", 
                    false);
            summaryLabel = createLabel("");
            pb.add(summaryLabel,                     cc.xy(1, 1));
            pb.add(createI18NLabel("LOANRET_AGENT"), cc.xy(3, 1));
            pb.add(agentCBX = createAgentCombobox(), cc.xy(5, 1));
            pb.add(createI18NLabel("ON"),            cc.xy(7, 1));
            pb.add(dateClosed,                       cc.xy(9, 1));

            contentPanel.add(pb.getPanel(), BorderLayout.NORTH);
            contentPanel.add(new JScrollPane(mainPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
            
            JPanel p = new JPanel(new BorderLayout());
            p.setBorder(BorderFactory.createEmptyBorder(5, 0, 2, 0));
            p.add(ButtonBarFactory.buildOKCancelApplyBar(okBtn, cancel, selectAllBtn), BorderLayout.CENTER);
            contentPanel.add(p, BorderLayout.SOUTH);
            contentPanel.setBorder(BorderFactory.createEmptyBorder(4, 12, 2, 12));
            
            setContentPane(contentPanel);
            
            doEnableOKBtn();
    
            //setIconImage(IconManager.getIcon("Preparation", IconManager.IconSize.Std16).getImage());
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            
            doEnableOKBtn();
            
            okBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae)
                {
                    setVisible(false);
                    isCancelled = false;
                }
            });
            
            cancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae)
                {
                    setVisible(false);
                }
            });
            
            selectAllBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae)
                {
                    selectAllItems();
                }
            });
            
            pack();
            
            Dimension size = getPreferredSize();
            size.width += 20;
            size.height = size.height > 500 ? 500 : size.height;
            setSize(size);
            
            return true;
        
        } catch (Exception ex)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(LoanReturnDlg.class, ex);
            // Error Dialog
            ex.printStackTrace();
            
            if (loan.getLoanNumber() == null) {
            	JOptionPane.showMessageDialog(null, getResourceString("UNSAVED_LOAN_ERROR"), getResourceString("Error"), JOptionPane.ERROR_MESSAGE);
            }
            
        } finally 
        {
            if (session != null)
            {
                session.close();
            }
        }
        return false;
    }
    
    /**
     * 
     */
    protected void doEnableOKBtn()
    {
        int retCnt = 0;
        int resCnt = 0;
        for (ColObjPanel colObjPanel : colObjPanels)
        {
            retCnt += colObjPanel.getItemReturnedCount() + colObjPanel.getTypeReturnedCount() + colObjPanel.getNonSpecimenReturnedCount();
            //resCnt += colObjPanel.getItemResolvedCount() + colObjPanel.getTypeResolvedCount();
        }
        //okBtn.setEnabled((retCnt > 0 || resCnt > 0) && agentCBX.getValue() != null && dateClosed.getValue() != null);
        okBtn.setEnabled((retCnt > 0) && agentCBX.getValue() != null && dateClosed.getValue() != null);

        summaryLabel.setText(String.format(getResourceString("LOANRET_NUM_ITEMS_2B_RET_FMT"), retCnt, resCnt));
    }
    
    /**
     * @return the return agent combobox
     */
    protected ValComboBoxFromQuery createAgentCombobox()
    {
        FormCellField fcf = new FormCellField(FormCellIFace.CellType.field,
                                               "1", "agent", FormCellFieldIFace.FieldType.querycbx, 
                                               FormCellFieldIFace.FieldType.querycbx, 
                                               "", "Agent", "", true,
                                               1, 1, 1, 1, "Changed", null, false);
        fcf.addProperty("name", "Agent");
        fcf.addProperty("title", getResourceString("LOANRET_AGENT_DO_RET_TITLE"));
        ValComboBoxFromQuery cbx = ViewFactory.createQueryComboBox(validator, fcf, true, true);
        cbx.setAsNew(true);
        cbx.setState(UIValidatable.ErrorType.Incomplete);
        return cbx;
    }
    
    
    /**
     * Sets all the spinners to there max values.
     */
    protected void selectAllItems()
    {
        for (ColObjPanel colObjPanel : colObjPanels)
        {
            for (PrepPanel pp : colObjPanel.getPanels())
            {
                pp.selectAllItems();
            }
        }
    }
    
    /**
     * Returns whether the dialog was canceled.
     * @return whether the dialog was canceled.
     */
    public boolean isCancelled()
    {
        return isCancelled;
    }

    /**
     * Returns the agent that is doing the return.
     * @return the agent that is doing the return.
     */
    public Agent getAgent()
    {
        return (Agent)agentCBX.getValue();
    }
    
    /**
     * Returns a Hastable of Preparation to Count.
     * @return a Hastable of Preparation to Count.
     */
    public List<LoanReturnInfo> getLoanReturnInfo()
    {
        List<LoanReturnInfo> returns = new Vector<LoanReturnInfo>();
        
        for (ColObjPanel colObjPanel : colObjPanels)
        {
            for (PrepPanel pp : colObjPanel.getPanels())
            {
                //if ((pp.getItemReturnedCount() + pp.getTypeReturnedCount()) > 0 || (pp.getItemResolvedCount() + pp.getTypeResolvedCount()) > 0)
            	if (pp.getItemReturnedCount() > 0 || pp.getTypeReturnedCount() > 0 || pp.getNonSpecimenReturnedCount() > 0)
                {
                    returns.add(pp.getLoanReturnInfo());
                }
            }
        }
        return returns;
    }

    //------------------------------------------------------------------------------------------
    //
    //------------------------------------------------------------------------------------------
    
    /**
     * The panel that contains the components for collection objects.
     */
    class ColObjPanel extends JPanel
    {
        protected CollectionObject  colObj;
        protected JCheckBox         checkBox;
        protected Vector<PrepPanel> panels = new Vector<PrepPanel>();       
        protected JDialog           dlgParent;
        
        /**
         * @param session
         * @param dlgParent
         * @param colObj
         * @param lpoList
         */
        public ColObjPanel(final DataProviderSessionIFace session,
                           final JDialog               dlgParent, 
                           final CollectionObject      colObj,
                           final List<LoanPreparation> lpoList)
        {
            super();
            this.dlgParent = dlgParent;
            this.colObj    = colObj;
            
            session.attach(colObj);
            
            setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
            //setBorder(BorderFactory.createCompoundBorder(new CurvedBorder(new Color(160,160,160)), getBorder()));
            //setBorder(new CurvedBorder(new Color(160,160,160)));
     
            PanelBuilder    pbuilder = new PanelBuilder(new FormLayout("f:p:g", "p,5px,p"), this);
            CellConstraints cc      = new CellConstraints();
     
            String taxonName = "";
            String identifier = "";
            
            Set<Determination> determinations = new HashSet<Determination>(); // mmk fragment
            
            
        	for (Fragment frag : colObj.getFragments())
        	{
        		determinations.addAll(frag.getDeterminations());
        		identifier = frag.getIdentifier();
        	}                                                                 // mmk fragment
        	
        	
        	// dl: get the taxon name for display (the original dialog displayed only the identifier)
            for (Determination deter : determinations)
            {
                if (deter.isCurrentDet())
                {
                    if (deter.getPreferredTaxon().getFullName() == null)
                    {
                        Taxon parent = deter.getPreferredTaxon().getParent();
                        String genus = parent.getFullName() == null ? parent.getName() : parent.getFullName();
                        taxonName = genus + " " + deter.getPreferredTaxon().getName();
                        
                    } else
                    {
                        taxonName = deter.getPreferredTaxon().getFullName();
                    }

                    break;
                }
            }
            
            String descr = String.format("%s: %s - %s", identifier, colObj.getIdentityTitle(), taxonName);
            descr = StringUtils.stripToEmpty(descr);
            
            checkBox = createCheckBox(descr);
            pbuilder.add(createLabel(descr), cc.xy(1,1));
            checkBox.setSelected(true);
            
            JPanel outerPanel = new JPanel();
            outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));
            outerPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
            
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            outerPanel.add(contentPanel);
            
            Color[] colors = new Color[] { new Color(255,255,255), new Color(235,235,255)};
            
            int i = 0;
            for (LoanPreparation lpo : lpoList)
            {
                PrepPanel pp = new PrepPanel(dlgParent, lpo);
                panels.add(pp);
                pp.setBackground(colors[i % 2]);
                contentPanel.add(pp);
                i++;
            }
            pbuilder.add(outerPanel, cc.xy(1,3));
            
            checkBox.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent ae)
                {
                    for (PrepPanel pp : panels)
                    {
                        pp.setEnabled(checkBox.isSelected());
                    }
                    repaint();
                }
            });
        }
        
        public void addActionListener(final ActionListener al, final ChangeListener cl)
        {
            checkBox.addActionListener(al);
            
            for (PrepPanel pp : panels)
            {
                pp.addChangeListener(cl);
            }
        }
        
        public JCheckBox getCheckBox()
        {
            return checkBox;
        }
        
        public int getItemReturnedCount()
        {
            int count = 0;
            if (checkBox.isSelected())
            {
                for (PrepPanel pp : panels)
                {
                    count += pp.getItemReturnedCount();
                }
            }
            return count;
        }
        
        public int getTypeReturnedCount()
        {
            int count = 0;
            if (checkBox.isSelected())
            {
                for (PrepPanel pp : panels)
                {
                    count += pp.getTypeReturnedCount();
                }
            }
            return count;
        }
        
        public int getNonSpecimenReturnedCount()
        {
            int count = 0;
            if (checkBox.isSelected())
            {
                for (PrepPanel pp : panels)
                {
                    count += pp.getNonSpecimenReturnedCount();
                }
            }
            return count;
        }

        /*public int getItemResolvedCount()
        {
            int count = 0;
            if (checkBox.isSelected())
            {
                for (PrepPanel pp : panels)
                {
                    count += pp.getItemResolvedCount();
                }
            }
            return count;
        }
        
        public int getTypeResolvedCount()
        {
            int count = 0;
            if (checkBox.isSelected())
            {
                for (PrepPanel pp : panels)
                {
                    count += pp.getTypeResolvedCount();
                }
            }
            return count;
        }*/

        public Vector<PrepPanel> getPanels()
        {
            return panels;
        }
        
       
    }
   
    //------------------------------------------------------------------------------------------
    // Return panel for each LoanPreparation.
    //------------------------------------------------------------------------------------------
    /**
     * Panel that contains all the components for selecting preparations for return in each loan.
     */
    class PrepPanel extends JPanel implements ActionListener
    {
        protected Preparation prep;
        protected LoanPreparation lpo;
        protected JLabel      label       = null;
        protected JLabel      retLabel    = null;
        //protected JLabel      resLabel    = null;
        protected JComponent  prepInfoBtn = null;
        protected JSpinner    itemReturnedSpinner; 
        //protected JSpinner    itemresolvedSpinner; 
        
        protected JSpinner    typeReturnedSpinner; 
        //protected JSpinner    typeResolvedSpinner; 
        
        protected JSpinner    nonSpecimenReturnedSpinner; 
        //protected JSpinner    nonSpecimenResolvedSpinner; 
        
        protected JDialog     parent;
        protected JTextField  remarks;
        
        protected int         itemQuantityReturned = 0;
        //protected int         itemQuantityResolved;
        protected int         itemQuantityLoaned;
        
        protected int         typeQuantityReturned = 0;
        //protected int         typeQuantityResolved;
        protected int         typeQuantityLoaned;
        
        protected int         nonSpecimenQuantityReturned = 0;
        //protected int         nonSpecimenQuantityResolved;
        protected int         nonSpecimenQuantityLoaned;
        
        
        protected int         maxValue = 0;
        protected boolean     unknownQuantity;

        /**
         * Constructs a panel representing the Preparation being returned.
         * @param parent the parent dialog
         * @param lpo the LoanPreparation being returned
         */
        public PrepPanel(final JDialog parent, 
                         final LoanPreparation lpo)
        {
            super();
            this.prep   = lpo.getPreparation();
            this.lpo    = lpo;
            this.parent = parent;
            
            
            Color color = new Color(192, 192, 192);
            Color bg = color.darker();
            Color fg = color.brighter();
            
            setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
            //setBorder(BorderFactory.createCompoundBorder(new CurvedBorder(new Color(160,160,160)), getBorder()));
     
            FormLayout fl = new FormLayout("max(120px;p),p," +
                                           "50px,2px,f:p:g", // 3,4,5
                                           //"50px,2px,p,p," + // 7,8,9,10
                                           //"f:p:g", //"p,0px,p:g", 
                                           "f:p:g,2px,f:p:g,2px,f:p:g,2px,p");
            PanelBuilder    pbuilder = new PanelBuilder(fl, this);
            CellConstraints cc       = new CellConstraints();
            
            int x = 1;
            int y = -1;
            
            pbuilder.add(label = createLabel(prep.getPrepType().getName()), cc.xy(x,1)); x += 1;  // 1
            label.setOpaque(false);
            
            boolean allItemReturned = false;
            boolean allTypeReturned = false;
            boolean allNonSpecimenReturned = false;
            
            //dl: beginning of code for components specific to the selection of items.
            
            if (prep.getCountAmt() !=  null)
            {
            	itemQuantityLoaned    = lpo.getItemCount();
            	for (LoanReturnPreparation lrpo : lpo.getLoanReturnPreparations())
            		if (lrpo.getItemCount() != null) itemQuantityReturned += lrpo.getItemCount();
                //itemQuantityResolved  = lpo.getQuantityResolved();
                
                //int quantityResOut   = itemQuantityLoaned - itemQuantityResolved;
                int quantityRetOut   = itemQuantityLoaned - itemQuantityReturned;
                
                //if ((quantityResOut > 0 || quantityRetOut > 0) && !lpo.getIsResolved())
                if ((quantityRetOut > 0) && !lpo.getIsResolved())
                {
                    maxValue = itemQuantityLoaned;
                    y += 2;
                    pbuilder.add(new VerticalSeparator(fg, bg, 20), cc.xy(x,y)); x += 1; // 2
                    
                    SpinnerModel retModel = new SpinnerNumberModel(itemQuantityReturned, //initial value
                                               itemQuantityReturned, //min
                                               itemQuantityLoaned,   //max
                                               1);               //step
                    itemReturnedSpinner = new JSpinner(retModel);
                    fixBGOfJSpinner(itemReturnedSpinner);
                    pbuilder.add(itemReturnedSpinner, cc.xy(x, y)); x += 2; // 3
                    setControlSize(itemReturnedSpinner);
                    
                    String fmtStr = String.format(getResourceString("LOANRET_OF_ITEM_FORMAT_RET"), itemQuantityLoaned);
                    pbuilder.add(retLabel = createLabel(fmtStr), cc.xy(x, y)); x += 2; // 5
                    
                    //pbuilder.add(new VerticalSeparator(fg, bg, 20), cc.xy(x,1)); x += 1; // 6
                    
                    /* SpinnerModel resModel = new SpinnerNumberModel(itemQuantityResolved, //initial value
                            itemQuantityResolved, //min
                            itemQuantityLoaned,   //max
                            1);               //step
                    resolvedSpinner = new JSpinner(resModel);
                    fixBGOfJSpinner(resolvedSpinner);
                    pbuilder.add(resolvedSpinner, cc.xy(x, 1)); x += 2; // 7
                    setControlSize(resolvedSpinner);
                    
                    fmtStr = String.format(getResourceString("LOANRET_OF_ITEM_FORMAT_RES"), itemQuantityLoaned);
                    pbuilder.add(retLabel = createLabel(fmtStr), cc.xy(x, 1)); x += 1; // 9

                    ChangeListener cl = new ChangeListener()
                    {
                        @Override
                        public void stateChanged(ChangeEvent e)
                        {
                            int lrpResolvedQty = (Integer)resolvedSpinner.getValue();
                            int lrpReturnedQty = (Integer)returnedSpinner.getValue();
                            
                            if (e != null)
                            {
                                if (e.getSource() == resolvedSpinner)
                                {
                                    if (lrpResolvedQty < lrpReturnedQty)
                                    {
                                        lrpReturnedQty = lrpResolvedQty;
                                        final int qty = lrpReturnedQty;
                                        SwingUtilities.invokeLater(new Runnable() {
                                            @Override
                                            public void run()
                                            {
                                                returnedSpinner.setValue(qty);
                                            }
                                        });
                                    }
                                } else if (e.getSource() == returnedSpinner)
                                {
                                    if (lrpReturnedQty > lrpResolvedQty)
                                    {
                                        lrpResolvedQty = lrpReturnedQty;
                                        final int qty = lrpReturnedQty;
                                        SwingUtilities.invokeLater(new Runnable() {
                                            @Override
                                            public void run()
                                            {
                                                resolvedSpinner.setValue(qty);
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    };
                    returnedSpinner.addChangeListener(cl);
                    resolvedSpinner.addChangeListener(cl);*/
                    
                } else
                {
                    Calendar lastReturnDate = null;
                    for (LoanReturnPreparation lrpo : lpo.getLoanReturnPreparations())
                    {
                        Calendar retDate = lrpo.getReturnedDate();
                        if (retDate != null)
                        {
                            if (lastReturnDate == null)
                            {
                                lastReturnDate = lrpo.getReturnedDate();
                                
                            } else if (retDate.after(lastReturnDate))
                            {
                                lastReturnDate = retDate;
                            }
                        }
                    }
                    
                    allItemReturned = true;
                    
                    if (itemQuantityLoaned > 0) {
                    y += 2;
                    pbuilder.add(new VerticalSeparator(fg, bg, 20), cc.xy(x,y)); x += 1; // 2
                    String fmtStr = lastReturnDate == null ? getResourceString("LOANRET_ALL_ITEM_RETURNED") :
                                 String.format(getResourceString("LOANRET_ALL_ITEM_RETURNED_ON_FMT"), 
                                               scrDateFormat.format(lastReturnDate));
                    pbuilder.add(retLabel = createLabel(fmtStr), cc.xywh(x, y, 3, 1));
                    }
                }

                
            } else
            {
                pbuilder.add(retLabel = createLabel(" " + getResourceString("LOANRET_UNKNOWN_NUM_ITEMS_AVAIL")), cc.xywh(1, y, 4, 1));
                unknownQuantity = true;
            }
            
            x = 2; //dl
            
            //dl: beginning of code for components specific to the selection of type specimens.
            
            if (prep.getCountAmt() !=  null)
            {
                typeQuantityLoaned = lpo.getTypeCount();
            	for (LoanReturnPreparation lrpo : lpo.getLoanReturnPreparations())
            		if (lrpo.getTypeCount() != null) typeQuantityReturned += lrpo.getTypeCount();
                //typeQuantityResolved = lpo.getQuantityResolved();
                
                //int quantityResOut   = typeQuantityLoaned - typeQuantityResolved;
                int quantityRetOut   = typeQuantityLoaned - typeQuantityReturned;
                
                //if ((quantityResOut > 0 || quantityRetOut > 0) && !lpo.getIsResolved())
                if ((quantityRetOut > 0) && !lpo.getIsResolved())
                {
                	y += 2;
                    maxValue = typeQuantityLoaned;
                    
                    pbuilder.add(new VerticalSeparator(fg, bg, 20), cc.xy(x,y)); x += 1; // 2
                    
                    SpinnerModel retModel = new SpinnerNumberModel(typeQuantityReturned, //initial value
                                               typeQuantityReturned, //min
                                               typeQuantityLoaned,   //max
                                               1);               //step

                    typeReturnedSpinner = new JSpinner(retModel);//dl
                    fixBGOfJSpinner(typeReturnedSpinner);
                    pbuilder.add(typeReturnedSpinner, cc.xy(x, y)); x += 2; // 3
                    setControlSize(typeReturnedSpinner);
                    
                    String fmtStr = String.format(getResourceString("LOANRET_OF_TYPE_FORMAT_RET"), typeQuantityLoaned);
                    pbuilder.add(retLabel = createLabel(fmtStr), cc.xy(x, y)); x += 1; // 5
                    
                    //pbuilder.add(new VerticalSeparator(fg, bg, 20), cc.xy(x,3)); x += 1; // 6
                    /*
                    SpinnerModel resModel = new SpinnerNumberModel(typeQuantityResolved, //initial value
                            typeQuantityResolved, //min
                            typeQuantityLoaned,   //max
                            1);               //step
                    resolvedSpinner2 = new JSpinner(resModel);
                    fixBGOfJSpinner(resolvedSpinner2);
                    pbuilder.add(resolvedSpinner2, cc.xy(x, 3)); x += 2; // 7
                    setControlSize(resolvedSpinner2);
                    
                    fmtStr = String.format(getResourceString("LOANRET_OF_TYPE_FORMAT_RES"), typeQuantityLoaned);
                    pbuilder.add(retLabel = createLabel(fmtStr), cc.xy(x, 3)); x += 1; // 9
                    
                    ChangeListener cl = new ChangeListener()
                    {
                        @Override
                        public void stateChanged(ChangeEvent e)
                        {
                            //int lrpResolvedQty = (Integer)resolvedSpinner2.getValue();
                            int lrpReturnedQty = (Integer)returnedSpinner2.getValue();
                            
                            if (e != null)
                            {
                                if (e.getSource() == resolvedSpinner2)
                                {
                                    if (lrpResolvedQty < lrpReturnedQty)
                                    {
                                        lrpReturnedQty = lrpResolvedQty;
                                        final int qty = lrpReturnedQty;
                                        SwingUtilities.invokeLater(new Runnable() {
                                            @Override
                                            public void run()
                                            {
                                                returnedSpinner2.setValue(qty);
                                            }
                                        });
                                    }
                                } else if (e.getSource() == returnedSpinner2)
                                {
                                    if (lrpReturnedQty > lrpResolvedQty)
                                    {
                                        lrpResolvedQty = lrpReturnedQty;
                                        final int qty = lrpReturnedQty;
                                        SwingUtilities.invokeLater(new Runnable() {
                                            @Override
                                            public void run()
                                            {
                                                resolvedSpinner2.setValue(qty);
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    };
                    returnedSpinner2.addChangeListener(cl);
                    resolvedSpinner2.addChangeListener(cl);*/
                    
                } else
                {
                    Calendar lastReturnDate = null;
                    for (LoanReturnPreparation lrpo : lpo.getLoanReturnPreparations())
                    {
                        Calendar retDate = lrpo.getReturnedDate();
                        if (retDate != null)
                        {
                            if (lastReturnDate == null)
                            {
                                lastReturnDate = lrpo.getReturnedDate();
                                
                            } else if (retDate.after(lastReturnDate))
                            {
                                lastReturnDate = retDate;
                            }
                        }
                    }
                    
                    allTypeReturned = true;
                    
                    if (typeQuantityLoaned > 0) {
                    y += 2;
                    pbuilder.add(new VerticalSeparator(fg, bg, 20), cc.xy(x,y)); x += 1; // 2
                    String fmtStr = lastReturnDate == null ? getResourceString("LOANRET_ALL_TYPE_RETURNED") :
                                 String.format(getResourceString("LOANRET_ALL_TYPE_RETURNED_ON_FMT"), 
                                               scrDateFormat.format(lastReturnDate));
                    pbuilder.add(retLabel = createLabel(fmtStr), cc.xywh(x, y, 3, 1));
                    }
                }

                
            } else
            {
                pbuilder.add(retLabel = createLabel(" " + getResourceString("LOANRET_UNKNOWN_NUM_TYPE_AVAIL")), cc.xywh(1, y, 4, 1));
                unknownQuantity = true;
            }
            
            x = 2;
            
            //dl: beginning of code for components specific to the selection of non specimens.
            
            if (prep.getCountAmt() !=  null)
            {
                nonSpecimenQuantityLoaned = lpo.getNonSpecimenCount() != null ? lpo.getNonSpecimenCount() : 0;
            	for (LoanReturnPreparation lrpo : lpo.getLoanReturnPreparations())
            		if (lrpo.getNonSpecimenCount() != null) nonSpecimenQuantityReturned += lrpo.getNonSpecimenCount();
                //typeQuantityResolved = lpo.getQuantityResolved();
                
                //int quantityResOut   = typeQuantityLoaned - typeQuantityResolved;
                int quantityRetOut   = nonSpecimenQuantityLoaned - nonSpecimenQuantityReturned;
                
                //if ((quantityResOut > 0 || quantityRetOut > 0) && !lpo.getIsResolved())
                if ((quantityRetOut > 0) && !lpo.getIsResolved())
                {
                	y += 2;
                    maxValue = nonSpecimenQuantityLoaned;
                    
                    pbuilder.add(new VerticalSeparator(fg, bg, 20), cc.xy(x,y)); x += 1; // 2
                    
                    SpinnerModel retModel = new SpinnerNumberModel(nonSpecimenQuantityReturned, //initial value
                                               nonSpecimenQuantityReturned, //min
                                               nonSpecimenQuantityLoaned,   //max
                                               1);               //step

                    nonSpecimenReturnedSpinner = new JSpinner(retModel);//dl
                    fixBGOfJSpinner(nonSpecimenReturnedSpinner);
                    pbuilder.add(nonSpecimenReturnedSpinner, cc.xy(x, y)); x += 2; // 3
                    setControlSize(nonSpecimenReturnedSpinner);
                    
                    String fmtStr = String.format(getResourceString("LOANRET_OF_NON_SPECIMEN_FORMAT_RET"), nonSpecimenQuantityLoaned);
                    pbuilder.add(retLabel = createLabel(fmtStr), cc.xy(x, y)); x += 1; // 5
                    
                    //pbuilder.add(new VerticalSeparator(fg, bg, 20), cc.xy(x,3)); x += 1; // 6
                    /*
                    SpinnerModel resModel = new SpinnerNumberModel(typeQuantityResolved, //initial value
                            typeQuantityResolved, //min
                            typeQuantityLoaned,   //max
                            1);               //step
                    resolvedSpinner2 = new JSpinner(resModel);
                    fixBGOfJSpinner(resolvedSpinner2);
                    pbuilder.add(resolvedSpinner2, cc.xy(x, 3)); x += 2; // 7
                    setControlSize(resolvedSpinner2);
                    
                    fmtStr = String.format(getResourceString("LOANRET_OF_TYPE_FORMAT_RES"), typeQuantityLoaned);
                    pbuilder.add(retLabel = createLabel(fmtStr), cc.xy(x, 3)); x += 1; // 9
                    
                    ChangeListener cl = new ChangeListener()
                    {
                        @Override
                        public void stateChanged(ChangeEvent e)
                        {
                            //int lrpResolvedQty = (Integer)resolvedSpinner2.getValue();
                            int lrpReturnedQty = (Integer)returnedSpinner2.getValue();
                            
                            if (e != null)
                            {
                                if (e.getSource() == resolvedSpinner2)
                                {
                                    if (lrpResolvedQty < lrpReturnedQty)
                                    {
                                        lrpReturnedQty = lrpResolvedQty;
                                        final int qty = lrpReturnedQty;
                                        SwingUtilities.invokeLater(new Runnable() {
                                            @Override
                                            public void run()
                                            {
                                                returnedSpinner2.setValue(qty);
                                            }
                                        });
                                    }
                                } else if (e.getSource() == returnedSpinner2)
                                {
                                    if (lrpReturnedQty > lrpResolvedQty)
                                    {
                                        lrpResolvedQty = lrpReturnedQty;
                                        final int qty = lrpReturnedQty;
                                        SwingUtilities.invokeLater(new Runnable() {
                                            @Override
                                            public void run()
                                            {
                                                resolvedSpinner2.setValue(qty);
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    };
                    returnedSpinner2.addChangeListener(cl);
                    resolvedSpinner2.addChangeListener(cl);*/
                    
                } else
                {
                    Calendar lastReturnDate = null;
                    for (LoanReturnPreparation lrpo : lpo.getLoanReturnPreparations())
                    {
                        Calendar retDate = lrpo.getReturnedDate();
                        if (retDate != null)
                        {
                            if (lastReturnDate == null)
                            {
                                lastReturnDate = lrpo.getReturnedDate();
                                
                            } else if (retDate.after(lastReturnDate))
                            {
                                lastReturnDate = retDate;
                            }
                        }
                    }
                    
                    allNonSpecimenReturned = true;
                    
                    if (nonSpecimenQuantityLoaned > 0) {
                    y += 2;
                    pbuilder.add(new VerticalSeparator(fg, bg, 20), cc.xy(x,y)); x += 1; // 2
                    String fmtStr = lastReturnDate == null ? getResourceString("LOANRET_ALL_NON_SPECIMEN_RETURNED") :
                                 String.format(getResourceString("LOANRET_ALL_NON_SPECIMEN_RETURNED_ON_FMT"), 
                                               scrDateFormat.format(lastReturnDate));
                    pbuilder.add(retLabel = createLabel(fmtStr), cc.xywh(x, y, 3, 1));
                    }
                }

                
            } else
            {
                pbuilder.add(retLabel = createLabel(" " + getResourceString("LOANRET_UNKNOWN_NUM_NON_SPECIMEN_AVAIL")), cc.xywh(1, 5, y, 1));
                unknownQuantity = true;
            }

            if (!allItemReturned || !allTypeReturned || !allNonSpecimenReturned)
            {
                remarks = new RemarksText();
                pbuilder.add(remarks, cc.xywh(1, y+2, 5, 1)); //dl changed 3 to 5
            }
            
            /*if (returnedSpinner != null)
            {
                returnedSpinner.addChangeListener(new ChangeListener()
                {
                    public void stateChanged(ChangeEvent ae)
                    {
                        Integer val = (Integer)returnedSpinner.getValue();
                    }
                });
            }*/
        }
        
        
		

		/**
         * Changes the BG color fo the text field in the spinner to the required color.
         * @param spin the spinner to be changed
         */
        protected void fixBGOfJSpinner(final JSpinner spin)
        {
            JComponent edComp = spin.getEditor();
            for (int i=0;i<edComp.getComponentCount();i++)
            {
                Component c = edComp.getComponent(i);
                if (c instanceof JTextField)
                {
                    c.setBackground(requiredfieldcolor.getColor());
                }
            }
        }
        
        /**
         * Return whether their is an unknown quantity.
         * @return whether their is an unknown quantity.
         */
        public boolean isUnknownQuantity()
        {
            return unknownQuantity;
        }

        /**
         * Sets all the spinners to there max values.
         */
        public void selectAllItems()
        {
            if (itemReturnedSpinner != null)
            {
                itemReturnedSpinner.setValue(itemQuantityLoaned);
            }
            if (typeReturnedSpinner != null)
            {
            typeReturnedSpinner.setValue(typeQuantityLoaned);
            }
            if (nonSpecimenReturnedSpinner != null)
            {
            nonSpecimenReturnedSpinner.setValue(nonSpecimenQuantityLoaned);
            }
            /*if (itemResolvedSpinner != null)
            {
                itemResolvedSpinner.setValue(itemQuantityLoaned);
                typeResolvedSpinner.setValue(typeQuantityLoaned);
            }*/
            /*if (itemResolvedSpinner != null)
            {
                itemResolvedSpinner.setValue(itemQuantityLoaned);
            }*/
            /*if (nonSpecimenResolvedSpinner != null)
            {
                nonSpecimenResolvedSpinner.setValue(nonSpecimenQuantityLoaned);
            }*/
        }

        /**
         * Returns the LoanPreparation for this panel.
         * @return the LoanPreparation for this panel.
         */
        public LoanPreparation getLoanPreparation()
        {
            return lpo;
        }

        /* (non-Javadoc)
         * @see javax.swing.JComponent#setEnabled(boolean)
         */
        public void setEnabled(final boolean enabled)
        {
            if (label != null)
            {
                label.setEnabled(enabled);
            }
            if (retLabel != null)
            {
                retLabel.setEnabled(enabled);
            }
            /*if (resLabel != null)
            {
                resLabel.setEnabled(enabled);
            }*/
            if (prepInfoBtn != null)
            {
                prepInfoBtn.setEnabled(enabled);
            }
            if (itemReturnedSpinner != null)
            {
                itemReturnedSpinner.setEnabled(enabled);
            }
            if (typeReturnedSpinner != null)
            {
                typeReturnedSpinner.setEnabled(enabled);
            }
            if (nonSpecimenReturnedSpinner != null)
            {
                nonSpecimenReturnedSpinner.setEnabled(enabled);
            }
        }
        
        /**
         * Adds a change listener.
         * @param cl the change listener
         */
        public void addChangeListener(final ChangeListener cl)
        {
            if (itemReturnedSpinner != null)
            {
                itemReturnedSpinner.addChangeListener(cl);
            }
            /*if (resolvedSpinner != null)
            {
                resolvedSpinner.addChangeListener(cl);
            }*/
        }
        
        /**
         * Returns the count from the spinner, the count of items being returning.
         * @return the count from the spinner, the count of items being returning.
         */
        public int getItemReturnedCount()
        {
            if (itemReturnedSpinner != null)
            {
                Object valObj = itemReturnedSpinner.getValue();
               return valObj == null ? 0 : ((Integer)valObj).intValue() - itemQuantityReturned;
                
            }
            // else
            return 0;
        }
        
        public int getTypeReturnedCount()
        {
            if (typeReturnedSpinner != null)
            {
                Object valObj = typeReturnedSpinner.getValue();
               return valObj == null ? 0 : ((Integer)valObj).intValue() - typeQuantityReturned;
                
            }
            // else
            return 0;
        }
        
        public int getNonSpecimenReturnedCount()
        {
            if (nonSpecimenReturnedSpinner != null)
            {
                Object valObj = nonSpecimenReturnedSpinner.getValue();
               return valObj == null ? 0 : ((Integer)valObj).intValue() - nonSpecimenQuantityReturned;
                
            }
            // else
            return 0;
        }
        
        public boolean getIsResolved() {
        	int returnedCount = getItemResolvedCount() + getTypeResolvedCount() + getNonSpecimenResolvedCount();
        	int loanedCount = itemQuantityLoaned + typeQuantityLoaned + nonSpecimenQuantityLoaned;
        	
        	return returnedCount == loanedCount;
        }
        
        public int getItemResolvedCount()
        {
        	return getItemReturnedCount() + itemQuantityReturned;
        }
        
        public int getTypeResolvedCount()
        {
        	return getTypeReturnedCount() + typeQuantityReturned;
        }
        
        public int getNonSpecimenResolvedCount()
        {
        	return getNonSpecimenReturnedCount() + nonSpecimenQuantityReturned;
        }
        
        /**
         * Returns the LoanReturnInfo describing the user input for the loan return.
         * @return the LoanReturnInfo describing the user input for the loan return
         */
        public LoanReturnInfo getLoanReturnInfo()
        {	
            return new LoanReturnInfo(lpo, 
                                      remarks != null ? remarks.getText() : null,
                                      getItemReturnedCount(), getTypeReturnedCount(), getNonSpecimenReturnedCount(),
                                      (Calendar) dateClosed.getValue(), getIsResolved());
        }
        
        /* (non-Javadoc)
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        public void actionPerformed(ActionEvent e)
        {
            List<Loan> loans = new Vector<Loan>();
            
            DataProviderSessionIFace session = null;
            try
            {
                session = DataProviderFactory.getInstance().createSession();
                
                session.attach(prep);

                for (LoanPreparation loanPO : prep.getLoanPreparations())
                {
                    loans.add(loanPO.getLoan());
                }
            } catch (Exception ex)
            {
                edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(LoanReturnDlg.class, ex);
                // Error Dialog
                ex.printStackTrace();
                
            } finally 
            {
                if (session != null)
                {
                    session.close();
                }
            }
            
            ViewIFace view  = AppContextMgr.getInstance().getView("Loan");
            final ViewBasedDisplayDialog dlg = new ViewBasedDisplayDialog(parent,
                    view.getViewSetName(),
                    "Loan",
                    null,
                    getResourceString("LoanSelectPrepsDlg.IAT_LOAN_REVIEW"),
                    getResourceString("CLOSE"),
                    null, // className,
                    null, // idFieldName,
                    false, // isEdit,
                    MultiView.RESULTSET_CONTROLLER);
            
            MultiView mv = dlg.getMultiView();
            Viewable currentViewable = mv.getCurrentView();
            if (currentViewable != null && currentViewable instanceof FormViewObj)
            {
                FormViewObj formViewObj = (FormViewObj)currentViewable;
                Component comp      = formViewObj.getControlByName("generateInvoice");
                if (comp instanceof JCheckBox)
                {
                    comp.setVisible(false);
                }

            }
            dlg.setModal(true);
            dlg.setData(loans);
            dlg.setVisible(true);
        }
    }

    //------------------------------------------------------------------------------------------
    //
    //------------------------------------------------------------------------------------------
    /**
     * Contains information about the loan return for use when creating a loanReturnPreparation obj.
     */
    public class LoanReturnInfo
    {
        protected LoanPreparation lpo;
        protected boolean         isResolved;
        protected String          remarks;
        protected int             returnedQty;
        protected int             resolvedQty;
		protected int itemCount;
		protected int typeCount;
		protected int nonSpecimenCount;
		protected Calendar returnedDate;
        
        public LoanReturnInfo(LoanPreparation lpo, 
                              String remarks, 
                              //int returnedQty,
                              //int resolvedQty,
                              int itemCount,
                              int typeCount,
                              int nonSpecimenCount,
                              Calendar returnedDate,
                              boolean isResolved)
        {
            super();
            this.lpo = lpo;
            this.remarks = remarks;
            ///this.returnedQty = returnedQty;
            //this.resolvedQty = resolvedQty;
            this.returnedQty = 0;
            this.resolvedQty = 0;
            this.isResolved  = isResolved;
            this.itemCount = itemCount;
            this.typeCount = typeCount;
            this.nonSpecimenCount = nonSpecimenCount;
            this.returnedDate = returnedDate;
        }
        public LoanPreparation getLoanPreparation()
        {
            return lpo;
        }
        public String getRemarks()
        {
            return remarks;
        }
        public int getReturnedQty()
        {
            return returnedQty;
        }
        public int getResolvedQty()
        {
            return resolvedQty;
        }
        /**
         * @return the isResolved
         */
        public boolean isResolved()
        {
            return isResolved;
        }
		public int getItemCount() {
			return itemCount;
		}
		public int getTypeCount() {
			return typeCount;
		}
		public int getNonSpecimenCount() {
			return nonSpecimenCount;
		}
		public Calendar getReturnedDate() {
			return returnedDate;
		}
		
        
    }
    
    class RemarksText extends JTextField
    {
        protected Insets inner;
        protected String bgStr     = getResourceString("LOANRET_REMARKS");
        protected Point  pnt       = null;
        protected Color  textColor = new Color(0,0,0,64);
        
        public RemarksText()
        {
            inner = getInsets();
        }
        
        /* (non-Javadoc)
         * @see java.awt.Component#paint(java.awt.Graphics)
         */
        @Override
        public void paint(Graphics g)
        {
            super.paint(g);

            String text = getText();

            if (text == null || text.length() == 0)
            {
                if (pnt == null)
                {
                    FontMetrics fm   = g.getFontMetrics();
                    pnt = new Point(inner.left, inner.top + fm.getAscent());
                }

                g.setColor(textColor);
                g.drawString(bgStr, pnt.x, pnt.y);
            }

        }
    }


}
