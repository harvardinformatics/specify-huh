package edu.harvard.huh.specify.plugins;

import static edu.ku.brc.ui.UIHelper.setControlSize;
import static edu.ku.brc.ui.UIRegistry.loadAndPushResourceBundle;
import static edu.ku.brc.ui.UIRegistry.popResourceBundle;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.apache.log4j.Logger;
import org.dom4j.Element;

import edu.ku.brc.af.ui.db.TextFieldWithQuery;
import edu.ku.brc.af.ui.db.ViewBasedDisplayIFace;
import edu.ku.brc.af.ui.forms.EditViewCompSwitcherPanel;
import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.MultiView;
import edu.ku.brc.af.ui.forms.UIPluginable;
import edu.ku.brc.af.ui.forms.validation.ValComboBox;
import edu.ku.brc.af.ui.forms.validation.ValComboBoxFromQuery;
import edu.ku.brc.af.ui.forms.validation.ValTextField;
import edu.ku.brc.helpers.HTTPGetter.ErrorCode;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.ui.GetSetValueIFace;
import edu.ku.brc.ui.IconManager;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.UIRegistry;

@SuppressWarnings("serial")
public class IndexFungorum extends JButton implements UIPluginable, GetSetValueIFace, IndexFungorumInfoGetterListener {

	private static final Logger log = Logger.getLogger(IndexFungorum.class);
	
	protected JTextField          textField;
	protected Taxon               taxon;
	protected JButton             infoBtn    = null;
	protected JProgressBar        progress   = null;

	protected ViewBasedDisplayIFace frame      = null;
	protected MultiView             multiView  = null;

	protected String title;
	
	protected FormViewObj formViewObj;
	protected ValTextField nameTextField;
	protected ValComboBox rankComboBox;
	protected ValComboBoxFromQuery parentComboBox;
	
	protected IndexFungorumInfoGetter getter;

	protected File cacheDir;
	
	// from TextFieldWithQuery
	private JPopupMenu popupMenu;
	private PopupMenuListener popupMenuListener;
	 
	private List<String> list = new ArrayList<String>();
	private List<Taxon> taxonList = new ArrayList<Taxon>();
	private Taxon selectedTaxon;
	private final boolean doAddAddItem = false; // in TextFieldWithQuery, this indicates that there is a "+" icon for adding a new record.
	
	private List<ListSelectionListener> listSelectionListeners = new ArrayList<ListSelectionListener>();
	
