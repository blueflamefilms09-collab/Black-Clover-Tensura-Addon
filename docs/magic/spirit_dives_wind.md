# Spirit Dive, Wind: the PACK look (Yuno's version and the basic Wind user)

The Wind element of the default (REEL) Spirit Dive look, redone from the owner's reference pack of Yuno's Spirit Dive. Standard tier = every Wind mage with the Sylph bond; Yuno tier = the mage whose
bound grimoire is Yuno's. Client option `spiritDive.windLook = PACK | REEL` (default `PACK`; `REEL` brings back the Wind of 3.3 unchanged). Nothing here is server work: no packet, no state, no new key
bind; what the server sends (the style number: element, stage, grade) is untouched.

Previews: `docs/previews/spirit_dive_wind/` (`wind_std.png`, `wind_yuno.png`, `yuno_weapons.png`, `close.png`, `entry_std.png`, `entry_yuno.png`, `half_dives.png`), each next to crops of the pack
(`references/`) and, for the full stage, the REEL Wind of 3.3 as "BEFORE". They are renders of the repo's software harness (`tools/render_preview`), **not game screenshots**: no bloom, additive
and translucent passes approximated, the arms hang because the harness has no animation.

## Sources (rule 10)

The pack itself (read with my own eyes, no wiki fetched this round): `Spirit_Dive.png`, `Spirit_of_Zephyr.png`, `images1.jpg` (Boreas bow), `images.jpg` (another mod: scale hint only), the five small
manga panels (Boreas, Saint Boreas, Saint Zephyr, Euros, Notos; the Japanese captions read: スピリット・オブ・エウロス, スピリット・オブ・ノトス, 風精霊創成魔法) and the contact sheet of the AMV.
No wiki page was needed or fetched; nothing could not be fetched.

## What the pack shows and what is drawn (Wind only)

| Pack | Drawn |
|---|---|
| a pale green-white light over the whole body, clothes and skin still visible | the body shell of the ANIME look in pale green / white (wash, flowing streaks, bright rim, soft hull), growing from the feet to the head on entry. The head only gets a third of the wash |
| a **crown of tall vertical bars with arrow-tip tops** on a thin ring floating over the head | a thin square ring band 10.6 px over the head and 8 (standard) / 14 (Yuno) bars with arrow tips, 5 to 7.6 px tall, each turning about the vertical to face the viewer; Yuno also has a second, lower ring. Replaces REEL's cube crown for Wind |
| ONE huge smooth **leaf-shaped wing** from the shoulder blade, swept up and back, an ornate cross at its tip | one big leaf (40 px long, 17 px wide) on the left shoulder blade with a cross sprite at 80 percent of its length, a smaller leaf (19 px) on the right; Yuno both sides get both (4 leaves, 12 percent bigger). Smooth sheet: pale fill, bright edge, midrib and veins. Unfolds with an overshoot on entry. Replaces REEL's hex-membrane leaves |
| a **circle of connected crosses and branches** round the wrist of the outstretched hand | a ring of 6 crosses joined by thorned branches, in the plane across the wrist and a copy that always faces the viewer (so it reads from every side). Standard: the left hand; Yuno: both. Replaces REEL's cube gauntlets |
| Yuno: a circle of crosses behind the back | Yuno only: a 31 px circle of 8 crosses, two rings and an inner ring behind the shoulders, turning slowly |
| Yuno: the long glowing **sword of Spirit of Zephyr** with crossed circles at the hilt and sparkles | a 46 px blade with fuller, cross guard and cross pommel, the three crossed ellipses and rings of the guard turning at the hilt, drawn out of the circle on entry. Sword at rest |
| Yuno: the **leaf bow of Spirit of Boreas** with cross ornaments and an arrow of light | the bow (leaf limbs, cross ornaments, thin string) held out front on the left and an arrow of light flying out of it, **while a page is being cast** (see decisions) |
| white loops of wind, four-point sparkles | 2 (standard) / 3 (Yuno) soft white ribbons looping round the body, 4 / 8 four-point sparkles drifting up |
| entry: flash, white ribbons, assembling | kept from REEL: flash and bloom, ring running out, ribbons spiralling up, the cage of circles (mint instead of cream), the body assembling, then crown, leaves and weapon in 22 ticks; the exit is the same clock backwards |

The first dive of Wind (`Stage.HALF`) is the left half: the left shoulder's big leaf, the left wrist circle, the half crown (left half of the ring and the bars with x >= 0) and the left arm and leg of the
body light. An upgrade keeps the left half on and forges the right one in. Colours are pale green, white and mint only (no blue; a test pins it).

## NEW / REPLACED / KEPT (rule 5)

