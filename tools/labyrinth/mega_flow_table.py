#!/usr/bin/env python3
"""
Draws the magic-alignment tables of the Rukh flow (docs/labyrinth/mega_flow.md) as two PNG pictures the owner can look at:

    python3 tools/labyrinth/mega_flow_table.py --out DIR [--tensura tensura-ids.md]

    flow_matrix.png    what a cast does on every road (Floor 0, the Holy road, the Abyssal road as shipped, the Abyssal road mirrored) with the numbers
    flow_families.png  every one of the mod's magic families by alignment, the pages whose name overrules their family, and examples of Tensura skills

Why a script and not a hand-made picture: the table has to be the TRUTH. It reads the same three files the game reads (the shipped
data/nusmp/mega/flow/alignments.json, the real MagicType.java and the numbers in FlowRules.java), applies the same rules as the Java class MagicAlignments
(an exact entry beats a keyword, whole words only, dark beats light, the first id with an entry decides alone) and scans the page ids of the real sources, so
a re-run after the owner edits the JSON or adds a family shows the new truth. The Python port of the rules is cross-checked against the Java class by the
build's own test (AlignmentDataTest pins the same results), and `--selfcheck` re-checks a few known ids here without Pillow.
Needs Pillow only for drawing; no game, no network.
"""
import argparse
import glob
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
SRC = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp")
DATA = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp", "mega", "flow", "alignments.json")

# ----------------------------------------------------------------------------------------------------------------------------- the rules (a port of MagicAlignments)
LIGHT, DARK, NEUTRAL = "light", "dark", "neutral"
RANK = {DARK: 2, LIGHT: 1, NEUTRAL: 0}


def normalize(s):
    return (s or "").strip().lower().replace(" ", "_").replace("-", "_")


def path_of(i):
    return i.split(":", 1)[1] if ":" in i else i


def tokens(i):
    return re.findall(r"[a-z0-9]+", normalize(i))


class Table:
    def __init__(self, doc):
        self.light = {normalize(w) for w in doc.get("light", [])}
        self.dark = {normalize(w) for w in doc.get("dark", [])}
        self.exact = {}
        for k, v in doc.get("exact", {}).items():
            v = v.strip().lower()
            self.exact[normalize(k)] = NEUTRAL if v == "none" else v

    def one(self, raw, allow_merged=True):
        """(alignment, explicit, rule) for one id, like MagicAlignments.verdictOf."""
        i = normalize(raw)
        if not i:
            return (NEUTRAL, False, "no rule names it")
        if allow_merged and i.startswith("merged:"):
            best = None
            for part in i[len("merged:"):].split("+"):
                if not part.strip():
                    continue
                v = self.one(part, False)
                best = v if best is None else self.stronger(best, v)
            return (NEUTRAL, False, "no rule names it") if best is None else (best[0], best[1], "merged magic: " + best[2])
        key = i
        e = self.exact.get(key)
        if e is None:
            key = path_of(i)
            e = None if key == i else self.exact.get(key)
        if e is not None:
            return (e, True, "exact entry '%s' is %s" % (key, e))
        words = tokens(path_of(i))
        d = next((w for w in words if w in self.dark), None)
        l = next((w for w in words if w in self.light), None)
        if d and l:
            return (DARK, False, "keywords '%s' (light) and '%s' (dark) both match, and dark wins" % (l, d))
        if d:
            return (DARK, False, "keyword '%s' (dark)" % d)
        if l:
            return (LIGHT, False, "keyword '%s' (light)" % l)
        return (NEUTRAL, False, "no rule names it")

    @staticmethod
    def stronger(a, b):
        if RANK[b[0]] > RANK[a[0]]:
            return b
        if RANK[b[0]] == RANK[a[0]] and b[1] and not a[1]:
            return b
        return a

    def classify(self, *ids):
        best = None
        for raw in ids:
            v = self.one(raw)
            if v[1]:
                return v[0]
            best = v if best is None else self.stronger(best, v)
        return best[0] if best else NEUTRAL


# ----------------------------------------------------------------------------------------------------------------------------- reading the repository
def read(p):
    with open(p, encoding="utf-8", errors="replace") as f:
        return f.read()


