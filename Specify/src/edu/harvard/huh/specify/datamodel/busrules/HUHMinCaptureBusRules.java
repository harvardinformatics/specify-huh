package edu.harvard.huh.specify.datamodel.busrules;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.db.DBFieldInfo;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.core.expresssearch.QueryAdjusterForDomain;
import edu.ku.brc.af.ui.forms.BaseBusRules;
import edu.ku.brc.af.ui.forms.BusinessRulesIFace;
import edu.ku.brc.af.ui.forms.FormDataObjIFace;
import edu.ku.brc.af.ui.forms.FormHelper;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.CollectingEvent;
import edu.ku.brc.specify.datamodel.Collection;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.DataModelObjBase;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Locality;
import edu.ku.brc.specify.datamodel.Preparation;

public class HUHMinCaptureBusRules extends BaseBusRules implements BusinessRulesIFace
{
    private static final Logger  log   = Logger.getLogger(HUHMinCaptureBusRules.class);

    public HUHMinCaptureBusRules()
    {
        super();
    }

    @Override
    public void addChildrenToNewDataObjects(Object newDataObj)
    {
        Fragment fragment = (Fragment) newDataObj;
        
        CollectionObject collectionObject = fragment.getCollectionObject();

        if (collectionObject == null)
        {
            collectionObject = new CollectionObject();
            collectionObject.initialize();
            fragment.setCollectionObject(collectionObject);
        }
        
        if (collectionObject.getCataloger() == null)
        {
            Agent agent = Agent.getUserAgent();
            collectionObject.setCataloger(agent);
            collectionObject.setCatalogedDate(Calendar.getInstance());
            collectionObject.setCatalogedDatePrecision((byte) UIFieldFormatterIFace.PartialDateEnum.Full.ordinal());
        }
    }
    
	@Override
	public void afterFillForm(Object dataObj)
	{

	}
    
	@Override
	public void beforeMerge(Object dataObj, DataProviderSessionIFace session)
	{
	    Fragment fragment = (Fragment) dataObj;
	    
	    CollectionObject collObj = fragment.getCollectionObject();
        if (collObj.getCollection() == null)
        {
            Collection catSeries = AppContextMgr.getInstance().getClassObject(Collection.class);
            collObj.setCollection(catSeries); 
        }
        
        CollectingEvent collEvt = collObj.getCollectingEvent();
        if (collEvt != null && collEvt.getId() == null)
        {
            collObj.setCollectingEvent(null);
        }
        
	    collObj = (CollectionObject) HUHFragmentBusRules.saveObject(collObj, session);
	    fragment.setCollectionObject(collObj);
	    
	    Preparation prep = fragment.getPreparation();
	    prep.setCountAmt(1);
	    
	    prep = (Preparation) HUHFragmentBusRules.saveObject(prep, session);
	    //fragment.setPreparation(prep);
	}

	/** Either the Fragment or its Preparation must have a barcode, and the barcode
     *  must be unique across the union of all Fragment and Preparation objects .
     * @param dataObj the Fragment to check
     * @return whether the Fragment or its Preparation has a unique barcode
     */
    protected STATUS isBarcodeOK(final FormDataObjIFace dataObj)
    {
        if (dataObj instanceof Fragment)
        {
            Fragment fragment = (Fragment) dataObj;
            Preparation prep  = fragment.getPreparation();

            String fieldName = "preparation.identifier";
            String barcode = (String)FormHelper.getValue(fragment, fieldName);
            
            fieldName = "identifier";  // drop the "preparation" bit for the remainder of this method
            String formattedBarcode = formatToUI(Preparation.class, fieldName, barcode);
            
            Integer prepCount = 0;
            Integer fragmentCount = 0;
            if (!StringUtils.isEmpty(barcode))
            {
                // count preparations with this barcode
                prepCount =
                    HUHMinCaptureBusRules.getCountSql(Preparation.class, "preparationId", fieldName, barcode, prep.getId());
                
                // count fragment records with this barcode
                fragmentCount =
                    HUHMinCaptureBusRules.getCountSql(Fragment.class, "fragmentId", fieldName, barcode, fragment.getId());
            }
            
            if (fragmentCount + prepCount == 0)
            {
                if (!StringUtils.isEmpty(barcode))
                {
                    // preparation has the barcode
                    return STATUS.OK;
                }
                else
                {
                    if (fragment.getIdentifier() != null)
                    {
                        // fragment has the barcode
                        return STATUS.OK;
                    }
                    reasonList.add(getErrorMsg("GENERIC_FIELD_MISSING", Fragment.class, fieldName, ""));
                    return STATUS.Error;
                }
            }

            reasonList.add(getErrorMsg("GENERIC_FIELD_IN_USE", Fragment.class, fieldName, formattedBarcode));
            return STATUS.Error;
        }
        
        throw new IllegalArgumentException();
    }
    
    /**
     * Search the table represented by dataClass for fields with fieldName having value fieldValue; omit records with primaryFieldName
     * having value id.
     * 
     * @param dataClass
     * @param primaryFieldName
     * @param fieldName
     * @param fieldValue
     * @param id
     * @return
     */
    static int getCountSql(final Class<?> dataClass, final String primaryFieldName, final String fieldName, final String fieldValue, final Integer id)
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
        if (id != null)
        {
            sql += " AND " + countColumnName + " <> " + id;
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
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(HUHMinCaptureBusRules.class, ex);
            }
        }
        return obj;
    }
    
	@Override
	public STATUS processBusinessRules(Object dataObj)
	{
		STATUS status = super.processBusinessRules(dataObj);
		
		if (!STATUS.OK.equals(status)) return status;
		
		return isBarcodeOK((FormDataObjIFace) dataObj);
	}
}
