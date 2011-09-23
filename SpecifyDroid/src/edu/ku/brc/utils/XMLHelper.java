/* Copyright (C) 2011, University of Kansas Center for Research
 * 
 * Specify Software Project, specify@ku.edu, Biodiversity Institute,
 * 1345 Jayhawk Boulevard, Lawrence, Kansas, 66045, USA
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package edu.ku.brc.utils;

import java.util.Properties;

import org.w3c.dom.Element;

/**
 * An XML Helper, this currently is for w3c DOM, probably won't need this since we will be switch to DOM4J.
 * 
 * (This Class needs to be moved to somewhere probably to the "ui" package.)
 *
 * @code_status Beta
 *
 * @author rods
 */
public class XMLHelper
{
    // Static Data Members
    private static boolean isEmptyAttrOK = false;

   /**
     * @param isEmptyAttrOK the isEmptyAttrOK to set
     */
    public static void setEmptyAttrOK(boolean isEmptyAttrOK)
    {
        XMLHelper.isEmptyAttrOK = isEmptyAttrOK;
    }

    public static boolean isNotEmpty(final String str)
    {
        return str != null && str.length() > 0;
    }
    
   /**
    * Get a string attribute value from an element value
    * @param element the element to get the attribute from
    * @param attrName the name of the attribute to get
    * @param defValue the default value if the attribute isn't there
    * @return the attr value or the default value
    */
   public static String getAttr(final Element element, final String attrName, final String defValue)
   {
       String str = element.getAttribute(attrName);
       return str != null ? str : defValue;
   }

   /**
    * Get a int attribute value from an element value
    * @param element the element to get the attribute from
    * @param attrName the name of the attribute to get
    * @param defValue the default value if the attribute isn't there
    * @return the attr value or the default value
    */
   public static int getAttr(final Element element, final String attrName, final int defValue)
   {
       String str = element.getAttribute(attrName);
       return isNotEmpty(str) ? Integer.parseInt(str) : defValue;
   }

   /**
    * Get a byte attribute value from an element value
    * @param element the element to get the attribute from
    * @param attrName the name of the attribute to get
    * @param defValue the default value if the attribute isn't there
    * @return the attr value or the default value
    */
   public static byte getAttr(final Element element, final String attrName, final byte defValue)
   {
       String str = element.getAttribute(attrName);
       return isNotEmpty(str) ? Byte.parseByte(str) : defValue;
   }

   /**
    * Get a short attribute value from an element value
    * @param element the element to get the attribute from
    * @param attrName the name of the attribute to get
    * @param defValue the default value if the attribute isn't there
    * @return the attr value or the default value
    */
   public static short getAttr(final Element element, final String attrName, final short defValue)
   {
       String str = element.getAttribute(attrName);
       return isNotEmpty(str) ? Short.parseShort(str) : defValue;
   }

   /**
    * Get a int attribute value from an element value
    * @param element the element to get the attribute from
    * @param attrName the name of the attribute to get
    * @param defValue the default value if the attribute isn't there
    * @return the attr value or the default value
    */
   public static boolean getAttr(final Element element, final String attrName, final boolean defValue)
   {
       String str = element.getAttribute(attrName);
       return isNotEmpty(str) ? Boolean.parseBoolean(str.toLowerCase()) : defValue;
   }
   
   /**
    * Get the value of the XML element with the given name that is a child of the given element.
    * 
    * @param element the parent XML element
    * @param name the name of the child element to get the value for
    * @return the data the child element's value
    */
   /*public static String getValue(final Element element, final String name)
   {
       Element node = (Element)element.selectSingleNode(name);
       if (node != null)
       {
           String data = node.getTextTrim();
           int inx = data.indexOf("("); //$NON-NLS-1$
           if (inx != -1)
           {
               int einx = data.indexOf(")"); //$NON-NLS-1$
               return data.substring(inx+1, einx);
           }
           return data;
       }
       
       // else
       // Although the name may not have been found it could be because no results came back
       log.debug("****** ["+name+"] was not found."); //$NON-NLS-1$ //$NON-NLS-2$
       return ""; //$NON-NLS-1$
   }*/
   
   
   
   public static void indent(final StringBuilder sb, final int width)
   {
       for (int i=0;i<width;i++)
       {
           sb.append(' ');
       }
   }
   
   public static void addAttr(final StringBuilder sb, final String name, final String value)
   {
       sb.append(' ');
       sb.append(name);
       sb.append("=\""); //$NON-NLS-1$
       sb.append(value == null ? "" : value); //$NON-NLS-1$
       sb.append('\"');
   }
   
   public static void addAttr(final StringBuilder sb, final String name, final Integer value)
   {
       sb.append(' ');
       sb.append(name);
       sb.append("=\""); //$NON-NLS-1$
       sb.append(value == null ? "" : value); //$NON-NLS-1$
       sb.append('\"');
   }
   
   public static void addAttr(final StringBuilder sb, final String name, final Short value)
   {
       sb.append(' ');
       sb.append(name);
       sb.append("=\""); //$NON-NLS-1$
       sb.append(value == null ? "" : value); //$NON-NLS-1$
       sb.append('\"');
   }
   
