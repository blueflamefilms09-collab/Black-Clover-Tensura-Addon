#!/usr/bin/env python3
"""The spoken lines of the Spirit of Light's seraph form (docs/magic/spirit_light.md): writes tools/audio/spirit_light_lines.json (35 lines: id, cue, voice, prompt with the ElevenLabs v4 audio tags,
the plain text) and the lang fragment tools/lang/spirit_light_voice.json (the same text, shown on the owner's action bar when the line is spoken).

The seraph speaks in ONE clear, resonant, gentle voice (an ElevenLabs library voice, never a clone of a real person; the licence is recorded in the doc). The lion cub never speaks: it makes
creature sounds (tools/gen_spirit_light_sounds.py). Ids are spiritlight_<cue>_<n>; the cues are the ones of spiritlight.LightMoodRules.Cue.

    python3 -B tools/audio/spirit_light_lines.py
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
VOICE = "eiNgkRczHJmif3gxSoyz"        # "Cherine Hani - Warm Storyteller" (ElevenLabs library, professional voice): warm, calm, clear, reassuring
VOICE_NAME = "Cherine Hani - Warm Storyteller"

# cue -> [(tags, text)]
LINES = {
    "greet": [("[serene, gentle]", "Peace be with you. I am here."), ("[warm, resonant]", "The light remembers you. I rise at your side."),
              ("[gentle]", "Be not afraid. I am your light."), ("[calm, resonant]", "I answer. Walk, and I will go before you.")],
    "farewell": [("[soft, tender]", "Rest now. I will keep the light for you."), ("[gentle]", "Until you call me again, be well."), ("[serene, fading]", "I return to the light. Peace.")],
    "rise": [("[radiant, resonant]", "Behold. The light takes its true form."), ("[awed, gentle]", "Many eyes open, that you may not be alone."), ("[firm, resonant]", "Stand firm. The heavens are with you.")],
    "settle": [("[soft]", "The vision fades. I am still near."), ("[gentle, quiet]", "I fold my wings, but I do not leave.")],
    "beam": [("[resonant, calm]", "Let the light go forth."), ("[firm, clear]", "Be pierced by the truth."), ("[gentle, swift]", "A ray of mercy, swift and straight."),
             ("[clear]", "Light, strike true."), ("[resonant, serene]", "Nothing hidden can stand before it.")],
    "pulse": [("[calm, resonant]", "Nothing hides from the light."), ("[gentle, clear]", "Let every shadow be known."), ("[quiet, sure]", "I see them. Now, so do you."), ("[clear, commanding]", "Be revealed.")],
    "damage": [("[pained, steady]", "I am hurt, but I endure."), ("[gentle, strained]", "The light flickers, yet it does not fail."), ("[calm, firm]", "Do not mind me. Keep your eyes forward."),
               ("[soft gasp, brave]", "Ah... this pain is a small thing.")],
    "low": [("[urgent, gentle]", "Your strength is failing. Hold on, I am with you."), ("[firm, caring]", "Do not yield. The light is not done with you."), ("[soft, steady]", "Breathe. Step back, and breathe.")],
    "win": [("[warm, relieved]", "Well done. The dark gives way."), ("[gentle, solemn]", "Justice is done. Rejoice, but gently."), ("[radiant, calm]", "The light prevails.")],
    "death": [("[grieving, tender]", "No... not yet. Rise again, my friend."), ("[soft, sorrowful]", "Sleep, bright one. The light will find you.")],
    "idle": [("[serene, quiet]", "All is calm. The light is with us."), ("[gentle, watchful]", "Walk on. I am watching the way.")],
}


def main():
    out, lang = [], {}
    for cue, items in LINES.items():
        for n, (tags, text) in enumerate(items, 1):
            lid = "spiritlight_%s_%d" % (cue, n)
            out.append({"id": lid, "cue": cue, "voice": VOICE, "prompt": "%s %s" % (tags, text), "text": text})
            lang["nusmp.spiritlight.say." + lid] = text
    doc = {"voice_id": VOICE, "voice_name": VOICE_NAME, "model": "eleven_v4", "lines": out}
    with open(os.path.join(HERE, "spirit_light_lines.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(doc, f, indent=1, ensure_ascii=False)
        f.write("\n")
    with open(os.path.join(ROOT, "tools", "lang", "spirit_light_voice.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(lang, f, indent=1, ensure_ascii=False)
        f.write("\n")
    print(len(out), "lines")


if __name__ == "__main__":
    main()
