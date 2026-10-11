package com.beeftech.feedcrib.data

import com.beeftech.database.entity.FeedCribEntity

data class AddCribForm(
    val code: String = "", val siteId: String = "", val penId: String = "",
    val rationId: String = "", val adi: String = "", val description: String = "",
    val method: String = "", val rfid: String = ""
)

fun adjustInitialAdi(value: String, steps: Int, rules: CribAdiRules): String {
    val current = value.toDoubleOrNull()?.takeIf { it.isFinite() } ?: rules.minimum
    var result = java.math.BigDecimal.valueOf(current).add(
        java.math.BigDecimal.valueOf(rules.adjustmentStep).multiply(java.math.BigDecimal.valueOf(steps.toLong())))
    result = result.max(java.math.BigDecimal.valueOf(rules.minimum))
    rules.maximum?.let { result = result.min(java.math.BigDecimal.valueOf(it)) }
    return result.stripTrailingZeros().toPlainString()
}

fun validateCribForm(form: AddCribForm, setup: CribSetup, existing: List<FeedCribEntity>): Map<String, String> = buildMap {
    val code = form.code.trim()
    if (code.isEmpty() || code.length > 32) put("code", "Enter a crib code of 1–32 characters.")
    if (existing.any { it.siteId == form.siteId && it.cribNumber.equals(code, true) }) put("code", "This code already exists on the selected site.")
    if (setup.sites.none { it.id == form.siteId }) put("site", "Select an existing site.")
    if (setup.pens.none { it.id == form.penId && it.siteId == form.siteId && it.active }) put("pen", "Select an active pen belonging to this site.")
    if (setup.rations.none { it.id == form.rationId && it.active }) put("ration", "Select an active ration.")
    val adi = form.adi.toDoubleOrNull()
    if (adi == null || !adi.isFinite() || adi < 0) put("adi", "Enter ADI zero or greater (temporary rule).")
    else if (adi < setup.adiRules.minimum || (setup.adiRules.maximum != null && adi > setup.adiRules.maximum))
        put("adi", "ADI is outside the configured range.")
    if (form.description.trim().length > 512) put("description", "Use at most 512 characters.")
    if (form.method.isNotEmpty() && form.method !in setup.methods) put("method", "Select an available feeding method.")
    val tag = form.rfid.trim()
    if (tag.length > 255) put("rfid", "Use at most 255 characters.")
    if (tag.isNotEmpty() && existing.any { it.rfid.equals(tag, true) }) put("rfid", "This tag already identifies another crib.")
}

