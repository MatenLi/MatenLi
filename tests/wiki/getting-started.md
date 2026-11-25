---
title: Getting started
order: 1
summary: A step-by-step guide for people who have never used a VPN client before.
---

## What MatenLi does

MatenLi is an Android app that sends your phone's internet traffic through a **server** you choose.
The server is run by someone else (a VPN provider, a friend, or yourself). MatenLi only holds the
settings that tell it how to reach that server.

You need three things:

1. An **Android phone** running Android 12 or newer.
2. A **link** from your provider. It starts with a word such as `vless://`, `vmess://`, `trojan://`,
   `ss://` or `hysteria2://`. Some providers give you a web address called a **subscription** instead.
3. The **MatenLi APK** from the [download page](/download/).

## Install the app

1. Open the [download page](/download/) on your phone and download the newest `matenli-debug` APK.
2. Open the file. Android asks you to allow installs from this source. Allow it for the browser you used.
3. Tap **Install**. The app is not in the Play Store, so Android may show a warning. It is the
   same APK that the public build pipeline produced.

> **Tip:** if you see "App not installed", you probably have an older MatenLi installed.
> Uninstall it first, then install again.

## Add your first server

1. Copy the link your provider gave you.
2. Open MatenLi and tap the **+** button at the bottom right.
3. Choose **Paste from clipboard**. The server appears in the list.
4. Tap the server once to select it. A small check mark shows which server is selected.

If you got a **subscription** address instead, choose **Add subscription** in the menu, paste the
address, and save. Every server in it is added to a group.

## Connect

1. Tap the big **Connect** button.
2. Android asks you to allow a VPN. Tap **OK**. The key icon in the status bar means the VPN is on.
3. Check the bottom bar. It shows the status, your speed, and the latency to a test address.

To disconnect, tap the same button again.

## Check that it works

Open a browser and search for "what is my ip". The address should now be the address of your server,
not your home or mobile address.

If the page does not load, read [Troubleshooting](/wiki/troubleshooting/).

## Words you will see

| Word         | What it means                                                                            |
| ------------ | ---------------------------------------------------------------------------------------- |
| Profile      | One server with its settings.                                                            |
| Group        | A folder of profiles, usually one per subscription.                                      |
| Subscription | A web address that lists many profiles. It can update itself.                            |
| Latency      | How long one round trip to a test address takes, in milliseconds. Lower is better.       |
| Route        | A rule that says which traffic goes through the server and which goes direct.            |
| Module       | An add-on that teaches MatenLi a new protocol or setting. See [Modules](/wiki/modules/). |

## Next steps

- [Profiles and links](/wiki/profiles/): add, edit, test and share servers.
- [Groups and subscriptions](/wiki/groups/): keep many servers organised and up to date.
- [Connecting and tests](/wiki/connecting/): what the latency numbers mean.
