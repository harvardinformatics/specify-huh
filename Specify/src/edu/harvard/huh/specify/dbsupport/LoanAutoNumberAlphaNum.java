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
package edu.harvard.huh.specify.dbsupport;

import static edu.ku.brc.ui.UIRegistry.getLocalizedMessage;

import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.db.AutoNumberGeneric;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterField;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.Division;
import edu.ku.brc.util.Pair;

/**
 * @author rod
 *
 * @code_status Alpha
 *
 * Aug 8, 2007
 *
 */
public class LoanAutoNumberAlphaNum extends AutoNumberGeneric
{
    
    private static final Logger  log = Logger.getLogger(LoanAutoNumberAlphaNum.class);
    
    /**
     * Default Constructor. 
     */
    public LoanAutoNumberAlphaNum()
    {
        super();
        
        classObj  = Loan.class;
        fieldName = "loanNumber";
    }

    /**
     * Constructor with args.
     * @param properties the args
     */
    public LoanAutoNumberAlphaNum(final Properties properties)
    {
        super(properties);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.core.db.AutoNumberGeneric#getHighestObject(edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace, org.hibernate.Session, java.lang.String, edu.ku.brc.util.Pair, edu.ku.brc.util.Pair)
     */
    @Override
    protected String getHighestObject(final UIFieldFormatterIFace formatter, // TODO: check this return type; this changed, better look up the Specify KU changes.
                                      final Session session, 
                                      final String  value,
                                      final Pair<Integer, Integer> yearPos, 
                                      final Pair<Integer, Integer> pos) throws Exception
    {
        if (value == null || pos == null)
        {
            errorMsg  = getLocalizedMessage("AUTONUM_INC_ERR", value != null ? value : "null", (pos != null ? pos : "null"));
            return null;
        }
        
        int posLen  = 0;

        //List list = session.createCriteria(classObj).addOrder( Order.desc(fieldName) ).setMaxResults(1).list();
        StringBuilder sb = new StringBuilder(" FROM "+classObj.getSimpleName()); //$NON-NLS-1$

        sb.append(" ORDER BY"); //$NON-NLS-1$

        try
        {
            if (pos != null)
            {
                posLen = pos.second - pos.first;

                sb.append(" substring("+fieldName+","+(pos.first+1)+","+posLen+") desc"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            }
            
            //System.err.println(sb.toString());
            List<?> list = session.createQuery(sb.toString()).setMaxResults(1).list();
            if (list.size() == 1)
            {
                return list.get(0).toString(); // TODO: this changed, better look up the Specify KU changes.
            }
            
        } catch (Exception ex)
        {
            ex.printStackTrace();
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(AutoNumberGeneric.class, ex);
            errorMsg  = ex.toString();
        }
        return null;
    }
}
