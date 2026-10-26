/**
 *  Aeotec Door Window Sensor 8 (Model: ZWA055) Hubitat Driver
 *
 *  Capabilities:
 *    TemperatureMeasurement
 *    RelativeHumidityMeasurement
 *    Battery
 *    ContactSensor
 *    AccelerationSensor   (active = tilted)
 *    ThreeAxis            (fed by Multilevel Sensor Acceleration X/Y/Z, m/s²)
 *    TamperAlert
 *    PowerSource
 *    Refresh, Polling, Configuration
 *
 *  Custom attributes:
 *    dewPoint          (reported by the device, Multilevel Sensor type 0x0B)
 *    tiltState
 *    moldAlert         (clear / detected)
 *    temperatureAlarm  (normal / high / low, needs alarm thresholds set)
 *
 *  Reference: Aeotec ZWA055 user guide (command classes & configuration parameters)
 *  1.0 - Initial Release
 */

import groovy.transform.Field

@Field static final Map CMD_CLASS_VERS = [
    0x31: 11, // Sensor Multilevel
    0x30: 2,  // Sensor Binary
    0x80: 1,  // Battery
    0x84: 2,  // Wake Up
    0x71: 8,  // Notification
    0x85: 2,  // Association
    0x70: 1,  // Configuration
    0x6C: 1   // Supervision (S2)
]

// Multilevel Sensor types (Z-Wave spec)
@Field static final int SENSOR_TEMPERATURE = 0x01
@Field static final int SENSOR_HUMIDITY    = 0x05
@Field static final int SENSOR_DEW_POINT   = 0x0B
@Field static final Map SENSOR_ACCEL_AXIS  = [0x34: "x", 0x35: "y", 0x36: "z"]

// Binary Sensor types used by this device
@Field static final int BINARY_DOOR_WINDOW = 0x0A
@Field static final int BINARY_TILT        = 0x0B
@Field static final int BINARY_GENERAL_MOLD = 0x01

