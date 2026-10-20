# Red Ochre Magic: the 1.93.0 rebuild

Red Ochre Magic is iron-red pigment and fired clay (`MagicType.RED_OCHRE`, colour `0xFFD64C37`). A wiki attribute with the generic five-page grimoire; rebuilt with clay armour, a painted totem, effects and spells (shared family code: `EarthKit`, `EarthArmorKit`, `EarthFamilyLayer`, `EarthPropPainter`). Where the other five are stone, Red Ochre is smooth, painted and tribal: red clay, pale-ochre bands, glowing glyphs.

## 1. NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| Temporary armour | REPLACED | 14 geo pieces (`red_ochre_*`) replace the old wiki shards shell for this magic |
| Construct | NEW | Ochre Totem (`red_ochre_totem`) |
| Effects | REPLACED | `EARTHFAM_*` in the ochre palette |
| Pages 0-4 (`attribute_*`) | KEPT ids, order, mastery bits; behaviour REPLACED | |
| Pages 5-6 (`ochre_totem`, `war_paint`) | NEW, appended | |
| Sprites | NEW | `earthfam_red_ochre_*` (splat, hand, stripe, sun, drop) |

## 2. Canon

The Black Clover wiki lists Red Ochre Magic as an attribute; its page was not reachable from the build session, so no user and no spell name could be checked, and none is claimed. The build follows the material: ochre is a pigment (the spells paint: weaken, mark, make glow) and baked clay (the armour, the totem). Marking and sapping are the nearest honest version of "paint on the enemy".

## 3. Armour: Clay armour

| Piece | Anchor | Shape |
|---|---|---|
| helm | head | a smooth dome with a painted brow band, two big curved horns, pale horn tips |
| chest | body | a smooth plate with a glowing sun glyph, painted bands, pigment drips |
| belt | body | a sash of hanging clay beads |
| mantle | body | a plume of clay feathers fanning down the back |
| pauldron_r / _l | arms | rounded domes with a ring of small spikes |
| vambrace_r / _l | arms | banded sleeves with painted rings |
| gauntlet_r / _l | arms | a clay mitten with a handprint glyph and finger ridges |
| greave_r / _l | legs | smooth boots with painted stripes |
| back | body | the sun disc: an eight-pointed star of two turned squares with a glyph core and a ring of rays, turning (12 s) |
| extra | head | five clay beads orbiting the head |

`ochre` and `ochre_pale` materials (smooth fired clay, painted bands and dots, glyph pixels in the glow map), glow `0xFF7A3C`, style 10 on `Aura.WIKI`.

## 4. Pages

| Id | Name | Tier | What it does |
|---|---|---|---|
| attribute_bolt | Red Ochre Bolt | starter | a pigment bolt, bursts r 1.2 for 9, Weakness 3.5 s |
| attribute_cut | Red Ochre Manifestation | mid | one brush stroke along the line (9 blocks, width 0.9): 10, Glowing 5 s, Weakness 3 s |
| attribute_field | Red Ochre Field | zone | a pool of pigment r 4.5 for 4 s: 4 every 16 ticks, Weakness, Slowness II |
| attribute_domain | Red Ochre Domain | signature, cd 45 s | war-paint burst r 7: 15, every foe marked (Glowing 8 s), Weakness II 5 s, clay armour 5 s |
| attribute_guard | Red Ochre Guard | mid | clay armour 9 s: Resistance II, thorns, +3 melee, 6 percent aura |
| ochre_totem | Ochre Totem | mid | a totem where you aim (two at most), 25 s, solid and breakable: every 28 ticks it spits pigment at the nearest foe in line of sight: 6, Weakness, Glowing |
| war_paint | War Paint | mid | 12 s: +5 melee, Speed I, the clay armour forges, foes within 8 blocks are marked; 5 percent aura |

## 5. Construct

`red_ochre_totem`: 34 cubes; a pedestal, three stacked painted faces (glyph glow eyes), a beak with a `jaw` bone, two wing fans, a turning sun crown; clips idle (crown turns, wings sway) and attack (jaw opens, wings flare). The painter hangs a glowing handprint above it and drops pigment off it.

## 6. Effects and render layers

Cast: a blob of pigment with falling drops; zone: sun sigil on a stained floor and glowing handprints stamped round the rim; burst: a pigment splat on the ground, drops flung in long arcs. Render types as in the Sandstone doc.

## 7. Self-tests and checks

As the Sandstone doc (scenes `ef_red_ochre_close`, `efm_red_ochre_totem_*`, `efp_totem`, `efx_red_ochre_*`).

## 8. NOT verified

Nothing was run in the game. Specific to Red Ochre: the totem's line-of-sight test and its solid hitbox against players walking into it, the weakness and glowing stacking on bosses (halved in code, not observed), how the handprint billboard reads in daylight.

## 1.99.0 armour policy

Red Ochre Magic has no armour in the anime, so since 1.99.0 **no cast puts the temporary armour of this document on the caster** (policy kind NONE). Everything described above as the armour (the render layer, its painter, geo models, textures and generator) is KEPT on disk and registered in `AuraRegistry`, so old saves and tests still resolve; it is simply never started by a page. What the pages did besides the armour (damage cut, thorns, resistance, constructs, effects) is unchanged. Where the armour carried the magic's look, every cast of a family page now sends a small `EARTHFAM_ZONE` glow that follows the caster instead (no armour piece is drawn). See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