	/**
	 *
	 */
	public IndexFungorum()
	{
		loadAndPushResourceBundle("specify_plugins");
        
        this.title = UIRegistry.getResourceString("IndexFungorumSearchTitle");
        String tooltip = UIRegistry.getResourceString("IndexFungorumSearchTooltip");
        
        popResourceBundle();
        
        setIcon(IconManager.getIcon("IndexFungorum", IconManager.IconSize.NonStd));

        this.setToolTipText(tooltip);
        
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
                    {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run()
                            {
                                doButtonAction();
                            }
                        });
                    }
        });
        
        File path = UIRegistry.getAppDataSubDir("cache", true);
        if (path.exists())
        {
        	String dirPath = path.getAbsolutePath();
        	cacheDir = new File(dirPath);
        	
        } else
        {
            String msg = "unable to create directory [" + path.getAbsolutePath() + "]";
            log.warn(msg);
            throw new RuntimeException(msg);
        }
	}

	/**
	 * 
	 */
	protected void doButtonAction()
	{
		log.debug("doButtonAction");
		
		String searchText = nameTextField.getText();
		log.info("Searching Index Fungorum for '" + searchText + "'");
		
		list.clear();
		taxonList.clear();
		
		if (getter == null)
        {
            getter = new IndexFungorumInfoGetter(this, searchText, cacheDir);
        }
        getter.start();
	}


	@Override
	public void addChangeListener(ChangeListener listener)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener l)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean canCarryForward()
	{
		return false;
	}

	@Override
	public String[] getCarryForwardFields()
	{
		return null;
	}

	@Override
	public String getTitle()
	{
		return title;
	}

	@Override
	public JComponent getUIComponent()
	{
		return this;
	}

	@Override
	public void initialize(Properties properties, boolean isViewMode)
	{

	}

	@Override
	public boolean isNotEmpty()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setCellName(String cellName)
	{
		;
	}

	@Override
	public void setParent(FormViewObj formViewObj)
	{
		this.formViewObj = formViewObj;

		Component name = formViewObj.getControlById("name");
		if (name instanceof ValTextField) {
			nameTextField = (ValTextField) name;
		}
		
		Component rank = formViewObj.getControlById("definitionItem");
		if (rank instanceof ValComboBox) {
			rankComboBox = (ValComboBox) rank;
		}
		
		Component parent = formViewObj.getControlById("parent");
		if (parent instanceof ValComboBoxFromQuery) {
			parentComboBox = (ValComboBoxFromQuery) parent;
		}
		else if (parent instanceof EditViewCompSwitcherPanel) {
			//parentComboBox = (EditViewCompSwitcherPanel) parent;
		}
	}

	@Override
	public void shutdown()
	{
		formViewObj = null;
		taxon = null;
	}

	@Override
	public Object getValue()
	{
		return taxon;
	}

	@Override
	public void setValue(Object value, String defaultValue)
	{   
		boolean enable = false;
		if (value != null && value instanceof Taxon)
		{
			taxon = (Taxon)value;

		}

		if (taxon != null)
		{
			enable = true;
		}
		setEnabled(enable);
	}

	@Override
	public void infoArrived(IndexFungorumInfoGetter getter) {
		log.debug("infoArrived");

		Element root = getter.getDom();
		List<Taxon> results = parseIFResults(root);
		
		for (Taxon t : results) {
			String name = t.getName();
			
			log.debug(name);
			list.add(name);
			taxonList.add(t);
		}
		
		// from TextFieldWithQuery
		//protected void showPopup()
		//{
		String currentText = nameTextField.getText();
		log.debug("current text: '" + currentText + "'");
		
		//if (hasNewText || currentText.length() == 0)
		if (currentText != null && currentText.length() != 0)
		{
			ActionListener al = new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					itemSelected((JMenuItem)e.getSource());
				}
			};

			popupMenu = new JPopupMenu();
			if (popupMenuListener != null)
			{
				popupMenu.addPopupMenuListener(popupMenuListener);
			}

			log.debug("Created popup menu");
			
			popupMenu.addPopupMenuListener(new PopupMenuListener() {
				public void popupMenuCanceled(PopupMenuEvent e)
				{
					if (selectedTaxon == null)
					{
						setText(""); //$NON-NLS-1$
					}
				}

				public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
				{
					if (selectedTaxon == null)
					{
						setText(""); //$NON-NLS-1$
					}
					//textField.requestFocus();
				}

				public void popupMenuWillBecomeVisible(PopupMenuEvent e)
				{
					//
				}
			});

			if (doAddAddItem)
			{
				JMenuItem mi = new JMenuItem(UIRegistry.getResourceString("TFWQ_ADD_LABEL")); //$NON-NLS-1$
				setControlSize(mi);

				popupMenu.add(mi);
				mi.addActionListener(al); 
			}

			for (String str : list)
			{
				String label = str;
				/*if (uiFieldFormatter != null)
	                {
	                    label = uiFieldFormatter.formatToUI(label).toString();
	                }*/
				JMenuItem mi = new JMenuItem(label);
				setControlSize(mi);

				popupMenu.add(mi);
				mi.addActionListener(al);
			}
		}

		if (popupMenu != null && (list.size() > 0 || doAddAddItem))
		{
			log.debug("going to show popup?");
			
			UIHelper.addSpecialKeyListenerForPopup(popupMenu);

			final Point     location = nameTextField.getLocation();
			final Dimension size     = nameTextField.getSize();

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					popupMenu.setInvoker(nameTextField);
					popupMenu.show(nameTextField, location.x, location.y+size.height);
					popupMenu.requestFocus();
				}
			});
		}

	    //}
	}

	// from TextFieldWithQuery
	private void itemSelected(final JMenuItem mi)
	{
		//log.debug("setting hasNewText to true");

		String selectedStr = mi.getText();
		int inx = popupMenu.getComponentIndex(mi);
		if (inx > -1)
		{
			if (taxonList.size() > 0 && !doAddAddItem)
			{
				selectedTaxon =  taxonList.get(doAddAddItem ? inx-1 : inx);

				nameTextField.setText(selectedStr);
				this.formViewObj.setNewObject(selectedTaxon);
			}

			if (listSelectionListeners != null)
			{
				notifyListenersOfChange(mi);
			}
		}
	}

	// from TextFieldWithQuery
	private void notifyListenersOfChange(final Object source)
	{
		if (listSelectionListeners != null)
		{
			ListSelectionEvent lse = source == null ? null : new ListSelectionEvent(source, 0, 0, false);
			for (ListSelectionListener l : listSelectionListeners)
			{
				l.valueChanged(lse);
			}
		}
	}

	@Override
	public void infoGetWasInError(IndexFungorumInfoGetter getter) {
		
		ErrorCode e = getter.getStatus();
		log.error("infoGetWasInError:" + e.name());
	}
	
	protected List<Taxon> parseIFResults(Element root) {
		
		log.debug("parseIFResults");
		
		List<Taxon> results = new ArrayList<Taxon>();

		for (Object o : root.elements("IndexFungorum")) {
			Element result = (Element) o;
			
			String name = result.valueOf("NAME_x0020_OF_x0020_FUNGUS");
			String authors = result.valueOf("AUTHORS");
			String publishedListReference = result.valueOf("PUBLISHED_x0020_LIST_x0020_REFERENCE");
			String specificEpithet = result.valueOf("SPECIFIC_x0020_EPITHET");
			String infraSpecificRank = result.valueOf("INFRASPECIFIC_x0020_RANK");
			String volume = result.valueOf("VOLUME");
			String page = result.valueOf("PAGE");
			String yearOfPublication = result.valueOf("YEAR_x0020_OF_x0020_PUBLICATION");
			String recordNumber = result.valueOf("RECORD_x0020_NUMBER");
			String uuid = result.valueOf("UUID");
			
			boolean isInfraSpecific = infraSpecificRank != null && infraSpecificRank.trim().length() > 0;
			
			Taxon t = new Taxon();
			t.initialize();
			
			t.setName(name);
			t.setGuid(uuid);
			results.add(t);
		}
		return results;
	}
}
