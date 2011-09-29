package edu.harvard.huh.oai.provider.dwc.crosswalk;

import java.io.StringWriter;
import java.math.BigInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import au.org.tern.ecoinformatics.oai.provider.util.DateUtils;
import dwc.huh_harvard_edu.tdwg_dwc_simple.SimpleDarwinRecord;
import dwc.huh_harvard_edu.tdwg_dwc_simple.TypeEnum;
import edu.harvard.huh.dwc.model.Occurrence;
import edu.harvard.huh.oai.provider.crosswalk.Crosswalk;
import edu.ku.brc.specify.datamodel.CollectionObject;

public class DwcCrosswalk implements Crosswalk {

	@Override
	public SimpleDarwinRecord crosswalk(Object nativeObject) {
		SimpleDarwinRecord record = new SimpleDarwinRecord();

		if (nativeObject instanceof Occurrence) {
			Occurrence occurrence = (Occurrence) nativeObject;
			
			record.setType(TypeEnum.OCCURRENCE.toString());
			
			String associatedMedia;
			String associatedOccurrences;
			String associatedReferences;
			String associatedSequences;
			String associatedTaxa;
			String behavior;
			
			String catalogNumber = occurrence.getCatalogNumber();
			if (catalogNumber != null) record.setCatalogNumber(catalogNumber);
			
			String disposition;
			String establishmentMeans;
			
			String individualCountString = occurrence.getIndividualCount();
			if (individualCountString != null) {
				try {
					record.setIndividualCount(BigInteger.valueOf(Integer.parseInt(individualCountString)));
				}
				catch (NumberFormatException nfe) {
					; // TODO: log parse exception for individual count
				}
			}
			
			String individualId = occurrence.getIndividualId();
			if (individualId != null) record.setIndividualID(individualId);
			
			String lifeStage;
			
			String occurrenceDetails = occurrence.getOccurrenceDetails();
			if (occurrenceDetails != null) record.setOccurrenceDetails(occurrenceDetails);
			
			String occurrenceRemarks = occurrence.getOccurrenceRemarks();
			if (occurrenceRemarks != null) record.setOccurrenceRemarks(occurrenceRemarks);
			
			String occurrenceStatus;
			String otherCatalogNumbers;
			String preparations;
			String previousIdentifications;
			
			String recordNumber = occurrence.getRecordNumber();
			if (recordNumber != null) record.setRecordNumber(recordNumber);
			
			String recordedBy = occurrence.getRecordedBy();
			if (recordedBy != null) record.setRecordedBy(recordedBy);
			
			String reproductiveCondition;
			String sex;
			
			
			
		}
		else {
			throw new IllegalArgumentException();
		}
		// match the native object to the SimpleDarwinRecord.
		
		return record;
	}

	// this method is based entirely on the superclass's version.  --mmk
	@Override
	public String crosswalkToString(Object nativeObject) {
		SimpleDarwinRecord occurrence = crosswalk(nativeObject);
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance("dwc.huh_harvard_edu.tdwg_dwc_simple");
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			StringWriter writer = new StringWriter();
			marshaller.marshal(occurrence, writer);
			
			return "<metadata>" + writer.toString() + "</metadata>";
		} 
		catch (JAXBException je) {
			
			return null;			
		}
	}

	// this method is based entirely on the superclass's version.  --mmk
	@Override
	public String getDatestamp(Object nativeItem) {

		return DateUtils.formatDate(((CollectionObject) nativeItem).getTimestampModified(), false);
	}

}
