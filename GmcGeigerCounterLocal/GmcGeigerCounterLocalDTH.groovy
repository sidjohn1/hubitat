/*
 * GMC Geiger Counter Local
 */

metadata {
	definition(name: "GMC Geiger Counter Local", namespace: "sidjohn1", author: "sidjohn1", importUrl: "https://raw.githubusercontent.com/staze/hubitat-gmcmap/master/hubitat-gmcmap.groovy") {
		capability "Sensor"
		attribute "CPM", "NUMBER"                // Count Per Minute reading from this Geiger Counter
		attribute "ACPM", "NUMBER"               // Average Count Per Minute reading from this Geiger Counter
		attribute "uSv", "NUMBER"                // uSv/h reading from this Geiger Counter
        attribute "upload", "string"             // upload responce from gmcmap.com
        attribute "timestamp", "string"          // Last update
	}
}