metadata {
    definition(
        name: "Aeotec Door Window Sensor 8",
        namespace: "sidjohn1",
        author: "sidjohn1",
    ) {
        capability "TemperatureMeasurement"
        capability "RelativeHumidityMeasurement"
        capability "Battery"
        capability "ContactSensor"
        capability "AccelerationSensor"
        capability "ThreeAxis"
        capability "TamperAlert"
        capability "PowerSource"
        capability "Refresh"
        capability "Polling"
        capability "Sensor"
        capability "Configuration"

        attribute "dewPoint", "number"
        attribute "tiltState", "string"
        attribute "moldAlert", "enum", ["clear", "detected"]
        attribute "temperatureAlarm", "enum", ["normal", "high", "low"]

        fingerprint mfr: "0371", prod: "0002", deviceId: "0089", pp: "02", deviceJoinName: "Aeotec Door Window Sensor 8"
        fingerprint mfr: "0371", prod: "0002", model: "ZWA055", deviceJoinName: "Aeotec Door Window Sensor 8"
    }

    preferences {
        input name: "tempUnit", type: "enum", title: "Temperature unit (also sets the device's reporting scale)",
            options: ["C":"Celsius","F":"Fahrenheit"], defaultValue: "C", required: true

        input name: "tempOffset", type: "decimal", title: "Temperature offset (in the unit above)",
            range: "-20..20", defaultValue: 0
        input name: "humOffset", type: "decimal", title: "Humidity offset %",
            range: "-20..20", defaultValue: 0

        input name: "tempReportDelta", type: "decimal", title: "Minimum temperature change to report (0 = disabled)",
            range: "0..25.5", defaultValue: 2.0
        input name: "humReportDelta", type: "decimal", title: "Minimum humidity change to report % (0 = disabled)",
            range: "0..25.5", defaultValue: 5.0
        input name: "accReportDelta", type: "number", title: "Minimum acceleration change to report (0 = disabled, 30–255; blank = device default)",
            range: "0..255"
        // Device max for param 24 is 65535 s (~18.2 h), so 24 h isn't offered here
        input name: "periodicReport", type: "enum", title: "Periodic report interval (battery, temperature, humidity, acceleration)",
            options: ["0":"Disabled (recommended)", "3600":"1 hour", "14400":"4 hours", "21600":"6 hours", "28800":"8 hours", "43200":"12 hours (default)", "64800":"18 hours"],
            defaultValue: "43200"
        input name: "checkInterval", type: "enum", title: "How often the sensor measures temperature and humidity",
            options: ["300":"5 minutes", "600":"10 minutes", "900":"15 minutes (default)", "1800":"30 minutes", "3600":"1 hour"],
            defaultValue: "900"

        input name: "highTempAlarm", type: "decimal", title: "High temperature alarm (in the unit above, blank = off)",
            range: "0.1..100"
        input name: "lowTempAlarm", type: "decimal", title: "Low temperature alarm (in the unit above, blank = off)",
            range: "-20..100"
        input name: "lowBatteryLevel", type: "number", title: "Low battery warning level %",
            range: "10..50", defaultValue: 20

        input name: "ledIndication", type: "bool", title: "Enable LED indication", defaultValue: true
        input name: "magnetClosedState", type: "enum", title: "Report closed when the magnet is…",
            options: ["closed":"Near (normal)","open":"Far away (reversed)"], defaultValue: "closed"
        input name: "tiltSensorMode", type: "enum", title: "Tilt sensor mode",
            options: ["0":"Disabled","1":"Enabled (with magnet)","2":"Enabled (sensor alone, no magnet)"], defaultValue: "1"
        input name: "tiltModeState", type: "enum", title: "Sensor-alone mode: contact state when tilted",
            options: ["0":"Open when tilted, closed when level","1":"Closed when tilted, open when level"], defaultValue: "0"
        input name: "tiltAngle", type: "number", title: "Tilt trigger angle (degrees)",
            range: "1..90", defaultValue: 5

        input name: "wakeUpInterval", type: "enum", title: "Wake Up Interval",
            options: ["3600":"1 hour", "14400":"4 hours", "21600":"6 hours", "28800":"8 hours", "43200":"12 hours", "86400":"24 hours"],
            defaultValue: "21600"
        input name: "binaryReportEnable", type: "bool", title: "Enable Binary Sensor reports (door, tilt, mold)",
            defaultValue: true
        input name: "readbackConfig", type: "bool", title: "Read settings back from the device after configuring (turn off once confirmed to save battery)",
            defaultValue: true
        input name: "debugLogging", type: "bool", title: "Enable debug logging (turns off after 30 minutes)", defaultValue: true
        input name: "infoLogging", type: "bool", title: "Enable description text logging", defaultValue: true
    }
}

/* ---------- Lifecycle ---------- */

def installed() {
    log.info "${device.displayName} installed"
    sendEvent(name: "contact", value: "closed")
    sendEvent(name: "acceleration", value: "inactive")
    sendEvent(name: "tiltState", value: "level")
    sendEvent(name: "tamper", value: "clear")
    sendEvent(name: "moldAlert", value: "clear")
    sendEvent(name: "temperatureAlarm", value: "normal")
    configure()
}

def updated() {
    if (infoLogging) log.info "${device.displayName} preferences updated"
    unschedule("logsOff")
    if (debugLogging) runIn(1800, "logsOff")
    configure()
}

def logsOff() {
    log.warn "${device.displayName} debug logging disabled"
    device.updateSetting("debugLogging", [value: "false", type: "bool"])
}

/* ---------- Parsing ---------- */

def parse(String description) {
    if (debugLogging) log.debug "parse() ${description}"
    def cmd = zwave.parse(description, CMD_CLASS_VERS)
    if (cmd) {
        if (debugLogging) log.debug "Parsed: ${cmd}"
        zwaveEvent(cmd)
    }
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    if (debugLogging) log.debug "Unhandled command: ${cmd}"
}

/* S2 Supervision: unwrap, handle, and acknowledge so the device doesn't retry */
def zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd) {
    def encap = cmd.encapsulatedCommand(CMD_CLASS_VERS)
    if (encap) zwaveEvent(encap)
    sendCommands([zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0,
        moreStatusUpdates: false, status: 0xFF, duration: 0)], 0)
}

/* ---------- Sensor Multilevel V11 ---------- */

