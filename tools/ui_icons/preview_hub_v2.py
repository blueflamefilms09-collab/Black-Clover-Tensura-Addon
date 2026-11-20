#!/usr/bin/env python3
"""
MOCK-UPS of the Multiverse Hub v2 (docs/ui_hub.md, "Hub v2"), drawn with PIL on top of tools/ui_preview/preview_ui.py. NOT game screenshots: the layout numbers are
the ones of client/hub/HubLayout (window 62% x 70% -> the 400 x 240 stage at scale 1.0 on a 640 x 360 screen, the icon strip, PANEL2, PLAYER, STATUS_LIST, SUBRAIL,
CODEX_BODY) and the icons are the real PNGs the mod ships (textures/gui/hub/), but the player is a blocky stand-in, the text is a monospace stand-in and the
page contents are sketches of what CodexPage draws. Run:

    python3 -B tools/ui_icons/preview_hub_v2.py docs/previews/hub_v2

Writes hub_v2_ring.png, hub_v2_status.png, hub_v2_codex_spells.png, hub_v2_codex_weapons.png and hub_v2_icons.png.
"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "ui_preview"))
import preview_ui as pu  # noqa: E402

ICONS = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "anime_multiverse", "textures", "gui", "hub")
SW, SH = 640, 360                       # the mock screen in GUI units (1280 x 720 at GUI scale 2)
STAGE_W, STAGE_H = 400, 240
WIN_X, WIN_Y = (SW - STAGE_W) // 2, (SH - STAGE_H) // 2
CAT_RGB = [0x5CD8FF, 0xE8C468, 0x9A7CFF, 0x7CF0B0, 0x8FD0FF]
CAT_NAMES = ["You", "Collection", "Magic", "World", "Reference"]

# HubLayout (v2) numbers
STRIP_CX, STRIP_NODE, STRIP_TOP, STRIP_PITCH = 19, 11, 42, 24
BACK = (19, 17, 9)
PANEL2, TITLE2 = (40, 24, 356, 192), (48, 29, 340, 10)
PLAYER, STATUS_LIST = (48, 44, 92, 166), (146, 44, 240, 166)
SUBRAIL, CODEX_BODY = (48, 44, 80, 166), (134, 44, 252, 166)
LINE = (14, 226, 372, 9)

# the Codex rail (HubCodexNav.NAV): id, category, label
NAV = [("overview", 0, "You"), ("status", 0, "Status"), ("weapon", 0, "Weapon"), ("beastwill", 0, "Beast Will"),
       ("weapons", 1, "Weapons"), ("relics", 1, "Relics"), ("bestiary", 1, "Bestiary"), ("spirit_lords", 1, "Spirit Lords"),
       ("spells", 2, "Spells"), ("magic", 2, "Magic"), ("effects", 2, "Effects"),
       ("blocks", 3, "Blocks"), ("entities", 3, "Entities"),
       ("guide", 4, "Guide"), ("rules", 4, "Rules"), ("wiki", 4, "Wiki")]

_cache = {}


def icon_img(name, glow=False):
    k = (name, glow)
    if k not in _cache:
        _cache[k] = Image.open(os.path.join(ICONS, name + ("_glow" if glow else "") + ".png")).convert("RGBA")
    return _cache[k]


def draw_icon(g, name, cx, cy, size, alpha=1.0, glow=0.0):
    s = g.s
    px = max(2, int(round(size * s)))
    for variant, a in ((False, alpha), (True, alpha * glow)):
        if a <= 0.02:
            continue
        im = icon_img(name, variant).resize((px, px), Image.BILINEAR)
        if a < 1.0:
            im.putalpha(im.split()[3].point(lambda v: int(v * a)))
        ox, oy = int(round(cx * s)) - px // 2, int(round(cy * s)) - px // 2
        if g.clip:                                   # the scissor of the rail / body cuts icons too
            c0, c1, c2, c3 = g.clip
            n0, n1, n2, n3 = max(ox, c0), max(oy, c1), min(ox + px, c2), min(oy + px, c3)
            if n2 <= n0 or n3 <= n1:
                continue
            im = im.crop((n0 - ox, n1 - oy, n2 - ox, n3 - oy))
            ox, oy = n0, n1
        g.img.alpha_composite(im, (ox, oy))


def new_stage():
    """The 400 x 240 stage on a transparent canvas, with the window's own dark background and the drifting motes."""
    keep = pu.W
    pu.W = STAGE_W
    try:
        g = pu.Gui(backdrop=False)
    finally:
        pu.W = keep
    g.img = Image.new("RGBA", (STAGE_W * g.s, STAGE_H * g.s), (0, 0, 0, 0))
    g.gradient(0, 0, STAGE_W, STAGE_H, pu.with_alpha(0x070B1A, 0.93), pu.with_alpha(0x03050C, 0.96))
    pu.hub_motes(g, 300.0, 0.8)
    return g


