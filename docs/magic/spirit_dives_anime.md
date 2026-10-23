# Spirit Dives, ANIME look: the wearer bathed in the light of the spirit (the default look since 3.2)

> The owner rejected two looks of the Spirit Dives: the 3.0.0 remake (`docs/magic/spirit_dives.md`, `V2`: wind halo, wing-blade cards, hair, mane and ribbon pieces) and the Djinn-Equip plates
> (`docs/magic/spirit_dives_djinn.md`, `DJINN`: "that's not a style I want for Spirit Dive"). They asked for **what the anime shows** and chose "Anime look from your screenshot". This is that look. It is
> option value `ANIME` of the client option `spiritDive.look` and the default; `DJINN`, `V2` and `CLASSIC` are all still there and choosable.

Nothing here was seen in a game. Every picture in `docs/previews/spirit_dive_anime/` is the project's headless rasteriser (`tools/render_preview`, the real painter classes against Minecraft stubs):
no bloom, no post effects, approximate blending, and the first person arms are in no picture. Read section 11 before trusting any statement about looks.

Sources of the canon (rule 10): WebSearch summaries of blackclover.fandom.com `Yuno_Grinberryall` / `Spirit_Dive` / `Saint_Valkyrie_Dress` / `Salamander` pages and gamerant / epicdope / comicbook.com articles;
the page bodies were not fetchable (direct downloads are blocked), the Japanese wiki fallback was not reachable either. The visual targets are the owner's own screenshots (Yuno's Spirit Magic stills incl. the
Spirit Dive, Noelle's variations, the Fuegoleon art), cropped into `docs/previews/spirit_dive_anime/references/`.

## 1. What was built

The wearer's **own skin and clothes stay visible**; over them, in the colours of the spirit:

