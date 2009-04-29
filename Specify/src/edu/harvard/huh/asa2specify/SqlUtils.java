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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class SqlUtils
{
    public static final String SqlDateFormat = "yyyy-mm-dd";
    
    private static final SimpleDateFormat formatter = new SimpleDateFormat(SqlDateFormat);

    public static String getInsertSql(String tableName, List<String> fieldNames, List<String> values) {
        StringBuilder sb = new StringBuilder();

        sb.append( "insert into " );
        sb.append( tableName );
        sb.append( "(" );

        for ( String fieldName : fieldNames ) {
            sb.append( fieldName );
            sb.append( ", " );
        }
        sb.delete( sb.length() - 2, sb.length() );

        sb.append( ") values (" );

        for ( String value : values ) {
            sb.append( value );
            sb.append( ", " );
        }
        sb.delete( sb.length() - 2, sb.length() );

        sb.append( ")" );

        return sb.toString();
    }
    
    public static String getInsertSql(String tableName, String fieldNameList, List<String> values) {
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
        sb.append(value);
        
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
        sb.append(whereValue);

        return sb.toString();
    }

    public static String sqlEscape(String string, char c) {
        if (string == null) return null;

        String chr = String.valueOf(c);
        
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

    public static String sqlString(String s) {
        if (s == null) return "null";
        
        return "\"" + sqlEscape(s) + "\"";
    }

    public static String sqlString(Byte b) {
        return "\"" + String.valueOf(b) + "\"";
    }
    
    public static String sqlString(Integer i) {
        return "\"" + String.valueOf(i) + "\"";
    }
    
    public static String sqlString(Calendar c) {
        return "\"" + formatter.format(c.getTime()) + "\"";
    }

    public static String iso8859toUtf8( String string ) throws LocalException
    {
        if (string != null) {
            try
            {
                return new String(string.getBytes("ISO-8859-1"), "UTF-8");
            }
            catch ( UnsupportedEncodingException e )
            {
                throw new LocalException("Couldn't convert string from iso8859 to utf8", e);
            }
        }
        return null;
    }
}
