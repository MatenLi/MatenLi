---
title: Groups and subscriptions
order: 3
summary: Organise servers into groups and keep subscriptions up to date automatically.
---

## Groups

A group is a folder of profiles. The tabs at the top of the main screen are your groups, the same
way NekoBox shows them. Swipe between them or tap a tab.

- **Local** is the default group for profiles you added yourself.
- Each **subscription** gets its own group, named after the subscription.
- Create your own groups from the **+** menu, for example `Work` or `Travel`.

## Subscriptions

A subscription is a web address. When MatenLi opens it, the response is a list of links, either plain
text or Base64 encoded. Every link becomes a profile in that group.

### Add a subscription

1. Tap **+** and choose **Add subscription**.
2. Paste the address and give the group a name.
3. Optional: set **Auto update** and an **interval** in minutes. The minimum is 15.
4. Optional: set a **filter**. Only profiles whose name matches the filter (a regular expression) are kept.
5. Optional: set a **user agent**. Some providers only answer known clients.
6. Tap **Save**. MatenLi downloads the list and shows what changed.

### What an update does

When a subscription updates, MatenLi compares the new list with the old one and shows a dialog:

| Result  | Meaning                                                      |
| ------- | ------------------------------------------------------------ |
| Added   | A new link appeared. It is added to the group.               |
| Updated | The same profile now has different settings. It is replaced. |
| Deleted | A link disappeared. It is removed from the group.            |

Profiles you added to the group by hand are **not** touched by an update.

### Example: a filter

Your provider lists 40 servers, and you only want the ones in Europe whose names start with `EU`:

```text
^EU
```

Save the group. Only profiles whose names start with `EU` remain.

## Dedupe

If two links point to the same server and port, **dedupe** keeps only one of them. Turn it on when
a provider repeats servers.

## Test a whole group

Open the group menu and choose **Test all**. Each profile in the group is tested in turn. The results
appear on the cards, and the list can be sorted by latency.

## Troubleshooting a subscription

| Problem                   | What to try                                                                                            |
| ------------------------- | ------------------------------------------------------------------------------------------------------ |
| "Could not download"      | Open the address in a browser. If it does not load there, the provider is down or the address changed. |
| Everything is removed     | The filter may be too strict. Clear it and update again.                                               |
| Some servers never appear | Set a different **user agent**, or ask the provider whether the list is for a specific client.         |
