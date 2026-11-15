#!/usr/bin/env python3
"""Rename the mod id inside binary NBT structure templates (3.84.1): every string / key that names the old namespace
(`nus` + `mp:`) becomes `anime_multiverse:`. NBT strings carry a length, so the files are parsed and written again, not text-replaced.
Usage: python3 tools/rename_modid_nbt.py [--dry]   (from the repo root)"""
import gzip, io, os, re, struct, sys
OLD, NEW = "nus" + "mp", "anime_multiverse"
PAT = re.compile(r"(?<![A-Za-z0-9_])" + OLD + r"(?=:)")
DRY = "--dry" in sys.argv
hits = 0

def fix(s):
    global hits
    t, k = PAT.subn(NEW, s)
    hits += k
    return t

def rd(b, o, fmt):
    v = struct.unpack_from(">" + fmt, b, o); return v[0], o + struct.calcsize(">" + fmt)

def rstr(b, o):
    n, o = rd(b, o, "H"); return b[o:o + n].decode("utf-8"), o + n   # modified UTF-8 == UTF-8 for the ASCII ids used here

def wstr(s):
    e = s.encode("utf-8"); return struct.pack(">H", len(e)) + e

def parse(b, o, t):
    if t in (1, 2, 3, 4, 5, 6):
        f = {1: "b", 2: "h", 3: "i", 4: "q", 5: "f", 6: "d"}[t]; n = struct.calcsize(">" + f)
        return ("raw", b[o:o + n]), o + n
    if t == 7:
        n, p = rd(b, o, "i"); return ("raw", b[o:p + n]), p + n
    if t == 8:
        s, o = rstr(b, o); return ("str", s), o
    if t == 9:
        et, o = rd(b, o, "B"); n, o = rd(b, o, "i"); items = []
        for _ in range(n):
            v, o = parse(b, o, et); items.append(v)
        return ("list", et, items), o
    if t == 10:
        ents = []
        while True:
            tt, o = rd(b, o, "B")
            if tt == 0: break
            k, o = rstr(b, o); v, o = parse(b, o, tt); ents.append((tt, k, v))
        return ("cmp", ents), o
    if t == 11:
        n, p = rd(b, o, "i"); return ("raw", b[o:p + 4 * n]), p + 4 * n
    if t == 12:
        n, p = rd(b, o, "i"); return ("raw", b[o:p + 8 * n]), p + 8 * n
    raise ValueError("tag " + str(t))

def write(v, out):
    k = v[0]
    if k == "raw": out.write(v[1])
    elif k == "str": out.write(wstr(fix(v[1])))
    elif k == "list":
        out.write(struct.pack(">Bi", v[1], len(v[2])))
        for x in v[2]: write(x, out)
    else:
        for tt, key, x in v[1]:
            out.write(struct.pack(">B", tt)); out.write(wstr(fix(key))); write(x, out)
        out.write(b"\x00")

files = 0
for d, ds, fs in os.walk("src"):
    for f in fs:
        if not f.endswith(".nbt"): continue
        p = os.path.join(d, f)
        raw = open(p, "rb").read()
        try: b = gzip.decompress(raw); zipped = True
        except OSError: b = raw; zipped = False
        if (OLD + ":").encode() not in b: continue
        t, o = rd(b, 0, "B"); name, o = rstr(b, o); v, o = parse(b, o, t)
        assert o == len(b), p
        out = io.BytesIO(); out.write(struct.pack(">B", t)); out.write(wstr(fix(name))); write(v, out)
        files += 1
        if not DRY:
            data = gzip.compress(out.getvalue(), 9, mtime=0) if zipped else out.getvalue()
            open(p, "wb").write(data)
print(("would change " if DRY else "changed ") + str(hits) + " names in " + str(files) + " files")
