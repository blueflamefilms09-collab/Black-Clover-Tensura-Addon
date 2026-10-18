# The remodel: every custom mob as a character of the art kit

Why: the owner said "i want my mod designs based on player models besides the Old Witch mob the rest need remodeling". The Old Witch (`tools/arc/char_black_market_witch.py`, entity
`nusmp:arc_npc`) is a humanoid on the player's proportions built with the art kit (`docs/arc/art_kit.md`): a painted face, hair, an outfit, props, accessories and a full set of clips. That
is the standard. The other custom mobs of the mod were hand-built cube models, vanilla models in a re-tint, or nothing at all (a stub, an orb). This remodel rebuilds every one of them as a
kit character: a humanoid mob is a kit character as it is, a creature is a kit character with its features as an anthropomorphic design (beastfolk ears, tail and muzzle, a serpent
coiled under a humanoid body, a slime child of gel, an armoured giant ...), keeping its name, colours, lore and size class.

Status: **written, built, previewed with the kit's harness, type-checked against the API stubs, and the pure tests run through the local JUnit shim. Nothing here has run in the game.**
Section 8 lists what is unverified and what the remodel dropped.

## 1. What did not change

* Entity ids, registration, attributes, AI, hitbox size classes, boss logic, drops, spawn eggs: untouched. A mob keeps its save format.
* Only the model, the texture, the animations and the renderer changed. The old models, textures, generators and renderer classes are all still on disk (owner rule: add the new, keep
  the old). The old renderers are still registered by the lines in `NUSMP.java`; the new ones are registered LAST (lowest priority) and NeoForge keeps the last renderer registered for a
  type, so the new look is the one drawn. To go back to an old look for a mob, delete its row from `client/remodel/RemodelTable.ENTRIES`.
* Left as they are on purpose (they are meant to look like the player): `nusmp:mirror_double`, `nusmp:player_avatar`. Already kit characters before this work: `nusmp:arc_npc` (the Old
  Witch and every NPC of the arcs) and every boss registered through `ArcEntities.boss` (`char_zagred`, `char_minotaur`, `char_black_goliath`, `char_amphisbaena`, ...).
* Not mobs, not remodelled: `thread_trap`, `magic_prop`, `magicule_scale`, `carriage`, the three named corpses (`jimmy`, `alfredo`, `carl`), the altars and archways of the Labyrinth.

## 2. The table: which entity wears which model

`client/remodel/RemodelTable.java` is the one place (pure, tested). Models are `assets/nusmp/{geo,animations}/entity/arc_<model>.*` and `textures/entity/arc_<model>[_glow].png`; a
second form is `arc_<model>_form2`; a skin is a texture `arc_<model>_<skin>` over the same body.

| entity | models it draws | renderer |
|---|---|---|
| `dante_zogratis` `vanica_zogratis` `zenon_zogratis` | the man, and `_form2` (the hybrid of the Gates) | `KitBossRenderer` |
| `lucius_zogratis` | `lucius_zogratis`, `_form2` (divine) | `KitBossRenderer` |
| `zogratis_clone` | `lucius_zogratis`, `_form2` by the form the clone was made in | `KitMobRenderer` |
| `devil_avatar` | `devil_avatar`, `_form2` (Gates), skin `megicula` (and `megicula_form2`) | `KitBossRenderer` |
| `god_of_order` | `god_of_order`, `_form2` (ledger open) | `KitBossRenderer` |
| `goliath` | `goliath`, `_form2` (Gates) | `KitBossRenderer` |
| `charybdis_spawn` | `charybdis_spawn`, `_form2` (flushed) | `KitBossRenderer` |
| `dark_disciple` | `dark_disciple`, skins `lucifero megicula beelzebub astaroth` | `KitMobRenderer` |
| `blood_beast` | `blood_beast` | `KitMobRenderer` |
| `floor_warden` `labyrinth_guardian` | `floor_warden` (the Guardian is the same body scaled by its size attribute through its hitbox) | `KitMobRenderer` |
| `living_armor` | `living_armor` | `KitMobRenderer` |
| `dungeon_direwolf` `basilisk` | their own models, with a `sleep` clip | `KitMobRenderer` |
| `dungeon_slime` | `dungeon_slime` (drawn translucent) | `KitMobRenderer` |
| `spirit_lord` | `spirit_salamander spirit_undine spirit_sylph spirit_gnome spirit_black_devil` by kind | `KitMobRenderer` |
| `elemental_spirit_lord` | the first four by kind | `KitMobRenderer` |
| `wind_spirit_lord` | `spirit_sylph`, drawn larger by its hitbox | `KitMobRenderer` |
| `grimoire_daemon` | `grimoire_daemon` (lesser), `_greater`, `_arch`, `_stone` by tier | `KitMobRenderer` |
| `painted_construct` | `painted_beast painted_knight painted_giant` by kind, tinted by the paint | `KitMobRenderer` |
| `rouge` | `rouge`, with `sit` | `KitMobRenderer` |
| `tower_wizard` | `tower_wizard` | `KitMobRenderer` |
| `cotton_sheep` | `cotton_sheep`, `cotton_sheep_cook` by role | `KitMobRenderer` |
| `cotton_cloud` | `cotton_cloud` | `KitCloudRenderer` |
| `zagred_legacy` | `zagred_legacy`, `_form2` (phase 4, with the trident) | `KitZagredRenderer` |

