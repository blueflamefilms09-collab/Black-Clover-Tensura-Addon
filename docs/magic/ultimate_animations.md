# Ultimate Magic: charge animations by kingdom, and Ultimates in character grimoires

Follow-up of `docs/magic/ultimate_magic.md` (owner answers: (a) a dedicated charge animation per kingdom, (b) what a character grimoire shows).
Previews (stand-in figure posed from the real keyframes, software drawn): `docs/previews/ultimate/anim_clover.png`, `anim_diamond.png`, `anim_spade.png`, `anim_heart.png` (6 frames of the wind-up pose, 6 of the hold, 4 of the release, front and side view each) and the overview `anim_kingdoms.png` (the four kingdoms next to the old default).

## NEW and REPLACED

| Part | New or replacement | Notes |
|---|---|---|
| 12 clips `ult_<kingdom>_<start/loop/release>` in `cast.animation.json` | NEW (appended; the 28 old clips are byte for byte as before) | written by `tools/gen_ultimate_anims.py` |
| `ultimate/UltimateAnim` (pure kingdom to clip mapping) | NEW | |
| `ultimate.kingdomAnimations` (SERVER, default true) | NEW | false = the old single loop |
| Wind-up plays the kingdom's start clip, then its loop; release plays its release clip | REPLACEMENT of "always `cast_mana_zone` then `cast_signature`" | the old ids stay registered and are the DEFAULT for a caster without a kingdom and for the setting off |
| `ultimate.showInCharacterGrimoires` (SERVER, default true) | NEW | false = the old rule |
| Character grimoire shows the Ultimate of its own attribute(s) | REPLACEMENT of "hidden in every character grimoire whose set does not list the page" | the plain and basic grimoire, and a character without a file, are untouched |
| `tools/ultimate_anim_preview.py`, `magic.ultimate.anim`, `magic.ultimate.charbook`, tests | NEW | |
| KEPT | everything else: `cast_mana_zone`, `cast_signature`, `cast_fail`, `CastAnimClient`, the payload, `UltimatePages` (its page clip is still `cast_mana_zone`), the Time Magic | |

## Kingdoms audited

`blackclover/Kingdom` has exactly four entries: **Clover, Spade, Heart, Diamond** (the suit of the grimoire cover). There is no Witch Forest, Elf or Dark Triad kingdom in the mod: the Dark Triad are the rulers of the Spade Kingdom (they follow the Spade clips), and the Witch Forest and the Elves have no entry in the kingdom list and no player data, so a player can not belong to them and they have no animation. The enum is the only list of kingdoms; `UltimateAnim.forKingdom` has a `default` branch and `UltimateAnimTest` fails for any kingdom added later until its three clips exist (decision for the owner: say if a Witch Forest / Elf style should become a fifth and sixth entry; the clips are a few lines in the generator).

The player's kingdom (server side, once per cast, `UltimateManager.kingdomOf`):

1. `Heritage.kingdom(player)`: the kingdom the Tower Wizard's questionnaire gave the player (persisted player data `nusmp_heritage.Kingdom`);
2. else the kingdom of the cover of the grimoire casting (`GrimoireBook.cover(instance).kingdom`), the kingdom that accepted the mage;
3. else none, which plays the old Mana Zone loop.

## The four personalities

The client applies the ARMS (replaced by the clip) and the LEGS (added to the standing pose) only; the body, head and position keys are in the clips for the Blockbench master but the current `CastAnimClient` ignores them (it says so: separate model roots would leave gaps). So every personality is carried by arms and legs.

