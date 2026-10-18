"""The advancement tree "Multiverse of Anime" (single source of truth; gen_advancements.py writes the JSON and the lang fragment).

Why: the tree is large (hundreds of nodes, 105 of them one per magic family), so it is built from short tables and loops instead of
hand written JSON; the quest pack (spec_quests*.py) points its advancement tasks at these paths, which keeps the two in sync.

Branches (folders): start, magic, spells, arcs, bosses, relics, weapons, dungeon, spirits, squads, world, challenges, future.
"""
from spec_lib import _crit
from spec_lib import (Spec, ev, ev_any, ev_all, tick, impossible, have, killed, boss_kill, dimension, talk, effect, consume, slug,
                      _crit)
from spec_magic import MAGICS, RESTRICTED, BRANCHES

S = Spec()
add = S.add

ROOT_BG = "nusmp:textures/block/grimoire_altar_top.png"


def AND(*crits):
    """All of several criteria specs (each keeps its own all / any meaning)."""
    lst, req = [], []
    for i, c in enumerate(crits):
        names = []
        for n, t, cond in c["list"]:
            nn = "%s%d_%s" % ("abcdefghijklmnop"[i], len(lst), n)
            lst.append((nn, t, cond))
            names.append(nn)
        if c["mode"] == "any":
            req.append(names)
        else:
            req.extend([n] for n in names)
    return {"mode": "req", "list": lst, "req": req}


def anyof(*crits):
    """Any one of several criteria specs: the union of their criteria, satisfied by whichever happens first."""
    lst = []
    for i, c in enumerate(crits):
        for n, t, cond in c["list"]:
            lst.append(("%s%d_%s" % ("abcdefghijklmnop"[i], len(lst), n), t, cond))
    return {"mode": "any", "list": lst}


# ================================================================================================================= root
add("root", None, "Multiverse of Anime",
    "Grimoires, relics and dungeons of many worlds. Every spell you cast and every foe you fell is written down here.",
    "nusmp:grimoire", tick(), frame="task", toast=False, chat=False, background=ROOT_BG)

# ================================================================================================================ start
st = "start/"
add(st + "first_grimoire", "root", "A Grimoire of Your Own", "Receive your grimoire, the book that will carry your magic.",
    "nusmp:grimoire", ev("grimoire.received"), xp=10)
add(st + "open_book", st + "first_grimoire", "Open the Cover", "Open your grimoire and read its first pages.",
    "nusmp:grimoire_summons", ev("grimoire.opened"), xp=5)
add(st + "ceremony", st + "first_grimoire", "The Acceptance Ceremony",
    "Take part in the Grimoire Acceptance Ceremony and let a grimoire choose you.",
    "nusmp:grimoire_altar", ev("ceremony.completed"), frame="goal", xp=30)
add(st + "questionnaire", st + "first_grimoire", "The Wizard's Questions",
    "Answer every question of the Tower Wizard and learn which magic is born in you.",
    "minecraft:enchanted_book", ev("wizard.questionnaire"), xp=15)
add(st + "meet_wizard", st + "first_grimoire", "A Word with the Wizard", "Speak with the wizard of a Grimoire Tower.",
    "minecraft:book", talk("nusmp:tower_wizard"), xp=5)
add(st + "first_spell", st + "open_book", "First Spell", "Cast a spell from your grimoire.",
    "minecraft:blaze_powder", ev("spell.cast"), xp=10)
add(st + "mana_use", st + "first_spell", "Spending Mana", "Cast twenty-five spells. Every spell costs mana, and mana refills slowly.",
    "minecraft:lapis_lazuli", ev("spell.cast", 25), xp=10)
add(st + "casts_50", st + "first_spell", "Practice Makes Perfect", "Cast fifty spells. Mastery comes from successful casts.",
    "minecraft:experience_bottle", ev("spell.cast", 50), xp=20)
add(st + "casts_500", st + "casts_50", "Tireless Caster", "Cast five hundred spells.", "minecraft:nether_star",
    ev("spell.cast", 500), frame="goal", xp=75)
add(st + "first_page", st + "first_spell", "A New Page", "Unlock a new page of your grimoire through mastery.",
    "minecraft:writable_book", ev("page.unlocked"), xp=10)
add(st + "pages_5", st + "first_page", "Five Pages Deep", "Unlock five pages.", "minecraft:written_book",
    ev("page.unlocked", 5), xp=25)
add(st + "pages_10", st + "pages_5", "A Full Grimoire", "Unlock ten pages: a grimoire that has grown with its master.",
    "minecraft:enchanted_book", ev("page.unlocked", 10), frame="goal", xp=60)
add(st + "mastery", st + "pages_10", "Mastery", "Fill the mastery bar of a grimoire completely.", "minecraft:nether_star",
    ev("mastery.max"), frame="challenge", xp=200)
add(st + "magic_stone", st + "first_grimoire", "Crystallised Mana", "Pick up a magic stone, the dungeon's currency.",
    "nusmp:magic_stone", have("nusmp:magic_stone"), xp=5)
add(st + "five_grimoires", st + "first_grimoire", "Collector of Covers", "Receive five grimoires over your journeys.",
    "minecraft:lectern", ev("grimoire.received", 5), frame="goal", xp=40)

# ================================================================================================================ magic
GROUPS = [
    # id, name, hub icon, blurb
    ("flame", "Flame Soul", "minecraft:blaze_powder", "fire, blast and molten stone"),
    ("water", "Water Soul", "minecraft:water_bucket", "tides, frost, mist and silver"),
    ("wind", "Wind Soul", "minecraft:feather", "gales, stars, storms and wings"),
    ("earth", "Earth Soul", "minecraft:oak_sapling", "stone, plants, metal and crystal"),
    ("light", "Light Soul", "minecraft:glowstone_dust", "light, lightning and the sword"),
    ("darkness", "Darkness Soul", "minecraft:ink_sac", "shadow, curses, venom and demons"),
    ("space", "Space Soul", "minecraft:ender_pearl", "rifts, mirrors, gravity and barriers"),
    ("time", "Time Soul", "minecraft:clock", "the hour, seals and wards"),
    ("battle", "Battle Soul", "minecraft:iron_sword", "bodies, bones, blood and steel"),
    ("fantasy", "Fantasy Soul", "minecraft:enchanted_book", "dreams, threads, songs and tricks"),
    ("empty", "Empty Soul", "minecraft:barrier", "the magics no soul is born with"),
]
G_ICON = {g[0]: g[2] for g in GROUPS}
FAM = {}
for mid, name, grp in MAGICS:
    FAM.setdefault(grp, []).append((mid, name))

