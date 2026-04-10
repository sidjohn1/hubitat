/**
*  Weather Panel for Apple Shortcuts
*
*  Copyright 2024 Sidney Johnson
*  If you like this code, please support the developer via PayPal: https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=XKDRYZ3RUNR9Y
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
*  Date: 2024-03-18
*
*	1.0 - Initial Release
*	1.1 - Added "Feels Like" for current temperature
*	1.2 - Added Rain Forcast as a possibility, requires a dedicated always on mac vm or ios device for screen scrapping the rain forcast
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
        
		attribute "currentCondition", "string"       // Current Weather Conditions
		attribute "currentPrecipAmount", "number"    // Current Precipitation Amount
        attribute "currentPrecipChance", "number"    // Current Precipitation Chance
        attribute "currentRainForcast", "string"     // Current Rain Forcast - not collected by default
        attribute "currentRainETA", "string"	     // Current Rain ETA - not collected by default
        attribute "forcastDay1Day", "string"         // Todays Day
        attribute "forcastDay1High", "number"        // Todays High Temp
        attribute "forcastDay1Low", "number"         // Todays Low Temp
        attribute "forcastDay1Condition", "string"   // Todays Condition
        attribute "forcastDay2Day", "string"         // Tomorrows Day
        attribute "forcastDay2High", "number"        // Tomorrows High Temp
        attribute "forcastDay2Low", "number"         // Tomorrows Low Temp
        attribute "forcastDay2Condition", "string"   // Tomorrows Condition
        attribute "forcastDay3Day", "string"         // Day After Tomorrows Day
        attribute "forcastDay3High", "number"        // Day After Tomorrows High Temp
        attribute "forcastDay3Low", "number"         // Day After Tomorrows Low Temp
        attribute "forcastDay3Condition", "string"   // Day After Tomorrows Condition
        attribute "forcastDay4Day", "string"         // Day After The Day After Tomorrows Day
        attribute "forcastDay4High", "number"        // Day After The Day After High Temp
        attribute "forcastDay4Low", "number"         // Day After The Day After Low Temp
        attribute "forcastDay4Condition", "string"   // Day After The Day After Condition
        attribute "temperatureFeelsLike", "number"   // Current "Feels Like" Temp
        attribute "timestamp", "number"              // Last update
	}
}
