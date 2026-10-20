# Memory Magic: the 1.93.0 rebuild

Memory Magic (`MagicType.MEMORY`, book `nusmp:book_memory`, colour 0xFFBCA8FF lavender) used to be the generic wiki attribute book. It is now `book/MemoryBook` with its own armour,
summon models, effect layer and spells.

## 1. Canon and what could not be checked
The Black Clover wiki could not be reached from the build environment and the repo has no wiki page for Memory Magic. No anime user of Memory Magic is named in the repo and none is
claimed here. The magic is built on the one idea its name gives: a moment kept and shown again. The spells are original (replays, echoes, forgetting, stepping back to a kept moment,
an archive that replays what hurt a foe).

## 2. NEW / REPLACED / KEPT
| Part | Status | What |
|---|---|---|
| Book class | REPLACED | `WikiBooks.attribute(MEMORY)` returns `MemoryBook` (`book/SongRebuild` dispatch) |
| Pages `attribute_bolt` `attribute_cut` `attribute_field` `attribute_domain` `attribute_guard` | REPLACED (same ids, order, mastery bits) | Recollection Bolt, Replay Line, Forgetting Field, Flashback Domain, Memory Veil |
| Pages `memory_echo` `memory_forget` `memory_recall` `memory_archive` | NEW (appended, modes 5 to 8) | 9 family pages of the 24 allowed |
| Armour `Aura.MEMORY` (`client/aura/MemoryAura`) | NEW | Reel Vestments, 14 pieces |
| Models `memory_echo`, `memory_orb` | NEW | |
| VFX `MEMORY_FX1/2/3` | NEW (appended to `VfxShape`) | |

## 3. The temporary armour: the Reel Vestments (`memory_armour`)
Every successful cast puts it on for 5 seconds; Memory Veil keeps it for 9 s. Same forge-in and shatter-out as the Djinn full equip. Lavender white over film black, paper, gold trim
and mirror glass.

| Piece | Anchor | Delay | Look | What |
|---|---|---|---|---|
| `crown` | head | 0 | solid | a film-strip band, a camera lens for a third eye, two photo-frame fins (hidden in first person) |
| `shards` | head | 12 | ghost | three glass shards turning over the head |
| `book` | body | 2 | solid | a cuirass of pages bound in gold with a lens-locket |
| `reel` | body | 5 | solid | a film reel standing behind the back, turning |
| `film` | body | 8 | solid | a strip of film hanging from the reel, swaying |
| `mirrors` | body | 11 | ghost | three hand-mirror shards orbiting the waist |
| `pauldron_r/l` | arms | 3 | solid | tilted photo frames with glass spikes on gold caps |
| `vambrace_r/l` | arms | 5 | solid | film-reel bands with frame rims |
| `gauntlet_r/l` | arms | 7 | solid | frame gloves with glass fingertips |
| `greave_r/l` | legs | 9, 10 | solid | stacked pages with a film strip at the ankle |

## 4. Summon models (`client/prop/MemoryRebuildPropPainter`)
| Model | Prop | What | Clips |
|---|---|---|---|
| `memory_echo` (28 cubes) | `MEMORY_1` | an afterimage of a person in a film frame with lit sprocket marks; translucent, flickering | idle, strike |
| `memory_orb` (13 cubes) | `MEMORY_2` | a photograph in a frame with a glowing picture and three small frames orbiting it: the body of the Recollection Bolt | idle, strike |

