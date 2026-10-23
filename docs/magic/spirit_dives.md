# Spirit Dive remake: the remade dives, the character tier and Fuegoleon's flaming arm

Owner request (round 1.99.x): "Remake all the Spirit Dives ... make the Fuegoleon user have his flaming arm when they acquire the spirit. If you need to remake the VFX, the effects must look 10x cooler
than the non-Character-Grimoire users."

Nothing here was seen in a game. Every picture is the project's headless rasteriser (`tools/render_preview`, the real painter classes against Minecraft stubs); the first person hand is not in any picture
(section 9). Read section 11 (what could not be verified) before trusting any number about looks.

## 1. What was built, in one paragraph

Every Spirit Dive (Wind, Water, Fire, Earth) has a **remade look** that replaces the 1.99 armour-like look by default: a lighter, more elemental dive (green wind mana, flame, flowing water, stone and crystal)
in two tiers. The **standard tier** is what every mage gets. The **character tier** is what the mage whose bound canon grimoire is the character of the element gets (`CharacterSpells.characterOf`):
**Yuno** on Wind, **Fuegoleon** on Fire, **Noelle** on Water. It adds that character's own pieces and a stack of effects (columns of light, helices, a large sigil on the ground and another behind the back,
orbiting rune rings, a shockwave and a pillar of light at the entry, glowing eyes and hands) in the character's colours (Yuno green-gold, Fuegoleon crimson-gold, Noelle blue-silver). Earth has no canon
character in the mod, so it is always the standard tier. A client option brings the 1.99 look back. **Fuegoleon's flaming right arm** is a separate, persistent look: while the Salamander is his (the
contract, the spirit out, or the Fire dive) the server turns it on and every client draws a molten flame arm from the shoulder, in third and first person, with embers and a mane of fire strands.

## 2. References and sources (rule 10: English wiki first, Japanese as the fallback)

| What | Source | How it was read |
|---|---|---|
| Spirit Dive forms: Yuno's first dive transforms only the left half (a green half crown, one green wing, the left arm green, a circle of connected crosses round the wrist); the fuller form has both sides (a circle of connected crosses behind the back, two wings, a full crown) | English fandom, *Spirit Dive* and *Yuno Grinberryall* pages (blackclover.fandom.com) | **search summary only** (WebSearch, extended): the page bodies could not be fetched (`getaddrinfo ENOTFOUND blackclover.fandom.com`) |
| Fuegoleon's dive: mainly the arms, covered in mana with draconic scales, horns and wings like the Salamander | English fandom, *Spirit Dive*, *Fuegoleon Vermillion* | search summary only |
| Fuegoleon's flaming RIGHT arm: "the fire spirit Salamander chose him and revived him, and he has a flaming right arm" (official profile) | Japanese: the official Black Clover account (@blackclover_off, "魔法騎士名鑑", Fuegoleon) and the Shonen Jump official character page (`shonenjump.com/j/sp_bclover/character/chara027.html`); two fan articles on the four great spirits | search snippets only (the pages were not fetched) |
| Yuno in the Spirit Dive: a green aura, a crown-like wind halo, wing-like wind blades, glowing green hair and eyes | the owner's screenshot `images/127.png` (the fandom Yuno page, Spirit Magic list) | looked at |
| Fuegoleon's arm: a molten, flame-shaped arm from the shoulder, a red-orange fire mane flowing off his body | the owner's screenshot `images/128.png` | looked at |

**Could not be fetched at all:** every fandom page body (the host does not resolve here), `ja.wikipedia.org` and the Japanese fandom (no result for 精霊同化 / スピリットダイブ). Details of the look that
the text sources do not give (hair and eye glow, the exact blade shapes) come from the two screenshots, not from text.

## 3. NEW / REPLACED / KEPT (rule 5)

