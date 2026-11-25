package dev.rffrr.mateli.ui.settings

import dev.rffrr.mateli.ui.backup.BackupSection

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.rffrr.mateli.R
import dev.rffrr.mateli.profiles.Mirror
import dev.rffrr.mateli.settings.PrefKeys
import dev.rffrr.mateli.settings.rememberPrefs
import dev.rffrr.mateli.ui.theme.presetAccents

/** Full settings, grouped like NekoBox's global preferences. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val prefs = rememberPrefs()
    val activity = LocalContext.current as? Activity
    val recreate: () -> Unit = { activity?.recreate() }
    var pickingApps by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        SettingSection(R.string.general_settings) {
            SwitchSetting(
                prefs, PrefKeys.AUTO_CONNECT, false,
                R.string.auto_connect, R.string.auto_connect_summary
            )
            MenuSetting(
                prefs, PrefKeys.NIGHT_THEME, "0", R.string.night_mode,
                listOf(
                    Option("0", stringResource(R.string.follow_system)),
                    Option("1", stringResource(R.string.enable)),
                    Option("2", stringResource(R.string.disable)),
                    Option("3", stringResource(R.string.auto))
                ),
                onChanged = recreate
            )
            MenuSetting(
                prefs, PrefKeys.APP_THEME, "0", R.string.app_theme,
                listOf(
                    Option("0", stringResource(R.string.theme_dynamic)),
                    Option("1", stringResource(R.string.theme_purple), presetAccents["1"]),
                    Option("2", stringResource(R.string.theme_blue), presetAccents["2"]),
                    Option("3", stringResource(R.string.theme_green), presetAccents["3"]),
                    Option("4", stringResource(R.string.theme_red), presetAccents["4"]),
                    Option("5", stringResource(R.string.theme_orange), presetAccents["5"]),
                    Option("6", stringResource(R.string.theme_custom))
                ),
                onChanged = recreate
            )
            TextSetting(
                prefs, PrefKeys.CUSTOM_ACCENT, "#6750A4", R.string.custom_accent,
                onChanged = recreate
            )
            SwitchSetting(
                prefs, PrefKeys.SHOW_BOTTOM_BAR, true,
                R.string.show_bottom_bar, R.string.show_bottom_bar_summary
            )
            MenuSetting(
                prefs, PrefKeys.SERVICE_MODE, "vpn", R.string.service_mode,
                listOf(
                    Option("vpn", stringResource(R.string.service_mode_vpn)),
                    Option("proxy", stringResource(R.string.service_mode_proxy)),
                    Option("transproxy", stringResource(R.string.service_mode_transproxy))
                )
            )
            MenuSetting(
                prefs, PrefKeys.TUN_IMPLEMENTATION, "0", R.string.tun_implementation,
                listOf(
                    Option("0", stringResource(R.string.tun_gvisor)),
                    Option("1", stringResource(R.string.tun_system)),
                    Option("2", stringResource(R.string.tun_mixed))
                )
            )
            MenuSetting(
                prefs, PrefKeys.MTU, "9000", R.string.mtu,
                listOf(Option("1500", "1500"), Option("9000", "9000"))
            )
            MenuSetting(
                prefs, PrefKeys.SPEED_INTERVAL, "1000", R.string.speed_interval,
                listOf(
                    Option("0", stringResource(R.string.disable)),
                    Option("500", stringResource(R.string.speed_500ms)),
                    Option("1000", stringResource(R.string.speed_1s)),
                    Option("3000", stringResource(R.string.speed_3s)),
                    Option("10000", stringResource(R.string.speed_10s))
                )
            )
            SwitchSetting(
                prefs, PrefKeys.PROFILE_TRAFFIC_STATS, true,
                R.string.profile_traffic_statistics, R.string.profile_traffic_statistics_summary
            )
            SwitchSetting(
                prefs, PrefKeys.SHOW_DIRECT_SPEED, true,
                R.string.show_direct_speed, R.string.show_direct_speed_sum
            )
            SwitchSetting(
                prefs, PrefKeys.SHOW_GROUP_IN_NOTIFICATION, false,
                R.string.show_group_in_notification
            )
            SwitchSetting(
                prefs, PrefKeys.ALWAYS_SHOW_ADDRESS, false,
                R.string.always_show_address, R.string.always_show_address_sum
            )
            SwitchSetting(
                prefs, PrefKeys.METERED, false,
                R.string.metered, R.string.metered_summary
            )
            SwitchSetting(
                prefs, PrefKeys.ACQUIRE_WAKE_LOCK, false,
                R.string.acquire_wake_lock, R.string.acquire_wake_lock_summary
            )
            MenuSetting(
                prefs, PrefKeys.LOG_LEVEL, "0", R.string.log_level,
                listOf(
                    Option("0", stringResource(R.string.log_level_none)),
                    Option("1", stringResource(R.string.log_level_warn)),
                    Option("2", stringResource(R.string.log_level_info)),
                    Option("3", stringResource(R.string.log_level_debug)),
                    Option("4", stringResource(R.string.log_level_trace))
                )
            )
            TextSetting(prefs, PrefKeys.CUSTOM_CONFIG, "", R.string.custom_config, multiline = true)
        }

        SettingSection(R.string.cag_route) {
            MenuSetting(
                prefs, PrefKeys.PER_APP_MODE, "off", R.string.per_app_mode,
                listOf(
                    Option("off", stringResource(R.string.per_app_off)),
                    Option("include", stringResource(R.string.per_app_include)),
                    Option("exclude", stringResource(R.string.per_app_exclude))
                )
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.per_app_choose)) },
                supportingContent = { Text(stringResource(R.string.per_app_choose_summary)) },
                modifier = Modifier.clickable { pickingApps = true }
            )
            if (pickingApps) {
                AppPickerDialog(prefs = prefs, onDismiss = { pickingApps = false })
            }
            SwitchSetting(prefs, PrefKeys.BYPASS_LAN, false, R.string.route_opt_bypass_lan)
            SwitchSetting(prefs, PrefKeys.BYPASS_LAN_IN_CORE, false, R.string.bypass_lan_in_core)
            MenuSetting(
                prefs, PrefKeys.TRAFFIC_SNIFFING, "1", R.string.traffic_sniffing,
                listOf(
                    Option("0", stringResource(R.string.off)),
                    Option("1", stringResource(R.string.sniff_routing)),
                    Option("2", stringResource(R.string.sniff_override))
                )
            )
            SwitchSetting(
                prefs, PrefKeys.RESOLVE_DESTINATION, false,
                R.string.resolve_destination, R.string.resolve_destination_summary
            )
            MenuSetting(
                prefs, PrefKeys.IPV6_MODE, "0", R.string.ipv6,
                listOf(
                    Option("0", stringResource(R.string.disable)),
                    Option("1", stringResource(R.string.enable)),
                    Option("2", stringResource(R.string.prefer)),
                    Option("3", stringResource(R.string.only))
                )
            )
            MenuSetting(
                prefs, PrefKeys.RULES_PROVIDER, "0", R.string.route_rules_provider,
                listOf(
                    Option("0", stringResource(R.string.route_rules_official)),
                    Option("1", stringResource(R.string.rules_loyalsoldier)),
                    Option("2", stringResource(R.string.rules_chocolate)),
                    Option("3", stringResource(R.string.rules_l11r))
                )
            )
        }

        SettingSection(R.string.cag_dns) {
            val strategies = listOf(
                Option("auto", stringResource(R.string.dns_strategy_auto)),
                Option("prefer_ipv6", stringResource(R.string.dns_strategy_prefer_ipv6)),
                Option("prefer_ipv4", stringResource(R.string.dns_strategy_prefer_ipv4)),
                Option("ipv4_only", stringResource(R.string.dns_strategy_ipv4_only)),
                Option("ipv6_only", stringResource(R.string.dns_strategy_ipv6_only))
            )
            TextSetting(prefs, PrefKeys.REMOTE_DNS, "https://dns.google/dns-query", R.string.remote_dns)
            MenuSetting(
                prefs, PrefKeys.DOMAIN_STRATEGY_REMOTE, "auto",
                R.string.domain_strategy_for_remote, strategies
            )
            TextSetting(prefs, PrefKeys.DIRECT_DNS, "https://223.5.5.5/dns-query", R.string.direct_dns)
            MenuSetting(
                prefs, PrefKeys.DOMAIN_STRATEGY_DIRECT, "auto",
                R.string.domain_strategy_for_direct, strategies
            )
            MenuSetting(
                prefs, PrefKeys.DOMAIN_STRATEGY_SERVER, "auto",
                R.string.domain_strategy_for_server, strategies
            )
            SwitchSetting(
                prefs, PrefKeys.ENABLE_DNS_ROUTING, true,
                R.string.enable_dns_routing, R.string.dns_routing_message
            )
            SwitchSetting(
                prefs, PrefKeys.ENABLE_FAKEDNS, true,
                R.string.enable_fakedns, R.string.fakedns_message
            )
            TextSetting(prefs, PrefKeys.FAKEDNS_RANGE4, "198.18.0.0/15", R.string.fakedns_range4)
            TextSetting(prefs, PrefKeys.FAKEDNS_RANGE6, "fc00::/18", R.string.fakedns_range6)
        }

        SettingSection(R.string.inbound_settings) {
            TextSetting(prefs, PrefKeys.MIXED_PORT, "2080", R.string.port_proxy, numeric = true)
            SwitchSetting(
                prefs, PrefKeys.APPEND_HTTP_PROXY, false,
                R.string.append_http_proxy, R.string.append_http_proxy_sum
            )
            SwitchSetting(
                prefs, PrefKeys.ALLOW_ACCESS, false,
                R.string.allow_access, R.string.allow_access_sum
            )
            TextSetting(prefs, PrefKeys.TUN_ADDRESS4, "172.19.0.1/30", R.string.tun_address4)
            TextSetting(prefs, PrefKeys.TUN_ADDRESS6, "fdfe:dcba:9876::1/126", R.string.tun_address6)
        }

        SettingSection(R.string.cag_misc) {
            TextSetting(
                prefs, PrefKeys.CONNECTION_TEST_URL, "https://www.gstatic.com/generate_204",
                R.string.connection_test_url
            )
            SwitchSetting(
                prefs, PrefKeys.ENABLE_CLASH_API, false,
                R.string.enable_clash_api, R.string.enable_clash_api_summary
            )
            SwitchSetting(
                prefs, PrefKeys.NETWORK_CHANGE_RESET, true,
                R.string.network_change_reset_connections
            )
            SwitchSetting(prefs, PrefKeys.WAKE_RESET, false, R.string.wake_reset_connections)
            SwitchSetting(prefs, PrefKeys.GLOBAL_ALLOW_INSECURE, false, R.string.global_allow_insecure)
            SwitchSetting(
                prefs, PrefKeys.ALLOW_INSECURE_ON_REQUEST, false,
                R.string.allow_insecure_on_request_sum
            )
            MenuSetting(
                prefs, PrefKeys.APP_TLS_VERSION, "1.2", R.string.app_tls_version,
                listOf(Option("1.2", "1.2"), Option("1.3", "1.3"))
            )
        }
        SettingSection(R.string.mirror_section) {
            SwitchSetting(
                prefs, PrefKeys.MIRROR_ENABLED, true,
                R.string.mirror_enabled, R.string.mirror_summary
            )
            TextSetting(prefs, PrefKeys.MIRROR_BASE, Mirror.DEFAULT_BASE, R.string.mirror_base)
            TextSetting(prefs, PrefKeys.MIRROR_UA, Mirror.DEFAULT_UA, R.string.mirror_ua)
            TextSetting(prefs, PrefKeys.MIRROR_OS, Mirror.DEFAULT_OS, R.string.mirror_os)
            TextSetting(prefs, PrefKeys.MIRROR_SUA, Mirror.DEFAULT_SUA, R.string.mirror_sua)
            TextSetting(prefs, PrefKeys.MIRROR_FMT, Mirror.DEFAULT_FMT, R.string.mirror_fmt)
        }
        SettingSection(R.string.country_lookup_section) {
            SwitchSetting(
                prefs, PrefKeys.COUNTRY_LOOKUP, true,
                R.string.country_lookup_title, R.string.country_lookup_summary
            )
            TextSetting(prefs, PrefKeys.IP2LOCATION_KEY, "", R.string.ip2location_key)
        }
        StorageSection()
        BackupSection()
    }
}
