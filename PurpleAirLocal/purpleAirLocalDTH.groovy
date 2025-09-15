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
*  Date: 2024-06-03
*
*  1.0 - Initial Release
*  1.1 - Corrected pressure reporting to mBar
*  1.2 - Added aqiDisplay, for dual laser model rounded AQI to a whole number after averaging, formatting tweaks, added temp and humidity adjustments, changed aqimessage to aqiMessage.
*  1.3 - Added voc, vocDisplay and vocMessage. AQI Clean up.
*  1.4 - Code clean up and refactoring.
*  1.5 - fix for aqiDisplay null value, reduced http connection timeout to 5 seconds
*
*/

import java.math.BigDecimal

metadata {
    definition(name: "PurpleAir AQI Local", namespace: "sidjohn1", author: "Sidney Johnson", importUrl: "https://raw.githubusercontent.com/sidjohn1/hubitat/main/PurpleAirLocal/purpleAirLocalDTH.groovy") {
        capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Air Quality"
        capability "Pressure Measurement"
        capability "Signal Strength"
        capability "Sensor"
        capability "Polling"

        attribute "pressure", "number"          // Current pressure in Millibars
        attribute "dewPoint", "number"          // °F
        attribute "aqi", "number"               // AQI (0-500)
        attribute "aqiDisplay", "string"        // AQI + short danger level
        attribute "aqiMessage", "string"        // AQI danger level
        attribute "pm01", "number"              // µg/m³ - PM1.0 particle reading - current
        attribute "pm25", "number"              // µg/m³ - PM2.5 particle reading - current
        attribute "pm10", "number"              // µg/m³ - PM10 particle reading - current
        attribute "voc", "number"               // IAQ - Index for Air Quality (0-500)
        attribute "vocDisplay", "string"        // IAQ + short danger level
        attribute "vocMessage", "string"        // IAQ danger level
        attribute "rssi", "string"              // Signal Strength attribute
        attribute "timestamp", "string"         //
    }
}

