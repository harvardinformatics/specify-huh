/*
 * Created on 2011 July 13
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

package edu.harvard.huh.specify.teststuff;

import edu.ku.brc.helpers.Encryption;

public class MakeKey {
	public static void main(String args[]) {
		System.out.println(Encryption.encrypt("specify", "specify"));
		
		// System.out.println(Encryption.decrypt("AE4E7DD3C1193AB66AA80183F5436F98", "specify"));
		
		/* use the encrypted string generated to update the specifyuser tables:
		 * 
		 *     update specifyuser set password="AE4E7DD3C1193AB66AA80183F5436F98";
		 * 
		 * when logging in to specify after having performed the query, generate
		 * a new master key (using the db root user and password as master, and
		 * the user and newly encrypted password as the users name and password.

		 */
	}
}
