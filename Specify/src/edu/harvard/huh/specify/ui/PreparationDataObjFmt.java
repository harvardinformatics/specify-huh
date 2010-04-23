package edu.harvard.huh.specify.ui;


import static edu.ku.brc.helpers.XMLHelper.xmlAttr;

import java.util.Properties;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.ku.brc.af.ui.forms.FormHelper;
import edu.ku.brc.af.ui.forms.formatters.DataObjDataField;
import edu.ku.brc.af.ui.forms.formatters.DataObjDataFieldFormatIFace;
import edu.ku.brc.af.ui.forms.formatters.DataObjSwitchFormatter;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.AccessionPreparation;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Geography;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.ui.DocumentAdaptor;
import edu.ku.brc.ui.UIHelper;

/**
 * @author rod
 *
 * @code_status Alpha
 *
 * Mar 26, 2008
 *
 */
public class PreparationDataObjFmt implements DataObjDataFieldFormatIFace, Cloneable
{   
    // Needed for the Custom Editor
    protected ChangeListener changeListener = null;
    protected JTextField     textField      = null;

    /**
     * Constructor.
     */
    public PreparationDataObjFmt()
    {
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.DataObjDataFieldFormatIFace#format(java.lang.Object)
     */
    public String format(final Object dataValue)
    {
        if (dataValue == null)
        {
            return "";
        }
        
        if (!(dataValue instanceof Preparation))
        {
            throw new RuntimeException("The data value set into PreparationDataObjFmt is not a Preparation ["+dataValue.getClass().getSimpleName()+"]");
        }

        String restricted = FormHelper.checkForRestrictedValue(Preparation.getClassTableId());
        if (restricted != null)
        {
            return restricted;
        }

        restricted = FormHelper.checkForRestrictedValue(Loan.getClassTableId());
        if (restricted != null)
        {
            return restricted;
        }
        
        restricted = FormHelper.checkForRestrictedValue(LoanPreparation.getClassTableId());
        if (restricted != null)
        {
            return restricted;
        }
        
        restricted = FormHelper.checkForRestrictedValue(Accession.getClassTableId());
        if (restricted != null)
        {
            return restricted;
        }
        
        restricted = FormHelper.checkForRestrictedValue(AccessionPreparation.getClassTableId());
        if (restricted != null)
        {
            return restricted;
        }
        
        String loanNumber      = null;
        String accessionNumber = null;
        String taxonomy        = null;
        String geography       = null;
        String description     = null;

        String result = null;
        
        Preparation p = (Preparation)dataValue;

        DataProviderSessionIFace session = null;
        String errMsg = null;

        try
        {
            session = DataProviderFactory.getInstance().createSession();
            p = session.merge(p);

            if (p.getLoanPreparations() != null && p.getLoanPreparations().size() > 0)
            {
                for (LoanPreparation lpo : p.getLoanPreparations())
                {
                    Taxon tx = lpo.getTaxon();
                    if (tx != null) taxonomy = tx.getFullName();

                    description = lpo.getDescriptionOfMaterial();
                    loanNumber = lpo.getLoan().getLoanNumber();

                    result = loanNumber + " " +
                             (taxonomy != null ? taxonomy + " " : (description != null ? description : ""));
                    
                    break;
                }

            }
            else if (p.getAccessionPreparations() != null && p.getAccessionPreparations().size() > 0)
            {
                for (AccessionPreparation ap : p.getAccessionPreparations())
                {
                    Taxon tx = ap.getTaxon();
                    if (tx != null) taxonomy = tx.getFullName();
                    
                    Geography geo = ap.getGeography();
                    if (geo != null) geography = geo.getFullName();
                    
                    description = ap.getDescriptionOfMaterial();
                    accessionNumber = ap.getAccession().getAccessionNumber();
                    
                    result = accessionNumber + " " +
                             (taxonomy  != null ? taxonomy  + " " : "") + 
                             (geography != null ? geography + " " : "") +
                             (geography == null && taxonomy == null && description != null ? description : "");
                    
                    break;
                }
            }
            else
            {
                result = p.getIdentifier();
            }

        } catch (Exception ex)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(PreparationDataObjFmt.class, ex);
            errMsg = ex.toString();
            ex.printStackTrace();

        } finally
        {
            if (session != null)
            {
                session.close();
            }
        }
        if (errMsg != null)
        {
            throw new RuntimeException(errMsg);
        }

        return result != null ? result : "";
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.DataObjDataFieldFormatIFace#getDataClass()
     */
    @Override
    public Class<?> getDataClass()
    {
        return CollectionObject.class;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.DataObjDataFieldFormatIFace#getFields()
     */
    @Override
    public DataObjDataField[] getFields()
    {
        return null;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.DataObjDataFieldFormatIFace#getName()
     */
    @Override
    public String getName()
    {
        return "BatchPrepDetail";
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.DataObjDataFieldFormatIFace#getValue()
     */
    @Override
    public String getValue()
    {
        return null;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.DataObjDataFieldFormatIFace#setValue()
     */
    @Override
    public void setValue(String value)
    {
        return;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.DataObjDataFieldFormatIFace#isDefault()
     */
    @Override
    public boolean isDefault()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.DataObjDataFieldFormatIFace#isDirectFormatter()
     */
    @Override
    public boolean isDirectFormatter()
    {
        return true;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.formatters.DataObjDataFieldFormatIFace#getSingleField()
     */
    @Override
    public String getSingleField()
    {
        return null;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.DataObjDataFieldFormatIFace#setTableAndFieldInfo()
     */
    @Override
    public void setTableAndFieldInfo()
    {

    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.DataObjDataFieldFormatIFace#setDataObjSwitchFormatter(edu.ku.brc.ui.forms.formatters.DataObjSwitchFormatter)
     */
    @Override
    public void setDataObjSwitchFormatter(DataObjSwitchFormatter objFormatter)
    {
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.formatters.DataObjDataFieldFormatIFace#getCustomEditor(javax.swing.JButton)
     */
    @Override
    public JPanel getCustomEditor(final ChangeListener l)
    {
        this.changeListener = l;
        this.textField      = UIHelper.createTextField();
        
        CellConstraints cc = new CellConstraints();
        PanelBuilder    pb = new PanelBuilder(new FormLayout("f:p:g", "p"));
        pb.add(textField, cc.xy(1, 1));
        
        textField.setText("");
        
        textField.getDocument().addDocumentListener(new DocumentAdaptor() {
            @Override
            protected void changed(DocumentEvent e)
            {
                changeListener.stateChanged(new ChangeEvent(this));
            }
        });
        return pb.getPanel();
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.formatters.DataObjDataFieldFormatIFace#isCustom()
     */
    @Override
    public boolean isCustom()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.formatters.DataObjDataFieldFormatIFace#hasEditor()
     */
    @Override
    public boolean hasEditor()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.formatters.DataObjDataFieldFormatIFace#isValid()
     */
    @Override
    public boolean isValid()
    {
        return !textField.getText().isEmpty();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.formatters.DataObjDataFieldFormatIFace#getLabel()
     */
    @Override
    public String getLabel()
    {
        return "Format";
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.formatters.DataObjDataFieldFormatIFace#doneEditting()
     */
    @Override
    public void doneEditting(final boolean wasCancelled)
    {
        // TODO
    }

    @Override
    public void init(String name, Properties properties)
    {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.DataObjDataFieldFormatIFace#toXML(java.lang.StringBuilder)
     */
    @Override
    public void toXML(StringBuilder sb)
    {
        sb.append("          <external");
        xmlAttr(sb, "class", getClass().getName());
        sb.append("/>\n");
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone() throws CloneNotSupportedException
    {
        PreparationDataObjFmt codof = (PreparationDataObjFmt) super.clone();

        return codof;
    }
}
