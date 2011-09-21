/**
 * This class modeled after the class of a similar name in the
 * au.org.tern.ecoinformatics.oai.provider.sample.service.impl package by
 * Terrestrial Ecosystem Research Network.  Their copyright statement is included
 * below.  --mmk 2011-09-20
 * 
 * Copyright 2010 Terrestrial Ecosystem Research Network, licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or
 * agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.harvard.huh.oai.specify;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import au.org.tern.ecoinformatics.oai.provider.service.NativeObjectService;

public class SpecifyOccurrenceService implements NativeObjectService {

	private SpecifyOccurrenceDao specifyOccurrenceDao;
	
	public void setSpecifyOccurrenceDao(SpecifyOccurrenceDao specifyOccurrenceDao) {
		this.specifyOccurrenceDao = specifyOccurrenceDao;
	}
	
	@Override
	public Object getObject(Long id) {

		return specifyOccurrenceDao.get(id);
	}
	
	@Override
	public List<Object> getObjects(Date fromDate, Date toDate) {
		List<Object> objects = new ArrayList<Object>();
		List<SpecifyOccurrence> occurrences = specifyOccurrenceDao.getOccurrences(fromDate, toDate); 
		for (SpecifyOccurrence occurrence : occurrences) {
			objects.add(occurrence);
		}
		
		return objects; 
	}

}
