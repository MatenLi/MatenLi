---
title: Routing, DNS and inbound
order: 5
summary: Choose what goes through the server, how names are resolved, and how local apps connect.
---

## Routing

A **route** is a rule. For each connection, MatenLi checks the rules from top to bottom. The first rule that
matches decides what happens to the traffic.

Each rule has three parts:

| Part       | Example               | Meaning                      |
| ---------- | --------------------- | ---------------------------- |
| Match type | Domain, IP, port, app | What the rule looks at.      |
| Value      | `*.example.org`       | The text or range to match.  |
| Outbound   | Proxy, Direct, Block  | Where matching traffic goes. |

### Examples

| Goal                               | Match type | Value                 | Outbound |
| ---------------------------------- | ---------- | --------------------- | -------- |
| Local banking app bypasses the VPN | Domain     | `bank.example.com`    | Direct   |
| Your home network stays local      | IP         | `192.168.0.0/16`      | Direct   |
| Block a tracker                    | Domain     | `tracker.example.net` | Block    |
| Route all video through the server | Port       | `1935`                | Proxy    |

Private addresses (`10.x`, `192.168.x`, `127.x`) are always sent direct so local devices keep working.

## DNS

DNS turns a name such as `example.org` into an address. MatenLi has three DNS servers:

| Server  | Used for                                                                                           |
| ------- | -------------------------------------------------------------------------------------------------- |
| Remote  | Names going through the proxy. Resolved on the server side.                                        |
| Local   | Names going direct, for example your home router.                                                  |
| Fake IP | Lets MatenLi see which name an app asked for, even for IP-only traffic. Turn it off if apps break. |

Settings for each server are in [Settings](/wiki/settings/#dns-settings).

## Inbound (local proxy)

MatenLi can also open a local proxy on your phone. Other apps, or another device on your Wi-Fi, can then
use MatenLi as a SOCKS or HTTP proxy.

| Setting    | Default | Notes                                                     |
| ---------- | ------- | --------------------------------------------------------- |
| Mixed port | 2080    | SOCKS and HTTP on one port.                               |
| Allow LAN  | Off     | Turn on only on a trusted network.                        |
| Auth       | None    | Add a user name and password before you allow LAN access. |

> **Warning:** an open proxy on a public network lets strangers use your server. Keep auth on.

## Apps (per-app VPN)

By default every app goes through the VPN. The **Apps** setting lets you choose:

- **Proxy all apps**: the default.
- **Only selected apps**: only the apps you tick go through the VPN.
- **Bypass selected apps**: every app goes through the VPN except the ones you tick.

Pick apps from the list. Search by name when the list is long.

> **Status:** the per-app picker replaces an earlier on/off switch. It arrives in the next release.
