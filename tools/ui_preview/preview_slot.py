#!/usr/bin/env python3
"""Mock-up of the hub in Tensura's "Coming Soon" spot (client/hub/HubEmblem.java): the ring emblem (magic categories slot) and the
compact card (status screen placeholder), idle and hovered. Same geometry as the Java (ring r = min(w,h)/2-2, node r = r/6, 8 nodes,
node 0 on top); PIL stand-in, not a game screenshot.   python3 tools/ui_preview/preview_slot.py docs/previews/ui_hub_slot"""
import math, os, sys
from PIL import Image, ImageDraw, ImageFont

TABS = [0x5CD8FF, 0xE8C468, 0x9A7CFF, 0x7CF0B0, 0xB39CF0, 0xFF9F6A, 0x8FD0FF, 0xC0C8D8]
CYAN, GOLD = (92, 216, 255), (232, 196, 104)
S = 6

def rgb(v): return ((v >> 16) & 255, (v >> 8) & 255, v & 255)

def emblem(d, x, y, w, h, hover, rot=0.0):
    cx, cy, r = x + w / 2, y + h / 2, min(w, h) / 2 - 2
    col = GOLD if hover else CYAN
    d.ellipse([(cx - r * .55) * S, (cy - r * .55) * S, (cx + r * .55) * S, (cy + r * .55) * S], outline=col + (150,), width=S)
    for i in range(48):
        a = rot + i * 2 * math.pi / 48
        px, py = cx + r * math.cos(a), cy + r * math.sin(a)
        s = 1.5 if i % 4 == 0 else 1
        d.rectangle([(px - s / 2) * S, (py - s / 2) * S, (px + s / 2) * S, (py + s / 2) * S], fill=col + (200,))
    nr = max(2, round(r / 6))
    for i in range(8):
        a = -math.pi / 2 + i * 2 * math.pi / 8 + rot
        px, py = cx + r * math.cos(a), cy + r * math.sin(a)
        c = rgb(TABS[i])
        d.regular_polygon(((px) * S, (py) * S, nr * S * 1.1), 8, rotation=22.5, fill=tuple(v // 3 for v in c) + (240,), outline=c + (255,))
    cr = max(2, nr - 1)
    d.regular_polygon((cx * S, cy * S, cr * S * 1.1), 8, rotation=22.5, fill=(GOLD if hover else (255, 255, 255)) + (230,))

def card(d, x, y, w, h, hover, font):
    d.rectangle([x * S, y * S, (x + w) * S, (y + h) * S], fill=(16, 16, 22, 230), outline=(GOLD if hover else CYAN) + (255,), width=S // 2)
    side = h - 4
    emblem(d, x + 2, y + 2, side, side, hover)
    tx = x + side + 8
    d.text((tx * S, (y + h / 2 - 10) * S), "Multiverse Hub", fill=GOLD if hover else (232, 196, 104), font=font)
    d.text((tx * S, (y + h / 2 + 2) * S), "Click, or press F7", fill=(154, 154, 174), font=font)

def main(out):
    os.makedirs(out, exist_ok=True)
    try: font = ImageFont.truetype("DejaVuSans.ttf", 7 * S)
    except OSError: font = ImageFont.load_default()
    img = Image.new("RGBA", (260 * S, 120 * S), (20, 22, 34, 255)); d = ImageDraw.Draw(img, "RGBA")
    d.text((6 * S, 4 * S), "Magic categories slot (was: Four Kingdoms logo)", fill=(200, 200, 215), font=font)
    emblem(d, 20, 22, 36, 36, False); emblem(d, 70, 22, 36, 36, True, 0.4)
    d.text((6 * S, 66 * S), "Status screen placeholder (was: compact status panel)", fill=(200, 200, 215), font=font)
    card(d, 20, 80, 130, 36, False, font); card(d, 160, 80, 90, 36, True, font)
    img.resize((520 * 2 // 2 * 2 // 2, 240)).save(os.path.join(out, "hub_slot.png")) if False else img.resize((1040, 480), Image.LANCZOS).save(os.path.join(out, "hub_slot.png"))

if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "docs/previews/ui_hub_slot")
