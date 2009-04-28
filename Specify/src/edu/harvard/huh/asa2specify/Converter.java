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
import java.util.List;

public abstract class Converter
{
    protected static final String SqlDateFormat = "%tY-%tm-%td";

    protected String iso8859toUtf8( String string ) {
        if ( string != null ) {
            try {
                return new String( string.getBytes("ISO-8859-1"), "UTF-8" );
            }
            catch ( UnsupportedEncodingException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }

    protected String sqlEscape(String string, char c)
    {
        if (string == null) return "null";

        String chr = String.valueOf(c);
        switch (c)
        {
            case '\'' :
                return '\'' + string.replaceAll(chr, "\\\\" + chr) + '\'';
            case '"'  :
                return '"' + string.replaceAll(chr, "\\\\" + chr) + '"';
            default :
                throw new IllegalArgumentException("Can't escape '" + c + "'");
        }
    }
    
    protected String getInsertSql(String tableName, List<String> fieldNames, List<String> values) {
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
}
