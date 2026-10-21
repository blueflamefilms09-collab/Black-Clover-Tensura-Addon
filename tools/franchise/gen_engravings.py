#!/usr/bin/env python3
"""Franchise engravings (the source table and its generator).

The mod's own engravings, one set per anime / world its relics, weapons, armour, magics and skills come from. An engraving is a 1.21.1 datapack enchantment
(data/nusmp/enchantment/<id>.json) listed in the tag data/tensura/tags/enchantment/engraving.json (so Tensura's engraving screens and the grindstone rules treat
it like its own engravings); what it DOES is code (franchise/FranchiseEvents.java over the pure franchise/FranchiseRules.java), driven by the numbers in
data/nusmp/franchise/engravings.json, which this script writes from the table below. Edit the table, run

    python3 -B tools/franchise/gen_engravings.py

and the enchantment jsons, the tag additions, the data file and the lang fragment (tools/lang/franchise_engravings.json) are rewritten. Re-runnable; it only adds
or rewrites the files it owns (data/nusmp/enchantment/fr_*.json), it never touches the older engravings.

A franchise entry:  id, name, colour (tooltip), theme (particle theme key of particle_themes.json for the proc puff), aliases (lower case strings that name it in a
wave header "inspired by X" or a roster "src"), words (lower case words in an item's own text that name it).
An engraving:       id (fr_<name>), franchise, name, flavour (one sentence), scope ('main' hand only, 'held' either hand, 'any' = held + worn armour + Curios),
and effects: a list of {kind, numbers}. Kinds are the ones of FranchiseRules (see the doc docs/items/franchise_engravings.md).
Every number is a share of the hit that was going to land, a flat count or a tick count; the code caps the sum of all attack bonuses (FranchiseRules.ATTACK_CAP).
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "src/main/resources/data/nusmp"
ENCH = DATA / "enchantment"
TAG = ROOT / "src/main/resources/data/tensura/tags/enchantment/engraving.json"
FRAG = ROOT / "tools/lang/franchise_engravings.json"
OUT = DATA / "franchise/engravings.json"

F = lambda id, name, colour, theme, aliases, words: dict(id=id, name=name, colour=colour, theme=theme, aliases=aliases, words=words)

FRANCHISES = [
    F("black_clover", "Black Clover", "#E8C14A", "magic.STAR", ["black clover"], ["asta", "yami sukehiro", "julius novachrono", "magic knight", "spade kingdom", "clover kingdom", "black bull", "crimson lion", "golden dawn", "silver eagle", "coral peacock", "five-leaf"]),
    F("tensura", "That Time I Got Reincarnated as a Slime", "#4FB8E8", "magic.WATER", ["slime", "tensura"], ["rimuru", "veldora", "milim", "benimaru", "great sage"]),
    F("magi", "Magi: The Labyrinth of Magic", "#7C5CE8", "relic.element.arcane", ["magi"], ["aladdin", "alibaba", "sinbad", "magoi", "djinn"]),
    F("solo_leveling", "Solo Leveling", "#6A3FD0", "magic.SHADOW", ["solo leveling"], ["jinwoo", "shadow monarch", "shadow soldier", "igris", "hunter rank"]),
    F("danmachi", "DanMachi", "#F0A04A", "relic.element.holy", ["danmachi", "is it wrong to try to pick up girls"], ["bell cranel", "falna", "hestia", "argonaut", "familia"]),
    F("fairy_tail", "Fairy Tail", "#F06A8A", "skill.celestial", ["fairy tail"], ["natsu", "erza", "lucy", "celestial spirit", "requip", "zodiac", "titania"]),
    F("shield_hero", "The Rising of the Shield Hero", "#4A8AD8", "relic.element.holy", ["shield hero"], ["naofumi", "raphtalia", "legendary weapon", "cardinal weapon", "vassal weapon", "waves of catastrophe"]),
    F("seven_deadly_sins", "The Seven Deadly Sins", "#E0883A", "magic.FLAME", ["seven deadly sins"], ["meliodas", "escanor", "holy knight", "sacred treasure", "lostvayne"]),
    F("dnd", "Dungeons and Dragons", "#C84A3A", "relic.wave.30", ["dungeons and dragons", "d&d", "dungeons & dragons"], ["forgotten realms", "magic item compendium", "vecna", "bag of holding", "dungeon master", "beholder"]),
    F("rezero", "Re:Zero", "#B8A8E8", "relic.wave.31", ["re:zero", "rezero"], ["subaru", "emilia", "witch factor", "return by death"]),
    F("fate", "Fate", "#D8B84A", "relic.wave.35", ["fate series", "fate/"], ["noble phantasm", "holy grail", "command seal", "heroic spirit"]),
    F("overlord", "Overlord", "#7A4AA8", "relic.wave.37", ["overlord"], ["nazarick", "ainz", "sorcerer king", "floor guardian"]),
    F("konosuba", "KonoSuba", "#E86A5A", "relic.wave.42", ["konosuba"], ["megumin", "kazuma"]),
    F("mushoku_tensei", "Mushoku Tensei", "#58B878", "relic.wave.41", ["mushoku tensei"], ["rudeus", "roxy", "eris", "paul greyrat"]),
    F("eminence_in_shadow", "The Eminence in Shadow", "#3A4A8A", "relic.wave.43", ["eminence in shadow"], ["cid kagenou", "shadow garden", "i am atomic"]),
    F("tower_of_god", "Tower of God", "#58A8D8", "relic.wave.44", ["tower of god"], ["shinsu", "twenty-fifth bam"]),
    F("fullmetal_alchemist", "Fullmetal Alchemist", "#C8B070", "magic.REINFORCEMENT", ["fullmetal alchemist"], ["automail", "philosopher's stone", "transmutation circle", "alchemist", "equivalent exchange"]),
    F("berserk", "Berserk", "#8A2A2A", "relic.element.blood", ["berserk"], ["behelit", "band of the hawk", "berserker armour", "dragonslayer", "guts"]),
    F("sao_log_horizon", "Sword Art Online and Log Horizon", "#58C8E8", "relic.wave.46", ["sword art online", "log horizon"], ["aincrad", "kirito", "asuna", "elder tale", "sword skill"]),
    F("jujutsu_kaisen", "Jujutsu Kaisen", "#5A5AE8", "relic.wave.34", ["jujutsu kaisen"], ["cursed energy", "black flash", "domain expansion", "sukuna", "gojo"]),
    F("frieren", "Frieren: Beyond Journey's End", "#A8D8E8", "relic.wave.38", ["frieren"], ["zoltraak", "mana suppression", "demon king's army"]),
    F("delicious_in_dungeon", "Delicious in Dungeon", "#E8A858", "relic.wave.39", ["delicious in dungeon", "dungeon meshi"], ["laios", "marcille", "senshi", "dungeon cooking", "walking mushroom"]),
    F("made_in_abyss", "Made in Abyss", "#D88A3A", "relic.wave.32", ["made in abyss"], ["riko", "reg", "nanachi", "white whistle", "curse of the abyss"]),
    F("tomb_raider_king", "Tomb Raider King", "#C8A84A", "relic.wave.33", ["tomb raider king"], ["tomb raider"]),
    F("yu_gi_oh", "Yu-Gi-Oh!", "#E8C84A", "relic.element.dream", ["yu-gi-oh", "yugioh"], ["duel disk", "millennium puzzle", "millennium item", "duelist", "heart of the cards"]),
    F("naruto", "Naruto", "#F08A2A", "magic.WIND", ["naruto"], ["chakra", "sharingan", "shadow clone", "akatsuki", "hokage"]),
    F("bleach", "Bleach", "#E8E8F0", "relic.element.soul", ["bleach"], ["zanpakuto", "bankai", "soul reaper", "reiatsu"]),
    F("one_piece", "One Piece", "#E85A4A", "relic.element.water", ["one piece"], ["devil fruit", "haki", "straw hat", "grand line"]),
    F("dragon_ball", "Dragon Ball", "#F0B83A", "magic.EXPLOSION", ["dragon ball"], ["saiyan", "kamehameha", "super saiyan", "zenkai"]),
    F("hunter_x_hunter", "Hunter x Hunter", "#58B858", "relic.element.nature", ["hunter x hunter", "hunter × hunter"], ["nen", "hatsu", "gon freecss", "killua", "hunter license"]),
    F("jojo", "JoJo's Bizarre Adventure", "#C84AA8", "relic.element.time", ["jojo"], ["stand user", "gold experience", "the world", "stand rush"]),
    F("exiled_heavy_knight", "The Exiled Heavy Knight Knows How to Game the System", "#8A9AA8", "relic.element.metal", ["exiled heavy knight"], ["heavy knight", "game the system"]),
]


def E(id, franchise, name, flavour, scope, *effects):
    return dict(id="fr_" + id, franchise=franchise, name=name, flavour=flavour, scope=scope, effects=list(effects))


def k(kind, **num):
    return dict(kind=kind, **num)


ENGRAVINGS = [
    # ---------------------------------------------------------------- Black Clover (3)
    E("grimoire_surge", "black_clover", "Grimoire Surge", "A grimoire answers a clear call.", "held",
      k("spell_refund", pct=0.012, cd=100), k("spell_power", pct=0.05, window=60)),
    E("anti_magic_edge", "black_clover", "Anti-Magic Edge", "A blade that does not care how well a foe is warded.", "main",
      k("anti_ward", pct=0.08)),
    E("wizard_kings_reserve", "black_clover", "Wizard King's Reserve", "A deep well of mana that fills itself.", "any",
      k("mana_trickle", pct=0.004)),
    # ---------------------------------------------------------------- Tensura (2)
    E("great_sage_insight", "tensura", "Great Sage's Insight", "The first look at a foe is already an analysis.", "main",
      k("first_strike", pct=0.07, window=200)),
    E("predator_harvest", "tensura", "Predator's Harvest", "What is defeated feeds the one who defeated it.", "main",
      k("kill_heal", heal=1.5, cd=60)),
    # ---------------------------------------------------------------- Magi (3)
    E("magoi_overflow", "magi", "Magoi Overflow", "Magoi returns to the one who spent it well.", "held",
      k("spell_refund", pct=0.01, cd=120)),
    E("rukh_wings", "magi", "Wings of the Rukh", "The Rukh carry you out of the way.", "any",
      k("dodge", chance=0.05, cd=400)),
    E("solomons_wisdom", "magi", "Solomon's Wisdom", "The wisdom of the king lends weight to spells.", "held",
      k("spell_power", pct=0.06, window=60)),
    # ---------------------------------------------------------------- Solo Leveling (2)
    E("shadow_extraction", "solo_leveling", "Shadow Extraction", "A fallen foe may rise to serve for a short while.", "main",
      k("kill_ally", chance=0.05, cd=900, life=400, entity="minecraft:skeleton")),
    E("rank_breaker", "solo_leveling", "Rank Breaker", "Strong foes are the ones that make a hunter grow.", "main",
      k("hit_bonus", cond="target_big", pct=0.07)),
    # ---------------------------------------------------------------- DanMachi (2)
    E("falna", "danmachi", "Falna", "Each victory is carved into the back as growth.", "main",
      k("kill_stacks", pct=0.015, max=5, decay=1200)),
    E("argonaut", "danmachi", "Argonaut", "Hold the charge, then release it in one blow.", "main",
      k("charge_hit", hits=6, pct=0.10)),
    # ---------------------------------------------------------------- Fairy Tail (3)
    E("guild_bond", "fairy_tail", "Guild Bond", "Comrades fight better beside each other.", "any",
      k("ally_regen", radius=8, interval=40, dur=80)),
    E("dragon_force", "fairy_tail", "Dragon Force", "Pain is only fuel for a mage of the guild.", "main",
      k("hit_bonus", cond="self_wounded", pct=0.08)),
    E("celestial_contract", "fairy_tail", "Celestial Contract", "The spirits repay a kept promise.", "held",
      k("spell_refund", pct=0.008, cd=140)),
    # ---------------------------------------------------------------- Shield Hero (3)
    E("vassal_bond", "shield_hero", "Vassal Bond", "A bearer who raises the shield is hard to move.", "any",
      k("shield_guard", pct=0.06)),
    E("tortoise_ward", "shield_hero", "Spirit Tortoise Ward", "The turtle's shell turns magic aside.", "any",
      k("ward", tag="magic", pct=0.06)),
    E("waves_hunter", "shield_hero", "Waves Hunter", "Born to answer the Waves: giants fall harder.", "main",
      k("hit_bonus", cond="target_big", pct=0.07)),
    # ---------------------------------------------------------------- Seven Deadly Sins (2)
    E("full_counter", "seven_deadly_sins", "Full Counter", "A blow taken is a blow returned.", "main",
      k("counter", pct=0.12, window=60)),
    E("sunshine", "seven_deadly_sins", "Sunshine", "At noon the strongest man alive steps forward.", "main",
      k("hit_bonus", cond="day", pct=0.08)),
    # ---------------------------------------------------------------- Dungeons and Dragons (3)
    E("natural_twenty", "dnd", "Natural Twenty", "Now and then the dice just fall your way.", "main",
      k("chance_roll", chance=0.05, cd=100, bonus=0.15)),
    E("sneak_attack", "dnd", "Sneak Attack", "A rogue strikes best from the shadows.", "main",
      k("hit_bonus", cond="sneaking", pct=0.10)),
    E("saving_throw", "dnd", "Saving Throw", "A lucky roll turns a blow aside.", "any",
      k("dodge", chance=0.05, cd=400)),
    # ---------------------------------------------------------------- Re:Zero (2)
    E("return_by_death", "rezero", "Return by Death", "At the edge, the world is set back a little.", "any",
      k("second_wind", threshold=0.25, heal=2.0, cd=3600)),
    E("unseen_hand", "rezero", "Unseen Hand", "Something unseen takes hold of a foe's step.", "main",
      k("debuff", effect="minecraft:slowness", chance=0.12, dur=40, cd=100)),
    # ---------------------------------------------------------------- Fate (2)
    E("noble_phantasm", "fate", "Noble Phantasm", "Blows build the legend, then the legend is released.", "main",
      k("charge_hit", hits=8, pct=0.12)),
    E("command_seal", "fate", "Command Seal", "One spent seal returns a breath of mana.", "held",
      k("spell_refund", pct=0.015, cd=600)),
    # ---------------------------------------------------------------- Overlord (2)
    E("aura_of_despair", "overlord", "Aura of Despair", "The sight of a supreme being saps the will.", "main",
      k("debuff", effect="minecraft:weakness", chance=0.15, dur=60, cd=100)),
    E("supreme_magic", "overlord", "Supreme Magic", "Spells cast by the highest ranks carry more weight.", "held",
      k("spell_power", pct=0.06, window=60)),
    # ---------------------------------------------------------------- KonoSuba (2)
    E("explosion", "konosuba", "Explosion!", "One grand blow, once the chant is over.", "main",
      k("charge_hit", hits=10, pct=0.15)),
    E("steal_skill", "konosuba", "Steal", "Take a little of what you cut.", "main",
      k("lifesteal", pct=0.04, cap=1.5, cd=20)),
    # ---------------------------------------------------------------- Mushoku Tensei (2)
    E("unrivalled_mage", "mushoku_tensei", "Unrivalled Mage", "Spells without a chant, a little sharper.", "held",
      k("spell_power", pct=0.06, window=60)),
    E("swordplay_rhythm", "mushoku_tensei", "Swordplay Rhythm", "Strike in time and each cut flows into the next.", "main",
      k("combo", pct=0.02, max=4, window=60)),
    # ---------------------------------------------------------------- The Eminence in Shadow (2)
    E("night_veil", "eminence_in_shadow", "Cloak of Night", "The shadow works best in the dark.", "main",
      k("hit_bonus", cond="night", pct=0.08)),
    E("shadow_garden", "eminence_in_shadow", "Shadow Garden", "Nobody saw where the blade went.", "any",
      k("dodge", chance=0.04, cd=400)),
    # ---------------------------------------------------------------- Tower of God (2)
    E("shinsu_flow", "tower_of_god", "Shinsu Flow", "Shinsu gathers around those who climb.", "any",
      k("mana_trickle", pct=0.004)),
    E("floor_climber", "tower_of_god", "Floor Climber", "Each fallen guardian is a step up.", "main",
      k("kill_haste", dur=80, cd=200)),
    # ---------------------------------------------------------------- Fullmetal Alchemist (2)
    E("equivalent_exchange", "fullmetal_alchemist", "Equivalent Exchange", "To gain something, something of equal worth is lost.", "main",
      k("blood_price", cost=1.0, min_hp=8.0, pct=0.12)),
    E("automail_plating", "fullmetal_alchemist", "Automail Plating", "Steel limbs turn arrows and bolts.", "any",
      k("ward", tag="projectile", pct=0.06)),
    # ---------------------------------------------------------------- Berserk (2)
    E("dragonslayer_weight", "berserk", "Dragonslayer's Weight", "A blade too big to be called a sword fells big things.", "main",
      k("hit_bonus", cond="target_big", pct=0.08)),
    E("strugglers_will", "berserk", "Struggler's Will", "Refuses to fall at the last step.", "any",
      k("second_wind", threshold=0.2, heal=2.0, cd=3600)),
    # ---------------------------------------------------------------- Sword Art Online and Log Horizon (2)
    E("sword_skill", "sao_log_horizon", "Sword Skill", "One skill leads straight into the next.", "main",
      k("combo", pct=0.025, max=4, window=50)),
    E("raid_chant", "sao_log_horizon", "Raid Chant", "A chanted spell lands a little harder.", "held",
      k("spell_power", pct=0.05, window=60)),
    # ---------------------------------------------------------------- Jujutsu Kaisen (2)
    E("black_flash", "jujutsu_kaisen", "Black Flash", "Sometimes the strike lands on the exact instant.", "main",
      k("chance_roll", chance=0.04, cd=120, bonus=0.15)),
    E("domain_pressure", "jujutsu_kaisen", "Domain Pressure", "The air weighs on whoever you face.", "main",
      k("debuff", effect="minecraft:slowness", chance=0.10, dur=40, cd=120)),
    # ---------------------------------------------------------------- Frieren (2)
    E("zoltraak", "frieren", "Zoltraak", "The common spell of the demon kind, honed over a thousand years.", "held",
      k("spell_power", pct=0.06, window=60)),
    E("mana_suppression", "frieren", "Mana Suppression", "A hidden mana pool is a hard one to read.", "any",
      k("ward", tag="magic", pct=0.05)),
    # ---------------------------------------------------------------- Delicious in Dungeon (2)
    E("cooks_harvest", "delicious_in_dungeon", "Cook's Harvest", "The dungeon always gives back something.", "main",
      k("kill_heal", heal=1.5, cd=60)),
    E("hearty_party", "delicious_in_dungeon", "Hearty Party", "A good table keeps a party going.", "any",
      k("ally_regen", radius=6, interval=60, dur=80)),
    # ---------------------------------------------------------------- Made in Abyss (2)
    E("delvers_resolve", "made_in_abyss", "Delver's Resolve", "Those who go down learn how to land.", "any",
      k("ward", tag="fall", pct=0.20)),
    E("hunters_whistle", "made_in_abyss", "Hunter's Whistle", "A whistle of the deepest rank carries weight against giants.", "main",
      k("hit_bonus", cond="target_big", pct=0.07)),
    # ---------------------------------------------------------------- Tomb Raider King (2)
    E("tomb_raiders_eye", "tomb_raider_king", "Tomb Raider's Eye", "A sharp eye finds the lucky moment.", "main",
      k("chance_roll", chance=0.05, cd=120, bonus=0.15)),
    E("relic_hunter", "tomb_raider_king", "Relic Hunter", "First contact is the cleanest one.", "main",
      k("first_strike", pct=0.07, window=200)),
    # ---------------------------------------------------------------- Yu-Gi-Oh! (2)
    E("heart_of_the_cards", "yu_gi_oh", "Heart of the Cards", "Believe in the deck and a card is drawn: a bonus effect.", "main",
      k("chance_roll", chance=0.07, cd=100, bonus=0.15, heal=1.0, haste=60, mana=0.01)),
    E("trap_card", "yu_gi_oh", "Trap Card", "A face-down card turns a blow aside.", "any",
      k("dodge", chance=0.05, cd=300)),
    # ---------------------------------------------------------------- Naruto (2)
    E("chakra_control", "naruto", "Chakra Control", "Well-kept chakra returns a little at a time.", "any",
      k("mana_trickle", pct=0.004)),
    E("substitution_jutsu", "naruto", "Substitution Jutsu", "A log is hit where you were.", "any",
      k("dodge", chance=0.06, cd=600)),
    # ---------------------------------------------------------------- Bleach (2)
    E("bankai", "bleach", "Bankai", "The final release, after enough blows were traded.", "main",
      k("charge_hit", hits=7, pct=0.12)),
    E("soul_burial", "bleach", "Soul Burial", "A soul reaper sends the dead on.", "main",
      k("hit_bonus", cond="undead", pct=0.12)),
    # ---------------------------------------------------------------- One Piece (2)
    E("armament_haki", "one_piece", "Armament Haki", "Haki hardens the body against blows.", "any",
      k("ward", tag="melee", pct=0.06)),
    E("observation_haki", "one_piece", "Observation Haki", "The next blow is felt before it lands.", "any",
      k("dodge", chance=0.05, cd=400)),
    # ---------------------------------------------------------------- Dragon Ball (2)
    E("zenkai", "dragon_ball", "Zenkai", "Blows taken make the next one stronger.", "main",
      k("counter", pct=0.10, window=100)),
    E("ki_control", "dragon_ball", "Ki Control", "Ki that is well controlled is not wasted.", "any",
      k("mana_trickle", pct=0.004)),
    # ---------------------------------------------------------------- Hunter x Hunter (2)
    E("nen_vow", "hunter_x_hunter", "Nen Vow", "A vow with a price makes the blow stronger.", "main",
      k("blood_price", cost=1.0, min_hp=10.0, pct=0.10)),
    E("gyo", "hunter_x_hunter", "Gyo", "Eyes full of aura see through a ward.", "main",
      k("anti_ward", pct=0.08)),
    # ---------------------------------------------------------------- JoJo (2)
    E("stand_rush", "jojo", "Stand Rush", "The barrage builds the longer it lasts.", "main",
      k("combo", pct=0.03, max=4, window=40)),
    E("life_giver", "jojo", "Life Giver", "A little life flows back from the cut.", "main",
      k("lifesteal", pct=0.03, cap=1.5, cd=20)),
    # ---------------------------------------------------------------- The Exiled Heavy Knight (2)
    E("heavy_plate_discipline", "exiled_heavy_knight", "Heavy Plate Discipline", "Blows glance off a well-built knight.", "any",
      k("ward", tag="melee", pct=0.07)),
    E("game_the_system", "exiled_heavy_knight", "Game the System", "Found the exploit: the first hit is the cheap one.", "main",
      k("first_strike", pct=0.07, window=200)),
]

# ---------------------------------------------------------------- text of an engraving's effects
COND_TEXT = {
    "target_big": "against big foes (60 or more health)", "self_wounded": "while you are at or under half health", "day": "in daylight",
    "night": "at night", "sneaking": "while sneaking", "undead": "against the undead", "always": "", "target_wounded": "against a foe at or under half health",
    "target_armoured": "against heavily armoured foes", "sprinting": "while sprinting", "airborne": "while falling", "wet": "in water or rain",
}
WARD_TEXT = {"magic": "magic", "projectile": "arrows and bolts", "fall": "falls", "melee": "melee blows", "explosion": "explosions", "fire": "fire"}


def pc(x):
    v = x * 100
    return ("%d%%" % round(v)) if abs(v - round(v)) < 0.05 else ("%.1f%%" % v)


def sec(t):
    return "%g s" % (t / 20.0)


def effect_text(e):
    kd = e["kind"]
    if kd == "hit_bonus":
        c = COND_TEXT.get(e["cond"], "")
        return ("+%s damage on your hits %s." % (pc(e["pct"]), c)).replace(" .", ".")
    if kd == "anti_ward":
        return "+%s damage against foes that carry a protective effect (resistance, absorption, regeneration)." % pc(e["pct"])
    if kd == "spell_refund":
        return "A spell cast returns %s of your maximum magicule (once every %s)." % (pc(e["pct"]), sec(e["cd"]))
    if kd == "spell_power":
        return "+%s damage on what your spell hits within %s of the cast." % (pc(e["pct"]), sec(e["window"]))
    if kd == "mana_trickle":
        return "Returns %s of your maximum magicule every 5 seconds." % pc(e["pct"])
    if kd == "chance_roll":
        outs = []
        if e.get("bonus"):
            outs.append("+%s damage on that hit" % pc(e["bonus"]))
        if e.get("heal"):
            outs.append("heals %g" % e["heal"])
        if e.get("haste"):
            outs.append("%s of Speed" % sec(e["haste"]))
        if e.get("mana"):
            outs.append("returns %s magicule" % pc(e["mana"]))
        return "%s chance on a hit (once every %s) of a bonus: %s." % (pc(e["chance"]), sec(e["cd"]), (" or ".join(outs)) if len(outs) < 3 else ", ".join(outs[:-1]) + " or " + outs[-1])
    if kd == "first_strike":
        return "+%s damage on the first hit against a foe you have not hit for %s." % (pc(e["pct"]), sec(e["window"]))
    if kd == "kill_heal":
        return "A kill heals %g (once every %s)." % (e["heal"], sec(e["cd"]))
    if kd == "kill_ally":
        return "%s chance on a kill (once every %s) to leave a short-lived ally for %s." % (pc(e["chance"]), sec(e["cd"]), sec(e["life"]))
    if kd == "kill_stacks":
        return "Each kill adds +%s damage, up to %d times; the growth fades %s after the last kill." % (pc(e["pct"]), e["max"], sec(e["decay"]))
    if kd == "charge_hit":
        return "Every hit charges the weapon; the %dth hit deals +%s damage and resets it." % (e["hits"], pc(e["pct"]))
    if kd == "ally_regen":
        return "Other players within %d blocks gain a faint regeneration (%s, refreshed every %s)." % (e["radius"], sec(e["dur"]), sec(e["interval"]))
    if kd == "shield_guard":
        return "-%s damage taken while you block or hold the shield." % pc(e["pct"])
    if kd == "ward":
        return "-%s damage taken from %s." % (pc(e["pct"]), WARD_TEXT.get(e["tag"], e["tag"]))
    if kd == "counter":
        return "After you take a hit, your next hit within %s deals +%s damage." % (sec(e["window"]), pc(e["pct"]))
    if kd == "dodge":
        return "%s chance to take no damage from a hit (once every %s)." % (pc(e["chance"]), sec(e["cd"]))
    if kd == "second_wind":
        return "When a hit leaves you at or under %s health, restores %g health (once every %s)." % (pc(e["threshold"]), e["heal"], sec(e["cd"]))
    if kd == "debuff":
        name = e["effect"].split(":")[1].replace("_", " ")
        return "%s chance on a hit (once every %s) to inflict %s for %s." % (pc(e["chance"]), sec(e["cd"]), name, sec(e["dur"]))
    if kd == "blood_price":
        return "Each hit costs you %g health (never below %g) and deals +%s damage." % (e["cost"], e["min_hp"], pc(e["pct"]))
    if kd == "combo":
        return "Hits within %s of each other stack +%s damage, up to %d times." % (sec(e["window"]), pc(e["pct"]), e["max"])
    if kd == "lifesteal":
        return "Heals %s of the damage you deal, at most %g per hit (once every %s)." % (pc(e["pct"]), e["cap"], sec(e["cd"]))
    if kd == "kill_haste":
        return "A kill grants %s of Speed (once every %s)." % (sec(e["dur"]), sec(e["cd"]))
    raise SystemExit("unknown kind " + kd)


SCOPE_TEXT = {"main": "Works while the item is in your main hand.", "held": "Works while the item is in either hand.",
              "any": "Works while the item is held, worn as armour or worn as a Curio."}


def describe(en):
    return " ".join([en["flavour"]] + [effect_text(e) for e in en["effects"]] + [SCOPE_TEXT[en["scope"]], "Effects are capped; stronger items carry a higher level (+25% per level)."])


def write_json(p, obj):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")


def enchantment_json(en):
    return {
        "description": {"translate": "enchantment.nusmp." + en["id"]},
        "supported_items": "#minecraft:enchantable/durability",
        "weight": 1,
        "max_level": 3,
        "min_cost": {"base": 1, "per_level_above_first": 0},
        "max_cost": {"base": 10, "per_level_above_first": 0},
        "anvil_cost": 1,
        "slots": ["any"],
        "effects": {},
    }


def main():
    fids = {f["id"] for f in FRANCHISES}
    ids = [e["id"] for e in ENGRAVINGS]
    assert len(ids) == len(set(ids)), "duplicate engraving id"
    for e in ENGRAVINGS:
        assert e["franchise"] in fids, e["id"]
        for fx in e["effects"]:
            effect_text(fx)
    for f in FRANCHISES:
        n = sum(1 for e in ENGRAVINGS if e["franchise"] == f["id"])
        assert n >= 2, f["id"] + " needs at least two engravings"
    lang = {}
    out_eng = []
    for e in ENGRAVINGS:
        write_json(ENCH / (e["id"] + ".json"), enchantment_json(e))
        lang["enchantment.nusmp." + e["id"]] = e["name"]
        lang["enchantment.nusmp." + e["id"] + ".desc"] = describe(e)
        out_eng.append(dict(id=e["id"], franchise=e["franchise"], name=e["name"], scope=e["scope"], effects=e["effects"]))
    for f in FRANCHISES:
        lang["franchise.nusmp." + f["id"]] = f["name"]
    lang["tooltip.nusmp.franchise"] = "Franchise: %s"
    write_json(FRAG, lang)
    write_json(OUT, {"version": 1, "comment": "Written by tools/franchise/gen_engravings.py: the franchises and the numbers of the franchise engravings (franchise/FranchiseData).",
                     "franchises": FRANCHISES, "engravings": out_eng})
    tag = json.loads(TAG.read_text(encoding="utf-8"))
    vals = tag["values"]
    for e in ENGRAVINGS:
        v = "nusmp:" + e["id"]
        if v not in vals:
            vals.append(v)                                                    # append only: the older entries stay where they are
    write_json(TAG, tag)
    print("franchises", len(FRANCHISES), "engravings", len(ENGRAVINGS))


if __name__ == "__main__":
    main()
