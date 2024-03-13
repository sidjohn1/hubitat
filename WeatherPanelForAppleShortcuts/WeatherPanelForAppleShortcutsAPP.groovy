/**
 *  Weather Panel for Apple Shortcuts
 *
 *  Copyright 2024 Sidney Johnson
 *  If you like this code, please support the developer via PayPal: https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=XKDRYZ3RUNR9Y
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
 *  Weather Panel For Apple Shortcuts Local App
 *
 *  Author: Sidney Johnson
 *  Date: 2024-03-18
 *
 *	Version: 1.0 - Apple Weather via shortcuts, converted day/night detection to hub sunrise/sunset
 *	Version: 1.1 - Added humidity
 *	Version: 1.2 - Added child device, and code refactor
 *
 */

definition(
    name: "Weather Panel For Apple Shortcuts Local App",
    namespace: "sidjohn1",
    author: "Sidney Johnson",
    description: "Weather Panel For Apple Shortcuts",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    oauth: true)

preferences {
    page(name: "selectDevices")
    page(name: "viewURL")
}

def selectDevices() {
    if(!state.accessToken){	
        //enable OAuth in the app settings or this call will fail
        createAccessToken()	
    }
	return dynamicPage(name: "selectDevices", install: true, uninstall: true) {
	    section("About") {
			paragraph "Weather Panel displays inside and outside temp and weather infomation as a web page. Also has a random customizable background."
			paragraph "${textVersion()}\n${textCopyright()}"
 	   }
		section("Select Device for...") {
			input "insideTemp", "capability.temperatureMeasurement", title: "Inside Tempature...", multiple: false, required: true
            input "outsideTemp", "capability.temperatureMeasurement", title: "Outside Tempature...", multiple: false, required: true
            input "showHumid", "bool", title:"Show Humidity", required: true, multiple:false, submitOnChange: true, defaultValue: false
            if (settings.showHumid){
            input "insideHumid", "capability.relativeHumidityMeasurement", title: "Inside Humidity...", multiple: false, required: true
            input "outsideHumid", "capability.relativeHumidityMeasurement", title: "Outside Humidity...", multiple: false, required: true
            }
            input "showForcast", "bool", title:"Show Forcast", required: true, multiple:false, submitOnChange: true, defaultValue: false
            input "forcastDevice", "device.WeatherPanelForAppleShortcutsLocalDevice", title: "Forcast Device...", multiple: false, required: true
		}
		section(hideable: true, hidden: true, "Optional Settings") {
            input "fontColor", "enum", title:"Select Font Color", required: false, multiple:false, defaultValue: "White", options: [3: 'Black',2: 'Ivory', 1:'White']
			input "fontSize", "enum", title:"Select Font Size", required: false, multiple:false, defaultValue: "Medium", options: [4: 'xSmall',3: 'Small',2: 'Medium', 1:'Large']
            input "localResources", "bool", title: "Use Local Resources?", required: false, defaultValue: false
		}
		section("Wallpaper URL") {
			input "wallpaperUrl", "text", title: "Wallpaper URL",defaultValue: "http://", required:false
		}
        section() {
			href "viewURL", title: "View URL"
		}
	}
}

def viewURL() {
	return dynamicPage(name: "viewURL", title: "${title ?: location.name} Weather Pannel URL", install:false) {
		section() {
            paragraph "Copy the URL below to the URL textbox in shortcuts"
			input "weatherUrl", "text", title: "URL",defaultValue: "${generateURL("update")}", required:false
			paragraph "Copy the URL below to any modern browser to view your ${title ?: location.name}s' Weather Panel. Add a shortcut to home screen of your mobile device to run as a native app."
			input "weatherUrl", "text", title: "URL",defaultValue: "${generateURL("html")}", required:false
			href url:"${generateURL("html")}", style:"embedded", required:false, title:"View", description:"Tap to view, then click \"Done\""
		}
	}
}

mappings {
    path("/html") { action: [GET: "generateHtml"] }
	path("/json") {	action: [GET: "generateJson"] }
    path("/update") {	action: [POST: "recieveUpdate"] }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
	log.info "Weather Panel ${textVersion()} ${textCopyright()}"
	generateURL()
        createChildDevice()
}

