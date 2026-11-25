package dev.rffrr.mateli.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import dev.rffrr.mateli.R

/** Drawer destinations, grouped the way NekoBox groups them. */
enum class Destination(val labelRes: Int, val icon: ImageVector) {
    Configuration(R.string.nav_configuration, Icons.Filled.Dns),
    Groups(R.string.nav_group, Icons.Filled.Folder),
    Routes(R.string.nav_route, Icons.Filled.CallSplit),
    Settings(R.string.nav_settings, Icons.Filled.Settings),
    Logs(R.string.nav_logs, Icons.Filled.Description),
    About(R.string.nav_about, Icons.Filled.Info);

    companion object {
        val mainSection = listOf(Configuration, Groups, Routes, Settings)
        val miscSection = listOf(Logs)
        val aboutSection = listOf(About)
    }
}
