package edu.harvard.huh.specify.datamodel.busrules;

import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.ku.brc.af.core.db.DBFieldInfo;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.core.expresssearch.QueryAdjusterForDomain;
import edu.ku.brc.af.ui.forms.BusinessRulesIFace;
import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.CollectingEvent;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.DataModelObjBase;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.busrules.AttachmentOwnerBaseBusRules;

public class HUHFragmentBusRules extends AttachmentOwnerBaseBusRules implements BusinessRulesIFace
{
    private static final Logger  log   = Logger.getLogger(HUHFragmentBusRules.class);

    public HUHFragmentBusRules()
    {
        super();
    }    
    
    @Override
    public void addChildrenToNewDataObjects(Object newDataObj)
    {
        super.addChildrenToNewDataObjects(newDataObj);
        
        /*// only add new collection object if there is not a valcombobox for it.
        if (formViewObj != null && newDataObj != null)
        {
            if (formViewObj.getControlByName("collectionObject") == null)
            {
                Fragment fragment = (Fragment) newDataObj;
                
                if (fragment.getCollectionObject() == null)
                {
                    CollectionObject collObj = new CollectionObject();
                    collObj.initialize();

                    Agent agent = Agent.getUserAgent();
                    collObj.setCataloger(agent);
                    collObj.setCatalogedDate(Calendar.getInstance());
                    collObj.setCatalogedDatePrecision((byte) UIFieldFormatterIFace.PartialDateEnum.Full.ordinal());

                    fragment.addReference(collObj, "collectionObject");
                    
                    CollectingEvent collEvt = new CollectingEvent();
                    collEvt.initialize();
                    collObj.addReference(collEvt, "collectingEvent");
                    
                    Locality loc = new Locality();
                    loc.initialize();
                    collEvt.addReference(loc, "locality");
                }
            }
        }*/
    }

    @Override
    public void beforeFormFill()
    {
        super.beforeFormFill();
        
        /*if (formViewObj != null)
        {
            if (formViewObj.getControlByName("collectionObject") != null) return;
            
            Fragment fragment = (Fragment) formViewObj.getDataObj();

            if (fragment != null)
            {
                CollectionObject collObj = fragment.getCollectionObject();
                if (collObj == null)
                {
                    collObj = new CollectionObject();
                    collObj.initialize();
                    
                    Agent agent = Agent.getUserAgent();
                    collObj.setCataloger(agent);
                    collObj.setCatalogedDate(Calendar.getInstance());
                    collObj.setCatalogedDatePrecision((byte) UIFieldFormatterIFace.PartialDateEnum.Full.ordinal());

                    fragment.addReference(collObj, "collectionObject");
                }
                CollectingEvent collEvt = collObj.getCollectingEvent();
                if (collEvt == null)
                {
                    collEvt = new CollectingEvent();
                    collEvt.initialize();
                    collObj.addReference(collEvt, "collectingEvent");
                }
                Locality loc = collEvt.getLocality();
                if (loc == null)
                {
                    loc = new Locality();
                    loc.initialize();
                    collEvt.addReference(loc, "locality");
                }
            }
        }*/
    }
    
    @Override
    public void beforeMerge(Object dataObj, DataProviderSessionIFace session)
    {        
        super.beforeMerge(dataObj, session);
        
        /*Fragment fragment = (Fragment) dataObj;
        CollectionObject collObj = fragment.getCollectionObject();
        
        if (collObj != null)
        {
            // save the collecting event
            CollectingEvent collEvt = collObj.getCollectingEvent();
            if (collEvt != null)
            {
                // save the locality
                Locality loc = collEvt.getLocality();
                if (loc != null)
                {
                    loc = (Locality) HUHFragmentBusRules.saveObject(loc, session);
                    collEvt.setLocality(loc);
                }
                collEvt = (CollectingEvent) HUHFragmentBusRules.saveObject(collEvt, session);
                //collObj.setCollectingEvent(collEvt);
            }
            
            // save the collection object
            collObj = (CollectionObject) HUHFragmentBusRules.saveObject(collObj, session);
            //fragment.setCollectionObject(collObj);
        }
        // save the preparation
        Preparation prep = fragment.getPreparation();

        if (prep != null)
        {
            prep.setCountAmt(1);

            // save the preparation
            prep = (Preparation) HUHFragmentBusRules.saveObject(prep, session);

            //fragment.setPreparation(prep);
        }*/
    }
    
