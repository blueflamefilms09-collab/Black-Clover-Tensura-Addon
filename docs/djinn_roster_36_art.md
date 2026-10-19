# The art of the 24 new Djinn (1.93.0): Bael to Murmur

Twenty-four Djinn were added after Vassago (rules, traits, Extreme Magics and weapon acts: `docs/djinn_roster_36.md`). This page is their **visuals**: the full-equip
armour set of each (light and Dark Djinn), the in-hand 3D weapon of each (light and dark twin), the Seal, the skins, the ambient particles and the dust palette. Code
that draws them: `client/relics/djinn/DjinnRoster36` (+ `DjinnRoster36Fx`); art tools: `tools/relics/djinn36_*.py`; previews: `docs/previews/djinn36/`.

## NEW / REPLACED / KEPT (owner rule 5)

| Part | Status | What |
|---|---|---|
| Armour sets `djinn/u_<id>/`, `djinn/d_<id>/` for the 24, their skins, glow skins, runes, dark veins, dark busts | NEW | every set has its own helm, horns / ears / crest, chest emblem, pauldrons, forearm and greave accents, wings (4 pieces), tail (18 of 24), halo (light set), back piece, mantle (15 of 24), halo in the light set (20 of 24), weapon piece; the Dark Djinn set adds the eye crest, the ring of thorns and black rukh on the wings |
| Shared-rig fallback `djinn/<id>/` (+ `<id>_armor.png`, `_rune`, `_bust`) for the 24 | NEW | the first twelve's old rig, built for the new Djinn from their `Look` so the fallback chain (`DjinnLook.chain`) and `DjinnAssetsTest` hold; it is drawn only if a unique set is missing |
| 24 Seals (`djinn_seal_<id>`), 24 light and 24 dark weapons (mesh, skin, glow skin, model json, icon model, 16 x 16 icon) | NEW | the Seal has a glyph of its own Djinn on the round seal; every weapon is a real 3D mesh in the hand with a glow layer and keeps a flat inventory icon (owner rule 7) |
| Motion row, ambient particles and dust palette of the 24 | NEW | `DjinnRoster36` (name-keyed, generated from `tools/relics/djinn36_roster.py`); `DjinnLook.style` / `ambient` fall back to it for ordinals past Vassago |
| The first twelve's meshes, skins, weapons, seals, motion rows, particles | KEPT | not one file of theirs changed (checked with `git status`) |
| `tools/relics/djinn_weapons.py` | KEPT, extended | its tables `DJINN`, `TARGET`, `NAMES`, `ACTS` now list all 36 (additive block at the end); the first twelve's outputs are byte-identical (`--check`) |
| Anything REPLACED | none | nothing the mod already had was swapped out |

## How they plug in

The loaders are name-keyed (`DjinnLook.dir/skinPath/runePath/bustPath` build every path from `Djinn.id()`), so the art needs no code: the files are named exactly like the
first twelve's and `DjinnMeshes` finds them once the enum has the entry. The forge-in and shatter of the full equip are `DjinnArmorLayer.assembled` and `part`, unchanged:
each piece is anchored on the model part it was built for, staggered by its own delay, flies in while it fades up and grows, and shatters out over the last ticks. The
glow pass is the additive `eyes` layer of each piece, the body-art is `<id>_u_rune` / `<id>_d_rune`, the fresnel aura is tinted by the Djinn's own glow colour.

The rule of the tail (`DjinnLookTest`): a Djinn has tail segments exactly when its `Look.tail` is not NONE. Six Djinn have no tail (Bael, Gusion, Buer, Dantalion, Crocell,
Furcas): no tail piece is built for them and their motion row has 0 segments; `DjinnRoster36.styleOf` also enforces it from the enum if a Look changes later.

## The 24 full equips

