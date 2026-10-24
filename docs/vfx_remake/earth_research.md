# Earth VFX remake: research and baseline (phase 1, no code changed)

Owner request: remake the 4 basic element VFX (fire, water, wind, EARTH) "using the internet, that means videos, images etcetera". This file is the EARTH research. Proposal: `earth_proposal.md`. Pictures: `docs/previews/vfx_remake/earth_baseline.png` (current look) and `earth_reference_board.png` (owner art).

## 0. How the research was done, and what failed (honest list)

The sandbox has no outbound internet. Fandom pages, videos and episode frames could NOT be opened. Only `WebSearch` worked, and it returns a short text summary of the pages it found, never the page itself. So:

| Source | Result |
|---|---|
| English: blackclover.fandom.com spell pages (Witch Hunter Claws, Rampaging Mother Earth, Divided Mother Earth, Mud Wall Partition, Mud Wall Cliff, Ground Wall, Earth Magic, Sarado) | Found through WebSearch, but only as summaries. Names, categories, one-line descriptions and some episode numbers (cited per line below). No colours, no motion. |
| English: episode pages (Episode 22, 72, 75, 96, 155) | Titles / one-line summaries only. |
| Japanese: ja.wikipedia, hissatuwaza.kill.jp (必殺技辞典), ja.namu.wiki, tv-tokyo.co.jp | Search summaries only. Gave first-episode numbers for 土壁横断 and 暴れ地母神, no visuals. 魔女狩りの爪 not found in Japanese. |
| AniList / MyAnimeList / Crunchyroll episode summaries | Not returned by the searches (MAL forum appeared once, unrelated). |
| Animator interviews / VFX breakdowns of Studio Pierrot | Searched. Only general articles came back (freelance animator "Beast" on episode 166, outsourcing to Mito Animation, 2018 production report praising effects work). NOTHING about rock or earth effects. Not usable. |
| Videos (3 mp4 in the upload folder) | Frames pulled with ffmpeg and looked at: `13c7e9c6` is the owner's Spirit Dive (wind) TikTok, `6243570a` is Noelle's Water Valkyrie TikTok, `e6e75ea1` is a dungeon room. None shows earth magic, so no earth motion reference came from video. (They are useful to the wind and water builders.) |
| Owner's own art (`Black_clover_VFX_Effects_art.zip`, `Black_Clover_Art.zip`, `Black_clover_modes.zip`) | Opened and looked at with my own eyes (all 24 + 33 + 10 images). Only 4 are earth/stone related, see the reference board. The 33 "Black Clover Art" screenshots are grimoire list tables (no effects); the modes zip is Asta/Yuno/wind and fan art (nothing earth). |
| Older repo boards (`docs/previews/gnome`) | Gnome is the 3D mob (model); not VFX, only used as a colour check. |

Bottom line: the canon TEXT facts are real and cited. The visual facts (colour, shape, motion) come from the owner's art and from general anime-cel conventions; they are NOT confirmed against the episode frames. Treat every "how it looks in the anime" below as: text from a wiki summary where cited, otherwise "inferred".

## 1. Canon earth magic in Black Clover (what the wikis say)

Earth Magic users: Sol Marron (Blue Rose, grimoire Earth + Creation), Mohl (Witch Hunter Claws), Sarado (Heart Kingdom Earth Spirit Guardian, True Earth Magic), Protobe Collina (Earth grimoire seen in the grimoire list screenshots). The spirit of earth, Gnome (JA ノーム / ノウム), has not appeared in the anime as far as the sources say (ja.namu.wiki settings page: the earth spirit is the only one of the four not shown; the page is old, the manga ended May 2026, so unverified).

Spell list of the Fandom "Category:Earth Magic Spells" (EN, via WebSearch): Rampaging Mother Earth, Divided Mother Earth, Mud Wall Partition, Mud Wall Cliff, Dirt Clod Seal, Witch Hunter Claws, Rising Ground, Giant King of Wisdom, March of Holy Fools, Ground Wall.

## 2. Effect by effect

Legend for "mod" lines: quad cost = max vertices seen in the repo preview / 4 (cap per effect 400 vertices = 100 quads, `VfxVertexBuffer`). Textures: `earth_rock` (brown-grey strata, opaque), `earth_dust` (soft tan puff), `earth_crack` (dark jagged strip), `earth_chunk` (9-point polygon), all from `tools/gen_element_vfx_textures.py`.