def zwaveEvent(hubitat.zwave.commands.sensormultilevelv11.SensorMultilevelReport cmd) {
    if (debugLogging) log.debug "SensorMultilevelReport type=${cmd.sensorType} scale=${cmd.scale} val=${cmd.scaledSensorValue}"
    String unit = settings.tempUnit ?: "C"

    if (SENSOR_ACCEL_AXIS.containsKey(cmd.sensorType as Integer)) {
        updateAxis(SENSOR_ACCEL_AXIS[cmd.sensorType as Integer], cmd.scaledSensorValue)
        return
    }

    switch (cmd.sensorType) {
        case SENSOR_TEMPERATURE:
            def temp = round1(convertTemp(cmd.scaledSensorValue, cmd.scale == 1 ? "F" : "C", unit))
            sendEvent(name: "temperature", value: temp, unit: "°${unit}",
                descriptionText: "${device.displayName} temperature is ${temp}°${unit}")
            if (infoLogging) log.info "${device.displayName} temperature ${temp}°${unit}"
            break
        case SENSOR_HUMIDITY:
            def hum = round1(cmd.scaledSensorValue)
            sendEvent(name: "humidity", value: hum, unit: "%",
                descriptionText: "${device.displayName} humidity is ${hum}%")
            if (infoLogging) log.info "${device.displayName} humidity ${hum}%"
            break
        case SENSOR_DEW_POINT:
            def dew = round1(convertTemp(cmd.scaledSensorValue, cmd.scale == 1 ? "F" : "C", unit))
            sendEvent(name: "dewPoint", value: dew, unit: "°${unit}")
            if (infoLogging) log.info "${device.displayName} dew point ${dew}°${unit}"
            break
    }
}

/*
 * Each axis arrives in its own report, usually in a quick burst. Cache the latest
 * value per axis and publish one combined threeAxis event shortly after the burst.
 */
private void updateAxis(String axis, value) {
    Map accel = (state.accel ?: [x: 0, y: 0, z: 0]) as Map
    accel[axis] = (value as BigDecimal).setScale(2, BigDecimal.ROUND_HALF_UP)
    state.accel = accel
    runInMillis(500, "publishThreeAxis")   // overwrites any pending call, so a burst yields one event
}

def publishThreeAxis() {
    Map a = (state.accel ?: [:]) as Map
    def x = a.x ?: 0
    def y = a.y ?: 0
    def z = a.z ?: 0
    String vec = "[x:${x},y:${y},z:${z}]"
    sendEvent(name: "threeAxis", value: vec, unit: "m/s²",
        descriptionText: "${device.displayName} threeAxis is ${vec}")
    if (infoLogging) log.info "${device.displayName} threeAxis ${vec} m/s²"
}

/* ---------- Sensor Binary V2 ---------- */

def zwaveEvent(hubitat.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd) {
    boolean isActive = cmd.sensorValue != 0
    switch (cmd.sensorType) {
        case BINARY_DOOR_WINDOW:
            // Reversal (param 5) is applied by the device itself
            setContact(isActive ? "open" : "closed")
            break
        case BINARY_TILT:
            setTilt(isActive)
            break
        case BINARY_GENERAL_MOLD:
            setMold(isActive)
            break
        default:
            if (debugLogging) log.debug "SensorBinaryReport type=${cmd.sensorType} value=${cmd.sensorValue} (ignored)"
    }
}

/* ---------- Battery ---------- */

def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
    def level = cmd.batteryLevel == 0xFF ? 1 : cmd.batteryLevel
    sendEvent(name: "battery", value: level, unit: "%", descriptionText: "${device.displayName} battery ${level}%")
    if (infoLogging) log.info "${device.displayName} battery ${level}%"
}

/* ---------- Notification V8 ---------- */

