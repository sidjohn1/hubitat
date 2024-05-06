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
*  Date: 2024-05-03
*
*	1.0 - Initial Release
*	1.1 - Corrected preasure reporting to mBar
*	1.2 - Added aqiDisplay, for dual laser model rounded AQI to a whole number after averaging, formating tweaks, added temp and hudmidity ajustments, changed aqimessage to aqiMessage.
*   1.3 - Added voc, vocDisplay and vocMessage. AQI Clean up.
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
        attribute "aqiDisplay", "string";         // AQI + short danger level
        attribute "aqiMessage", "string";         // AQI danger level
        attribute "pm01", "number";               // µg/m³ - PM1.0 particle reading - current
        attribute "pm25", "number";               // µg/m³ - PM2.5 particle reading - current
        attribute "pm10", "number";               // µg/m³ - PM10 particle reading - current
        attribute "voc", "number";                // IAQ - Index for Air Quality (0-500)
        attribute "vocDisplay", "string";         // IAQ + short danger level
        attribute "vocMessage", "string";         // IAQ danger level
		attribute "rssi", "string";               // Signal Strength attribute
        attribute "timestamp", "string"           //
	}
}

preferences {
	section("URIs") {
		input "ipAddress", "text", title: "Local IP Address", required: true
		input name: "realTime", type: "bool", title: "2 min average or Real Time", defaultValue: false
		input name: "updateMins", type: "enum", title: "Update frequency (minutes)", description: "Default: 5<br>Disabled: 0", defaultValue: '5', options: ['0', '1', '2', '5','10','15','30'], required: true
        input name: "temperatureAdj", type: "number", title: "Temperature Calibration", description: "Adjust temperature up/down in °F [(-50)-(+50)]<br>Default: -8", range: "-50..50", defaultValue: -8, displayDuringSetup: true, required: true
        input name: "humidityAdj", type: "number", title: "Humidity Calibration", description: "Adjust humidity up/down in percent [(-50)-(+50)]<br>Default: 4", range: "-50..50", defaultValue: 4, displayDuringSetup: true, required: true
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
    def voc
    def adjustedTemperature
    def adjustedHumidity
	if (logEnable) log.debug "Device polling..."
	def url = "http://${ipAddress}/json?live=${realTime}"
    if (logEnable) log.debug url  
	try {
		httpGet(url) { resp -> 
			if (logEnable) log.debug resp.getData()           
       		if (resp && (resp.status == 200)) {
				if (settings.temperatureAdj) {
					adjustedTemperature = (resp?.data?."current_temp_f".toInteger() + settings.temperatureAdj.toInteger())
					sendEvent(name: "temperature", value: adjustedTemperature, unit: '°F')
					if (logEnable) log.debug "Temperature Calibrated: ${resp?.data?.current_temp_f} + ${settings.temperatureAdj} = ${adjustedTemperature}"
				}
				else {
					sendEvent(name: "temperature", value: resp?.data?.current_temp_f, unit: '°F')
				}
                    
				if (settings.humidityAdj) {
					adjustedHumidity = (resp?.data?."current_humidity".toInteger() + settings.humidityAdj.toInteger())
					sendEvent(name: "humidity", value: adjustedHumidity, unit: '%')
					if (logEnable) log.debug "Humidity Calibrated: ${resp?.data?.current_humidity} + ${settings.humidityAdj} = ${adjustedHumidity}"
				}
				else {
					sendEvent(name: "humidity", value: resp?.data?.current_humidity, unit: '%')
				}
				
				sendEvent(name: 'dewPoint', value: resp?.data?.current_dewpoint_f, unit: '°F')
				sendEvent(name: 'pressure', value: resp?.data?.pressure, unit: 'mBar')
				if (resp?.data?."pm2.5_aqi" != null && resp?.data?."pm2.5_aqi_b" != null) {
					aqi = ((resp?.data?."pm2.5_aqi".toBigDecimal() + resp?.data?."pm2.5_aqi_b".toBigDecimal()) / 2.0 ).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP)
					if (logEnable) log.debug "AQI Averaged: ${aqi}"
				}
				else if (resp?.data?."pm2.5_aqi" != null && resp?.data?."pm2.5_aqi_b" == null) {
					aqi = (resp?.data?."pm2.5_aqi".toBigDecimal())
					if (logEnable) log.debug "AQI Not Averaged: ${aqi}"
				}
				sendEvent(name: 'aqi', value: aqi)
				if 	(aqi < 51)  {
					sendEvent(name: 'aqiDisplay', value: "${aqi} - GOOD")
					sendEvent(name: 'aqiMessage', value: "GOOD: little to no health risk")
				}
				else if (aqi < 101) {
					sendEvent(name: 'aqiDisplay', value: "${aqi} - MODERATE")
					sendEvent(name: 'aqiMessage', value: "MODERATE: slight risk for some people")
				}
				else if (aqi < 151) {
					sendEvent(name: 'aqiDisplay', value: "${aqi} - UNHEALTHY")
					sendEvent(name: 'aqiMessage', value: "UNHEALTHY: for sensitive groups")
				}
				else if (aqi < 201) {
					sendEvent(name: 'aqiDisplay', value: "${aqi} - UNHEALTHY")
					sendEvent(name: 'aqiMessage', value: "UNHEALTHY: for most people")
				}
				else if (aqi < 301) {
					sendEvent(name: 'aqiDisplay', value: "${aqi} - VERY UNHEALTHY")
					sendEvent(name: 'aqiMessage', value: "VERY UNHEALTHY: serious effects for everyone")
				}
				else if (aqi < 501) {
					sendEvent(name: 'aqiDisplay', value: "${aqi} - HAZARDOUS")
					sendEvent(name: 'aqiMessage', value: "HAZARDOUS: emergency conditions for everyone")
				}
				else {
					sendEvent(name: 'aqiDisplay', value: "${aqi} - UNKNOWN")
					sendEvent(name: 'aqiMessage', value: "UNKNOWN: invalid reading")
				}
				
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

                if (resp?.data?."gas_680" != null) {
					voc = (resp?.data?."gas_680".toBigDecimal())
                    if (logEnable) log.debug "VOC Detected"
                    sendEvent(name: 'voc', value: voc)
                    if 	(voc < 51)  {
                        sendEvent(name: 'vocDisplay', value: "${voc} - EXCELLENT")
					    sendEvent(name: 'vocMessage', value: "EXCELLENT: pure air, best for well-being")
				    }
                    else if (voc < 101) {
					    sendEvent(name: 'vocDisplay', value: "${voc} - GOOD")
					    sendEvent(name: 'vocMessage', value: "GOOD: no irritation or impact on well-being")
				    }
                    else if (voc < 151) {
					    sendEvent(name: 'vocDisplay', value: "${voc} - LIGHLY POLLUTED")
					    sendEvent(name: 'vocMessage', value: "LIGHLY POLLUTED: Ventilation suggested")
				    }
                    else if (voc < 201) {
					    sendEvent(name: 'vocDisplay', value: "${voc} - MODERATELY POLLUTED")
					    sendEvent(name: 'vocMessage', value: "MODERATELY POLLUTED: Ventilate with clean air")
				    }
                    else if (voc < 251) {
					    sendEvent(name: 'vocDisplay', value: "${voc} - HEAVILY POLLUTED")
					    sendEvent(name: 'vocMessage', value: "HEAVILY POLLUTED: Optimize ventilation")
				    }
                    else if (voc < 351) {
					    sendEvent(name: 'vocDisplay', value: "${voc} - SEVERLY POLLUTED")
					    sendEvent(name: 'vocMessage', value: "SEVERLY POLLUTED: Maximize ventilation & reduce attendance")
				    }
                    else if (voc < 501) {
					    sendEvent(name: 'vocDisplay', value: "${voc} - EXTREMELY POLLUTED")
					    sendEvent(name: 'vocMessage', value: "EXTREMELY POLLUTED: Maximize ventilation & avoid attendance")
				    }
                    else {
					    sendEvent(name: 'vocDisplay', value: "${voc} - UNKNOWN")
					    sendEvent(name: 'vocMessage', value: "UNKNOWN: invalid reading")
				    }

				}
                else if (logEnable) log.debug "VOC Not Detected"

				sendEvent(name: 'rssi', value: resp?.data?.rssi, unit: 'db')
				sendEvent(name: 'timestamp', value: resp?.data?.DateTime, displayed: false)
			}
			else {
				log.error "Invalid response for PurpleAir request: ${resp}"
			}
		}
	}
	catch(Exception e) {
		log.debug "error occured calling httpget ${e}"
	}
}
