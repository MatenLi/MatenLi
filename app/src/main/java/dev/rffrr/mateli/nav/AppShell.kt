@file:OptIn(ExperimentalMaterial3Api::class)

package dev.rffrr.mateli.nav

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import dev.rffrr.mateli.R
import dev.rffrr.mateli.config.RouteStore
import dev.rffrr.mateli.config.buildSingBoxConfig
import dev.rffrr.mateli.config.copySingBoxConfig
import dev.rffrr.mateli.config.unsupportedReason
import dev.rffrr.mateli.core.CoreRuntime
import dev.rffrr.mateli.core.CoreService
import dev.rffrr.mateli.core.CoreState
import dev.rffrr.mateli.core.ExitIp
import dev.rffrr.mateli.core.ExitInfo
import dev.rffrr.mateli.core.Probes
import dev.rffrr.mateli.core.UrlProbe
import dev.rffrr.mateli.core.UrlResult
import dev.rffrr.mateli.profiles.Profile
import dev.rffrr.mateli.profiles.ProfileStore
import dev.rffrr.mateli.profiles.RefreshResult
import dev.rffrr.mateli.profiles.ShareLinkParser
import dev.rffrr.mateli.profiles.SubscriptionStore
import dev.rffrr.mateli.profiles.SubscriptionUpdater
import dev.rffrr.mateli.profiles.blankProfile
import dev.rffrr.mateli.settings.PrefKeys
import dev.rffrr.mateli.settings.rememberPrefs
import dev.rffrr.mateli.ui.about.AboutScreen
import dev.rffrr.mateli.ui.groups.GroupsScreen
import dev.rffrr.mateli.ui.logs.LogsScreen
import dev.rffrr.mateli.ui.main.AddProfileDialog
import dev.rffrr.mateli.ui.main.ConfigurationBottomBar
import dev.rffrr.mateli.ui.main.ConfigurationScreen
import dev.rffrr.mateli.ui.main.ManualProtocolDialog
import dev.rffrr.mateli.ui.main.ProfileEditorDialog
import dev.rffrr.mateli.ui.main.ShareDialog
import dev.rffrr.mateli.profiles.Subscription
import dev.rffrr.mateli.ui.routes.RoutesScreen
import dev.rffrr.mateli.ui.settings.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LATENCY_URL = "https://www.gstatic.com/generate_204"

/** Id of the group that holds profiles added by hand (not from a subscription). */
private const val LOCAL_GROUP = "local"

