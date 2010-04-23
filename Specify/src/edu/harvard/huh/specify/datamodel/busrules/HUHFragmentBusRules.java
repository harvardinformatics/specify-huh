package edu.harvard.huh.specify.datamodel.busrules;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.db.DBFieldInfo;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.core.expresssearch.QueryAdjusterForDomain;
import edu.ku.brc.af.ui.forms.BaseBusRules;
import edu.ku.brc.af.ui.forms.BusinessRulesIFace;
import edu.ku.brc.af.ui.forms.BusinessRulesOkDeleteIFace;
import edu.ku.brc.af.ui.forms.DraggableRecordIdentifier;
import edu.ku.brc.af.ui.forms.FormDataObjIFace;
import edu.ku.brc.af.ui.forms.FormHelper;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.Collection;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.DataModelObjBase;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.busrules.CollectionObjectBusRules;

public class HUHFragmentBusRules extends BaseBusRules implements BusinessRulesIFace
{
    private static final Logger  log   = Logger.getLogger(HUHFragmentBusRules.class);
    
	@Override
	public void aboutToShutdown()
	{
		// TODO Auto-generated method stub
		super.aboutToShutdown();
	}

	@Override
	public void addChildrenToNewDataObjects(Object newDataObj)
	{
	    super.addChildrenToNewDataObjects(newDataObj);
	       Fragment fragment = (Fragment) newDataObj;
	        
	        if (fragment != null)
	        {
	            if (fragment.getPreparation() == null)
	            {
	                Preparation prep = new Preparation();
	                prep.initialize();
	                fragment.addReference(prep, "preparation");
	            }
	        }
	}

	@Override
	public void afterDeleteCommit(Object dataObj)
	{
		// TODO Auto-generated method stub
		super.afterDeleteCommit(dataObj);
	}

	@Override
	public void afterFillForm(Object dataObj)
	{

	}

	@Override
	public boolean afterSaveCommit(Object dataObj, DataProviderSessionIFace session)
	{
		// TODO Auto-generated method stub
		return super.afterSaveCommit(dataObj, session);
	}

	@Override
	public void afterSaveFailure(Object dataObj, DataProviderSessionIFace session)
	{
		// TODO Auto-generated method stub
		super.afterSaveFailure(dataObj, session);
	}

	@Override
	public void beforeDelete(Object dataObj, DataProviderSessionIFace session)
	{
		// TODO Auto-generated method stub
		super.beforeDelete(dataObj, session);
	}

	@Override
	public boolean beforeDeleteCommit(Object dataObj, DataProviderSessionIFace session)
	    throws Exception
	{
		// TODO Auto-generated method stub
		return super.beforeDeleteCommit(dataObj, session);
	}

	@Override
	public void beforeFormFill() {
		// TODO Auto-generated method stub
		super.beforeFormFill();
	}

	@Override
	public void beforeMerge(Object dataObj, DataProviderSessionIFace session)
	{

		Fragment fragment = (Fragment) dataObj;
		
		if (fragment != null)
		{
		    CollectionObject collObj = fragment.getCollectionObject();
		    boolean newColObj = false;
		    
		    if (collObj == null)
		    {
		        collObj = new CollectionObject();
		        collObj.initialize();
		        newColObj = true;
		    }
            if (collObj.getCollection() == null)
            {
                Collection catSeries = AppContextMgr.getInstance().getClassObject(Collection.class);
                collObj.setCollection(catSeries); 
            }
            saveObject(collObj, session);
            if (newColObj) fragment.addReference(collObj, "collectionObject");
            
            Preparation prep = fragment.getPreparation();
            boolean newPrep = false;
            
            if (prep == null)
            {
                prep = new Preparation();
                prep.initialize();
                newPrep = true;
            }
            saveObject(prep, session);
            if (newPrep) fragment.addReference(prep, "preparation");
		}
	}

	@Override
	public void beforeSave(Object dataObj, DataProviderSessionIFace session)
	{
		// TODO Auto-generated method stub
		super.beforeSave(dataObj, session);
	}

	@Override
	public boolean beforeSaveCommit(Object dataObj, DataProviderSessionIFace session)
	    throws Exception
	{
		// TODO Auto-generated method stub
		return super.beforeSaveCommit(dataObj, session);
	}

	@Override
	public boolean canCreateNewDataObject()
	{
		// TODO Auto-generated method stub
		return super.canCreateNewDataObject();
	}

