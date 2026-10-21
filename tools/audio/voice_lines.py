#!/usr/bin/env python3
"""The spoken voice line manifest (docs/audio/voice_plan.md): writes tools/audio/voice_lines.json.
Each line: id, voice role, voice id, prompt (ElevenLabs v4 with audio tags). Deterministic; the Djinn lines are built from docs/djinn_roster_36.json."""
import json, os
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
VOICES = {"system": "iBeucLPI8hr8MOJjjYTL", "boss": "d3qrd1ge6zWDvUxIeXiH", "npc": "fV5vDeUO8ghvK7ntjiVC", "djinn": "QjEdRoRdZC2i3ncmFpNy"}
SYSTEM = {
    "floor_enter": "System notice. You have entered a new floor.",
    "floor_sealed": "Warning. The way ahead is sealed until every enemy has fallen.",
    "lockdown_start": "Challenge room detected. The doors are locked.",
    "lockdown_clear": "Challenge cleared. The doors are open.",
    "ambush_warning": "Warning. Hostile squad approaching from ahead.",
    "rest_safe": "Safe zone reached. You may rest.",
    "boss_ahead": "Warning. A floor boss awaits beyond this door.",
    "rift_open": "Notice. A rift has opened nearby.",
    "floor_clear": "Floor cleared. The stairs are open.",
    "low_health": "Warning. Vital signs critical.",
}
BOSS = {  # family: (intro, phase2, death)
    "holy": ("[calm, resonant] Kneel, wanderer. The light has judged you, and found you wanting.", "[louder] The heavens themselves answer my call!", "[fading] The light... forgives... even this."),
    "evil": ("[low, slow] You climbed all this way, only to kneel before me. Come, mortal.", "[snarling] Enough games! Taste my true power!", "[faltering] Impossible... I am... a god..."),
    "undead": ("[hollow whisper] You will join us. All who enter here join us.", "[rattling] The dead rise at my word!", "[rasping] Rest... at last..."),
    "fire": ("[roaring] Burn! Everything you love will burn!", "[furious] My flames are only beginning!", "[dying] The fire... goes out..."),
    "ice": ("[cold, measured] Silence, little spark. The cold will take you gently.", "[sharp] The frost does not forgive!", "[softly] So cold... and so quiet..."),
    "storm": ("[booming] Hear the thunder! It is my voice, and it speaks your end!", "[shouting] Let the sky fall upon you!", "[breathless] The storm... passes..."),
    "void": ("[distant, echoing] There is nothing here. There was never anything here.", "[unsettling] The void opens. Come inside.", "[fading] Return... to nothing..."),
    "dragon": ("[deep growl] Tiny creature. You dare enter my hoard?", "[thundering] Feel the fury of a dragon!", "[slow, weary] A worthy foe... farewell..."),
    "devil": ("[silky, amused] Ah, a visitor. Let us make a bargain, you and I.", "[cackling] Now you see what a devil truly is!", "[snarling] You will regret this contract!"),
    "angel": ("[serene] Be still. I will end your suffering swiftly.", "[radiant] Witness the wrath of the heavens!", "[gentle] I return to the light..."),
}
NPC = {
    "merchant_greet": "[warm, cheerful] Welcome back, adventurer! You made it out alive, and with loot, too.",
    "merchant_sell": "[friendly] Fine pieces, these. A fair price, as always.",
    "merchant_buy": "[pleased] Pleasure doing business with you!",
    "merchant_farewell": "[warm] Come back soon, and stay alive down there!",
    "quest_offer": "[earnest] I have a task for you, if you are brave enough.",
    "quest_done": "[delighted] You did it! Here is your reward, well earned.",
    "quest_farewell": "[kind] Be careful out there, adventurer.",
    "guide_greet": "[friendly] First time in the dungeon? Stay close to the stairs and keep your wits about you.",
}
AFF_TAG = {"Lightning": "crackling", "Fire": "burning", "Water": "flowing", "Wind": "sweeping", "Earth": "rumbling", "Ice": "frosty", "Light": "radiant", "Dark": "shadowy"}
def main():
    lines = []
    for k, t in SYSTEM.items(): lines.append({"id": "system_" + k, "role": "system", "voice": VOICES["system"], "prompt": t})
    for fam, (a, b, c) in BOSS.items():
        for n, t in zip(("intro", "phase2", "death"), (a, b, c)): lines.append({"id": "boss_%s_%s" % (fam, n), "role": "boss", "voice": VOICES["boss"], "prompt": t})
    for k, t in NPC.items(): lines.append({"id": "npc_" + k, "role": "npc", "voice": VOICES["npc"], "prompt": t})
    for r in json.load(open(os.path.join(ROOT, "docs", "djinn_roster_36.json"))):
        tag = AFF_TAG.get(r["affinity"], "dark")
        lines.append({"id": "djinn_%s_summon" % r["id"], "role": "djinn", "voice": VOICES["djinn"], "prompt": "[%s, low] I am %s, %s. Speak your command, vessel." % (tag, r["name"], r["title"])})
        lines.append({"id": "djinn_%s_attack" % r["id"], "role": "djinn", "voice": VOICES["djinn"], "prompt": "[%s, fierce] Feel the %s of %s!" % (tag, r["affinity"].lower(), r["name"])})
    json.dump(lines, open(os.path.join(HERE, "voice_lines.json"), "w"), indent=1)
    print(len(lines), "lines")
main()
