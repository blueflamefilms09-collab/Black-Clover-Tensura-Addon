#!/usr/bin/env python3
"""Why: the Beast Will Core (item nusmp:beast_will_core) is a solid 3D orb in hand, on the ground and in frames, like the rank Mana Cores (docs/solid_cores.md). It reuses the sphere / skin / mesh
writer of tools/gen_mana_core_models.py (same dialect, same outward winding, same glow shell).

The look: a rough, dark stone (24 x 12 sphere, radius jittered a few percent, facets of different grey) split by a network of fissures that glow white-hot; the fissures are widest and brightest round
the equator (the 'heart'). The skin is NEUTRAL (grey stone, near-white fissures): the WILL's colour is applied by the renderer as a vertex tint (beastwill/BeastCoreLook, the tint of the will's data
file), so one skin + one glow skin serve all 9 wills and any data-pack will; the glow pass (additive, a 1.015 shell) is tinted the same way and pulses. The inventory icon is the old flat sprite, repainted
solid and neutral by gen_core_icon.py and tinted per will by an item colour handler.

    python3 -B tools/beastwill/gen_beast_core_model.py [--sheet out.png]

Writes under src/main/resources/assets/nusmp: weapon_meshes/beast_will_core.txt, textures/entity/weapon/beast_will_core.png (+ _glow.png), models/item/beast_will_core.json (builtin/entity, the display
transforms of the rank cores), and appends "beast_will_core": "handitem" to weapon_classes.json. Cost per core in hand: 288 body quads + 288 glow quads = 576 quads, no allocation.
"""
import json
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
import gen_mana_core_models as g  # noqa: E402

CID = "beast_will_core"


def rs():
    return np.random.RandomState(4501)


def field():
    """(stone colour, glow map) as float arrays (TH, TW, 3)."""
    r = rs()
    d = g.directions(g.TW, g.TH)
    sites = r.normal(size=(52, 3))
    sites /= np.linalg.norm(sites, axis=1, keepdims=True)
    dist = 1.0 - d.reshape(-1, 3) @ sites.T                          # (n, sites): 0 = on the site
    order = np.argsort(dist, axis=1)
    f1 = np.take_along_axis(dist, order[:, :1], 1)[:, 0]
    f2 = np.take_along_axis(dist, order[:, 1:2], 1)[:, 0]
    cell = order[:, 0].reshape(g.TH, g.TW)
    edge = ((f2 - f1) / np.maximum(f1 + f2, 1e-9)).reshape(g.TH, g.TW)   # 0 on a cell border, grows to the cell middle
    heart = np.exp(-(d[..., 1] ** 2) / 0.22)                              # 1 on the equator, 0 at the poles
    width = 0.075 * (1.0 + 1.2 * heart)
    crack = np.clip(1.0 - edge / width, 0, 1) ** 1.6
    facet = 0.24 + 0.22 * r.uniform(size=52)[cell]
    noise = sum(np.sin((d @ r.normal(size=3)) * f + r.uniform(0, 6.28)) for f in (9, 17, 31, 47)) / 4.0
    lum = np.clip(facet + 0.045 * noise, 0.08, 0.5)
    rim = np.clip(1.0 - edge / (width * 3.2), 0, 1)                       # a dark rim round each fissure
    lum = lum * (1.0 - 0.45 * rim)
    stone = lum[..., None] * np.array([1.0, 0.97, 0.93])
    hot = np.array([1.0, 0.98, 0.94])
    skin = stone * (1 - crack[..., None]) + hot * crack[..., None]
    gl = np.clip(crack * (0.55 + 0.45 * heart), 0, 1)
    halo = gl.copy()
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
        halo = np.maximum(halo, 0.45 * np.roll(np.roll(gl, dx, 1), dy, 0))
    return np.clip(skin, 0, 1), np.clip(halo, 0, 1)[..., None] * np.ones(3)


def radius_field():
    r = np.random.RandomState(77)
    f = r.normal(size=(g.RING + 1, g.SEG))
    for _ in range(2):
        f = (f + np.roll(f, 1, 1) + np.roll(f, -1, 1)) / 3.0
    f = f / np.abs(f).max() * 0.07
    f[0, :] = f[0, :].mean()
    f[-1, :] = f[-1, :].mean()
    return 1.0 + f


def write_classes():
    return g.add_classes([CID])


def main():
    sheet = sys.argv[sys.argv.index("--sheet") + 1] if "--sheet" in sys.argv else ""
    skin, glow = field()
    g.RANKS["beastwill"] = (None, 1.0, "D8C8FF", 0.08, 1.0)               # runtime only: the glow colour (replaced per will by the renderer), pulse speed, full radius jitter
    tex = os.path.join(g.ASSETS, "textures", "entity", "weapon")
    os.makedirs(tex, exist_ok=True)
    g.to_img(skin).save(os.path.join(tex, CID + ".png"), optimize=True)
    g.to_img(glow).save(os.path.join(tex, CID + "_glow.png"), optimize=True)
    text = g.mesh_text(CID, "beastwill", radius_field(), True).replace("tools/gen_mana_core_models.py", "tools/beastwill/gen_beast_core_model.py")
    g.write(os.path.join(g.ASSETS, "weapon_meshes", CID + ".txt"), text)
    model = {"parent": "minecraft:builtin/entity", "gui_light": "front", "textures": {"particle": "nusmp:item/" + CID}, "display": g.DISPLAY}
    g.write(os.path.join(g.ASSETS, "models", "item", CID + ".json"), json.dumps(model, indent=2) + "\n")
    print("classes added:", write_classes())
    if sheet:
        from PIL import Image
        tiles = []
        for tint in (0x7E54D6, 0x7FB5FF, 0xE0A53A, 0x4B3F8C):
            body = np.array([(255 - (255 - ((tint >> s) & 255)) * 0.6) / 255 for s in (16, 8, 0)])
            o = g.render_orb(skin * body, 128, spin=0.3)
            gl = g.render_orb(glow * np.array([((tint >> s) & 255) / 255 for s in (16, 8, 0)]), 128, light=(0, 0, 1), spin=0.3)[..., :3]
            tiles.append(g.to_img(np.concatenate([np.clip(o[..., :3] + gl, 0, 1), o[..., 3:]], -1)))
        im = Image.new("RGB", (128 * len(tiles), 128), (96, 96, 100))
        for i, t in enumerate(tiles):
            im.paste(t, (i * 128, 0), t)
        im.save(sheet)


if __name__ == "__main__":
    main()
