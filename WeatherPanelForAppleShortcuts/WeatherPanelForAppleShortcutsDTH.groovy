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
*  Weather Panel For Apple Shortcuts Local Device
*
*  Author: Sidney Johnson
*
*  Date: 2024-03-10
*
*	1.0 - Initial Release
*
*/

metadata {
	definition(
		name: "Weather Panel For Apple Shortcuts Local Device",
		description: "Weather Panel For Apple Shortcuts",
		namespace: "sidjohn1",
		author: "sidjohn1", 
		importUrl: "https://raw.githubusercontent.com/sidjohn1/hubitat/main/WeatherPanelForAppleShortcuts/WeatherPanelForAppleShortcutsDTH.groovy"){
        capability "AirQuality"
		capability "RelativeHumidityMeasurement"
        capability "Sensor"
        capability "TemperatureMeasurement"
        
		attribute "currentCondition", "string"       // Average Count Per Minute reading from this Geiger Counter
		attribute "currentPrecipAmount", "number"    // uSv/h reading from this Geiger Counter
        attribute "currentPrecipChance", "number"    // upload responce from gmcmap.com
        attribute "forcastDay1Day", "string"         // upload responce from gmcmap.com
        attribute "forcastDay1High", "number"        // upload responce from gmcmap.com
        attribute "forcastDay1Low", "number"         // upload responce from gmcmap.com
        attribute "forcastDay1Condition", "string"   // upload responce from gmcmap.com
        attribute "forcastDay2Day", "string"         // upload responce from gmcmap.com
        attribute "forcastDay2High", "number"        // upload responce from gmcmap.com
        attribute "forcastDay2Low", "number"         // upload responce from gmcmap.com
        attribute "forcastDay2Condition", "string"   // upload responce from gmcmap.com
        attribute "forcastDay3Day", "string"         // upload responce from gmcmap.com
        attribute "forcastDay3High", "number"        // upload responce from gmcmap.com
        attribute "forcastDay3Low", "number"         // upload responce from gmcmap.com
        attribute "forcastDay3Condition", "string"   // upload responce from gmcmap.com
        attribute "forcastDay4Day", "string"         // upload responce from gmcmap.com
        attribute "forcastDay4High", "number"        // upload responce from gmcmap.com
        attribute "forcastDay4Low", "number"         // upload responce from gmcmap.com
        attribute "forcastDay4Condition", "string"   // upload responce from gmcmap.com
        attribute "timestamp", "number"              // Last update
	}
}
