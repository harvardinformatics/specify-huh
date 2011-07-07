/*
 * Created on 2011 June 29th
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

package edu.harvard.huh.binding;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.jibx.runtime.JiBXException;

/** Utility class to be used by the JibX bindings for processing Calendar objects.
 * JibX, can by default, only handle dates as java Date objects. Many of the Specify
 * datamodel objects contain fields of type Calendar.
 * 
 * @author lowery
 *
 */
public abstract class Utility {
	/** Method for deserializing a Calendar object from a text String
	 * 
	 * @param text
	 * @return
	 * @throws JiBXException
	 * @throws ParseException
	 */
	public static Calendar deserializeCalendar(String text) throws JiBXException, ParseException {
		if (text == null) {
			return null;
		} else {
			SimpleDateFormat dateFormat = new SimpleDateFormat();
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(dateFormat.parse(text));
			return calendar;
		}
	}
	
	/** Method for serializing a Calendar object to a String
	 * 
	 * @param calendar
	 * @return
	 * @throws JiBXException
	 */
	public static String serializeCalendar(Calendar calendar) throws JiBXException {
		GregorianCalendar date = (GregorianCalendar) calendar;
		Format formatter = new SimpleDateFormat("MMM");
		return date.get(Calendar.DATE) + "/" + formatter.format(date.getTime()) + "/" + date.get(Calendar.YEAR);
		//return calendar.getTime().toString();
	}
}
