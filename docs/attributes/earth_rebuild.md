# Earth Magic: the 1.93.0 rebuild

Earth Magic (`MagicType.EARTH`, book `EarthBook`, colour `0xFFB08850`, Gnome spirit) is the core earth magic: soil, mud, grass and roots. Unlike its kin it was never a generic wiki book, so the rebuild is additive: the six existing pages stay exactly as they were (ids, order, mastery bits, code), three pages are appended, and the armour, the Clay Guardian and the effects are new. Shared family code: `EarthKit`, `EarthArmorKit`, `EarthFamilyLayer`, `EarthPropPainter`.

## 1. NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| Pages `spike_line`, `wall`, `mother_earth_split`, `boulder_shot`, `witch_hunter_claws`, `rampaging_mother_earth` | KEPT (code unchanged; only an extra earth-family effect and, on the signature, the armour flash are added) | Earth Spikes, Mud Wall Partition, Divided Mother Earth, Boulder Shot, Witch Hunter Claws, Rampaging Mother Earth |
| Pages `earth_golem`, `earth_armor`, `sinkhole` | NEW, appended after the six | |
| Temporary armour | NEW | the Gnome Plate (`earth_*` pieces, style 100 on `Aura.WIKI`); Earth had no armour before |
| Construct | NEW | Clay Guardian (`earth_golem`) |
| Effects | NEW, beside the old `EarthSpellLayer` shapes (`STONE_SPIKES`, `EARTH_RISE`, `EARTH_FISSURE`, `EARTH_CLAWS`, `EARTH_RAMPAGE`, which stay) | `EARTHFAM_*` in the earth palette: the claws stand on a gnome sigil, the signature has a burst |
| `EarthBook` overrides | NEW | `onDamageEntity` / `onTakenDamage` so the Gnome Plate's thorns and melee bonus work (the book is not an `ElementBook`) |
| Sprites | NEW | `earthfam_earth_*` (clod, blade, root, circle, dust) |

## 2. Canon

The existing pages already use canon names (Mud Wall Partition, Divided Mother Earth, Rampaging Mother Earth, Witch Hunter Claws; the Earth Spirit is Gnome). The Black Clover wiki pages for Earth Magic were not reachable in the build session, so no further spell could be read. The new pages are the nearest honest versions: a golem of soil (Earth Magic shapes earth into constructs), an earthen armour, and a sinkhole (the ground opening under a target, as in Divided Mother Earth but as a trap that closes again). The look (packed soil, grass tufts, roots, a seed-core) is invented from the material.

## 3. Armour: the Gnome Plate

| Piece | Anchor | Shape |
|---|---|---|
| helm | head | a soil dome with grass on top, grass tufts and two root horns |
| chest | body | layered soil with a grass shoulder line, clods, a gold seed-core, roots hanging below |
| belt | body | hanging root tendrils |
| mantle | body | a grass-covered yoke and roots and blades down the back |
| pauldron_r / _l | arms | a soil mound with grass blades and a pebble |
| vambrace_r / _l | arms | packed earth sleeves with a grass cuff and root spurs |
| gauntlet_r / _l | arms | a clod fist with a grass top |
| greave_r / _l | legs | earthen boots with a grassy knee cap |
| back | body | a gold-glowing seed-core in two crossed rings of clods, turning in opposite directions (8 s) |
| extra | head | root antlers with gold buds, swaying |

Materials `earth` and `grass` (pebbles, roots, grass on top faces and fringes), glow `0xD2E65A`.

## 4. Pages

| Id | Name | Tier | What it does |
|---|---|---|---|
| earth_golem | Clay Guardian | mid | the golem for 25 s (+EP): follows, slams the nearest foe with both fists (9 x size, r 2.6, lift, Slowness II) |
| earth_armor | Gnome Plate | mid | the armour for 10 s: Resistance II, thorns, +3 melee, 6 percent aura |
| sinkhole | Swallowing Ground | zone | the earth opens under the target (or the aim) r 2.6: 6 and rooted (Slowness IX, controlTicks 50, hard control), then 24 ticks later closes with 9 and a lift; bosses are held for less and lifted less |

The signature `rampaging_mother_earth` now also forges the Gnome Plate for 5 s (it only ever applies when no armour is on).

## 5. Construct

`earth_golem`: 43 cubes, a colossus 44 px tall; bones pelvis, two legs (thigh + shin), chest, head, two arms (arm + forearm); clods and grass, glowing gold eyes and core; clips idle, walk, attack (both arms raised then slammed). Painter extras: motes of soil lifted off the shoulders.

## 6. Self-tests and checks

As the Sandstone doc, plus `EarthFamilyRebuildTest.earthBookKeepsItsSixPagesFirst` (page order of the book read from its source) and the self-test `magic.earth_family_rebuild.books` (the six ids first, then the three). Scenes: `ef_earth_close`, `efm_earth_golem_*`, `efp_golem`, `efx_earth_*`.

## 7. NOT verified

Nothing was run in the game. Specific to Earth: the interplay of the new `onTakenDamage` with the book's spirit hooks, the golem's pathing over uneven ground, whether the older effect layer and the new burst overlap too busily on the claws.
