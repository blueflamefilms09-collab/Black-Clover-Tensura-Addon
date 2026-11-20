#!/usr/bin/env python3
"""The own voice-line set of the Kakashi Hatake boss (docs/bosses/kakashi.md, section "Voice").

OWN lines only: nothing here is shared with any other boss. No ElevenLabs is used or planned for this set: another worker records the audio offline and drops the files in later.
A line with no recording shows its subtitle only (chat line of the boss plus, once a file exists, the sound's own closed caption), so the boss is complete without a single audio file.

Writes:
  tools/audio/kakashi_lines.json                                         the manifest for the audio worker: id, group, text (the subtitle), delivery (acting note), file (where the .ogg goes), trigger (when it is said)
  src/main/resources/data/anime_multiverse/kakashi/lines.json            what the game reads: groups of {id, text} (pure data, read by arc/kakashi/KakashiLines)
  tools/lang/kakashi_voice.json                                          closed-caption text of every line (subtitles.anime_multiverse.kakashi.<id>), merged into en_us.json by the release step

Audio drop-in (no code change needed):
  1. record each line to  src/main/resources/assets/anime_multiverse/sounds/kakashi/<id>.ogg   (mono, 44.1 kHz, Ogg Vorbis)
  2. python3 -B tools/audio/kakashi_voice.py        registers every id whose file exists (data/anime_multiverse/kakashi/recorded.json + the sounds.json entries)

    python3 -B tools/audio/kakashi_lines.py
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT_MANIFEST = os.path.join(HERE, "kakashi_lines.json")
OUT_DATA = os.path.join(ROOT, "src", "main", "resources", "data", "anime_multiverse", "kakashi", "lines.json")
OUT_LANG = os.path.join(ROOT, "tools", "lang", "kakashi_voice.json")

CALM = "calm, lazy, a little amused, relaxed breathing"
FOCUS = "low, focused, quiet intensity"
SHOUT = "a sharp shout, clipped, held for a beat"

# group: (trigger, [(delivery, text)])
GROUPS = {
    "intro": ("the fight starts", [
        (CALM, "Maa, maa. I came to read in peace, but it seems you are not going to allow that."),
        (CALM, "Sorry I am late. I got lost on the road of life."),
        (CALM, "You walk in like you own the road. Fine. Let me see what you carry."),
        (FOCUS, "My eye is open. Do not make me use all of it."),
    ]),
    "phase_pack": ("phase 2 begins (75 percent): the pack", [
        (CALM, "Good. That was a real strike. Now let us make this a crowd."),
        (FOCUS, "Pakkun, everyone. Get out here, we have company."),
    ]),
    "phase_mangekyo": ("phase 3 begins (50 percent): the Mangekyo", [
        (FOCUS, "I borrowed this eye from a friend. He would want me to use it well."),
        (FOCUS, "Space bends where I look. Try to stand on the right side of it."),
    ]),
    "phase_susanoo": ("phase 4 begins (25 percent): the Perfect Susanoo", [
        (FOCUS, "So this is what the last of it feels like. Then I will not hold back."),
        (SHOUT, "Come out. Show them what a promise looks like when it has a sword."),
    ]),
    "raikiri": ("Raikiri wind-up", [
        (FOCUS, "Raikiri."),
        (FOCUS, "Hold still. This will only take one breath."),
    ]),
    "raikiri_miss": ("Raikiri dodged, he overshoots", [
        (CALM, "Oh. You moved. That is annoying."),
        (CALM, "Not bad. Do it again and I will be impressed."),
    ]),
    "kunai": ("Kunai Barrage", [
        (CALM, "Little things first. They add up."),
        (CALM, "Watch the tags, not the knives."),
    ]),
    "copy": ("Sharingan Copy", [
        (FOCUS, "I saw that. Thank you, I will keep it."),
        (CALM, "Mine now. Here, have it back."),
    ]),
    "beast_fang": ("Lightning Beast Tracking Fang", [
        (SHOUT, "Lightning Beast. Run it down."),
        (FOCUS, "It follows. Find a wall."),
    ]),
    "water_dragon": ("Water Dragon Bullet", [
        (SHOUT, "Water Dragon."),
        (CALM, "You are wet now. That matters, you will see why."),
    ]),
    "ninken": ("Ninken Pursuit", [
        (SHOUT, "Hunting Fang. Bring them to the ground."),
        (CALM, "The pack never loses a scent."),
    ]),
    "clones": ("Shadow Clone Ambush", [
        (CALM, "One of me is bad enough. Three is rude, I know."),
        (SHOUT, "Surround them."),
    ]),
    "mist": ("Hidden Mist", [
        (CALM, "Fog. A very old trick, and still a good one."),
        (FOCUS, "Breathe slowly. You cannot see me, but I can see you."),
    ]),
    "wall": ("Earth Wall", [
        (CALM, "Earth Style. Nothing flies through this."),
    ]),
    "kamui_step": ("Kamui Step", [
        (FOCUS, "Behind you."),
        (CALM, "The door is on the other side. Mind the draft."),
    ]),
    "kamui_raikiri": ("Kamui Raikiri", [
        (FOCUS, "Kamui. Raikiri."),
        (FOCUS, "You will not feel where it comes from."),
    ]),
    "kamui_shuriken": ("Kamui Shuriken", [
        (SHOUT, "Kamui Shuriken."),
        (FOCUS, "Four doors. Stand in none of them."),
    ]),
    "kamui_vortex": ("Kamui Vortex", [
        (FOCUS, "Everything goes in. Hold on to something."),
        (FOCUS, "Space turns. Let it take you, or fight it."),
    ]),
    "kamui_phase": ("he phases through a blow", [
        (CALM, "Too slow."),
        (CALM, "Nothing there. You hit the air between worlds."),
    ]),
    "kawarimi": ("he swaps for a log", [
        (CALM, "A log. Sorry, it was the closest thing to me."),
        (CALM, "Ha. You hit the firewood."),
    ]),
    "susanoo": ("Perfect Susanoo rises", [
        (SHOUT, "Susanoo."),
        (FOCUS, "Stand behind me. All of you who have not run yet."),
    ]),
    "susanoo_break": ("the Susanoo shield breaks", [
        (FOCUS, "It cracked. Good. I was getting tired of hiding behind it."),
        (CALM, "Fair enough. That was a strong hit."),
    ]),
    "purple": ("Purple Lightning wind-up", [
        (FOCUS, "Purple Lightning. This is the last of what I have."),
        (SHOUT, "Do not let it find your feet."),
    ]),
    "hurt": ("he takes a heavy hit", [
        (CALM, "Heh. That one stung."),
        (CALM, "Good. Better than I expected."),
        (FOCUS, "Careful. I am still standing."),
        (FOCUS, "Those who abandon their comrades are worse than scum. Remember that."),
    ]),
    "low": ("he is under 15 percent", [
        (FOCUS, "My chakra is nearly gone. So is the time you have to finish this."),
        (CALM, "One more exchange. Make it count."),
    ]),
    "kill": ("a fighter falls", [
        (FOCUS, "That is how it ends when you are careless. I am sorry."),
        (CALM, "Do not follow them. Back up, breathe, begin again."),
    ]),
    "victory": ("every fighter is down, he wins", [
        (CALM, "It is over. Next time, bring more than strength."),
        (FOCUS, "Remember this. Strength alone never protected anyone."),
    ]),
    "idle": ("idle taunt while the fight pauses", [
        (CALM, "Take your time. I was late once, I know the feeling."),
        (CALM, "Maa, are you done catching your breath?"),
        (FOCUS, "Your friends are watching your back. Do not waste that."),
    ]),
    "defeat": ("he is beaten", [
        (CALM, "Well. I read the whole book and you wrote the last page. Good ending."),
        (CALM, "You are strong. Look after each other, that is where it comes from."),
    ]),
    # ---- the canon kit pass (appended): one group per new move, plus the strain, counter and Six Paths calls
    "lightning_cutter": ("Lightning Cutter", [
        (SHOUT, "Lightning Cutter."),
        (FOCUS, "Throw lightning at me and I will cut it in two."),
    ]),
    "cutter_cut": ("his Lightning Cutter cuts a bolt that reached him", [
        (CALM, "Lightning cut in two. I did say so."),
        (FOCUS, "A bolt is only a very fast line. I cut lines."),
    ]),
    "lightning_clone": ("Lightning Release: Shadow Clone", [
        (FOCUS, "Lightning Release, Shadow Clone. Careful, it bites."),
        (CALM, "Do not touch that one."),
    ]),
    "lclone_break": ("a foe breaks a clone of lightning and is paralysed", [
        (CALM, "That is the lightning. It stops the legs, not the mind."),
        (CALM, "I did warn you about the clone."),
    ]),
    "purple_strike": ("Purple Electricity: the empowered strike", [
        (FOCUS, "Purple Electricity."),
        (CALM, "Close range is where it hits hardest."),
    ]),
    "purple_burst": ("Purple Electricity: the burst all round him", [
        (SHOUT, "Purple Electricity, all around me."),
        (FOCUS, "Stay back, or stay down."),
    ]),
    "purple_chain": ("Purple Electricity: through the water", [
        (FOCUS, "Wet ground carries it. Everyone connected, everyone shocked."),
        (CALM, "Purple Electricity, through the water."),
    ]),
    "kamui_warp": ("Kamui, long range: the Distant Warp", [
        (FOCUS, "Kamui. I only need to look at you."),
        (FOCUS, "Into the other space. You will come out somewhere else."),
    ]),
    "earth_fang": ("Summoning: Earth Release: Tracking Fang", [
        (SHOUT, "Earth Release, Tracking Fang."),
        (CALM, "My dogs know your scent. Hold him."),
    ]),
    "eight_gates": ("the Eight Gates", [
        (FOCUS, "Gate of Opening, Healing, Life. That is as far as I dare."),
        (CALM, "Eight Gates. A little of it, only a little."),
    ]),
    "kubikiri": ("Kubikiribocho", [
        (CALM, "Kubikiribocho. A heavy one, so stand clear."),
        (CALM, "Not my sword, but it works for me."),
    ]),
    "chakra_sabre": ("White Light Chakra Sabre", [
        (FOCUS, "White Light Chakra Sabre."),
        (CALM, "My father's way of fighting. Quick and quiet."),
    ]),
    "kunai_wire": ("Kunai Wire", [
        (CALM, "A wire. Simple, and you walked right into it."),
        (FOCUS, "Come here."),
    ]),
    "thunder_god": ("Flying Thunder God Kunai", [
        (FOCUS, "A kunai. Mark it and be gone."),
        (CALM, "A marker, a flash, and I am wherever I left one."),
    ]),
    "great_fireball": ("Fire Release: Great Fireball", [
        (SHOUT, "Fire Release, Great Fireball."),
        (CALM, "I have seen this done. It is easier with an eye that copies."),
    ]),
    "six_paths": ("the Six Paths awakening", [
        (FOCUS, "Obito. Lend me the rest. Both eyes, and a little more."),
        (SHOUT, "This is not mine to keep. I will use it once."),
    ]),
    "sixpaths_end": ("the Six Paths chakra leaves him", [
        (CALM, "That is all of it. It leaves as quickly as it came."),
        (FOCUS, "Out of borrowed chakra. Hit me while you can."),
    ]),
    "strain": ("the Sharingan strain fills and his eye bleeds", [
        (FOCUS, "My eye. It is bleeding. Give me a moment."),
        (CALM, "The Sharingan never rests, and I never rest with it."),
    ]),
    "counter": ("he counters the kind of attack the players use most", [
        (CALM, "You lean on that one move. So I prepared for it."),
        (FOCUS, "I watched you. That no longer works on me."),
        (CALM, "Observe first, attack second. An old habit."),
    ]),
}

# lines that name a technique: the name is from the English Naruto wiki rundown the owner supplied (naruto.fandom.com), the wording is his manner
WIKI = "technique name from the English Naruto wiki rundown supplied by the owner (naruto.fandom.com), wording in his manner"
WIKI_LINES = {
    "Lightning Cutter.", "Lightning Release, Shadow Clone. Careful, it bites.", "Purple Electricity.", "Purple Electricity, all around me.", "Purple Electricity, through the water.",
    "Kamui. I only need to look at you.", "Earth Release, Tracking Fang.", "Gate of Opening, Healing, Life. That is as far as I dare.", "Eight Gates. A little of it, only a little.",
    "Kubikiribocho. A heavy one, so stand clear.", "White Light Chakra Sabre.", "A kunai. Mark it and be gone.", "Fire Release, Great Fireball.",
    "Obito. Lend me the rest. Both eyes, and a little more.", "My eye. It is bleeding. Give me a moment.",
}


# canon-attested phrases, from memory (the wikis were unreachable): id text -> source. Everything else is written "in his manner".
CANON = {
    "Sorry I am late. I got lost on the road of life.": "manga ch. 2 / anime ep. 1 (his first-meeting excuse), paraphrased; from memory, wiki not reachable",
    "Those who abandon their comrades are worse than scum. Remember that.": "manga ch. 15 / anime ep. 24 (the 'scum' speech), shortened; from memory, wiki not reachable",
    "Raikiri.": "jutsu call-out, name from the series (Raikiri, 'Lightning Cutter'); wiki not reachable",
    "Kamui. Raikiri.": "jutsu call-out, names from the series (Kamui Raikiri); wiki not reachable",
    "Kamui Shuriken.": "jutsu call-out, name from the series; wiki not reachable",
    "Susanoo.": "jutsu call-out, name from the series (Susanoo); wiki not reachable",
    "Water Dragon.": "jutsu call-out, Water Style: Water Dragon Bullet; wiki not reachable",
    "Maa, are you done catching your breath?": "his 'maa, maa' verbal habit; line in his manner",
}


def lines():
    out = []
    for g, (trigger, items) in GROUPS.items():
        for n, (delivery, text) in enumerate(items, 1):
            lid = "kakashi_%s_%d" % (g, n)
            out.append({"id": lid, "group": g, "trigger": trigger, "text": text, "delivery": delivery,
                        "source": CANON.get(text, WIKI if text in WIKI_LINES else "in his manner (original wording, no wiki source)"),
                        "file": "assets/anime_multiverse/sounds/kakashi/%s.ogg" % lid})
    return out


def main():
    ls = lines()
    ids = [l["id"] for l in ls]
    assert len(ids) == len(set(ids))
    manifest = {
        "voice": None,
        "note": "Own set for the Kakashi Hatake boss. Audio: generated offline by the coordinator with the Kokoro tool (profile kakashi in tools/audio/offline_voice_profiles.json, main repo). Record offline, one file per id at the path in 'file' (mono, 44.1 kHz, Ogg Vorbis), then run tools/audio/kakashi_voice.py. A line without a file shows its subtitle only.",
        "groups": {g: t for g, (t, _) in GROUPS.items()},
        "lines": ls,
    }
    with open(OUT_MANIFEST, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(manifest, fh, indent=1, ensure_ascii=False)
        fh.write("\n")
    data = {"version": 1, "groups": {g: [{"id": l["id"], "text": l["text"]} for l in ls if l["group"] == g] for g in GROUPS}}
    os.makedirs(os.path.dirname(OUT_DATA), exist_ok=True)
    with open(OUT_DATA, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(data, fh, indent=1, ensure_ascii=False)
        fh.write("\n")
    lang = {"subtitles.anime_multiverse.kakashi." + l["id"]: l["text"] for l in ls}
    with open(OUT_LANG, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(lang, fh, indent=1, ensure_ascii=False)
        fh.write("\n")
    print("lines:", len(ls), "groups:", len(GROUPS))


if __name__ == "__main__":
    main()
