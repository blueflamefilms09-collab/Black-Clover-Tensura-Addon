#!/usr/bin/env python3
"""Writes the lang fragment of Shishou (tools/lang/shishou.json, flat key -> text; the owner merges fragments into en_us.json).

    python3 -B tools/gen_shishou_lang.py

The skill names are the English names plus the Japanese name (kanji and romaji) the owner's notes give. SOURCE of each name: the romaji (Nennou, Nenka, Ibukuro, Taju Kekkai, Chousokuryou, Magikansi, Kuroen,
Kurokaminari, Kuukan Idou, Kuukan Zandan, Maka) are the owner's own words and the Japanese web-search snippets (not full pages); the KANJI are written from those readings and are NOT verified against a wiki page
(English and Japanese wiki hosts are not reachable from this environment). The names that are the owner's design only (Skill Eater, Skill Sharing, Wielder Registration, Stat Concealment, Elemental Synthesis,
Sword Arts Mastery, Vibration Magic, Doppelganger, Divine Sword Release and the absorbed magics) carry a descriptive Japanese name that is an editorial guess. docs/items/shishou.md lists this honestly.
The words of the voice lines are tools/lang/shishou_voice.json (tools/audio/shishou_lines.py).
"""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
OUT = os.path.join(ROOT, "tools", "lang", "shishou.json")

SKILLS = {
    "telekinesis": ("Telekinesis", "念動 Nennou"),
    "thought_communication": ("Thought Communication", "思念伝達 Nenka"),
    "ultra_speed_regeneration": ("Ultra-Speed Regeneration", "超速再生 Chousokuryou"),
    "stat_concealment": ("Stat Concealment", "偽装 Gisou"),
    "skill_eater": ("Skill Eater", "技能捕食 Hoshoku"),
    "skill_sharing": ("Skill Sharing", "技能共有 Gaisou"),
    "wielder_registration": ("Wielder Registration", "使用者登録 Keiyaku"),
    "appraisal": ("Appraisal", "鑑定 Kantei"),
    "multilayer_barrier": ("Multilayer Barrier", "多重結界 Taju Kekkai"),
    "stomach": ("Stomach", "胃袋 Ibukuro"),
    "elemental_synthesis": ("Elemental Synthesis", "属性合成 Gouseisei"),
    "sword_arts_mastery": ("Sword Arts Mastery", "剣術 Kenjutsu"),
    "magic_sense": ("Magic Sense", "魔力感知 Magikansi"),
    "black_flame": ("Black Flame", "黒炎 Kuroen"),
    "stealth": ("Stealth and Camouflage", "隠蔽 Inpei"),
    "vibration_magic": ("Vibration Magic", "振動魔法 Shindou"),
    "spatial_motion": ("Spatial Motion", "空間移動 Kuukan Idou"),
    "maka": ("Demon Lord's Haki", "魔王覇気 Maka"),
    "divine_sword_release": ("Divine Sword Release", "神剣解放 Shinken Kaihou"),
    "black_lightning": ("Black Lightning", "黒雷 Kurokaminari"),
    "spatial_blade": ("Spatial Blade", "空間斬 Kuukan Zandan"),
    "doppelganger": ("Doppelganger", "分身 Bunshin"),
    "fire_magic": ("Fire Magic", "火魔法 Kaen"),
    "wind_magic": ("Wind Magic", "風魔法 Fuuma"),
    "earth_magic": ("Earth Magic", "土魔法 Tsuchi"),
    "water_ice_magic": ("Water and Ice Magic", "水氷魔法 Hyousui"),
    "darkness_magic": ("Darkness Magic", "闇魔法 Yami"),
    "healing_magic": ("Healing Magic", "回復魔法 Kaifuku"),
    "purification": ("Purification", "浄化 Joka"),
    "poison_coat": ("Poison Coat", "毒 Doku"),
    "spatial_motion_short": ("Spatial Motion (short)", "空間移動 Kuukan Idou"),
}

TEXT = {
    "item.nusmp.shishou": "Shishou",
    "entity.nusmp.shishou": "Shishou",
    "effect.nusmp.black_flame": "Black Flame",
    "key.nusmp.shishou_throw": "Shishou: throw / recall",
    "key.nusmp.shishou_cast": "Shishou: cast the selected skill",
    "key.nusmp.shishou_next": "Shishou: select the next skill",
    "shishou.grade": "Akashic Relic - Grade: EX-Singularity",
    "shishou.tooltip.level": "Level %s",
    "shishou.tooltip.soulbound": "Soulbound. Only its wielder may hold it.",
    "shishou.tooltip.stomach": "Shift + right click: Stomach (internal storage).",
    "shishou.tooltip.keys": "Keys: throw or recall, cast, next skill (Controls > Multiverse of Anime).",
    "shishou.tooltip.skill": "%s (%s)",
    "shishou.false.name": "Rusty Sword",
    "shishou.false.line": "An old, ordinary sword.",
    "shishou.theft.line": "Let go of me, you aren't my wielder.",
    "shishou.storage.title": "Shishou: Stomach",
    "shishou.thought.broadcast": "Shishou: %s",
    "shishou.msg.not_ready": "He is still hunting. Wait until he settles.",
    "shishou.msg.reserved": "He waits for his wielder.",
    "shishou.msg.exists": "There is only one Shishou in this world.",
    "shishou.msg.copy": "A copy of me cannot exist.",
    "shishou.msg.torn_away": "He tears himself out of your hands.",
    "shishou.msg.hold": "Hold me to cast.",
    "shishou.msg.cooldown": "%s is recharging (%s s).",
    "shishou.msg.mana": "My mana is too low for %s.",
    "shishou.msg.nothing": "Nothing answers.",
    "shishou.msg.stomach_locked": "Stomach unlocks at level 21.",
    "shishou.hud.level": "Lv %s",
    "shishou.hud.mana": "Mana %s / %s",
    "shishou.hud.no_skill": "No skill to cast yet",
    "shishou.hud.barrier": "Barrier",
    "shishou.hud.sense": "Sense",
    "shishou.hud.stealth": "Veil",
    "shishou.hud.released": "Released",
    "shishou.hud.health": "Health %s / %s",
    "shishou.hud.estimate": "Level ~%s   Mana ~%s",
    "shishou.hud.weak": "Weak to:",
    "shishou.hud.gives": "Skill: %s",
    "shishou.hud.item_value": "Worth %s experience to eat",
    "shishou.element.fire": "fire",
    "shishou.element.ice": "ice",
    "shishou.element.lightning": "lightning",
    "shishou.element.poison": "poison",
    "shishou.element.dark": "darkness",
    "shishou.element.holy": "holy",
    "shishou.element.none": "none",
}


def main():
    out = dict(TEXT)
    for sid, (en, jp) in SKILLS.items():
        out["shishou.skill." + sid] = en
        out["shishou.skill." + sid + ".jp"] = jp
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        json.dump(out, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(len(out), "keys")


if __name__ == "__main__":
    main()
