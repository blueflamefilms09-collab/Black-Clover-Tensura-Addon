#!/usr/bin/env python3
"""Writes src/main/resources/assets/anime_multiverse/devil_kits.json: the abilities and passives of every devil of the roster, from the anime it is from
and its magic aspect (docs/devil_mastery.md). The table below is the source; DevilKitTest validates the output (shapes, bounds, the anime and aspect of each devil).

    python3 -B tools/devil/gen_devil_kits.py            write the file
    python3 -B tools/devil/gen_devil_kits.py --check    exit 1 if the file on disk differs from what this script writes
"""
import json, os, sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "src/main/resources/assets/anime_multiverse/devil_kits.json")

def fx(i, t, a=0): return ["minecraft:" + i, t, a]

def art(id, name, kind, dmg, cd, cost, rng, canon, fxs=(), special="", self=(), tier=None):
    d = {"id": id, "name": name, "kind": kind, "dmg": dmg, "cd": cd, "cost": cost, "range": rng, "fx": list(fxs), "special": special, "self": list(self), "canon": canon}
    if tier is not None: d["tier"] = tier
    return d

def stat(id, name, tier, attr, v, canon): return {"id": id, "name": name, "tier": tier, "kind": "stat", "type": attr, "value": v, "canon": canon}
def resist(id, name, tier, typ, v, canon): return {"id": id, "name": name, "tier": tier, "kind": "resist", "type": typ, "value": v, "canon": canon}
def regen(id, name, tier, v, canon): return {"id": id, "name": name, "tier": tier, "kind": "regen", "value": v, "canon": canon}
def onhit(id, name, tier, f, canon): return {"id": id, "name": name, "tier": tier, "kind": "onhit", "fx": f, "canon": canon}

def kit(anime, aspect, source, arts, passives, stages, unionName="", union=()):
    for i, a in enumerate(arts):
        a.setdefault("tier", i)
    d = {"anime": anime, "aspect": aspect, "source": source, "arts": arts, "passives": passives, "stages": stages}
    if union:
        d["unionName"] = unionName
        d["union"] = [dict(u, tier=4) for u in union]
    else:
        d["unionName"] = unionName or "Devil Union"
    return d

BC = "Black Clover"
S_BC = ("English wiki (blackclover.fandom.com) NOT reachable from the build machine; names and powers from web-search summaries of it and of sportskeeda, comicbook.com, fandomwire, animehunch, epicdope; "
        "Japanese wiki NOT reachable. Numbers are this mod's design.")
S_CSM = "Chainsaw Man wiki NOT reachable; names from web-search summaries and the wiki's article titles as remembered (Fox Devil Kon, Aki's Curse Devil nail and hammer, Aki's Future Devil right eye, Himeno's Ghost Devil arm). Numbers are design."
S_BB = "Kuroshitsuji wiki NOT reachable; Sebastian's silver cutlery, speed and soul-eating from the series as summarised by web search. Numbers are design."
S_BE = "Ao no Exorcist wiki NOT reachable; the Demon Kings' domains (Iblis fire, Egyn water, Amaimon earth, Astaroth rot, Lucifer light) from web-search summaries. The 'kin of' devils and their arts are design."
S_TS = "Tensura wiki NOT reachable; the seven Primordials and their colours from web-search summaries. The 'daemon of a line' devils and their arts are design."
S_DR = "Dororo wiki NOT reachable; Hyakkimaru's hidden arm blades and the Hall of Hell pact from web-search summaries. Numbers are design."
S_BK = "Berserk wiki NOT reachable; the God Hand, the Behelit and the Eclipse from web-search summaries. Numbers are design."
S_FT = "Fairy Tail wiki NOT reachable; Etherious and Zeref's books from web-search summaries. Numbers are design."

D = {}

# ------------------------------------------------------------------ Black Clover
D["liebe"] = kit(BC, "anti_magic", S_BC, [
    art("black_slash", "Black Slash", "strike", 1.0, 8, 0.4, 7, "Asta's anti-magic blade: the cut cancels the magic it passes through.", [fx("weakness", 80, 0)], "strip"),
    art("black_hurricane", "Black Hurricane", "burst", 0.9, 14, 0.6, 6, "Asta's whirling swing that throws everything around him back.", [fx("slowness", 60, 1)], "push"),
    art("black_divider", "Black Divider", "rush", 1.3, 20, 0.8, 9, "Asta's lunging slash that splits magic in two.", [], "strip"),
    art("black_meteorite", "Black Meteorite", "burst", 2.0, 45, 1.4, 8, "Asta's heavy anti-magic slam from above.", [fx("slowness", 60, 2)], "launch")],
    [resist("anti_magic_skin", "Anti-Magic Skin", 0, "magic", 0.25, "Liebe's anti-magic: spells lose their bite on the host."),
     stat("mana_zone", "Mana Zone", 2, "speed", 0.12, "Asta's Mana Zone: he feels the opponent's flow and moves ahead of it."),
     regen("never_give_up", "Never Give Up", 3, 0.3, "Asta's endurance: the host keeps standing (design number).")],
    ["Black Asta", "Anti-Magic Aura", "Devil Union"], "Devil Union: Liebe", [
        stat("union_swords", "Upgraded Swords", 4, "attack", 5.0, "The Devil Union upgrades every sword Asta holds (Demon-Slayer, Demon-Dweller, Demon-Destroyer)."),
        resist("union_anti_magic", "Union Anti-Magic Coat", 4, "magic", 0.25, "In the union the anti-magic covers the whole body.")])

