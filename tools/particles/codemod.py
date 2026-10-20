#!/usr/bin/env python3
"""Re-runnable codemod: moves the mod's particle call sites from the vanilla particles and from the Light/Dark Rukh to the custom particle facade (particle.fx.Fx).

What it rewrites (map.json says where and with which theme):
  * level.sendParticles(ParticleTypes.X, x, y, z, count, dx, dy, dz, speed)   ->  Fx.spray(level, THEME, FxOld.X, x, ..., speed)
  * level.addParticle(ParticleTypes.X, x, y, z, vx, vy, vz)                   ->  Fx.add(level, THEME, FxOld.X, x, ..., vz)
  * PerfParticles.send(level, ParticleTypes.X, ...): the budgeted emitters keep PerfParticles.send (a test pins it); only the particle becomes Fx.options(THEME, FxOld.X)
  * level.sendParticles / addParticle(new DustParticleOptions(c, s), ...)      ->  Fx.sprayDust / Fx.addDust(level, THEME, c, s, ...)
  * RukhFx.spray/burst/stream/gather/aura and RukhClientFx.* (outside the Djinn and dungeon paths)  ->  Fx.* with the theme as second argument
  * RukhFx.Alignment -> FxTone, level.sendParticles(RukhParticles.light(), ...) -> Fx.spray(level, THEME, FxTone.LIGHT, ...), RukhParticles.light() as a value -> Fx.options(THEME, FxTone.LIGHT)
  * any other mappable ParticleTypes.X used as a value (a switch result, a helper argument) -> Fx.options(THEME, FxOld.X)
It never touches: Time Magic files, the particle libraries, files other builders work in (the lanes in map.json: relics with --include-relics, the rift pockets with
--include-lanes), calls that carry data it cannot map (block / item / falling-dust options, dust built by a helper, a particle typed SimpleParticleType). Rukh calls in the Djinn and
dungeon paths ("rukh_keep") stay Rukh by the owner's decision. It is idempotent: a second run changes nothing.

It also writes the two generated files the rewritten code needs: particle/fx/FxThemes.java (one ThemeKey constant per theme of map.json) and particle/fx/FxOld.java (one entry per vanilla
particle of map.json "particles").

    python3 -B tools/particles/codemod.py                        # rewrite in place and print the report
    python3 -B tools/particles/codemod.py --check                # change nothing; exit 1 if a run would change something (for CI)
    python3 -B tools/particles/codemod.py --include-relics --include-lanes   # after the other builders merged: the relic waves, client relic FX and the rift pockets too
    python3 -B tools/particles/codemod.py --report out.json      # also write the report as json
    python3 -B tools/particles/codemod.py --only 'item/**'       # only the files matching a glob
"""
import argparse
import json
import os
import re
import sys
from collections import Counter, defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
PKG = "com.newuniverse.nusmp"
FX_PKG = PKG + ".particle.fx"
FX_DIR_REL = "particle/fx"

NAMES = {"Fx": FX_PKG + ".Fx", "FxTone": FX_PKG + ".FxTone", "FxOld": FX_PKG + ".FxOld", "FxThemes": FX_PKG + ".FxThemes", "ThemeKey": FX_PKG + ".ThemeKey"}


# ---------------------------------------------------------------------------------------------- globs
def glob_re(g):
    out = []
    i = 0
    while i < len(g):
        c = g[i]
        if c == "*":
            if i + 1 < len(g) and g[i + 1] == "*":
                i += 1
                if i + 1 < len(g) and g[i + 1] == "/":
                    i += 1
                    out.append("(?:.*/)?")
                else:
                    out.append(".*")
            else:
                out.append("[^/]*")
        elif c == "?":
            out.append("[^/]")
        elif c in ".^$|(){}+[]\\":
            out.append("\\" + c)
        else:
            out.append(c)
        i += 1
    return re.compile("".join(out) + r"\Z")