add("magic/awakening", "root", "The Awakening of Magic", "Cast three spells and let the world learn your name.",
    "nusmp:grimoire", ev("spell.cast", 3), frame="goal", xp=15)
for gid, gname, gicon, blurb in GROUPS:
    fams = FAM[gid]
    add("magic/soul_" + gid, "magic/awakening", gname,
        "Cast any magic of the %s (%s)." % (gname, blurb), gicon, ev_any(*[("spell.cast", m) for m, _ in fams]), frame="goal", xp=20)
    for mid, name in fams:
        restricted = mid in RESTRICTED
        add("magic/cast_" + mid, "magic/soul_" + gid, name + " Magic",
            ("Cast a spell of %s Magic. This magic is never rolled for a soul: it comes from a devil, a command or an admin." % name)
            if restricted else "Cast a spell of %s Magic." % name,
            gicon, ev("spell.cast", 1, mid), frame="challenge" if restricted else "task", xp=60 if restricted else 8)
    add("magic/signature_" + gid, "magic/soul_" + gid, gname + ": Signature",
        "Cast a signature page of any magic of the %s." % gname, gicon,
        ev_any(*[("spell.cast.signature", m) for m, _ in fams]), frame="goal", xp=40)
    add("magic/master_" + gid, "magic/signature_" + gid, gname + ": All Magics",
        "Cast every magic of the %s at least once (%d in all)." % (gname, len(fams)), gicon,
        ev_all(*[("spell.cast", m) for m, _ in fams]), frame="challenge", xp=150 + 10 * len(fams))
for n, ttl, desc, fr, xp in [(5, "Five Kinds of Magic", "Cast five different magics.", "task", 20),
                             (10, "Ten Kinds of Magic", "Cast ten different magics.", "goal", 40),
                             (25, "Two-and-Twenty-Five", "Cast twenty-five different magics.", "goal", 80),
                             (50, "Half a Hundred", "Cast fifty different magics.", "challenge", 200),
                             (105, "Every Magic There Is", "Cast all 105 magics of the Multiverse.", "challenge", 1000)]:
    add("magic/distinct_%d" % n, "magic/awakening", ttl, desc, "minecraft:nether_star" if n >= 50 else "minecraft:enchanted_book",
        ev("spell.distinct", n), frame=fr, xp=xp)

# the nine mastery branches used by the quest chapter "Magic Mastery": one signature and one all-casts advancement each
NAME = {m: n for m, n, _ in MAGICS}
for bid, btitle, bicon, bblurb, bfams in BRANCHES:
    add("magic/branch_%s_signature" % bid, "magic/awakening", btitle + ": Signature",
        "Cast a signature page of any magic in this branch (%s)." % bblurb, bicon,
        ev_any(*[("spell.cast.signature", m) for m in bfams]), frame="goal", xp=40)
    add("magic/branch_%s_all" % bid, "magic/branch_%s_signature" % bid, btitle + ": Every Magic",
        "Cast each of the %d magics of this branch at least once (%s)." % (len(bfams), ", ".join(NAME[m] for m in bfams[:4]) + ", ..."),
        bicon, ev_all(*[("spell.cast", m) for m in bfams]), frame="challenge", xp=100 + 8 * len(bfams))

# =============================================================================================================== spells
sp = "spells/"
add(sp + "signature", "root", "A Signature Spell", "Cast the signature page of a grimoire: the spell a mage is known by.",
    "minecraft:nether_star", ev("spell.cast.signature"), frame="goal", xp=30)
add(sp + "signature_10", sp + "signature", "Known by Name", "Cast signature pages ten times.", "minecraft:beacon",
    ev("spell.cast.signature", 10), xp=40)
add(sp + "signature_50", sp + "signature_10", "A Legend in the Making", "Cast signature pages fifty times.", "minecraft:beacon",
    ev("spell.cast.signature", 50), frame="goal", xp=100)
add(sp + "merged", sp + "signature", "Two Magics, One Spell", "Merge two magics into a single spell.", "minecraft:smithing_table",
    ev("magic.merged"), frame="goal", xp=60)
add(sp + "merged_5", sp + "merged", "Alchemist of Magic", "Merge magics five times.", "minecraft:smithing_table",
    ev("magic.merged", 5), frame="challenge", xp=150)
add(sp + "engraved", sp + "signature", "Engraved", "Engrave a weapon at the Relic Forge with a Tensura engraving.", "minecraft:grindstone",
    ev("weapon.engraved"), xp=25)
CHAR = [
    ("dimension_slash_equinox", "Yami: Dimension Slash", "Unlock Yami Sukehiro's dimension-cutting page."),
    ("unite_canis", "Nacht: Unite, Canis", "Unlock Nacht Faust's shadow-beast page."),
    ("thunder_fiend", "Luck: Thunder Fiend", "Unlock Luck Voltia's thunder page."),
    ("curse_workers_neighbor", "Gordon: Curse Workers", "Unlock Gordon Agrippa's page."),
    ("raging_black_bull", "Henry: Raging Black Bull", "Unlock Henry Legolant's page."),
    ("revelation_of_the_cowardly", "Zora: Revelation", "Unlock Zora Ideale's page."),
    ("leo_rugiens", "Fuegoleon: Leo Rugiens", "Unlock the Vermillion lion page."),
    ("spirit_of_zephyr", "Yuno: Spirit of Zephyr", "Unlock Yuno's wind-spirit page."),
    ("yggdrasil", "William: Yggdrasil", "Unlock William Vangeance's World Tree page."),
    ("ripper_cut", "Jack: Ripper Cut", "Unlock Jack the Ripper's page."),
    ("painted_menagerie", "Rill: Painted Menagerie", "Unlock Rill Boismortier's painting page."),
    ("glamour_world", "Dorothy: Glamour World", "Unlock Dorothy Unsworth's dream page."),
    ("vortex_shield", "Kaiser: Vortex Shield", "Unlock Kaiser Granvorka's storm page."),
    ("mercury_rain", "Nozel: Mercury Rain", "Unlock Nozel Silva's mercury page."),
    ("branching_array", "Secre: Branching Array", "Unlock Secre Swallowtail's sealing page."),
    ("thread_trap", "Vanessa: Thread Trap", "Unlock Vanessa Enoteca's thread page."),
    ("paladin_conversion", "Lucius: Paladin Conversion", "Unlock Lucius Zogratis' soul page."),
    ("true_soul_corpse", "Rades: True Soul Corpse", "Unlock Rades Spirito's soul-corpse page."),
    ("song_crescendo", "Kahono: Song Crescendo", "Unlock Kahono's song page."),
    ("butoh_dance", "Kiato: Butoh Dance", "Unlock Kiato's dance page."),
]
add(sp + "characters", sp + "signature", "Walk in Their Shoes", "Unlock a page that belongs to one of the canon mages.",
    "minecraft:player_head", ev_any(*[("page.unlocked", pid) for pid, _, _ in CHAR]), frame="goal", xp=40)