/** Root of the app: drawer, top bar, group tabs, profile list and bottom bar. */
@Composable
fun AppShell() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = rememberPrefs()
    val profileStore = remember { ProfileStore(context) }
    val routeStore = remember { RouteStore(context) }
    val subStore = remember { SubscriptionStore(context) }
    val snackbar = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var current by rememberSaveable { mutableStateOf(Destination.Configuration) }
    var profiles by remember { mutableStateOf(profileStore.list()) }
    var subs by remember { mutableStateOf(subStore.list()) }
    var selectedId by remember { mutableStateOf(profileStore.selectedId()) }
    var activeGroup by rememberSaveable { mutableStateOf(LOCAL_GROUP) }
    var pendingReconnect by remember { mutableStateOf<String?>(null) }
    var showPaste by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Profile?>(null) }
    var editingIsNew by remember { mutableStateOf(false) }
    var unsupported by remember { mutableStateOf<Profile?>(null) }
    var waitingForVpn by remember { mutableStateOf<Profile?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sortByUrl by rememberSaveable { mutableStateOf(false) }
    var diff by remember { mutableStateOf<RefreshResult?>(null) }
    var sharing by remember { mutableStateOf<Profile?>(null) }
    var pendingJson by remember { mutableStateOf<Profile?>(null) }
    val urlPings = remember { mutableStateMapOf<String, UrlResult>() }
    var urlTesting by remember { mutableStateOf(emptySet<String>()) }
    var latency by remember { mutableStateOf<Long?>(null) }
    var exitInfo by remember { mutableStateOf<ExitInfo?>(null) }

    val state by CoreRuntime.state.collectAsState()
    val stats by CoreRuntime.stats.collectAsState()
    val logs by CoreRuntime.logs.collectAsState()
    val lastError by CoreRuntime.lastError.collectAsState()
    val showBottomBar = prefs.bool(PrefKeys.SHOW_BOTTOM_BAR, true)
    val selectedProfile = profiles.firstOrNull { it.id == selectedId }
    val isConnected = state == CoreState.Connected

    // Group tabs: one per subscription, plus Local for profiles that did not come from one.
    val groups: List<Pair<String, String>> =
        listOf(LOCAL_GROUP to stringResource(R.string.group_local)) + subs.map { it.id to it.name }
    val currentGroup = if (groups.any { it.first == activeGroup }) activeGroup else LOCAL_GROUP
    // Tabs only when there is more than one subscription. With one, the list shows everything.
    val showTabs = subs.size > 1
    val updateTarget: Subscription? = if (showTabs) subs.firstOrNull { it.id == currentGroup } else subs.firstOrNull()

    fun groupOf(profileId: String): String =
        subs.firstOrNull { profileId in it.profileIds }?.id ?: LOCAL_GROUP

    val inGroup = if (showTabs) profiles.filter { groupOf(it.id) == currentGroup } else profiles
    val shown = if (sortByUrl) {
        inGroup.sortedWith(compareBy<Profile>({ urlPings[it.id]?.ms == null }, { urlPings[it.id]?.ms ?: 0L }))
    } else {
        inGroup
    }

    fun refreshProfiles() {
        profiles = profileStore.list()
        subs = subStore.list()
        selectedId = profileStore.selectedId()
    }

    fun showMessage(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }

    /** Tests the given profiles in one probe core. Works connected or not; each result shows its time or its reason. */
    fun urlTestProfiles(targets: List<Profile>) {
        if (targets.isEmpty()) {
            showMessage(context.getString(R.string.connect_no_profile))
            return
        }
        if (urlTesting.isNotEmpty()) {
            showMessage(context.getString(R.string.url_test_busy))
            return
        }
        val ids = targets.map { it.id }.toSet()
        urlTesting = ids
        scope.launch {
            try {
                val results = UrlProbe.testAll(
                    context, prefs, targets, prefs.string(PrefKeys.CONNECTION_TEST_URL, LATENCY_URL)
                )
                results.forEach { (id, result) -> urlPings[id] = result }
            } finally {
                urlTesting = urlTesting - ids
            }
        }
    }

    /** Profile bar: tests only the profile it was clicked on. */
    fun urlTest(profile: Profile) = urlTestProfiles(listOf(profile))

    /** Top bar: tests every profile in the group on screen. */
    fun testGroup() = urlTestProfiles(inGroup)

    fun updateGroup() {
        val sub = updateTarget
        if (sub == null) {
            showMessage(context.getString(R.string.group_local_no_update))
            return
        }
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { SubscriptionUpdater.refresh(profileStore, subStore, sub, prefs) }
            }
            result
                .onSuccess {
                    diff = it
                    refreshProfiles()
                }
                .onFailure { error ->
                    showMessage(context.getString(R.string.update_failed, error.message ?: "unknown error"))
                }
        }
    }

    fun importText(text: String) {
        val added = mutableListOf<Profile>()
        var failed = 0
        val trimmed = text.trim()
        val lines = if (trimmed.startsWith("{")) listOf(trimmed) else trimmed.lines().map { it.trim() }.filter { it.isNotEmpty() }
        lines.forEach { line ->
            runCatching { ShareLinkParser.parse(line) }
                .onSuccess { added.add(it) }
                .onFailure { failed++ }
        }
        if (added.isEmpty()) {
            showMessage(context.getString(R.string.import_none))
            return
        }
        added.forEach { profileStore.add(it) }
        if (selectedId == null) profileStore.select(added.first().id)
        refreshProfiles()
        showMessage(context.getString(R.string.import_summary, added.size, failed))
    }

    val vpnConsent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val profile = waitingForVpn
        waitingForVpn = null
        if (result.resultCode == Activity.RESULT_OK && profile != null) {
            CoreService.start(context, buildSingBoxConfig(prefs, profile, routeStore.list()))
        } else {
            CoreRuntime.markError(context.getString(R.string.vpn_permission_denied))
        }
    }

    val qrScanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { importText(it) }
    }

    val fileImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val text = uri?.let {
            runCatching { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() } }.getOrNull()
        }
        if (text.isNullOrBlank()) showMessage(context.getString(R.string.import_file_failed)) else importText(text)
    }

    val saveJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        val target = pendingJson
        pendingJson = null
        if (uri != null && target != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(buildSingBoxConfig(prefs, target, routeStore.list()).toByteArray(Charsets.UTF_8))
                        } ?: error("could not open the file")
                    }.isSuccess
                }
                showMessage(context.getString(if (saved) R.string.share_file_saved else R.string.share_file_failed))
            }
        }
    }

    fun startProfile(profile: Profile) {
        val vpn = prefs.string(PrefKeys.SERVICE_MODE, "vpn") == "vpn"
        val consent = if (vpn) VpnService.prepare(context) else null
        if (consent != null) {
            waitingForVpn = profile
            vpnConsent.launch(consent)
        } else {
            CoreService.start(context, buildSingBoxConfig(prefs, profile, routeStore.list()))
        }
    }

    fun toggleCore() {
        if (state != CoreState.Idle) {
            pendingReconnect = null
            CoreService.stop(context)
            return
        }
        val profile = selectedProfile
        when {
            profile == null -> showMessage(context.getString(R.string.connect_no_profile))
            unsupportedReason(profile) != null -> unsupported = profile
            else -> startProfile(profile)
        }
    }

    /** Picking another profile while connected stops the core, then starts it on the new profile. */
    fun selectProfile(id: String) {
        val previous = selectedId
        profileStore.select(id)
        refreshProfiles()
        if (state != CoreState.Idle && id != previous) {
            pendingReconnect = id
            CoreService.stop(context)
            showMessage(context.getString(R.string.reconnecting))
        }
    }

    LaunchedEffect(state) {
        if (state == CoreState.Idle) {
            val next = pendingReconnect
            if (next != null) {
                pendingReconnect = null
                profiles.firstOrNull { it.id == next }?.let { target ->
                    if (unsupportedReason(target) != null) unsupported = target else startProfile(target)
                }
            }
        }
    }

    // Auto update: refresh subscriptions whose delay has passed, while the app is open.
    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        subStore.list()
            .filter { it.autoUpdate && now - it.updatedAt > it.updateMinutes * 60_000L }
            .forEach { sub ->
                withContext(Dispatchers.IO) {
                    runCatching { SubscriptionUpdater.refresh(profileStore, subStore, sub, prefs) }
                }.onSuccess {
                    diff = it
                    refreshProfiles()
                }
            }
    }

    // Exit IP and country, looked up once per connection.
    LaunchedEffect(isConnected) {
        exitInfo = null
        if (isConnected && prefs.bool(PrefKeys.COUNTRY_LOOKUP, true)) {
            val key = prefs.string(PrefKeys.IP2LOCATION_KEY, "")
            exitInfo = withContext(Dispatchers.IO) { ExitIp.lookup(key) }
        }
    }

    // HTTPS latency through the core, every 10 seconds while connected.
    LaunchedEffect(isConnected) {
        latency = null
        while (isConnected) {
            latency = Probes.urlTest(LATENCY_URL)
            delay(10_000L)
        }
    }

    LaunchedEffect(lastError) {
        lastError?.let { snackbar.showSnackbar(it) }
    }

    val onSelectDestination: (Destination) -> Unit = { destination ->
        current = destination
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)
                )
                DrawerItems(Destination.mainSection, current, onSelectDestination)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp))
                DrawerItems(Destination.miscSection, current, onSelectDestination)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp))
                DrawerItems(Destination.aboutSection, current, onSelectDestination)
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(current.labelRes)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.menu_open))
                        }
                    },
                    actions = {
                        if (current == Destination.Configuration) {
                            IconButton(
                                enabled = updateTarget != null,
                                onClick = { updateGroup() }
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_update_group))
                            }
                            IconButton(onClick = { testGroup() }) {
                                Icon(Icons.Filled.Timer, contentDescription = stringResource(R.string.action_url_test_top))
                            }
                            IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.action_search))
                            }
                            Box {
                                IconButton(onClick = { showAddMenu = true }) {
                                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_profile))
                                }
                                DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_scan_qr)) },
                                        onClick = {
                                            showAddMenu = false
                                            qrScanner.launch(
                                                ScanOptions()
                                                    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                                    .setBeepEnabled(false)
                                                    .setOrientationLocked(false)
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_import_clipboard)) },
                                        onClick = {
                                            showAddMenu = false
                                            val text = clipboardText(context)
                                            if (text == null) showMessage(context.getString(R.string.profile_clipboard_empty))
                                            else importText(text)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_import_file)) },
                                        onClick = {
                                            showAddMenu = false
                                            fileImport.launch(arrayOf("*/*"))
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_paste)) },
                                        onClick = {
                                            showAddMenu = false
                                            showPaste = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_manual)) },
                                        onClick = {
                                            showAddMenu = false
                                            showManual = true
                                        }
                                    )
                                }
                            }
                            Box {
                                IconButton(onClick = { showMoreMenu = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
                                }
                                DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_sort_delay)) },
                                        leadingIcon = { Icon(Icons.Filled.Sort, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            sortByUrl = !sortByUrl
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.copy_config)) },
                                        leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            copySingBoxConfig(context, prefs, selectedProfile, routeStore.list())
                                        }
                                    )
                                    if (!showBottomBar) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    stringResource(
                                                        if (state == CoreState.Idle) R.string.action_connect else R.string.action_disconnect
                                                    )
                                                )
                                            },
                                            leadingIcon = { Icon(Icons.Filled.PowerSettingsNew, contentDescription = null) },
                                            onClick = {
                                                showMoreMenu = false
                                                toggleCore()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (current == Destination.Configuration && showBottomBar) {
                    ConfigurationBottomBar(
                        state = state,
                        stats = stats,
                        latency = latency,
                        exit = exitInfo,
                        selected = selectedProfile,
                        onToggle = { toggleCore() }
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (current == Destination.Configuration && searching) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.profile_search_hint)) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                if (current == Destination.Configuration && showTabs) {
                    ScrollableTabRow(
                        selectedTabIndex = groups.indexOfFirst { it.first == currentGroup }.coerceAtLeast(0),
                        edgePadding = 8.dp
                    ) {
                        groups.forEach { (id, name) ->
                            Tab(
                                selected = id == currentGroup,
                                onClick = { activeGroup = id },
                                text = {
                                    Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            )
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    when (current) {
                        Destination.Configuration -> ConfigurationScreen(
                            profiles = shown,
                            selectedId = selectedId,
                            urlPings = urlPings,
                            urlTesting = urlTesting,
                            query = query,
                            onSelect = { id -> selectProfile(id) },
                            onDelete = { id ->
                                profileStore.remove(id)
                                urlPings.remove(id)
                                refreshProfiles()
                            },
                            onEdit = { profile ->
                                editingIsNew = false
                                editing = profile
                            },
                            onUrlTest = { profile -> urlTest(profile) },
                            onShare = { profile -> sharing = profile }
                        )
                        Destination.Groups -> GroupsScreen(
                            onChanged = { refreshProfiles() },
                            onRefreshed = { result ->
                                diff = result
                                refreshProfiles()
                            }
                        )
                        Destination.Routes -> RoutesScreen()
                        Destination.Settings -> SettingsScreen()
                        Destination.Logs -> LogsScreen(
                            logs = logs,
                            onClear = { CoreRuntime.clearLogs() }
                        )
                        Destination.About -> AboutScreen()
                    }
                }
            }
        }
    }

    if (showPaste) {
        AddProfileDialog(
            onDismiss = { showPaste = false },
            onSave = { added ->
                added.forEach { profileStore.add(it) }
                if (selectedId == null && added.isNotEmpty()) profileStore.select(added.first().id)
                refreshProfiles()
                showPaste = false
                showMessage(context.getString(R.string.profile_added, added.size))
            }
        )
    }

    if (showManual) {
        ManualProtocolDialog(
            onDismiss = { showManual = false },
            onPick = { type ->
                showManual = false
                editingIsNew = true
                editing = blankProfile(type)
            }
        )
    }

    editing?.let { target ->
        ProfileEditorDialog(
            profile = target,
            onDismiss = { editing = null },
            onSave = { saved ->
                if (editingIsNew) profileStore.add(saved) else profileStore.replace(saved)
                editing = null
                refreshProfiles()
                showMessage(context.getString(R.string.profile_saved))
            }
        )
    }

    unsupported?.let { profile ->
        UnsupportedDialog(
            profile = profile,
            reason = unsupportedReason(profile).orEmpty(),
            onDismiss = { unsupported = null }
        )
    }

    diff?.let { result ->
        DiffDialog(result = result, onDismiss = { diff = null })
    }

    sharing?.let { target ->
        ShareDialog(
            profile = target,
            configJson = buildSingBoxConfig(prefs, target, routeStore.list()),
            onSaveFile = {
                pendingJson = target
                sharing = null
                saveJson.launch("${target.name}.json")
            },
            onMessage = { showMessage(it) },
            onDismiss = { sharing = null }
        )
    }
}

