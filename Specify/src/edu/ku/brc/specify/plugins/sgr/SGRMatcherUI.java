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
/**
 * 
 */
package edu.ku.brc.specify.plugins.sgr;

import java.awt.Frame;
import java.awt.HeadlessException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.NavBoxButton;
import edu.ku.brc.af.core.NavBoxItemIFace;
import edu.ku.brc.sgr.SGRMatcher;
import edu.ku.brc.sgr.datamodel.DataModel;
import edu.ku.brc.sgr.datamodel.MatchConfiguration;
import edu.ku.brc.specify.datamodel.Institution;
import edu.ku.brc.ui.CustomDialog;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author ben
 *
 * @code_status Alpha
 *
 * Created Date: May 26, 2011
 *
 */
public class SGRMatcherUI extends CustomDialog
{
    public static enum WeightChoice
    {
        Ignore,
        Low,
        Normal,
        High,
    }
    
    public static final String [] availableFields = 
        {"collectors", "collector_number", "location", 
        "date_collected", "date_split", "scientific_name"};   
    
    private final SGRMatcherUIPanel                  uiPanel;
    private final Function<MatchConfiguration, Void> finished;
    private NavBoxItemIFace                          nbi             = null;
    
    public static SGRMatcherUI dialogForNewConfig(Frame parent, 
                                                  Function<MatchConfiguration, Void> onFinished)
    {
        UIRegistry.loadAndPushResourceBundle("specify_plugins");
        String title = UIRegistry.getResourceString("SGR_CREATE_MATCHER");
        UIRegistry.popResourceBundle();
        
        SGRMatcherUI dialog = new SGRMatcherUI(parent, title, new SGRMatcherUIPanel(null), onFinished);
        return dialog;
    }
    
    public static SGRMatcherUI dialogForEditing(Frame parent, NavBoxItemIFace nbi)
    {
        UIRegistry.loadAndPushResourceBundle("specify_plugins");
        String title = UIRegistry.getResourceString("SGR_EDIT_MATCHER");
        UIRegistry.popResourceBundle();

        SGRMatcherUI dialog = new SGRMatcherUI(parent, title, new SGRMatcherUIPanel(nbi), null);
        dialog.nbi = nbi;
        return dialog;
    }
    
    private SGRMatcherUI(Frame frame, String title, SGRMatcherUIPanel uiPanel,
                         Function<MatchConfiguration, Void> finished) 
        throws HeadlessException
    {
        super(frame, title, true, CustomDialog.OKCANCELHELP, uiPanel); 
        
        this.finished = finished;
        this.uiPanel = uiPanel;
        pack();
    }
    
    @Override
    protected void okButtonPressed() 
    {
        if (nbi == null)
        {
            SGRMatcher.Factory mf = SGRMatcher.getFactory();
            
            String name = uiPanel.name.getText();
            
            mf.serverUrl = uiPanel.serverUrl.getText();
            mf.nRows = ((Number) uiPanel.nRows.getValue()).intValue();
            mf.boostInterestingTerms = uiPanel.boost.isSelected();
            
            List<String> fields = new LinkedList<String>();
            List<String> boosts = new LinkedList<String>();
            for (String field : availableFields)
            {
                switch ((WeightChoice)uiPanel.similarityFields.get(field).getSelectedItem())
                {
                    case High:
                        boosts.add(field + "^5.0");
                        fields.add(field);
                        break;
                    case Low:
                        boosts.add(field + "^0.2");
                        fields.add(field);
                        break;
                    case Normal:
                        fields.add(field);
                        break;
                }
            }
            mf.similarityFields = StringUtils.join(fields.iterator(), ',');
            mf.queryFields = StringUtils.join(boosts.iterator(), ' ');
            mf.filterQuery = uiPanel.filterQuery.getText();
            
            finished.apply(
                    DataModel.persistMatchConfiguration(name, mf)
            );
        } else {
            MatchConfiguration matchConfig = (MatchConfiguration)nbi.getData();
            String name = uiPanel.name.getText();
            matchConfig.updateProperties(name, "");
            ((NavBoxButton) nbi).setLabelText(name);
            ((NavBoxButton) nbi).getParent().repaint();
        }
        dispose();
    }
    
    private static class SGRMatcherUIPanel extends JPanel
    {
        JTextField             name             = new JTextField("New matcher");

        JTextField             serverUrl        = new JTextField(
                                                        "http://dhwd99p1.nhm.ku.edu:8983/solr");

        JFormattedTextField    nRows            = new JFormattedTextField(10);

        JCheckBox              boost            = new JCheckBox("Boost Interesting Terms", true);

        Map<String, JComboBox> similarityFields = new HashMap<String, JComboBox>(
                                                        availableFields.length);

        JTextField             filterQuery      = new JTextField();

        JTextArea              remarks          = new JTextArea(10, 20);
        
