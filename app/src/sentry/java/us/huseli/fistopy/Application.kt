package us.huseli.fistopy

import dagger.hilt.android.HiltAndroidApp
import io.sentry.android.core.SentryAndroid

@HiltAndroidApp
class Application : AbstractApplication() {
    override fun onCreate() {
        super.onCreate()

        SentryAndroid.init(this) { options ->
            options.dsn = "https://5041e4581e3c61008722fb79f5528e26@o453550.ingest.us.sentry.io/4507448607113216"
            options.isEnableUserInteractionTracing = true
            options.isAttachScreenshot = true
            options.isAttachViewHierarchy = true
        }
    }
}
