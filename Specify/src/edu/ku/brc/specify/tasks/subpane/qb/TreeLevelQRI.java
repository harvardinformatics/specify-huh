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
package edu.ku.brc.specify.tasks.subpane.qb;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.db.DBFieldInfo;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.dbsupport.DataProviderSessionIFace.QueryIFace;
import edu.ku.brc.specify.config.SpecifyAppContextMgr;
import edu.ku.brc.specify.datamodel.TreeDefIface;
import edu.ku.brc.specify.datamodel.TreeDefItemIface;
import edu.ku.brc.specify.datamodel.Treeable;
import edu.ku.brc.ui.UIRegistry;
import edu.ku.brc.util.Pair;

/**
 * @author timbo
 *
 * @code_status Alpha
 *
 */
public class TreeLevelQRI extends FieldQRI
{
    protected final int rankId;
    protected final int treeDefId;

    protected String    tableAlias = null;
    
    /**
     * @param parent
     * @param fi
     * @param rankId
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public TreeLevelQRI(final TableQRI parent, final DBFieldInfo fi, final int rankId)
            throws Exception
    {
        super(parent, fi);
        this.rankId = rankId;
        SpecifyAppContextMgr spMgr = (SpecifyAppContextMgr )AppContextMgr.getInstance();
        TreeDefIface<?, ?, ?> treeDef = spMgr.getTreeDefForClass((Class<? extends Treeable<?,?,?>> )getTableInfo().getClassObj());
        treeDefId = treeDef.getTreeDefId();
        TreeDefItemIface<?, ?, ?> treeDefItem = null;
        treeDefItem = treeDef.getDefItemByRank(rankId);
        if (treeDefItem != null)
        {
            title = treeDefItem.getName();
        }
        else
        {
            throw new NoTreeDefItemException(rankId);
        }
    }
    
    /**
     * @return the rankId
     */
    public int getRankId()
    {
        return rankId;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.tasks.subpane.qb.FieldQRI#getFieldName()
     */
    @Override
    public String getFieldName()
    {
        return title;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.tasks.subpane.qb.FieldQRI#getTableInfo()
     */
    @Override
    public DBTableInfo getTableInfo()
    {
        return table.getTableTree().getTableInfo();
    }
    
    /**
     * @param ta
     * @return complete specification of the field to be used in sql/hql fields clause.
     * 
     * Has a side-effect: Sets the tableAlias property
     */
    protected String getSQLFldName(final TableAbbreviator ta)
    {
//        tableAlias = ta.getAbbreviation(table.getTableTree());
//        return tableAlias + ".nodeNumber";
        
        return "";
    }
    
    /**
     * @return the tableAlias used for this field when sql was generated.
     */
    public String getTableAlias()
    {
        return tableAlias;
    }
    
    /**
     * @return the treeDefId
     */
    public int getTreeDefId()
    {
        return treeDefId;
    }
    
    /**
     * @return the class of nodes in the tree.
     */
    public Class<?> getTreeDataClass()
    {
        return getTableInfo().getClassObj();
    }
    
    /**
     * @return name of the field which links to the level's tree definition.
     */
    protected String getTreeDefIdFldName()
    {
        //sql?
        //return getTableInfo().getClassObj().getSimpleName() + "treeDefId";
        
        //hql
        return "definition";
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.tasks.subpane.qb.FieldQRI#getSQLFldSpec(edu.ku.brc.specify.tasks.subpane.qb.TableAbbreviator, boolean)
     */
    @Override
    public String getSQLFldSpec(final TableAbbreviator ta, final boolean forWhereClause,
    		final boolean forSchemaExport)
    {
        String result = getSQLFldName(ta);
        return result;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.tasks.subpane.qb.FieldQRI#isFieldHidden()
     */
    @Override
    public boolean isFieldHidden()
    {
        //this object wouldn't have been created if it was hidden (not in the tree def).
        return false;
    }
    
    protected String capitalize(final String toCap)
    {
        return toCap.substring(0, 1).toUpperCase().concat(toCap.substring(1));
    }
    
    /**
     * @return the maximum number of 'nodenumber between x and y' conditions to add to a where clause.
     */
    protected int getMaxNodeConditions()
    {
    	//return 0; //no max
    	//XXX determine a reasonable value to use here. Don't think there is a definite restriction on where clause complexity 
    	//in DBMS's but a where clause with a huge number of or conditions (easily hundreds for something like 
    	//species like '%us') CAN'T be very efficient and might kill the system.
    	return 523;    	
    }
    
    
    /* (non-Javadoc)
	 * @see edu.ku.brc.specify.tasks.subpane.qb.FieldQRI#getNullCondition(edu.ku.brc.specify.tasks.subpane.qb.TableAbbreviator, boolean, boolean)
	 */
	@Override
	public String getNullCondition(TableAbbreviator ta,
			boolean forSchemaExport, boolean negate)
	{
        String result = "exists (select treetbl.nodeNumber from " + table.getTableTree().getName() + " treetbl where "
        		+ "treetbl.rankId = " + rankId + " and " + ta.getAbbreviation(table.getTableTree()) + ".nodeNumber between "
        		+ "treetbl.nodeNumber and treetbl.highestChildNodeNumber)";
        if (!negate)
        {
        	result = "not " + result;
        }
        return result;
	}
	
	/**
	 * 
	 * @param node
	 * @return
	 */
	private String getDescendantsIn(DataProviderSessionIFace session, Class clazz, Integer parentId) {
	    String ids = getDescendantsInRecursive(session, clazz, parentId);
	    if (ids.length() > 0) {
	        return ids.substring(0, ids.length() - 2);
	    } else {
	        return "";
	    }
	}
	
	private String getDescendantsInRecursive(DataProviderSessionIFace session, Class clazz, Integer parentId) {
	    String in = "";
	    
	    QueryIFace query = session.createQuery("select e.id from " + clazz.getCanonicalName() + " e where e.parent.id = :parentId", false);
	    query.setParameter("parentId", parentId);
	    List<Integer> ids = (List<Integer>) query.list();
	    for (Integer id : ids) {
	        in += id + ", ";
            in += getDescendantsIn(session, clazz, id);
	    }
	    
	    return in;
	}

	/**
     * @param criteria
     * @param ta
     * @param operStr
     * @param negate
     * @return a where clause condition for the given criteria using tree node-numbers.
     * 
     * Looks up the matching node (1 node - opearators are restricted for TreeLevels) and creates 
     * a condition to get it's descendants. 
     */
    @SuppressWarnings("unchecked")
    public String getNodeNumberCriteria(final String criteria, final TableAbbreviator ta, 
                                        final String operStr, final boolean negate) throws ParseException
    {
        if (criteria.equals("'%'") || criteria.equals("'*'"))
        {
        	//same as no condition. Almost - Like '%' won't return nulls, but maybe it should.
        	return null;
        }
                
    	DataProviderSessionIFace session = DataProviderFactory.getInstance()
        .createSession();
        try
        {
            SpecifyAppContextMgr spMgr = (SpecifyAppContextMgr )AppContextMgr.getInstance();
            TreeDefIface<?, ?, ?> treeDef = spMgr.getTreeDefForClass((Class<? extends Treeable<?,?,?>> )getTableInfo().getClassObj());

            String className = getTableInfo().getClassObj().getSimpleName();
            List<?> matches = session.getDataList("from " + className + " where name " + operStr + " " +  criteria + " and " + className + "TreeDefId = " + treeDef.getTreeDefId()
                    + " and rankId =" + String.valueOf(rankId));
            List<Pair<Integer, Integer>> nodeInfo = new LinkedList<Pair<Integer, Integer>>();
            if (matches.size() == 0)
            {
                return "2+2=2"; //that'll do the trick. 
            }
            
            if (getMaxNodeConditions() > 0 && matches.size() > getMaxNodeConditions())
            {
            	throw new ParseException(UIRegistry.getResourceString("QB_TOO_MANY_TREE_RANK_MATCHES"), -1);
            }
            
            ArrayList<String> inStatements = new ArrayList<String>();
            for (Object match : matches)
            {
                Treeable<?,?,?> node = (Treeable<?,?,?>)match;
//                nodeInfo.add(new Pair<Integer, Integer>(node.getNodeNumber(), node.getHighestChildNodeNumber()));
                String ids = getDescendantsIn(session, node.getClass(), node.getTreeId());
                inStatements.add(ids);
            }
            StringBuilder result = new StringBuilder();
            for (String in : inStatements)
            {
                if (result.length() > 0)
                {
                    if (negate)
                    {
                        result.append(" and ");
                    }
                    else
                    {
                        result.append(" or ");
                    }
                }
                result.append(ta.getAbbreviation(table.getTableTree()) + ".nodeNumber");
                if (negate)
                {
                    result.append(" not "); 
                }
                result.append(" in (");
                result.append(in);
                result.append(")");
            }
            return "(" + result.toString() + ")";
        }
        finally
        {
            session.close();
        }
    }

    @SuppressWarnings("serial")
    public class NoTreeDefItemException extends Exception
    {
        /**
         * @param rankId - rank of the level without a definition.
         */
        public NoTreeDefItemException(int rankId)
        {
            super("No TreeDefItem for " + String.valueOf(rankId));
        }
    }

}