D["lucifero"] = kit(BC, "gravity", S_BC, [
    art("gravity_crush", "Gravity Crush", "strike", 1.0, 8, 0.4, 8, "Dante's gravity magic pins and drags foes in front of the host.", [fx("slowness", 80, 2)], "pull"),
    art("heavy_field", "Heavy Field", "burst", 1.0, 16, 0.6, 7, "A field where the weight of everything is multiplied.", [fx("slowness", 100, 3)], ""),
    art("devils_descent", "Devil's Descent", "rush", 1.4, 22, 0.9, 10, "The king of devils falls on his prey.", [], "launch"),
    art("black_hole", "Singularity", "burst", 2.0, 45, 1.5, 9, "Gravity folded into a point that eats everything near it.", [fx("nausea", 80, 0)], "pull")],
    [resist("weightless_body", "Weightless Body", 0, "fall", 0.5, "A gravity devil's host does not feel the ground's pull."),
     stat("heavy_fist", "Heavy Fist", 2, "attack", 2.0, "Blows that carry their own weight."),
     stat("devil_king", "King of Devils", 3, "health", 4.0, "Lucifero, the strongest of the devils, makes his host harder to kill.")],
    ["Devil's Horns", "Gravity Wings", "Devil Union"], "Devil Union: Lucifero", [
        stat("union_lord", "Underworld King's Might", 4, "attack", 6.0, "A union with Lucifero carries the strongest devil's force."),
        onhit("union_weight", "Crushing Weight", 4, fx("slowness", 60, 2), "In the union every blow adds weight to the foe.")])

D["beelzebub"] = kit(BC, "spatial", S_BC, [
    art("spatial_slash", "Spatial Slash", "strike", 1.0, 8, 0.4, 9, "Zenon's spatial magic cuts space itself; the foe is shoved back.", [fx("nausea", 60, 0)], "push"),
    art("rift_step", "Rift Step", "rush", 1.1, 14, 0.6, 12, "A step through folded space, striking what stands at the other end.", [], ""),
    art("space_prison", "Space Prison", "burst", 0.8, 24, 0.9, 7, "Space closed around the foes: they can barely move.", [fx("slowness", 100, 4)], ""),
    art("dimension_cut", "Dimension Cut", "strike", 2.0, 40, 1.4, 12, "A cut that crosses worlds.", [], "")],
    [stat("spatial_sense", "Spatial Sense", 0, "reach", 1.0, "A space devil's host reaches further than their arm."),
     stat("fold_step", "Fold Step", 2, "speed", 0.15, "Short folds of space make every step longer."),
     resist("space_veil", "Space Veil", 3, "projectile", 0.3, "Arrows and bolts bend around the host.")],
    ["Horns of the Rift", "Wings of the Rift", "Devil Union"], "Devil Union: Beelzebub", [
        stat("union_rift", "Ruler of the Underworld", 4, "attack", 5.0, "A union with one of the three strongest devils."),
        resist("union_fold", "Folded Skin", 4, "physical", 0.2, "Blows land where the host is not.")])

D["astaroth"] = kit(BC, "time", S_BC, [
    art("time_slow", "Slowed Time", "strike", 0.8, 8, 0.4, 8, "Time crawls for the foes in front of the host.", [fx("slowness", 80, 3), fx("mining_fatigue", 60, 2)], ""),
    art("time_skip", "Time Skip", "rush", 1.2, 14, 0.6, 10, "The host is simply somewhere else a moment later.", [], ""),
    art("stasis", "Stasis", "burst", 0.8, 28, 1.0, 7, "Everything around the host is held for a breath.", [fx("slowness", 80, 4), fx("mining_fatigue", 80, 3)], ""),
    art("quickened", "Quickened Time", "ward", 0, 40, 1.0, 4, "The host's own time runs faster.", [], "", [fx("speed", 160, 2), fx("haste", 160, 1)])],
    [stat("quick_hands", "Quick Hands", 0, "speed", 0.10, "Time devils' hosts move a little ahead of everyone."),
     stat("borrowed_moments", "Borrowed Moments", 2, "attack", 2.0, "Strikes arrive before the foe sees them."),
     regen("time_mend", "Rewinding Wounds", 3, 0.3, "Wounds close as if time ran back (design number).")],
    ["Hourglass Horns", "Clockwork Wings", "Devil Union"], "Devil Union: Astaroth", [
        stat("union_time", "Master of Time", 4, "speed", 0.2, "A union with the time devil: the host lives a second ahead."),
        onhit("union_stop", "Stopped Blow", 4, fx("slowness", 40, 4), "Blows stop the foe's time for a moment.")])

