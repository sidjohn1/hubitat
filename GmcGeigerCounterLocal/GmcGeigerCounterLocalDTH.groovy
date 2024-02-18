/*
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*  GMC Geiger Counter Local
*
*  Author: Sidney Johnson
*
*  Date: 2024-02-18
*
*	1.0 - Initial Release
*
*/

metadata {
	definition(
		name: "GMC Geiger Counter Local",
		description: "GMC Geiger Counter Local",
		namespace: "sidjohn1",
		author: "sidjohn1", 
		importUrl: "https://raw.githubusercontent.com/sidjohn1/hubitat/main/GmcGeigerCounterLocal/GmcGeigerCounterLocalDTH.groovy"){
		capability "Sensor"
		attribute "CPM", "NUMBER"                // Count Per Minute reading from this Geiger Counter
		attribute "ACPM", "NUMBER"               // Average Count Per Minute reading from this Geiger Counter
		attribute "uSv", "NUMBER"                // uSv/h reading from this Geiger Counter
        attribute "upload", "string"             // upload responce from gmcmap.com
        attribute "timestamp", "string"          // Last update
	}
}
