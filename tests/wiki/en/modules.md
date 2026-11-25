---
title: Modules
order: 7
summary: How modules extend MatenLi with new protocols and settings, and how to manage them.
---

> **Status:** this page describes module format version 1, which the next release implements.
> The schema below is the contract that module authors can rely on.

## What a module is

A module is a **ZIP archive** with two kinds of file inside:

1. `module.json`: describes the module. The name, the protocols it adds, and the settings it needs.
2. Native library files (`.so`): code that the module needs at runtime, if any.

Install a module with **Modules > Import (zip)**. The app reads the JSON, checks it, and lists the module.

```text
my-module.zip
├── module.json
└── libmymodule.so
```

## What you can do with a module

| Action            | What it does                                                  |
| ----------------- | ------------------------------------------------------------- |
| Pin               | Keeps the module at the top of the list.                      |
| Enable or disable | Turns the module's protocols on or off without deleting it.   |
| Delete            | Removes the module and its settings.                          |
| Update            | Checks the GitHub releases of the module for a newer version. |
| Settings          | Opens the module's custom settings.                           |

## The module.json schema (version 1)

| Field                | Type         | Required | Meaning                                                                                               |
| -------------------- | ------------ | -------- | ----------------------------------------------------------------------------------------------------- |
| `apiVersion`         | number       | yes      | Must be `1`.                                                                                          |
| `name`               | text         | yes      | Shown in the list.                                                                                    |
| `version`            | text         | yes      | Semantic version, for example `1.2.0`. Used by updates.                                               |
| `description`        | text         | yes      | One or two sentences.                                                                                 |
| `homepage`           | URL          | no       | Project page.                                                                                         |
| `updateUrl`          | URL          | no       | GitHub repository, for example `https://github.com/owner/repo`. Updates come from its latest release. |
| `schemes`            | list of text | no       | Link prefixes the module handles at module level.                                                     |
| `helperBinary`       | object       | no       | Native library to load, per platform. For example `{"android": "libmymodule.so"}`.                    |
| `parallelPing`       | boolean      | no       | Ping this module's profiles at the same time. Default `false`.                                        |
| `pingWhileConnected` | boolean      | no       | Keep latency updated while connected. Default `true`.                                                 |
| `pingTimeoutSec`     | number       | no       | Seconds before a ping gives up. Default `10`.                                                         |
| `handoverMode`       | text         | no       | How the module hands traffic over. `signal` is the only value in version 1.                           |
| `relayWindowSec`     | number       | no       | Seconds to keep a relay alive during a handover. Default `0`.                                         |
| `protocols`          | list         | no       | Protocols the module adds. See below.                                                                 |
| `customSettings`     | list         | no       | Settings shown on the module page. See below.                                                         |

### Protocols

Each entry in `protocols` adds one protocol to the link parser and to the core:

| Field      | Type         | Meaning                                                                                                            |
| ---------- | ------------ | ------------------------------------------------------------------------------------------------------------------ |
| `id`       | text         | Unique id, lowercase, for example `stormdns-dns`.                                                                  |
| `name`     | text         | Shown in the profile editor.                                                                                       |
| `core`     | text         | `sing-box` or `xray`. The core that runs this protocol.                                                            |
| `schemes`  | list of text | Link prefixes, for example `["stormdns"]`.                                                                         |
| `outbound` | object       | The outbound (or core) configuration. Use `${key}` placeholders filled from the profile and from `customSettings`. |

### Custom settings

Each entry in `customSettings` becomes a control on the module page:

| Field        | Type         | Meaning                                       |
| ------------ | ------------ | --------------------------------------------- |
| `key`        | text         | Unique name. Use it in `${key}` placeholders. |
| `title`      | text         | The label.                                    |
| `type`       | text         | `text`, `number`, `switch` or `choice`.       |
| `default`    | any          | Starting value.                               |
| `min`, `max` | number       | Limits for `number`.                          |
| `options`    | list of text | Choices for `choice`.                         |
| `help`       | text         | A short explanation shown under the control.  |

## Example: the StormDNS module

This is a complete `module.json` with the protocol and settings fields added. The first fields come from the
original StormDNS description.

```json
{
  "apiVersion": 1,
  "name": "StormDNS",
  "version": "1.0.0",
  "description": "DNS tunnel: traffic is wrapped in DNS queries to open recursive resolvers. Useful where only DNS (port 53) is open. Carries TCP and DNS only, so UDP protocols do not work through it.",
  "homepage": "https://github.com/masterking32/MasterDnsVPN",
  "updateUrl": "https://github.com/MatenLi/stormdns-module",
  "schemes": ["stormdns"],
  "parallelPing": false,
  "pingWhileConnected": true,
  "handoverMode": "signal",
  "relayWindowSec": 0,
  "pingTimeoutSec": 60,
  "helperBinary": { "android": "libmdvpnhelper.so" },
  "protocols": [
    {
      "id": "stormdns-dns",
      "name": "StormDNS",
      "core": "sing-box",
      "schemes": ["stormdns"],
      "outbound": {
        "type": "socks",
        "server": "127.0.0.1",
        "server_port": "${localPort}",
        "version": "5"
      }
    }
  ],
  "customSettings": [
    {
      "key": "localPort",
      "title": "Local port",
      "type": "number",
      "default": 5353,
      "min": 1024,
      "max": 65535,
      "help": "The port the helper listens on."
    },
    {
      "key": "resolvers",
      "title": "Resolvers",
      "type": "text",
      "default": "",
      "help": "Comma separated DNS resolvers. Leave empty to use the built-in list."
    },
    {
      "key": "mode",
      "title": "Mode",
      "type": "choice",
      "default": "fast",
      "options": ["fast", "stable"],
      "help": "Fast is quicker, stable survives poor networks."
    },
    {
      "key": "compression",
      "title": "Compression",
      "type": "switch",
      "default": true
    }
  ]
}
```

What each part does in this example:

- The `protocols` entry means a link starting with `stormdns://` creates a StormDNS profile.
- `${localPort}` is replaced with the value from the module page, so you can change the port without editing the module.
- `helperBinary` names the native library that runs the DNS tunnel. The app loads it when the profile connects.

## Updating a module from GitHub

If `updateUrl` is set, **Check for update** reads the latest release of that repository. When its
`version` is higher than the installed one, the module shows **Update**. The release must contain a `.zip`
file in the format above.

## Rules and limits

- A module cannot change the app's own files or other modules.
- Keep `module.json` valid JSON. MatenLi shows the line and column of the first error.
- `helperBinary` is loaded as a library inside the app. Programs that run as separate processes are
  limited by Android, so a module must not depend on starting its own executable.
- Only install modules from people you trust. A module runs native code.
- See the [Plugins](/wiki/plugins/) page for how to write one.
