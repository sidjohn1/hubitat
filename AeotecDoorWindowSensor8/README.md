# Aeotec Door Window Sensor 8 (ZWA055) – Hubitat Driver

A Hubitat Elevation driver for the **Aeotec Door / Window Sensor 8 (ZWA055)**. It reports open/closed, tilt, temperature, humidity, dew point, 3-axis acceleration, tamper, mold danger, temperature alarms, battery and power source, and exposes the sensor's main configuration parameters as plain-language preferences.

> **Only for the Door / Window Sensor 8.** Do not use this driver on a Door / Window Sensor 7 (ZWA008, ZWA011 or ZWA012). Those models use different configuration parameter numbers, and this driver would change settings you didn't intend to.

---

## Features

| Feature | Hubitat attribute | Source on the sensor |
|---|---|---|
| Open / closed | `contact` | Notification (Access Control) and Binary Sensor 0x0A |
| Tilt | `tiltState` (tilted / level), `acceleration` (active / inactive) | Notification (open in tilt position, 0xFE) and Binary Sensor 0x0B |
| 3-axis acceleration | `threeAxis` (m/s²) | Multilevel Sensor 0x34 / 0x35 / 0x36 |
| Temperature | `temperature` | Multilevel Sensor 0x01 |
| Humidity | `humidity` | Multilevel Sensor 0x05 |
| Dew point | `dewPoint` | Multilevel Sensor 0x0B (calculated by the sensor) |
| Tamper | `tamper` (clear / detected) | Notification (Home Security) |
| Mold danger | `moldAlert` (clear / detected) | Notification (Weather Alarm) and Binary Sensor 0x01 |
| Temperature alarm | `temperatureAlarm` (normal / high / low) | Notification (Heat Alarm) |
| Battery | `battery` | Battery report |
| Power source | `powerSource` (battery / dc) | Notification (Power Management), pairing mode, wake-ups |

The driver also:

- Supports S2 security (commands are encapsulated and supervision requests are acknowledged).
- Queues configuration changes and applies them at the next wake-up, since a battery-paired sensor is asleep most of the time.
- Can read settings back from the sensor and log them, so you can confirm what the device actually has.
- Turns debug logging off automatically after 30 minutes.

---

## Installation

### Option 1: Hubitat Package Manager (recommended)

