---
title: Profiles and links
order: 2
summary: Add, edit, test, duplicate, share and delete servers.
---

## What a profile is

A profile is one server together with everything needed to reach it: the address, the port,
the protocol, the security settings and a name. Each profile is stored on your phone only.

## Adding a profile

There are five ways to add one. Tap **+** at the bottom right and choose:

| Option               | Use it when                                                   |
| -------------------- | ------------------------------------------------------------- |
| Paste from clipboard | You copied a link. This is the usual way.                     |
| Scan QR code         | Your provider shows a QR code.                                |
| Import from file     | You saved a text file with one link per line.                 |
| Add manually         | You know the server details and want to type them.            |
| Add subscription     | You have a subscription address. See [Groups](/wiki/groups/). |

### Supported link types

| Link start                 | Protocol          | Notes                             |
| -------------------------- | ----------------- | --------------------------------- |
| `vless://`                 | VLESS             | Works with Reality and TLS.       |
| `vmess://`                 | VMess             |                                   |
| `trojan://`                | Trojan            |                                   |
| `ss://`                    | Shadowsocks       |                                   |
| `hysteria2://` or `hy2://` | Hysteria 2        | UDP based.                        |
| `tuic://`                  | TUIC              | Runs on the sing-box core.        |
| `socks://`, `http://`      | SOCKS, HTTP proxy |                                   |
| `{ ... }`                  | Raw sing-box JSON | Paste a complete outbound object. |

> **Status:** XHTTP (SplitHTTP) links need the Xray core. That core is being added in the next
> release. Until then MatenLi shows the message "XHTTP needs the Xray core" instead of connecting.

## Editing a profile

Tap and hold a profile, or tap the **...** button on it, then choose **Edit**. The editor shows every
field for that protocol. Fields that are not needed for the protocol are hidden.

- Change the **name** to something you will recognise.
- Leave **advanced** fields alone unless your provider told you to change them.
- Tap **Save**. Errors appear under the field that caused them.

## Actions on a profile

Tap the **...** button on a profile to open its menu:

| Action        | What it does                                                                                   |
| ------------- | ---------------------------------------------------------------------------------------------- |
| Test          | Measures latency to the test address. The result appears on the card.                          |
| TCP ping      | Checks that the server port answers. Faster and does not need the VPN.                         |
| Edit          | Opens the editor.                                                                              |
| Duplicate     | Makes a copy, useful for trying a different port or route.                                     |
| Share link    | Shows the link and a QR code.                                                                  |
| Copy link     | Copies the link to the clipboard.                                                              |
| Move to group | Moves the profile to another group.                                                            |
| Delete        | Removes the profile. A subscription profile comes back the next time the subscription updates. |

## Example

You have a link:

```text
vless://0f5c1a2e-1111-4444-9999-abcdef012345@example.org:443?security=tls&type=ws&path=%2Fchat&sni=example.org#My%20home%20server
```

Pasting this creates a profile named **My home server**. Its server is `example.org`, port `443`,
over TLS with WebSocket. Tap **Test** to measure it.

## Tips

- Keep one profile per server. Duplicate it when you want a variant.
- Name profiles by place and provider, for example `Helsinki, provider A`. Names are free text.
- Do not share links you did not create. A link contains the password to the server.
