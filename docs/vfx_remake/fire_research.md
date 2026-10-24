# Fire (Flame Magic) VFX remake - phase 1 research and baseline

Phase 1 only: research, the "before" picture and the proposal (`fire_proposal.md`). No code, texture, version or changelog was changed.
Owner request: remake the 4 basic element effects (Fire, Water, Wind, Earth) using the internet, videos and images. This file is the FIRE part.

## 0. What could and could not be fetched (read this first)

The sandbox has no usable outbound web for page fetching and the search tool returns text summaries only. Nothing visual was downloaded.

| Source | Result |
|---|---|
| blackclover.fandom.com pages (EN wiki, spell and episode pages) | NOT fetchable. Only the titles, URLs and a one-paragraph summary came back through WebSearch. |
| Japanese sources (tv-tokyo.co.jp anime site, shonenjump.com, dic.pixiv.net, ja.wikipedia, ja.namu.wiki, Dream Legend wikiwiki) | NOT fetchable. Summaries through WebSearch only. |
| AniList / MyAnimeList / Crunchyroll episode summaries | Not returned by any query (no hits). |
| YouTube / Pierrot / animator interviews / VFX breakdowns | NONE found. The only production facts found: Studio Pierrot, S1 director Tatsuya Yoshihara, later Ayataka Tanemura; episode 172 credits a "Pierrot Digital Animation Room" and "3DCG: Infinity1". No fire-specific breakdown exists in the results. |
| Video or image frames of the anime | IMPOSSIBLE here (no download of binary media; WebSearch is text). |
| Owner art in `/root/.claude/uploads/2c8ee7c0-.../` (Fuegoleon image `da96e299-image.jpg`, `Black_Clover_Art.zip` grimoire sheets, `Black_clover_VFX_Effects_art.zip`, `Black_clover_modes.zip`) | READ and looked at with my own eyes. This is the only visual ground truth in this phase. |
| Repo boards in `docs/previews/` (fire_casting, fuegoleon_arm_djinn) | Listed, existing Salamander boards not re-analysed (they concern the armour and dragon, not the VFX shapes). |

Consequence: the "how it looks in the anime" column below is text from fan wikis (second hand, often one line) plus what the owner's own stills show.
No line claims frame-level knowledge of the animation. Where a search said "not found" it is written as "not found", not guessed.

Source key used per line: `[EN-wiki]` = blackclover.fandom.com via WebSearch summary, `[EN-other]` = TV Tropes / Baidu / other English pages via WebSearch,
`[JA-wiki]` = Japanese page via WebSearch summary (named), `[OWNER-ART]` = a still the owner supplied, `[MOD]` = this repo's code.

## 1. Canon facts found (fire users and spells)

