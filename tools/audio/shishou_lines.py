#!/usr/bin/env python3
"""The spoken lines of Shishou (docs/items/shishou.md, section "Voice"): writes tools/audio/shishou_lines.json, the manifest the voice tools and the tests read, the words of every line as lang
text (tools/lang/shishou_voice.json: key shishou.line.<id>) and the TEXTS block of src/main/java/com/newuniverse/nusmp/shishou/ShishouVoiceLines.java.

NO AUDIO IS GENERATED YET: the ElevenLabs account is out of credits and the voice itself has still to be designed with the owner. The manifest header carries the voice id as `"voice": null` until one is
chosen; tools/audio/shishou_voice_generate.py refuses to run while it is null. Until then no line is registered (ShishouVoiceLines.REGISTERED is empty) and every moment is shown as Thought Communication text
on the wielder's action bar only.

Each line: id (shishou_<group>_<n>), group (the moment it is said), prompt (ElevenLabs v4 text with a leading audio tag such as [calm, dry, wise]), text (the words alone: the subtitle and the action-bar text).
The group names and counts are src/main/java/com/newuniverse/nusmp/shishou/ShishouVoiceLines.java's; ShishouVoiceAssetsTest keeps the manifest, the Java list, the lang text, sounds.json and the files in step.

One-command finish once a voice is chosen and credits exist:
  1. put the chosen voice id in the header of tools/audio/shishou_lines.json ("voice": "<id>", "voice_name": "<name>")  (or set VOICE_ID / VOICE_NAME below and run this script again)
  2. ELEVENLABS_API_KEY=<key> python3 -B tools/audio/shishou_voice_generate.py <raw_dir>     (generates the missing takes, then converts and registers them: it calls the converter itself)
  or by hand:   python3 -B tools/audio/shishou_voice_convert.py <raw_dir>

  python3 -B tools/audio/shishou_lines.py        write the manifest, the lang text and the Java TEXTS block
"""
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
VOICE_ID = None                                     # the ElevenLabs voice id, chosen with the owner (None until then)
VOICE_NAME = None
MODEL = "eleven_v4"
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "shishou", "ShishouVoiceLines.java")
LANG = os.path.join(ROOT, "tools", "lang", "shishou_voice.json")

GROUPS = {
    "claim": ["[calm, dry, amused] So you are my wielder. Try not to bore me.", "[warm, measured] I will teach you what a sword can be.", "[quiet, wise] Hold me properly. We have a great deal to learn."],
    "recall": ["[calm] Back in your hand. Good.", "[dry] You called, wielder. I came."],
    "throw": ["[calm, focused] Leave the fighting to me for a moment.", "[dry] Fly, then. I will hold the line."],
    "levelup": ["[pleased, measured] I have grown. Another step, wielder.", "[dry, proud] I grow stronger, as I intended.", "[calm] Power gathers in me. Use it well."],
    "skill_utility": ["[curious, calm] A new skill. How useful.", "[matter-of-fact] This will make your life easier, wielder."],
    "skill_magic": ["[intrigued] I have eaten a new spell. Delicious.", "[wise, calm] Magic, absorbed. Let us put it to use."],
    "skill_combat": ["[sharp, pleased] A new edge for the blade.", "[calm, confident] Now we can cut deeper."],
    "skill_space": ["[quiet, amused] Space bends a little easier now.", "[calm] A new way to move. Do not waste it."],
    "eat": ["[satisfied, dry] Delicious.", "[calm] Eaten. Its power is mine.", "[amused] Do not look away. This is how a sword grows."],
    "lowmana": ["[calm, concerned] My mana is running low, wielder.", "[dry, serious] I need rest. Do not draw on me now."],
    "dormant": ["[quiet, weary] I must sleep for a while. Find me again.", "[calm] My strength is spent. I will wait."],
    "danger": ["[urgent, calm] Wielder, hostiles approach from behind.", "[sharp] Danger behind you. Turn.", "[firm] Your health is failing. Retreat, or let me fight."],
    "thief": ["[cold, firm] Let go of me, you aren't my wielder.", "[sharp] Hands off. I do not serve you."],
    "appraise": ["[calm, analytical] I have looked at it. Listen carefully.", "[dry] It is weak to something. Find out what.", "[matter-of-fact] An ordinary thing, but it may be eaten."],
    "barrier_on": ["[calm, firm] Barrier raised. Nothing gets through."],
    "barrier_off": ["[tense] The barrier is broken. Be careful."],
    "victory": ["[satisfied] Well fought.", "[dry, pleased] As expected. Nothing could stand against us."],
    "idle": ["[calm, wise] A sword should never stop learning.", "[dry, amused] You swing me well enough, for a beginner.", "[thoughtful] Every battle is a lesson, wielder.", "[quiet] Quiet today. I do not trust quiet."],
}


def spoken_text(prompt):
    """The words of a prompt without its leading audio tags: '[calm] Back in your hand.' -> 'Back in your hand.'"""
    return re.sub(r"\[[^\]]*\]\s*", "", prompt).strip()


def lines():
    out = []
    for g, prompts in GROUPS.items():
        for i, p in enumerate(prompts, 1):
            out.append({"id": "shishou_%s_%d" % (g, i), "group": g, "prompt": p, "text": spoken_text(p)})
    return out


def main():
    ls = lines()
    path = os.path.join(HERE, "shishou_lines.json")
    voice, name = VOICE_ID, VOICE_NAME
    if os.path.exists(path):                                          # a voice id written into the header by hand is kept
        try:
            old = json.load(open(path, encoding="utf-8"))
            if isinstance(old, dict):
                voice = voice or old.get("voice")
                name = name or old.get("voice_name")
        except ValueError:
            pass
    doc = {"voice": voice, "voice_name": name, "model": MODEL,
           "note": "No audio is generated yet: voice is null until the owner and the assistant have designed or chosen it. tools/audio/shishou_voice_generate.py refuses to run while it is null.",
           "lines": ls}
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(doc, f, indent=1, ensure_ascii=False)
        f.write("\n")
    lang = json.load(open(LANG, encoding="utf-8")) if os.path.exists(LANG) else {}
    lang = {k: v for k, v in lang.items() if not k.startswith("shishou.line.")}
    for ln in ls:
        lang["shishou.line." + ln["id"]] = ln["text"]
    with open(LANG, "w", encoding="utf-8", newline="\n") as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write("\n")
    java = open(JAVA, encoding="utf-8").read()
    a, b = java.index("    // BEGIN TEXTS"), java.index("    // END TEXTS")
    texts = ", ".join('"%s"' % ln["text"].replace("\\", "\\\\").replace('"', '\\"') for ln in ls)
    block = ("    // BEGIN TEXTS (rewritten by tools/audio/shishou_lines.py: the words of every manifest line, in manifest order; they are the Thought Communication text and the fallback of the lang keys)\n"
             "    private static final String[] TEXTS = {%s};\n" % texts)
    java = java[:a] + block + java[b:]
    with open(JAVA, "w", encoding="utf-8", newline="\n") as f:
        f.write(java)
    print(len(ls), "lines; voice =", voice)


if __name__ == "__main__":
    main()
