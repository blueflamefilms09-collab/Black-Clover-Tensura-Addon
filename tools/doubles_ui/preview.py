"""Draws a mock-up of the Real Double stack (docs/doubles_ui.md) with the SAME constants and layout rules as DoublesUiMath.java / DoublesOverlay.java,
so the owner can see the layout without the game. It is a picture made with PIL, not a screenshot of the game.

    python3 tools/doubles_ui/preview.py          # writes docs/doubles_ui_preview.png
"""
import os
from PIL import Image, ImageDraw, ImageFont

# ---- the constants of DoublesUiMath
W, HEADER_H, ACTIVE_H, ROW_H, GAP, MORE_H, MAX_ROWS = 116, 10, 28, 16, 2, 10, 16
# ---- the colours of DoublesOverlay (ARGB -> RGB)
BG = (16, 8, 24, 176)
EDGE, GOLD, TEXT, DIM, TRACK, TIME = (200, 176, 255), (232, 196, 104), (230, 242, 255), (143, 163, 191), (42, 33, 64), (154, 124, 255)
HP_GOOD, HP_MID, HP_LOW = (124, 240, 176), (232, 196, 104), (255, 106, 106)
S = 4  # screen pixels per GUI pixel


def shown(n, mx): return 0 if n <= 0 else min(min(n, MAX_ROWS), max(1, mx))
def hidden(n, mx): return max(0, min(n, MAX_ROWS) - shown(n, mx))
def source(slot, active, n):
    if active < 0 or active >= n: return slot
    if slot == 0: return active
    return slot - 1 if slot <= active else slot
def card_h(slot, has): return ACTIVE_H if has and slot == 0 else ROW_H
def card_top(slot, has):
    y = HEADER_H + GAP
    for i in range(slot): y += card_h(i, has) + GAP
    return y
def total_h(n, mx, has):
    s = shown(n, mx)
    if s == 0: return 0
    bottom = card_top(s, has) - GAP
    return bottom + GAP + MORE_H if hidden(n, mx) > 0 else bottom


def font(px):
    for f in ("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",):
        if os.path.exists(f): return ImageFont.truetype(f, px)
    return ImageFont.load_default()


def draw_stack(img, ox, oy, doubles, active, mx=4, dist=True):
    """doubles: list of (hp fraction, seconds left, blocks away). active: index or -1 (none picked)."""
    d = ImageDraw.Draw(img, "RGBA")
    f = font(7 * S // 2 + 1)
    n, has = len(doubles), 0 <= active < len(doubles)

    def box(x, y, w, h, c): d.rectangle([ox + x * S, oy + y * S, ox + (x + w) * S - 1, oy + (y + h) * S - 1], fill=c)
    def txt(x, y, s, c): d.text((ox + x * S, oy + y * S), s, font=f, fill=c)
    def tw(s): return d.textlength(s, font=f) / S

    box(0, 0, W, HEADER_H, BG)
    txt(4, 1, "Real Doubles", EDGE)
    for slot in range(shown(n, mx)):
        i = source(slot, active, n)
        hp, sec, dst = doubles[i]
        top, h, act = card_top(slot, has), card_h(slot, has), has and slot == 0
        box(0, top, W, h, BG)
        if act:
            box(0, top, W, 1, GOLD); box(0, top + h - 1, W, 1, GOLD); box(0, top, 1, h, GOLD); box(W - 1, top, 1, h, GOLD)
        ty = top + (3 if act else 2)
        txt(4, ty, "Double %d" % (i + 1), GOLD if act else TEXT)
        t = "%d:%02d" % (sec // 60, sec % 60)
        tx = W - 4 - tw(t)
        txt(tx, ty, t, DIM)
        if dist: txt(tx - 6 - tw("%dm" % dst), ty, "%dm" % dst, DIM)
        bx, bw = 4, W - 8
        hy, hh, tyy, th = (top + 14, 5, top + 21, 3) if act else (top + 11, 2, top + 14, 1)
        box(bx, hy, bw, hh, TRACK); box(bx, hy, round(bw * hp), hh, HP_GOOD if hp > .5 else HP_MID if hp > .25 else HP_LOW)
        box(bx, tyy, bw, th, TRACK); box(bx, tyy, round(bw * min(1, sec / 60.0)), th, TIME)
    if hidden(n, mx) > 0:
        top = card_top(shown(n, mx), has)
        box(0, top, W, MORE_H, BG)
        txt(4, top + 1, "+%d more" % hidden(n, mx), DIM)


def main():
    root = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
    pw, ph = (W + 14) * S, 118 * S
    cols = [
        ("One double, none picked", [(0.8, 52, 6)], -1, 4),
        ("Three doubles, Double 2 picked", [(1.0, 58, 5), (0.55, 40, 9), (0.2, 21, 14)], 1, 4),
        ("Seven doubles, Double 6 picked, 4 shown", [(1.0, 58, 5), (0.9, 50, 7), (0.7, 44, 6), (0.55, 40, 9), (0.4, 33, 12), (0.2, 21, 14), (0.8, 12, 22)], 5, 4),
    ]
    img = Image.new("RGB", (pw * len(cols), ph + 14 * S), (34, 52, 40))
    d = ImageDraw.Draw(img)
    cap = font(3 * S + 2)
    for k, (title, rows, act, mx) in enumerate(cols):
        x0 = k * pw
        d.rectangle([x0, 0, x0 + pw - 1, ph + 14 * S], fill=(34 + 8 * (k % 2), 52, 40 + 6 * (k % 2)))
        d.text((x0 + 7 * S, 3 * S), title, font=cap, fill=(230, 240, 230))
        draw_stack(img, x0 + 7 * S, 12 * S, rows, act, mx)
    out = os.path.join(root, "docs", "doubles_ui_preview.png")
    img.save(out)
    print(out, img.size)


if __name__ == "__main__":
    main()