def zwaveEvent(hubitat.zwave.commands.notificationv8.NotificationReport cmd) {
    if (debugLogging) log.debug "NotificationReport type=${cmd.notificationType} event=${cmd.event} params=${cmd.eventParameter}"

    switch (cmd.notificationType) {
        case 0x06: // Access Control
            // 0x16 Open: event parameter 0x01 = open in tilt position, 0x00/none = regular position
            if (cmd.event == 0x16) {
                setContact("open")
                setTilt(cmd.eventParameter?.size() > 0 && cmd.eventParameter[0] == 0x01)
            }
            else if (cmd.event == 0x17) {
                setContact("closed")
                setTilt(false)
            }
            else if (cmd.event == 0xFE) setTilt(true)
            else if (cmd.event == 0x00 && cmd.eventParameter?.contains(0xFE)) setTilt(false)
            break

        case 0x07: // Home Security
            if (cmd.event == 0x03) setTamper("detected")
            else if (cmd.event == 0x00) setTamper("clear")
            break

        case 0x08: // Power Management
            if (cmd.event == 0x02) setPowerSource("battery")
            else if (cmd.event == 0x03) setPowerSource("dc")   // USB
            else if (cmd.event == 0x01) inferPowerSource()     // "power applied" – source not stated
            else if (cmd.event == 0x0B) log.warn "${device.displayName} battery replacement needed"
            else if (cmd.event == 0x00 && cmd.eventParameter?.contains(0x03)) setPowerSource("battery") // USB removed
            else if (cmd.event == 0x00 && cmd.eventParameter?.contains(0x02)) setPowerSource("dc")      // battery no longer supplying
            break

        case 0x04: // Heat Alarm (needs high/low temperature alarm preferences)
            if (cmd.event == 0x02) setTempAlarm("high")
            else if (cmd.event == 0x06) setTempAlarm("low")
            else if (cmd.event == 0x00) setTempAlarm("normal")
            break

        case 0x10: // Weather Alarm – mold danger
            if (cmd.event == 0x02) setMold(true)
            else if (cmd.event == 0x00) setMold(false)
            break

        default:
            if (debugLogging) log.debug "Notification type ${cmd.notificationType} not handled"
    }
}

/* ---------- Configuration ---------- */

@Field static final Map PARAM_NAMES = [
    1: "Measurement interval (s)", 5: "Magnet closed state", 12: "Sensor limit enable (bits)",
    14: "High temp alarm (x0.1)", 15: "Low temp alarm (x0.1)", 22: "Binary Sensor reports",
    23: "Low battery level %", 24: "Periodic report (s)", 27: "Tilt sensor mode",
    28: "Tilt state in mode 2", 33: "Tilt trigger angle", 35: "Tilt timeout (mode 2)"
]

def zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
    String name = PARAM_NAMES[cmd.parameterNumber as Integer] ?: "Parameter"
    log.info "${device.displayName} param ${cmd.parameterNumber} (${name}) = ${cmd.scaledConfigurationValue}"
}

/* ---------- Wake Up ---------- */

def zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpNotification cmd) {
    if (infoLogging) log.info "${device.displayName} woke up"
    // Only battery-paired (sleeping) units send wake-ups; a USB notification will override this
    if (device.currentValue("powerSource") in [null, "unknown"]) setPowerSource("battery")
    List cmds = []
    if (state.pendingConfig) {
        cmds += configCmds()
        state.pendingConfig = false
        if (infoLogging) log.info "${device.displayName} applying pending configuration"
    }
    cmds += refreshCmds()
    cmds << zwave.wakeUpV2.wakeUpNoMoreInformation()
    sendCommands(cmds)
}

/* ---------- State helpers ---------- */

private void setContact(String value) {
    sendEvent(name: "contact", value: value, descriptionText: "${device.displayName} contact is ${value}")
    if (infoLogging) log.info "${device.displayName} contact is ${value}"
}

private void setTilt(boolean tilted) {
    String tilt = tilted ? "tilted" : "level"
    sendEvent(name: "acceleration", value: tilted ? "active" : "inactive",
        descriptionText: "${device.displayName} tilt state is ${tilt}")
    sendEvent(name: "tiltState", value: tilt)
    if (infoLogging) log.info "${device.displayName} tilt state is ${tilt}"
}

private void setTamper(String value) {
    sendEvent(name: "tamper", value: value, descriptionText: "${device.displayName} tamper ${value}")
    if (infoLogging) log.info "${device.displayName} tamper ${value}"
}

private void setMold(boolean detected) {
    String v = detected ? "detected" : "clear"
    sendEvent(name: "moldAlert", value: v, descriptionText: "${device.displayName} mold danger ${v}")
    if (detected) log.warn "${device.displayName} mold danger detected"
    else if (infoLogging) log.info "${device.displayName} mold danger clear"
}

