package edu.harvard.huh.specify.plugins;

import static edu.ku.brc.ui.UIRegistry.getResourceString;
import static edu.ku.brc.ui.UIRegistry.loadAndPushResourceBundle;
import static edu.ku.brc.ui.UIRegistry.popResourceBundle;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;

import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.UIPluginable;
import edu.ku.brc.af.ui.forms.validation.ValComboBox;
import edu.ku.brc.af.ui.forms.validation.ValTextField;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.AgentVariant;
import edu.ku.brc.specify.datamodel.GroupPerson;
import edu.ku.brc.specify.datamodel.PickListItem;
import edu.ku.brc.ui.GetSetValueIFace;

/**
 * For Bug ID: 223. GroupFullNameVariant is a button on the Agent form's
 * variant's add/edit form. It generates a string of a group's members' full
 * names in the order of the group members. It will only appear if the agent
 * type is "Group" and variant type is "Full Name."
 * 
 * @author lchan
 * 
 */
public class GroupFullNameVariant extends JButton implements UIPluginable,
        GetSetValueIFace {

    private static final long serialVersionUID = 1L;

    private String title = null;

    private Agent agent;

    private FormViewObj parent;

    public GroupFullNameVariant() {
        loadAndPushResourceBundle("specify_plugins");

        title = getResourceString("GroupFullNameVariantPlugin");
        this.setText(title);

        String tooltip = getResourceString("GroupFullNameVariantTooltip");
        this.setToolTipText(tooltip);

        popResourceBundle();

        this.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doButtonAction();
            }
        });

        this.setEnabled(false);
    }

    /**
     * Creates the string of variant names in the order of the members.
     */
    protected void doButtonAction() {
    	AgentVariant formAgentVariant = (AgentVariant) parent.getDataObj();
    	Agent agent = formAgentVariant.getAgent();
    	
        String groupFullName = "";

        // getGroups returns a set. Sort them to the order specified in "group"
        // first.
        ArrayList<GroupPerson> members = new ArrayList<GroupPerson>(
                agent.getGroups());
        Collections.sort(members);

        // This loop expects that all agents have full names!
        for (int i = 0; i < members.size(); i++) {
            GroupPerson member = members.get(i);
            for (AgentVariant agentVariant : member.getMember().getVariants()) {
                if (agentVariant.getVarType().equals(AgentVariant.FULLNAME)) {
                    String rearrangedName = agentVariant.getName();
                    groupFullName += rearrangedName;
                    if (i < members.size() - 2) {
                        groupFullName += ", ";
                    } else if (i < members.size() - 1) {
                        groupFullName += " & ";
                    }
                }
            }
        }

        // Sets the value of the var type name field of the UI
        Component[] dialogComponents = getParent().getComponents();
        for (Component component : dialogComponents) {
            if (component instanceof ValTextField) {
                ((ValTextField) component).setText(groupFullName);
            }
        }
    }

    /**
     * Bootstraps the action listener to act on var type combobox changes. The
     * handler is handleVarTypeChange(...). I'm doing this here The bootstrap
     * must occur here, otherwise the parent field will be null.
     */
    @Override
    public void setValue(Object value, String defaultValue) {
        // I check the two parents up because Specify calls this method twice.
        // The second ancestor is up until the second call.
        if (parent.getMVParent() != null
                && parent.getMVParent().getMultiViewParent() != null) {
            this.setEnabled(isAgentTypeGroup());
        }

        if (parent != null) {
            ValComboBox varType = parent.getCompById("2");
            varType.getComboBox().addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    handleVarTypeChange(e);
                }
            });
        }
    }

    @Override
    public Object getValue() {
        return agent;
    }

    /**
     * Returns true of the agent type is a group.
     * 
     * @return true of the agent type is a group
     */
    private boolean isAgentTypeGroup() {
        FormViewObj botanist = parent.getMVParent().getMultiViewParent()
                .getCurrentViewAsFormViewObj();
        ValComboBox agentType = (ValComboBox) botanist
                .getControlByName("agentType");
        return agentType.getValue().equals("Group");
    }

    /**
     * Handles the change event of the var type combobox of the variant view.
     * Will set this button visible if the var type is "Full Name" and the agent
     * type is "Group."
     * 
     * @param e
     *            passed from Java swing
     */
    private void handleVarTypeChange(ActionEvent e) {
        this.setEnabled(false);

        JComboBox varTypeCb = (JComboBox) e.getSource();
        PickListItem varType = (PickListItem) varTypeCb.getSelectedItem();
        if (varType != null && varType.getTitle().equals("Full Name")) {
            if (isAgentTypeGroup()) {
                this.setEnabled(true);
            }
        }
    }

    @Override
    public void initialize(Properties properties, boolean isViewMode) {
    }

    @Override
    public void setCellName(String cellName) {
    }

    @Override
    public JComponent getUIComponent() {
        return this;
    }

    @Override
    public void shutdown() {
        agent = null;
    }

    @Override
    public void setParent(FormViewObj parent) {
        this.parent = parent;
    }

    @Override
    public boolean isNotEmpty() {
        return false;
    }

    @Override
    public boolean canCarryForward() {
        return false;
    }

    @Override
    public String[] getCarryForwardFields() {
        return null;
    }

    @Override
    public String getTitle() {
        return title;
    }

}