    @Override
    public boolean okToEnableDelete(final Object dataObj)
    {
        boolean ok = super.okToEnableDelete(dataObj);
        
        if (!ok) return false;
        
        if (dataObj instanceof Fragment)
        {
            Fragment fragment = (Fragment) dataObj;
            
            // In our collectionrelationship form, the user can associate a fragment
            // with the current object.  The associated fragment becomes the right side,
            // and the current becomes the left.  f.getRightSideRels() returns
            // CollectionRelationship objects for which fragment f is on the right side.
            if (fragment.getRightSideRels().size() > 0) return false;
            if (fragment.getLeftSideRels().size()  > 0) return false;
        }
        
        return true;
    }
    
    @Override
    public boolean beforeDeleteCommit(final Object dataObj, final DataProviderSessionIFace session) throws Exception
    {
        boolean ok = super.beforeDeleteCommit(dataObj, session);
        
        if (!ok) return ok;
        
        if (dataObj instanceof Fragment)
        {
            Fragment fragment = (Fragment) dataObj;
            
            CollectionObject collObj = fragment.getCollectionObject();
            if (collObj != null)
            {
                if (collObj.getFragments().size() <= 1)
                {
                    // check business rules
                    BusinessRulesIFace delBusRules = DBTableIdMgr.getInstance().getBusinessRule(collObj);
                    if (delBusRules != null)
                    {
                        if (!delBusRules.okToEnableDelete(collObj)) return false;
                        
                        delBusRules.beforeDelete(collObj, session);
                        session.delete(collObj);
                        delBusRules.beforeDeleteCommit(collObj, session);                        
                    }
                    session.delete(collObj);
                }
            }
            
            Preparation prep = fragment.getPreparation();
            if (prep != null)
            {
                if (prep.getFragments().size() <= 1)
                {
                    // check business rules
                    BusinessRulesIFace delBusRules = DBTableIdMgr.getInstance().getBusinessRule(prep);
                    if (delBusRules != null)
                    {
                        if (!delBusRules.okToEnableDelete(prep)) return false;
                        
                        delBusRules.beforeDelete(prep, session);
                        session.delete(prep);
                        delBusRules.beforeDeleteCommit(prep, session);                        
                    }
                    session.delete(prep);
                }
            }
        }
        return true;
    }
    
    
    /** Either the Fragment or its Preparation must have a barcode, and the barcode
     *  must be unique across the union of all Fragment and Preparation objects.
     *  
     * @param dataObj the Fragment to check
     * @return whether the Fragment or its Preparation has a unique barcode
     */    
    static STATUS checkBarcode(final Fragment fragment, List<String> reasonList)
    {
        Preparation prep  = fragment.getPreparation();
        
        String fBarcodeFieldName = "identifier";
        String fBarcode = fragment.getIdentifier();
        Integer fId = fragment.getId();
        
        String pBarcodeFieldName = fBarcodeFieldName;
        String pBarcode = null;
        Integer pId = null;
        
        if (prep != null)
        {
            pBarcode = prep.getIdentifier();
            pId = prep.getId();
        }
        
        // There must be a barcode associated with at least one of the fragments on this preparation
        if (fBarcode == null && !HasOtherBarcode(prep))
        {
            reasonList.add(getErrorMsg("GENERIC_FIELD_MISSING", Fragment.class, fBarcodeFieldName, ""));
            return STATUS.Error;
        }
        
        if (StringUtils.isEmpty(fBarcode)) return STATUS.OK;
        
        // If the fragment has a barcode, it must not already exist in other fragment records
        // or other preparation records in the database
        
        // count saved fragment records with this barcode
        int fCount = getCountSql(Fragment.class, "fragmentId", fBarcodeFieldName, fBarcode, fId);

        if (fCount > 0)
        {
            reasonList.add(getErrorMsg("GENERIC_FIELD_IN_USE", Fragment.class, fBarcodeFieldName, fBarcode));
            return STATUS.Error;
        }

        // count saved preparation records with this barcode
        int pCount = getCountSql(Preparation.class, "preparationId", pBarcodeFieldName, fBarcode, pId);

        if (pCount > 0)
        {
            reasonList.add(getErrorMsg("GENERIC_FIELD_IN_USE", Preparation.class, pBarcodeFieldName, fBarcode));
            return STATUS.Error;
        }
            
        // count unsaved records with this barcode
        if (fBarcode.equals(pBarcode))
        {
            reasonList.add(getErrorMsg("GENERIC_FIELD_IN_USE", Fragment.class, fBarcodeFieldName, fBarcode));
            return STATUS.Error;
        }
        
        return STATUS.OK;
    }

