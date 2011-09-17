package edu.harvard.huh.specify.dbsupport;

// mmk 8/29/2011

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.datamodel.CollObjBarcode;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.Collector;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.PrepBarcode;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;

public class CollObjInfo {
	
	private String collectorName = null;
	private String barcode = null;
	
	private List<String> currentDets = new ArrayList<String>();
	private List<String> higherTaxa = new ArrayList<String>();
	private List<String> typeNames = new ArrayList<String>();
	private List<String> typeStatuses = new ArrayList<String>();
	
	public CollObjInfo(CollectionObject collObj) {
		
		Set<String> currentDets = new HashSet<String>();
		Set<String> higherTaxa = new HashSet<String>();
		Set<String> typeNames = new HashSet<String>();
		Set<String> typeStatuses = new HashSet<String>();
		
		this.barcode = null;

		for ( Collector c : collObj.getCollectingEvent().getCollectors()) {
			if (c.getIsPrimary()) {
				this.collectorName = c.getAgent().getCollectorName();
				break;
			}
	    }
		addNames(collObj, currentDets, higherTaxa, typeNames, typeStatuses);
		
		this.higherTaxa.addAll(higherTaxa);
		Collections.sort(this.higherTaxa);
		
		this.currentDets.addAll(currentDets);
		Collections.sort(this.currentDets);
		
		this.typeNames.addAll(typeNames);
		Collections.sort(this.typeNames);
		
		this.typeStatuses.addAll(typeStatuses);
		Collections.sort(this.typeStatuses);
	}

	public static List<CollObjInfo> GetCollObjInfo(Preparation preparation) {

		List<CollObjInfo> list = new ArrayList<CollObjInfo>();
		
		// First add the collection object directly referenced by relation, if there is one.
		CollectionObject attachedCollObj = preparation.getCollectionObject();
		list.add(new CollObjInfo(attachedCollObj));

		// Then add the collection objects that share a barcode with this preparation, by barcode
		DataProviderSessionIFace session = null;
		try {
			session = DataProviderFactory.getInstance().createSession();

			for (PrepBarcode prepBarcode : preparation.getBarcodes()) {

				for (CollObjBarcode collObjBarcode : session.getDataList(CollObjBarcode.class, "Barcode", prepBarcode)) {

					for (CollectionObject collObj : collObjBarcode.getCollectionObjects()) {
						CollObjInfo collObjInfo = new CollObjInfo(collObj);
						collObjInfo.setBarcode(collObjBarcode.getBarcode());
						list.add(collObjInfo);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		finally {
			if (session != null) {
				session.close();
			}
		}
		
		Collections.sort(list, new Comparator<CollObjInfo>() {

			@Override
			public int compare(CollObjInfo o1, CollObjInfo o2) {
				String s1 = o1.getBarcode() != null ? o1.getBarcode() :
								o1.getCollectorName() != null ? o1.getCollectorName() : "z";
								
				String s2 = o2.getBarcode() != null ? o2.getBarcode() :
					o2.getCollectorName() != null ? o2.getCollectorName() : "z";

					return s1.compareTo(s2);
			}
			
		});
		return list;
	}
	
	public String getBarcode() {
		return this.barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}
	
	public String getCollectorName() {
		return this.collectorName;
	}

	public List<String> getCurrentDets() {
		return this.currentDets;
	}
	
	public List<String> getHigherTaxa() {
		return this.higherTaxa;
	}

	public List<String> getTypeNames() {
		return this.typeNames;
	}
	
	public List<String> getTypeStatuses() {
		return this.typeStatuses;
	}

	public boolean isType() {
		return typeNames.size() > 0;
	}
	
	private void addNames(CollectionObject collObj, Set<String> currentDets, Set<String> higherTaxa, Set<String> typeNames, Set<String> typeStatuses) {

		for (Determination d : collObj.getDeterminations()) {
			
			String name = d.getTaxon().getFullName();
			String typeStatus = d.getTypeStatusName();
			boolean isCurrent = d.isCurrentDet();
			
			if (typeStatus != null) {
				typeNames.add(name);
				typeStatuses.add(typeStatus);
			}
			else if (isCurrent) {
				currentDets.add(name);
			}
			
			if (typeStatus != null || isCurrent) {
				Taxon t = d.getTaxon();
    			while (t != null && t.getRankId() >= TaxonTreeDef.FAMILY) t = t.getParent();
    			if (t != null) higherTaxa.add(t.getFullName());
			}
		}
	}
}