| Fact | Source |
|---|---|
| Fire Magic is one of the four main attributes; its spirit is Salamander. | [EN-wiki] Fire Magic page (summary) |
| Fuegoleon Vermillion, captain of the Crimson Lion Kings; his right arm is lost in the Eye of the Midnight Sun assassination (episodes 23-27 range); Salamander leaves Fana and chooses him; he uses Salamander's Breath on the elf. Revives as the flame-armed Fuegoleon in anime episode 98 ("Sleeping Lion"). | [EN-wiki] Fuegoleon / Assassination pages; episode 98 from [JA-wiki] anime-maketto.com summary |
| Mereoleona Vermillion, Flame, new captain; Ep 71 "The Uncrowned, Undefeated Lioness" shows the Crimson Lions training; Ep 91 she sets clones and Zora on fire. | [EN-wiki] Episode 71 / 91 pages (summary) |
| Spell names found on the EN wiki fire category: Spiral Flame, Leo Rugiens, Sol Linea, Profound Spiral Flames, Salamander's Breath, Wild Bursting Flame (VIZ/Funimation title "Exploding Flames"), Exercitus Leonum. | [EN-wiki] Category:Fire Magic Spells, Profound Spiral Flames, Wild Bursting Flame |
| NOT found anywhere: "Calderos", "Ignis Columna" (as a pillar), "Crimson Lion King" spell page, any episode number for Leo Rugiens. The mod uses these names; their canon status is unverified (the mod's own `docs/attributes/flame_rebuild.md` says the same). | search "not found" results |
| Fuegoleon fire in the anime: blasts three Dark Disciples through a wall, catches several devils in swirling flames to block the castle exit. | [EN-wiki] Fuegoleon page (summary) |
| Episode 151 (anime original, captains' battle) is praised by a fan blog for its animation and for the flame-wielding captain; episode 63 praised as "movie-quality" battle animation (not fire-specific). | [JA-wiki-like blogs] tadahome.info, moyugenn.com (WebSearch summary) |
| No frame-by-frame description of the flames (colour, outline, bloom) was returned by any query. | all searches |

## 2. The owner's own fire art: what it shows (visual ground truth)

All from the owner's files; crops on `docs/previews/vfx_remake/fire_reference_board.png`.

| Still | What the fire looks like | Use for the remake |
|---|---|---|
| Fuegoleon with the Salamander arm (`da96e299-image.jpg`) [OWNER-ART] | Flat cel-painted flame: 3-4 solid tones (yellow-gold #FFC21A, orange #FF7A12, red-orange #E83A10, deep red edge), tongues that hook and curl sideways, no black outline, no white core; a few 2-4 px yellow and red ember flecks; the ground fire on the right is a soft yellow-white bloom with orange haze; whole frame graded warm orange-brown. | Direction A (cel) and the shared palette. |
| Fire grimoire covers (`Black_Clover_Art.zip`, Fire Magic rows) [OWNER-ART] | Glow-based fire: saturated orange body, yellow-white centre, red edge, soft outer bloom; the Crimson Lion covers go red > orange > yellow with white-hot highlights. | Direction B (glow). |
| `Red_Shine_Mace.webp` (VFX art zip) [OWNER-ART] | A small gun-flash: soft yellow-white ball, ragged orange edge, no hard outline. | Small bursts, embers. |
| `Gravity_Singularity.webp` [OWNER-ART] | Warm orange-gold scene grade where a big effect dominates. | Optional screen-tint idea (client only, bounded). |
| `Black_clover_modes.zip` | No fire images (green wind and blue water modes). Not used. | - |
| Dark / Sealing / VFX zips (manga impact frames: Dark_Cloaked_*, Iron_God_of_War, Hydra) [OWNER-ART] | Black-and-white ink panels with white speed lines and bold brush shapes. | Direction C (ink language). |

## 3. The 11 existing fire effects: canon, anime look, mod, cost, gap

Costs are from the repo preview (`python tools/vfx_preview/preview.py`), the **maximum vertices of one activation in the worst frame** (4 vertices = 1 quad; the engine
caps one activation at 400 vertices, `VfxVertexBuffer.MAX_VERTICES_PER_ACTIVATION`). Baseline picture: `docs/previews/vfx_remake/fire_baseline.png`.
Preview scenes: `tools/vfx_preview/scenes.json` (fire_*), `scenes.d/flame_fx.json` (flame_fx_*), `scenes.d/fire_baseline.json` (flame_trail, flame_explosion, added in this phase).

Textures in use (all in `textures/particle/`, all WHITE-alpha shapes that are tinted by the vertex colour and drawn ADDITIVE, `VfxBlend.FIRE` = ADD):
`fire_tongue` 64x128, `fire_band` 256x64, `fire_ribbon` 32x128, `fire_lion` 256x256, `flamefx_flame` 192x96, `flamefx_tongue` 96x192, `flamefx_ember` 64x64, `flamefx_burst` 256x256,
`flamefx_ring` 256x256, `flamefx_lick` 256x64, `flamefx_lion` 256x256, `flamefx_smoke` 128x128; plus shared `GLOW`, `SPARK`, `FLAME`, `MAGIC_EXPLOSION`, `EARTH_DUST` (smoke).

### 3.1 FIRE_LION (FireSpellLayer) - Leo Rugiens

| | |
|---|---|
| Canon | Leo Rugiens (EN-wiki, listed as a Fire Magic offensive spell, no description in the summary). JA: no confirmed JA title for it; JA summaries describe Fuegoleon's lion spell as 大火炎獅子の咆哮 ("roar of the great flame lion"): a giant lion made of flame that breathes fire from its mouth and burns the target [JA-wiki: playnews/ciatr-type summary]; ja.namu list gives Fuegoleon "Roar of the Great Flame Lion" and "Hand of the Flame Lion" with lion-shaped flames [EN-other: NamuWiki]. |
| Anime look | Not described in any fetched text beyond "a giant lion of flame". Visual language from owner art: cel flame tongues, orange/yellow/red tones. |
| Mod now | A camera-facing sprite of a lion face (`fire_lion`) with 16 flame tongues around it as a mane, a ribbon body of fire, 10 tongues licking up, 12 embers, glow, shock ring on arrival; leaps along an arc; screen shake. |
| Cost | max 264 vertices (66 quads); 34 ticks. |
| Gap | A flat sticker of a lion face with a spiky halo; no body, no legs, no jaw, no roar. Colours are pale (additive white core). It reads as a sun, not a lion. |

### 3.2 FIRE_SPEAR (FireSpellLayer) - Sol Linea

| | |
|---|---|
| Canon | Sol Linea (EN: "a beam of concentrated fire from the hand" [EN-other: TV Tropes]). JA: ソル・リネア / 小太陽光芒: a laser fired from a small fireball that ignites what it pierces [JA-wiki: Dream Legend wikiwiki summary]. |
| Anime look | Not described beyond the line above: a thin concentrated beam from a small fire ball. |
| Mod now | A white-hot ribbon core, three flame strands spiralling round it, a spearhead tongue and glow, 12 embers; 16 ticks. |
| Cost | max 196 vertices (49 quads). |
| Gap | It is drawn as a corkscrew spear, canon is a straight beam from a small sun ball. Strands look like thin lines at range (see baseline: thin streak). |

### 3.3 FIRE_PILLAR (FireSpellLayer) - Ignis Columna / Calderos

| | |
|---|---|
| Canon | "Ignis Columna" is NOT found as a pillar. JA search maps 螺旋火炎 (Ignis Columna in the Dream Legend wiki) to a defensive vortex of fire wrapped round the caster [JA-wiki: Dream Legend summary]; another JA page says the same, a flame whirl that blocks attacks. "Calderos": not found in EN or JA. |
| Anime look | Defensive spinning flame around the caster (text only). |
| Mod now | Ground glow, ring, one fire band hoop at the base, four tall flame tongues as a column, eight loose tongues, 14 embers; 30 ticks; shake if power >= 1.5; also used by the TrueMagic FLAME zone. |
| Cost | max 220 vertices (55 quads). |
| Gap | Column is four pale sprites forming one flame; a vortex (spiralling band with a hollow centre) would match the JA description for Ignis Columna better. |

### 3.4 FIRE_BURST (FireSpellLayer) - generic flame explosion (Exploding Fireball, WikiSpells, BasicArts)

| | |
|---|---|
| Canon | Generic. Closest: Wild Bursting Flame's fireballs; Exploding Flame; no specific anime frames found. |
| Mod now | Glow, a big glow billboard, shock ring, fire band hoop, 12 flame tongues thrown out, 14 embers, 4 smoke puffs (alpha). 22 ticks. |
| Cost | max 244 vertices (61 quads). |
| Gap | A ring of petals ("crown") that looks like a lotus lying on the ground; no heat core, no ball of fire, smoke is a flat grey card. |

### 3.5 FIRE_SPIRAL (FireSpellLayer) - Spiral Flame

| | |
|---|---|
| Canon | Spiral Flame (Leopold Vermillion): "tornado-like blasts of fire from his extended hands"; "two powerful twisters of fire from his palms, a larger one when he holds his arms out the same way"; Profound Spiral Flames = bigger, made with a natural mana array, extends over great distances [EN-wiki: Spiral Flame, Profound Spiral Flames; EN-other: TV Tropes]. Leopold's Spiral Flame described as whirlwinds of flame from the hands [EN-other: NamuWiki]. JA name 螺旋炎 / 螺旋火炎 (writing varies) [JA-wiki]. |
| Anime look | Twisting fire twister from the palms. |
| Mod now | Core ribbon, three corkscrew strands that widen to the head, spinning fire ring "drill bit", tongue at the tip, glow, 12 embers; 22 ticks. |
| Cost | max 268 vertices (67 quads). |
| Gap | Best match of the old set (a widening vortex) but built of hair-thin ribbons: it reads as a wire spring, not a tornado of flame; no flame body between strands; the palette is white-gold. |

### 3.6 FIRE_WILD (FireSpellLayer) - Wild Bursting Flame

| | |
|---|---|
| Canon | "The user thrusts their hand downwards and a burst of flames manifests on the ground surrounding the user. From this circle of flame, multiple fireballs manifest and launch in every direction", cannot be aimed; VIZ / Funimation title "Exploding Flames" [EN-wiki: Wild Bursting Flame]; "multiple fireballs launched in all directions" [EN-other: TV Tropes]. |
| Mod now | Three waves 6 ticks apart; each throws 9 flame tongues as beams outward, a band of fire spreading on the ground, a shock ring, embers raining; screen shake. |
| Cost | max 288 vertices (72 quads). |
| Gap | Canon is a ground circle of flame and FIREBALLS flying outward. The mod throws flat leaf-shaped tongues (looks like a flower in the baseline); no balls, no ground circle of fire. |

### 3.7 FLAMEFX_CAST (FlameFxLayer) - the Flame book's cast stream (Leo / Spiral / Sol Linea / Crimson Lion King pages via `FlameArts`)

| | |
|---|---|
| Canon | Stands for several (see above). |
| Mod now | A charge: sun-lion sigil and ring plane at the caster, bloom; then a head of fire runs to the target trailing a core ribbon and three winding strands, embers, flash and ring on arrival. 22 ticks. |
| Cost | max 180 vertices (45 quads). |
| Gap | In the baseline the stream is a faint thin line with a white star at the head; the three strands vanish at distance; no fire body. Cheapest effect, most used (FlameArts, Bulls character arts). |

### 3.8 FLAMEFX_ZONE (FlameFxLayer) - sun-lion field (Calderos, Wild Burst, Ignis Columna via `FlameArts`)

| | |
|---|---|
| Mod now | Ground glow, sun-lion sigil plane turning, ring, 14 flame tongues standing on the rim, 6 inner tongues, 16 embers; 80 ticks. |
| Cost | max 156 vertices (39 quads). |
| Gap | A magic circle with tiny thorns, not a field of fire; flames are 0.5-1 block spikes. The sigil is a stylised lion that reads well as a "rune", so it should stay as an option, but the fire itself is absent. |

### 3.9 FLAMEFX_BURST (FlameFxLayer) - starburst impact

| | |
|---|---|
| Mod now | Glow, a large starburst billboard (white flash decaying in 7 ticks), two ground rings, ten flame strips thrown out and up, 14 ballistic embers, 5 smoke puffs; 28 ticks. |
| Cost | max 216 vertices (54 quads). |
| Gap | Reads as a holy-light flash rather than fire: white starburst and thin rings; no orange mass. |

### 3.10 FLAME_TRAIL (FlameMagicLayer) - stream used by the Explosion and Magma books, BasicArts, CharacterArtsBulls

| | |
|---|---|
| Mod now | Two scrolling `FLAME` ribbons (body and core), burning head glow and billboard, 18 embers; 24 ticks. |
| Cost | max 164 vertices (41 quads). |
| Gap | A straight lit rod; looks like a laser pointer (baseline). |

### 3.11 FLAME_EXPLOSION (FlameMagicLayer) - fireball burst used by Explosion / Magma books

| | |
|---|---|
| Mod now | Heat flash, 22 `FLAME` billboard petals in a sphere, a `MAGIC_EXPLOSION` core, ground ring, 24 sparks; shake for power >= 1.5; 26 ticks. |
| Cost | max 292 vertices (73 quads), the most expensive. |
| Gap | The nearest to a real fireball: a lumpy additive blob. Washes to white, no red, no smoke, no scorch. |

## 4. Cross-cutting gaps (the same in all 11)

1. **Every flame texture is a white alpha shape, tinted and drawn ADDITIVE.** Additive tinted white cannot show dark red edges, outlines or shadow tones, and it saturates to white where layers overlap. Result: pale, uniform, "sparkly" fire (the baseline sheet).
   The owner's art is multi-tone and mostly NOT additive: it needs baked-colour textures and ordinary alpha blending (`VfxBlend.ALPHA`, drawn before ADD) for the body, with ADD only for glow.
2. **No animation of the shape itself.** Each sprite is static and only rotates, scales or flickers in alpha; anime fire is redrawn every 2-3 frames (flipbook of a few distinct silhouettes).
3. **Canon mismatches** (Section 3): Wild Bursting Flame has no fireballs; Spiral Flame is a spring not a tornado; Ignis Columna is a column but canon (JA) is a vortex; Sol Linea is a corkscrew, canon is a straight beam from a small sun.
4. **Scale**: at 1.0 power the baseline streams are 0.1-0.3 blocks thick; from a player's distance they vanish.
5. **Sixteen additive overlaps** per frame brighten to white; the colour ramp (red outside, yellow core) of the anime is lost.

## 5. Budget facts the remake must respect

* One activation: <= 400 vertices (100 quads) hard cap; `ctx.seg(full, min)` scales counts by the client detail level; `hasBudget(n)` guards extra quads.
* Current maxima per effect: 156 to 292 vertices. The remake should stay <= each current maximum (rule 11).
* No new packets are needed: the remake changes only client drawing of existing `VfxShape` ids (server and packets untouched, rule 9).
* Time Magic VFX files are untouched (rule 3).

## 6. Files created in this phase

* `docs/vfx_remake/fire_research.md` (this file), `docs/vfx_remake/fire_proposal.md`
* `docs/previews/vfx_remake/fire_baseline.png`, `docs/previews/vfx_remake/fire_reference_board.png`
* `tools/vfx_preview/scenes.d/fire_baseline.json` (two baseline preview scenes; no code changed)
