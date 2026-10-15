#!/usr/bin/env python3
"""
Rukh purge (docs/rukh/r1_core.md): rewrites vanilla `ParticleTypes.<NAME>` in the scoped Java files to the Rukh types and reports what it
could not do safely. Scope = docs/rukh/scope.txt, the same file RukhPurgeScopeTest reads (one source of truth).

    python3 tools/rukh/purge.py --scope p1                 # dry run: per file replacements + RISKY sites, nothing written
    python3 tools/rukh/purge.py --scope p1 --apply         # write the safe replacements
    python3 tools/rukh/purge.py --scope p1 --check         # exit 1 if a scoped file still has a vanilla particle (same rules as the test)
    python3 tools/rukh/purge.py --scope all --list         # list the files of a scope
    python3 tools/rukh/purge.py --apply --as dark relics/wave/Wave3.java   # explicit files (relative to src/main/java/com/newuniverse/nusmp or the repo); --as forces the alignment

Scopes: all, p1, p2, p3 (slices: the `@pN` tag of the FIRST scope.txt include line that matches a file). Why a script and not a find/replace:
a blind replace would break static initialisers (the Rukh registry is not bound at class-init time: the call must be lazy), case labels,
enum arguments and every non-simple particle (Dust, Item, Block, Shriek, ...) whose OPTIONS carry data; those lines are RISKY: they are
reported and left for a human. A line is replaced as a whole or not at all. A line with `// rukh-allow: <reason>` is skipped everywhere.
"""
import argparse
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
PKG = "src/main/java/com/newuniverse/nusmp"
RUKH = "com.newuniverse.nusmp.particle.rukh.RukhParticles"

DARK = {
    "SMOKE", "LARGE_SMOKE", "SOUL", "SOUL_FIRE_FLAME", "SQUID_INK", "GLOW_SQUID_INK", "SCULK_SOUL", "SCULK_CHARGE_POP", "WITCH",
    "WARPED_SPORE", "CRIMSON_SPORE", "ASH", "WHITE_ASH", "CAMPFIRE_COSY_SMOKE", "CAMPFIRE_SIGNAL_SMOKE", "DRAGON_BREATH", "EFFECT",
    "DAMAGE_INDICATOR", "SONIC_BOOM", "PORTAL", "REVERSE_PORTAL", "ELDER_GUARDIAN", "ANGRY_VILLAGER", "INFESTED", "OMINOUS_SPAWNING",
    "RAID_OMEN", "TRIAL_OMEN", "DRIPPING_OBSIDIAN_TEAR", "FALLING_OBSIDIAN_TEAR", "LANDING_OBSIDIAN_TEAR", "SPIT", "WHITE_SMOKE",
}
# particle types whose options carry data (not SimpleParticleType in 1.21.1): never rewritten by the script
NONSIMPLE = {"BLOCK", "BLOCK_MARKER", "DUST", "DUST_COLOR_TRANSITION", "ENTITY_EFFECT", "FALLING_DUST", "SCULK_CHARGE", "ITEM",
             "VIBRATION", "SHRIEK", "DUST_PILLAR"}
OPTION_CLASSES = re.compile(r"DustParticleOptions|DustColorTransitionOptions|ItemParticleOption|BlockParticleOption|ColorParticleOption|"
                            r"ShriekParticleOption|VibrationParticleOption|SculkChargeParticleOptions|PowerParticleOption|SpellParticleOption")
PT = re.compile(r"\bParticleTypes\.([A-Z][A-Z0-9_]*)")
# same rules as RukhPurgeScopeTest
BAD = re.compile(r"ParticleTypes\.|DustParticleOptions|new\s+DustColorTransitionOptions|ItemParticleOption|BlockParticleOption")
CALL = re.compile(r"\b(?:addParticle|sendParticles)\(\s*(?!(?:$|[a-z_][A-Za-z0-9_]*\s*,|(?:com\.newuniverse\.nusmp\.particle\.rukh\.)?RukhParticles\.))")


def glob_re(g):
    out, i = [], 0
    while i < len(g):
        c = g[i]
        if c == "*":
            if g[i:i + 2] == "**":
                i += 1
                if g[i + 1:i + 2] == "/":
                    i += 1
                    out.append("(?:.*/)?")
                else:
                    out.append(".*")
            else:
                out.append("[^/]*")
        elif c == "?":
            out.append("[^/]")
        elif c == "[":
            j = g.index("]", i)
            cls = g[i + 1:j]
            out.append("[" + ("^" + cls[1:] if cls.startswith("!") else cls) + "]")
            i = j
        elif c in "\\.^$|(){}+":
            out.append("\\" + c)
        else:
            out.append(c)
        i += 1
    return re.compile("".join(out))


def load_scope(root):
    inc, exc = [], []
    for raw in open(os.path.join(root, "docs/rukh/scope.txt"), encoding="utf-8"):
        s = raw.strip()
        if not s or s.startswith("#"):
            continue
        m = re.search(r"\s+@(p\d+)$", s)
        tag = m.group(1) if m else None
        if m:
            s = s[:m.start()].strip()
        if s.startswith("!"):
            exc.append(glob_re(s[1:].strip()))
        else:
            inc.append((glob_re(s), tag))
    return inc, exc


