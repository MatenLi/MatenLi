# Design notes

## Core

- `CoreService` is a VpnService that hosts the sing-box command server.
  It starts with a complete config built by `buildSingBoxConfig`.
- `PlatformBridge` implements libbox's platform interface: it opens the TUN
  through `VpnService.Builder`, protects sockets, and reports interfaces and logs.
- `CoreRuntime` holds state, traffic stats, uptime and the log buffer for the UI.
- Proxy mode skips the VPN consent step and runs the local mixed inbound only.

## Config

`buildSingBoxConfig` takes the settings, the selected profile and the route rules.
Without a profile it routes everything direct. The outbound from the profile is
tagged `proxy`; a `direct` outbound is always present.

## Storage

- Settings: SharedPreferences `matenli_settings`, keys in `PrefKeys`.
- Profiles, selected profile, route rules and subscriptions: JSON files in app storage.
- Nothing leaves the device except the requests the user asks for (subscription
  refresh, URL test, TCP ping).

## Navigation

Drawer destinations follow NekoBox: Configuration, Groups, Routes, Settings,
Logs, Dashboard, About. The bottom bar is optional; without it the connect
button moves into the top bar.

## Theme

Dynamic colour is the default. Presets and the custom colour replace the
primary scheme. Night mode follows the system unless set.

## Website

`docs/` is the GitHub Pages site. The download page reads the latest release
from the GitHub API at run time, so versions and files are never hard-coded.