def families():
    """[(NAME, display, soul, particle)] in file order, from MagicType.java."""
    out = []
    for m in re.finditer(r'^\s+([A-Z][A-Z0-9_]*)\("([^"]+)", "([^"]+)", "([A-Z]+)",[^\n]*?RukhParticles\.(light|dark)\(\)', read(os.path.join(SRC, "blackclover", "MagicType.java")), re.M):
        out.append((m.group(1), m.group(2), m.group(4), m.group(5)))
    return out


PAGE = re.compile(r'(?<![A-Za-z0-9_])(?:BookPage\.)?(?:starter|mid|zone|signature|daily)\(\s*"([a-z0-9_]+)"\s*,\s*"([^"]*)"|new BookPage\(\s*"([a-z0-9_]+)"\s*,\s*"([^"]*)"')
FAM = re.compile(r"(?:super\(|new [A-Za-z]*Book\()\s*MagicType\.([A-Z0-9_]+)")


def pages(names):
    """[(family or '', page id, page name)]: the family is the one the page's book file (or the nearest preceding book constructor) names; '' = a universal page."""
    out = []
    for f in sorted(glob.glob(SRC + "/**/*.java", recursive=True)):
        t = read(f)
        if not PAGE.search(t):
            continue
        base = re.sub(r"(Book|Arts|Pages|Summons|Ext)$", "", os.path.basename(f)[:-5])
        guess = re.sub(r"(?<!^)([A-Z])", r"_\1", base).upper()
        cur = guess if guess in names else ""
        found = FAM.findall(t)
        if not cur and len(set(found)) == 1 and found[0] in names:
            cur = found[0]
        for line in t.split("\n"):
            m = FAM.search(line)
            if m and m.group(1) in names:
                cur = m.group(1)
            for pm in PAGE.finditer(line):
                out.append((cur.lower(), pm.group(1) or pm.group(3), pm.group(2) or pm.group(4)))
    return out


def numbers():
    t = read(os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "labyrinth", "mega", "flow", "FlowRules.java"))

    def num(name):
        m = re.search(name + r"\s*=\s*([0-9.]+)", t)
        return float(m.group(1))

    return {"mult": num("DEFAULT_MULTIPLIER"), "multMin": num("MIN_MULTIPLIER"), "multMax": num("MAX_MULTIPLIER"), "pct": num("DEFAULT_REJECT_PERCENT"),
            "minHp": num("DEFAULT_REJECT_MIN_HEALTH"), "cool": int(num("DEFAULT_REJECT_COOLDOWN_TICKS")), "window": int(num("DEFAULT_CAST_WINDOW_TICKS"))}


def fmt(x):
    return ("%g" % x)


# ----------------------------------------------------------------------------------------------------------------------------- drawing
BG, PANEL, PANEL2, INK, MUTE, LINE = (18, 20, 28), (28, 31, 42), (36, 40, 54), (236, 238, 245), (150, 156, 175), (58, 63, 82)
C_LIGHT, C_DARK, C_NEUT, C_BAD, C_GOOD = (255, 216, 110), (178, 132, 255), (140, 170, 200), (255, 98, 98), (118, 214, 140)
FONTS = ["/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"]
BOLDS = ["/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"]


def font(size, bold=False):
    from PIL import ImageFont
    for p in (BOLDS if bold else FONTS):
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def tw(d, s, f):
    return d.textlength(s, font=f)


def wrap(d, s, f, w):
    lines, cur = [], ""
    for word in s.split():
        trial = word if not cur else cur + " " + word
        if tw(d, trial, f) <= w:
            cur = trial
        else:
            if cur:
                lines.append(cur)
            cur = word
    if cur:
        lines.append(cur)
    return lines


def block(d, x, y, s, f, w, fill=INK, gap=4):
    """Draws wrapped text, returns the y below it."""
    h = f.size + gap
    for line in wrap(d, s, f, w):
        d.text((x, y), line, font=f, fill=fill)
        y += h
    return y


def block_height(d, s, f, w, gap=4):
    return len(wrap(d, s, f, w)) * (f.size + gap)


def rrect(d, box, r, fill, outline=None):
    d.rounded_rectangle(box, r, fill=fill, outline=outline, width=2 if outline else 0)