for pid, ttl, desc in CHAR:
    add(sp + "page_" + pid, sp + "characters", ttl, desc, "minecraft:writable_book", ev("page.unlocked", 1, pid), xp=15)
add(sp + "characters_all", sp + "characters", "The Whole Roster", "Unlock the canon page of every mage listed under Walk in Their Shoes.",
    "minecraft:totem_of_undying", ev_all(*[("page.unlocked", pid) for pid, _, _ in CHAR]), frame="challenge", xp=500)
SPECIAL = [
    # id, title, desc, icon, event key, event arg
    ("butoh", "The Dance Begins", "Start a Butoh dance.", "minecraft:note_block", "butoh.dance", None),
    ("song", "A Song Takes Hold", "Sing a Song Magic song and let it pulse.", "minecraft:jukebox", "song.sung", None),
    ("corpse", "The Dead Rise", "Raise a soul corpse.", "minecraft:wither_skeleton_skull", "corpse.raised", None),
    ("paladin", "Paladin Conversion", "Turn a soul into a paladin.", "minecraft:totem_of_undying", "paladin.converted", None),
    ("rouge", "Rouge Is Safe", "Save Rouge, the cat of the thread witch.", "minecraft:string", "rouge.saved", None),
    ("thread_trap", "Caught in the Web", "Spring a thread trap.", "minecraft:string", "thread.trap", None),
    ("shadow_travel", "Through the Shadows", "Travel through the shadows.", "minecraft:ender_eye", "shadow.travel", None),
    ("time_stop", "Stop the Clock", "Stop time.", "minecraft:clock", "time.stop", None),
    ("unite_canis", "Unite: Canis", "Take the Canis form.", "minecraft:bone", "unite.mode", "canis"),
    ("unite_equus", "Unite: Equus", "Take the Equus form.", "minecraft:saddle", "unite.mode", "equus"),
    ("unite_felis", "Unite: Felis", "Take the Felis form.", "minecraft:string", "unite.mode", "felis"),
    ("unite_gallus", "Unite: Gallus", "Take the Gallus form.", "minecraft:feather", "unite.mode", "gallus"),
]
add(sp + "special", sp + "signature", "Strange Arts", "Use one of the mod's special arts.", "minecraft:amethyst_shard",
    ev_any(*[(k, a) for _, _, _, _, k, a in SPECIAL]), frame="goal", xp=20)
for key, ttl, desc, icon, k, a in SPECIAL:
    add(sp + "art_" + key, sp + "special", ttl, desc, icon, ev(k, 1, a), xp=10)
add(sp + "unite_all", sp + "special", "Four Forms of the Shadow", "Take all four Unite forms: Canis, Equus, Felis and Gallus.",
    "minecraft:ender_eye",
    ev_all(("unite.mode", "canis"), ("unite.mode", "equus"), ("unite.mode", "felis"), ("unite.mode", "gallus")),
    frame="challenge", xp=200)

# =============================================================================================================== bosses
bo = "bosses/"
BOSSES = [
    # entity path, title, desc, icon, frame, xp, kind ('event' = boss_kill, 'kill' = vanilla kill only is not used)
    ("dante_zogratis", "Dante, Gravity Incarnate", "Defeat Dante Zogratis, the gravity of the Dark Triad.", "nusmp:dante_gravity_crown", "goal", 150),
    ("vanica_zogratis", "Vanica, Mistress of Curses", "Defeat Vanica Zogratis, the curse-weaver.", "minecraft:ender_eye", "goal", 150),
    ("zenon_zogratis", "Zenon, the Spatial Hunter", "Defeat Zenon Zogratis, who walks between the gates.", "nusmp:zenon_spatial_ring", "goal", 150),
    ("lucius_zogratis", "Lucius, the Final Hour", "Defeat Lucius Zogratis in his arena.", "nusmp:zogratis_hourglass", "challenge", 600),
    ("zagred", "Zagred, the Devil of the Word", "Defeat Zagred.", "nusmp:last_word", "challenge", 500),
    ("devil_avatar", "The Devil's Avatar", "Defeat the Devil Avatar.", "nusmp:devil_horn", "challenge", 400),
    ("goliath", "Goliath", "Defeat Goliath, the stone giant that guards a floor of the dungeon.", "minecraft:heavy_core", "goal", 200),
    ("charybdis_spawn", "Spawn of Charybdis", "Defeat the Spawn of Charybdis in its basin.", "minecraft:nautilus_shell", "goal", 200),
    ("floor_warden", "The Floor Warden", "Defeat a Warden of the Labyrinth.", "minecraft:iron_bars", "goal", 120),
    ("god_of_order", "The God of Order", "Defeat the God of Order, judge of the relic ledger.", "nusmp:notice_of_order", "challenge", 600),
    ("wind_spirit_lord", "Lord of the Gale", "Defeat the Wind Spirit Lord.", "minecraft:feather", "goal", 120),
    ("grimoire_daemon", "Daemon of the Grimoire", "Defeat a Grimoire Daemon.", "minecraft:wither_skeleton_skull", "goal", 80),
]
add(bo + "first", "root", "First Blood", "Defeat any boss of the Multiverse.", "minecraft:iron_sword", ev("boss.defeated"), xp=40)
for path, ttl, desc, icon, fr, xp in BOSSES:
    add(bo + path, bo + "first", ttl, desc, icon, boss_kill(path), frame=fr, xp=xp)
add(bo + "triad", bo + "dante_zogratis", "The Dark Triad Falls", "Defeat Dante, Vanica and Zenon.", "nusmp:charter_grimoire_dante",
    AND(boss_kill("dante_zogratis"), boss_kill("vanica_zogratis"), boss_kill("zenon_zogratis")), frame="challenge", xp=400)
add(bo + "count_3", bo + "first", "Bounty Hunter", "Defeat three bosses.", "minecraft:crossbow", ev("boss.defeated", 3), xp=60)
add(bo + "count_10", bo + "count_3", "Slayer of Legends", "Defeat ten bosses.", "minecraft:netherite_sword", ev("boss.defeated", 10),
    frame="goal", xp=150)
add(bo + "count_25", bo + "count_10", "Calamity", "Defeat twenty-five bosses.", "minecraft:wither_skeleton_skull", ev("boss.defeated", 25),
    frame="challenge", xp=400)