	@Override
	public void createNewObj(boolean doSetIntoAndValidateArg, Object oldDataObj)
	{
		// TODO Auto-generated method stub
		super.createNewObj(doSetIntoAndValidateArg, oldDataObj);
	}

	@Override
	public boolean doesSearchObjectRequireNewParent()
	{
		// TODO Auto-generated method stub
		return super.doesSearchObjectRequireNewParent();
	}

	@Override
	public void formShutdown()
	{
		// TODO Auto-generated method stub
		super.formShutdown();
	}

	@Override
	public String getDeleteMsg(Object dataObj)
	{
		// TODO Auto-generated method stub
		return super.getDeleteMsg(dataObj);
	}

	@Override
	public String getMessagesAsString()
	{
		// TODO Auto-generated method stub
		return super.getMessagesAsString();
	}

	@Override
	public List<String> getWarningsAndErrors()
	{
		// TODO Auto-generated method stub
		return super.getWarningsAndErrors();
	}

	@Override
	public void initialize(Viewable viewable)
	{
		// TODO Auto-generated method stub
		super.initialize(viewable);
	}

    /** Either the Fragment or its Preparation must have a barcode, and the barcode
     *  must be unique across the union of all Fragment and Preparation objects .
     * @param dataObj the Fragment to check
     * @return whether the Fragment or its Preparation has a unique barcode
     */
    protected STATUS isBarcodeOK(final FormDataObjIFace dataObj)
    {
        String fieldName = "identifier";
        
        Class<?> fragmentClass = Fragment.class;
        Fragment fragment = (Fragment) dataObj;
        
        DBTableInfo fragmentTableInfo = DBTableIdMgr.getInstance().getByClassName(fragmentClass.getName());
        DBFieldInfo fragmentFieldInfo = fragmentTableInfo.getFieldByName(fieldName);
        
        String fragmentBarcode = (String)FormHelper.getValue(fragment, fieldName);
        String formattedFragmentBarcode = "";
        if (fragmentFieldInfo != null && fragmentFieldInfo.getFormatter() != null && fragmentBarcode != null)
        {
            Object fmtObj = fragmentFieldInfo.getFormatter().formatToUI(fragmentBarcode);
            if (fmtObj != null)
            {
                formattedFragmentBarcode = fmtObj.toString();
            }
        }
        
        Class<?> prepClass = Preparation.class;
        Preparation prep  = fragment.getPreparation();
        
        DBTableInfo prepTableInfo = DBTableIdMgr.getInstance().getByClassName(prepClass.getName());
        DBFieldInfo prepFieldInfo = prepTableInfo.getFieldByName(fieldName);

        String prepBarcode = (String)FormHelper.getValue(prep, fieldName);
        String formattedPrepBarcode = "";
        if (prepFieldInfo != null && prepFieldInfo.getFormatter() != null && prepBarcode != null)
        {
            Object fmtObj = prepFieldInfo.getFormatter().formatToUI(prepBarcode);
            if (fmtObj != null)
            {
                formattedPrepBarcode = fmtObj.toString();
            }
        }

        if (StringUtils.isEmpty(fragmentBarcode) && StringUtils.isEmpty(prepBarcode))
        {
            reasonList.add(getErrorMsg("GENERIC_FIELD_MISSING", fragmentClass, fieldName, formattedFragmentBarcode));

            return STATUS.Error;
        }
        
        if (!StringUtils.isEmpty(fragmentBarcode) && fragmentBarcode.equals(prepBarcode))
        {
            reasonList.add(getErrorMsg("GENERIC_FIELD_IN_USE", fragmentClass, fieldName, formattedFragmentBarcode));

            return STATUS.Error;
        }

        // Let's check for duplicates of the fragment identifier
        Integer fragmentCount = getCountSql(fragmentClass, "fragmentId", fieldName, fragmentBarcode, dataObj.getId());
        if (fragmentCount == null) fragmentCount = 0;

        Integer prepCount = getCountSql(Preparation.class, "preparationId", fieldName, fragmentBarcode, null);
        if (prepCount == null) prepCount = 0;

        if (fragmentCount + prepCount != 0)
        {
            reasonList.add(getErrorMsg("GENERIC_FIELD_IN_USE", fragmentClass, fieldName, formattedFragmentBarcode));

            return STATUS.Error;
        }
        
        // Check for duplicates of the preparation identifier
        fragmentCount = getCountSql(fragmentClass, "fragmentId", fieldName, prepBarcode, null);
        if (fragmentCount == null) fragmentCount = 0;
        
        prepCount = getCountSql(Preparation.class, "preparationId", fieldName, fragmentBarcode, prep.getId());
        if (prepCount == null) prepCount = 0;
        
        if (fragmentCount + prepCount == 0)
        {
            reasonList.add(getErrorMsg("GENERIC_FIELD_IN_USE", fragmentClass, fieldName, formattedPrepBarcode));
        }
        
        return STATUS.OK;
    }
    
