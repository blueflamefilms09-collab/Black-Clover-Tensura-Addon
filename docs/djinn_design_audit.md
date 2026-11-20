# Djinn design audit

Written by `python tools/djinn/design_audit.py` (do not edit by hand; run it again after a change). It reads the 36 Djinn of `relics/djinn/Djinn.java`, their armour meshes (`djinn/<id>`, `u_<id>`, `d_<id>`), weapon meshes, skins, seals, tiny Djinn skins, ambient particles, weapon table (`docs/djinn_roster_36.json`) and compares them. Rules of the comparison are in the script header.

* Duplicates found: **0** (after the rework of this release; the list of what was reworked is below)
* Concept mismatches found: **0**
* Findings looked at and accepted (reason given): **8**
* Shared parts of the fallback rig (`djinn/<id>/`, never drawn while `u_<id>` exists, one template by design): 178 pairs, not counted

## What the audit led to (REPLACED: the old file stays, `djinnLookClassic` brings it back)

| what | Djinn | how |
|---|---|---|
| Lesser Summon bust (light) | balam, haagenti, vinea, alloces, furfur, furcas, amdusias | new <id>_bust2.png with its own headgear, element mantle and glyph (tools/djinn/bust2.py) |
| Lesser Summon bust (dark) | all 36 | new <id>_d_bust2.png: one figure per Djinn instead of one black figure with a different gem (tools/djinn/bust2.py) |
| tiny Djinn model | vepar, barbatos | new tinydjinn_<id>_v2 (geo, light and dark skins, glow maps) from tools/tiny_djinn/specs_v2.json |
| Extreme Magic damage concept | focalor | water -> wind in djinn_extreme_types.json (the Warden of Gales throws a Seven-Wind Cyclone) |

## Duplicates (same design twice)

None.

## Concept mismatches (look / abilities / weapon against the aspect)

None.

## Looked at and accepted

| kind | djinn | finding | why it stays |
|---|---|---|---|
| asset colour | amdusias | weapon skin is painted hue 19 but the Djinn's colour is hue 284 (95 degrees apart) | the Alicorn Rapier is an ivory horn (low saturation orange) with a lilac grip: the pale unicorn horn is the point of the weapon |
| concept | camio | painted armour skin leans hue 218, outside the SOUND range | crow-black plumage with a blue sheen: Songbird of the Crows is dark navy by lore, its sound comes from notes, quills and the crow wing |
| concept | kimaris | painted armour skin leans hue 301, outside the STRENGTH range | the plum and rose of the dark steed's fire; Kimaris's Extreme is a black-fire charge (concept dark) and its colour says so |
| extreme concept | camio | Extreme Magic hits as 'dark' but the Djinn is SOUND (expected sound) | its Extreme is a murder of crows diving (fear), drawn with the dark damage type; the Djinn keeps Sound for its look and its notes |
| extreme concept | haagenti | Extreme Magic hits as 'wind' but the Djinn is STRENGTH (expected force) | its Extreme is a cauldron of bronze wind (a vortex): concept wind is what it draws |
| extreme concept | kimaris | Extreme Magic hits as 'dark' but the Djinn is STRENGTH (expected force) | its Extreme is a charge of black fire: concept dark is the right one |
| palette | baal | body colours 11.5 dE apart, glows 10.4 dE (LIGHTNING vs LIGHT: two elements one colour) | Andrealphus is a peacock: its blue is plumage, its element is carried by gold stars, eyes, the divine VFX and the light weapon, Baal's by bolts, black iron and lightning VFX |
| palette | paimon | body colours 11.2 dE apart, glows 10.9 dE (WIND vs WATER: two elements one colour) | Paimon is pale sky cyan with a near-white glow and feathers, Vepar deep wreck teal with coral and brass; the element is in the VFX, the weapon and the motifs |

The Djinn panel's medallion silhouette is drawn from (horns, wings, ears): 6 of the 26 silhouettes are shared by two or three Djinn. They still differ in halo, tail, palette and accent (no two Djinn share horns, wings, tail and halo: checked here), so the panels are not copies; nothing was changed.


## Every Djinn

