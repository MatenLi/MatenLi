#!/usr/bin/env python3
"""Builds the MatenLi documentation site in every language from Markdown sources.

Sources:  wiki/en/*.md, wiki/ru/*.md   one file per page; the same file name in each language
          (write and format these; Prettier formats them)
Output:   docs/<lang>/wiki/<slug>/index.html   one page per Markdown file
          docs/<lang>/wiki/index.html          documentation home with the page cards
          docs/<lang>/wiki/PROMPT.md           the AI prompt, raw, for AIs to fetch
          docs/wiki/index.html                 redirect to /en/wiki/ (keeps old links working)
          docs/wiki/PROMPT.md                  the English AI prompt, raw
          docs/assets/wiki.css                 the stylesheet, copied from scripts/wiki.css

Every source file starts with a front-matter block:

    ---
    title: Plugins
    order: 8
    summary: What a module is and how MatenLi loads it.
    ---

Inside the Markdown, links to "/wiki/..." are rewritten to the page's own language.

Requires: pip install markdown   (Python-Markdown 3.x)

Usage:  python3 scripts/build_wiki.py [repo_root]
"""
import html
import os
import re
import shutil
import sys

import markdown

ROOT = os.path.abspath(sys.argv[1]) if len(sys.argv) > 1 else os.path.abspath(
    os.path.join(os.path.dirname(__file__), ".."))
SRC_ROOT = os.path.join(ROOT, "wiki")
DOCS = os.path.join(ROOT, "docs")

LABELS = {
    "en": {
        "name": "English",
        "docs": "Documentation",
        "download": "Download",
        "github": "GitHub",
        "side": "Documentation",
        "on_page": "On this page",
        "contact": "MatenLi is open source. Contact:",
        "home_title": "Documentation",
        "home_summary": "Everything about MatenLi: the app, profiles, routing, modules and plugins.",
        "site_suffix": "MatenLi docs",
        "lang_switch": "Русский",
        "redirect": "Opening the English documentation",
    },
    "ru": {
        "name": "Русский",
        "docs": "Документация",
        "download": "Скачать",
        "github": "GitHub",
        "side": "Документация",
        "on_page": "На этой странице",
        "contact": "MatenLi с открытым кодом. Связь:",
        "home_title": "Документация",
        "home_summary": "Всё о MatenLi: приложение, профили, маршрутизация, модули и плагины.",
        "site_suffix": "документация MatenLi",
        "lang_switch": "English",
        "redirect": "Открываю документацию на русском",
    },
}
OTHER = {"en": "ru", "ru": "en"}

FRONT = re.compile(r"\A---\n(.*?)\n---\n", re.S)
WIKI_LINK = re.compile(r'(href=")/wiki/')


def parse(path):
    text = open(path, encoding="utf-8").read()
    m = FRONT.match(text)
    if not m:
        raise SystemExit(f"{path}: missing front matter")
    meta = {}
    for line in m.group(1).splitlines():
        if ":" in line:
            k, v = line.split(":", 1)
            meta[k.strip()] = v.strip()
    meta.setdefault("order", "999")
    meta["order"] = int(meta["order"])
    meta["slug"] = os.path.splitext(os.path.basename(path))[0]
    meta["body"] = text[m.end():]
    return meta


def slugify_unicode(value, separator):
    """Heading ids that keep Unicode letters, so Russian headings get readable anchors."""
    value = re.sub(r"[^\w\s-]", "", value).strip().lower()
    return re.sub(r"[-\s]+", separator, value)


def render(body, lang):
    md = markdown.Markdown(extensions=["tables", "fenced_code", "toc", "attr_list", "admonition", "sane_lists"],
                           extension_configs={"toc": {"permalink": "#", "slugify": slugify_unicode}})
    html_body = md.convert(body)
    html_body = WIKI_LINK.sub(lambda m: f'{m.group(1)}/{lang}/wiki/', html_body)
    return html_body, md.toc_tokens


