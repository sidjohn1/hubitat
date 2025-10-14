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
*  Date: 2024-04-29
*
*	1.0 - Initial Release
*	1.1 - Added CPM_Display and CPM_Message
*
*/

metadata {
	definition(
		name: "GMC Geiger Counter Local Device", description: "GMC Geiger Counter Local", namespace: "sidjohn1", author: "sidjohn1", importUrl: "https://raw.githubusercontent.com/sidjohn1/hubitat/main/GmcGeigerCounterLocal/GmcGeigerCounterLocalDTH.groovy"){
		capability "Sensor"
		attribute "CPM", "NUMBER"                // Count Per Minute reading from this Geiger Counter
        attribute "CPM_Display", "string";       // CPM + short danger level
        attribute "CPM_Message", "string";       // Danger level + reccomedation
		attribute "ACPM", "NUMBER"               // Average Count Per Minute reading from this Geiger Counter
		attribute "uSv", "NUMBER"                // uSv/h reading from this Geiger Counter
        attribute "upload", "string"             // upload responce from gmcmap.com
        attribute "timestamp", "string"          // Last update
	}
}
// What about the time <sudo> for the apple car?