## 3. Each mob: before, after, design

Every character file starts with a `Why:` docstring that says where the look comes from. Colours are the old models' where they had any; everything else is the game's design, not canon.

**Dante Zogratis** (`char_dante_zogratis.py`). Before: hand-built cube model `zogratis_dante_base/hybrid`. After: a broad, tan man with swept-back dark hair, a crown, crossed scars, a goatee,
green eyes, a brown tunic with wrist wraps and the red scarf (a ring and two swaying tails, `tools/remodel/lib.py scarf`). Form 2 = 100% Lucifero: violet skin, devil horns through the crown,
glowing violet eyes with fangs, a broken gravity halo.

**Vanica Zogratis** (`char_vanica_zogratis.py`). Before: `zogratis_vanica_base/hybrid`. After: a slim woman, black bob under a crown, one red eye and a black eyepatch with a gold emblem,
an open smile, a belted short red dress with a spade, black sleeves and long boots. Form 2 = 100% Megicula: crimson skin with cracks and a forehead sigil, oni horns, a halo of thorns, a
floating blood orb, long red claws (an `extra`).

**Zenon Zogratis** (`char_zenon_zogratis.py`). Before: `zogratis_zenon_base/hybrid`. After: a lean pale man, short black hair, a vertical scar through the forehead and one eye, pale eyes, a
maroon belted vest over a dark tank, wrists wrapped. Form 2 = 100% Beelzebub: grey skin, a bone crest for hair, bone spines and pauldrons, insect wing remnants, hollow indigo eyes.

