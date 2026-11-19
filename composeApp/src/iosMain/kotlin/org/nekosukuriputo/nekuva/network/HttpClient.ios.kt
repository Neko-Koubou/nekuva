package org.nekosukuriputo.nekuva.network

import io.ktor.client.HttpClient

actual fun createHttpClient(): HttpClient = HttpClient()
