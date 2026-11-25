#!/usr/bin/env python3
"""
Usage:  python3 kotlin_check.py <project_dir>
Exit code 0 = no findings, 1 = findings, 2 = bad usage.
"""
import os
import re
import sys

PROJECT_PREFIX = "dev.rffrr.mateli"

# Calls that only work in a composable context.
COMPOSABLE_ONLY = re.compile(
    r"\b(stringResource|pluralStringResource|LocalContext|LocalDensity|LocalConfiguration|"
    r"remember[A-Z]\w*|rememberCoroutineScope|mutableStateOf|collectAsState|animate\w*AsState|"
    r"MaterialTheme)\b|\bremember\s*[({]"
)
# Brace lambdas that are NOT composable content (non-inline callbacks, coroutines, threads).
# forEach and runCatching are inline, so composable calls inside them are legal and are not listed.
NON_COMPOSABLE_BRACE = re.compile(
    r"\b(onClick|onValueChange|onDismissRequest|onConfirm|onSave|onRefresh|onToggle|onChanged|"
    r"onRefreshed|onRevoke|onSelect|onLongClick|onCheckedChange|onDelete|onTest|setOnClickListener)"
    r"\s*=\s*\{|\b(launch|thread)\s*\{"
)
FUN_DECL = re.compile(r"\bfun\s+(<[^>]*>\s*)?([\w.]+)\s*\(")
IMPORT = re.compile(r"^import\s+([\w.]+)(\s+as\s+\w+)?\s*$")
R_STRING = re.compile(r"\bR\.string\.(\w+)")
PREF_KEY = re.compile(r"\bPrefKeys\.(\w+)")
# Imports that are known not to exist as top-level symbols.
BAD_IMPORTS = {
    "androidx.compose.foundation.lazy.item": "item is a LazyListScope member, not an import",
}


def kt_files(root):
    for dirpath, _, names in os.walk(root):
        if "/build" in dirpath or "/.git" in dirpath:
            continue
        for n in names:
            if n.endswith(".kt"):
                yield os.path.join(dirpath, n)


def strip_strings_and_comments(line):
    line = re.sub(r'"(\\.|[^"\\])*"', '""', line)
    line = re.sub(r"//.*", "", line)
    return line


def check_composable_context(path, lines, findings):
    """Position-aware scope scan. Events on each line are ordered by column: composable-only calls,
    non-composable lambda keywords (onClick, runCatching, ...), and braces. A non-composable keyword
    marks the next '{' as a non-composable lambda. A call is an error when such a lambda is open at
    the call's column, or when the nearest enclosing fun is not @Composable."""
    stack = []  # entries: {"kind": "fun"|"lambda", "composable": bool, "noncomp": bool}
    pending_composable = False
    for lineno, raw in enumerate(lines, 1):
        line = strip_strings_and_comments(raw)
        stripped = line.strip()
        if stripped.startswith("@Composable"):
            pending_composable = True
            continue
        if stripped.startswith("@") or not stripped:
            continue
        is_fun = FUN_DECL.search(line) is not None
        events = []
        for m in COMPOSABLE_ONLY.finditer(line):
            events.append((m.start(), "call", m.group(0)))
        noncomp_braces = {m.end() - 1 for m in NON_COMPOSABLE_BRACE.finditer(line)}
        for i, ch in enumerate(line):
            if ch in "{}":
                events.append((i, ch, None))
        events.sort(key=lambda e: e[0])
        fun_opened_here = False
        for pos, kind, name in events:
            if kind == "call":
                if any(s["noncomp"] for s in stack):
                    findings.append((path, lineno, f"'{name}' used inside a non-composable lambda"))
                else:
                    funs = [s for s in stack if s["kind"] == "fun"]
                    if funs and not funs[-1]["composable"]:
                        findings.append((path, lineno, f"'{name}' used in a function that is not @Composable"))
            elif kind == "{":
                if is_fun and not fun_opened_here:
                    stack.append({"kind": "fun", "composable": pending_composable, "noncomp": False})
                    fun_opened_here = True
                    pending_composable = False
                elif pos in noncomp_braces:
                    stack.append({"kind": "lambda", "composable": False, "noncomp": True})
                else:
                    stack.append({"kind": "lambda", "composable": False, "noncomp": False})
            elif kind == "}":
                if stack:
                    stack.pop()
        if not is_fun:
            pending_composable = False


def declared_symbols(files):
    decl = re.compile(
        r"\b(class|object|interface|fun|val|var|typealias|enum class|data class|sealed class|const val)\s+"
        r"(?:<[^>]*>\s*)?(?:[\w.]+\.)?(\w+)"
    )
    names = set()
    for path in files:
        with open(path, encoding="utf-8") as f:
            for line in f:
                for m in decl.finditer(line):
                    names.add(m.group(2))
    return names


def main():
    if len(sys.argv) != 2 or not os.path.isdir(sys.argv[1]):
        print(__doc__)
        return 2
    root = sys.argv[1]
    files = list(kt_files(root))
    findings = []

    string_names = set()
    values_dir = os.path.join(root, "app/src/main/res/values")
    if os.path.isdir(values_dir):
        for n in os.listdir(values_dir):
            if n.endswith(".xml"):
                with open(os.path.join(values_dir, n), encoding="utf-8") as f:
                    string_names |= set(re.findall(r'<string name="([^"]+)"', f.read()))

    prefs_file = next((p for p in files if p.endswith("PrefKeys.kt") or
                       re.search(r"object PrefKeys", open(p, encoding="utf-8").read())), None)
    pref_names = set()
    if prefs_file:
        with open(prefs_file, encoding="utf-8") as f:
            pref_names = set(re.findall(r"const val (\w+)", f.read()))

    symbols = declared_symbols(files)

    for path in files:
        with open(path, encoding="utf-8") as f:
            lines = f.read().splitlines()
        seen_imports = {}
        for lineno, raw in enumerate(lines, 1):
            m = IMPORT.match(raw.strip())
            if m:
                target = m.group(1)
                if target in seen_imports:
                    findings.append((path, lineno, f"duplicate import {target}"))
                seen_imports[target] = lineno
                if target in BAD_IMPORTS:
                    findings.append((path, lineno, f"bad import {target}: {BAD_IMPORTS[target]}"))
                if target == PROJECT_PREFIX + ".R" or target.endswith(".R"):
                    pass
                elif target.startswith(PROJECT_PREFIX + "."):
                    last = target.rsplit(".", 1)[1]
                    if last != "*" and last not in symbols:
                        findings.append((path, lineno, f"project import {target}: symbol '{last}' not declared"))
            for r in R_STRING.finditer(raw):
                if string_names and r.group(1) not in string_names:
                    findings.append((path, lineno, f"R.string.{r.group(1)} missing from strings.xml"))
            for p in PREF_KEY.finditer(raw):
                if pref_names and p.group(1) not in pref_names:
                    findings.append((path, lineno, f"PrefKeys.{p.group(1)} not defined"))
        check_composable_context(path, lines, findings)

    rel = lambda p: os.path.relpath(p, root)
    for path, lineno, msg in sorted(set(findings)):
        print(f"{rel(path)}:{lineno}: {msg}")
    print(f"{len(set(findings))} finding(s) in {len(files)} Kotlin file(s)")
    return 1 if findings else 0


if __name__ == "__main__":
    sys.exit(main())
