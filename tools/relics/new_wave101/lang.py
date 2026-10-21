"""Writes the lang fragment of wave 101 (tools/lang/genesis_new.json): name, short description and card of the fifteen relics.

    python3 -B tools/relics/new_wave101/lang.py

The name and the short description are read from Wave101.java (so they cannot drift), the card is built from the same source: the grade line, the passives (every attribute, resistance and
lent skill of the def, in words), the cost line of every act (cooldown, magicules, sneak, uses a day) and the engravings, plus the sentence written below for each passive flag and each act.
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "relics", "wave", "Wave101.java")
OUT = os.path.join(ROOT, "tools", "lang", "genesis_new.json")

SLOT = {"NECK": "worn in the Neck slot", "HEAD": "worn as a helmet", "HANDS": "worn in the Hands slot", "BELT": "worn in the Belt slot", "CHARM": "worn in the Charm slot",
        "CHEST": "worn as a chestplate", "BACK": "worn in the Back slot", "MAIN_HAND": ""}
ATTR = {"max_health": ("maximum health", "add"), "max_magicule": ("maximum magicules", "mult"), "luck": ("luck", "add"), "movement_speed": ("movement speed", "mult"),
        "entity_interaction_range": ("reach", "add"), "attack_damage": ("attack damage", "add"), "attack_speed": ("attack speed", "add"), "knockback_resistance": ("knockback resistance", "pct"),
        "armor": ("armour", "add")}
RESIST = {"minecraft:magic": "magic", "tensura:curse": "curse", "minecraft:is_projectile": "projectile", "minecraft:lightning_bolt": "lightning", "minecraft:is_fall": "fall",
          "minecraft:is_explosion": "explosion"}

FLAG = {
    "millennium_puzzle": "When a blow would kill you, the other soul takes it: you wake at 40% health with Resistance IV for 3 seconds and a shield (once every 5 minutes).",
    "millennium_ring": "A monster that hunts you within 24 blocks glows through the walls for 3 seconds, so it is never a surprise.",
    "millennium_rod": "70% more damage against a foe under 30% of its health, which it also weakens (Weakness II for 6 seconds).",
    "millennium_key": "No fear, confusion, insanity, hypnosis, sleep, nausea or darkness can stay on you.",
    "millennium_necklace": "Once every 8 seconds the first blow of 3 or more is seen coming and halved.",
    "ea_sword_of_rupture": "60% more damage against a foe with 100 health or more.",
    "requiem_stand_arrow": "80% more damage while you are under 40% of your health; 5% of the damage comes back to you as life.",
}
SET = ("Millennium set (the seven items are worn together: the Puzzle at the neck, the Eye as a helmet, the Ring on the hand, the Rod in the hand, the Key on the belt, the Scale as a charm, "
       "the Necklace as a chestplate; none needs attunement): with 3 pieces Resistance I; 4, Regeneration I; 5, Strength I; 6, Speed I; all 7 make those Resistance II, Regeneration II and Strength II. "
       "The damage and healing of every Millennium act grow by 5% for each piece worn beyond the first (30% with all seven).")
ACTS = {
    "other_me": "The other soul steps forward: Strength III and Resistance II for 20 seconds and an absorbing shield.",
    "shadow_game": "A game of shadows: every foe within 14 blocks (up to 12) takes a 110 magic blow, Slowness IV, Weakness II and Glowing for 8 seconds and is marked (you deal 20% more to it while the mark lasts); you gain Resistance I and a shield.",
    "mind_read": "Every foe within 24 blocks glows through the walls for 12 seconds and is marked as read (you deal 25% more to it); you borrow the future vision for 12 seconds.",
    "truth_gaze": "A beam of light 32 blocks long: every foe on it takes a 130 magic blow, Weakness III for 8 seconds and the read mark.",
    "dowsing_needles": "Every hostile within 48 blocks glows for 10 seconds, and you sense magic for 30 seconds.",
    "draw_power": "Every foe within 12 blocks (up to 12) takes an 80 magic blow; you take back 4% of your maximum magicules for each (24% at most) and some life.",
    "mind_control": "The mob in your sights within 24 blocks (not a boss, not a player) is yours for 12 seconds and hunts its own kind; a boss or a player only falters (Slowness IV, Weakness II for 4 seconds).",
    "pharaohs_command": "Up to three mobs within 16 blocks (no boss, no player) bend to your will for 8 seconds.",
    "judgement_strike": "A line of light 28 blocks long: every foe on it takes a heavy blow (1.2 times the rod's damage) and Slowness IV for 6 seconds.",
    "open_the_soul_room": "Every foe within 12 blocks loses its good effects, takes a 95 magic blow and Weakness II for 8 seconds; you and your friends within 12 blocks are cleared of fear, confusion and the like.",
    "mind_garden": "You and your friends within 14 blocks are cleansed of ailments, healed (60 for you, half for friends) and given Regeneration III and Resistance II for 12 seconds.",
    "weigh_the_heart": "The monster in your sights (24 blocks) has its heart weighed against the feather: an undead, a demon, a boss or any monster takes a 150 magic blow, Weakness III for 10 seconds and the weighed mark (+30% damage from you for 12 seconds); an animal, a villager or a player is spared.",
    "ammits_verdict": "Every monster within 14 blocks is weighed at once (120 magic damage each, marked, glowing); you are mended 12 for every heart devoured, up to five.",
    "foresight": "For 12 seconds: Speed II, Resistance II and a shield, every foe within 24 blocks glows, and the future vision is lent to you.",
    "prophecy": "You are mended for 60% of the health you are missing and cleansed, take almost no damage for 3 seconds (Resistance IV) and gain a shield for 10 seconds.",
    "rupture_slash": "A wide cut of rupture: every foe in a 160 degree cone within 9 blocks takes 90% of Ea's damage and Weakness II for 8 seconds.",
    "space_cut_wind": "The sundered space: a line 36 blocks long, every foe on it takes 130% of Ea's damage and Slowness IV for 8 seconds.",
    "enuma_elish": "The rupture of heaven and earth: every foe within 16 blocks takes 140% of Ea's damage and Weakness III for 10 seconds and is thrown back.",
    "piercing_requiem": "A thrust of gold 20 blocks long: every foe on it takes 110% of the arrow's damage and glows for 8 seconds.",
    "return_to_zero": "Everything returns to zero: a shield, Resistance II for 6 seconds, your ailments cleansed, and every foe within 6 blocks thrown back.",
    "stand_awakened": "Strength IV and Speed III for 20 seconds, a shield and some life (40% of the arrow's damage).",
    "truth_seeking_barrage": "Eight black orbs fly at the nearest foes in front within 30 blocks: a 110 magic blow each, Slowness II for 4 seconds.",
    "shinra_tensei": "The push of the world: every foe within 14 blocks takes a 120 magic blow and Slowness III for 6 seconds and is thrown far back.",
    "chibaku_tensei": "A planetoid of gravity: every foe within 16 blocks is pulled in and takes a 140 magic blow and Slowness V for 10 seconds.",
    "fusion": "The crystal fuses with you: Strength III and Regeneration III for 20 seconds, a shield and some life.",
    "boundary_dissolution": "The boundary between things dissolves: every foe within 12 blocks takes a 130 magic blow and Weakness III for 8 seconds and is pushed back.",
    "transcendence": "The crystal gives what you were about to become: Strength IV and Resistance III for 25 seconds, a great shield and 100 health back.",
    "dragon_radar": "Every hostile within 48 blocks glows for 12 seconds, and you sense magic for 32 seconds.",
    "eternal_dragons_roar": "A roar of the eternal dragon: every foe on a line 32 blocks long takes a 150 magic blow and Slowness III for 6 seconds.",
    "shenrons_wish": "One wish: you are healed in full, your ailments are cleansed, you gain a great shield, Regeneration III for 10 seconds and Strength II for 30 seconds, and your friends within 12 blocks gain Regeneration III and absorption.",
    "gear_fifth": "Speed IV and Strength III for 24 seconds and a shield: the body laughs at the rules.",
    "rubber_bajrang": "A stretch of rubber: you rush 14 blocks and every foe on the way takes a 150 blow and Slowness III for 4 seconds.",
    "drums_of_liberation": "Every friend within 16 blocks gains Strength II and Speed II for 30 seconds, and you are mended.",
    "arrow_of_hope": "An arrow of light 40 blocks long: every foe on it takes a 160 magic blow and glows for 8 seconds.",
    "purify_despair": "You and your friends within 16 blocks are cleansed of ailments, healed (80 for you) and given Regeneration III and Resistance II for 12 seconds.",
    "law_of_cycles": "The law rewrites the field: every foe within 20 blocks takes a 180 magic blow and Weakness III for 12 seconds and is pushed back.",
    "spell_card_leave": "You step 32 blocks along your sight.",
    "spell_card_reflection": "A shield (about 100 health), Resistance II for 6 seconds, and every foe within 8 blocks is pushed away.",
    "spell_card_magnetic_force": "Every foe within 18 blocks is pulled to you and takes a 100 magic blow and Slowness IV for 8 seconds.",
}
CLASS = {"millennium_rod": "staff", "ea_sword_of_rupture": "longsword", "requiem_stand_arrow": "dagger"}


def fmt_amount(x):
    return ("%g" % x)


def passive_words(chain):
    parts = []
    for m in re.finditer(r'Passive\.attr\("[a-z]+:(?:generic\.|player\.)?([a-z_]+)", ([\d.]+), Operation\.(\w+)\)', chain):
        key, amt, op = m.group(1), float(m.group(2)), m.group(3)
        name, kind = ATTR[key]
        if kind == "mult" or op != "ADD_VALUE":
            parts.append("+%s%% %s" % (fmt_amount(round(amt * 100, 2)), name))
        elif kind == "pct":
            parts.append("+%s%% %s" % (fmt_amount(round(amt * 100, 2)), name))
        else:
            parts.append("+%s %s" % (fmt_amount(amt), name))
    for m in re.finditer(r'Passive\.resist\("([a-z_:]+)", ([\d.]+)\)', chain):
        parts.append("%s%% less %s damage" % (fmt_amount(round(float(m.group(2)) * 100)), RESIST.get(m.group(1), m.group(1))))
    skills = [m.group(1).replace("_", " ") for m in re.finditer(r'Passive\.skill\("tensura:([a-z_]+)"\)', chain)]
    if skills:
        parts.append("the lent skill%s %s" % ("s" if len(skills) > 1 else "", ", ".join(skills)))
    return ", ".join(parts)


def act_lines(chain, rid):
    out = []
    for m in re.finditer(r'Act\.of\("([a-z_]+)", "([^"]+)",', chain):
        key, name = m.group(1), m.group(2)
        seg = chain[m.end():]
        nxt = seg.find("Act.of(")
        seg = seg[:nxt] if nxt >= 0 else seg
        mm = re.search(r"\.cooldown\((\d+)\)", seg)
        cd = int(mm.group(1)) // 20 if mm else 0
        mana = re.search(r"\.mana\((\d+)\)", seg)
        day = re.search(r"\.usesPerDay\((\d+)\)", seg)
        bits = ["sneak + use" if ".needsSneak()" in seg else "use"]
        bits.append("cooldown %s" % (("%d s" % cd) if cd < 120 else ("%g min" % (cd / 60.0))))
        if mana:
            bits.append("%s%% magicules" % mana.group(1))
        if day:
            bits.append("%s a day" % day.group(1))
        out.append("%s (%s): %s" % (name, "; ".join(bits), ACTS[key]))
    return out


def main():
    src = open(JAVA, encoding="utf-8").read()
    blocks = re.split(r"(?=out\.accept\(RelicDef\.of\()", src)
    out = {}
    count = 0
    for blk in blocks:
        m = re.match(r'out\.accept\(RelicDef\.of\("([a-z0-9_]+)", "([^"]+)", Grade\.(\w+), RelicSlot\.(\w+)\)', blk)
        if not m:
            continue
        rid, name, grade, slot = m.group(1), m.group(2), m.group(3), m.group(4)
        desc = re.search(r'\.desc\("((?:[^"\\]|\\.)*)"\)', blk).group(1)
        needs = ".attune()" in blk[:400]
        lines = []
        if slot == "MAIN_HAND":
            dmg = re.search(r"\.melee\(([A-Z_]+), ([\d.]+)\)", blk)
            lines.append("Genesis Grade %s (%s damage, %s attacks a second).%s" % (CLASS[rid], "%g" % {"millennium_rod": 300.5, "ea_sword_of_rupture": 305.0, "requiem_stand_arrow": 260.0}[rid], dmg.group(2),
                                                                                  " Needs attunement." if needs else " Needs no attunement."))
        else:
            lines.append("Genesis Grade relic, %s.%s" % (SLOT[slot], " Needs attunement." if needs else " Needs no attunement (so the whole Millennium set can be worn at once)."))
        if rid in FLAG:
            lines.append("Passive: %s" % FLAG[rid])
        words = passive_words(blk)
        lines.append(("While held: " if slot == "MAIN_HAND" else "While worn: ") + words + ".")
        lines += act_lines(blk, rid)
        if rid.startswith("millennium_"):
            lines.append(SET)
        eng = re.search(r"\.engrave\(([^)]*)\)", blk)
        if eng:
            names = [e.strip().strip('"').split("@")[0].replace("_", " ") for e in eng.group(1).split(",")]
            lines.append("Tensura engravings: %s." % ", ".join(names))
        out["item.nusmp.%s" % rid] = name
        out["item.nusmp.%s.desc" % rid] = desc
        out["item.nusmp.%s.card" % rid] = "\n".join(lines)
        count += 1
    assert count == 15, count
    with open(OUT, "w", newline="\n", encoding="utf-8") as f:
        json.dump(dict(sorted(out.items())), f, indent=2, ensure_ascii=True)
        f.write("\n")
    print("wrote %d entries for %d relics" % (len(out), count))


if __name__ == "__main__":
    main()
