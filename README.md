# MatenLi

Android client for sing-box. Profiles are imported from share links or pasted
outbound JSON, and the sing-box core runs inside the app through libbox.

Owner: [@r_FFR_r](https://t.me/r_FFR_r) on Telegram.
Source: https://github.com/MatenLi/MatenLi

## Features

- Protocols from share links: VLESS, VMess, Trojan, Shadowsocks, Hysteria2, TUIC, SOCKS, HTTP.
- Other sing-box outbounds (WireGuard, SSH, ...): paste the outbound JSON.
- Groups: subscription URLs (plain or base64). Refresh replaces only that group's servers.
- Routing rules: domain suffix, domain keyword, IP range and port, sent to proxy, direct or block.
- VPN mode through VpnService and a TUN device, or proxy mode with a local mixed inbound.
- Dashboard with live speed, totals, uptime, URL test and TCP ping.
- Core log.
- Settings: Material You colours, preset accents or a custom hex colour; DNS, inbound, log level and more.
- Copy the full sing-box config to the clipboard.

## Not supported yet

- xHTTP (SplitHTTP) transport. Such profiles are listed and show a notice instead of connecting.

## Build

Requirements: Android 12 or newer (minSdk 31).

The build downloads the libbox 1.14.4 AAR into `app/libs/libbox.aar`. Then:

    ./gradlew assembleDebug

CI builds a debug APK on every push to `main`.

## Licence

Apache-2.0. See [LICENSE](LICENSE).