class Globs:
    def __init__(self, globs):
        self.res = [glob_re(g) for g in globs]

    def has(self, rel):
        return any(r.match(rel) for r in self.res)


# ---------------------------------------------------------------------------------------------- java scanning
def mask(src):
    """Same length as src with comments, string literals, text blocks and char literals blanked (newlines kept), so scans ignore them."""
    out = list(src)
    n = len(src)
    i = 0
    while i < n:
        c = src[i]
        two = src[i:i + 2]
        if two == "//":
            j = src.find("\n", i)
            j = n if j < 0 else j
            for k in range(i, j):
                out[k] = " "
            i = j
        elif two == "/*":
            j = src.find("*/", i + 2)
            j = n if j < 0 else j + 2
            for k in range(i, j):
                if src[k] != "\n":
                    out[k] = " "
            i = j
        elif src[i:i + 3] == '"""':
            j = src.find('"""', i + 3)
            j = n if j < 0 else j + 3
            for k in range(i, j):
                if src[k] != "\n":
                    out[k] = " "
            i = j
        elif c == '"':
            j = i + 1
            while j < n and src[j] != '"':
                j += 2 if src[j] == "\\" else 1
            j = min(n, j + 1)
            for k in range(i + 1, j - 1):
                out[k] = " "
            i = j
        elif c == "'":
            j = i + 1
            while j < n and src[j] != "'":
                j += 2 if src[j] == "\\" else 1
            j = min(n, j + 1)
            for k in range(i + 1, j - 1):
                out[k] = " "
            i = j
        else:
            i += 1
    return "".join(out)


def match_close(m, open_idx):
    """Index of the bracket that closes the one at open_idx in masked text m (or -1)."""
    pairs = {"(": ")", "[": "]", "{": "}"}
    stack = [pairs[m[open_idx]]]
    i = open_idx + 1
    while i < len(m):
        c = m[i]
        if c in pairs:
            stack.append(pairs[c])
        elif c in ")]}":
            if not stack or c != stack[-1]:
                return -1
            stack.pop()
            if not stack:
                return i
        i += 1
    return -1


def split_args(m, open_idx, close_idx):
    """Spans (start, end) of the top-level arguments between the parentheses."""
    spans = []
    depth = 0
    start = open_idx + 1
    for i in range(open_idx + 1, close_idx):
        c = m[i]
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        elif c == "," and depth == 0:
            spans.append((start, i))
            start = i + 1
    if m[start:close_idx].strip() or spans:
        spans.append((start, close_idx))
    return spans


def recv_start(m, dot_idx):
    """Start index of the receiver expression that ends right before the '.' at dot_idx."""
    i = dot_idx - 1
    while i >= 0 and m[i] == " ":
        i -= 1
    end = i
    while i >= 0:
        c = m[i]
        if c in ")]":
            op = "(" if c == ")" else "["
            depth = 0
            while i >= 0:
                if m[i] == c:
                    depth += 1
                elif m[i] == op:
                    depth -= 1
                    if depth == 0:
                        break
                i -= 1
            if i < 0:
                return -1
            i -= 1
        elif c.isalnum() or c in "_.$":
            i -= 1
        else:
            break
    s = i + 1
    return s if s <= end else -1


# ---------------------------------------------------------------------------------------------- the codemod
class Mapper:
    def __init__(self, map_path):
        with open(map_path, encoding="utf-8") as fh:
            self.map = json.load(fh)
        self.skip = Globs(self.map["skip"]["globs"])
        self.keep = Globs(self.map["rukh_keep"]["globs"])
        self.lane_relics = Globs(self.map["lanes"]["relics"])
        self.lane_other = Globs(self.map["lanes"]["other"])
        self.themes = [(glob_re(g), t) for g, t in self.map["themes"]]
        self.default_theme = self.map["default_theme"]
        self.enum_themes = self.map.get("enum_themes", {})
        self.particles = {k: v for k, v in self.map["particles"].items() if k != "comment"}

    def theme(self, rel):
        for r, t in self.themes:
            if r.match(rel):
                return t
        return self.default_theme

    def all_themes(self):
        s = {t for _, t in self.themes}
        s.add(self.default_theme)
        return sorted(s)


