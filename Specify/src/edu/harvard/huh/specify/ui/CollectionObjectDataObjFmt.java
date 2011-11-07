package edu.harvard.huh.specify.ui;


import static edu.ku.brc.helpers.XMLHelper.xmlAttr;
import static edu.ku.brc.ui.UIRegistry.getResourceString;

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
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterMgr;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.CollectingEvent;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Collector;
import edu.ku.brc.ui.DateWrapper;
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
public class CollectionObjectDataObjFmt implements DataObjDataFieldFormatIFace, Cloneable
{   
    // Needed for the Custom Editor
    protected ChangeListener changeListener = null;
    protected JTextField     textField      = null;
    
    String emptyCollectorName   = getResourceString("CollectionObjectDataObjFmt.EmptyCollectorName");
    String emptyCollectorNumber = getResourceString("CollectionObjectDataObjFmt.EmptyCollectorNumber");

    /**
     * Constructor.
     */
    public CollectionObjectDataObjFmt()
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
        
        if (!(dataValue instanceof CollectionObject))
        {
            throw new RuntimeException("The data value set into CollectionObjectDataObjFmt is not a CollectionObject ["+dataValue.getClass().getSimpleName()+"]");
        }

        String restricted = FormHelper.checkForRestrictedValue(CollectionObject.getClassTableId());
        if (restricted != null)
        {
            return restricted;
        }

        restricted = FormHelper.checkForRestrictedValue(Agent.getClassTableId());
        if (restricted != null)
        {
            return restricted;
        }
        
        String collectorName   = emptyCollectorName;
        String collectorNumber = emptyCollectorNumber;
        String collectionDate  = null;

        CollectionObject co = (CollectionObject)dataValue;

        DataProviderSessionIFace session = null;
        String errMsg = null;

        try
        {
            session = DataProviderFactory.getInstance().createSession();
            if (co.getId() != null) co = session.merge(co);

            if (co.getCollectingEvent() != null)
            {
                CollectingEvent ce = co.getCollectingEvent();
                if (ce.getCollectors().size() > 0)
                {
                    Collector ctr = ce.getCollectors().iterator().next();

                    if (ctr.getAgent() != null)
                    {
                        Agent agent = ctr.getAgent();
                        if (agent.getCollectorName() != null)
                        {
                            collectorName = agent.getCollectorName();
                        }

                        if (ctr.getEtAl() != null) collectorName += ", " + ctr.getEtAl();
                    }
                }
                if (ce.getStartDate() != null)
                {
                    if (ce.getStartDatePrecision() != null)
                    {
                        DateWrapper scrDateFormat = UIFieldFormatterMgr.getDateWrapper(ce.getStartDatePrecision());
                        collectionDate = scrDateFormat.format(ce.getStartDate().getTime());
                    }
                }
                if (ce.getStationFieldNumber() != null)
                {
                    collectorNumber = ce.getStationFieldNumber();
                }
            }

        } catch (Exception ex)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(CollectionObjectDataObjFmt.class, ex);
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

        return collectorName + " " + collectorNumber + (collectionDate != null ? ", " + collectionDate : "");
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
        return "CollectionObjectDetail";
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
        CollectionObjectDataObjFmt codof = (CollectionObjectDataObjFmt) super.clone();

        return codof;
    }
}