| | What |
|---|---|
| **NEW** | the character tier and the remade standard tier of the four dives (`book/SpiritDiveTier`, the grade in bits 4-5 of the style number of `Aura.SPIRIT_DIVE`); 14 new geo pieces (`spiritdive_arm_wind_halo`, `wind_blades_l/r`, `wind_hair`, `fire_mane`, `fire_cape`, `fire_flames_l/r`, `flame_arm_r`, `water_ribbon_l/r`, `water_rings`, `water_train`, `earth_crown`); the procedural effects (`client/aura/SpiritDiveFx`); the sprites (`textures/aura/sdv_*`: light column, entry pillar, shockwave, four sigils, rune ring, flame sheet, wisp), the vein layers (`spirit2_<element>_veins`) and a warmer fire skin (`spirit2_fire_shell`); the particle trail (`client/aura/DiveParticles`, seven themes `spirit_dive.*`); Fuegoleon's flaming arm (`Aura.FUEGOLEON_ARM`, `book/FuegoleonArm`, `FuegoleonArmRules`, `client/aura/FuegoleonArmAura`, `FuegoleonArmFx`, `FireArmFirstPerson`); the server config `nusmp-spiritdive-server.toml` and the client config `nusmp-spiritdive-client.toml`; two self-tests; unit and scan tests; the generator kit (`tools/gen_spirit_dive_v2.py`, `sdv_lib.py`, `sdv_sprites.py`) and the preview sheet tool |
| **REPLACED** (the old one stays behind the client option `spiritDive.classicLook`) | the LOOK of the four dives: the heavy armour-like piece lists (greaves, claws, sleeves, the cross-hatched fire wings, the water wings and halo of the Valkyrie armour at the full stage ...) are no longer drawn by default; the lists of the remade look are `SpiritDivePieces.pieces(element, stage, grade)`; the old lists, painter passes, models and textures are all still there and `classicLook = true` draws exactly the 1.99 dive |
| **KEPT** | how a dive is entered, left and paid (`GrimoireBook.spiritDive`, the gauge, the stats, the upkeep, the half and full stages, the upgrade): not touched; the style numbers of the standard tier are the same numbers as before (the grade bits are 0); every old model, texture, enum constant and page; the Time Magic VFX (not touched) |

## 4. The tiers

The server chooses the tier when the dive starts and on every renewal (once a second): `SpiritDiveForm.gradeOf(player, element)` = `SpiritDiveTier.gradeFor(element, canonId)`, where `canonId` is the id of
the canon grimoire bound to the player. A character grimoire only earns its OWN look on its OWN element; a stray combination is the standard tier. The server switch `spiritDive.characterTier = false`
sends every dive as standard.

