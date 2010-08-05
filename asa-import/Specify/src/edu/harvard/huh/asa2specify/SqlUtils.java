/* This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.harvard.huh.asa2specify;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class SqlUtils
{
    public static final String SqlDateFormat = "yyyy-MM-dd hh:mm:ss";
    
    private static final SimpleDateFormat formatter = new SimpleDateFormat(SqlDateFormat);
    
    public static String getInsertSql(String tableName, String fieldNameList, String[] values) {
        StringBuilder sb = new StringBuilder();

        sb.append( "insert into " );
        sb.append( tableName );
        sb.append( "(" );
        sb.append( fieldNameList );
        sb.append( ") values (" );

        for ( String value : values ) {
            sb.append( value );
            sb.append( ", " );
        }
        sb.delete( sb.length() - 2, sb.length() );

        sb.append( ")" );

        return sb.toString();
    }
    
    public static String getQueryIdByFieldSql(String tableName, String idFieldName, String fieldName, String value)
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append("select ");
        sb.append(idFieldName);
        sb.append(" from ");
        sb.append(tableName);
        sb.append(" where ");
        sb.append(fieldName);
        sb.append("=");
        sb.append(SqlUtils.sqlString(value));
        
        return sb.toString();
    }
    
    public static String getQueryRankByIdSql(String tableName, String idFieldName, Integer id)
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append("select RankID from ");
        sb.append(tableName);
        sb.append(" where ");
        sb.append(idFieldName);
        sb.append("=");
        sb.append(String.valueOf(id));
        
        return sb.toString();
    }
    
    public static String getUpdateSql(String tableName, String setField, String setValue, String whereField, String whereValue) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("update ");
        sb.append(tableName);
        sb.append(" set ");
        sb.append(setField);
        sb.append("=");
        sb.append(setValue);
        sb.append(" where ");
        sb.append(whereField);
        sb.append("=");
        sb.append(SqlUtils.sqlString(whereValue));

        return sb.toString();
    }

    public static String getUpdateSql(String tableName, String[] fields, String[] values, String whereField, String whereValue) {
        StringBuilder sb = new StringBuilder();

        sb.append( "update " );
        sb.append( tableName );
        sb.append( " set " );


        for ( int i = 0; i < fields.length; i++ ) {
            sb.append( fields[ i ] );
            sb.append( "= " );
            sb.append( values[ i ] );
            sb.append( ", " );
        }
        sb.delete( sb.length() - 2, sb.length() );

        sb.append(" where ");
        sb.append(whereField);
        sb.append("=");
        sb.append(whereValue);

        return sb.toString();
    }
    
    public static String getUpdateSql(String tableName, String[] fields, String[] values, String whereField, Integer whereValue) {
    	return getUpdateSql(tableName, fields, values, whereField, String.valueOf(whereValue));
    }
    
    public static String sqlEscape(String string, char c) {
        if (string == null) return null;

        String chr = String.valueOf(c);
        
        string = string.replaceAll("\\\\", "\\\\\\\\");

        switch (c)
        {
            case '\'' :
            case '"'  :
                return string.replaceAll(chr, "\\\\" + chr);
            default : throw new IllegalArgumentException("Can't escape '" + c + "'");
        }
    }
    
    public static String sqlEscape(String string) {
        if (string == null) return null;
        return sqlEscape(string, '"');
    }
    
    public static String addressOrdinal(Integer agentId)
    {
        return "(select count(ad.AddressID)+1 from address ad where ad.AgentID=" + String.valueOf(agentId) + ")";
    }

    public static String now() {
        return "now()";
    }

    public static String one() {
        return "1";
    }

    public static String sqlString(String s) {
        if (s == null) return "null";
        
        return "\"" + sqlEscape(s) + "\"";
    }

    public static String sqlString(Boolean b) {
        if (b == null) return "null";
        return String.valueOf(b);
    }
    
    public static String sqlString(Byte b) {
        if (b == null) return "null";
        return String.valueOf(b);
    }
    
    public static String sqlString(Short s) {
        if (s == null) return "null";
        return String.valueOf(s);
    }
    
    public static String sqlString(Integer i) {
        if (i == null) return "null";
        return String.valueOf(i);
    }
    
    public static String sqlString(Float f) {
        if (f == null) return "null";
        return String.valueOf(f);
    }
    
    public static String sqlString(Double d) {
        if (d == null) return "null";
        return String.valueOf(d);
    }
    
    public static String sqlString(Calendar c) {
        if (c == null) return "null";
        return "\"" + formatter.format(c.getTime()) + "\"";
    }

    public static String sqlString(Timestamp t) {
        if (t == null) return "null";
        return "\"" + formatter.format(t.getTime()) + "\"";
    }
    
    public static Date parseDate(String s) throws LocalException
    {
        if (s == null) return null;
        
        Date date = null;
        try
        {
            date = formatter.parse(s);
        }
        catch (ParseException e)
        {
            throw new LocalException("Couldn't parse date", e);
        }
        return date;
    }
    
    public static Integer parseInt(String s) throws LocalException
    {
        if (s == null) return null;
        
        Integer integer = null;
        try
        {
            integer = Integer.parseInt(s);
        }
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse integer", e);
        }
        return integer;
    }
    
    public static BigDecimal parseBigDecimal(String s) throws LocalException
    {
        if (s == null) return null;
        
        Double dbl = null;
        try {
            dbl = Double.parseDouble(s);
        }
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse double", e);
        }
        return BigDecimal.valueOf(dbl);
    }
    
    public static Float parseFloat(String s) throws LocalException
    {
        if (s == null) return null;
        
        Float f = null;
        try {
            f = Float.parseFloat(s);
        }
        catch (NumberFormatException e)
        {
            throw new LocalException("Couldn't parse double", e);
        }
        return f;
    }

    public static String iso8859toUtf8( String string ) throws LocalException
    {
        return string;

        /*if (string != null) {
            try
            {
                return new String(string.getBytes("ISO-8859-1"), "UTF-8");
            }
            catch ( UnsupportedEncodingException e )
            {
                throw new LocalException("Couldn't convert string from iso8859 to utf8", e);
            }
        }
        return null;*/
    }
}