def scoped_files(root, scope):
    inc, exc = load_scope(root)
    base = os.path.join(root, PKG)
    res = []
    for d, _, fs in os.walk(base):
        for f in sorted(fs):
            if not f.endswith(".java"):
                continue
            rel = os.path.relpath(os.path.join(d, f), base).replace(os.sep, "/")
            if any(p.fullmatch(rel) for p in exc):
                continue
            tag = next((t for p, t in inc if p.fullmatch(rel)), "NONE")
            if tag == "NONE":
                continue
            if scope == "all" or tag == scope:
                res.append(os.path.join(base, rel))
    return sorted(res)


def is_comment(line):
    t = line.strip()
    return t.startswith("//") or t.startswith("*") or t.startswith("/*")


def risky_reason(line, is_enum_file, name_ok):
    t = line.strip()
    if "rukh-allow:" in line:
        return "rukh-allow"
    if OPTION_CLASSES.search(line):
        return "particle OPTIONS class (Dust/Item/Block/...)"
    if re.search(r"\bParticleOptions\b", line) and "(" not in t.split("ParticleOptions")[0]:
        return "ParticleOptions-typed value"
    if re.match(r"(public |private |protected )?(static |final )+", t) and ("=" in t or "->" in t or "::" in t):
        return "static/final initialiser (must become lazy)"
    if re.match(r"case\s", t) or re.search(r"\bcase\s[^:>]*ParticleTypes", line):
        return "case label"
    if is_enum_file and re.match(r"[A-Z][A-Z0-9_]*\s*\(", t):
        return "enum constructor argument"
    if re.search(r"\bParticleTypes\b(?!\.[A-Z])", re.sub(r"^import .*", "", line)):
        return "ParticleTypes used through another expression"
    if not name_ok:
        return "non-simple particle type (carries options)"
    return None


def process(path, force, apply, root):
    src = open(path, encoding="utf-8").read()
    lines = src.split("\n")
    enum_file = bool(re.search(r"\benum\s+\w+", src))
    n_rep, risky, out = 0, [], []
    for no, line in enumerate(lines, 1):
        if is_comment(line) or line.lstrip().startswith("import ") or not (PT.search(line) or OPTION_CLASSES.search(line) or re.search(r"\bParticleTypes\b", line)):
            out.append(line)
            continue
        names = PT.findall(line)
        name_ok = all(n not in NONSIMPLE for n in names)
        why = risky_reason(line, enum_file, name_ok)
        if why:
            if why != "rukh-allow":
                risky.append((no, why, line.strip()))
            out.append(line)
            continue
        def sub(m):
            n = m.group(1)
            dark = (force == "dark") if force else (n in DARK)
            return "%s.%s()" % (RUKH, "dark" if dark else "light")
        new, k = PT.subn(sub, line)
        n_rep += k
        out.append(new)
    if apply and n_rep:
        open(path, "w", encoding="utf-8", newline="").write("\n".join(out))
    return n_rep, risky


def check(path):
    bad = []
    for no, line in enumerate(open(path, encoding="utf-8").read().split("\n"), 1):
        if is_comment(line) or "rukh-allow:" in line:
            continue
        if BAD.search(line) or CALL.search(line):
            bad.append((no, line.strip()))
    return bad


def resolve(root, f):
    for c in (f, os.path.join(root, f), os.path.join(root, PKG, f)):
        if os.path.isfile(c):
            return os.path.abspath(c)
    sys.exit("no such file: " + f)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("files", nargs="*")
    ap.add_argument("--scope", help="all | p1 | p2 | p3 (docs/rukh/scope.txt)")
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--as", dest="force", choices=["dark", "light"])
    ap.add_argument("--root", default=ROOT)
    a = ap.parse_args()
    root = os.path.abspath(a.root)
    files = [resolve(root, f) for f in a.files]
    if a.scope:
        files += scoped_files(root, a.scope)
    if not files:
        ap.error("give --scope or files")
    files = sorted(set(files))
    if a.list:
        print("\n".join(os.path.relpath(f, os.path.join(root, PKG)) for f in files))
        return
    if a.check:
        total = 0
        for f in files:
            for no, t in check(f):
                print("%s:%d: %s" % (os.path.relpath(f, os.path.join(root, PKG)), no, t))
                total += 1
        print("rukh purge check: %d vanilla particle site(s) left in %d file(s)" % (total, len(files)))
        sys.exit(1 if total else 0)
    tot, trisky = 0, 0
    for f in files:
        n, risky = process(f, a.force, a.apply, root)
        rel = os.path.relpath(f, os.path.join(root, PKG))
        if n or risky:
            print("%s: %d replaced%s" % (rel, n, "" if a.apply else " (dry run)"))
            for no, why, t in risky:
                print("    RISKY %d [%s] %s" % (no, why, t[:150]))
        tot += n
        trisky += len(risky)
    left = sum(len(check(f)) for f in files)
    print("%s: %d replacement(s), %d RISKY site(s) to fix by hand, %d vanilla site(s) still flagged by --check, %d file(s)"
          % ("applied" if a.apply else "dry run", tot, trisky, left, len(files)))


if __name__ == "__main__":
    main()
