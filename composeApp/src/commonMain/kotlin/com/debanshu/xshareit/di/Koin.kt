package com.debanshu.xshareit.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import kotlinx.serialization.json.Json
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.ksp.generated.module

@Module
@ComponentScan("com.debanshu.xshareit.data")
class DataModule {
    @Single
    fun json() = Json {
        prettyPrint = true
        isLenient = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Single
    fun httpClient(json: Json) = HttpClient {
        install(ContentNegotiation) {
            json(json, contentType = ContentType.Application.Json)
        }
        install(Logging){
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }

    @Single
    fun httpServerFactory(json: Json): (String, Int) -> EmbeddedServer<*, *> = { host, port ->
        embeddedServer(CIO, host = host, port = port) {
            install(CORS) {
                anyHost()
            }
            install(ServerContentNegotiation) {
                json(json, contentType = ContentType.Application.Json)
            }
        }
    }
}

@Module
@ComponentScan("com.debanshu.xshareit.domain")
class ViewModelModule

@Module(
    includes = [DataModule::class, ViewModelModule::class]
)
class AppModule

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        modules(
            AppModule().module
        )
        config?.invoke(this)
    }
}