def uninstalled() {
	removeChildDevices(getChildDevices())
}

def recieveUpdate() {
    log.info "Recieved metrics"
    render contentType: "text/plain", data: "OK", status: 200
    def json 
    try {
        json = parseJson(request.body).sort()
        log.debug json
        def dev = getChildDevice(deviceUID())
        json.each {
            dev.sendEvent(name: "${it.key}", value: "${it.value}")
        }
        dev.sendEvent(name: 'timestamp', value: now())
        }
        catch (Exception e) {
            log.error "JSON received from device is invalid! ${request.body}: ${e}"
        return
    }
}

def generateHtml() {
	render contentType: "text/html", headers: ["Access-Control-Allow-Origin": "*"], data: "<!DOCTYPE html>\n<html>\n<head>${head()}</head>\n<body>\n${body()}\n</body></html>", status: 200
}

def generateJson() {
	render contentType: "application/json", headers: ["Access-Control-Allow-Origin": "*"], data: "${jsonData()}", status: 200
}

def head() {

def color1
def color2
def color3
def font1
def font2
def font3
def humidHTML1 = ""
def humidHTML2 = ""
def iconW
def rTimeout    
def temp1TA
def weatherDataContent
    
rTimeout = Math.floor(Math.random() * (1000000 - 800000 + 1) ) + 1750000
rTimeout = rTimeout.toInteger()
    
switch (settings.fontSize) {
	case "1":
	font1 = "50"
	font2 = "20"
	font3 = "10"
	break;
	case "2":
	font1 = "48"
	font2 = "18"
	font3 = "10"
	break;
	case "3":
	font1 = "46"
	font2 = "16"
	font3 = "10"
	break;
    case "4":
	font1 = "44"
	font2 = "16"
	font3 = "7"
	break;
}

switch (settings.fontColor) {
    case "1":
	color1 = "255,255,255"
	color2 = "0,0,0"
    color3 = "0,0,0"
	break;
	case "2":
	color1 = "255,248,220"
	color2 = "222,184,135"
    color3 = "0,0,0"
	break;
	case "3":
    color1 = "0,0,0"
	color2 = "255,255,255"
    color3 = "255,255,255"
	break;
}
    
if (showHumid == true) {
    humidHTML1 = "</b><t>&#47;</t>' + item.humid1 + '<b>&#37;"
    humidHTML2 = "</b><t>&#47;</t>' + item.humid2 + '<b>&#37;"
} 
    
if (showForcast == true) {
	iconW = "47"
	temp1TA = "right"
	weatherDataContent = """	    		content += '<div id="icon"><i class="wi wi-' + item.icon + '"></i></div>';
	    		content += '<div id="temp1" class="text3"><p>' + item.temp1 + '°<b>${getTemperatureScale()}${humidHTML1}&nbsp;<br>Inside&nbsp;</b><br>' + item.temp2 + '°<b>${getTemperatureScale()}${humidHTML2}&nbsp;<br>Outside&nbsp;</b><br></p></div>';
    			content += '<div id="cond" class="text2"><p>' + item.cond + '&nbsp;</p></div>';
    			content += '<div id="forecast" class="text3"><p>' + item.forcastDay1Day + '<br><i class="wi wi-' + item.forecastDay1Icon + '"></i>&nbsp;&nbsp;' + item.forcastDay1High + '°<br><u>' + item.forcastDay1Low + '°</u></p><br></div>';
    			content += '<div id="forecast" class="text3"><p>' + item.forcastDay2Day + '<br><i class="wi wi-' + item.forecastDay2Icon + '"></i>&nbsp;&nbsp;' + item.forcastDay2High + '°<br><u>' + item.forcastDay2Low + '°</u></p><br></div>';
    			content += '<div id="forecast" class="text3"><p>' + item.forcastDay3Day + '<br><i class="wi wi-' + item.forecastDay3Icon + '"></i>&nbsp;&nbsp;' + item.forcastDay3High + '°<br><u>' + item.forcastDay3Low + '°</u></p><br></div>';
    			content += '<div id="forecast" class="text3"><p>' + item.forcastDay4Day + '<br><i class="wi wi-' + item.forecastDay4Icon + '"></i>&nbsp;&nbsp;' + item.forcastDay4High + '°<br><u>' + item.forcastDay4Low + '°</u></p><br></div>';"""
                
}
   else {
	iconW = "100"
	temp1TA = "left"
   	weatherDataContent = """	    		content += '<div id="icon"><i class="wi wi-' + item.icon + '"></i></div>';
	    		content += '<div id="temp1" class="text1"><p>' + item.temp1 + '°<b>${getTemperatureScale()}<br>Inside</b></p></div>';
	    		content += '<div id="temp2" class="text1"><p>' + item.temp2 + '°<b>${getTemperatureScale()}<br>Outside</b></p></div>';
    			content += '<div id="cond" class="text1"><p>' + item.cond + '&nbsp;</p></div>';"""
}

"""<!-- Meta Data -->
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<meta name="Description" content="Weather Panel" />
	<meta name="application-name" content="Weather Panel" />
	<meta name="apple-mobile-web-app-title" content="Weather Panel">
	<meta name="keywords" content="weather,panel,hubitat" />
	<meta name="Author" content="sidjohn1" />
<!-- Apple Web App -->
	<meta name="apple-mobile-web-app-capable" content="yes" />
	<meta name="mobile-web-app-capable" content="yes" />
	<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
	<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0" />
	<link rel="apple-touch-icon-precomposed" href="/local/weatherpanel.png" />
<!-- Stylesheets -->
<style type="text/css">
body{
	background-size: cover;
	-webkit-background-size: cover;
	-moz-background-size: cover;
	-o-background-size: cover;
	background-attachment: fixed;
	background-color: rgb(${color3});
	background-position: center;
    background-repeat: no-repeat;
	overflow: hidden;
	margin: 0 0;
	width: 100%;
	height: 100%;
}
b{
	font-size: 20px;
	font-size: ${font3}vh;
	vertical-align: super;
}
p{
	font-family:Gotham, "Helvetica Neue", Helvetica, Arial, sans-serif;
	color: rgb(${color1});
	text-shadow: 2px 2px 1px rgb(${color2});
	margin:0 0;
	opacity: 0.9;
}
i{
	color: rgb(${color1});
	text-shadow: 2px 2px 1px rgb(${color2});
	vertical-align: middle;
	opacity: 0.9;
}
t{
    font-weight: 100;
}
div{
	background: transparent;
}
u{
	text-decoration: overline;
}
.text1 {
	font-weight: bold;
	vertical-align: text-top;
	margin-top: -3%;
}
.text2 {
	font-weight: 900;
    letter-spacing: 5px;
	vertical-align: super;
	margin-top: -3%;
	margin-bottom: 1%;
}
.text3 {
	font-weight: bold;
	vertical-align: super;
}
#data {
	display: flex;
	display: -webkit-flex;
	flex-direction: row;
	-webkit-flex-direction: row;
	flex-wrap: wrap;
	-webkit-flex-wrap: wrap;
}
#icon{
	margin: 3% 0 0 1%;
	font-size: 20px;
	font-size: ${font1}vh;
	text-align: center;
	width: ${iconW}%;
}
#temp1{
	text-align: ${temp1TA};
	float: left;
	width: 48%;
	margin-left: 2%;
	font-size: 20px;
	font-size: ${font2}vh;
	line-height: 13vh;
}
#humid1{
	text-align: ${temp1TA};
	float: left;
	width: 48%;
	margin-left: 2%;
	font-size: 20px;
	font-size: ${font2}vh;
	line-height: 13vh;
}
#temp2{
	text-align: right;
	float: right;
	width: 48%;
	margin-right: 2%;
	font-size: 20px;
	font-size: ${font2}vh;
	line-height: 13vh;
}
#humid2{
	text-align: right;
	float: right;
	width: 48%;
	margin-right: 2%;
	font-size: 20px;
	font-size: ${font2}vh;
	line-height: 13vh;
}
#cond{
	white-space: nowrap;
	text-align: right;
	width: 100%;
	font-size: 20px;
	font-size: ${font3}vh;
}
#forecast{
	white-space: nowrap;
	text-align: right;
	width: 20%;
	font-size: 20px;
	font-size: 7vh;
	background: rgba(${color3},.5);
	vertical-align: middle;
    padding-right: 5%;
}
</style>
<link type="text/css" rel="stylesheet" href="/local/weatherpanel.css"/>
<link rel="shortcut icon" type="image/png" href="/local/weatherpanel.png"/>
<link rel="manifest" href="/local/weatherpanel.json">
    <!-- Page Title -->
    <title>Weather Panel</title>
  	<!-- Javascript -->
<script type="text/javascript" charset="utf-8" src="/local/weatherpanel.js"></script>
<script type="text/javascript">
\$(window).load(function(){
	var bg = '';
	var tImage = new Image();
	\$("#data").click(function(){
		var path = "${wallpaperUrl}";
		var fileList = "index.json";
		\$.getJSON(path+fileList,function(list,status){
			var mime = '*';
			while (mime.search('image')){
				obj = list[Math.floor(Math.random()*list.length)];
				mime=obj.mime;
			}
			bg = path+obj.path;
			bg = bg.replace('#','%23');
            \$('<img src="'+bg+'"/>');
            setTimeout(function(){
				document.body.background = bg;
			},3109);
		});
        setTimeout('\$("#data").click()', ${rTimeout});
	});
	\$("#data").click();
});
</script>

<script type="text/javascript">
\$(document).ready(function(){
	weatherData = function () {
		\$.getJSON("${generateURL("json")}",function(weather){
		var content = '';
			\$.each(weather.data, function(i,item){
${weatherDataContent}
				\$("#data").empty();
    			\$(content).appendTo("#data");
    		});
    	});
    	setTimeout(weatherData, 62500);
	}
	weatherData();
});
</script>
"""
}