        public SGRMatcherUIPanel(NavBoxItemIFace nbi)
        {
            super();
            
            for (String field : availableFields)
            {
                JComboBox comboBox = new JComboBox(WeightChoice.values());
                comboBox.setSelectedItem(WeightChoice.Normal);
                similarityFields.put(field, comboBox);
            }
            
            if (nbi != null)
            {
                MatchConfiguration matchConfig = (MatchConfiguration)nbi.getData();
                name.setText(matchConfig.name());
                
                serverUrl.setText(matchConfig.serverUrl());
                serverUrl.setEditable(false);
                
                nRows.setText("" + matchConfig.nRows());
                
                boost.setSelected(matchConfig.boostInterestingTerms());
                boost.setEnabled(false);
                
                Set<String> selectedFields = 
                    parseSimilarityFields(matchConfig.similarityFields());
                
                Map<String, Float> boosts = parseBoosts(matchConfig.queryFields());
                
                for (String field : availableFields)
                {
                    if (!selectedFields.contains(field))
                    {
                        similarityFields.get(field).setSelectedItem(WeightChoice.Ignore);
                    }
                    else if (boosts.containsKey(field))
                    {
                        if (boosts.get(field) > 1.0f)
                            similarityFields.get(field).setSelectedItem(WeightChoice.High);
                        else if (boosts.get(field) < 1.0f)
                            similarityFields.get(field).setSelectedItem(WeightChoice.Low);
                        else
                            similarityFields.get(field).setSelectedItem(WeightChoice.Normal);
                    }
                    else
                        similarityFields.get(field).setSelectedItem(WeightChoice.Normal);
                }
                
                for (JComboBox cb : similarityFields.values())
                {
                    cb.setEnabled(false);
                }
                
                filterQuery.setText(matchConfig.filterQuery());
                filterQuery.setEditable(false);
                
                remarks.setText(matchConfig.remarks());
            }
            else
            {
                filterQuery.setText("-institution_code:\"" +
                        AppContextMgr.getInstance().getClassObject(Institution.class).getCode()
                        + "\"");
            }
            
            int rows = availableFields.length + 6;
            StringBuilder colSpec = new StringBuilder();
            for (int i = 0; i < rows; i++) colSpec.append("p, 2dlu,");
            colSpec.append("p");
            
            FormLayout layout = 
                new FormLayout("right:max(50dlu;p), 4dlu, 150dlu:grow", colSpec.toString());
            
            PanelBuilder builder = new PanelBuilder(layout, this);
            builder.setDefaultDialogBorder();
            CellConstraints cc = new CellConstraints();
            
            int y = 1;
            builder.addLabel("Name",        cc.xy(1, y));
            builder.add(name,               cc.xy(3, y));
            y += 2;
            
            builder.addLabel("Server URL",  cc.xy(1, y));
            builder.add(serverUrl,          cc.xy(3, y));
            y += 2;
            
            builder.addSeparator("Filters", cc.xyw(1, y, 3));
            y += 2;
            
            builder.addLabel("Filter",      cc.xy(1, y));
            builder.add(filterQuery,        cc.xy(3, y));
            y += 2;
            
            builder.addSeparator("Similarity", cc.xyw(1, y, 3));
            y += 2;
            
            SGRColumnOrdering columnOrdering = SGRColumnOrdering.getInstance();
            
            for (String field : availableFields)
            {
                String label = columnOrdering.getHeadingFor(field);
                label = label == null ? WordUtils.capitalize(field) : label;
                
                builder.addLabel(label,                     cc.xy(1, y));
                builder.add(similarityFields.get(field),    cc.xy(3, y));
                y += 2;
            }
            
//            builder.addLabel("Number of Results", cc.xy(1, y));
//            builder.add(nRows,                    cc.xy(3, y));
//            y += 2;
            
//            builder.add(boost,              cc.xy(3, y));
//            y += 2;
            
//            builder.addLabel("Remarks",     cc.xy(1, y));
            
            builder.addSeparator("Remarks", cc.xyw(1, y, 3));
            y += 2;
            
            builder.add(remarks,            cc.xyw(1, y, 3));
            y += 2;
        }
        
        private Set<String> parseSimilarityFields(String fields)
        {
            ImmutableSet.Builder<String> b = ImmutableSet.builder();
            
            for (String field : StringUtils.split(fields, ','))
                   b.add(field.trim());
            
            return b.build();
        }
        
        private Map<String, Float> parseBoosts(String queryFields)
        {
            ImmutableMap.Builder<String, Float> b = ImmutableMap.builder();
            
            for (String field : availableFields)
            {
                Pattern p = Pattern.compile(field + "\\^" + "([-+]?[0-9]*\\.?[0-9]+)");
                Matcher m = p.matcher(queryFields);
                if (m.find())
                {
                    b.put(field, Float.valueOf(m.group(1)));
                }
            }
            return b.build();
        }
    }

}
