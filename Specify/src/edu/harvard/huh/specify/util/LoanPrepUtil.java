package edu.harvard.huh.specify.util;

import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.LoanReturnPreparation;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;

public class LoanPrepUtil {
	private LoanPreparation loanPreparation;
	private Preparation prep;
	
	public LoanPrepUtil(LoanPreparation loanPreparation) {
			this.loanPreparation = loanPreparation;
			prep = loanPreparation.getPreparation();
	}
	
	public int getItemCount() {
		int itemCount = 0;
		if (isLot() != null && !isLot()) {
			for (Fragment f : prep.getFragments()) {
				if (!isType(f)) {
					itemCount++;
				}
			}
		} else {
			itemCount = loanPreparation.getItemCount() != null ? loanPreparation.getItemCount() : 0;
		}
		return itemCount;
	}

	public int getTypeCount() {
		int typeCount = 0;
		if (isLot() != null && !isLot()) {
			for (Fragment f : prep.getFragments()) {
				if (isType(f)) {
					typeCount++;
				}
			}
		} else {
			typeCount = loanPreparation.getTypeCount() != null ? loanPreparation.getTypeCount() : 0;
		}
		return typeCount;
	}
	
	public int getNonSpecimenCount() {
		return loanPreparation.getNonSpecimenCount() != null ? loanPreparation.getNonSpecimenCount() : 0;
	}
	
	public int getItemReturned() {
		int itemReturned = 0;
		for (LoanReturnPreparation lrp : loanPreparation.getLoanReturnPreparations()) {
			itemReturned += lrp.getItemCount() != null ? lrp.getItemCount() : 0;
		}
		return itemReturned;
	}
	
	public int getTypeReturned() {
		int typeReturned = 0;
		for (LoanReturnPreparation lrp : loanPreparation.getLoanReturnPreparations()) {
			typeReturned += lrp.getTypeCount() != null ? lrp.getTypeCount() : 0;
		}
		return typeReturned;
	}
	
	public int getNonSpecimenReturned() {
		int nonSpecimenReturned = 0;
		for (LoanReturnPreparation lrp : loanPreparation.getLoanReturnPreparations()) {
			nonSpecimenReturned += lrp.getNonSpecimenCount() != null ? lrp.getNonSpecimenCount() : 0;
		}
		return nonSpecimenReturned;
	}
	
	/**
	 * Will check whether or not the preparation associated with the LoanPreparation is a
	 * lot. This method will return null if the LoanPreparation holds a null reference to
	 * preparation or if the Preparation holds a null reference to PrepType.
	 * 
	 * @return Boolean
	 */
	public Boolean isLot() {
		if (loanPreparation.getPreparation() != null) {
			Preparation prep = loanPreparation.getPreparation();
			if (prep.getPrepType() != null && prep.getPrepType().getName() != null) {
				return prep.getPrepType().getName().equals("Lot");
			}
		}
		return null; // Either preparation or preptype is null
	}
	
	private boolean isType(Fragment f) {
		if (f.getDeterminations() != null) {
			for (Determination d : f.getDeterminations()) {
				if (d.getTypeStatusName() != null) {
					return true;
				}
			}
		}
		return false;
	}

	public String getTaxonName() {
		if (prep != null) {
	        Determination det = null;
	
			for (Fragment f : prep.getFragments())
			{
			    for (Determination d : f.getDeterminations())
			    {
			        if (d.getTypeStatusName() != null)
			        {
			            det = d;
			            break;
			        }
			        else if (d.isCurrentDet())
			        {
			            det = d;
			        }
			    }
			}
	
			if (det != null)
			{
			    Taxon t = det.getTaxon();
			    return t.getFullName();
			}
		}
		return null;
	}
}