D["megicula"] = kit(BC, "curse", S_BC, [
    art("curse_mark", "Curse Mark", "strike", 1.0, 8, 0.4, 8, "Vanica's curse magic marks the foe; it rots from within.", [fx("wither", 100, 1)], ""),
    art("ward_breaker", "Ward Breaker", "burst", 0.9, 16, 0.6, 6, "Protections of those near the host are torn off.", [], "strip"),
    art("curse_chain", "Cursed Chain", "strike", 1.3, 22, 0.9, 9, "A chain of cursed light binds the foe.", [fx("slowness", 80, 3), fx("weakness", 80, 1)], "pull"),
    art("megicula_wrath", "Megicula's Wrath", "burst", 2.0, 45, 1.4, 8, "The curse devil's wrath pours out of the host.", [fx("wither", 120, 2)], "")],
    [resist("curse_ward", "Curse-Warding", 0, "magic", 0.25, "Megicula is a curse-warding devil: curses slide off its host."),
     stat("hexed_edge", "Hexed Edge", 2, "attack", 2.0, "The host's hits carry a little of the curse."),
     onhit("withering_touch", "Withering Touch", 3, fx("wither", 60, 0), "Touch of the curse devil: wounds do not close at once.")],
    ["Cursed Horns", "Cursed Wings", "Devil Union"], "Devil Union: Megicula", [
        stat("union_curse", "Curse-Warding Union", 4, "attack", 4.5, "A union with Megicula: the curse is the host's weapon."),
        resist("union_ward", "Curse Wards", 4, "magic", 0.25, "The union turns every curse aside.")])

D["lucifugus"] = kit(BC, "word", S_BC, [
    art("word_bind", "Word of Binding", "strike", 0.8, 8, 0.4, 8, "Kotodama: the spoken word binds the foe's limbs.", [fx("slowness", 80, 2), fx("weakness", 80, 1)], ""),
    art("word_silence", "Word of Silence", "burst", 0.8, 16, 0.6, 7, "Kotodama: a command of silence that strips protections.", [fx("blindness", 60, 0)], "strip"),
    art("word_fall", "Word of Falling", "strike", 1.2, 24, 0.9, 9, "Kotodama: 'fall'; and the foe falls.", [fx("slowness", 60, 4)], "launch"),
    art("word_law", "Law of Words", "ward", 0, 40, 1.0, 4, "Kotodama: the host speaks their own safety into being.", [], "", [fx("resistance", 160, 1), fx("absorption", 200, 2)])],
    [resist("spoken_shield", "Spoken Shield", 0, "magic", 0.2, "What the host says will not happen to them, mostly does not."),
     stat("commanding_voice", "Commanding Voice", 2, "attack", 2.0, "Orders land with the weight of a blow."),
     regen("word_heal", "Word of Mending", 3, 0.3, "Kotodama: 'be healed' (design number).")],
    ["Horns of the Word", "Wings of the Word", "Devil Union"], "Devil Union: Lucifugus", [
        stat("union_word", "Kotodama Unbound", 4, "attack", 4.5, "In the union a spoken word is as strong as a strike."),
        onhit("union_command", "Command", 4, fx("weakness", 60, 2), "Every blow carries an order the foe has to obey.")])

# ------------------------------------------------------------------ Chainsaw Man
CM = "Chainsaw Man"
D["fox_devil"] = kit(CM, "beast", S_CSM, [
    art("kon_bite", "Kon!", "strike", 1.0, 8, 0.4, 6, "The Fox Devil bites at the call of 'Kon'.", [fx("slowness", 40, 1)], "pull"),
    art("fox_head", "Giant Fox Head", "burst", 1.2, 18, 0.7, 6, "A fox's giant head swallows what stands near the host.", [], "push"),
    art("hunting_pounce", "Hunting Pounce", "rush", 1.3, 20, 0.8, 9, "The fox lunges, and the host with it.", [], ""),
    art("fox_hunt", "Fox Hunt", "strike", 1.8, 40, 1.2, 8, "The whole pack bites at once.", [fx("weakness", 80, 1)], "")],
    [stat("keen_nose", "Keen Nose", 0, "speed", 0.08, "A fox's quick feet."),
     onhit("fox_fangs", "Fox Fangs", 2, fx("slowness", 40, 1), "Bites that do not let go."),
     stat("devil_pelt", "Devil Pelt", 3, "armor", 3.0, "The devil's thick pelt.")],
    ["Fox Ears", "Fox Tail", "Fox Form"], "Fox Form")

