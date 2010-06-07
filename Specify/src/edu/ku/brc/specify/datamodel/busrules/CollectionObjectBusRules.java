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
package edu.ku.brc.specify.datamodel.busrules;

import static edu.ku.brc.ui.UIRegistry.getLocalizedMessage;
import static edu.ku.brc.ui.UIRegistry.getResourceString;
import static edu.ku.brc.ui.UIRegistry.showLocalizedMsg;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.db.DBFieldInfo;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.core.expresssearch.QueryAdjusterForDomain;
import edu.ku.brc.af.ui.forms.BusinessRulesOkDeleteIFace;
import edu.ku.brc.af.ui.forms.FormDataObjIFace;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.Accession;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.CollectingEvent;
import edu.ku.brc.specify.datamodel.Collection;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.CollectionObjectAttribute;
import edu.ku.brc.specify.datamodel.DeaccessionPreparation;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.specify.datamodel.PrepType;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.Project;

/**
 * @author rods
 *
 * @code_status Beta
 *
 * Created Date: Jan 24, 2007
 *
 */
public class CollectionObjectBusRules extends AttachmentOwnerBaseBusRules
{
    //private static final Logger  log = Logger.getLogger(CollectionObjectBusRules.class);
    
    /**
     * Constructor.
     */
    public CollectionObjectBusRules()
    {
        super(CollectionObject.class);
    }
    
    /**
     * @param disciplineType
     * @return
     */
    protected PrepType getDefaultPrepType()
    {
        DBTableInfo tableInfo = DBTableIdMgr.getInstance().getInfoById(PrepType.getClassTableId());
        if (tableInfo != null)
        {
            String sqlStr = QueryAdjusterForDomain.getInstance().adjustSQL("FROM PrepType WHERE collectionId = COLLID");
            log.debug(sqlStr);
            if (StringUtils.isNotEmpty(sqlStr))
            {
                DataProviderSessionIFace session = null;
                try
                {
                    session = DataProviderFactory.getInstance().createSession();
                    List<?> dataList = session.getDataList(sqlStr);
                    if (dataList != null && !dataList.isEmpty())
                    {
                        // XXX for now we just get the First one
                        return (PrepType)dataList.iterator().next();
                    }
                    // No Data Error
        
                } catch (Exception ex)
                {
                    ex.printStackTrace();
                    edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                    edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(CollectionObjectBusRules.class, ex);
                    log.error(ex);
                    
                } finally
                {
                    if (session != null)
                    {
                        session.close();
                    }
                }
            } else
            {
                log.error("Query String is empty for tableId["+tableInfo.getTableId()+"]");
            }
        } else
        {
            throw new RuntimeException("Error looking up PickLIst's Table Name PrepType");
        }
        return null;
    }
    
