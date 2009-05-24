package edu.harvard.huh.asa2specify;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import edu.harvard.huh.asa.BDate;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;

public class DateUtils {
	
	public static final int MIN_YEAR = 1766;
	public static final int MAX_YEAR = 2008;
	private static final String[] MONTHS =
		{ "January", "February", "March", "April", "May", "June",
			"July", "August", "September", "October", "November", "December" };
	
	public static Timestamp toTimestamp(Date date) {
	    return new Timestamp(date.getTime());
	}
	
	public static Calendar toCalendar(Date date) {
		GregorianCalendar c = new GregorianCalendar();
		c.clear();

		c.set(GregorianCalendar.YEAR, date.getYear());
		c.set(GregorianCalendar.MONTH, date.getMonth());
		c.set(GregorianCalendar.DAY_OF_MONTH, date.getDay());

		return c;
	}
	
	/**
	 * Return a string representation in the format "mmm dd, yyyy - mmm dd, yyyy"
	 */
	public static String getShortDate(BDate date) {
		if ( date == null ) return "";

		String startDate = getShortDate(date.getStartMonth(), date.getStartDay(), date.getStartYear());
		String endDate = getShortDate(date.getEndMonth(), date.getEndDay(), date.getEndYear());

		return startDate + " - " + endDate;
	}
	
	private static String getMMM(Integer i) {
		if ( !isAsaNull(i) && i.intValue() <= 12 ) return MONTHS[i.intValue()].substring(0, 3);
		else return null;
	}
	
	private static String getShortDate(Integer month, Integer day, Integer year) {
		StringBuffer result = new StringBuffer();

		String mmm1 = getMMM(month);

		if ( mmm1 != null ) {
			result.append(mmm1);

			if ( !isAsaNull(day) ) result.append(" " + day.toString());
			if ( !isAsaNull(year) ) result.append(", " + year.toString());
		}
		else {
			if ( !isAsaNull(day) ) {
				result.append("??? " + day.toString());
				if ( !isAsaNull(year) ) result.append(", " + year.toString());
			}
			else {
				if ( !isAsaNull(year) ) result.append(year.toString());
			}
		}

		return result.toString();
	}
	
	/**
	 * Return true if the date passes isValidCollectionDate and has a year.  Assume months start with 1.
	 */
	public static boolean isValidSpecifyDate(Integer year, Integer month, Integer day) {
		return isValidCollectionDate(year, month, day) && ! isAsaNull(year);
	}
	
	/**
	 * Return true if the date is a logical calendar date and the year, if present, is a
	 * valid collection year.  Otherwise return false.  Assume months start with 1.
	 */
	public static boolean isValidCollectionDate(Integer year, Integer month, Integer day) {
		GregorianCalendar c = new GregorianCalendar();
		c.clear();
		
		if (! isAsaNull(year)) {

			int yr = year.intValue();

			if (isValidCollectionYear(yr)) {

				c.set(GregorianCalendar.YEAR, yr);

				if (! isAsaNull(month)) {

					int mo = month.intValue() - 1;

					if (isValidCalendarMonth(c, mo)) {

						c.set(GregorianCalendar.MONTH, mo);

						if (! isAsaNull(day)) {

							int dy = day.intValue();

							if (isValidCalendarDayOfMonth(c, dy)) {
							
								c.set(GregorianCalendar.DAY_OF_MONTH, dy);
							}
							else {  // day out of bounds
								return false;
							}
						}
						else { // day is null
						}
					}
					else { // month out of bounds
						return false;
					}
				}
				else { // month is null
					if (isAsaNull(day)) {
						// day is null
					}
					else { // day is not null
						return false;
					}
				}
			}
			else { // year is out of bounds
				return false;
			}
		}
		else { // year is null, using default year
			if (! isAsaNull(month)) {

				int mo = month.intValue() - 1;

				if (isValidCalendarMonth(c, mo)) {

					c.set(GregorianCalendar.MONTH, mo);

					if (day != null && day.intValue() != 0) {

						int dy = day.intValue();

						if (isValidCalendarDayOfMonth(c, dy)) {
						
							c.set(GregorianCalendar.DAY_OF_MONTH, dy);
						}
						else {  // day out of bounds
							return false;
						}
					}
					else { // day is null
					}
				}
				else { // month out of bounds
					return false;
				}
			}
			else { // month is null
				if (! isAsaNull(day)) {
					return false;
				}
				else { // day is null
				}
			}
		}

		return true;
	}