private void setTempAlarm(String value) {
    sendEvent(name: "temperatureAlarm", value: value, descriptionText: "${device.displayName} temperature alarm ${value}")
    if (value != "normal") log.warn "${device.displayName} temperature alarm ${value}"
    else if (infoLogging) log.info "${device.displayName} temperature alarm normal"
}

private void setPowerSource(String value) {
    sendEvent(name: "powerSource", value: value)
    if (infoLogging) log.info "${device.displayName} power source ${value}"
}

private convertTemp(value, String from, String to) {
    BigDecimal t = value as BigDecimal
    if (from == to) return t
    return to == "F" ? (t * 9 / 5) + 32 : (t - 32) * 5 / 9
}

private BigDecimal round1(value) {
    return (value as BigDecimal).setScale(1, BigDecimal.ROUND_HALF_UP)
}

/* ---------- Commands ---------- */

def configure() {
    if (infoLogging) log.info "configure() ${device.displayName}"
    // Sent now (works if the sensor is awake or USB powered) and again at the next wake-up.
    // Tap the sensor's button once to wake it immediately.
    state.pendingConfig = true
    if (device.currentValue("powerSource") in [null, "unknown"]) inferPowerSource()
    sendCommands(configCmds() + refreshCmds())
}

/*
 * Pairing mode decides how the unit behaves: paired on USB = always listening,
 * paired on battery = sleeping. Hubitat keeps the node info frame in "zwNodeInfo";
 * bit 7 of its first byte is the "listening" flag.
 */
private void inferPowerSource() {
    try {
        String info = device.getDataValue("zwNodeInfo")
        if (info) {
            int caps = Integer.parseInt(info.trim().split(/\s+/)[0], 16)
            setPowerSource((caps & 0x80) ? "dc" : "battery")
            return
        }
    } catch (e) {
        if (debugLogging) log.debug "inferPowerSource: ${e}"
    }
    if (debugLogging) log.debug "inferPowerSource: no node info, waiting for wake-up or power notification"
}

def refresh() {
    if (infoLogging) log.info "refresh() ${device.displayName}"
    sendCommands(refreshCmds())
}

def poll() { refresh() }

private List configCmds() {
    int interval = toIntOrNull(settings.wakeUpInterval) ?: 21600

    List cmds = [
        zwave.associationV2.associationSet(groupingIdentifier: 1, nodeId: [zwaveHubNodeId]),
        zwave.wakeUpV2.wakeUpIntervalSet(seconds: interval, nodeid: zwaveHubNodeId),
        // Param 64 first: offsets and thresholds below are in this scale
        cfg(64, 1, (settings.tempUnit == "F") ? 1 : 0),
        cfg(2,  1, toTenths(settings.tempReportDelta, 2.0, 0, 255)),
        cfg(3,  1, toTenths(settings.humReportDelta, 5.0, 0, 255)),
        cfg(4,  1, (settings.ledIndication == false) ? 0 : 0x3F),
        cfg(5,  1, (settings.magnetClosedState == "open") ? 1 : 0),
        cfg(22, 1, (settings.binaryReportEnable == false) ? 0 : 1),
        cfg(25, 2, toTenths(settings.tempOffset, 0, -200, 200)),
        cfg(26, 2, toTenths(settings.humOffset, 0, -200, 200)),
        cfg(27, 1, toIntOrNull(settings.tiltSensorMode) != null ? toIntOrNull(settings.tiltSensorMode) : 1),
        cfg(28, 1, toIntOrNull(settings.tiltModeState) == 1 ? 1 : 0),
        cfg(33, 1, Math.max(1, Math.min(90, toIntOrNull(settings.tiltAngle) ?: 5))),
        cfg(1,  4, Math.max(30, Math.min(65535, toIntOrNull(settings.checkInterval) ?: 900))),
        cfg(23, 1, Math.max(10, Math.min(50, toIntOrNull(settings.lowBatteryLevel) ?: 20)))
    ]

    // Temperature alarms: params 14/15 hold the thresholds (x0.1), param 12 bit 0 = high, bit 2 = low
    BigDecimal hi = toDecimalOrNull(settings.highTempAlarm)
    BigDecimal lo = toDecimalOrNull(settings.lowTempAlarm)
    int limitBits = 0
    if (hi != null) {
        cmds << cfg(14, 2, Math.max(1, Math.min(1000, (hi * 10).setScale(0, BigDecimal.ROUND_HALF_UP).intValue())))
        limitBits |= 0x01
    }
    if (lo != null) {
        cmds << cfg(15, 2, Math.max(-200, Math.min(1000, (lo * 10).setScale(0, BigDecimal.ROUND_HALF_UP).intValue())))
        limitBits |= 0x04
    }
    cmds << cfg(12, 1, limitBits)
    if (hi == null && lo == null && device.currentValue("temperatureAlarm") != "normal") setTempAlarm("normal")

    Integer p = toIntOrNull(settings.periodicReport)
    if (p != null) {
        if (p != 0 && p < 30) p = 30
        cmds << cfg(24, 4, Math.min(p, 65535))
    }

    BigDecimal accRaw = toDecimalOrNull(settings.accReportDelta)
    if (accRaw != null && accRaw > 0 && accRaw < 1) {
        // Leftover m/s² value (e.g. 0.05) from the previous driver version; not valid for param 36
        log.warn "${device.displayName} ignoring old acceleration delta ${accRaw}; set a value of 0 or 30–255"
        device.removeSetting("accReportDelta")
    } else if (accRaw != null) {
        int a = accRaw.setScale(0, BigDecimal.ROUND_HALF_UP).intValue()
        if (a != 0 && a < 30) a = 30
        cmds << cfg(36, 1, Math.min(a, 255))
    }

    // Optional readback so the log shows what the device actually has
    if (settings.readbackConfig != false) {
        PARAM_NAMES.keySet().each { cmds << zwave.configurationV1.configurationGet(parameterNumber: it) }
    }
    return cmds
}

