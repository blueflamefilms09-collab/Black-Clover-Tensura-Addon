#!/usr/bin/env python3
"""The own voice lines of the Goku and Sinbad bosses (docs/boss_redesign_goku_sinbad.md). ONE source of truth: the tables below.

    python3 tools/audio/boss_lines_gen.py            writes tools/audio/goku_lines.json, tools/audio/sinbad_lines.json,
                                                     the generated Java table src/main/java/com/newuniverse/nusmp/arc/lines/BossLinesData.java
                                                     and the subtitle fragment tools/lang/boss_lines_gs.json, and appends the subtitle keys to
                                                     assets/anime_multiverse/lang/en_us.json (append only; a key that exists is kept)
    python3 tools/audio/boss_lines_gen.py --register  after the audio exists (sounds/voice/goku/<id>.ogg, sounds/voice/sinbad/<id>.ogg, made by
                                                     tools/audio/offline_voice.py): rewrites the REGISTERED list of BossLines.java and appends the
                                                     sounds.json events of the files that exist. A line without a file stays text only.

A line is {id, group, text, delivery, source}. `delivery` is a note for the voice tool; `source` says where the line comes from: "catchphrase" (a technique
name or phrase the character is known for), "in the character's manner" (written for the fight in his way of speaking), or "kept from the old boss" (the text the
boss said before this release). The wiki could not be fetched while writing (the session's network refused the fandom hosts), so no line is claimed as verbatim.
"""
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
J = os.path.join(ROOT, "tools", "audio")
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "arc", "lines")
LANG_FRAG = os.path.join(ROOT, "tools", "lang", "boss_lines_gs.json")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")

CM = "in the character's manner"
CP = "catchphrase"
KEEP = "kept from the old boss"

# (group, delivery, source, [texts])
GOKU = [
    ("intro", "[cheerful, excited, eager for a fight]", CM, ["Hey! I'm Son Goku. Let's have a great fight!", "Wow, you look strong! This is going to be fun!", "I've been waiting for someone like you!"]),
    ("form_ssj", "[a roaring battle cry, then a grin]", KEEP, ["HAAAAAA! Super Saiyan!", "Heh. Okay, now I'm serious."]),
    ("form_ssj2", "[a sharper shout, confident]", KEEP, ["Super Saiyan 2! Feel this!", "Not bad! Now it gets real!"]),
    ("form_ssj3", "[a long scream of effort, then low and steady]", KEEP, ["AAAAAAARGH! Super Saiyan 3!", "Hah... this takes a lot out of me. Better end it fast!"]),
    ("form_god", "[calm, quiet, almost gentle]", KEEP, ["A calm, divine power... Super Saiyan God."]),
    ("form_blue", "[cool, focused, a small smile]", KEEP, ["Super Saiyan Blue! Don't blink."]),
    ("form_kaioken", "[a hard shout, strained]", KEEP, ["Kaio-ken times ten, on top of Blue!", "Kaio-ken! My body can take it!"]),
    ("form_ui_sign", "[surprised, then amazed]", KEEP, ["My body... is moving by itself.", "Whoa, what is this feeling? It's amazing!"]),
    ("form_mui", "[serene, quiet, then a happy laugh]", KEEP, ["This is it. Perfected Ultra Instinct.", "No thinking. Just move."]),
    ("form_tui", "[wide open, joyful, calling out]", KEEP, ["True Ultra Instinct. Come on, give me everything!"]),
    ("combo", "[quick grunts of effort]", CM, ["Hah! Hya! Take this!", "Here I come!"]),
    ("kamehameha", "[drawn out, building, then a huge release]", CP, ["Ka... me... ha... me... HAAAAA!", "Kamehameha!"]),
    ("spirit_bomb", "[reaching up, pleading, then determined]", CP, ["Everyone... lend me your energy!", "Just a little more... Spirit Bomb!"]),
    ("spirit_bomb_broken", "[a pained grunt, surprised]", CM, ["Nngh... the Spirit Bomb!", "Ugh! It's coming apart!"]),
    ("transmit", "[a quick playful call]", CP, ["Instant Transmission!", "Too slow! Behind you!"]),
    ("kaioken_burst", "[a short hard shout]", CP, ["Kaio-ken!", "Kaio-ken times three!"]),
    ("cage", "[confident, a challenge]", CM, ["Zanzoken! Can you keep up with me?", "Try to follow this!"]),
    ("flare", "[a sharp shout, hands to the forehead]", CP, ["Solar Flare!", "Taiyoken!"]),
    ("disc", "[focused, a little proud]", CM, ["Here, catch! It comes right back!", "Watch it spin!"]),
    ("hurt_big", "[a wince, then a laugh]", CM, ["Ow! That one hurt!", "Heh, you got me good there!"]),
    ("dodge", "[surprised, light]", CM, ["Whoa, my body moved on its own!", "Too slow!"]),
    ("low_hp", "[breathing hard, still smiling]", CM, ["Phew... I'm running low. But I'm not done yet!", "Haa... haa... this is the best part!"]),
    ("taunt", "[friendly, teasing]", CM, ["Come on, show me more!", "You're getting stronger! Great!", "Don't hold back on me!"]),
    ("player_death", "[worried, apologetic]", CM, ["Oh no, did I go too far? Sorry!", "Hey! You okay? Man, I overdid it."]),
    ("victory", "[kind, a little sheepish]", CM, ["That was a good fight! Come back when you're stronger!", "Huh? Come on, get up! Let's go again later."]),
    ("defeat", "[warm, out of breath, grateful]", KEEP, ["Heh... that was a great fight. Thanks.", "I haven't felt this alive in years!", "You win. Let's do this again sometime!"]),
]