	/**
	 * Return a Calendar if there is a completely valid interval with corresponding end fields
	 * for each start field, or if the start date is a valid collection date and using the start
	 * date fields for the missing end date fields yields a valid collection end date.  Return
	 * null otherwise, or if the end date fields are all missing.
	 */
	public static Calendar getInterpolatedEndDate(BDate date) {
		if (isValidCollectionDate(date.getStartYear(), date.getStartMonth(), date.getStartDay())) {
		
			Calendar startCal =
				getDefaultCalendar(date.getStartYear(), date.getStartMonth(), date.getStartDay());

			Integer endYear = isAsaNull(date.getEndYear()) ? date.getStartYear() : date.getEndYear();
			Integer endMonth = isAsaNull(date.getEndMonth()) ? date.getStartMonth() : date.getEndMonth();
			Integer endDay = isAsaNull(date.getEndDay()) ? date.getStartDay() : date.getEndDay();

			Calendar endCal =
				getDefaultCalendar(endYear, endMonth, endDay);

			if ( startCal.compareTo(endCal) > 0) return null;

		
			if (! isAsaNull(date.getStartYear()) &&
					! isAsaNull(date.getStartMonth()) &&
						! isAsaNull(date.getStartDay())) {
				
				// Apr 25, 1920 - May 1, 1920
				if (! isAsaNull(date.getEndYear()) &&
						! isAsaNull(date.getEndMonth()) &&
							! isAsaNull(date.getEndDay())) { return endCal; }
				
				// Apr 25, 1920 - May 1
				else if (isAsaNull(date.getEndYear()) &&
							! isAsaNull(date.getEndMonth()) &&
								! isAsaNull(date.getEndDay())) { return endCal; }
				
				// Apr 25, 1920 - 27
				else if (isAsaNull(date.getEndYear()) &&
							isAsaNull(date.getEndMonth()) &&
								! isAsaNull(date.getEndDay())) { return endCal; }
			}
			
			else if (! isAsaNull(date.getStartYear()) &&
						! isAsaNull(date.getStartMonth()) &&
							isAsaNull(date.getStartDay())) {

				// Apr 1920 - May 1920
				if (! isAsaNull(date.getEndYear()) &&
						! isAsaNull(date.getEndMonth()) &&
							isAsaNull(date.getEndDay())) { return endCal; }
				
				// Apr 1920 - May
				else if (isAsaNull(date.getEndYear()) &&
							! isAsaNull(date.getEndMonth()) &&
								isAsaNull(date.getEndDay())) { return endCal; }
			}
			
			else if (! isAsaNull(date.getStartYear()) &&
						isAsaNull(date.getStartMonth()) &&
							isAsaNull(date.getStartDay())) {
				
				// 1920 - 1921
				if (! isAsaNull(date.getEndYear()) &&
						isAsaNull(date.getEndMonth()) &&
							isAsaNull(date.getEndDay())) { return endCal; }
			}
			
			else if ( isAsaNull(date.getStartYear()) &&
						! isAsaNull(date.getStartMonth()) &&
							! isAsaNull(date.getStartDay())) {
				
				// Apr 25 - Apr 27
				if (isAsaNull(date.getEndYear()) &&
						! isAsaNull(date.getEndMonth()) &&
							! isAsaNull(date.getEndDay())) { return endCal; }
			
				// Apr 25, 1920 - 27
				else if (isAsaNull(date.getEndYear()) &&
							isAsaNull(date.getEndMonth()) &&
								! isAsaNull(date.getEndDay())) { return endCal; }
			}
			
			else if ( isAsaNull(date.getStartYear()) &&
						! isAsaNull(date.getStartMonth()) &&
							isAsaNull(date.getStartDay())) {
		
				// 1920 - 1921
				if ( isAsaNull(date.getEndYear()) &&
						! isAsaNull(date.getEndMonth()) &&
							isAsaNull(date.getEndDay())) { return endCal; }
			}
		}	
		
		return null;
	}
	
	/**
	 * Return a Calendar to represent the start date of the bdate interval if the bdate start
	 * date is valid.  Otherwise, return null.  This value is for CollectingEvent.endDate.
	 */
	public static Calendar getSpecifyStartDate(BDate date) {
		if (isValidSpecifyDate(date.getStartYear(), date.getStartMonth(), date.getStartDay()))
			return getDefaultCalendar(date.getStartYear(), date.getStartMonth(), date.getStartDay());
		else
			return null;
	}
	
	/**
	 * Return a Calendar to represent the start date of the bdate interval if the bdate start
	 * date is valid.  Otherwise, return null.  This value is for CollectingEvent.endDate.
	 */
	public static Calendar getSpecifyEndDate(BDate date) {
		if (isValidSpecifyDate(date.getEndYear(), date.getEndMonth(), date.getEndDay()))
			return getDefaultCalendar(date.getEndYear(), date.getEndMonth(), date.getEndDay());
		else
			return null;
	}
	