Installing through [Hubitat Package Manager (HPM)](https://community.hubitat.com/t/release-hubitat-package-manager-hpm-hubitatcommunity/94471) lets HPM notify you of updates and install them for you.

**Add the custom repository (one time):**

1. In Hubitat, open **Apps → Hubitat Package Manager**.
2. Choose **Package Manager Settings**.
3. Choose **Add a Custom Repository**.
4. Paste this URL and click **Save**:
   ```
   https://raw.githubusercontent.com/sidjohn1/hubitat/main/repository.json
   ```
5. Click **Done** to return to the main HPM menu.

**Install the driver:**

1. In HPM, choose **Install**, then **Search by Keywords**.
2. Search for `Aeotec Door Window Sensor 8` and select it from the results.
3. Click **Next** and confirm to install. HPM adds the driver under **Drivers Code**.

**Or install directly from the manifest** (no custom repository needed):

1. In HPM, choose **Install**, then **From a URL**.
2. Paste this URL and click **Next**:
   ```
   https://raw.githubusercontent.com/sidjohn1/hubitat/main/AeotecDoorWindowSensor8/packageManifest.json
   ```
3. Confirm to install.

**Already installed the driver by hand?** In HPM, choose **Match Up** so HPM recognizes the existing copy and can update it.

**Updating:** in HPM, choose **Update**, select **Aeotec Door Window Sensor 8**, and confirm. After updating, wake the sensor so it picks up any new settings.

### Option 2: Manual install

1. In Hubitat, go to **Drivers Code → New Driver**.
2. Click **Import**, paste this URL, and click **Import**, then **Save**:
   ```
   https://raw.githubusercontent.com/sidjohn1/hubitat/main/AeotecDoorWindowSensor8/AeotecDoorWindowSensor8DTH.groovy
   ```
   Or paste the contents of `AeotecDoorWindowSensor8DTH.groovy` directly and click **Save**.

To update a manual install, open the driver in **Drivers Code**, click **Import** again, and **Save**.

### After installing

1. Pair the sensor (see below), or open an existing sensor's device page and change **Type** to **Aeotec Door Window Sensor 8**.
2. Set your preferences and click **Save Preferences**.
3. **Tap the sensor's button once** to wake it so it receives the settings.

### Pairing: USB vs battery

How the sensor is powered *while pairing* decides how it behaves afterwards:

- **Paired on USB:** always listening. Changes reach it immediately and it uses the battery only as a backup. It won't switch to sleeping mode if you later unplug USB.
- **Paired on battery:** a sleeping device. It only talks to the hub when it wakes up, on its wake-up interval or when you tap the button.

To pair, put Hubitat into Z-Wave inclusion mode and **tap the sensor's button twice within 1 second**. SmartStart (QR code) also works.

### Updating from an older version of this driver

The original driver stored the acceleration threshold as a decimal (for example `0.05`). The current driver detects that leftover value, logs a warning, and removes it. After updating, open the preferences once, check each setting, and click **Save Preferences**.

---

## Preferences

Values marked with a parameter number are written to the sensor.

### Measurement and reporting

| Preference | Options / range | Default | Param |
|---|---|---|---|
| Temperature unit | Celsius / Fahrenheit (also sets the sensor's reporting scale) | Celsius | 64 |
| Temperature offset | −20 to 20, in the unit above | 0 | 25 |
| Humidity offset | −20 to 20 % | 0 | 26 |
| Minimum temperature change to report | 0 (disabled) to 25.5 | 2.0 | 2 |
| Minimum humidity change to report | 0 (disabled) to 25.5 % | 5.0 | 3 |
| Minimum acceleration change to report | 0 (disabled) or 30–255; blank keeps the device default | blank | 36 |
| Periodic report interval | Disabled (recommended), 1, 4, 6, 8, 12 (default), 18 hours | 12 hours | 24 |
| Measurement interval | 5, 10, 15 (default), 30, 60 minutes | 15 minutes | 1 |

The periodic report interval tops out at 18 hours because the sensor's maximum for this parameter is 65535 seconds.

### Alarms and battery

| Preference | Options / range | Default | Param |
|---|---|---|---|
| High temperature alarm | 0.1 to 100, in the unit above; blank = off | off | 14 (+ bit 0 of 12) |
| Low temperature alarm | −20 to 100, in the unit above; blank = off | off | 15 (+ bit 2 of 12) |
| Low battery warning level | 10–50 % | 20 % | 23 |

### Contact and tilt

| Preference | Options / range | Default | Param |
|---|---|---|---|
| Report closed when the magnet is… | Near (normal) / Far away (reversed) | Near | 5 |
| Tilt sensor mode | Disabled / Enabled (with magnet) / Enabled (sensor alone, no magnet) | Enabled (with magnet) | 27 |
| Sensor-alone mode: contact state when tilted | Open when tilted / Closed when tilted | Open when tilted | 28 |
| Tilt trigger angle | 1–90° | 5° | 33 |
| Enable Binary Sensor reports | On / off (door, tilt, mold) | On | 22 |

### Device and driver

| Preference | Options / range | Default | Param |
|---|---|---|---|
| Enable LED indication | On / off (all LED indications) | On | 4 |
| Wake Up Interval | 1, 4, 6, 8, 12, 24 hours | 6 hours | Wake Up interval |
| Read settings back after configuring | On / off | On | — |
| Enable debug logging | On / off; turns off after 30 minutes | On | — |
| Enable description text logging | On / off | On | — |

---

## How it works

### Tilt modes

| Mode | What decides open / closed | When tilt is checked | What you see in Hubitat |
|---|---|---|---|
| **Disabled** | Magnet only | Never | `tiltState` stays level |
| **Enabled (with magnet)** | Magnet | Only after the magnet moves away, then after a 5-second timeout | contact open + tilted, or contact open + level |
| **Enabled (sensor alone)** | The sensor's own angle, past the trigger angle for 8 seconds | Continuously | contact open + tilted, or contact closed + level (direction set by parameter 28) |

"With magnet" mode is designed for tilt-and-turn windows. In that mode, picking the sensor up while the magnet is near does nothing. "Sensor alone" suits garage doors, hatches and anything without a magnet.

### Three-axis acceleration

The sensor sends X, Y and Z as three separate reports. The driver keeps the latest value for each axis and, about half a second after the last one, sends a single `threeAxis` event such as `[x:0.12,y:-9.78,z:0.40]` (m/s²). Values are read as signed numbers, per the Z-Wave standard.

Acceleration is reported on the periodic report interval, when it changes by more than the acceleration threshold (if set), and whenever the driver requests it (Configure, Refresh and every wake-up).

### Power source

The sensor only sends a power-source notification when power changes. To fill the value in sooner, the driver:

1. reads the pairing mode from Hubitat's stored node info on Configure (listening = `dc`, sleeping = `battery`),
2. sets `battery` on the first wake-up if the value is still unknown (only battery-paired units wake up),
3. uses the sensor's notification whenever USB is plugged in or removed. USB power is reported as `dc`.

### Battery life

Every wake-up already requests all readings, so periodic reports largely duplicate that work. Pick one approach:

- **Recommended:** set Periodic report interval to **Disabled** and set Wake Up Interval to how often you want fresh values.
- Or use a long wake-up interval (12–24 hours) and let periodic reports handle regular updates.

Measuring every 15 minutes is cheap; the sensor only transmits when a value changes past your thresholds. Once your settings are confirmed, turn off **Read settings back after configuring**.

---

## Troubleshooting

**Settings don't seem to take effect.**
A battery-paired sensor only receives settings while awake. After saving, tap the button once. The log should show `applying pending configuration`, followed by `param …` lines showing each value the sensor now has.

**No `param …` lines appear after tapping the button.**
The sensor isn't receiving commands from the driver. Turn on debug logging, tap the button again, and check what arrives.

**Tilt only changes when the magnet moves.**
The sensor is in "with magnet" mode (parameter 27 = 1). Set Tilt sensor mode to "Enabled (sensor alone, no magnet)", save, and wake the sensor.

**`threeAxis` is empty.**
Tap the button and look in the debug log for `SensorMultilevelReport type=52`, `53` and `54`. If you see other type numbers, or none, the sensor is reporting differently from the Z-Wave standard.

**Power source shows `unknown`.**
That value is left over from an older driver. Click Configure and tap the button; it will be replaced.

**Temperature alarm never changes.**
Set at least one alarm threshold. With both blank, the alarms are turned off on the sensor.

---

## Known limitations

- **`threeAxis` format:** sent as `[x:..,y:..,z:..]`. Confirm your dashboard tile reads it; if not, the format is a one-line change in `publishThreeAxis()`.
- **Acceleration threshold units:** Aeotec's manual doesn't state the units for parameter 36. The driver passes your value straight through.
- **Wake Up Interval default:** the manual doesn't give the sensor's factory value; 6 hours is the driver's default.
- **Readback:** the wake-up interval isn't a numbered parameter, so it isn't included in the settings readback.

---

## Configuration parameter reference

Parameters this driver writes, as documented in Aeotec's ZWA055 user guide.

| Param | Size | Purpose | Driver preference |
|---|---|---|---|
| 1 | 4 | Threshold check (measurement) time, seconds | Measurement interval |
| 2 | 1 | Minimum temperature change (×0.1) | Minimum temperature change |
| 3 | 1 | Minimum humidity change (×0.1) | Minimum humidity change |
| 4 | 1 | LED indication bit field | Enable LED indication |
| 5 | 1 | State when the magnet is closed | Report closed when the magnet is… |
| 12 | 1 | Sensor limit enable bit field | Set automatically from the alarm thresholds |
| 14 | 2 | Temperature upper watermark (×0.1) | High temperature alarm |
| 15 | 2 | Temperature lower watermark (×0.1) | Low temperature alarm |
| 22 | 1 | Enable Binary Sensor reports | Enable Binary Sensor reports |
| 23 | 1 | Low battery threshold % | Low battery warning level |
| 24 | 4 | Periodic reports, seconds | Periodic report interval |
| 25 | 2 | Temperature offset (×0.1) | Temperature offset |
| 26 | 2 | Humidity offset (×0.1) | Humidity offset |
| 27 | 1 | Tilt sensor mode | Tilt sensor mode |
| 28 | 1 | State of tilt in mode 2 | Sensor-alone mode: contact state when tilted |
| 33 | 1 | Tilt triggered angle | Tilt trigger angle |
| 36 | 1 | Minimum acceleration change | Minimum acceleration change |
| 64 | 1 | Temperature and dew point scale | Temperature unit |

The settings readback also requests parameter 35 (tilt timeout in mode 2) for reference.

---

## References

- [Aeotec Door / Window Sensor 8 user guide (ZWA055)](https://aeotec.freshdesk.com/support/solutions/articles/6000278047-door-window-sensor-8-user-guide-zwa055-)
- [Hubitat driver capability list](https://docs2.hubitat.com/en/developer/driver/capability-list)