D["curse_devil"] = kit(CM, "curse", S_CSM, [
    art("nail_hammer", "Nail and Hammer", "strike", 1.2, 8, 0.5, 8, "Aki's Curse Devil: a nail hammered in; the wound opens after.", [fx("wither", 100, 1)], ""),
    art("straw_doll", "Straw Doll", "burst", 1.0, 16, 0.6, 6, "The curse jumps from foe to foe near the host.", [fx("wither", 80, 1)], ""),
    art("hammer_blow", "Hammer Blow", "rush", 1.4, 22, 0.9, 9, "The hammer comes down on the nail.", [], "launch"),
    art("years_paid", "Years Paid", "ward", 0, 40, 1.0, 4, "The Curse Devil's price is paid in years: the host takes them back as strength.", [], "", [fx("strength", 200, 1), fx("absorption", 200, 1)])],
    [stat("lifespan_price", "Price of Years", 0, "attack", 2.0, "Aki pays years of his life for the Curse Devil's power."),
     onhit("slow_curse", "Slow Curse", 2, fx("wither", 60, 0), "Wounds of the Curse Devil keep eating."),
     resist("curse_hide", "Curse Hide", 3, "magic", 0.25, "The Curse Devil's host turns curses back.")],
    ["Cursed Nail", "Cursed Hammer", "Curse Form"], "Curse Form")

D["future_devil"] = kit(CM, "foresight", S_CSM, [
    art("future_sight", "Future Sight", "ward", 0, 20, 0.5, 4, "Aki's right eye: he sees the next blow and steps out of it.", [], "", [fx("resistance", 120, 1), fx("speed", 120, 1)]),
    art("foretold_strike", "Foretold Strike", "strike", 1.1, 10, 0.5, 8, "A strike that lands where the foe will be.", [fx("glowing", 100, 0)], ""),
    art("seen_twice", "Seen Twice", "burst", 1.0, 20, 0.8, 6, "Everything near the host is seen a moment ahead.", [fx("slowness", 60, 2)], ""),
    art("destiny_cut", "Destiny Cut", "rush", 1.8, 40, 1.2, 10, "The host runs the one line that ends the fight.", [], "")],
    [resist("dodge_sense", "Dodge Sense", 0, "physical", 0.12, "The right eye warns of blows."),
     stat("next_step", "Next Step", 2, "speed", 0.1, "Steps taken before the foe moves."),
     resist("clear_eye", "Clear Eye", 3, "projectile", 0.3, "Shots are seen leaving the bow.")],
    ["Right Eye Opens", "Future Eye", "Future Form"], "Future Form")

D["ghost_devil"] = kit(CM, "ghost", S_CSM, [
    art("ghost_hands", "Ghost Hands", "strike", 0.8, 8, 0.4, 8, "Himeno's Ghost Devil: a giant hand reaches from nowhere and holds the foe.", [fx("slowness", 80, 4)], "pull"),
    art("grasping_arms", "Grasping Arms", "burst", 0.8, 16, 0.6, 6, "Hands rise around the host and hold everything.", [fx("slowness", 60, 3)], ""),
    art("ghost_vanish", "Ghost Vanish", "ward", 0, 30, 0.8, 4, "The host fades from sight.", [], "", [fx("invisibility", 160, 0), fx("speed", 100, 1)]),
    art("ghost_rush", "Ghost Rush", "rush", 1.2, 24, 0.9, 10, "Hands carry the host through the foes.", [], "")],
    [stat("ghostly_step", "Ghostly Step", 0, "speed", 0.08, "Light as a haunting."),
     resist("intangible", "Intangible", 2, "physical", 0.12, "Part of every blow passes through."),
     stat("ghost_grip", "Ghost Grip", 3, "attack", 1.5, "A hand always helps the host's.")],
    ["Ghost Hand", "Ghost Arms", "Ghost Form"], "Ghost Form")

# ------------------------------------------------------------------ Black Butler
D["sebastian"] = kit("Black Butler", "soul", S_BB, [
    art("silver_cutlery", "Silver Cutlery", "strike", 1.0, 8, 0.4, 12, "Sebastian throws silverware by the dozen with perfect aim.", [], ""),
    art("demonic_speed", "Demonic Speed", "rush", 1.3, 14, 0.6, 11, "A butler is always where he is needed.", [], ""),
    art("soul_reaping", "Soul Reaping", "strike", 1.4, 24, 0.9, 7, "A taste of the soul he is owed: the wound feeds the host.", [fx("weakness", 60, 1)], "drain"),
    art("hell_of_a_butler", "One Hell of a Butler", "ward", 0, 40, 1.0, 4, "Nothing is permitted to go wrong.", [], "", [fx("speed", 200, 1), fx("regeneration", 120, 1), fx("resistance", 200, 1)])],
    [resist("composure", "Composure", 0, "physical", 0.15, "Not a hair out of place."),
     stat("flawless_service", "Flawless Service", 2, "attack", 3.0, "Every strike is carried out perfectly."),
     regen("hellish_vigor", "Hellish Vigour", 3, 0.4, "Demonic recovery.")],
    ["Horns of Service", "Wings of Service", "True Form"], "True Form")