PAGE = """<!doctype html>
<html lang="{lang}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title} · {suffix}</title>
<meta name="description" content="{summary}">
<link rel="icon" href="/assets/icon-512.png">
<link rel="stylesheet" href="/assets/site.css">
<link rel="stylesheet" href="/assets/wiki.css">
<link rel="alternate" hreflang="en" href="{alt_en}">
<link rel="alternate" hreflang="ru" href="{alt_ru}">
</head>
<body>
<header class="topbar">
  <a class="brand" href="/"><img src="/assets/icon-512.png" alt="" width="32" height="32"> MatenLi</a>
  <nav>
    <a href="/{lang}/wiki/">{docs}</a>
    <a href="/download/">{download}</a>
    <a href="https://github.com/MatenLi/MatenLi">{github}</a>
    <a class="lang-switch" href="{switch_href}" hreflang="{other}" lang="{other}">{lang_switch}</a>
  </nav>
</header>
<div class="layout">
  <aside class="side">
    <p class="side-title">{side}</p>
    {sidebar}
  </aside>
  <main class="content">
    <p class="crumbs"><a href="/{lang}/wiki/">{docs}</a> / {title}</p>
    <h1>{title}</h1>
    <p class="summary">{summary}</p>
    {toc_block}
    {body}
    <nav class="pager">{pager}</nav>
  </main>
</div>
<footer class="foot">{contact} <a href="https://t.me/r_FFR_r">@r_FFR_r</a></footer>
</body>
</html>
"""


def page_href(lang, slug):
    return f"/{lang}/wiki/{slug}/" if slug else f"/{lang}/wiki/"


def sidebar_html(lang, pages, current=None):
    items = []
    for p in pages:
        cls = ' class="active"' if p["slug"] == current else ""
        items.append(f'<a{cls} href="{page_href(lang, p["slug"])}">{html.escape(p["title"])}</a>')
    return '<nav class="toc-side">' + "".join(items) + "</nav>"


def pager_html(lang, pages, index):
    parts = []
    if index > 0:
        prev_p = pages[index - 1]
        parts.append(f'<a class="prev" href="{page_href(lang, prev_p["slug"])}">&larr; {html.escape(prev_p["title"])}</a>')
    if index + 1 < len(pages):
        next_p = pages[index + 1]
        parts.append(f'<a class="next" href="{page_href(lang, next_p["slug"])}">{html.escape(next_p["title"])} &rarr;</a>')
    return "".join(parts)


def toc_html(lang, toc_tokens):
    def walk(tokens):
        out = []
        for t in tokens:
            if t["level"] > 3:
                continue
            kids = walk(t.get("children", []))
            out.append(f'<li><a href="#{t["id"]}">{html.escape(t["name"])}</a>' +
                       (f"<ul>{kids}</ul>" if kids else "") + "</li>")
        return "".join(out)
    inner = walk(toc_tokens)
    if not inner:
        return ""
    return (f'<details class="on-this-page" open><summary>{LABELS[lang]["on_page"]}</summary>'
            f"<ul>{inner}</ul></details>")


def switch_target(lang, slug):
    """The same page in the other language. Falls back to that language's home if the page is missing."""
    other = OTHER[lang]
    other_dir = os.path.join(SRC_ROOT, other)
    if slug and os.path.exists(os.path.join(other_dir, slug + ".md")):
        return page_href(other, slug)
    return page_href(other, None)