   public static void addAttr(final StringBuilder sb, final String name, final Boolean value)
   {
       sb.append(' ');
       sb.append(name);
       sb.append("=\""); //$NON-NLS-1$
       sb.append(value == null ? "" : value); //$NON-NLS-1$
       sb.append('\"');
   }
   
   public static void addAttr(final StringBuilder sb, final String name, final Byte value)
   {
       sb.append(' ');
       sb.append(name);
       sb.append("=\""); //$NON-NLS-1$
       sb.append(value == null ? "" : value); //$NON-NLS-1$
       sb.append('\"');
   }
   
   public static void addNode(final StringBuilder sb, final int indent, final String name, final boolean isEnd)
   {
       indent(sb, indent);
       sb.append('<');
       if (isEnd) sb.append('/');
       sb.append(name);
       sb.append('>');
       if (isEnd) sb.append('\n');
   }
   
   public static void addNode(final StringBuilder sb, final int indent, final String name, final String value)
   {
       addNode(sb, indent, name, false);
       sb.append(value);
       addNode(sb, 0, name, true);
   }
   public static void openNode(final StringBuilder sb, final int indent, final String name)
   {
       indent(sb, indent);
       sb.append('<');
       sb.append(name);
   }
   
   public static void closeNode(final StringBuilder sb, final int indent, final String name)
   {
       indent(sb, indent);
       sb.append('<');
       sb.append('/');
       sb.append(name);
       sb.append('>');
   }
  
   public static void closeNode(final StringBuilder sb)
   {
       sb.append('/');
       sb.append('>');
   }
   
   public static void xmlAttr(final StringBuilder sb, final String attr, final String val)
   {
       if (isNotEmpty(val) || isEmptyAttrOK)
       {
           sb.append(' ');
           sb.append(attr);
           sb.append("=\""); //$NON-NLS-1$
           sb.append(val);
           sb.append('\"');
       }
   }
   
   public static void xmlAttr(final StringBuilder sb, final String attr, final Integer val)
   {
       if (val != null || isEmptyAttrOK)
       {
           xmlAttr(sb, attr, val.toString());
       }
   }
   
   public static void xmlAttr(final StringBuilder sb, final String attr, final Short val)
   {
       if (val != null || isEmptyAttrOK)
       {
           xmlAttr(sb, attr, val.toString());
       }
   }
   
   public static void xmlAttr(final StringBuilder sb, final String attr, final Byte val)
   {
       if (val != null || isEmptyAttrOK)
       {
           xmlAttr(sb, attr, val.toString());
       }
   }
   
   public static void xmlAttr(final StringBuilder sb, final String attr, final Boolean val)
   {
       if (val != null || isEmptyAttrOK)
       {
           xmlAttr(sb, attr, val.toString());
       }
   }
   
   public static void xmlNode(final StringBuilder sb, final int indent, final String tag, final String val, final boolean useCData)
   {
       if (val != null || isEmptyAttrOK)
       {
           indent(sb, indent);
           sb.append("<"); //$NON-NLS-1$
           sb.append(tag);
           sb.append(">"); //$NON-NLS-1$
           if (useCData) sb.append("<![CDATA["); //$NON-NLS-1$
           sb.append(val);
           if (useCData) sb.append("]]>"); //$NON-NLS-1$
           sb.append("</"); //$NON-NLS-1$
           sb.append(tag);
           sb.append(">\n"); //$NON-NLS-1$
       }
   }
   
   public static void xmlProps(final StringBuilder sb, final Properties props)
   {
       if (props != null)
       {
           int i = 0;
           for (Object key : props.keySet())
           {
               if (i > 0) sb.append(';');
               sb.append(key.toString());
               sb.append("="); //$NON-NLS-1$
               sb.append(props.getProperty(key.toString()));
               i++;
           }
       }
   }
   
   /*public static void xmlAttr(final StringBuilder sb, final String attr, final Boolean val, final Boolean defaultVal)
   {
       if (val != null || (defaultVal != null && val.equals(defaultVal)))
       {
           xmlAttr(sb, attr, val);
       }
   }
   
   public static void xmlAttr(final StringBuilder sb, final String attr, final String val, final String defaultVal)
   {
       if (val != null || (defaultVal != null && val.equals(defaultVal)))
       {
           xmlAttr(sb, attr, val);
       }
   }
   
   public static void xmlAttr(final StringBuilder sb, final String attr, final Integer val, final Integer defaultVal)
   {
       if (val != null || (defaultVal != null && val.equals(defaultVal)))
       {
           xmlAttr(sb, attr, val);
       }
   }
   public static void xmlAttr(final StringBuilder sb, final String attr, final Integer val, final Short defaultVal)
   {
       if (val != null || (defaultVal != null && val.equals(defaultVal)))
       {
           xmlAttr(sb, attr, val);
       }
   }
   public static void xmlAttr(final StringBuilder sb, final String attr, final Byte val, final Byte defaultVal)
   {
       if (val != null || (defaultVal != null && val.equals(defaultVal)))
       {
           xmlAttr(sb, attr, val);
       }
   }*/


}
