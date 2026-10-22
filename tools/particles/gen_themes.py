"""Writes src/main/resources/data/nusmp/particle_themes.json: the particle identity of every magic, relic grade, relic element, skill family and boss.

A theme is [mote, accent, spark, ambient] (the four slots of particle.fx.FxSlot: the signature mote, the glyph or shape, the hard bright one, the soft one), a colour for the
tintable particles, and a dark flavour (the shadowed twin the old Dark Rukh calls map to). Run:  python3 -B tools/particles/gen_themes.py
The generator checks every particle id against the catalog and every MagicType constant against src/main/java/.../blackclover/MagicType.java.
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
RES = os.path.join(ROOT, "src", "main", "resources")

# dark flavours by group: [mote, accent, spark, ambient]
DARK_SETS = {
    "fire": ["cinder", "ember", "void_speck", "soot_puff"],
    "water": ["ink_splat", "droplet", "void_speck", "soot_puff"],
    "earth": ["earth_chip", "void_speck", "cinder", "soot_puff"],
    "wind": ["feather_black", "void_speck", "soul_wisp", "smoke_wisp"],
    "light": ["void_speck", "soul_wisp", "rune_luna", "soot_puff"],
    "dark": ["void_speck", "shadow_tendril", "soul_wisp", "soot_puff"],
    "space": ["void_speck", "nebula_puff", "rune_eye", "soot_puff"],
    "life": ["spore", "leaf_autumn", "void_speck", "poison_cloud"],
    "battle": ["ash_flake", "cinder", "blood_drop", "soot_puff"],
    "fantasy": ["void_speck", "dream_star", "soul_wisp", "nebula_puff"],
}

# magic: name -> (group, "mote accent spark ambient", colour)
MAGIC = {
    "FLAME": ("fire", "ember flame_tongue spark soot_puff", "FF7A2E"),
    "EXPLOSION": ("fire", "spark cinder ember soot_puff", "FFB02E"),
    "MAGMA": ("fire", "lava_blob ember cinder soot_puff", "FF4A1C"),
    "WATER": ("water", "droplet bubble glint mote_soft", "3F8FFF"),
    "ICE": ("water", "snowflake shard_ice glint mote_soft", "9FE0FF"),
    "MERCURY": ("water", "mana_mote shard_glass glint mote_soft", "C7D2E8"),
    "MIST": ("water", "mote_soft smoke_wisp droplet mote", "CFE3F2"),
    "WIND": ("wind", "mote feather_white glint smoke_wisp", "BFF5E0"),
    "STAR": ("wind", "star4 glint dream_star mote", "FFF2A8"),
    "STORM": ("wind", "electric_orb rune_bolt spark smoke_wisp", "7AA8FF"),
    "EARTH": ("earth", "earth_chip sand_grain shard_crystal ash_flake", "9A7A4E"),
    "PLANT": ("life", "leaf_green spore seed petal", "5FCB4F"),
    "SAND": ("earth", "sand_grain earth_chip glint ash_flake", "E6C887"),
    "LIGHT": ("light", "mote glint star4 mote_soft", "FFF6C8"),
    "LIGHTNING": ("light", "electric_orb rune_bolt spark smoke_wisp", "FFE45E"),
    "SWORD": ("battle", "glint star4 spark mote_soft", "DDE6F5"),
    "DARK": ("dark", "void_speck rune_luna soul_wisp soot_puff", "7A3FCB"),
    "SHADOW": ("dark", "void_speck shadow_tendril soot_puff smoke_wisp", "4A2F7A"),
    "POISON": ("dark", "spore bubble droplet poison_cloud", "7BD13A"),
    "SPATIAL": ("space", "mana_mote rune_key glint nebula_puff", "8F5BFF"),
    "MIRROR": ("space", "glint shard_glass star4 mote_soft", "D9ECFF"),
    "GRAVITY": ("space", "void_speck earth_chip mana_mote soot_puff", "5A3FA0"),
    "TIME": ("space", "mote clock_tick glint mote_soft", "BFA4FF"),
    "SEALING": ("space", "rune_key rune_sol glint mote_soft", "E8D28C"),
    "REINFORCEMENT": ("battle", "mote spark earth_chip smoke_wisp", "FFB84D"),
    "BEAST": ("battle", "ember bone_chip spark ash_flake", "D98A3A"),
    "BONE": ("battle", "bone_chip ash_flake cinder soot_puff", "E8E0CC"),
    "BLOOD": ("battle", "blood_drop void_speck ember soot_puff", "A8182B"),
    "CREATION": ("fantasy", "mote gem_sparkle star4 mote_soft", "FFD27A"),
    "COPY": ("fantasy", "mana_mote page glint mote_soft", "7FD6FF"),
    "ILLUSION": ("fantasy", "dream_star soul_wisp glint nebula_puff", "C48BFF"),
    "DREAM": ("fantasy", "dream_star nebula_puff star4 mote_soft", "F5B8FF"),
    "ANTI_MAGIC": ("dark", "void_speck rune_eye ash_flake soot_puff", "3A3A44"),
    "STEEL": ("battle", "glint shard_glass spark ash_flake", "B8C2D2"),
    "THREAD": ("fantasy", "thread mote glint mote_soft", "F2D4A0"),
    "TRANSMUTATION": ("earth", "mana_mote cog gem_sparkle smoke_wisp", "D9A441"),
    "ASH": ("fire", "ash_flake cinder ember soot_puff", "9A9AA6"),
    "COTTON": ("fantasy", "mote_soft thread petal smoke_wisp", "F4F0FF"),
    "RECOMBINATION": ("fantasy", "mana_mote gem_sparkle spark mote_soft", "FF8FD0"),
    "PAINTING": ("fantasy", "paint_splat thread glint ink_splat", "FF6FB0"),
    "WORLD_TREE": ("life", "leaf_green rune_leaf seed spore", "6FE08A"),
    "DICE": ("fantasy", "glint card spark mote_soft", "FFE08A"),
    "SLASH": ("battle", "glint spark shard_glass mote_soft", "E0E6F0"),
    "COMPASS": ("space", "rune_sol glint mana_mote mote_soft", "9FD6FF"),
    "KOTODAMA": ("fantasy", "mote rune_sol rune_luna mote_soft", "FFFFFF"),
    "GAME": ("fantasy", "card gem_sparkle spark mote_soft", "6FFFA8"),
    "DEMON_BEAST": ("dark", "bone_chip ember void_speck soot_puff", "9B2A2A"),
    "BODY": ("battle", "mote earth_chip spark smoke_wisp", "FFD08A"),
    "EYE": ("fantasy", "rune_eye glint mote mote_soft", "7FE8FF"),
    "EYEBALL": ("dark", "rune_eye void_speck blood_drop soot_puff", "C84060"),
    "CURSE": ("dark", "void_speck rune_luna soul_wisp soot_puff", "9B3FCB"),
    "CURSE_WARDING": ("space", "rune_sol rune_key glint mote_soft", "D9C8FF"),
    "DEMON_FIRE": ("dark", "ember flame_soul void_speck soot_puff", "9B4BFF"),
    "DEMON_ICE": ("dark", "snowflake shard_ice void_speck mote_soft", "5A6FFF"),
    "DEMON_LIGHT": ("dark", "glint void_speck star4 soot_puff", "D94BFF"),
    "DEMON_WATER": ("dark", "bubble droplet void_speck soot_puff", "2A4FA8"),
    "BARRIER": ("space", "shard_glass rune_sol glint mote_soft", "7FD6C8"),
    "KEY": ("space", "rune_key glint mote mote_soft", "FFD36A"),
    "CHAIN": ("battle", "chain_link spark glint ash_flake", "9AA3B8"),
    "BUTOH": ("fantasy", "music_note mote petal mote_soft", "FF9AC8"),
    "BRIAR": ("life", "spore leaf_green seed ash_flake", "3FA05A"),
    "CHERRY_BLOSSOM": ("life", "petal_cherry petal mote mote_soft", "FF9FC4"),
    "FUNGUS": ("life", "spore mote bubble smoke_wisp", "9FD060"),
    "FOOD": ("fantasy", "mote seed gem_sparkle smoke_wisp", "FFB36B"),
    "CRYSTAL": ("earth", "shard_crystal gem_sparkle glint mote_soft", "B98CFF"),
    "CORUNDUM": ("earth", "gem_sparkle shard_crystal glint mote_soft", "FF4A7A"),
    "BRONZE": ("earth", "cog spark earth_chip ash_flake", "C9863C"),
    "COPPER": ("earth", "electric_orb cog spark ash_flake", "E0743A"),
    "IRON": ("battle", "spark chain_link earth_chip ash_flake", "A9B0BE"),
    "BLACK_OIL": ("dark", "ink_splat void_speck ember soot_puff", "1A1424"),
    "GEL": ("water", "bubble droplet mote_soft mote", "8FE8C0"),
    "GLASS": ("earth", "shard_glass glint mote mote_soft", "CDEBFF"),
    "BUBBLE": ("water", "bubble glint mote_soft droplet", "A8E0FF"),
    "ICE_WEDGE": ("water", "shard_ice snowflake glint mote_soft", "7FC4FF"),
    "LEGION": ("fantasy", "mana_mote spirit_firefly spark mote_soft", "FFD27A"),
    "AIR": ("wind", "mote_soft feather_white smoke_wisp glint", "D7F5FF"),
    "HAIR": ("battle", "thread mote spark smoke_wisp", "FFD0A0"),
    "MEMORY": ("fantasy", "page mote dream_star mote_soft", "B8D0FF"),
    "MINERAL": ("earth", "gem_sparkle earth_chip shard_crystal ash_flake", "8FE0D0"),
    "MODIFICATION": ("fantasy", "cog gem_sparkle spark mote_soft", "FF9A5A"),
    "MUCUS": ("water", "bubble droplet spore mote_soft", "B8F07A"),
    "MUD": ("earth", "earth_chip droplet sand_grain soot_puff", "7A5A3A"),
    "NAIL": ("battle", "spark shard_glass chain_link ash_flake", "D0D4DC"),
    "PERMEATION": ("space", "void_speck mote rune_wave mote_soft", "8FA8FF"),
    "POISON_PLANT": ("life", "spore leaf_green bubble poison_cloud", "9AD03A"),
    "RED_OCHRE": ("earth", "paint_splat earth_chip ember ash_flake", "C4503A"),
    "ROCK": ("earth", "earth_chip sand_grain spark ash_flake", "8A8478"),
    "SANDSTONE": ("earth", "sand_grain earth_chip glint ash_flake", "D8B878"),
    "SCALE": ("battle", "shard_crystal gem_sparkle glint mote_soft", "6FD0B8"),
    "SHAKUDO": ("earth", "glint spark cinder soot_puff", "4A5A88"),
    "SKIN": ("battle", "petal mote thread smoke_wisp", "FFC9A8"),
    "SMOKE": ("dark", "smoke_wisp soot_puff ember ash_flake", "8A8A96"),
    "SNOW": ("water", "snowflake mote_soft glint smoke_wisp", "F4FAFF"),
    "SONG": ("fantasy", "music_note mote star4 mote_soft", "FFE08A"),
    "SOUL_CORPSE": ("dark", "soul_wisp bone_chip void_speck soot_puff", "7FD0C0"),
    "SOUL": ("dark", "soul_wisp spirit_firefly void_speck mote_soft", "9FE8FF"),
    "SOUND": ("wind", "music_note rune_wave glint mote_soft", "9FD0FF"),
    "SPIKE": ("earth", "shard_crystal earth_chip spark ash_flake", "B0B8C8"),
    "SWITCHING": ("space", "mana_mote rune_key glint mote_soft", "FFA8FF"),
    "TONGUE": ("battle", "droplet blood_drop bubble mote_soft", "E86A8A"),
    "TREE": ("life", "leaf_green seed leaf_autumn spore", "6FB04F"),
    "STONE": ("earth", "earth_chip sand_grain glint ash_flake", "9A9A9A"),
    "VINE": ("life", "leaf_green thread seed spore", "4FA04A"),
    "VORTEX": ("wind", "aura_wisp mana_mote glint smoke_wisp", "8FC8FF"),
    "WING": ("wind", "feather_white mote glint mote_soft", "FFFFFF"),
}

# other themes: id -> (group for the dark flavour or None, particles, colour)
OTHER = {
    # relic grades
    "relic": ("light", "mote glint star4 mote_soft", "FFD89A"),
    "relic.grade.NORMAL": ("light", "mote_soft mote ash_flake", "AAAAAA"),
    "relic.grade.SPECIAL": ("life", "mote spore glint", "55FF55"),
    "relic.grade.RARE": ("water", "mote glint mana_mote", "55FFFF"),
    "relic.grade.UNIQUE": ("fantasy", "glint gem_sparkle dream_star mote", "FF77FF"),
    "relic.grade.LEGEND": ("fire", "star4 glint ember gem_sparkle", "FFAA00"),
    "relic.grade.GOD": ("light", "glint star4 rune_sol mote", "FFE9A0"),
    "relic.grade.GENESIS": ("space", "star4 mana_mote dream_star nebula_puff", "E8D8FF"),
    "relic.grade.AKASHIC": ("space", "star4 glint gem_sparkle dream_star", "FFFFFF"),                # EX-Singularity (above Genesis): sharp stars and gem glints, tinted with the glow's current hue
    # relic elements (also chosen by hue from a relic's colour)
    "relic.element.fire": ("fire", "ember flame_tongue spark soot_puff", "FF7A2E"),
    "relic.element.water": ("water", "bubble droplet mana_mote mote_soft", "3F8FFF"),
    "relic.element.ice": ("water", "snowflake shard_ice glint mote_soft", "9FE0FF"),
    "relic.element.wind": ("wind", "mote feather_white glint smoke_wisp", "BFF5E0"),
    "relic.element.earth": ("earth", "earth_chip sand_grain glint ash_flake", "9A7A4E"),
    "relic.element.storm": ("wind", "electric_orb rune_bolt spark smoke_wisp", "7AA8FF"),
    "relic.element.nature": ("life", "leaf_green petal spore seed", "5FCB4F"),
    "relic.element.light": ("light", "glint star4 mote mote_soft", "FFF6C8"),
    "relic.element.holy": ("light", "glint feather_white star4 mote_soft", "FFF0B8"),
    "relic.element.dark": ("dark", "void_speck soul_wisp rune_luna soot_puff", "7A3FCB"),
    "relic.element.demon": ("dark", "ember void_speck flame_soul soot_puff", "B03A5A"),
    "relic.element.arcane": ("space", "mana_mote rune_sol gem_sparkle mote_soft", "8F7BFF"),
    "relic.element.blood": ("battle", "blood_drop void_speck ember soot_puff", "A8182B"),
    "relic.element.metal": ("battle", "spark shard_glass chain_link ash_flake", "B8C2D2"),
    "relic.element.time": ("space", "clock_tick cog glint mote_soft", "BFA4FF"),
    "relic.element.space": ("space", "mana_mote rune_key glint nebula_puff", "8F5BFF"),
    "relic.element.soul": ("dark", "soul_wisp spirit_firefly void_speck mote_soft", "9FE8FF"),
    "relic.element.music": ("fantasy", "music_note mote star4 mote_soft", "FFE08A"),
    "relic.element.poison": ("dark", "spore bubble droplet poison_cloud", "7BD13A"),
    "relic.element.dream": ("fantasy", "dream_star soul_wisp star4 nebula_puff", "F5B8FF"),
    "relic.element.gem": ("earth", "gem_sparkle shard_crystal glint mote_soft", "FF6FB0"),
    "relic.order": ("light", "rune_sol glint star4 mote_soft", "FFE9A0"),
    "relic.pair": ("fantasy", "mana_mote glint dream_star mote_soft", "FFB0E0"),
    "relic.forge": ("fire", "spark ember cog soot_puff", "FF9A3C"),
    "relic.socket": ("space", "gem_sparkle mana_mote glint mote_soft", "B98CFF"),
    "relic.wave": ("light", "mote glint star4 mote_soft", "FFD89A"),
    # skills
    "skill": ("light", "mote glint spark mote_soft", "FFFFFF"),
    "skill.god": ("light", "glint star4 rune_sol mote", "FFE9A0"),
    "skill.sword": ("battle", "glint spark star4 mote_soft", "DDE6F5"),
    "skill.requip": ("fire", "glint gem_sparkle spark ember", "FF5A4A"),
    "skill.paladin": ("light", "glint feather_white star4 mote_soft", "FFE9A0"),
    "skill.tree": ("fantasy", "mote mana_mote glint mote_soft", "CFE3FF"),
    "skill.blackwings": ("dark", "feather_black void_speck spark soot_puff", "2A2038"),
    "skill.grimoire": ("fantasy", "mana_mote rune_sol glint mote_soft", "CFE3FF"),
    "skill.celestial": ("light", "star4 glint mana_mote mote_soft", "A8D8FF"),
    "skill.shishou": ("light", "glint rune_sol spark mote_soft", "7FEBFF"),
    # bosses
    "boss": ("dark", "ember void_speck spark soot_puff", "C03030"),
    "boss.lucius": ("light", "star4 rune_sol glint mote_soft", "FFE9A0"),
    "boss.zenon": ("dark", "bone_chip void_speck rune_luna soot_puff", "C8C0A8"),
    "boss.dante": ("fire", "ember flame_soul spark soot_puff", "A82A2A"),
    "boss.vanica": ("dark", "blood_drop feather_black ember soot_puff", "8A1A2A"),
    "boss.elsdocia": ("water", "snowflake shard_ice glint mote_soft", "9FE0FF"),
    "boss.avatar": ("dark", "void_speck shadow_tendril blood_drop soot_puff", "8A1A3A"),
    "boss.abyssal": ("dark", "soul_wisp void_speck bubble soot_puff", "3A4FA0"),
    "boss.charybdis": ("water", "bubble droplet glint mote_soft", "2F7FD0"),
    "boss.goliath": ("earth", "earth_chip sand_grain spark ash_flake", "8A8070"),
    "boss.zagred": ("dark", "rune_eye rune_key spark soot_puff", "9B2FC0"),
    "boss.celestial": ("light", "star4 glint mana_mote mote_soft", "A8D8FF"),
    # dungeon
    "dungeon": ("space", "mote mana_mote glint mote_soft", "B8C8FF"),
    "dungeon.gate": ("space", "rune_key mana_mote glint mote_soft", "9F8FFF"),
    "dungeon.vault": ("light", "gem_sparkle glint star4 mote", "FFD27A"),
    "dungeon.squad": ("battle", "spark mote glint ash_flake", "E0C080"),
    "dungeon.seal": ("space", "rune_sol rune_key glint mote_soft", "E8D28C"),
    "dungeon.trial": ("light", "rune_bolt spark glint mote_soft", "9FE0FF"),
    "dungeon.crucible": ("fire", "rune_flame ember spark soot_puff", "FFA040"),
    "dungeon.decennial": ("light", "star4 rune_sol gem_sparkle mote_soft", "FFE9A0"),
    "dungeon.guardian": ("dark", "rune_eye void_speck spark soot_puff", "9B6BFF"),
    "dungeon.lock": ("space", "rune_key glint mana_mote mote_soft", "FFD36A"),
    "dungeon.safe": ("life", "spirit_firefly mote petal mote_soft", "9FFFC0"),
    "dungeon.rift": ("space", "void_speck nebula_puff mana_mote soot_puff", "8F5BFF"),
    "dungeon.humanoid": ("dark", "ash_flake void_speck ember soot_puff", "8A7A9A"),
    "dungeon.trap": ("battle", "spark cog ash_flake soot_puff", "D0A850"),
    # misc systems
    "corpse": ("dark", "soul_wisp bone_chip void_speck soot_puff", "7FD0C0"),
    "grimoire.daemon": ("dark", "rune_eye void_speck ember soot_puff", "9B3FCB"),
    "wizard": ("space", "mana_mote rune_sol glint mote_soft", "8F7BFF"),
    "arena.lucius": ("light", "star4 rune_sol glint mote_soft", "FFE9A0"),
    "arena.chronicle": ("fantasy", "page rune_sol glint mote_soft", "E8D8A8"),
    "item.brooch": ("light", "glint gem_sparkle star4 mote_soft", "FFD9A0"),
    "item.rotdoom": ("dark", "soul_wisp spore void_speck soot_puff", "7A3A8A"),
    "item.providence": ("light", "glint feather_white star4 mote_soft", "FFFFFF"),
    "item": ("light", "mote glint star4 mote_soft", "FFD89A"),
    "world": ("life", "mote spore leaf_green mote_soft", "CFE8B0"),
}


WAVE_ELEMENTS = ["fire", "storm", "ice", "nature", "holy", "arcane", "blood", "metal", "water", "dark", "wind", "earth", "time", "space", "soul",
                 "music", "poison", "dream", "gem", "demon", "light", "fire", "ice", "storm", "nature", "arcane", "dark", "holy", "water", "metal"]


def add_waves():
    """relic.wave.<N>: the relic waves (relics/wave/WaveN*.java) each get the particles of one element, so no two neighbouring waves look alike."""
    for n in range(110):
        e = WAVE_ELEMENTS[n % len(WAVE_ELEMENTS)]
        g, p, c = OTHER["relic.element." + e]
        parts = p.split()
        if (n // len(WAVE_ELEMENTS)) % 2 == 1 or n >= 20 and n % 2 == 1:      # the second round of an element (and every odd wave past 20) swaps its first two particles
            parts[0], parts[1] = parts[1], parts[0]
        OTHER["relic.wave.%d" % n] = (g, " ".join(parts), c)


def catalog_ids():
    with open(os.path.join(RES, "data", "nusmp", "particle_catalog.json"), encoding="utf-8") as fh:
        return {p["id"] for p in json.load(fh)["particles"]}


def magic_names():
    src = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "blackclover", "MagicType.java")
    names = re.findall(r"^    ([A-Z_0-9]+)\(\"", open(src, encoding="utf-8").read(), re.M)
    return names


def build():
    add_waves()
    ids = catalog_ids()
    names = magic_names()
    missing = [n for n in names if n not in MAGIC]
    extra = [n for n in MAGIC if n not in names]
    if missing or extra:
        sys.exit("magic types without a theme: %s; themes for unknown magic: %s" % (missing, extra))
    themes = {}

    def put(key, group, particles, rgb):
        p = particles.split()
        for x in p:
            if x not in ids:
                sys.exit("%s names an unknown particle %s" % (key, x))
        if not 1 <= len(p) <= 4:
            sys.exit("%s needs 1-4 particles" % key)
        t = {"particles": p, "color": "#" + rgb}
        if group in DARK_SETS:
            t["dark"] = {"particles": DARK_SETS[group]}
        themes[key] = t

    for n in names:
        g, p, c = MAGIC[n]
        put("magic." + n, g, p, c)
    for k, (g, p, c) in OTHER.items():
        put(k, g, p, c)
    return {
        "version": 1,
        "comment": "Particle identity of every magic, relic, skill and boss. Slots: [mote, accent, spark, ambient]. Generated by tools/particles/gen_themes.py.",
        "default": {"particles": ["mote", "glint", "star4", "mote_soft"], "color": "#FFFFFF",
                    "dark": {"particles": ["void_speck", "soot_puff", "shadow_tendril", "soul_wisp"], "color": "#5A3A8C"}},
        "themes": themes,
    }


def main():
    doc = build()
    out = os.path.join(RES, "data", "nusmp", "particle_themes.json")
    with open(out, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(doc, fh, indent=1)
        fh.write("\n")
    print("themes:", len(doc["themes"]), "->", out)


if __name__ == "__main__":
    main()