| Element | Standard tier (every mage) | Character tier |
|---|---|---|
| **Wind** | the mana skin; left half: a half mantle, a ring round the wrist, half a crown, one honeycomb wing; full: the right half too and the full crown (the series' first dive is left-only, kept); one light column ring, one helix of wind, wisps of hair, glowing eyes, a ring of the element on the ground, a few motes | **Yuno**: plus the crown HALO (a turning ring of leaf prongs above the head), the WING-LIKE WIND BLADES (three long swept blades a side, behind the back), streaming hair, three helices, eight light columns, the large sigil of connected crosses on the ground AND behind the back, two orbiting rune rings, glowing hands, the entry (shockwave, second ring, pillar of light, flash); green-gold |
| **Fire** | the mana skin (a warm crimson-orange skin instead of the old near-black one); draconic scales on the arms, pauldrons, chest, small flame wings then the full pair of FLAME WINGS; one helix of flame, a ring, embers, eyes | **Fuegoleon**: plus the FLAME MANE (flame tongues streaming back from the head), the cape of flame, the sun-of-flames sigil, three helices of flame ribbons, orbiting rune rings, the entry; the right arm is Fuegoleon's molten arm (section 6) instead of a scaled one; crimson-gold |
| **Water** | the Valkyrie armour (lighter: no greaves or vambraces) at the half stage, the Saint Valkyrie Dress at the full stage (feather wings, gold halo, sash), a ripple ring, drops | **Noelle**: plus the water RIBBONS from the shoulders, the TRAIN of the dress, the gyroscope HALO of three water rings (instead of the gold halo), feathered sleeves, the wave sigil, helices of water, the entry; blue-silver |
| **Earth** | mossy stone plates, a ROCK CROWN with five floating amber crystals, the floating rocks at the full stage, a vein layer of amber crystal veins over the body, dust bits, a stone ring | (none: the earth spirit is never named) |

Level of detail (`SpiritDiveTier.lod`, by the distance from the camera): up to 24 blocks the whole look; up to 48 blocks no small detail pieces and the cheap effects (the character tier keeps its signature
pieces); up to 64 blocks only a ring on the ground; beyond that nothing. At most **three** dives per frame draw the character tier (`SpiritDiveFrame.admitCharacter`, a per frame budget): the rest of a crowd
of Yunos draws the standard tier. The viewer's `spiritDive.characterEffects = false` draws every dive as standard.

## 5. How the look is drawn (the engines it uses)

* The layer is the existing `Aura.SPIRIT_DIVE` (`client.aura.SpiritDiveAura`): pass 1 the translucent mana skin, pass 2 the geo pieces (each forged in and shattered out by `EquipAnimation`, glow maps additive),
  pass 3 the additive haze and the new vein layer, pass 4 `SpiritDiveFx`. The pieces are GeoLite models made by `tools/gen_spirit_dive_v2.py` on the shared kit of `tools/great_spirits_lib.py`; the new
  shapes (blades, flames, ribbons, prongs) are flat boxes whose texture carries the silhouette, a bright rim and a glow map.
* `SpiritDiveFx` draws only flat or camera-facing sprites with `AuraRender.additive` (the SafeAdditive rules: sprites that are black under their alpha; `SafeAdditiveTextureScanTest` is green), in
  layer space, from the numbers of `SpiritDiveTier.budget`. One scratch quaternion, textures in statics: nothing is allocated per frame.
  The only per frame objects are the small `LazyBuffers` consumers the aura layer hands out for every buffer a painter asks for (the design of `PlayerAuraLayer`, shared by every aura; about a dozen per
  diving player per frame); `SpiritDiveRemakeScanTest` pins that no painter method of the remake builds a vector, quaternion, location, string or collection.
* The client particle trail (`DiveParticles`) goes through `Fx` and so through `PerfBudgets.clientParticles`, the effect quality and the distance bands.
* The colour grading per tier is in `SpiritDiveTier.glow / primary / accent` (pure, tested).

## 6. Fuegoleon's flaming right arm

**Who and when (the server decides).** `book/FuegoleonArm` checks every player once every 40 ticks (staggered by entity id). The arm is on when ALL of: the server switch `spiritDive.fuegoleonArm` is on,
the player is alive, the bound grimoire is Fuegoleon's, and at least one of: the Salamander contract is held (`SpiritLordSkill.bondedType` = "Salamander": the Spirit Lord skill or the bond kind of the Spirit
Charm), the Salamander (small or big form) is out (`FireCasting.salamanderOut`), or the Fire Spirit Dive runs. It goes (the lease is cleared at the next check, at once on death) when none holds: the spirit
was released, the bond was lost, the grimoire is not Fuegoleon's any more. The decision is the pure `FuegoleonArmRules.reason`. At the start of a Fire dive the arm lease is sent immediately.
A one line message ("The Salamander's flame runs down your right arm.") appears when it first lights.

**How it reaches the clients.** One `Aura.FUEGOLEON_ARM` lease (100 ticks, renewed every 40): the existing per-player aura sync (`PlayerAuras`, keyed by player UUID, dropped at logout), one small packet to
the players that track the wearer, at most 0.5 per second per Fuegoleon plus one at every change of state. Style 1 = at rest (the contract is held), 2 = the Salamander is out or the dive runs. The cast
flare is NOT sent: the client reads the cast animation it already has (`DiveCast` -> `CastAnimClient.oneShotActive`).

