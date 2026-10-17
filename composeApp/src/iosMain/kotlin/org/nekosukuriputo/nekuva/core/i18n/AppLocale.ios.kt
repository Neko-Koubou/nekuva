package org.nekosukuriputo.nekuva.core.i18n

import java.util.Locale

private val systemDefaultLocale: Locale = Locale.getDefault()

actual fun applyAppLocale(tag: String) {
    val target = if (tag.isBlank()) systemDefaultLocale else Locale.forLanguageTag(tag)
    Locale.setDefault(target)
}

actual fun recreateForLocale() = Unit
