package dev.rffrr.mateli

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.rffrr.mateli.nav.AppShell
import dev.rffrr.mateli.storage.DataDir
import dev.rffrr.mateli.ui.theme.MatenLiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DataDir.migrate(this)
        setContent {
            MatenLiTheme {
                AppShell()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Access may have been granted in system settings while the app was away.
        DataDir.migrate(this)
    }
}
