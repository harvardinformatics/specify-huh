/**
 * Copyright 2010 Terrestrial Ecosystem Research Network, licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or
 * agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.harvard.huh.oai.provider.crosswalk;

import dwc.huh_harvard_edu.tdwg_dwc_simple.SimpleDarwinRecord;

public interface Crosswalk {

	public SimpleDarwinRecord crosswalk(Object nativeObject);
	
	public String crosswalkToString(Object nativeObject);

	public String getDatestamp(Object nativeItem);
	
}