def matrix_image(n, counts, out):
    from PIL import Image, ImageDraw
    W = 2100
    img = Image.new("RGB", (W, 1500), BG)
    d = ImageDraw.Draw(img)
    f_t, f_h, f_b, f_s, f_n = font(40, True), font(25, True), font(21), font(18), font(23, True)
    d.text((48, 34), "The Rukh flow: what a cast does on each road", font=f_t, fill=INK)
    d.text((48, 90), "Numbers are the shipped defaults (config file nusmp-labyrinth-mega-flow-server.toml, section [flow]). A cast is Light, Dark or Neutral by the table in alignments.json.", font=f_s, fill=MUTE)

    cols = [("Cast is", 330), ("Floor 0 (the Hub)", 300), ("HOLY road  +1, +2 ...   flow = LIGHT", 520), ("ABYSSAL road  -1, -2 ...   as shipped: no flow", 470), ("ABYSSAL, if abyssalFlow = \"dark\"", 420)]
    x0, y0 = 48, 140
    heights = {}
    cells = {
        LIGHT: ["LIGHT magic\n%d families" % counts[LIGHT],
                "x1  nothing happens: the Hub has no flow",
                "BOOSTED  x%s damage on every hit you land within %d ticks after the cast. Light Rukh gather on the target." % (fmt(n["mult"]), n["window"]),
                "x1  the Abyss has no flow by default",
                "REJECTED  the place throws your light out, at the same price as a Dark cast on the Holy road: %s%% of maximum health (at least %s HP), once per %d ticks, never below 1 HP." % (fmt(n["pct"]), fmt(n["minHp"]), n["cool"])],
        DARK: ["DARK magic\n%d families" % counts[DARK],
               "x1  nothing happens",
               "REJECTED  you lose %s%% of your maximum health (at least %s HP), at most once per %d ticks, and never below 1 HP. Your Dark Rukh scatter, the place's Light Rukh close in." % (fmt(n["pct"]), fmt(n["minHp"]), n["cool"]),
               "x1  the Abyss has no flow by default",
               "BOOSTED  x%s damage for %d ticks after the cast. Dark Rukh gather on the target." % (fmt(n["mult"]), n["window"])],
        NEUTRAL: ["NEUTRAL magic\n%d families" % counts[NEUTRAL],
                  "x1", "x1  neither helped nor hurt. A neutral cast also ends the %d-tick window of an earlier Light cast." % n["window"], "x1", "x1"],
    }
    colour = {LIGHT: C_LIGHT, DARK: C_DARK, NEUTRAL: C_NEUT}
    # header row
    x = x0
    hh = 0
    for title, w in cols:
        hh = max(hh, block_height(d, title, f_h, w - 28))
    hh += 26
    for title, w in cols:
        rrect(d, (x, y0, x + w - 8, y0 + hh), 10, PANEL2)
        block(d, x + 14, y0 + 13, title, f_h, w - 36)
        x += w
    y = y0 + hh + 8
    for a in (LIGHT, DARK, NEUTRAL):
        row = cells[a]
        rh = 0
        for (title, w), text in zip(cols, row):
            hh2 = 0
            for part in text.split("\n"):
                head, _, rest = part.partition("  ")
                hh2 += (30 + block_height(d, rest, f_b, w - 36)) if head in ("BOOSTED", "REJECTED") and rest else block_height(d, part, f_b, w - 36)
            rh = max(rh, hh2)
        rh += 30
        x = x0
        for ci, ((title, w), text) in enumerate(zip(cols, row)):
            bad = text.startswith("REJECTED")
            good = text.startswith("BOOSTED")
            edge = C_BAD if bad else C_GOOD if good else None
            rrect(d, (x, y, x + w - 8, y + rh), 10, PANEL, edge)
            yy = y + 14
            for k, part in enumerate(text.split("\n")):
                if ci == 0 and k == 0:
                    yy = block(d, x + 14, yy, part, f_h, w - 36, fill=colour[a])
                elif ci == 0:
                    yy = block(d, x + 14, yy, part, f_s, w - 36, fill=MUTE)
                else:
                    head, _, rest = part.partition("  ")
                    if head in ("BOOSTED", "REJECTED") and rest:
                        d.text((x + 14, yy), head, font=f_n, fill=C_GOOD if head == "BOOSTED" else C_BAD)
                        yy = block(d, x + 14, yy + 30, rest, f_b, w - 36)
                    else:
                        yy = block(d, x + 14, yy, part, f_b, w - 36, fill=INK if head != "x1" else MUTE)
            x += w
        y += rh + 8

    # the numbers
    y += 22
    d.text((x0, y), "The numbers behind it", font=f_h, fill=INK)
    y += 44
    items = [
        ("Multiplier", "x%s (config holyMultiplier, allowed range %s to %s). 10 damage becomes %s." % (fmt(n["mult"]), fmt(n["multMin"]), fmt(n["multMax"]), fmt(10 * n["mult"]))),
        ("Window", "%d ticks (1.5 s) after a cast. A hit counts as coming from the cast when it lands inside it; a spell that hurts for longer is boosted only for this long." % n["window"]),
        ("Rejection", "%s%% of maximum health, never less than %s HP (one heart). A 20 HP player loses 2 HP; a 100 HP player loses %s HP; a 1000 HP player loses %s HP." % (fmt(n["pct"]), fmt(n["minHp"]), fmt(100 * n["pct"] / 100), fmt(1000 * n["pct"] / 100))),
        ("Cooldown", "%d ticks (1 s) per player: five Dark spells in a row cost one rejection, not five." % n["cool"]),
        ("Never lethal", "the drain stops at 1 HP (config nonLethal). Creative and spectator players are exempt."),
        ("Absolute", "the price is taken straight from health: armour, the Resistance effect and the Rare grade's magic resistance do not soften it."),
        ("Limits that stay", "a boss with a damage-per-second budget pays the EXTRA damage from that budget; a boosted hit on a player is held under the balance law's one-hit cap."),
        ("Ambient", "every 2 s, 3 Rukh of the flow's colour drift around each player standing in a place that has a flow (config ambient)."),
    ]
    for head, text in items:
        d.text((x0 + 14, y), head, font=f_b, fill=C_LIGHT)
        y2 = block(d, x0 + 250, y, text, f_b, W - x0 * 2 - 270)
        y = max(y + 30, y2) + 6
    img = img.crop((0, 0, W, y + 24))
    img.save(out)
    return img.size