    static STATUS checkBarcode(final Preparation prep, List<String> reasonList)
    {
        String fieldName = "identifier";
        
        // There must be a barcode associated with at least one of the fragments on this preparation
        if (!HasOtherBarcode(prep))
        {
            reasonList.add(getErrorMsg("GENERIC_FIELD_MISSING", Fragment.class, fieldName, ""));
            return STATUS.Error;
        }
 
        // Each barcode must not exist in the set of saved fragments and preparation excluding the
        // current preparation and its fragments; find the set of existing ids.
        Integer prepId = prep.getId();
        HashSet<Integer> fragmentIds = new HashSet<Integer>();
        
        // The various barcodes in the set of unsaved fragments and this preparation must be distinct;
        // find the set of barcodes.
        HashSet<String> barcodes = new HashSet<String>();
        String pBarcode = prep.getIdentifier();
        
        for (Fragment fragment : prep.getFragments())
        {
            Integer fragmentId = fragment.getId();
            if (fragmentId != null) fragmentIds.add(fragmentId);
            
            String fBarcode = fragment.getIdentifier();
            if (!StringUtils.isEmpty(fBarcode))
            {
                if (barcodes.contains(fBarcode) || fBarcode.equals(pBarcode))
                {
                    reasonList.add(getErrorMsg("GENERIC_FIELD_IN_USE", Fragment.class, fieldName, fBarcode));
                    return STATUS.Error;
                }
                barcodes.add(fBarcode);
            }
        }

        // check this preparation's barcode against database
        if (!StringUtils.isEmpty(pBarcode))
        {
            int pCount = getCountSql(Preparation.class, "preparationId", fieldName, pBarcode, prepId);

            if (pCount > 0)
            {
                reasonList.add(getErrorMsg("GENERIC_FIELD_IN_USE", Fragment.class, fieldName, pBarcode));
                return STATUS.Error;
            }
            
            int fCount = getCountSql(Fragment.class, "fragmentId", fieldName, pBarcode, fragmentIds);

            if (fCount > 0)
            {
                reasonList.add(getErrorMsg("GENERIC_FIELD_IN_USE", Fragment.class, fieldName, pBarcode));
                return STATUS.Error;
            }
        }

        // check each fragment's barcode against the database
        for (Fragment fragment : prep.getFragments())
        {
            String fBarcode = fragment.getIdentifier();

            if (fBarcode != null)
            {
                int pCount = getCountSql(Preparation.class, "preparationId", fieldName, fBarcode, prepId);

                if (pCount > 0)
                {
                    reasonList.add(getErrorMsg("GENERIC_FIELD_IN_USE", Fragment.class, fieldName, fBarcode));
                    return STATUS.Error;
                }

                int fCount = getCountSql(Fragment.class, "fragmentId", fieldName, fBarcode, fragmentIds);

                if (fCount > 0)
                {
                    reasonList.add(getErrorMsg("GENERIC_FIELD_IN_USE", Fragment.class, fieldName, fBarcode));
                    return STATUS.Error;
                }
            }
        }

        return STATUS.OK;
    }
    
    /**
     * Return true if the preparation's Identifier is not empty, or if at least one
     * of its associated fragments' Identifiers is not empty.  Otherwise return false.
     */
    static boolean HasOtherBarcode(final Preparation prep)
    {
        if (prep == null) return false;
        
        String barcode = prep.getIdentifier();
        
        if (!StringUtils.isEmpty(barcode)) return true;
        
        for (Fragment fragment : prep.getFragments())
        {
            barcode = fragment.getIdentifier();
            if (!StringUtils.isEmpty(barcode)) return true;
        }
        
        return false;
    }

