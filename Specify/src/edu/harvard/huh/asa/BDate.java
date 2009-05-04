package edu.harvard.huh.asa;

import java.text.MessageFormat;

public class BDate {
	
	private int id;
	
	private Integer startYear;
	private Integer startMonth;
	private Integer startDay;
	private String startPrecision;
	private Integer endYear;
	private Integer endMonth;
	private Integer endDay;
	private String endPrecision;
	private String text;
	
	public BDate() { ; }
	
	public int getId() { return id; }
	
	public Integer getStartYear() { return startYear; }
	
	public Integer getStartMonth() { return startMonth; }
	
	public Integer getStartDay() { return startDay; }
	
	public String getStartPrecision() { return startPrecision; }
	
	public Integer getEndYear() { return endYear; }
	
	public Integer getEndMonth() { return endMonth; }
	
	public Integer getEndDay() { return endDay; }
	
	public String getEndPrecision() { return endPrecision; }
	
	public String getText() { return text; }
	
	public void setId(int id) { this.id = id; }
		
	public void setStartYear(Integer startYear) { this.startYear = startYear; }
	
	public void setStartMonth(Integer startMonth) { this.startMonth = startMonth; }
	
	public void setStartDay(Integer startDay) { this.startDay = startDay; }
	
	public void setStartPrecision(String startPrecision) { this.startPrecision = startPrecision; }
	
	public void setEndYear(Integer endYear) { this.endYear = endYear; }
	
	public void setEndMonth(Integer endMonth) { this.endMonth = endMonth; }
	
	public void setEndDay(Integer endDay) { this.endDay = endDay; }
	
	public void setEndPrecision(String endPrecision) { this.endPrecision = endPrecision; }
	
	public void setText(String text) { this.text = text; }
		
	public String toString() {
		String month1 = startMonth == null ? "NULL" : startMonth.toString();
		
		String day1 = startDay == null ? "NULL" : startDay.toString();
		
		String year1 = startYear == null ? "NULL" : startYear.toString();
		
		String month2 = endMonth == null ? "NULL" : endMonth.toString();
		
		String day2 = endDay == null ? "NULL" : endDay.toString();
		
		String year2 = endYear == null ? "NULL" : endYear.toString();
		
		String txt = text == null ? "" : "// " + text;
		
		String format = "{0}: {1} {2} {3} {4} -> {5} {6} {7} {8} {9}";
		Object[] args = { String.valueOf(id), year1, month1, day1, startPrecision.toString(), year2, month2, day2, endPrecision.toString(), txt };

		return MessageFormat.format(format, args);
	}
}