def body() {  
"""<div id="data"></div>"""
}

def jsonData(){
//log.debug "refreshing weather"
sendEvent(name:"weatherRefresh", value:"refreshing weather", descriptionText:"weatherRefresh is refreshing weather", eventType:"SOLUTION_EVENT", displayed: true)

def weatherIcons = []
def currentTime = now()
def sunriseTime = location.sunrise
def sunsetTime = location.sunset

if (settings.insideHumid && settings.outsideHumid) {
    humid1 = insideHumid.currentValue("humidity")
    humid2 = outsideHumid.currentValue("humidity")
} 
   
if(currentTime > sunriseTime.time && currentTime < sunsetTime.time) {
        weatherIcons = ["Sunny": "day-sunny", "Clear": "day-sunny", "Mostly Clear": "day-sunny","Mostly Sunny": "day-sunny-overcast", "Mostly Cloudy": "day-cloudy", "Partly Cloudy": "day-cloudy", "Cloudy": "day-cloudy", "Windy": "day-windy", "Breezy, light wind": "day-windy", "Snow": "day-snow", "rain_snow": "day-rain-mix", "rain_sleet": "day-sleet", "snow_sleet": "day-sleet", "fzra": "day-sleet", "rain_fzra": "day-sleet", "snow_fzra": "day-sleet", "sleet": "day-sleet", "Drizzle": "day-sprinkle", "Rain": "day-showers", "Heavy Rain": "day-rain", "Thunderstorm": "day-storm-showers", "tsra_hi": "day-storm-showers", "tornado": "tornado", "hurricane": "hurricane", "tropical_storm": "hurricane", "dust": "day-haze", "smokey": "day-haze", "haze": "day-haze", "hot": "hot", "cold": "snowflake-cold", "blizzard": "snowflake-cold", "fog": "day-haze"]
}
else{
    	weatherIcons = ["Sunny": "night-clear", "Clear": "night-clear", "Mostly Clear": "night-clear", "Mostly Sunny": "night-alt-partly-cloudy", "Mostly Cloudy": "night-alt-cloudy", "Partly Cloudy": "night-alt-cloudy", "Cloudy": "night-alt-cloudy", "Windy": "alt-cloudy-windy", "Breezy, light wind": "alt-cloudy-windy", "Snow": "night-alt-snow", "rain_snow": "night-alt-rain-mix", "rain_sleet": "night-alt-sleet", "snow_sleet": "night-alt-sleet", "fzra": "night-alt-sleet", "rain_fzra": "night-alt-sleet", "snow_fzra": "night-alt-sleet", "sleet": "night-alt-sleet", "Drizzle": "night-alt-sprinkle", "Rain": "night-alt-showers", "Heavy Rain": "night-alt-rain", "Thunderstorm": "night-alt-storm-showers", "tsra_hi": "night-alt-storm-showers", "tornado": "tornado", "hurricane": "hurricane", "tropical_storm": "night-alt-thunderstorm", "dust": "night-fog", "smokey": "night-fog", "haze": "night-fog", "hot": "hot","cold": "snowflake-cold","blizzard": "snowflake-cold","fog": "night-fog"]
}

forecastNowIcon = weatherIcons[forcastDevice.currentValue("currentCondition").toString()] ?:"na"
forecastDay1Icon = weatherIcons[forcastDevice.currentValue("forcastDay1Condition").toString()] ?:"na"
forecastDay2Icon = weatherIcons[forcastDevice.currentValue("forcastDay2Condition").toString()] ?:"na"
forecastDay3Icon = weatherIcons[forcastDevice.currentValue("forcastDay3Condition").toString()] ?:"na"
forecastDay4Icon = weatherIcons[forcastDevice.currentValue("forcastDay4Condition").toString()] ?:"na"

"""{"data": [{"icon":"${forecastNowIcon}","cond":"${forcastDevice.currentValue("currentCondition")}","temp1":"${Math.round(insideTemp.currentValue("temperature"))}","temp2":"${Math.round(outsideTemp.currentValue("temperature"))}"
,"humid1":"${humid1?:"na"}","humid2":"${humid2?:"na"}"
,"forcastDay1Day":"${forcastDevice.currentValue("forcastDay1Day")}","forecastDay1Icon":"${forecastDay1Icon}","forcastDay1High":"${forcastDevice.currentValue("forcastDay1High")}","forcastDay1Low":"${forcastDevice.currentValue("forcastDay1Low")}"
,"forcastDay2Day":"${forcastDevice.currentValue("forcastDay2Day")}","forecastDay2Icon":"${forecastDay2Icon}","forcastDay2High":"${forcastDevice.currentValue("forcastDay2High")}","forcastDay2Low":"${forcastDevice.currentValue("forcastDay2Low")}"
,"forcastDay3Day":"${forcastDevice.currentValue("forcastDay3Day")}","forecastDay3Icon":"${forecastDay3Icon}","forcastDay3High":"${forcastDevice.currentValue("forcastDay3High")}","forcastDay3Low":"${forcastDevice.currentValue("forcastDay3Low")}"
,"forcastDay4Day":"${forcastDevice.currentValue("forcastDay4Day")}","forecastDay4Icon":"${forecastDay4Icon}","forcastDay4High":"${forcastDevice.currentValue("forcastDay4High")}","forcastDay4Low":"${forcastDevice.currentValue("forcastDay4Low")}"}]}"""
}

private def generateURL(data) {    
	if (!state?.accessToken) {
		try {
			def accessToken = createAccessToken()
			log.debug "Creating new Access Token: $state.accessToken"
		} catch (ex) {
			log.error "Creating new Access Token: Failed"
			log.error ex
		}
    }
    def url = "${getFullLocalApiServerUrl()}/${data}?access_token=${state.accessToken}"
return "$url"
}

private def deviceUID() {
    def UIDpre = state.accessToken.split('-')
    UID = [UIDpre[3],UIDpre[4]].join()
    return UID
}

private createChildDevice() {
    if (!getChildDevice(deviceUID())){
        addChildDevice("sidjohn1", "Weather Panel For Apple Shortcuts Local Device", deviceUID(), ["name": "Weather Panel ${deviceUID()}", "label": "Weather Panel For Apple Shortcuts", isComponent: true])
        log.debug "Creating device ${deviceUID()}"
        }
}

private removeChildDevices(devices) {
	devices.each {
		deleteChildDevice(it.deviceNetworkId) // 'it' is default
	}
}

private def textVersion() {
    def text = "Version 1.2"
}

private def textCopyright() {
    def text = "Copyright © 2024 Sidjohn1"
}