    static int getCountSql(final Class<?> dataClass, final String primaryFieldName, final String fieldName, final String fieldValue, final Integer id)
    {
        HashSet<Integer> ids = new HashSet<Integer>();

        if (id != null) ids.add(id);
        
        return getCountSql(dataClass, primaryFieldName, fieldName, fieldValue, ids);
    }

    /**
     * Search the table represented by dataClass for fields with fieldName having value fieldValue; omit records with primaryFieldName
     * having value in the set of ids.
     */
    static int getCountSql(final Class<?> dataClass, final String primaryFieldName, final String fieldName, final String fieldValue, final Set<Integer> ids)
    {
        DBTableInfo tableInfo = DBTableIdMgr.getInstance().getByClassName(dataClass.getName());
        DBFieldInfo fieldInfo = tableInfo.getFieldByName(fieldName);
        DBFieldInfo primaryFieldInfo = tableInfo.getFieldByName(primaryFieldName);
        
        // find a primary key column to do count() on
        String  countColumnName = null;
        if (primaryFieldInfo != null)
        {
           countColumnName = primaryFieldInfo.getColumn(); 
        } else
        {
            if (tableInfo.getIdFieldName().equals(primaryFieldName))
            {
                countColumnName = tableInfo.getIdColumnName();
            }
        }
        
        // do we have to wrap the field value in quotes?
        String quote = fieldInfo.getDataClass() == String.class || fieldInfo.getDataClass() == Date.class ? "'" : "";
        
        // compose sql: select count(primary_key) from table_name where field_name = field_value
        String tableName = tableInfo.getName();
        String compareColumName = fieldInfo.getColumn();
        String sql = String.format("SELECT COUNT(%s) FROM %s WHERE %s = %s", countColumnName, tableName, compareColumName, quote + fieldValue + quote);

        // if we should ignore records with a given primary_key from the result, add a clause: and primary_key <> id_value
        if (ids != null && ids.size() > 0)
        {
            sql += " AND " + countColumnName + " NOT IN (";
            for (Integer id : ids) sql +=  id.toString() + " ";
            sql += ") ";
        }

        // not sure what special columns might be but I saw this called somewhere else
        String special = QueryAdjusterForDomain.getInstance().getSpecialColumns(tableInfo, false);
        sql += StringUtils.isNotEmpty(special) ? (" AND "+special) : "";
        
        log.debug(sql);
        
        Integer count = BasicSQLUtils.getCount(sql);
        return count == null ? 0 : count;
    }

    static String formatToUI(Class<?> dataClass, String fieldName, String value)
    {
        DBTableInfo tableInfo = DBTableIdMgr.getInstance().getByClassName(dataClass.getName());
        DBFieldInfo fieldInfo = tableInfo.getFieldByName(fieldName);
        
        String formattedField = "";
        if (fieldInfo != null && fieldInfo.getFormatter() != null && value != null)
        {
            Object fmtObj = fieldInfo.getFormatter().formatToUI(value);
            if (fmtObj != null)
            {
                formattedField = fmtObj.toString();
            }
        }
        
        return formattedField;
    }
    
    static DataModelObjBase saveObject(final DataModelObjBase obj, final DataProviderSessionIFace session)
    {
        if (obj != null)
        {
            try
            {
                if (obj.getId() != null)
                {
                    return session.merge(obj);
                }
                else
                {
                    session.save(obj);
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(HUHFragmentBusRules.class, ex);
            }
        }
        return obj;
    }
    
    static STATUS checkDeterminations(final Fragment fragment, List<String> reasonList)
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
                return STATUS.Warning;
            }
        }
        return STATUS.OK;
    }

    @Override
	public STATUS processBusinessRules(Object dataObj)
	{
		STATUS status = super.processBusinessRules(dataObj);
		
		if (!STATUS.OK.equals(status)) return status;
		
		if (dataObj instanceof Fragment)
        {
            Fragment fragment = (Fragment) dataObj;
            
            status = checkBarcode(fragment, reasonList);
        }

		if (!STATUS.OK.equals(status)) return status;
		
		if (dataObj instanceof Preparation)
		{
		    Preparation prep = (Preparation) dataObj;
		    status = HUHPreparationBusRules.isParentOK(prep, reasonList);
		}

        return status;
	}
}
