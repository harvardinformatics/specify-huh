/**
 * 
 */
package edu.ku.brc.specify.tasks.subpane.qb;

import java.util.Calendar;

/**
 * @author Administrator
 *
 *Formats dates for export to MySQL
 */
public class CalendarExportFormatter extends ExportFieldFormatter
{
	/* (non-Javadoc)
	 * @see edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace#formatToUI(java.lang.Object[])
	 */
	@Override
	public Object formatToUI(Object... data)
	{
		if (data[0] == null)
		{
			return null;
		}
		Calendar calendar = (Calendar )data[0];
		int num = calendar.get(Calendar.MONTH)+1;
		String monStr = String.valueOf(num);
		if (num < 10)
		{
			monStr = "0" + monStr; 
		}
		num = Calendar.DAY_OF_MONTH;
		String numStr = String.valueOf(num);
		if (num < 10)
		{
			numStr = "0" + numStr;
		}
		return calendar.get(Calendar.YEAR) + "-" + monStr + "-" + numStr;
	}
}