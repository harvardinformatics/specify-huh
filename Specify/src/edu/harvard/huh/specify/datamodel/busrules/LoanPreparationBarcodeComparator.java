package edu.harvard.huh.specify.datamodel.busrules;

import edu.ku.brc.specify.datamodel.Fragment;
import edu.ku.brc.specify.datamodel.LoanPreparation;

import java.util.Comparator;

/**
 * Compares two loan preparations by their barcodes. Order is based on Java's
 * string comparison method. On a preparation with multiple barcoded fragments,
 * uses the last one.
 * 
 * @author lchan
 */
public class LoanPreparationBarcodeComparator implements
		Comparator<LoanPreparation> {

	private String getIdentifier(LoanPreparation loanPreparation) {
		String identifier = null;
		for (Fragment frag : loanPreparation.getPreparation().getFragments()) {
			identifier = frag.getIdentifier();
		}
		
		// Some fragments have null identifiers.  Whhhy? :(
		if (identifier == null) {
			return "";
		} else {
			return identifier;
		}
	}

	@Override
	public int compare(LoanPreparation arg0, LoanPreparation arg1) {
		String arg0Id = getIdentifier(arg0);
		String arg1Id = getIdentifier(arg1);

		return arg0Id.compareTo(arg1Id);
	}

}
