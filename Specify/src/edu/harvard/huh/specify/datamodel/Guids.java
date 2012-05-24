/* Guids.java
 * 
 * Copyright (C) 2012, President and Fellows of Harvard College
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
package edu.harvard.huh.specify.datamodel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;


/**
 * Hibernate annotated PDO for the HUH guids table.
 * 
 * 
 * @author mole
 *
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true, dynamicUpdate=true)
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "guids")
public class Guids implements java.io.Serializable {

	private int guidid;
	private String uuid;
	private String tablename;
	private int primarykey;
	private String state;
	
	/**
	 * primary key
	 * 
	 * @return the guidid
	 */
    @Id
    @GeneratedValue
    @Column(name = "guidid")
	public int getGuidid() {
		return guidid;
	}
	/**
	 * @param guidid the guidid to set
	 */
	public void setGuidid(int guidid) {
		this.guidid = guidid;
	}
	/**
	 * @return the uuid
	 */
    @Column(name = "uuid", nullable = false)
	public String getUuid() {
		return uuid;
	}
	/**
	 * @param uuid the uuid to set
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	/**
	 * @return the tablename
	 */
    @Column(name = "tablename", nullable = false)
	public String getTablename() {
		return tablename;
	}
	/**
	 * @param tablename the tablename to set
	 */
	public void setTablename(String tablename) {
		this.tablename = tablename;
	}
	/**
	 * @return the primarykey
	 */
    @Column(name = "primarykey", nullable = false)
	public int getPrimarykey() {
		return primarykey;
	}
	/**
	 * @param primarykey the primarykey to set
	 */
	public void setPrimarykey(int primarykey) {
		this.primarykey = primarykey;
	}
	/**
	 * @return the state
	 */
    @Column(name = "state", nullable = true)
	public String getState() {
		return state;
	}
	/**
	 * @param state the state to set
	 */
	public void setState(String state) {
		this.state = state;
	} 
	
}
