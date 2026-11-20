# The Lords of the Mortal Sins: remake of the Berserk weapons

Owner request: remake the Black Axe of Sloth, the Black Gunblade of Envy, the Black Spear of Vitality (in the mod: **Vanity**, see the naming note) and the Send Forth Holy Sword (Seifort)
to the standard of the Black Sword of Greed: far better looks, fully integrated systems, custom engravings, effects of their own, a grade above Unique. Also: rework Greed's abilities and all
Berserk weapon abilities with VFX at the quality of the Time Magic, explain every negative effect to the player when they take the weapon, and give the Half-Starved state (and the other weapons) a
mastery bar in the Tensura status menu.

## Sources

The English and Japanese wikis could **not be fetched** in this session (every request failed with ENOTFOUND). Canon (who wields what, what the weapon looks like, what it does) is written from
memory of the anime, so treat the details as "inspired by" until the owner has checked them against the wiki. Nothing in the numbers below is canon; those are game balance.

## NEW / REPLACED / KEPT

| | What |
|---|---|
| NEW | Three forms per weapon, 8 new item ids (`*_titan`, `*_chains`, `*_rifle`, `*_coil`, `*_mirror`, `*_peacock`, `seifort_holy_sword_radiant`, `seifort_holy_sword_sent`), each a real Blender mesh with its own skin and glow skin, icon and item model |
| NEW | One-of-one rule (attachment `lords` + SavedData `nusmp_lords`), burden (the explained drawback), mastery tracks, a Lord's decree (ultimate) per weapon, form switch (R and skill mode), `lords_switch` / `lords_hud` packets |
| NEW | Half-Starved Mastery for Greed (secondary bar in the Tensura status menu), with the same bar for the other four |
| NEW | 5 custom engravings (enchantments in the `#tensura:engraving` tag): Weight of Sloth, Hollow of Envy, Cracked Mirror, Penance of Seifort, Hunger of the Blade |
| NEW | VFX of their own (`VfxShape.LORD_*`, `LordLayer`, `tools/vfx_preview/scenes.d/lords.json`), one per art, decree, state and level-up |
| NEW | Better Combat attribute files for the 8 new forms, weapon classes, lang (`tools/lang/lords.json`), tests |
| REPLACED | The mesh, skin and glow of the four old ids (form 0 of each weapon) by Blender meshes. The old ones are **kept** under `weapon_meshes/classic` and `textures/entity/weapon/classic` and come back with the client setting `lords.classicModels` |
| REPLACED | Greed's half-starvation: it no longer touches the food bar (see below). The old behaviour returns with the server setting `lords.hungerOfTheBlade = false` |
| REPLACED | Grade: Unique became GOD for the four Lords and GENESIS for Greed (display rank in `rankings.json`; damage is not regraded) |
| KEPT | Old item ids, old 16x16 sprites (the new forms get new simple sprites), old techniques and their cooldowns (the four old skill modes keep their order; decree and form are appended as modes 5 and 6), old VFX (`lords.classicVfx`), the Chakrams (no mastery: their sin is unnamed in canon) |

## The four weapons

| Weapon | Forms (level that opens them) | Arts | Decree (level 8) | Drawback (named) | Mastery |
|---|---|---|---|---|---|
| Black Axe of Sloth (violet iron, half-closed eye) | Black Axe, Titan's Cleaver (3), Chained Anchor (6) | Dead Weight, Titan's Verdict, Drowsing Chains | Abyssal Stillness | Weight of Sloth: you walk and dig slower | Mastery of Stillness |
| Black Gunblade of Envy (green, revolver cylinder, barrel on the spine) | Gunblade, Coveted Rifle (3), Serpent Coil (6) | Green-Eyed Shot, Jealous Volley, Coveting Coil | Covet All | Hollow of Envy: you lose maximum health | Mastery of Covetousness |
| Black Spear of Vanity (magenta, mirror, peacock fan of eyes, cracked pane) | Spear, Mirror Glaive (3), Peacock Halberd (6) | Vain Thrust, Reflected Gaze, Fan of Vanity | Hall of Mirrors | Cracked Mirror: you glow, visible through walls | Mastery of Reflection |
| Seifort Holy Sword (ivory and gold, feather guard, halo) | Holy Sword, Radiant (3), Sent Forth (6) | Grand Cross, Pillar of Judgement, Send Forth | Holy Judgement | Penance: healing you receive is halved | Mastery of Devotion |
| Black Sword Greed (existing) | its own forms | Secret Technique | Lord of Gluttonous Greed | Hunger of the Blade | Half-Starved Mastery |