# ------------------------------------------------------------------ Blue Exorcist
BE = "Blue Exorcist"
D["kin_of_iblis"] = kit(BE, "flame", S_BE, [
    art("blue_flames", "Blue Flames", "burst", 1.0, 10, 0.5, 6, "Iblis, King of Fire: blue flame that burns what it touches.", [], "burn"),
    art("flame_lance", "Flame Lance", "strike", 1.1, 10, 0.5, 9, "A lance of blue fire.", [], "burn"),
    art("flame_rush", "Flame Rush", "rush", 1.3, 20, 0.8, 9, "The host streaks through the foes on a trail of flame.", [], "burn"),
    art("king_of_fire", "Wrath of the Fire King", "burst", 2.0, 45, 1.4, 8, "A ring of fire from the host.", [], "burn")],
    [resist("flame_skin", "Flame Skin", 0, "fire", 0.5, "A flame demon is not hurt by fire."),
     stat("burning_heart", "Burning Heart", 2, "attack", 2.0, "Flames feed the blows."),
     regen("ember_mend", "Ember Mending", 3, 0.3, "Blue flame closes wounds.")],
    ["Flame Horns", "Flame Wings", "Fire King's Form"], "Fire King's Form")

D["kin_of_egyn"] = kit(BE, "water", S_BE, [
    art("water_whip", "Water Whip", "strike", 0.9, 8, 0.4, 9, "Egyn, King of Water: a whip of water.", [fx("mining_fatigue", 80, 1)], "push"),
    art("tidal_wave", "Tidal Wave", "burst", 1.0, 16, 0.6, 7, "A wave from the host.", [fx("slowness", 60, 2)], "push"),
    art("drowning_grip", "Drowning Grip", "strike", 1.3, 24, 0.9, 8, "Water closes in around the foe.", [fx("weakness", 80, 2), fx("slowness", 80, 2)], "pull"),
    art("flowing_ward", "Flowing Ward", "ward", 0, 40, 1.0, 4, "Water takes the blow's strength.", [], "", [fx("resistance", 180, 1), fx("regeneration", 100, 1)])],
    [resist("water_body", "Water Body", 0, "physical", 0.12, "Blows sink into the flow."),
     stat("swift_current", "Swift Current", 2, "speed", 0.1, "Quick as a stream."),
     regen("spring_mend", "Spring Mending", 3, 0.3, "Clean water heals.")],
    ["Tide Horns", "Tide Wings", "Water King's Form"], "Water King's Form")

D["kin_of_amaimon"] = kit(BE, "earth", S_BE, [
    art("earth_fist", "Earth Fist", "strike", 1.1, 8, 0.4, 6, "Amaimon, King of Earth: a fist of stone.", [fx("slowness", 40, 1)], "push"),
    art("quake", "Quake", "burst", 1.1, 16, 0.6, 7, "The ground jumps.", [fx("slowness", 60, 2)], "launch"),
    art("stone_skin", "Stone Skin", "ward", 0, 30, 0.8, 4, "The host is rock.", [], "", [fx("resistance", 180, 2)]),
    art("earth_king", "Fall of the Earth King", "burst", 2.0, 45, 1.4, 8, "A mountain falls on the foes.", [fx("slowness", 80, 3)], "")],
    [stat("heavy_frame", "Heavy Frame", 0, "armor", 2.0, "A body of earth."),
     stat("earth_fists", "Earth Fists", 2, "attack", 2.0, "Fists that hit like stones."),
     stat("unmoving", "Unmoving", 3, "knockback", 0.4, "Hard to push.")],
    ["Stone Horns", "Stone Wings", "Earth King's Form"], "Earth King's Form")

D["kin_of_astaroth"] = kit(BE, "rot", S_BE, [
    art("rot_miasma", "Rot Miasma", "burst", 0.8, 10, 0.4, 6, "Astaroth, King of Rot: a cloud that spoils flesh.", [fx("poison", 100, 1)], ""),
    art("spoiled_touch", "Spoiled Touch", "strike", 0.9, 10, 0.5, 7, "A touch that rots.", [fx("poison", 80, 1), fx("weakness", 60, 0)], ""),
    art("decay_rush", "Decay Rush", "rush", 1.0, 20, 0.8, 8, "A rush through a cloud of decay.", [fx("poison", 60, 1)], ""),
    art("plague_wave", "Plague Wave", "burst", 1.6, 40, 1.2, 8, "A wave of rot.", [fx("wither", 100, 1)], "")],
    [resist("rot_hide", "Rot Hide", 0, "magic", 0.15, "Nothing living clings to a rotting thing."),
     onhit("festering", "Festering Wounds", 2, fx("poison", 60, 0), "Blows that fester."),
     regen("scavenge", "Scavenge", 3, 0.25, "What rots feeds the host.")],
    ["Rot Horns", "Rot Wings", "Rot King's Form"], "Rot King's Form")

