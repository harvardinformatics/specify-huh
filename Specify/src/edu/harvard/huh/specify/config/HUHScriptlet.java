package edu.harvard.huh.specify.config;

import edu.ku.brc.specify.config.Scriptlet;

public class HUHScriptlet extends Scriptlet {

    /**
     * Builds the shipped to agent's name string.
     * @param firstName
     * @param lastName
     * @param middleInitial
     */
	@Override
    public String buildNameString(String firstName, String lastName, String middleInitial)
    {
        String name = firstName == null ? "" : firstName;
        if (middleInitial != null)
        {
            name += " " + middleInitial;
        }
        
        if (name.length() > 0) name += " ";
        
        name += lastName;
        
        return name;
    }
	
    /**
     * Builds the shipped to agent's name string.
     * @param title
     * @param firstName
     * @param lastName
     * @param middleInitial
     */
    public String buildNameString(String title, String firstName, String lastName, String middleInitial)
    {
        String name = buildNameString(firstName, lastName, middleInitial);
        if (title != null)
        {
        	name = title + " " + name;
        }
        return name;
    }
    
    /**
     * Builds the shipped to agent's name string.
     * @param override - if this field is present it will be returned unchanged
     * @param title
     * @param firstName
     * @param lastName
     * @param middleInitial
     */
    public String buildNameString(String override, String title, String firstName, String lastName, String middleInitial)
    {
    	if (override != null) return override;
    	
    	return buildNameString(title, firstName, lastName, middleInitial);
    }
}