## 5. Spells
| Id | Name | Tier | What | Notes |
|---|---|---|---|---|
| `attribute_bolt` | Recollection Bolt | starter | orb, raw 7 in a burst of 1.5, and 16 ticks later the same blow again (raw 4.2) | |
| `attribute_cut` | Replay Line | mid | line of 14 blocks, raw 5, replayed 10 ticks later (raw 5) | |
| `attribute_field` | Forgetting Field | zone | radius 5 for 6 s: per second raw 2.5, Blindness 1.5 s (1 s on bosses), mobs forget their target; allies shake off a harmful effect and heal 1.5 percent | |
| `attribute_domain` | Flashback Domain | signature | radius 8: raw 10 and Blindness 3 s, replayed 20 ticks later (raw 8); cooldown 900 | |
| `attribute_guard` | Memory Veil | mid | Night Vision, melee +4, thorns, 9 s; the next hit within 8 s is forgotten (cancelled, a boss's hit cut by 70 percent) | hook `AttributeEvents.incoming` |
| `memory_echo` | Echo | mid | an afterimage for 15 s: blinks beside the nearest foe every 1.5 s, strikes raw 5, replays the blow 8 ticks later raw 3 | cap 1 |
| `memory_forget` | Forget | mid | the foe in sight: a mob's target is cleared every half second for 6 s and it is slowed; a player gets Weakness and Blindness | shared hard-control lock `GrimoireBook.control`, time by `BalanceLaw.controlTicks` |
| `memory_recall` | Recall | mid | back to your place and health of 3 to 7 s ago, harmful effects shaken off | history ring `book/MemoryRewind`, hook `AttributeEvents.playerTick` |
| `memory_archive` | Archive | signature | foes within 16 blocks (up to 8) are archived for 6 s; at the end 60 percent of everything that hurt each is replayed on it, capped at 35 percent of its health (half for a boss); cooldown 1500 | hook `AttributeEvents.damaged` |

Damage through `GrimoireBook.hurt` (EP law); the Archive replay is a single magic-source hit of the already scaled, capped total (so it is not scaled twice). Bosses take half the raw
value. No other hard control than Forget, which uses the shared lock. The history ring, the veil and the archive keep their state in small maps keyed by UUID that the hooks clean
(logout clears the ring).

## 6. VFX
`vfx/client/layer/MemoryFxLayer`, textures `tools/gen_memory_rebuild_textures.py` -> `textures/particle/memory_*.png`: FX1 Recollection (a film reel and a frame contract on the caster, a
head of light with a film strip, a ghostly second strip following a moment later and photo frames left along the path, a frame and a ring opening on impact), FX2 Forgetting Field (a ring of
film frames turning on the ground, a slow reel over the middle, film strips unwinding on the rim, frames drifting up and dissolving), FX3 Flashback (flash, a reel opening, frames flung out
and spinning, rings of frames, glass shards, a second fainter flash a few ticks later: the replay). 48 to 108 vertices. Scenes `tools/vfx_preview/scenes.d/memory_rebuild.json`.

## 7. Render layers
Armour: solid pieces cutout (translucent while forging or shattering), ghost pieces translucent, glow maps additive full bright, extras additive (a ring of frames, sparks, the lens glow).
Echo: translucent model, additive glow and blink flash. Orb: cutout model, additive frame trail, glow and spark. Order: solid, translucent, additive.

## 8. Files
New: `book/MemoryBook`, `MemoryRewind`; `prop/MemoryRebuildProps`; `client/aura/MemoryAura`; `client/prop/MemoryRebuildPropPainter`; `vfx/client/layer/MemoryFxLayer`;
`tools/gen_memory_rebuild_geo.py`, `gen_memory_rebuild_textures.py`; assets `*/entity/memory_*`, `textures/particle/memory_*`; scenes; `tools/lang/memory_rebuild.json`.

## 9. Not verified
No game run. Verified: type check with negative control, JUnit of `MemoryRewind`, geo checks, preview tools. Not seen in the real game: Recall's teleport against real terrain (it refuses
when the old place is blocked), the veil against real damage sources, the echo's blink against moving foes, the archive's tally against damage over time. Sources of a hit that never
reach `LivingDamageEvent.Post` (damage that is fully absorbed) are not counted by the Archive.

## 1.99.0 armour policy

Memory Magic has no armour in the anime, so since 1.99.0 **no cast puts the temporary armour of this document on the caster** (policy kind NONE). Everything described above as the armour (the render layer, its painter, geo models, textures and generator) is KEPT on disk and registered in `AuraRegistry`, so old saves and tests still resolve; it is simply never started by a page. What the pages did besides the armour (damage cut, thorns, resistance, constructs, effects) is unchanged. Where the armour carried the magic's look, every cast of a family page now sends a small `MEMORY_FX2` glow that follows the caster instead (no armour piece is drawn). See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