def const_name(theme):
    return re.sub(r"[^A-Za-z0-9]", "_", theme).upper()


class FileJob:
    def __init__(self, rel, src, mapper, flags, sibling_names):
        self.rel = rel
        self.src = src
        self.mapper = mapper
        self.flags = flags
        self.theme = mapper.theme(rel)
        self.theme_const = const_name(self.theme)
        self.sibling_names = sibling_names
        self.counts = Counter()
        self.left = []          # (line, reason, text)
        self.keep_rukh = mapper.keep.has(rel)
        self.uses = set()
        self._tmp = set()
        self.names = self._resolve_names()

    # -- names: use the simple name unless it is taken in this file or package
    def _resolve_names(self):
        res = {}
        for n, fq in NAMES.items():
            taken = False
            if re.search(r"\b(?:class|enum|interface|record)\s+%s\b" % n, self.src):
                taken = True
            for m in re.finditer(r"^import\s+(?:static\s+)?([\w.]+)\.%s\s*;" % n, self.src, re.M):
                if m.group(1) + "." + n != fq:
                    taken = True
            if n + ".java" in self.sibling_names and self._package() != FX_PKG:
                taken = True
            res[n] = fq if taken else n
        return res

    def _package(self):
        m = re.search(r"^package\s+([\w.]+)\s*;", self.src, re.M)
        return m.group(1) if m else ""

    def theme_at(self, m, idx):
        """The theme expression for a call at idx: the file's theme, or, in an enum file listed in map.json 'enum_themes', the theme of the enum constant whose line it is on."""
        prefix = self.mapper.enum_themes.get(self.rel)
        if prefix:
            ls = m.rfind("\n", 0, idx) + 1
            em = re.match(r"\s*([A-Z][A-Z0-9_]*)\(", m[ls:idx + 1])
            if em:
                return '%s.of("%s%s")' % (self.nm("ThemeKey"), prefix, em.group(1))
        return "%s.%s" % (self.nm("FxThemes"), self.theme_const)

    def nm(self, simple):
        self._tmp.add(simple)
        return self.names[simple]

    # -- the rewriting passes
    def run(self):
        for _ in range(12):
            before = self.src
            self.src = self._pass(self.src)
            if self.src == before:
                break
        if self.counts:
            self._fix_imports()
        return self.src

    def _line_of(self, src, idx):
        return src.count("\n", 0, idx) + 1

    def _pass(self, src):
        m = mask(src)
        edits = []
        covered = []

        def free(a, b):
            return all(b <= x or a >= y for x, y in covered)

        def add(a, b, text, kind):
            ok = free(a, b)
            if ok:
                edits.append((a, b, text))
                covered.append((a, b))
                self.counts[kind] += 1
                self.uses.update(self._tmp)
            self._tmp.clear()
            return ok

        mp = self.mapper
        class _Ref:                      # the theme constant, naming FxThemes only when a candidate edit is really built
            def __str__(_):
                return "%s.%s" % (self.nm("FxThemes"), self.theme_const)
        theme_ref = _Ref()
        # ---- sendParticles / addParticle / PerfParticles.send with a vanilla or Rukh first argument
        for mt in re.finditer(r"(?P<head>\.\s*sendParticles|\.\s*addParticle)\s*\(", m):
            op = mt.end() - 1
            cl = match_close(m, op)
            if cl < 0:
                continue
            args = split_args(m, op, cl)
            head = re.sub(r"\s", "", mt.group("head"))
            rs = recv_start(m, mt.start())
            if rs < 0:
                continue
            recv_text = src[rs:mt.start()].rstrip()
            call_start = rs
            is_add = head == ".addParticle"
            want = 7 if is_add else 9
            if len(args) != want:
                continue
            first = args[0]
            rest = args[1:]
            ftxt = src[first[0]:first[1]].strip()
            rest_text = ",".join(src[a:b] for a, b in rest)
            fm = re.fullmatch(r"(?:net\.minecraft\.core\.particles\.)?ParticleTypes\.([A-Z_]+)", ftxt)
            if fm and fm.group(1) in mp.particles:
                name = fm.group(1)
                if is_add:
                    new = "%s.add(%s, %s, %s.%s,%s)" % (self.nm("Fx"), recv_text, theme_ref, self.nm("FxOld"), name, rest_text)
                else:
                    new = "%s.spray(%s, %s, %s.%s,%s)" % (self.nm("Fx"), recv_text, theme_ref, self.nm("FxOld"), name, rest_text)
                add(call_start, cl + 1, new, "vanilla_add" if is_add else "vanilla_spray")
                continue
            dm = re.match(r"new\s+(?:net\.minecraft\.core\.particles\.)?DustParticleOptions\s*\(", ftxt)
            if dm:
                raw = src[first[0]:first[1]]
                base = first[0] + (len(raw) - len(raw.lstrip()))
                fo = base + dm.end() - 1
                fc = match_close(m, fo)
                if fc == base + len(ftxt) - 1:
                    dargs = split_args(m, fo, fc)
                    if len(dargs) == 2:
                        col = src[dargs[0][0]:dargs[0][1]].strip()
                        size = src[dargs[1][0]:dargs[1][1]].strip()
                        kind = "addDust" if is_add else "sprayDust"
                        new = "%s.%s(%s, %s, %s, %s,%s)" % (self.nm("Fx"), kind, recv_text, theme_ref, col, size, rest_text)
                        add(call_start, cl + 1, new, "dust_add" if is_add else "dust_spray")
                        continue
            if not self.keep_rukh:
                rm = re.fullmatch(r"(?:%s\.)?RukhParticles\.(light|dark)\(\)" % re.escape(PKG + ".particle.rukh"), ftxt)
                if rm:
                    tone = "%s.%s" % (self.nm("FxTone"), rm.group(1).upper())
                    kind = "add" if is_add else "spray"
                    new = "%s.%s(%s, %s, %s,%s)" % (self.nm("Fx"), kind, recv_text, theme_ref, tone, rest_text)
                    add(call_start, cl + 1, new, "rukh_particle_" + kind)
                    continue
                rm = re.fullmatch(r"(?:%s\.)?RukhParticles\.of\((.+)\)" % re.escape(PKG + ".particle.rukh"), ftxt, re.S)
                if rm:
                    kind = "add" if is_add else "spray"
                    new = "%s.%s(%s, %s, %s,%s)" % (self.nm("Fx"), kind, recv_text, theme_ref, rm.group(1).strip(), rest_text)
                    add(call_start, cl + 1, new, "rukh_particle_" + kind)
                    continue
        # ---- RukhFx / RukhClientFx facade calls
        if not self.keep_rukh:
            for mt in re.finditer(r"(?:\b(?:com\s*\.\s*newuniverse\s*\.\s*nusmp\s*\.\s*(?:client\s*\.\s*)?particle\s*\.\s*rukh\s*\.\s*))?\b(?:RukhFx|RukhClientFx)\s*\.\s*(spray|burst|stream|gather|aura)\s*\(", m):
                op = mt.end() - 1
                cl = match_close(m, op)
                if cl < 0:
                    continue
                args = split_args(m, op, cl)
                if len(args) < 2:
                    continue
                a0 = src[args[0][0]:args[0][1]].strip()
                rest_text = ",".join(src[a:b] for a, b in args[1:])
                new = "%s.%s(%s, %s,%s)" % (self.nm("Fx"), mt.group(1), a0, theme_ref, rest_text)
                add(mt.start(), cl + 1, new, "rukh_" + mt.group(1))
        # ---- comparisons with the Rukh type and Rukh types as values
        if not self.keep_rukh:
            for mt in re.finditer(r"(?P<lhs>[A-Za-z_][\w.]*(?:\([^()]*\))?)\s*==\s*(?:com\s*\.\s*newuniverse\s*\.\s*nusmp\s*\.\s*particle\s*\.\s*rukh\s*\.\s*)?RukhParticles\s*\.\s*(?P<w>dark|light)\(\)", m):
                tone = "%s.%s" % (self.nm("FxTone"), mt.group("w").upper())
                add(mt.start(), mt.end(), "%s.toneOf(%s) == %s" % (self.nm("Fx"), src[mt.start("lhs"):mt.end("lhs")], tone), "rukh_compare")
            for mt in re.finditer(r"(?:com\s*\.\s*newuniverse\s*\.\s*nusmp\s*\.\s*particle\s*\.\s*rukh\s*\.\s*)?\bRukhParticles\s*\.\s*(light|dark)\(\)", m):
                tone = "%s.%s" % (self.nm("FxTone"), mt.group(1).upper())
                add(mt.start(), mt.end(), "%s.options(%s, %s)" % (self.nm("Fx"), self.theme_at(m, mt.start()), tone), "rukh_value")
            for mt in re.finditer(r"(?:com\s*\.\s*newuniverse\s*\.\s*nusmp\s*\.\s*particle\s*\.\s*rukh\s*\.\s*)?\bRukhParticles\s*\.\s*of\s*\(", m):
                op = mt.end() - 1
                cl = match_close(m, op)
                if cl < 0:
                    continue
                add(mt.start(), cl + 1, "%s.options(%s, %s)" % (self.nm("Fx"), theme_ref, src[op + 1:cl].strip()), "rukh_value")
            # ---- the Alignment type
            for mt in re.finditer(r"(?:com\s*\.\s*newuniverse\s*\.\s*nusmp\s*\.\s*particle\s*\.\s*rukh\s*\.\s*)?\bRukhFx\s*\.\s*Alignment\b", m):
                if re.match(r"\s*\.\s*type\s*\(", m[mt.end():mt.end() + 12]):
                    continue
                add(mt.start(), mt.end(), self.nm("FxTone"), "rukh_alignment")
            if re.search(r"^import\s+%s\.particle\.rukh\.RukhFx\.Alignment\s*;" % re.escape(PKG), src, re.M):
                for mt in re.finditer(r"(?<![\w.])Alignment\b(?!\s*;)", m):
                    line_start = m.rfind("\n", 0, mt.start()) + 1
                    if m[line_start:mt.start()].lstrip().startswith("import"):
                        continue
                    add(mt.start(), mt.end(), self.nm("FxTone"), "rukh_alignment")
        # ---- any other ParticleTypes.X used as a value
        for mt in re.finditer(r"(?:net\s*\.\s*minecraft\s*\.\s*core\s*\.\s*particles\s*\.\s*)?\bParticleTypes\s*\.\s*([A-Z_]+)\b(?!\s*\()", m):
            name = mt.group(1)
            line_start = m.rfind("\n", 0, mt.start()) + 1
            line_end = m.find("\n", mt.end())
            line_end = len(m) if line_end < 0 else line_end
            line = m[line_start:line_end]
            if line.lstrip().startswith("import"):
                continue
            if name not in mp.particles:
                continue
            if re.search(r"\bSimpleParticleType\b|\bParticleType<", line):
                continue
            if re.search(r"new\s+\w*ParticleOption\s*\(\s*$", m[line_start:mt.start()]) or re.search(r"ParticleOption\w*\s*\(\s*$", m[max(0, mt.start() - 40):mt.start()]):
                continue
            add(mt.start(), mt.end(), "%s.options(%s, %s.%s)" % (self.nm("Fx"), theme_ref, self.nm("FxOld"), name), "vanilla_value")
        if not edits:
            return src
        edits.sort(key=lambda e: e[0], reverse=True)
        out = src
        for a, b, text in edits:
            out = out[:a] + text + out[b:]
        return out

    # -- imports
    def _fix_imports(self):
        src = self.src
        m = mask(src)
        body = "\n".join(l for l in m.split("\n") if not l.lstrip().startswith("import "))
        drop = []
        for simple, fq in (("RukhFx", PKG + ".particle.rukh.RukhFx"), ("RukhClientFx", PKG + ".client.particle.rukh.RukhClientFx"),
                           ("RukhParticles", PKG + ".particle.rukh.RukhParticles"), ("Alignment", PKG + ".particle.rukh.RukhFx.Alignment"),
                           ("ParticleTypes", "net.minecraft.core.particles.ParticleTypes")):
            if re.search(r"^import\s+%s\s*;" % re.escape(fq), src, re.M):
                if not re.search(r"(?<![\w.])%s\b" % simple, body):
                    drop.append(fq)
        for fq in drop:
            src = re.sub(r"^import\s+%s\s*;[ \t]*\r?\n" % re.escape(fq), "", src, flags=re.M)
        need = []
        for simple in sorted(self.uses):
            if self.names[simple] == simple:
                fq = NAMES[simple]
                if not re.search(r"^import\s+%s\s*;" % re.escape(fq), src, re.M) and self._package() != FX_PKG:
                    need.append("import %s;" % fq)
        if need:
            imps = list(re.finditer(r"^import\s+[^;]+;[ \t]*\r?$", src, re.M))
            if imps:
                at = imps[-1].end()
                src = src[:at] + "\n" + "\n".join(need) + src[at:]
            else:
                pm = re.search(r"^package\s+[\w.]+\s*;[ \t]*\r?$", src, re.M)
                at = pm.end() if pm else 0
                src = src[:at] + "\n\n" + "\n".join(need) + src[at:]
        self.src = src

    # -- what is left
    def leftovers(self):
        m = mask(self.src)
        res = []
        for mt in re.finditer(r"\bParticleTypes\s*\.\s*([A-Z_]+)\b", m):
            line_start = m.rfind("\n", 0, mt.start()) + 1
            line_end = m.find("\n", mt.end())
            line = self.src[line_start:line_end if line_end >= 0 else len(self.src)].strip()
            if line.startswith("import"):
                continue
            name = mt.group(1)
            if name in ("BLOCK", "ITEM", "FALLING_DUST", "BLOCK_MARKER", "DUST", "DUST_COLOR_TRANSITION", "ENTITY_EFFECT", "BLOCK_CRUMBLE_DUST", "DUST_PILLAR"):
                reason = "carries data (block state / item / colour)"
            elif name not in self.mapper.particles:
                reason = "no mapping for ParticleTypes.%s" % name
            else:
                reason = "typed as SimpleParticleType / not a value the facade can replace"
            res.append((self._line_of(self.src, mt.start()), reason, line[:140]))
        for mt in re.finditer(r"new\s+(?:net\.minecraft\.core\.particles\.)?DustParticleOptions\s*\(", m):
            line_start = m.rfind("\n", 0, mt.start()) + 1
            line_end = m.find("\n", mt.end())
            line = self.src[line_start:line_end if line_end >= 0 else len(self.src)].strip()
            res.append((self._line_of(self.src, mt.start()), "dust options held in a variable, field or helper (the declared type would change)", line[:140]))
        if not self.keep_rukh:
            for mt in re.finditer(r"\b(RukhFx|RukhClientFx|RukhParticles)\b", m):
                line_start = m.rfind("\n", 0, mt.start()) + 1
                line_end = m.find("\n", mt.end())
                line = self.src[line_start:line_end if line_end >= 0 else len(self.src)].strip()
                if line.startswith("import"):
                    continue
                res.append((self._line_of(self.src, mt.start()), "Rukh use the codemod cannot map (handled by hand)", line[:140]))
        return res


