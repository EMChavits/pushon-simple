/**
 * PushOn (Simple) — App v1.1.0
 *
 * PushOn – Simple Pushover Notifications for Hubitat
 * Status: Feature complete • Maintenance: bugfixes only
 *
 * UX contract (locked):
 * - Works with Rule Machine → Actions → Send / Speak a Message
 * - No Custom Actions, no parameterised commands
 * - Four fixed devices (Low / Normal / High / Emergency)
 * - App-level test button only (fixed payload), no device test command
 *
 * v1.1.0 adds: OPTIONAL expert Emergency timing override (retry/expire)
 * - Hidden behind explicit confirmation checkbox
 * - Strict validation with safe failure (reject + disable + restore defaults)
 */

import groovy.transform.Field

@Field static final String VARIANT    = "simple"
@Field static final String VERSION    = "1.1.0"
@Field static final String LOG_PREFIX = "[PushOn Simple v1.1.0]"

// LOCKED namespace
@Field static final String NS          = "EMC.pushon.simple"
@Field static final String DRIVER_NAME = "PushOn Simple Notification Device"

// Fixed device labels (UX locked)
@Field static final Map<String, String> DEVICE_LABELS = [
    low      : "PushOn - Low",
    normal   : "PushOn - Normal",
    high     : "PushOn - High",
    emergency: "PushOn - Emergency"
]

// Fixed Pushover priorities
@Field static final Map<String, Integer> DEVICE_PRIORITIES = [
    low      : -1,
    normal   :  0,
    high     :  1,
    emergency:  2
]

// Default Emergency behaviour (v1.0.0 baseline)
@Field static final Integer DEFAULT_EMERGENCY_RETRY_SEC  = 300   // 5 minutes
@Field static final Integer DEFAULT_EMERGENCY_EXPIRE_SEC = 3600  // 1 hour

// Expert override validation rules (LOCKED for v1.1.0)
@Field static final Integer MIN_EXPERT_RETRY_SEC  = 60
@Field static final Integer MIN_EXPERT_EXPIRE_SEC = 60
@Field static final Integer MAX_EXPERT_EXPIRE_SEC = 10800  // Pushover limit (3 hours)

// Fixed App-level test payload
@Field static final String TEST_MESSAGE = "PushOn Simple test notification"

definition(
    name       : "PushOn",
    namespace  : "EMC.pushon.simple",
    author     : "Eugene M Chavits",
    description: "Simple Pushover Notifications for Hubitat (simple v1.1.0)",
    category   : "Notifications",
    iconUrl    : "https://raw.githubusercontent.com/hubitat/HubitatPublic/master/images/icons/notifications.png",
    iconX2Url  : "https://raw.githubusercontent.com/hubitat/HubitatPublic/master/images/icons/notifications@2x.png"
)

preferences {
    page(name: "mainPage")
    page(name: "devicePage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "PushOn (Simple) — v${VERSION}", install: true, uninstall: true) {

        // Validation warnings (shown near top, as per UI blueprint)
        if (state?.validationError) {
            section("WARNING") {
                paragraph "⚠ Settings not saved\n\nEmergency timing override was rejected.\nDefaults have been restored.\n\nReason:\n${state.validationError}"
            }
        }

        section("ABOUT THIS APP") {
            paragraph "PushOn Simple is a lightweight Hubitat app for sending Pushover notifications at fixed priorities.\nIt is intentionally limited by design to keep notifications predictable, easy to understand, and easy to maintain. \nAs a result, advanced customisation is out of scope."
        }

        section("PUSHOVER CREDENTIALS") {
            input name: "appToken", type: "password", title: "Application API Token", required: true
            input name: "userKey",  type: "password", title: "User Key",              required: true

            paragraph "Credentials status: ${credentialsOk() ? 'Configured' : 'Missing'}"
            paragraph "<em>Note: This status updates after you click Done, but credentials are applied only when you click “Create / Update notification devices”.</em>"
        }

        section("OPTIONAL TITLE PREFIXES (per device)") {
            paragraph "If set, the prefix will be used as the message title for that device.\nExample: HOME ALERTS, SECURITY, BATTERY"
            input name: "titlePrefixLow",       type: "text", title: "PushOn – Low",       required: false
            input name: "titlePrefixNormal",    type: "text", title: "PushOn – Normal",    required: false
            input name: "titlePrefixHigh",      type: "text", title: "PushOn – High",      required: false
            input name: "titlePrefixEmergency", type: "text", title: "PushOn – Emergency", required: false
        }

        section("ADVANCED SETTINGS (Emergency Timing - Expert Use Only)") {

            input name: "enableExpertEmergencyTiming",
                  type: "bool",
                  title: "I understand these settings override PushOn Simple defaults",
                  required: false,
                  defaultValue: false,
                  submitOnChange: true

            if (settings?.enableExpertEmergencyTiming) {

                paragraph "Override the default Emergency retry and expiry timings.\nIf these values are too low, Emergency notifications may repeat too often or stop sooner than expected."

                input name: "emergencyRetrySec",
                      type: "number",
                      title: "Emergency retry interval (seconds)",
                      required: true,
                      defaultValue: DEFAULT_EMERGENCY_RETRY_SEC

                input name: "emergencyExpireSec",
                      type: "number",
                      title: "Emergency expiry time (seconds)",
                      required: true,
                      defaultValue: DEFAULT_EMERGENCY_EXPIRE_SEC

                paragraph "Validation rules:\n• Retry ≥ ${MIN_EXPERT_RETRY_SEC} seconds\n• Expiry ≥ ${MIN_EXPERT_EXPIRE_SEC} seconds\n• Expiry ≤ ${MAX_EXPERT_EXPIRE_SEC} seconds\n• Expiry ≥ Retry"

            } else {
                paragraph "Defaults in use:\n• Retry: ${DEFAULT_EMERGENCY_RETRY_SEC} seconds\n• Expiry: ${DEFAULT_EMERGENCY_EXPIRE_SEC} seconds"
            }
        }

        section("DEVICE") {
            href name: "deviceLink",
                 title: "Create / Update notification devices",
                 description: "Creates or updates the 4 PushOn notification devices. Safe to run any time.",
                 page: "devicePage"
        }

        section("TEST NOTIFICATION (optional)") {
            paragraph "Sends a Normal priority test notification using a fixed message payload."
            input name: "btnTest", type: "button", title: "Send test notification"
        }

        section("WHAT NEXT") {
            paragraph "In Rule Machine, use: Actions → Send/Speak a Message."
            paragraph "Choose the appropriate PushOn device: Low, Normal, High, or Emergency."
        }

        section("NOTES") {
            paragraph "Low and Normal notifications respect quiet hours."
            paragraph "High and Emergency notifications bypass quiet hours, as per Pushover behaviour."
            paragraph "Emergency notifications repeat until acknowledged.\nThe default retry/expiry is 5 minutes / 1 hour unless an expert override is enabled and valid."
            paragraph "PushOn Simple is feature complete and in maintenance mode (bugfixes only)."
        }
    }
}

