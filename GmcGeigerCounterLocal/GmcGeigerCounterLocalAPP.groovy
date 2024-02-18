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
*  GMC Geiger Counter Local
*
*  Author: Sidney Johnson
*
*  Date: 2024-02-18
*
*	1.0 - Initial Release
*
*/

definition(
        name: "GMC Geiger Counter Local",
        namespace: "sidjohn1",
        author: "sidjohn1",
        description: "GMC Geiger Counter Local",
        category: "Amazing Apps",
        iconUrl: "",
        iconX2Url: "",
        oauth: [displayName: "Local GMC Geiger Counter", displayLink: "https://sharptools.io"]){
}

preferences(){
    page(name: "setupScreen")
}

def setupScreen(){
    if(!state.accessToken){	
        //enable OAuth in the app settings or this call will fail
        createAccessToken()	
    }
    def uri = getFullLocalApiServerUrl()
    //+ "/?access_token=${state.accessToken}"
    return dynamicPage(name: "setupScreen", uninstall: true, install: true){
    section("Settings:") {
		input name: "gmcUpload", type: "bool", title: "Automatically Submit to gmcmaps.com", defaultValue: true
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        }
        section("NGINX Config:"){ 
            paragraph("""server {
            listen       80;     
                location /gmc {
                set &#36;token \"\";
                if (&#36;is_args) {
                    set &#36;token \"&\";
                }
                set &#36;args \"&#36;{args}&#36;{token}access_token=${state.accessToken}\";
                proxy_read_timeout 10s;
                proxy_pass ${uri}/;
                proxy_redirect default;
            }
        }""")
        }
    }
}

def updated() {
	log.warn "debug logging is: ${logEnable == true}"
	if (logEnable) runIn(1800, logsOff)
}

def uninstalled() {
	removeChildDevices(getChildDevices())
}

mappings {
    //the root path - you can also map other paths or use parameters in paths and posted data
    path("/") { 			 action: [ GET: 	"renderWebsite" ] }
}

def renderWebsite(){
    if (logEnable)log.debug "Rendering page: params = ${params}"
    
    if (params?.GID){
        if (!getChildDevice(params?.GID)){
            addChildDevice("sidjohn1", "GMC Geiger Counter Local", params?.GID, ["name": "Geiger Counter ${params?.GID}", "label": "Local GMC Geiger Counter", isComponent: false])
            def dev = getChildDevice(params?.GID)
            dev.sendEvent(name: "CPM", value: "${params?.CPM}")
	        dev.sendEvent(name: "ACPM", value: "${params?.ACPM}")	
	        dev.sendEvent(name: "uSV", value: "${params?.uSV}")
            dev.sendEvent(name: 'timestamp', value: now())
        }
        else {
            def dev = getChildDevice(params?.GID)
            dev.sendEvent(name: "CPM", value: "${params?.CPM}")
	        dev.sendEvent(name: "ACPM", value: "${params?.ACPM}")	
	        dev.sendEvent(name: "uSV", value: "${params?.uSV}")
            dev.sendEvent(name: 'timestamp', value: now())
        }
        if (params?.AID && gmcUpload){
            def url = "http://www.GMCmap.com/log2.asp?AID=${params?.AID}&GID=${params?.GID}&CPM=${params?.CPM}"
            if (params?.ACPM)url +="&ACPM=${params?.ACPM}"
            if (params?.uSV)url +="&uSV=${params?.uSV}"
            if (logEnable)log.debug "gmcmaps.com payload ${url}"
            try {
                httpGet(url) { resp ->
                    if (logEnable) log.debug resp.getData()
                    if (resp && (resp.status == 200)) {
                        def dev = getChildDevice(params?.GID)
                        dev.sendEvent(name: "upload", value: "${resp?.data}")
                    }
                    else {
                        log.error "Invalid response for gmcmap request: ${resp} ${resp.status}"
            	    }
	            }
	        }
            catch(Exception e) {
                log.debug "error occured calling httpget ${e}"
	        }
        }
        else {
            def dev = getChildDevice(params?.GID)
            dev.sendEvent(name: "upload", value: "N/A")
        }
        html = "<html><head><body>OK.ERR0</body></html>"
        render contentType: "text/html", data: html, status: 200
    }
    else {
        html = "<html><head><body>Error! Geiger Counter is not found.ERR2.</body></html>"
        render contentType: "text/html", data: html, status: 200
    }
}

def logsOff() {
	log.warn "debug logging disabled..."
	app.updateSetting("logEnable", [value: "false", type: "bool"])
}

private removeChildDevices(devices) {
	devices.each {
		deleteChildDevice(it.deviceNetworkId) // 'it' is default
	}
}
