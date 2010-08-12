package edu.harvard.huh.specify.config;

import edu.ku.brc.specify.config.Scriptlet;

// this class may be called in  iReport jrxml
public class HUHScriptlet extends Scriptlet {

    /**
     * Builds the shipped to agent's name string.
     * @param firstName
     * @param lastName
     */
    public String buildNameString(String firstName, String lastName)
    {
        String name = firstName == null ? "" : firstName;
        
        if (name.length() > 0) name += " ";
        
        name += lastName;
        
        return name;
    }
    
    /**
     * Builds the shipped to agent's name string.
     * @param title
     * @param firstName
     * @param lastName
     */
    @Override
    public String buildNameString(String title, String firstName, String lastName)
    {
        String name = buildNameString(firstName, lastName);

        if (title != null) name = title + " " + name;
        
        return name;
    }
	
    /**
     * Builds the shipped to agent's name string.
     * @param title
     * @param firstName
     * @param lastName
     * @param jobTitle
     */
    public String buildNameString(String title, String firstName, String lastName, String jobTitle)
    {
        String name = buildNameString(title, firstName, lastName);
        if (jobTitle != null)
        {
        	name = name + ", " + jobTitle;
        }
        return name;
    }

}
