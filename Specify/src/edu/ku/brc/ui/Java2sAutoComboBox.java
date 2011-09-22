
/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * -Redistribution of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MIDROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or intended
 * for use in the design, construction, operation or maintenance of any
 * nuclear facility.
 */
package edu.ku.brc.ui;

import java.awt.event.ItemEvent;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxEditor;

/**
 * Taken from a un Microsystems, Inc example and then modified.
 * 
 * @author rods
 *
 * @code_status Alpha
 *
 * Created Date: Apr 6, 2011
 *
 */
public class Java2sAutoComboBox extends JComboBox
{

    private AutoTextFieldEditor autoTextFieldEditor;
    private boolean             isFired;

    /**
     * @param list
     */
    public Java2sAutoComboBox(java.util.List<?> list)
    {
        isFired             = false;
        autoTextFieldEditor = new AutoTextFieldEditor(list);
        setEditable(true);
        
        setModel(new DefaultComboBoxModel(list.toArray())
        {
            protected void fireContentsChanged(Object obj, int i, int j)
            {
                if (!isFired)
                {
                    super.fireContentsChanged(obj, i, j);
                }
            }
        });
        setEditor(autoTextFieldEditor);
    }

    /* (non-Javadoc)
     * @see javax.swing.JComboBox#getSelectedItem()
     */
    @Override
    public Object getSelectedItem()
    {
        Object obj = super.getSelectedItem();
        /*if (obj instanceof TitleValueIFace)
        {
            TitleValueIFace t = (TitleValueIFace)obj;
            System.out.println("["+t.getTitle()+"]["+t.getValue()+"]");
            return ((TitleValueIFace)obj).getValue();
        }*/
        return obj;
    }


    /**
     * @param obj
     */
    void setSelectedValue(final Object obj)
    {
        if (isFired)
        {
            return;
        } 
        
        if (obj == null)
        {
            setSelectedIndex(-1);
            return;
        }
        
        isFired = true;
        TitleValueIFace tviObj = obj instanceof TitleValueIFace ? (TitleValueIFace)obj : null;
        
        for (int i=0;i<getModel().getSize();i++)
        {
            Object modelObj = getModel().getElementAt(i);
            if (modelObj instanceof TitleValueIFace)
            {
                TitleValueIFace tvi = (TitleValueIFace)modelObj;
                if (tviObj != null)
                {
                    if (tviObj.getValue().equals(tvi.getValue()))
                    {
                        setSelectedIndex(i);
                        break;
                    }
                } else if (obj.equals(tvi.getValue()))
                {
                    setSelectedIndex(i);
                    break;
                }
            } else if (obj == modelObj || modelObj.equals(obj))
            {
                setSelectedIndex(i);
                break;
            }
        }
        
        fireItemStateChanged(new ItemEvent(this, 701, selectedItemReminder, 1));
        isFired = false;
    }

    /* (non-Javadoc)
     * @see javax.swing.JComboBox#fireActionEvent()
     */
    @Override
    protected void fireActionEvent()
    {
        if (!isFired)
        {
            super.fireActionEvent();
        }
    }

    /**
     * @return the autoTextFieldEditor
     */
    public AutoTextFieldEditor getAutoTextFieldEditor()
    {
        return autoTextFieldEditor;
    }


    /**
     * @return
     */
    public boolean isCaseSensitive()
    {
        return autoTextFieldEditor.getAutoTextFieldEditor().isCaseSensitive();
    }

    /**
     * @param flag
     */
    public void setCaseSensitive(boolean flag)
    {
        autoTextFieldEditor.getAutoTextFieldEditor().setCaseSensitive(flag);
    }

    /**
     * @return
     */
    public boolean isStrict()
    {
        return autoTextFieldEditor.getAutoTextFieldEditor().isStrict();
    }

    /**
     * @param flag
     */
    public void setStrict(boolean flag)
    {
        autoTextFieldEditor.getAutoTextFieldEditor().setStrict(flag);
    }

    /**
     * @return
     */
    public java.util.List<?> getDataList()
    {
        return autoTextFieldEditor.getAutoTextFieldEditor().getDataList();
    }

    /**
     * @param list
     */
    public void setDataList(java.util.List<?> list)
    {
        Object edtItem = editor.getItem(); // get the current item in the edit field
        
        autoTextFieldEditor.getAutoTextFieldEditor().setDataList(list);
        setModel(new DefaultComboBoxModel(list.toArray()));
        
        // Now try to re-select the previously selected itme
        // and do this by value
        if (edtItem != null)
        {
            int index = 0;
            for (Object obj : list.toArray())
            {
                if (obj.toString().equals(edtItem.toString()))
                {
                    final int cbxIndex = index;
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Java2sAutoComboBox.this.setSelectedIndex(cbxIndex);
                        }
                    });
                }
                index++;
            }
        }
    }

    //------------------------------------------------------------------------------
    public class AutoTextFieldEditor extends BasicComboBoxEditor
    {

        public Java2sAutoTextField getAutoTextFieldEditor()
        {
            return (Java2sAutoTextField) editor;
        }

        public AutoTextFieldEditor(java.util.List<?> list)
        {
            editor = new Java2sAutoTextField(list, Java2sAutoComboBox.this);
        }
    }


}
