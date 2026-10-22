#!/usr/bin/env python3
"""The Sylph's spoken line manifest (docs/magic/sylph.md, "Voice lines"): writes tools/audio/sylph_lines.json, the twin of tools/audio/voice_lines.json for the wind spirit Sylph.

    python3 -B tools/audio/sylph_lines.py            writes the manifest (every line she is meant to have)

Each line: id (sylph_<group>_<n>), role "sylph", voice (the ElevenLabs library voice id), group (the happening it is said at, see sylph/SylphMood), prompt (what the voice is asked to say: eleven_v4
with audio tags) and text (what she says, for the subtitle; no tags). The audio is generated one line at a time with the ElevenLabs connector (docs/audio/spoken_voices.md describes the way) and
converted by `python3 -B tools/audio/sylph_voice_convert.py <raw dir>` (mono Ogg Vorbis, 32 kHz, trimmed, loudness normalised, `sounds.json` entries and the Java list appended for the lines whose
file exists). Deterministic.
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
VOICE = "Nggzl2QAXh3OijoXD116"          # "Candy - Girly and Sweet", an ElevenLabs library voice (young, cheerful, bright): see the licence note of docs/magic/sylph.md

# group -> [(tags, text)], the lines of one happening, in order; the prompt is "[tags] text"
GROUPS = [
    ("link", [("cheerful", "Hi hi! It's me, Sylph! Let's go together!"),
              ("excited", "I'm here! I'm here! Where are we flying to?"),
              ("sweetly", "Yay, you called! I'll stay right beside you!")]),
    ("unlink", [("softly", "Okay, bye bye! Call me again, okay?"),
                ("cheerful", "See you later! Don't have too much fun without me!"),
                ("sleepy", "Mm, I'll go nap in the wind. Bye bye.")]),
    ("cast", [("cheerful", "Here goes the wind!"),
              ("playful", "Whoosh! Take this!"),
              ("excited", "Gust, gust, go!"),
              ("determined", "Wind, listen to me!"),
              ("giggling", "Hee hee, blow them away!"),
              ("bright", "Fly, little breeze!"),
              ("cheerful", "Leave it to me!")]),
    ("big", [("powerful, shouting", "Spirit of the wind, answer me!"),
             ("intense", "Everyone, stand back! Here comes the storm!"),
             ("shouting", "Tornado, go go go!")]),
    ("shout", [("fierce, shouting", "I won't let anyone hurt you!"),
               ("powerful", "This is my true power!"),
               ("commanding", "Wind of the heavens, rise!")]),
    ("hurt", [("gasp", "Ouch! Are you okay?!"),
              ("worried", "Hey! That hurt! Be careful!"),
              ("angry, cute", "Don't hurt my friend!"),
              ("startled", "Eek! Watch out!")]),
    ("startle", [("yelp", "Waah! What was that?!"),
                 ("gasp", "Kyaa! Don't scare me like that!")]),
    ("idle", [("humming", "La la la, the wind feels so nice today."),
              ("curious", "Hey, hey, what are we doing next?"),
              ("dreamy", "The clouds look like cotton candy. Yum!"),
              ("playful", "I'm bored. Let's go somewhere fun!"),
              ("cheerful", "Do you hear that? The wind is singing."),
              ("sighs", "Hmm hmm hmm... nothing's happening."),
              ("proud", "I'm the strongest wind spirit, you know!")]),
    ("worry", [("worried", "You're hurt! Please be careful!"),
               ("scared", "Are you okay? Your health is low!"),
               ("anxious", "Hold on! Don't give up!"),
               ("trembling", "Please, please don't fall down!")]),
    ("cheer", [("excited", "Yay! You did it!"),
               ("proud", "That was amazing! Do it again!"),
               ("delighted", "Hooray! One down!"),
               ("cheerful", "You're so strong!"),
               ("giggles", "Hee hee, nice one!")]),
    ("sad", [("crying", "No... please wake up..."),
             ("sobbing", "Don't leave me alone..."),
             ("tearful", "I'll wait for you. Come back soon.")]),
    ("giggle", [("giggles", "Hee hee hee!"),
                ("laughs", "Ahaha, tee hee!"),
                ("giggling", "Hihi, that tickles!"),
                ("laughs happily", "Ehehe!")]),
    ("pout", [("pouting", "Hmph! I'm not talking to you."),
              ("sulky", "Hmph. You never play with me.")]),
    ("smug", [("smug", "Hehe, I knew I'd be useful."),
              ("teasing", "Told you I was the best.")]),
    ("sleepy", [("yawning", "Ahh... I'm sleepy..."),
                ("drowsy", "Just five more minutes...")]),
    ("victory", [("cheering", "We won! We won!"),
                 ("excited", "Victory! I'm so happy!")]),
]


def lines():
    out = []
    for group, items in GROUPS:
        for i, (tags, text) in enumerate(items, 1):
            out.append({"id": "sylph_%s_%d" % (group, i), "role": "sylph", "voice": VOICE, "group": group, "prompt": "[%s] %s" % (tags, text), "text": text})
    return out


def main():
    ls = lines()
    with open(os.path.join(HERE, "sylph_lines.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(ls, f, indent=1, ensure_ascii=False)
        f.write("\n")
    print(len(ls), "lines in", len(GROUPS), "groups")


if __name__ == "__main__":
    main()