private List refreshCmds() {
    return [
        zwave.batteryV1.batteryGet(),
        zwave.sensorMultilevelV11.sensorMultilevelGet(sensorType: SENSOR_TEMPERATURE),
        zwave.sensorMultilevelV11.sensorMultilevelGet(sensorType: SENSOR_HUMIDITY),
        zwave.sensorMultilevelV11.sensorMultilevelGet(sensorType: SENSOR_DEW_POINT),
        zwave.sensorMultilevelV11.sensorMultilevelGet(sensorType: 0x34),
        zwave.sensorMultilevelV11.sensorMultilevelGet(sensorType: 0x35),
        zwave.sensorMultilevelV11.sensorMultilevelGet(sensorType: 0x36),
        zwave.sensorBinaryV2.sensorBinaryGet(sensorType: BINARY_DOOR_WINDOW),
        zwave.sensorBinaryV2.sensorBinaryGet(sensorType: BINARY_TILT),
        zwave.sensorBinaryV2.sensorBinaryGet(sensorType: BINARY_GENERAL_MOLD),
        zwave.notificationV8.notificationGet(notificationType: 0x08, event: 0x02),
        zwave.notificationV8.notificationGet(notificationType: 0x08, event: 0x03),
        zwave.notificationV8.notificationGet(notificationType: 0x07, event: 0x00)
    ]
}

private BigDecimal toDecimalOrNull(value) {
    if (value == null || value.toString().trim() == "") return null
    try { return new BigDecimal(value.toString().trim()) } catch (e) { return null }
}

private Integer toIntOrNull(value) {
    BigDecimal d = toDecimalOrNull(value)
    return d == null ? null : d.setScale(0, BigDecimal.ROUND_HALF_UP).intValue()
}

private cfg(int param, int size, int value) {
    return zwave.configurationV1.configurationSet(parameterNumber: param, size: size, scaledConfigurationValue: value)
}

private int toTenths(value, defaultValue, int min, int max) {
    BigDecimal v = (value != null ? value : defaultValue) as BigDecimal
    int raw = Math.round(v * 10) as Integer
    return Math.max(min, Math.min(max, raw))
}

private void sendCommands(List cmds, Long delay = 300) {
    List formatted = cmds.collect { zwaveSecureEncap(it.format()) }
    List actions = delay > 0 ? delayBetween(formatted, delay) : formatted
    sendHubCommand(new hubitat.device.HubMultiAction(actions, hubitat.device.Protocol.ZWAVE))
}