| Kingdom | Personality (canon it follows) | Wind-up pose (10 ticks) | Hold (loop) | Release |
|---|---|---|---|---|
| **Clover** | defiant, raw, shouting (Asta, Yami, Mereoleona) | arms swing up from low, overshoot, settle thrown wide (Z 88), wide planted stance | 4.0 s: chest-out breathing, arm drift, two "roar" surges (a big one at 1.0 s, a small one at 3.0 s) where the arms lift 10 to 16 degrees and the legs brace | fists overhead (0.10 s), SLAM down in front at 0.20 s with a lunging stance, rebound, arms fall wide and relax |
| **Diamond** | disciplined, regal, controlled (the Diamond Kingdom's scholars and nobles) | hands snap together low in front, then lift smoothly to chest height; feet together and rigid | 6.0 s: the joined hands rise slowly to the face and sink again (one cycle), a faint tremor of power, nothing else moves | hands draw in to the chest, then part and are held out like a decree, stance never moves, slow dignified lowering |
| **Spade** | cold, sharp, ruthless (the Dark Triad, Lucius' court) | arms snap up and cross over the chest (one hand above the other so they do not clip), staggered narrow stance | 5.0 s: almost still, a slow shoulder drift, two sharp jolts (1.5 s and 3.6 s) of the crossed arms | arms whip tighter for two ticks, then thrust outward as two blades (contact 0.15 s), held cold, dropped without ceremony |
| **Heart** | calm, graceful, warm (Heart Kingdom, Mana Method) | one hand settles on the chest, the other rises and opens to the side | 4.8 s: the open hand blooms up and sinks like a flower with a small circle, the hand on the chest only breathes | the open hand rises first, both hands open in an embrace (0.22 s), a long soft landing |
| *no kingdom / setting off* | the old look | `cast_mana_zone` from tick 0 | `cast_mana_zone` (1.6 s loop) | `cast_signature` |

All poses were authored from the owner's brief in the request; no wiki page was fetched for them (the canon traits are the owner's), so no source list or "could not fetch" list applies (rule 10).

## Format and how it is played

* The same file and format as every other cast clip: `assets/nusmp/animations/player/cast.animation.json`, Blockbench / GeckoLib animation JSON `format_version 1.8.0` (this repo uses its own `GeoAnim` loader and `CastAnimClient`, not Player Animator). Bones `body / left_arm / right_arm / left_leg / right_leg`, degrees in the vanilla model's own rotation convention, times in seconds with one key per game tick (20 per second), linear interpolation. The file imports into Blockbench through Animation > Import; the repo keeps no `.bbmodel` sources (the guide mentions one, none exists), so the Blockbench-compatible naming is the clip id itself: `ult_<kingdom>_<start|loop|release>`, at most 48 characters (the payload limit).
* Authored by code: `python3 tools/gen_ultimate_anims.py` adds or refreshes ONLY the `ult_*` clips and leaves every other clip as it is on disk (`--check` lists them without writing). Redundant keys on straight lines are dropped (the result is the same motion).
* **start** is 1.0 s long and flagged `loop: true` ON PURPOSE: `CastAnimClient` only blends a new clip out of the previous one when the previous one loops. The server swaps to the loop clip at tick 10 (`UltimateAnim.START_TICKS`), so the start clip never wraps; its last pose equals the first pose of the loop (test), so the hand-over is a 3 tick blend with no jump.
* **loop**: first key equals last key (test), 4 to 6 s with several periods inside (breath, drift, accents); a 5 to 20 second wind-up shows it 1 to 5 times.
* **release**: 1.0 s, one-shot, contact within the first third, ends relaxed. The cast sends its stop 20 ticks after the release (the last pulse), so the clip fits exactly.

Flow (all on the server, through the EXISTING `CastAnim.play` payload `CastAnimPayload(entity id, clip name)`; there is no packet of its own):

| Tick | What |
|---|---|
| 0 | `UltimateManager.start` chooses the clips once (`UltimateAnim.choose(ultimate.kingdomAnimations, kingdom)`) and plays the first clip. The grimoire then plays the page's own clip as it does for every page; `UltimateManager.pageClip` (one line in `GrimoireBook`) answers with the clip already playing, which the client ignores, so the page can no longer replace the kingdom's pose |
| 10 | the hold clip (once; a style without a start clip, the default, has nothing to swap) |
| release | the release clip |
| +20 | `CastAnim.stop` (as before); a cancel plays `cast_fail` and stops (as before) |

## Cost and budgets (rule 11)

* **Server**: at most 4 `CastAnim.play` payloads plus 1 stop per cast (it was 3 plus 1), each an entity id and a clip name of at most 48 characters (about 30 bytes) to the players tracking the caster only (`sendToPlayersTrackingEntityAndSelf`); at most `maxSimultaneous` casts exist (default 2, hard cap 8), so at most 40 small payloads per whole cast on the whole server, never per tick. The kingdom lookup is two tag reads once per cast. No new per-tick work: the step adds one integer comparison (`age == 10`).
* **Client**: draw cost is the existing cast-animation player: one entry per animating entity in `CastAnimClient`'s map, one pose sample (5 bones, 2 tracks each) per animating player per frame, only for players that are rendered. The Ultimate adds at most `maxSimultaneous` (8) animating casters at once; the clips are plain keyframes (7 to 49 keys per bone and track). Honest note: the existing `CastAnimClient.sample` and `GeoAnim.Track.sample` allocate small float arrays per call, so "no per-frame allocation" is NOT claimed for the cast-animation player as a whole; this change adds no new allocation to it and no client class. The clips are loaded once with the file.
* **Memory**: the animation file grew from 114 KB to 175 KB (the 12 clips).
* **Players animating at once**: the casters (at most 8) plus whatever other cast clips are playing; nothing about a clip depends on the number of players.

## (b) Character grimoires and the Ultimate

Audit: `CharacterSetRegistry.hidden` hid every page of a bound character's own book that the set does not list, and `opens` answered "no" for it, so the Ultimate (a page no set lists) was hidden in, and never opened by, the grimoire of the character whose attribute it is. The pages of other books were never touched by a character grimoire.

Server setting `ultimate.showInCharacterGrimoires` (SERVER, default **true**), pure rule `CharacterSetRegistry.hidden(canon, book, page, modeZero, hideSignatures, ultimatePage, showUltimate)` and `opens(...)`:

| Grimoire | Setting on | Setting off (the old behaviour) |
|---|---|---|
| bound to a character WITH a set, Ultimate of his own book(s) | shown, opens by the ordinary mastery order and the Ultimate gate | hidden, never opens |
| bound to a character WITH a set, Ultimate of another attribute | hidden | not touched (shown if the book is there) |
| bound to a character without a file (`floga`) | shown | shown |
| plain / basic grimoire (bound to nobody) | shown | shown |
| mode 0 and every page that is not an Ultimate | unchanged | unchanged |

"His own book(s)" = the book of his set plus every other book in which a page is reserved for him in `BasicSignatures` (Charmy: Cotton and Food; Gordon: Poison and Curse; Finral: Spatial; ...). With the eight flagship Ultimates in the game now: Yuno sees the Wind Ultimate, Noelle Water, Fuegoleon / Mereoleona / Magna / Leopold Fire, Luck and Gadjah Lightning, Yami Dark, Karna and Lemiel Light, Finral and Langris Spatial; Asta, Charlotte, Dante, Vanica, Zenon, Lucius, Rades and the others have no Ultimate book yet, so for them every Ultimate stays hidden. When another builder adds an Ultimate row for a book, the rule picks it up with no change.

## Tests

Pure: `UltimateAnimTest` (the mapping for every kingdom, the setting off and no kingdom give the old ids, the hold is due once at tick 10, the real clips: exist, real bones, on the tick grid, start flagged loop and long enough never to wrap, start ends where the loop begins, loops close and are neither frozen nor jumpy, releases fit 20 ticks and end relaxed, the four kingdoms differ by at least 40 degrees, the 28 old clips are still there), `UltimateAnimSourceScanTest` (pure class, at most 4 plays per cast and none in a loop, no packet of its own, the payload unchanged, the kingdom looked up once per cast, SERVER setting, one hook line, defaults are the old ids), `CharacterUltimateTest` (every shipped character against every Ultimate, setting on and off, several kingdoms' characters, a second book, the basic grimoire, mode 0, `opens`, the wiring). In game (CI server smoke, prefix `magic.ultimate`): `magic.ultimate.anim` and `magic.ultimate.charbook`. `CharacterRestSelfTests` (the older "listed == hidden" check) now leaves the Ultimate page out, since that page is the one the setting changes; no other `CharacterSet*Test` or `BasicGrimoiresTest` pinned the old hidden state.

## Not verified

Nothing was played. No clip was seen in a real client: the previews are a block-figure stand-in with no skin, robe, lighting, head, body or blending (the body and head keys are ignored by the client as it stands), so how the arms read with a real skin, the grimoire floating at the right hand (the guide's 30 degree rule: the Diamond and Spade poses bring both arms in front of the chest, so the book may sit inside them), armour and robes is unknown; the 3 tick blends at the start-to-loop and loop-to-release hand-overs were reasoned from `CastAnimClient`, not watched; a second player's view and a joining player (who sees the loop only after the next clip change) were not tried; no multiplayer run; the server settings were not edited in a running server (only the pure paths of the "off" cases are tested; the game test reads the defaults).
