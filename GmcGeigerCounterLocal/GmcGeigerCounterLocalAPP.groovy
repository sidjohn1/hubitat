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
*  GMC Geiger Counter Local
*
*  Author: Sidney Johnson
*
*  Date: 2026-08-25
*
*   1.0 - Initial Release
*   1.1 - Added CPM_Display
*   1.2 - Added Integrations menu definition
*   1.3 - Added pCi radon support, radon status events, fixed typos,
*         refactored duplicate device-update logic, added input validation
*   1.4 - Added 24hr backoff for gmcmap.com uploads after receiving an HTTP 500
*   1.5 - Fixed groovy.lang.StringWriterIOException: java.io.IOException: Stream closed error
*
*/

import groovy.transform.Field

definition(
    name: "GMC Geiger Counter Local App",
    description: "GMC Geiger Counter Local Integration",
    menu: "Integrations",
    namespace: "sidjohn1",
    author: "sidjohn1",
    oauth: true,
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/sidjohn1/hubitat/main/GmcGeigerCounterLocal/GmcGeigerCounterLocalAPP.groovy")
{
}

// Length of time to suspend gmcmap.com uploads after a HTTP 500 response
@Field static final long GMC_BACKOFF_MS = 24L * 60L * 60L * 1000L

preferences() {
    page(name: "setupScreen")
}

def setupScreen() {
    if (!state.accessToken) {
        createAccessToken()
    }
    def uri = getFullLocalApiServerUrl()
    return dynamicPage(name: "setupScreen", uninstall: true, install: true) {
        section("Settings:") {
            input name: "gmcUpload", type: "bool", title: "Automatically Submit to gmcmap.com", defaultValue: false
            input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        }
        section("Status:") {
            if (state.gmcBackoffUntil && state.gmcBackoffUntil > now()) {
                paragraph("gmcmap.com uploads are currently paused until ${new Date(state.gmcBackoffUntil)} due to a prior server error (HTTP 500).")
            }
        }
        section("NGINX Config:") {
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
    if (!logEnable) unschedule(logsOff)
}

def uninstalled() {
    removeChildDevices(getChildDevices())
}

mappings {
    path("/") { action: [ GET: "renderWebsite" ] }
}

def renderWebsite() {
    if (logEnable) log.debug "Received metrics: ${params}"

    if (!params?.GID) {
        render contentType: "text/html", data: "<html><head><body>Error! Geiger Counter is not found.ERR2.</body></html>", status: 200
        return
    }

    def dev = getChildDevice(params.GID)
    if (!dev) {
        dev = addChildDevice(
            "sidjohn1",
            "GMC Geiger Counter Local Device",
            params.GID,
            ["name": "Geiger Counter ${params.GID}", "label": "Local GMC Geiger Counter", isComponent: false]
        )
    }

    updateDevice(dev, params)

    if (params?.AID && gmcUpload) {
        relayToGmcMap(dev, params)
    } else {
        dev.sendEvent(name: "upload", value: "N/A")
    }

    render contentType: "text/html", data: "<html><head><body>OK.ERR0</body></html>", status: 200
}

private void updateDevice(dev, params) {
    // CPM (Geiger / radiation)
    def cpm = safeInt(params?.CPM)
    dev.sendEvent(name: "CPM",  value: "${cpm}")
    dev.sendEvent(name: "ACPM", value: "${safeDecimal(params?.ACPM)}")
    dev.sendEvent(name: "uSV",  value: "${safeDecimal(params?.uSV)}")

    def (cpmDisplay, cpmMessage) = cpmStatus(cpm)
    dev.sendEvent(name: "CPM_Display", value: cpmDisplay)
    dev.sendEvent(name: "CPM_Message", value: cpmMessage)

    // pCi/L (radon) — only present on RadonScan / RadonPRO
    if (params?.pCi) {
        def pci = safeDecimal(params.pCi)
        dev.sendEvent(name: "pCi", value: "${pci}")

        def (radonDisplay, radonMessage) = radonStatus(pci)
        dev.sendEvent(name: "pCi_Display", value: radonDisplay)
        dev.sendEvent(name: "pCi_Message", value: radonMessage)
    }

    dev.sendEvent(name: "timestamp", value: now())
}

private void relayToGmcMap(dev, params) {
    // Skip the upload entirely if we're still within a backoff window from a prior HTTP 500
    if (state.gmcBackoffUntil && now() < state.gmcBackoffUntil) {
        if (logEnable) log.debug "Skipping gmcmap.com upload, in backoff until ${new Date(state.gmcBackoffUntil)}"
        dev.sendEvent(name: "upload", value: "Backoff until ${new Date(state.gmcBackoffUntil)}")
        return
    }

    def url = "http://www.GMCmap.com/log2.asp?AID=${params.AID}&GID=${params.GID}&CPM=${params.CPM}"
    if (params?.ACPM) url += "&ACPM=${params.ACPM}"
    if (params?.uSV)  url += "&uSV=${params.uSV}"
    if (params?.pCi)  url += "&pCi=${params.pCi}"

    if (logEnable) log.debug "gmcmap.com payload: ${url}"

    try {
        httpGet([uri: url, contentType: "text/plain", timeout: 10]) { resp ->
    		def body = resp?.data?.text ?: resp?.data?.toString()
    		if (logEnable) log.debug "gmcmap.com response: ${body}"
    		if (resp && resp.status == 200) {
        		if (state.gmcBackoffUntil) state.remove('gmcBackoffUntil')
        		dev.sendEvent(name: "upload", value: "${body}")
    		} else if (resp && resp.status == 500) {
        		state.gmcBackoffUntil = now() + GMC_BACKOFF_MS
        		log.error "gmcmap.com returned HTTP 500. Pausing uploads until ${new Date(state.gmcBackoffUntil)}"
        		dev.sendEvent(name: "upload", value: "Error: 500 - pausing uploads for 24h")
    		} else {
        		log.error "Invalid response from gmcmap: ${resp?.status}"
        		dev.sendEvent(name: "upload", value: "Error: ${resp?.status}")
    		}
		}
    } catch (groovyx.net.http.HttpResponseException e) {
        // Some HTTP client configs throw on non-2xx statuses instead of handing back a response
        if (e.response?.status == 500) {
            state.gmcBackoffUntil = now() + GMC_BACKOFF_MS
            log.error "gmcmap.com returned HTTP 500. Pausing uploads until ${new Date(state.gmcBackoffUntil)}"
            dev.sendEvent(name: "upload", value: "Error: 500 - pausing uploads for 24h")
        } else {
            log.error "Error calling gmcmap: ${e}"
            dev.sendEvent(name: "upload", value: "Error: ${e.message}")
        }
    } catch (Exception e) {
        log.error "Error calling gmcmap: ${e}"
        dev.sendEvent(name: "upload", value: "Error: ${e.message}")
    }
}

private List cpmStatus(int cpm) {
    if      (cpm < 51)   return ["${cpm} - NORMAL",         "Normal levels. No action needed."]
    else if (cpm < 100)  return ["${cpm} - MEDIUM",         "Medium levels. Monitor regularly."]
    else if (cpm < 1000) return ["${cpm} - HIGH",           "High levels. Investigate source."]
    else if (cpm < 2000) return ["${cpm} - VERY HIGH",      "Very high levels. Leave area."]
    else                 return ["${cpm} - EXTREMELY HIGH", "Extremely high levels. Evacuate immediately!"]
}

private List radonStatus(BigDecimal pci) {
    if      (pci < 2.0) return ["${pci} pCi/L - SAFE",      "Safe radon level. No action needed."]
    else if (pci < 4.0) return ["${pci} pCi/L - ATTENTION", "Medium radon level. Check ventilation regularly."]
    else                return ["${pci} pCi/L - WARNING",    "Dangerous radon level. Ventilate immediately."]
}

private int safeInt(val) {
    try { return val?.toString()?.isInteger() ? val.toString().toInteger() : 0 }
    catch (e) { return 0 }
}

private BigDecimal safeDecimal(val) {
    try { return val?.toString()?.isBigDecimal() ? val.toString().toBigDecimal() : 0.0 }
    catch (e) { return 0.0 }
}

def logsOff() {
    log.warn "Debug logging disabled."
    app.updateSetting("logEnable", [value: "false", type: "bool"])
}

private removeChildDevices(devices) {
    devices.each { deleteChildDevice(it.deviceNetworkId) }
}
