#!/usr/bin/env python3
"""
Mock-ups of the Weapon Book (client/heroweapon/forms/WeaponBookScreen.java), drawn with PIL. NOT the game: it is a re-implementation of the screen's draw
calls and block layout, built on the Gui kit of tools/ui_preview/preview_ui.py (the anime theme's panel, well, bar, hover), with the layout rectangles
computed by the same formulas as BookLayout.java (the constants are read from that file). Item icons are stand-ins (a coloured square with the item's
first letter) because the game's item textures are not in the source tree, and the text uses the preview kit's monospace stand-in font (6 px per
character, a little wider than the game's). The Java is the truth; use this to judge layout, colours and what a state looks like.

    python3 tools/heroweapon/preview_forms_book.py OUT_DIR [--scale 2]

Writes book_available.png, book_locked.png, book_active.png, book_sealed.png, book_vassal.png, book_none.png, book_small.png (284 x 160 GUI pixels) into OUT_DIR.
"""
import importlib.util
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))


def load_kit():
    spec = importlib.util.spec_from_file_location("preview_ui", os.path.join(ROOT, "tools", "ui_preview", "preview_ui.py"))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


KIT = load_kit()
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "client", "heroweapon", "forms", "BookLayout.java")


def constants():
    """BookLayout's integer constants, read from the Java source so the preview follows the code."""
    src = open(JAVA).read()
    out = {}
    for m in re.finditer(r"(?:public static final int|,)\s*([A-Z_]+)\s*=\s*(\d+)", src):
        out[m.group(1)] = int(m.group(2))
    return out


C = constants()


class Rect:
    def __init__(self, x, y, w, h):
        self.x, self.y, self.w, self.h = x, y, w, h

    @property
    def right(self):
        return self.x + self.w

    @property
    def bottom(self):
        return self.y + self.h


