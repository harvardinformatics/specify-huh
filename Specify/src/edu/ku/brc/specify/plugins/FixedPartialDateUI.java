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
package edu.ku.brc.specify.plugins;

import static edu.ku.brc.ui.UIHelper.createComboBox;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.ku.brc.af.ui.forms.ViewFactory;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatter;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterMgr;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace.PartialDateEnum;
import edu.ku.brc.af.ui.forms.validation.UIValidatable;
import edu.ku.brc.af.ui.forms.validation.ValFormattedTextField;
import edu.ku.brc.af.ui.forms.validation.ValFormattedTextFieldSingle;
import edu.ku.brc.ui.DocumentAdaptor;
import edu.ku.brc.ui.GetSetValueIFace;
import edu.ku.brc.ui.UIRegistry;

public class FixedPartialDateUI extends PartialDateUI
{
    private static final Logger log  = Logger.getLogger(PartialDateUI.class);
    
    public FixedPartialDateUI()
    {
        super();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.UIPluginable#initialize(java.util.Properties, boolean)
     */
    @Override
    public void initialize(final Properties properties, final boolean isViewMode)
    {
        this.isDisplayOnly = isViewMode;
        
        dateFieldName = properties.getProperty("df");
        dateTypeName  = properties.getProperty("tp");

        String partialDateName = properties.getProperty("pd");
        
        if (PartialDateEnum.Full.name().equals(partialDateName)) dateType = PartialDateEnum.Full;
        else if (PartialDateEnum.Month.name().equals(partialDateName)) dateType = PartialDateEnum.Month;
        else if (PartialDateEnum.Year.name().equals(partialDateName)) dateType = PartialDateEnum.Year;
        else dateType = PartialDateEnum.Year;

        createUI();
    }
    
    /**
     * Creates the UI.
     */
    protected void createUI()
    {
        DocumentAdaptor docListener = null;
        if (!isDisplayOnly)
        {
            docListener = new DocumentAdaptor()
            {
                @Override
                protected void changed(DocumentEvent e)
                {
                    super.changed(e);
                    
                    if (!ignoreDocChanges)
                    {
                        isChanged     = true;
                        isDateChanged = true;
                        if (changeListener != null)
                        {
                            changeListener.stateChanged(new ChangeEvent(FixedPartialDateUI. this));
                        }
                    }
                }
            };
        }
        
        List<UIFieldFormatterIFace> partialDateList = UIFieldFormatterMgr.getInstance().getDateFormatterList(true);
        for (UIFieldFormatterIFace uiff : partialDateList)
        {
            if (uiff.getName().equals("PartialDateMonth"))
            {
                ValFormattedTextField tf = new ValFormattedTextField(uiff, isDisplayOnly, !isDisplayOnly);
                tf.setRequired(isRequired);
                if (docListener != null) tf.addDocumentListener(docListener);
                uivs[1]       = tf;
                textFields[1] = tf.getTextField();
                if (isDisplayOnly)
                {
                    ViewFactory.changeTextFieldUIForDisplay(textFields[1], false);
                }
                
            } else if (uiff.getName().equals("PartialDateYear"))
            {
                ValFormattedTextField tf = new ValFormattedTextField(uiff, isDisplayOnly, !isDisplayOnly);
                tf.setRequired(isRequired);
                if (docListener != null) tf.addDocumentListener(docListener);
                uivs[2]       = tf;
                textFields[2] = tf.getTextField();
                if (isDisplayOnly)
                {
                    ViewFactory.changeTextFieldUIForDisplay(textFields[2], false);
                }
            }
        }
        
        List<UIFieldFormatterIFace> dateList = UIFieldFormatterMgr.getInstance().getDateFormatterList(false);
        for (UIFieldFormatterIFace uiff : dateList)
        {
            if (uiff.getName().equals("Date"))
            {
                ValFormattedTextFieldSingle tf = new ValFormattedTextFieldSingle(uiff, isDisplayOnly, false);
                tf.setRequired(isRequired);
                if (docListener != null) tf.addDocumentListener(docListener);
                uivs[0]       = tf;
                textFields[0] = tf;
                if (isDisplayOnly)
                {
                    ViewFactory.changeTextFieldUIForDisplay(textFields[0], false);
                }

            }
        }
        
        cardPanel = new JPanel(cardLayout);
        
        String[] formatKeys = {"PARTIAL_DATE_FULL", "PARTIAL_DATE_MONTH", "PARTIAL_DATE_YEAR"};
        String[] labels     = new String[formatKeys.length];
        for (int i=0;i<formatKeys.length;i++)
        {
            labels[i]  = UIRegistry.getResourceString(formatKeys[i]);
            cardPanel.add(labels[i], (JComponent)uivs[i]);
        }
        
        formatSelector = createComboBox(labels);
        formatSelector.setSelectedIndex(0);
        
        JComponent typDisplayComp = null;
        if (!isDisplayOnly)
        {
            comboBoxAL = new ActionListener() {
                public void actionPerformed(ActionEvent ae)
                {
                    JComboBox cbx       = (JComboBox)ae.getSource();
                    Object    dataValue = ((GetSetValueIFace)currentUIV).getValue();//isDateChanged ? ((GetSetValueIFace)currentUIV).getValue() : Calendar.getInstance();
                    currentUIV = uivs[cbx.getSelectedIndex()];
                    
                    ignoreDocChanges = true;
                    ((GetSetValueIFace)currentUIV).setValue(dataValue, null);
                    ignoreDocChanges = false;
                    
                    cardLayout.show(cardPanel, formatSelector.getSelectedItem().toString());
                    
                    isChanged = true;
                    if (changeListener != null)
                    {
                        changeListener.stateChanged(new ChangeEvent(FixedPartialDateUI. this));
                    }
                }
            };
            typDisplayComp = formatSelector;
            formatSelector.addActionListener(comboBoxAL);
        }
        
        PanelBuilder    builder = new PanelBuilder(new FormLayout("f:p:g", "p"), this);
        CellConstraints cc      = new CellConstraints();
        builder.add(cardPanel,      cc.xy(1,1));
        
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.UIPluginable#getCarryForwardFields()
     */
    @Override
    public String[] getCarryForwardFields()
    {
        return new String[] { dateFieldName };
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.GetSetValueIFace#getValue()
     */
    @Override
    public Object getValue()
    {
        Object fieldVal = null;
        if (currentUIV != null)
        {
            fieldVal = ((GetSetValueIFace)currentUIV).getValue();
        }
        if (!isDisplayOnly &&
            dataObj != null && 
            StringUtils.isNotEmpty(dateFieldName) &&
            //StringUtils.isNotEmpty(dateTypeName) && 
            isChanged)
        {
            verifyGetterSetters();
            
            setter.setFieldValue(dataObj, dateFieldName, fieldVal != null && StringUtils.isNotEmpty(fieldVal.toString()) ? fieldVal : null);
            //setter.setFieldValue(dataObj, dateTypeName, formatSelector.getSelectedIndex()+1); // Need to add one because the first value is None
        }
        return dataObj;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.GetSetValueIFace#setValue(java.lang.Object, java.lang.String)
     */
    @Override
    public void setValue(final Object value, final String defaultValue)
    {
        dataObj = value;
        
        if (dataObj != null)
        {
            verifyGetterSetters();

        } else
        {
            for (UIValidatable uiv : uivs)
            {
                ((GetSetValueIFace)uiv).setValue(null, "");
            }
            currentUIV = uivs[0];
            return;
        }
        
        Calendar date = null;

        Object dateObj = getter.getFieldValue(value, dateFieldName);
        if (dateObj == null && StringUtils.isNotEmpty(defaultValue) && defaultValue.equals("today"))
        {
            date = Calendar.getInstance();
        }
        
        if (dateObj instanceof Calendar)
        {
            date = (Calendar)dateObj;
        }
        
        int inx = dateType.ordinal();
        
        if (inx > 0)
        {
            inx--; // need to subtract one because the first item is "None"
        } else
        {
            log.error(dateTypeName+" was zero and shouldn't have been!");
        }
        
        currentUIV = uivs[inx];
        if (currentUIV != null)
        {
            ignoreDocChanges = true;
            ((GetSetValueIFace)currentUIV).setValue(date, "");
            ignoreDocChanges = false;
        }
        
        //dateType = UIFieldFormatter.PartialDateEnum.values()[inx+1];
        formatSelector.removeActionListener(comboBoxAL);
        formatSelector.setSelectedIndex(inx);
        formatSelector.addActionListener(comboBoxAL);
        cardLayout.show(cardPanel, formatSelector.getModel().getElementAt(inx).toString());
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.validation.UIValidatable#reset()
     */
    @Override
    public void reset()
    {
        for (UIValidatable uiv : uivs)
        {
            uiv.reset();
        }
        isChanged     = false;
        isDateChanged = false;
        dataObj       = null;
        
        for (JTextField tf : textFields)
        {
            if (tf != null)
            {
                tf.setText("");
            }
        }
        
    }    
}
