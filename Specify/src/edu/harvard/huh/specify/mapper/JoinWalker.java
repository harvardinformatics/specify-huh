package edu.harvard.huh.specify.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import edu.harvard.huh.specify.mapper.SpecifyFieldMappingDesc.PathSegment;

import edu.ku.brc.af.core.db.DBRelationshipInfo;
import edu.ku.brc.af.core.db.DBRelationshipInfo.RelationshipType;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.ui.forms.DataObjectGettable;
import edu.ku.brc.af.ui.forms.DataObjectGettableFactory;
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

	private HashMap<Class<?>, HashMap<String, TreeDefItemStandardEntry>> levels =
		new HashMap<Class<?>, HashMap<String, TreeDefItemStandardEntry>>();
		
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
			levels.put(treeClass, new HashMap<String, TreeDefItemStandardEntry>());

			for (TreeDefItemStandardEntry entry : treeDef.getStandardLevels()) {
				levels.get(treeClass).put(entry.getName(), entry);
			}
		}
	}

	public Object getPathValue(Object object, String joinPath, String fieldName, boolean isRelationship) {

		List<PathSegment> joins = parseTablePath(joinPath);

		PathSegment lastJoin = joins.get(joins.size() - 1);
		String relationshipName = null;

		DBTableInfo fromTableInfo = null;
		for (PathSegment join : joins) {

			Integer tableId = join.getTableId();
			DBTableInfo toTableInfo = tableMgr.getInfoById(tableId);
			if (toTableInfo == null) throw new RuntimeException("Couldn't find table for id: " + tableId);

			if (! toTableInfo.getPermissions().canView()) return null;

			// if this is the first in the series, just get the next one
			if (fromTableInfo == null) {

				fromTableInfo = toTableInfo;
				continue;
			}

			DBRelationshipInfo relationshipInfo = null;
			relationshipName = join.getRelationshipName();
			if (relationshipName != null) {
				// get the relationship by relationship name
				relationshipInfo = fromTableInfo.getRelationshipByName(relationshipName);
				
				if (relationshipInfo == null && fromTableInfo.getClass().isAssignableFrom(Treeable.class)) {
					// possibly this is the name of a rank
					Class<?> fromClass = toTableInfo.getClass();
					
					for (Class<?> treeClass : levels.keySet()) {
						if (fromClass.isAssignableFrom(treeClass)) {
							if (levels.get(treeClass).keySet().contains(relationshipName)) {
								object = getParent((Treeable<?,?,?>) object, levels.get(treeClass).get(relationshipName).getRank());
								if (object == null) return null;
								fromTableInfo = toTableInfo;
								continue;
							}
						}
					}
				}
			}
			else {
				// get the relationship by table id
				for (DBRelationshipInfo candidateRelInfo : fromTableInfo.getRelationships()) {

					if (candidateRelInfo.getClassName().equals(toTableInfo.getClassName())) {
						relationshipInfo = candidateRelInfo;
						break;
					}
				}
			}
			if (relationshipInfo == null) throw new RuntimeException("Couldn't find relationship from table with id " +
					fromTableInfo.getTableId() + " to table with id " + tableId);

			// get the joined object by relationship
			DataObjectGettable getter = DataObjectGettableFactory.get(object.getClass().getName(), gettableClassName);
			object = getter.getFieldValue(object, relationshipInfo.getName());

			// this object graph didn't contain the field we were looking for.
			if (object == null) return null;

			RelationshipType relationshipType = relationshipInfo.getType();

			// Is our object really a collection?  We may have to take only the first member.
			if (relationshipType.equals(RelationshipType.ZeroOrOne) ||
					relationshipType.equals(RelationshipType.OneToMany) ||
						relationshipType.equals(RelationshipType.ManyToMany)) {

				if (!join.equals(lastJoin)) {
					
					if (! (object instanceof Collection<?>)) {
						
						// The data model configuration did not match the object graph
						throw new RuntimeException("Found single object when collection expected for " +
								"table with id " + fromTableInfo.getTableId() + ", relationship '" + relationshipInfo.getName() + "': " + object.getClass().getName() + "");
					}
					
					Collection<?> collection = (Collection<?>) object;

					if (collection.size() < 1) {
						return null;
					}
					else {
						Object firstObject = collection.iterator().next();

						if (collection.size() == 1) {
							object = firstObject;
						}
						else {
							// Silently pruning the tree of objects, taking only the first in this collection with us
							// as we walk the rest of the table path
							
							if (firstObject instanceof Comparable) { // really any DataModelObjBase object could be, since they have timestamps, but that is not yet implemented
								List<Comparable<?>> list = new ArrayList<Comparable<?>>();
								for (Object comparableObj : collection) {
									list.add((Comparable<?>) comparableObj);
								}
								Collections.sort((List<? extends Comparable<? super Comparable<?>>>) list);
								object = list.get(0);
							}
							else {
								// punt.
								object = collection.iterator().next();
							}
						}
					}
				}
			}
			else if (object instanceof Collection<?>) {
				// The data model configuration did not match the object graph
				throw new RuntimeException("Found collection when single object expected for " +
						"table with id " + fromTableInfo.getTableId() + ", relationship '" + relationshipInfo.getName() + "': " + object.getClass().getName() + "");
			}
			fromTableInfo = toTableInfo;
		}

		// we might still have to get the last field.
		if (!fieldName.equals(relationshipName)) {

			if (object instanceof Treeable<?,?,?> && levels.get(object.getClass()).keySet().contains(fieldName)) {
				return getParent((Treeable<?,?,?>) object, levels.get(object.getClass()).get(fieldName).getRank());
			}
			DataObjectGettable getter = DataObjectGettableFactory.get(object.getClass().getName(), gettableClassName);
			object = getter.getFieldValue(object, fieldName);
		}

		if (object instanceof Collection<?>) {

			Collection<?> collection = (Collection<?>) object;

			if (collection.size() >= 1) {
				Object firstObject = collection.iterator().next();

				if (firstObject instanceof Comparable) {
					List<Comparable<?>> orderedList = new ArrayList<Comparable<?>>();
					for (Object comparableObj : collection) {
						orderedList.add((Comparable<?>) comparableObj);
					}
					Collections.sort((List<? extends Comparable<? super Comparable<?>>>) orderedList);
					return isRelationship ? orderedList : orderedList.get(0);  // silently pruning the list if a collection was not expected
				}
			}
			return null; // empty collection
		}
		else {
			if (isRelationship) {
				List<Object> list = new ArrayList<Object>();
				list.add(object);
				return list;
			}
			else {
				return object;
			}
		}
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