#!/usr/bin/env python3
"""
Mock-up of the custom boss bar without the game (the game itself cannot be run here): composes a bar from the GENERATED textures and the style json
files with the same layout and the same drawing order as client/gui/bossbar/CustomBarPainter, and writes contact sheets.

    python3 tools/boss_bar_preview.py                   # docs/previews/boss_bars/sheet_NN.png (4 styles each) + overview.png
    python3 tools/boss_bar_preview.py --style holy      # one style, four states, to docs/previews/boss_bars/<style>.png

What it can NOT show: the animation (shimmer scroll, entrance, shatter, pulses), the font of the game, the live portrait. See docs/boss_bars.md.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp")
TEX = os.path.join(ASSETS, "textures", "gui", "boss_bar")
DATA = os.path.join(ASSETS, "boss_bars")
OUT = os.path.join(HERE, "..", "docs", "previews", "boss_bars")

INDEX = json.load(open(os.path.join(DATA, "_index.json")))
L = INDEX["layout"]
WIN = L["window"]
W, H = L["width"], L["height"]
CAST_Y, ROW_Y, FULL_H = 43, 48, 54

try:
    FONT = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 9)
    FONT_S = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 7)
except OSError:
    FONT = FONT_S = ImageFont.load_default()

SAMPLES = {
    "holy": ("Seraphiel the Dawnbringer", "Choir-Warden of the Gilded Court"), "evil": ("Vorthak the Hollow Crown", "Tyrant of the Iron Thorn"),
    "undead": ("Maelis the Pale Choir", "Banshee Matriarch"), "fire": ("Ignareth, Forge-Saint", "Keeper of the Ashen Anvil"),
    "ice": ("Skadi the Rimebound", "Sentinel of the Frozen Keep"), "storm": ("Zephyron Stormcall", "Thunder Seraph of the Spire"),
    "void": ("Nul'Azoth", "Star-Eater of the Observatory"), "forest": ("Ilyarel Greenmantle", "Verdant Archon"),
    "water": ("Thalassa of the Drowned Choir", "Tide-Warden"), "blood": ("Belisarra the Third Cut", "Sanguine Prophet"),
    "clockwork": ("Tick-Tyrant Orrery", "Regent of the Brass Vault"), "mirror": ("Duke Vael of Glass", "The Many-Faced"),
    "underworld": ("Megicula", "Supreme Devil of the Root"), "qliphoth_gate": ("Walgner", "Warden of the First Gate"),
    "devil": ("Zariel the Fallen", "Archduchess of the Pit"), "supreme_devil": ("Lucifero", "Crown of the Tree of Qliphoth"),
    "dragon": ("Aurelith the Platinum Wyrm", "Order Dragon"), "angel": ("Haniel of the Quiet Wing", "Archangel"),
    "archon": ("Solarion the Judge", "Archon of the Sun"), "lich": ("Jezreth the Undying Scholar", "Lich of the Weeping Graves"),
    "fallen_king": ("Quillothe the Hollow Crown", "Fallen King of the Shattered Hall"), "pocket_tomb": ("Khemet-Ra, Warden of Sands", "Guardian of the Gilded Tomb"),
    "plague": ("Mother Rotbloom", "Plague Lord of the Marsh"), "zogratis": ("Dante Zogratis", "Lucifero 60%"),
    "midnight_sun": ("Fana", "Eye of the Midnight Sun"), "seven_sins": ("Meliodas", "Dragon's Sin of Wrath"),
    "spade_kingdom": ("Morris", "Spade Kingdom Raid"), "magic_knights": ("Heath Grice", "The Death Knight"),
    "danmachi": ("Minotaur", "Orario Labyrinth"), "lucius": ("Lucius Zogratis", "Ultimate Wizard King"),
    "god_of_order": ("God of Order", "The World's Judge"), "goliath": ("Goliath", "Floor Warden of Steel"),
    "charybdis": ("Charybdis Spawn", "Floor Warden of the Deep"), "warden": ("Labyrinth Guardian (Frost)", "Floor +10 Warden"),
    "spirit_king": ("The Celestial Spirit King", "Third Form"), "zagred": ("Zagred", "The Word Soul"),
}


def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


class Assets:
    def __init__(self):
        self.sheet = Image.open(os.path.join(TEX, "emblems.png")).convert("RGBA")
        self.fxs = np.array(Image.open(os.path.join(TEX, "fillfx.png")).convert("RGBA"))
        self.chains = np.array(Image.open(os.path.join(TEX, "chains.png")).convert("RGBA"))
        self.frames, self.styles = {}, {}

    def style(self, sid):
        if sid not in self.styles:
            self.styles[sid] = json.load(open(os.path.join(DATA, sid + ".json")))
            self.frames[sid] = Image.open(os.path.join(TEX, "frame_%s.png" % sid)).convert("RGBA")
        return self.styles[sid], self.frames[sid]

    def emblem(self, name):
        i = INDEX["emblems"].index(name)
        c = L["emblem_cols"]
        return self.sheet.crop(((i % c) * 32, (i // c) * 32, (i % c) * 32 + 32, (i // c) * 32 + 32))


A = Assets()


def tinted(arr, rgb, alpha=1.0):
    out = arr.astype(np.float32).copy()
    out[..., 0] *= rgb[0] / 255.0
    out[..., 1] *= rgb[1] / 255.0
    out[..., 2] *= rgb[2] / 255.0
    out[..., 3] *= alpha
    return out


def comp(dst, src, x, y, add=False):
    """dst, src float RGBA arrays (0-255). src placed at (x, y). Normal 'over' or additive (SRC_ALPHA, ONE)."""
    h, w = src.shape[:2]
    x0, y0 = max(0, x), max(0, y)
    x1, y1 = min(dst.shape[1], x + w), min(dst.shape[0], y + h)
    if x1 <= x0 or y1 <= y0:
        return
    s = src[y0 - y:y1 - y, x0 - x:x1 - x]
    d = dst[y0:y1, x0:x1]
    sa = (s[..., 3:4] / 255.0)
    if add:
        d[..., :3] = np.minimum(255, d[..., :3] + s[..., :3] * sa)
        d[..., 3:4] = np.maximum(d[..., 3:4], sa * 255)
    else:
        da = d[..., 3:4] / 255.0
        oa = sa + da * (1 - sa)
        d[..., :3] = np.where(oa > 0, (s[..., :3] * sa + d[..., :3] * da * (1 - sa)) / np.maximum(oa, 1e-6), 0)
        d[..., 3:4] = oa * 255


def roman(n):
    return ["", "I", "II", "III", "IV", "V", "VI"][max(0, min(6, n))]


def render(sid, hp=1.0, ghost=None, phase=1, phases=3, thresholds=(0.66, 0.33), shield=0.0, invulnerable=False, enraged=False, cast=None,
           lair=None, adds=-1, fighters=-1, name=None, title=None, heal=False, S=3, t=7.3):
    st, frame = A.style(sid)
    c = st["colors"]
    ghost = hp if ghost is None else ghost
    nm, ti = SAMPLES.get(sid, (sid, ""))
    name, title = name or nm, title or ti
    canvas = np.zeros((FULL_H * S, W * S, 4), np.float32)

    def layer(i):
        y = L["layer_y"][i]
        im = frame.crop((0, y, L["atlas_w"], y + L["layer_h"])).resize((W * S, H * S), Image.LANCZOS)
        return np.array(im).astype(np.float32)

    comp(canvas, layer(0), 0, 0)
    # socket content: the emblem
    em = A.emblem(st["emblem"]).resize((26 * S, 26 * S), Image.LANCZOS)
    comp(canvas, np.array(em).astype(np.float32), int((L["socket"][0] - 13) * S), int((L["socket"][1] - 13) * S))
    wx, wy, ww, wh = WIN
    fw = int(round(hp * ww)) if hp > 0 else 0
    if hp > 0 and fw < 1:
        fw = 1
    gw = int(round(ghost * ww))
    top, bot = hexc(c["fill_top"]), hexc(c["fill_bottom"])
    if invulnerable:
        top, bot = tuple(int(v * 0.5 + w * 0.5) for v, w in zip(top, hexc(c["invulnerable"]))), tuple(int(v * 0.5 + w * 0.5) for v, w in zip(bot, hexc(c["invulnerable"])))

    def rect(x, y, w, h, rgb, a=1.0, add=False):
        if w <= 0 or h <= 0:
            return
        arr = np.zeros((int(h * S), int(w * S), 4), np.float32)
        arr[..., :3] = rgb
        arr[..., 3] = 255 * a
        comp(canvas, arr, int(x * S), int(y * S), add)

    def grad(x, y, w, h, t, b, a=1.0):
        if w <= 0:
            return
        hh = int(h * S)
        arr = np.zeros((hh, int(w * S), 4), np.float32)
        for j in range(hh):
            k = j / max(1, hh - 1)
            arr[j, :, :3] = [t[i] + (b[i] - t[i]) * k for i in range(3)]
        arr[..., 3] = 255 * a
        comp(canvas, arr, int(x * S), int(y * S))

    if gw > fw:
        grad(wx + fw, wy, gw - fw, wh, (255, 255, 255), hexc(c["glow"]), 0.85)
    if fw > 0:
        grad(wx, wy, fw, wh, top, bot)
        fxi = INDEX["fx"].index(st["fx"])
        tile = A.fxs[fxi * L["fx_row_h"]:(fxi + 1) * L["fx_row_h"]]
        reps = int(math.ceil(fw / L["fx_w"])) + 1
        strip = np.tile(tile, (1, reps, 1))
        off = int(t * 10) % L["fx_w"]
        strip = strip[:, off:off + fw]
        im = Image.fromarray(strip.astype(np.uint8)).resize((fw * S, wh * S), Image.NEAREST)
        comp(canvas, tinted(np.array(im), hexc(c["glow"]), 0.55), wx * S, wy * S, add=True)
        rect(wx, wy, fw, 1, (255, 255, 255), 0.35)
        rect(wx + fw - 2, wy, 2, wh, hexc(c["glow"]), 0.8, add=True)
    if shield > 0:
        sw = int(round(min(1.0, shield) * ww))
        fxi = INDEX["fx"].index("shield")
        tile = A.fxs[fxi * L["fx_row_h"]:(fxi + 1) * L["fx_row_h"]]
        strip = np.tile(tile, (1, int(math.ceil(sw / L["fx_w"])) + 1, 1))[:, :sw]
        rect(wx, wy + wh - 4, sw, 4, hexc(c["shield"]), 0.55)
        im = Image.fromarray(strip.astype(np.uint8)).resize((sw * S, wh * S), Image.NEAREST)
        comp(canvas, tinted(np.array(im), hexc(c["shield"]), 0.5), wx * S, wy * S, add=True)
        rect(wx, wy + wh - 4, sw, 1, (255, 255, 255), 0.8)
    if invulnerable:
        rect(wx, wy, ww, wh, hexc(c["invulnerable"]), 0.35)
        ch = A.chains
        strip = np.tile(ch, (1, int(math.ceil(ww / 64)) + 1, 1))[:, :ww]
        im = Image.fromarray(strip.astype(np.uint8)).resize((ww * S, 16 * S), Image.NEAREST)
        comp(canvas, tinted(np.array(im), hexc(c["invulnerable"]), 0.95), wx * S, int((wy + wh / 2 - 8) * S), add=True)
    for th in thresholds if phases > 1 else ():
        mx = wx + int(round(th * ww))
        passed = hp <= th
        rect(mx, wy, 1, wh, (0, 0, 0), 0.85)
        rect(mx + 1, wy, 1, wh, hexc(c["marker"]), 0.35 if passed else 0.85)
        for r in range(3):
            rect(mx - 1 + r * 0 - (2 - r) + 1, wy - 3 + r, 1 + (2 - r) * 2 - 0, 1, hexc(c["marker"]), 0.45 if passed else 1.0)
    if hp < 0.2 and hp > 0:
        rect(wx, wy, ww, wh, (255, 40, 40), 0.22 * (0.5 + 0.5 * math.sin(t * 5)))
    # frame and glow
    fr = layer(1)
    comp(canvas, fr, 0, 0)
    gl = layer(2)
    gcol = (255, 90, 60) if enraged else (hexc(c["invulnerable"]) if invulnerable else hexc(c["glow"]))
    comp(canvas, tinted(gl, gcol, 0.85), 0, 0, add=True)
    img = Image.fromarray(np.clip(canvas, 0, 255).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(img)

    def text(s, x, y, rgb, font, anchor="la", shadow=hexc(c["shadow"])):
        d.text((x * S + S * 0.7, y * S + S * 0.7), s, font=font, fill=shadow + (255,), anchor=anchor)
        d.text((x * S, y * S), s, font=font, fill=rgb + (255,), anchor=anchor)

    big = ImageFont.truetype(FONT.path, int(7.4 * S)) if hasattr(FONT, "path") else FONT
    small = ImageFont.truetype(FONT_S.path, int(5.6 * S)) if hasattr(FONT_S, "path") else FONT_S
    px, py, pw, ph = L["plate"]
    text(name, px + pw / 2, py + 1.2, hexc(c["name"]), big, "ma")
    tline = ("Invulnerable" if invulnerable else title)
    text(tline, px + pw / 2, py + 10.2, hexc(c["invulnerable"]) if invulnerable else hexc(c["title"]), small, "ma")
    if phases > 1:
        text(roman(phase), px + 3, py + 1.5, hexc(c["marker"]), big, "la")
    text("%d%%" % (round(hp * 100) if hp > 0.005 else 0), px + pw - 3, py + 1.5, (255, 255, 255), big, "ra")
    # under row
    if cast is not None:
        rect(wx, CAST_Y, ww, 3, (0, 0, 0), 0.7)
        rect(wx, CAST_Y, int(ww * cast[1]), 3, hexc(c["cast"]))
        text(cast[0], wx + ww / 2, ROW_Y - 1, hexc(c["cast"]), small, "ma")
    elif lair is not None:
        text("! " + lair[0] + "  %.1fs" % lair[1], wx + ww / 2, ROW_Y - 1, hexc(c["lair"]), small, "ma")
    if phases > 1:
        text("Phase %s" % roman(phase), wx, ROW_Y - 1, hexc(c["title"]), small, "la")
    right = []
    if adds >= 0:
        right.append("Adds %d" % adds)
    if fighters > 0:
        right.append("%d fighters" % fighters)
    if right:
        text("   ".join(right), wx + ww, ROW_Y - 1, (230, 230, 230), small, "ra")
    return img


STATES = [
    dict(hp=1.0, phase=1, phases=3, thresholds=(0.66, 0.33)),
    dict(hp=0.58, ghost=0.74, phase=2, phases=3, thresholds=(0.66, 0.33), shield=0.3, cast=("Radiant Nova", 0.6), fighters=3),
    dict(hp=0.17, phase=3, phases=3, thresholds=(0.66, 0.33), enraged=True, adds=3, lair=("Falling Stars", 2.4), fighters=3),
    dict(hp=0.45, phase=2, phases=3, thresholds=(0.66, 0.33), invulnerable=True),
]


def backdrop(w, h):
    a = np.zeros((h, w, 3), np.float32)
    for y in range(h):
        k = y / max(1, h - 1)
        a[y, :, :] = (46 + 18 * k, 70 + 20 * k, 52 + 14 * k)
    rng = np.random.RandomState(4)
    a += rng.randint(-5, 6, (h, w, 1))
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)).convert("RGBA")


def sheet(ids, path, S=2):
    cw, ch = W * S + 16, FULL_H * S + 12
    img = backdrop(cw * 2 + 8, ch * 2 * len(ids) + 12 * len(ids))
    d = ImageDraw.Draw(img)
    f = ImageFont.truetype(FONT.path, 11) if hasattr(FONT, "path") else FONT
    y = 6
    for sid in ids:
        d.text((8, y), sid, font=f, fill=(255, 255, 255, 255))
        for k, stt in enumerate(STATES):
            bar = render(sid, S=S, **stt)
            img.alpha_composite(bar, (8 + (k % 2) * cw, y + 12 + (k // 2) * ch))
        y += ch * 2 + 12
    img.convert("RGB").save(path, optimize=True)


def main(argv):
    os.makedirs(OUT, exist_ok=True)
    ids = INDEX["styles"]
    if "--style" in argv:
        sid = argv[argv.index("--style") + 1]
        sheet([sid], os.path.join(OUT, sid + ".png"), 3)
        return
    for n in range(0, len(ids), 4):
        sheet(ids[n:n + 4], os.path.join(OUT, "sheet_%02d.png" % (n // 4 + 1)))
    S = 2
    cols = 2
    rows = (len(ids) + cols - 1) // cols
    cw, ch = W * S + 12, FULL_H * S + 6
    img = backdrop(cols * cw + 6, rows * ch + 6)
    for i, sid in enumerate(ids):
        img.alpha_composite(render(sid, S=S, hp=0.82, phase=1, phases=3, thresholds=(0.66, 0.33)), (6 + (i % cols) * cw, 6 + (i // cols) * ch))
    img.convert("RGB").save(os.path.join(OUT, "overview.png"), optimize=True)
    print("wrote", len(ids), "styles")


if __name__ == "__main__":
    main(sys.argv[1:])
