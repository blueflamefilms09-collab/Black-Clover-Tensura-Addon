#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
stubgen.py - turns the javap API dump (.github/api/api.tar.gz) into compilable Java stub sources,
compiles them ONCE with the real javac (pruning every member/class whose signature cannot be resolved)
and keeps the result as a cache (stubs/, stubs.jar, pruned.txt, meta.json) that typecheck.py reuses.

Library use:   ensure_cache(...)  -> Cache object (paths)       (what typecheck.py calls)
Command line:  python3 -B tools/typecheck/stubgen.py [--rebuild] [--cache DIR] [--show-pruned] [-v]

The cache lives in $TYPECHECK_CACHE (default <repo>/build/typecheck) and is keyed by a hash of the dump, this
generator, the hand-written extras and the helper-jar list, so it is rebuilt only when one of them changes.
It is built under a lock (mkdir based, works on Linux and Windows) and published with an atomic rename, so many
agents can run the checker at the same time.
"""
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tarfile
import tempfile
import time
import urllib.error
import urllib.request
import zipfile
from collections import defaultdict

GEN_VERSION = "5"
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
API_TAR = os.path.join(ROOT, ".github", "api", "api.tar.gz")
EXTRAS_DIR = os.path.join(HERE, "extras")
MAVEN = "https://repo.maven.apache.org/maven2/"

# (file name, maven path, sha256)  -- real jars for everything the Minecraft API signatures mention that the dump
# does not describe. Downloaded once into <cache>/libs.
LIBS = [
    ("guava-33.1.0-jre.jar", "com/google/guava/guava/33.1.0-jre/guava-33.1.0-jre.jar", None),
    ("failureaccess-1.0.2.jar", "com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar", None),
    ("fastutil-8.5.12.jar", "it/unimi/dsi/fastutil/8.5.12/fastutil-8.5.12.jar", None),
    ("netty-buffer-4.1.97.Final.jar", "io/netty/netty-buffer/4.1.97.Final/netty-buffer-4.1.97.Final.jar", None),
    ("netty-common-4.1.97.Final.jar", "io/netty/netty-common/4.1.97.Final/netty-common-4.1.97.Final.jar", None),
    ("gson-2.10.1.jar", "com/google/code/gson/gson/2.10.1/gson-2.10.1.jar", None),
    ("slf4j-api-2.0.9.jar", "org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar", None),
    ("log4j-api-2.22.1.jar", "org/apache/logging/log4j/log4j-api/2.22.1/log4j-api-2.22.1.jar", None),
    ("commons-lang3-3.14.0.jar", "org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar", None),
    ("commons-io-2.15.1.jar", "commons-io/commons-io/2.15.1/commons-io-2.15.1.jar", None),
    ("jopt-simple-5.0.4.jar", "net/sf/jopt-simple/jopt-simple/5.0.4/jopt-simple-5.0.4.jar", None),
    ("annotations-24.1.0.jar", "org/jetbrains/annotations/24.1.0/annotations-24.1.0.jar", None),
    ("jsr305-3.0.2.jar", "com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar", None),
    ("lwjgl-3.3.3.jar", "org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3.jar", None),
    ("lwjgl-glfw-3.3.3.jar", "org/lwjgl/lwjgl-glfw/3.3.3/lwjgl-glfw-3.3.3.jar", None),
    ("mixinextras-common-0.4.1.jar", "io/github/llamalad7/mixinextras-common/0.4.1/mixinextras-common-0.4.1.jar", None),
]
TEST_LIBS = [
    ("junit-jupiter-api-5.10.2.jar", "org/junit/jupiter/junit-jupiter-api/5.10.2/junit-jupiter-api-5.10.2.jar", None),
    ("junit-jupiter-params-5.10.2.jar", "org/junit/jupiter/junit-jupiter-params/5.10.2/junit-jupiter-params-5.10.2.jar", None),
    ("junit-platform-commons-1.10.2.jar", "org/junit/platform/junit-platform-commons/1.10.2/junit-platform-commons-1.10.2.jar", None),
    ("opentest4j-1.3.0.jar", "org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar", None),
    ("apiguardian-api-1.1.2.jar", "org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar", None),
]
JOML_NAME = "joml-1.10.5.jar"
JOML_MAVEN = "org/joml/joml/1.10.5/joml-1.10.5.jar"
JOML_LOCAL = os.path.join(ROOT, "tools", "vfx_preview", "lib", JOML_NAME)

JDK_PREFIXES = ("java.", "javax.", "jdk.", "sun.", "com.sun.", "org.w3c.", "org.xml.", "org.ietf.")


def log(msg, verbose=True):
    if verbose:
        print(msg, file=sys.stderr, flush=True)


# ====================================================================== small text helpers

def take_balanced(s, i, open_c="<", close_c=">"):
    """s[i] == open_c; returns the index just after the matching close_c."""
    depth = 0
    for j in range(i, len(s)):
        c = s[j]
        if c == open_c:
            depth += 1
        elif c == close_c:
            depth -= 1
            if depth == 0:
                return j + 1
    raise ValueError("unbalanced: " + s)


def split_top(s, sep=","):
    out, depth, cur = [], 0, []
    for c in s:
        if c == "<":
            depth += 1
        elif c == ">":
            depth -= 1
        if c == sep and depth == 0:
            out.append("".join(cur).strip())
            cur = []
        else:
            cur.append(c)
    last = "".join(cur).strip()
    if last:
        out.append(last)
    return [x for x in out if x]


def strip_generics(t):
    """java.util.List<a.B<c>>[] -> java.util.List[]"""
    out, depth = [], 0
    for c in t:
        if c == "<":
            depth += 1
        elif c == ">":
            depth -= 1
        elif depth == 0:
            out.append(c)
    return "".join(out)


_INNER_RE = re.compile(r">\.(?=[A-Za-z_])")


def fix_inner(s):
    """javap prints an inner class of a generic class as  Outer<T>.Inner ; stubs make every nested class static, so
    that becomes the plain nested name  Outer$Inner  (outer type arguments are dropped)."""
    while True:
        m = _INNER_RE.search(s)
        if not m:
            return s
        j = m.start()
        depth, i = 0, j
        while i >= 0:
            c = s[i]
            if c == ">":
                depth += 1
            elif c == "<":
                depth -= 1
                if depth == 0:
                    break
            i -= 1
        s = s[:i] + "$" + s[m.end():]


_TOKEN_RE = re.compile(r"[A-Za-z_][\w$]*(?:\.[A-Za-z_][\w$]*)+")


def class_tokens(decl):
    """all fully qualified class names mentioned in a (normalised) javap declaration fragment"""
    return _TOKEN_RE.findall(decl)


def src_type(t):
    """javap type text -> Java source type text"""
    return fix_inner(t).replace("$", ".")


def outer_of(raw):
    i = raw.rfind("$")
    return raw[:i] if i > 0 else None


def simple_of(raw):
    i = raw.rfind("$")
    if i > 0:
        return raw[i + 1:]
    return raw.rsplit(".", 1)[-1]


def pkg_of(raw):
    top = raw.split("$", 1)[0]
    return top.rsplit(".", 1)[0] if "." in top else ""


def is_synthetic_name(raw):
    """anonymous / local / lambda style class names (Foo$1, Foo$1Local, Foo$$Lambda)"""
    return bool(re.search(r"\$(\d|\$)", raw))


# ====================================================================== javap model

MEMBER_MODS = {"public", "protected", "private", "static", "final", "abstract", "native", "synchronized", "default",
               "strictfp", "transient", "volatile"}
CLASS_MODS = {"public", "protected", "private", "static", "final", "abstract", "sealed", "non-sealed", "strictfp"}
PRIM_ZERO = {"boolean": "false", "byte": "(byte) 0", "short": "(short) 0", "char": "'\\0'", "int": "0", "long": "0L",
             "float": "0f", "double": "0d"}
PRIM_NONCONST = {
    "boolean": "java.lang.Boolean.valueOf(false)", "byte": "java.lang.Byte.valueOf((byte) 0)",
    "short": "java.lang.Short.valueOf((short) 0)", "char": "java.lang.Character.valueOf('\\0')",
    "int": "java.lang.Integer.valueOf(0)", "long": "java.lang.Long.valueOf(0L)",
    "float": "java.lang.Float.valueOf(0f)", "double": "java.lang.Double.valueOf(0d)"}


class Member:
    __slots__ = ("kind", "mods", "tparams", "ret", "name", "params", "varargs", "throws", "const", "raw", "tokens", "idx")

    def __init__(self):
        self.const = None
        self.tparams = ""
        self.params = []
        self.varargs = False
        self.throws = []
        self.ret = ""


class ClassInfo:
    __slots__ = ("raw", "mods", "kind", "tparams", "ext", "impls", "members", "header_tokens", "children", "external")

    def __init__(self):
        self.members = []
        self.children = []
        self.impls = []
        self.ext = None
        self.tparams = ""
        self.external = False

    @property
    def simple(self):
        return simple_of(self.raw)

    @property
    def pkg(self):
        return pkg_of(self.raw)


def parse_member(line):
    s = line.strip()
    if not s.endswith(";"):
        return None
    s = s[:-1].rstrip()
    if s.startswith("static {"):
        return None
    n = len(s)
    depth, paren, eq = 0, -1, -1
    for i in range(n):
        c = s[i]
        if c == "<":
            depth += 1
        elif c == ">":
            depth -= 1
        elif depth == 0:
            if c == "(":
                paren = i
                break
            if c == "=" and i > 0 and s[i - 1] == " ":
                eq = i
                break
    m = Member()
    m.raw = s
    left = s[:paren] if paren >= 0 else (s[:eq] if eq >= 0 else s)
    left = left.strip()
    mods = []
    while True:
        mm = re.match(r"([A-Za-z]+)\s+", left)
        if mm and mm.group(1) in MEMBER_MODS:
            mods.append(mm.group(1))
            left = left[mm.end():]
        else:
            break
    m.mods = set(mods)
    if left.startswith("<"):
        j = take_balanced(left, 0)
        m.tparams = left[:j]
        left = left[j:].lstrip()
    if paren >= 0:
        rest = s[paren:]
        end = take_balanced(rest, 0, "(", ")")
        pstr = rest[1:end - 1]
        tail = rest[end:].strip()
        m.params = split_top(pstr)
        if m.params and m.params[-1].endswith("..."):
            m.varargs = True
        if tail.startswith("throws "):
            m.throws = split_top(tail[7:])
        parts = left.rsplit(None, 1)
        if len(parts) == 1:
            m.kind = "ctor"
            m.name = parts[0]
            m.ret = ""
        else:
            m.kind = "method"
            m.ret, m.name = parts[0].strip(), parts[1]
    else:
        parts = left.rsplit(None, 1)
        if len(parts) != 2:
            return None
        m.kind = "field"
        m.ret, m.name = parts[0].strip(), parts[1]
        if eq >= 0:
            m.const = s[eq + 1:].strip()
    decl = s[:paren] if paren >= 0 else (s[:eq] if eq >= 0 else s)
    if paren >= 0:
        decl = s  # parameters + throws are part of the declaration
    m.tokens = set(class_tokens(fix_inner(decl)))
    return m


def parse_header(line):
    s = line.strip()
    assert s.endswith("{"), line
    s = s[:-1].strip()
    toks = s.split(" ")
    i, mods = 0, []
    while toks[i] in CLASS_MODS:
        mods.append(toks[i])
        i += 1
    kind = toks[i]
    rest = " ".join(toks[i + 1:])
    mm = re.match(r"[\w.$]+", rest)
    c = ClassInfo()
    c.raw = mm.group(0)
    rest = rest[mm.end():]
    c.mods = set(mods)
    c.kind = kind  # class | interface | enum | record | @interface
    if rest.startswith("<"):
        j = take_balanced(rest, 0)
        c.tparams = rest[:j]
        rest = rest[j:]
    rest = rest.strip()
    # clauses at depth 0
    clauses, depth, cur, key = {}, 0, [], None
    words = []
    buf = []
    for ch in rest + " ":
        if ch == "<":
            depth += 1
        elif ch == ">":
            depth -= 1
        if ch == " " and depth == 0:
            if buf:
                words.append("".join(buf))
                buf = []
        else:
            buf.append(ch)
    cur_key, cur_val = None, []
    for w in words:
        if w in ("extends", "implements", "permits") and depth == 0:
            if cur_key:
                clauses[cur_key] = " ".join(cur_val)
            cur_key, cur_val = w, []
        else:
            cur_val.append(w)
    if cur_key:
        clauses[cur_key] = " ".join(cur_val)
    if kind == "interface":
        c.ext = None
        c.impls = split_top(clauses.get("extends", ""))
    else:
        c.ext = clauses.get("extends")
        c.impls = split_top(clauses.get("implements", ""))
    if c.ext:
        c.ext = fix_inner(c.ext)
    c.impls = [fix_inner(x) for x in c.impls]
    c.tparams = fix_inner(c.tparams)
    # refine kind
    if kind == "interface" and c.impls == ["java.lang.annotation.Annotation"]:
        c.kind = "annotation"
        c.impls = []
    elif kind == "class" and c.ext and re.fullmatch(r"java\.lang\.Enum<" + re.escape(c.raw) + r">", c.ext):
        c.kind = "enum"
        c.ext = None
    elif kind == "class" and c.ext == "java.lang.Record":
        c.kind = "record"
        c.ext = None
    ht = set()
    ht.update(class_tokens(c.tparams))
    c.header_tokens = ht
    return c


def parse_blocks(text):
    """yields ClassInfo for every class block of a javap listing"""
    cur = None
    for line in text.splitlines():
        if not line.strip():
            continue
        if line.startswith("Compiled from"):
            continue
        if line.startswith("Picked up") or line.startswith("Error:") or line.startswith("Warning:"):
            continue
        if not line.startswith(" ") and line.rstrip().endswith("{"):
            try:
                cur = parse_header(line)
            except Exception as e:  # noqa
                cur = None
                print("stubgen: unparsable header: %s (%s)" % (line, e), file=sys.stderr)
            continue
        if line.startswith("}"):
            if cur is not None:
                yield cur
            cur = None
            continue
        if cur is not None and line.startswith("  "):
            try:
                m = parse_member(line)
            except Exception as e:  # noqa
                print("stubgen: unparsable member: %s (%s)" % (line, e), file=sys.stderr)
                m = None
            if m is not None:
                m.idx = len(cur.members)
                cur.members.append(m)


def parse_tparams(tp):
    """'<T, U extends a.B<T> & c.D>' -> {'T': None, 'U': 'a.B<T>'}  (first bound only)"""
    out = {}
    if not tp:
        return out
    for part in split_top(tp[1:-1]):
        mm = re.match(r"([\w$]+)(?:\s+extends\s+(.*))?$", part)
        if mm:
            b = mm.group(2)
            if b:
                b = split_top(b, "&")[0]
            out[mm.group(1)] = b
    return out


def erase(t, tvars):
    """erasure of a javap type, tvars: name -> bound text or None"""
    t = strip_generics(fix_inner(t)).strip()
    arr = ""
    if t.endswith("..."):
        t = t[:-3]
        arr += "[]"
    while t.endswith("[]"):
        arr += "[]"
        t = t[:-2].strip()
    seen = 0
    while t in tvars and seen < 8:
        b = tvars[t]
        t = strip_generics(fix_inner(b)).strip() if b else "java.lang.Object"
        seen += 1
    return t + arr


# ====================================================================== universe

class Universe:
    def __init__(self, dump, external):
        self.dump = dump          # raw name -> ClassInfo
        self.external = external  # raw name -> ClassInfo (javap of JDK / jars)

    def get(self, raw):
        return self.dump.get(raw) or self.external.get(raw)

    def exists(self, raw):
        return raw in self.dump or raw in self.external


def jar_class_names(jars):
    names = set()
    for j in jars:
        try:
            with zipfile.ZipFile(j) as z:
                for n in z.namelist():
                    if n.endswith(".class") and not n.startswith("META-INF/") and not n.endswith("module-info.class") \
                            and not n.endswith("package-info.class"):
                        names.add(n[:-6].replace("/", "."))
        except Exception:
            pass
    return names


def run(cmd, **kw):
    return subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace", **kw)


def javap_load(names, classpath, verbose=False):
    found = {}
    names = sorted(names)
    for i in range(0, len(names), 120):
        batch = names[i:i + 120]
        p = run(["javap", "-protected", "-constants", "-cp", classpath] + batch)
        for c in parse_blocks(p.stdout):
            c.external = True
            found[c.raw] = c
    return found


# ====================================================================== constants / initialisers

def java_const(ftype, v):
    """javap constant text -> Java literal, or None when it cannot be parsed"""
    v = v.strip()
    try:
        if ftype == "java.lang.String":
            return v if (len(v) >= 2 and v[0] == '"' and v[-1] == '"') else None
        if ftype == "boolean":
            return v if v in ("true", "false") else None
        if ftype == "char":
            if len(v) >= 3 and v[0] == "'" and v[-1] == "'":
                return v
            return "(char) %d" % int(v)
        if ftype in ("int", "short", "byte"):
            n = int(v)
            return str(n) if ftype == "int" else "(%s) %d" % (ftype, n)
        if ftype == "long":
            return str(int(v.rstrip("lL"))) + "L"
        if ftype in ("float", "double"):
            sfx = "f" if ftype == "float" else "d"
            body = v[:-1] if v[-1:] in ("f", "d", "F", "D") else v
            if body in ("NaN",):
                return "(0.0%s / 0.0%s)" % (sfx, sfx)
            if body in ("Infinity", "+Infinity"):
                return "(1.0%s / 0.0%s)" % (sfx, sfx)
            if body == "-Infinity":
                return "(-1.0%s / 0.0%s)" % (sfx, sfx)
            float(body)
            return body + sfx
    except ValueError:
        return None
    return None


# ====================================================================== the generator

class Pruner:
    """what has been removed from the stubs, and why"""

    def __init__(self):
        self.classes = {}   # raw -> reason
        self.members = {}   # (cls raw, idx) -> reason
        self.ifaces = {}    # (cls raw, iface text) -> reason
        self.bridges = 0
        self.abstract_fix = {}  # cls raw -> reason (made abstract)
        self.ctor_only = {}     # cls raw -> True: drop all ctors' super call strategy changes (unused)


class Generator:
    def __init__(self, dump, universe, skip_names):
        self.dump = dump
        self.uni = universe
        self.skip = skip_names  # raw names available as real jars (not stubbed)
        self.pr = Pruner()
        self.index = defaultdict(list)  # class token -> [(kind, cls raw, idx)]
        self.top = [c for c in dump.values() if outer_of(c.raw) is None]
        self.children = defaultdict(list)
        for c in dump.values():
            o = outer_of(c.raw)
            if o is not None and o in dump:
                self.children[o].append(c)
        for lst in self.children.values():
            lst.sort(key=lambda x: x.raw)
        self._build_index()

    # ---------------------------------------------------------------- reference index (for cascade pruning)
    def _build_index(self):
        for c in self.dump.values():
            for t in c.header_tokens:
                self.index[t].append(("ht", c.raw, -1))
            if c.ext:
                for t in set(class_tokens(c.ext)):
                    self.index[t].append(("hx", c.raw, -1))
            for k, it in enumerate(c.impls):
                for t in set(class_tokens(it)):
                    self.index[t].append(("hi", c.raw, k))
            for m in c.members:
                for t in m.tokens:
                    self.index[t].append(("m", c.raw, m.idx))

    def prune_class(self, raw, reason):
        work = [(raw, reason)]
        while work:
            r, why = work.pop()
            if r in self.pr.classes:
                continue
            self.pr.classes[r] = why
            for ch in self.children.get(r, []):
                work.append((ch.raw, "enclosing class pruned: " + r))
            for kind, cr, idx in self.index.get(r, []):
                if cr in self.pr.classes:
                    continue
                dep = "uses pruned/missing type " + r
                if kind == "m":
                    self.pr.members.setdefault((cr, idx), dep)
                elif kind == "hi":
                    c = self.dump[cr]
                    self.pr.ifaces.setdefault((cr, c.impls[idx]), dep)
                else:  # hx / ht : superclass or type parameter bound is gone -> the class goes too
                    work.append((cr, "header uses pruned/missing type " + r))

    def prune_member(self, cr, idx, reason):
        if cr not in self.pr.classes:
            self.pr.members.setdefault((cr, idx), reason)

    # ---------------------------------------------------------------- initial (static) pruning
    def static_prune(self, verbose=True):
        missing = defaultdict(int)
        for tok in list(self.index.keys()):
            if tok in self.dump and tok not in self.skip:
                continue
            if tok in self.skip:
                continue
            if self.uni.exists(tok):
                continue
            missing[tok] += 1
        for tok in sorted(missing):
            self.prune_class(tok, "type not available (not in dump, helper jars or JDK)")
        # members/classes mentioning anonymous or local classes
        for c in self.dump.values():
            if is_synthetic_name(c.raw):
                self.prune_class(c.raw, "anonymous / local class")
        for tok in list(self.index.keys()):
            if is_synthetic_name(tok):
                self.prune_class(tok, "anonymous / local class referenced")
        for c in list(self.dump.values()):
            o = outer_of(c.raw)
            if o is not None and o not in self.dump and not is_synthetic_name(c.raw):
                self.prune_class(c.raw, "enclosing class %s is not in the dump" % o)
        return missing

    # ---------------------------------------------------------------- emitting
    def is_pruned_member(self, c, m):
        return (c.raw, m.idx) in self.pr.members

    def class_ctors(self, c):
        return [m for m in c.members if m.kind == "ctor" and not self.is_pruned_member(c, m)] \
            if not c.external else [m for m in c.members if m.kind == "ctor"]

    def super_call(self, c):
        """the explicit super(...) statement for a stub constructor of class c ('' if none is needed)"""
        if c.kind in ("interface", "annotation", "enum") or not c.ext:
            return ""
        sup_raw = strip_generics(c.ext).strip()
        if sup_raw in ("java.lang.Object", "java.lang.Record"):
            return ""
        sc = self.uni.get(sup_raw)
        if sc is None:
            return "super();"
        if sc.kind in ("interface", "annotation", "enum"):
            return ""
        cands = []
        same_pkg = sc.pkg == c.pkg
        ctors = self.class_ctors(sc)
        had_ctor = any(m.kind == "ctor" for m in sc.members)
        for m in ctors:
            if "private" in m.mods:
                continue
            if "public" in m.mods or "protected" in m.mods:
                cands.append(m)
        if not cands:
            if not had_ctor and same_pkg:
                return "super();"
            if not had_ctor:
                return "super();"
            return "super();"
        tv = parse_tparams(sc.tparams)

        def rank(m):
            mtv = parse_tparams(m.tparams)
            has_tv = any(strip_generics(p).replace("...", "").replace("[]", "").strip() in tv or
                         strip_generics(p).replace("...", "").replace("[]", "").strip() in mtv for p in m.params)
            return (1 if m.throws else 0, 1 if has_tv else 0, len(m.params))
        cands.sort(key=rank)
        m = cands[0]
        mtv = dict(tv)
        mtv.update(parse_tparams(m.tparams))
        args = []
        for p in m.params:
            q = p
            arr = False
            if q.endswith("..."):
                q = q[:-3]
                arr = True
            pl = strip_generics(q).strip()
            base = pl
            dims = 0
            while base.endswith("[]"):
                base = base[:-2].strip()
                dims += 1
            if arr:
                dims += 1
            if dims == 0 and base in PRIM_ZERO:
                args.append(PRIM_ZERO[base])
            elif base in mtv:
                args.append("null")
            else:
                e = erase(p, mtv)
                args.append("(%s) null" % src_type(e))
        return "super(%s);" % ", ".join(args)

    def default_value(self, t):
        """annotation element default for type text t (None -> element stays mandatory)"""
        tt = fix_inner(t).strip()
        if tt.endswith("[]"):
            return "{}"
        if tt in PRIM_ZERO:
            return {"boolean": "false", "char": "'\\0'"}.get(tt, "0")
        if tt == "java.lang.String":
            return '""'
        if tt.startswith("java.lang.Class"):
            mm = re.match(r"java\.lang\.Class<\?\s+extends\s+([\w.$]+)(?:<.*>)?>$", tt)
            if mm and self.uni.exists(mm.group(1)):
                return src_type(mm.group(1)) + ".class"
            if tt == "java.lang.Class<?>" or tt == "java.lang.Class":
                return "java.lang.Object.class"
            return None
        e = self.uni.get(tt)
        if e is not None and e.kind == "enum":
            consts = self.enum_constants(e)
            if consts:
                return src_type(tt) + "." + consts[0]
        return None

    def enum_constants(self, c):
        out = []
        for m in c.members:
            if m.kind != "field":
                if out:
                    break
                continue
            if "static" in m.mods and "final" in m.mods and m.ret == c.raw and not (c.raw, m.idx) in self.pr.members:
                out.append(m.name)
            elif out:
                break
        return out

    def emit_file(self, top):
        """-> (text, linemap) ; linemap[i] = key of line i+1"""
        lines = []
        lm = []
        pkg = top.pkg
        if pkg:
            lines.append("package %s;" % pkg)
            lm.append(None)
        lines.append("")
        lm.append(None)
        self.emit_class(top, 0, lines, lm)
        return "\n".join(lines) + "\n", lm

    def emit_class(self, c, depth, lines, lm):
        if c.raw in self.pr.classes:
            return
        ind = "    " * depth
        nested = outer_of(c.raw) is not None
        kind = c.kind
        mods = []
        if "public" in c.mods:
            mods.append("public")
        if kind in ("class", "record"):
            if nested:
                mods.append("static")
            if "abstract" in c.mods or c.raw in self.pr.abstract_fix:
                mods.append("abstract")
            if "final" in c.mods and "abstract" not in c.mods and c.raw not in self.pr.abstract_fix:
                mods.append("final")
        kw = {"class": "class", "record": "class", "interface": "interface", "annotation": "@interface",
              "enum": "enum"}[kind]
        head = " ".join(mods + [kw, c.simple])
        if kind in ("class", "record", "interface", "enum", "annotation") and c.tparams and kind not in ("enum", "annotation"):
            head += src_type(c.tparams)
        impls = [i for i in c.impls if (c.raw, i) not in self.pr.ifaces]
        if kind in ("class", "record"):
            if c.ext and strip_generics(c.ext).strip() not in ("java.lang.Object", "java.lang.Record"):
                head += " extends " + src_type(c.ext)
            if impls:
                head += " implements " + ", ".join(src_type(i) for i in impls)
        elif kind == "interface":
            if impls:
                head += " extends " + ", ".join(src_type(i) for i in impls)
        elif kind == "enum":
            if impls:
                head += " implements " + ", ".join(src_type(i) for i in impls)
        lines.append(ind + head + " {")
        lm.append(("c", c.raw))
        ind2 = ind + "    "
        tvars = parse_tparams(c.tparams)
        members = [m for m in c.members if not self.is_pruned_member(c, m)]
        consts = []
        if kind == "enum":
            consts = self.enum_constants(c)
            const_set = set(consts)
            if consts:
                lines.append(ind2 + ", ".join(consts) + ";")
                lm.append(("m", c.raw, [m.idx for m in c.members if m.kind == "field" and m.name in const_set][0]))
            else:
                lines.append(ind2 + ";")
                lm.append(None)
        else:
            const_set = set()
        seen = set()
        has_ctor = False
        for m in members:
            if m.kind == "field":
                if kind == "enum" and m.name in const_set and "static" in m.mods:
                    continue
                if m.name.startswith("$") or m.name.startswith("this$") or m.name.startswith("val$"):
                    continue
                lines.append(ind2 + self.emit_field(c, m))
                lm.append(("m", c.raw, m.idx))
            elif m.kind == "ctor":
                if kind in ("enum", "interface", "annotation"):
                    continue
                has_ctor = True
                lines.append(ind2 + self.emit_ctor(c, m))
                lm.append(("m", c.raw, m.idx))
            else:
                nm = m.name
                if nm.startswith("lambda$") or nm.startswith("access$") or nm.startswith("$") or nm == "$values":
                    continue
                if kind == "enum" and ((nm == "values" and not m.params) or
                                       (nm == "valueOf" and len(m.params) == 1 and m.params[0] == "java.lang.String")):
                    continue
                mt = dict(tvars)
                mt.update(parse_tparams(m.tparams))
                key = (nm, tuple(erase(p, mt) for p in m.params))
                if key in seen:
                    self.pr.bridges += 1
                    continue
                seen.add(key)
                lines.append(ind2 + self.emit_method(c, m))
                lm.append(("m", c.raw, m.idx))
        if kind in ("class", "record") and not has_ctor:
            # every constructor of the real class is private / package-private: keep it that way
            lines.append(ind2 + "%s() { %s }" % (c.simple, self.super_call(c)))
            lm.append(("c", c.raw))
        for ch in self.children.get(c.raw, []):
            self.emit_class(ch, depth + 1, lines, lm)
        lines.append(ind + "}")
        lm.append(None)

    def emit_field(self, c, m):
        mods = []
        if "public" in m.mods or c.kind in ("interface", "annotation"):
            mods.append("public")
        elif "protected" in m.mods:
            mods.append("protected")
        static = "static" in m.mods or c.kind in ("interface", "annotation")
        final = "final" in m.mods or c.kind in ("interface", "annotation")
        if static:
            mods.append("static")
        if final:
            mods.append("final")
        t = fix_inner(m.ret)
        init = None
        if m.const is not None:
            init = java_const(t, m.const)
        if init is None and final:
            init = PRIM_NONCONST.get(t, "null")
        s = " ".join(mods + [src_type(t), m.name])
        if init is not None:
            s += " = " + init
        return s + ";"

    def emit_ctor(self, c, m):
        mods = []
        if "public" in m.mods:
            mods.append("public")
        elif "protected" in m.mods:
            mods.append("protected")
        tp = (src_type(m.tparams) + " ") if m.tparams else ""
        params = self.param_list(m)
        s = " ".join(mods + [tp + c.simple]) if mods else tp + c.simple
        s += "(" + params + ")"
        if m.throws:
            s += " throws " + ", ".join(src_type(x) for x in m.throws)
        s += " { " + self.super_call(c) + " }"
        return s

    def param_list(self, m):
        out = []
        for i, p in enumerate(m.params):
            out.append("%s arg%d" % (src_type(p), i))
        return ", ".join(out)

    def emit_method(self, c, m):
        iface = c.kind == "interface"
        annot = c.kind == "annotation"
        mods = []
        if "public" in m.mods or iface or annot:
            mods.append("public")
        elif "protected" in m.mods:
            mods.append("protected")
        static = "static" in m.mods
        abstract = "abstract" in m.mods
        if c.kind == "enum":
            abstract = False
        native = "native" in m.mods
        default = "default" in m.mods
        if annot:
            d = self.default_value(m.ret)
            s = "%s %s()" % (src_type(m.ret), m.name)
            if d is not None:
                s += " default " + d
            return "public " + s + ";"
        if static:
            mods.append("static")
        if iface:
            if default:
                mods.append("default")
            body = static or default
        else:
            if "final" in m.mods:
                mods.append("final")
            if abstract:
                mods.append("abstract")
            if native:
                mods.append("native")
            if "synchronized" in m.mods:
                mods.append("synchronized")
            body = not (abstract or native)
        tp = (src_type(m.tparams) + " ") if m.tparams else ""
        s = " ".join(mods + [tp + src_type(m.ret), m.name]) + "(" + self.param_list(m) + ")"
        if m.throws:
            s += " throws " + ", ".join(src_type(x) for x in m.throws)
        if body:
            s += " { throw new java.lang.UnsupportedOperationException(); }"
        else:
            s += ";"
        return s

    def emit_all(self, outdir):
        """writes all stub files; returns {abs file path: linemap}"""
        maps = {}
        for top in self.top:
            if top.raw in self.pr.classes or top.raw in self.skip:
                continue
            text, lm = self.emit_file(top)
            rel = top.raw.replace(".", "/") + ".java"
            path = os.path.join(outdir, rel)
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", encoding="utf-8", newline="\n") as f:
                f.write(text)
            maps[os.path.abspath(path)] = lm
        return maps


# ====================================================================== javac driver

ERR_RE = re.compile(r"^(?P<file>.+?\.java):(?P<line>\d+): (?P<kind>error|warning): (?P<msg>.*)$")


def javac_cmd():
    return "javac"


def javac_major():
    p = run(["javac", "-version"])
    m = re.search(r"javac (\d+)", p.stdout + p.stderr)
    return int(m.group(1)) if m else 0


def write_argfile(path, items):
    with open(path, "w", encoding="utf-8") as f:
        for it in items:
            f.write('"' + it.replace("\\", "\\\\").replace('"', '\\"') + '"\n')


def parse_javac(output):
    """-> list of dict(file, line, kind, msg, detail lines)"""
    errs, cur = [], None
    for ln in output.splitlines():
        m = ERR_RE.match(ln)
        if m:
            cur = {"file": m.group("file"), "line": int(m.group("line")), "kind": m.group("kind"),
                   "msg": m.group("msg"), "detail": []}
            errs.append(cur)
        elif ln.startswith(("Picked up", "Note:")) or re.match(r"^\d+ (errors?|warnings?)$", ln):
            cur = None
        elif cur is not None:
            cur["detail"].append(ln)
    return errs


def compile_stubs(files, classpath, outdir, workdir, extra_flags=()):
    argfile = os.path.join(workdir, "files.args")
    write_argfile(argfile, files)
    cmd = ["javac", "-proc:none", "-nowarn", "-Xlint:none", "-XDshould-stop.ifError=FLOW", "-Xmaxerrs", "1000000",
           "-Xmaxwarns", "0", "-encoding", "UTF-8", "-implicit:none", "-g:none", "-d", outdir,
           "-cp", classpath, "-J-Xss16m"]
    if javac_major() >= 21:
        cmd += ["--release", "21"]
    cmd += list(extra_flags)
    cmd += ["@" + argfile]
    p = run(cmd)
    return p.returncode, p.stdout + "\n" + p.stderr


# ====================================================================== cache management

class DirLock:
    """mkdir based lock: portable, with stale detection (dead pid or no heartbeat for a long time)"""

    def __init__(self, path, stale=1500):
        self.path = path
        self.stale = stale
        self.held = False

    def _owner_file(self):
        return os.path.join(self.path, "owner")

    def _stale(self):
        try:
            with open(self._owner_file(), "r") as f:
                pid = int((f.read().split() or ["0"])[0])
        except Exception:
            pid = 0
        try:
            age = time.time() - os.path.getmtime(self._owner_file())
        except OSError:
            try:
                age = time.time() - os.path.getmtime(self.path)
            except OSError:
                return False
        if os.name == "posix" and pid > 0:
            try:
                os.kill(pid, 0)
            except ProcessLookupError:
                return True
            except Exception:
                pass
        return age > self.stale

    def acquire(self, timeout=3600, announce=None):
        t0 = time.time()
        told = False
        while True:
            try:
                os.mkdir(self.path)
                with open(self._owner_file(), "w") as f:
                    f.write("%d %s\n" % (os.getpid(), time.strftime("%H:%M:%S")))
                self.held = True
                return
            except FileExistsError:
                if self._stale():
                    shutil.rmtree(self.path, ignore_errors=True)
                    continue
                if announce and not told:
                    announce()
                    told = True
                if time.time() - t0 > timeout:
                    raise TimeoutError("could not get the typecheck cache lock " + self.path)
                time.sleep(0.5)

    def heartbeat(self):
        try:
            os.utime(self._owner_file(), None)
        except OSError:
            pass

    def release(self):
        if self.held:
            shutil.rmtree(self.path, ignore_errors=True)
            self.held = False


def sha256_file(p):
    h = hashlib.sha256()
    with open(p, "rb") as f:
        for b in iter(lambda: f.read(1 << 20), b""):
            h.update(b)
    return h.hexdigest()


def download(url, dest, verbose=True):
    last = None
    for attempt in range(7):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "nusmp-typecheck/1"})
            with urllib.request.urlopen(req, timeout=180) as r, open(dest + ".part", "wb") as f:
                shutil.copyfileobj(r, f)
            if not zipfile.is_zipfile(dest + ".part"):
                raise IOError("not a zip/jar")
            os.replace(dest + ".part", dest)
            return
        except Exception as e:  # noqa
            last = e
            code = getattr(e, "code", None)
            if code == 404:
                break
            time.sleep(min(60, 4 * (attempt + 1)))
    # last resort: curl (honours the same proxy environment)
    if shutil.which("curl"):
        p = run(["curl", "-sS", "-L", "-m", "300", "-o", dest + ".part", url])
        if p.returncode == 0 and os.path.exists(dest + ".part") and zipfile.is_zipfile(dest + ".part"):
            os.replace(dest + ".part", dest)
            return
    raise RuntimeError("could not download %s (%s)" % (url, last))


def ensure_libs(cache_root, with_tests=False, verbose=True):
    libdir = os.path.join(cache_root, "libs")
    os.makedirs(libdir, exist_ok=True)
    wanted = list(LIBS) + (list(TEST_LIBS) if with_tests else [])
    paths = []
    for name, mpath, _sha in wanted:
        dest = os.path.join(libdir, name)
        if not (os.path.exists(dest) and zipfile.is_zipfile(dest)):
            log("typecheck: downloading %s" % name, verbose)
            download(MAVEN + mpath, dest, verbose)
        paths.append(dest)
    joml = JOML_LOCAL
    if not os.path.exists(joml):
        joml = os.path.join(libdir, JOML_NAME)
        if not (os.path.exists(joml) and zipfile.is_zipfile(joml)):
            log("typecheck: downloading %s" % JOML_NAME, verbose)
            download(MAVEN + JOML_MAVEN, joml, verbose)
    paths.append(joml)
    return paths


def extras_files():
    out = []
    if os.path.isdir(EXTRAS_DIR):
        for dp, _dn, fns in os.walk(EXTRAS_DIR):
            for fn in sorted(fns):
                if fn.endswith(".java"):
                    out.append(os.path.join(dp, fn))
    return sorted(out)


def compute_key():
    h = hashlib.sha256()
    h.update(("gen" + GEN_VERSION).encode())
    for p in [API_TAR, os.path.abspath(__file__)] + extras_files():
        h.update(os.path.relpath(p, ROOT).encode())
        with open(p, "rb") as f:
            h.update(f.read())
    for n, m, _ in LIBS:
        h.update(n.encode())
    return h.hexdigest()[:16]


class Cache:
    def __init__(self, base, key):
        self.base = base
        self.key = key
        self.dir = os.path.join(base, key)
        self.stubs = os.path.join(self.dir, "stubs")
        self.jar = os.path.join(self.dir, "stubs.jar")
        self.pruned = os.path.join(self.dir, "pruned.txt")
        self.meta_path = os.path.join(self.dir, "meta.json")
        self.libs = []
        self.meta = {}

    def load_meta(self):
        with open(self.meta_path, "r", encoding="utf-8") as f:
            self.meta = json.load(f)


def default_cache_base():
    return os.environ.get("TYPECHECK_CACHE") or os.path.join(ROOT, "build", "typecheck")


def ensure_cache(base=None, rebuild=False, verbose=True, with_tests=False):
    base = os.path.abspath(base or default_cache_base())
    os.makedirs(base, exist_ok=True)
    key = compute_key()
    cache = Cache(base, key)
    if not rebuild and os.path.exists(cache.meta_path) and os.path.exists(cache.jar):
        cache.load_meta()
        cache.libs = ensure_libs_locked(base, with_tests, verbose)
        return cache
    lock = DirLock(os.path.join(base, ".lock"))
    lock.acquire(announce=lambda: log("typecheck: another process is building the stub cache, waiting ...", verbose))
    try:
        if not rebuild and os.path.exists(cache.meta_path) and os.path.exists(cache.jar):
            cache.load_meta()
            cache.libs = ensure_libs(base, with_tests, verbose)
            return cache
        cache.libs = ensure_libs(base, with_tests, verbose)
        build_cache(cache, lock, verbose)
        cache.load_meta()
        cleanup_old(base, key)
        return cache
    finally:
        lock.release()


def ensure_libs_locked(base, with_tests, verbose):
    """libs may be missing although the stub set exists (tests flag): fetch under the lock"""
    libdir = os.path.join(base, "libs")
    names = [n for n, _, _ in LIBS] + ([n for n, _, _ in TEST_LIBS] if with_tests else [])
    if all(os.path.exists(os.path.join(libdir, n)) for n in names) and \
            (os.path.exists(JOML_LOCAL) or os.path.exists(os.path.join(libdir, JOML_NAME))):
        return ensure_libs(base, with_tests, False)
    lock = DirLock(os.path.join(base, ".lock"))
    lock.acquire()
    try:
        return ensure_libs(base, with_tests, verbose)
    finally:
        lock.release()


def cleanup_old(base, keep):
    try:
        for n in os.listdir(base):
            p = os.path.join(base, n)
            if n in (keep, "libs", ".lock") or not os.path.isdir(p):
                continue
            if n.startswith(".tmp-") or re.fullmatch(r"[0-9a-f]{16}", n):
                try:
                    if time.time() - os.path.getmtime(p) > 6 * 3600:
                        shutil.rmtree(p, ignore_errors=True)
                except OSError:
                    pass
    except OSError:
        pass


def load_dump(verbose=True):
    """extract api.tar.gz in memory and parse every class block"""
    classes = {}
    with tarfile.open(API_TAR, "r:gz") as tar:
        for ti in tar.getmembers():
            if not ti.isfile():
                continue
            text = tar.extractfile(ti).read().decode("utf-8", errors="replace")
            for c in parse_blocks(text):
                classes[c.raw] = c
    return classes


def build_cache(cache, lock, verbose=True):
    t0 = time.time()
    tmp = os.path.join(cache.base, ".tmp-%s-%d" % (cache.key, os.getpid()))
    shutil.rmtree(tmp, ignore_errors=True)
    os.makedirs(tmp)
    stubs_dir = os.path.join(tmp, "stubs")
    work = os.path.join(tmp, "work")
    os.makedirs(work)
    log("typecheck: building the stub cache (one time, a few minutes) ...", verbose)
    dump = load_dump(verbose)
    log("typecheck: %d classes in the API dump" % len(dump), verbose)
    jar_names = jar_class_names(cache.libs)
    classpath = os.pathsep.join(cache.libs)
    skip = set()
    for raw in list(dump):
        top = raw.split("$", 1)[0]
        if top in jar_names:
            skip.add(raw)
    # external classes: every referenced class that is not in the dump (JDK, helper jars)
    refs = set()
    for c in dump.values():
        refs.update(c.header_tokens)
        if c.ext:
            refs.update(class_tokens(c.ext))
        for i in c.impls:
            refs.update(class_tokens(i))
        for m in c.members:
            refs.update(m.tokens)
    ext_names = {r for r in refs if r not in dump or r in skip}
    ext_names = {r for r in ext_names if not is_synthetic_name(r)}
    extern = javap_load(ext_names, classpath, verbose)
    log("typecheck: %d external types resolved (JDK + helper jars), %d unresolved"
        % (len(extern), len([r for r in ext_names if r not in extern])), verbose)
    uni = Universe({k: v for k, v in dump.items() if k not in skip}, extern)
    gen = Generator({k: v for k, v in dump.items() if k not in skip}, uni, set())
    missing = gen.static_prune(verbose)
    log("typecheck: %d unavailable types pruned up front (%d classes / %d members dependent)"
        % (len(missing), len(gen.pr.classes), len(gen.pr.members)), verbose)
    # hand written extras (the dump wins when it has the same class)
    extra_list = []
    extras_out = os.path.join(tmp, "extras")
    for p in extras_files():
        rel = os.path.relpath(p, EXTRAS_DIR)
        fq = rel[:-5].replace(os.sep, ".")
        if fq in dump:
            continue
        dest = os.path.join(extras_out, rel)
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        shutil.copyfile(p, dest)
        extra_list.append(os.path.abspath(dest))
    rounds = 0
    classes_out = os.path.join(work, "classes")
    while True:
        rounds += 1
        lock.heartbeat()
        shutil.rmtree(stubs_dir, ignore_errors=True)
        os.makedirs(stubs_dir)
        shutil.rmtree(classes_out, ignore_errors=True)
        os.makedirs(classes_out)
        maps = gen.emit_all(stubs_dir)
        files = sorted(maps) + extra_list
        log("typecheck: round %d: compiling %d stub files ..." % (rounds, len(files)), verbose)
        rc, out = compile_stubs(files, classpath, classes_out, work)
        errs = [e for e in parse_javac(out) if e["kind"] == "error"]
        if rc == 0 and not errs:
            break
        if rounds > 40:
            raise RuntimeError("stub pruning does not converge:\n" + out[-3000:])
        progressed = classify_errors(gen, maps, errs, extra_list, verbose)
        log("typecheck:   %d errors -> pruned: %d classes, %d members, %d interfaces so far"
            % (len(errs), len(gen.pr.classes), len(gen.pr.members), len(gen.pr.ifaces)), verbose)
        if not progressed:
            dbg = os.path.join(cache.base, "last-stub-errors.txt")
            with open(dbg, "w", encoding="utf-8") as f:
                f.write(out)
            raise RuntimeError("stub errors that cannot be pruned (see %s):\n%s" % (dbg, out[:3000]))
    # pack stubs.jar
    jar_tmp = os.path.join(tmp, "stubs.jar")
    with zipfile.ZipFile(jar_tmp, "w", zipfile.ZIP_DEFLATED) as z:
        for dp, _dn, fns in os.walk(classes_out):
            for fn in fns:
                full = os.path.join(dp, fn)
                z.write(full, os.path.relpath(full, classes_out).replace(os.sep, "/"))
    write_pruned(gen, os.path.join(tmp, "pruned.txt"), dump, missing)
    n_stub_classes = sum(1 for c in gen.dump.values() if c.raw not in gen.pr.classes)
    meta = {"key": cache.key, "gen_version": GEN_VERSION, "rounds": rounds,
            "dump_classes": len(dump), "stub_classes": n_stub_classes,
            "stub_files": len(maps), "extras": len(extra_list),
            "pruned_classes": len([1 for r in gen.pr.classes if r in dump]), "pruned_members": len([1 for k in gen.pr.members if k[0] not in gen.pr.classes]),
            "dropped_interfaces": len(gen.pr.ifaces), "bridge_duplicates": gen.pr.bridges,
            "total_members": sum(len(c.members) for c in gen.dump.values()),
            "build_seconds": round(time.time() - t0, 1),
            "libs": [os.path.basename(p) for p in cache.libs],
            "api_sha256": sha256_file(API_TAR)}
    with open(os.path.join(tmp, "meta.json"), "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=1)
    shutil.rmtree(work, ignore_errors=True)
    shutil.rmtree(extras_out, ignore_errors=True)
    if os.path.exists(cache.dir):
        shutil.rmtree(cache.dir, ignore_errors=True)
    os.replace(tmp, cache.dir)
    log("typecheck: stub cache ready in %.0fs: %s" % (time.time() - t0, cache.dir), verbose)


def classify_errors(gen, maps, errs, extra_list, verbose):
    """turns javac errors inside stub files into prunes; returns True if anything new was pruned"""
    progressed = False
    extra_set = {os.path.abspath(p) for p in extra_list}
    unmapped = []
    for e in errs:
        f = os.path.abspath(e["file"])
        if f in extra_set:
            raise RuntimeError("error in a hand written extras stub: %s:%d: %s" % (f, e["line"], e["msg"]))
        lm = maps.get(f)
        if lm is None or e["line"] - 1 >= len(lm) or lm[e["line"] - 1] is None:
            unmapped.append(e)
            continue
        key = lm[e["line"] - 1]
        reason = "javac: " + e["msg"]
        sym = ""
        for d in e["detail"]:
            if d.strip().startswith("symbol:"):
                sym = d.strip()
                break
        if sym:
            reason += " [" + sym + "]"
        if key[0] == "m":
            _, cr, idx = key
            if cr in gen.pr.classes or (cr, idx) in gen.pr.members:
                continue
            gen.pr.members[(cr, idx)] = reason
            progressed = True
        else:  # class header (or synthesized constructor)
            cr = key[1]
            if cr in gen.pr.classes:
                continue
            if "is not abstract and does not override abstract method" in e["msg"] and gen.dump[cr].kind in ("class", "record") \
                    and cr not in gen.pr.abstract_fix:
                gen.pr.abstract_fix[cr] = reason
                progressed = True
                continue
            gen.prune_class(cr, reason)
            progressed = True
    if unmapped and verbose:
        for e in unmapped[:5]:
            log("typecheck:   (unmapped stub error) %s:%d: %s" % (e["file"], e["line"], e["msg"]), verbose)
    return progressed


def write_pruned(gen, path, dump, missing):
    lines = []
    pr = gen.pr
    cls_reasons = defaultdict(list)
    for r, why in sorted(pr.classes.items()):
        cls_reasons[why.split(" [")[0] if why.startswith("javac") else why].append(r)
    lines.append("# pruned.txt - what the stub generator had to leave out of the API dump (see tools/typecheck/README.md)")
    total_members = sum(len(c.members) for c in gen.dump.values())
    lines.append("# classes in dump: %d   classes pruned: %d   members in dump: %d   members pruned: %d   "
                 "interfaces dropped from implements: %d   bridge duplicates skipped: %d   made abstract: %d"
                 % (len(dump), len([1 for r in pr.classes if r in dump]), total_members, len(pr.members),
                    len(pr.ifaces), pr.bridges, len(pr.abstract_fix)))
    lines.append("")
    lines.append("## Types referenced by the API but not available (neither in the dump, a helper jar nor the JDK)")
    for tok in sorted(missing):
        lines.append("MISSING-TYPE %s  (%d references)" % (tok, len(gen.index.get(tok, []))))
    lines.append("")
    lines.append("## Classes left out")
    for r in sorted(pr.classes):
        if r in dump:
            lines.append("PRUNE-CLASS %s  # %s" % (r, pr.classes[r].replace("\n", " ")))
    lines.append("")
    lines.append("## Classes made abstract (the real class is concrete but a stub method could not be generated)")
    for r in sorted(pr.abstract_fix):
        lines.append("ABSTRACT %s  # %s" % (r, pr.abstract_fix[r]))
    lines.append("")
    lines.append("## Interfaces dropped from an implements/extends list")
    for (cr, it), why in sorted(pr.ifaces.items()):
        if cr not in pr.classes:
            lines.append("DROP-IMPLEMENTS %s -> %s  # %s" % (cr, it, why))
    lines.append("")
    lines.append("## Members left out (the javap declaration as printed in the dump)")
    for (cr, idx), why in sorted(pr.members.items()):
        if cr in pr.classes:
            continue
        m = gen.dump[cr].members[idx]
        lines.append("PRUNE-MEMBER %s :: %s  # %s" % (cr, m.raw, why.replace("\n", " ")))
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")


# ====================================================================== command line

def main(argv=None):
    ap = argparse.ArgumentParser(description="Build (or show) the stub cache used by typecheck.py")
    ap.add_argument("--cache", help="cache folder (default $TYPECHECK_CACHE or <repo>/build/typecheck)")
    ap.add_argument("--rebuild", action="store_true", help="rebuild even if the cache is current")
    ap.add_argument("--tests", action="store_true", help="also fetch the JUnit jars")
    ap.add_argument("--show-pruned", action="store_true", help="print the summary of pruned.txt")
    ap.add_argument("-q", "--quiet", action="store_true")
    a = ap.parse_args(argv)
    cache = ensure_cache(a.cache, rebuild=a.rebuild, verbose=not a.quiet, with_tests=a.tests)
    print("stub cache: %s" % cache.dir)
    print(json.dumps(cache.meta, indent=1))
    if a.show_pruned:
        with open(cache.pruned, encoding="utf-8") as f:
            for i, ln in enumerate(f):
                if i < 6 or ln.startswith("MISSING-TYPE") or ln.startswith("PRUNE-CLASS"):
                    sys.stdout.write(ln)
    return 0


if __name__ == "__main__":
    sys.exit(main())
