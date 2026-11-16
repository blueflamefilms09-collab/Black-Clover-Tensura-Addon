# Dungeon-boss rework: the Floor Warden and the three creatures

Companion of `docs/dungeon_bosses_audit.md` (the survey and the ranking). Status: **written and previewed, not compiled and not run in a game** (the session had no Gradle; CI compiles it, and the
server smoke test key `dboss` builds the entities). Nothing is committed and the version is not bumped (the hand-over does both).

## 1. What was built, how to read it, what is not included

* **Floor Warden** (`FloorWardenEntity`), a new look and a new move set:
  * Look: a taller (3.4 m) horned warden with a thorn halo, gauntlets and a rune mantle, a lit chest keyhole with the Shadow Key hanging from a chain, three swinging seal tags, a bigger
    hammer. Made from the art-kit spec `tools/arc/char_floor_warden.py` (so the player-model kit, the in-game default, and the art-kit fallback both change).
  * Five moves (`DungeonReworkRules.WARDEN`, numbers in one place): **Hammer Slam** (a ring grows on the floor for 1.2 s, then 9 damage and a shove inside it), **Chain Lash** (violet chains tie up to
    three players at range for 1.1 s, then 5 damage, a drag toward it and a short slow), **Shadow Step** (a mark on the floor where it will land, 0.7 s later it is there and cleaves 6 damage in 3.2
    blocks), **Seal Pulse** (phase 2: three rings roll out to 16 blocks, 7 damage each, a jump clears them), **Key Toll** (phase 3, the ultimate: two lit sigils appear, 3.5 s later 22 damage to
    everyone in 30 blocks, a graze of 20 percent inside a sigil; it is winded for 3 s afterwards).
  * Phases at 50 percent (the bar turns pink, rings of light, a roar, cooldowns 15 percent shorter) and 25 percent (red bar, speed +12 percent, the Key Toll, cooldowns 30 percent shorter).
  * Soft enrage after 150 s of one fight: +20 percent damage, cooldowns 20 percent shorter; it resets after 10 s without a target.
  * Damage goes through `ZogratisDamage.hit` (the boss damage law, the 40 percent player hit cap of `BalanceLaw`), the slow through `BalanceLaw.beginControl` / `controlTicks`.
  * The boss DPS budget (6 percent of max health per second taken) is now wired into the Warden (`BudgetedBoss`, the three hooks) while the rework is on.
* **Direwolf**: Pounce (crouch ring, 0.7 s, then a leap, 5 damage) and Pack Howl (quickens up to 6 wolves for 5 s, every 30 s).
* **Basilisk**: Petrifying Gaze (a line to you for 1.5 s; it only hurts and slows if you are looking at it), Venom Spit (a poison bolt, needs line of sight), Tail Sweep (a ring around it).
* **Living Armor**: Shield Bash (a shove) and, below half health, Rally Rattle (Resistance for itself and up to 6 armours nearby, every 35 s).
* A creature move starts only against a **player** target (hunting prey stays the old bite) and never two at once; one second between moves.
* **Not included**: the Labyrinth Guardian and the Decennial bosses (the new move set is off for any subclass), the Slime, the Goliath, Charybdis Spawn, the Foundry mobs, the NPCs. New sound files
  were not made (see 5). New creature looks were not made (their behaviour was the gap).

## 2. NEW / REPLACED / KEPT (rule 5)

| part | |
|---|---|
| `dungeon/rework/*` (rules, strikes helper, `WardenBrain`, `BeastBrain`, self-test), the five Warden moves, the six creature moves, the phases, the enrage, the new messages | NEW |
| `dungeonBosses.*` settings in `BossRulesConfig` (appended section) | NEW |
| the Warden's art-kit spec, geo, animation, skins, glow maps, player-model extras and table row | REPLACED in place (the old spec is saved as `tools/arc/legacy/char_floor_warden_v1.py.txt`; the model ids stay, so the renderers, `RemodelTableTest` and saves are unaffected) |
| the Warden's old Ground Slam and Shadow Step, the half-health Living Armor summon, the loot (Shadow Key in code), the boss bar, the Guardian's whole fight, the old bite of the creatures | KEPT; the old chooser runs when `dungeonBosses.warden` is false |
| `FloorWardenEntity.hurt` / `setHealth` / `baseTick` / `readAdditionalSaveData` | extended; they pass straight through unless the rework is on |

## 3. Settings (SERVER, `nusmp-boss-rules-server.toml`, section `dungeonBosses`)

| key | default | effect |
|---|---|---|
| `rework` | true | master switch; false restores every dungeon boss and creature to the old fight (read every tick) |
| `warden` | true | the Warden's move set (needs `rework`) |
| `creatures` | true | the Direwolf, Basilisk and Living Armor moves (needs `rework`) |

The new look is data (a model replacement), not a setting; the old spec is kept in `tools/arc/legacy/`.

## 4. Cost, worst case (rule 11)