**What it looks like.** Third person (`FuegoleonArmAura`): the 3D model `spiritdive_arm_flame_arm_r` (a shoulder flare, an upper arm, an elbow, a forearm and a fist of living flame: a bright orange skin cut by
thin dark red cracks, glowing, the cross-section turned into an eight-pointed star so it reads round, with flame tongues that sway, rising from the shoulder and elbow and falling from the fist); the
frame-animated flame sheets round it (`textures/aura/sdv_flame_sheet.png`, four frames, about eleven a second, crossed planes so they work in the hand too); embers rising off the arm; a soft heat-haze glow
behind it (a real refraction cannot be done with the mod's render types: it is a breathing glow, said plainly); and four ribbons of fire streaming off the shoulder (the "fire mane" of the reference).
The flames leap during a cast and burn highest during the Fire dive. First person (`FireArmFirstPerson`): for an empty hand the arm is drawn inside the game's own arm draw (`RenderArmEvent`), for a hand
that holds an item at the same hand pose the Djinn gauntlet uses (`RenderHandEvent`); only the RIGHT arm, only while the arm is on, nothing else of the hand is changed.

**Settings.** Server: `spiritDive.fuegoleonArm` (default true). Client (`nusmp-spiritdive-client.toml`): `fuegoleonArm.view` = `OFF`, `FLARE` (only while casting, diving or with the Salamander out) or
`ALWAYS` (default: also at rest, a quieter flame). Not for anyone whose grimoire is not Fuegoleon's: other Fire users get the standard Salamander look.

**3.1: the default look of the arm is now the Djinn-style flame limb** (a sculpted limb of flame forged in piece by piece, `docs/magic/fuegoleon_arm_djinn.md`); the arm described in this section is kept as the client option `fuegoleonArm.look = V3` (and `CLASSIC` for the plain molten model).

## 7. Settings

| File | Key | Default | Effect |
|---|---|---|---|
| `nusmp-spiritdive-server.toml` (SERVER) | `spiritDive.characterTier` | true | false: every dive is sent as the standard tier |
| | `spiritDive.fuegoleonArm` | true | false: the arm is never turned on |
| `nusmp-spiritdive-client.toml` (CLIENT) | `spiritDive.classicLook` | false | true: the 1.99 look, exactly as it was |
| | `spiritDive.characterEffects` | true | false: every dive is drawn as the standard tier |
| | `spiritDive.particles` | true | the particle trail |
| | `fuegoleonArm.view` | ALWAYS | OFF / FLARE / ALWAYS |

## 8. Worst-case costs (rule 11: a change that cannot be shown to be bounded does not ship)

Every number is a hard bound from the code, pinned by `SpiritDiveTierTest`, `FuegoleonArmRulesTest` and `SpiritDiveRemakeScanTest` (the scan reads the real model files and sums the cubes: 24 vertices a
cube, two passes, body and glow).

**Client, per diving player, per frame, at full detail (up to 24 blocks):**

| | pieces (vertices) | mana skin (4 passes, at most 12 boxes) | effects (quads / vertices) | total (vertices) | cap |
|---|---|---|---|---|---|
| standard tier, worst element (Water full) | 6480 (Wind full 4992, Fire full 3840, half 5184, Earth full 3216) | 1056 | 42 quads = 168 | about 7700 | 8500 (`SpiritDiveTier.vertexCap`) |
| character tier, worst (Yuno full 6096; Noelle 6000; Fuegoleon 5088) | 6096 | 1056 | 130 quads = 520 | about 7700 | 10500 |
| Fuegoleon's arm (own aura) | 19 cubes = 912 | - | 42 quads = 168 | about 1080 | - |

The effect budget of one dive (`SpiritDiveTier.budget`, the painter loops over exactly these fields): standard FULL = 4 light columns (8 quads), 6 motes, 4 bits, 3 hair strands (12), 1 helix (8), 1 ring
(2), 2 flares = 42 quads; character FULL = 8 columns (16), 2 pillar planes (4), 18 motes, 12 bits, 9 strands (36), 3 helices (24), 7 rings (14), 6 flares = 130 quads. At 24-48 blocks the standard tier
draws 2 columns + 2 motes + 1 ring and the character tier 4 columns, 6 motes, 3 strands, 1 helix, 3 rings and 2 flares; at 48-64 blocks both draw one ring; beyond 64 nothing. At most 3 dives per frame
draw the character tier. The effects of far players are skipped. **Client particles:** at most 6 dives within 24 blocks per tick, one particle every third tick for a standard dive and two a tick for a
character dive: at most 12 per tick (240 a second) before the client particle budget (`clientParticlesPerTick`, 400) and the quality and distance bands thin them; one burst of 8 / 28 particles on the
tick after a dive begins.

**Measuring:** the remake adds four sections to the performance probe (`/multiverse perf`): `c.spiritdive.paint` (the remade dive of one player), `c.fuegoleon.arm` (the arm of one player),
`c.spiritdive.particles` (the trail, once a tick) and `s.spiritdive.arm` (the server check, only on a wearer's own turn); `SpiritDiveRemakeScanTest` keeps them in place.

**Server, per player:** the tier costs one grimoire NBT read a second per DIVING player. Fuegoleon's arm costs, for every player, one `instanceof` and one modulo per tick, and once every 40 ticks one
grimoire-slot read (every non-Fuegoleon player stops there; a Fuegoleon player also reads the spirit bond, the firecast state map and the aura table). No entity is spawned, no chunk is loaded, nothing is
scanned. **Network:** the dive adds no packet (the grade rides inside the style number of the lease the dive already sent every second); the arm adds at most 0.5 packets a second per Fuegoleon plus one
per change of state, sent to the players that track him only. **Cleanup:** the lease table (`PlayerAuras`) is dropped at logout; the arm lease is cleared on death and expires 5 seconds after the server stops renewing it.

## 9. What each part is tested by

* `SpiritDiveTierTest` (pure): the tier follows the canon grimoire and the element and never borrows another character's look; the style number round-trips (48 combinations), the 1.99 numbers are unchanged,
  a bad byte never throws; the budgets are ordered and bounded; the levels of detail cut at 24 / 48 / 64 blocks and never improve with distance; the colour grading is green / crimson-gold / blue-silver;
  the particle trail is bounded; the piece lists (signature pieces belong to their character, Yuno's first dive stays left-only, the upgrade forges only what is missing).
* `FuegoleonArmRulesTest` (pure): the reason matrix (switch, grimoire, alive, contract / link / dive), the styles, one check per wearer per 40 ticks and an even spread over a crowd, at most 0.5 packets a
  second, what the client draws for OFF / FLARE / ALWAYS.
* `SpiritDiveRemakeScanTest` (reads the repo): every piece list under its vertex cap; no client class in the common classes; the painters allocate nothing and build no location or string in their frame
  methods; no hand-made additive render type; every texture, model, animation clip, particle theme and lang key the code names exists; the classic look is still there; no hunger.
* Self-tests (CI server smoke test): **`magic.spirit_dive_looks`** (every file exists, the particle themes exist; with a stand-in player: no canon grimoire = standard, Yuno's on Wind = Yuno, Fuegoleon's on
  Fire = Fuegoleon, Noelle's on Water = Noelle, Fuegoleon's on Wind and Yuno's on Fire = standard) and **`magic.fuegoleon_arm`** (no arm without the contract; the arm with it, the lease sent; none for
  another character's grimoire or without a grimoire; gone when the contract changes; on for the Fire dive; the renewal cost).
* The existing tests of the dive (`SpiritDivePiecesTest`, the armour policy tests, the geo and asset tests, the perf scan tests, `ServerSafetyTest`, `ConfigFileNamesTest`, the particle / texture / lang tests)
  still pass; `python3 -B tools/safe_additive/scan_textures.py` reports no offending texture.

## 10. Previews (`docs/previews/spirit_dives/`, rebuilt by `python3 -B tools/spirit_dive_previews.py`) and the iteration log

All of them are the headless rasteriser: translucent and additive passes are approximated, there is no bloom, no sky, and the camera is the harness's.

| File | What it shows |
|---|---|
| `wind_yuno.png`, `fire_fuegoleon.png`, `water_noelle.png`, `earth.png` | per element: the 1.99 look, the standard tier, the character tier (front and back); Earth: front, back, half |
| `half_dives.png` | the first (half) dives of every element, standard and character |
| `entry_yuno.png`, `entry_fuegoleon.png`, `entry_noelle.png` | the entry of the character tier at ticks 3, 8, 14 and 22 |
| `fuegoleon_arm.png`, `fuegoleon_arm_states.png` | the flaming arm from three sides, and at rest / Salamander out / casting (the cast flare is switched on in the harness with `SDV_CAST=1`; in a Fire dive the flare is always at its highest) |
| `fuegoleon_arm_first_person_approx.png` | **MOCK / APPROXIMATION**: a camera behind the right shoulder with the arm swung forward. The real first person hand (`FireArmFirstPerson`) cannot be rendered by the harness (it needs the game's hand pass); this picture only shows what the model and flames look like seen along the arm |

Iteration against the owner's two screenshots (each round = changes, a render of every scene, a look at the sheets):

1. **First render.** Yuno: the new blades were big and radiating, but pointed FORWARD over the face (the yaw sign of a bone in these files is the opposite of the fire feathers'); the honeycomb wings and the
   blades overlapped. Fuegoleon: the mane was huge and pale yellow, the near-black fire skin hid the body, the cross-hatched wings were busy. Water and Earth were fine; the Earth crystals floated out of frame.
2. **Orientation and palette.** Blades swept back (negative yaw), fan opened up (rolls -80 to -28), the fire skin replaced by a warm translucent one, flames re-coloured amber-red instead of white-yellow,
   the mane made smaller and brought down behind the head.
3. **Helices and the missing ribbons.** The strands (hair) and the new helices were invisible: the sprite faded to nothing along its length and a long helix only showed its bottom end. The wisp sprite was
   redrawn, every segment now a comet streak with a bright head, the helix thickened; flame wings replaced the cross-hatched wings in the remade fire dive; the ground ring colours follow the character.
4. **Over-exposure.** Stacked additive layers turned the Fuegoleon tier white at close range: glow multipliers of the flame pieces lowered, columns and back sigil alpha lowered, the flame body alpha raised
   so the flames stay orange on a bright background, one mane tongue and one wing tongue removed; the sigil behind the back added (Yuno's circle of connected crosses is behind his back in the series).
5. **The arm.** First version: translucent, so it showed the skin underneath as a pale peach block, and boxy. Now an opaque body at every strength (only the flames and the glow follow the strength),
   a second turn of every segment at 45 degrees (an eight-pointed section), flame sheets tinted orange, the "at rest" strength raised to a visible quiet flame.
6. **Close range.** White-out of the hand flares and columns at 3 blocks reduced; the dive stopped drawing a second copy of the arm (the arm aura draws it, in the dive too).

## 11. What could not be verified (honest list)

* **Nothing was seen in a running game.** All pictures are the harness; the final look of translucency, bloom, the particle themes and the distance behaviour is unknown.
* **The first person hand** (`FireArmFirstPerson`: the placement for an empty hand and for a held item, the camera basis of the flames in the hand) is written from the game's own hand code and the Djinn
  gauntlet's placement, not run. `FireArmFirstPerson.ENABLED = false` switches it off in one line.
* **The cast flare** reads `CastAnimClient.oneShotActive` (public, existing); in the harness it is a stub (`SDV_CAST=1` shows the flare).
* **The heat shimmer** is a soft breathing glow, not a distortion.
* **The per frame cap of character tiers** counts frames with `RenderLevelStageEvent` (AFTER_SKY), the way the 3D compass does; if that event does not arrive every dive is admitted (distance still applies).
* **Alex (slim) arms:** the molten arm is narrowed to 80 percent about the arm's pivot like the other arm pieces; not looked at.
* **Spirit contracts by other means:** the arm follows `SpiritLordSkill.bondedType` and `FireCasting.salamanderOut`; a player given the Salamander by another route that sets neither is not covered.
* **Light, Darkness and Space spirits** have no Spirit Dive in the mod (their packages have links and abilities only), so there is nothing to remake for them.
* The file `src/main/resources/data/nusmp/particle_themes.json` is written by `tools/particles/gen_themes.py` (seven themes appended); if another change also edits the generator, re-run it after the merge.