SINBAD = [
    ("intro", "[smooth, charming, a king at ease]", CM, ["So, you came all this way to meet me. How flattering.", "Let me show you what the King of the Seven Seas can do.", "An adventure at last. Shall we begin?"]),
    ("equip_baal", "[commanding, resonant]", CM, ["Spirit of Wrath and Heroes, Baal! Dwell in my body: Djinn Equip!", "Come forth, Baal!"]),
    ("equip_valefor", "[sly, light, playful]", CM, ["Spirit of Falsehood and Confidence, Valefor! Dwell in my body!", "Valefor, lend me your cunning!"]),
    ("equip_zepar", "[low, controlled, a little dangerous]", CM, ["Spirit of Spirit and Puppetry, Zepar! Dwell in my body!", "Zepar, make them listen."]),
    ("equip_focalor", "[crisp, bright]", CM, ["Spirit of Control and Obedience, Focalor! Dwell in my body!", "Focalor, carry me on the wind!"]),
    ("equip_vepar", "[steady, cold, precise]", CM, ["Vepar! Dwell in my body, and draw every blade to me!", "Vepar, rain steel on my enemies."]),
    ("equip_crocell", "[bright, lifting, sure]", CM, ["Crocell! Dwell in my body, and let your light cut through!", "Crocell, shine for me!"]),
    ("equip_furfur", "[low, intense, almost a whisper]", CM, ["Furfur! Dwell in my body, and tear the darkness open!", "Furfur... come."]),
    ("unequip", "[easy, breath out]", CM, ["That is enough of that, for now.", "Rest, my djinn."]),
    ("sword", "[calm, a flourish]", CM, ["Allow me.", "A simple blade is enough for you."]),
    ("magoi", "[quiet, certain]", CM, ["Your magic means nothing to me. Magoi Manipulation.", "I will take that power from you."]),
    ("bararaq", "[a sharp call, lightning-bright]", CP, ["Bararaq Saiqa!", "Lightning, answer me! Bararaq!"]),
    ("inqerad", "[grand, rising, a king speaking to the sky]", CP, ["Bararaq... Inqerad-Saiqa!", "Stand in the light, or be struck down!"]),
    ("garufor", "[cold, controlled]", CP, ["Garufor Zairu!", "Garufor Kirestal! Freeze!"]),
    ("stagnation", "[hushed, stretching the words]", CM, ["Everything slows. Everything stops.", "Be still."]),
    ("scream", "[a deep breath, then a sharp command]", CM, ["Look away, if you value your mind!", "Sleep!"]),
    ("foraz", "[exultant, a sweeping gesture]", CP, ["Foraz Zora!", "Two storms, become one!"]),
    ("vepar_istella", "[sharp, a king's command]", CP, ["Vepar Istella! A thousand swords!", "Fall, blades of steel!"]),
    ("crocell_saber", "[quick, light, a smile in the voice]", CM, ["Three cuts, and not one you will see.", "Crocell's saber, hah!"]),
    ("furfur_rend", "[low, harsh, tearing]", CM, ["Rend the dark!", "Darkness, open wide."]),
    ("orrery", "[playful, then ceremonial]", CM, ["Seven djinn, seven lights. Fly, my Rukh!", "The Rukh love me. Let them show you."]),
    ("sigil", "[solemn, drawing a circle]", CM, ["This circle is my kingdom. Choose where you stand.", "Only the dark is safe here."]),
    ("seven_seas", "[soaring, full of confidence]", CM, ["Seven Seas, answer your king!", "All seven, together!"]),
    ("foresight", "[amused, a knowing smile]", KEEP, ["I already saw that coming.", "The flow of destiny never lies."]),
    ("gates", "[serious for once, deeper]", CM, ["Enough games. I will use every djinn I have.", "Do not make me regret taking you seriously."]),
    ("hurt_big", "[a short grunt, then a smile]", CM, ["Ha! Now that stung.", "Careful. A king bleeds too."]),
    ("low_hp", "[strained, but proud]", CM, ["Not yet. A king never kneels first.", "My Household would never forgive me for losing here."]),
    ("taunt", "[teasing, flirtatious]", CM, ["Is that all? Come, impress me.", "You fight well. Almost as well as I sail.", "Do not stop now. I am just warming up."]),
    ("player_death", "[calm, almost regretful]", CM, ["A pity. You had courage.", "Rest well. The sea remembers brave ones."]),
    ("victory", "[easy, gracious]", CM, ["The Seven Seas are mine. Try again someday.", "A fine fight. Go home and train."]),
    ("defeat", "[warm, a little breathless, accepting]", CM, ["Well fought. The sea has taken me, this time.", "You are stronger than I judged. I will not forget it.", "So this is how a king falls. Worth it."]),
]

