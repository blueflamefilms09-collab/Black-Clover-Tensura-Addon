# Devil forms and the devil's figure (3.97.0)

**What was missing.** The 26 devils of the eight worlds all looked the same when a player signed with one: a copy of the player's body with the tier's Weg markings, a pair of generic horns and a pair of generic wings. And the ritual
was only pictures (a circle, a sigil, a shadow): nobody stood there. This release gives every devil (1) its own **form**, worn on the player's body and forged in stage by stage like the Djinn Full Equip, and (2) a **figure**
that stands in front of the caller during the ritual, speaks to them, makes its offer and, if they sign, merges into them.

Nothing here could be seen in a running game by its author (no game, no compiler in the session: CI is the first compiler). The meshes and skins are made by the four asset sets (Blender) and are NOT in this change: until they
are merged, a devil without a mesh draws exactly the old generic look (on the player and as the figure). See "What could not be checked".

## How a player sees it

1. `/devil call <devil>` begins the ritual as before. A **figure** of that devil rises out of the circle three blocks in front of the caller, facing them (30 ticks), and says three lines at 15 %, 45 % and 75 % of the ritual
   (chat, to the caller only, in the devil's colour, grey italics).
2. When the ritual ends the figure **roars** (two seconds) and says its fourth line, the offer: **[Sign]** / **[Refuse]** in chat as before. It stays for the 30 second offer.
3. **Sign:** the figure shrinks, fades and slides into the player (20 ticks) with a burst in the devil's colour; the contract is written as before. **Refuse, time out, break the circle:** it dissolves (30 ticks).
4. A contract with a devil of another world remembers which devil it is. From **5 %** the markings are tinted in the devil's colour; from **20 %** the **head piece** is forged onto the head; from **50 %** the **arms, forearms,
   wings and tail**; at **100 %** and in the **Devil Union** the **whole form** (torso and legs), the glow in the devil's colour and the attribute's ground ring. Parts fly in from their own side, fade up and grow
   (the Djinn's own motion, `EquipAnimation`), staggered 0 to 10 ticks; when the level drops or the union ends they shatter out the same way.
5. Other players see it too (the server tells everyone who can see the devil which devil it is).

The old generic horns and wings are drawn only for a contract **without** a devil of another world (the four grimoire devils, contracts signed before 3.76.0) or when the devil has no mesh yet or the client setting is off.

## How it is built

### Server (common, no client class)

| file | what |
|---|---|
| `devil/DevilNpc.java` | the figure: a plain `Entity` (no AI, no health, invulnerable, not pickable, not pushable, no collision, `noPhysics`, never saved). Three synced values: owner UUID, devil id, state. Ticks a counter; a terminal state removes it after its animation; one check every 10 ticks. |
| `devil/DevilEntities.java` | the entity type `anime_multiverse:devil_npc` (fire immune, tracked within 8 chunks, update every 10 ticks, `noSave`). |
| `devil/DevilNpcs.java` | everything the server decides: make it (`spawn`), the lines (`speakAt`, `speak`), the offer, merge, dissolve, discard, the figure's own check. Kept in the caller's live contract (`DevilContract.npc`, a UUID, never saved), not in a map. |
| `devil/DevilNpcRules.java` | pure numbers: states, animation lengths, cap rule, lifetime clamps, the check, the line ticks. |
| `devil/DevilNpcLines.java` | four lines for each of the 26 devils (generated, see "Lore"), English fallback of the lang keys `anime_multiverse.devil.npc.line.<id>.<0..3>` (`tools/lang/devil_npc.json`). |
| `devil/DevilFormRules.java` | pure rules of the form: the 11 roles, stages, anchors, forge delays, fly-in directions, level of detail by distance, glow, the attribute's ring and motion, the mesh file names. |
| edits | `DevilRituals` (spawn at begin, speak, offer, merge on sign, dissolve on refuse and break), `DevilContracts` (logout, dimension change, death discard the figure; the visual packet carries the devil), `DevilNet.Visual` (new optional last field), `DevilConfig` (`npc` section), `DevilContract` (live fields `npc`, `npcAt`), `DevilCommands` (`/devil clear` discards it), `WorldDevil.indexOf`, `DevilBoot` (registers the type), `DevilSelfTests`. |

The state machine of the figure: `RISING (30 ticks) -> WAITING -> OFFER (roar for 40 ticks, then idle)`, and from any of them `MERGE (20 ticks)` on sign or `DISSOLVE (30 ticks)` on refuse / timeout / break. The terminal states
remove it when their animation is over.

### Client

| file | what |
|---|---|
| `client/devil/DevilNpcRenderer.java` | draws the figure with `BossMeshDraw` (`entity_meshes/devil_<id>.txt`): the RISE action sunk and fading in, idle breathing, the ROAR action at the offer, the merge (shrinks, fades, slides into the owner's chest), the dissolve (the DEATH action and a fade); a pulsing glow in the devil's colour. Culled beyond 64 blocks, low-detail copy beyond 40, no glow beyond 48. Fallback when the mesh is missing: a plain humanoid with the old horns and wings. |
| `client/devil/DevilFormLayer.java` | the form on a player (a `RenderLayer` on both player renderers, after the markings). Each mesh part is placed by the pose of the body part it is anchored on, so it walks, turns, crouches and swings with the player. |
| `client/bossmesh/BossMeshDraw.drawParts` | new method next to `draw`: places every part with a callback instead of the procedural pose and draws each with its own opacity (the forging). `draw` is untouched. |
| edits | `DevilClient` (`Vis.devil`, the forge state, the registrations, the per-frame budget reset), `DevilClientConfig` (`customForms`), `WegLayer` (markings in the devil's colour; skips the generic horns and wings when the form is drawn). |

### How the mesh is read (for the people who make the meshes)

A devil's mesh is `entity_meshes/devil_<id>.txt` (+ `devil_<id>_lod.txt`), skin `textures/entity/boss_mesh/devil_<id>.png`, glow `..._glow.png`, the same format as the boss meshes (`EntityMeshData`, at most 3000 quads, body and
glow together). The form layer reads the eleven **role names** as the `MeshPose` roles: `torso head arm_l arm_r forearm_l forearm_r leg_l leg_r wing_l wing_r tail` (the first eight are required; wings and tail are optional).

| role | worn on | forged in at | delay (ticks) |
|---|---|---|---|
| head | the head | 20 % | 0 |
| arm_l, forearm_l | the left arm | 50 % | 2 |
| arm_r, forearm_r | the right arm | 50 % | 3 |
| wing_l, wing_r | the body (back) | 50 % | 8, 9 |
| tail | the body (back) | 50 % | 10 |
| torso | the body | 100 % or union | 1 |
| leg_l, leg_r | the legs | 100 % or union | 6, 7 |

Authoring contract: the mesh is modelled **feet on the floor (y = 0), facing +z, about the height of a player (1.8 blocks)**, with the joints where the player's are: neck / shoulder line 1.5 blocks up, the arms' shoulders 1.375
blocks up and 0.3125 blocks to each side, the hips 0.75 blocks up and 0.12 blocks to each side. Left is +x (the mesh's left arm is the player's left arm). The pivots in the file are free (the layer turns each part about the
**player's** joint, not about the file's pivot; the file's pivot is used only for the wing beat and the tail sway). Model the form a little larger than the player's body (about 0.03 blocks) so the skin does not poke through: the
vanilla body is not hidden.

The figure draws the same file with the boss-mesh poses (`MeshPose`: RISE, ROAR, DEATH, idle), so the same roles carry the animation there; a mesh made for the layer works for the figure unchanged.

## Settings

| setting | file | default | what |
|---|---|---|---|
| `devil.npc.enabled` | server `serverconfig/nusmp-devil-server.toml` | true | false = exactly the old behaviour: the ritual's pictures only, no entity, the old shadow at the offer |
| `devil.npc.max` | server | 8 (0 to 64) | the most figures on the whole server at once (one per player). Over the cap the old pictures-only shadow plays |
| `devil.npc.lifetimeSeconds` | server | 60 (10 to 180) | the longest a figure may live, whatever else happens |
| `devil.customForms` | client `config/nusmp-devil-client.toml` | true | false = the generic horns and wings and the white markings exactly as before |

(`devil.wegLayer = false` still turns every body change off, the forms included.)

## NEW / REPLACED / KEPT

| | |
|---|---|
| **NEW** | the devil's figure (entity, renderer, state machine, lines); the form layer and `BossMeshDraw.drawParts`; the optional devil field of `DevilNet.Visual`; the `npc` server settings and the `customForms` client setting; `DevilFormRules`, `DevilNpcRules`, `DevilNpcLines`; the lang keys in `tools/lang/devil_npc.json`; the `devil.npc` self-test; the tests listed below. |
| **REPLACED** | for a contract with a devil of another world that has a mesh: the generic horns and wings (replaced by the devil's own parts), and the white markings (now in the devil's colour). The ritual's closing SHADOW_UNITE picture is replaced by the figure's roar (it still plays when there is no figure). |
| **KEPT** | everything else, behind the settings above: the generic horns, wings, markings and the shadow for old-pact devils, devils without a mesh, `customForms = false` and `npc.enabled = false`; the old 4-argument `Visual` constructor and the old packet format (a client or server on the old format still works: the field is optional on the wire); every enum (nothing was added to one); save format (nothing new is saved). |

## Cost (rule 11), worst case

**Server.**
- Entities: at most `min(online players, npc.max)` figures, default at most 8, hard maximum 64, each alive at most `npc.lifetimeSeconds` (60). One per player: a new ritual removes the old figure first.
- Per tick per figure: one counter increment. Once every 10 ticks one check (one player lookup by UUID, one contract lookup). No scan of entities, blocks or chunks anywhere; the cap is counted once per ritual begin by looking at the online players' contracts (at most one UUID lookup per online player per level).
- Packets: one add-entity packet per figure to the players tracking it (8 chunks), then at most four small synced-data updates over its life (state changes); it never moves, so no position packets. Chat: 4 lines to the caller only. Pictures: one merge burst on sign (the same effect path as before, through the effect budget). Nothing per second.
- Cleanup: on logout, death, dimension change, `/devil clear`, refuse, break, timeout, sign (after 20 ticks), the safety lifetime, a figure whose owner is gone (the check), and the entity is never saved so a server stop leaves none.

**Client** (quads are the mesh cap of 3000 per mesh, body and glow together, until the real meshes are in; then the table of `docs` for each mesh replaces it):
- A figure: at most 3000 quads, 3 draw batches (cutout or translucent, additive glow); only within 64 blocks; far copy beyond 40 blocks, no glow beyond 48. With the default 8 figures all in view: 24 000 quads (never seen in practice: they are 8 different players' rituals).
- A player's form: at most 3000 quads + 8 vertices of ground ring + at most 4 draw batches; at most 4 players at full detail and 6 at the far copy per frame (`NoelleFormRules.budgeted`); nothing beyond 48 blocks. Worst case a frame: 4 x 3000 + 6 x 3000 (the far copy is never heavier, test) = 30 000 quads; typical (you plus one friend) 6 000.
- No allocation per frame in the layer, the placer and `drawParts` (pinned by `PerfSourceScanTest`); the figure's renderer makes one rotation object per figure per frame. Sections `c.devil.form` and `c.devil.npc` of the performance probe time them.

## Lore (rule 10)

The 104 lines (4 per devil) were written from memory of the series, not from the wikis: **no wiki page was fetched in this session** (the English wiki first and the Japanese one as the fallback were not attempted, so nothing
is sourced and nothing could "not be fetched": all of it is unchecked). They are in character in tone only; names, voice and fine details should be checked against the wikis before they are called canon. Each is one chat line
(at most 140 characters, a test pins that) and every line is a lang key with the English as the fallback, so a translation or a correction is a lang edit.

## Tests

- `DevilFormRulesTest` (pure): roles equal `MeshPose`'s, stages, anchors, delays, level of detail and the crowd budget, the figure's rules (cap, switch, lifetime, check), the line ticks inside the ritual, 4 lines each for the 26 devils and the lang file matches `DevilNpcLines`.
- `DevilFormAssetsTest` (reads the shipped files with the game's parser): every devil has mesh, low-detail mesh, skin and glow map; quads at most 3000; the basic 8 roles present; role-like names valid; parents before children; the far copy has the same roles and is not heavier; textures square powers of two; feet on the floor. **It fails until the four asset sets are merged.**
- `DevilSourceScanTest` (extended): only `DevilNpcs` may spawn an entity; the figure is bounded, never saved, untouchable, checked every 10 ticks; logout, dimension, death, refuse, break and sign end it; the form layer and the renderer are bounded and timed; the new pure classes name no Minecraft type.
- `PerfSourceScanTest` (extended): the placer, `drawParts`, the layer and the figure's draw build no object per frame; the new files build no location per frame; the two probe sections exist.
- Self-test `devil.npc` (CI server smoke): the type is registered; the cap / switch rules; a stand-in's ritual makes one figure in front of it, a second replaces it, the offer / merge / dissolve states, discard removes it, and a figure whose owner is not online removes itself within 10 ticks.

## What could not be checked

- Nothing was compiled and nothing was seen in a game. In particular the geometry that anchors a mesh part on the body part (the mirror between the layer's frame and the mesh's frame, the turn about the player's joint) is derived on paper and
  matches how `NoelleFormRig` places the Djinn's pieces, but a mesh sitting a block off, a mirrored arm or a wrongly turned wing would be seen only in a game. A preview with the real meshes is the first thing to look at.
- No mesh exists yet: the layer and the figure are untested against a real one; `DevilFormAssetsTest` fails until they are merged.
- The vanilla body is not hidden under the form (meshes must be modelled slightly larger); slim arms are not narrowed; armour is drawn by the game's own layer on top of the body and may be covered by a bulky mesh.
- The attribute's `motes` (six drifting particles at the whole form) are specified in `DevilFormRules` but not drawn yet; only the ground ring is.
- The off switch `npc.enabled` is covered by pure rules and by reading the code path; the self-test cannot flip a server config.
- The lore lines are unchecked against the wikis (above).
