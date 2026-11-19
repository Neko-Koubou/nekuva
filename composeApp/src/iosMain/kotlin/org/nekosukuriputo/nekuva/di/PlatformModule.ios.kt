package org.nekosukuriputo.nekuva.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import org.koin.core.module.Module
import org.koin.dsl.module
import org.nekosukuriputo.nekuva.core.os.NetworkState
import org.nekosukuriputo.nekuva.core.prefs.CrashSafeSettings
import org.nekosukuriputo.nekuva.local.data.IosLocalStorageManager
import org.nekosukuriputo.nekuva.local.data.LocalStorageManager
import org.nekosukuriputo.nekuva.reader.data.IosPagePersister
import org.nekosukuriputo.nekuva.reader.domain.PagePersister
import platform.Foundation.NSUserDefaults

actual val platformModule: Module = module {
    single<ObservableSettings> {
        CrashSafeSettings(
            NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults),
        )
    }
    single { NetworkState(get()) }
    single<LocalStorageManager> { IosLocalStorageManager(get()) }
    single<PagePersister> { IosPagePersister(get()) }
}
