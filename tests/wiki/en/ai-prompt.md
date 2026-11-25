---
title: AI prompt
order: 12
summary: A ready-made prompt that makes an AI assistant write a MatenLi plugin for you.
---

Paste the full prompt into an AI assistant and describe the plugin you want. The complete prompt is
also published as a plain file for AIs to fetch: [/wiki/PROMPT.md](/wiki/PROMPT.md).

## How to use it

1. Open the [PROMPT.md file](/wiki/PROMPT.md) and copy everything in it.
2. Start a new chat with an AI assistant and paste the prompt first.
3. Under the prompt, write what you want. For example:

   > Make a MatenLi plugin for the StormDNS tunnel. Users pick a local port and a resolver list.

4. Read the answer. Save the `module.json` it gives you, check it with a JSON validator, and zip it.
5. Import the ZIP in MatenLi under **Modules**.

## What the prompt does

The prompt tells the AI:

- the exact module format (version 1), including every field and its type
- which cores exist and which fields they expect
- to ask one question when the protocol is unclear
- to return only a valid `module.json` and a short explanation

## Tips

- Give the AI the link or documentation of the protocol you want to add.
- Ask it to explain each field in plain words, so you learn the format.
- Always check the result. An AI can make mistakes, and a plugin runs on your phone.