### 2.1 Earth Spikes: `STONE_SPIKES` (page `spike_line`; also WikiSpells line 261)

- Canon: no canon spell of this name in the sources. Nearest: Rising Ground, "attacks with rock fragments rising from the ground" (EN fan wiki summary via namu.wiki; Fandom page not read) and Sarado raising earth pillars that stop just short of Charlotte and Mimosa in Episode 155 (EN Fandom/animehunch/bubbleblabber summaries via WebSearch).
- Anime look: text only. Pillars rise from a field. Colour, shape, dust: inferred.
- Mod now: up to 12 four-sided pyramids (`spike`, 16 verts each) erupting one per block, 2 ticks apart, with a crack strip, a dust billboard and one flying chunk each. 41 quads max. Brown (`earth_rock`), shaded by a fixed light. Look in the baseline board: thin identical cones, evenly spaced, like traffic cones.
- Gap: pillars are cones (canon is a pillar / rock fragments), no dark outline, no flat anime shading, dust is a tan puff with no cel edge, no ground displacement.

### 2.2 Ground Wall / Mud Wall Partition: `EARTH_RISE` (page `wall`; Ground Wall in WikiSpells 801; character arts)

- Canon EN: Mud Wall Partition: "the user slams their hands on the ground, and a large earthen wall rises from the ground" (Fandom page via WebSearch); anime Episode 72, Sol splits the hot spring bath to shield Charlotte (EN Fandom Episode 72 page, manga Chapter 111). Mud Wall Cliff: heightens the Mud Wall Partition (Fandom). Ground Wall: supplementary Earth spell, "erects a huge wall of earth" (namu.wiki summary).
- Canon JA: 土壁横断 (Mud Wall Partition) "raises the ground to make a wall", listed as first appearing "第111話" by hissatuwaza.kill.jp, which is the manga chapter (the anime episode is 72 per EN wiki); another snippet said 第124話, so the JA number is inconsistent and I do not trust it.
- Anime look: text only: a big flat earthen wall rises in one piece from the floor. Inferred: slab, dust at base, cel puff.
- Mod now: NO wall at all. It draws two crack strips along the base, 10 dust billboards, 16 flying rubble billboards and one dust ring. 40 quads. The wall block itself is the real block placed by the spell (server side); the VFX is only the dust. In the board: a low brown fog and dots.
- Gap: the signature read of the spell, a slab rising, is missing; the effect is dust only.

### 2.3 Divided Mother Earth / "Mother Earth Split": `EARTH_FISSURE` (page `mother_earth_split`)

- Canon EN: Divided Mother Earth is an Earth + Creation Magic spell: with an open grimoire the user summons several golems made only of the earth underfoot, muscular, simple design, smaller than Rampaging Mother Earth (Fandom page via WebSearch). User Sol Marron. Anime Episode 75: Sol makes several smaller golems that ambush from underground (Fandom Episode 75).
- Canon JA: 分裂地母神 (hissatuwaza / ja namu snippets: "creates a giant earth golem split in several").
- IMPORTANT canon mismatch: the mod calls it "Mother Earth Split" and draws a crack with slabs. Canon is a group of small golems. A fissure is the mod's own invention (the 1.93 doc already says the wiki pages were not reachable). Not changing gameplay in this task; the proposals draw the canon golems as an option.
- Mod now: zig-zag crack (additive glow edge in the page colour `0xFFFFB45A`), up to 8 leaning slabs (`slab`, 20 verts), dust and chunks. 76 quads. In the board: a row of dark brown blocks tilted along a line.
- Gap: blocks read as boxes, no anime outline, and canon golems are absent.

### 2.4 Witch Hunter Claws: `EARTH_CLAWS` (page `witch_hunter_claws`)