D["kin_of_lucifer"] = kit(BE, "light", S_BE, [
    art("holy_light", "Holy Light", "strike", 1.1, 8, 0.4, 10, "Lucifer, King of Light: a beam that marks the foe.", [fx("glowing", 100, 0)], ""),
    art("dawn_burst", "Dawn Burst", "burst", 1.1, 16, 0.6, 7, "A flash that blinds.", [fx("blindness", 60, 0)], "push"),
    art("light_step", "Light Step", "rush", 1.3, 20, 0.8, 11, "The host travels as light.", [], ""),
    art("radiant_ward", "Radiant Ward", "ward", 0, 40, 1.0, 4, "A veil of light.", [], "", [fx("resistance", 180, 1), fx("absorption", 200, 2)])],
    [resist("light_veil", "Light Veil", 0, "magic", 0.2, "Light turns dark magic aside."),
     stat("light_foot", "Light Foot", 2, "speed", 0.12, "Quick as a ray."),
     stat("morning_star", "Morning Star", 3, "attack", 2.5, "The King of Light's strength.")],
    ["Halo Horns", "Wings of Light", "Light King's Form"], "Light King's Form")

# ------------------------------------------------------------------ Tensura
TS = "That Time I Got Reincarnated as a Slime"
def daemon(aspect, a1, a2, a3, a4, p1, p2, p3):
    return kit(TS, aspect, S_TS, [a1, a2, a3, a4], [p1, p2, p3], ["Daemon Horns", "Daemon Wings", "Primordial Form"], "Primordial Form")

D["daemon_noir"] = daemon("curse",
    art("abyssal_curse", "Abyssal Curse", "strike", 1.0, 8, 0.4, 9, "The black Primordial's curse.", [fx("wither", 100, 1)], ""),
    art("gluttonous_maw", "Gluttonous Maw", "burst", 1.1, 16, 0.6, 7, "The black Primordial devours what is near.", [fx("weakness", 80, 1)], "drain"),
    art("black_decree", "Black Decree", "ward", 0, 36, 1.0, 4, "A Primordial's word is law: the host is hard to harm.", [], "", [fx("resistance", 200, 2), fx("absorption", 200, 2)]),
    art("primordial_nova", "Primordial Nova", "burst", 2.0, 45, 1.5, 9, "A black sun at the host's feet.", [fx("wither", 120, 2)], ""),
    resist("noir_ward", "Noir Ward", 0, "magic", 0.25, "The line of Noir shrugs spells off."),
    stat("noir_might", "Primordial Might", 2, "attack", 2.5, "A daemon's strength."),
    regen("noir_regen", "Daemon Recovery", 3, 0.4, "Daemons mend quickly."))
D["daemon_blanc"] = daemon("light",
    art("white_lance", "White Lance", "strike", 1.0, 8, 0.4, 10, "A lance of white light.", [fx("glowing", 100, 0)], ""),
    art("pure_flash", "Pure Flash", "burst", 1.0, 16, 0.6, 7, "A blinding flash.", [fx("blindness", 60, 0)], "push"),
    art("white_rush", "White Rush", "rush", 1.3, 20, 0.8, 10, "Light itself.", [], ""),
    art("blanc_aegis", "Blanc's Aegis", "ward", 0, 40, 1.0, 4, "A shell of white light.", [], "", [fx("resistance", 180, 1), fx("absorption", 200, 2)]),
    resist("white_veil", "White Veil", 0, "magic", 0.2, "Light repels dark magic."),
    stat("blanc_speed", "Light Speed", 2, "speed", 0.1, "Daemon quickness."),
    stat("blanc_edge", "Radiant Edge", 3, "attack", 2.0, "Light in every blow."))
D["daemon_jaune"] = daemon("flame",
    art("yellow_flare", "Yellow Flare", "strike", 1.0, 8, 0.4, 9, "A bolt of yellow fire.", [], "burn"),
    art("flame_ring", "Flame Ring", "burst", 1.0, 16, 0.6, 7, "A ring of fire.", [], "burn"),
    art("blazing_dash", "Blazing Dash", "rush", 1.3, 20, 0.8, 9, "A charge in a trail of fire.", [], "burn"),
    art("jaune_inferno", "Jaune's Inferno", "burst", 2.0, 45, 1.4, 8, "A storm of yellow fire.", [], "burn"),
    resist("fire_blood", "Fire Blood", 0, "fire", 0.5, "A fire daemon does not burn."),
    stat("jaune_heat", "Burning Strength", 2, "attack", 2.0, "Heat behind blows."),
    regen("jaune_mend", "Cinder Mending", 3, 0.3, "Fire cauterises."))
D["daemon_violet"] = daemon("rot",
    art("violet_miasma", "Violet Miasma", "burst", 0.8, 10, 0.4, 6, "A cloud of purple spoilage.", [fx("poison", 100, 1)], ""),
    art("rotting_touch", "Rotting Touch", "strike", 1.0, 10, 0.5, 7, "A touch that spoils.", [fx("poison", 80, 1), fx("weakness", 60, 0)], ""),
    art("decay_dash", "Decay Dash", "rush", 1.1, 20, 0.8, 8, "A dash through spoilage.", [fx("poison", 60, 1)], ""),
    art("violet_plague", "Violet Plague", "burst", 1.7, 42, 1.3, 8, "A plague on the foes.", [fx("wither", 100, 1)], ""),
    resist("violet_hide", "Violet Hide", 0, "magic", 0.15, "Spells rot before they land."),
    onhit("violet_fester", "Festering Blows", 2, fx("poison", 60, 0), "Wounds fester."),
    stat("violet_might", "Plague Might", 3, "attack", 2.0, "A daemon's strength."))
