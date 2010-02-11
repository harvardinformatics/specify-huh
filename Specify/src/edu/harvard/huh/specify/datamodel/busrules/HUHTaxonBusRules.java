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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;

import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.AgentVariant;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.Treeable;
import edu.ku.brc.specify.datamodel.busrules.CollectionObjectBusRules;
import edu.ku.brc.specify.datamodel.busrules.TaxonBusRules;
import edu.ku.brc.specify.treeutils.TreeHelper;

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
public class HUHTaxonBusRules extends TaxonBusRules
{    
    /**
     * Constructor.
     */
    public HUHTaxonBusRules()
    {
        super();
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.busrules.BaseBusRules#beforeSave(java.lang.Object, edu.ku.brc.dbsupport.DataProviderSessionIFace)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void beforeSave(Object dataObj, DataProviderSessionIFace session)
    {
        super.beforeSave(dataObj, session);
        
        if (dataObj instanceof Taxon)
        {
           Taxon taxon = (Taxon) dataObj;
           
           String parAuthorName   = null;
           String stdAuthorName   = null;

           if (taxon.getStdAuthor() != null)
           {
               stdAuthorName = taxon.getStdAuthor().getAuthorName();
               
               if (taxon.getStdExAuthor() != null)
               {
                   String stdExAuthorName = taxon.getStdExAuthor().getAuthorName();
                   
                   if (stdAuthorName != null && stdExAuthorName != null)
                   {
                       stdAuthorName = stdAuthorName + " ex " + stdExAuthorName;
                   }
               }
               
               if (taxon.getParAuthor() != null)
               {
                   parAuthorName = taxon.getParAuthor().getAuthorName();
                   
                   if (taxon.getParExAuthor() != null)
                   {
                       String parExAuthorName = taxon.getParExAuthor().getAuthorName();
                       
                       if (parAuthorName != null && parExAuthorName != null)
                       {
                           parAuthorName = parAuthorName + " ex " + parExAuthorName;
                       }
                   }
               }

               if (stdAuthorName != null && parAuthorName != null)
               {
                   stdAuthorName = "(" + parAuthorName + ") " + stdAuthorName;
               }

               // set its author
               taxon.setAuthor(stdAuthorName);
           }
        }
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#processBusinessRules(java.lang.Object)
     */
    @Override
    public STATUS processBusinessRules(final Object dataObj)
    {
        reasonList.clear();
        STATUS status =  super.processBusinessRules(dataObj);
        
        if (status == STATUS.OK)
        {
            status = checkAuthors((Taxon) dataObj);
        }
        return status;
    }
    
    /**
     * Process business rules specifically related to determinations.  Return OK status, or add error messages
     * to reasonList and return Error status.
     * @param dataObj
     * @return 
     */
    protected STATUS checkAuthors(final Taxon taxon)
    {
        // add authors as we check the rules
        List<Agent> authors = new ArrayList<Agent>();
        
        // check that if there is a par ex author, then there is a par author
        if (taxon.getParExAuthor() != null)
        {
            if (taxon.getParAuthor() == null)
            {
                reasonList.add(getResourceString("TaxonBusRules.NO_PAR_AUTHOR_FOR_EX"));
                return STATUS.Error;
            }
            authors.add(taxon.getParExAuthor());
        }

        // check that if there is a std ex author, then there is a std author
        if (taxon.getStdExAuthor() != null)
        {
            if (taxon.getStdAuthor() == null)
            {
                reasonList.add(getResourceString("TaxonBusRules.NO_STD_AUTHOR_FOR_EX"));
                return STATUS.Error;
            }
            authors.add(taxon.getStdExAuthor());
        }
        
        // check that if there is a par author, then there is a std author
        if (taxon.getParAuthor() != null)
        {
            if (taxon.getStdAuthor() == null)
            {
                reasonList.add(getResourceString("TaxonBusRules.NO_STD_AUTHOR_FOR_PAR"));
                return STATUS.Error;
            }
            authors.add(taxon.getParAuthor());
        }
        
        if (taxon.getStdAuthor() != null)
        {
            authors.add(taxon.getStdAuthor());
        }
        
        // check that all authors have auth abbrev variants
        STATUS authorStatus = STATUS.OK;
        
        DataProviderSessionIFace tmpSession = DataProviderFactory.getInstance().createSession();
        for (Agent author : authors)
        {
            try
            {
                tmpSession.attach(author);
                
            } catch (Exception ex)
            {
                ex.printStackTrace();
                edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(CollectionObjectBusRules.class, ex);
                break;
            }
            
            Hibernate.initialize(author);
            
            boolean foundIt = false;
            for (AgentVariant variant : author.getVariants())
            {
                if (variant.getVarType().equals(AgentVariant.AUTHOR_ABBREV))
                {
                    foundIt = true;
                    break;
                }
            }
            if (!foundIt)
            {
                reasonList.add(String.format(getResourceString("TaxonBusRules.NO_AUTHOR_ABBREV_VAR"), author.getLastName()));
                authorStatus = STATUS.Error;
            }
        }
        tmpSession.close();
        
        if (authorStatus != STATUS.OK)
        {
            return authorStatus;
        }

        // warn if no std author
        if (taxon.getStdAuthor() == null)
        {
            reasonList.add(getResourceString("TaxonBusRules.NO_STD_AUTHOR"));

            return STATUS.Warning;
        }
        
        return STATUS.OK;
    }
}