Naming note: the owner wrote "Black Spear of Vitality". The mod's existing item is `black_spear_of_vanity` (Lord of Empty Vanity), so the remake keeps that id and name. If "Vitality" was
meant as a different weapon, it is a decision for the owner (a rename is only a lang change; the ids stay).

## Every drawback is explained

When a player first binds a Lord weapon, a warning is sent in chat (`LordText.warn`): what it takes, what it does, how it recovers, how to handle it and what the mastery changes. The same lines are in
the item tooltip and the skill description, and the status menu shows the mastery. All lines are lang keys (`lords.drawback.<sin>.1..4`, Greed 1..6) and `LordFilesTest` fails if a line is missing, thin
(under 60 characters), does not say "never touch" (the food bar is never touched), how it recovers, or what the mastery does.

Numbers (all in `LordRules`, pure, unit tested): the burden grows with each art (12 / 18 / 22), the decree (45) and each plain hit (1); Strained from 50, Overloaded at 100 for 6 s (the weapon
refuses its arts); it drains 1.5 per second (3x faster when the weapon is stowed), a kill lifts 8. Mastery experience is 50 n^2 for level n, maximum level 10; each level gives 4% less burden and
5% faster recovery.

## Greed: old and new

- Old: the half-starved state doubled the hunger and used the starvation routine of the blade.
- New (`lords.hungerOfTheBlade = true`, the default): Hunger of the Blade. Satiation below 20 is Ravenous: the blade drinks 1.5% of your current magicule per second, you receive half of any
  healing and 70% of the devoured bonuses work; at 0 it is Dormant. It never hurts you and never touches the food bar. Half-Starvation (a mode of the skill) is chosen by the player: x2 devouring and
  x1.5 stats (plus 1% per level), but 2.5x the hunger (minus 0.1 per level). Time spent half-starved fills the **Half-Starved Mastery** bar (shown in the Tensura status menu), whose levels
  slow the hunger, add stats and return 4% of the healing the blade takes.

## Settings

Server (`nusmp-lords-server.toml`): `lordsEnabled` (false restores the old soul-bound-by-carrier items), `burden`, `inactivityRelease` (14 days), `hungerOfTheBlade`, `themedVfx`, `damagePercent`,
`burdenPercent`, `masteryPercent`. Client (`nusmp-lords-vfx-client.toml`): `classicVfx` (old particles instead of the new effects), `classicModels` (old meshes), `statusMasteryBar` (the bar in the status menu).

## Worst-case cost (rule 11)

- Server: one tick handler per player online, once per second per player (staggered); a Lord art affects at most `MAX_TARGETS` entities (the existing cap); no world scan, no chunk load; per-player maps
  are cleaned on logout and death (`LordService.forget`). One SavedData with at most one owner per weapon (5 entries).
- Network: the effect packet is the existing shared one, sent to players near the effect only, one per art / decree and one per second per state; the HUD packet is deduplicated per player (only when
  level, percent, meter or flags change); the form switch is one C2S packet validated for sender, cooldown and ownership.
- Client: each effect is one `LordLayer` instance with a quad budget and a distance cull, drawn through the existing perf budgets; no per-frame allocation. Meshes: 2800 quad lines at most (same as Greed).

## Models

All 12 meshes were made in **Blender 5.3** (headless `bpy`, Cycles bake of the skin and the glow skin) through `tools/blender/lord_weapons.py` on top of `bl_lib.py`; none in Blockbench, none faked from
code boxes. Quad lines per mesh (budget 2800): axe 969, titan 1501, chains 1595; gunblade 1040, rifle 822, coil 1226; spear 825, mirror 1126, peacock 1313; Seifort 774, radiant 1292, sent 743.
The 8 new 16x16 icons are simple procedural sprites (not Blender). The item model json of the four old ids was regenerated for the new mesh (the display transform follows the new place line);
with `lords.classicModels` the old mesh is drawn with those transforms, so it may sit slightly off in the hand.

## Previews

`docs/berserk_weapons_previews/` (models: `models/`, effects: `vfx/`). The effect previews are made by `tools/vfx_preview/preview.py` with `scenes.d/lords.json`.

## Not seen in a game

Nothing was run in Minecraft in this session. The models, effects and the status bar were checked through the Blender previews, the VFX preview tool and the pure/source tests only. The effects
reuse the existing white-mask particle textures. CI (build and server smoke) was not run: the new smoke prefixes are `lords.rules`, `lords.world`, `lords.burden` (to be added to the smoke list by the
coordinator). Display grade only: the damage of the weapons is not regraded. The custom engravings carry no effect of their own (the mechanics live in the Lord service).