- **NEW**: `SpiritDiveWind` (painter), `SpiritDiveWindRules`, `SpiritDiveWindGeometry` (pure), `SpiritDiveWindSelfTests`, 12 textures `sdw_*`, `tools/gen_spirit_dive_wind_textures.py`, the scene, preview and helper
  tools, the client option `spiritDive.windLook`, probe section `c.spiritdive.wind`, the tests below.
- **REPLACED (for Wind, by default; behind `windLook = REEL`)**: the REEL Wind's cube crown, cube gauntlets, hex-membrane leaves, blue-teal mosaic body and gold limb bands, the REEL sword and its circle. They
  stay registered and drawn when `windLook = REEL`.
- **KEPT unchanged**: the REEL look of Fire, Water and Earth; the ANIME, DJINN, V2 and CLASSIC looks; the server, the style number, the tiers, the entry clock and ribbons; the first-person arms; Time Magic VFX.
- Small additive edits to the REEL painter: `ribbons` and `entry` are package-private and `entry`/`cage` take the colour of the cage (REEL passes its cream, `0xFFF0C0`, as before).

## Faults of the REEL Wind against the pack, and what fixed them

1. Blue-teal block mosaic over the whole body hid the clothes and read as another colour. Now a pale green-white light over the clothes.
2. A crown of glowing **cubes** with a gold ring; the pack has a thin ring and tall arrow-tipped bars. Now thin ring plus bars.
3. Cube **gauntlets** on both forearms; the pack has none, it has the ring of crosses at the wrist. Replaced.
4. A **hex-membrane** (honeycomb) leaf, several rows, flat symmetric on both sides; the pack has one smooth sheet and a cross at the tip. Replaced by one big smooth leaf, a small one, a cross.
5. No crosses anywhere (the pack's recurring motif). Now on the leaf, the wrist, the back circle, the sword and the bow.
6. Gold bands and a cream cage: out of palette. Gold bands dropped for Wind, the cage is mint.
7. The sword was a bare strip with one circle; the pack's has crossed circles and sparkles at the hilt. The bow of Boreas was missing.

## Budget (rule 11), worst case per Wind diver

Vertices at the full stage (entry sprites included), PACK against the REEL Wind it replaces (`SpiritDiveWindRules.vertices` against `SpiritDiveReelRules.vertices`, summed by `SpiritDiveWindTest`):

| | FULL detail | MID (24 to 48 blocks) | FAR (48 to 64) | beyond 64 |
|---|---|---|---|---|
| standard | 1660 (REEL 2136) | 740 (1016) | 128 (128) | 0 |
| Yuno | 2172 (REEL 2592) | 740 (1016) | 128 (128) | 0 |

First dive (half): standard 1140 / 436 / 56, Yuno 1460 / 436 / 56. The caps of the look (3200 character, 2400 standard) and the 3.0.0 V2 worst case (7700) hold. At most three character dives per
frame (`SpiritDiveFrame.admitCharacter`, the rest draw the standard tier); the character tier is never drawn beyond 24 blocks. Steady state (after the 22 ticks of the entry) the entry sprites are gone, so
the standing dive is about 850 (standard) / 1150 (Yuno) vertices. No allocation per frame (statics, cached textures, scratch arrays, scan test), no packet (the bow follows the cast animation the client
already has), no server work, no per-player state, no camera access, timed in `c.spiritdive.wind`.

## Tests (rule 12)

`SpiritDiveWindTest` (budgets against REEL at every tier, level of detail and stage; the crown, leaves, geometry, option, weapon rule, palette), `SpiritDiveWindAssetTest` (the 12 textures: sizes, clean
under alpha 0, never blue, leaf rib, bar arrow head, rings), `SpiritDiveWindScanTest` (server safety, no allocation in frame methods, level of detail / probe / spec use, the REEL Wind and the other looks
still there, the option and tools registered). Self-test `magic.spirit_dive_wind` (files, tables, budgets, option values), registered in `SpiritDiveBoot`; **it must be added to the smoke list in
`.github/workflows/smoke.yml`** (not touched by this builder). The 143 + 83 tests of the other Spirit Dive looks, `ServerSafetyTest`, `ConfigFileNamesTest`, `PerfSourceScanTest` and
`ParticleThemesTest` still pass. No new pattern, so `PerfSourceScanTest` is unchanged.

## Not included, not verified

- **Euros and Notos** (Yuno's other wind spirit forms): the pack has only tiny manga panels (about 190 px, black and white), too little to build from; not included.
- Soft leaf and sparkle **particles** only through the existing capped particle trail (unchanged); the fur-collar glow is skipped (no mesh).
- The bow shows during **any** cast, not only Spirit of Boreas: the dive cannot know which page is cast without a packet. The two weapons swap instantly (no cross-fade).
- Never seen in a game: software render, no bloom. The wrist circle sits at the hand of the model, which hangs in the harness; in a game it follows the arm, so it should be checked with a raised arm.
