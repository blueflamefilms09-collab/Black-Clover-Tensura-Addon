# Mana Method audit (builder `manamethod`, CLAUDE.md rule 5: new or replacement, checked BEFORE building)

Spec: the owner's master prompt, section 1 "Mana Method (Magic Formula)". The prompt names generic classes (`RuneEntity`, `RuneGeneratorUtil`, PlayerAnimator);
this audit maps each bullet to what the mod really has. Result key: **exists** (nothing to build), **partly** (extended next to the old code), **missing** (new).
Old behaviour is never removed (rule 2): every new thing sits next to the old one and the whole system can be switched off with a SERVER setting
(`nusmp-mana-method-server.toml`, `manaMethod.enabled`).

## Per spec bullet

| # | Spec bullet | State | What exists (file) | Decision |
|---|---|---|---|---|
| 1 | Rune modifiers `Homing` / `Growth` carried in the spell's data | **missing** | `book/SpellRuntime.Bolt` has a `homing` target (blend 0.25 per tick, final radius) and no growth; `firecast/FireCasting.homing` is Magna-only. Nothing is stored as spell data. | NEW `mana/method/RuneMods`, `RuneSpec` (+ NBT form), `RuneRules.steer` / `growthRadius` / `growthDamage`. Homing is a bounded turn rate toward ONE target picked at launch; Growth is capped. |
| 2 | True Magic: bypasses magic resistance | **missing** | `mana/zone/TrueMagicRules` has the numbers of the older True Magic page (devil x1.6, fuel share, power); `soul/Paladins.resistance` shows how Tensura's `MAGIC_RESISTANCE` is read (fraction, or percent above 1). No spell cancels it. | NEW `RuneRules.resistanceBypass` (cancels the target's resistance up to x3, the balance law's caps still apply), used by the True Magic Mana Method page. |
| 3 | True Magic: places real blocks on impact (True Water = Water Source blocks) | **missing** (the old page says "No world edits") | `mana/zone/TrueMagic` (visual only, kept). `book/SpellRuntime.tempBlock` (one block, no cap, lost on a restart). Protected-area convention: `Level.isLoaded` + `mayInteract` + not in the Labyrinth / a guarded world (`relics/wave/Wave18Camp.free`, `dungeon/DungeonGuard.on`). | NEW `mana/method/TrueBlockRules` (pure) + `TrueBlocks` (ledger: per cast 7, per caster 21, global 160, expiry, removed on stop). Fire, water, stone, moss; wind and lightning place nothing. |
| 4 | Casting spawns a rune construct just in front of the player | **missing** | `TrueMagic.cast` draws `VfxShape.MAGIC_CIRCLE` in the air above the AIM POINT (not in front of the caster) and strikes the aim point; `item/MagicGear.placeRune` (Rune Stone trap) is a `SpellRuntime.zone` with an uncapped entity query. No entity type for a construct (`PaintedConstructEntity` is a tamed minion, far too heavy). | NEW `mana/method/RuneManager`: the construct is a tiny server-side record (NO entity type: nothing to sync, save or render), capped 2 per caster and 24 globally, auto-despawn. This is the cheapest existing mechanism (`SpellRuntime`) with the caps it lacks. |
| 5 | Brief charge delay while the runes form | **partly** | `TrueMagic` waits `DELAY_TICKS` = 24 through `SpellRuntime.later`. | The new construct charges 18 ticks (26 for True Magic), the rune is drawn for exactly that time. |
| 6 | The projectile / effect is fired FROM the rune's location, modifiers in the spell's NBT | **missing** | `TrueMagic.strike` hits at the aim point, from nowhere. | NEW bolt launched from the construct centre; the spec rides in a `CompoundTag` (`RuneSpecNbt`) the bolt reads at launch and at impact. |
| 7 | `RuneGeneratorUtil`: radius, complexity (rings / symbols), particle type | **missing** | `vfx/client/layer/*` draw geometry layers (`VfxShape.MAGIC_CIRCLE`), not particle arrays. Custom rune particles exist: `particle_catalog.json` family `rune` (`rune_flame`, `rune_wave`, `rune_leaf`, `rune_bolt`, `rune_sol`, `rune_luna`, `rune_eye`, `rune_key`) and `mana_mote`. | NEW `mana/method/RuneGeneratorUtil` (pure; JOML), uses those particles. |
| 8 | sin / cos plot in a flat 2D plane | **missing** | - | In `RuneGeneratorUtil.geometry` (rings, glyph marks, star polygons), cached per (rings, symbols). |
| 9 | Quaternion rotation so the plane faces the player's look vector | **missing** | `vfx/client/VfxPose` has JOML helpers for layers only. | `RuneGeneratorUtil.faceQuaternion` (yaw then pitch, no roll: glyphs stay upright; normal = minus the look vector, i.e. toward the caster). |
| 10 | Render the arrays client-side without bogging down the server | **missing** | Budgets exist: `perf/PerfBudgets.clientParticles`, `PerfSettings.vfxQuality`, `particle/fx/FxClientLod`, custom particles via `FxTypes` / `TintedParticleOptions` (pattern: `vfx/client/RecombAnimeFx`). | NEW client-only holder `client/manamethod/RuneClient` draws from ONE small clientbound packet per cast; budgeted, distance culled, LOD, cached geometry. The server sends the packet only, never the particles. |
| 11 | Hooked into the casting flow, no keybinds | **partly** | `book/GrimoireBook.pages()` appends universal pages (Mana Zone, Omni Volley, Pressure Space, True Magic, spirit pages) and gates them with `isZoneGated` -> `ManaZoneManager.gateOpenFor` (late game). | NEW pages appended after the spirit pages: Flame / Water / Wind / Earth Rune and True Magic Rune, same gate, no key. `book/pages15` does not fit (it is per family and mastery-gated), so the plain `BookPage` API is used, like `ManaZoneActions`. |
| 12 | Existing "Mana Method" content | **exists** | Heart kingdom perk "Mana Method flows easily" (-5 % cost, `mana/Heritage`, `blackclover/GrimoireCover`, `item/RobeItem`); the Mana Method Rune Stone item (`item/MagicGear.storeRune` / `placeRune`, `RelicItem.Kind.RUNE_STONE`); the older True Magic page (`mana/zone/TrueMagic`, `TrueMagicRules`). | KEPT unchanged. The Heart perk already lowers the cost of the new pages (it is applied in `getMagiculeCost`). |

## NEW / REPLACED / KEPT

* **NEW**: everything in `mana/method/*`, `client/manamethod/RuneClient`, the packet `nusmp:mana_method_rune`, config `nusmp-mana-method-server.toml`, five pages, self-tests `magic.mana_method*`, lang fragment `tools/lang/mana_method.json`, doc `docs/magic/mana_method.md`.
* **REPLACED**: nothing. The Mana Method is new next to the older True Magic page.
* **KEPT**: `TrueMagic` / `TrueMagicRules` (the page `true_magic_rune_array` and its tests), the Rune Stone, the Heart perk, `SpellRuntime`, every old page and mode number.

## Shared files touched (append only)

`book/GrimoireBook` (pages appended last; `isZoneGated` also asks the new ids), `NUSMP` (one boot line), `docs/server_safe_allow.txt` (one packet file), and two
pinned lists that assumed the spirit pages were the last of a book (`spiritcontrol/SpiritControlSelfTests`, `mana/zone/ManaZoneSelfTests`, plus the scan test
of the spirit pages): they now allow the Mana Method pages after them.