def gen_fx_old(mapper):
    lines = []
    for name, v in mapper.particles.items():
        slot = v.get("slot", "MOTE")
        fixed = '"%s"' % v["fixed"] if "fixed" in v else "null"
        rgb = int(v["rgb"], 16) if "rgb" in v else -1
        themed = "true" if v.get("themed") else "false"
        lines.append("    %s(FxSlot.%s, %s, %d, %s)" % (name, slot, fixed, rgb, themed))
    cases = "\n".join("            case %s -> ParticleTypes.%s;" % (n, n) for n in mapper.particles)
    return '''package com.newuniverse.nusmp.particle.fx;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * GENERATED by tools/particles/codemod.py from tools/particles/map.json ("particles"): do not edit by hand, edit the map and run the codemod again.
 * Why: the vanilla particles the mod used to spawn, each with the theme slot (or the fixed custom particle) that replaces it and an optional dye colour. The codemod writes
 * {@code FxOld.X} into the rewritten calls instead of {@code ParticleTypes.X}, so the call keeps the information about the original: its replacement, and the vanilla particle {@link Fx}
 * falls back to when {@code particles.custom} is off.
 */
public enum FxOld {
%s;

    /** The theme slot that replaces this particle (used when {@link #fixed} is null). */
    public final FxSlot slot;
    /** A custom particle id that replaces it whatever the theme (an elemental one: a flame stays a flame), or null. */
    public final String fixed;
    /** Dye colour (0xRRGGBB) for a tintable replacement, or -1. */
    public final int rgb;
    /** A fixed replacement takes the theme colour (shapes: impact, flash, slash). */
    public final boolean themed;

    FxOld(FxSlot slot, String fixed, int rgb, boolean themed) {
        this.slot = slot;
        this.fixed = fixed;
        this.rgb = rgb;
        this.themed = themed;
    }

    /** The vanilla particle this entry stands for. */
    public ParticleOptions vanilla() {
        return switch (this) {
%s
        };
    }
}
''' % (",\n".join(lines), cases)