def layout(sw, sh):
    """BookLayout.of(sw, sh)."""
    pw = min(sw, max(C["MIN_PANEL_W"], min(C["MAX_PANEL_W"], sw - 16)))
    ph = min(sh, max(C["MIN_PANEL_H"], min(C["MAX_PANEL_H"], sh - 16)))
    px, py = (sw - pw) // 2, (sh - ph) // 2
    pad, gap = C["PAD"], C["GAP"]
    header = Rect(px + pad, py + 5, pw - 2 * pad, C["HEADER_H"] - 7)
    list_w = max(C["MIN_LIST_W"], min(C["MAX_LIST_W"], pw * 36 // 100))
    body_y, body_h = py + C["HEADER_H"], ph - C["HEADER_H"] - C["FOOTER_H"]
    lst = Rect(px + pad, body_y, list_w, body_h)
    pane_x = lst.right + gap
    pane = Rect(pane_x, body_y, px + pw - pad - pane_x, body_h)
    foot_y = py + ph - C["FOOTER_H"] + (C["FOOTER_H"] - C["BUTTON_H"]) // 2
    return dict(panel=Rect(px, py, pw, ph), header=header, list=lst, pane=pane,
                foot_left=Rect(lst.x, py + ph - C["FOOTER_H"], lst.w, C["FOOTER_H"]), foot_right=Rect(pane.x, foot_y, pane.w, C["BUTTON_H"]))


def button_rect(lay, i, n):
    gap = 4
    w = (lay["foot_right"].w - gap * (n - 1)) // n
    return Rect(lay["foot_right"].x + i * (w + gap), lay["foot_right"].y, w, C["BUTTON_H"])


# ------------------------------------------------------------------------------------------------ sample data
SIN = {"wrath": 0xC2181B, "pride": 0x7A3FB5, "lust": 0xD94A8C, "envy": 0x2E9F5B, "greed": 0xD9A520, "sloth": 0x6A7B8C, "gluttony": 0x8B4A1F}
ITEM_COLORS = {"leaves": 0x4CAF50, "glow_berries": 0xE0A030, "leather": 0x8B5A2B, "iron_ingot": 0xC8CDD2, "blaze_rod": 0xE8A010, "ender_pearl": 0x2E8B7B,
               "bone": 0xE8E4D0, "nether_star": 0xEFEFFF, "diamond": 0x4DEBD8, "planks": 0xB08850, "coal": 0x303030}


def form(id_, name, tier, parents, req, mastery=120, stats=None, bonus=None, skills=None, passives=None, desc="", curse=False, sin=None):
    return dict(id=id_, name=name, tier=tier, parents=parents, requires=req, mastery=mastery, stats=stats or {}, bonus=bonus or {}, skills=skills or {},
                passives=passives or [], desc=desc, curse=curse, sin=sin)


def tagq(name, n):
    return dict(tag=name, count=n)


def itemq(name, n):
    return dict(item=name, count=n)


SHIELD = [
    form("base", "Small Shield", 0, [], [], 0, {"armor": 2.0}, desc="The shape every Legendary Shield starts in: plain, round, and impossible to put down.", passives=["counter_ready"],
         skills={"sneak": ("Air Strike Shield", "A ghost of the shield flies out and strikes a foe at range."), "key1": ("Shield Prison", "Closes a cage of light round a foe.")}),
    form("leaf_shield", "Leaf Shield", 1, ["base"], [tagq("leaves", 32), itemq("glow_berries", 8)], 120, {"armor": 3.0, "movement_speed": 0.04}, {"armor": 0.5},
         {"sneak": ("Air Strike Shield", "A ghost of the shield flies out and strikes a foe at range."), "key2": ("Leaf Cover", "A canopy of leaves turns aside the next blows.")},
         ["verdant_skin"], "Woven from what grows: lighter, quicker, and kind to the one who carries it."),
    form("iron_shield", "Iron Shield", 1, ["base"], [itemq("iron_ingot", 24), itemq("coal", 16)], 120, {"armor": 4.0, "movement_speed": -0.02}, {"armor_toughness": 0.5},
         desc="Plain iron, riveted: heavy and certain."),
    form("chimera_shield", "Chimera Viper Shield", 2, ["leaf_shield"], [itemq("bone", 32), itemq("leather", 32)], 200, {"armor": 4.0, "attack_damage": 2.0}, {"max_health": 1.0},
         {"sneak": ("Poison Fang", "The shield bites back at whoever struck it.")}, ["viper_bite"], "A venom-glanded hide laid over the face: it bites those who strike it."),
    form("blaze_shield", "Blaze Shield", 2, ["iron_shield"], [itemq("blaze_rod", 16), itemq("iron_ingot", 32)], 200, {"armor": 5.0}, {"knockback_resistance": 0.05},
         desc="Tempered in fire until the face runs orange when it is struck."),
    form("ender_shield", "Void Shield", 3, ["chimera_shield", "blaze_shield"], [itemq("ender_pearl", 24), tagq("planks", 64)], 300, {"armor": 6.0, "reach": 0.5}, {"luck": 1.0},
         desc="It remembers where it has been, and so can go there again."),
    form("star_shield", "Star Shield", 4, ["ender_shield"], [itemq("nether_star", 4), itemq("diamond", 128)], 400, {"armor": 8.0, "armor_toughness": 4.0}, {"armor": 2.0},
         {"key3": ("Starfall Wall", "A wall of falling stars; the ultimate of the shield.")}, desc="The last shape short of the curse."),
    form("wrath_1", "Shield of Wrath", 1, ["base"], [], 120, {"attack_damage": 4.0, "armor": 2.0}, desc="Anger given a face. It will take what you have, and give you back its power.",
         curse=True, sin="wrath"),
    form("wrath_2", "Shield of Rage", 2, ["wrath_1"], [itemq("blaze_rod", 16)], 200, {"attack_damage": 8.0}, curse=True, sin="wrath", desc="The rage has a plan now."),
]


def tag_have(name, absorbed):
    return sum(v for k, v in absorbed.items() if k == name)


class State:
    def __init__(self, forms, active, learned, mastery, mastered, absorbed, series=(), offered="", exp=3460, kind="Legendary Shield", cls="Holy Weapon", holder="Naofumi Iwatani",
                 corruption=0, bound=True, inventory=None):
        self.forms, self.active, self.learned, self.mastery, self.mastered = forms, active, set(learned) | {"base"}, mastery, set(mastered)
        self.absorbed, self.series, self.offered, self.exp = absorbed, set(series), offered, exp
        self.kind, self.cls, self.holder, self.corruption, self.bound = kind, cls, holder, corruption, bound
        self.inventory = inventory or []


def met(q, absorbed):
    key = q.get("tag") or q.get("item")
    return absorbed.get(key, 0) >= q["count"]


def status(f, st):
    if f["id"] in st.learned:
        return "ACTIVE" if f["id"] == st.active else "LEARNED"
    if f["curse"] and f["sin"] not in st.series:
        return "SEALED"
    ok = all(p in st.learned for p in f["parents"]) and all(met(q, st.absorbed) for q in f["requires"])
    return "AVAILABLE" if ok else "LOCKED"


def level_of(exp):
    lv = 0
    for L in range(1, 101):
        if round(40.0 * L ** 1.7) <= exp:
            lv = L
    return lv


def exp_for(level):
    return 0 if level == 0 else round(40.0 * level ** 1.7)


# ------------------------------------------------------------------------------------------------ the drawing (a port of WeaponBookScreen)
def rows_of(forms):
    ordinary = [f for f in forms if not f["curse"]]
    curse = [f for f in forms if f["curse"]]
    known = {f["id"] for f in ordinary}
    kids, roots = {}, []
    for f in ordinary:
        if not f["parents"] or f["parents"][0] not in known:
            roots.append(f)
        else:
            kids.setdefault(f["parents"][0], []).append(f)
    out, seen = [], set()

    def walk(f):
        if f["id"] in seen:
            return
        seen.add(f["id"])
        out.append((f, f["tier"], False))
        for c in kids.get(f["id"], []):
            walk(c)

    for r in roots:
        walk(r)
    if curse:
        mt = min(f["tier"] for f in curse)
        order = []
        for f in curse:
            if f["sin"] not in order:
                order.append(f["sin"])
        first = True
        for f in sorted(curse, key=lambda f: (order.index(f["sin"]), f["tier"])):
            out.append((f, max(0, f["tier"] - mt), first))
            first = False
    return out


def wrap(text, width):
    lines, line = [], ""
    for wd in text.split():
        if (len(line) + len(wd) + (1 if line else 0)) * 6 <= width:
            line = (line + " " + wd) if line else wd
        else:
            lines.append(line)
            line = wd
    if line:
        lines.append(line)
    return lines


def trunc(text, width):
    """WeaponBookScreen.fit: the text cut to the width with three dots (6 px per character here, a little wider than the game's font)."""
    n = max(0, width // 6)
    if len(text) <= n:
        return text
    return text[:max(0, n - 3)] + "..."


def sin_text(sin):
    """WeaponBookScreen.sinText: the sin's colour lightened for text."""
    return KIT.mix(0xFF000000 | SIN[sin], 0xFFFFFFFF, 0.38)


def stat_text(key, v):
    label = {"attack_damage": "Attack damage", "attack_speed": "Attack speed", "armor": "Armor", "armor_toughness": "Armor toughness", "max_health": "Max health",
             "movement_speed": "Movement speed", "knockback_resistance": "Knockback resistance", "reach": "Reach", "luck": "Luck"}[key]
    pct = key in ("movement_speed", "knockback_resistance")
    shown = v * 100 if pct else v
    num = ("%.2f" % abs(shown)).rstrip("0").rstrip(".")
    return "%s %s%s%s" % (label, "+" if shown >= 0 else "-", num, "%" if pct else "")


def icon(g, x, y, key, size=14):
    col = 0xFF000000 | ITEM_COLORS.get(key, 0x888888)
    g.fill(x, y, x + size, y + size, col)
    g.outline(x, y, size, size, 0x66000000)
    g.text(key[0].upper(), x + 4, y + 3, 0xFF101010)


def render(sw, sh, st, selected, scale, path, whisper_t=0.0):
    K = KIT
    K.W, K.H = sw, sh
    g = K.Gui(scale=scale)
    g.scrim()
    lay = layout(sw, sh)
    P = lay["panel"]
    g.panel(P.x, P.y, P.w, P.h, K.CYAN, 0.6)
    t = 40.0
    # ---- header
    h = lay["header"]
    if not st.bound:
        g.center("You have no hero weapon.", P.x + P.w // 2, P.y + P.h // 2 - 10, K.TEXT_DIM)
        g.center("Bind one with a Calling Gem, or ask an operator.", P.x + P.w // 2, P.y + P.h // 2 + 2, K.TEXT_DIM)
        g.save(path)
        return
    bar_w = max(70, min(120, h.w // 3))
    left_w = h.w - bar_w - 10
    g.text(trunc(st.kind, left_w), h.x, h.y, K.GOLD, bold=True)
    g.text(trunc(st.cls + (" \u00b7 " + st.holder if st.holder else ""), left_w), h.x, h.y + 11, K.TEXT_DIM)
    lv = level_of(st.exp)
    bx = h.right - bar_w
    lv_text = "Lv %d" % lv
    g.text(lv_text, bx, h.y, K.TEXT)
    exp = "%d / %d" % (st.exp, exp_for(lv + 1))
    if len(lv_text) * 6 + 8 + len(exp) * 6 <= bar_w:                       # the numbers only when they fit; else they are a tooltip
        g.text(exp, h.right - len(exp) * 6, h.y, K.TEXT_DIM)
    prog = (st.exp - exp_for(lv)) / max(1, exp_for(lv + 1) - exp_for(lv))
    g.bar(bx, h.y + 12, bar_w, 4, prog, K.VIOLET, t)
    # ---- the list
    r = lay["list"]
    g.well(r.x, r.y, r.w, r.h, K.CYAN)
    rows = rows_of(st.forms)
    entries = []
    for f, indent, heading in rows:
        if heading:
            entries.append((None, 0, True))
        entries.append((f, indent, False))
    vis = max(1, (r.h - 2) // C["ROW_H"])
    g.clip = (int((r.x + 1) * scale), int((r.y + 1) * scale), int((r.right - 1) * scale), int((r.bottom - 1) * scale))
    for i, (f, indent, heading) in enumerate(entries[:vis]):
        y = r.y + 1 + i * C["ROW_H"]
        if heading:
            g.text("Curse Series", r.x + 5, y + 3, K.RED)
            continue
        s = status(f, st)
        if f["id"] == selected:
            g.hover(r.x + 1, y, r.w - 2, C["ROW_H"], 1.0, K.CYAN)
        ix = r.x + 5 + min(indent, 5) * 6
        base = (0xFF000000 | SIN[f["sin"]]) if f["curse"] else {"ACTIVE": K.CYAN, "LEARNED": K.GREEN, "AVAILABLE": K.GOLD}.get(s, K.TEXT_DIM)
        mx_, my_ = ix, y + 3
        if s == "ACTIVE":
            g.fill(mx_, my_, mx_ + 7, my_ + 7, base); g.fill(mx_ + 2, my_ + 2, mx_ + 5, my_ + 5, 0xFFFFFFFF)
        elif s == "LEARNED":
            g.fill(mx_, my_, mx_ + 7, my_ + 7, base)
        elif s == "AVAILABLE":
            g.outline(mx_, my_, 7, 7, base); g.fill(mx_ + 2, my_ + 2, mx_ + 5, my_ + 5, K.with_alpha(base, 0.35 + 0.65 * 0.5 * (1 + __import__("math").sin(t / 30 * 6.28318))))
        elif s == "LOCKED":
            g.outline(mx_, my_, 7, 7, K.with_alpha(base, 0.8))
        else:
            g.outline(mx_, my_, 7, 7, K.with_alpha(base, 0.8)); g.fill(mx_ + 1, my_ + 3, mx_ + 6, my_ + 4, K.with_alpha(base, 0.8))
        col = {"ACTIVE": K.WHITE, "LEARNED": K.TEXT, "AVAILABLE": K.GOLD, "LOCKED": K.TEXT_DIM}.get(s)
        if s == "SEALED":
            col = K.mix(sin_text(f["sin"]), K.TEXT_DIM, 0.35)
        g.text(trunc(f["name"], r.right - (ix + 11) - 7), ix + 11, y + 3, col)
        if f["id"] in st.mastered:
            g.fill(r.right - 9, y + 5, r.right - 6, y + 8, K.GOLD)
    g.clip = None
    # ---- the details pane
    p = lay["pane"]
    g.well(p.x, p.y, p.w, p.h, K.CYAN)
    f = next(x for x in st.forms if x["id"] == selected)
    s = status(f, st)
    w = p.w - 12 - 6
    blocks = []

    def add(hh, fn):
        blocks.append((hh, fn))

    def heading(text):
        add(14, lambda x, y: g.heading(text, x, y, w, K.GOLD))

    badge = {"ACTIVE": ("IN USE", K.CYAN), "LEARNED": ("LEARNED", K.GREEN), "AVAILABLE": ("READY TO LEARN", K.GOLD), "LOCKED": ("LOCKED", K.TEXT_DIM),
             "SEALED": ("SEALED", sin_text(f["sin"] or "wrath"))}[s]
    where = (("Curse form \u00b7 %s \u00b7 " % f["sin"].title()) if f["curse"] else "") + "Tier %d" % f["tier"]

    def title(x, y):
        g.text(trunc(f["name"], w - len(badge[0]) * 6 - 6), x, y, K.GOLD, bold=True)
        g.text(badge[0], x + w - len(badge[0]) * 6, y, badge[1])
        g.text(trunc(where, w), x, y + 11, K.TEXT_DIM)

    add(22, title)
    d = wrap(f["desc"], w)
    if d:
        add(len(d) * 10, lambda x, y: [g.text(l, x, y + i * 10, K.TEXT) for i, l in enumerate(d)])
    missing = [next(z for z in st.forms if z["id"] == q)["name"] for q in f["parents"] if q not in st.learned]
    if missing and s not in ("ACTIVE", "LEARNED"):
        nd = wrap("Learn first: " + ", ".join(missing), w)
        add(len(nd) * 10, lambda x, y: [g.text(l, x, y + i * 10, K.RED) for i, l in enumerate(nd)])
    if s == "SEALED":
        sd = wrap("Sealed: this series opens only when the weapon offers its curse and you embrace it.", w)
        add(len(sd) * 10, lambda x, y: [g.text(l, x, y + i * 10, K.RED) for i, l in enumerate(sd)])
    if f["requires"]:
        heading("The gem must absorb")
        for q in f["requires"]:
            key = q.get("tag") or q.get("item")
            have = min(q["count"], st.absorbed.get(key, 0))
            done = have >= q["count"]
            label = ("Any " + key.title()) if q.get("tag") else key.replace("_", " ").title()

            def row(x, y, q=q, key=key, have=have, done=done, label=label):
                icon(g, x, y, key)
                cnt = "%d/%d" % (have, q["count"])
                g.text(trunc(label, w - 18 - len(cnt) * 6 - 4), x + 18, y, K.GREEN if done else K.TEXT)
                g.text(cnt, x + w - len(cnt) * 6, y, K.GREEN if done else K.TEXT_DIM)
                g.bar(x + 18, y + 11, w - 18, 4, have / q["count"], K.GREEN if done else K.CYAN, t)
            add(20, row)
    if s not in ("ACTIVE", "LEARNED", "SEALED"):
        feed = []
        for key, n in st.inventory:
            if any((q.get("tag") or q.get("item")) == key and not met(q, st.absorbed) for q in f["requires"]):
                feed.append((key, n))
        if feed:
            heading("Feed the gem")
            per = max(1, (w + 2) // 20)
            rows_n = (len(feed) + per - 1) // per

            def feed_row(x, y):
                for i, (key, n) in enumerate(feed):
                    sx, sy = x + (i % per) * 20, y + (i // per) * 20
                    g.well(sx, sy, 18, 18, K.CYAN)
                    icon(g, sx + 1, sy + 1, key, 16)
                    if n > 1:
                        g.text(str(n), sx + 18 - len(str(n)) * 6, sy + 10, 0xFFFFFFFF)
            add(rows_n * 20, feed_row)
            hint = wrap("Click: feed what is needed. Shift-click: the whole stack. Sneak + use with it in the off hand also feeds.", w)
            add(len(hint) * 10, lambda x, y: [g.text(l, x, y + i * 10, K.TEXT_DIM) for i, l in enumerate(hint)])
    if f["stats"]:
        heading("While in use")
        for k, v in f["stats"].items():
            add(11, lambda x, y, k=k, v=v: g.text(trunc(stat_text(k, v), w), x + 4, y, K.RED if v < 0 else K.TEXT))
    if f["bonus"]:
        heading("Permanent, once mastered")
        got = f["id"] in st.mastered
        for k, v in f["bonus"].items():
            add(11, lambda x, y, k=k, v=v: g.text(trunc(stat_text(k, v), w), x + 4, y, K.GREEN if got else K.TEXT_DIM))
    if f["skills"]:
        heading("Techniques")
        for slot, (name, desc) in f["skills"].items():
            label = {"use": "Use", "sneak": "Sneak + Use", "key1": "Key 1", "key2": "Key 2", "key3": "Key 3"}[slot]
            ult = slot == "key3" and f["id"] == "star_shield"

            def skill(x, y, label=label, name=name, ult=ult):
                tx = "[%s] " % label
                g.text(tx, x + 4, y, K.TEXT_DIM)
                g.text(trunc(name, w - len(tx) * 6 - 14), x + 4 + len(tx) * 6, y, K.GOLD if ult else K.TEXT)
                if ult:
                    g.text("*", x + w - 6, y, K.GOLD)
            add(12, skill)
    if f["passives"]:
        heading("Passives")
        for pid in f["passives"]:
            add(12, lambda x, y, pid=pid: g.text(trunc(pid.replace("_", " ").title(), w - 8), x + 4, y, K.TEXT))
    if f["mastery"] > 0:
        heading("Mastery")
        can = f["id"] in st.learned
        have = min(f["mastery"], st.mastery.get(f["id"], 0))
        done = f["id"] in st.mastered

        def mastery(x, y):
            text = ("%d points once learned" % f["mastery"]) if not can else ("Mastered" if done else "%d / %d" % (have, f["mastery"]))
            g.text(text, x, y, K.GREEN if done else K.TEXT_DIM)
            g.bar(x, y + 10, w, 4, (have / f["mastery"]) if can else 0, K.GREEN if done else K.VIOLET, t)
        add(17, mastery)
    total = sum(b[0] + 4 for b in blocks) - 4
    g.clip = (int((p.x + 1) * scale), int((p.y + 1) * scale), int((p.right - 1) * scale), int((p.bottom - 1) * scale))
    y = p.y + 4
    for hh, fn in blocks:
        if y + hh > p.y and y < p.bottom:
            fn(p.x + 6, y)
        y += hh + 4
    g.clip = None
    if total + 8 > p.h:                                                        # the scroll bar
        track = p.h - 4
        bar_h = max(10, track * p.h // (total + 8))
        g.fill(p.right - 4, p.y + 2, p.right - 2, p.bottom - 2, K.with_alpha(K.TEXT_DIM, 0.25))
        g.fill(p.right - 4, p.y + 2, p.right - 2, p.y + 2 + bar_h, K.with_alpha(K.CYAN, 0.85))
    # ---- the footer
    fl = lay["foot_left"]
    if st.offered and st.offered not in st.series:
        text = "The weapon whispers: " + st.offered.title()
        lines = wrap(text, fl.w)[:2]
        y0 = fl.y + (fl.h - len(lines) * 10) // 2
        for i, line in enumerate(lines):
            g.text(line, fl.x, y0 + i * 10, K.mix(sin_text(st.offered), 0xFFFFFFFF, 0.15))
    elif st.series:
        first = sorted(st.series)[0]
        g.text(trunc("Curse: " + ", ".join(x.title() for x in sorted(st.series)), fl.w), fl.x, fl.y + (3 if st.corruption else (fl.h - 9) // 2), sin_text(first))
        if st.corruption:
            g.bar(fl.x, fl.y + 3 + 9 + 3, fl.w - 2, 3, st.corruption / 1000.0, 0xFF000000 | SIN[first], t)
    embrace = bool(st.offered) and st.offered not in st.series
    n = 3 if embrace else 2
    labels = [("In use" if s == "ACTIVE" else "Use", s == "LEARNED"), ("Learn", s == "AVAILABLE")] + ([("Embrace", True)] if embrace else [])
    for i, (label, enabled) in enumerate(labels):
        b = button_rect(lay, i, n)
        if enabled:
            g.button(b.x, b.y, b.w, b.h, label, 0.0)
        else:
            g.panel(b.x, b.y, b.w, b.h, K.TEXT_FAINT, 0.25, 1.0)
            g.center(label, b.x + b.w // 2, b.y + (b.h - 8) // 2, K.TEXT_FAINT)
    g.save(path)


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    scale = int(sys.argv[sys.argv.index("--scale") + 1]) if "--scale" in sys.argv else 2
    os.makedirs(out, exist_ok=True)
    absorbed = {"leaves": 41, "glow_berries": 8, "iron_ingot": 12, "coal": 16, "bone": 6, "leather": 20, "blaze_rod": 3}
    inv = [("iron_ingot", 41), ("bone", 28), ("leather", 64), ("coal", 12)]
    learned = {"leaf_shield", "iron_shield"} if False else {"leaf_shield"}
    base = dict(forms=SHIELD, learned=learned, mastery={"leaf_shield": 120, "base": 0}, mastered={"leaf_shield"}, absorbed=absorbed, inventory=inv)
    render(427, 240, State(active="leaf_shield", **base), "iron_shield", scale, os.path.join(out, "book_available.png"))
    render(427, 240, State(active="base", **base), "chimera_shield", scale, os.path.join(out, "book_locked.png"))
    full = dict(base, learned={"leaf_shield", "iron_shield", "chimera_shield"}, mastery={"leaf_shield": 120, "chimera_shield": 83}, absorbed=dict(absorbed, bone=40))
    render(427, 240, State(active="chimera_shield", **full), "chimera_shield", scale, os.path.join(out, "book_active.png"))
    render(427, 240, State(active="leaf_shield", offered="wrath", corruption=0, **base), "wrath_1", scale, os.path.join(out, "book_sealed.png"))
    hammer = [form("base", "Hammer", 0, [], [], 0, {"attack_damage": 1.0}, desc="The Hammer of the Seven Stars."),
              form("iron_hammer", "Iron Hammer", 1, ["base"], [itemq("iron_ingot", 24)], 120, {"attack_damage": 2.0}, {"armor": 0.5}, desc="A heavier head."),
              form("gold_hammer", "Gold Hammer", 2, ["iron_hammer"], [itemq("diamond", 32)], 200, {"attack_damage": 3.0}, desc="A flashier head.")]
    render(427, 240, State(hammer, "base", set(), {}, set(), {"iron_ingot": 24}, kind="Vassal Hammer", cls="Seven Star Weapon", holder="", exp=120, inventory=[("diamond", 5)]),
           "iron_hammer", scale, os.path.join(out, "book_vassal.png"))
    render(427, 240, State(SHIELD[:1], "base", set(), {}, set(), {}, bound=False), "base", scale, os.path.join(out, "book_none.png"))
    render(284, 160, State(active="base", **base), "leaf_shield", scale, os.path.join(out, "book_small.png"))
    print("wrote 7 pictures into", out)


if __name__ == "__main__":
    main()
