/*
 * Created on 2012 January 17
 *
 * Copyright © 2011 President and Fellows of Harvard College
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * @Author: David B. Lowery  lowery@cs.umb.edu
 */

package edu.harvard.huh.specify.util;

import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.LoanPreparation;
import edu.ku.brc.specify.datamodel.LoanReturnPreparation;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;

/**
 * This utility class can be used for common operations performed on preparations
 * used in transactions
 * 
 * @author lowery
 *
 */
public class LoanPrepUtil {
	private LoanPreparation loanPreparation;
	private Preparation prep;
	
	/** Constructor takes a LoanPrepartion as an argument and
	 * initializes the instance variables.
	 * 
	 * @param loanPreparation
	 */
	public LoanPrepUtil(LoanPreparation loanPreparation) {
			this.loanPreparation = loanPreparation;
			prep = loanPreparation.getPreparation();
	}
	
	/** Obtain the item count for this preparation based on the PrepType. Lots use
	 * the value stored in the itemCount field in LoanPreparation. Non-Lots will 
	 * calculate this value based on the Fragment count.
	 * 
	 * @return itemCount
	 */
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

	/** Obtain the type count for this preparation based on the PrepType. Lots use
	 * the value stored in the typeCount field in LoanPreparation. Non-Lots will 
	 * calculate this value based on the Fragment count (which are types).
	 * 
	 * @return typeCount
	 */
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
	
	/**
	 * Non-specimen counts are always obtained from the LoanPreparation
	 * nonSpecimenCount field.
	 * 
	 * @return nonSpecimenCount
	 */
	public int getNonSpecimenCount() {
		return loanPreparation.getNonSpecimenCount() != null ? loanPreparation.getNonSpecimenCount() : 0;
	}
	
	/**
	 * Calculates the quantity of items returned based on the itemCount found in the
	 * current LoanPreparation's collection of LoanReturnPreparation objects.
	 *  
	 * @return itemReturned
	 */
	public int getItemReturned() {
		int itemReturned = 0;
		for (LoanReturnPreparation lrp : loanPreparation.getLoanReturnPreparations()) {
			itemReturned += lrp.getItemCount() != null ? lrp.getItemCount() : 0;
		}
		return itemReturned;
	}
	
	/**
	 * Calculates the quantity of types returned based on the typeCount found in the
	 * current LoanPreparation's collection of LoanReturnPreparation objects.
	 *  
	 * @return typeReturned
	 */
	public int getTypeReturned() {
		int typeReturned = 0;
		for (LoanReturnPreparation lrp : loanPreparation.getLoanReturnPreparations()) {
			typeReturned += lrp.getTypeCount() != null ? lrp.getTypeCount() : 0;
		}
		return typeReturned;
	}
	
	/**
	 * Calculates the quantity of  returned based on the nonSpecimenCount found in the
	 * current LoanPreparation's collection of LoanReturnPreparation objects.
	 *  
	 * @return nonSpecimenReturned
	 */
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
			return isLot(prep);
		}
		return null; // Either preparation or preptype is null
	}
	
	/**
	 * A static helper method to be used when determining whether or not any
	 * given preparation is of type "lot".
	 * 
	 * @param prep
	 * @return Boolean
	 */
	public static Boolean isLot(Preparation prep) {
		if (prep.getPrepType() != null && prep.getPrepType().getName() != null) {
			return prep.getPrepType().getName().equals("Lot");
		}
		return null;
	}
	
	/** Static helper method for determining if any given fragment is a type. Returns
	 * true if any of the determinations contain a value for typeStatusName
	 * 
	 * @param f
	 * @return boolean
	 */
	public static boolean isType(Fragment f) {
		if (f.getDeterminations() != null) {
			for (Determination d : f.getDeterminations()) {
				if (d.getTypeStatusName() != null) {
					return true;
				}
			}
		}
		return false;
	}

	/** Using the Preparation from the current LoanPreparation object (set in the constructor),
	 * find the full name and return it.
	 * 
	 * @return String
	 */
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
