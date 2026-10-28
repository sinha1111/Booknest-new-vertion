package com.tricosoft.booknest.ui

import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

fun formatCount(n: Int): String =
    if (n >= 1000) String.format(Locale.US, "%.1fk", n / 1000.0) else n.toString()

private val ISO_TS = Regex("""(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})(?:\.(\d+))?(Z|[+-]\d{2}(?::?\d{2})?)?""")

/**
 * Supabase timestamp ("2026-10-14T05:12:33.123456+00:00") -> epoch millis.
 * java.time use kora hoy ni, karon seta Android 8 (API 26) er niche nei; app minSdk 24 (Android 7).
 */
private fun parseIsoMillis(iso: String): Long? {
    val m = ISO_TS.find(iso) ?: return null
    val g = m.groupValues
    val cal = GregorianCalendar(TimeZone.getTimeZone("UTC"))
    cal.clear()
    cal.set(g[1].toInt(), g[2].toInt() - 1, g[3].toInt(), g[4].toInt(), g[5].toInt(), g[6].toInt())
    var millis = cal.timeInMillis
    val frac = g[7]
    if (frac.isNotEmpty()) millis += (("0.$frac").toDouble() * 1000).toLong()
    val tz = g[8]
    if (tz.isNotEmpty() && tz != "Z") {
        val sign = if (tz[0] == '-') -1 else 1
        val digits = tz.substring(1).replace(":", "")
        val hh = digits.substring(0, 2).toInt()
        val mm = if (digits.length >= 4) digits.substring(2, 4).toInt() else 0
        millis -= sign * (hh * 60L + mm) * 60_000L
    }
    return millis
}

/** Web er timeAgo: "5m ago" / "3h ago" / "2d ago". */
fun timeAgo(iso: String): String {
    val millis = parseIsoMillis(iso) ?: System.currentTimeMillis()
    val diff = System.currentTimeMillis() - millis
    val mins = (diff / 60000).coerceAtLeast(0)
    if (mins < 60) return "${mins}m ago"
    val hours = mins / 60
    if (hours < 24) return "${hours}h ago"
    return "${hours / 24}d ago"
}