* Server, Warden, per tick: nothing but a few integer checks when it has no target. In a fight at most one `victims` query (8 players maximum, a bounding box and a sort of those few) per tick, only
  during a move; Seal Pulse runs it for 30 ticks per cast. Phase and enrage announcements: one query per event. **No entity is spawned** (the half-health two Living Armors are the old code).
* Server, creatures: nothing unless awake with a player target; a move runs 12 to 40 ticks with at most one bounded query per tick; the two pack calls do one entity query of at most 6 entities
  once per 30 s or more.
* Effects (`Fx.ring`, `Fx.send`, `Fx.stream`, theme `dungeon.seal`, through the facade's particle budgets, near players only): the worst moment is the Key Toll telegraph, about 4 rings and bursts
  every 5 ticks plus one 40-point ring every 10 ticks, so under 250 particles per second per Warden before the budget cuts them. Packets: the facade batches them; no new packet type.
* Sounds: one vanilla sound per step of a move (a few per second), played at a point (the game sends them to nearby players only).
* Damage worst case (`DungeonReworkRulesTest`): base damage per second of ONE player if every move were used as often as its cooldown allows: phase 1 3.2, phase 2 5.6, phase 3 7.5, enraged phase 3
  11.3 (a ceiling: the moves share one body); each hit still goes through the balance law, so one hit never takes more than 40 percent of a player's health. The test holds 12 as the ceiling and
  the self-test 14.
* Client: no new client code; the new Warden model has 3 928 vertices (the old one fewer, the Foundry characters are of this size too); the distance and LOD rules of the kit renderer apply.

## 5. Sounds

No new `SoundEvent` was registered (so no `.ogg` is needed). The moves recombine vanilla events (anvil, chain, bell, warden sonic charge and boom, ravager roar, wither spawn, wolf growl and howl,
guardian beam, spider hiss, shield block, iron golem blow) and the creatures' own family voices (`MobVoice`: aggro, attack, cast). This is the weakest part: a bespoke set (a bell toll for the Key
Toll, a chain rattle, a hammer ring) would need recorded files and is not done.

## 6. Previews (`docs/dungeon_bosses_previews/`)

Made by `python3 -B tools/dungeon_bosses/make_previews.py` (JDK 21, numpy, Pillow). Honest labels:

| file | what it is |
|---|---|
| `overview.png` | the Warden, Direwolf, Basilisk, Living Armor, Slime, and the Goliath and Charybdis art-kit fallbacks (**real `GeoDraw` renders of the art-kit models**), then the existing Blender renders of the Goliath, Minotaur and Lucifero (**Blender**), then the Warden's in-game look (`docs/previews/humanoids/floor_warden.png`, **software preview of the player-model kit**) |
| `floor_warden_sheet.png` | front, 3/4, side, back of the art-kit Warden, two clip strips (attack, cast), the move list. **Real model render**, but of the art-kit fallback; the in-game default is the player-model kit (same spec, see the overview) |
| `direwolf_sheet.png`, `basilisk_sheet.png`, `living_armor_sheet.png` | the same layout; the looks are unchanged, only the moves are new |
| `slime_sheet.png`, `goliath_sheet.png`, `charybdis_sheet.png` | the same layout for the creatures and bosses that were rated but not reworked (the Goliath sheet is the art-kit fallback; in game it is the Blender mesh) |
| `floor_warden_moves.png` | **a schematic, not a render**: the Warden's five telegraphs from above, drawn from the numbers of `DungeonReworkRules` |

Not available: a render of the particles. The offline VFX preview (`tools/vfx_preview`) draws the `vfx/client/layer` classes; the move telegraphs are server-sent `Fx` particle themes, which it
cannot show, so `floor_warden_moves.png` is a diagram. The pose strips are the models' own attack and cast clips, not the moves above.

## 7. Blender (rule 14)

**No Blender was available in this environment, so no model was made in Blender.** The Warden is built by the art-kit generator (`tools/arc`, box models and painted textures), like every character
the creatures share. A Blender mesh Warden (as the Goliath has) is the next step and needs a machine with Blender.

## 8. Tests and the smoke key

* `DungeonReworkRulesTest` (pure numbers), `DungeonBossSourceScanTest` (no client class, no static mutable state, no spawn, bounded queries, damage only through the law, the settings and the gates,
  the smoke list order, the lang keys, no new sound event).
* Self-test key `dboss` (`DungeonBossSelfTests`, registered in `BossHooks`), inserted in `.github/workflows/smoke.yml` directly before `scaling`; the tail of the list is unchanged.
* Lang fragment `tools/lang/dungeon_bosses.json` (four messages); `en_us.json` was not edited, run the lang merge.

## 9. What was not verified

* Nothing was compiled (no Gradle here): every API call was read and written carefully, but CI may find an error.
* Nothing was seen in a game: the feel of the telegraphs (timings, rings), the sounds and the Chain Lash drag are untested. The numbers are in `DungeonReworkRules` for tuning.
* `Fx` particle appearance of the `dungeon.seal` theme is the old one.
* A wandering Warden (the Dungeon Master's patrol) now fights with the new moves too; the patrol logic itself is untouched.
* The player-model skin of the new Warden was checked only in the software preview.
