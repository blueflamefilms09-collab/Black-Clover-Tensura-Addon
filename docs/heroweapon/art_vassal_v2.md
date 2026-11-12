# Vassal weapons, model pass 2 (3.79.0)

Base looks of the Vassal weapons built by `tools/heroweapon/art_hart3.py` and `tools/heroweapon/art_hart4.py`.
All ten were remodelled: the five fighting weapons first, then the other five (carriage, book, mirror, instrument, sewing set).
Sheets (the scripts' own previews, before on the left, after on the right):
`docs/previews/vassal_weapons/before_after_katana_scythe_fan.png`, `before_after_harpoon_chain.png`, `before_after_carriage_book.png`, `before_after_mirror_instrument_sewing_set.png`.

## What changed, per weapon

| Weapon | Quads before | Quads after | Parts | What changed |
|---|---|---|---|---|
| `vassal_katana` | 1761 | 2061 | 10 -> 12 | Broader, thicker, more curved blade with a long kissaki; a dark groove (bo-hi) that glows together with the hamon; a larger sakura guard with wider inlays; gold langets on both faces at the blade base (new part `blade_langets`); a pink cord tied under the guard with three tassels beside the hilt (new part `sageo`, inside the weapon's own length); thicker hilt, larger gems with a stronger glow. |
| `vassal_scythe` | 2160 | 2203 | 14 | Much heavier hooked blade (wider, longer, with a counter-blade heel), four fangs on the spine band, a bright violet glowing edge, larger gem, plate and halo, thicker snath, wraps, collars and cuff, bigger butt disc with longer spikes, bigger plume. The mesh is 8% taller (233.5 -> 253.0 model cm); the apparent length in hand is the same class size, so the snath reads slightly shorter. |
| `vassal_fan` | 2429 | 2359 | 12 | A war fan: midnight-blue leaf grading to violet at the rim, 21 steel blade teeth on the rim, heavier ribs and guards ending in points, a larger pivot plate and gem, a lit cyan rim and thread, brighter blossom branch. |
| `vassal_harpoon` | 1954 | 2156 | 12 | Broad leaf head (half width 19.5), long swept barbs plus a second smaller barb pair on the socket, a wider lightning bolt vein and a lit edge, larger gem with claws, thicker shaft, bands, butt and socket. Grip line unchanged (0.243). |
| `vassal_chain` | 1866 | 2384 | 10 -> 11 | Heavier links (the first seven glow, fading along the chain), a morning-star weight (larger ball with lit engraved seams, eight spikes and a tip spike), larger gem in a bigger bezel, larger clasp and pommel, a blackened knuckle bow with three studs (new part `knuckle_bow`, kept out of `grip_cap` so the hand point stays: grip line 0.081 -> 0.082). |

| `vassal_carriage` | 1847 | 2397 | 14 (same names) | Twelve heavier spokes (was eight) and a larger hub with a stronger amber hub light, a gold mudguard arching over each wheel with three stays and a lit amber line, a crown of eight prongs and a ring round the roof jewel, bigger roof jewel and lantern jewel with a stronger glow, longer and wider plumes. The mesh contract is kept: length 120, width over the wheels 72, canopy height, wheel diameter, body length, seat points, exactly the 14 part names (`CarriageMeshContractTest` ok; the new gold is in the existing `posts` part). |
| `vassal_book` | 2037 | 2477 | 14 (same names) | Bigger sapphire (radius 5.0, dome 3.6) and star, a second ring where the star's points start and longer prongs, larger corner caps with a domed boss each, a second finer gold frame, six spine bands plus a broad gold cap at head and foot, a wider strap, gilt page edges, stronger glow on the star, frame, gem and page block. The loose pages and the cover size are unchanged (they already fill the item cube). |
| `vassal_mirror` | 2508 | 2546 | 14 (same names) | A heavier head (frame 12.6 x 18.0 instead of 11.2 x 16.4, rim 4.2 x 2.6), larger pearls, bigger crest petals (the middle one 17.5 cm), larger gem and claws, bigger volutes, a fatter fluted handle, collar and pommel, a lit ring round the glass (the bezel glows; the pearls' faint light paid for it), stronger glass glow. |
| `vassal_instrument` | 2321 | 2586 | 14 (same names) | A body 12 % broader, thicker glowing strings (warm light, was almost none), the purfling inlay of the top plate lit, a larger gem with larger gold rings and claws, three gold collars on the neck, a thicker bow stick, a softly glowing bow hair. |
| `vassal_sewing_set` | 1784 | 2359 | 11 (same names) | A heavy needle (shaft radius 2.3 instead of 1.25) with a bigger thick eye and a lit shaft, a bigger spool (flange radius 8.0 instead of 6.5) with lit silver rims, a bigger thimble pommel, a larger gem with eight silver claws, a thicker lit thread plus a lit silk helix wound round the shaft, larger scissors. The needle point stays at the same height (length 122.6 cm). |

## NEW / REPLACED / KEPT

- NEW: the classic copies of the ten old looks (in the `classic/` folders, read when the client setting `classicHeroWeaponModels` is on) (mesh, skin, glow skin; exact git copies of the files before this pass), so one
  renderer switch can bring the old look back. They are not wired to any setting yet.
  - `weapon_meshes/classic/vassal_{katana,scythe,fan,harpoon,chain,carriage,book,mirror,instrument,sewing_set}.txt`
  - `textures/entity/weapon/classic/vassal_{...same ten...}.png`
  - `textures/entity/weapon/classic/vassal_{...same ten...}_glow.png`
- REPLACED: mesh, skin and glow skin of all ten Vassal weapons.
- KEPT: every id and file name, the 16x16 sprites, both item models of every weapon (display transforms untouched), the size
  classes (`classes_hart3.json`, `classes_hart4.json`), all shared tool libraries.

## Cost

Client only, no server work, no packets. Worst case 2586 quad lines for one weapon (instrument); the test cap is 2800. Skins stay 512 px.

## Still to do outside the art scripts (shared files)

- `tools/weapon_grip/grip_golden.json` was regenerated after the pass (katana, scythe, chain and mirror grips moved slightly).
- The new skins are RGBA as the scripts write them; the committed ones were palette-shrunk by a later commit. Run the texture
  shrink again if the jar size matters.
- The item models were left as committed. A fresh script run would write a display scale 0.3% (harpoon) and 0.5% (chain) smaller.

## Not seen in a game

Everything here was judged on the scripts' preview renders only: not the first- and third-person hold, not the glow in the
dark, not the gem state looks or upgrade tiers drawn on top of these meshes.
