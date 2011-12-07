/*
 * Created on Dec 07, 2011
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

package edu.harvard.huh.specify.tests;


import java.util.LinkedList;
import java.util.Queue;

public class TestParameter {
	private Queue<String> paramStrings = new LinkedList<String>();
	
	public TestParameter(String propString) {
		for (String s : propString.split(",")) {
			s.trim();
			paramStrings.offer(s);
		}
	}
	
	public String getParam() {
		return paramStrings.poll();
	}
	
	public boolean hasNext() {
		return !paramStrings.isEmpty();
	}
}
