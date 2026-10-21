"""The effect tables of the four God gear relics of Wave102: assets/nusmp/visual_upgrade/<id>.json, the same data tables the 1.102.0 God worn upgrade uses (read by
relics/upgrade/UpgradeFxData.java, ambient layers played by client/relics/fx/UpgradeFxClient.java, the act visuals by relics/upgrade/UpgradeActFx.java: both are by relic id, so a
new relic needs only its file). Every particle is one of the mod's own (the token `fx`, or `nusmp:<id>` of data/nusmp/particle_catalog.json), never a vanilla particle.

Positions are the wearer's local frame in blocks: x to the wearer's left, y up from the feet, z behind. Each relic has nine ambient layers (motes, a halo, an energy line, a ground
cast, shoulder / hand glow, orbiting sparks, a trail ...) and one or two visuals for each of its acts (the act key is the slug of the act's name).
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "visual_upgrade")


def hx(c):
    return "%06X" % c


def motes(name, box, particle, color, rate=5, speed=0.01, size=1.0, mx=2):
    return {"name": name, "kind": "motes", "particle": particle, "color": hx(color), "size": size, "per_second": rate, "box": box, "speed": speed, "max_per_tick": mx}


def ring(name, center, radius, particle, color, rate=8, spin=0.25, plane="flat", size=1.0, mx=3, kind="ring"):
    return {"name": name, "kind": kind, "particle": particle, "color": hx(color), "size": size, "per_second": rate, "center": center, "radius": radius, "spin": spin, "plane": plane, "max_per_tick": mx}


def line(name, a, b, particle, color, rate=6, speed=0.0, size=1.0, mx=2):
    return {"name": name, "kind": "line", "particle": particle, "color": hx(color), "size": size, "per_second": rate, "from": a, "to": b, "speed": speed, "max_per_tick": mx}


def ground(name, radius, particle, color, rate=5, size=1.0, mx=2, center=(0, 0, 0)):
    return {"name": name, "kind": "ground", "particle": particle, "color": hx(color), "size": size, "per_second": rate, "center": list(center), "radius": radius, "max_per_tick": mx}


def trail(name, center, particle, color, rate=8, size=1.0, spread=0.5, mx=3):
    return {"name": name, "kind": "trail", "particle": particle, "color": hx(color), "size": size, "per_second": rate, "center": center, "radius": spread, "max_per_tick": mx}


def act(shape, color, duration=24, power=3.0, at="feet", particle="nusmp:mote", count=36, spread=1.2, radius=2.0, height=1.2):
    return {"shape": shape, "color": hx(color), "duration": duration, "power": power, "at": at, "particle": particle, "count": count, "spread": spread, "radius": radius, "height": height}


TABLES = {
    "perfect_susanoo_plate": dict(grade="GOD", layers=[
        motes("chakra_motes", [-0.5, 0.3, -0.3, 0.5, 1.8, 0.3], "nusmp:aura_wisp", 0xB890FF, 8, 0.012),
        motes("violet_dust", [-0.45, 0.5, -0.25, 0.45, 1.5, 0.25], "fx", 0x6A4ABA, 6, 0.006, 0.8),
        ring("ribcage_halo", [0, 1.35, 0.1], 0.6, "nusmp:halo_ring", 0xB890FF, 7, 0.16, "vertical"),
        ring("susanoo_circle", [0, 0.9, 0], 1.0, "nusmp:rune_eye", 0xB890FF, 7, -0.18, "flat", mx=2),
        line("collar_edge", [-0.35, 1.55, -0.25], [0.35, 1.55, -0.25], "nusmp:spark", 0xD8C070, 4),
        ground("chakra_floor", 1.4, "nusmp:smoke_wisp", 0x6A4ABA, 6, 1.0),
        motes("shoulder_glow", [0.25, 1.4, -0.15, 0.6, 1.8, 0.15], "nusmp:mote_soft", 0xB890FF, 3, 0.01),
        ring("rib_sparks", [0, 1.1, 0], 0.8, "nusmp:glint", 0xD8C070, 5, 0.3, "flat", kind="orbit"),
        trail("armour_wake", [0, 0.8, 0.35], "nusmp:aura_wisp", 0x6A4ABA, 6, 1.0, 0.6)],
        acts={"susanoo_guard": [act("SEAL_TRINITY", 0xB890FF, 30, 4.0, "feet", "nusmp:aura_wisp", 40, 1.4), act("LIGHT_FLARE", 0xD8C070, 16, 2.5, "feet", "nusmp:glint", 24, 0.8, 3.0)],
              "totsuka_verdict": [act("MAGIC_CIRCLE_EXPLOSION", 0xB890FF, 40, 7.0, "feet", "nusmp:rune_eye", 80, 3.0), act("SEAL_CHAINS", 0x6A4ABA, 34, 5.0, "feet", "nusmp:aura_wisp", 50, 2.4)]}),
    "joyboys_straw_hat": dict(grade="GOD", layers=[
        motes("sun_motes", [-0.4, 1.4, -0.1, 0.4, 2.1, 0.5], "nusmp:glint", 0xFFD870, 8, 0.014),
        motes("straw_dust", [-0.5, 1.6, -0.2, 0.5, 2.0, 0.3], "fx", 0xE8C878, 6, 0.006, 0.8),
        ring("sun_halo", [0, 1.95, 0.45], 0.6, "nusmp:halo_ring", 0xFFD870, 9, 0.2, "vertical"),
        ring("drum_circle", [0, 0.9, 0], 1.0, "nusmp:rune_sol", 0xFFD870, 7, 0.14, "flat", mx=2),
        line("brim_edge", [-0.55, 1.78, -0.1], [0.55, 1.78, -0.1], "nusmp:mote", 0xFFF0A0, 5),
        ground("laughing_floor", 1.3, "nusmp:mote_soft", 0xE8C878, 6, 1.0),
        motes("rubber_sparks", [0.3, 0.8, -0.15, 0.6, 1.4, 0.15], "nusmp:spark", 0xC03030, 3, 0.01),
        ring("sun_orbit", [0, 1.7, 0], 0.7, "nusmp:star4", 0xFFD870, 5, 0.35, "flat", kind="orbit"),
        trail("wind_wake", [0, 0.9, 0.4], "nusmp:wind_swirl", 0xFFF0A0, 6, 1.0, 0.7)],
        acts={"rubber_dash": [act("WIND_GALE", 0xFFF0A0, 22, 3.0, "feet", "nusmp:wind_swirl", 40, 1.2), act("WIND_SLASH", 0xFFD870, 16, 2.5, "aim", "nusmp:mote", 24, 0.6, 5.0)],
              "sun_gods_laughter": [act("LIGHT_FLARE", 0xFFD870, 30, 6.0, "feet", "nusmp:glint", 80, 2.8), act("MAGIC_CIRCLE_EXPLOSION", 0xE8C878, 38, 6.0, "feet", "nusmp:rune_sol", 70, 2.8)]}),
    "greed_island_binder": dict(grade="GOD", layers=[
        motes("loose_pages", [0.1, 0.4, -0.3, 0.7, 1.3, 0.3], "nusmp:page", 0x80FFB0, 6, 0.01),
        motes("restricted_glow", [0.15, 0.5, -0.25, 0.65, 1.2, 0.25], "fx", 0x4ADA90, 6, 0.006, 0.8),
        ring("book_halo", [0.4, 0.9, -0.1], 0.35, "nusmp:halo_ring", 0x80FFB0, 6, 0.2, "vertical"),
        ring("slot_circle", [0, 0.9, 0], 1.0, "nusmp:rune_key", 0x80FFB0, 7, 0.16, "flat", mx=2),
        line("page_edge", [0.3, 1.0, -0.15], [0.55, 0.65, -0.15], "nusmp:spark", 0xE0C060, 4),
        ground("island_floor", 1.3, "nusmp:mote_soft", 0x4ADA90, 6, 1.0),
        motes("card_sparks", [0.2, 0.7, -0.2, 0.7, 1.4, 0.2], "nusmp:glint", 0xE0C060, 3, 0.01),
        ring("card_orbit", [0.4, 0.9, -0.1], 0.6, "nusmp:card", 0x80FFB0, 5, 0.4, "flat", kind="orbit"),
        trail("ink_wake", [0.3, 0.7, 0.3], "nusmp:ink_splat", 0x4ADA90, 5, 1.0, 0.5)],
        acts={"detect_and_mark": [act("MAGIC_CIRCLE", 0x80FFB0, 26, 3.0, "feet", "nusmp:rune_key", 40, 1.4), act("LIGHT_FLARE", 0xE0C060, 14, 2.0, "eye", "nusmp:glint", 20, 0.6, 3.0)],
              "restricted_slot_reflect": [act("SEAL_TRINITY", 0x80FFB0, 30, 4.0, "feet", "nusmp:card", 44, 1.8), act("WIND_RING", 0xE0C060, 20, 3.0, "feet", "nusmp:page", 40, 1.6)],
              "card_storm": [act("MAGIC_CIRCLE_EXPLOSION", 0x80FFB0, 42, 7.0, "feet", "nusmp:card", 90, 3.2), act("SEAL_TRINITY", 0xE0C060, 36, 5.0, "feet", "nusmp:page", 60, 2.6)]}),
    "requiem_ladybug_brooch": dict(grade="GOD", layers=[
        motes("golden_motes", [-0.4, 0.8, -0.2, 0.5, 1.7, 0.2], "nusmp:glint", 0xFFE860, 7, 0.01),
        motes("requiem_dust", [-0.4, 0.5, -0.2, 0.4, 1.5, 0.2], "fx", 0xE8C838, 6, 0.006, 0.8),
        ring("ladybug_halo", [0.1, 1.2, -0.2], 0.4, "nusmp:halo_ring", 0xFFE860, 7, 0.22, "vertical"),
        ring("zero_circle", [0, 0.9, 0], 1.0, "nusmp:ring_pulse", 0xFFE860, 6, -0.12, "flat", mx=2),
        line("pin_gleam", [0.1, 1.15, -0.3], [0.1, 0.85, -0.3], "nusmp:spark", 0xFFF0A0, 4),
        ground("golden_floor", 1.3, "nusmp:mote_soft", 0xE8C838, 6, 1.0),
        motes("brooch_glow", [-0.05, 1.05, -0.3, 0.3, 1.45, -0.15], "nusmp:star4", 0xFFE860, 3, 0.004),
        ring("wing_orbit", [0, 1.1, 0], 0.7, "nusmp:feather_fire", 0xFFE860, 4, 0.3, "flat", kind="orbit"),
        trail("experience_wake", [0, 0.8, 0.35], "nusmp:mote", 0xFFE860, 6, 1.0, 0.5)],
        acts={"return_to_zero": [act("SEAL_TRINITY", 0xFFE860, 30, 4.0, "feet", "nusmp:ring_pulse", 44, 1.4), act("LIGHT_FLARE", 0xFFF0A0, 16, 2.5, "feet", "nusmp:glint", 24, 0.8, 3.0)],
              "golden_experience": [act("MAGIC_CIRCLE", 0xFFE860, 34, 5.0, "feet", "nusmp:glint", 60, 2.4), act("WIND_RING", 0xE8C838, 24, 3.5, "feet", "nusmp:mote", 40, 2.0)]}),
}


def write_all():
    os.makedirs(OUT, exist_ok=True)
    for rid, t in TABLES.items():
        data = {"id": rid, "grade": t["grade"], "layers": t["layers"], "acts": t["acts"]}
        with open(os.path.join(OUT, rid + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=1)
            f.write("\n")
    print("wrote %d effect tables" % len(TABLES))


if __name__ == "__main__":
    write_all()