| Djinn | Head | Wings | Piece behind the back | Weapon in hand |
|---|---|---|---|---|
| Bael | crown of nine spires under two crossing arches with an orb, cat ears, plumes | rank of swallow-tailed royal standards on a gilded bone | the high back of a throne: pillars, pointed double arch, upholstered panel, sun-rune | claymore with a crown for a guard |
| Agares | crocodile snout across the brow, eyes on the crown, dorsal scutes and spines | dorsal sails: barbed spines, scalloped membrane, glowing veins | open crocodile jaws round the shoulders | harpoon with two tooth-set barbs and a coil of rope |
| Valefor | thief's hood with a trailing scarf, long ass's ears, a bolt at each temple | storm-torn cloak: ragged strips with lightning | three great keys crossed round a heart of storm | pair of crossed wavy kris with stolen hearts |
| Barbatos | great antlers, a crown of oak leaves, moss hair | boughs: bark bones, forked branches, oak leaves, berries | horn-bow arching over the shoulders, seven arrows from a quiver | recurve bow of living wood with antler tips and a nocked arrow |
| Gusion | crown of five standing glass panes, a mirror on the brow, a breaking wave at each temple | fan of mirror panes with glowing frames | the tide mirror: a ring of silver wave-scrolls round a pane of light, surf at its foot | rapier of silver water with a mirror-disc guard |
| Vepar | coral crown, a nautilus shell at each temple, long fin ears, seaweed hair | manta wings: a broad membrane, ribs of light, a whip at the tip | a ship's wheel with eight turned handles and a rope of light | ship's anchor with a chain |
| Shax | a dove's wings spread above the brow, a veil sewn over the mouth | short broad dove's wing: rounded primaries, rows of coverts, an alula | wreath of olive leaves with three doves in flight | pair of crossed war fans of nine white feathers |
| Buer | a small wheel on the crown, herb sprigs, leaf ears, beaded braids | fronds of healing herb: spines with marching leaf pairs | the great five-spoked wheel with leaves at the spoke ends and healing crosses | staff with a serpent and a five-spoked wheel of herbs |
| Andrealphus | aigrette of three stalks with tufts, a fan of feather crown, swept plumes with eyes | a peacock's train: long plumes ending in eyes of light | the peacock fan: eleven plumes with eyes | lance with a star head and three eyed plumes |
| Kimaris | steed's face plate with glowing eyes, tall horse ears, flaming mane | bat wings: bone fingers, scalloped membrane, fire fringe, wrist claw | a great horseshoe with glowing nails and two pennons | cavalry lance with a hand-guard cone and a fire-edged pennon |
| Amdusias | a spiral unicorn horn on the brow, deer ears, a tuning fork at each temple | harp wings: a silver frame, strings of light, a spray of feathers | organ pipes fanned behind the shoulders with a lyre crest | rapier whose blade is a spiral horn, tuning-fork guard |
| Belial | a crown that is only its frame (three hoops floating), horns curling into hoops | wings of nothing but their frame: bone fingers, outline, struts, shreds | the throne with nobody in it: thin pillars, arch, empty crown, open shackles | greatsword whose blade is an outline round a glowing hollow |
| Decarabia | carapace cap with a star on the brow, eyes on stalks, pincers at the temples | overlapping carapace plates with four jointed crab legs | a great pentagram in a ring with crab legs | pair of crab pincers opening about a star |
| Seere | small wings at the temples, a swept hair streamer | narrow scythe-wings of seven long primaries | a pinwheel of six wind blades round an eye of calm | swift curved sabre with a feather guard and a ring pommel |
| Dantalion | flat scholar's cap, a face on the back of the head and at each temple | fan of loose pages with script of light | five tomes riding a ring round a great rune | staff topped with a great chained tome |
| Andromalius | hood with a third eye on the forehead and a serpent coiled round it | lattice of thin bone with watching eyes | one great eye with rays, a ring of small eyes | spear with a serpent and an eye in the blade |
| Glasya-Labolas | hound's skull over the brow, folded ears, collar of spikes, swept horns | fan of curved blades on bone fingers, toothed | a ring of fangs (the maw) with three slashes of red light | slaughter fangs: three great claws on a spiked fist bar |
| Haagenti | bull's horns with bronze caps, goggles on the brow, a nose ring | bronze riveted blade-feathers with a turning cog at the joint | an alembic: glass flask of glowing spirit, swan-neck, coil, vial, three cogs | great bronze cleaver with a toothed back, bull-horn guard, flask pommel |
| Crocell | crown of standing ice, an icicle beard, crystal horns | crystal spines with branching crystals like a snowflake | a great snowflake | staff with a head of ice crystals about a snowflake |
| Furcas | open sallet with nasal bar, a frozen plume, a long white beard | a torn knight's cloak with icicles along its edge | a kite shield with a cross of light, two lances crossed behind | halberd with a pennon of the ice wind |
| Balam | three small faces on the brow (ram, man, bull), lyre horns, ram curls, flame crown | three plumes of fire, each a fan of four blades with a flame between | an arch of fire carrying three great faces | trident with a flame on each prong and three faces on the crossbar |
| Alloces | brow plate with two lion's eyes, a mane of locks and flames, round ears | a mane of fire: curling locks with a flame along each | the sun of a mane: a disc, sixteen flame rays, a ring | longsword with a lion-face guard and a mane at the pommel |
| Camio | crow's beak hood, a ruff of black feathers, feather tufts | a crow's wing: eight spread primaries with curled tips | a murder of seven crows circling a small ring, notes of light | staff ending in a raven's quill with a crow perched on it |
| Murmur | skull mask with a drumstick standing from each temple, dreadlocks with bones | ribs of bone with drumskins stretched between them | the great bass drum with a skull, two small drums, two mallets crossed | mace whose head is a drum with a skull skin and a ring of flanges |