    /**
     * @return the default preparer
     */
    protected Agent getDefaultPreparedByAgent()
    {
        return Agent.getUserAgent();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.busrules.BaseBusRules#addChildrenToNewDataObjects(java.lang.Object)
     */
    @Override
    public void addChildrenToNewDataObjects(final Object newDataObj)
    {
        super.addChildrenToNewDataObjects(newDataObj);
        
        CollectionObject colObj = (CollectionObject)newDataObj;
        if (AppContextMgr.getInstance().getClassObject(Collection.class).getIsEmbeddedCollectingEvent())
        {
            // Carry Forward may have already added 
            // some values so we need to check to make sure
            // before adding new ones automatically.
            if (colObj.getCollectingEvent() == null)
            {
                CollectingEvent ce = new CollectingEvent();
                ce.initialize();
                colObj.addReference(ce, "collectingEvent");
            }
            
            /* Need to be able to configure this via prefs
            if (colObj.getPreparations().size() == 0)
            {
                Preparation prep = new Preparation();
                prep.initialize();
                prep.setCount(1);
                prep.setPrepType(getDefaultPrepType());
                prep.setPreparedDate(Calendar.getInstance());
                colObj.addReference(prep, "preparations");
                prep.setPreparedByAgent(getDefaultPreparedByAgent());
            }*/
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
        
        CollectionObject colObj = (CollectionObject)dataObj;
        if (status == STATUS.OK && colObj.getId() == null)
        {
            DBTableInfo tblInfo   = DBTableIdMgr.getInstance().getInfoById(1); // don't need to check for null
            DBFieldInfo fieldInfo = tblInfo.getFieldByName("catalogNumber");
            UIFieldFormatterIFace fmt = fieldInfo.getFormatter();
            if ((fmt != null && fmt.getAutoNumber() == null) || !formViewObj.isAutoNumberOn())
            {
                status = processBusinessRules(null, dataObj, true);
            }

        }
        if (status == STATUS.OK)
        {
            status = checkDeterminations(dataObj);
        }
        return status;
    }
    
    /**
     * Process business rules specifically related to determinations.  Return OK status, or add error messages
     * to reasonList and return Error status.
     * @param dataObj
     * @return 
     */
    protected STATUS checkDeterminations(final Object dataObj)
    {
        // check that a current determination exists
    	CollectionObject collObj = (CollectionObject) dataObj;
    	
    	for (Fragment fragment : collObj.getFragments()) // mmk fragment
    	{
    		if (fragment.getDeterminations().size() > 0)
    		{
    			int currents = 0;
    			for (Determination det : fragment.getDeterminations())
    			{
    				if (det.isCurrentDet())
    				{
    					currents++;
    				}
    			}
    			if (currents != 1)
    			{
    				if (currents == 0)
    				{
    					reasonList.add(getResourceString("CollectionObjectBusRules.CURRENT_DET_REQUIRED"));
    				}
    				else
    				{
    					reasonList.add(getResourceString("CollectionObjectBusRules.ONLY_ONE_CURRENT_DET"));
    				}
    				return STATUS.Error;
    			}
    		}
    	}
        return STATUS.OK;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.ku.brc.ui.forms.BaseBusRules#beforeMerge(java.lang.Object,
     *      edu.ku.brc.dbsupport.DataProviderSessionIFace)
     */
    @Override
    public void beforeMerge(final Object dataObj, 
                            final DataProviderSessionIFace session)
    {
        super.beforeMerge(dataObj, session);
        
        CollectionObject colObj = (CollectionObject)dataObj;
        if (AppContextMgr.getInstance().getClassObject(Collection.class).getIsEmbeddedCollectingEvent())
        {
            CollectingEvent ce = colObj.getCollectingEvent();
            if (ce != null)
            {
                try
                {
                    if (ce != null && ce.getId() != null)
                    {
                        CollectingEvent mergedCE = session.merge(colObj.getCollectingEvent());
                        colObj.setCollectingEvent(mergedCE);
                    } else
                    {
                        session.save(colObj.getCollectingEvent());
                    }
                    
                } catch (Exception ex)
                {
                    ex.printStackTrace();
                    edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                    edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(CollectionObjectBusRules.class, ex);
                }
            }
        }
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.busrules.AttachmentOwnerBaseBusRules#beforeDeleteCommit(java.lang.Object, edu.ku.brc.dbsupport.DataProviderSessionIFace)
     */
    @Override
    public boolean beforeDeleteCommit(Object dataObj, DataProviderSessionIFace session) throws Exception
    {
        boolean ok = super.beforeDeleteCommit(dataObj, session);
        
        if (ok)
        {
            CollectionObject colObj = (CollectionObject)dataObj;
            if (AppContextMgr.getInstance().getClassObject(Collection.class).getIsEmbeddedCollectingEvent())
            {
                CollectingEvent ce = colObj.getCollectingEvent();
                if (ce != null)
                {
                    Locality loc = ce.getLocality();
                    
                    try
                    {
                        session.delete(loc);
                        session.delete(ce);
                        return true;
                        
                    } catch (Exception ex)
                    {
                        edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                        edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(CollectionObjectBusRules.class, ex);
                        ex.printStackTrace();
                        return false;
                    }
                }
            }
        }
        return ok;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#afterSaveCommit(java.lang.Object, edu.ku.brc.dbsupport.DataProviderSessionIFace)
     */
    @Override
    public boolean afterSaveCommit(final Object dataObj, final DataProviderSessionIFace session)
    {
        setLSID((FormDataObjIFace)dataObj);

        return super.afterSaveCommit(dataObj, session);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.busrules.BaseBusRules#okToDelete(java.lang.Object, edu.ku.brc.dbsupport.DataProviderSessionIFace, edu.ku.brc.ui.forms.BusinessRulesOkDeleteIFace)
     */
    @Override
    public void okToDelete(final Object dataObjArg,
                           final DataProviderSessionIFace sessionArg,
                           final BusinessRulesOkDeleteIFace deletable)
    {
        reasonList.clear();
        
        boolean isOK = true;
        if (deletable != null && dataObjArg instanceof FormDataObjIFace && ((FormDataObjIFace)dataObjArg).getId() != null)
        {
            Integer count = BasicSQLUtils.getCount("SELECT COUNT(*) FROM collectionobject WHERE CollectionObjectID = " + ((FormDataObjIFace)dataObjArg).getId());
            if (count != null && count == 0)
            {
                showLocalizedMsg("NO_RECORD_FOUND_TITLE", "NO_RECORD_FOUND");
                return;
            }
            
            Object dataObj = dataObjArg;
            DataProviderSessionIFace session = sessionArg != null ? sessionArg : DataProviderFactory.getInstance().createSession();
            try
            {
                dataObj = session.merge(dataObj);
    
                CollectionObject colObj = (CollectionObject)dataObj;
                
                /*if (colObj.getAccession() != null)
                {
                    isOK = false;
                    addDeleteReason(Accession.getClassTableId());
                }*/
                
                if (isOK)
                {
                    // 03/25/09 rods - Added these extra null checks because of Bug 6848
                    // I can't reproduce it and these should never be null.
                	Set<Preparation> preparations = new HashSet<Preparation>(); // mmk fragment
                	for (Fragment fragment : colObj.getFragments())
                	{
                		preparations.add(fragment.getPreparation());
                	}
                	
                    for (Preparation prep : preparations)
                    {
                        if (prep != null)
                        {
                            if (prep.getLoanPreparations() != null && !prep.getLoanPreparations().isEmpty())
                            {
                                isOK = false;
                                addDeleteReason(LoanPreparation.getClassTableId());
                            }
                            
                            if (prep.getDeaccessionPreparations() != null && !prep.getDeaccessionPreparations().isEmpty())
                            {
                                isOK = false;
                                addDeleteReason(DeaccessionPreparation.getClassTableId());
                            }
                            
                            if (!isOK)
                            {
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ex)
            {
                edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(CollectionObjectBusRules.class, ex);
                ex.printStackTrace();
                // Error Dialog
                
            } finally
            {
                if (sessionArg == null && session != null)
                {
                    session.close();
                }
            }
            
            deletable.doDeleteDataObj(dataObj, session, isOK);
            
        } else
        {
            super.okToDelete(dataObjArg, sessionArg, deletable);
        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BaseBusRules#shouldCloneField(java.lang.String)
     */
    @Override
    public boolean shouldCloneField(String fieldName)
    {
        // Depending on the Type of Discipline the Collecting Events can be shared 
        // a ManyToOne from CollectionObject to Collecting (Fish).
        //
        // Or it acts like a OneToOne where each CE acts as if it is "embedded" or is
        // a part of the CO.
        //System.err.println(fieldName);
        if (fieldName.equals("collectingEvent"))
        {
            // So we need to clone it make a full copy when it is embedded.
            return AppContextMgr.getInstance().getClassObject(Collection.class).getIsEmbeddedCollectingEvent();
            
        }
        
        if (fieldName.equals("collectionObjectAttribute"))
        {
            return true;
        }
        
        return false;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.BaseBusRules#processBusinessRules(java.lang.Object, java.lang.Object, boolean)
     */
    @Override
    public STATUS processBusinessRules(final Object parentDataObj, final Object dataObj, final boolean isEdit)
    {
        reasonList.clear();
        
        if (!(dataObj instanceof CollectionObject))
        {
            return STATUS.Error;
        }
        
        STATUS duplicateNumberStatus = isCheckDuplicateNumberOK("catalogNumber", 
                                                                (FormDataObjIFace)dataObj, 
                                                                CollectionObject.class, 
                                                                "collectionObjectId");
        
        return duplicateNumberStatus;
    }

    
//    /* (non-Javadoc)
//     * @see edu.ku.brc.specify.datamodel.busrules.AttachmentOwnerBaseBusRules#beforeSaveCommit(java.lang.Object, edu.ku.brc.dbsupport.DataProviderSessionIFace)
//     */
//    @Override
//    public boolean beforeSaveCommit(Object dataObj, DataProviderSessionIFace session)
//            throws Exception
//    {
//        if (!super.beforeSaveCommit(dataObj, session))
//        {
//            return false;
//        }
//        
//        for (Determination det : ((CollectionObject )dataObj).getDeterminations())
//        {
//            if (det.isCurrent())
//            {
//                return true;
//            }
//        }
//        return false;
//    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#isOkToAssociateSearchObject(java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean isOkToAssociateSearchObject(Object parentdataObj, Object dataObjectFromSearch)
    {
        if (parentdataObj instanceof Project)
        {
            reasonList.clear();
            
            Project          prj    = (Project)parentdataObj;
            CollectionObject colObj = (CollectionObject)dataObjectFromSearch;
            
            for (CollectionObject co : prj.getCollectionObjects())
            {
                if (co.getId().equals(colObj.getId()))
                {
                    reasonList.add(getLocalizedMessage("CO_DUP", colObj.getIdentityTitle()));
                    return false;
                }
            }
        }
        return true;
    }
    
    public static void fixDupColObjAttrs()
    {
        String sql = "SELECT * FROM (SELECT CollectionObjectAttributeID, count(*) as cnt FROM collectionobject c WHERE CollectionObjectAttributeID IS NOT NULL group by CollectionObjectAttributeID) T1 WHERE cnt > 1";
        Vector<Object[]> rows = BasicSQLUtils.query(sql);
        if (rows != null)
        {
            for (Object[] row : rows)
            {
                DataProviderSessionIFace session = null;
                try
                {
                    session = DataProviderFactory.getInstance().createSession();
                    CollectionObjectAttribute colObjAttr = session.get(CollectionObjectAttribute.class, (Integer)row[1]);
                    
                    int cnt = 0;
                    for (CollectionObject co : colObjAttr.getCollectionObjects())
                    {
                        if (cnt > 0)
                        {
                            CollectionObjectAttribute colObjAttribute = (CollectionObjectAttribute)colObjAttr.clone();
                            co.setCollectionObjectAttribute(colObjAttribute);
                            colObjAttribute.getCollectionObjects().add(co);
                            
                            session.beginTransaction();
                            session.saveOrUpdate(colObjAttribute);
                            session.saveOrUpdate(co);
                            session.commit();
                        }
                        cnt++;
                    }
                    
                } catch (Exception ex)
                {
                   session.rollback();
                   ex.printStackTrace();
                } finally
                {
                    if (session != null)
                    {
                        session.close();
                    }
                }
            }
        }
    }
}