add(bo + "lucius_stage2", bo + "lucius_zogratis", "The Second Stage", "Push Lucius into his second stage.", "nusmp:zogratis_hourglass",
    ev("boss.phase", 1, "lucius:2"), xp=100)
add(bo + "lucius_stage3", bo + "lucius_stage2", "The Third Stage", "Push Lucius into his third and final stage.",
    "nusmp:zogratis_hourglass", ev("boss.phase", 1, "lucius:3"), frame="goal", xp=200)
add(bo + "zagred_phase2", bo + "zagred", "Zagred Unbound", "Push Zagred into his second phase.", "nusmp:last_word",
    ev("boss.phase", 1, "zagred:2"), xp=80)
add(bo + "all", bo + "count_25", "No Boss Left Standing", "Defeat every boss the Multiverse has: all twelve of them.",
    "minecraft:dragon_egg",
    AND(*[boss_kill(p) for p, *_ in BOSSES]), frame="challenge", xp=1500)

# =============================================================================================================== relics
re_ = "relics/"
add(re_ + "first", "root", "Relic of Many Worlds", "Obtain a relic. They are rare: each one exists only once in the whole world.",
    "nusmp:bag_of_holding", ev("relic.obtained"), frame="goal", xp=50)
add(re_ + "equipped", re_ + "first", "Attuned", "Wear or wield a relic and let it answer you.", "nusmp:ring_of_power",
    ev("relic.equipped"), xp=20)
GR = [("normal", "Normal Grade"), ("special", "Special Grade"), ("rare", "Rare Grade"), ("unique", "Unique Grade"),
      ("legend", "Legend Grade"), ("god", "God Grade"), ("genesis", "Genesis Grade")]