def devicePage() {
    dynamicPage(name: "devicePage", title: "Device setup", install: false, uninstall: false) {
        section("Result") {
            paragraph ensureDevices()
        }
        section("Next steps") {
            paragraph "Rule Machine → Actions → Send / Speak a Message → select one of the PushOn devices."
        }
    }
}

def appButtonHandler(btn) {
    if (btn == "btnTest") {
        state.lastTest = sendTest()
    }
}

def installed() {
    log.info "${LOG_PREFIX} installed"
    ensureDevices()
}

def updated() {
    log.info "${LOG_PREFIX} updated"

    // Apply v1.1.0 expert override validation with safe failure
    def vr = validateExpertEmergencyTiming()
    if (!vr.ok) {
        // Reject override, disable it, restore defaults, show warning
        state.validationError = vr.msg

        // Force safe defaults & disable override
        app.updateSetting("enableExpertEmergencyTiming", [value: false, type: "bool"])
        app.updateSetting("emergencyRetrySec",  [value: DEFAULT_EMERGENCY_RETRY_SEC,  type: "number"])
        app.updateSetting("emergencyExpireSec", [value: DEFAULT_EMERGENCY_EXPIRE_SEC, type: "number"])

        // Ensure devices receive default timings (never apply invalid values)
        ensureDevices()
        return
    }

    // Clear any previous validation error on a good save
    state.validationError = null

    ensureDevices()
}

def uninstalled() {
    getChildDevices()?.each { d ->
        try {
            deleteChildDevice(d.deviceNetworkId)
        } catch (e) {
            log.warn "${LOG_PREFIX} Could not delete child device ${d?.label}: ${e?.class?.simpleName}"
        }
    }
    log.info "${LOG_PREFIX} uninstalled"
}

private boolean credentialsOk() {
    return safeTrim(settings?.appToken) && safeTrim(settings?.userKey)
}

private Map validateExpertEmergencyTiming() {
    // If expert override is OFF, always OK (defaults apply)
    if (!(settings?.enableExpertEmergencyTiming)) {
        return [ok: true, msg: null]
    }

    Integer retry  = toInt(settings?.emergencyRetrySec)
    Integer expire = toInt(settings?.emergencyExpireSec)

    if (retry == null)  return [ok: false, msg: "Retry must be set."]
    if (expire == null) return [ok: false, msg: "Expiry must be set."]

    if (retry < MIN_EXPERT_RETRY_SEC) {
        return [ok: false, msg: "Retry must be at least ${MIN_EXPERT_RETRY_SEC} seconds."]
    }
    if (expire < MIN_EXPERT_EXPIRE_SEC) {
        return [ok: false, msg: "Expiry must be at least ${MIN_EXPERT_EXPIRE_SEC} seconds."]
    }
    if (expire > MAX_EXPERT_EXPIRE_SEC) {
        return [ok: false, msg: "Expiry cannot exceed ${MAX_EXPERT_EXPIRE_SEC} seconds (Pushover limit)."]
    }
    if (expire < retry) {
        return [ok: false, msg: "Expiry must be equal to or longer than the retry interval."]
    }

    return [ok: true, msg: null]
}

