package com.purelilith.networkscanner.data.scanner

import android.content.Context
import com.purelilith.networkscanner.R
import org.json.JSONArray

enum class NameSource(val label: String) {
    MDNS_TXT("имя от самого устройства (mDNS)"),
    HOSTNAME_RULE("правило по имени хоста"),
    MDNS_SERVICE("тип сервиса (mDNS)"),
    HOSTNAME_CLEANED("очищенное имя хоста"),
    PORTS("догадка по портам"),
    UNKNOWN("не определено")
}

data class FriendlyDeviceName(val title: String, val detail: String?, val source: NameSource)

object FriendlyName {

    private class Rule(
        val pattern: Regex,
        val title: String,
        val keepDetails: Boolean,
        val type: DeviceType?,
        val typeIsGuess: Boolean
    )

    private var rules: List<Rule> = emptyList()

    fun init(context: Context) {
        val text = context.resources.openRawResource(R.raw.name_rules)
            .bufferedReader().use { it.readText() }
        val array = JSONArray(text)
        rules = (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Rule(
                pattern = Regex(o.getString("match")),
                title = o.getString("title"),
                keepDetails = o.optBoolean("keepDetails", true),
                type = o.optString("type").takeIf { it.isNotBlank() }?.let { DeviceType.fromName(it) },
                typeIsGuess = o.optBoolean("guess", false)
            )
        }
    }


    fun resolve(
        hostname: String?,
        mdnsName: String?,
        services: List<String>,
        portGuess: String?
    ): FriendlyDeviceName {
        val host = hostname?.takeIf { it.isNotBlank() }

        if (!mdnsName.isNullOrBlank())
            return FriendlyDeviceName(mdnsName, null, NameSource.MDNS_TXT)

        host?.let { matchRule(it) }?.let { return it }

        typeFromServices(services)?.let {
            return FriendlyDeviceName(it, null, NameSource.MDNS_SERVICE)
        }

        host?.let { cleanedName(it) }?.let {
            return FriendlyDeviceName(it, null, NameSource.HOSTNAME_CLEANED)
        }

        if (portGuess != null) return FriendlyDeviceName(portGuess, null, NameSource.PORTS)

        return FriendlyDeviceName("Неизвестное устройство", null, NameSource.UNKNOWN)
    }

    private fun findRule(hostname: String): Rule? {
        val lower = hostname.substringBefore('.').lowercase()
        return rules.firstOrNull { it.pattern.containsMatchIn(lower) }
    }

    fun resolveType(hostname: String?, services: List<String>, openPorts: List<Int>): TypeResult {
        val host = hostname?.takeIf { it.isNotBlank() }

        host?.let { findRule(it) }?.let { rule ->
            val ruleType = rule.type
            if (ruleType != null && ruleType != DeviceType.UNKNOWN) {
                return TypeResult(ruleType, rule.typeIsGuess)
            }
        }

        deviceTypeFromServices(services)?.let { return it }

        return deviceTypeFromPorts(openPorts) ?: TypeResult(DeviceType.UNKNOWN, false)
    }

    private fun deviceTypeFromServices(services: List<String>): TypeResult? = when {
        services.any { it.startsWith("_googlecast") } -> TypeResult(DeviceType.SPEAKER, false)
        services.any { it.startsWith("_ipp") || it.startsWith("_printer") } -> TypeResult(DeviceType.PRINTER, false)
        services.any { it.startsWith("_hap") } -> TypeResult(DeviceType.SMART_HOME, false)
        services.any { it.startsWith("_spotify-connect") } -> TypeResult(DeviceType.SPEAKER, true)
        services.any { it.startsWith("_smb") } -> TypeResult(DeviceType.COMPUTER, true)
        else -> null
    }

    private fun deviceTypeFromPorts(openPorts: List<Int>): TypeResult? = when {
        9100 in openPorts -> TypeResult(DeviceType.PRINTER, true)
        8009 in openPorts -> TypeResult(DeviceType.SPEAKER, true)
        80 in openPorts || 8080 in openPorts -> TypeResult(DeviceType.ROUTER, true)
        22 in openPorts -> TypeResult(DeviceType.SERVER, true)
        else -> null
    }

    private fun tokensOf(hostname: String): List<String> =
        hostname.substringBefore('.').split('-', '_', ' ').filter { it.isNotBlank() }
    private fun isRandomId(token: String): Boolean {
        val t = token.lowercase()
        return t.length >= 6 && t.all { it in '0'..'9' || it in 'a'..'f' } && t.any { it.isDigit() }
    }

    private fun matchRule(hostname: String): FriendlyDeviceName? {
        val lower = hostname.substringBefore('.').lowercase()
        val rule = rules.firstOrNull { it.pattern.containsMatchIn(lower) } ?: return null

        val detail = if (rule.keepDetails)
            tokensOf(hostname)
                .filterNot(::isRandomId)
                .filterNot { rule.pattern.containsMatchIn(it.lowercase()) }
                .joinToString(" ").ifBlank { null }
        else null

        return FriendlyDeviceName(rule.title, detail, NameSource.HOSTNAME_RULE)
    }

    private fun typeFromServices(services: List<String>): String? = when {
        services.any { it.startsWith("_googlecast") } -> "Chromecast / Google TV / колонка"
        services.any { it.startsWith("_airplay") } -> "Устройство Apple (AirPlay)"
        services.any { it.startsWith("_ipp") || it.startsWith("_printer") } -> "Принтер"
        services.any { it.startsWith("_hap") } -> "Устройство умного дома (HomeKit)"
        services.any { it.startsWith("_spotify-connect") } -> "Колонка / плеер"
        services.any { it.startsWith("_smb") } -> "Компьютер или NAS (общий доступ к файлам)"
        else -> null
    }

    private fun cleanedName(hostname: String): String? =
        prettify(tokensOf(hostname).filterNot(::isRandomId)).ifBlank { null }

    private fun prettify(tokens: List<String>): String =
        tokens.joinToString(" ") { token ->
            if (token.all { it.isLowerCase() || !it.isLetter() })
                token.replaceFirstChar { it.uppercase() }
            else token
        }
}