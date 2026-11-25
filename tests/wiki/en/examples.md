---
title: Examples
order: 10
summary: Complete module files you can copy, with notes on what each part does.
---

Each example is a complete `module.json`. Copy one into a file with that name, put it in a folder,
zip the folder, and import the ZIP.

## Example 1: a setting only

Adds a switch to MatenLi without any protocol. Useful to learn the format.

```json
{
  "apiVersion": 1,
  "name": "Extra logging",
  "version": "1.0.0",
  "description": "Writes more detail to the log for debugging.",
  "customSettings": [
    {
      "key": "verbose",
      "title": "Verbose log",
      "type": "switch",
      "default": false,
      "help": "Shows extra lines in the log. Turn off after debugging."
    }
  ]
}
```

## Example 2: a protocol with a choice

Adds a new link type that runs on sing-box and lets the user pick a mode.

```json
{
  "apiVersion": 1,
  "name": "Example tunnel",
  "version": "1.0.0",
  "description": "A demo protocol that shows how links become profiles.",
  "updateUrl": "https://github.com/your-name/example-tunnel",
  "schemes": ["extunnel"],
  "protocols": [
    {
      "id": "extunnel",
      "name": "Example tunnel",
      "core": "sing-box",
      "schemes": ["extunnel"],
      "outbound": {
        "type": "socks",
        "server": "${host}",
        "server_port": "${port}",
        "version": "5"
      }
    }
  ],
  "customSettings": [
    {
      "key": "mode",
      "title": "Mode",
      "type": "choice",
      "default": "auto",
      "options": ["auto", "fast", "safe"],
      "help": "Auto chooses for you."
    }
  ]
}
```

A link `extunnel://example.org:1080` would create a profile whose host is `example.org` and whose port
is `1080`.

## Example 3: a helper library

Adds a protocol that needs a native library. The library is placed in the ZIP next to `module.json`.

```json
{
  "apiVersion": 1,
  "name": "Helper protocol",
  "version": "1.0.0",
  "description": "Runs a helper library that the protocol needs.",
  "helperBinary": { "android": "libhelper.so" },
  "pingTimeoutSec": 30,
  "protocols": [
    {
      "id": "helper-proto",
      "name": "Helper protocol",
      "core": "sing-box",
      "schemes": ["helper"],
      "outbound": {
        "type": "socks",
        "server": "127.0.0.1",
        "server_port": "${localPort}"
      }
    }
  ],
  "customSettings": [
    {
      "key": "localPort",
      "title": "Local port",
      "type": "number",
      "default": 1080,
      "min": 1024,
      "max": 65535
    }
  ]
}
```

The ZIP then contains `module.json` and `libhelper.so`. Only use a library built for the phone's
processor (arm64 for almost all modern Android phones).

## Example 4: updating from GitHub

Set `updateUrl` to the repository. Every new release with a higher `version` is offered as an update:

```json
{
  "apiVersion": 1,
  "name": "Updatable plugin",
  "version": "2.3.1",
  "description": "Checks its own repository for updates.",
  "updateUrl": "https://github.com/your-name/updatable-plugin"
}
```

## Checklist before you publish

- The file is valid JSON.
- `apiVersion` is `1`.
- Every `key` in `customSettings` is unique.
- Every `${name}` in an outbound has a matching `key`, or a field the profile provides.
- The ZIP contains `module.json` at the top level, not inside another folder.
