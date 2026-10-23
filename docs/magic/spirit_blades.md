# Spirit blades: Spirit of Zephyr (sword) and Spirit of Boreas (bow)

Two creative-display items, the weapons of the Wind Spirit Dive as real things you can hold. They are the owner's answer "yes to #2" (docs/magic/spirit_dives_wind3.md, open question 2):
the Zephyr sword and the Boreas bow also become items. Prefix of the self-test: `magic.spirit_blades`. Assets: `spirit_of_zephyr`, `spirit_of_boreas`, leaf sprite `spirit_blades_leaf.png`.
Sources of the look: the owner's Wind pack (the long glowing sword with crossed rings at the hilt; the leaf bow with cross ornaments and an arrow of light), docs/magic/spirit_dives_wind.md and
spirit_dives_wind3.md. No wiki was fetched for this build (nothing here needs a canon fact the pack and the earlier docs do not already give).

## What was built

| | Spirit of Zephyr | Spirit of Boreas |
|---|---|---|
| kind | sword (netherite level: 7 damage, 1.6 swings a second, 2031 uses, fire resistant) | bow, a plain `BowItem` (vanilla's draw time, power, arrows, Infinity, 384 uses) |
| in hand | real 3D model, 2196 quads (1392 body + 804 glow), `gripxyz` on the wrapped grip | real 3D model, 1226 quads at rest (822 + 404 glow), plus the three draw stages `_pull1..3` (arrow of light on the nock, 1370 quads at full draw) |
| look | long mint-green flat blade with a bright bevel, a glowing fuller pair, a cross-guard whose quillons curve up into leaf points, three crossed glowing rings round the hilt, a wrapped green grip, a glowing gem pommel | two LEAF limbs (a leaf section: narrow at the grip, widest in the middle, a point at the tip) with a glowing vein and a cross ornament each, a slim wrapped riser with a ring and a gem, a thin string of light, an arrow of light |
| glow | additive glow skin, pulsing (0.18 to 0.45, `A8FFD2`) | same family (0.16 to 0.45) |
| effect in hand | a few drifting leaves (see below) | the same leaves |
| effect in play | wind-green sparkles when it hits (server, capped) | a short glowing trail behind its arrows (server, capped) |
| inventory icon | the old-style 16 x 16 sprite, rendered from the finished mesh | same |
| engravings (rule 6) | severance 1, barrier_piercing 1, holy_weapon 1 | magic_interference 1 |
| Better Combat | `bettercombat:sword` (+0.25 range) | ranged, excluded (like every bow) |
| class / grip | `longsword`, grip from the mesh (`gripxyz`) | `bow`, held like vanilla's bow (`bow_vanilla.py`, BowDisplayGuardTest) |

Obtaining them: **creative tab only**. No loot table, no recipe, no drop, no trade (`SpiritBladesAssetsTest` reads every data file to prove it). Engravings are applied once
(`WeaponEngravings`, flag `NUEngraved`) when the stack first sits in an inventory.

Not special magic: no keybind, no packet, no skill. The Wind Spirit Dive painter (builder "windfinal") draws its own meshes (`sdw3_meshes`, unchanged); the items use their own meshes in
`weapon_meshes/`, authored with the weapon kit.

## NEW / REPLACED / KEPT (rule 5)

- NEW: the two items, their meshes, skins, glow skins, icons, item models, class rows, grip rows, Better Combat row, engravings, lang fragment (`tools/lang/spirit_blades.json`),
  creative-tab entry, hit sparkles, arrow trail, hand leaves, self-test `magic.spirit_blades`, tests, previews, this doc.
- REPLACED: nothing (nothing like these items existed).
- KEPT: every existing weapon, the Wind Spirit Dive meshes (`sdw3_meshes/sword.txt`, `bow.txt`: not touched, not renamed), the Time Magic VFX. Shared files were only appended to
  (`WeaponEngravings`, `NUSMP`, `NUCreativeTab`, `WeaponRenderer`, both `weapon_classes.json`, the display-preset manifest, the Better Combat manifest, the grip golden).

## How it is built

`tools/gen_spirit_blades_models.py` (hooked as `python3 -B tools/gen_weapon_models.py spirit_blades`; deterministic) builds both meshes with the kit of `tools/relics/new197_*.py` (lofts, sweeps,
lathes, named parts, a painted 512 px skin, an additive glow skin, per-vertex smoothed normals), exactly like the Stormcaller Staff and the Genesis relics, then writes the class rows and the
display presets by text insertion (other rows byte for byte), and the leaf sprite. `tools/relics/bow_vanilla.py` then lays the bow on vanilla's diagonal and size.
`python3 tools/weapon_grip/grip_table.py --write` refreshed the grip golden (40 lines, only the new meshes).

The weapon renderer (`client/WeaponRenderer.java`) draws them like every other mesh weapon: body, glow pass, bow draw stages through `ItemStateVariants`; one appended line calls
`client/SpiritBladesFx.extras` after the glow pass.

## Budgets (rules 9 and 11), worst cases

Server (decided on the server, nothing trusts the client; no packet of the mod; ordinary vanilla particle packets to players near the spot only):

| piece | cap | worst case |
|---|---|---|
| sword hit sparkles | 6 particles per burst (4 dust + 2 end rod), one burst per attacker every 4 ticks, 4 bursts per server tick over everyone, the throttle map holds at most 256 attackers and forgets one on logout | 24 particles in a tick, 2 particle packets per burst, at most 8 packets in a tick |
| bow trail | 4 trailed arrows per owner, 32 over the server, a puff every 3 ticks for at most 40 ticks (14 puffs in an arrow's life), 2 particles per puff (1 dust + 1 end rod), 8 puffs per server tick | 16 particles and 16 packets in a tick (8 puffs x 2 calls); an arrow past a cap flies without a trail |
| trail upkeep | a list of at most 32 small records, walked once per tick (<= 32 steps), no world scan, no chunk load, no entity spawned; a removed or stopped arrow leaves the list; cleared when the server stops | O(32) per tick |
| engravings | once per stack (marker), the existing once-a-second inventory sweep | no new cost |

Client (drawing only):

| piece | cap | worst case |
|---|---|---|
| the model | 1392 + 804 quads (sword), 822 + 404 (bow at rest), 924 + 446 (bow at full draw) | like the Genesis weapons the mod already draws, fewer than the 971 x 2 glow quads of a Genesis weapon |
| the leaves | 6 leaves x 2 crossed planes x 2 faces = 24 quads (96 vertices), one extra additive buffer, skipped beyond 24 blocks from the camera, no allocation per frame (pure function of the time), `-Dnusmp.spiritBlades.leaves=off` switches them off | 24 quads per weapon per frame |

Pinned by `SpiritBladesRulesTest` (caps, throttle, leaf maths), `SpiritBladesSourceScanTest` (no client class in the common package, no world scan / spawn / chunk call, every particle call uses the
capped counts, the painter has no allocation and the far skip), `SpiritBladesAssetsTest` (files, rows, engravings, leaf sprite black under alpha, creative-only), and the in-game self-test
`magic.spirit_blades` (registration under the model ids, netherite-equal damage read from the item's attribute component, vanilla bow durability, engravings applied once and not twice, a real hit
by a stand-in throttled and forgotten on logout, the trail capped at 4 per owner and ended with its arrows).

## Previews (software render, `docs/previews/spirit_blades/`)

Made with the project's headless rasteriser (`tools/item_preview/preview_weapons.py` core, driven by `tools/item_preview/preview_spirit_blades.py`): no game, no GPU.
`turntable_zephyr.png`, `turntable_boreas.png` (eight yaw steps), `bow_draw_stages.png` (rest, 1/3, 2/3, full draw), `inventory_icons.png` (the 16 x 16 sprites at 8x),
`in_hand_grip_sheet.png` (where the hand holds each one: third person right / left, first person, from `tools/weapon_grip/preview_grip.py`).

## Honest caveats (not seen in a game)

- Everything above the Java level was checked by software render and tests only. The glow pulse, the additive blend of the leaves and the arrow trail colours were NOT seen in a real game. The
  preview rasteriser shows the glow skin without bloom, so the in-game blades will glow more than the previews.
- The sword's three rings give the hilt a busy outline in first person (they are part of the owner's reference); if they get in the way the rings are one part each (`ring_a`, `ring_b`, `ring_c`)
  and can be thinned or dropped in `gen_spirit_blades_models.py`.
- The Zephyr mesh is 1.9 blocks long (the rings and the gem add to the 1.62 class length), so the display-preset manifest gave it a first-person shrink (`preset`).
- The hit sparkles and the trail are plain vanilla particles (mint dust and end rod), not the mod's own particle types: honest and cheap, but not as rich as the spells' effects.
- Damage uses the netherite numbers; `WeaponGrades.damageOr` (the mod's balance table) is not consulted because the owner asked for netherite level.
- The bow's trail also starts for an arrow shot by a dispenser-like source only if it goes through `BowItem.customArrow` (it does not); only the player's draw of the bow trails.
