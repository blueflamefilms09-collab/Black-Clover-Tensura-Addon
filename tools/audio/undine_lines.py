#!/usr/bin/env python3
"""The spoken lines of the Undine (docs/magic/undine.md, section "Voice lines"): writes tools/audio/undine_lines.json, the manifest the voice tools and the tests read.

Each line: id (undine_<group>_<n>), group (the moment it is said: link, farewell, cast, wave, hurt, idle, lowhp, win, died), voice (the ElevenLabs library voice id), model, prompt (ElevenLabs v4
with audio tags). The voice is a LIBRARY voice ("Julie I", a mature, warm, soft, calm woman; id below): not cloned from anybody by us. Her tone for the Undine comes from the tags.
The group names and counts are src/main/java/com/newuniverse/nusmp/undine/UndineLines.java's; UndineVoiceAssetsTest keeps the manifest, the Java list, sounds.json and the files in step.

  python3 -B tools/audio/undine_lines.py                  write the manifest
  python3 -B tools/audio/undine_voice_generate.py         (needs ELEVENLABS_API_KEY) generate every line whose mp3 is missing, then convert them
  python3 -B tools/audio/undine_voice_convert.py RAW_DIR  convert the mp3 takes in RAW_DIR (<id>.mp3) into the game's ogg files and register them
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
VOICE_ID = "g12yXXabMWeFoLMzxPrF"          # ElevenLabs library voice "Julie I": mature, warm, soft, reflective, calm (category: professional, shared in the voice library)
VOICE_NAME = "Julie I"
MODEL = "eleven_v4"

GROUPS = {
    "link": ["[soft, serene] I am here, beside you.", "[gentle] The water answers you, as it always has.", "[warm, airy] Call, and I will rise.", "[soft smile] Good to see you again, my friend."],
    "farewell": ["[soft, wistful] I return to the water. Call me again.", "[gentle] Until the tide brings us together.", "[quiet] Rest well. I will be listening."],
    "cast": ["[calm] Flow.", "[gentle, firm] Let the water carry it.", "[softly] Rise, and run.", "[calm, focused] Tides, obey.", "[quiet] Go, little wave.", "[serene] As the river wills.",
             "[firm] Sea, lend me your strength.", "[focused] Be washed away."],
    "wave": ["[powerful, resonant] Rise, great sea! Crash upon them!", "[commanding, echoing] Hear the roar of the deep!", "[intense] Drown their fury beneath the waves!",
             "[ringing] Let the ocean remember its name!"],
    "hurt": ["[concerned] Be careful.", "[worried] You are bleeding.", "[firm, caring] Do not let them touch you.", "[gasps] Oh!", "[concerned] Stay with me."],
    "idle": ["[dreamy, soft] The water is calm tonight.", "[gentle, humming] Hm... listen to the stream.", "[serene] Every river finds the sea in the end.", "[soft] I can hear the rain, far away.",
             "[wistful] The tide remembers everyone.", "[warm] Take your time. I am not going anywhere."],
    "lowhp": ["[worried] Your strength is fading. Let me help you.", "[urgent, gentle] Please, be careful. You are hurt.", "[soft, pleading] Do not give up. I am with you.",
              "[concerned] Breathe. The water will mend you."],
    "win": ["[pleased] Well done.", "[soft smile] The tide is yours.", "[warm] As I knew you would.", "[gentle laugh] Beautifully done."],
    "died": ["[heartbroken, quiet] No... come back to the water...", "[sorrowful] Sleep now. I will carry you home."],
}


def lines():
    out = []
    for g, prompts in GROUPS.items():
        for i, p in enumerate(prompts, 1):
            out.append({"id": "undine_%s_%d" % (g, i), "group": g, "voice": VOICE_ID, "voice_name": VOICE_NAME, "model": MODEL, "prompt": p})
    return out


def main():
    ls = lines()
    with open(os.path.join(HERE, "undine_lines.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(ls, f, indent=1, ensure_ascii=False)
        f.write("\n")
    print(len(ls), "lines")


if __name__ == "__main__":
    main()
