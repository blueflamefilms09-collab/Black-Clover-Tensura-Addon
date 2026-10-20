"""The effect tables of the God worn relics: assets/nusmp/visual_upgrade/<id>.json (read by relics/upgrade/UpgradeFxData.java, played by client/relics/fx/UpgradeFxClient.java for the
ambient layers and by relics/upgrade/UpgradeActFx.java for the acts).

Positions are the wearer's local frame in blocks: x to the wearer's left, y up from the feet, z behind. Every relic has at least eight ambient layers (idle motes, a halo ring, an
energy line along its signature edge, a ground cast, shoulder / hand glow, orbiting sparks, a stride trail ...) and an effect of its own for each of its acts.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import items as IT  # noqa: E402
import metrics as M  # noqa: E402


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


def act(shape, color, duration=24, power=3.0, at="feet", particle="minecraft:end_rod", count=36, spread=1.2, radius=2.0, height=1.2):
    return {"shape": shape, "color": hx(color), "duration": duration, "power": power, "at": at, "particle": particle, "count": count, "spread": spread, "radius": radius, "height": height}


def build_all():
    P = {}
    # ------------------------------------------------------------------ BACK
    P["flammes_founder_cloak"] = dict(color=0xFFF0C0, layers=[
        motes("starlight", [-0.5, 0.4, 0.25, 0.5, 1.9, 0.8], "minecraft:end_rod", 0xFFF0C0, 7, 0.012),
        motes("gold_dust", [-0.45, 0.3, 0.3, 0.45, 1.6, 0.7], "dust", 0xC8A860, 6, 0.006, 0.8),
        ring("founders_halo", [0, 1.28, 0.42], 0.55, "minecraft:end_rod", 0xFFF0C0, 9, 0.12, "vertical"),
        ring("mana_circle", [0, 0.9, 0], 0.9, "minecraft:enchant", 0xFFF0C0, 7, 0.2, "flat", mx=2),
        line("hem_gleam", [-0.45, 0.25, 0.45], [0.45, 0.25, 0.45], "minecraft:end_rod", 0xFFF0C0, 4),
        ground("nameless_circle", 1.1, "dust", 0xC8A860, 5, 0.9),
        motes("shoulder_stars", [0.25, 1.55, -0.1, 0.55, 1.8, 0.2], "minecraft:glow", 0xFFF0C0, 3, 0.01),
        ring("orbit_sparks", [0, 1.1, 0], 0.75, "minecraft:glow", 0xFFF0C0, 5, 0.35, "flat", kind="orbit"),
        trail("cape_dust", [0, 0.9, 0.45], "minecraft:end_rod", 0xFFF0C0, 6)],
        acts={"the_right_spell": [act("MAGIC_CIRCLE", 0xFFF0C0, 30, 4.0, "feet", "minecraft:end_rod", 40, 1.6), act("LIGHT_FLARE", 0xFFF0C0, 18, 2.5, "aim", "minecraft:glow", 30, 0.8, 8.0)],
              "founders_magic": [act("MAGIC_CIRCLE_EXPLOSION", 0xFFF0C0, 40, 7.0, "feet", "minecraft:end_rod", 90, 3.0), act("SEAL_TRINITY", 0xC8A860, 36, 5.0, "feet", "minecraft:enchant", 60, 2.6)]})
    P["mantle_of_the_overlord"] = dict(color=0xFF5A7A, layers=[
        motes("courteous_gloom", [-0.5, 0.3, 0.25, 0.5, 1.8, 0.8], "minecraft:smoke", 0xFF5A7A, 6, 0.01),
        motes("crimson_dust", [-0.45, 0.4, 0.3, 0.45, 1.6, 0.7], "dust", 0x9A2A3A, 7, 0.006),
        ring("skull_halo", [0, 1.35, 0.5], 0.5, "minecraft:soul_fire_flame", 0xFF5A7A, 7, -0.18, "vertical"),
        ring("domination_circle", [0, 0.95, 0], 1.05, "dust", 0xFF5A7A, 8, 0.15, "flat"),
        line("collar_edge", [-0.35, 1.9, 0.38], [0.35, 1.9, 0.38], "minecraft:witch", 0xFF5A7A, 4),
        ground("overlord_floor", 1.4, "dust", 0x9A2A3A, 6, 1.0),
        motes("shoulder_embers", [0.3, 1.5, -0.1, 0.6, 1.8, 0.2], "minecraft:flame", 0xFF5A7A, 3, 0.01),
        ring("watching_eyes", [0, 1.2, 0], 0.85, "minecraft:witch", 0xFF5A7A, 5, 0.3, "flat", kind="orbit"),
        trail("train_smoke", [0, 0.6, 0.5], "minecraft:large_smoke", 0x9A2A3A, 6, 1.0, 0.7)],
        acts={"overlords_gaze": [act("DEVIL_CIRCLE", 0xFF5A7A, 26, 4.0, "feet", "minecraft:witch", 40, 1.5), act("ANTI_MAGIC_SLASH", 0x9A2A3A, 16, 3.0, "aim", "minecraft:soul_fire_flame", 24, 1.0, 6.0)],
              "absolute_domination": [act("SEAL_CHAINS", 0xFF5A7A, 40, 6.0, "feet", "minecraft:witch", 90, 3.0), act("NIHILITY_ZONE", 0x9A2A3A, 40, 6.0, "feet", "minecraft:smoke", 70, 4.0)]})
    P["pactbreaker_shroud"] = dict(color=0xD8A0FF, layers=[
        motes("void_motes", [-0.5, 0.3, 0.25, 0.5, 1.8, 0.8], "minecraft:portal", 0xD8A0FF, 8, 0.008),
        motes("torn_pages", [-0.55, 0.4, 0.3, 0.55, 1.5, 0.8], "dust", 0xA04AFF, 7, 0.01, 0.9),
        ring("broken_seal", [0, 1.15, 0.5], 0.5, "minecraft:dragon_breath", 0xD8A0FF, 7, 0.1, "vertical"),
        ring("pact_ring", [0, 0.8, 0], 1.0, "minecraft:enchant", 0xD8A0FF, 8, -0.2, "flat"),
        line("fracture_line", [0.1, 1.5, 0.45], [-0.12, 0.35, 0.5], "minecraft:witch", 0xD8A0FF, 7),
        ground("unbound_floor", 1.3, "minecraft:reverse_portal", 0xA04AFF, 5),
        motes("snapped_chain_sparks", [0.3, 1.0, 0.0, 0.62, 1.5, 0.3], "minecraft:electric_spark", 0xD8A0FF, 3, 0.0),
        ring("pages_orbit", [0, 1.1, 0], 0.8, "minecraft:enchant", 0xA04AFF, 5, 0.32, "flat", kind="orbit"),
        trail("shroud_wake", [0, 0.8, 0.5], "minecraft:dragon_breath", 0xA04AFF, 6, 1.0, 0.8)],
        acts={"break_the_pact": [act("SEAL_TRINITY", 0xD8A0FF, 34, 5.0, "feet", "minecraft:enchant", 70, 2.4), act("ANTI_MAGIC_SLASH", 0xA04AFF, 16, 3.0, "eye", "minecraft:witch", 30, 1.2, 3.0)],
              "void_step": [act("SPATIAL_RIFT", 0xD8A0FF, 20, 3.0, "feet", "minecraft:portal", 60, 1.0), act("SPATIAL_RIFT", 0xA04AFF, 20, 3.0, "aim", "minecraft:reverse_portal", 60, 1.0, 10.0)]})
    P["shadow_monarchs_mantle"] = dict(color=0xA060FF, layers=[
        motes("rising_shadow", [-0.5, 0.2, 0.2, 0.5, 2.0, 0.8], "minecraft:soul_fire_flame", 0xA060FF, 8, 0.02),
        motes("violet_mist", [-0.6, 0.2, 0.3, 0.6, 1.2, 0.9], "dust", 0x5A2A9A, 9, 0.008, 1.2),
        ring("crown_of_flames", [0, 1.95, 0.35], 0.4, "minecraft:soul_fire_flame", 0xA060FF, 8, 0.25, "vertical"),
        ring("domain_ring", [0, 0.7, 0], 1.25, "dust", 0x5A2A9A, 9, 0.12, "flat"),
        line("blade_edge", [0.4, 1.6, 0.0], [0.7, 2.1, 0.2], "minecraft:witch", 0xA060FF, 6),
        ground("monarch_shadow", 1.7, "minecraft:smoke", 0x5A2A9A, 8, 1.2, 3),
        motes("shoulder_flames", [0.28, 1.5, -0.1, 0.62, 2.0, 0.2], "minecraft:soul", 0xA060FF, 4, 0.02),
        ring("shade_soldiers", [0, 1.0, 0], 1.0, "minecraft:soul_fire_flame", 0xA060FF, 5, -0.28, "flat", kind="orbit"),
        trail("shadow_train", [0, 0.7, 0.5], "minecraft:large_smoke", 0x5A2A9A, 8, 1.0, 0.8)],
        acts={"arise": [act("SHADOW_UNITE", 0xA060FF, 36, 5.0, "feet", "minecraft:soul_fire_flame", 80, 2.5), act("SHADOW_POOL", 0x5A2A9A, 40, 5.0, "feet", "minecraft:soul", 40, 3.0)],
              "monarchs_domain": [act("NIHILITY_ZONE", 0xA060FF, 50, 8.0, "feet", "minecraft:smoke", 100, 5.0), act("SHADOW_POOL", 0x5A2A9A, 50, 9.0, "feet", "minecraft:soul_fire_flame", 90, 5.0)]})
    P["shadows_slime_cloak"] = dict(color=0x8A70FF, layers=[
        motes("slime_bubbles", [-0.5, 0.2, 0.25, 0.5, 1.7, 0.8], "minecraft:glow_squid_ink", 0x8A70FF, 5, 0.012),
        motes("drips", [-0.5, 0.3, 0.3, 0.5, 0.9, 0.7], "dust", 0x8A70FF, 8, -0.01, 0.9),
        ring("slime_halo", [0, 1.2, 0.45], 0.45, "minecraft:glow", 0x8A70FF, 6, 0.15, "vertical"),
        ring("jelly_ring", [0, 0.8, 0], 0.9, "dust", 0x4A3AA8, 7, 0.18, "flat"),
        line("blade_pseudopod", [-0.38, 0.75, -0.12], [-0.52, 0.2, -0.2], "minecraft:glow", 0x8A70FF, 7),
        ground("puddle", 1.0, "dust", 0x4A3AA8, 6, 1.0),
        motes("nucleus_glow", [-0.15, 0.8, 0.3, 0.15, 1.2, 0.6], "minecraft:end_rod", 0x8A70FF, 3, 0.004),
        ring("blob_companions", [0, 1.1, 0], 0.75, "minecraft:glow", 0x8A70FF, 4, 0.3, "flat", kind="orbit"),
        trail("goo_trail", [0, 0.1, 0.4], "dust", 0x8A70FF, 9, 1.0, 0.6)],
        acts={"slime_blade": [act("WIND_SLASH", 0x8A70FF, 18, 3.0, "aim", "minecraft:glow", 30, 0.6, 4.0), act("SLASH_BLADES", 0x4A3AA8, 14, 2.5, "eye", "minecraft:glow_squid_ink", 20, 0.6, 3.0)],
              "i_am_atomic": [act("MAGIC_CIRCLE_EXPLOSION", 0x8A70FF, 44, 8.0, "feet", "minecraft:end_rod", 110, 3.5), act("LIGHTNING_GOD", 0x8A70FF, 30, 6.0, "aim", "minecraft:glow", 60, 2.0, 12.0)]})
    P["silent_winter_mantle"] = dict(color=0xCFF4FF, layers=[
        motes("snowfall", [-0.6, 0.6, -0.2, 0.6, 2.0, 0.8], "minecraft:snowflake", 0xCFF4FF, 11, -0.01),
        motes("frost_dust", [-0.5, 0.4, 0.25, 0.5, 1.6, 0.7], "dust", 0x9FE3FF, 7, 0.004, 0.8),
        ring("ice_crown", [0, 1.7, 0], 0.55, "minecraft:end_rod", 0xCFF4FF, 8, 0.1, "flat"),
        ring("winter_circle", [0, 0.8, 0], 1.1, "minecraft:snowflake", 0x9FE3FF, 8, -0.15, "flat"),
        line("icicle_edge", [-0.5, 1.3, 0.1], [0.5, 1.3, 0.1], "minecraft:end_rod", 0xCFF4FF, 4),
        ground("hoarfrost", 1.6, "minecraft:snowflake", 0xCFF4FF, 9, 1.0, 3),
        motes("shoulder_frost", [0.3, 1.4, -0.1, 0.65, 1.8, 0.2], "minecraft:glow", 0x9FE3FF, 3, 0.0),
        ring("flake_orbit", [0, 1.1, 0], 0.85, "minecraft:snowflake", 0xCFF4FF, 6, 0.2, "flat", kind="orbit"),
        trail("breath_trail", [0, 0.9, 0.4], "minecraft:cloud", 0xCFF4FF, 6, 1.0, 0.6)],
        acts={"hoarfrost_shroud": [act("WATER_RING", 0x9FE3FF, 30, 4.0, "feet", "minecraft:snowflake", 70, 1.6), act("MIRROR_DOUBLE", 0xCFF4FF, 24, 3.0, "feet", "minecraft:end_rod", 30, 1.0)],
              "winters_breath": [act("WIND_RING", 0xCFF4FF, 28, 5.0, "eye", "minecraft:snowflake", 90, 2.4), act("WATER_BURST", 0x9FE3FF, 26, 4.0, "aim", "minecraft:cloud", 50, 1.6, 5.0)]})
    P["six_faced_dragons_scale"] = dict(color=0xFF9A4A, layers=[
        motes("dragon_embers", [-0.5, 0.4, 0.2, 0.5, 1.9, 0.8], "minecraft:flame", 0xFF9A4A, 8, 0.015),
        motes("gold_scales", [-0.5, 0.4, 0.3, 0.5, 1.5, 0.7], "dust", 0xD8B050, 7, 0.004),
        ring("six_faces_halo", [0, 1.7, 0.2], 0.62, "minecraft:flame", 0xFF9A4A, 8, 0.18, "flat"),
        ring("dragon_circle", [0, 0.8, 0], 1.2, "dust", 0xFF9A4A, 8, 0.15, "flat"),
        line("spine_ridge", [0, 1.7, 0.35], [0, 0.4, 0.5], "minecraft:flame", 0xFF9A4A, 7, 0.01),
        ground("heat_haze", 1.5, "minecraft:flame", 0xFF9A4A, 6, 1.0, 3),
        motes("shoulder_smoulder", [0.3, 1.5, -0.1, 0.65, 1.9, 0.2], "minecraft:lava", 0xFF9A4A, 2, 0.0),
        ring("ember_orbit", [0, 1.0, 0], 0.85, "minecraft:flame", 0xFF9A4A, 5, 0.3, "flat", kind="orbit"),
        trail("fire_wake", [0, 0.5, 0.45], "minecraft:smoke", 0xFF9A4A, 7, 1.0, 0.7)],
        acts={"dragons_pride": [act("FIRE_PILLAR", 0xFF9A4A, 30, 3.0, "feet", "minecraft:flame", 60, 1.4), act("SEAL_TRINITY", 0xD8B050, 28, 3.5, "feet", "minecraft:flame", 30, 2.0)],
              "six_faces": [act("FIRE_WILD", 0xFF9A4A, 40, 7.0, "feet", "minecraft:flame", 110, 3.5), act("WIND_TORNADO", 0xFF9A4A, 36, 5.0, "feet", "minecraft:lava", 40, 2.5), act("FIRE_BURST", 0xD8B050, 24, 5.0, "eye", "minecraft:flame", 60, 2.0)]})
    P["urek_mazinos_chain_mantle"] = dict(color=0xFFD860, layers=[
        motes("iron_sparks", [-0.5, 0.4, 0.25, 0.5, 1.6, 0.8], "minecraft:electric_spark", 0xFFD860, 5, 0.0),
        motes("gold_dust", [-0.5, 0.2, 0.3, 0.5, 1.4, 0.7], "dust", 0xC8A030, 7, 0.003),
        ring("tower_halo", [0, 1.4, 0.5], 0.5, "minecraft:wax_on", 0xFFD860, 6, 0.1, "vertical"),
        ring("shackle_ring", [0, 0.9, 0], 1.05, "dust", 0xC8A030, 8, 0.12, "flat"),
        line("chain_curtain", [-0.45, 1.5, 0.45], [0.45, 1.5, 0.45], "minecraft:electric_spark", 0xFFD860, 5),
        ground("tower_foundation", 1.3, "dust", 0xC8A030, 6, 1.0),
        motes("shoulder_links", [0.3, 1.5, -0.1, 0.65, 1.7, 0.2], "minecraft:wax_on", 0xFFD860, 3, 0.0),
        ring("chain_orbit", [0, 1.1, 0], 0.85, "minecraft:electric_spark", 0xFFD860, 4, 0.22, "flat", kind="orbit"),
        trail("drag_sparks", [0, 0.1, 0.5], "minecraft:crit", 0xFFD860, 6, 1.0, 0.8)],
        acts={"chain_down": [act("SEAL_CHAINS", 0xFFD860, 34, 5.0, "feet", "minecraft:electric_spark", 70, 2.4), act("THREAD_LINE", 0xC8A030, 20, 3.0, "aim", "minecraft:crit", 30, 0.8, 7.0)],
              "unbreakable_tower": [act("EARTH_SPIKES", 0xC8A030, 36, 5.0, "feet", "minecraft:crit", 60, 3.0), act("MAGIC_CIRCLE", 0xFFD860, 40, 6.0, "feet", "minecraft:wax_on", 50, 3.0)]})
    # ------------------------------------------------------------------ BELT
    P["excaliburs_scabbard_avalon"] = dict(color=0xA0FFD8, layers=[
        motes("avalon_lights", [0.25, 0.1, -0.15, 0.55, 1.0, 0.2], "minecraft:end_rod", 0xA0FFD8, 8, 0.012),
        motes("fairy_dust", [0.2, 0.2, -0.2, 0.6, 0.9, 0.3], "dust", 0xA0FFD8, 8, 0.01, 0.8),
        ring("healing_ring", [0.4, 0.7, 0], 0.4, "minecraft:happy_villager", 0xA0FFD8, 6, 0.3, "flat"),
        ring("utopia_circle", [0, 0.4, 0], 1.0, "minecraft:end_rod", 0xD8C060, 8, 0.15, "flat"),
        line("scabbard_edge", [0.35, 0.95, -0.05], [0.55, 0.15, 0.1], "minecraft:end_rod", 0xD8C060, 9),
        ground("avalon_floor", 1.2, "dust", 0xA0FFD8, 6, 1.0),
        motes("mouth_glow", [0.3, 0.9, -0.15, 0.5, 1.0, 0.05], "minecraft:glow", 0xA0FFD8, 4, 0.006),
        ring("light_orbit", [0.42, 0.55, 0], 0.5, "minecraft:glow", 0xA0FFD8, 5, 0.4, "flat", kind="orbit"),
        trail("fairy_trail", [0.45, 0.4, 0.2], "minecraft:happy_villager", 0xA0FFD8, 5, 1.0, 0.5)],
        acts={"all_far_away": [act("ELF_CIRCLE", 0xA0FFD8, 30, 4.0, "feet", "minecraft:happy_villager", 60, 1.8), act("LIGHT_FLARE", 0xD8C060, 22, 3.0, "eye", "minecraft:end_rod", 36, 1.0)],
              "far_distant_utopia": [act("MAGIC_CIRCLE_EXPLOSION", 0xA0FFD8, 46, 6.0, "feet", "minecraft:end_rod", 100, 3.0), act("TREE_CANOPY", 0xD8C060, 40, 5.0, "feet", "minecraft:happy_villager", 70, 3.0)]})
    P["silvered_sash_of_reflections"] = dict(color=0xF0F8FF, layers=[
        motes("glass_glints", [-0.45, 0.5, -0.35, 0.45, 1.5, 0.35], "minecraft:end_rod", 0xF0F8FF, 8, 0.003),
        motes("mirror_dust", [-0.45, 0.2, -0.3, 0.45, 1.2, 0.3], "dust", 0xCFD8E8, 7, 0.004, 0.8),
        ring("reflected_halo", [0, 1.55, 0], 0.55, "minecraft:glow", 0xF0F8FF, 7, 0.15, "flat"),
        ring("reflection_ring", [0, 0.9, 0], 0.9, "minecraft:end_rod", 0xCFD8E8, 8, -0.18, "flat"),
        line("bandolier_line", [-0.35, 1.45, -0.3], [0.38, 0.85, -0.3], "minecraft:end_rod", 0xF0F8FF, 9),
        ground("mirror_floor", 1.3, "dust", 0xF0F8FF, 6, 0.9),
        motes("tail_shards", [0.15, 0.1, -0.25, 0.45, 0.8, 0.0], "minecraft:glow", 0xF0F8FF, 4, -0.004),
        ring("shard_orbit", [0, 1.0, 0], 0.8, "minecraft:end_rod", 0xF0F8FF, 6, 0.35, "flat", kind="orbit"),
        trail("silver_trail", [0, 0.8, 0.3], "minecraft:glow", 0xCFD8E8, 5, 1.0, 0.5)],
        acts={"reflect": [act("MIRROR_FRAME", 0xF0F8FF, 24, 3.0, "feet", "minecraft:end_rod", 40, 1.2), act("MIRROR_DOUBLE", 0xCFD8E8, 20, 3.0, "feet", "minecraft:glow", 20, 1.0)],
              "mirrored_step": [act("MIRROR_STEP", 0xF0F8FF, 22, 3.0, "feet", "minecraft:end_rod", 50, 1.2), act("MIRROR_SHATTER", 0xCFD8E8, 22, 3.0, "aim", "minecraft:glow", 40, 1.2, 8.0)]})
    # ------------------------------------------------------------------ CHEST
    P["berserker_armor_shard"] = dict(color=0xFF3A3A, layers=[
        motes("rage_embers", [-0.4, 0.8, -0.35, 0.4, 1.7, 0.3], "minecraft:flame", 0xFF3A3A, 8, 0.015),
        motes("blood_dust", [-0.45, 0.6, -0.3, 0.45, 1.5, 0.3], "dust", 0xA82A2A, 8, 0.004, 1.0),
        ring("rage_halo", [0, 1.95, 0], 0.42, "minecraft:angry_villager", 0xFF3A3A, 4, 0.2, "flat", mx=1),
        ring("berserk_ring", [0, 0.9, 0], 1.0, "dust", 0xFF3A3A, 9, 0.25, "flat"),
        line("crack_line", [-0.25, 1.4, -0.32], [0.3, 0.8, -0.32], "minecraft:flame", 0xFF3A3A, 8),
        ground("scorch", 1.3, "minecraft:flame", 0xA82A2A, 6, 1.0, 2),
        motes("shoulder_flames", [0.3, 1.5, -0.2, 0.6, 1.9, 0.2], "minecraft:lava", 0xFF3A3A, 3, 0.0),
        ring("blood_orbit", [0, 1.1, 0], 0.8, "minecraft:flame", 0xFF3A3A, 5, 0.38, "flat", kind="orbit"),
        trail("fury_trail", [0, 0.9, 0.4], "minecraft:smoke", 0xA82A2A, 8, 1.0, 0.6)],
        acts={"berserk": [act("BLOOD_FX1", 0xFF3A3A, 30, 3.0, "feet", "minecraft:flame", 60, 1.4), act("FIRE_PILLAR", 0xA82A2A, 24, 2.5, "feet", "minecraft:lava", 20, 1.0)],
              "blood_frenzy": [act("SLASH_SCYTHE", 0xFF3A3A, 26, 5.0, "feet", "minecraft:crit", 70, 2.6), act("DEVIL_CIRCLE", 0xA82A2A, 36, 6.0, "feet", "minecraft:angry_villager", 40, 3.0)]})
    P["orsteds_dragon_god_armor"] = dict(color=0xFF5A6A, layers=[
        motes("dragon_gloom", [-0.45, 0.6, -0.35, 0.45, 1.8, 0.35], "minecraft:smoke", 0xFF5A6A, 6, 0.012),
        motes("curse_motes", [-0.5, 0.4, -0.3, 0.5, 1.6, 0.4], "dust", 0x8A2A3A, 8, 0.006),
        ring("seal_halo", [0, 1.1, 0.55], 0.7, "minecraft:witch", 0xFF5A6A, 8, 0.1, "vertical"),
        ring("hundred_curses", [0, 0.8, 0], 1.15, "minecraft:soul_fire_flame", 0xFF5A6A, 8, -0.2, "flat"),
        line("crest_line", [0, 1.55, -0.34], [0, 0.55, -0.34], "minecraft:witch", 0xFF5A6A, 6),
        ground("dragon_god_floor", 1.6, "dust", 0x8A2A3A, 7, 1.1, 3),
        motes("pauldron_fire", [0.3, 1.5, -0.2, 0.7, 2.0, 0.2], "minecraft:flame", 0xFF5A6A, 3, 0.01),
        ring("curse_orbit", [0, 1.1, 0], 0.9, "minecraft:witch", 0xFF5A6A, 5, 0.3, "flat", kind="orbit"),
        trail("patient_wake", [0, 0.8, 0.4], "minecraft:large_smoke", 0x8A2A3A, 6, 1.0, 0.7)],
        acts={"dragon_gods_guard": [act("SEAL_TRINITY", 0xFF5A6A, 32, 4.0, "feet", "minecraft:witch", 60, 2.0), act("FIRE_PILLAR", 0x8A2A3A, 26, 3.0, "feet", "minecraft:flame", 40, 1.4)],
              "hundred_curses": [act("DEVIL_CIRCLE", 0xFF5A6A, 44, 7.0, "feet", "minecraft:soul_fire_flame", 100, 3.5), act("SEAL_CHAINS", 0x8A2A3A, 40, 6.0, "feet", "minecraft:witch", 80, 3.0)]})
    # ------------------------------------------------------------------ FEET
    P["greaves_of_the_wind_walker"] = dict(color=0x7A6CFF, layers=[
        motes("breeze_motes", [-0.35, 0.0, -0.3, 0.35, 0.8, 0.3], "minecraft:cloud", 0xCFE0FF, 8, 0.01),
        motes("storm_sparks", [-0.35, 0.05, -0.3, 0.35, 0.5, 0.3], "minecraft:electric_spark", 0x7A6CFF, 5, 0.0),
        ring("wind_ring", [0, 0.35, 0], 0.45, "minecraft:cloud", 0x7A6CFF, 8, 0.8, "flat"),
        ring("ankle_ring", [0, 0.12, 0], 0.4, "dust", 0x7A6CFF, 8, -0.9, "flat"),
        line("shin_edge", [0.1, 0.55, -0.15], [0.1, 0.1, -0.2], "minecraft:end_rod", 0xC9A45A, 6),
        ground("gale_floor", 1.1, "minecraft:cloud", 0xCFE0FF, 7, 1.0),
        motes("heel_wings", [-0.35, 0.05, 0.1, 0.35, 0.35, 0.5], "minecraft:glow", 0x7A6CFF, 4, 0.0),
        ring("breeze_orbit", [0, 0.4, 0], 0.55, "minecraft:cloud", 0xCFE0FF, 6, 1.1, "flat", kind="orbit"),
        trail("wind_trail", [0, 0.2, 0.3], "minecraft:cloud", 0xCFE0FF, 14, 1.0, 0.5, 4)],
        acts={"gust": [act("WIND_SLASH", 0x7A6CFF, 16, 2.0, "feet", "minecraft:cloud", 30, 0.8, 5.0)], "feather_fall": [act("WIND_RING", 0xCFE0FF, 22, 2.5, "feet", "minecraft:cloud", 40, 1.2)],
              "dust_devil": [act("WIND_TORNADO", 0x7A6CFF, 40, 3.0, "aim", "minecraft:cloud", 60, 1.4, 5.0)], "gust_of_wind": [act("WIND_GALE", 0xCFE0FF, 26, 3.0, "eye", "minecraft:cloud", 50, 1.2, 4.0)],
              "warding_wind": [act("FX_SPIRIT_AURA", 0x7A6CFF, 40, 3.0, "feet", "minecraft:electric_spark", 40, 1.4)], "wind_wall": [act("WIND_EMPEROR", 0xCFE0FF, 30, 4.0, "aim", "minecraft:cloud", 60, 2.0, 5.0)],
              "control_winds": [act("FX_WIND_GUST", 0x7A6CFF, 30, 4.0, "eye", "minecraft:end_rod", 60, 2.0, 4.0)], "wind_walk": [act("SPIRIT_AURA", 0xCFE0FF, 50, 4.0, "feet", "minecraft:cloud", 90, 2.0), act("WIND_RING", 0x7A6CFF, 36, 5.0, "feet", "minecraft:electric_spark", 50, 3.0)]})
    # ------------------------------------------------------------------ NECK
    P["white_whistle"] = dict(color=0xFFFFFF, layers=[
        motes("breath", [-0.2, 1.1, -0.5, 0.2, 1.5, -0.2], "minecraft:cloud", 0xFFFFFF, 7, 0.006),
        motes("note_motes", [-0.4, 0.9, -0.6, 0.4, 1.7, 0.1], "minecraft:note", 0xFFFFFF, 3, 0.012, 1.0, 1),
        ring("sound_halo", [0, 1.35, -0.3], 0.45, "minecraft:end_rod", 0xFFFFFF, 8, 0.3, "vertical"),
        ring("abyss_ring", [0, 0.9, 0], 0.8, "minecraft:glow", 0xD8C878, 7, 0.2, "flat"),
        line("cord_gleam", [-0.15, 1.5, -0.2], [0.0, 1.15, -0.32], "minecraft:end_rod", 0xD8C878, 6),
        ground("white_floor", 1.1, "dust", 0xFFFFFF, 5, 0.9),
        motes("whistle_glow", [-0.1, 1.05, -0.4, 0.1, 1.3, -0.3], "minecraft:glow", 0xFFFFFF, 4, 0.004),
        ring("wave_orbit", [0, 1.2, -0.3], 0.5, "minecraft:end_rod", 0xFFFFFF, 5, 0.5, "flat", kind="orbit"),
        trail("echo_trail", [0, 1.2, 0.2], "minecraft:glow", 0xFFFFFF, 5, 1.0, 0.4)],
        acts={"white_call": [act("MAGIC_CIRCLE", 0xFFFFFF, 30, 3.0, "feet", "minecraft:note", 24, 1.2), act("LIGHT_FLARE", 0xD8C878, 24, 3.0, "eye", "minecraft:end_rod", 40, 1.6)],
              "descent": [act("SPATIAL_RIFT", 0xFFFFFF, 34, 4.0, "feet", "minecraft:end_rod", 70, 2.4), act("WIND_RING", 0xD8C878, 30, 5.0, "feet", "minecraft:cloud", 60, 3.0)]})
    # ------------------------------------------------------------------ HANDS
    P["hand_of_vecna"] = dict(color=0xE0245E, layers=[
        motes("cold_mist", [0.3, 0.3, -0.1, 0.55, 1.0, 0.1], "minecraft:snowflake", 0xE0245E, 7, -0.004),
        motes("red_motes", [-0.55, 0.4, -0.15, 0.55, 1.1, 0.15], "dust", 0xE0245E, 8, 0.008),
        ring("vecna_circle", [0.42, 0.1, 0], 0.45, "dust", 0xE0245E, 9, 0.5, "flat"),
        ring("wish_ring", [0, 1.0, 0], 0.95, "minecraft:witch", 0xC9A24A, 6, -0.18, "flat"),
        line("rune_forearm", [0.42, 1.1, -0.15], [0.42, 0.55, -0.15], "minecraft:witch", 0xE0245E, 7),
        ground("withered_floor", 1.2, "dust", 0x8E8A78, 6, 1.0),
        motes("gold_cuff_glints", [-0.62, 0.55, -0.12, -0.3, 1.0, 0.12], "minecraft:wax_on", 0xC9A24A, 3, 0.0),
        ring("finger_orbit", [0.42, 0.6, 0], 0.45, "minecraft:soul_fire_flame", 0xE0245E, 5, 0.6, "flat", kind="orbit"),
        trail("grave_trail", [0.45, 0.6, 0.2], "minecraft:smoke", 0xE0245E, 6, 1.0, 0.4)],
        acts={"touch_of_death": [act("SLASH_SCYTHE", 0xE0245E, 26, 4.0, "aim", "minecraft:soul", 40, 0.8, 3.0), act("BONE_FX1", 0x8E8A78, 22, 3.0, "aim", "minecraft:witch", 30, 1.0, 3.0)],
              "rimed_grasp": [act("WATER_RING", 0x9FE3FF, 28, 4.0, "aim", "minecraft:snowflake", 70, 1.6, 3.0)], "skeleton_to_jelly": [act("BODY_FX1", 0xE0245E, 30, 4.0, "aim", "minecraft:glow_squid_ink", 50, 1.0, 3.0)],
              "choose_wish": [act("SPELL_CARD", 0xC9A24A, 20, 2.0, "eye", "minecraft:enchant", 20, 0.8)],
              "wish": [act("MAGIC_CIRCLE_EXPLOSION", 0xE0245E, 46, 7.0, "feet", "minecraft:witch", 90, 3.0), act("SEAL_TRINITY", 0xC9A24A, 40, 5.0, "feet", "minecraft:enchant", 60, 3.0)]})
    P["aerie_courier_gloves"] = dict(color=0xC8DCFF, layers=[
        motes("wind_wisps", [-0.6, 0.4, -0.2, 0.6, 1.1, 0.25], "minecraft:cloud", 0xC8DCFF, 8, 0.01),
        motes("sky_dust", [-0.55, 0.5, -0.15, 0.55, 1.1, 0.15], "dust", 0x7AA8FF, 7, 0.006, 0.8),
        ring("wrist_ring_r", [-0.42, 0.65, 0], 0.4, "minecraft:cloud", 0xC8DCFF, 7, 0.9, "flat"),
        ring("wrist_ring_l", [0.42, 0.65, 0], 0.4, "minecraft:cloud", 0xC8DCFF, 7, -0.9, "flat"),
        line("wing_edge", [-0.65, 0.75, 0.0], [-0.75, 0.55, 0.2], "minecraft:end_rod", 0xC8DCFF, 6),
        ground("courier_floor", 1.1, "minecraft:cloud", 0xC8DCFF, 6, 1.0),
        motes("feather_down", [-0.7, 0.5, -0.1, 0.7, 1.0, 0.3], "minecraft:glow", 0xC8DCFF, 3, -0.004),
        ring("parcel_orbit", [0, 0.8, 0], 0.85, "minecraft:cloud", 0x7AA8FF, 6, 0.6, "flat", kind="orbit"),
        trail("gust_trail", [0, 0.7, 0.3], "minecraft:cloud", 0xC8DCFF, 12, 1.0, 0.7, 4)],
        acts={"couriers_dash": [act("WIND_GALE", 0xC8DCFF, 22, 3.0, "feet", "minecraft:cloud", 50, 1.2), act("WIND_SLASH", 0x7AA8FF, 16, 2.5, "aim", "minecraft:end_rod", 24, 0.6, 6.0)],
              "gale_parcel": [act("WIND_TORNADO", 0xC8DCFF, 32, 3.5, "aim", "minecraft:cloud", 70, 1.4, 4.0), act("WIND_RING", 0x7AA8FF, 24, 4.0, "eye", "minecraft:end_rod", 30, 1.4)]})
    P["bloodbound_gauntlets"] = dict(color=0xFF5A5A, layers=[
        motes("blood_mist", [-0.6, 0.4, -0.2, 0.6, 1.1, 0.25], "dust", 0xC01818, 9, 0.004, 1.1),
        motes("drips", [-0.55, 0.2, -0.15, 0.55, 0.9, 0.15], "minecraft:dripping_lava", 0xFF5A5A, 2, 0.0, 1.0, 1),
        ring("fist_ring_r", [-0.42, 0.55, 0], 0.4, "dust", 0xFF5A5A, 8, 0.7, "flat"),
        ring("fist_ring_l", [0.42, 0.55, 0], 0.4, "dust", 0xFF5A5A, 8, -0.7, "flat"),
        line("vein_line", [-0.42, 1.0, -0.28], [-0.42, 0.45, -0.28], "minecraft:flame", 0xFF5A5A, 6),
        ground("offering_floor", 1.2, "dust", 0xC01818, 7, 1.0),
        motes("knuckle_sparks", [-0.6, 0.35, -0.3, 0.6, 0.55, -0.1], "minecraft:crit", 0xFF5A5A, 4, 0.0),
        ring("blood_orbit", [0, 0.8, 0], 0.85, "dust", 0xC01818, 5, 0.45, "flat", kind="orbit"),
        trail("wound_trail", [0, 0.7, 0.3], "dust", 0xC01818, 8, 1.0, 0.6)],
        acts={"blood_fist": [act("BLOOD_FX1", 0xFF5A5A, 24, 3.0, "aim", "minecraft:crit", 50, 0.8, 2.5), act("SLASH_WAVE", 0xC01818, 16, 2.5, "aim", "minecraft:flame", 24, 0.6, 3.0)],
              "bloodbond": [act("DEVIL_CIRCLE", 0xC01818, 34, 4.0, "feet", "minecraft:heart", 14, 1.4), act("SEAL_TRINITY", 0xFF5A5A, 30, 3.5, "feet", "minecraft:flame", 40, 2.0)]})
    P["incinerator_arm"] = dict(color=0xFFB060, layers=[
        motes("vent_embers", [-0.62, 0.5, 0.0, -0.3, 1.4, 0.4], "minecraft:flame", 0xFFB060, 10, 0.02),
        motes("heat_haze", [-0.6, 0.3, -0.2, 0.6, 1.2, 0.3], "dust", 0xC86A3A, 8, 0.01, 1.1),
        ring("furnace_ring", [-0.42, 0.55, 0], 0.45, "minecraft:flame", 0xFFB060, 8, 0.6, "flat"),
        ring("muzzle_ring", [-0.42, -0.1, 0], 0.35, "minecraft:lava", 0xFFB060, 5, -0.8, "flat", mx=2),
        line("barrel_line", [-0.42, 0.5, -0.1], [-0.42, -0.2, -0.1], "minecraft:flame", 0xFFB060, 8),
        ground("scorched_floor", 1.3, "minecraft:flame", 0xC86A3A, 7, 1.0, 2),
        motes("pipe_smoke", [-0.7, 1.1, 0.2, -0.4, 1.6, 0.5], "minecraft:large_smoke", 0x2A2430, 5, 0.02),
        ring("spark_orbit", [-0.42, 0.55, 0], 0.55, "minecraft:electric_spark", 0xFFB060, 5, 0.9, "flat", kind="orbit"),
        trail("exhaust_trail", [-0.5, 0.9, 0.4], "minecraft:smoke", 0xC86A3A, 7, 1.0, 0.5)],
        acts={"fist_of_fire": [act("FIRE_BURST", 0xFFB060, 24, 4.0, "aim", "minecraft:flame", 70, 1.4, 3.0), act("FX_FIRE_ERUPTION", 0xC86A3A, 22, 3.0, "aim", "minecraft:lava", 20, 1.0, 3.0)],
              "incinerator": [act("FLAME_TRAIL", 0xFFB060, 30, 6.0, "eye", "minecraft:flame", 110, 1.0, 10.0), act("FIRE_PILLAR", 0xC86A3A, 30, 7.0, "aim", "minecraft:lava", 60, 3.0, 12.0), act("FLAME_EXPLOSION", 0xFFB060, 30, 7.0, "aim", "minecraft:flame", 80, 3.0, 14.0)]})
    # ------------------------------------------------------------------ HEAD
    P["eye_of_vecna"] = dict(color=0xFFD84A, layers=[
        motes("gold_motes", [-0.4, 1.6, -0.4, 0.4, 2.3, 0.4], "minecraft:end_rod", 0xFFD84A, 6, 0.008),
        motes("crimson_dust", [-0.45, 1.5, -0.45, 0.45, 2.1, 0.45], "dust", 0xE0245E, 7, 0.005, 0.9),
        ring("optic_halo", [0, 2.15, 0], 0.6, "minecraft:glow", 0xFFD84A, 9, 0.2, "flat"),
        ring("eyebite_ring", [0, 1.9, 0], 0.85, "dust", 0xE0245E, 8, -0.3, "flat"),
        line("brow_line", [-0.25, 1.88, -0.32], [0.25, 1.88, -0.32], "minecraft:end_rod", 0xFFD84A, 6),
        ground("seeing_floor", 1.2, "dust", 0xE0245E, 6, 1.0),
        motes("tail_motes", [-0.15, 1.2, 0.3, 0.15, 1.9, 0.6], "minecraft:witch", 0xE0245E, 3, 0.0),
        ring("eyelet_orbit", [0, 1.9, 0], 0.6, "minecraft:end_rod", 0xFFD84A, 5, 0.38, "flat", kind="orbit"),
        trail("gaze_trail", [0, 1.8, 0.3], "minecraft:glow", 0xFFD84A, 5, 1.0, 0.3)],
        acts={"clairvoyance": [act("MAGIC_CIRCLE", 0xFFD84A, 30, 3.0, "eye", "minecraft:end_rod", 40, 1.2), act("FX_SPIRIT_AURA", 0xE0245E, 36, 3.0, "feet", "minecraft:glow", 20, 1.4)],
              "crown_of_madness": [act("DEVIL_CIRCLE", 0xE0245E, 30, 4.0, "aim", "minecraft:witch", 50, 1.8, 5.0)], "disintegrate": [act("LIGHTNING_SPEAR", 0xE0245E, 22, 4.0, "eye", "minecraft:crit", 60, 0.6, 12.0), act("MIRROR_RAY", 0xFFD84A, 20, 4.0, "eye", "minecraft:end_rod", 40, 0.6, 12.0)],
              "dominate_monster": [act("KOTO_AURA", 0xE0245E, 32, 4.0, "aim", "minecraft:witch", 50, 1.6, 5.0)], "eyebite": [act("SEAL_TRINITY", 0xFFD84A, 30, 4.0, "aim", "minecraft:glow", 40, 1.8, 6.0)]})
    P["basilica_halo"] = dict(color=0xFFF4B0, layers=[
        motes("falling_light", [-0.45, 0.8, -0.45, 0.45, 2.4, 0.45], "minecraft:end_rod", 0xFFF4B0, 8, -0.006),
        motes("gold_dust", [-0.5, 0.5, -0.5, 0.5, 2.1, 0.5], "dust", 0xFFD860, 8, 0.004, 0.9),
        ring("halo_ring", [0, 2.35, 0], 0.62, "minecraft:end_rod", 0xFFF4B0, 14, 0.15, "flat", mx=4),
        ring("benediction_circle", [0, 0.1, 0], 1.3, "minecraft:glow", 0xFFD860, 9, 0.12, "flat"),
        line("light_shaft", [0, 2.4, 0], [0, 3.6, 0], "minecraft:end_rod", 0xFFF4B0, 10, 0.02),
        ground("basilica_floor", 1.7, "dust", 0xFFF4B0, 8, 1.0, 3),
        motes("temple_wings", [-0.6, 1.7, -0.05, 0.6, 2.1, 0.2], "minecraft:glow", 0xFFF4B0, 4, 0.004),
        ring("cherub_orbit", [0, 1.9, 0], 0.8, "minecraft:end_rod", 0xFFD860, 6, 0.3, "flat", kind="orbit"),
        trail("blessed_trail", [0, 1.6, 0.3], "minecraft:glow", 0xFFF4B0, 5, 1.0, 0.4)],
        acts={"benediction": [act("ELF_CIRCLE", 0xFFF4B0, 34, 4.0, "feet", "minecraft:end_rod", 80, 2.2), act("LIGHT_BLADE", 0xFFD860, 24, 3.0, "feet", "minecraft:glow", 40, 1.4)],
              "smite_the_dark": [act("LIGHT_FLARE", 0xFFF4B0, 26, 4.0, "eye", "minecraft:end_rod", 60, 1.6), act("SLASH_WAVE", 0xFFD860, 20, 3.0, "eye", "minecraft:glow", 30, 1.0, 5.0)]})
    P["godking_crown"] = dict(color=0x5CF0B4, layers=[
        motes("emerald_motes", [-0.45, 1.7, -0.45, 0.45, 2.7, 0.45], "minecraft:happy_villager", 0x5CF0B4, 7, 0.012),
        motes("jade_dust", [-0.5, 1.5, -0.5, 0.5, 2.3, 0.5], "dust", 0x5CF0B4, 8, 0.006, 0.9),
        ring("crown_halo", [0, 2.55, 0], 0.62, "minecraft:end_rod", 0x5CF0B4, 11, -0.2, "flat", mx=3),
        ring("flight_ring", [0, 1.0, 0], 1.1, "minecraft:glow", 0x5CF0B4, 8, 0.2, "flat"),
        line("spire_line", [0, 2.0, 0], [0, 3.1, 0], "minecraft:end_rod", 0x5CF0B4, 9, 0.02),
        ground("godking_floor", 1.6, "dust", 0x5CF0B4, 8, 1.0, 3),
        motes("wing_motes", [-0.75, 1.9, -0.1, 0.75, 2.4, 0.3], "minecraft:glow", 0x5CF0B4, 5, 0.004),
        ring("skill_orbit", [0, 2.0, 0], 0.8, "minecraft:enchant", 0x5CF0B4, 7, 0.4, "flat", kind="orbit"),
        trail("ascent_trail", [0, 1.8, 0.3], "minecraft:end_rod", 0x5CF0B4, 7, 1.0, 0.5)],
        acts={"ascend": [act("MAGIC_CIRCLE_EXPLOSION", 0x5CF0B4, 50, 8.0, "feet", "minecraft:end_rod", 120, 3.5), act("SEAL_TRINITY", 0x5CF0B4, 44, 6.0, "feet", "minecraft:happy_villager", 70, 3.0), act("FX_SPIRIT_NOVA", 0x5CF0B4, 36, 5.0, "feet", "minecraft:glow", 60, 3.0)],
              "dismiss_ascension": [act("WIND_RING", 0x5CF0B4, 24, 3.0, "feet", "minecraft:cloud", 40, 1.6)]})
    P["limitless_blindfold"] = dict(color=0xC8E8FF, layers=[
        motes("six_eyes_motes", [-0.45, 1.5, -0.45, 0.45, 2.2, 0.45], "minecraft:end_rod", 0xC8E8FF, 7, 0.006),
        motes("blue_dust", [-0.5, 1.4, -0.5, 0.5, 2.1, 0.5], "dust", 0x8AC8FF, 8, 0.004, 0.9),
        ring("infinity_ring", [0, 1.9, 0], 0.62, "minecraft:glow", 0xC8E8FF, 12, 0.25, "flat", mx=3),
        ring("infinity_ring_tilt", [0, 1.9, 0], 0.9, "minecraft:end_rod", 0x8AC8FF, 10, -0.2, "vertical", mx=3),
        line("tail_line", [0, 1.8, 0.3], [0, 0.9, 0.45], "minecraft:glow", 0x8AC8FF, 7),
        ground("void_floor", 1.4, "dust", 0x8AC8FF, 7, 1.0),
        motes("blue_orb", [0.4, 1.9, -0.1, 0.6, 2.1, 0.1], "minecraft:soul_fire_flame", 0x8AC8FF, 4, 0.0),
        ring("hollow_orbit", [0, 2.0, 0], 0.85, "minecraft:witch", 0xC8E8FF, 6, 0.45, "flat", kind="orbit"),
        trail("barrier_trail", [0, 1.8, 0.2], "minecraft:glow", 0xC8E8FF, 5, 1.0, 0.3)],
        acts={"infinity": [act("MERCURY_DOME", 0xC8E8FF, 34, 4.0, "feet", "minecraft:glow", 60, 1.8), act("FX_SPIRIT_AURA", 0x8AC8FF, 30, 3.0, "feet", "minecraft:end_rod", 40, 1.4)],
              "hollow_purple": [act("DEMON_METEOR", 0xA060FF, 40, 7.0, "aim", "minecraft:witch", 90, 2.0, 12.0), act("MAGIC_CIRCLE_EXPLOSION", 0x8AC8FF, 36, 6.0, "aim", "minecraft:end_rod", 80, 3.0, 14.0)]})
    P["odins_all_seeing_eye"] = dict(color=0xA8D8FF, layers=[
        motes("raven_feathers", [-0.55, 1.7, -0.2, 0.55, 2.3, 0.3], "minecraft:smoke", 0x6070A0, 6, 0.004),
        motes("rune_motes", [-0.45, 1.6, -0.45, 0.45, 2.3, 0.45], "minecraft:enchant", 0xA8D8FF, 7, 0.008),
        ring("sight_halo", [0, 2.3, 0], 0.58, "minecraft:end_rod", 0xA8D8FF, 9, 0.2, "flat"),
        ring("odin_circle", [0, 1.0, 0], 1.1, "minecraft:enchant", 0xD8B860, 8, -0.15, "flat"),
        line("spear_line", [0, 2.5, 0.2], [0, 2.5, -0.5], "minecraft:end_rod", 0xD8B860, 8),
        ground("god_tomb_floor", 1.4, "dust", 0xA8D8FF, 7, 1.0),
        motes("eye_glints", [-0.15, 1.8, -0.4, 0.15, 2.0, -0.3], "minecraft:glow", 0xA8D8FF, 5, 0.0),
        ring("huginn_muninn", [0, 2.1, 0], 0.85, "minecraft:soul", 0xA8D8FF, 5, 0.5, "flat", kind="orbit"),
        trail("known_trail", [0, 1.8, 0.3], "minecraft:enchant", 0xA8D8FF, 6, 1.0, 0.4)],
        acts={"one_eye_open": [act("COMPASS_ARRAY", 0xA8D8FF, 30, 4.0, "feet", "minecraft:enchant", 50, 1.8), act("MIRROR_RAY", 0xD8B860, 20, 3.0, "eye", "minecraft:end_rod", 30, 0.6, 12.0)],
              "sacrifice_for_sight": [act("SEAL_TRINITY", 0xA8D8FF, 40, 6.0, "feet", "minecraft:enchant", 70, 3.0), act("COMPASS_LOCK", 0xD8B860, 36, 5.0, "aim", "minecraft:glow", 50, 2.0, 10.0)]})
    P["ras_dawn_crown"] = dict(color=0xFFE080, layers=[
        motes("dawn_motes", [-0.5, 1.6, -0.5, 0.5, 2.8, 0.5], "minecraft:end_rod", 0xFFE080, 9, 0.014),
        motes("gold_dust", [-0.5, 1.5, -0.5, 0.5, 2.4, 0.5], "dust", 0xFFF0C0, 8, 0.008, 0.9),
        ring("sun_halo", [0, 2.1, 0.45], 0.62, "minecraft:flame", 0xFFE080, 12, 0.1, "vertical", mx=4),
        ring("noon_ring", [0, 0.1, 0], 1.4, "dust", 0xFFE080, 9, 0.15, "flat"),
        line("ray_line", [0, 2.5, 0.35], [0, 3.4, 0.35], "minecraft:end_rod", 0xFFF0C0, 10, 0.02),
        ground("dawn_floor", 1.8, "minecraft:flame", 0xFFE080, 8, 1.0, 3),
        motes("cobra_sparks", [-0.1, 2.0, -0.4, 0.1, 2.3, -0.3], "minecraft:glow", 0xFFE080, 4, 0.0),
        ring("sun_orbit", [0, 1.9, 0], 0.85, "minecraft:flame", 0xFFE080, 6, 0.35, "flat", kind="orbit"),
        trail("sunrise_trail", [0, 1.8, 0.4], "minecraft:end_rod", 0xFFE080, 7, 1.0, 0.5)],
        acts={"first_light": [act("LIGHT_FLARE", 0xFFE080, 26, 3.5, "feet", "minecraft:end_rod", 60, 2.0), act("FIRE_PILLAR", 0xFFF0C0, 22, 3.0, "feet", "minecraft:flame", 40, 1.4)],
              "noon": [act("FIRE_WILD", 0xFFE080, 46, 8.0, "feet", "minecraft:flame", 120, 4.0), act("LIGHT_BLADE", 0xFFF0C0, 38, 7.0, "eye", "minecraft:end_rod", 90, 4.0), act("MAGIC_CIRCLE_EXPLOSION", 0xFFE080, 40, 7.0, "feet", "minecraft:end_rod", 80, 4.0)]})
    return P


def write_all(out_dir=None):
    out_dir = out_dir or M.UPG
    os.makedirs(out_dir, exist_ok=True)
    P = build_all()
    missing = [i for i in IT.IDS if i not in P]
    if missing:
        raise SystemExit("no effect table for " + ", ".join(missing))
    for rid, d in P.items():
        data = {"id": rid, "grade": "GOD", "layers": d["layers"], "acts": d["acts"]}
        with open(os.path.join(out_dir, rid + ".json"), "w", encoding="utf8", newline="\n") as fh:
            json.dump(data, fh, indent=1)
            fh.write("\n")
    return P


if __name__ == "__main__":
    P = write_all()
    print("%d effect tables, layers: %s" % (len(P), {k: len(v["layers"]) for k, v in P.items()}))
