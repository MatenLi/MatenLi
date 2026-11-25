---
title: Connecting and tests
order: 4
summary: What the Connect button, the bottom bar and the latency numbers mean.
---

## The Connect button

The big button at the bottom of the main screen connects and disconnects. Its icon shows the state:

| Icon                    | State     | Meaning                                                                 |
| ----------------------- | --------- | ----------------------------------------------------------------------- |
| Power off               | Off       | Not connected.                                                          |
| Animated neurology icon | Testing   | The app is testing the selected server.                                 |
| Router off              | Error     | The last test or connection failed. Read the message in the bottom bar. |
| Network intel           | Connected | The test succeeded and the VPN is on.                                   |

> **Status:** the animated icons are part of the next release. Today the button shows a plain
> connect and disconnect state.

## The bottom bar

The bottom bar shows, from left to right:

- The **status**: Off, Connecting, Connected or Error.
- The **speed**: upload and download, updated every second while connected.
- The **latency**: the round-trip time to the test address, checked every ten seconds while connected.
- The **country** of your exit address, when the country lookup is turned on in settings.

## URL test

A URL test sends an HTTPS request to a test address and measures how long the answer takes. The default
address is `https://www.gstatic.com/generate_204`. It returns no content, so it is small and fast.

Use it to compare servers. Tap **Test** on a profile, or **Test all** on a group.

## TCP ping

A TCP ping only checks that the server port accepts a connection. It does not use the VPN, so it works
while you are disconnected and it is much faster. A failed TCP ping usually means the server is down,
the port is closed, or your network blocks it.

## Reading the numbers

| Latency       | Colour         | What it usually means                         |
| ------------- | -------------- | --------------------------------------------- |
| Under 150 ms  | Green          | Good. Suitable for everything.                |
| 150 to 400 ms | Amber          | Usable. Calls and games may feel slow.        |
| Over 400 ms   | Red            | Slow. Try another server.                     |
| Timeout       | Red, "timeout" | The server did not answer within the timeout. |

Latency depends on distance. A server on another continent is slow even when it is healthy.

## Example

1. You have three profiles. Tap **Test all** on the group.
2. The results show 120 ms, 310 ms and a timeout.
3. Select the 120 ms profile and connect.
4. Once connected, the bottom bar shows about 125 ms, which is close to the test result.

## Settings that change connection behaviour

These are in [Settings](/wiki/settings/):

- **Auto connect** connects when the app starts.
- **Ping while connected** keeps the latency number up to date.
- **Ping timeout** sets how long a test waits before it gives up.
- **Test URL** changes the address used for URL tests.
