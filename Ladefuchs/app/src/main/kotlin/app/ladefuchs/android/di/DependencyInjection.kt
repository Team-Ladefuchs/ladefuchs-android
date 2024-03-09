package app.ladefuchs.android.di

import android.app.Application
import android.content.pm.PackageInfo
import android.os.Build
import app.ladefuchs.android.BuildConfig
import app.ladefuchs.android.chargecards.api.ChargeCardsApi
import app.ladefuchs.android.chargecards.api.ChargeCardsApiImpl
import app.ladefuchs.android.chargecards.data.ChargeCardsRepository
import app.ladefuchs.android.chargecards.data.ChargeCardsRepositoryImpl
import app.ladefuchs.android.chargecards.domain.GetCardsUseCase
import app.ladefuchs.android.chargecards.domain.GetOperatorsUseCase
import app.ladefuchs.android.chargecards.ui.ChargeCardViewModel
import app.ladefuchs.android.helper.printLog
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.request.headers
import io.ktor.client.statement.request
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // viewModel
    viewModel {
        ChargeCardViewModel(
            getOperatorsUseCase = get(),
            getCardsUseCase = get()
        )
    }

    // domain
    factory {
        GetOperatorsUseCase(
            chargeCardsRepository = get()
        )
    }
    factory {
        GetCardsUseCase(
            chargeCardsRepository = get()
        )
    }

    // repository
    single<ChargeCardsRepository> {
        ChargeCardsRepositoryImpl(
            chargeCardsApi = get()
        )
    }

    // api
    single<ChargeCardsApi> {
        ChargeCardsApiImpl(
            httpClient = get()
        )
    }

    // httpclient
    single {
        provideKtorClient(application = get())
    }
}

private fun provideKtorClient(
    application: Application,
): HttpClient {
    val client = HttpClient(CIO) {
        expectSuccess = false
        followRedirects = true

        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.ladefuchs.app"
            }

            headers {
                val context = application.applicationContext

                val packageInfo: PackageInfo =
                    context.packageManager.getPackageInfo(context.packageName, 0)
                val version: String = packageInfo.versionName

                val versionCode =
                    packageInfo.longVersionCode

                // User-Agent
                append(
                    "User-Agent",
                    "Ladefuchs-Android/$version (Build $versionCode) Android/${Build.VERSION.RELEASE}",
                )

                // Authentication
                val apiToken: String = BuildConfig.apiKey
                append(
                    "Authorization",
                    "Bearer $apiToken",
                )
            }
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    printLog(message)
                }
            }
            level = LogLevel.ALL
        }

        install(ResponseObserver) {
            onResponse {
                printLog("<== ${it.status} ${it.request.url}")
            }
        }

        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                },
            )
        }
    }

    return client
}