Every set also has: a chest emblem of its own on a cuirass or harness, shoulder pieces that match the head, forearm and greave accents of the family (`djinn36_lib.FAMILY36`), the
Half Equip pieces (the brow crest, the main-hand pauldron, vambrace, gauntlet) and a Dark Djinn variant (black metal and cloth, gold trim, the Djinn's accent in the crystals).

## Motion, particles, frame cost

`DjinnRoster36` gives each Djinn a motion row (wing beat, tail segments, halo spin, back-piece spin and breathing, weapon size, tail sway: all 24 rows differ), an ambient
recipe (two particle keys with rates, and where they are born; all 24 recipes differ from each other and from the twelve) and a **dust palette**: a coloured dust particle
(the Djinn's glow fading into its body colour; a Dark Djinn's saturated colour fading into black violet) that moves as its affinity moves (heat rises, water falls, wind
swirls, strength sinks, sound spreads in rings, light twinkles, lightning jitters). `Frame` is the quads drawn in a Full Equip (tail counted five times) in the light and
the dark set; the budget is 5200.

| Djinn | Weapon kind | Weapon length (blocks) | Ambient (main + second, born at) | Frame light / dark | Tail |
|---|---|---|---|---|---|
| Bael | claymore | 3.1 | mote 9/s + spark 3/s, above | 3980 / 4412 | no |
| Agares | harpoon | 3.4 | bubble 8/s + ash 3/s, feet | 3461 / 4133 | yes |
| Valefor | daggers | 2.3 | crit 9/s + spark 4/s, wings | 3680 / 3972 | yes |
| Barbatos | bow | 2.7 | petal 7/s + feather 3/s, wings | 4278 / 4690 | yes |
| Gusion | rapier | 2.9 | drip 7/s + mote2 4/s, above | 4002 / 4506 | no |
| Vepar | anchor | 3.0 | bubble 11/s + snow 3/s, wings | 4467 / 4872 | yes |
| Shax | fans | 2.3 | feather 9/s + enchant 5/s, above | 3952 / 4389 | yes |
| Buer | staff | 2.9 | spore 8/s + petal 4/s, body | 3977 / 4509 | no |
| Andrealphus | lance | 3.3 | mote2 10/s + enchant 5/s, wings | 4365 / 4777 | yes |
| Kimaris | lance | 3.5 | smoke 9/s + flame 4/s, feet | 3195 / 3851 | yes |
| Amdusias | rapier | 2.6 | note 4/s + mote 7/s, above | 3825 / 4251 | yes |
| Belial | greatsword | 3.2 | ash 8/s + smoke 5/s, above | 3424 / 3968 | yes |
| Decarabia | pincers | 2.3 | drip 5/s + shard 5/s, feet | 3812 / 4234 | yes |
| Seere | sabre | 2.8 | wind 12/s + snow 3/s, feet | 3716 / 4280 | yes |
| Dantalion | staff | 2.8 | enchant 11/s + ink 3/s, above | 3699 / 3967 | no |
| Andromalius | spear | 3.2 | ink 7/s + crit 6/s, body | 3575 / 3799 | yes |
| Glasya-Labolas | claws | 2.3 | ember 10/s + ash 5/s, body | 3094 / 3742 | yes |
| Haagenti | cleaver | 2.7 | flame 6/s + smoke 5/s, hand | 4044 / 4684 | yes |
| Crocell | staff | 3.0 | snow 12/s + shard 3/s, wings | 4293 / 4673 | no |
| Furcas | halberd | 3.4 | snow 7/s + wind 5/s, body | 2971 / 3487 | no |
| Balam | trident | 3.1 | flame 8/s + mote 4/s, above | 4166 / 4686 | yes |
| Alloces | longsword | 3.0 | ember 9/s + spark 4/s, wings | 4277 / 4825 | yes |
| Camio | staff | 2.8 | feather 6/s + note 3/s, wings | 4058 / 4336 | yes |
| Murmur | mace | 2.8 | spark 6/s + note 3/s, hand | 4062 / 4456 | yes |

## Weapons

The kinds, names and acts are the rules table's (`djinn36_weapon_data.py` is a copy of it for the tools). Each mesh is derived from the armour set's own weapon piece by
`djinn_weapons.py` (the same code as the first twelve): scaled to its target length under the size rule (2.2 to 3.6 blocks; the short kinds, daggers, fans, pincers and
claws, are drawn at 2.3 so they meet the 2.2 floor of `DjinnWeaponAssetsTest`), oriented with the head at -x, `orient manual`, white glow so the painted glow skin shows
as painted, `place` on the sprite diagonal, the hand holding it where the armour layer holds it. The model json and the 16 x 16 icon come from the same tool. The Dark
twin is the dark set's weapon piece in black metal and gold. The effects of a weapon in hand are its glow layer (pulsing as the other weapons' do), the particles of its
Djinn and the dust palette; the crimson void layer of the Genesis Demon-Slayer is **not** used (`DjinnWeaponAssetsTest` allows only the body and glow layers), so Belial's
hollow blade, Kimaris' lance and Glasya-Labolas' fangs carry their dimension in the glow layer instead.

