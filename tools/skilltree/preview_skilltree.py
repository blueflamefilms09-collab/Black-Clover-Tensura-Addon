#!/usr/bin/env python3
"""
Mock-ups of the Skill Tree screen and of the "Skill Tree" button in the existing menus, drawn with PIL. NOT the game.

What is real and what is a mock:
  * REAL: every rectangle, node position, node state, price and routed line comes from the Java classes of the screen (ScreenLayout, TreeLayout, LineRouter, NodeState,
    ButtonPlacement). This tool compiles tools/skilltree/LayoutDump.java against src/main/java and runs it, so the geometry cannot drift from the code.
  * MOCK: colours, panels, text, icons and the menus' own art are re-implementations (the theme kit of tools/ui_preview/preview_ui.py, which this file imports), drawn with a
    6-pixel monospace stand-in for Minecraft's proportional font and coloured squares in place of item icons. Wrapping and clipping therefore differ a little from the game.

    python3 tools/skilltree/preview_skilltree.py OUT_DIR            # writes skilltree_*.png and menu_*.png into OUT_DIR
    python3 tools/skilltree/preview_skilltree.py OUT_DIR --scale 3  # bigger

Needs: a JDK 21 (javac and java on the PATH), Pillow, numpy. The route is tools/skilltree/sample_fire_dragon_slayer.json (a SAMPLE of the shape of the Fire Dragon Slayer tree:
9 nodes, two branches, a Secret Art that needs one node of each).
"""
import hashlib
import importlib.util
import json
import math
import os
import shutil
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
SRC = os.environ.get("SKILLTREE_SRC") or os.path.join(ROOT, "src", "main", "java")   # a merge preview may point this at another source root