def families_image(fams, table, pg, out, tensura):
    from PIL import Image, ImageDraw
    W = 2100
    # classify
    by = {LIGHT: [], DARK: [], NEUTRAL: []}
    pages_of = {}
    for fam, pid, name in pg:
        pages_of[fam] = pages_of.get(fam, 0) + 1
    for name, disp, soul, particle in fams:
        by[table.classify(name.lower())].append((name, disp, soul, particle))
    f_t, f_h, f_b, f_s = font(40, True), font(25, True), font(20), font(17)
    probe = ImageDraw.Draw(Image.new("RGB", (10, 10)))

    def col_height(items, w, per_line=1):
        return 0

    # estimate height generously, then crop
    img = Image.new("RGB", (W, 3600), BG)
    d = ImageDraw.Draw(img)
    d.text((48, 34), "Every magic family of the mod, by alignment", font=f_t, fill=INK)
    d.text((48, 90), "%d families: %d Light, %d Dark, %d Neutral. Read from MagicType.java and alignments.json. A dot on a Neutral family marks one whose particles are drawn in Dark Rukh." % (len(fams), len(by[LIGHT]), len(by[DARK]), len(by[NEUTRAL])), font=f_s, fill=MUTE)
    y = 150
    # light + dark side by side
    colw = (W - 48 * 2 - 24) // 2
    left_y = right_y = y
    for x, a, title, tcol, note in ((48, LIGHT, "LIGHT  (boosted on the Holy road)", C_LIGHT, "the mod's own LIGHT soul: Light, Lightning, Sword"),
                                    (48 + colw + 24, DARK, "DARK  (rejected on the Holy road)", C_DARK, "the DARKNESS soul (14 families) plus Blood and Anti-Magic")):
        yy = y
        rrect(d, (x, yy, x + colw, yy + 70 + 34 * ((len(by[a]) + 1) // 2) + 16), 12, PANEL)
        d.text((x + 18, yy + 12), title, font=f_h, fill=tcol)
        d.text((x + 18, yy + 46), note, font=f_s, fill=MUTE)
        cy = yy + 80
        half = (len(by[a]) + 1) // 2
        for k, (name, disp, soul, particle) in enumerate(by[a]):
            cx = x + 18 + (k // half) * (colw // 2)
            ry = cy + (k % half) * 34
            d.ellipse((cx, ry + 8, cx + 12, ry + 20), fill=tcol)
            label = disp.replace(" Magic", "")
            d.text((cx + 22, ry + 2), label, font=f_b, fill=INK)
            d.text((cx + 22 + tw(d, label, f_b) + 10, ry + 6), soul.lower(), font=f_s, fill=MUTE)
        bottom = cy + half * 34 + 8
        if a == LIGHT:
            left_y = bottom
        else:
            right_y = bottom
    y = max(left_y, right_y) + 20
    # neutral
    neutral = by[NEUTRAL]
    cols = 4
    rows = (len(neutral) + cols - 1) // cols
    cw = (W - 96) // cols
    rrect(d, (48, y, W - 48, y + 100 + rows * 31 + 10), 12, PANEL)
    d.text((66, y + 12), "NEUTRAL  (x1, nothing happens)", font=f_h, fill=C_NEUT)
    d.text((66, y + 46), "%d families. Deliberately neutral: a test lists every one, so a new family has to be placed on purpose. Dot = draws Dark Rukh particles but is not a dark magic (how it LOOKS, not what it IS)." % len(neutral), font=f_s, fill=MUTE)
    cy = y + 84
    for k, (name, disp, soul, particle) in enumerate(neutral):
        cx = 66 + (k // rows) * cw
        ry = cy + (k % rows) * 31
        if particle == "dark":
            d.ellipse((cx, ry + 8, cx + 11, ry + 19), fill=C_DARK)
        d.text((cx + 20, ry + 2), disp.replace(" Magic", ""), font=f_b, fill=INK)
    y = cy + rows * 31 + 30

    # pages whose name overrules their family
    over_dark, over_light, neutralised = [], [], []
    keywords_only = Table({"light": sorted(table.light), "dark": sorted(table.dark), "exact": {}})
    for fam, pid, pname in pg:
        pa = table.classify(pid, fam) if fam else table.classify(pid)
        fa = table.classify(fam) if fam else NEUTRAL
        if pa != fa:
            (over_dark if pa == DARK else over_light if pa == LIGHT else neutralised).append((pname, fam))
        elif pa == NEUTRAL and keywords_only.classify(pid) != NEUTRAL:
            neutralised.append((pname, fam))
    seen = set()
    uni = [(pn, f) for pn, f in over_dark if not (pn in seen or seen.add(pn))]
    y0 = y
    colw3 = (W - 96 - 48) // 3
    boxes = [
        ("Pages that are DARK although their family is not", C_DARK, uni, "a page entry (Devil Union, Devil Dive, Black Lightning: Battle Fiend) or a dark word in the name"),
        ("Pages that are LIGHT although their family is not", C_LIGHT, [(a, b) for a, b in over_light], "none: no page outside the Light families names light"),
        ("Pages a dark word would catch, kept NEUTRAL by an entry", C_NEUT, [(a, b) for a, b in neutralised], "an exact entry beats a keyword (the Black Bulls are a squad; a ward against curses is a ward)"),
    ]
    bh = 0
    for title, tc, items, note in boxes:
        h = 60 + block_height(d, note, f_s, colw3 - 36) + max(1, len(items)) * 27 + 20
        bh = max(bh, h)
    for k, (title, tc, items, note) in enumerate(boxes):
        x = 48 + k * (colw3 + 24)
        rrect(d, (x, y0, x + colw3, y0 + bh), 12, PANEL)
        yy = block(d, x + 18, y0 + 14, title, f_h, colw3 - 36, fill=tc) + 2
        yy = block(d, x + 18, yy, note, f_s, colw3 - 36, fill=MUTE) + 6
        for pname, fam in items[:40]:
            d.text((x + 18, yy), "%s   (%s)" % (pname, fam.replace("_", " ") if fam else "every grimoire"), font=f_s, fill=INK)
            yy += 27
    y = y0 + bh + 24

    # Tensura examples
    d.text((48, y), "Tensura skills (read when the player activates one that is not a grimoire page)", font=f_h, fill=INK)
    y += 42
    sections = [(LIGHT, C_LIGHT, "LIGHT", ["tensura:light", "tensura:solar_beam", "tensura:solar_flare", "tensura:solar_rain", "tensura:solar_wave", "tensura:sacred_haki", "tensura:light_transform",
                                           "tensura:magic_light_transform", "tensura:divine_ki_release", "tensura:divine_berserker"]),
                (DARK, C_DARK, "DARK", ["tensura:darkness", "tensura:dark_cube", "tensura:darkness_cannon", "tensura:true_darkness", "tensura:shadow_bind", "tensura:shadow_motion", "tensura:curse",
                                        "tensura:curse_bind", "tensura:hellfire", "tensura:black_flame", "tensura:black_lightning", "tensura:demon_lord_haki", "tensura:demon_dominate",
                                        "tensura:blood_mist", "tensura:darkness_transform", "tensura:dark_eight_palms", "tensura:death_march_dance", "tensura:create_greater_undead"]),
                (NEUTRAL, C_NEUT, "NEUTRAL, deliberately", ["tensura:lighten", "tensura:lightning_lance", "tensura:poison", "tensura:healing", "tensura:fire_ball", "tensura:flight", "tensura:megiddo",
                                                             "tensura:heavenly_eye", "tensura:analyze", "tensura:teleport"])]
    sw = (W - 96 - 48) // 3
    sy0, sh = y, 0
    for k, (a, col, title, ids) in enumerate(sections):
        ok = [i for i in ids if table.classify(i) == a]
        sections[k] = (a, col, title, ok)
        sh = max(sh, 70 + len(ok) * 26 + 10)
    for k, (a, col, title, ok) in enumerate(sections):
        x = 48 + k * (sw + 24)
        rrect(d, (x, sy0, x + sw, sy0 + sh), 12, PANEL)
        d.text((x + 18, sy0 + 12), title, font=f_h, fill=col)
        for j, i in enumerate(ok):
            d.text((x + 18, sy0 + 54 + j * 26), i, font=f_s, fill=INK)
    y = sy0 + sh + 24
    d.text((48, y), "Everything not named by an entry or a word is NEUTRAL, which is also the answer when the table is empty or broken: the flow then does nothing. Edit data/nusmp/mega/flow/alignments.json to retune.", font=f_s, fill=MUTE)
    y += 44
    img = img.crop((0, 0, W, y))
    img.save(out)
    return img.size, {k: len(v) for k, v in by.items()}


def selfcheck(table):
    cases = [(("light",), LIGHT), (("lightning",), LIGHT), (("dark",), DARK), (("devil_union", "light"), DARK), (("curse_cleanse", "curse_warding"), NEUTRAL),
             (("flame",), NEUTRAL), (("tensura:lighten",), NEUTRAL), (("fallen_angel_gate", "spatial"), DARK), (("merged:flame+poison",), DARK), (("raging_black_bull", "recombination"), NEUTRAL)]
    bad = [(c, table.classify(*c), w) for c, w in cases if table.classify(*c) != w]
    return bad


def main():
    ap = argparse.ArgumentParser(description="Draws the magic-alignment tables of the Rukh flow")
    ap.add_argument("--out", default=".")
    ap.add_argument("--data", default=DATA)
    ap.add_argument("--selfcheck", action="store_true")
    a = ap.parse_args()
    table = Table(json.loads(read(a.data)))
    if a.selfcheck:
        bad = selfcheck(table)
        print("selfcheck:", "OK" if not bad else bad)
        return 1 if bad else 0
    fams = families()
    names = {f[0] for f in fams}
    pg = pages(names)
    if len(fams) < 100:
        print("the family scan found only %d families: MagicType.java no longer has the shape this script reads" % len(fams))
        return 2
    n = numbers()
    os.makedirs(a.out, exist_ok=True)
    fam_counts = {LIGHT: 0, DARK: 0, NEUTRAL: 0}
    for name, disp, soul, particle in fams:
        fam_counts[table.classify(name.lower())] += 1
    counts = fam_counts
    s1 = matrix_image(n, counts, os.path.join(a.out, "flow_matrix.png"))
    s2, by = families_image(fams, table, pg, os.path.join(a.out, "flow_families.png"), None)
    print("families %s, pages %d -> %s" % (counts, len(pg), a.out))
    print("flow_matrix.png", s1, "flow_families.png", s2)
    return 0


if __name__ == "__main__":
    sys.exit(main())
