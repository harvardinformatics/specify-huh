package edu.harvard.huh.specify.datamodel.busrules;

import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.util.List;

import edu.ku.brc.af.ui.forms.BusinessRulesIFace;
import edu.ku.brc.af.ui.forms.BusinessRulesOkDeleteIFace;
import edu.ku.brc.af.ui.forms.DraggableRecordIdentifier;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.af.ui.forms.BaseBusRules;

public class HUHFragmentBusRules extends BaseBusRules implements
		BusinessRulesIFace {

	@Override
	public void aboutToShutdown() {
		// TODO Auto-generated method stub
		super.aboutToShutdown();
	}

	@Override
	public void addChildrenToNewDataObjects(Object newDataObj) {
		// TODO Auto-generated method stub
		super.addChildrenToNewDataObjects(newDataObj);
	}

	@Override
	public void afterDeleteCommit(Object dataObj) {
		// TODO Auto-generated method stub
		super.afterDeleteCommit(dataObj);
	}

	@Override
	public void afterFillForm(Object dataObj) {
		// TODO Auto-generated method stub
		super.afterFillForm(dataObj);
	}

	@Override
	public boolean afterSaveCommit(Object dataObj,
			DataProviderSessionIFace session) {
		// TODO Auto-generated method stub
		return super.afterSaveCommit(dataObj, session);
	}

	@Override
	public void afterSaveFailure(Object dataObj,
			DataProviderSessionIFace session) {
		// TODO Auto-generated method stub
		super.afterSaveFailure(dataObj, session);
	}

	@Override
	public void beforeDelete(Object dataObj, DataProviderSessionIFace session) {
		// TODO Auto-generated method stub
		super.beforeDelete(dataObj, session);
	}

	@Override
	public boolean beforeDeleteCommit(Object dataObj,
			DataProviderSessionIFace session) throws Exception {
		// TODO Auto-generated method stub
		return super.beforeDeleteCommit(dataObj, session);
	}

	@Override
	public void beforeFormFill() {
		// TODO Auto-generated method stub
		super.beforeFormFill();
	}

	@Override
	public void beforeMerge(Object dataObj, DataProviderSessionIFace session) {
		// TODO Auto-generated method stub
		super.beforeMerge(dataObj, session);
	}

	@Override
	public void beforeSave(Object dataObj, DataProviderSessionIFace session) {
		// TODO Auto-generated method stub
		super.beforeSave(dataObj, session);
	}

	@Override
	public boolean beforeSaveCommit(Object dataObj,
			DataProviderSessionIFace session) throws Exception {
		// TODO Auto-generated method stub
		return super.beforeSaveCommit(dataObj, session);
	}

	@Override
	public boolean canCreateNewDataObject() {
		// TODO Auto-generated method stub
		return super.canCreateNewDataObject();
	}

	@Override
	public void createNewObj(boolean doSetIntoAndValidateArg, Object oldDataObj) {
		// TODO Auto-generated method stub
		super.createNewObj(doSetIntoAndValidateArg, oldDataObj);
	}

	@Override
	public boolean doesSearchObjectRequireNewParent() {
		// TODO Auto-generated method stub
		return super.doesSearchObjectRequireNewParent();
	}

	@Override
	public void formShutdown() {
		// TODO Auto-generated method stub
		super.formShutdown();
	}

	@Override
	public String getDeleteMsg(Object dataObj) {
		// TODO Auto-generated method stub
		return super.getDeleteMsg(dataObj);
	}

	@Override
	public String getMessagesAsString() {
		// TODO Auto-generated method stub
		return super.getMessagesAsString();
	}

	@Override
	public List<String> getWarningsAndErrors() {
		// TODO Auto-generated method stub
		return super.getWarningsAndErrors();
	}

	@Override
	public void initialize(Viewable viewable) {
		// TODO Auto-generated method stub
		super.initialize(viewable);
	}

	@Override
	public boolean isOkToAssociateSearchObject(Object newParentDataObj,
			Object dataObjectFromSearch) {
		// TODO Auto-generated method stub
		return super.isOkToAssociateSearchObject(newParentDataObj, dataObjectFromSearch);
	}

	@Override
	public boolean isOkToSave(Object dataObj, DataProviderSessionIFace session) {
		// TODO Auto-generated method stub
		return super.isOkToSave(dataObj, session);
	}

	@Override
	public void okToDelete(Object dataObj, DataProviderSessionIFace session,
			BusinessRulesOkDeleteIFace deletable) {
		// TODO Auto-generated method stub
		super.okToDelete(dataObj, session, deletable);
	}

	@Override
	public boolean okToEnableDelete(Object dataObj) {
		// TODO Auto-generated method stub
		return super.okToEnableDelete(dataObj);
	}

	@Override
	public STATUS processBusinessRules(Object dataObj) {
		// TODO Auto-generated method stub
		return super.processBusinessRules(dataObj);
	}

	@Override
	public STATUS processBusinessRules(Object parentDataObj, Object dataObj,
			boolean isExistingObject) {
		// TODO Auto-generated method stub
		return super.processBusinessRules(parentDataObj, dataObj, isExistingObject);
	}

	@Override
	public Object processSearchObject(Object newParentDataObj,
			Object dataObjectFromSearch) {
		// TODO Auto-generated method stub
		return super.processSearchObject(newParentDataObj, dataObjectFromSearch);
	}

	@Override
	public void setObjectIdentity(Object dataObj,
			DraggableRecordIdentifier draggableIcon) {
		// TODO Auto-generated method stub
		super.setObjectIdentity(dataObj, draggableIcon);
	}

	@Override
	public boolean shouldCloneField(String fieldName) {
		// TODO Auto-generated method stub
		return super.shouldCloneField(fieldName);
	}

	@Override
	public boolean shouldCreateSubViewData(String fieldName) {
		// TODO Auto-generated method stub
		return super.shouldCreateSubViewData(fieldName);
	}
}