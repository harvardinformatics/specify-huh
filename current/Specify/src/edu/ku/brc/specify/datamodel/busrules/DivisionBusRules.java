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

import javax.swing.SwingUtilities;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.ui.forms.BaseBusRules;
import edu.ku.brc.af.ui.forms.BusinessRulesOkDeleteIFace;
import edu.ku.brc.af.ui.forms.FormDataObjIFace;
import edu.ku.brc.af.ui.forms.ResultSetController;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.Division;
import edu.ku.brc.specify.datamodel.Institution;
import edu.ku.brc.specify.dbsupport.SpecifyDeleteHelper;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author rod
 *
 * @code_status Alpha
 *
 * Oct 1, 2008
 *
 */
public class DivisionBusRules extends BaseBusRules
{

    /**
     * @param dataClasses
     */
    public DivisionBusRules()
    {
        super(Division.class);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#addChildrenToNewDataObjects(java.lang.Object)
     */
    @Override
    public void addChildrenToNewDataObjects(Object newDataObj)
    {
        super.addChildrenToNewDataObjects(newDataObj);
        
        Institution institution = AppContextMgr.getInstance().getClassObject(Institution.class);
        ((Division)newDataObj).setInstitution(institution);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#initialize(edu.ku.brc.af.ui.forms.Viewable)
     */
    @Override
    public void initialize(Viewable viewableArg)
    {
        super.initialize(viewableArg);
        
        if (formViewObj != null && formViewObj.getMVParent().isTopLevel())
        {
            ResultSetController rsc = formViewObj.getRsController();
            if (rsc != null)
            {
                if (rsc.getNewRecBtn() != null) rsc.getNewRecBtn().setVisible(false);
                if (rsc.getDelRecBtn() != null) rsc.getDelRecBtn().setVisible(false);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#afterFillForm(java.lang.Object)
     */
    @Override
    public void afterFillForm(Object dataObj)
    {
        super.afterFillForm(dataObj);
    }


    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#okToEnableDelete(java.lang.Object)
     */
    @Override
    public boolean okToEnableDelete(Object dataObj)
    {
        return true;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#processBusinessRules(java.lang.Object)
     */
    @Override
    public STATUS processBusinessRules(final Object dataObj)
    {
        reasonList.clear();
        
        if (!(dataObj instanceof Division))
        {
            return STATUS.Error;
        }
        
        STATUS nameStatus = isCheckDuplicateNumberOK("name", 
                                                      (FormDataObjIFace)dataObj, 
                                                      Division.class, 
                                                      "userGroupScopeId");
        
        return nameStatus != STATUS.OK ? STATUS.Error : STATUS.OK;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.busrules.BaseBusRules#okToDelete(java.lang.Object, edu.ku.brc.dbsupport.DataProviderSessionIFace, edu.ku.brc.ui.forms.BusinessRulesOkDeleteIFace)
     */
    @Override
    public void okToDelete(final Object                     dataObj,
                           final DataProviderSessionIFace   session,
                           final BusinessRulesOkDeleteIFace deletable)
    {
        reasonList.clear();
        
        if (deletable != null)
        {
            Division division = (Division)dataObj;
            
            Integer id = division.getId();
            if (id != null)
            {
                Division currDivision = AppContextMgr.getInstance().getClassObject(Division.class);
                if (currDivision.getId().equals(division.getId()))
                {
                    UIRegistry.showError("You cannot delete the current Division."); // I18N
                    
                } else
                {
                    String sql = "SELECT count(*) FROM agent a WHERE a.DivisionID = " + division.getId();
                    int count = BasicSQLUtils.getCount(sql);
                    if (count > 0)
                    {
                        UIRegistry.showError(String.format("There are too many agents associated with this the `%s` Division.", division.getName())); // I18N
                    } else
                    {
                        try
                        {
                            SpecifyDeleteHelper delHelper = new SpecifyDeleteHelper(true);
                            delHelper.delRecordFromTable(Division.class, division.getId(), true);
                            delHelper.done();
                            
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run()
                                {
                                    // This is called instead of calling 'okToDelete' because we had the SpecifyDeleteHelper
                                    // delete the actual dataObj and now we tell the form to remove the dataObj from
                                    // the form's list and them update the controller appropriately
                                    
                                    formViewObj.updateAfterRemove(true); // true removes item from list and/or set
                                }
                            });
                            
                        } catch (Exception ex)
                        {
                            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(DivisionBusRules.class, ex);
                            ex.printStackTrace();
                        }
                    }
                }
            } else
            {
                super.okToDelete(dataObj, session, deletable);
            }
            
        } else
        {
            super.okToDelete(dataObj, session, deletable);
        }
    }

    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#beforeDelete(java.lang.Object, edu.ku.brc.dbsupport.DataProviderSessionIFace)
     */
    @Override
    public void beforeDelete(Object dataObj, DataProviderSessionIFace session)
    {
        super.beforeDelete(dataObj, session);
        
        /*Division division = (Division)dataObj;
        
        Statement stmt = null;
        try
        {
            if (true)
            {
                stmt = DBConnection.getInstance().getConnection().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
                
                List<Integer> disciplineIds = new Vector<Integer>();
                ResultSet rs = stmt.executeQuery("SELECT DisciplineID FROM discipline WHERE DivisionID = "+division.getId());
                while (rs.next())
                {
                    disciplineIds.add(rs.getInt(1));
                }         
                rs.close();
                
                Hashtable<Integer, Boolean> permitHash = new Hashtable<Integer, Boolean>();
                
                String sql = "SELECT permit.PermitID FROM division INNER JOIN accession ON division.UserGroupScopeId = accession.DivisionID "+
                             "INNER JOIN accessionauthorization ON accession.AccessionID = accessionauthorization.AccessionID "+
                             "INNER JOIN permit ON accessionauthorization.PermitID = permit.PermitID WHERE division.UserGroupScopeId = "+division.getId();
                rs = stmt.executeQuery(sql);
                while (rs.next())
                {
                    int id = rs.getInt(1);
                    permitHash.put(id, true);
                    Permit obj = session.get(Permit.class, id);
                    System.err.println(obj.getIdentityTitle());
                    session.delete(obj);
                }         
                rs.close();
                
                sql = "SELECT permit.PermitID FROM division INNER JOIN repositoryagreement ON division.UserGroupScopeId = repositoryagreement.DivisionID "+
                      "INNER JOIN accessionauthorization ON repositoryagreement.RepositoryAgreementID = accessionauthorization.AccessionID "+
                      "INNER JOIN permit ON accessionauthorization.PermitID = permit.PermitID WHERE division.UserGroupScopeId = "+division.getId();
                rs = stmt.executeQuery(sql);
                while (rs.next())
                {
                    int id = rs.getInt(1);
                    if (permitHash.get(id) == null)
                    {
                        Permit obj = session.get(Permit.class, id);
                        System.err.println(obj.getIdentityTitle());
                        session.delete(obj);
                        permitHash.put(id, true);
                    }
                }         
                rs.close();
                
                rs = stmt.executeQuery("SELECT AccessionID FROM accession WHERE DivisionID = "+division.getId());
                while (rs.next())
                {
                    Accession obj = session.get(Accession.class, rs.getInt(1));
                    System.err.println(obj.getIdentityTitle());
                    session.delete(obj);
                }         
                rs.close();
                
                rs = stmt.executeQuery("SELECT RepositoryAgreementID FROM repositoryagreement WHERE DivisionID = "+division.getId());
                while (rs.next())
                {
                    RepositoryAgreement obj = session.get(RepositoryAgreement.class, rs.getInt(1));
                    System.err.println(obj.getIdentityTitle());
                    session.delete(obj);
                }         
                rs.close();
                
                rs = stmt.executeQuery("SELECT LoanID FROM loan WHERE DivisionID = "+division.getId());
                while (rs.next())
                {
                    Loan obj = session.get(Loan.class, rs.getInt(1));
                    System.err.println(obj.getIdentityTitle());
                    session.delete(obj);
                }         
                rs.close();
                
                rs = stmt.executeQuery("SELECT GiftID FROM gift WHERE DivisionID = "+division.getId());
                while (rs.next())
                {
                    Gift obj = session.get(Gift.class, rs.getInt(1));
                    System.err.println(obj.getIdentityTitle());
                    session.delete(obj);
                }         
                rs.close();
                
                rs = stmt.executeQuery("SELECT ConservDescriptionID FROM conservdescription WHERE DivisionID = "+division.getId());
                while (rs.next())
                {
                    ConservDescription obj = session.get(ConservDescription.class, rs.getInt(1));
                    System.err.println(obj.getIdentityTitle());
                    session.delete(obj);
                }         
                rs.close();
                
                rs = stmt.executeQuery("SELECT TreatmentEventID FROM treatmentevent WHERE DivisionID = "+division.getId());
                while (rs.next())
                {
                    TreatmentEvent obj = session.get(TreatmentEvent.class, rs.getInt(1));
                    System.err.println(obj.getIdentityTitle());
                    session.delete(obj);
                }         
                rs.close();
                
                for (Integer dspId : disciplineIds)
                {
                    List<Integer> ids = new Vector<Integer>();
                    rs = stmt.executeQuery("SELECT SpAppResourceDirID FROM spappresourcedir WHERE DisciplineID = "+dspId);
                    while (rs.next())
                    {
                        ids.add(rs.getInt(1));
                    }         
                    rs.close();
                    
                    if (ids.size() > 0)
                    {
                        for (Integer id : ids)
                        {
                            SpAppResourceDir obj = session.get(SpAppResourceDir.class, id);
                            System.err.println(obj.getIdentityTitle());
                            session.delete(obj);
                        }
                    }
                    
                    ids = new Vector<Integer>();
                    rs = stmt.executeQuery("SELECT CollectingTripID FROM collectingtrip WHERE DisciplineID = "+dspId);
                    while (rs.next())
                    {
                        ids.add(rs.getInt(1));
                    }         
                    rs.close();
                    
                    if (ids.size() > 0)
                    {
                        for (Integer id : ids)
                        {
                            CollectingTrip obj = session.get(CollectingTrip.class, id);
                            System.err.println(obj.getIdentityTitle());
                            session.delete(obj);
                        }
                    }
                    
                    ids = new Vector<Integer>();
                    rs = stmt.executeQuery("SELECT CollectionID FROM collection WHERE DisciplineID = "+dspId);
                    while (rs.next())
                    {
                        ids.add(rs.getInt(1));
                    }         
                    rs.close();
                    
                    for (Integer id : ids)
                    {
                        rs  = stmt.executeQuery("select CollectionObjectID from collectionobject where CollectionMemberID = "+id);
                        while (rs.next())
                        {
                            CollectionObject co = session.get(CollectionObject.class, rs.getInt(1));
                            session.delete(co);
                        }
                        rs.close();
                    }
                }
                
                stmt.close();

            } else
            {
                stmt = DBConnection.getInstance().getConnection().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
                
                List<Integer> disciplineIds = new Vector<Integer>();
                ResultSet rs = stmt.executeQuery("SELECT DisciplineID FROM discipline WHERE DivisionID = "+division.getId());
                while (rs.next())
                {
                    disciplineIds.add(rs.getInt(1));
                }         
                rs.close();
                
                for (Integer dspId : disciplineIds)
                {
                    List<Integer> collectionIds = new Vector<Integer>();
                    rs = stmt.executeQuery("SELECT CollectionID FROM collection WHERE DisciplineID = "+dspId);
                    while (rs.next())
                    {
                        collectionIds.add(rs.getInt(1));
                    }         
                    rs.close();
                    
                    for (Integer colId : collectionIds)
                    {
                        session.deleteHQL("DELETE FROM CollectionObject WHERE collectionMemberId = "+colId);
                    }
                }
                
                stmt.close();

            }
            
        } catch (Exception ex)
        {
            edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
            edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(DivisionBusRules.class, ex);
            ex.printStackTrace();
            //log.error(ex);
            throw new RuntimeException(ex);
            
        } finally
        {
            try
            {
                stmt.close();
                
            } catch (Exception ex)
            {
                edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(DivisionBusRules.class, ex);
                ex.printStackTrace();
                //log.error(ex);
                throw new RuntimeException(ex);
            }
        }*/

    }

}
