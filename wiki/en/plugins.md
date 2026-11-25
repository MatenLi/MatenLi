---
title: Plugins
order: 8
summary: What a plugin is, where it comes from, and how to find or write one.
---

Plugins are the add-ons that MatenLi loads as [modules](/wiki/modules/). This page is the overview.
Use the other pages when you want to act:

- [Make your own plugin](/wiki/make-a-plugin/): a step-by-step guide that needs no programming experience.
- [Examples](/wiki/examples/): complete, working module files to copy.
- [AI prompt](/wiki/ai-prompt/): a prompt that makes an AI assistant write a plugin for you.

## What a plugin can add

| Kind           | Example                                               |
| -------------- | ----------------------------------------------------- |
| A protocol     | A new link type, such as `stormdns://`.               |
| A core setting | A switch or choice that changes the generated config. |
| A test         | A custom latency check for a protocol.                |
| A helper       | A native library that a protocol needs to run.        |

## Installing a plugin

1. Download the plugin's `.zip` file.
2. Open **Modules** in the drawer and tap **Import (zip)**.
3. Choose the file. The plugin appears in the list.
4. Open it to change its settings, pin it, or disable it.

## Where to find plugins

- The MatenLi GitHub organisation lists the official plugins.
- Any GitHub repository that publishes a release with a `.zip` in the module format can be used.
- Ask in the Telegram group before you install a plugin from someone you do not know.

## Trust

A plugin runs code on your phone. Install only plugins whose source you can read, from releases you trust.
Check the `updateUrl` before you enable automatic update checks.

## Making your own

Start with [Make your own plugin](/wiki/make-a-plugin/). Most plugins are just a `module.json` file. You only
need a native library when the protocol needs code that is not written in JSON.
