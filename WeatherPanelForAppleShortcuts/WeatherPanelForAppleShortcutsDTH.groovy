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
*  Forcast Attribute set
*
*  Author: Sidney Johnson
*
*  Date: 2024-03-09
*
*	1.0 - Initial Release
*
*/

metadata {
    definition (name: "Forcast Attribute set", namespace: "sidjohn1", author: "Sidney Johnson") {
        capability "Sensor"
		
        attribute "currentPrecipAmount", "number"
	    attribute "currentPrecipChance", "number"
        attribute "zforecast_1", "string"
	    attribute "zforecast_2", "string"
        attribute "zforecast_3", "string"
	    attribute "zforecast_4", "string"
        attribute "zforecast_5", "string"
	    attribute "zforecast_6", "string"
        attribute "zforecast_7", "string"
	    attribute "zforecast_8", "string"
        attribute "zforecast_9", "string"
	    attribute "zforecast_10", "string"
        attribute "textDescription", "string"
        attribute "todaysHigh", "number"
        attribute "todaysLow", "number"

        command "setZforecast_1", ["string"]
        command "setZforecast_2", ["string"]
        command "setZforecast_3", ["string"]
        command "setZforecast_4", ["string"]
        command "setZforecast_5", ["string"]
        command "setZforecast_6", ["string"]
        command "setZforecast_7", ["string"]
        command "setZforecast_8", ["string"]
        command "setZforecast_9", ["string"]
        command "setZforecast_10", ["string"]
        command "setTextDescription", ["string"]
    }   
}

preferences {
	input("debugEnable", "bool", title: "Enable debug logging?")
}
def setTextDescription(val) {
    val2 = val.split(':')
	sendEvent(name: "textDescription", value: val2[0])
    sendEvent(name: "currentPrecipAmount", value: val2[1])
    sendEvent(name: "currentPrecipChance", value: val2[2])
    if(debugEnable) log.debug "set textDescription = $val"
}
def setZforecast_1(val) {
	sendEvent(name: "zforecast_1", value: val)
    val2 = val.split(':')
    sendEvent(name: "todaysHigh", value: val2[1].substring(0, val2[1].length() - 2))
    sendEvent(name: "todaysLow", value: val2[2].substring(0, val2[2].length() - 2))
    if(debugEnable) log.debug "set zforecast_1 = $val"
}
def setZforecast_2(val) {
	sendEvent(name: "zforecast_2", value: val)
    if(debugEnable) log.debug "set zforecast_2 = $val"
}
def setZforecast_3(val) {
	sendEvent(name: "zforecast_3", value: val)
    if(debugEnable) log.debug "set zforecast_3 = $val"
}
def setZforecast_4(val) {
	sendEvent(name: "zforecast_4", value: val)
    if(debugEnable) log.debug "set zforecast_4 = $val"
}
def setZforecast_5(val) {
	sendEvent(name: "zforecast_5", value: val)
    if(debugEnable) log.debug "set zforecast_5 = $val"
}
def setZforecast_6(val) {
	sendEvent(name: "zforecast_6", value: val)
    if(debugEnable) log.debug "set zforecast_6 = $val"
}
def setZforecast_7(val) {
	sendEvent(name: "zforecast_7", value: val)
    if(debugEnable) log.debug "set zforecast_7 = $val"
}
def setZforecast_8(val) {
	sendEvent(name: "zforecast_8", value: val)
    if(debugEnable) log.debug "set zforecast_8 = $val"
}
def setZforecast_9(val) {
	sendEvent(name: "zforecast_9", value: val)
    if(debugEnable) log.debug "set zforecast_9 = $val"
}
def setZforecast_10(val) {
	sendEvent(name: "zforecast_10", value: val)
    if(debugEnable) log.debug "set zforecast_10 = $val"
}

def installed() {
	log.trace "installed()"
    setTextDescription('')
    setZforecast_1('')
    setZforecast_2('')
    setZforecast_3('')
    setZforecast_4('')
    setZforecast_5('')
    setZforecast_6('')
    setZforecast_7('')
    setZforecast_8('')
    setZforecast_9('')
    setZforecast_10('')
}

def updated(){
	log.trace "updated()"
	if(debugEnable) runIn(1800,logsOff)
}

void logsOff() {
	log.debug "debug logging disabled..."
	device.updateSetting("debugEnable",[value:"false",type:"bool"])
}

