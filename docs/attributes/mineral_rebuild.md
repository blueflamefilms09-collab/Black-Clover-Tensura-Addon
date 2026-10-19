# Mineral Magic: the 1.93.0 rebuild

Mineral Magic is ore and crystal grown out of the rock (`MagicType.MINERAL`, colour `0xFF78A8C8`). A wiki attribute with the generic five-page grimoire; rebuilt with crystal armour, spires, thrown lances, effects and spells (shared family code: `EarthKit`, `EarthArmorKit`, `EarthFamilyLayer`, `EarthPropPainter`). It is the only magic of the six with several palettes in one look: blue-steel quartz, amethyst violet, malachite green and pyrite gold crystals in a grey-blue ore matrix, with faceted bright glow.

## 1. NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| Temporary armour | REPLACED | 14 geo pieces (`mineral_*`) replace the old wiki shards shell for this magic |
| Constructs | NEW | Mineral Spire (`mineral_spire`), the crystal lance of Mineral Bolt (`mineral_lance`) |
| Effects | REPLACED | `EARTHFAM_*` in the mineral palette |
| Pages 0-4 (`attribute_*`) | KEPT ids, order, mastery bits; behaviour REPLACED | |
| Pages 5-6 (`mineral_spire`, `lance_volley`) | NEW, appended | |
| Sprites | NEW | `earthfam_mineral_*` (shard, flare, facet, circle, ring) |

## 2. Canon

The wiki lists Mineral Magic as an attribute; its page was not reachable in the build session, so no user and no spell name could be read or are claimed. The magic is built as the crystal-and-ore magic its name says, kept apart from Crystal Magic (its own grimoire) by the ore matrix and the several mineral colours, and from Corundum and Glass by shape (spires and prisms instead of gems and panes). The Domain's Energy Drain (crystals resonating and taking magicules) is a design choice, not canon.

## 3. Armour: Crystal armour

| Piece | Anchor | Shape |
|---|---|---|
| helm | head | an ore-matrix cap with a tiara of five fanned crystals and three rising from behind |
| chest | body | matrix plates with a geode: a nest of amethyst and quartz crystals in an opening, crystal teeth below |
| belt | body | a row of short crystal teeth |
| mantle | body | a fan of nine quartz and five amethyst prisms across the back |
| pauldron_r / _l | arms | a matrix base with a cluster of five crystals of different size and colour, two spurs outward |
| vambrace_r / _l | arms | matrix sleeves with four crystal fins sweeping back |
| gauntlet_r / _l | arms | crystal knuckles forward |
| greave_r / _l | legs | matrix shins with crystal spurs at the knees and ankles |
| back | body | two crystal wings, each a fan of six long prisms, opening and closing a few degrees (5 s) |
| extra | head | six crystal shards orbiting the head (9 s) |

Materials `crystal` / `amethyst` / `malachite` / `pyrite` / `matrix` (facet bands, a luminous core, ore flecks), glow `0x78DCFF` (violet on the amethyst), style 3 on `Aura.WIKI`. The crystals are boxes aimed with `aim()` in `tools/gen_earth_family_armor.py`.

## 4. Pages

| Id | Name | Tier | What it does |
|---|---|---|---|
| attribute_bolt | Mineral Bolt | starter | a crystal lance prop at 1.9 blocks per tick for 30 ticks: pierces every foe on its line once (9 x size each, up to 12, Slowness II), bursts where it ends (5) |
| attribute_cut | Mineral Manifestation | mid | a row of crystal erupts along the line: 8 points, 10 each, small lift |
| attribute_field | Mineral Field | zone | the crystal garden: three spires (up to five with EP) around the aim for about 10 s, each pulsing 5 x size every 24 ticks in r 3.4 with Slowness II |
| attribute_domain | Mineral Domain | signature, cd 45 s | resonance burst r 7: 16, and when the control lock is free every foe loses 2 percent of its magicules to you (`EnergyBridge.drain`); crystal armour 5 s |
| attribute_guard | Mineral Guard | mid | crystal armour 9 s: Resistance II, thorns, +3 melee, 6 percent aura |
| mineral_spire | Crystal Spire | mid | one spire (1.2 x size) where you aim, two at most, 15 s, solid and breakable |
| lance_volley | Lance Volley | mid | five lances in a 40 degree fan |

## 5. Constructs

`mineral_spire`: 16 cubes; a matrix base with rubble, one 40 px quartz prism, an amethyst prism and a ring of six satellites on their own bones; clips grow (scale from nothing with a small overshoot, 0.55 s), idle (satellites sway), pulse. The painter adds glints and a turning lattice ring on the floor. `mineral_lance`: 7 cubes, a 40 px quartz prism with an amethyst tail and four spurs, spinning.

## 6. Effects and render layers

Cast: a crystal lance with glints; zone: crystals standing on the rim and a hex-lattice sigil; burst: crystals grow out of the middle, a ring of small crystals stands on the ground. The burst's puff is drawn additive here (an alpha hexagon read as a grey block in the preview). Render types as in the Sandstone doc.

## 7. Self-tests and checks

As the Sandstone doc (scenes `ef_mineral_close`, `efm_mineral_spire_*`, `efp_spire`, `efp_lance`, `efx_mineral_*`). The armour's vertex count was 6192 in one frame in the preview before the mantle and wing fans were thinned (budget note: keep under 6000); it is 5952 after (the heaviest of the six sets; Earth 5232, Red Ochre 4800, Stone 3792).

## 8. NOT verified

Nothing was run in the game. Specific to Mineral: the lance's pierce list (kept per prop id in a map, removed when the prop ends), the energy drain amounts on real Tensura mobs, the glow brightness of the crystals in daylight, how the crystal wings look from the first-person back view (they are behind the camera).
