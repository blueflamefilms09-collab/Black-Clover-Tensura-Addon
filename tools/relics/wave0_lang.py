#!/usr/bin/env python3
"""Wave 0 (the sixteen items the mod already had, remade onto the relic system): writes the lang fragment
scratchpad/frag_relics/relics_wave0.lang.json (item.nusmp.<id>.card = the card as game rules, item.nusmp.<id>.desc = the short text,
item.nusmp.devil_contract = the card's name). The short text is read from Wave0.java so the two never drift apart.
Idempotent: python tools/relics/wave0_lang.py [output-path]"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
SRC = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "relics", "wave", "Wave0.java")
DEFAULT_OUT = "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad/frag_relics/relics_wave0.lang.json"

CARDS = {
 "quicklings_ivy": (
  "Requires attunement. A +1 finesse rapier: +1 attack damage and a little more attack speed. Every hit bites for 1d4 more poison damage. "
  "Sprout Ivy (the worn-relic key; six a day, fifteen seconds apart, the 2 per short rest of the card): the next hit within ten seconds sprouts a tangle of ivy "
  "that restrains the target. It must beat a Strength save (DC 16) or it is rooted, and it saves again every second for up to three seconds (one on a boss). "
  "A critical hit sprouts the ivy for free. Tensura: Sprout Ivy lends Sticky Steel Thread for ten seconds and the ivy adds Tensura's Webbed. "
  "The blade's own right-click Ensnaring Ivy, Thorn Flurry and engravings are unchanged. Tabletop became: a d20 save against the target's toughness (knockback "
  "resistance, health, armour toughness, +4 on a boss), a round is a second, a short rest is a cooldown."),
 "magebane_longsword": (
  "Requires attunement (any caster: here, anyone). A +2 blade (+2 attack damage). A blow on a creature that is casting (using an item) breaks it off, and a blow on one "
  "that holds a buff makes it save against Constitution with disadvantage (DC 10 or half the damage): on a failure it loses a buff, its skills are jammed and Tensura's "
  "Magic Interference lands on it. When any creature within 3.5 blocks begins a skill or a spell, the wielder strikes it as a reaction (once every three seconds). "
  "Set bonus, with the Magebane Shield attuned too: Dispel Magic once until the next dawn (the worn-relic key): on the creature you aim at or, with no one aimed at, on "
  "everything within 8 blocks, it strips every buff (the weak ones for sure), erases spell bolts and wards, jams skills, and lends Magic Jamming and Chant Annulment "
  "for thirty seconds. Tensura: Magic Sense is lent while it is wielded. The blade's own Dispel Surge, Spellbreak Ward and engravings are unchanged."),
 "magebane_shield": (
  "Requires attunement (any caster: here, anyone). +1 armour while held. Advantage on saves against spells: spell damage from a caster within five blocks is a fifth less, "
  "and a harmful effect from one is shrugged off more often (a save with advantage, DC 14). Tensura lends Magic Resistance while it is held. Absorb Elements (once until the "
  "next dawn, automatic): the first elemental blow (fire, cold, lightning) of the day is halved, the matching Tensura resistance skill is lent for thirty seconds, a Tensura "
  "Magic Barrier rises for ten, and your next melee hit returns 3d6 of that element. Set bonus, with the Magebane Longsword attuned too: when a spell or blast of 6 or more hurts "
  "you, you cover behind the shield (once until the next dawn): with the shield raised you take nothing, otherwise an even chance of nothing and else half. "
  "The shield's own blocking, Spellbreak Parry, Absorb Surge, the one-third magic cut and engravings are unchanged."),
 "bolt_thrower": (
  "Requires attunement. A crossbow of force: +2 attack damage, and every bolt (and the Thrower's Mark discharge) hits for 2 more force damage, plus 1d8 against a construct "
  "(a golem, a colossus, a statue). It needs no loading and no ammunition (as before). Tensura lends Farsight while it is held (the +2 to hit became sight). The Force Bolt, "
  "Triple Volley, Thrower's Marks and engravings are unchanged."),
 "brooch_pin": (
  "A dagger that folds into a brooch. Drawn in the hand it is a +1 dagger (+1 attack damage). Worn in a Curios slot and not drawn it passes for jewellery: creatures see you a "
  "quarter less well and Tensura's Presence Concealment is kept on you (the disadvantage on checks to tell it is a weapon). Unfolding is taking it from the Curios slot into the hand "
  "and folding is putting it back. The Piercing Pin, Ruby Ward, Backstab and engravings are unchanged."),
 "club_of_rot": (
  "A critical hit (a blow struck while falling) adds 2d6 necrotic damage, not to the dead or to constructs, and the target must save against Constitution (DC 15) or be "
  "sickened (Weakness II and Mining Fatigue, the disadvantage on attacks and checks); it saves again every second. Tensura lends Poison Resistance while it is held. "
  "Putrid Slam, Plague Cloud, the Rot that every hit inflicts, and the engravings are unchanged."),
 "crossbow_of_swift_death": (
  "Requires attunement. A bolt that strikes a surprised creature (asleep, struck from behind, or hunting someone else or no one) lands as a critical hit: 60% more damage. "
  "The creature must save against Wisdom (DC 12) or be frightened (a mob drops its target and runs, a player is slowed, Tensura's Fear lands on it), and saves again every second; "
  "a boss is only weakened. A frame found in the world carries 3d6 notches (+1% crit chance each, to +20%). Tensura lends Coercion while it is held. The crit and execution rules, "
  "the notch for every crit kill, Death Mark and the engravings are unchanged."),
 "doom_hammer": (
  "A blow that leaves a living creature (not undead, not a construct) under 35% of its health marks it doomed for eight seconds: it can not be stabilised, so it mends half as "
  "much, and Tensura's Curse lands on it. Under 25% the hammer adds 5d6 necrotic damage on top (it may well kill). Doom Slam, Sentence of Doom, the armour break and the engravings "
  "are unchanged."),
 "weapon_of_resurgence": (
  "Requires attunement. A +1 weapon that bonds with a paladin: attunement is the bond, and Tensura's Self Regeneration is lent while it is held. Every hit still returns a point of "
  "the Lay on Hands pool (three on the wicked). Lay on Hands (the worn-relic key): spends up to 10 points of the pool to mend the ally you look at (or yourself) 2 hit points a point, "
  "and 5 more lift an affliction. Resurgent Smite, Chains of Sanctity and the engravings are unchanged."),
 "hand_of_providence": (
  "Requires attunement; worn in the Curios hands slot. Tensura lends Holy Resistance while it is worn. Mage Hand (the worn-relic key): a spectral hand, shaped by your god, fetches every "
  "dropped item and orb of experience within 12 blocks (up to 16) to you, and lends Gravity Manipulation for twenty seconds. Mage Hand: Lay on Hands: the ally you aim at up to 18 blocks "
  "away (or you) is mended for 30% of their health (at least 8), regenerates, is cleansed and gains Tensura's Healthcare: Lay on Hands through the mage hand. Held in the hand, the gauntlet "
  "keeps its healing fists, Laying On of Hands, Providence Strike and engravings."),
 "devil_contract": (
  "Signed in blood, forever binding, one at a time. Use it as before to bargain with a devil of your grimoire (the three prices are unchanged), or sign one of the four Infernal "
  "Contracts of the card (click a term in the chat, /infernalpact, or the contract's acts). You can have only one contract or devil at a time; this is also the Devil bond of the Djinn system. "
  "Blood: your youth returns: a full heal, +2 hearts, +1 attack damage, +4% speed and +10% maximum aura. Riches: 64 Magic Stones, 8 medium and 4 high-quality crystals, 32 emeralds and 8 diamonds at once. "
  "Knowledge: Luck +2, Tensura's Sage kept lent, and the ritual Contact Other Plane (sneak and use a book, every five minutes): Sage, Analytical Appraisal and Magic Sense for a minute and every hostile "
  "presence within 40 blocks glows for ten seconds (a quarter of the time the answer is madness: nausea and weakness). Power: a 15% larger magicule pool, grimoire magic 20% cheaper, and at each new day "
  "three Tensura spells (flame, lightning, a barrier) are lent for ten minutes. Curse: Damned: when you die your soul goes into a crystal that drops where you fell; you return Soulless "
  "(30% less maximum health and weakened) until someone uses the crystal while you are in the world, or one day has passed."),
 "prism_blade": (
  "Requires attunement. Every hit adds 1d8 radiant damage (a swing counts once). Blinding Radiance (the worn-relic key; once a day): everything within 9 blocks saves against Constitution (DC 15) "
  "or is blinded (Tensura's Flashed Blindness too), and saves again every second. Chromatic Strike (three a day, thirty seconds apart): the next hit within ten seconds adds 4d8 radiant damage and the target saves "
  "against Wisdom (DC 15) or is frightened (or, if you were sneaking, charmed: it will not turn on you for the time it lasts). Tensura lends Light Resistance while it is held. The blade's own Blinding Flash, "
  "Chromatic Strike beam and engravings are unchanged."),
 "permafrost_sabre": (
  "Requires attunement. A +1 sabre. Cold damage is halved while it is held (and Tensura's Cold Resistance is lent). Every hit adds 1d6 cold damage and Tensura's Chill (2d6 as a set). "
  "Boreal Fusion (the worn-relic key), with the Cyclone Rapier in the other hand and both attuned: a 2.2 block whirlwind of frost forms in front of you for twelve seconds and wind and ice are lent to you "
  "(Wind Manipulation, Ice Breath). Creatures in it save against Strength (DC 16, with disadvantage within 1.6 blocks of you): on a failure 2d6 cold and 2d6 force and they are held, up to two medium "
  "creatures or one large one, and cut again every second, saving to break free. Use it again to move it up to 9 blocks toward where you aim, sneak to dismiss it. Twice a day. "
  "Whirlwind Frost, Permafrost Sweep and the engravings are unchanged. As a set the pair is Very Rare: here it earns the 2d6, Boreal Fusion and the whirlwind."),
 "cyclone_rapier": (
  "Requires attunement. A +1 light rapier (+1 attack damage, +0.3 attack speed). Every hit adds 1d6 force damage (2d6 as a set). Boreal Fusion (the worn-relic key), with the Permafrost Sabre in the other hand "
  "and both attuned: the whirlwind of frost, as on the sabre. Cyclone Thrust, Gale Lance and the engravings are unchanged."),
 "bloodbound_katana": (
  "Hits on a living creature (not undead, not a construct) store Blood Charges, up to 10 (the item's own rule, now with the dead and the made giving none); every charge bites on every blow (+0.8 damage). "
  "Blood Sip (toggle, the worn-relic key): while on, each hit costs you 1d4 health (never leaving you under two hearts) and gives one more charge, and Tensura's Drain is lent. Bloodflare (toggle): while on, each hit spends three "
  "charges for 1d8 necrotic damage, twice a hit at most. A full blade (10 charges) is unstable: each hit costs you a little blood. Bloodflare (right-click), Crimson Reaping and the engravings are unchanged. "
  "Tabletop became: the Charisma bonus to the maximum is the fixed ten; a free action is a toggle."),
 "thundercleave": (
  "Requires attunement. A +1 greatsword with two charges (a long rest is a day: they refill at dawn, or by one every ten seconds while you hold it under an open thunderstorm). Thunder Sweep (the worn-relic key): "
  "spend a charge and the blade cuts a wide arc: up to three creatures within 3.4 blocks take 2d6 thunder damage, are pushed back and deafened (Darkness and Tensura's Silence) for two seconds. Tensura lends "
  "Electricity Resistance while it is held. Thunder Arc, Thunderstorm and the engravings are unchanged."),
}

def main():
    src = open(SRC, encoding="utf-8").read()
    descs = dict(re.findall(r'RelicDef\.of\("([a-z_]+)".*?\.desc\("((?:[^"\\]|\\.)*)"\)', src, re.S))
    out = {}
    for rid, card in CARDS.items():
        d = descs.get(rid)
        if d is None:
            raise SystemExit("no .desc found for " + rid + " in Wave0.java")
        out["item.nusmp." + rid + ".card"] = card
        out["item.nusmp." + rid + ".desc"] = d.replace('\\"', '"')
    out["item.nusmp.devil_contract"] = "Infernal Contract"
    path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_OUT
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(out, f, indent=2, ensure_ascii=False)
    print("wrote", path, len(out), "keys")

if __name__ == "__main__":
    main()
