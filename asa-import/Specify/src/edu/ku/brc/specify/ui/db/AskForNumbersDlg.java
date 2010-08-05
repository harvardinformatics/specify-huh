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
package edu.ku.brc.specify.ui.db;

import static edu.ku.brc.ui.UIRegistry.getLocalizedMessage;
import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DocumentFilter;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.db.DBFieldInfo;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.core.expresssearch.QueryAdjusterForDomain;
import edu.ku.brc.af.ui.forms.FormDataObjIFace;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.dbsupport.RecordSetIFace;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.RecordSet;
import edu.ku.brc.specify.datamodel.SpecifyUser;
import edu.ku.brc.ui.CustomDialog;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author rod
 * @author maureen
 * 
 * @code_status Alpha
 *
 * Apr 10, 2008
 * Jul 13, 2010
 *
 */
public class AskForNumbersDlg extends CustomDialog
{
    private static final Logger log = Logger.getLogger(AskForNumbersDlg.class);
    
    protected Vector<Integer> numbersList = new Vector<Integer>(); // FragmentIDs
    protected String          labelKey;
    protected JTextArea       textArea;
    protected JTextArea       status;
    protected JScrollPane     statusSP;
    protected StringBuilder   errorList = new StringBuilder();
    
    private char listSep  = ',';           // dialog for catalog numbers uses this char to separate entries
    private int  entryLen = 9;             // catalog numbers have this length
    private String entryRegex = "^\\d+$";  // catalog numbers are numeric
    /**
     * @param dialog
     * @param title
     * @param isModal
     * @param whichBtns
     * @param contentPanel
     * @throws HeadlessException
     */
    public AskForNumbersDlg(final String  titleKey,
                            final String  labelKey
    ) throws HeadlessException
    {
        super((Frame)UIRegistry.getTopWindow(), getResourceString(titleKey), true, OKCANCELHELP, null);
        this.labelKey  = labelKey;
        
        this.helpContext = "AskForCatNumbers";
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.CustomDialog#createUI()
     */
    @Override
    public void createUI()
    {
        super.createUI();
        
        PanelBuilder    pb = new PanelBuilder(new FormLayout("p,2px,f:p:g", "p,4px,f:p:g"));
        CellConstraints cc = new CellConstraints();
        
        textArea = UIHelper.createTextArea(5, 30);
        status   = UIHelper.createTextArea(3, 30);
        status.setEditable(false);
        
        // add a listener that appends "," to the text area whenever 9 digits have been entered
        // 
        textArea.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void changedUpdate(DocumentEvent e)
            {
                // TODO Auto-generated method stub
            }

            @Override
            public void insertUpdate(DocumentEvent e)
            {
                final int length = textArea.getText().length();
                if (length >= entryLen)
                {
                    Runnable r = new Runnable()
                    {
                        public void run()
                        {
                            String text = textArea.getText();
                            if (text.substring(length - entryLen).matches(entryRegex))
                            {
                                textArea.append(String.valueOf(listSep));
                                textArea.setCaretPosition(length+1);
                            }
                        }
                    };
                    new Thread(r).start();
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                // TODO Auto-generated method stub
                
            }
            
        });
        
        
        
        pb.add(UIHelper.createI18NLabel(labelKey, SwingConstants.RIGHT), cc.xy(1,1));
        pb.add(UIHelper.createScrollPane(textArea), cc.xy(3,1));
        
        statusSP = new JScrollPane(status, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pb.add(statusSP, cc.xy(3,3));
        statusSP.setVisible(false);
        
        contentPanel = pb.getPanel();
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        pack();
    }
    
    /**
     * @return
     */
    protected boolean processNumbers()
    {
        status.setText("");
        numbersList.clear();
        errorList.setLength(0);
        
        DBTableInfo           ti        = DBTableIdMgr.getInstance().getByClassName(Fragment.class.getName());
        DBFieldInfo           fi        = ti.getFieldByName("identifier");
        UIFieldFormatterIFace formatter = fi.getFormatter();
        
        boolean isOK = true;
        
        String catNumbersStr = textArea.getText().trim();
        if (StringUtils.isNotEmpty(catNumbersStr))
        {
            DataProviderSessionIFace session = null;
            try
            {
                session = DataProviderFactory.getInstance().createSession();
                
                for (String catNumStr : StringUtils.split(catNumbersStr, listSep))
                {
                    String catNum      = catNumStr.trim();
                    String catNumForDB = catNum;
                    if (formatter != null)
                    {
                        try
                        {
                            catNumForDB = (String)formatter.formatFromUI(catNum);
                            
                        } catch (java.lang.NumberFormatException ex)
                        {
                            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(AskForNumbersDlg.class, ex);
                            errorList.append(getLocalizedMessage("AFN_NUMFMT_ERROR", catNum));
                            errorList.append("\n");
                            isOK = false;
                        }
                    }
                    
                    if (StringUtils.isNotEmpty(catNumForDB))
                    {

                        String hql = QueryAdjusterForDomain.getInstance().adjustSQL( // TODO unhardcode
                                "SELECT f.id FROM Preparation pp join pp.fragments f WHERE (pp.identifier = '"+catNumForDB+"' or f.identifier='"+catNumForDB+"') AND f.collectionMemberId = COLLID");
                        //log.debug(sql);
                        Integer fragmentId = (Integer)session.getData(hql);
                        
                        if (fragmentId != null)
                        {

                            // check that this item is not already out on loan
                            hql = QueryAdjusterForDomain.getInstance().adjustSQL(
                                    "SELECT f.id FROM Fragment f JOIN f.preparation.loanPreparations lp " +
                                    "WHERE lp.isResolved = false and " +
                                    "(f.identifier = '"+catNumForDB+"' OR f.preparation.identifier='"+catNumForDB+"') AND " +
                                    "f.collectionMemberId = COLMEMID"
                                    );
                            
                            Integer onLoanId = (Integer)session.getData(hql);
                            
                            if (onLoanId == null)
                            {
                                numbersList.add(fragmentId);
                            }
                            else
                            {
                                errorList.append(getLocalizedMessage("AFN_ONLOAN_ERROR", catNum));
                                errorList.append("\n");
                                isOK = false;
                            }
                        }
                        else
                        {
                            errorList.append(getLocalizedMessage("AFN_NOTFND_ERROR", fi.getTitle(), catNum));
                            errorList.append("\n");
                            isOK = false;
                        }
                        
                        if (!isOK)
                        {
                            statusSP.setVisible(true);
                            pack();
                        }
                    }
                }
                
            } catch (Exception ex)
            {
                edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(AskForNumbersDlg.class, ex);
                log.error(ex);
                ex.printStackTrace();
                
            } finally
            {
                if (session != null)
                {
                    session.close();
                }
            }
        }
        return isOK;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.CustomDialog#okButtonPressed()
     */
    @Override
    protected void okButtonPressed()
    {
        if (processNumbers())
        {
            super.okButtonPressed();
        } else
        {
            status.setText(errorList.toString());
        }
    }

    /**
     * @return the numbersList a valid list of numbers.
     */
    public Vector<Integer> getNumbersList()
    {
        return numbersList;
    }
    
    /**
     * @return
     */
    public RecordSetIFace getRecordSet()
    {
        if (numbersList.size() > 0)
        {
            RecordSet rs = new RecordSet();
            rs.initialize();
            rs.setSpecifyUser(AppContextMgr.getInstance().getClassObject(SpecifyUser.class));
            rs.setDbTableId(DBTableIdMgr.getInstance().getByClassName(Fragment.class.getName()).getTableId());  // TODO unhardcode
            for (Integer id : numbersList)
            {
                rs.addItem(id);
            }
            return rs;
        }
        return null;
    }
}
