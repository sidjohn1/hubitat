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
*  ical Today
*
*  Author: Sidney Johnson
*
*  Date: 2024-02-08
*
*	1.0 - Initial Release
*
*/

import java.text.SimpleDateFormat
import java.util.TimeZone

metadata {
    definition (
        name: "ical today", 
        namespace: "sidjohn1", 
        author: "sidjohn1",
        importUrl:"https://raw.githubusercontent.com/sidjohn1/hubitat/main/icalToday/icalTodayDTH.groovy"
    ) {
        capability "Actuator"
        capability "Sensor"
        capability "Initialize"

        attribute "tileAttr", "string" 
    }   
}

preferences {
    input("icalink", "string", title: "ical link(s), seperate with a ;")
    input("updatefeq", "number", title: "Polling Rate (minuites)\nDefault:60", default:60)
    input("maxEvt", "number", title: "max number of events to show, if you regualy see 'please select an atribute' on dashboad, reduce this number\nDefault:10", default:10)
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    
}
def installed() {
    log.trace "installed()"
    initialize()
}
def updated(){
    sendEvent(name:"tileAttr",value:"Nothing here yet ")
    log.trace "updated() -  "
    if (logEnable) runIn(1800, logsOff)
    initialize()
}

def initialize(){
    if (icalink == null){
        log.warn "${device} - No ical link"
        return
    }
    if (updatefeq == null) updatefeq = 60
    state.updatefeq = updatefeq*60
    if (maxEvt == null) maxEvt = 10
    state.maxEvt = maxEvt

    
    log.info "${device} initialize - update fequency= ${state.updatefeq}, max events= ${state.maxEvt}"
    if (icalink != null) runIn(5,getdata)
}

def logsOff() {
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable", [value: "false", type: "bool"])
}

void getdata(){
    if (logEnable) log.debug "${device} get data"
//    Map reqParams = [
//            uri: icalink,
//            timeout: 10
//        ]
    HashMap iCalMap = [:] 
    Integer eCount = 0
    iCalMap.put("event",[:])
    try {
        icalinks = icalink.split(";")
        icalinks.each { it ->
            if(it.startsWith(" ")) it = it.replaceFirst(" ","")
            Map reqParams = [
                uri: it,
                timeout: 10
            ]
     
             
        httpGet(reqParams) { resp ->
            if(resp.status == 200) {
                if (logEnable) log.debug "rest status${resp.status}"
                wkStr = resp.data
                //iCalMap.put("event",[:])
               // Integer eCount = 0
                wkStr.eachLine{
                    if(!it.startsWith(" ")){
                    List dSplit= it.split(":")
                    if(dSplit.size()>1){
                         if (dSplit[0].trim()=="BEGIN" && dSplit[1].trim()=="VEVENT") {
                            eCount++
                            iCalMap.event.put(eCount.toString(),[:])
                        }
                        if (eCount != 0 && dSplit[1].trim()!=null){
                            if (dSplit[0].trim().contains("DTSTART")) iCalMap.event[eCount.toString()].put("start",dSplit[1].trim())
                            else if (dSplit[0].trim().contains("DTEND")) iCalMap.event[eCount.toString()].put("end",dSplit[1].trim())
                            else if (dSplit[0].trim()=="LOCATION" && state.shLoc) iCalMap.event[eCount.toString()].put("location",dSplit[1].trim())
                            else if (dSplit[0].trim()=="STATUS") iCalMap.event[eCount.toString()].put("status",dSplit[1].trim())     //CONFIRMED or TENTATIVE
                            else if (dSplit[0].trim()=="SUMMARY") iCalMap.event[eCount.toString()].put("summary",dSplit[1].trim())
                            else if (dSplit[0].trim()=="SEQUENCE") iCalMap.event[eCount.toString()].put("repeatNum",dSplit[1].trim())
                            else if (dSplit[0].trim()=="RRULE") iCalMap.event[eCount.toString()].put("repeatFreq",dSplit[1].trim())
                       }
                    }
                    else { // blank - location, attiees etc
                    }
                  }
                }
            } //end 200 resp
            else { // not 200
                log.warn "${device} Response code ${resp.status}"
            }
        } //end http get
    } //end each ical
    } //end try
    catch (e) {
        log.warn "${device} CATCH $e"
    }
    
    Date today = new Date()
    String todaydate = new SimpleDateFormat("dd-MM-yy").format(today)
    if (logEnable) log.debug "${today} & ${todaydate}"
    
//need to re forcast dates prio to sorting
    if (logEnable)     log.debug "${iCalMap.event.size()}"
    iCalMap.event = iCalMap.event.values()sort{ a, b -> a.start <=> b.start} //sort the data
    if (logEnable)    log.debug "sorted ${iCalMap.event.size()}"
    iCalMap.event = iCalMap.event.unique()
    if (logEnable)     log.debug "filltered ${iCalMap.event.size()}"
    
    Integer MaxCount = 0
   
  attrString = ""

    iCalMap.event.each{
      if (MaxCount < state.maxEvt){
          if (it.start == null) it.start = it.end // not used that i know off
          if (it.end == null) it.end = it.start //used some envents didnt have a end date

          (t,d,z) = timeHelp(it.start)
          fullstart = z
          datestart = d
          timestart = t
          
          (t,d,z) = timeHelp(it.end)
          fullend = z
          timeend = t

          if (today>=fullstart && today<=fullend || todaydate==datestart) { //and not canciled?
          MaxCount = MaxCount +1
              
// all day or times              
            if(it.start.indexOf("T") == -1) {attrString+="All Day"} //all day event
            else attrString+=timestart+" to "+timeend+" " //time event
              
//description          
            attrString+="${it.summary}</br>" //description
          }
          
    }
   } 
    
//log.debug"end"
    if(attrString.length() >= 1024) log.warn "To many Char. please reduce max number of events or turn off location = ${attrString.length()}"
    sendEvent(name:"tileAttr",value:attrString)
    log.info "done get"
    runIn(state.updatefeq,getdata)
}
                    
private timeHelp(data) {
    if (logEnable) log.debug "timeHelp data= $data"
    Date zDate
    if (data.contains("Z")) zDate =  toDateTime(data)
    else if (data.contains("T")) zDate = new SimpleDateFormat("yyyyMMdd'T'kkmmss").parse(data)
    else zDate = new SimpleDateFormat("yyyyMMdd").parse(data)
    if (logEnable) log.debug "zDate= $zDate"
    String localTime = new SimpleDateFormat("HH:mm").format(zDate)
    String dateTrim = new SimpleDateFormat("dd-MM-yy").format(zDate)
    if (logEnable) log.debug "timeHelp return=$zDate & $localTime & $dateTrim"     
    return [localTime, dateTrim,zDate]
}
