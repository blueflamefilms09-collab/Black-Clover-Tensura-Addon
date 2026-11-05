"""3.56.0: the 30 icons of the Dragon Ball skills (textures/skill/db_<id>.png and db_<id>_unique.png), 32x32, one per skill, no two alike.
Each character has two colours and a number of stars (1..7, as on the dragon balls). Unique = gold frame, Ultimate = black frame lit in the character's colour with a halo.
    python tools/gen_db_icons.py
Deterministic; refuses to finish if two icons are identical."""
import hashlib, math, os
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "skill")
S = 256

# id: (main colour, second colour, stars)
CHARS = {
    "db_goku": ((255, 140, 30), (40, 120, 255), 4), "db_vegeta": ((40, 90, 230), (255, 215, 60), 3), "db_gohan": ((140, 60, 200), (230, 50, 50), 5),
    "db_piccolo": ((60, 170, 70), (255, 130, 30), 1), "db_krillin": ((255, 150, 40), (250, 250, 250), 2), "db_android18": ((250, 215, 80), (60, 120, 220), 6),
    "db_android17": ((40, 40, 50), (70, 220, 110), 7), "db_goten": ((255, 140, 30), (90, 180, 255), 1), "db_trunks": ((150, 90, 220), (210, 190, 255), 2),
    "db_tien": ((70, 180, 90), (255, 235, 90), 3), "db_yamcha": ((255, 140, 30), (30, 30, 40), 5), "db_chiaotzu": ((245, 245, 250), (225, 60, 60), 6),
    "db_roshi": ((70, 190, 170), (255, 150, 190), 7), "db_future_trunks": ((190, 195, 215), (140, 70, 210), 4), "db_bulma": ((60, 210, 220), (255, 130, 190), 2),
}


def star(d, cx, cy, r, fill):
    pts = []
    for i in range(10):
        a = -math.pi / 2 + i * math.pi / 5
        rr = r if i % 2 == 0 else r * 0.42
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    d.polygon(pts, fill=fill)


def icon(main, second, stars, ultimate, idx):
    img = Image.new("RGBA", (S, S), (14, 14, 22, 255))
    d = ImageDraw.Draw(img)
    # background: vertical gradient of the second colour
    for y in range(S):
        t = y / S
        d.line([(0, y), (S, y)], fill=tuple(int(second[i] * (0.18 + 0.30 * t) + 10) for i in range(3)) + (255,))
    # halo
    halo = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    hd = ImageDraw.Draw(halo)
    hd.ellipse((28, 28, S - 28, S - 28), fill=main + ((150 if ultimate else 90),))
    img.alpha_composite(halo.filter(ImageFilter.GaussianBlur(18)))
    d = ImageDraw.Draw(img)
    # the ball
    d.ellipse((50, 50, S - 50, S - 50), fill=tuple(int(c * 0.55) for c in main) + (255,))
    d.ellipse((58, 58, S - 66, S - 66), fill=main + (255,))
    d.ellipse((78, 74, 122, 112), fill=tuple(min(255, c + 90) for c in main) + (255,))
    # the stars on the ball
    pos = [(128, 128)] if stars == 1 else [(128 + math.cos(2 * math.pi * k / stars - math.pi / 2) * 38, 128 + math.sin(2 * math.pi * k / stars - math.pi / 2) * 38) for k in range(stars)]
    for (x, y) in pos:
        star(d, x, y, 17 if stars > 3 else 22, second + (255,))
    # a small mark so a Unique and its Ultimate also differ in the middle: the Ultimate gets a ring
    if ultimate:
        d.ellipse((46, 46, S - 46, S - 46), outline=second + (255,), width=6)
    # frame
    frame = (232, 190, 70) if not ultimate else tuple(int(c * 0.5) + 20 for c in main)
    d.rectangle((0, 0, S - 1, S - 1), outline=frame + (255,), width=14)
    d.rectangle((14, 14, S - 15, S - 15), outline=(0, 0, 0, 255), width=3)
    # per-icon tag so two icons can never be identical: a tiny notch pattern on the frame
    for b in range(6):
        if (idx >> b) & 1:
            d.rectangle((20 + b * 30, 2, 34 + b * 30, 10), fill=(255, 255, 255, 255))
    return img.resize((32, 32), Image.LANCZOS)


def main():
    os.makedirs(OUT, exist_ok=True)
    seen = {}
    for idx, (cid, (m, s, n)) in enumerate(CHARS.items()):
        for ult, name in ((False, cid + "_unique"), (True, cid)):
            im = icon(m, s, n, ult, idx * 2 + (1 if ult else 0) + 1)
            h = hashlib.md5(im.tobytes()).hexdigest()
            assert h not in seen, name + " is identical to " + seen[h]
            seen[h] = name
            im.save(os.path.join(OUT, name + ".png"))
    print(len(seen), "icons written")


if __name__ == "__main__":
    main()
