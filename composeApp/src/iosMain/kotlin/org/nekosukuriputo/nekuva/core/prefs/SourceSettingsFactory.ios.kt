package org.nekosukuriputo.nekuva.core.prefs

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import platform.Foundation.NSUserDefaults

actual fun getSourceObservableSettings(sourceName: String): ObservableSettings {
    val suite = NSUserDefaults(suiteName = "org.nekosukuriputo.nekuva.source.$sourceName")
    return CrashSafeSettings(NSUserDefaultsSettings(suite))
}
