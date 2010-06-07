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
package edu.harvard.huh.specify.datamodel.busrules;

import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.awt.Component;
import java.awt.Window;

import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.busrules.AgentBusRules;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.UIRegistry;

/**
 * This alters the UI depending on which type of agent is set.
 * 
 * @author rods
 *
 * @code_status Complete
 *
 * Created Date: Jan 24, 2007
 *
 */
public class HUHAgentBusRules extends AgentBusRules
{    
    // these are the cell@id values in the viewdefs used by AgentBusRules
    protected static final String AGENT_TYPE    = "0";
    protected static final String TITLE         = "1";
    protected static final String LAST_NAME     = "3";
    protected static final String MIDDLE_INIT   = "4";
    protected static final String FIRST_NAME    = "5";
    protected static final String EMAIL         = "7";
    protected static final String ABBREV        = "8";
    protected static final String ADDRESSES     = "9";
    protected static final String AGT_VARS      = "10";
    protected static final String GUID          = "11";
    protected static final String INITIALS      = "12";
    protected static final String JOB_TITLE     = "14";
    protected static final String REMARKS       = "16";
    protected static final String URL           = "17";
    protected static final String DATE_OF_BIRTH = "19";
    protected static final String DATE_OF_DEATH = "20";
    protected static final String GROUP_PERSONS = "31";

    // these were added by HUH
    protected static final String AGT_CITS      = "agentCitations";
    protected static final String AGT_GEOS      = "agentGeographies";
    protected static final String AGT_SPECS     = "agentSpecialties";
    protected static final String AGT_ATTCHMTS  = "agentAttachments";
    protected static final String DATES_TYPE    = "datesType";
    protected static final String INTERESTS     = "interests";
    protected static final String PARENT_ORG    = "organization";

    /**
     * Constructor.
     */
    public HUHAgentBusRules()
    {
        super();
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#processBusinessRules(java.lang.Object)
     */
    @Override
    public STATUS processBusinessRules(final Object dataObj)
    {
        reasonList.clear();
        STATUS status =  super.processBusinessRules(dataObj);

        return status;
    }
    
    /**
     * Process business rules specifically related to determinations.  Return OK status, or add error messages
     * to reasonList and return Error status.
     * @param dataObj
     * @return 
     */
    protected STATUS checkVariants(final Agent agent)
    {
        // check that at least one variant exists
        if (agent.getVariants().size() < 1)
        {
            reasonList.add(getResourceString("AgentBusRules.NO_VARIANTS"));

            return STATUS.Warning;
        }

        return STATUS.OK;
    } 

    /**
     * Clears the values and hides some UI depending on what type is selected
     * @param cbx the type cbx
     */
    @Override
    protected void fixUpTypeCBX(final JComboBox cbx)
    {
        if (formViewObj != null)
        {
            Agent agent = (Agent)formViewObj.getDataObj();
            if (agent != null)
            {
                final Component addrSubView = formViewObj.getCompById(ADDRESSES);
                
                byte agentType = (byte)cbx.getSelectedIndex();
                if (agentType != Agent.PERSON)
                {
                    agent.setMiddleInitial(null);
                    agent.setFirstName(null);
                    agent.setTitle(null);
                    
                } else
                {
                    if (addrSubView != null)
                    {
                        if (!addrSubView.isVisible())
                        {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run()
                                {
                                    Component topComp = UIHelper.getWindow(addrSubView);
                                    Component topMost = UIRegistry.getTopWindow();
                                    if (topComp != topMost && topComp != null)
                                    {
                                        ((Window)topComp).pack();
                                    }
                                }
                            });
                        }
                        addrSubView.setVisible(true);
                    }
                }
                agent.setAgentType(agentType);
                fixUpFormForAgentType(agent, true);
            }
        }
    }
    
    /**
     * Fix up labels in UI per the type of Agent
     * @param agent the current agent
     * @param doSetOtherValues indicates it should set values
     */
    @Override
    protected void fixUpFormForAgentType(final Agent   agent,
                                         final boolean doSetOtherValues)
    {
        boolean isPerson = agent.getAgentType() == null || agent.getAgentType() == Agent.PERSON;
        boolean isGroup = agent.getAgentType() != null && agent.getAgentType() == Agent.GROUP;
        boolean isOrganization = agent.getAgentType() != null && agent.getAgentType() == Agent.ORG;
        
        if (!isPerson)
        {
            agent.setFirstName(null);
            agent.setMiddleInitial(null);
        }
        enableFieldAndLabel(ABBREV, isOrganization, doSetOtherValues ? agent.getAbbreviation() : null);
        // address is handled in fixUpTypeCBX
        enableFieldAndLabel(FIRST_NAME, isPerson, doSetOtherValues ? agent.getFirstName() : null);
        setVisible(GROUP_PERSONS, isGroup);
        enableFieldAndLabel(MIDDLE_INIT, isPerson, doSetOtherValues ? agent.getMiddleInitial() : null);
        enableFieldAndLabel(INITIALS, isPerson, doSetOtherValues ? agent.getInitials() : null);
        enableFieldAndLabel(INTERESTS, isPerson, doSetOtherValues ? agent.getInterests() : null);
        enableFieldAndLabel(JOB_TITLE, isPerson, doSetOtherValues ? agent.getJobTitle() : null);
        enableFieldAndLabel(TITLE, isPerson, doSetOtherValues ? agent.getTitle() : null);
        
        
        // Last Name
        String lbl = UIRegistry.getResourceString(isPerson ? "AG_LASTNAME" : "AG_NAME");
        lastLabel.setText(lbl + ":");
        

    }
    
    protected void setVisible(String componentId, boolean visible)
    {
        Component field  = formViewObj.getCompById(componentId);
        if (field != null) field.setVisible(visible);
    }
}
