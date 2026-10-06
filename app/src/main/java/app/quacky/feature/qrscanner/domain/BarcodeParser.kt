package app.quacky.feature.qrscanner.domain

import android.net.Uri

enum class BarcodeType(val displayName: String) {
    URL("Website Link"),
    WIFI("Wi-Fi Network"),
    UPI("UPI Payment"),
    CONTACT("Contact Card"),
    EMAIL("Email"),
    SMS("SMS Message"),
    PHONE("Phone Number"),
    PRODUCT("Product Barcode"),
    TEXT("Plain Text")
}

data class ParsedBarcode(
    val rawValue: String,
    val formatName: String,
    val type: BarcodeType,
    val title: String,
    val subtitle: String,
    val details: Map<String, String> = emptyMap()
)

object BarcodeParser {

    fun parse(rawValue: String, formatName: String = "QR_CODE"): ParsedBarcode {
        val trimmed = rawValue.trim()

        // 1. UPI Payment
        if (trimmed.startsWith("upi://pay", ignoreCase = true)) {
            val vpa = extractQueryParam(trimmed, "pa") ?: ""
            val name = extractQueryParam(trimmed, "pn") ?: ""
            val amount = extractQueryParam(trimmed, "am")
            val note = extractQueryParam(trimmed, "tn") ?: ""
            val currency = extractQueryParam(trimmed, "cu") ?: "INR"

            val details = mutableMapOf<String, String>()
            if (vpa.isNotBlank()) details["VPA / UPI ID"] = vpa
            if (name.isNotBlank()) details["Payee Name"] = name
            if (!amount.isNullOrBlank()) details["Amount"] = "$currency $amount"
            if (note.isNotBlank()) details["Note"] = note

            val subtitleText = if (!amount.isNullOrBlank()) {
                "$currency $amount to ${name.ifBlank { vpa }}"
            } else {
                name.ifBlank { vpa }
            }

            return ParsedBarcode(
                rawValue = trimmed,
                formatName = formatName,
                type = BarcodeType.UPI,
                title = "UPI Payment",
                subtitle = subtitleText,
                details = details
            )
        }

        // 2. Wi-Fi
        if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
            val details = parseWifiString(trimmed)
            val ssid = details["SSID"] ?: "Wi-Fi Network"
            val security = details["Security"] ?: "None"
            return ParsedBarcode(
                rawValue = trimmed,
                formatName = formatName,
                type = BarcodeType.WIFI,
                title = ssid,
                subtitle = "Security: $security",
                details = details
            )
        }