## Item ids (72)

`djinn_seal_bael`, `djinn_weapon_bael`, `djinn_weapon_bael_dark`, `djinn_seal_agares`, `djinn_weapon_agares`, `djinn_weapon_agares_dark`, `djinn_seal_valefor`, `djinn_weapon_valefor`, `djinn_weapon_valefor_dark`, `djinn_seal_barbatos`, `djinn_weapon_barbatos`, `djinn_weapon_barbatos_dark`, `djinn_seal_gusion`, `djinn_weapon_gusion`, `djinn_weapon_gusion_dark`, `djinn_seal_vepar`, `djinn_weapon_vepar`, `djinn_weapon_vepar_dark`, `djinn_seal_shax`, `djinn_weapon_shax`, `djinn_weapon_shax_dark`, `djinn_seal_buer`, `djinn_weapon_buer`, `djinn_weapon_buer_dark`, `djinn_seal_andrealphus`, `djinn_weapon_andrealphus`, `djinn_weapon_andrealphus_dark`, `djinn_seal_kimaris`, `djinn_weapon_kimaris`, `djinn_weapon_kimaris_dark`, `djinn_seal_amdusias`, `djinn_weapon_amdusias`, `djinn_weapon_amdusias_dark`, `djinn_seal_belial`, `djinn_weapon_belial`, `djinn_weapon_belial_dark`, `djinn_seal_decarabia`, `djinn_weapon_decarabia`, `djinn_weapon_decarabia_dark`, `djinn_seal_seere`, `djinn_weapon_seere`, `djinn_weapon_seere_dark`, `djinn_seal_dantalion`, `djinn_weapon_dantalion`, `djinn_weapon_dantalion_dark`, `djinn_seal_andromalius`, `djinn_weapon_andromalius`, `djinn_weapon_andromalius_dark`, `djinn_seal_glasya_labolas`, `djinn_weapon_glasya_labolas`, `djinn_weapon_glasya_labolas_dark`, `djinn_seal_haagenti`, `djinn_weapon_haagenti`, `djinn_weapon_haagenti_dark`, `djinn_seal_crocell`, `djinn_weapon_crocell`, `djinn_weapon_crocell_dark`, `djinn_seal_furcas`, `djinn_weapon_furcas`, `djinn_weapon_furcas_dark`, `djinn_seal_balam`, `djinn_weapon_balam`, `djinn_weapon_balam_dark`, `djinn_seal_alloces`, `djinn_weapon_alloces`, `djinn_weapon_alloces_dark`, `djinn_seal_camio`, `djinn_weapon_camio`, `djinn_weapon_camio_dark`, `djinn_seal_murmur`, `djinn_weapon_murmur`, `djinn_weapon_murmur_dark`

