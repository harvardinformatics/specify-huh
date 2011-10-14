package edu.harvard.huh.specify.mapper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import edu.harvard.huh.specify.mapper.SpecifyFieldMappingDesc.PathSegment;

import edu.ku.brc.af.core.db.DBRelationshipInfo;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.ui.forms.DataObjectGettable;
import edu.ku.brc.af.ui.forms.DataObjectGettableFactory;
import edu.ku.brc.af.ui.forms.formatters.DataObjFieldFormatMgr;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace.PartialDateEnum;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterMgr;
import edu.ku.brc.specify.datamodel.DataModelObjBase;
import edu.ku.brc.specify.datamodel.Geography;
import edu.ku.brc.specify.datamodel.GeographyTreeDef;
import edu.ku.brc.specify.datamodel.GeologicTimePeriod;
import edu.ku.brc.specify.datamodel.GeologicTimePeriodTreeDef;
import edu.ku.brc.specify.datamodel.LithoStrat;
import edu.ku.brc.specify.datamodel.LithoStratTreeDef;
import edu.ku.brc.specify.datamodel.Storage;
import edu.ku.brc.specify.datamodel.StorageTreeDef;
import edu.ku.brc.specify.datamodel.Taxon;
import edu.ku.brc.specify.datamodel.TaxonTreeDef;
import edu.ku.brc.specify.datamodel.TreeDefIface;
import edu.ku.brc.specify.datamodel.TreeDefItemStandardEntry;
import edu.ku.brc.specify.datamodel.Treeable;
import edu.ku.brc.util.Pair;

public class JoinWalker {
	
	private DBTableIdMgr tableMgr = null;
	private String gettableClassName = "edu.ku.brc.af.ui.forms.DataGetterForObj";

	private HashMap<Class<?>, HashMap<String, TreeDefItemStandardEntry>> treeLevels =
		new HashMap<Class<?>, HashMap<String, TreeDefItemStandardEntry>>();
	
	private SimpleDateFormat yearFormat;
	private SimpleDateFormat monthFormat;
	private SimpleDateFormat dayFormat;
	private SimpleDateFormat timeFormat;
	
	public JoinWalker(DBTableIdMgr tableMgr) {
		this.tableMgr = tableMgr;
		
		List<Pair<Class<? extends Treeable<?,?,?>>, TreeDefIface<?,?,?>>> treeClassDefs =
			new ArrayList<Pair<Class<? extends Treeable<?,?,?>>, TreeDefIface<?,?,?>>>();

		treeClassDefs.add(new Pair<Class<? extends Treeable<?,?,?>>, TreeDefIface<?,?,?>>(Geography.class, new GeographyTreeDef()));
		treeClassDefs.add(new Pair<Class<? extends Treeable<?,?,?>>, TreeDefIface<?,?,?>>(GeologicTimePeriod.class, new GeologicTimePeriodTreeDef()));
		treeClassDefs.add(new Pair<Class<? extends Treeable<?,?,?>>, TreeDefIface<?,?,?>>(LithoStrat.class, new LithoStratTreeDef()));
		treeClassDefs.add(new Pair<Class<? extends Treeable<?,?,?>>, TreeDefIface<?,?,?>>(Storage.class, new StorageTreeDef()));
		treeClassDefs.add(new Pair<Class<? extends Treeable<?,?,?>>, TreeDefIface<?,?,?>>(Taxon.class, new TaxonTreeDef()));
		
		for (Pair<Class<? extends Treeable<?,?,?>>, TreeDefIface<?,?,?>> treeClassPair : treeClassDefs) {
			
			Class<? extends Treeable<?,?,?>> treeClass = treeClassPair.getFirst();
			TreeDefIface<?,?,?> treeDef = treeClassPair.getSecond();
			treeLevels.put(treeClass, new HashMap<String, TreeDefItemStandardEntry>());

			for (TreeDefItemStandardEntry entry : treeDef.getStandardLevels()) {
				treeLevels.get(treeClass).put(entry.getName(), entry);
			}
		}
	}

	public SimpleDateFormat getYearFormat() {
		return yearFormat;
	}

	public void setYearFormat(SimpleDateFormat yearFormat) {
		this.yearFormat = yearFormat;
	}
	
	public SimpleDateFormat getMonthFormat() {
		return monthFormat;
	}
	
	public void setMonthFormat(SimpleDateFormat monthFormat) {
		this.monthFormat = monthFormat;
	}
	
	public SimpleDateFormat getDayFormat() {
		return dayFormat;
	}
	
	public void setDayFormat(SimpleDateFormat dayFormat) {
		this.dayFormat = dayFormat;
	}
	
