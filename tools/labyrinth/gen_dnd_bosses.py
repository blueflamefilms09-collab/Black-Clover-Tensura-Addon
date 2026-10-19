#!/usr/bin/env python3
"""Writes data/nusmp/mega/dnd_bosses/<side>_t<tier><a|b>.json: the twenty Decennial boss profiles (1.65.0): ten Holy (ascension), ten Evil (descent), five tiers of two.

    python3 tools/labyrinth/gen_dnd_bosses.py            # writes
    python3 tools/labyrinth/gen_dnd_bosses.py --check    # exit 1 when a file on disk differs

Why a generator: twenty files of the same shape must agree on the schema (labyrinth/mega/guard/DndProfile) and on the escalation (resistances, legendary points and the mythic / execution
unlocks grow with the tier); one table here says which anime and which D&D creature each boss fuses and which traits it has, and DndProfilesFilesTest reads the files back."""
import json, os, sys
ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp", "mega", "dnd_bosses")

# (side, tier, variant, name, anime, dnd, forms, flags)   flags: A anti-magic aura, F frightful aura, M absorbs magic, X execution
T = [
 ("holy", 1, 0, "Seraph-Djinn of the Blessed Earth", ["Magi: a Djinn of a dungeon"], ["Deva", "Treant"], ["Seraph Djinn", "Unbound Vessel", "Metal Vessel Ascendant"], "F"),
 ("holy", 1, 1, "The Dawnlit Aviary Wyrm", ["The Beginning After the End: a Dragon King"], ["Gold Dragon", "Roc"], ["Dawn Wyrm", "Aviary Sovereign", "Mythic Sunwing"], "F"),
 ("holy", 2, 0, "Thunder-Choir Archon", ["Fairy Tail: a lightning Dragon Slayer"], ["Storm Giant", "Couatl"], ["Choirmaster", "Thunder Dragon-Slayer", "Heaven's Voice"], "F"),
 ("holy", 2, 1, "Prismatic Glassworks Sovereign", ["Tensura: a True Dragon"], ["Prismatic Dragon"], ["Glass Sovereign", "Refracted Dragon", "True Dragon Haki"], "FM"),
 ("holy", 3, 0, "Inquisitor of Balanced Scales", ["The Exiled Heavy Knight: a Calamity-class monster lord"], ["Modron Primus", "Planetar"], ["Inquisitor", "Judge of Balance", "Absolute Verdict"], "FX"),
 ("holy", 3, 1, "Silver-Flame Zealot King", ["Black Clover: a Magic Emperor"], ["Lich", "Solar"], ["Zealot King", "Silver-Flame Emperor", "Wizard-King Ascendant"], "FMX"),
 ("holy", 4, 0, "Zodiac Chariot Djinn", ["Magi: a Fallen Djinn"], ["Astral Dreadnought", "Solar"], ["Charioteer Djinn", "Zodiac Equip", "Comet Vessel"], "AFX"),
 ("holy", 4, 1, "Comet-Forge Titan", ["Fairy Tail: an Etherias"], ["Tarrasque", "Titan"], ["Forge Titan", "Starlight Colossus", "Etherias Core"], "FMX"),
 ("holy", 5, 0, "Fractal Spire Singularity", ["Tensura: an Ultimate-class Demon Lord"], ["Aboleth Primordial", "Pazuzu"], ["Spire Lord", "Non-Euclidean Sovereign", "Genesis Loom"], "AFMX"),
 ("holy", 5, 1, "Sovereign of the Throne of Primeval Light", ["Magi: Solomon's Alma Torran"], ["Solar", "Tiamat"], ["Light Sovereign", "Primeval Dragon", "Throne of First Light"], "AFMX"),
 ("evil", 1, 0, "Umbral Catacomb Lich-Djinn", ["Magi: a dark Djinn"], ["Acererak", "Lich"], ["Catacomb Lich", "Umbral Djinn", "Phylactery Unbound"], "F"),
 ("evil", 1, 1, "Necrotic Miasma Hydra", ["The Beginning After the End: a beast of the Beast Glades"], ["Zombie Hydra", "Shambling Mound"], ["Miasma Hydra", "Many-Headed Rot", "Plague Mythic"], "F"),
 ("evil", 2, 0, "Blood-Iron Oubliette Warden", ["The Exiled Heavy Knight: a Calamity monster"], ["Pit Fiend", "Iron Golem"], ["Oubliette Warden", "Blood-Iron Tyrant", "Calamity Warden"], "F"),
 ("evil", 2, 1, "Pandemonium Hellhound Tyrant", ["Black Clover: a Devil"], ["Orcus", "Hellhound"], ["Hellhound Tyrant", "Devil Union", "Pandemonium Prince"], "FM"),
 ("evil", 3, 0, "Phantasmal Theatre Mirror-Lord", ["Tensura: a Demon Lord of illusion"], ["Demogorgon", "Mirror Dragon"], ["Mirror-Lord", "Two-Faced Prince", "Phantasmal Sovereign"], "FX"),
 ("evil", 3, 1, "Whispering Sand Wyrm", ["Fairy Tail: a Dragon of the desert"], ["Blue Dragon", "Purple Worm"], ["Sand Wyrm", "Whispering Dragon", "Dune Calamity"], "FMX"),
 ("evil", 4, 0, "Voidweaver Matron of Anti-Mana", ["Black Clover: the Anti-Magic Devil"], ["Vecna", "Beholder"], ["Voidweaver", "Anti-Mana Matron", "Null Sovereign"], "AFMX"),
 ("evil", 4, 1, "Leviathan of Liquid Darkness", ["Tensura: a Calamity-class monster"], ["Kraken Primordial", "Tarrasque"], ["Dark Leviathan", "Abyssal Tide", "Primordial Dark"], "AFX"),
 ("evil", 5, 0, "Acnologia-Class Dragon of Frozen Tartarus", ["Fairy Tail: the Dragon King of the Apocalypse"], ["Tiamat", "White Dragon Prime"], ["Tartarus Dragon", "Entropy Dragon King", "End-of-Time Calamity"], "AFMX"),
 ("evil", 5, 1, "The Event Horizon Singularity", ["Tensura: a Demon Lord of the void"], ["Vecna Ascended", "Tarrasque"], ["Horizon Lord", "Singularity", "Event Horizon Absolute"], "AFMX"),
]
LEG = [("tail_sweep", 1), ("cantrip", 1), ("blink", 1), ("wing_buffet", 2), ("frightful_presence", 2), ("magic_drain", 2)]
LAIR = ["tremor", "searing_light", "anti_magic_field"]
# 1.66.0: more of the kit, by side: the Holy road snaps spells back and rains stars; the Evil road drains life and breathes a necrotic mist; the dragons add a haki pulse, the deep ones a gravity crush