# move id -> group said when the boss starts to cast it (a move that is not listed is silent)
CALLOUTS = {
    "goku": {"meteor_combo": "combo", "kamehameha": "kamehameha", "instant_transmission": "transmit", "kaioken_burst": "kaioken_burst", "spirit_bomb": "spirit_bomb",
             "zanzoken_cage": "cage", "solar_flare": "flare", "ki_disc": "disc"},
    "sinbad": {"sword_slash": "sword", "magoi_thrust": "magoi", "bararaq": "bararaq", "bararaq_saiqa": "bararaq", "bararaq_inqerad_saiqa": "inqerad", "garufor_zairu": "garufor",
               "garufor_kirestal": "garufor", "stagnation": "stagnation", "command_scream": "scream", "foraz_zora": "foraz", "vepar_istella": "vepar_istella",
               "saber_flurry": "crocell_saber", "rending_darkness": "furfur_rend", "seven_seas_sovereign": "seven_seas", "rukh_orrery": "orrery", "kingdom_sigil": "sigil"},
}

COOLDOWN = {"intro": 0, "defeat": 0, "victory": 600, "player_death": 100, "taunt": 1200, "hurt_big": 400, "low_hp": 1200, "dodge": 300, "foresight": 400, "unequip": 400}
DEFAULT_COOLDOWN = 100


def lines(boss, table):
    out = []
    for group, delivery, source, texts in table:
        for i, t in enumerate(texts, 1):
            out.append(dict(id="%s_%s_%d" % (boss, group, i), group=group, text=t, delivery=delivery, source=source))
    return out


def jstr(s):
    return json.dumps(s, ensure_ascii=True)


def write_json(boss, table, note):
    path = os.path.join(J, boss + "_lines.json")
    data = {"boss": boss, "voice_profile": boss, "note": note, "lines": lines(boss, table)}
    with open(path, "w") as f:
        json.dump(data, f, indent=1)
        f.write("\n")
    return path