	public SimpleDateFormat getTimeFormat() {
		return timeFormat;
	}
	
	public void setTimeFormat(SimpleDateFormat timeFormat) {
		this.timeFormat = timeFormat;
	}

	private DBTableInfo getTableInfo(PathSegment join) {
		Integer tableId = join.getTableId();
		DBTableInfo tableInfo = tableMgr.getInfoById(tableId);
		if (tableInfo == null) throw new RuntimeException("Couldn't find table for id: " + tableId);
		return tableInfo;
	}

	private DBRelationshipInfo getRelationship(String relationshipName, DBTableInfo fromTableInfo, DBTableInfo toTableInfo) {

		if (relationshipName == null) {

			// get the relationship by table id
			for (DBRelationshipInfo candidateRelInfo : fromTableInfo.getRelationships()) {

				if (candidateRelInfo.getClassName().equals(toTableInfo.getClassName())) {
					return candidateRelInfo;
				}
			}
		}
		else {
			// get the "from" table's relationship information by this name
			return fromTableInfo.getRelationshipByName(relationshipName);
		}
		return null;
	}
	
	private List<?> collectionToSortedList(Collection<?> collection) {
		if (collection.size() >= 1) {
			Object firstObject = collection.iterator().next();

			if (firstObject instanceof Comparable) {
				List<Comparable> orderedList = new ArrayList<Comparable>();
				for (Object comparableObj : collection) {
					orderedList.add((Comparable) comparableObj);
				}
				Collections.sort(orderedList);
				return orderedList;
			}
			else if (firstObject instanceof DataModelObjBase) {
				List<DataModelObjBase> orderedList = new ArrayList<DataModelObjBase>();
				for (Object dataModelObj : collection) {
					orderedList.add((DataModelObjBase) dataModelObj);
				}
				Collections.sort(orderedList, new Comparator<DataModelObjBase>() {

					@Override
					public int compare(DataModelObjBase o1, DataModelObjBase o2) {
						return o1.getTimestampModified().compareTo(o2.getTimestampModified());
					}
					
				});
				return orderedList;
			}
			else {
				List<Object> unorderedList = new ArrayList<Object>();
				for (Object object : collection) {
					unorderedList.add(object);
				}
				return unorderedList;
			}
		}
		return null; // empty collection
	}

	private Integer getTreeRank(Class<?> toClass, String relationshipName) {
		for (Class<?> treeClass : treeLevels.keySet()) {
			if (toClass.isAssignableFrom(treeClass)) {
				if (treeLevels.get(treeClass).keySet().contains(relationshipName)) {
					return Integer.valueOf(treeLevels.get(treeClass).get(relationshipName).getRank());
				}
			}
		}
		return null;
	}

	private String applyFormatter(Object object, String fieldName) {
		List<UIFieldFormatterIFace> formatters = UIFieldFormatterMgr.getInstance().getFormatterList(object.getClass(), fieldName);
		if (formatters.size() > 0) {
			UIFieldFormatterIFace formatter = formatters.get(0);
			if (formatter.isDate()) {
				PartialDateEnum partialDateType = formatter.getPartialDateType();
				if (partialDateType == null ||
						partialDateType.equals(PartialDateEnum.None)) {
					return applyDateFormat(object, timeFormat);
				}
				else if (partialDateType.equals(PartialDateEnum.Full)) {
					return applyDateFormat(object, dayFormat);
				}
				else if (partialDateType.equals(PartialDateEnum.Month)) {
					return applyDateFormat(object, monthFormat);
				}
				else if (partialDateType.equals(PartialDateEnum.Year)) {
					return applyDateFormat(object, yearFormat);
				}
				else if (partialDateType.equals(PartialDateEnum.Time)) {
					return applyDateFormat(object, timeFormat);
				}
				else {
					return object.toString(); // TODO: should probably warn here or something
				}
			}
			else {
				return formatter.formatToUI(object).toString();
			}
		}
		else {
			return object.toString();
		}
	}
	
	private String applyDateFormat(Object object, SimpleDateFormat format) {

		if (object instanceof Calendar) return format.format((Calendar) object);
		else if (object instanceof Date)  return format.format((Date) object);
        else return null;
	}

	private String applyAggregator(List<?> list) {
		Object firstObject = list.get(0);
		String result = DataObjFieldFormatMgr.getInstance().aggregate(list, firstObject.getClass());
		return result != null && result.length() == 0 ? null : result;
	}

