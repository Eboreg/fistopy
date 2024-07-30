package us.huseli.fistopy

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.fistopy.compose.App
import us.huseli.fistopy.compose.FistopyTheme
import us.huseli.fistopy.viewmodels.AppViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<AppViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                    .detectLeakedClosableObjects()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .build()
            )
        }

        installSplashScreen()
        super.onCreate(savedInstanceState)

        viewModel.incrementAppStartCount()
        intent?.also { handleIntent(it) }

        /**
         * Recomposition of the below happens at app start for unknown reasons ... hopefully, it only happens when run
         * from the IDE, in the emulator? Does not seem to happen on device.
         * Some hints: https://stackoverflow.com/questions/72301445/why-is-setcontent-being-called-twice
         */
        setContent {
            val umlautifier by viewModel.umlautifier.collectAsStateWithLifecycle()

            CompositionLocalProvider(LocalUmlautifier provides umlautifier) {
                FistopyTheme {
                    App(startDestination = remember { viewModel.getStartDestination() })
                }
            }
        }

        viewModel.clearCustomStartDestination()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        intent.data?.pathSegments?.also { pathSegments ->
            if (pathSegments.getOrNull(0) == "spotify" && pathSegments.getOrNull(1) == "import-albums") {
                viewModel.handleSpotifyIntent(intent)
            }
            if (pathSegments.getOrNull(0) == "lastfm" && pathSegments.getOrNull(1) == "auth") {
                viewModel.handleLastFmIntent(intent)
            }
        }
    }
}
