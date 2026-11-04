#!/usr/bin/env python3
"""Appends the English lang keys of the 24 rank Mana Cores (3.51.0) to assets/nusmp/lang/en_us.json (textual append, idempotent).
Keys: item.nusmp.core_<rank>_<variant>, manacore.core_<rank>_<variant>.{passive,a1,a2}, manacore.tooltip.*, manacore.variant.*, manacore.msg.*.
Run from the repo root: python tools/manacore/gen_lang.py"""
import json, pathlib, sys

LANG = pathlib.Path("src/main/resources/assets/nusmp/lang/en_us.json")

C = {  # id: (name, passive, a1, a2)   a1/a2 end with the cost placeholder
 "core_e_standard": ("Crude Mana Pebble (E-Class Core)", "Mana Leak: a trickle of regeneration, but a little stored MP leaks away. Fragile Resonance: +5% attack speed.",
   "[Press] Core Spark: a burst of raw mana: 10 s of haste and jump boost. Costs %s.", "[Sneak + Press] Mana Shard Toss: a crystal shard in a line: minor damage and a short slow. Costs %s."),
 "core_e_beast": ("Ridge-Wolf Fang Core (E-Class Beast Core)", "Beast Instinct: night vision and +1 attack damage. Predator's Hunger: a kill heals 3% of your health.",
   "[Press] Wolf Howl: a shockwave that knocks nearby foes back and confuses them. Costs %s.", "[Sneak + Press] Savage Lunge: a short dash that strikes what it meets. Costs %s."),
 "core_e_ancient": ("Relic-Scrap Core (E-Class Ancient Core)", "Dormant Circuit: a weak projectile that hits you is turned into a trickle of MP.",
   "[Press] Overclock Pulse: 5 s of fast casting and mining, at the price of 2 hearts (not usable on low health). Costs %s.", "[Sneak + Press] Temporal Echo: step back to where you stood 1 s ago. Costs %s."),
 "core_d_standard": ("Refined Sphere (D-Class Core)", "Stable Channeling: this core's actives cost 5% less. Aura Shield: the first 5 points of damage every 10 s are absorbed.",
   "[Press] Mana Burst: a wave of mana that pushes foes back and wipes small projectiles. Costs %s.", "[Sneak + Press] Elemental Infusion: strength, and your hits set foes on fire for 8 s. Costs %s."),
 "core_d_beast": ("Crested Boar Bile Core (D-Class Beast Core)", "Toxic Resilience: immune to poison.",
   "[Press] Venom Spit: corrosive bile poisons the foes ahead for 6 s. Costs %s.", "[Sneak + Press] Hardened Hide: rocky skin: resistance for 8 s. Costs %s."),
 "core_d_ancient": ("Asura-Touched D-Core (D-Class Ancient Core)", "Minor Spatial Anchor: strong knockback resistance, you are hard to displace.",
   "[Press] Spatial Ripple: blink 4 blocks forward; foes along the path take damage. Costs %s.", "[Sneak + Press] Inertial Lock: a foe stops dead for 1.5 s. Costs %s."),
 "core_c_standard": ("Luminous Crystal Orb (C-Class Core)", "Mana Resonance: this core's area effects are 10% wider. Radiant Aura: night vision.",
   "[Press] Luminous Beam: a piercing beam of light through up to 3 foes. Costs %s.", "[Sneak + Press] C-Tier Barrier: for 4 s arrows and fireballs are stopped, and you gain absorption. Costs %s."),
 "core_c_beast": ("Wind-Skimmer Core (C-Class Beast Core)", "Wind Rider: no fall damage and +15% movement speed.",
   "[Press] Air Cutter: a razor crescent of wind that tears through foes. Costs %s.", "[Sneak + Press] Gale Burst: a gust that launches you high. Costs %s."),
 "core_c_ancient": ("Runebound C-Core (C-Class Ancient Core)", "Spell Matrix: every third active of this core is free and nearly instant.",
   "[Press] Rune Trapping: lay an invisible rune (3 at most); a foe that steps on it is held for 3 s. Costs %s.", "[Sneak + Press] Kinetic Redirection: catch the projectiles near you and throw them back at your foes. Costs %s."),
 "core_b_standard": ("Plasma-Charged Sphere (B-Class Core)", "Lightning Reflexes: 10% chance to dodge a melee hit. Overload Conduit: an active within 2 s of the last deals 15% more.",
   "[Press] Chain Lightning Arc: a bolt that leaps between up to 5 foes and stuns them briefly. Costs %s.", "[Sneak + Press] Static Field: for 6 s a pulsing field shocks foes near you and what strikes you. Costs %s."),
 "core_b_beast": ("Flame-Serpent Core (B-Class Beast Core)", "Magma Blood: immune to fire and lava; the heat feeds a little MP.",
   "[Press] Inferno Eruption: fire erupts in a circle around you. Costs %s.", "[Sneak + Press] Serpent Dash: a dash wrapped in dragon flame that burns what it crosses. Costs %s."),
 "core_b_ancient": ("Singularity B-Core (B-Class Ancient Core)", "Gravity Well: nearby dropped items are pulled to you.",
   "[Press] Micro-Singularity: a gravity point where you look drags foes toward it. Costs %s.", "[Sneak + Press] Mass Inversion: a foe is flung straight up. Costs %s."),
 "core_a_standard": ("High-Density Plasma Core (A-Class Core)", "Mana Core Condensation: fast MP regeneration. Plasma Armor: melee attackers are zapped.",
   "[Press] Plasma Nova: a 10-block blast of plasma. Costs %s.", "[Sneak + Press] Lightning Flash Step: a lightning teleport up to 15 blocks that shocks the path. Costs %s."),
 "core_a_beast": ("Shadow Panther Core + Will (A-Class Beast Core)", "Acquire Phase (Shadow Stride): night vision and speed while carried; the Acquire form itself is the Shadow Panther Will of the Beast Will system.",
   "[Press] Apex Roar: foes within 20 blocks are stunned and flee. Costs %s.", "[Sneak + Press] Integrate Phase (Panther Avatar): offers the Shadow Panther Will, or starts / ends its Integrate form. Paid by the Beast Will (%s)."),
 "core_a_ancient": ("Dimension-Tear A-Core (A-Class Ancient Core)", "Spatial Shifting: every 15 s the next attack passes through you.",
   "[Press] Rift Slash: a spatial cut through everything in front of you. Costs %s.", "[Sneak + Press] Temporal Stutter Field: foes around you are slowed to a crawl for 6 s. Costs %s."),
 "core_aa_standard": ("Prismatic Faceted Core (AA-Class Core)", "Prismatic Refraction: magic damage against you is halved.",
   "[Press] Prismatic Beam Matrix: five beams that seek the nearest foes. Costs %s.", "[Sneak + Press] Crystalline Prison: a foe is locked in crystal and stands still. Costs %s."),
 "core_aa_beast": ("Thunder-Fang Chimera Core + Will (AA-Class Beast Core)", "Acquire Phase (Chimera Grace): half lightning damage and shocking claws against melee attackers; the form itself is the Thunder-Fang Chimera Will.",
   "[Press] Multi-Head Strike: phantom heads strike everything around you. Costs %s.", "[Sneak + Press] Integrate Phase (Chimera Transformation): offers the Thunder-Fang Chimera Will, or starts / ends its Integrate form. Paid by the Beast Will (%s)."),
 "core_aa_ancient": ("Void-Weaver AA-Core (AA-Class Ancient Core)", "Void Step: your recent trail of spatial distortions hurts foes that step on it.",
   "[Press] Void Collapse: space collapses where you look and drags foes into it. Costs %s.", "[Sneak + Press] Chrono-Lock: a foe is held outside time for 4 s. Costs %s."),
 "core_s_standard": ("Runed Geometric Star Sphere (S-Class Core)", "Divine Geometry: orbiting star geometry stops a projectile every 2 s. Mana Sovereign: this core's cooldowns are 30% shorter.",
   "[Press] Starburst Protocol: runes burst into a pillar of light across a wide zone. Costs %s.", "[Sneak + Press] Absolute Domain: a 15-block zone where foes crawl for 8 s. Costs %s."),
 "core_s_beast": ("Elderwood Guardian Core + Supreme Will (S-Class Beast Core)", "Acquire Phase (Guardian Roots): natural regeneration while carried; the form itself is the Elderwood Guardian Will.",
   "[Press] Nature's Wrath Thorns: thorns and roots burst across 20 blocks, piercing and holding foes. Costs %s.", "[Sneak + Press] Integrate Phase (Elderwood Avatar): offers the Elderwood Guardian Will, or starts / ends its Integrate form. Paid by the Beast Will (%s)."),
 "core_s_ancient": ("Celestial Chrono-Core (S-Class Ancient Core)", "Temporal Safeguard: lethal damage is undone once every 150 s: you return to where you stood 3 s ago at half health. Immune to time stops.",
   "[Press] Causality Rewrite: strips the buffs and absorption a foe and its neighbours gained. Costs %s.", "[Sneak + Press] World Stills (Mini-World): everything around you stops for 4 s while you move freely. Costs %s."),
 "core_ss_standard": ("Cosmic Constellation Sphere (SS-Class Core)", "Infinite Horizon: while in combat your MP refills fast.",
   "[Press] Constellation Judgment: after a moment starlight grids lock and detonate over 18 blocks. Costs %s.", "[Sneak + Press] Stellar Singularity: after a moment a mini-supernova burns everything within 30 blocks. Costs %s."),
 "core_ss_beast": ("Nihil Dragon Core + Mythic Will (SS-Class Beast Core)", "World-Eater's Maw: a kill heals a quarter of your health. The Acquire form is the Dragon Will of the Beast Will system.",
   "[Press] Cataclysmic Dragon Roar: a shockwave that flings foes back and weakens them. Costs %s.", "[Sneak + Press] Integrate Phase (Nihil Dragon Ascendancy): offers the Dragon Will, or starts / ends its Integrate form. Paid by the Beast Will (%s)."),
 "core_ss_ancient": ("World Line Core / Dragon Fragment (SS-Class Ancient Core)", "Timeline Mastery: faster movement and attacks; immune to every time-manipulation effect.",
   "[Press] World of Still Seconds (SS-Tier): the world around you stops for 10 s while you move freely. Costs %s.", "[Sneak + Press] Last Second of Creation: a 30 s countdown: time races, gravity dies, everything floats, then the zone collapses in a final explosion. Cooldown 600 s. Costs %s."),
}
MSG = {
 "manacore.tooltip.rank": "%s-Class %s", "manacore.variant.standard": "Core", "manacore.variant.beast": "Beast Core", "manacore.variant.ancient": "Ancient Core",
 "manacore.tooltip.use": "Right-click: first active. Sneak + right-click: second active. The passive works while held or worn.",
 "manacore.tooltip.will": "Works with the Beast Will system.",
 "manacore.msg.too_weak": "A rank %s core needs %s Existence Points.", "manacore.msg.cooldown": "Ready in %s s.", "manacore.msg.no_mp": "Not enough magicules (%s).",
 "manacore.msg.control_cd": "Hard control is cooling down (%s s).", "manacore.msg.low_health": "Too weak to overclock.", "manacore.msg.no_target": "Nothing to affect.",
 "manacore.msg.blocked": "Something is in the way.", "manacore.msg.no_history": "No place to return to yet.", "manacore.msg.no_projectile": "No projectile near you.",
 "manacore.msg.busy": "Already under way.", "manacore.msg.safeguard": "Time folds back: the blow never landed.", "manacore.msg.last_second": "The final second has been declared.",
 "manacore.msg.countdown": "The future collapses in %s s.", "manacore.msg.will_off": "Beast Wills are switched off on this server.", "manacore.msg.will_missing": "That Beast Will is not loaded.",
 "manacore.msg.will_swap": "Another Beast Will is active: swap to this one first.", "manacore.msg.dropped": "A core falls: %s.",
}

def main():
    entries = {}
    for cid, (name, passive, a1, a2) in C.items():
        entries["item.nusmp." + cid] = name
        entries["manacore." + cid + ".passive"] = "Passive - " + passive
        entries["manacore." + cid + ".a1"] = a1
        entries["manacore." + cid + ".a2"] = a2
    entries.update(MSG)
    text = LANG.read_text(encoding="utf-8")
    first = next(iter(entries))
    if json.dumps(first) + ":" in text:
        print("already present"); return
    body = ",\n".join("  %s: %s" % (json.dumps(k), json.dumps(v, ensure_ascii=False)) for k, v in entries.items())
    end = text.rstrip().rfind("}")
    new = text[:end].rstrip() + ",\n" + body + "\n}\n"
    json.loads(new)  # must stay valid
    LANG.write_text(new, encoding="utf-8")
    print("added", len(entries))

if __name__ == "__main__":
    sys.exit(main())