private String ensureDevices() {
    if (!credentialsOk()) {
        return "Credentials are missing. Enter your Application API Token and User Key, then click Done."
    }

    def out = []
    DEVICE_LABELS.each { String key, String label ->
        def dni = childDni(key)
        def dev = getChildDevice(dni)

        if (!dev) {
            try {
                dev = addChildDevice(NS, DRIVER_NAME, dni, [label: label, name: label, isComponent: false])
                out << "Created: ${label}"
            } catch (e) {
                log.error "${LOG_PREFIX} Failed to create ${label}: ${e}"
                out << "FAILED to create: ${label}. Check logs."
                return out.join("\n")
            }
        } else {
            // Keep labels aligned with locked UX naming
            if (dev.label != label) {
                try {
                    dev.setLabel(label)
                    out << "Updated label: ${label}"
                } catch (e) {
                    log.warn "${LOG_PREFIX} Could not update label for ${dev?.deviceNetworkId}: ${e?.class?.simpleName}"
                }
            }
        }

        // Per-device config push (including optional expert emergency timing)
        pushConfig(dev, DEVICE_PRIORITIES[key], titleFor(key), emergencyTimingFor(key))
    }

    return out ? out.join("\n") : "Devices are present and up-to-date."
}

private String sendTest() {
    if (!credentialsOk()) {
        return "Cannot send: credentials are missing. Enter your token/user key and click Done."
    }

    // Ensure devices/config exist
    ensureDevices()

    def dev = getChildDevice(childDni("normal"))
    if (!dev) {
        log.warn "${LOG_PREFIX} Test failed: PushOn – Normal device not found."
        return "Failed: PushOn – Normal device not found. Run “Create / Update notification devices”."
    }

    try {
        dev.deviceNotification(TEST_MESSAGE)
        log.info "${LOG_PREFIX} test notification attempted (Normal)"
        return "Attempted (Normal). If it does not arrive, check Hubitat logs."
    } catch (e) {
        log.warn "${LOG_PREFIX} Test send exception: ${e?.class?.simpleName}"
        return "Failed: exception sending test. Check Hubitat logs."
    }
}

private void pushConfig(def dev, Integer pri, String prefix, Map timing) {
    // Never log secrets
    dev.updateDataValue("pushon.token",   safeTrim(settings?.appToken))
    dev.updateDataValue("pushon.user",    safeTrim(settings?.userKey))
    dev.updateDataValue("pushon.pri",     (pri ?: 0).toString())
    dev.updateDataValue("pushon.prefix",  prefix ?: "")
    dev.updateDataValue("pushon.variant", VARIANT)
    dev.updateDataValue("pushon.version", VERSION)

    // Emergency timing override is only relevant for the Emergency device
    if (pri == 2) {
        Integer r = timing?.retry as Integer
        Integer e = timing?.expire as Integer
        dev.updateDataValue("pushon.retry",  (r ?: DEFAULT_EMERGENCY_RETRY_SEC).toString())
        dev.updateDataValue("pushon.expire", (e ?: DEFAULT_EMERGENCY_EXPIRE_SEC).toString())
    } else {
        // Ensure non-emergency devices do not carry timing data values (belt-and-braces)
        dev.removeDataValue("pushon.retry")
        dev.removeDataValue("pushon.expire")
    }
}

private Map emergencyTimingFor(String key) {
    if (key != "emergency") return null

    if (!(settings?.enableExpertEmergencyTiming)) {
        return [retry: DEFAULT_EMERGENCY_RETRY_SEC, expire: DEFAULT_EMERGENCY_EXPIRE_SEC]
    }

    // Only return timing if valid; otherwise defaults (invalid values are rejected in updated(), but belt-and-braces)
    def vr = validateExpertEmergencyTiming()
    if (!vr.ok) {
        return [retry: DEFAULT_EMERGENCY_RETRY_SEC, expire: DEFAULT_EMERGENCY_EXPIRE_SEC]
    }

    Integer retry  = toInt(settings?.emergencyRetrySec)  ?: DEFAULT_EMERGENCY_RETRY_SEC
    Integer expire = toInt(settings?.emergencyExpireSec) ?: DEFAULT_EMERGENCY_EXPIRE_SEC
    return [retry: retry, expire: expire]
}

private String titleFor(String key) {
    switch (key) {
        case "low":       return safeTrim(settings?.titlePrefixLow)
        case "normal":    return safeTrim(settings?.titlePrefixNormal)
        case "high":      return safeTrim(settings?.titlePrefixHigh)
        case "emergency": return safeTrim(settings?.titlePrefixEmergency)
        default:          return null
    }
}

private String childDni(String suffix) {
    // Stable DNIs so devices update instead of recreating
    return "pushon:${VARIANT}:${app.id}:${suffix}"
}

private static Integer toInt(def v) {
    try {
        if (v == null) return null
        return Integer.parseInt(v.toString().trim())
    } catch (ignored) {
        return null
    }
}

private static String safeTrim(def v) {
    def s = v?.toString()?.trim()
    return s ? s : null
}