| # | Djinn | aspect | colour / glow | horns, wings, tail, halo | ambient | weapon (acts) | Half Equip, Extreme | duplicates | mismatches |
|---|---|---|---|---|---|---|---|---|---|
| 0 | Baal | Lightning | #3C8CFF / #BFE4FF | crescent, blade, whip, spokes | spark/mote | lance (thunder_throw, judgement_step) | chain, baal bolg: judgement of thunder | - | - |
| 1 | Amon | Heat | #FF5A14 / #FFD27A | ram, fire, flame, ring | ember/smoke | greatsword (sun_cleave, ember_wall) | melt, amon salos: crown of the sun | - | - |
| 2 | Vinea | Water | #2D9CDB / #B5F0FF | crescent, veil, tendril, runes | bubble/drip | trident (tide_spear, frost_surge) | freeze, vinea zalg: the drowning sea | - | - |
| 3 | Focalor | Wind | #62D6A0 / #E6FFF1 | spike, feather, whip, ring | wind/feather | glaive (cyclone_sweep, updraft_leap) | push, focalor zaleph: seven-wind cyclone | - | - |
| 4 | Zagan | Life | #58C75A / #E4FFB8 | stag, feather, tendril, runes | petal/spore | staff (bloom_heal, thorn_snare) | lifesteal, zagan genesis: bloom of life | - | - |
| 5 | Zepar | Strength | #D43A3A / #FFB9A0 | ram, membrane, blade, none | shard/ash | axe (titan_slam, rock_breaker) | crush, zepar fang: titan's fall | - | - |
| 6 | Furfur | Sound | #B77BFF / #F1DDFF | crown, veil, tendril, runes | note/enchant | horn (resonance_wave, dissonance) | deafen, furfur requiem: chord of ruin | - | - |
| 7 | Marchosias | Heat | #E23A22 / #FFA878 | spike, membrane, flame, none | ember/flame | claws (ember_rush, brand) | brand, marchosias hunt: emberfang run | - | - |
| 8 | Sitri | Light | #FFE070 / #FFFFFF | crown, feather, none, ring | mote/mote2 | bow (dawn_arrow, radiant_burst) | smite, sitri dawn: radiance of first light | - | - |
| 9 | Bifron | Lightning | #7B4DFF / #D2C2FF | stag, blade, blade, spokes | spark/crit | glaive (chain_lightning, storm_pillars) | paralyze, bifron skyfall: pillars of storm | - | - |
| 10 | Paimon | Wind | #4FC3D6 / #DFFAFF | crescent, feather, whip, runes | feather/snow | glaive (crescent_wind, heavenly_dash) | slice, paimon tempest: heavenly gale | - | - |
| 11 | Vassago | Water | #2F55D8 / #B8CCFF | ram, membrane, tendril, none | drip/ink | scythe (abyssal_reap, drowning_pull) | drown, vassago maelstrom: abyssal tide | - | - |
| 12 | Bael | Strength | #C9A227 / #FFF0A0 | crown, blade, none, spokes | mote/spark | claymore (royal_decree, throne_quake) | kneel, bael thronus: edict of the first king | - | - |
| 13 | Agares | Water | #1FA37A / #B8FFE0 | ram, veil, blade, none | bubble/ash | harpoon (tidal_lunge, undertow) | drag, agares maw: jaws of the rising tide | - | - |
| 14 | Valefor | Lightning | #5A3DB5 / #E0D2FF | spike, blade, whip, ring | crit/spark | daggers (spark_snatch, storm_heist) | plunder, valefor larceny: storm-heart heist | - | - |
| 15 | Barbatos | Life | #2E7D32 / #CFFFB0 | stag, veil, whip, none | petal/feather | bow (wild_arrow, green_ambush) | hunt, barbatos venatio: the wild hunt | - | - |
| 16 | Gusion | Water | #3D7EAA / #D6F2FF | crescent, veil, none, runes | drip/mote2 | rapier (mirror_thrust, tide_mirror) | foresight, gusion speculum: verdict of the tide mirror | - | - |
| 17 | Vepar | Water | #00A6B8 / #BFFCFF | none, veil, tendril, ring | bubble/snow | anchor (anchor_drop, shipwreck) | wreck, vepar naufragium: wrecker's surge | - | - |
| 18 | Shax | Sound | #8E8EA8 / #F0F0FF | none, feather, whip, ring | feather/enchant | fans (wing_hush, muted_flurry) | hush, shax silentium: hush of the white dove | - | - |
| 19 | Buer | Life | #9CCC3B / #F4FFC0 | stag, feather, none, ring | spore/petal | staff (wheel_mend, cleansing_spokes) | mend, buer rota: the wheel of mending | - | - |
| 20 | Andrealphus | Light | #2F6FDB / #A8D4FF | crown, feather, whip, spokes | mote2/enchant | lance (star_spear, peacock_fan) | dazzle, andrealphus pavo: fall of a hundred stars | - | - |
| 21 | Kimaris | Strength | #3A2A3F / #FF6A8A | spike, membrane, whip, none | smoke/flame | lance (charge_lance, dark_gallop) | trample, kimaris equus: charge of the dark steed | - | painted armour skin leans hue 301, outside the STRENGTH range |
| 22 | Amdusias | Sound | #D58AF0 / #FFE3FF | spike, feather, tendril, runes | note/mote | rapier (horn_note, grand_chord) | shockwave, amdusias concerto: the unicorn's last chord | - | - |
| 23 | Belial | Heat | #7A0F2E / #FF7096 | crown, fire, flame, none | ash/smoke | greatsword (hollow_cleave, throne_ash) | cinder, belial inanis: fall of the hollow throne | - | - |
| 24 | Decarabia | Light | #E0B45A / #FFF4C8 | none, membrane, blade, spokes | drip/shard | pincers (pincer_snap, star_shell) | shell, decarabia astra: star-shell nova | - | - |
| 25 | Seere | Wind | #E8E8C0 / #FFFFFF | none, feather, whip, spokes | wind/snow | sabre (swift_slash, gale_step) | quicken, seere celer: passage of the swift wind | - | - |
| 26 | Dantalion | Light | #C0A0FF / #F2E8FF | crown, veil, none, runes | enchant/ink | staff (forbidden_page, many_faces) | beguile, dantalion facies: lecture of a thousand faces | - | - |
| 27 | Andromalius | Lightning | #2B2B6B / #9CA8FF | stag, blade, tendril, runes | ink/crit | spear (seeker_bolt, unveil) | reveal, andromalius inquisitio: thunder finds the hidden | - | - |
| 28 | Glasya-Labolas | Strength | #A00000 / #FF9090 | ram, membrane, whip, none | ember/ash | claws (savage_rend, bloodhound_rush) | bloodlust, glasya-labolas cruor: the slaughter howl | - | - |
| 29 | Haagenti | Strength | #B87333 / #FFD9A0 | crescent, feather, whip, none | flame/smoke | cleaver (bull_gore, great_work) | transmute, haagenti opus: the great work | - | - |
| 30 | Crocell | Water | #9ADCF0 / #FFFFFF | crown, veil, none, ring | snow/shard | staff (frost_bolt, rime_field) | rime, crocell glacies: the sage's frozen tide | - | - |
| 31 | Furcas | Wind | #AAB7C8 / #FFFFFF | stag, blade, none, ring | snow/wind | halberd (ice_wind_sweep, knights_gale) | icewind, furcas boreas: charge of the ice wind | - | - |
| 32 | Balam | Heat | #D1622A / #FFE0A0 | ram, fire, whip, spokes | flame/mote | trident (triple_flame, prophecy) | triple, balam trias: prophecy of three flames | - | - |
| 33 | Alloces | Heat | #E8892B / #FFF0B0 | crown, fire, whip, ring | ember/spark | longsword (lion_slash, lions_roar) | roar, alloces leonis: the lion's judgement | - | - |
| 34 | Camio | Sound | #2A3340 / #A0C8FF | none, feather, blade, runes | feather/note | staff (crow_flock, murder_of_crows) | carrion, camio corvus: a murder of crows | - | painted armour skin leans hue 218, outside the SOUND range |
| 35 | Murmur | Sound | #6B3FA0 / #E8C8FF | crescent, membrane, tendril, spokes | spark/note | mace (death_drum, funeral_march) | drumbeat, murmur tympanum: the death drum | - | - |