/** Plain text from the clipboard, or null when it is empty. */
private fun clipboardText(context: Context): String? {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return null
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
}

@Composable
private fun DrawerItems(
    destinations: List<Destination>,
    selected: Destination,
    onSelect: (Destination) -> Unit
) {
    destinations.forEach { destination ->
        NavigationDrawerItem(
            label = { Text(stringResource(destination.labelRes)) },
            icon = { Icon(destination.icon, contentDescription = null) },
            selected = destination == selected,
            onClick = { onSelect(destination) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}

/** Shown instead of connecting when the selected profile uses a transport the core cannot run yet. */
@Composable
private fun UnsupportedDialog(profile: Profile, reason: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.PowerSettingsNew, contentDescription = null) },
        title = { Text(stringResource(R.string.xhttp_title)) },
        text = { Text(stringResource(R.string.xhttp_message, profile.name, reason)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        }
    )
}

/** What the last subscription refresh added, updated or removed. */
@Composable
private fun DiffDialog(result: RefreshResult, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.diff_title, result.subscription.name)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (result.added.isEmpty() && result.updated.isEmpty() && result.deleted.isEmpty()) {
                    Text(stringResource(R.string.diff_none))
                }
                DiffSection(R.string.diff_added, result.added)
                DiffSection(R.string.diff_updated, result.updated)
                DiffSection(R.string.diff_deleted, result.deleted)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        }
    )
}

@Composable
private fun DiffSection(titleRes: Int, names: List<String>) {
    if (names.isEmpty()) return
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
    names.forEach { name ->
        Text(text = name, style = MaterialTheme.typography.bodyMedium)
    }
}
