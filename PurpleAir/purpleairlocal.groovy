/**
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
*  PurpleAir AQI Local
*
*  Author: Sidney Johnson
*
*  Date: 2023-01-08
*
*	1.0.00 - Initial Release
*
*/

metadata {
	definition(name: "PurpleAir AQI Local", namespace: "sidjohn1", author: "Sidney Johnson", importUrl: "https://raw.githubusercontent.com/sidjohn1/hubitat/main/PurpleAir/purpleairlocal.groovy") {
        capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Signal Strength"
        capability "Sensor"
        capability "Polling"

        attribute "pressure", "number"
        attribute "aqi", "number"				// current AQI
	attribute "rssi", "string"				// Signal Strength attribute (not supporting lqi)
        attribute 'message', 'string'
        attribute "timestamp", "string"
	}
}

preferences {
	section("URIs") {
		input "ipAddress", "text", title: "Local IP Address", required: true
		input name: "realTime", type: "bool", title: "2m average or Real Time", defaultValue: false
		input name: 'updateMins', type: 'enum', description: "Select the update frequency", title: "Update frequency (minutes)\n0 is disabled", defaultValue: '5', options: ['0', '1', '2', '5','10','15','30'], required: true
		input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
	}
}

def logsOff() {
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def updated() {
	unschedule()
	log.info "Device updated..."
	log.warn "debug logging is: ${logEnable == true}"
	if (realTime == null) {
		realTime = false
	}
	if (logEnable) runIn(1800, logsOff)
	if(updateMins != "0") {
        	Random rand = new Random()
        	int randomSeconds = rand.nextInt(49)
		schedule("${randomSeconds} */${updateMins} * ? * *", poll)
	}
	
}

def parse(String description) {
	if (logEnable) log.debug(description)
}

def poll() {
	if (logEnable) log.debug "Device polling..."
	def url = "http://${ipAddress}/json?live=${realTime}"
    if (logEnable) log.debug url  
	try {
		httpGet(url) { resp -> 
			if (logEnable) log.debug resp.getData()           
                if (resp && (resp.status == 200)) {
			        sendEvent(name: "temperature", value: resp?.data?.current_temp_f, unit: 'F')
			        sendEvent(name: 'humidity', value: resp?.data?.current_humidity, unit: '%')
			        sendEvent(name: 'pressure', value: resp?.data?.pressure, unit: 'inHg')
			        sendEvent(name: 'aqi', value: resp?.data?."pm2.5_aqi")
            
			        if 		(aqi < 51)  {sendEvent(name: 'message', value: "GOOD: little to no health risk");}
			        else if (aqi < 101) {sendEvent(name: 'message', value: "MODERATE: slight risk for some people");}
			        else if (aqi < 151) {sendEvent(name: 'message', value: "UNHEALTHY for sensitive groups");}
			        else if (aqi < 201) {sendEvent(name: 'message', value: "UNHEALTHY for most people");}
			        else if (aqi < 301) {sendEvent(name: 'message', value: "VERY UNHEALTHY: serious effects for everyone");}
			        else 				{sendEvent(name: 'message', value: "HAZARDOUS: emergency conditions for everyone");}
            
			        sendEvent(name: 'rssi', value: resp?.data?.rssi, unit: 'db')
			        sendEvent(name: 'timestamp', value: resp?.data?.DateTime, displayed: false)
                }
            else {
                log.error "Invalid response for PurpleAir request: ${resp}"
            }
		}
	} catch(Exception e) {
		log.debug "error occured calling httpget ${e}"
	}
}