	public String getPathValue(Object object, List<PathSegment> joins, String fieldName, boolean isRelationship) {

		String relationshipName = null;

		DBTableInfo fromTableInfo = null;
		for (PathSegment join : joins) {
			
			// get table info, which tells us what fields are available
			DBTableInfo toTableInfo = getTableInfo(join);

			// check permissions
			if (! toTableInfo.getPermissions().canView()) return null;

			// if this is the first in the series, just get the next one
			if (fromTableInfo == null) {

				fromTableInfo = toTableInfo;
				continue;
			}
		
			// the path segment might contain the relationship name
			relationshipName = join.getRelationshipName();
			DBRelationshipInfo relationshipInfo = getRelationship(relationshipName, fromTableInfo, toTableInfo);

			// possibly this is the name of a rank rather than a true "join" relationship;
			// if this is so, the "from" table will not have a relationship by this name,
			// and the "from" table will also be one of the "tree" tables
			if (relationshipInfo == null) {

				if (fromTableInfo.getClass().isAssignableFrom(Treeable.class)) {

					Class<?> toClass = toTableInfo.getClass();
					Integer rankId = getTreeRank(toClass, relationshipName);
					if (rankId != null) {
						object = getParent((Treeable<?,?,?>) object, rankId);
						if (object == null) return null;
						fromTableInfo = toTableInfo;
						continue;
					}
				}			
				throw new RuntimeException("Couldn't find relationship from table with id " +
						fromTableInfo.getTableId() + " to table with id " + toTableInfo.getTableId());
			}

			// Is our object really a collection?  We may have to take only the first member.
			if (object instanceof Collection<?>) {
				List<?> orderedList = collectionToSortedList((Collection<?>) object);

				if (orderedList == null || orderedList.size() < 1) {
					return null;
				}
				else {
					// Silently pruning the tree of objects, taking only the first in this collection with us
					object = orderedList.get(0);
				}
			}

			// get the joined object by relationship
			DataObjectGettable getter = DataObjectGettableFactory.get(object.getClass().getName(), gettableClassName);
			object = getter.getFieldValue(object, relationshipInfo.getName());

			// this object graph might not contain a value in the field we are looking for.
			if (object == null) return null;

			fromTableInfo = toTableInfo;
		}

		// the last token in the path may be a field rather than a join, so we might still have to get the last field.
		if (!fieldName.equals(relationshipName)) {

			if (object instanceof Treeable<?,?,?> && treeLevels.get(object.getClass()).keySet().contains(fieldName)) {
				object = getParent((Treeable<?,?,?>) object, treeLevels.get(object.getClass()).get(fieldName).getRank());
			}
			else {
				DataObjectGettable getter = DataObjectGettableFactory.get(object.getClass().getName(), gettableClassName);
				object = getter.getFieldValue(object, fieldName);
			}
		}

		// here we allow a list of items, which will be aggregated, formatted and returned, unless a list was not requested
		if (object instanceof Collection<?>) {

			List<?> orderedList = collectionToSortedList((Collection<?>) object);
			if (orderedList == null) return null;

			if (isRelationship) {
				return applyAggregator(orderedList);
			}
			// prune the list if a collection was not requested by the mapItem TODO: apply aggregator/formatter here
			else {
				object = orderedList.get(0);
			}
		}

		return applyFormatter(object, fieldName);
	}
	
	public static List<PathSegment> parseTablePath(String tablePath) {

		if (tablePath == null || !tablePath.matches("\\d+(-\\w+)?(,\\d+(-\\w+)?)*")) {
			throw new IllegalArgumentException("TablePath must match '\\d+(-\\w+)?(,\\d+(-\\w+)?)*': " + tablePath);
		}

		String[] pathSegmentStrings = tablePath.split(",");
		List<PathSegment> pathSegments = new ArrayList<PathSegment>(pathSegmentStrings.length);

		for (String pathSegmentString : pathSegmentStrings) {

			Integer tableId = null;
			String relationshipName = null;

			int i = pathSegmentString.indexOf('-');

			if (i >= 0) {
				tableId = Integer.parseInt(pathSegmentString.substring(0, i));
				relationshipName = pathSegmentString.substring(i + 1);
			}
			else {
				tableId = Integer.parseInt(pathSegmentString);
			}
			pathSegments.add(new PathSegment(tableId, relationshipName));
		}
		
		return pathSegments;
	}
	
	private Treeable<?,?,?> getParent(Treeable<?,?,?> treeable, int rankId) {
		do {
			if (treeable.getRankId() == rankId) return treeable;
			Treeable<?,?,?> parent = treeable.getParent();
			if (parent == null) break;
			treeable = parent;
		} while (treeable.getRankId() >= rankId);
		return null;
	}
}