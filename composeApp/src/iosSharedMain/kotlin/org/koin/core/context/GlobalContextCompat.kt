package org.koin.core.context

import org.koin.core.Koin
import org.koin.mp.KoinPlatformTools

object GlobalContext {
    fun get(): Koin = KoinPlatformTools.defaultContext().get()
    fun getOrNull(): Koin? = KoinPlatformTools.defaultContext().getOrNull()
}
