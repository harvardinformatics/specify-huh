/* Copyright (C) 2009, University of Kansas Center for Research
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
package edu.ku.brc.specify.ui;

import javax.swing.ImageIcon;

/**
 * An interface defining the basic capabilities of classes that map various objects
 * to {@link ImageIcon}s.
 *
 * @author jstewart
 */
public interface ObjectIconMapper
{
    /**
     * Returns an ImageIcon representing the given Object.
     *
     * @param o any object
     * @return an ImageIcon representing the Object argument
     */
    public ImageIcon getIcon(Object o);
    
    /**
     * Returns an array of the classes mapped by this ObjectIconMapper.
     *
     * @return an array containing the mapped classes
     */
    public Class<?>[] getMappedClasses();
}