def on_screen(stage, title_hint=None):
    """Puts the stage on the mock 640 x 360 screen: world backdrop, dimmed, the window with its frame and shadow."""
    keep = (pu.W, pu.H)
    pu.W, pu.H = SW, SH
    try:
        scr = pu.Gui(backdrop=True)
    finally:
        pu.W, pu.H = keep
    scr.gradient(0, 0, SW, SH, pu.with_alpha(0x000000, 0.55), pu.with_alpha(0x000000, 0.70))
    scr.fill(WIN_X - 3, WIN_Y - 3, WIN_X + STAGE_W + 3, WIN_Y + STAGE_H + 3, pu.with_alpha(0x000000, 0.45))
    scr.img.alpha_composite(stage.img, (WIN_X * scr.s, WIN_Y * scr.s))
    scr.outline(WIN_X - 1, WIN_Y - 1, STAGE_W + 2, STAGE_H + 2, pu.with_alpha(pu.CYAN, 0.55))
    scr.outline(WIN_X - 2, WIN_Y - 2, STAGE_W + 4, STAGE_H + 4, pu.with_alpha(pu.CYAN, 0.18))
    note = "MOCK-UP: PIL re-drawing of the hub v2 layout (window 62%, not a game screenshot)"
    scr.text(note, 6, SH - 12, pu.with_alpha(0xFFFFFF, 0.55))
    return scr


def octagon_node(g, cx, cy, r, rgb, hover=0.0, active=False, alpha=1.0):
    body = pu.with_alpha(pu.mix(0xFF0A1024, 0xFF000000 | rgb, 0.12 + 0.22 * hover + (0.16 if active else 0.0)), 0.9 * alpha)
    pu.hub_octagon(g, cx, cy, r, body)
    pu.hub_octagon_edge(g, cx, cy, r, pu.with_alpha(rgb, min(1.0, (0.85 + 0.25 * hover + (0.3 if active else 0.0)) * alpha)))
    if active:
        pu.hub_octagon_edge(g, cx, cy, r + 2, pu.with_alpha(0xFFFFFF, 0.35 * alpha))


def node_icons(g, centres, r, size, hover=-1, active=-1):
    for i, tab in enumerate(pu.HUB_TABS):
        cx, cy = int(round(centres[i][0])), int(round(centres[i][1]))
        hv = 1.0 if i == hover else 0.0
        if hv > 0:
            g.magic_circle(cx, cy, r * 1.9, 300.0, 0xFF000000 | tab[2], 0.9, pu.FAST, 1.0)
        octagon_node(g, cx, cy, r, tab[2], hv, i == active)
        draw_icon(g, "node_" + tab[0], cx, cy, size, 1.0, 1.0 if (i == hover or i == active) else 0.0)


