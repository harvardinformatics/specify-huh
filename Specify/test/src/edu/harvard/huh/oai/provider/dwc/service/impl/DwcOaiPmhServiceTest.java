package edu.harvard.huh.oai.provider.dwc.service.impl;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

import ORG.oclc.oai.server.catalog.RecordFactory;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import au.org.tern.ecoinformatics.oai.provider.model.IdentifierRecord;
import au.org.tern.ecoinformatics.oai.provider.model.ListIdentifiersResponse;
import au.org.tern.ecoinformatics.oai.provider.model.Record;
import au.org.tern.ecoinformatics.oai.provider.service.NativeObjectService;
import edu.ku.brc.specify.datamodel.CollectionObject;

public class DwcOaiPmhServiceTest extends TestCase {

	// see au.org.tern.ecoinformatics.oai.provider.util.DateUtils
	private String OAI_DATE_FORMAT = "yyyy-MM-dd";
	
	private String DWC_METADATA_PREFIX = "dwc";
	private String badMetadataPrefix = "badMetadataPrefix";

	private Integer id1 = new Integer(1);
	private Integer id2 = new Integer(2);
	private Integer unsupportedId = new Integer(3);

	private String oaiId1 = "co.1";
	private String oaiId2 = "co.2";
	private String badFormatOaiId = "badOaiIdFormat";
	private String unsupportedItemOaiId = "co.3";
	private String noRecordOaiId = "co.4";

	private CollectionObject collObj1 = null;
	private CollectionObject collObj2 = null;
	private CollectionObject unsupportedItem = null;

	private Timestamp timestamp0 = Timestamp.valueOf("1990-01-01 00:00:00");
	private Timestamp timestamp1 = Timestamp.valueOf("1992-06-23 15:37:04");
	private Timestamp timestamp2 = Timestamp.valueOf("2000-01-01 00:00:00");
	private Timestamp timestamp3 = Timestamp.valueOf("2001-01-01 00:00:00");

	private boolean isDeleted = false;
	
	private DwcOaiPmhService oaiService = null;

	@Before
	public void setUp() {
		final HashMap<CollectionObject, String> objects = new HashMap<CollectionObject, String>();
		
		collObj1 = new CollectionObject();
		collObj1.setCollectionObjectId(id1);
		collObj1.setTimestampCreated(timestamp1);
		
		objects.put(collObj1, oaiId1);
		
		collObj2 = new CollectionObject();
		collObj2.setCollectionObjectId(id2);
		collObj2.setTimestampCreated(timestamp2);
		
		objects.put(collObj2, oaiId2);
		
		unsupportedItem = new CollectionObject();
		unsupportedItem.setCollectionObjectId(unsupportedId);
		
		oaiService = new DwcOaiPmhService();
		
		oaiService.setNativeObjectService(new NativeObjectService() {

			@Override
			public Object getObject(Long id) {
				for (CollectionObject collObj : objects.keySet()) {
					if (collObj.getId() == id.longValue()) return collObj;
				}
				return null;
			}

			@Override
			public List<Object> getObjects(Date fromDate, Date toDate) {
				List<Object> result = new ArrayList<Object>();
				for (CollectionObject collObj : objects.keySet()) {
					Timestamp timestamp = collObj.getTimestampModified();
					if (fromDate != null) {
						if (fromDate.equals(timestamp) || fromDate.before(timestamp)) {
							if (toDate == null) {
								result.add(collObj);
							}
							else {
								if (toDate.equals(timestamp) || toDate.after(timestamp)) {
									result.add(collObj);
								}
							}
						}
					}
					else {
						if (toDate == null) {
							result.add(collObj);
						}
						else {
							if (toDate.equals(timestamp) || toDate.after(timestamp)) {
								result.add(collObj);
							}
						}
					}
				}
				return result;
			}
			
		});
		
		Properties properties = new Properties();
		oaiService.setRecordFactory(new RecordFactory(properties) {

			@Override
			public String fromOAIIdentifier(String identifier) {
				for (CollectionObject collObj : objects.keySet()) {
					if (identifier.equals(objects.get(collObj))) return String.valueOf(collObj.getId());
				}
				return null;
			}

			@Override
			public String quickCreate(Object nativeItem, String schemaURL,
					String metadataPrefix) throws IllegalArgumentException,
					CannotDisseminateFormatException {
				if (badMetadataPrefix.equals(metadataPrefix)) throw new CannotDisseminateFormatException(metadataPrefix);
				if (nativeItem.equals(unsupportedItem)) throw new IllegalArgumentException();
				return null;
			}

			@Override
			public String getOAIIdentifier(Object nativeItem) {
				for (CollectionObject collObj : objects.keySet()) {
					if (collObj.getId().equals(((CollectionObject) nativeItem).getId())) return objects.get(collObj);
				}
				return null;
			}

			@Override
			public String getDatestamp(Object nativeItem) {
				CollectionObject collObj = (CollectionObject) nativeItem;
				return (new SimpleDateFormat(OAI_DATE_FORMAT)).format(collObj.getTimestampModified());
			}

			@Override
			public Iterator getSetSpecs(Object nativeItem)
					throws IllegalArgumentException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean isDeleted(Object nativeItem) {
				// TODO Auto-generated method stub
				return isDeleted;
			}

			@Override
			public Iterator getAbouts(Object nativeItem) {
				// TODO Auto-generated method stub
				return null;
			}
			
		});
	}
	