**Lucius Zogratis** (`char_lucius_zogratis.py`; the clones wear it). Before: the hand-built model the kit was copied from (`zogratis_lucius_base/divine`) plus the renderer-drawn grimoire
(`LuciusBook`). After: a pale man, spiky black hair, red eyes, a sigil on the forehead, a faint smile, a white fur mantle over a dark collar, a long black cloak with a fur lining over a
brown-maroon robe with crossed straps, and a floating grimoire (the kit's `float` prop, so `LuciusBook` is no longer drawn). Form 2 = divine: pale-blue and white robe, THREE pairs of
feathered wings (the kit builds one; the other two are made in `extra` with the kit's own feather routine on the same wing bones), a halo of gears, gold stag antlers, gold eyes; the
renderer floats it as before.

**Dark Disciple** (`char_dark_disciple.py`). Before: one hooded cube model with four texture skins. After: a hooded acolyte in a skull mask, a ragged robe written with runes, a ritual dagger
and a floating orb of the devil's colour. The four devils are texture-only skins of the same body (`VARIANTS`, built by `gen_remodel.py`, 10 KB each).

**Blood Beast** (`char_blood_beast.py`). Before: a quadruped lion-wolf of blood. After: a small beastfolk of translucent-looking blood-red skin, a mane of blood thorns, a lion-wolf muzzle with
fangs (`extra`), pointed ears, a tufted tail, claws, glowing veins on the chest. Rise from the puddle = `spawn`; crouch and leap = the attack clip's wind-up and hit pose.

**Devil Avatar** (`char_devil_avatar.py`). Before: `devil_avatar_base/gates` with a Megicula skin each. After: a lanky horned devil, ragged hooded cloak, claws, a thin tail with a spade tip, a
turning halo of gears, glowing cracks. Form 2 = Gates: longer horns and a second pair, spines, a bigger halo of flames, three floating sigil tablets that circle it. Megicula skins are
texture-only (`megicula`, `megicula_form2`).

**God of Order** (`char_god_of_order.py`). Before: `god_of_order_base/gates`. After: a tall slender figure in white and gold, a porcelain full mask with two slits of light, a crown of prongs, a
cape, a halo of gears, the long measuring staff. Form 2 = the ledger open: a bigger counter-turning halo and six pages that orbit it. It also has the three clips the visit plays
(`read`, `beam`, `bow`), made from other clips of the model (`lib.pose_clip`) so the arms stay solved against the staff.

**Goliath** (`char_goliath.py`). Before: `goliath_body` with a cracked skin and a clip per move. After: a huge, widened body of pale stone stretched to six blocks, plates of masonry with moss on
the trims, a heavy brow, tiny amber eyes, boulder fists, a head drawn 2.1 times bigger so the face reads. Form 2 = every crack open and burning. The move clips (slam, stomp, roar, fist,
line, rubble) are gone: a regular move plays `cast`, the ultimate `cast_big`, the rise plays `spawn`.

**Charybdis Spawn** (`char_charybdis_spawn.py`). Before: a squid-whale leviathan. After: a huge teal-scaled titan that floats, one scarred blind eye and one cyan eye, a beak and a ring of eight
arms where a mouth would be, two long segmented feeding tentacles, dragon-membrane fins, a tail ending in a whale's fluke, barnacles and two broken harpoons (all `extra`). Form 2 = flushed:
white-hot scales, violet back, rose veins. The renderer keeps the swim pitch.

**Floor Warden and Labyrinth Guardian** (`char_floor_warden.py`). Before: the vanilla zombie model with the armour layer in a violet re-tint, scaled 1.75. After: a huge knight of empty armour:
a closed helm with one slit of light, plate in dark violet with gold seal trim, a tabard with the seal, a ragged cape, a great hammer. The Guardian wears the same body larger.

**Living Armor** (`char_living_armor.py`). Before: the zombie model with the armour layer. After: a body of wet dark-violet flesh with a pale glowing eye, packed into scavenged plate (a dented
breastplate, a big pauldron, rusty greaves) and a notched sword.

**Direwolf** (`char_dungeon_direwolf.py`). Before: a quadruped dire wolf. After: a broad, heavily furred wolf-man with a wolf muzzle and lower fangs (`extra`), tall ears, a bushy tail, a dark mane,
pale boots for paws, claws, scars, amber eyes. `sleep` = curled up (`lib.sleep_pose`).

**Basilisk** (`char_basilisk.py`). Before: a low armoured lizard. After: a lizard-folk serpent: a scaled humanoid whose legs vanish into four stacked coils of serpent body (`extra`), a wedge snout, a bone
crest for hair, venom-green eyes, a scale cuirass, a long scaled tail. Built small so the whole stays low. `sleep` = coiled down.

**Dungeon Slime** (`char_dungeon_slime.py`). Before: the vanilla slime body in a teal gel. After: a slime child of glassy teal gel with a big smiling face, a gel dome of hair with a drip, stubby limbs,
a little tail; drawn translucent.

**Spirits** (`char_spirit_*.py`). Salamander: a dragonfolk with a flame mane, ram horns, dragon wings, a flame-tipped tail, a scale cuirass (old: a winged fire dragon). Undine: a slim woman of
sea-blue skin with long foam hair, fin ears, a rippled dress and mantle, a ball of water, drawn translucent (old: a woman of water). Sylph: a small green sprite with twin tails, dragonfly wings, a
leaf dress, a wand (old: a green fairy; the Wind Spirit Lord boss, which drew nothing, now wears her larger). Gnome: a huge slouching stone giant with a hollow face and two amber eyes (old: a
hunched stone giant). Black Devil: new (it only had an orb): a small black-violet devil sprite with horns, bat wings, a tail and a dark orb with a red glow.

**Grimoire Daemons** (`char_grimoire_daemon*.py`, shared design in `tools/remodel/daemons.py`). Four tiers: lesser (ash grey robe, amber glow), greater (crimson, devil horns, a three-leaf
grimoire), arch (violet and gold, ram horns, a halo, TWO pairs of membrane wings, five leaves), rule stone (a stone idol with its hands together in front of a carved tablet with a glowing glyph).

**Painted Constructs** (`char_painted_*.py`, shared in `tools/remodel/painted.py`). Hound-folk, knight and giant, each painted ONCE in pale paint; the renderer multiplies the paint's colour into the
body (three characters serve seven paints instead of twenty-one textures). The genome's other beast heads (cat, bear, boar, griffin, dragon) are not drawn.

**Rouge** (`char_rouge.py`). Before: a small cat of red thread. After: a catgirl in crimson thread on a child's body (the renderer shrinks it to the 0.35 hitbox), cat ears, a long tail, gold eyes, a
bell choker. `sit` for her SIT state, hop and scurry are walk and run, leap and tap are the attack, unravel shrinks and fades, re-knit plays `spawn`. The cat on her summoner's head
(`RougeCatLayer`) draws the same kit model in its `sit` pose (a constant `KIT_CAT` switches back to the cube cat).

**Tower Wizard** (`char_tower_wizard.py`). Before: the owner's design in cubes. After: the same old wizard (wide floppy hat, long grey beard, tired kind eyes, a brown-mauve robe, a glowing green orb),
an NPC whose `cast` clip is its talking.

**Cotton Sheep and Cloud** (`char_cotton_sheep.py`, `char_cotton_sheep_cook.py`, `char_cotton_cloud.py`). The sheep: a tall widened body in cream wool with a wool-ruffed collar, a long pale face, curled
horns, drooping ears; the cook adds a chef's hat, a blue neckerchief and a pan. The cloud is a prop, so its character is only the head of a child of cotton, set at the rim of a heap of wool puffs
(2.6 blocks across): a cloud with a sleepy face; the platform, the ride, the wrap (three stacked heaps) and the burst keep their modes.

**Zagred (legacy)** (`char_zagred_legacy.py`). Before: a cube devil with bat wings and a trident from phase 4. After: a black-violet lanky devil with a cracked bare ribcage, glowing red runes, ram horns,
bat wings with lit veins, a long whip tail; form 2 (phase 4) carries the trident. The fresnel aura shader and the target ring are kept (`KitZagredRenderer`).

## 4. How it is built and rendered

* Python: `tools/arc/char_<model>.py` per model (one file each, the kit's convention; `ENTITIES = [...]` marks a remodelled one), `tools/remodel/lib.py` (extra clips, scarf, head scaling, shifting a bone),
  `tools/remodel/{daemons,sheep,painted}.py` (shared designs, spec dicts only), `tools/remodel/gen_remodel.py` (builds the kit models, writes the skins, `--check`), `tools/remodel/gen_table.py` (writes
  `client/remodel/RemodelLooks.java` from `tools/arc/manifest.json`).
* Java, all new in `client/remodel/`: `RemodelTable` (pure), `RemodelLooks` (generated), `KitMath` (pure), `KitClips`, `KitBossRenderer<T extends ZogratisBoss>` (the framework bosses),
  `KitBossHooks` (Lucius's float and Julius glow, the Avatar's skin, the Goliath's rise, the God of Order's visit clips), `KitMobRenderer<T extends LivingEntity>` with a `Probe` per mob in `KitProbes`,
  `KitCloudRenderer`, `KitZagredRenderer`, `RemodelRenderers` (the one registration call, `RemodelRenderers.init(modEventBus)`, added to `NUSMP.java` after the old registrations).
* Files per model `<m>`: `tools/arc/char_<m>.py` (the spec), `assets/nusmp/geo/entity/arc_<m>.geo.json`, `assets/nusmp/animations/entity/arc_<m>.animation.json`,
  `assets/nusmp/textures/entity/arc_<m>.png` and `arc_<m>_glow.png`, `tools/render_preview/scenes.d/arc_<m>.json` (the preview scenes), and for a second form the same under `arc_<m>_form2`.
  The 33 characters: `dante_zogratis vanica_zogratis zenon_zogratis lucius_zogratis dark_disciple blood_beast devil_avatar god_of_order goliath charybdis_spawn floor_warden living_armor
  dungeon_direwolf basilisk dungeon_slime spirit_salamander spirit_undine spirit_sylph spirit_gnome spirit_black_devil grimoire_daemon grimoire_daemon_greater grimoire_daemon_arch
  grimoire_daemon_stone painted_beast painted_knight painted_giant rouge tower_wizard cotton_sheep cotton_sheep_cook cotton_cloud zagred_legacy`. The skins are
  `arc_dark_disciple_{lucifero,megicula,beelzebub,astaroth}[_glow].png` and `arc_devil_avatar_megicula[_form2][_glow].png` (`tools/render_preview/scenes.d/remodel_skins.json` previews them).
* The clips are the kit's (`idle walk run attack cast cast_big hurt transform death`, optionally `spawn`) plus `sleep` (direwolf, basilisk), `sit` (Rouge), `read beam bow` (God of Order), which
  the renderers play by state. The pose clock is the Zogratis one (`ZogratisPose`), so a boss's attack, cast, ultimate, hurt, transformation and death play as before.

## 5. Build and check

```
python3 -B tools/remodel/gen_remodel.py                # the kit models of every remodelled character and their skins
python3 -B tools/remodel/gen_table.py                  # RemodelLooks.java from the manifest (run after rebuilding a model)
python3 -B tools/remodel/gen_remodel.py --check        # the kit's --check (every character), then the skins by pixels
python3 -B tools/gen_arc_characters.py --preview <model> front back idle attack   # look at one
```
Tests (JUnit 5, pure): `client/remodel/RemodelTableTest` (every entity registered, every model has geometry, animations, texture, glow, second form, skins and the clips the clock plays and the
mob's own; the looks are the manifest's numbers and the kit found no problem; every character file and table row agree; every family and entity has a renderer case; the NUSMP line is there;
the mobs meant to look like the player were left alone) and `client/remodel/KitMathTest`.

## 6. Sizes

33 characters, 9 second forms (42 models) and 6 skins (12 texture files): about 2.9 MB of geometry, 9.4 MB of animation, 0.5 MB of texture, 12.9 MB uncompressed, about 1.9 MB deflated in the
jar (JSON compresses well). An arc character is about 310 KB uncompressed (the clips are most of it); the remodelled ones average 300 KB. A skin costs about 9 KB instead of 310 KB. The kit does not
allow clips to be shared between models (the arms are solved against each body and prop, and a model's file names only its own bones), so there was nothing to dedupe; what is shared: the Painted
Constructs share one set of textures across seven paints, the Guardian wears the Warden's files, the clones wear Lucius's, the wind boss wears Sylph's, the four devils of the Disciple and the
Avatar of Megicula are textures only.

| model | geometry KB | animation KB | texture + glow KB |
|---|---|---|---|
| basilisk | 89 | 262 | 7 |
| blood_beast | 107 | 296 | 10 |
| charybdis_spawn | 101 | 390 | 16 |
| charybdis_spawn_form2 | 101 | 371 | 17 |
| cotton_cloud | 19 | 166 | 22 |
| cotton_sheep | 73 | 271 | 9 |
| cotton_sheep_cook | 83 | 286 | 11 |
| dante_zogratis | 82 | 160 | 7 |
| dante_zogratis_form2 | 92 | 153 | 10 |
| dark_disciple | 49 | 227 | 8 |
| devil_avatar | 72 | 239 | 11 |
| devil_avatar_form2 | 92 | 234 | 12 |
| dungeon_direwolf | 69 | 299 | 11 |
| dungeon_slime | 32 | 230 | 4 |
| floor_warden | 50 | 194 | 13 |
| god_of_order | 62 | 201 | 14 |
| god_of_order_form2 | 75 | 197 | 15 |
| goliath | 36 | 178 | 11 |
| goliath_form2 | 36 | 166 | 12 |
| grimoire_daemon | 43 | 190 | 6 |
| grimoire_daemon_arch | 83 | 238 | 23 |
| grimoire_daemon_greater | 53 | 191 | 8 |
| grimoire_daemon_stone | 32 | 148 | 5 |
| living_armor | 33 | 173 | 5 |
| lucius_zogratis | 94 | 202 | 10 |
| lucius_zogratis_form2 | 147 | 230 | 36 |
| painted_beast | 66 | 296 | 7 |
| painted_giant | 26 | 186 | 5 |
| painted_knight | 51 | 223 | 7 |
| rouge | 54 | 298 | 6 |
| spirit_black_devil | 127 | 297 | 10 |
| spirit_gnome | 33 | 160 | 6 |
| spirit_salamander | 134 | 276 | 14 |
| spirit_sylph | 54 | 229 | 8 |
| spirit_undine | 46 | 199 | 9 |
| tower_wizard | 58 | 198 | 11 |
| vanica_zogratis | 40 | 160 | 5 |
| vanica_zogratis_form2 | 64 | 184 | 9 |
| zagred_legacy | 126 | 261 | 14 |
| zagred_legacy_form2 | 135 | 254 | 15 |
| zenon_zogratis | 55 | 150 | 5 |
| zenon_zogratis_form2 | 76 | 175 | 8 |

TOTAL geo 2950 KB, anim 9440 KB, tex 452 KB (+ skins 53 KB) = 12895 KB uncompressed, 42 models

## 7. Merging

* The new files are all new paths (no collisions with other workers' packages: nothing under `labyrinth/mega/foundry/` or `skill/` was touched).
* Edits to existing files: `NUSMP.java` (one line, `RemodelRenderers.init`), `entity/SpiritLordEntity.java` (an entity event on cast and a client tick field, so the companion spirit can show a
  cast pose; server behaviour unchanged), `client/RougeCatLayer.java` (the head cat draws the kit model), `tools/arc/manifest.json` (the kit rewrites it: take both sides' entries).
* `tools/render_preview/scenes.d/arc_<model>.json` and `remodel_skins.json` are generated.
* Version, CHANGELOG, README, gradle.properties, the mods toml and en_us.json were NOT touched (the lead does that); no new lang keys were needed (`tools/remodel/lang_fragment.json` is empty).

## 8. Unverified, dropped, caveats

Nothing here has run in the game. What was run: the kit builds and previews of every model (front, face, attack and the special clips of most), the kit's own arm and clip rules (no problem), the
type check of the Java against the API stubs, the pure tests through the local shim.

* Not seen in game: the scale of each model against its hitbox (`Size.fit` draws a model at its hitbox height times a boost: direwolf 1.3, basilisk 1.15, slime 1.3, blood beast 1.2, painted
  beast 1.15), the light on dark models (the harness's light differs from the game's), the registration priority (the last renderer registered for a type wins in NeoForge 21.1; the new listener is
  `EventPriority.LOWEST`).
* Dropped: the move clips of the Goliath (slam, stomp, roar, fist, line, rubble: all play `cast` or `cast_big`); the second translucent overlays of Undine (water) and Sylph (wings and bubbles: Undine
  is drawn translucent instead, Sylph's wings are part of the model); the equipment a Living Armor wears is not drawn on it; the genome heads of the painted beasts (one hound for all); the
  vanilla item the legacy Zagred held in phase 4 (the trident is in its form 2 model); `LuciusBook` (the kit's floating grimoire replaces it); the companion Spirit Lord is now a spirit body, not the floating
  orb (`SpiritOrbRenderer` stays in the tree; delete the `spirit_lord` row of `RemodelTable` to bring the orb back).
* The kit's `guard` idle breaks its own arm rule with a held one-handed prop or bare hands (`PROBLEM ... starts 60 degrees away from the idle pose`); the characters use `proud`, `slouch` or `relaxed`.
  A bare-handed character must use `attack: punch` (the other styles need a prop). Both are limits of the kit, not changed here.
* The cotton cloud is a face on a heap of puffs; riders stand on the heap (about a block high at scale 1, taller than the 0.6 hitbox: they sink into it a little).
* The head cat of Rouge uses a scale of 0.36 that was chosen by size, not seen on a head.
