#!/usr/bin/env python3
"""Writes data/nusmp/mega/dnd_bosses/famous/<id>.json: D&D's famous villains as Decennial boss profiles (1.67.0).

    python3 tools/labyrinth/gen_dnd_famous.py            # writes
    python3 tools/labyrinth/gen_dnd_famous.py --check    # exit 1 when a file on disk differs

Why a second pool and not twenty more base files: the owner's rule is "add the new, keep the old". The twenty ordinary profiles of gen_dnd_bosses.py stay; a sector boss is one of these
famous bosses with the server's chance (`[guard] famousPercent`, default 50), picked by the floor and the sector id so it never changes after a reload, and only from the bosses whose tier range
fits the floor and whose side (holy, evil or any) fits the road. Each one carries its signature in the data: the legendary actions that suit it, its breath weapon, whether a spell glances off it
(`reflective`: the Tarrasque), whether it holds an anti-magic aura (`A`), the Word of Ending (`X`), and its three forms."""
import json, os, sys
ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp", "mega", "dnd_bosses", "famous")

# id, name, side, tier range, D&D source, anime echo, forms, legendary actions (id, cost), lair actions, flags (A anti-magic aura, F frightful, M absorbs magic, X execution, R reflective), breath, resistances, points
F = [
 ("beholder", "Beholder, the Sphere of Many Eyes", "any", (2, 3), ["Beholder"], ["Black Clover: Anti-Magic cone"], ["Beholder", "Elder Orb", "Death Tyrant"],
  [("cantrip", 1), ("eye_rays", 2), ("blink", 1)], ["tremor", "anti_magic_field"], "A", "fire", 3, 3),
 ("mind_flayer_elder_brain", "Elder Brain of the Illithid Colony", "evil", (2, 4), ["Elder Brain", "Mind Flayer"], ["Tensura: a Demon Lord of mind"], ["Elder Brain", "Ulitharid", "Sovereign Mind"],
  [("mind_blast", 2), ("cantrip", 1), ("magic_drain", 2)], ["anti_magic_field", "tremor"], "F", "poison", 3, 3),
 ("strahd", "Strahd von Zarovich, the Devil of Barovia", "evil", (2, 3), ["Vampire Lord"], ["Black Clover: a Devil-bound noble"], ["Vampire Lord", "Mist Sovereign", "Lord of Barovia"],
  [("vampiric_bite", 1), ("blink", 1), ("counterspell", 1), ("frightful_presence", 2)], ["necrotic_mist", "tremor"], "F", "poison", 3, 3),
 ("lolth", "Lolth, Queen of the Demonweb", "evil", (3, 4), ["Lolth", "Drider Matriarch"], ["Fairy Tail: a spider Dragon-Slayer"], ["Spider Queen", "Demonweb Weaver", "Queen of Spiders"],
  [("vampiric_bite", 1), ("tail_sweep", 1), ("frightful_presence", 2), ("life_drain", 1)], ["necrotic_mist", "searing_light"], "FM", "poison", 4, 4),
 ("zariel", "Zariel, Archduchess of Avernus", "evil", (3, 4), ["Zariel", "Pit Fiend"], ["Black Clover: a Supreme Devil"], ["Fallen Archangel", "Archduchess", "Hellfire Sovereign"],
  [("dragon_breath", 2), ("tail_sweep", 1), ("cantrip", 1), ("frightful_presence", 2)], ["searing_light", "falling_stars", "tremor"], "FX", "fire", 4, 4),
 ("kraken", "The Kraken of the Black Tide", "evil", (3, 4), ["Kraken"], ["Tensura: a Calamity-class monster"], ["Kraken", "Storm Kraken", "Tidebreaker Ascendant"],
  [("tail_sweep", 1), ("gravity_crush", 2), ("dragon_breath", 2), ("wing_buffet", 2)], ["tremor", "falling_stars"], "F", "lightning", 4, 4),
 ("demogorgon", "Demogorgon, Prince of Demons", "evil", (3, 5), ["Demogorgon"], ["Tensura: a Demon Lord with two minds"], ["Two-Headed Prince", "Gaze of Madness", "Prince of Demons Unbound"],
  [("mind_blast", 2), ("tail_sweep", 1), ("eye_rays", 2), ("frightful_presence", 2)], ["tremor", "necrotic_mist", "anti_magic_field"], "FMX", "fire", 4, 4),
 ("asmodeus", "Asmodeus, Lord of the Nine Hells", "evil", (4, 5), ["Asmodeus"], ["Black Clover: the Supreme Devil"], ["Lord of Nine Hells", "Hellfire Archdevil", "Contract Absolute"],
  [("dragon_breath", 2), ("counterspell", 1), ("death_touch", 2), ("frightful_presence", 2)], ["searing_light", "falling_stars", "anti_magic_field"], "AFMX", "fire", 5, 5),
 ("orcus", "Orcus, Demon Prince of Undeath", "evil", (4, 5), ["Orcus", "Wand of Orcus"], ["Tensura: an undead Demon Lord"], ["Prince of Undeath", "Wand-Bearer", "Death Incarnate"],
  [("death_touch", 2), ("life_drain", 1), ("tail_sweep", 1), ("frightful_presence", 2)], ["necrotic_mist", "tremor", "anti_magic_field"], "AFX", "poison", 5, 5),
 ("acererak", "Acererak, the Devourer of the Tomb", "evil", (4, 5), ["Demilich", "Acererak"], ["Magi: a dark Djinn of a dungeon"], ["Demilich", "Devourer", "Eternal Lich"],
  [("death_touch", 2), ("magic_drain", 2), ("counterspell", 1), ("life_drain", 1)], ["anti_magic_field", "necrotic_mist", "searing_light"], "AFMX", "poison", 5, 5),
 ("tiamat", "Tiamat, the Five-Headed Queen", "evil", (4, 5), ["Tiamat"], ["Fairy Tail: a Dragon King"], ["Five-Headed Queen", "Chromatic Mother", "Queen of Evil Dragons"],
  [("dragon_breath", 2), ("tail_sweep", 1), ("haki_pulse", 2), ("frightful_presence", 2)], ["tremor", "falling_stars", "necrotic_mist"], "FMX", "acid", 5, 5),
 ("vecna", "Vecna, the Whispered One", "evil", (5, 5), ["Vecna", "Archlich"], ["Black Clover: the Anti-Magic Devil"], ["Whispered One", "Eye and Hand", "Master of the Spider Throne"],
  [("death_touch", 2), ("counterspell", 1), ("magic_drain", 2), ("blink", 1), ("frightful_presence", 2)], ["anti_magic_field", "necrotic_mist", "searing_light", "tremor"], "AFMX", "poison", 5, 5),
 ("tarrasque", "The Tarrasque, the Last Sleeper", "any", (4, 5), ["Tarrasque"], ["Tensura: a Calamity beast"], ["Tarrasque", "Armoured Calamity", "End of Worlds"],
  [("tail_sweep", 1), ("wing_buffet", 2), ("haki_pulse", 2), ("gravity_crush", 2)], ["tremor", "falling_stars"], "FR", "fire", 5, 4),
 ("bahamut", "Bahamut, the Platinum Dragon", "holy", (4, 5), ["Bahamut"], ["The Beginning After the End: the Dragon King"], ["Platinum Dragon", "Lord of the Wind", "Dragon God"],
  [("dragon_breath", 2), ("counterspell", 1), ("tail_sweep", 1), ("haki_pulse", 2)], ["searing_light", "falling_stars", "tremor"], "FX", "frost", 5, 5),
 ("solar", "The Solar, Slayer with the Bow", "holy", (3, 4), ["Solar"], ["Magi: a Djinn of judgement"], ["Solar", "Slaying Bow", "Judgement Unbound"],
  [("death_touch", 2), ("counterspell", 1), ("cantrip", 1), ("blink", 1)], ["searing_light", "falling_stars"], "FX", "lightning", 4, 4),
 ("dracolich", "The Dracolich of the Frozen Crypt", "evil", (3, 4), ["Dracolich"], ["Fairy Tail: a dead Dragon"], ["Dracolich", "Skeletal Wyrm", "Phylactery Dragon"],
  [("dragon_breath", 2), ("life_drain", 1), ("tail_sweep", 1), ("frightful_presence", 2)], ["necrotic_mist", "tremor"], "FM", "frost", 4, 4),
 ("lich_king", "The Lich King of the Ossuary", "evil", (2, 3), ["Lich"], ["Black Clover: a Wizard-King of the dead"], ["Lich", "Arch-Lich", "Undying King"],
  [("cantrip", 1), ("counterspell", 1), ("life_drain", 1), ("magic_drain", 2)], ["necrotic_mist", "anti_magic_field"], "M", "poison", 3, 3),
]

