package edu.harvard.huh.specify.datamodel.busrules;

import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JCheckBox;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.ku.brc.af.core.db.DBFieldInfo;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.core.expresssearch.QueryAdjusterForDomain;
import edu.ku.brc.af.ui.forms.BusinessRulesIFace;
import edu.ku.brc.af.ui.forms.FormViewObj;
import edu.ku.brc.af.ui.forms.MultiView;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.CollectionRelationship;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.busrules.AttachmentOwnerBaseBusRules;

public class HUHFragmentBusRules extends AttachmentOwnerBaseBusRules implements BusinessRulesIFace
{
    private static final Logger  log   = Logger.getLogger(HUHFragmentBusRules.class);

    public HUHFragmentBusRules()
    {
        super();
    }

    /**
     * Checking a determination as current automatically sets all other current
     * determinations as non current.
     * 
     * @author lchan
     * @param viewableArg
     */
    @Override
    public void initialize(Viewable viewableArg) {
        super.initialize(viewableArg);

        if (viewable instanceof FormViewObj) {
            formViewObj = (FormViewObj) viewable;
        }

        // Finds the determination form view object among the fragment's
        // children. Adds a listener on the determination's isCurrent checkbox.
        // If the checkbox is selected, sets the isCurrent field of the
        // other data model objects to false.
        for (MultiView fragmentKids : formViewObj.getKids()) {
            if (fragmentKids.getCurrentViewAsFormViewObj().getCellName()
                    .equals("determinations")) {
                final FormViewObj determinationsFvo = fragmentKids
                        .getCurrentViewAsFormViewObj();
                final JCheckBox isCurrentCb = (JCheckBox) determinationsFvo
                        .getControlByName("isCurrent");
                isCurrentCb.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (isCurrentCb.isSelected()) {
                            @SuppressWarnings("unchecked")
                            List<Determination> determinations = (List<Determination>) determinationsFvo
                                    .getDataList();
                            for (Determination determination : determinations) {
                                if (determinationsFvo.getDataObj() != determination) {
                                    determination.setIsCurrent(false);
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    @Override
    public boolean isOkToAssociateSearchObject(Object newParentDataObj, Object dataObjectFromSearch)
    {
        reasonList.clear();

        if (dataObjectFromSearch instanceof Fragment)
        {
            Fragment fragment = (Fragment) dataObjectFromSearch;

            if (newParentDataObj instanceof Preparation)
            {
                Preparation preparation = (Preparation) newParentDataObj;

                if (fragment.getPreparation() != null &&
                        !fragment.getPreparation().equals(preparation))
                {
                    reasonList.add(getErrorMsg("FragmentBusRules.ITEM_HAS_PARENT", Fragment.class, "preparation", null));
                    return false;
                }
                else return true;
            }

            if (newParentDataObj instanceof CollectionObject)
            {
                CollectionObject collObj = (CollectionObject) newParentDataObj;

                if (fragment.getCollectionObject() != null &&
                        !fragment.getCollectionObject().equals(collObj))
                {
                    reasonList.add(getErrorMsg("FragmentBusRules.ITEM_HAS_PARENT", Fragment.class, "collectionObject", null));
                    return false;
                }
                else return true;
            }
            
            if (newParentDataObj instanceof CollectionRelationship)
            {
                CollectionRelationship rel = (CollectionRelationship) newParentDataObj;
                
                if (rel.getLeftSide().equals(rel.getRightSide()))
                {
                    reasonList.add(getResourceString("FragmentBusRules.REFLEXIVE_RELATION"));
                    return false;
                }
            }
        }
        return true;
    }
    
    @Override
    public boolean okToEnableDelete(final Object dataObj)
    {
        boolean ok = super.okToEnableDelete(dataObj);
        
        if (!ok) return false;
        
        if (dataObj instanceof Fragment)
        {
            Fragment fragment = (Fragment) dataObj;
          
            String fId = String.valueOf(fragment.getId());
            Integer colRelId = null;
            
            // Items must be removed from collection objects and preps first.
            // Otherwise, we may leave collection objects or preps that can't be found by barcode.
            if (fragment.getCollectionObject() != null) return false;
            if (fragment.getPreparation() != null) return false;
            
            // In our collectionrelationship form, the user can associate a fragment
            // with the current object.  The associated fragment becomes the right side,
            // and the current becomes the left.  f.getRightSideRels() returns
            // CollectionRelationship objects for which fragment f is on the right side.            
            int rCount = getCountSql(CollectionRelationship.class, "collectionRelationshipId", "rightSide", fId, colRelId);
            if (rCount > 0) return false;
            
            int lCount = getCountSql(CollectionRelationship.class, "collectionRelationshipId", "leftSide", fId, colRelId);
            if (lCount > 0) return false;
        }
        
        return true;
    }
    
    /** Either the Fragment or its Preparation must have a barcode, and the barcode
     *  must be unique across the union of all Fragment and Preparation objects.
     *  
     * Return error message string if there was a problem with duplicate/missing barcode.  Return null if no
     * errors found.
     * 
     * @param dataObj the Fragment to check
     * @return whether the Fragment or its Preparation has a unique barcode
     */    
    static String checkForBarcodeError(final Fragment fragment)
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
        if (StringUtils.isEmpty(fBarcode))
        {
            if (HasOtherBarcode(prep))
            {
                return null;
            }
            else
            {
                return getErrorMsg("GENERIC_FIELD_MISSING", Fragment.class, fBarcodeFieldName, "");
            }
        }
        
        // If the fragment has a barcode, it must not already exist in other fragment records
        // or other preparation records in the database
        
        // count saved fragment records with this barcode
        int fCount = getCountSql(Fragment.class, "fragmentId", fBarcodeFieldName, fBarcode, fId);

        if (fCount > 0)
        {
            return getErrorMsg("GENERIC_FIELD_IN_USE", Fragment.class, fBarcodeFieldName, fBarcode);
        }

        // count saved preparation records with this barcode
        int pCount = getCountSql(Preparation.class, "preparationId", pBarcodeFieldName, fBarcode, pId);

        if (pCount > 0)
        {
            return getErrorMsg("GENERIC_FIELD_IN_USE", Preparation.class, pBarcodeFieldName, fBarcode);
        }
            
        // check that the possibly unsaved prep doesn't have the same barcode
        if (fBarcode.equals(pBarcode))
        {
            return getErrorMsg("GENERIC_FIELD_IN_USE", Fragment.class, fBarcodeFieldName, fBarcode);
        }
        
        return null;
    }

    /**
     * Either the Fragment or its Preparation must have a barcode, and the barcode
     * must be unique across the union of all Fragment and Preparation objects.
     *  
     * Return error message string if there was a problem with duplicate/missing barcode.  Return null if no
     * errors found.
     * 
     * @param prep
     * @return
     */
    static String checkForBarcodeError(final Preparation prep)
    {
        String fBarcodeFieldName = "identifier";
        Integer fId = null;
        
        String pBarcodeFieldName = fBarcodeFieldName;
        String pBarcode = prep.getIdentifier();
        Integer pId = prep.getId();
        
        // There must be a barcode associated with at least one of the fragments on this preparation
        if (StringUtils.isEmpty(pBarcode))
        {
            if (HasOtherBarcode(prep))
            {
                return null;
            }
            else
            {
                return getErrorMsg("GENERIC_FIELD_MISSING", Preparation.class, pBarcodeFieldName, "");
            }
        }
 
        // If the prep has a barcode, it must not already exist in other fragment records
        // or other preparation records in the database
        
        // count saved fragment records
        int fCount = getCountSql(Fragment.class, "fragmentId", fBarcodeFieldName, pBarcode, fId);

        if (fCount > 0)
        {
            return getErrorMsg("GENERIC_FIELD_IN_USE", Preparation.class, pBarcodeFieldName, pBarcode);
        }

        // count saved preparation records with this barcode
        int pCount = getCountSql(Preparation.class, "preparationId", pBarcodeFieldName, pBarcode, pId);

        if (pCount > 0)
        {
            return getErrorMsg("GENERIC_FIELD_IN_USE", Preparation.class, pBarcodeFieldName, pBarcode);
        }
        
        return null;
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

    /**
     * Lookup values for table name and field names from parameters, and then execute
     * 
     * SELECT COUNT(primaryFieldName) FROM dataClass WHERE fieldName = fieldValue AND primaryFieldName NOT IN (id)
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
        String quote = fieldInfo != null ? (fieldInfo.getDataClass() == String.class || fieldInfo.getDataClass() == Date.class ? "'" : "") : "";
        
        // compose sql: select count(primary_key) from table_name where field_name = field_value
        String tableName = tableInfo.getName();
        String compareColumName = fieldInfo != null ? fieldInfo.getColumn() : tableInfo.getRelationshipByName(fieldName).getColName();
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
    
    /**
     * If the fragment has determinations, exactly one ought be marked current, and
     * no more than one is allowed to be marked "is filed under"
     * 
     * @param fragment
     * @return String warning message if problem found, otherwise null
     */
    protected static String checkForDeterminationWarning(final Fragment fragment)
    {
        if (fragment.getDeterminations().size() > 0)
        {
            int currentDets = 0;
            int filedUnderDets = 0;
            for (Determination det : fragment.getDeterminations())
            {
                if (det.isCurrentDet())
                {
                    currentDets++;
                }
                
                if (det.getYesNo3() != null && det.getYesNo3())
                {
                    filedUnderDets++;
                }
            }

            if (currentDets < 1)
            {
                return getResourceString("CollectionObjectBusRules.CURRENT_DET_REQUIRED");
            }
            else if (currentDets > 1)
            {
                return getResourceString("CollectionObjectBusRules.ONLY_ONE_CURRENT_DET");
            }
            else if (filedUnderDets > 1)
            {
                return getResourceString("CollectionObjectBusRules.ONLY_ONE_FILED_DET");
            }
        }

        return null;
    }

    @Override
	public STATUS processBusinessRules(Object dataObj)
	{
		STATUS status = super.processBusinessRules(dataObj);
		
		if (!STATUS.OK.equals(status)) return status;
		
		if (dataObj instanceof Fragment)
        {
            Fragment fragment = (Fragment) dataObj;
            
            String barcodeError = checkForBarcodeError(fragment);
            
            if (barcodeError != null)
            {
                reasonList.add(barcodeError);
                status = STATUS.Error;
            }
            
            if (!STATUS.OK.equals(status)) return status;
            
            String determinationWarning = checkForDeterminationWarning(fragment);
            
            if (determinationWarning != null)
            {
                reasonList.add(determinationWarning);
                status = STATUS.Warning;
            }
        }
        return status;
	}
}