def build_language(lang):
    src = os.path.join(SRC_ROOT, lang)
    if not os.path.isdir(src):
        raise SystemExit(f"missing source folder: {src}")
    labels = LABELS[lang]
    out = os.path.join(DOCS, lang, "wiki")
    if os.path.isdir(out):
        shutil.rmtree(out)
    os.makedirs(out)

    pages = [parse(os.path.join(src, f)) for f in sorted(os.listdir(src))
             if f.endswith(".md") and f != "PROMPT.md"]
    pages.sort(key=lambda p: (p["order"], p["slug"]))
    for p in pages:
        p["title"] = p.get("title", p["slug"])

    for i, p in enumerate(pages):
        body_html, toc = render(p["body"], lang)
        page_dir = os.path.join(out, p["slug"])
        os.makedirs(page_dir, exist_ok=True)
        alt_en = page_href("en", p["slug"]) if lang == "en" else switch_target("ru", p["slug"])
        alt_ru = page_href("ru", p["slug"]) if lang == "ru" else switch_target("en", p["slug"])
        with open(os.path.join(page_dir, "index.html"), "w", encoding="utf-8") as f:
            f.write(PAGE.format(
                lang=lang,
                title=html.escape(p["title"]),
                summary=html.escape(p.get("summary", "")),
                suffix=labels["site_suffix"],
                docs=labels["docs"],
                download=labels["download"],
                github=labels["github"],
                side=labels["side"],
                contact=labels["contact"],
                lang_switch=labels["lang_switch"],
                other=OTHER[lang],
                switch_href=switch_target(lang, p["slug"]),
                alt_en=alt_en,
                alt_ru=alt_ru,
                sidebar=sidebar_html(lang, pages, p["slug"]),
                toc_block=toc_html(lang, toc),
                body=body_html,
                pager=pager_html(lang, pages, i),
            ))

    cards = []
    for p in pages:
        cards.append(f'<a class="card" href="{page_href(lang, p["slug"])}"><h3>{html.escape(p["title"])}</h3>'
                     f'<p>{html.escape(p.get("summary", ""))}</p></a>')
    home_body = '<div class="cards">' + "".join(cards) + "</div>"
    with open(os.path.join(out, "index.html"), "w", encoding="utf-8") as f:
        f.write(PAGE.format(
            lang=lang,
            title=labels["home_title"],
            summary=labels["home_summary"],
            suffix=labels["site_suffix"],
            docs=labels["docs"],
            download=labels["download"],
            github=labels["github"],
            side=labels["side"],
            contact=labels["contact"],
            lang_switch=labels["lang_switch"],
            other=OTHER[lang],
            switch_href=page_href(OTHER[lang], None),
            alt_en=page_href("en", None),
            alt_ru=page_href("ru", None),
            sidebar=sidebar_html(lang, pages),
            toc_block="",
            body=home_body,
            pager="",
        ))

    prompt_src = os.path.join(SRC_ROOT, "en", "PROMPT.md")
    if os.path.exists(prompt_src):
        shutil.copy(prompt_src, os.path.join(out, "PROMPT.md"))
    return len(pages)


REDIRECT = """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>MatenLi documentation</title>
<meta http-equiv="refresh" content="0; url=/en/wiki/">
<link rel="canonical" href="/en/wiki/">
<script>location.replace("/en/wiki/" + location.search + location.hash);</script>
</head>
<body>
<p><a href="/en/wiki/">English documentation</a> · <a href="/ru/wiki/">Документация на русском</a></p>
</body>
</html>
"""


def main():
    if not os.path.isdir(SRC_ROOT):
        raise SystemExit(f"missing source folder: {SRC_ROOT}")
    os.makedirs(os.path.join(DOCS, "assets"), exist_ok=True)
    shutil.copy(os.path.join(ROOT, "scripts", "wiki.css"), os.path.join(DOCS, "assets", "wiki.css"))

    # Old single-language output lived in docs/wiki/; it is rebuilt here as a redirect.
    legacy = os.path.join(DOCS, "wiki")
    if os.path.isdir(legacy):
        shutil.rmtree(legacy)
    os.makedirs(legacy)

    counts = {lang: build_language(lang) for lang in LABELS}

    with open(os.path.join(legacy, "index.html"), "w", encoding="utf-8") as f:
        f.write(REDIRECT)
    en_prompt = os.path.join(SRC_ROOT, "en", "PROMPT.md")
    if os.path.exists(en_prompt):
        shutil.copy(en_prompt, os.path.join(legacy, "PROMPT.md"))

    print("built " + ", ".join(f"{lang}: {n} pages" for lang, n in counts.items()) + f" into {DOCS}")


if __name__ == "__main__":
    main()
