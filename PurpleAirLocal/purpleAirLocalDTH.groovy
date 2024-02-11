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
*  Date: 2024-01-08
*
*	1.0 - Initial Release
*
*/
import java.math.BigDecimal

metadata {
	definition(name: "PurpleAir AQI Local", namespace: "sidjohn1", author: "Sidney Johnson", importUrl: "https://raw.githubusercontent.com/sidjohn1/hubitat/main/PurpleAirLocal/purpleAirLocalDTH.groovy") {
        capability "Temperature Measurement";
        capability "Relative Humidity Measurement";
        capability "Air Quality";
        capability "Pressure Measurement";
        capability "Signal Strength";
        capability "Sensor";
        capability "Polling"

        attribute "pressure", "number";           // Current pressure in Millibars
        attribute "dewPoint", "number";           // °F
        attribute "aqi", "number";                // AQI (0-500)
        attribute 'aqimessage', 'string';         // AQI danger level
        attribute "pm01", "number";               // µg/m³ - PM1.0 particle reading - current
        attribute "pm25", "number";               // µg/m³ - PM2.5 particle reading - current
        attribute "pm10", "number";               // µg/m³ - PM10 particle reading - current
	attribute "rssi", "string";               // Signal Strength attribute
        attribute "timestamp", "string"           //
	}
}

preferences {
	section("URIs") {
		input "ipAddress", "text", title: "Local IP Address", required: true
		input name: "realTime", type: "bool", title: "2 min average or Real Time", defaultValue: false
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
	def aqi
	def pm01
	def pm25
	def pm10
	if (logEnable) log.debug "Device polling..."
	def url = "http://${ipAddress}/json?live=${realTime}"
    if (logEnable) log.debug url  
	try {
		httpGet(url) { resp -> 
			if (logEnable) log.debug resp.getData()           
        		if (resp && (resp.status == 200)) {
				sendEvent(name: "temperature", value: resp?.data?.current_temp_f, unit: '°F')
				sendEvent(name: 'humidity', value: resp?.data?.current_humidity, unit: '%')
				sendEvent(name: 'dewPoint', value: resp?.data?.current_dewpoint_f, unit: '°F')
              			sendEvent(name: 'pressure', value: resp?.data?.pressure, unit: 'inHg')

				if (resp?.data?."pm2.5_aqi" != null && resp?.data?."pm2.5_aqi_b" != null) {
                  			aqi = ((resp?.data?."pm2.5_aqi".toBigDecimal() + resp?.data?."pm2.5_aqi_b".toBigDecimal()) / 2.0 ).toBigDecimal().setScale(2, BigDecimal.ROUND_HALF_UP)
                  			if (logEnable) log.debug "AQI Averaged: ${aqi}"
              			}
              			else if (resp?.data?."pm2.5_aqi" != null && resp?.data?."pm2.5_aqi_b" == null) {
                  			aqi = (resp?.data?."pm2.5_aqi".toBigDecimal())
                 			if (logEnable) log.debug "AQI Not Averaged: ${aqi}"
              			}
				sendEvent(name: 'aqi', value: aqi)
            
			        if 	(aqi < 51)  {sendEvent(name: 'aqimessage', value: "GOOD: little to no health risk");}
			        else if (aqi < 101) {sendEvent(name: 'aqimessage', value: "MODERATE: slight risk for some people");}
			        else if (aqi < 151) {sendEvent(name: 'aqimessage', value: "UNHEALTHY: for sensitive groups");}
			        else if (aqi < 201) {sendEvent(name: 'aqimessage', value: "UNHEALTHY: for most people");}
			        else if (aqi < 301) {sendEvent(name: 'aqimessage', value: "VERY UNHEALTHY: serious effects for everyone");}
              			else if (aqi < 401) {sendEvent(name: 'aqimessage', value: "HAZARDOUS: emergency conditions for everyone");}
			        else {sendEvent(name: 'aqimessage', value: "HAZARDOUS: emergency conditions for everyone");}

              			if (resp?.data?."pm1_0_atm" != null && resp?.data?."pm1_0_atm_b" != null) {
                  			pm01 = ((resp?.data?."pm1_0_atm".toBigDecimal() + resp?.data?."pm1_0_atm_b".toBigDecimal()) / 2.0 ).toBigDecimal().setScale(2, BigDecimal.ROUND_HALF_UP)
                  			if (logEnable) log.debug "PM01 Averaged: ${pm01}"
              			}
              			else if (resp?.data?."pm1_0_atm" != null && resp?.data?."pm1_0_atm_b" == null) {
                  			pm01 = (resp?.data?."pm1_0_atm".toBigDecimal())
                 			if (logEnable) log.debug "PM01 Not Averaged: ${pm01}"
              			}
              			sendEvent(name: 'pm01', value: pm01, unit: 'µg/m³')
                    
              			if (resp?.data?."pm2_5_atm" != null && resp?.data?."pm2_5_atm_b" != null) {
                  			pm25 = ((resp?.data?."pm2_5_atm".toBigDecimal() + resp?.data?."pm2_5_atm_b".toBigDecimal()) / 2.0 ).toBigDecimal().setScale(2, BigDecimal.ROUND_HALF_UP)
                  			if (logEnable) log.debug "PM25 Averaged: ${pm25}"
              			}
              			else if (resp?.data?."pm2_5_atm" != null && resp?.data?."pm2_5_atm_b" == null) {
                  			pm25 = (resp?.data?."pm2_5_atm".toBigDecimal())
                  			if (logEnable) log.debug "PM25 Not Averaged: ${pm25}"
              			}
              			sendEvent(name: 'pm25', value: pm25, unit: 'µg/m³')
                    
              			if (resp?.data?."pm10_0_atm" != null && resp?.data?."pm10_0_atm_b" != null) {
                  			pm10 = ((resp?.data?."pm10_0_atm".toBigDecimal() + resp?.data?."pm10_0_atm_b".toBigDecimal()) / 2.0 ).toBigDecimal().setScale(2, BigDecimal.ROUND_HALF_UP)
                  			if (logEnable) log.debug "PM10 Averaged: ${pm10}"
              			}
              			else if (resp?.data?."pm10_0_atm" != null && resp?.data?."pm10_0_atm_b" == null) {
                  			pm10 = (resp?.data?."pm10_0_atm".toBigDecimal())
                  			if (logEnable) log.debug "PM10 Not Averaged: ${pm10}"
              			}
              			sendEvent(name: 'pm10', value: pm10, unit: 'µg/m³')
                        
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