        // 3. Contact (vCard / MeCard)
        if (trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) || trimmed.startsWith("MECARD:", ignoreCase = true)) {
            val details = parseContactString(trimmed)
            val name = details["Name"] ?: "Contact"
            val phone = details["Phone"] ?: details["Mobile"] ?: ""
            return ParsedBarcode(
                rawValue = trimmed,
                formatName = formatName,
                type = BarcodeType.CONTACT,
                title = name,
                subtitle = phone.ifBlank { details["Email"] ?: "Contact Card" },
                details = details
            )
        }

        // 4. Email (mailto / MATMSG)
        if (trimmed.startsWith("mailto:", ignoreCase = true) || trimmed.startsWith("MATMSG:", ignoreCase = true)) {
            val details = parseEmailString(trimmed)
            val to = details["To"] ?: trimmed.removePrefix("mailto:")
            return ParsedBarcode(
                rawValue = trimmed,
                formatName = formatName,
                type = BarcodeType.EMAIL,
                title = to,
                subtitle = details["Subject"] ?: "Email message",
                details = details
            )
        }

        // 5. SMS (smsto / sms)
        if (trimmed.startsWith("smsto:", ignoreCase = true) || trimmed.startsWith("sms:", ignoreCase = true)) {
            val parts = trimmed.split(":", limit = 3)
            val number = parts.getOrNull(1)?.substringBefore("?") ?: ""
            val body = if (parts.size >= 3) parts[2] else (runCatching { Uri.parse(trimmed).getQueryParameter("body") }.getOrNull() ?: "")
            val details = mutableMapOf<String, String>()
            if (number.isNotBlank()) details["Recipient"] = number
            if (body.isNotBlank()) details["Message"] = body
            return ParsedBarcode(
                rawValue = trimmed,
                formatName = formatName,
                type = BarcodeType.SMS,
                title = number.ifBlank { "SMS" },
                subtitle = body.ifBlank { "SMS message" },
                details = details
            )
        }

        // 6. Phone number (tel:)
        if (trimmed.startsWith("tel:", ignoreCase = true)) {
            val number = trimmed.removePrefix("tel:").removePrefix("TEL:")
            return ParsedBarcode(
                rawValue = trimmed,
                formatName = formatName,
                type = BarcodeType.PHONE,
                title = number,
                subtitle = "Phone number",
                details = mapOf("Phone" to number)
            )
        }

        // 7. URL (http / https / www.)
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("www.", ignoreCase = true)
        ) {
            val fullUrl = if (trimmed.startsWith("www.", ignoreCase = true)) "https://$trimmed" else trimmed
            val host = extractHost(fullUrl)
            return ParsedBarcode(
                rawValue = fullUrl,
                formatName = formatName,
                type = BarcodeType.URL,
                title = host,
                subtitle = fullUrl,
                details = mapOf("URL" to fullUrl, "Host" to host)
            )
        }

        // 8. Product Barcode (EAN, UPC, Code 128 / numeric)
        val isProductFormat = formatName in listOf(
            "EAN_13", "EAN_8", "UPC_A", "UPC_E", "CODE_128", "CODE_39", "ITF"
        )
        if (isProductFormat && trimmed.all { it.isDigit() || it == '-' }) {
            return ParsedBarcode(
                rawValue = trimmed,
                formatName = formatName,
                type = BarcodeType.PRODUCT,
                title = trimmed,
                subtitle = "$formatName Barcode",
                details = mapOf("Barcode" to trimmed, "Format" to formatName)
            )
        }

        // 9. Default Plain Text
        return ParsedBarcode(
            rawValue = trimmed,
            formatName = formatName,
            type = BarcodeType.TEXT,
            title = if (trimmed.length > 40) trimmed.take(40) + "…" else trimmed,
            subtitle = "${trimmed.length} characters",
            details = mapOf("Text" to trimmed)
        )
    }

    private fun parseWifiString(raw: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val payload = raw.removePrefix("WIFI:").removePrefix("wifi:")
        val tokens = payload.split(";")
        for (token in tokens) {
            if (token.startsWith("S:")) map["SSID"] = token.substring(2)
            if (token.startsWith("T:")) map["Security"] = token.substring(2)
            if (token.startsWith("P:")) map["Password"] = token.substring(2)
            if (token.startsWith("H:")) map["Hidden"] = if (token.substring(2).toBoolean()) "Yes" else "No"
        }
        return map
    }

    private fun parseContactString(raw: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val lines = raw.lines()
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("FN:") -> map["Name"] = trimmed.substring(3)
                trimmed.startsWith("N:") && !map.containsKey("Name") -> {
                    val nameParts = trimmed.substring(2).split(";").filter { it.isNotBlank() }
                    map["Name"] = nameParts.reversed().joinToString(" ")
                }
                trimmed.startsWith("TEL") -> map["Phone"] = trimmed.substringAfter(":")
                trimmed.startsWith("EMAIL") -> map["Email"] = trimmed.substringAfter(":")
                trimmed.startsWith("ORG:") -> map["Organization"] = trimmed.substring(4)
                trimmed.startsWith("TITLE:") -> map["Title"] = trimmed.substring(6)
            }
        }
        // Fallback for MECARD
        if (raw.startsWith("MECARD:", ignoreCase = true)) {
            val content = raw.removePrefix("MECARD:")
            val tokens = content.split(";")
            for (token in tokens) {
                if (token.startsWith("N:")) map["Name"] = token.substring(2)
                if (token.startsWith("TEL:")) map["Phone"] = token.substring(4)
                if (token.startsWith("EMAIL:")) map["Email"] = token.substring(6)
                if (token.startsWith("ORG:")) map["Organization"] = token.substring(4)
            }
        }
        return map
    }

    private fun parseEmailString(raw: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (raw.startsWith("mailto:", ignoreCase = true)) {
            val to = raw.substringBefore("?").removePrefix("mailto:").removePrefix("MAILTO:")
            map["To"] = to
            extractQueryParam(raw, "subject")?.let { map["Subject"] = it }
            extractQueryParam(raw, "body")?.let { map["Body"] = it }
        } else if (raw.startsWith("MATMSG:", ignoreCase = true)) {
            val content = raw.removePrefix("MATMSG:")
            val tokens = content.split(";")
            for (token in tokens) {
                if (token.startsWith("TO:")) map["To"] = token.substring(3)
                if (token.startsWith("SUB:")) map["Subject"] = token.substring(4)
                if (token.startsWith("BODY:")) map["Body"] = token.substring(5)
            }
        }
        return map
    }

    private fun extractQueryParam(uriString: String, key: String): String? {
        val query = uriString.substringAfter("?", "")
        if (query.isEmpty()) return null
        for (pair in query.split("&")) {
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2 && parts[0].equals(key, ignoreCase = true)) {
                return runCatching { java.net.URLDecoder.decode(parts[1], "UTF-8") }.getOrDefault(parts[1])
            }
        }
        return null
    }

    private fun extractHost(urlString: String): String {
        val noScheme = urlString.substringAfter("://")
        val hostAndPort = noScheme.substringBefore("/").substringBefore("?")
        return hostAndPort.substringBefore(":")
    }
}
