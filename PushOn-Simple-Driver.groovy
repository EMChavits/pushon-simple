/**
 * PushOn Simple Notification Device — Driver v1.1.0
 *
 * Status: Feature complete • Maintenance: bugfixes only
 *
 * Implements Hubitat Notification capability for Rule Machine:
 * Actions → Send / Speak a Message
 *
 * Locked behaviours:
 * - Fixed priority per device (data value)
 * - Emergency retry/expire defaults to 5 minutes / 1 hour
 * - OPTIONAL expert override for Emergency retry/expire (validated + safe fallback)
 * - Silent duplicate suppression (3 minutes)
 * - Ignore empty/whitespace-only messages (silent)
 * - No custom commands
 *
 * Security:
 * - Never log token/user key
 * - Sanitise errors
 */

import groovy.transform.Field

@Field static final String VARIANT    = "simple"
@Field static final String VERSION    = "1.1.0"
@Field static final String LOG_PREFIX = "[PushOn Simple v1.1.0]"

@Field static final String API_URL = "https://api.pushover.net/1/messages.json"

// Default Emergency behaviour (v1.0.0 baseline)
@Field static final Integer DEFAULT_EMERGENCY_RETRY_SEC  = 300   // 5 minutes
@Field static final Integer DEFAULT_EMERGENCY_EXPIRE_SEC = 3600  // 1 hour

// Expert override validation rules (LOCKED for v1.1.0)
@Field static final Integer MIN_EXPERT_RETRY_SEC  = 60
@Field static final Integer MIN_EXPERT_EXPIRE_SEC = 60
@Field static final Integer MAX_EXPERT_EXPIRE_SEC = 10800  // Pushover limit (3 hours)

// Duplicate suppression window (spec allows 2-5 minutes; chosen 3 minutes)
@Field static final Integer DEDUPE_WINDOW_SEC = 180

metadata {
    definition(
        name     : "PushOn Simple Notification Device",
        namespace: "EMC.pushon.simple",
        author   : "Eugene M Chavits"
    ) {
        capability "Notification"
        capability "Actuator"
    }

    preferences {
        // Optional and off by default; keeps logs quiet for normal users
        input name: "enableDebug", type: "bool", title: "Enable debug logging", required: false, defaultValue: false
    }
}

def deviceNotification(String message) {
    sendPush(message)
}

private void sendPush(String message) {
    String msg = (message ?: "").trim()

    // Empty/whitespace-only protection (silent; debug-only note)
    if (!msg) {
        logDebug "Ignored empty/whitespace message"
        return
    }

    // Pushover message limit
    if (msg.size() > 1024) {
        msg = msg.take(1024)
    }

    // Silent duplicate suppression
    if (isDuplicate(msg)) {
        logDebug "Suppressed duplicate within ${DEDUPE_WINDOW_SEC}s"
        return
    }

    String token = device.getDataValue("pushon.token")
    String user  = device.getDataValue("pushon.user")

    if (!token || !user) {
        log.warn "${LOG_PREFIX} Cannot send: missing credentials. Open PushOn app and click Done."
        return
    }

    Integer pri = parsePriority(device.getDataValue("pushon.pri"))
    String title = (device.getDataValue("pushon.prefix") ?: "").trim()

    Map body = [
        token   : token,
        user    : user,
        message : msg,
        priority: pri
    ]

    if (title) {
        body.title = title
    }

    if (pri == 2) {
        Map timing = resolveEmergencyTiming()
        body.retry  = timing.retry
        body.expire = timing.expire
    }

    // Commit dedupe state only when attempting a send
    commitLastMessage(msg)

    logDebug "Sending: priority=${pri}, titlePrefix=${title ? '(set)' : '(none)'}, len=${msg.size()}"

    try {
        httpPost(
            [
                uri               : API_URL,
                requestContentType: "application/x-www-form-urlencoded",
                body              : body,
                timeout           : 10
            ]
        ) { resp ->
            Integer status = resp?.status as Integer
            if (status == 200) {
                logDebug "Sent OK (HTTP 200)"
            } else {
                log.warn "${LOG_PREFIX} Send failed (HTTP ${status ?: 'unknown'})."
            }
        }
    } catch (Exception e) {
        log.warn "${LOG_PREFIX} Send exception: ${e.class.simpleName}: ${safeErr(e.message)}"
    }
}

private Map resolveEmergencyTiming() {
    Integer retry  = toInt(device.getDataValue("pushon.retry"))
    Integer expire = toInt(device.getDataValue("pushon.expire"))

    // Safe fallback to defaults if values are missing or invalid
    if (!isValidEmergencyTiming(retry, expire)) {
        return [retry: DEFAULT_EMERGENCY_RETRY_SEC, expire: DEFAULT_EMERGENCY_EXPIRE_SEC]
    }

    return [retry: retry, expire: expire]
}

private static boolean isValidEmergencyTiming(Integer retry, Integer expire) {
    if (retry == null || expire == null) return false
    if (retry < MIN_EXPERT_RETRY_SEC) return false
    if (expire < MIN_EXPERT_EXPIRE_SEC) return false
    if (expire > MAX_EXPERT_EXPIRE_SEC) return false
    if (expire < retry) return false
    return true
}

private static Integer parsePriority(String p) {
    try {
        return Integer.parseInt((p ?: "0").toString())
    } catch (ignored) {
        return 0
    }
}

private static Integer toInt(def v) {
    try {
        if (v == null) return null
        return Integer.parseInt(v.toString().trim())
    } catch (ignored) {
        return null
    }
}

private boolean isDuplicate(String msg) {
    Long lastAt = (state.lastMsgAt as Long) ?: null
    String lastMsg = (state.lastMsg as String) ?: null

    if (!lastAt || !lastMsg) return false
    if (lastMsg != msg) return false

    Long ageMs = now() - lastAt
    return (ageMs >= 0 && ageMs <= (DEDUPE_WINDOW_SEC * 1000L))
}

private void commitLastMessage(String msg) {
    state.lastMsg = msg
    state.lastMsgAt = now()
}

private void logDebug(String m) {
    if (settings?.enableDebug) {
        log.debug "${LOG_PREFIX} ${VARIANT} v${VERSION} — ${m}"
    }
}

private static String safeErr(def m) {
    def s = (m == null ? "unknown" : m.toString())
    s = s.replaceAll(/[\\r\\n]+/, " ")
    return s.take(240)
}