- Canon EN: Witch Hunter Claws: "with an open grimoire the user creates a giant mole made of earth, which can dig underground and ambush targets from behind; its grip is strong enough to crush an adult" (Fandom page via WebSearch; user listed: Mohl). A "Witch Hunter Claws: Dirt Clod Seal" variant is mentioned (Fandom, summary only). JA: not found.
- Canon mismatch: the mod draws 5 stone claws closing over a ring. Canon is an earth MOLE. Claws do match the name only.
- Mod now: cracked ring (additive glow), 5 two-joint cones that close over the centre, 8 dust, 8 chunks. 80 quads. In the board the claws read as a tent or umbrella of cones.
- Gap: cones read as a tent, no canon mole, no digging motion (mound travelling under the ground), no grip.

### 2.5 Rampaging Mother Earth: `EARTH_RAMPAGE` (page `rampaging_mother_earth`, signature)

- Canon EN: offensive Earth + Creation spell: the user opens a grimoire and forms a golem from the ground underfoot; highly muscular, simple design, strong enough to crush the target with its hands; can be split into smaller golems; steered with simple commands. English dub voice line "Gaia's Rage", wiki name "Raging Mother Goddess" (Fandom page via WebSearch). Anime appearances: Episode 22 (zombie fight in the capital), 75 (Sol vs the crystal, golem held by Flash Bog), 96 (Sol rides the golem, Yami jumps on it) (Fandom episode pages via WebSearch). JA 暴れ地母神: "a giant earth golem; used for attack and for transport" (ja.wikipedia characters list); hissatuwaza says first appearance 第26話 (not cross-checked, differs from EN ep 22).
- Mod now: a wave of paired slabs rolling from `from` to `to`, the front rises highest, a torn-ground strip, crest dust and rubble. 93 quads, the most expensive effect (400-vertex cap nearly reached, 372). In the board: two rows of brown boxes like a row of crates.
- Gap: canon is a muscular GOLEM. Owner art "Ideal_Closer" is the best reference: a stone giant of flat polygon plates with dark outlines (reference board 1). Mod has slab rows, no body.

### 2.6 Shared shapes used by earth pages

- `EARTH_SPIKES` (ElementShapesLayer): ~10 camera-facing shard beams (`SHARD` texture) plus one bloom glow. About 11-22 quads. Used by many NON-earth books (Eruption, Quicksand Tomb, Titan Stomp, Ossuary, Steel Spikes, Gravity). A new earth look must not change these without an explicit decision, rule 2. `FX_EARTH_SPIKES`: used by CharacterArtsGold and CelestialArts only; no layer read here, not evaluated.
- `EARTHFAM_CAST / ZONE / BURST` (EarthFamilyLayer, 1.93.0): one layer for six magics (Sandstone, Rock, Red Ochre, Stone, Earth, Mineral); identity from the colour (`EarthFxStyle`). Earth uses soil `0xB08850`, glow `0xD2E65A` (acid green), dark `0x3A2412`. Costs measured in the preview: CAST 35 quads, ZONE 92, BURST 65 (design note says about 60 max). Used by earth_golem, earth_armor, sinkhole and the earth signature flash. The board shows a dark golden sigil with small cones and yellow glints. Not canon either (the green glow and grass are the mod's own).
- Boulder Shot (`TensuraShots.BOULDER_SHOT`) is a projectile shot from the Tensura side, not this layer; out of scope.

## 3. Cross-effect gaps (what the remake must fix)

1. Look: brown textured 3D boxes and cones with fixed shading; no anime language (dark outline, flat shading, bright rim, dust "cels"). The owner's earth art (Ideal_Closer, Gravity_Singularity, Red_Shine_Mace) is flat cel, dark outline, cool grey for stone, orange-brown for soil.
2. Silhouette: slabs and cones, no big readable shape (wall, golem, mole).
3. Dust: one tan blurry puff (`earth_dust`); the anime reference shows grey cel-lobed puffs.
4. Impact: no flash, no radial shards, no freeze frame on hit.
5. Cost: Rampage 93 quads and Claws 80 are near the 100-quad cap; the remake must be at or below those.
6. Canon: Claws (mole), Divided Mother Earth (small golems), Rampaging (golem) differ from canon; Ground Wall has no wall.

## 4. Unverified (not seen in any frame)

Colours of the anime earth spells, whether dust is grey or tan in the anime, how the golem forms (rises out of the ground vs rolls up), whether the mole is a mound or a full body. Needs the owner to say, or episode frames (episodes 22, 72, 75, 96 for the golem and wall).