prev = re_ + "first"
for i, (g, ttl) in enumerate(GR):
    add(re_ + "grade_" + g, prev, ttl, "Hold a relic of %s." % ttl, "nusmp:ring_of_power" if i < 3 else "minecraft:nether_star",
        ev("relic.grade", 1, g), frame="task" if i < 4 else ("goal" if i < 6 else "challenge"), xp=10 + 30 * i * i // 2)
    prev = re_ + "grade_" + g
add(re_ + "count_10", re_ + "first", "A Small Hoard", "Obtain ten relics over time.", "nusmp:bag_of_holding", ev("relic.obtained", 10), xp=60)
add(re_ + "count_30", re_ + "count_10", "A Museum of Many Worlds", "Obtain thirty relics over time.", "nusmp:bag_of_holding",
    ev("relic.obtained", 30), frame="goal", xp=150)
add(re_ + "forge_block", re_ + "first", "The Relic Forge", "Carry a Relic Forge.", "nusmp:relic_forge", have("nusmp:relic_forge"), xp=15)
add(re_ + "forged", re_ + "forge_block", "Reforged", "Use the forge to engrave or reforge a relic.", "minecraft:anvil",
    ev("relic.forged"), frame="goal", xp=40)
add(re_ + "socketed", re_ + "forged", "Socketed", "Set a socket core into a relic.", "minecraft:amethyst_shard",
    ev("relic.socketed"), xp=40)
add(re_ + "tier", re_ + "socketed", "Tier Ritual", "Raise a relic through a tier ritual.", "minecraft:beacon", ev("relic.tier"),
    frame="goal", xp=80)
add(re_ + "djinn", re_ + "grade_legend", "A Djinn Answers", "Bind a Djinn to a metal vessel.", "nusmp:djinn_seal_baal",
    ev("relic.djinn_bound"), frame="goal", xp=100)
SEALS = ["amon", "baal", "bifron", "focalor", "furfur", "marchosias", "paimon", "sitri", "vassago", "vinea", "zagan", "zepar"]
add(re_ + "djinn_seals", re_ + "djinn", "Twelve Seals of the Djinn", "Carry all twelve Djinn seals.", "nusmp:djinn_seal_paimon",
    have(*["nusmp:djinn_seal_" + x for x in SEALS], mode="all"), frame="challenge", xp=400)
add(re_ + "pedestal", re_ + "first", "On a Pedestal", "Carry a Relic Pedestal.", "nusmp:relic_pedestal", have("nusmp:relic_pedestal"), xp=15)
add(re_ + "ledger", re_ + "first", "In the Ledger", "Be entered in the Unique Ledger of the God of Order.", "nusmp:notice_of_order",
    ev("relic.unique_ledger"), xp=40)
add(re_ + "pair", re_ + "equipped", "A Matched Pair", "Carry both halves of a relic pair: the Twin Hook Swords, right and left.",
    "nusmp:twin_hook_swords", have("nusmp:twin_hook_swords", "nusmp:twin_hook_swords_left", mode="all"), frame="goal", xp=60)
add(re_ + "pairs_all", re_ + "pair", "Three Pairs", "Carry both halves of the Twin Hook Swords, the Whirling Blades and the Twin Swords of Deflection.",
    "nusmp:whirling_blades",
    have("nusmp:twin_hook_swords", "nusmp:twin_hook_swords_left", "nusmp:whirling_blades", "nusmp:whirling_blades_left",
         "nusmp:twin_swords_of_deflection", "nusmp:twin_swords_of_deflection_left", mode="all"), frame="challenge", xp=200)
add(re_ + "vecna", re_ + "equipped", "The Pair of Vecna", "Carry both the Hand and the Eye of Vecna, a bonded pair with a set bonus.", "nusmp:hand_of_vecna",
    have("nusmp:hand_of_vecna", "nusmp:eye_of_vecna", mode="all"), frame="challenge", xp=300)
STONES = ["the_mind_stone", "the_power_stone", "the_reality_stone", "the_soul_stone", "the_space_stone_or_the_tesseract",
          "the_time_stone_or_the_eye_of_agamotto"]
add(re_ + "stone_any", re_ + "grade_genesis", "A Stone of Infinity", "Carry any Infinity Stone.", "nusmp:the_power_stone",
    have(*["nusmp:" + s for s in STONES], mode="any"), frame="goal", xp=200)
add(re_ + "gauntlet", re_ + "stone_any", "The Infinity Gauntlet", "Carry the Infinity Gauntlet.", "nusmp:the_infinity_gauntlet",
    have("nusmp:the_infinity_gauntlet"), frame="goal", xp=300)
add(re_ + "stones_all", re_ + "gauntlet", "All Six Stones", "Carry all six Infinity Stones and the Gauntlet at once.",
    "nusmp:the_infinity_gauntlet", have(*["nusmp:" + s for s in STONES], "nusmp:the_infinity_gauntlet", mode="all"),
    frame="challenge", xp=2000)

# ============================================================================================================== weapons
we = "weapons/"
add(we + "first", "root", "Arms of Many Worlds", "Pick up one of the mod's named weapons.", "nusmp:magic_tool_sword",
    have("nusmp:magic_tool_sword", "nusmp:magic_tool_spear", "nusmp:magic_tool_bow", "nusmp:demon_slayer_sword", "nusmp:demon_slasher_katana",
         "nusmp:elsdocia", "nusmp:rimeheart_runeblade", "nusmp:otherworld_trident", "nusmp:last_word", mode="any"), xp=15)
add(we + "mana_tools", we + "first", "Forged of Mana", "Carry the mana-forged sword, spear and bow.", "nusmp:magic_tool_bow",
    have("nusmp:magic_tool_sword", "nusmp:magic_tool_spear", "nusmp:magic_tool_bow", mode="all"), xp=20)
add(we + "demon_slayer", we + "first", "The Demon-Slayer", "Carry the Demon-Slayer Sword, the Genesis anti-magic blade.",
    "nusmp:demon_slayer_sword", have("nusmp:demon_slayer_sword"), frame="goal", xp=80)
add(we + "genesis", we + "demon_slayer", "Genesis Edge", "Strike with the Genesis Demon-Slayer and feel its edge.", "nusmp:demon_slayer_sword",
    ev("weapon.genesis"), frame="challenge", xp=300)
add(we + "anti_magic_trio", we + "demon_slayer", "Three Anti-Magic Blades", "Carry the Demon-Slayer, the Demon-Dweller and the Demon-Destroyer.",
    "nusmp:demon_dweller_sword", have("nusmp:demon_slayer_sword", "nusmp:demon_dweller_sword", "nusmp:demon_destroyer_sword", mode="all"),
    frame="goal", xp=120)
add(we + "licht", we + "anti_magic_trio", "Blades of Light", "Carry the Licht Dweller and the Licht Destroyer.", "nusmp:licht_dweller_sword",
    have("nusmp:licht_dweller_sword", "nusmp:licht_destroyer_sword", mode="all"), xp=60)
add(we + "katana", we + "first", "A Cut Through Magic", "Carry the Demon Slasher Katana.", "nusmp:demon_slasher_katana",
    have("nusmp:demon_slasher_katana"), xp=30)
add(we + "miasma", we + "katana", "Miasma Katana", "Carry the Miasma-Infused Katana.", "nusmp:miasma_infused_katana",
    have("nusmp:miasma_infused_katana"), xp=30)
add(we + "elsdocia", we + "first", "Elsdocia", "Carry Elsdocia, the blade of the wizard kings.", "nusmp:elsdocia", have("nusmp:elsdocia"),
    frame="goal", xp=100)
add(we + "runeblade", we + "first", "Rimeheart Runeblade", "Carry the Rimeheart Runeblade.", "nusmp:rimeheart_runeblade",
    have("nusmp:rimeheart_runeblade"), xp=40)
add(we + "trident", we + "first", "Otherworld Trident", "Carry the Otherworld Trident of Zagred.", "nusmp:otherworld_trident",
    have("nusmp:otherworld_trident"), xp=60)
add(we + "last_word", we + "trident", "The Last Word", "Carry the quill-blade Last Word.", "nusmp:last_word", have("nusmp:last_word"), xp=60)
add(we + "rapier", we + "first", "Spell-Forged Rapier", "Carry the Spell-Forged Rapier.", "nusmp:spell_forged_rapier",
    have("nusmp:spell_forged_rapier"), xp=30)
add(we + "greatsword", we + "first", "Sword Rain", "Carry the Severing Greatsword.", "nusmp:severing_greatsword",
    have("nusmp:severing_greatsword"), xp=30)
add(we + "null_blade", we + "first", "Null Blade", "Carry the Null Blade.", "nusmp:null_blade", have("nusmp:null_blade"), xp=30)
add(we + "armory", we + "elsdocia", "Ten Engravings", "Engrave ten weapons at the Relic Forge.",
    "minecraft:netherite_sword", ev("weapon.engraved", 10), frame="challenge", xp=250)

# ============================================================================================================== dungeon
du = "dungeon/"
# 1.58.0: the Living Dungeon, its Maws and its Descent Gates were removed (docs/labyrinth/mega_v2_prune.md): the advancements "Into the Living
# Dungeon", "The Maw", "Dungeon Maw" and "Descent Gate" went with them, and so did the Spire's "The Labyrinth" (its children hang off the
# Mega-Dungeon now). The criteria of "Dungeon Delver" below keep their old doors (names a0_..d3_) so saved progress still counts.
# the Mega-Dungeon alternatives are appended LAST so the criterion names of the older doors (a0_..d3_) never change for saved progress
DUNGEON_ENTER = anyof(ev("dungeon.entered"), ev("labyrinth.entered"), dimension("nusmp:living_dungeon"), dimension("nusmp:labyrinth"),
                      ev("mega.entered"), dimension("nusmp:mega_dungeon"))
add(du + "entered", "root", "Dungeon Delver", "Enter the dungeon of the Multiverse: step through the Gateway into the Mega-Dungeon.",
    "minecraft:lantern", DUNGEON_ENTER, xp=10)
add(du + "goliath", du + "entered", "Pit Guardian", "Defeat Goliath, the stone giant that guards a floor.", "minecraft:heavy_core",
    ev("dungeon.floor_boss", 1, "goliath"), frame="goal", xp=150)
add(du + "charybdis", du + "entered", "The Whirlpool", "Defeat the Spawn of Charybdis.", "minecraft:nautilus_shell",
    ev("dungeon.floor_boss", 1, "charybdis"), frame="goal", xp=150)
add(du + "both_floors", du + "goliath", "Both Floors Clear", "Defeat both floor bosses, Goliath and Charybdis.", "minecraft:beacon",
    AND(ev("dungeon.floor_boss", 1, "goliath"), ev("dungeon.floor_boss", 1, "charybdis")), frame="challenge", xp=300)
add(du + "meat", du + "entered", "Delicious in Dungeon", "Eat cooked monster meat. The dungeon feeds those who cook it.",
    "nusmp:purified_monster_meat", consume("nusmp:purified_monster_meat"), xp=20)
add(du + "basilisk", du + "meat", "Basilisk Fillet", "Eat a roast basilisk fillet.", "nusmp:roast_basilisk_fillet",
    consume("nusmp:roast_basilisk_fillet"), xp=20)
add(du + "direwolf", du + "meat", "Direwolf Haunch", "Eat a roast direwolf haunch.", "nusmp:roast_direwolf_haunch",
    consume("nusmp:roast_direwolf_haunch"), xp=20)
add(du + "labyrinth_event", du + "mega", "Gate of the Labyrinth", "Pass the Gate of the Labyrinth.", "nusmp:labyrinth_gate",
    ev("labyrinth.entered"), xp=10)
add(du + "archway", du + "mega", "Sealed Archway", "Carry a Sealed Archway.", "nusmp:sealed_archway", have("nusmp:sealed_archway"), xp=10)
add(du + "warden", du + "mega", "Warden Down", "Defeat the Warden of a floor.", "minecraft:iron_bars",
    ev("labyrinth.warden_defeated"), frame="goal", xp=100)
add(du + "shadow_key", du + "warden", "The Shadow Key", "Carry a Shadow Key.", "nusmp:shadow_key", have("nusmp:shadow_key"), xp=30)
add(du + "seal", du + "shadow_key", "A Seal Opens", "Open a seal of the Labyrinth with the key.", "minecraft:tripwire_hook",
    ev("labyrinth.seal_opened"), frame="goal", xp=80)
add(du + "peak_altar", du + "seal", "Peak Altar", "Carry a Peak Altar.", "nusmp:peak_altar", have("nusmp:peak_altar"), xp=20)
add(du + "vault", du + "seal", "The Vault", "Open the vault at the top of the Labyrinth.", "nusmp:relic_pedestal",
    ev("labyrinth.vault_opened"), frame="challenge", xp=300)
add(du + "traps", du + "mega", "Tabletop Traps", "Carry a pitfall tile, a snuffer glyph or a riddle stone.", "nusmp:pitfall_tile",
    have("nusmp:pitfall_tile", "nusmp:snuffer_glyph", "nusmp:riddle_stone", mode="any"), xp=15)

# The Mega-Dungeon (1.54.0): one dimension that grows a sector at a time. Its branch hangs off the root like the Labyrinth's, so the older
# dungeons keep their own branches.
add(du + "mega", "root", "The Mega-Dungeon", "Enter the Mega-Dungeon, one endless dungeon that grows every time a sector guardian falls.",
    "nusmp:labyrinth_gate", dimension("nusmp:mega_dungeon"), frame="goal", xp=60)
add(du + "mega_gateway", du + "mega", "Through the Gateway", "Step through a Gateway into the Mega-Dungeon.", "minecraft:lantern",
    ev("mega.entered"), xp=10)
add(du + "mega_rest", du + "mega", "A Safe Seat", "Rest at a seat, a hearth or a bed in a safe zone. Nothing else heals you down here.",
    "minecraft:campfire", ev("mega.rested"), xp=20)
add(du + "mega_wanderer", du + "mega", "Hide and Seek", "Get away from a wandering boss without dying.", "minecraft:sculk_shrieker",
    ev("mega.wanderer_survived"), frame="goal", xp=80)
add(du + "mega_sector", du + "mega", "Sector Cleared", "Defeat the Labyrinth Guardian of a sector.", "minecraft:heavy_core",
    ev("mega.sector_cleared"), frame="goal", xp=120)
add(du + "mega_sector_3", du + "mega_sector", "Three Sectors Deep", "Clear three sectors of the Mega-Dungeon.", "minecraft:beacon",
    ev("mega.sector_cleared", 3), frame="challenge", xp=300)
add(du + "mega_crucible", du + "mega_sector", "The Crucible of Choice", "Take a relic from a Crucible. Only one can be taken, and the dungeon answers.",
    "nusmp:relic_pedestal", ev("mega.crucible_choice"), frame="challenge", xp=300)
# 1.55.0: the bidirectional Mega-Dungeon (Floor 0, the two roads, the System, standard gear); appended after the older ones so nothing existing changes
add(du + "mega_hub", du + "mega", "Floor 0", "Arrive in the Sanctuary Hub: a safe place to build, and the start of both roads.",
    "minecraft:grass_block", ev("mega.hub"), xp=15)
add(du + "mega_holy", du + "mega_hub", "The Holy Road", "Clear the first floor of the Holy road. From now on the Abyssal road is closed to you.",
    "minecraft:bell", ev("mega.aligned", arg="holy"), xp=40)
add(du + "mega_abyssal", du + "mega_hub", "The Abyssal Road", "Clear the first floor of the Abyssal road. From now on the Holy road is closed to you.",
    "minecraft:echo_shard", ev("mega.aligned", arg="abyssal"), xp=40)
add(du + "mega_decennial", du + "mega_hub", "Ten Floors", "Reach floor +10 or floor -10 of the Mega-Dungeon.",
    "minecraft:nether_star", _crit("any", ev("mega.floor", 1, "+10", name="floor_up_10")["list"] + ev("mega.floor", 1, "-10", name="floor_down_10")["list"]), frame="goal", xp=100)
add(du + "mega_system", du + "mega_holy", "The System", "Finish a quest that the System gives you on a Holy floor.",
    "minecraft:written_book", ev("mega.system_quest"), xp=40)
add(du + "mega_shadow", du + "mega_system", "Arise", "Extract the shadow of a defeated Holy enemy with a Shadow Core.",
    "minecraft:wither_skeleton_skull", ev("mega.shadow"), frame="goal", xp=80)
add(du + "mega_gear", du + "mega", "Standard Issue", "Find a graded piece of standard gear in the Mega-Dungeon.",
    "minecraft:iron_sword", ev("mega.gear"), xp=20)
add(du + "mega_gear_rare", du + "mega_gear", "Rare Grade", "Find a Rare-grade piece of standard gear, made to turn magic aside.",
    "minecraft:diamond_sword", ev("mega.gear", arg="rare"), frame="goal", xp=60)

# ============================================================================================================== spirits
spi = "spirits/"
add(spi + "charm", "root", "Spirit Charm", "Carry a Spirit Charm, the key to the spirits.", "nusmp:spirit_charm", have("nusmp:spirit_charm"),
    xp=15)
add(spi + "lord_skill", spi + "charm", "Spirit Lord", "Carry the Spirit Lord skill.", "nusmp:spirit_lord_skill", have("nusmp:spirit_lord_skill"),
    frame="goal", xp=60)
add(spi + "contract", spi + "charm", "A Spirit Answers", "Sign a contract with a spirit.", "minecraft:soul_lantern",
    ev("spirit.contract", 1, "spirit"), frame="goal", xp=80)
add(spi + "meet_elemental", spi + "charm", "True Body of an Element", "Meet one of the four elemental Spirit Lords.",
    "nusmp:spirit_core_fire", talk("nusmp:elemental_spirit_lord"), xp=30)
add(spi + "cores", spi + "meet_elemental", "Four Spirit Cores", "Carry the fire, water, wind and earth spirit cores.",
    "nusmp:spirit_core_earth", have("nusmp:spirit_core_fire", "nusmp:spirit_core_water", "nusmp:spirit_core_wind", "nusmp:spirit_core_earth",
                                    mode="all"), frame="goal", xp=100)
add(spi + "devil_contract", "root", "A Devil's Contract", "Carry a devil contract. The price is always read last.", "nusmp:devil_contract",
    have("nusmp:devil_contract"), xp=30)
add(spi + "devil_union", spi + "devil_contract", "Devil Union", "Unite with a devil.", "nusmp:devil_horn", ev("devil.union"), frame="challenge",
    xp=300)
add(spi + "devil_core", spi + "devil_contract", "A Devil's Core", "Carry a devil core.", "nusmp:devils_core",
    have("nusmp:devil_core_zagred", "nusmp:devil_core_megicula", "nusmp:devil_core_lucifero", "nusmp:devil_core_astaroth",
         "nusmp:devil_core_liebe", "nusmp:devil_core_lilith", "nusmp:devil_core_nahamah", "nusmp:devil_core_walgner",
         "nusmp:devil_core_beelzebub", mode="any"), frame="goal", xp=100)
add(spi + "devil_cores_all", spi + "devil_core", "Nine Devil Cores", "Carry all nine devil cores.", "nusmp:devil_core_beelzebub",
    have("nusmp:devil_core_zagred", "nusmp:devil_core_megicula", "nusmp:devil_core_lucifero", "nusmp:devil_core_astaroth",
         "nusmp:devil_core_liebe", "nusmp:devil_core_lilith", "nusmp:devil_core_nahamah", "nusmp:devil_core_walgner",
         "nusmp:devil_core_beelzebub", mode="all"), frame="challenge", xp=1000)
add(spi + "soul_corruption", spi + "devil_union", "Soul Corruption", "Feel your soul corrode.", "minecraft:wither_skeleton_skull",
    effect("nusmp:soul_corruption"), xp=40)
add(spi + "paladin", spi + "devil_contract", "Soul Paladin", "Be marked a paladin.", "minecraft:totem_of_undying",
    effect("nusmp:paladin"), xp=40)

# ============================================================================================================== squads
sq = "squads/"
add(sq + "created", "root", "A Squad of Your Own", "Found a Magic Knight squad. Squads run on FTB Teams: the captain is the team owner.",
    "minecraft:white_banner", ev("squad.created"), frame="goal", xp=60)
add(sq + "joined", sq + "created", "Join the Ranks", "Join a Magic Knight squad.", "minecraft:red_banner", ev("squad.joined"), xp=30)
add(sq + "captain", sq + "created", "Captain", "Be named the captain of a squad.", "minecraft:black_banner", ev("squad.captain"), frame="goal",
    xp=60)
add(sq + "vice", sq + "captain", "Vice-Captain", "Name or become a vice-captain.", "minecraft:shield", ev("squad.vice"), xp=40)
add(sq + "charter", sq + "created", "A Written Canon", "Carry a canon grimoire for a boss.", "nusmp:charter_grimoire_goliath",
    have("nusmp:charter_grimoire_dante", "nusmp:charter_grimoire_vanica", "nusmp:charter_grimoire_zenon", "nusmp:charter_grimoire_lucius",
         "nusmp:charter_grimoire_devil_avatar", "nusmp:charter_grimoire_goliath", "nusmp:charter_grimoire_charybdis_spawn", mode="any"),
    xp=30)
add(sq + "charter_all", sq + "charter", "The Complete Canon", "Carry all seven boss canon grimoires.", "nusmp:charter_grimoire_lucius",
    have("nusmp:charter_grimoire_dante", "nusmp:charter_grimoire_vanica", "nusmp:charter_grimoire_zenon", "nusmp:charter_grimoire_lucius",
         "nusmp:charter_grimoire_devil_avatar", "nusmp:charter_grimoire_goliath", "nusmp:charter_grimoire_charybdis_spawn", mode="all"),
    frame="challenge", xp=300)

# =============================================================================================================== world
wo = "world/"
KINGS = [("clover", "Clover Kingdom", "minecraft:oak_sapling"), ("spade", "Spade Kingdom", "minecraft:black_banner"),
         ("heart", "Heart Kingdom", "minecraft:red_banner"), ("diamond", "Diamond Kingdom", "minecraft:diamond")]
add(wo + "kingdoms", "root", "Four Kingdoms", "Be accepted by one of the four kingdoms.", "minecraft:white_banner",
    ev("grimoire.received"), frame="goal", xp=20)
for k, ttl, icon in KINGS:
    add(wo + "kingdom_" + k, wo + "kingdoms", ttl, "Receive a grimoire from the %s." % ttl, icon, ev("grimoire.kingdom", 1, k), xp=15)
add(wo + "all_kingdoms", wo + "kingdoms", "Citizen of All Four", "Receive a grimoire from each of the four kingdoms.", "minecraft:beacon",
    ev_all(*[("grimoire.kingdom", k) for k, _, _ in KINGS]), frame="challenge", xp=300)
add(wo + "library", wo + "kingdoms", "Grimoire Library", "Enter the Grimoire Library dimension.", "minecraft:lectern",
    dimension("nusmp:grimoire_library"), xp=20)
add(wo + "dream", wo + "kingdoms", "The Dream World", "Enter the Dream dimension.", "minecraft:glow_ink_sac",
    dimension("nusmp:dream"), xp=20)
add(wo + "arena", "root", "The Final Hour", "Enter the arena of Lucius Zogratis.", "nusmp:zogratis_hourglass", ev("arena.entered"),
    frame="goal", xp=100)
add(wo + "arena_dim", wo + "arena", "The Arena Dimension", "Arrive in the Zogratis Arena dimension.", "nusmp:arena_return_sigil",
    dimension("nusmp:zogratis_arena"), xp=20)
add(wo + "hourglass_item", wo + "arena", "Hourglass of the Final Hour", "Carry the Hourglass of the Final Hour.", "nusmp:zogratis_hourglass",
    have("nusmp:zogratis_hourglass"), xp=30)
add(wo + "hourglass_used", wo + "hourglass_item", "Turn the Glass", "Use the Hourglass and let the final hour begin.",
    "nusmp:zogratis_hourglass", ev("arena.hourglass_used"), xp=40)
add(wo + "sigil", wo + "arena", "Return Sigil", "Carry an Arena Return Sigil.", "nusmp:arena_return_sigil", have("nusmp:arena_return_sigil"), xp=15)
add(wo + "arena_victory", wo + "hourglass_used", "Hour's End", "Win in the arena.", "minecraft:totem_of_undying", ev("arena.victory"),
    frame="challenge", xp=500)
add(wo + "codex", wo + "kingdoms", "Compat Codex", "Carry the Compat Codex, the book of cross-mod arts.", "nusmp:compat_codex",
    have("nusmp:compat_codex"), xp=15)

# ========================================================================================================= challenges
ch = "challenges/"
add(ch + "root", "root", "Trials of the Multiverse", "Harder deeds, for those who have done the rest.", "minecraft:netherite_ingot",
    ev("boss.defeated", 1), frame="goal", xp=20)
add(ch + "norelic", ch + "root", "With Bare Hands", "Win in the arena of Lucius carrying no relic at all.", "minecraft:iron_sword",
    ev("arena.victory", 1, "norelic"), frame="challenge", xp=1500)
add(ch + "time_decay", ch + "root", "Time Decay", "Feel the passing hour ravage you.", "minecraft:clock", effect("nusmp:time_decay"),
    frame="goal", xp=50)
add(ch + "severed", ch + "root", "Severed", "Be struck by a severing blow.", "minecraft:iron_sword", effect("nusmp:severed"), xp=40)
add(ch + "nullified", ch + "root", "Nullified", "Have your relics nullified.", "minecraft:barrier", effect("nusmp:relic_nullified"), xp=40)
add(ch + "paladins_5", ch + "root", "An Army of Souls", "Convert five souls into paladins.", "minecraft:totem_of_undying",
    ev("paladin.converted", 5), frame="goal", xp=100)
add(ch + "corpses_10", ch + "root", "Necromancer", "Raise ten soul corpses.", "minecraft:wither_skeleton_skull", ev("corpse.raised", 10),
    frame="goal", xp=100)
add(ch + "all_magic", ch + "root", "Master of Every Magic", "Cast all 105 magics.", "minecraft:nether_star", ev("spell.distinct", 105),
    frame="challenge", xp=1000)
add(ch + "genesis_relic", ch + "root", "Genesis in Hand", "Hold a relic of Genesis grade.", "minecraft:nether_star", ev("relic.grade", 1, "genesis"),
    frame="challenge", xp=500)
add(ch + "ten_pages_boss", ch + "root", "Armed and Ready", "Unlock ten pages and defeat three bosses.", "minecraft:enchanted_golden_apple",
    AND(ev("page.unlocked", 10), ev("boss.defeated", 3)), frame="goal", xp=150)
add(ch + "lucius_all_stages", ch + "root", "Three Stages, One Life", "Push Lucius through all three stages.", "nusmp:zogratis_hourglass",
    AND(ev("boss.phase", 1, "lucius:2"), ev("boss.phase", 1, "lucius:3")), frame="challenge", xp=400)

# ================================================================================================= arcs and future
ar = "arcs/"
# (key, title, milestone desc, icon, milestone crit)
ARCS = [
    ("hage_village", "Hage Village", "Be chosen by a grimoire, as Asta and Yuno were at the edge of the world.", "nusmp:grimoire",
     ev("ceremony.completed")),
    ("entrance_exam", "Magic Knights Entrance Exam", "Take the tower's exam and learn what magic is born in you.", "minecraft:enchanted_book",
     ev("wizard.questionnaire")),
    ("dungeon_arc", "The Dungeon Arc", "Enter a dungeon of the Multiverse.", "nusmp:labyrinth_gate", DUNGEON_ENTER),
    ("dark_triad", "Dark Triad: Royal Capital", "Call on a signature spell and stand with the Royal Capital.", "minecraft:nether_star",
     ev("spell.cast.signature")),
    ("eye_midnight", "Eye of the Midnight Sun", "Wear a relic of a lost age.", "nusmp:ring_of_power", ev("relic.equipped")),
    ("witches_forest", "The Witches' Forest", "Save Rouge, the witch's cat.", "minecraft:string", ev("rouge.saved")),
    ("underwater_temple", "The Underwater Temple", "Cast the magic of water.", "minecraft:water_bucket", ev("spell.cast", 1, "water")),
    ("royal_knights", "Royal Knights Selection Exam", "Pass the gate of the Labyrinth.", "nusmp:labyrinth_gate", ev("labyrinth.entered")),
    ("spade_raid", "Spade Kingdom Raid", "Defeat any boss of the Multiverse.", "minecraft:black_banner", ev("boss.defeated")),
    ("heart_elf", "Heart Kingdom: Elf Reincarnation", "Sign a contract with a spirit.", "minecraft:red_banner", ev("spirit.contract", 1, "spirit")),
    ("diamond_kingdom", "The Diamond Kingdom", "Merge two magics into one.", "minecraft:diamond", ev("magic.merged")),
    ("spade_final", "Spade Kingdom: the Final Arc", "Enter the arena of the Final Hour.", "nusmp:zogratis_hourglass", ev("arena.entered")),
    ("devil_arc", "The Devil Arc", "Unite with a devil.", "nusmp:devil_horn", ev("devil.union")),
]
prev = "root"
for key, ttl, desc, icon, crit in ARCS:
    add(ar + key, "root" if key == "hage_village" else ar + prev, ttl, desc, icon, crit, frame="goal", xp=75)
    prev = key

FUTURE = [
    # arc, boss slot, title, what is missing
    ("dungeon_arc", "vetto", "Vetto", "the Dungeon Arc's boss"),
    ("dark_triad", "boss", "The Capital Raid's Champion", "the Royal Capital arc's boss"),
    ("eye_midnight", "boss", "The Eye of the Midnight Sun", "the Eye of the Midnight Sun arc's boss"),
    ("witches_forest", "boss", "The Witch Queen", "the Witches' Forest boss"),
    ("underwater_temple", "boss", "Guardian of the Temple", "the Underwater Temple boss"),
    ("royal_knights", "boss", "The Examiner", "the Royal Knights Selection Exam boss"),
    ("heart_elf", "boss", "The Reincarnated Elf", "the Elf Reincarnation boss"),
    ("diamond_kingdom", "boss", "The Diamond Kingdom's Champion", "the Diamond Kingdom boss"),
    ("devil_arc", "megicula", "Megicula", "the devil Megicula"),
    ("devil_arc", "lucifero", "Lucifero", "the devil Lucifero"),
    ("devil_arc", "astaroth", "Astaroth", "the devil Astaroth"),
]
for arc, slot, ttl, what in FUTURE:
    a = add("future/%s_%s" % (arc, slot), ar + arc, ttl,
            "Coming soon: %s is not in the Multiverse yet. This advancement is granted by its death once it is made." % what,
            "minecraft:barrier", impossible(), frame="goal", xp=150, hidden=True, chat=True)
    a.future = True

import spec_arc  # noqa: E402,F401  1.61.0: the characters of the arcs (tools/quests/arc_roster.txt)
spec_arc.build(S)
