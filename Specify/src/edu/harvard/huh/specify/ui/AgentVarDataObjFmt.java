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
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.Author;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Collector;
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
public class AgentVarDataObjFmt implements DataObjDataFieldFormatIFace, Cloneable
{   
    // Needed for the Custom Editor
    protected ChangeListener changeListener = null;
    protected JTextField     textField      = null;
    
    String name;
    int varType;
    
    String emptyName   = getResourceString("AgentVarDataObjFmt.NoVariant");

    /**
     * Constructor.
     */
    public AgentVarDataObjFmt()
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
        
        if (!(dataValue instanceof Agent || dataValue instanceof Author || dataValue instanceof Collector))
        {
            throw new RuntimeException("The data value set into AgentVarDataObjFmt is not an Author, Collector, or Agent ["+dataValue.getClass().getSimpleName()+"]");
        }

        String restricted = FormHelper.checkForRestrictedValue(Agent.getClassTableId());
        if (restricted != null)
        {
            return restricted;
        }
        
        Agent agent = null;
        String agentName = null;
        
        DataProviderSessionIFace session = null;
        String errMsg = null;

        try
        {
            session = DataProviderFactory.getInstance().createSession();

            Object obj = session.merge(dataValue);

            if (obj instanceof Author) agent = ((Author) obj).getAgent();
            else if (obj instanceof Collector) agent = ((Collector) obj).getAgent();
            else agent = (Agent) obj;
            
            agentName = agent.getVariantName(varType);
            
        } catch (Exception ex)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(AgentVarDataObjFmt.class, ex);
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

        return agentName != null ? agentName : emptyName;
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
        return name;
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
        this.name = name;
        this.varType = Integer.parseInt(properties.getProperty("varType"));
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.formatters.DataObjDataFieldFormatIFace#toXML(java.lang.StringBuilder)
     */
    @Override
    public void toXML(StringBuilder sb)
    {
        sb.append("          <external");
        xmlAttr(sb, "class", getClass().getName());
        sb.append(">\n");
        sb.append("            <param");
        xmlAttr(sb, "name", "varType");
        sb.append(">");
        sb.append(varType);
        sb.append("</param>\n");
        sb.append("          </external>\n");
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone() throws CloneNotSupportedException
    {
        AgentVarDataObjFmt codof = (AgentVarDataObjFmt) super.clone();

        return codof;
    }
}