| Part | What it is | How it is drawn |
|---|---|---|
| **Shell (A)** | the energy wrapped round every limb: streaks / flame tongues / currents / glowing cracks flowing along it, a bright cel outline at the silhouette, a soft glow outside | the player's own model parts, each inflated about its own centre by a fixed number of pixels (even thickness), in four passes: a translucent *cover* (the light on the clothes), an additive scrolling *wash* (vanilla energy-swirl type with the offset quantised to 24 cached steps), an additive *rim hull* and a dimmer *soft hull* whose quads are drawn with the winding reversed, so only the faces turned away survive the culling: the ring of the hull that sticks out beyond the silhouette, the body hides the rest by depth. No shader. |
| **Hair (B)** | glowing hair | a tinted cap on the hat layer (strands in flat tones, hair on the top, back, sides, a fringe) and curved, tapered **locks** (subdivided strips with shared edges, sway with movement, lift on the entry) |
| **Eyes (B)** | glowing eyes | two slanted glows in the plane of the face (z = -4.08 of the head box) so they turn and pitch with the head, and a soft glow behind; none under a helmet or in first person |
| **Crown and halo (C)** | the crown of the stills: a circlet with bars of light rising from it (wind: bars, fire: flame tongues, water: wave crests with a drop on each, earth: crystals) | an upright camera-facing sprite above the forehead, generated textures, no cubes. Character tier: also the thin spiky ring of light turning slowly behind the head |
| **Plumes (D)** | the big smooth translucent sheets sweeping out from the shoulder blades | subdivided Bezier strips with shared edge vertices, layered two deep (shorter, narrower, brighter toward the front), a travelling wave on the CPU, swept back by speed; two passes: the translucent body with the cel bands and the dark outline baked in, then the additive glow of the leading edge |
| **Backdrop (E)** | the spirit assimilated (character tier only, close up) | the spirit's own geo (Sylph fairy with wings, Undine mermaid, Salamander, Gnome), additive, 22 percent alpha, a little larger and behind the wearer, drawn with a plain light texture so it is a ghost in the colour of the dive |
| **Entry (F)** | a short transformation | a flash, the shell grows from the feet up over 18 ticks (legs, then body and arms, then the head's hair), the hair lifts, the crown opens with a pulse, a ring pulse and a burst of the element, the plumes open with an overshoot; the exit is the same backwards |

Per element (the stills and the owner's art; "cel" = flat bands, a bright core, a dark outline where the art has one):

| Element | Colours | Plumes | Hair | Crown | Extra |
|---|---|---|---|---|---|
| **Wind** (Sylph, Yuno) | pale green-white, white core | broad pale plumes with white streak lines, up-and-out sweeps | white-green spikes | bars (a tiara) | character: the circle of connected crosses stands behind the back, turning (the existing `sdv_sigil_wind`), a circle of crosses round the left wrist in the first dive, the Sylph ghost |
| **Fire** (Salamander, Fuegoleon) | cel-banded like his flame arm: dark red outline, red, orange, yellow core | a long **flame cloak** down the back, flame wings, flame sweeps | long **flame hair** streaming up and back, the Salamander's two **horns** (canon: draconic features) | flame tongues | his right arm is the separate accepted limb: `FuegoleonArmAura.drawsArm(player)` is asked and the shell leaves the arm out, so no second arm is drawn; the Salamander ghost |
| **Water** (Undine, Noelle) | blue and silver | blue-silver water **ribbons** (thin, long, curling) and one wide flowing sheet | glowing blue hair, **twin tails** hanging from the back of the head | wave crests with drops, a scalloped ring halo | the ripple circle behind the back, the Undine ghost |
| **Earth** (Gnome, never a character tier) | grey and gold, amber glow veins | angular **rock shards** (three flat segments) | crystal spikes | crystals | glowing cracks flowing over the body |

### Stages (the canon progression, WebSearch summaries)
* **First dive of Wind (HALF)**: the LEFT half only: the left arm and leg green, ONE wing (the left plume), the left half of the hair, a HALF crown standing behind the head on the left, the left eye, a circle of crosses round the left wrist.
* **Complete form (FULL)**: both sides covered, TWO wings, the FULL crown, the circle of crosses behind the back (character tier).
* **UPGRADE** (half to full): the left half stays on at full strength, the right side opens with the entry animation.
* The first dive of Fire, Water and Earth dresses the arms only (canon: Fuegoleon's arms cloaked in mana), with one small plume and no head effects.

### Tiers
**Standard** (every mage): shell, cap and 9 locks, eyes, crown, 2 plume rows x 2 layers (8 segments), ground ring. **Character** (the mage whose bound grimoire is Yuno's, Fuegoleon's or Noelle's; `SpiritDiveTier`):
3 plume rows x 2 layers (10 segments), 14 locks, 4 hand and foot trails, the second halo, the circle behind the back, the ghost of the spirit, the entry sprites. The server entry point, the tier plumbing, the style bits
and the configs are untouched; only the client look changed.

## 2. The faults of the two rejected looks, and what was changed (item I)

Looked at: `docs/previews/spirit_dives/*` (3.0.0) and `docs/previews/spirit_dive_djinn/*` (plates).

| # | Fault in the old previews | Fix in the ANIME look |
|---|---|---|
| 1 | **Blocky pieces.** The 3.0.0 crown is a stack of towers above the head, the hair and mane are cubes, the plates of the Djinn look are boxes with edges; the silhouette is a pile of cuboids. | Nothing on the head, the back or the hair is a cube: crown and halo are generated smooth sprites, the plumes and locks are subdivided strips with shared edges. The only boxes left are the player's own body parts (the shell follows them) and the spirit ghost. |
| 2 | **Floating, separate pieces.** The halo ring and crown float above the head with a gap, the blade cards hover beside the shoulders, nothing connects them to the body. | Plumes grow out of the shoulder blades (their root is on the body and fades in), locks out of the skull, the crown sits on the forehead's circlet; everything is rooted and moves with the head and body. |
| 3 | **Flat cards.** The wing blades are rectangles with straight edges and a hex or dot pattern, they show their edge-on thickness of zero from the side and read as paper strips. | Strips are curved, tapered to a point, wavy, and the width vector blends the sheet's own normal with the camera's look vector (a semi-billboard), so a sheet never turns edge-on; each quad takes the winding that faces the viewer. |
| 4 | **The body was covered or replaced.** V2 plates on torso and arms, the Djinn full armour hides the skin and the clothes completely: "that's not a style I want". | The skin and the clothes stay: the shell is translucent / additive light over them and a rim outline round them. The face keeps most of its own colour (a third of the light the clothes get). |
| 5 | **Pixel noise instead of cel shading.** Honeycomb, dither and dot textures, no flat bands, no outline, no bright core: the glow looks like noise at any distance, unlike the stills' flat tones. | Every texture is cel-banded by `tools/gen_spirit_dive_anime_textures.py`: outline, shade, base, mid, light bands, a bright leading edge, thin streak lines; fire is banded like Fuegoleon's arm (dark red, red, orange, yellow core). |
| 6 | **No energy body.** The body is untouched or a flat tint; nothing flows along the limbs and nothing is brighter at the silhouette, so the character is not "bathed in light". | Scrolling streaks / tongues / currents / cracks along the limbs, the cover that tints the clothes, a bright cel rim round the silhouette and a soft glow round that. |
| 7 | **Stiff hair, round eyes.** The strands of 3.0.0 are straight flat quads; the eyes are round billboards in front of the face; no hair colour on the head itself. | A glowing hair cap on the hat layer plus swaying tapered locks that lift on the entry; slanted eyes drawn in the face's own plane with a soft glow behind, hidden under a helmet. |
| 8 | **Effect overload and cost.** Spirals, rune rings, sigils on the ground and the back, columns of light bury the figure (3.0.0, about 7,700 vertices a diver), the plates reach 11,000 to 17,000. | Few, large shapes (a crown, two or three plume rows, the shell); at most 5,384 vertices for the heaviest character dive and 1,412 for any standard one (section 7). |

**What still differs from the references** (honest): the stills are painted: soft gradients, a huge single swoosh of a plume, the character's face and hands, hair that is hundreds of strands; here the plumes are a layered fan of
leaf-shaped strips (the harness shows them as petals when seen from the front), the hair is a cap plus fourteen locks, the body is still the Minecraft box model with the player's pixel skin (the shell can only wrap it),
the outline is as blocky as the body it follows, the face has no anime features, Yuno's hands do not reach out, the sky and the light bloom of the stills are missing, the spirit ghosts are boxy (the spirit models are made of cubes),
Noelle's sword of water (canon: she can create one in the Saint Valkyrie form) is not part of the dive, and the harness cannot show the glow bleeding over the surroundings.

## 3. The look option and its default (item G)

`nusmp-spiritdive-client.toml`: `spiritDive.look = ANIME | DJINN | V2 | CLASSIC` (default **ANIME**, was DJINN). `Look` is `{DJINN, V2, CLASSIC, ANIME}`: ANIME was appended, nothing was removed. `classicLook = true` still wins and
means CLASSIC; `characterEffects = false` draws every dive as the standard tier; `particles` still switches the particle trail. A client that never read its config draws ANIME.

## 4. Files

| Area | File |
|---|---|
| pure rules (common, no client class) | `book/SpiritDiveAnimeRules.java` (look parsing, tiers, specs, budgets, timeline, palettes, texture names), `book/SpiritDiveAnimeGeometry.java` (plume / lock / trail tables, profiles, spines, strip edges, winding rule) |
| painters (client) | `client/aura/SpiritDiveAnime.java` (the painter), `SpiritDiveAnimeShell.java` (the inflating, flipping consumer and the shell passes), `SpiritDiveAnimeRibbon.java` (the strips), `DiveAnimeFirstPerson.java` (first person arms), `DiveHead.java` (the helmet question) |
| self-test | `book/SpiritDiveAnimeSelfTests.java` (key `magic.spirit_dive_anime`) |
| textures | `tools/gen_spirit_dive_anime_textures.py` writes `assets/nusmp/textures/aura/sda_*.png` (36 files: swirl, plume, plume glow, lock, lock glow, halo, crown, hair x 4 elements + rim, soft, eye, burst) |
| previews | `tools/spirit_dive_anime_scenes.py` (43 scenes), `tools/spirit_dive_anime_previews.py` (the sheets), stubs `tools/render_preview/stubs/.../NURenderTypes.java`, `DiveHead.java` |
| tests | `SpiritDiveAnimeTest`, `SpiritDiveAnimeScanTest`, `SpiritDiveAnimeAssetTest` |

## 5. NEW / REPLACED / KEPT (rules 2 and 5)

| | What |
|---|---|
| **NEW** | the ANIME look (shell, hair cap and locks, eyes, crown and halo, plumes, trails, spirit ghost, entry and exit, first person arms), its textures and generator, its rules and tests, the self-test, the preview scenes, the references |
| **REPLACED** (as the default only) | the default look of the Spirit Dives: DJINN becomes ANIME (config default, `SpiritDiveLook` default, `DEFAULT_LOOK`); nothing was deleted |
| **KEPT** | `DJINN` (the plates), `V2` (the 3.0.0 remake), `CLASSIC` (1.99), all their painters, pieces, textures, models and tests; Fuegoleon's flame arm (every look of it); the server entry point (`GrimoireBook.spiritDive`), `SpiritDiveForm`, `SpiritDiveTier`, the style bits, the leases, the sounds, the particle trail (`DiveParticles`, themes `spirit_dive.*`); the Time Magic VFX untouched |

Existing files edited (additions only): `SpiritDiveDjinnPieces.java` (`ANIME` appended to `Look`, `DEFAULT_LOOK = ANIME`), `SpiritDiveAura.java` (one dispatch line), `SpiritDiveLook.java` (default ANIME, `anime()`), `DiveClientConfig.java`
(default ANIME and the comments), `DiveClient.java` (two listeners), `NURenderTypes.java` (`spiritSwirl`: the one place a vanilla additive type may be named, `SafeAdditiveSourceScanTest`), `SpiritDiveBoot.java` (one line).
Existing tests updated only where they pinned the default look: `SpiritDiveDjinnTest` (default look, number of look values), `SpiritDiveDjinnScanTest` (the number of look values, the config default).

## 6. How it is read in a game (no shader, no new asset format)

* The shell reuses `renderToBuffer`-equivalents: each of the six body parts is rendered through one reused `VertexConsumer` that moves every vertex out along the part's three axes (the corners of a cuboid are
  `p + d * (sx * ax + sy * ay + sz * az)`, the signs from the vertex's side of the part's centre, so the corners stay joined) and, for the hulls, emits each quad as 0, 3, 2, 1. The axes are the columns of the part's matrix
  (JOML names a field `m<column><row>`: found and fixed in the preview). A mod that renders model parts through its own fast path (Embeddium) falls back to the vanilla path because this consumer is not a buffer writer.
* The wash scrolls with the vanilla `energySwirl` type; the offset is quantised to 24 steps and one type is cached per element and step (at most 96 for the life of the client).
* Plumes, locks and trails are quads with per-vertex alpha (fade in at the root) in the vanilla translucent emissive type (body) and the alpha-weighted additive type of `SafeAdditive` (glow).

## 7. Budgets and worst case (rules 9 and 11, item H)

Counted by `SpiritDiveAnimeRules.vertices` (a vertex is a quad corner, four per quad), checked against an independent count in `SpiritDiveAnimeTest` and against the harness.

| Dive (full stage) | FULL (up to 24 blocks) | MID (up to 48) | FAR (up to 64) | beyond 64 |
|---|---|---|---|---|
| standard, Wind / Fire / Water / Earth | 1,412 | 596 | 120 | 0 |
| character Wind (Yuno, with the Sylph ghost) | 4,304 | 1,156 | 120 | 0 |
| character Fire (Fuegoleon, with the Salamander ghost) | 3,752 | 1,156 | 120 | 0 |
| character Water (Noelle, with the Undine mermaid ghost) | **5,384** (worst) | 1,156 | 120 | 0 |
| for comparison: the V2 remake (3.0.0) | about 7,700 | | | |

* **Worst case of one diver: 5,384 vertices (30 percent under V2); of a standard diver 1,412.** During the 18 tick entry up to 56 more vertices (14 quads of flash, ring pulse, burst columns).
* **Per frame:** at most `CHARACTER_PER_FRAME` = 3 character tiers (the rest draw the standard tier): 3 x 5,384 + 1,412 per further diver in range. Beyond 64 blocks nothing is drawn; far players (48 to 64) draw only the outline.
* **Passes:** FULL draws 21 part renders (shell) + the plume, lock and trail strips; MID 16 part renders and two thinner plume rows; FAR 5.
* **CPU per frame and diver (character tier):** about 12 plume spines of 11 points, 14 locks of 6, 4 trails of 7 (about 250 points, each a few `sin` and one `pow`); no allocation (statics only; the only objects made per frame are vanilla's own inside `ModelPart.render`).
* **Network:** no new packet. **Server:** no new work (the look is client only; the self-test is pure). **Particles:** unchanged: the trail of `DiveParticles` through `Fx` (at most 6 dives x 2 particles per tick = 12 a tick, through the client particle budget and the effect quality).
* **Entities:** none. **First person:** two passes over one arm, at most 2 x 24 vertices, at half strength; nothing on the head.
* The geo ghosts are drawn once per diver: Sylph fairy and wings 492 quads, Undine mermaid 762, Salamander 354, Gnome 144 (counted from the files by `SpiritDiveAnimeAssetTest`).

## 8. Tests (rule 12)

* `SpiritDiveAnimeTest` (pure): ANIME is the default and parses, the old values stay, tier mapping (character only for Yuno on Wind, Fuegoleon on Fire, Noelle on Water, never Earth), levels of detail, the 18 tick entry and its mirrored exit, the
  feet-up growth order, the swirl steps, the vertex count of every element x stage x tier x level checked against an independent count and under V2, the crowd bound, every plume / lock / trail spine finite and inside its bound over
  ages, speeds and fall speeds, the profiles, the strip width, the winding rule, the half dive and the horns.
* `SpiritDiveAnimeScanTest` (source): common classes name no client class and the pure ones no Minecraft class; the painters allocate nothing and build no location or string in their frame methods; render types cached; the
  level of detail, culling, character cap and probe are used; no packet; the flame arm is not redrawn; helmet and first person guards; the old looks are still dispatched and their files exist; the option, the self-test and the hooks are registered.
* `SpiritDiveAnimeAssetTest` (files): every texture exists with its size; every additive texture has no colour under alpha 0 (SafeAdditive); swirls opaque with dark gaps; plume bodies translucent with a solid dark outline and a fading root; the hair cap covers top and back and leaves the face free; the spirit model quad counts equal the budget's.
* Self-test (CI server smoke test): **`magic.spirit_dive_anime`**: files, tables, budgets, style round trip, option values. Registered by `SpiritDiveBoot` like the other dive self-tests; the prefix has to be added to `SMOKE_EXPECT` in `.github/workflows/smoke.yml` (not edited here).
* Run in this session: the new tests, the Djinn / V2 / tier / arm tests, `ServerSafetyTest`, `PerfSourceScanTest`, `ConfigFileNamesTest`, `SafeAdditiveSourceScanTest`, `SafeAdditiveTextureScanTest`, `TextureAlphaTest`, `GeoAssetsTest`: all green.

## 9. Previews (`docs/previews/spirit_dive_anime/`)

`wind.png`, `fire.png`, `water.png`, `earth.png` (front, 3/4, back and side of both tiers next to the reference crops), `close_<element>.png` (the cap, eyes, crown, shell), `entry_<element>_<tier>.png` (the entry at ticks 1 to 18 and the
exit), `half_dives.png` (the four first dives and a Wind upgrade). Iterated about fifteen rounds against the references; the faults found on the way (the shell outline floating off the body because the matrix columns were read as rows, hair locks that streamed fifty pixels behind the head, a
crown made of radial spikes instead of the stills' bars, plumes four times the width of the body, the pale grey face, hanging icicles from the cap, the boxy ghost) are fixed.

## 10. Rule 12 checklist

New work was checked for new versus replacement (section 5); it is kept behind the option that restores the old looks; shared files were only appended to. The version, the changelog and the README are the coordinator's.

## 11. What could not be verified (honest list)

* Nothing was seen in a running game, with real lighting, bloom, the real skin of a player, the real depth buffer or other mods. The harness is a software rasteriser.
* The rim outline relies on the depth buffer hiding the reversed hull behind the body (true in the harness; the aura layer draws after the model, which is what vanilla does) and on `RenderType.eyes` style types culling back faces
  (the alpha-weighted additive type of `AdditiveTypes` is built the same way; if it did not cull, the hull would show as a bright box round every part).
* `RenderType.energySwirl` is lit by vanilla's entity shader; the light passed is full bright, but the directional shading of the entity shader still dims faces turned away from the light a little.
* The eyes sit at the vanilla face positions; a skin with a different face layout (a mask, big eyes) will look off. A helmet is detected from the head slot only (not from a Curios hat).
* First person (`DiveAnimeFirstPerson`) copies the placement of the game's hand draw as `DiveFirstPerson` does, and has not been seen.
* Large draw distances and many divers at once were counted, not measured (`/multiverse perf` has the section `c.spiritdive.anime`).
* The harness prints a handful of `NORMAL_MISMATCH` warnings on the unlit translucent passes: the strips' normals are not used by an unlit type.
* The Japanese wiki fallback and the English wiki page bodies could not be fetched; the canon rests on search summaries and the owner's screenshots.
* Decision for the owner: whether the ghost of the spirit behind the wearer (25 percent in the brief, 22 here because it reads as a boxy shape) should stay, and whether Noelle's sword of water should get its own page.