def gen_fx_themes(mapper):
    consts = "\n".join('    public static final ThemeKey %s = ThemeKey.of("%s");' % (const_name(t), t) for t in mapper.all_themes())
    return '''package com.newuniverse.nusmp.particle.fx;

/**
 * GENERATED by tools/particles/codemod.py from tools/particles/map.json ("themes"): do not edit by hand.
 * Why: one {@link ThemeKey} constant per theme the rewritten call sites name, so a call does not look a string up in a table: it reads a static field.
 */
public final class FxThemes {
    private FxThemes() {}

%s
}
''' % consts


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--root", default=os.path.join(REPO, "src", "main", "java", "com", "newuniverse", "nusmp"))
    ap.add_argument("--map", default=os.path.join(HERE, "map.json"))
    ap.add_argument("--include-relics", action="store_true", help="also process the relic waves, the client relic FX and the other relic helpers (other builders' lane)")
    ap.add_argument("--include-lanes", action="store_true", help="also process the other lanes of map.json (the rift pockets)")
    ap.add_argument("--check", action="store_true", help="write nothing; exit 1 if a run would change something")
    ap.add_argument("--report", help="write the report as json")
    ap.add_argument("--only", help="only the files whose path matches this glob")
    ap.add_argument("--diff", action="store_true", help="print a unified diff of every file that would change")
    ap.add_argument("--gen-only", action="store_true", help="only (re)write FxThemes.java and FxOld.java")
    ap.add_argument("--no-gen", action="store_true", help="do not write FxThemes.java / FxOld.java")
    a = ap.parse_args()

    mp = Mapper(a.map)
    only = glob_re(a.only) if a.only else None
    files = []
    for dp, _, fns in os.walk(a.root):
        for fn in fns:
            if fn.endswith(".java"):
                p = os.path.join(dp, fn)
                files.append((os.path.relpath(p, a.root).replace(os.sep, "/"), p))
    files.sort()
    siblings = defaultdict(set)
    for rel, _ in files:
        siblings[os.path.dirname(rel)].add(os.path.basename(rel))

    changed = []
    counts = Counter()
    left = {}
    skipped = Counter()
    by_pkg = defaultdict(list)
    scanned = 0
    for rel, p in files:
        if a.gen_only:
            break
        if only and not only.match(rel):
            continue
        if mp.skip.has(rel):
            skipped["skip list (Time Magic, particle libraries, mod entry)"] += 1
            continue
        is_relic_lane = mp.lane_relics.has(rel) and not mp.keep.has(rel)
        is_other_lane = mp.lane_other.has(rel)
        with open(p, encoding="utf-8", newline="") as fh:
            src = fh.read()
        if not re.search(r"ParticleTypes\.|Rukh(?:Fx|ClientFx|Particles)|DustParticleOptions", src):
            continue
        scanned += 1
        if is_relic_lane and not a.include_relics:
            skipped["lane: relics (other builders; --include-relics)"] += 1
            left[rel] = [(0, "lane: relics (other builders; run with --include-relics after they merge)", "")]
            continue
        if is_other_lane and not a.include_lanes:
            skipped["lane: rift pockets (other builders; --include-lanes)"] += 1
            left[rel] = [(0, "lane: rift pockets (other builders; run with --include-lanes after they merge)", "")]
            continue
        crlf = "\r\n" in src
        work = src.replace("\r\n", "\n") if crlf else src
        job = FileJob(rel, work, mp, a, siblings[os.path.dirname(rel)])
        new = job.run()
        if new != work:
            if a.diff:
                import difflib
                sys.stdout.writelines(difflib.unified_diff(work.splitlines(True), new.splitlines(True), "a/" + rel, "b/" + rel, n=0))
            changed.append(rel)
            counts.update(job.counts)
            by_pkg[rel.split("/")[0] if "/" in rel else "(root)"].append(rel)
            if not a.check:
                with open(p, "w", encoding="utf-8", newline="") as fh:
                    fh.write(new.replace("\n", "\r\n") if crlf else new)
        lo = job.leftovers()
        if lo:
            left[rel] = lo

    if not a.no_gen and not a.only:
        gen_dir = os.path.join(a.root, FX_DIR_REL)
        for name, text in (("FxOld.java", gen_fx_old(mp)), ("FxThemes.java", gen_fx_themes(mp))):
            path = os.path.join(gen_dir, name)
            old = open(path, encoding="utf-8", newline="").read() if os.path.exists(path) else None
            if old != text:
                changed.append(FX_DIR_REL + "/" + name)
                if not a.check:
                    with open(path, "w", encoding="utf-8", newline="") as fh:
                        fh.write(text)

    # ---- report
    total_sites = sum(counts.values())
    print("files with a particle call scanned: %d" % scanned)
    print("files changed: %d   call sites replaced: %d" % (len(changed), total_sites))
    for k, v in sorted(counts.items()):
        print("   %-24s %d" % (k, v))
    print("changed files by top package:")
    for pk, fl in sorted(by_pkg.items()):
        print("   %-14s %3d  %s" % (pk, len(fl), ""))
    leftover_total = sum(len(v) for v in left.values())
    reasons = Counter(r for v in left.values() for _, r, _ in v)
    print("call sites left: %d in %d files" % (leftover_total, len(left)))
    for r, c in reasons.most_common():
        print("   %4d  %s" % (c, r))
    for k, v in skipped.items():
        print("skipped files: %d  %s" % (v, k))
    if a.report:
        with open(a.report, "w", encoding="utf-8") as fh:
            json.dump({"changed": changed, "by_package": by_pkg, "replaced": dict(counts), "left": {k: [list(x) for x in v] for k, v in left.items()},
                       "skipped": dict(skipped)}, fh, indent=1)
    if a.check and changed:
        print("pending changes:", len(changed))
        sys.exit(1)


if __name__ == "__main__":
    main()
