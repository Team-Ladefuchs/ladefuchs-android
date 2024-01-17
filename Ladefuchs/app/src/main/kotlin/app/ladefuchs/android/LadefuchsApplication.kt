package app.ladefuchs.android

import android.app.Application
import app.ladefuchs.android.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level

class LadefuchsApplication : Application(), KoinComponent {
    override fun onCreate() {
        super.onCreate()

        // setup dependency injection
        startKoin {
            // Koin Android logger
            androidLogger(
                if (BuildConfig.DEBUG) {
                    Level.ERROR
                } else {
                    Level.NONE
                }
            )

            // inject Android context
            androidContext(applicationContext)

            // use modules
            modules(
                listOf(appModule)
            )
        }
    }
}
