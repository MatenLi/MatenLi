---
title: Make your own plugin
order: 9
summary: A plain-language guide to building a MatenLi module, with no programming experience needed.
---

This guide builds a small plugin from scratch. You need a text editor, a ZIP tool, and a GitHub account
if you want to publish it. No programming is needed for the first version.

## Step 1: understand the parts

A plugin is a folder that becomes a ZIP file:

```text
my-plugin/
└── module.json
```

`module.json` is a text file that describes your plugin. That is all a simple plugin needs.

## Step 2: write module.json

Create a file called `module.json` with a text editor. Copy this to start:

```json
{
  "apiVersion": 1,
  "name": "My first plugin",
  "version": "0.1.0",
  "description": "Adds a setting to my own server."
}
```

Save it. This plugin does nothing yet, but MatenLi accepts it. You can now test the import.

## Step 3: check the file

Paste the content into any JSON checker, for example a page that says "JSON validator". If it says valid, the
file is correct. A common mistake is a missing comma or a missing quote. Each `"` must have a pair.

## Step 4: add a setting

A setting appears on the module page. Add a `customSettings` list:

```json
{
  "apiVersion": 1,
  "name": "My first plugin",
  "version": "0.1.0",
  "description": "Adds a setting to my own server.",
  "customSettings": [
    {
      "key": "greeting",
      "title": "Greeting",
      "type": "text",
      "default": "hello",
      "help": "Any text you like."
    }
  ]
}
```

Import the file again. The module page now shows a text box called **Greeting**.

## Step 5: add a protocol

A protocol tells MatenLi how to build a connection from a link. This is the part that needs care.
Copy the example in [Examples](/wiki/examples/) and change only the names and the scheme. The
[Modules](/wiki/modules/) page lists every field.

## Step 6: zip it

1. Select `module.json` and any other files.
2. Right-click and choose **Compress** (Windows) or **Create archive** (macOS, Linux).
3. Name the archive `my-plugin.zip`. The file must be a ZIP, not a RAR or 7z.

## Step 7: publish it on GitHub

1. Create a new public repository, for example `my-plugin`.
2. Upload `module.json` in the repository.
3. Open **Releases**, click **Create a new release**, choose a tag like `v0.1.0`, and attach `my-plugin.zip`.
4. Add `"updateUrl": "https://github.com/your-name/my-plugin"` to `module.json` so updates work.

When you raise the `version` and publish a new release, the app offers the update.

## Common problems

| Problem                   | Fix                                                                             |
| ------------------------- | ------------------------------------------------------------------------------- |
| "Invalid JSON"            | Check commas, quotes and brackets. Use a JSON validator.                        |
| "Unsupported apiVersion"  | Set `"apiVersion": 1`.                                                          |
| The setting does not show | Check that `customSettings` is inside the main object, not inside another item. |
| Update does not appear    | The release tag must be higher than the installed `version`.                    |
