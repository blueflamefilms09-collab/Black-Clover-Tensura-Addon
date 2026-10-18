"""elite/items: writes the lang fragment tools/elite/items_lang.json (English). The owner's rule for this job is that en_us.json is NOT edited by the builders: the lead merges the fragments.

    python3 -B tools/elite/gen_items_lang.py [--check]

Every translatable string the subsystem uses is here: item and block names, the creative tab, the crate tiers, the tooltips (the weapons' technique lines are generated from their data files, so
the text a player reads is the text of the numbers), the messages and the subtitles. Every Java call that uses Component.translatableWithFallback has its default text here too, so a missing
fragment only changes the wording, never shows a raw key. Ids are read from the Java sources (EliteIds.java, PlushKinds.java) so the files cannot drift apart.
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from art_lib import DATA, ROOT, write_text  # noqa: E402

JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "elite", "items")

NAMES = {
    "astral_edge": "Astral Edge", "void_edge": "Void Edge", "fenrir_sword": "Fenrir Sword", "runic_sword_renewed": "Runic Sword (Renewed)", "runic_sword_ancient": "Runic Sword (Ancient)",
    "runic_sword_mythical": "Runic Sword (Mythical)",
    "nightingale_hood": "Nightingale Hood", "nightingale_cloak": "Nightingale Cloak", "nightingale_leggings": "Nightingale Leggings", "nightingale_boots": "Nightingale Boots",
    "potential_catalyst": "Potential Catalyst", "health_potion_small": "Small Health Potion", "health_potion_medium": "Medium Health Potion", "health_potion_large": "Large Health Potion",
    "chronicle_book": "Chronicle Book", "walpurgis_orb": "Walpurgis Orb", "walpurgis_seal": "Walpurgis Seal",
    "common_crate": "Common Crate", "rare_crate": "Rare Crate", "elite_crate": "Elite Crate",
    "common_crate_key": "Common Crate Key", "rare_crate_key": "Rare Crate Key", "elite_crate_key": "Elite Crate Key",
}
LORE = {
    "astral_edge": "Forged from the metal of fallen stars; it remembers the shape of the sky.",
    "void_edge": "It cuts the space it passes through. Something looks back from its cracks.",
    "fenrir_sword": "Named for the wolf that swallowed the sun. Its wolves are still hungry.",
    "runic_sword_renewed": "An old rune-blade, mended. One rune still answers.",
    "runic_sword_ancient": "A rune-blade from before the first kingdoms. Two runes answer.",
    "runic_sword_mythical": "The legend the rune-smiths wrote about themselves. Three runes answer.",
}


def java_list(name):
    src = open(os.path.join(JAVA, "EliteIds.java"), encoding="utf-8").read()
    m = re.search(r"List<String> %s = List\.of\(([^)]*)\)" % name, src)
    return re.findall(r'"([a-z0-9_]+)"', m.group(1))


def plush_kinds():
    src = open(os.path.join(JAVA, "plush", "PlushKinds.java"), encoding="utf-8").read()
    return re.findall(r'k\("([^"]+)",\s*\d\)', src)


def build():
    d = {}
    for i in java_list("WEAPONS") + java_list("ARMOR") + java_list("CONSUMABLES"):
        d["item.nusmp." + i] = NAMES[i]
    for t in ("common", "rare", "elite"):
        d["block.nusmp.%s_crate" % t] = NAMES[t + "_crate"]
        d["item.nusmp.%s_crate_key" % t] = NAMES[t + "_crate_key"]
        d["crate_tier.nusmp." + t] = t.capitalize()
    for name in plush_kinds():
        d["block.nusmp.plush_" + name.lower().replace(" ", "_")] = name + " Plushie"
    d["itemGroup.nusmp.elite_items"] = "Elite Items and Plushies"
    # weapons: the lore line and the two technique lines come from the data files
    for i in java_list("WEAPONS"):
        a = json.load(open(os.path.join(DATA, "elite", "items", "abilities", i + ".json"), encoding="utf-8"))
        d["tooltip.nusmp.%s.lore" % i] = LORE[i]
        p, alt = a["primary"], a.get("alt", a["primary"])
        d["tooltip.nusmp.%s.primary" % i] = "Right-click: %s. %s" % (p["name"], p.get("desc", ""))
        d["tooltip.nusmp.%s.alt" % i] = "Sneak + right-click: %s. %s" % (alt["name"], alt.get("desc", ""))
    d.update({
        "tooltip.nusmp.elite_weapon.swing": "A full-strength swing adds its bonus strike.",
        "tooltip.nusmp.runic.slots": "Rune slots: %s of %s used",
        "tooltip.nusmp.runic.empty": "  (empty slot)",
        "tooltip.nusmp.runic.how": "Drag an engraving book or a rune onto the sword with the right mouse button.",
        "message.nusmp.runic.full": "All %s rune slots are in use.",
        "message.nusmp.runic.duplicate": "This rune is already slotted.",
        "message.nusmp.runic.slotted": "Rune slotted: %s (%s/%s)",
        "tooltip.nusmp.nightingale.set": "Nightingale set (4 pieces): Nightsong",
        "tooltip.nusmp.nightingale.speed": "  Speed I",
        "tooltip.nusmp.nightingale.sight": "  Night Vision in the dark",
        "tooltip.nusmp.nightingale.lullaby": "  Sneaking: the Lullaby heals you and your allies nearby",
        "tooltip.nusmp.health_potion.heal": "Restores %s health at once.",
        "tooltip.nusmp.health_potion.regen": "Then regenerates %s more over %s s.",
        "tooltip.nusmp.health_potion.absorb": "%s Absorption for %s s.",
        "tooltip.nusmp.health_potion.cooldown": "Shared cooldown: %s s.",
        "tooltip.nusmp.catalyst.line1": "Raises you for good: %s Skill Points and +%s maximum health.",
        "tooltip.nusmp.catalyst.line2": "At most %s can be used by one person.",
        "tooltip.nusmp.catalyst.use": "Right-click to use it. It is used up.",
        "message.nusmp.catalyst.spent": "You can hold no more potential: %s of %s catalysts used.",
        "message.nusmp.catalyst.used": "Your potential opens: %s of %s catalysts used.",
        "message.nusmp.chronicle.blank": "The pages are blank. No chronicle is written here yet.",
        "message.nusmp.chronicle.bound": "A chronicle is bound to this book: %s. Its pages will open in a later season.",
        "tooltip.nusmp.chronicle.blank": "A blank chronicle.",
        "tooltip.nusmp.chronicle.id": "Chronicle: %s",
        "tooltip.nusmp.chronicle.note": "It opens nothing yet; it only remembers which chronicle it belongs to.",
        "tooltip.nusmp.walpurgis_orb": "A crystal of the Walpurgis council.",
        "tooltip.nusmp.walpurgis_seal": "The seal of the Walpurgis council.",
        "tooltip.nusmp.elite_ref": "Bound to: %s",
        "tooltip.nusmp.crate_key": "Opens one %s Crate. Use it on the crate.",
        "tooltip.nusmp.crate_key.used": "The key is used up.",
        "message.nusmp.crate.locked": "This crate is locked. It needs a %s.",
        "message.nusmp.crate.sealed": "The crates are sealed on this server.",
        "message.nusmp.crate.opened": "The %s Crate opens:",
        "message.nusmp.crate.empty": "  ...it is empty.",
        "message.nusmp.plush.collected": "Plushie collected: %s (%s/%s)",
        "subtitles.nusmp.plush_squeak": "Plushie squeaks",
        "subtitles.nusmp.crate_unlock": "Crate unlocks",
        "subtitles.nusmp.crate_reveal": "Crate shimmers",
        "subtitles.nusmp.crate_locked": "Crate thuds",
    })
    return d


def main(argv):
    text = json.dumps(build(), indent=2, ensure_ascii=False) + "\n"
    path = os.path.join(HERE, "items_lang.json")
    if "--check" in argv:
        ok = os.path.exists(path) and open(path, encoding="utf-8").read() == text
        print("up to date" if ok else "OUT OF DATE")
        sys.exit(0 if ok else 1)
    write_text(path, text)
    print("items_lang.json:", len(json.loads(text)), "keys")


if __name__ == "__main__":
    main(sys.argv[1:])