# ---------------------------------------------------------------- the ring
def ring_view(hover=2):
    g = new_stage()
    t = 220.0
    cx, cy = pu.HUB_HOME_C
    g.magic_circle(cx, cy, 100, t, pu.CYAN, 0.2, pu.CALM, 1.0)
    pu.hub_ring(g, cx, cy, pu.HUB_HOME_R, 72, t * 0.003, pu.CYAN, 0.5, 1.0, 6)
    pu.hub_ring(g, cx, cy, pu.HUB_HOME_R - 24, 48, -t * 0.004, pu.VIOLET, 0.35, 1.0, 4)
    for i, tab in enumerate(pu.HUB_TABS):
        nx, ny = pu.HUB_HOME[i]
        dx, dy = nx - cx, ny - cy
        ln = math.hypot(dx, dy)
        ux, uy = dx / ln, dy / ln
        pu.hub_spoke(g, cx + ux * 34, cy + uy * 34, nx - ux * (pu.HUB_HOME_NODE + 3), ny - uy * (pu.HUB_HOME_NODE + 3), 0xFF000000 | tab[2], 1.0, 1.0)
    g.magic_circle(cx, cy, 52, t, pu.VIOLET, 0.85, pu.STANDARD, 1.0)
    g.fill(cx - 24, cy - 24, cx + 24, cy + 24, pu.with_alpha(0x0A1024, 0.6))
    g.center("MULTI", cx, cy - 10, pu.GOLD)
    g.center("VERSE", cx, cy + 1, pu.CYAN)
    node_icons(g, pu.HUB_HOME, pu.HUB_HOME_NODE, 24, hover=hover)
    for i, tab in enumerate(pu.HUB_TABS):
        nx, ny = int(round(pu.HUB_HOME[i][0])), int(round(pu.HUB_HOME[i][1]))
        shown, _ = pu.hub_fit(tab[3], 92)
        w = len(shown) * 6
        r = pu.HUB_HOME_NODE
        lx, ly = {0: (nx - w // 2, ny - r - 13), 1: (nx + r + 4, ny - 4), 2: (nx - r - 4 - w, ny - 4), 3: (nx - w // 2, ny + r + 4)}[pu.HUB_SIDES[i]]
        g.text(shown, lx, ly, pu.mix(pu.TEXT, pu.GOLD, 1.0 if i == hover else 0.0))
    shown, _ = pu.hub_fit(pu.HUB_TABS[hover][3] + ": " + pu.HUB_TABS[hover][4], LINE[2])
    g.center(shown, LINE[0] + LINE[2] // 2, LINE[1], pu.TEXT)
    return on_screen(g)


# ---------------------------------------------------------------- the page view chrome
def page_chrome(active, title, t=300.0):
    g = new_stage()
    # back button above the strip
    bx, by, br = BACK
    pu.hub_octagon(g, bx, by, br, pu.with_alpha(0x0A1024, 0.85))
    pu.hub_octagon_edge(g, bx, by, br, pu.with_alpha(pu.GOLD, 0.65))
    g.fill(bx + 1, by - 4, bx + 3, by - 2, pu.with_alpha(pu.GOLD, 0.95)); g.fill(bx - 1, by - 2, bx + 1, by, pu.with_alpha(pu.GOLD, 0.95))
    g.fill(bx - 3, by, bx - 1, by + 2, pu.with_alpha(pu.GOLD, 0.95)); g.fill(bx - 1, by + 2, bx + 1, by + 4, pu.with_alpha(pu.GOLD, 0.95))
    g.text("MULTIVERSE HUB", 34, 10, pu.with_alpha(pu.GOLD, 0.8))
    centres = [(STRIP_CX, STRIP_TOP + i * STRIP_PITCH) for i in range(8)]
    node_icons(g, centres, STRIP_NODE, 16, hover=-1, active=active)
    rgb = pu.HUB_TABS[active][2]
    px, py, pw, ph = PANEL2
    g.panel_scan(px, py, pw, ph, 0xFF000000 | rgb, 0.7, 1.0, t)
    g.heading(title, TITLE2[0], TITLE2[1], TITLE2[2], rgb)
    return g, rgb


def scrollbar(g, rect, rgb, frac=0.0, thumb=0.45):
    x, y, w, h = rect
    sx = x + w + 2
    g.fill(sx, y, sx + 3, y + h, pu.with_alpha(0xFFFFFF, 0.08))
    th = int(h * thumb)
    g.fill(sx, y + int((h - th) * frac), sx + 3, y + int((h - th) * frac) + th, pu.with_alpha(rgb, 0.85))


def bottom_line(g, text):
    shown, _ = pu.hub_fit(text, LINE[2])
    g.center(shown, LINE[0] + LINE[2] // 2, LINE[1], pu.TEXT_DIM)


# ---------------------------------------------------------------- the player panel
def player_panel(g, rect, rgb, t=300.0, pose="swing"):
    x, y, w, h = rect
    g.well(x, y, w, h, 0xFF000000 | rgb, 1.0)
    cx = x + w // 2
    floor = y + h - 30
    # floor circle (CodexSkin.floorCircle): two flat ellipses
    g.circle_layer("circle_glow", cx, floor, 30, 0.0, rgb, 0.55, False) if False else None
    for k, a in ((34, 0.10), (26, 0.16), (18, 0.22)):
        layer = Image.new("RGBA", (int(k * 2 * g.s), int(k * 0.7 * g.s)), (0, 0, 0, 0))
        ImageDraw.Draw(layer).ellipse([0, 0, layer.width - 1, layer.height - 1], fill=pu.rgba(pu.with_alpha(rgb, a)))
        g.img.alpha_composite(layer, (int((cx - k) * g.s), int((floor - k * 0.35) * g.s)))
    layer = Image.new("RGBA", (int(68 * g.s), int(24 * g.s)), (0, 0, 0, 0))
    ImageDraw.Draw(layer).ellipse([0, 0, layer.width - 1, layer.height - 1], outline=pu.rgba(pu.with_alpha(rgb, 0.8)), width=max(1, g.s // 2))
    g.img.alpha_composite(layer, (int((cx - 34) * g.s), int((floor - 12) * g.s)))
    # the figure (blocky stand-in) in the "wind-up" pose of the cycle, a sword in the right hand
    u = (h - 64) / 32.0
    cy = floor - 16 * u
    g.fill(cx - 4 * u, cy - 16 * u, cx + 4 * u, cy - 8 * u, 0xFFC8946A)
    g.fill(cx - 4 * u, cy - 8 * u, cx + 4 * u, cy + 4 * u, 0xFF3B6EA8)
    g.fill(cx - 8 * u, cy - 8 * u, cx - 4 * u, cy + 4 * u, 0xFFC8946A)
    # raised sword arm
    g.fill(cx + 4 * u, cy - 14 * u, cx + 8 * u, cy - 6 * u, 0xFFC8946A)
    g.fill(cx + 6 * u, cy - 24 * u, cx + 7.2 * u, cy - 14 * u, 0xFFD8E4F8)
    g.fill(cx + 5 * u, cy - 15 * u, cx + 8.2 * u, cy - 14 * u, pu.GOLD)
    g.fill(cx - 4 * u, cy + 4 * u, cx, cy + 16 * u, 0xFF2B3E6B)
    g.fill(cx, cy + 4 * u, cx + 4 * u, cy + 16 * u, 0xFF2B3E6B)
    g.center("Wind-up", cx, y + h - 12, pu.TEXT_FAINT)
    g.text("Idle sway - pose cycle - follows the mouse", x + 2, y + 3, pu.TEXT_FAINT) if False else None


def status_view():
    g, rgb = page_chrome(0, "Status")
    player_panel(g, PLAYER, rgb)
    x, y, w, h = STATUS_LIST
    cards = [(pu.CYAN, "Power", ["Existence points: 1,245,300", "Magicule: 88,200   Aura: 40,100"], -1),
             (pu.GOLD, "Magic Knight Rank", ["5th Class Junior Magic Knight"], -1),
             (pu.GOLD, "Grimoire", ["Four-leaf Clover", "Pages 3/12  slots 6  mastery 25%"], 0.25),
             (pu.VIOLET, "Active effects", ["Strength II   4:12", "Regeneration   endless"], -1)]
    keep = g.clip
    g.clip = (x * g.s, y * g.s, (x + w) * g.s, (y + h) * g.s)
    yy = y
    for crgb, title, lines, fr in cards:
        ch = 4 + 11 + len(lines) * 10 + (8 if fr >= 0 else 0) + 3
        g.well(x, yy, w, ch, 0xFF000000 | crgb, 1.0)
        g.fill(x, yy, x + 2, yy + ch, pu.with_alpha(crgb, 0.9))
        g.text(title, x + 6, yy + 4, 0xFF000000 | crgb)
        ly = yy + 15
        for l in lines:
            g.text(pu.hub_fit(l, w - 12)[0], x + 6, ly, pu.TEXT)
            ly += 10
        if fr >= 0:
            g.bar(x + 6, ly + 1, w - 12, 4, fr, 0xFF000000 | crgb, 300.0)
        yy += ch + 4
    g.clip = keep
    scrollbar(g, STATUS_LIST, rgb, 0.0, 0.7)
    bottom_line(g, "Status: your power, your standing and your active effects.   Esc closes.")
    return on_screen(g)


# ---------------------------------------------------------------- the Codex rail and its pages
def codex_rail(g, current, hover=-1):
    x, y, w, h = SUBRAIL
    g.fill(x, y, x + w, y + h, pu.with_alpha(0x0A1024, 0.55))
    keep = g.clip
    g.clip = (x * g.s, y * g.s, (x + w) * g.s, (y + h) * g.s)
    yy = y + 1
    last = -1
    for i, (pid, cat, label) in enumerate(NAV):
        if cat != last:
            last = cat
            draw_icon(g, "cat_" + ["you", "collection", "magic", "world", "reference"][cat], x + 8, yy + 6, 10, 0.9, 0.0)
            g.text(CAT_NAMES[cat].upper(), x + 16, yy + 2, pu.with_alpha(CAT_RGB[cat], 0.9))
            yy += 12
        on = pid == current
        if on:
            g.fill(x + 1, yy, x + w - 1, yy + 14, pu.with_alpha(CAT_RGB[cat], 0.22))
            g.fill(x + 1, yy, x + 3, yy + 14, pu.with_alpha(CAT_RGB[cat], 0.95))
        elif i == hover:
            g.fill(x + 1, yy, x + w - 1, yy + 14, pu.with_alpha(0xFFFFFF, 0.07))
        draw_icon(g, "codex_" + pid, x + 11, yy + 7, 12, 1.0, 1.0 if on else 0.0)
        g.text(pu.hub_fit(label, w - 24)[0], x + 20, yy + 3, pu.WHITE if on else pu.TEXT)
        yy += 14
    g.clip = keep
    g.fill(x + w + 2, y, x + w + 3, y + h, pu.with_alpha(0xFFFFFF, 0.10))


def codex_spells():
    g, rgb = page_chrome(6, "Codex   /   Spells")
    codex_rail(g, "spells", hover=10)
    x, y, w, h = CODEX_BODY
    keep = g.clip
    g.clip = (x * g.s, y * g.s, (x + w) * g.s, (y + h) * g.s)
    col = w // 2 - 8
    spells = [("Fire Ball", "Basic fire sphere", 0x39), ("Ice Lance", "Pierces armour", 0x5CD8FF), ("Wind Cutter", "Fast crescent", 0x7CF0B0),
              ("Gravity Well", "Pulls near foes", 0x9A7CFF), ("Dimension Slash", "Space cut", 0xB39CF0), ("Time Skip", "Short stop", 0xE8C468),
              ("Void Step", "Short blink", 0x8FD0FF), ("Heal", "Restore ally", 0x7CF0B0), ("Thunder Spear", "Chain bolt", 0xFFE06A), ("Mana Burst", "Area wave", 0xFF9F6A)]
    for i, (name, desc, c) in enumerate(spells):
        cx_ = x + 2 + (i % 2) * (col + 12)
        cy_ = y + 2 + (i // 2) * 30
        g.well(cx_, cy_, col, 27, 0xFF000000 | (c if c > 0xFF else 0x9A7CFF), 1.0)
        g.fill(cx_ + 3, cy_ + 3, cx_ + 24, cy_ + 24, pu.with_alpha(0xFF000000 | (c if c > 0xFF else 0x9A7CFF), 0.35))
        g.outline(cx_ + 3, cy_ + 3, 21, 21, pu.with_alpha(0xFFFFFF, 0.25))
        g.text(pu.hub_fit(name, col - 32)[0], cx_ + 28, cy_ + 5, pu.TEXT)
        g.text(pu.hub_fit(desc, col - 32)[0], cx_ + 28, cy_ + 15, pu.TEXT_DIM)
    g.clip = keep
    scrollbar(g, CODEX_BODY, rgb, 0.0, 0.5)
    bottom_line(g, "Codex: Spells.   Click a page in the rail, Up/Down steps through them, Esc goes back to the ring.")
    return on_screen(g)


def codex_weapons():
    g, rgb = page_chrome(6, "Codex   /   Weapons")
    codex_rail(g, "weapons", hover=-1)
    x, y, w, h = CODEX_BODY
    keep = g.clip
    g.clip = (x * g.s, y * g.s, (x + w) * g.s, (y + h) * g.s)
    names = ["Demon-Slayer", "Lord's Edge", "Greed Blade", "Twin Fang", "Void Reaper", "Star Lance", "Dragon Fang", "Hollow Bow", "Thunder Mace", "Glass Katana",
             "Ash Scythe", "Moon Staff"]
    col = w // 2 - 8
    for i, n in enumerate(names):
        cx_ = x + 2 + (i % 2) * (col + 12)
        cy_ = y + 2 + (i // 2) * 24
        g.well(cx_, cy_, col, 21, 0xFFE8C468, 1.0)
        g.fill(cx_ + 3, cy_ + 3, cx_ + 18, cy_ + 18, pu.with_alpha(0x0A1024, 0.9))
        g.outline(cx_ + 3, cy_ + 3, 15, 15, pu.with_alpha(pu.GOLD, 0.6))
        g.fill(cx_ + 6, cy_ + 12, cx_ + 15, cy_ + 13, 0xFFD8E4F8); g.fill(cx_ + 13, cy_ + 6, cx_ + 14, cy_ + 12, 0xFFD8E4F8)
        g.text(pu.hub_fit(n, col - 26)[0], cx_ + 22, cy_ + 7, pu.TEXT)
    g.clip = keep
    scrollbar(g, CODEX_BODY, rgb, 0.0, 0.55)
    bottom_line(g, "Codex: Weapons (a catalog page: item, name, hover for its tooltip).")
    return on_screen(g)


# ---------------------------------------------------------------- the icon sheet
def icon_sheet():
    names = []
    for t in pu.HUB_TABS:
        names.append("node_" + t[0])
    for pid, cat, label in NAV:
        names.append("codex_" + pid)
    names.append("codex_data")
    for k in ("you", "collection", "magic", "world", "reference"):
        names.append("cat_" + k)
    cell, cols = 92, 8
    rows = (len(names) + cols - 1) // cols
    im = Image.new("RGBA", (cols * cell + 8, rows * (cell + 8) + 34), (8, 12, 26, 255))
    d = ImageDraw.Draw(im)
    f = ImageFont.truetype(pu.FONT, 10)
    d.text((8, 8), "Hub v2 icons: normal | glow at 40 px, then 16 and 24 px (the sizes in the hub).   MOCK-UP SHEET, the PNGs are the real files.", font=f, fill=(190, 205, 230, 255))
    for i, n in enumerate(names):
        cx, cy = (i % cols) * cell + 8, (i // cols) * (cell + 8) + 30
        d.rectangle([cx, cy, cx + cell - 4, cy + cell], outline=(40, 56, 90, 255))
        for k, glow in enumerate((False, True)):
            ic = icon_img(n, glow).resize((40, 40), Image.NEAREST if False else Image.BILINEAR)
            im.alpha_composite(ic, (cx + 2 + k * 42, cy + 4))
        small = icon_img(n).resize((16, 16), Image.BILINEAR)
        im.alpha_composite(small, (cx + 6, cy + 48))
        small2 = icon_img(n).resize((24, 24), Image.BILINEAR)
        im.alpha_composite(small2, (cx + 28, cy + 44))
        d.text((cx + 3, cy + cell - 18), n[:15], font=f, fill=(230, 240, 255, 255))
    return im


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(out, exist_ok=True)
    jobs = {"hub_v2_ring.png": ring_view, "hub_v2_status.png": status_view, "hub_v2_codex_spells.png": codex_spells,
            "hub_v2_codex_weapons.png": codex_weapons}
    for name, fn in jobs.items():
        fn().save(os.path.join(out, name))
        print("wrote", name)
    icon_sheet().save(os.path.join(out, "hub_v2_icons.png"))
    print("wrote hub_v2_icons.png")


if __name__ == "__main__":
    main()