D["daemon_rouge"] = daemon("soul",
    art("crimson_reap", "Crimson Reap", "strike", 1.1, 8, 0.4, 8, "The red Primordial reaps a little of the foe's life.", [fx("weakness", 60, 1)], "drain"),
    art("blood_tide", "Blood Tide", "burst", 1.1, 16, 0.6, 7, "A tide of red that feeds the host.", [], "drain"),
    art("crimson_rush", "Crimson Rush", "rush", 1.4, 20, 0.8, 10, "A red streak.", [], ""),
    art("rouge_feast", "Rouge's Feast", "burst", 2.0, 45, 1.5, 9, "A feast on every foe near the host.", [fx("weakness", 100, 2)], "drain"),
    stat("rouge_vigor", "Crimson Vigour", 0, "health", 4.0, "Life taken is life kept."),
    stat("rouge_might", "Primordial Might", 2, "attack", 2.5, "A daemon's strength."),
    regen("rouge_regen", "Blood Mending", 3, 0.5, "The red line heals fastest."))
D["daemon_vert"] = daemon("earth",
    art("vert_thorn", "Green Thorn", "strike", 1.0, 8, 0.4, 8, "A thorn of the earth.", [fx("slowness", 60, 1)], "push"),
    art("vert_quake", "Vert Quake", "burst", 1.1, 16, 0.6, 7, "The ground jumps.", [fx("slowness", 60, 2)], "launch"),
    art("bark_skin", "Bark Skin", "ward", 0, 30, 0.8, 4, "The host is wood and stone.", [], "", [fx("resistance", 180, 2)]),
    art("vert_avalanche", "Vert Avalanche", "burst", 2.0, 45, 1.4, 8, "A slide of rock.", [fx("slowness", 80, 3)], ""),
    stat("vert_frame", "Rooted Frame", 0, "armor", 2.0, "A sturdy body."),
    stat("vert_fists", "Stone Fists", 2, "attack", 2.0, "Heavy blows."),
    stat("vert_still", "Unshaken", 3, "knockback", 0.4, "Hard to move."))
D["daemon_bleu"] = daemon("water",
    art("bleu_whip", "Blue Whip", "strike", 0.9, 8, 0.4, 9, "A whip of water.", [fx("mining_fatigue", 80, 1)], "push"),
    art("bleu_wave", "Bleu Wave", "burst", 1.0, 16, 0.6, 7, "A wave of the blue Primordial.", [fx("slowness", 60, 2)], "push"),
    art("bleu_grip", "Drowning Grip", "strike", 1.3, 24, 0.9, 8, "Water closes in around the foe.", [fx("weakness", 80, 2), fx("slowness", 80, 2)], "pull"),
    art("bleu_ward", "Bleu's Ward", "ward", 0, 40, 1.0, 4, "A shell of water.", [], "", [fx("resistance", 180, 1), fx("regeneration", 100, 1)]),
    resist("bleu_body", "Water Body", 0, "physical", 0.12, "Blows sink into the flow."),
    stat("bleu_current", "Swift Current", 2, "speed", 0.1, "Quick as a river."),
    regen("bleu_spring", "Spring Mending", 3, 0.3, "Clean water heals."))

# ------------------------------------------------------------------ Dororo, Berserk, Fairy Tail
D["hall_demon"] = kit("Dororo", "beast", S_DR, [
    art("hidden_blades", "Hidden Blades", "strike", 1.1, 8, 0.4, 5, "Hyakkimaru's arms hide swords: a short, fast cut.", [], ""),
    art("blade_dance", "Blade Dance", "burst", 1.1, 14, 0.6, 5, "A whirl of blades on both arms.", [], "push"),
    art("demon_hunt", "Demon Hunt", "rush", 1.4, 20, 0.8, 9, "A charge with the blades out.", [], ""),
    art("prosperity_shield", "Price of Prosperity", "ward", 0, 40, 1.0, 4, "The Hall's wealth turned to armour.", [], "", [fx("absorption", 200, 3), fx("resistance", 160, 1)])],
    [stat("borrowed_body", "Borrowed Body", 0, "health", 4.0, "The Hall of Hell gives a body for the price of a body."),
     stat("blade_arms", "Blade Arms", 2, "attack", 2.5, "Arms that are swords."),
     resist("demon_hide", "Demon Hide", 3, "physical", 0.15, "A hide of the Hall.")],
    ["Demon's Brow", "Blade Arms", "Hall Form"], "Hall Form")