Textures: `textures/item/<id>.png` for each; models `models/item/<id>.json` (Seal: generated item model; weapons: builtin/entity with the standard display) and
`models/item/<id>_icon.json` for the weapons; weapon meshes `weapon_meshes/<id>.txt`, skins `textures/entity/weapon/<id>.png` and `_glow.png`. Names:
`tools/lang/djinn36_art.json` (the same names the rules builder put in `en_us.json`).

## Edits to existing files (all additions)

* `client/relics/djinn/DjinnLook.java`: `style(int)` and `ambient(int)` ask `DjinnRoster36` for an ordinal past their tables instead of returning the fallback at once (two three-line blocks).
* `client/relics/djinn/DjinnAmbient.java`: one line at the end of `emit`: `DjinnRoster36Fx.emitPalette(...)`.
* `tools/relics/djinn_weapons.py`: an additive block (`DJINN +=`, `TARGET.update`, `NAMES.update`, `ACTS.update`) before `lang_fragment`.
* `tools/item_preview/preview_weapons.py`: reads `qn` lines (per-vertex normals), which it did not before, so the Djinn weapons can be previewed.

New files: `client/relics/djinn/DjinnRoster36.java` (pure tables, no Minecraft types), `DjinnRoster36Fx.java` (the dust), `tools/relics/djinn36_*.py`, `tools/lang/djinn36_art.json`,
the assets above, `docs/previews/djinn36/`.

## Tools

`python3 -B tools/relics/djinn36_build.py [--djinn bael,agares] [--only armor|base|skins|seals|weapons|lang]` writes everything (idempotent, about 8 s a Djinn);
`--preview DIR` renders a mannequin sheet per Djinn (half equip, full light front and back, the weapon, full dark front and back, half dark) and `--icons DIR` the icon
sheet; `tools/relics/djinn36_java.py` writes `DjinnRoster36.java` from the table; `tools/relics/djinn36_check.py` is the CI check (files, budget, tail rule, distinct rows,
Java in step, `djinn_weapons.py --check`); `python3 tools/item_preview/preview_weapons.py djinn_weapon_bael ...` previews a weapon as the renderer places it.
The armour is built with the first generation's `mesh_lib` (real cross-sections: lofts, sweeps, lathes) through the same atlas painter, so each piece has box-free UVs, a
painted skin per material and an emissive glow map; `geo_check.py` checks Bedrock geo models and does not apply to these baked mesh files.

## Not verified

Nothing was run in the game. The previews are the mesh library's software rasteriser posing the pieces like `DjinnArmorLayer` does: they show shapes and colours, not the
real lighting, the animation (wing beat, tail wave, forge-in) or the dust. The tests that need only files and pure classes were run with the real 36-entry enum
(`DjinnLookTest`, `DjinnUniqueAssetsTest`, `DjinnWeaponAssetsTest`, `DjinnAssetsTest`): all pass except `DjinnLookTest.theDarkAuraIsRedVioletAndStillTellsTheDjinnApart`,
because Baal (`0xBFE4FF`) and Andrealphus (`0xBFE3FF`) have glow colours one level apart, which `DjinnLook.darkAura` (22% of the glow) cannot tell apart; that is a colour
of the roster table, not of the art (a glow of `0xB8E0FF` for Andrealphus, or a looser test, resolves it).