# 1.94.0: the abyssal look each famous boss wears (tools/abyssal/look_<look>.py, docs/labyrinth/abyssal_bosses.md); the profile's `look` field names it
LOOKS = {"beholder": "beholder", "mind_flayer_elder_brain": "elder_brain", "strahd": "strahd", "lolth": "lolth", "zariel": "zariel", "kraken": "kraken", "demogorgon": "demogorgon",
         "asmodeus": "asmodeus", "orcus": "orcus", "acererak": "acererak", "tiamat": "tiamat", "vecna": "vecna", "tarrasque": "tarrasque", "bahamut": "bahamut", "solar": "solar",
         "dracolich": "dracolich", "lich_king": "lich_king"}

def build(r):
    (fid, name, side, (lo, hi), dnd, anime, forms, leg, lair, flags, breath, resist, pts) = r
    out = _build(r)
    if fid in LOOKS: out["look"] = LOOKS[fid]
    return out

def _build(r):
    (fid, name, side, (lo, hi), dnd, anime, forms, leg, lair, flags, breath, resist, pts) = r
    return {"famous": True, "side": side, "tier_min": lo, "tier_max": hi, "name": name, "anime": anime, "dnd": dnd, "forms": forms,
            "resistances": resist, "legendary_points": pts,
            "legendary_actions": [{"id": i, "cost": c} for i, c in leg], "lair_actions": lair, "breath": breath,
            "anti_magic_aura": "A" in flags, "frightful_aura": "F" in flags, "absorb_magic": "M" in flags, "reflective": "R" in flags,
            "mythic": True, "execution": "X" in flags}

def main(check):
    os.makedirs(OUT, exist_ok=True); bad = 0
    for r in F:
        text = json.dumps(build(r), indent=2) + "\n"
        path = os.path.join(OUT, r[0] + ".json")
        if check:
            if not os.path.exists(path) or open(path).read() != text: print("STALE", r[0]); bad += 1
        else:
            open(path, "w").write(text)
    print("%d famous bosses" % len(F)); return 1 if bad else 0

if __name__ == "__main__":
    sys.exit(main("--check" in sys.argv))
