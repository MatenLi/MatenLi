---
title: Troubleshooting
order: 11
summary: Fixes for the most common problems, from connection failures to port errors.
---

## The VPN will not connect

| Symptom                       | Likely cause                      | Fix                                                                                |
| ----------------------------- | --------------------------------- | ---------------------------------------------------------------------------------- |
| Connect does nothing          | The VPN permission was refused    | Tap Connect again and allow the VPN.                                               |
| "XHTTP needs the Xray core"   | The link uses XHTTP               | Wait for the Xray core in the next release.                                        |
| "port already in use"         | An old core still holds the port  | Tap Disconnect, wait five seconds, then connect. Restart the phone if it persists. |
| "outbound ... makes no sense" | A config error in a custom config | Remove Custom config in Settings, or fix the detour setting.                       |
| Connects, but no pages load   | The server or DNS is wrong        | Use Test on the profile. If the TCP ping fails, the server is unreachable.         |

## Pages are slow

- Lower the **MTU** to `1280` in App settings.
- Test other profiles and pick the one with the lowest latency.
- Turn off **Fake IP** in DNS settings if some apps misbehave.

## A site or app does not work

- Check whether it works with the VPN off. If not, the problem is not MatenLi.
- Add a **Direct** rule for that domain in [Routing](/wiki/routing/).
- Some apps detect VPNs. Use the **Apps** setting to bypass them.

## A subscription does not update

- Open the subscription address in a browser. If it fails, the provider is down.
- Check the **user agent** setting for the group.
- Make sure the interval is at least 15 minutes.

## A module does not load

- The `module.json` must be valid JSON. Check it with a JSON validator.
- `apiVersion` must be `1`.
- The ZIP must contain `module.json` at the top, not inside a folder.

## Collecting information for a bug report

Open **Settings > About** and copy the version and core versions. Then open the log and copy the last
lines around the error. Post them in the Telegram group or the GitHub issues. Remove your server
addresses and passwords first.

## Still stuck

Ask in the Telegram group: [@r_FFR_r](https://t.me/r_FFR_r).