	@Test
	public void testGetDateFormat() {
		// Document the date format of the oai records
		assertEquals(oaiService.getDateFormat(), OAI_DATE_FORMAT);
	}

	@Test
	public void testGetMetadataFormats() {
		// We currently support only dwc
		assertTrue(oaiService.getMetadataFormats().contains(DWC_METADATA_PREFIX));
		assertEquals(oaiService.getMetadataFormats().size(), 1);
	}

	@Test
	public void testGetSets() {
		// We are not currently supporting sets
		assertEquals(0, oaiService.getSets().size());
	}

	@Test
	public void testGetRecord() {
		// return null record for bad oai identifier format
		assertNull(oaiService.getRecord(badFormatOaiId, DWC_METADATA_PREFIX));
		
		// return null record for bad metadata format
		assertNull(oaiService.getRecord(oaiId1, badMetadataPrefix));
		
		// invoke a CannotDisseminateFormatException from RecordFactory: return null for non-existent record
		assertNull(oaiService.getRecord(noRecordOaiId, DWC_METADATA_PREFIX));
		
		// invoke an IllegalArgumentException from RecordFactory: return null for unsupported object/metadata combination
		assertNull(oaiService.getRecord(unsupportedItemOaiId, DWC_METADATA_PREFIX));

		Record record = oaiService.getRecord(oaiId1, DWC_METADATA_PREFIX);
		
		assertNotNull(record);
		assertEquals(record.datestamp, (new SimpleDateFormat(OAI_DATE_FORMAT)).format(timestamp1));
		assertEquals(record.deleted, isDeleted);
		assertEquals(record.identifier, oaiId1);
	}

	@Test
	public void testListIdentifiers() {
		
		// Test a null interval, which should return both (all) records
		ListIdentifiersResponse response = oaiService.listIdentifiers(null, null, DWC_METADATA_PREFIX, null);
		List<IdentifierRecord> records = response.getIdentifierRecords();
		
		assertEquals(2, records.size());
		
		List<String> identifiers = new ArrayList<String>();
		for (IdentifierRecord record : records) {
			identifiers.add(record.getIdentifier());
		}
		assertTrue(identifiers.contains(oaiId1));
		assertTrue(identifiers.contains(oaiId2));
	}

	@Test
	public void testListIdentifiersDates1() {
		
		// Test an interval that contains both records
		ListIdentifiersResponse response = oaiService.listIdentifiers(timestamp0, timestamp3, DWC_METADATA_PREFIX, null);
		List<IdentifierRecord> records = response.getIdentifierRecords();
		
		assertEquals(2, records.size());

		List<String> identifiers = new ArrayList<String>();
		for (IdentifierRecord record : records) {
			identifiers.add(record.getIdentifier());
		}
		assertTrue(identifiers.contains(oaiId1));
		assertTrue(identifiers.contains(oaiId2));
	}
	
	@Test
	public void testListIdentifiersDates2() {
		// Test an interval that contains the first record and not the second
		ListIdentifiersResponse response = oaiService.listIdentifiers(timestamp0, timestamp1, DWC_METADATA_PREFIX, null);
		List<IdentifierRecord> records = response.getIdentifierRecords();
		
		assertEquals(1, records.size());

		List<String> identifiers = new ArrayList<String>();
		for (IdentifierRecord record : records) {
			identifiers.add(record.getIdentifier());
		}
		assertTrue(identifiers.contains(oaiId1));
	}
	
	@Test
	public void testListIdentifiersDates3() {
		// Test an interval that contains the second record and not the first
		ListIdentifiersResponse response = oaiService.listIdentifiers(timestamp2, timestamp3, DWC_METADATA_PREFIX, null);
		List<IdentifierRecord> records = response.getIdentifierRecords();
		
		assertEquals(1, records.size());

		List<String> identifiers = new ArrayList<String>();
		for (IdentifierRecord record : records) {
			identifiers.add(record.getIdentifier());
		}
		assertTrue(identifiers.contains(oaiId2));
	}
	
	@Test
	public void testListIdentifiersDates4() {
		// Test an interval that pre-dates both records
		ListIdentifiersResponse response = oaiService.listIdentifiers(timestamp0, timestamp0, DWC_METADATA_PREFIX, null);
		List<IdentifierRecord> records = response.getIdentifierRecords();
		
		assertEquals(0, records.size());
	}
	
	@Test
	public void testListIdentifiersDates5() {
		// Test an interval that post-dates both records
		ListIdentifiersResponse response = oaiService.listIdentifiers(timestamp3, timestamp3, DWC_METADATA_PREFIX, null);
		List<IdentifierRecord> records = response.getIdentifierRecords();
		
		assertEquals(0, records.size());
	}
}