preferences {
    section("URIs") {
        input "ipAddress", "text", title: "Local IP Address", required: true
        input name: "realTime", type: "bool", title: "2 min average or Real Time", defaultValue: false
        input name: "updateMins", type: "enum", title: "Update frequency (minutes)", description: "Default: 5<br>Disabled: 0", defaultValue: '5', options: ['0', '1', '2', '5', '10', '15', '30'], required: true
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
    log.warn "debug logging is: ${logEnable}"
    realTime = realTime ?: false
    if (logEnable) runIn(1800, logsOff)
    if (updateMins != "0") {
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
    def params = [ uri: url, timeout: 5]
	// What about the time for the apple car?
    if (logEnable) log.debug url
    try {
        httpGet(params) { resp ->
            if (logEnable) log.debug resp.getData()
            if (resp?.status == 200) {
                processResponse(resp?.data)
            } else {
                log.error "Invalid response for PurpleAir request: ${resp}"
            }
        }
    } catch (Exception e) {
        log.error "Error occurred during httpGet: ${e.message}"
    }
}

def processResponse(data) {
    calibrateTempHum('temperature', data?.current_temp_f, settings.temperatureAdj, '°F', 'Temperature Calibrated')
    calibrateTempHum('humidity', data?.current_humidity, settings.humidityAdj, '%', 'Humidity Calibrated')
    sendEvent(name: 'dewPoint', value: data?.current_dewpoint_f, unit: '°F')
    sendEvent(name: 'pressure', value: data?.pressure, unit: 'mBar')
    processAQI(data)
    processPM(data, 'pm01', 'pm1_0_atm', 'pm1_0_atm_b')
    processPM(data, 'pm25', 'pm2_5_atm', 'pm2_5_atm_b')
    processPM(data, 'pm10', 'pm10_0_atm', 'pm10_0_atm_b')
    processVOC(data?.gas_680)
    sendEvent(name: 'rssi', value: data?.rssi, unit: 'db')
    sendEvent(name: 'timestamp', value: data?.DateTime, displayed: false)
}

def calibrateTempHum(name, value, adjustment, unit, logMessage) {
    if (adjustment) {
        def adjustedValue = (value.toInteger() + adjustment.toInteger())
        sendEvent(name: name, value: adjustedValue, unit: unit)
        if (logEnable) log.debug "${logMessage}: ${value} + ${adjustment} = ${adjustedValue}"
    } else {
        sendEvent(name: name, value: value, unit: unit)
    }
}

def processAQI(data) {
    def aqi = calculateAverage(data?."pm2.5_aqi", data?."pm2.5_aqi_b", 0)
    if (aqi != null) {
    	sendEvent(name: 'aqi', value: aqi)
    	def (display, message) = getAQIMessage(aqi)
    	sendEvent(name: 'aqiDisplay', value: display)
    	sendEvent(name: 'aqiMessage', value: message)
    }    
}

def processPM(data, name, value1, value2) {
    def pm = calculateAverage(data[value1], data[value2], 2)
    sendEvent(name: name, value: pm, unit: 'µg/m³')
}

def processVOC(voc) {
    if (voc) {
        sendEvent(name: 'voc', value: voc)
        def (display, message) = getVOCMessage(voc)
        sendEvent(name: 'vocDisplay', value: display)
        sendEvent(name: 'vocMessage', value: message)
    } else if (logEnable) {
        log.debug "VOC Not Detected"
    }
}

def calculateAverage(value1, value2, scale) {
    if (value1 && value2) {
        return ((value1.toBigDecimal() + value2.toBigDecimal()) / 2.0).setScale(scale, BigDecimal.ROUND_HALF_UP)
    } else if (value1) {
        return value1.toBigDecimal().setScale(scale, BigDecimal.ROUND_HALF_UP)
    }
    return null
}

def getAQIMessage(aqi) {
    if (aqi < 51) {
        return ["${aqi} - GOOD", "GOOD: little to no health risk"]
    } else if (aqi < 101) {
        return ["${aqi} - MODERATE", "MODERATE: slight risk for some people"]
    } else if (aqi < 151) {
        return ["${aqi} - UNHEALTHY", "UNHEALTHY: for sensitive groups"]
    } else if (aqi < 201) {
        return ["${aqi} - UNHEALTHY", "UNHEALTHY: for most people"]
    } else if (aqi < 301) {
        return ["${aqi} - VERY UNHEALTHY", "VERY UNHEALTHY: serious effects for everyone"]
    } else if (aqi < 501) {
        return ["${aqi} - HAZARDOUS", "HAZARDOUS: emergency conditions for everyone"]
    }
    return ["${aqi} - UNKNOWN", "UNKNOWN: invalid reading"]
}

def getVOCMessage(voc) {
    if (voc < 51) {
        return ["${voc} - EXCELLENT", "EXCELLENT: pure air, best for well-being"]
    } else if (voc < 101) {
        return ["${voc} - GOOD", "GOOD: no irritation or impact on well-being"]
    } else if (voc < 151) {
        return ["${voc} - LIGHTLY POLLUTED", "LIGHTLY POLLUTED: Ventilation suggested"]
    } else if (voc < 201) {
        return ["${voc} - MODERATELY POLLUTED", "MODERATELY POLLUTED: Ventilate with clean air"]
    } else if (voc < 251) {
        return ["${voc} - HEAVILY POLLUTED", "HEAVILY POLLUTED: Optimize ventilation"]
    } else if (voc < 351) {
        return ["${voc} - SEVERELY POLLUTED", "SEVERELY POLLUTED: Maximize ventilation & reduce attendance"]
    } else if (voc < 501) {
        return ["${voc} - EXTREMELY POLLUTED", "EXTREMELY POLLUTED: Maximize ventilation & avoid attendance"]
    }
    return ["${voc} - UNKNOWN", "UNKNOWN: invalid reading"]
}