def _load_theme_kit():
    path = os.path.join(ROOT, "tools", "ui_preview", "preview_ui.py")
    spec = importlib.util.spec_from_file_location("preview_ui", path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


pu = _load_theme_kit()
SCALE = 2
T = 120.0  # the animation clock of every still

CYAN, VIOLET, GOLD, GREEN, RED, WHITE = pu.CYAN, pu.VIOLET, pu.GOLD, pu.GREEN, pu.RED, pu.WHITE
TEXT, DIM, FAINT = pu.TEXT, pu.TEXT_DIM, pu.TEXT_FAINT
SEALED = 0xFF7C8496
with_alpha, scale_alpha, mix = pu.with_alpha, pu.scale_alpha, pu.mix


# Minecraft's default font: advance width in pixels (glyph plus one pixel of spacing) per character, from the font's ascii page. Letters and digits are 6, with the narrow
# exceptions below. The mock draws each character in its own slot of this width so that wrapping and clipping decisions come out close to the game's.
_NARROW = {" ": 4, "!": 2, '"': 5, "'": 3, "(": 5, ")": 5, "*": 5, "<": 5, ">": 5, ",": 2, ".": 2, ":": 2, ";": 2, "I": 4, "[": 4, "]": 4, "`": 3, "f": 5, "i": 2, "k": 5, "l": 3,
           "t": 4, "{": 5, "}": 5, "|": 2, "@": 7, "~": 7}


def mc_width(text, bold=False):
    return sum(_NARROW.get(c, 6) + (1 if bold else 0) for c in text)


def jround(v):
    """Java's Math.round (half up) so that the mock lands on the same pixel as the game."""
    return int(math.floor(v + 0.5))


# ------------------------------------------------------------------ the Java side
class JavaLayout:
    """Compiles LayoutDump once and runs it for a size, a route and a player state."""

    def __init__(self):
        self.out = tempfile.mkdtemp(prefix="stgui_dump_")
        env = dict(os.environ, JAVA_TOOL_OPTIONS="")
        src = os.path.join(HERE, "LayoutDump.java")
        r = subprocess.run(["javac", "-d", self.out, "-sourcepath", SRC, src], capture_output=True, text=True, env=env)
        if r.returncode != 0:
            raise SystemExit("javac failed:\n" + r.stderr)

    def close(self):
        shutil.rmtree(self.out, ignore_errors=True)

    def run(self, w, h, route, unlocked, sp, levels, label_px):
        lines = [f"size {w} {h}", f"label {label_px}", f"route {route['_id']} {route['color'].lstrip('#')} {1 if unlocked else 0} {sp}"]
        for n in route["nodes"]:
            parents = ",".join(f"{p}:1" if isinstance(p, str) else f"{p['node']}:{p.get('level', 1)}" for p in n.get("parents", [])) or "-"
            costs = ",".join(str(c) for c in n.get("costs", [])) or "-"
            lines.append(f"node {n['id']} {n['x']} {n['y']} {n['cost']} {n['max_level']} {parents} {costs}")
        for node_id, lv in levels.items():
            lines.append(f"level {node_id} {lv}")
        env = dict(os.environ, JAVA_TOOL_OPTIONS="")
        r = subprocess.run(["java", "-cp", self.out, "LayoutDump"], input="\n".join(lines) + "\n", capture_output=True, text=True, env=env)
        if r.returncode != 0:
            raise SystemExit("LayoutDump failed:\n" + r.stderr)
        return json.loads(r.stdout)


# ------------------------------------------------------------------ drawing kit on top of the theme kit
class TG(pu.Gui):
    def __init__(self, w, h, scale):
        pu.W, pu.H = w, h
        super().__init__(scale)
        self.w, self.h = w, h

    def text(self, s_, x, y, argb, bold=False, shadow=False):
        """Proportional text: every character in a slot of its Minecraft advance width."""
        cx = x
        for ch in s_:
            adv = _NARROW.get(ch, 6) + (1 if bold else 0)
            if ch != " ":
                pu.Gui.text(self, ch, cx + (adv - 6) / 2.0, y, argb, bold, shadow)
            cx += adv

    def center(self, s_, cx, y, argb, bold=False):
        self.text(s_, cx - mc_width(s_, bold) / 2.0, y, argb, bold)

    def clip_rect(self, r):
        self.clip = (int(r[0] * self.s), int(r[1] * self.s), int((r[0] + r[2]) * self.s), int((r[1] + r[3]) * self.s))

    def unclip(self):
        self.clip = None

    def item(self, cx, cy, key, size=16.0):
        """A stand-in for an item icon: a coloured square with a highlight, derived from the id."""
        hsh = hashlib.md5(key.encode()).digest()
        base = 0xFF000000 | (hsh[0] // 2 + 100) << 16 | (hsh[1] // 2 + 100) << 8 | (hsh[2] // 2 + 100)
        x0, y0 = cx - size / 2, cy - size / 2
        inner = size * 0.78
        ix, iy = cx - inner / 2, cy - inner / 2
        self.fill(ix, iy, ix + inner, iy + inner, base)
        self.fill(ix, iy, ix + inner, iy + max(1.0, inner * 0.18), mix(base, WHITE, 0.45))
        self.fill(ix, iy + inner * 0.8, ix + inner, iy + inner, mix(base, 0xFF000000, 0.35))

    def padlock(self, x, y, argb, k=1, keyhole=0xC0000000):
        """TreeSkin.padlock, optionally drawn k times bigger (the detail card draws it at 2x)."""
        f = self.fill
        f(x + 1 * k, y + 1 * k, x + 2 * k, y + 4 * k, argb)
        f(x + 4 * k, y + 1 * k, x + 5 * k, y + 4 * k, argb)
        f(x + 2 * k, y, x + 4 * k, y + 1 * k, argb)
        f(x, y + 4 * k, x + 6 * k, y + 9 * k, argb)
        f(x + 2 * k, y + 5 * k, x + 4 * k, y + 7 * k, keyhole)
        f(x + 2 * k, y + 7 * k, x + 3 * k, y + 8 * k, keyhole)

    def gem(self, x, y, argb):
        for i, (l, wd) in enumerate([(3, 1), (2, 3), (1, 5), (0, 7), (1, 5), (2, 3), (3, 1)]):
            self.fill(x + l, y + i, x + l + wd, y + i + 1, argb)
        self.fill(x + 3, y + 1, x + 4, y + 3, with_alpha(WHITE, 0.55))

    def node_frame(self, x, y, size, rgb, body, edge):
        a = rgb & 0xFFFFFF
        top = with_alpha(mix(0xFF0A1226, 0xFF000000 | a, 0.30), body)
        bottom = with_alpha(mix(0xFF060A16, 0xFF000000 | a, 0.12), body)
        self.gradient(x, y, x + size, y + size, top, bottom)
        self.outline(x, y, size, size, with_alpha(a, edge))
        ln = max(2, size // 4)
        c = with_alpha(a, min(1.0, edge + 0.15))
        self.fill(x, y, x + ln, y + 1, c); self.fill(x, y, x + 1, y + ln, c)
        self.fill(x + size - ln, y, x + size, y + 1, c); self.fill(x + size - 1, y, x + size, y + ln, c)
        self.fill(x, y + size - 1, x + ln, y + size, c); self.fill(x, y + size - ln, x + 1, y + size, c)
        self.fill(x + size - ln, y + size - 1, x + size, y + size, c); self.fill(x + size - 1, y + size - ln, x + size, y + size, c)

    def selection(self, x, y, size, argb):
        o, ln = 3, max(3, size // 3)
        x0, y0, x1, y1 = x - o, y - o, x + size + o, y + size + o
        self.fill(x0, y0, x0 + ln, y0 + 1, argb); self.fill(x0, y0, x0 + 1, y0 + ln, argb)
        self.fill(x1 - ln, y0, x1, y0 + 1, argb); self.fill(x1 - 1, y0, x1, y0 + ln, argb)
        self.fill(x0, y1 - 1, x0 + ln, y1, argb); self.fill(x0, y1 - ln, x0 + 1, y1, argb)
        self.fill(x1 - ln, y1 - 1, x1, y1, argb); self.fill(x1 - 1, y1 - ln, x1, y1, argb)

    def piece(self, x0, y0, x1, y1, t, argb):
        half = t // 2
        if y0 == y1:
            self.fill(min(x0, x1), y0 - half, max(x0, x1) + 1, y0 - half + t, argb)
        else:
            self.fill(x0 - half, min(y0, y1), x0 - half + t, max(y0, y1) + 1, argb)

    def chain(self, pts, t, argb):
        for i in range(0, len(pts) - 3, 2):
            self.piece(pts[i], pts[i + 1], pts[i + 2], pts[i + 3], t, argb)

    def pieces(self, pts, t, argb):
        for i in range(0, len(pts) - 3, 4):
            self.piece(pts[i], pts[i + 1], pts[i + 2], pts[i + 3], t, argb)

    def tchip(self, r, active, hovered, rgb):
        self.chip(r[0], r[1], r[2], r[3], active, hovered, rgb)


def dashes(pts, dash, gap, phase):
    """Polyline.dashes of the Java code."""
    period = dash + gap
    out, travelled = [], 0
    for i in range(0, len(pts) - 3, 2):
        x0, y0, x1, y1 = pts[i:i + 4]
        ln = abs(x1 - x0) + abs(y1 - y0)
        if ln == 0:
            continue
        ux, uy = (x1 > x0) - (x1 < x0), (y1 > y0) - (y1 < y0)
        s = 0
        while s < ln:
            pos = (travelled + s + phase) % period
            if pos < dash:
                e = min(ln, s + (dash - pos))
                out += [x0 + ux * s, y0 + uy * s, x0 + ux * e, y0 + uy * e]
                s = e
            else:
                s = min(ln, s + (period - pos))
        travelled += ln
    return out


def wrap(text, width_px, bold=False):
    """Greedy word wrap with the game's glyph widths."""
    lines, line = [], ""
    for word in text.split():
        cand = (line + " " + word).strip()
        if mc_width(cand, bold) <= width_px:
            line = cand
        else:
            if line:
                lines.append(line)
            line = word
    if line:
        lines.append(line)
    return lines or [""]


def ellipsize(text, width_px):
    if mc_width(text) <= width_px:
        return text
    cut = text
    while cut and mc_width(cut + "...") > width_px:
        cut = cut[:-1]
    return cut.rstrip() + "..."


def pretty(s):
    return " ".join(p.capitalize() for p in s.replace(":", "_").split("_") if p)


# ------------------------------------------------------------------ the player states the stills show
SAMPLE_LEVELS = {"iron_fist": 3, "scales": 1, "claw": 1}
STATES = {
    "locked": dict(unlocked=False, sp=0, levels={}, selected=None),
    "unlocked": dict(unlocked=True, sp=1, levels=SAMPLE_LEVELS, selected="iron_fist"),
    "buyable": dict(unlocked=True, sp=5, levels=SAMPLE_LEVELS, selected="roar"),
}
OTHER_ROUTES = [("Lightning Dragon Slayer", "minecraft:lightning_rod", 0x6FD0FF), ("Sky Dragon Slayer", "minecraft:feather", 0x7CF0B0), ("Ice-Make", "minecraft:blue_ice", 0x9AB8FF)]


def load_route():
    with open(os.path.join(HERE, "sample_fire_dragon_slayer.json"), encoding="utf-8") as f:
        route = json.load(f)
    route["_id"] = "nusmp:fire_dragon_slayer"
    return route


def total_cost(route):
    tot = 0
    for n in route["nodes"]:
        costs = n.get("costs", [])
        for lv in range(1, n["max_level"] + 1):
            tot += costs[min(len(costs) - 1, lv - 1)] if costs else n["cost"]
    return tot


# ------------------------------------------------------------------ the screen
ROW_HEADING_H, ROW_GAP, ICON_COLUMN = 12, 1, 22


def list_text_width(area_w):
    return max(20, area_w - 6 - ICON_COLUMN - 2)


def draw_screen(java, w, h, state_name):
    """The Skill Tree screen in one of the three states, with the geometry Java computed."""
    st = STATES[state_name]
    route = load_route()
    rgb = int(route["color"].lstrip("#"), 16)
    L = java.run(w, h, route, st["unlocked"], st["sp"], st["levels"], mc_width("Skill Tree"))
    lay = L["layout"]
    nodes = L["tree"]["nodes"]
    by_id = {n["id"]: i for i, n in enumerate(nodes)}
    rnodes = {n["id"]: n for n in route["nodes"]}
    g = TG(w, h, SCALE)
    accent = rgb if st["unlocked"] else (SEALED & 0xFFFFFF)

    # --- background: scrim, circle, panel and wells
    g.scrim(0.9)
    g.magic_circle(w / 2, h / 2, min(w, h) * 0.62, T, VIOLET, 0.20, pu.CALM)
    p = lay["panel"]
    g.panel_scan(p[0], p[1], p[2], p[3], accent, 0.9, 1.0, T)
    g.well(*lay["routes"], CYAN)
    cv = lay["canvas"]
    g.well(*cv, accent)
    g.well(*lay["detail"], accent)
    zoom, panx, pany = L["tree"]["initialView"]
    g.clip_rect([cv[0] + 1, cv[1] + 1, cv[2] - 2, cv[3] - 2])
    g.magic_circle(cv[0] + cv[2] / 2, cv[1] + cv[3] / 2, min(cv[2], cv[3]) * 0.62, T, accent, 0.12 if st["unlocked"] else 0.06, pu.CALM)
    step = max(10, jround(16 * zoom))
    ox, oy = jround(panx) % step, jround(pany) % step
    for yy in range(cv[1] + oy, cv[1] + cv[3], step):
        for xx in range(cv[0] + ox, cv[0] + cv[2], step):
            g.fill(xx, yy, xx + 1, yy + 1, with_alpha(accent, 0.10))
    if not st["unlocked"]:                                    # NodeCanvas.drawBackground: one big faint padlock behind the greyed tree
        k = max(3, min(cv[2] // 14, cv[3] // 20))
        g.clip_rect([cv[0] + 1, cv[1] + 1, cv[2] - 2, cv[3] - 2])
        g.padlock(cv[0] + (cv[2] - 6 * k) // 2, cv[1] + (cv[3] - 9 * k) // 2, with_alpha(accent, 0.10), k, with_alpha(0x000000, 0.18))
    g.unclip()

    # --- header
    hd = lay["header"]
    g.text("SKILL TREE", hd[0] + 4, hd[1] + 6, GOLD, True)
    sp_text = f"SP {st['sp']}"
    sp_w = mc_width(sp_text) + 11
    cb = lay["closeButton"]
    sp_x = cb[0] - 10 - sp_w
    g.gem(sp_x, hd[1] + 6, mix(CYAN, WHITE, 0.15))
    g.text(sp_text, sp_x + 11, hd[1] + 6, TEXT if st["sp"] > 0 else DIM)
    cx = hd[0] + 4 + mc_width("SKILL TREE", True) + 14
    room = sp_x - 10 - cx
    if room > 24:
        cls = "Class: Fire Dragon Slayer" if st["unlocked"] else "Class: none yet"
        g.text(ellipsize(cls, room), cx, hd[1] + 6, (0xFF000000 | rgb) if st["unlocked"] else DIM)
    g.button(*cb, "Close")

    # --- route list (RouteList.build / drawRoute)
    ro = lay["routeList"]
    tw = list_text_width(ro[2])
    g.clip_rect([ro[0] + 1, ro[1] + 1, ro[2] - 2, ro[3] - 2])
    y = ro[1] + 2
    g.heading("FAIRY TAIL", ro[0] + 4, y, ro[2] - 8, GOLD & 0xFFFFFF)
    y += ROW_HEADING_H
    rows = [(route["name"], route["icon"], rgb, st["unlocked"], True)] + [(n, i, c, False, False) for n, i, c in OTHER_ROUTES]
    for name, icon, col, unlocked, selected in rows:
        lines = wrap(name, tw)[:3]
        n_lines = len(lines)
        rh = 16 + 9 * n_lines
        rr = (ro[0] + 3, y, ro[2] - 6, rh)
        r_rgb = col if unlocked else (SEALED & 0xFFFFFF)
        g.tchip(rr, selected, False, r_rgb if selected else (CYAN & 0xFFFFFF))
        iy = rr[1] + (rh - 16) // 2
        g.item(rr[0] + 3 + 8, iy + 8, icon)
        if not unlocked:
            g.fill(rr[0] + 3, iy, rr[0] + 19, iy + 16, with_alpha(0x05070E, 0.55))
            g.padlock(rr[0] + 8, iy + 3, with_alpha(WHITE, 0.8))
        active = unlocked and name == route["name"]
        name_col = FAINT if not unlocked else (GOLD if active else 0xFF000000 | r_rgb)
        for i, ln in enumerate(lines):
            g.text(ln, rr[0] + ICON_COLUMN, rr[1] + 2 + i * 9, name_col)
        sy = rr[1] + 2 + 9 * n_lines
        twr = rr[2] - ICON_COLUMN - 2
        if unlocked:
            owned = sum(1 for nd in route["nodes"] if st["levels"].get(nd["id"], 0) >= 1)
            state = "ACTIVE" if active else "UNLOCKED"
            g.text(state, rr[0] + ICON_COLUMN, sy, GOLD if active else GREEN)
            cnt = f"{owned}/{len(route['nodes'])}"
            if mc_width(state) + 6 + mc_width(cnt) <= twr:
                g.text(cnt, rr[0] + ICON_COLUMN + twr - mc_width(cnt), sy, DIM)
            g.bar(rr[0] + 4, rr[1] + rh - 4, rr[2] - 8, 2, owned / len(route["nodes"]), r_rgb, T)
        else:
            g.text("LOCKED", rr[0] + ICON_COLUMN, sy, mix(RED, DIM, 0.4))
        y += rh + ROW_GAP
    g.unclip()
    if st["unlocked"]:
        sab = lay["setActiveButton"]                              # the route is already the active class: the button is disabled
        g.panel(sab[0], sab[1], sab[2], sab[3], pu.TEXT_FAINT, 0.25, 1.0)
        g.center("Active", sab[0] + sab[2] / 2.0, sab[1] + (sab[3] - 8) // 2, pu.TEXT_FAINT)

    # --- strip: the route's name and state, and the three view chips
    sx, sy_, sw, sh = lay["strip"]
    chip_left = lay["zoomOut"][0]
    if not st["unlocked"]:
        g.padlock(sx + 5, sy_ + (sh - 9) // 2, SEALED)
        g.text(ellipsize("Requires: " + route["book_name"], chip_left - 6 - (sx + 17)), sx + 17, sy_ + (sh - 8) // 2, TEXT)
    else:
        room_s = chip_left - 6 - (sx + 6)
        g.text(ellipsize(route["name"], room_s), sx + 6, sy_ + (sh - 8) // 2 - 4, 0xFF000000 | rgb)
        g.text("ACTIVE", sx + 6, sy_ + (sh - 8) // 2 + 5, GOLD)
    for key, label in (("zoomOut", "-"), ("zoomIn", "+"), ("zoomFit", "Fit")):
        r = lay[key]
        g.tchip(r, False, False, CYAN & 0xFFFFFF)
        g.center(label, r[0] + r[2] / 2.0, r[1] + (r[3] - 8) // 2 + 1, TEXT)

    # --- canvas: lines
    sel_id = st["selected"]
    sel_i = by_id.get(sel_id, -1) if st["unlocked"] else -1
    g.clip_rect([cv[0] + 1, cv[1] + 1, cv[2] - 2, cv[3] - 2])
    thin, thick = max(1, jround(zoom)), max(1, jround(1.5 * zoom))
    for e in L["tree"]["edges"]:
        pts = []
        for k in range(0, len(e["points"]), 2):
            pts += [cv[0] + jround(panx + e["points"][k] * zoom), cv[1] + jround(pany + e["points"][k + 1] * zoom)]
        child, parent = nodes[e["child"]], nodes[e["parent"]]
        if not st["unlocked"]:
            g.pieces(dashes(pts, 3, 3, 0), thin, with_alpha(SEALED, 0.40))
        elif child["level"] >= 1:
            g.chain(pts, thick + 2, with_alpha(rgb, 0.16))
            g.chain(pts, thick, with_alpha(rgb, 0.95))
        elif parent["level"] >= e["req"]:
            g.pieces(dashes(pts, 3, 2, int(T * 0.5)), thin, with_alpha(rgb, 0.65))
        else:
            g.chain(pts, thin, with_alpha(0x5C6E88, 0.45))

    # --- canvas: nodes
    order = [i for i in range(len(nodes)) if i != sel_i] + ([sel_i] if sel_i >= 0 else [])
    for i in order:
        n = nodes[i]
        s = max(8, jround(22 * zoom))
        x = cv[0] + jround(panx + (n["cx"] - 11) * zoom)
        y = cv[1] + jround(pany + (n["cy"] - 11) * zoom)
        kind = n["status"]
        frame_rgb = accent
        body, edge, dim = {"ROUTE_LOCKED": (0.50, 0.40, 0.55), "PARENTS_LOCKED": (0.42, 0.34, 0.45), "TOO_EXPENSIVE": (0.55, 0.62, 0.12),
                           "AVAILABLE": (0.70, 1.0, 0.0), "OWNED": (0.90, 1.0, 0.0), "MAXED": (0.95, 1.0, 0.0)}[kind]
        if kind == "MAXED":
            frame_rgb = GOLD & 0xFFFFFF
        if n["canBuy"]:
            pulse = 0.5 + 0.5 * math.sin((T / 36.0 + i * 0.13) * 2 * math.pi)
            g.outline(x - 2, y - 2, s + 4, s + 4, with_alpha(accent, 0.08 + 0.22 * pulse))
            g.outline(x - 1, y - 1, s + 2, s + 2, with_alpha(accent, 0.18 + 0.34 * pulse))
        g.node_frame(x, y, s, frame_rgb, body, edge)
        g.item(x + s / 2, y + s / 2, rnodes[n["id"]].get("icon", route["icon"]), 16.0 * s / 22.0)
        is_ = jround(16.0 * s / 22.0)
        if dim > 0:
            g.fill(x + (s - is_) // 2, y + (s - is_) // 2, x + (s - is_) // 2 + is_, y + (s - is_) // 2 + is_, with_alpha(0x05070E, dim))
        if n["level"] > 0:
            maxed = kind == "MAXED"
            text = "MAX" if maxed else f"{n['level']}/{n['max']}"
            if mc_width(text) > s - 3:
                text = str(n["level"])
            if zoom >= 0.75:
                tw_ = mc_width(text)
                x1, y1 = x + s - 1, y + s - 1
                g.fill(x1 - tw_ - 3, y1 - 9, x1, y1, with_alpha(0x03050A, 0.85))
                g.text(text, x1 - tw_ - 1, y1 - 8, GOLD if maxed else WHITE)
            else:
                g.fill(x + 2, y + s - 3, x + s - 2, y + s - 1, with_alpha(0x03050A, 0.85))
                g.fill(x + 2, y + s - 3, x + 2 + jround((s - 4) * min(1.0, n["level"] / max(1, n["max"]))), y + s - 1, GOLD if maxed else WHITE)
        if i == sel_i:
            g.selection(x, y, s, with_alpha(WHITE, 0.55 + 0.35 * (0.5 + 0.5 * math.sin(T / 30.0 * 2 * math.pi))))
    g.unclip()

    # --- detail card (DetailPanel.build)
    dt = lay["detailText"]
    g.clip_rect([dt[0], dt[1], dt[2], dt[3]])
    yy = dt[1]
    wpx = dt[2]

    def para(text, color, bold=False):
        nonlocal yy
        for ln in wrap(text, wpx, bold):
            g.text(ln, dt[0], yy, color, bold)
            yy += 10
        yy += 2

    def head(text):
        nonlocal yy
        g.heading(text, dt[0], yy, dt[2], GOLD & 0xFFFFFF)
        yy += 14

    if not st["unlocked"]:
        g.padlock(dt[0] + dt[2] // 2 - 6, yy + 1, with_alpha(accent, 0.85), 2)
        yy += 22
        para("Route sealed", GOLD, True)
        yy += 4
        para("Requires: " + route["book_name"], TEXT)
        para(route["book_lore"], DIM)
        yy += 5
        para("Read its Skill Book once and every node of this route opens for good.", TEXT)
        yy += 5
        para("Found in the Black Market and on the Abyssal floors of the Mega-Dungeon.", DIM)
    elif sel_i < 0:
        para(route["name"], 0xFF000000 | rgb, True)
        para(route["desc"], TEXT)
        yy += 5
        owned = sum(1 for n in route["nodes"] if st["levels"].get(n["id"], 0) >= 1)
        para(f"Nodes learned: {owned} / {len(route['nodes'])}. The whole route costs {total_cost(route)} SP.", DIM)
        yy += 5
        para("Select a node to read about it.", FAINT)
    else:
        n = nodes[sel_i]
        rn = rnodes[n["id"]]
        para(rn["name"], GOLD, True)
        para(f"Level {n['level']} (maximum)" if n["status"] == "MAXED" else f"Level {n['level']} / {n['max']}", 0xFF000000 | rgb)
        state_words = {"AVAILABLE": ("Ready to learn", GREEN), "OWNED": ("Learned", GREEN), "MAXED": ("Maxed", GOLD), "TOO_EXPENSIVE": ("Not enough SP", RED),
                       "PARENTS_LOCKED": ("Locked by its parents", DIM), "ROUTE_LOCKED": ("Sealed", DIM)}[n["status"]]
        para(state_words[0], state_words[1])
        para("Active skill" if rn.get("skill") else "Passive", DIM)
        yy += 2
        para(rn["desc"], TEXT)
        if rn.get("values"):
            yy += 5
            head("PER LEVEL")
            for key in sorted(rn["values"]):
                para(pretty(key), DIM)
                vals = rn["values"][key]
                parts = []
                for lv in range(1, n["max"] + 1):
                    v = vals[min(len(vals) - 1, lv - 1)]
                    t = ("%g" % v)
                    parts.append(f"[{t}]" if lv == n["level"] else t)
                para("  ".join(parts), TEXT)
        if n["status"] != "MAXED":
            yy += 3
            head("COST")
            para(f"Next level: {n['price']} SP", GREEN if st["sp"] >= n["price"] else RED)
            para(f"You have: {st['sp']} SP", DIM)
        if rn.get("parents"):
            yy += 5
            head("REQUIRES")
            for p_ in rn["parents"]:
                pid, need = (p_, 1) if isinstance(p_, str) else (p_["node"], p_.get("level", 1))
                have = st["levels"].get(pid, 0)
                para(f"{rnodes[pid]['name']}, level {need}" + ("" if have >= need else f"  ({have})"), GREEN if have >= need else RED)
    g.unclip()
    bb = lay["buyButton"]
    if st["unlocked"] and sel_i >= 0:
        n = nodes[sel_i]
        label = {"OK": f"Buy {n['price']} SP", "MAXED": "Maxed", "NO_ROUTE": "Sealed", "NO_SP": f"Need {n['price']} SP", "PARENTS": "Locked", "UNKNOWN_NODE": "Locked"}[n["verdict"]]
        if not n["canBuy"]:
            g.panel(bb[0], bb[1], bb[2], bb[3], pu.TEXT_FAINT, 0.25, 1.0)
            g.center(label, bb[0] + bb[2] / 2.0, bb[1] + (bb[3] - 8) // 2, pu.TEXT_FAINT)
        else:
            g.button(*bb, label, 0.0)

    # --- footer
    fo = lay["footer"]
    hint = "Drag to pan  |  Wheel to scroll  |  Ctrl+Wheel to zoom  |  Double-click or Enter to buy"
    if mc_width(hint) > fo[2] - 4:
        hint = "Drag: pan  Ctrl+Wheel: zoom  Enter: buy"
    g.text(ellipsize(hint, fo[2] - 4), fo[0] + 2, fo[1] + 1, FAINT)
    return g


# ------------------------------------------------------------------ the menus with the new button
def with_button(base, java, menu, w, h):
    route = load_route()
    L = java.run(w, h, route, True, 0, {}, 45)
    x, y, bw, bh = L["buttons"][menu]
    base.button(x, y, bw, bh, "Skill Tree", 0.0)
    # a thin marker so the reader's eye finds it in the still
    base.outline(x - 2, y - 2, bw + 4, bh + 4, with_alpha(GOLD, 0.9))
    return base


def main():
    global SCALE
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    out = args[0] if args else "."
    if "--scale" in sys.argv:
        SCALE = int(sys.argv[sys.argv.index("--scale") + 1])
        pu.S = SCALE
    os.makedirs(out, exist_ok=True)
    java = JavaLayout()
    try:
        for (w, h) in ((427, 240), (854, 480)):
            for name in ("locked", "unlocked", "buyable"):
                path = os.path.join(out, f"skilltree_{name}_{w}x{h}.png")
                draw_screen(java, w, h, name).save(path)
                print("wrote", os.path.basename(path))
        draw_screen(java, 320, 240, "buyable").save(os.path.join(out, "skilltree_buyable_320x240.png"))
        print("wrote skilltree_buyable_320x240.png")
        jobs = [("menu_codex_427x240.png", "CODEX", 427, 240, lambda: pu.codex(0, 120)), ("menu_four_kingdoms_427x240.png", "FOUR_KINGDOMS", 427, 240, lambda: pu.four_kingdoms(300)),
                ("menu_status_427x240.png", "STATUS", 427, 240, lambda: pu.status(200)), ("menu_codex_854x480.png", "CODEX", 854, 480, lambda: pu.codex(2, 500))]
        for name, menu, w, h, make in jobs:
            pu.W, pu.H = w, h
            base = make()
            with_button(base, java, menu, w, h).save(os.path.join(out, name))
            print("wrote", name)
    finally:
        java.close()


if __name__ == "__main__":
    main()