D["god_hand"] = kit("Berserk", "causality", S_BK, [
    art("fates_hand", "Fate's Hand", "strike", 1.1, 8, 0.4, 9, "The God Hand closes its fist: fortune turns from the foe.", [fx("unluck", 200, 1), fx("weakness", 60, 1)], "pull"),
    art("sacrificial_rite", "Sacrificial Rite", "burst", 1.2, 18, 0.7, 7, "What is dear is taken.", [fx("wither", 80, 1)], "drain"),
    art("causal_step", "Causal Step", "rush", 1.4, 20, 0.8, 11, "The host arrives because it had already happened.", [], ""),
    art("eclipse", "Eclipse", "burst", 2.2, 50, 1.6, 10, "The sun goes out.", [fx("blindness", 80, 0), fx("slowness", 80, 3)], "")],
    [resist("inevitable", "Inevitable", 0, "physical", 0.15, "A blow is only as bad as fate lets it be."),
     stat("hand_of_fate", "Hand of Fate", 2, "attack", 3.0, "Blows land because they had to."),
     regen("branded_life", "Branded Life", 3, 0.4, "What is sacrificed returns in another form.")],
    ["Brand's Heat", "Behelit's Wings", "God Hand Form"], "God Hand Form")

D["etherious"] = kit("Fairy Tail", "ether", S_FT, [
    art("ethernano_blast", "Ethernano Blast", "strike", 1.1, 8, 0.4, 10, "Raw ethernano loosed from the host.", [fx("levitation", 30, 0)], "push"),
    art("books_of_zeref", "Book of Zeref", "burst", 1.1, 16, 0.6, 7, "A page of the book tears open.", [fx("slowness", 60, 2)], "push"),
    art("etherious_dash", "Etherious Dash", "rush", 1.3, 20, 0.8, 10, "A burst of speed from a demon's body.", [], ""),
    art("etherious_release", "Etherious Release", "burst", 2.0, 45, 1.5, 9, "The demon's true form released for an instant.", [fx("weakness", 100, 1)], "launch")],
    [resist("curse_blood", "Demon Blood", 0, "magic", 0.2, "An Etherious is made of magic and shrugs it off."),
     stat("ethernano_surge", "Ethernano Surge", 2, "attack", 2.5, "More magic behind each strike."),
     regen("etherious_heal", "Etherious Recovery", 3, 0.4, "Demons close their own wounds.")],
    ["Demon Horns", "Demon Wings", "Etherious Form"], "Etherious Form")

# ------------------------------------------------------------------ double unions (two held devils, the Devil Union of the first)
FUSIONS = [
    {"a": "lucifero", "b": "megicula", "name": "Dark Triad Union: Gravity and Curse", "canon": False,
     "note": "Canon: Dante hosts Lucifero and Vanica hosts Megicula (siblings of the Dark Triad, web-search summaries of sportskeeda / epicdope). A union of both at once is not shown in the series: DESIGN.",
     "passives": [stat("fusion_weight_curse", "Weighted Curse", 4, "attack", 3.0, "Gravity and curse together."), onhit("fusion_drag", "Dragging Curse", 4, fx("wither", 60, 1), "Cursed blows are heavy.")]},
    {"a": "lucifero", "b": "beelzebub", "name": "Dark Triad Union: Gravity and Space", "canon": False,
     "note": "Canon: Dante hosts Lucifero and Zenon hosts Beelzebub. A fused union is not shown in the series: DESIGN.",
     "passives": [stat("fusion_fold_weight", "Folded Weight", 4, "attack", 3.0, "Space and gravity folded on a single blow."), stat("fusion_reach", "Folded Reach", 4, "reach", 1.0, "Space lengthens the arm.")]},
    {"a": "megicula", "b": "beelzebub", "name": "Dark Triad Union: Curse and Space", "canon": False,
     "note": "Canon: Vanica hosts Megicula and Zenon hosts Beelzebub. A fused union is not shown in the series: DESIGN.",
     "passives": [stat("fusion_hex_rift", "Hexed Rift", 4, "attack", 3.0, "Curses that cross space."), resist("fusion_ward", "Spatial Ward", 4, "magic", 0.2, "Curses and spells cannot find the host.")]},
    {"a": "liebe", "b": "astaroth", "name": "Anti-Magic Time Union", "canon": False,
     "note": "Canon: Asta's Devil Union with Liebe (comicbook.com / sportskeeda). A second union with a time devil is not shown in the series: DESIGN.",
     "passives": [stat("fusion_cancel_time", "Cancelled Time", 4, "speed", 0.15, "Anti-magic and time together."), onhit("fusion_strip_slow", "Stripping Stop", 4, fx("slowness", 40, 3), "Blows that cancel and hold.")]},
]

def build():
    return {"version": 1, "doc": "docs/devil_mastery.md. Written by tools/devil/gen_devil_kits.py.", "devils": D, "fusions": FUSIONS}

if __name__ == "__main__":
    text = json.dumps(build(), indent=1, ensure_ascii=False) + "\n"
    if "--check" in sys.argv:
        cur = open(OUT, encoding="utf-8").read() if os.path.exists(OUT) else ""
        sys.exit(0 if cur == text else 1)
    open(OUT, "w", encoding="utf-8").write(text)
    print("wrote", OUT, len(D), "devils", len(FUSIONS), "fusions")
