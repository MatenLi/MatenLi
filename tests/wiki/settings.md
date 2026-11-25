---
title: Settings reference
order: 6
summary: Every setting in MatenLi, what it does, and its default.
---

Open **Settings** from the drawer. The settings are grouped in sections. This page lists them in the same order.

> **Status:** Block ads, Country lookup, Export settings and Import settings arrive in the next release.
> Route, DNS and Inbound settings are shown here as the target layout.

## App settings

| Setting         | Default | What it does                                                                                               |
| --------------- | ------- | ---------------------------------------------------------------------------------------------------------- |
| Auto connect    | Off     | Connects to the selected profile when the app opens.                                                       |
| Night mode      | System  | Light, dark or follow the system.                                                                          |
| App theme       | Dynamic | Dynamic colours from your wallpaper, one of five presets, or a custom colour (hex, for example `#7F52CF`). |
| Show bottom bar | On      | Turn off to get more room for the list.                                                                    |
| Service mode    | VPN     | VPN is the normal mode. Proxy-only runs the local proxy without a VPN.                                     |
| TUN interface   | On      | The virtual network device that carries all traffic.                                                       |
| MTU             | 1500    | Packet size. Lower it (for example 1280) if pages stall on some networks.                                  |
| Speed interval  | 1 s     | How often the speed numbers update.                                                                        |
| Log level       | Info    | How much the core writes to the log. Debug is verbose.                                                     |
| Custom config   | Empty   | Your own sing-box JSON. It replaces the generated config. Advanced.                                        |

## Route settings

| Setting        | Default | What it does                               |
| -------------- | ------- | ------------------------------------------ |
| Final outbound | Proxy   | What traffic matches no rule goes through. |
| Block ads      | Off     | Blocks a list of advertising domains.      |
| Bypass private | On      | Keeps local addresses direct.              |

## DNS settings

| Setting    | Default                     | What it does                                |
| ---------- | --------------------------- | ------------------------------------------- |
| Remote DNS | `https://1.1.1.1/dns-query` | DNS used for names going through the proxy. |
| Local DNS  | `8.8.8.8`                   | DNS used for names going direct.            |
| Fake IP    | On                          | See [Routing](/wiki/routing/#dns).          |
| Strategy   | IPv4 first                  | Choose IPv4 only, IPv6 only, or prefer one. |

## Inbound settings

| Setting                | Default | What it does                            |
| ---------------------- | ------- | --------------------------------------- |
| Mixed port             | 2080    | Local SOCKS and HTTP port.              |
| Allow LAN              | Off     | Lets other devices use the local proxy. |
| Auth user and password | Empty   | Protects the local proxy.               |

## Connection testing

| Setting              | Default                                | What it does                                                |
| -------------------- | -------------------------------------- | ----------------------------------------------------------- |
| Test URL             | `https://www.gstatic.com/generate_204` | Address for URL tests.                                      |
| Ping timeout         | 5 s                                    | How long a test waits.                                      |
| Ping while connected | On                                     | Keeps the latency number current.                           |
| Parallel ping        | Off                                    | Tests several profiles at once. Can overload slow networks. |

## Country lookup

| Setting        | Default | What it does                                                       |
| -------------- | ------- | ------------------------------------------------------------------ |
| Country lookup | On      | Looks up the country of your exit address with api.ip2location.io. |
| API key        | Empty   | Optional. Without a key the free limit is 1,000 lookups a day.     |

Only the exit IP address is sent to ip2location.io. Turn this off to keep your address off third-party servers.

## Modules

| Setting           | What it does                                     |
| ----------------- | ------------------------------------------------ |
| Installed modules | The modules page. See [Modules](/wiki/modules/). |

## Other

| Setting         | What it does                        |
| --------------- | ----------------------------------- |
| Documentation   | Opens this site.                    |
| About           | Version, core versions and contact. |
| Export settings | Saves all settings to a file.       |
| Import settings | Loads a settings file.              |
