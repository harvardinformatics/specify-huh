package edu.harvard.huh.specify.plugins;

import static edu.ku.brc.ui.UIRegistry.getResourceString;
import static edu.ku.brc.ui.UIRegistry.loadAndPushResourceBundle;
import static edu.ku.brc.ui.UIRegistry.popResourceBundle;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;
import org.dom4j.Element;

import edu.ku.brc.af.ui.db.ViewBasedDisplayIFace;
import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.MultiView;
import edu.ku.brc.af.ui.forms.UIPluginable;
import edu.ku.brc.af.ui.forms.validation.ValComboBox;
import edu.ku.brc.af.ui.forms.validation.ValComboBoxFromQuery;
import edu.ku.brc.af.ui.forms.validation.ValTextField;
import edu.ku.brc.helpers.HTTPGetter.ErrorCode;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.extras.FishBaseInfoGetter;
import edu.ku.brc.ui.GetSetValueIFace;
import edu.ku.brc.ui.IconManager;
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
	protected ValTextField searchTextField;

	protected File cacheDir;
	
	/**
	 *
	 */
	public IndexFungorum()
	{
		loadAndPushResourceBundle("specify_plugins");
        
        title = UIRegistry.getResourceString("IndexFungorumSearchTitle");
        String tooltip = UIRegistry.getResourceString("IndexFungorumSearchTooltip");
        
        popResourceBundle();
        
        setIcon(IconManager.getIcon("IndexFungorum", IconManager.IconSize.NonStd));
        setText(title);
        this.setToolTipText(tooltip);
        
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0)
            {
                doButtonAction();
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
		
		String searchText = searchTextField.getText();
		log.info("Searching Index Fungorum for '" + searchText + "'");
		
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
		
		Component searchText = formViewObj.getControlById("searchText");
		if (searchText instanceof ValTextField) {
			searchTextField = (ValTextField) searchText;
		}
		
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
		}
	}

	@Override
	public void infoGetWasInError(IndexFungorumInfoGetter getter) {
		
		ErrorCode e = getter.getStatus();
		log.error("infoGetWasInError:" + e.name());
	}
	
	@SuppressWarnings("unchecked")
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
			
			Taxon t = new Taxon();
			t.initialize();
			
			t.setName(name);
			results.add(t);
		}
		return results;
	}
}