	/** Return a String that represents a longhand version of the start date as entered into
	 * Asa.  This field should be provided if the bdate is a valid collection date but not a 
	 * valid specify date (e.g. it lacks a year).
	 */
	public static String getSpecifyStartDateVerbatim(BDate date) {
		if (isValidCollectionDate(date.getStartYear(), date.getStartMonth(), date.getStartDay()))
			return getSpecifyDateVerbatim(date.getStartYear(), date.getStartMonth(), date.getStartDay());
		else
			return null;
	}
	
	/** Return a String that represents a longhand version of the end date as entered into
	 * Asa.  This field should be provided if the bdate is a valid collection date but not a 
	 * valid specify date (e.g. it lacks a year).
	 */
	public static String getSpecifyEndDateVerbatim(BDate date) {
		if (isValidCollectionDate(date.getEndYear(), date.getEndMonth(), date.getEndDay()))
			return getSpecifyDateVerbatim(date.getEndYear(), date.getEndMonth(), date.getEndDay());
		else
			return null;
	}
	
	/** Return a String that represents a longhand version of the end date as entered into
	 * Asa.  This field should be provided if the bdate is a valid collection date but not a 
	 * valid specify date (e.g. it lacks a year).
	 */
	public static String getSpecifyEndDateVerbatim(Calendar date) {
		Integer year = new Integer(date.get(Calendar.YEAR));
		Integer month = new Integer(date.get(Calendar.MONTH));
		Integer day = new Integer(date.get(Calendar.DAY_OF_MONTH));

		if (isValidCollectionDate(year, month, day))
			return getSpecifyDateVerbatim(year, month, day);
		else
			return null;
	}
	
	public static boolean isAsaNull(Integer i) {
		return i == null || i.intValue() == 0;
	}
	
	private static boolean isValidCollectionYear(int yr) {
		return yr >= MIN_YEAR && yr <= MAX_YEAR;
	}
	
	private static boolean isValidCalendarMonth(GregorianCalendar c, int mo) {
		return mo >= c.getActualMinimum(GregorianCalendar.MONTH) &&
			mo <= c.getActualMaximum(GregorianCalendar.MONTH);
	}
	
	private static boolean isValidCalendarDayOfMonth(GregorianCalendar c, int dy) {
		return dy >= c.getActualMinimum(GregorianCalendar.DAY_OF_MONTH) &&
			dy <= c.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
	}
		
	/**
	 *  Create and return a GregorianCalendar, filling in the missing fields with
	 *  defaults; don't check validity; assume months start with 1
	 */
	public static GregorianCalendar getDefaultCalendar(Integer year, Integer month, Integer day) {
		GregorianCalendar c = new GregorianCalendar();
		c.clear();
		
		if (! isAsaNull(year)) c.set(GregorianCalendar.YEAR, year.intValue());
		if (! isAsaNull(month)) c.set(GregorianCalendar.MONTH, month.intValue()-1);
		if (! isAsaNull(day)) c.set(GregorianCalendar.DAY_OF_MONTH, day.intValue());
		
		return c;
	}
	
	public static byte getDatePrecision(Integer year, Integer month, Integer day) {
	    byte result = (byte) UIFieldFormatterIFace.PartialDateEnum.None.ordinal();
	    
	    if (! isAsaNull(year)) {
	        result = (byte)  UIFieldFormatterIFace.PartialDateEnum.Year.ordinal();
	        if (! isAsaNull(month)) {
	            result = (byte) UIFieldFormatterIFace.PartialDateEnum.Month.ordinal();
	            if (! isAsaNull(day)) {
	                result = (byte) UIFieldFormatterIFace.PartialDateEnum.Full.ordinal();
	            }
	        }
	    }
	    
	    return result;
	}
	
	// don't call this unless the params have been vetted by isValidCollectionDate
	// assume months begin at 1
	private static String getSpecifyDateVerbatim(Integer year, Integer month, Integer day) {
		if (isAsaNull(month)) {
			return isAsaNull(year) ? null : year.toString();
		}
		else {
			if (isAsaNull(day)) {
				if (isAsaNull(year)) {
					return MONTHS[month.intValue()-1];
				}
				else {
					return MONTHS[month.intValue()-1] + ", " + year.toString();
				}
			}
			else {
				if (isAsaNull(year)) {
					return  MONTHS[month.intValue()-1] + " " + day.intValue();
				}
				else {
					return MONTHS[month.intValue()-1] + " " + day.intValue() + ", " + year.toString();
				}
			}
		}
	}
}