# 1.94.0: the ordinary profiles that name an abyssal look (docs/labyrinth/abyssal_bosses.md): the Lich-Djinn of the first evil tier is a lich. The others keep the look they had.
LOOKS = {("evil", 1, 0): "lich"}

def build(row):
    out = _build(row)
    look = LOOKS.get((row[0], row[1], row[2]))
    if look: out["look"] = look
    return out

def _build(row):
    side, tier, variant, name, anime, dnd, forms, flags = row
    n_leg = min(len(LEG), 2 + tier)
    legendary = [{"id": i, "cost": c} for i, c in LEG[:n_leg]]
    if "F" not in flags: legendary = [a for a in legendary if a["id"] != "frightful_presence"]
    if "A" not in flags and tier < 3: legendary = [a for a in legendary if a["id"] != "magic_drain"]
    if side == "holy":
        if tier >= 2: legendary.append({"id": "counterspell", "cost": 1})
    else:
        legendary.append({"id": "life_drain", "cost": 1})
    if "F" in flags and tier >= 3: legendary.append({"id": "haki_pulse", "cost": 2})
    if tier >= 4: legendary.append({"id": "gravity_crush", "cost": 2})
    lair = (LAIR[:1 + min(2, (tier + 1) // 2 - (1 if tier == 1 else 0))] if tier > 1 else LAIR[:1])
    if tier >= 2: lair = lair + ["falling_stars" if side == "holy" else "necrotic_mist"]
    if tier >= 4: lair = lair + ["necrotic_mist" if side == "holy" else "falling_stars"]
    return {"side": side, "tier": tier, "variant": variant, "name": name, "anime": anime, "dnd": dnd, "forms": forms,
            "resistances": 2 + (tier + 1) // 2, "legendary_points": 3 if tier < 4 else 4 if tier == 4 else 5,
            "legendary_actions": legendary, "lair_actions": lair,
            "anti_magic_aura": "A" in flags, "frightful_aura": "F" in flags, "absorb_magic": "M" in flags, "mythic": True, "execution": "X" in flags}

def main(check):
    os.makedirs(OUT, exist_ok=True); bad = 0
    for row in T:
        fid = "%s_t%d%s" % (row[0], row[1], "a" if row[2] == 0 else "b")
        text = json.dumps(build(row), indent=2) + "\n"
        path = os.path.join(OUT, fid + ".json")
        if check:
            if not os.path.exists(path) or open(path).read() != text: print("STALE", fid); bad += 1
        else:
            open(path, "w").write(text)
    print("%d profiles" % len(T)); return 1 if bad else 0

if __name__ == "__main__":
    sys.exit(main("--check" in sys.argv))