def write_java():
    os.makedirs(JAVA, exist_ok=True)
    out = ["package com.newuniverse.nusmp.arc.lines;", "",
           "/** GENERATED by tools/audio/boss_lines_gen.py from tools/audio/goku_lines.json and sinbad_lines.json: do not edit by hand. Ids, groups and texts of the lines of the Goku and Sinbad bosses. */",
           "final class BossLinesData {", "    private BossLinesData() {}", ""]
    for boss, table in (("GOKU", GOKU), ("SINBAD", SINBAD)):
        ls = lines(boss.lower(), table)
        for name, key in (("IDS", "id"), ("GROUPS", "group"), ("TEXTS", "text")):
            out.append("    static final String[] %s_%s = {" % (boss, name))
            for ln in ls:
                out.append("        %s," % jstr(ln[key]))
            out.append("    };")
            out.append("")
    for boss in ("goku", "sinbad"):
        keys = sorted(CALLOUTS[boss])
        out.append("    static final String[] %s_MOVES = {%s};" % (boss.upper(), ", ".join(jstr(k) for k in keys)))
        out.append("    static final String[] %s_MOVE_GROUPS = {%s};" % (boss.upper(), ", ".join(jstr(CALLOUTS[boss][k]) for k in keys)))
        out.append("")
    out.append("    /** Cooldown in ticks of a group (per boss), 0 = none. */")
    out.append("    static final String[] COOLDOWN_GROUPS = {%s};" % ", ".join(jstr(k) for k in sorted(COOLDOWN)))
    out.append("    static final long[] COOLDOWN_TICKS = {%s};" % ", ".join(str(COOLDOWN[k]) for k in sorted(COOLDOWN)))
    out.append("    static final long DEFAULT_COOLDOWN = %d;" % DEFAULT_COOLDOWN)
    out.append("}")
    path = os.path.join(JAVA, "BossLinesData.java")
    with open(path, "w") as f:
        f.write("\n".join(out) + "\n")
    return path


def subtitle_fragment():
    frag = {}
    for boss, label, table in (("goku", "Goku", GOKU), ("sinbad", "Sinbad", SINBAD)):
        for ln in lines(boss, table):
            frag["subtitles.anime_multiverse.voice." + ln["id"]] = "%s: \"%s\"" % (label, ln["text"])
    return frag


def write_lang():
    frag = subtitle_fragment()
    with open(LANG_FRAG, "w") as f:
        json.dump(frag, f, indent=2, ensure_ascii=False)
        f.write("\n")
    en = os.path.join(ASSETS, "lang", "en_us.json")
    src = open(en, encoding="utf-8").read()
    add = [(k, v) for k, v in frag.items() if ('"%s"' % k) not in src]
    if add:
        body = src.rstrip()
        assert body.endswith("}")
        tail = "".join(',\n  %s: %s' % (json.dumps(k), json.dumps(v, ensure_ascii=False)) for k, v in add)
        open(en, "w", encoding="utf-8").write(body[:-1].rstrip() + tail + "\n}\n")
    return len(add)


def register():
    base = os.path.join(ASSETS, "sounds", "voice")
    have = []
    for boss, table in (("goku", GOKU), ("sinbad", SINBAD)):
        for ln in lines(boss, table):
            if os.path.exists(os.path.join(base, boss, ln["id"] + ".ogg")):
                have.append(ln["id"])
    jp = os.path.join(JAVA, "BossLines.java")
    s = open(jp).read()
    new = "    // BEGIN REGISTERED (rewritten by tools/audio/boss_lines_gen.py --register)\n    private static final String[] REGISTERED = {%s};\n    // END REGISTERED" % ", ".join(jstr(i) for i in have)
    s2 = re.sub(r"    // BEGIN REGISTERED.*?// END REGISTERED", lambda m: new, s, flags=re.S)
    open(jp, "w").write(s2)
    sj = os.path.join(ASSETS, "sounds.json")
    sounds = open(sj).read()
    add = ""
    for i in have:
        key = "voice." + i
        if ('"%s"' % key) in sounds:
            continue
        role = i.split("_")[0]
        add += ',\n  "%s": {\n    "sounds": [{ "name": "anime_multiverse:voice/%s/%s", "stream": false }],\n    "subtitle": "subtitles.anime_multiverse.voice.%s"\n  }' % (key, role, i, i)
    if add:
        body = sounds.rstrip()
        assert body.endswith("}")
        open(sj, "w").write(body[:-1].rstrip() + add + "\n}\n")
    print("registered %d lines with a file" % len(have))


def main(argv):
    if "--register" in argv:
        register()
        return
    n1 = write_json("goku", GOKU, "Own line set of the Goku boss, in his Dragon Ball manner. Audio: tools/audio/offline_voice.py profile 'goku'. Text only until the files exist.")
    n2 = write_json("sinbad", SINBAD, "Own line set of the Sinbad boss, in his Magi manner (spell names as docs/sinbad_boss_lore.md writes them). Audio: tools/audio/offline_voice.py profile 'sinbad'. Text only until the files exist.")
    j = write_java()
    added = write_lang()
    print("wrote", n1, n2, j, "lang keys appended:", added, "goku lines", len(lines("goku", GOKU)), "sinbad lines", len(lines("sinbad", SINBAD)))


if __name__ == "__main__":
    main(sys.argv[1:])