    private int getCountSql(final Class<?> dataClass, final String primaryFieldName, final String fieldName, final String fieldValue, final Integer id)
    {
        DBTableInfo tableInfo = DBTableIdMgr.getInstance().getByClassName(dataClass.getName());
        DBFieldInfo primaryFieldInfo = tableInfo.getFieldByName(primaryFieldName);
        
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
        
        DBFieldInfo fieldInfo = tableInfo.getFieldByName(fieldName);
        String quote = fieldInfo.getDataClass() == String.class || fieldInfo.getDataClass() == Date.class ? "'" : "";
        
        String tableName = tableInfo.getName();
        String compareColumName = fieldInfo.getColumn();
        String sql = String.format("SELECT COUNT(%s) FROM %s WHERE %s = %s", countColumnName, tableName, compareColumName, quote + fieldValue + quote);

        if (id != null)
        {
            sql += " AND " + countColumnName + " <> " + id;
        }

        String special = QueryAdjusterForDomain.getInstance().getSpecialColumns(tableInfo, false);
        sql += StringUtils.isNotEmpty(special) ? (" AND "+special) : "";
        
        log.debug(sql);
        
        return BasicSQLUtils.getCount(sql);
    }

    @Override
	public boolean isOkToAssociateSearchObject(Object newParentDataObj, Object dataObjectFromSearch)
	{
		// TODO Auto-generated method stub
		return super.isOkToAssociateSearchObject(newParentDataObj, dataObjectFromSearch);
	}

	@Override
	public boolean isOkToSave(Object dataObj, DataProviderSessionIFace session)
	{
		// TODO Auto-generated method stub
		return super.isOkToSave(dataObj, session);
	}

	@Override
	public void okToDelete(Object dataObj, DataProviderSessionIFace session, BusinessRulesOkDeleteIFace deletable)
	{
		// TODO Auto-generated method stub
		super.okToDelete(dataObj, session, deletable);
	}

	@Override
	public boolean okToEnableDelete(Object dataObj)
	{
		// TODO Auto-generated method stub
		return super.okToEnableDelete(dataObj);
	}

	@Override
	public STATUS processBusinessRules(Object dataObj)
	{
		STATUS status = super.processBusinessRules(dataObj);
		
		if (!STATUS.OK.equals(status)) return status;
		
		return isBarcodeOK((FormDataObjIFace) dataObj);
	}

	@Override
	public STATUS processBusinessRules(Object parentDataObj, Object dataObj, boolean isExistingObject)
	{
		// TODO Auto-generated method stub
		return super.processBusinessRules(parentDataObj, dataObj, isExistingObject);
	}

	@Override
	public Object processSearchObject(Object newParentDataObj, Object dataObjectFromSearch)
	{
		// TODO Auto-generated method stub
		return super.processSearchObject(newParentDataObj, dataObjectFromSearch);
	}

	@Override
	public void setObjectIdentity(Object dataObj, DraggableRecordIdentifier draggableIcon)
	{
		// TODO Auto-generated method stub
		super.setObjectIdentity(dataObj, draggableIcon);
	}

	@Override
	public boolean shouldCloneField(String fieldName)
	{
		if ("preparation".equals(fieldName)) return true;
		
		return super.shouldCloneField(fieldName);
	}

	@Override
	public boolean shouldCreateSubViewData(String fieldName)
	{
		// TODO Auto-generated method stub
		return super.shouldCreateSubViewData(fieldName);
	}
    
    private void saveObject(final DataModelObjBase obj, final DataProviderSessionIFace session)
    {
        if (obj != null)
        {
            try
            {
                if (obj.getId() != null)
                {
                    session.merge(obj);
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
    }
}
