# Pages 15, batch 2: Water, Ice, Fluids and Art

Owner goal: every grimoire has 15 spells. This batch owns these books (and no other): `book_creation`, `book_mist`, `book_painting`, `book_snow`, `book_ice`, `book_water`, `book_demon_water`, `book_mucus`, `book_mud`, `book_food`, `book_eye`, `book_demon_ice`, `book_bubble`, `book_ice_wedge`, `book_gel`, `book_black_oil`, `book_eyeball`.

How to build a page: `docs/magic/pages15_recipe.md`. Files this batch edits: `src/main/java/com/newuniverse/nusmp/book/pages15/Batch2.java`, `tools/lang/pages15_batch2.json`,
`src/test/resources/armour_policy/pages15_batch2_new_pages.json`, this file, and (character grimoires) `character_spells/noelle.json` and `character_spells/rill.json`. Test: `Pages15Batch2Test`.

**What was built, in plain words.** 93 new spell pages in 17 books, every one appended after the old pages of its book (no mode, unlock bit, mastery or save moves; all behind `magic.pagesFifteen`, which restores the old
books when switched off). 20 pages are canon (with the wiki page or chapter named in the Origin column; several only take the NAME from the wiki and the effect from this mod) and 73 are original, written in the style
of the book. Nothing is replaced. Each book now shows at least 15 pages in its basic grimoire (the table below + `python3 -B tools/page_counts.py`). Nothing here was seen in a game.

## Pages

One table row per page (the test `Pages15SourceScanTest` reads it): book, id, name, tier (starter / mid / zone / signature), origin (`original`, or `canon: <source>`), then free columns (what it does, the shape it reuses, worst case).

| Book | Id | Name | Tier | Origin | What it does | Reuses | Worst case |
|---|---|---|---|---|---|---|---|
| `book_creation` | `created_crystal_armor` | Creation: Crystal Armor | starter | canon: en-wiki: Creation Magic (attribute page, via search summary): armor composed of crystals | You create a shell of crystal from your own mana: Resistance and a few absorption hearts for ten seconds. | selfBuff (custom) + WIKI_MAGIC_FIELD | caster only, 1 packet |
| `book_creation` | `wind_hawk` | Creation: Wind Hawk | mid | canon: en-wiki: Creation Magic (attribute page, via search summary): hawk made of wind streams | Two hawks made of wind streams are created and hunt the nearest foes, striking from the air. | volley + WIKI_MAGIC_CAST | 2 to 5 bolts, each one hit, no per-tick scan beyond the bolt flight |
| `book_creation` | `climbing_coelacanth` | Creation: Climbing Coelacanth | starter | canon: en-wiki: Creation Magic (file page "Climbing Coelacanth", name only; the effect is this mod's) | A coelacanth made of mana swims up through the air in a line, hitting what it passes and lifting it off the ground. | line + WIKI_MAGIC_CAST | up to 4 targets, 1 to 2 packets |
| `book_creation` | `created_spear` | Creation: Spear | starter | original | A long spear of mana is thrust forward and pierces everything on its line. | line + WIKI_MAGIC_CAST | up to 4 targets, 1 to 2 packets |
| `book_creation` | `created_shield` | Creation: Shield | starter | original | A created shield orbits you for ten seconds: your melee hits deal more and whoever strikes you is struck back. | empower + WIKI_MAGIC_FIELD | caster only, a timed buff, 1 packet |
| `book_creation` | `created_arrows` | Creation: Arrow Volley | mid | original | Four created arrows leave your hand and curve toward the nearest foes. | volley + WIKI_MAGIC_CAST | 2 to 5 bolts, each one hit, no per-tick scan beyond the bolt flight |
| `book_creation` | `created_chains` | Creation: Chains | mid | original | Chains made of mana wrap one foe: it is held in place and weakened (the shared control lock applies). | bind + WIKI_MAGIC_CAST | 1 target, 1 packet, shared control lock |
| `book_creation` | `created_hammer` | Creation: Hammer | mid | original | A giant hammer is created at the aim point and slams down, knocking everything near it back. | nova + WIKI_MAGIC_BURST | up to 6 targets, 1 to 2 packets |
| `book_creation` | `created_steed` | Creation: Steed | mid | original | You create a steed of mana and ride it forward in one rush (a pure move, no damage). | dash + WIKI_MAGIC_CAST | the caster moves once, 1 packet |
| `book_creation` | `created_cage` | Creation: Cage | zone | original | Bars of mana close in a cage at the aim point: foes inside take a little damage and are slowed hard for four seconds. | field + WIKI_MAGIC_FIELD | one zone of 80 ticks every 10 ticks, up to 8 targets per pulse, 1 packet per pulse; 1 of the 2 kit zones |
| `book_creation` | `created_repairs` | Creation: Field Repairs | zone | original | A workshop of mana mends you and your allies nearby for a fifth of their health. | healAllies + ElementBook shapes | players near the caster, 1 packet, healing decay applies |
| `book_creation` | `created_colossus` | Creation: Colossus | signature | original | A colossus of mana rises around you and crashes down: a heavy blast that knocks foes back and leaves them slowed. | nova + WIKI_MAGIC_BURST | up to 10 targets, 1 to 2 packets |
| `book_mist` | `mist_doubles` | Mist Doubles | mid | canon: en-wiki: Foyal Migusteau / Mist Magic (via search summary): illusions of himself | Mist takes your shape while you slip away: mobs hunting you lose your trail and you turn invisible for three seconds. | decoy (ElementBook) + ElementBook shapes | mobs within 16 blocks lose the caster, 1 packet |
| `book_mist` | `mist_bullets` | Condensed Mist Bullets | mid | canon: en-wiki: Foyal Migusteau / Mist Magic (via search summary): condenses mist into water bullets | Mist is condensed into four water bullets that home in on the nearest foes. | volley + MIST_MAGIC | 2 to 5 bolts, each one hit, no per-tick scan beyond the bolt flight |
| `book_mist` | `concealing_fog` | Concealing Fog | zone | canon: en-wiki: Foyal Migusteau / Mist Magic (via search summary): mist hides his position | A thick fog follows you for five seconds: foes inside cannot see you and are blinded, taking light damage. | field + MIST_MAGIC | one zone of 100 ticks every 20 ticks, up to 8 targets per pulse, 1 packet per pulse; 1 of the 2 kit zones |
| `book_mist` | `dew_needles` | Dew Needles | starter | original | Three needles of condensed dew fly in a narrow fan. | volley + MIST_MAGIC | 2 to 5 bolts, each one hit, no per-tick scan beyond the bolt flight |
| `book_mist` | `rolling_fog` | Rolling Fog | starter | original | A wave of cold fog rolls out in front of you, damaging and slowing foes in the cone. | cone + MIST_MAGIC | up to 4 targets, 1 to 2 packets |
| `book_mist` | `chilling_haze` | Chilling Haze | starter | original | A haze closes round you: nearby foes are chilled, slowed and weakened. | nova + MIST_MAGIC | up to 4 targets, 1 to 2 packets |
| `book_mist` | `vapor_lance` | Vapor Lance | mid | original | Steam is drawn into a lance and fired in a straight line through every foe on it. | line + MIST_MAGIC | up to 6 targets, 1 to 2 packets |
| `book_mist` | `morning_mist` | Morning Mist | mid | original | A soft morning mist settles on you and your allies nearby and restores a part of their health. | healAllies + ElementBook shapes | players near the caster, 1 packet, healing decay applies |
| `book_mist` | `mist_ambush` | Mist Ambush | mid | original | You dissolve into mist and re-form behind your target, striking everything on the way. | dash + MIST_MAGIC | the caster moves once, 1 packet |
| `book_mist` | `mist_cloak` | Mist Cloak | starter | original | A cloak of mist hides you for three seconds and quickens your steps for five. | selfBuff (custom) + MIST_MAGIC | caster only, 1 packet |
| `book_mist` | `sea_of_fog` | Sea of Fog | signature | original | The whole area is drowned in a sea of fog for five seconds: foes inside are blinded, slowed and wounded again and again. | field + MIST_MAGIC | one zone of 100 ticks every 10 ticks, up to 10 targets per pulse, 1 packet per pulse; 1 of the 2 kit zones |
| `book_painting` | `heat_infused_ice` | Painting Magic: Invasion of Heat-Infused Ice | mid | canon: en-wiki: Invasion of Heat-Infused Ice (spell page, repo copy) | A circle opens and pours ice over one foe: it is held in place and left brittle (weakened). | bind + PAINTING_FX2 | 1 target, 1 packet, shared control lock |
| `book_painting` | `color_splash` | Painting Magic: Colour Splash | starter | original | A bucket of paint bursts at the aim point: foes in it are hit and blinded for a moment. | nova + PAINT_SPLAT | up to 4 targets, 1 to 2 packets |
| `book_painting` | `ink_darts` | Painting Magic: Ink Darts | starter | original | Three darts of wet ink fly from the brush and slow what they hit. | volley + PAINT_STROKE | 2 to 5 bolts, each one hit, no per-tick scan beyond the bolt flight |
| `book_painting` | `canvas_screen` | Painting Magic: Canvas Screen | starter | original | You paint a screen before yourself: Resistance and an absorption heart for six seconds. | selfBuff (custom) + PAINT_CAMO | caster only, 1 packet |
| `book_painting` | `gloss_slide` | Painting Magic: Gloss Slide | starter | original | You paint a glossy track under your feet and slide along it (a pure move, no damage). | dash + PAINT_TRAIL | the caster moves once, 1 packet |
| `book_painting` | `wet_paint` | Painting Magic: Wet Paint | mid | original | A patch of wet paint is brushed on the ground: foes standing in it are slowed and take a little damage. | field + PAINT_SPLAT | one zone of 60 ticks every 20 ticks, up to 6 targets per pulse, 1 packet per pulse; 1 of the 2 kit zones |
| `book_painting` | `vague_azur` | Painting Magic: Vague d'Azur | mid | original | A wave of azure paint is swept across the field, hitting everything in a wide line and pushing it back. | line + PAINT_TRAIL | up to 6 targets, 1 to 2 packets |
| `book_painting` | `etincelle_rouge` | Painting Magic: Étincelle Rouge | mid | original | A streak of red paint becomes a spark that bursts on impact and sets foes alight. | bolt + PAINTING_FX1, PAINTING_FX3 | one bolt, up to 6 targets at the burst, 2 packets |
| `book_painting` | `verdure_embrace` | Painting Magic: Verdure Embrace | zone | original | You paint a green meadow beneath your allies: it restores a fifth of their health. | healAllies + ElementBook shapes | players near the caster, 1 packet, healing decay applies |
| `book_painting` | `pigment_storm` | Painting Magic: Pigment Storm | zone | original | A storm of flying pigments rages at the aim point for four seconds, wounding and blinding foes. | field + PAINTING_FX2 | one zone of 80 ticks every 10 ticks, up to 8 targets per pulse, 1 packet per pulse; 1 of the 2 kit zones |
| `book_painting` | `gallery_of_giants` | Painting Magic: Gallery of Giants | signature | original | Giants painted on the wall step out of the canvas and stamp down: a heavy blast that knocks foes back and slows them. | nova + PAINT_BEAST | up to 10 targets, 1 to 2 packets |
| `book_snow` | `miniature_snow_garden` | Miniature Garden of Illusory Snow | zone | canon: en-wiki: Fragil Tormenta (Miniature Garden of Illusory Snow, via search summary) | A tiny garden of drifting snow grows at the aim point: foes inside grow drowsy, slowed and clumsy for a few seconds. | field + SNOW_FX2 | one zone of 80 ticks every 20 ticks, up to 8 targets per pulse, 1 packet per pulse; 1 of the 2 kit zones |
| `book_snow` | `snow_barrier` | Snow Barrier | mid | canon: en-wiki: Neige (snow barriers that hide an area, via search summary) | A barrier of whirling snow hides the area round you: foes inside cannot see and take a little cold damage. | field + SNOW_FX2 | one zone of 80 ticks every 20 ticks, up to 6 targets per pulse, 1 packet per pulse; 1 of the 2 kit zones |
| `book_snow` | `snow_binding` | Snow Binding | starter | canon: en-wiki: Neige (Snow Binding Magic, via search summary) | Snow piles up and shapes itself round one foe, holding it still (the shared control lock applies). | bind + SNOW_FX3 | 1 target, 1 packet, shared control lock |
| `book_snow` | `powder_burst` | Powder Burst | starter | original | A burst of powder snow explodes round you and pushes nearby foes away. | nova + SNOW_FX3 | up to 4 targets, 1 to 2 packets |
| `book_snow` | `snowball_barrage` | Snowball Barrage | starter | original | Four hard-packed snowballs are thrown in a fan and slow what they hit. | volley + SNOW_FX1 | 2 to 5 bolts, each one hit, no per-tick scan beyond the bolt flight |
| `book_snow` | `frostbite_wind` | Frostbite Wind | mid | original | A freezing wind full of snow sweeps a cone before you: foes are cut, slowed and weakened. | cone + SNOW_FX1 | up to 6 targets, 1 to 2 packets |
| `book_snow` | `snow_avalanche` | Snow Avalanche | signature | original | A wall of snow is brought down on the aim point: a heavy blast that buries foes, pushes them back and slows them. | nova + SNOW_FX3 | up to 10 targets, 1 to 2 packets |
| `book_ice` | `ice_funeral` | Ice Funeral | signature | canon: en-wiki: Ice Funeral (spell page, en-wiki) | Ice closes round you in a crown of spikes: a signature blast on every foe nearby. The ice also holds you, slowing you for three seconds (the canon spell kills the user too; here it only costs you your footing). | cast (custom) + ICE_FX3 | up to 10 targets, 1 to 2 packets |
| `book_ice` | `ice_slicer` | Ice Slicer | mid | canon: en-wiki: Ice Slicer (en-wiki page title; effect from a game guide, Roblox Black Clover Kingdom Grimshot, via search) | A sharp icicle rises from the ground and flies at your target, slicing and freezing it in place. | bolt + ICE_FX1, ICE_FX3 | one bolt, up to 6 targets at the burst, 2 packets |
| `book_ice` | `ice_needle` | Ice Needle | starter | canon: en-wiki: Ice Needle (en-wiki page title only; effect is this mod's) | Two thin needles of ice fly at the foes in front of you. | volley + ICE_FX1 | 2 to 5 bolts, each one hit, no per-tick scan beyond the bolt flight |
| `book_ice` | `jotunns_ice_floe` | Jötunn's Ice Floe | mid | canon: en-wiki: Jötunn's Ice Floe (en-wiki page title; effect from a game guide, via search) | A ball of ice forms in the air above the aim point and is smashed down half a second later, hitting and slowing everything under it. | trap (custom, delayed) + ICE_FX1, ICE_FX3 | one hit after a short wait, up to 6 targets, 2 packets |
| `book_ice` | `cage_of_infinite_hail` | Cage of Infinite Hail | zone | canon: en-wiki: Cage of Infinite Hail (spell page: Ice Mist Compound Magic, repo copy) | Hail falls without end inside a cage of ice and mist at the aim point: foes in it are pelted and slowed for five seconds. | field + ICE_FX2 | one zone of 100 ticks every 10 ticks, up to 8 targets per pulse, 1 packet per pulse; 1 of the 2 kit zones |
| `book_ice` | `endless_ice_fangs` | Endless Ice Fangs | mid | canon: en-wiki: Endless Ice Fangs (en-wiki page title only; effect is this mod's) | A row of ice fangs bursts out of the ground in a wide line, hitting and throwing foes into the air. | line + ICE_FX3 | up to 6 targets, 1 to 2 packets |
| `book_water` | `sea_dragon_god_vortex` | Sea Dragon God's Vortex | zone | canon: other-en: Noelle Silva spell list (fan article found by search; low confidence, not on the wiki summary) | A whirlpool opens at the aim point and drags every foe in it toward its centre, hitting and slowing them. | pull (custom) + WATERFX_ZONE | up to 8 targets once, 1 packet |
| `book_water` | `sea_dragon_god_tail` | Sea Dragon God's Tail | mid | canon: other-en: Noelle Silva spell list (fan article found by search; low confidence, not on the wiki summary) | A tail of water sweeps round in a wide arc and throws back everything it hits. | cone + WATER_BURST | up to 6 targets, 1 to 2 packets |
| `book_water` | `riptide_undertow` | Riptide Undertow | mid | original | A current seizes one foe by the legs and holds it in place (the shared control lock applies). | bind + WATERFX_CAST | 1 target, 1 packet, shared control lock |
| `book_water` | `riptide_dash` | Riptide Dash | starter | original | You ride a wave forward, hitting and pushing aside everything on the way. | dash + WATERFX_CAST | the caster moves once, 1 packet |
| `book_water` | `tidal_geyser` | Tidal Geyser | starter | original | A geyser bursts up under the aim point and throws the foes above it into the air. | nova + WATERFX_BURST | up to 4 targets, 1 to 2 packets |
| `book_demon_water` | `brine_lash` | Demon Water Magic: Brine Lash | starter | original | A whip of black brine cracks along a line and slows what it hits. | line + DEMON_WATER_FX1 | up to 4 targets, 1 to 2 packets |
| `book_demon_water` | `drowned_chains` | Demon Water Magic: Drowned Chains | mid | original | Chains of black water hold one foe in place and sap its strength (the shared control lock applies). | bind + DEMON_WATER_FX2 | 1 target, 1 packet, shared control lock |
| `book_demon_water` | `abyssal_ambush` | Demon Water Magic: Abyssal Ambush | mid | original | You sink into the dark water and rise behind your target, striking everything on the way. | dash + DEMON_WATER_FX1 | the caster moves once, 1 packet |
| `book_demon_water` | `black_deluge` | Demon Water Magic: Black Deluge | zone | original | A deluge of black water floods the aim point for five seconds: foes in it are battered and slowed. | field + DEMON_WATER_FX2 | one zone of 100 ticks every 10 ticks, up to 8 targets per pulse, 1 packet per pulse; 1 of the 2 kit zones |
| `book_demon_water` | `depth_pressure` | Demon Water Magic: Depth Pressure | starter | original | The pressure of the deep crushes round you: nearby foes are hurt, pushed away and left dizzy. | nova + DEMON_WATER_FX3 | up to 4 targets, 1 to 2 packets |
| `book_mucus` | `mucus_spit` | Mucus Magic: Mucus Spit | starter | original | You spit a spray of sticky mucus in a cone: foes are hit and slowed. | cone + MUCUS_FX1 | up to 4 targets, 1 to 2 packets |
| `book_mucus` | `mucus_siphon` | Mucus Magic: Siphoning Mucus | starter | original | A blob of mucus flies at one foe, soaks up a little of its magicule and feeds a part of it to you as health. | bolt + MUCUS_FX1, MUCUS_FX3 | one bolt, up to 4 targets at the burst, 2 packets |
| `book_mucus` | `mucus_cocoon` | Mucus Magic: Mucus Cocoon | mid | original | You wrap yourself in a cocoon of mucus: Resistance II and absorption hearts for eight seconds. | selfBuff (custom) + MUCUS_FX2 | caster only, 1 packet |
| `book_mucus` | `glob_slam` | Mucus Magic: Glob Slam | mid | original | A huge glob of mucus is dropped on the aim point: foes under it are hit, pushed and slowed. | nova + MUCUS_FX3 | up to 6 targets, 1 to 2 packets |
| `book_mucus` | `mucus_whirl` | Mucus Magic: Mucus Whirlpool | zone | original | Mucus swirls into a whirlpool at the aim point and drags foes toward its centre, slowing them. | pull (custom) + MUCUS_FX2 | up to 8 targets once, 1 packet |
| `book_mud` | `mud_splash` | Mud Magic: Mud Splash | starter | original | Mud splashes up round you and into the eyes of the foes nearby. | nova + MUD_FX3 | up to 4 targets, 1 to 2 packets |
| `book_mud` | `mud_slide` | Mud Magic: Mud Slide | starter | original | You slide forward on a rush of mud, bumping aside what is in the way. | dash + MUD_FX1 | the caster moves once, 1 packet |
| `book_mud` | `grasping_mud` | Mud Magic: Grasping Mud | mid | original | A hand of mud grabs one foe and drags it to you. | reel (custom) + MUD_FX1 | 1 target, 1 packet |
| `book_mud` | `mud_armor` | Mud Magic: Mud Armor | mid | original | A hard crust of mud covers you for twelve seconds: your melee hits deal more, you take less, and attackers are struck back. | empower + MUD_FX2 | caster only, a timed buff, 1 packet |
| `book_mud` | `quagmire` | Mud Magic: Quagmire | zone | original | The ground at the aim point turns into a quagmire for five seconds: foes sink in it, are hurt and are slowed hard. | field + MUD_FX2 | one zone of 100 ticks every 10 ticks, up to 8 targets per pulse, 1 packet per pulse; 1 of the 2 kit zones |
| `book_food` | `mana_munch` | Mana Munch | mid | canon: en-wiki: Food Magic (attribute page, via search summary): eats the opponent's mana | You bite the mana out of one foe: a little damage, a part of its magicule is taken, and you recover some health. | bolt + FOOD_FX1, FOOD_FX3 | one bolt, up to 6 targets at the burst, 2 packets |
| `book_food` | `restoring_plate` | Restoring Plate | starter | canon: en-wiki: Food Magic (attribute page, via search summary): a plate of food that restores power (Sally page) | A plate of warm food restores a part of the health of you and your allies nearby. | healAllies + ElementBook shapes | players near the caster, 1 packet, healing decay applies |
| `book_food` | `spice_burst` | Spice Burst | starter | original | A cloud of burning spice explodes round you and sets nearby foes alight. | nova + FOOD_FX3 | up to 4 targets, 1 to 2 packets |
| `book_food` | `syrup_flood` | Syrup Flood | zone | original | A flood of sticky syrup spreads at the aim point for four seconds: foes in it are slowed and hurt. | field + FOOD_FX2 | one zone of 80 ticks every 10 ticks, up to 8 targets per pulse, 1 packet per pulse; 1 of the 2 kit zones |
| `book_eye` | `hypnotic_gaze` | Hypnotic Gaze | mid | original | Your eyes catch one foe: it is held where it stands, dazed and hurt (the shared control lock applies). | bind + EYE_FX2 | 1 target, 1 packet, shared control lock |
| `book_eye` | `dazzle_sweep` | Dazzle Sweep | starter | original | You sweep your gaze across a wide cone: foes in it are dazzled and blinded for two seconds. | cone + EYE_FX1 | up to 4 targets, 1 to 2 packets |
| `book_eye` | `silencing_gaze` | Silencing Gaze | starter | original | A narrow gaze lands on the foes in front of you and shuts their magic for three seconds. | line + EYE_FX1 | up to 4 targets, 1 to 2 packets |
| `book_eye` | `watchful_circle` | Watchful Circle | zone | original | Eyes open round you for four seconds: foes in the circle are hurt, weakened and made visible through walls. | field + EYE_FX2 | one zone of 80 ticks every 10 ticks, up to 8 targets per pulse, 1 packet per pulse; 1 of the 2 kit zones |
| `book_demon_ice` | `demon_ice_fangs` | Demon Ice: Frost Fangs | starter | original | Three fangs of black ice fly from your hand and slow what they bite. | volley + DEMON_ICE_FX1 | 2 to 5 bolts, each one hit, no per-tick scan beyond the bolt flight |
| `book_demon_ice` | `demon_ice_chains` | Demon Ice: Frozen Chains | mid | original | Chains of black ice freeze one foe solid for a moment (the shared control lock applies). | bind + DEMON_ICE_FX2 | 1 target, 1 packet, shared control lock |
| `book_demon_ice` | `demon_ice_comet` | Demon Ice: Black Comet | zone | original | A comet of black ice is called down on the aim point and lands a second later, hurting and badly slowing everything under it. | trap (custom, delayed) + DEMON_ICE_FX1, DEMON_ICE_FX3 | one hit after a short wait, up to 8 targets, 2 packets |
| `book_bubble` | `soap_slide` | Soap Slide | starter | original | You slide forward on a track of soap bubbles (a pure move, no damage). | dash + BUBBLE_FX1 | the caster moves once, 1 packet |
| `book_bubble` | `bubble_whirl` | Bubble Whirl | mid | original | Bubbles whirl at the aim point and draw the foes into a lifting current. | pull (custom) + BUBBLE_FX2 | up to 6 targets once, 1 packet |
| `book_bubble` | `bubble_bath` | Bubble Bath | zone | original | A warm bath of bubbles rises under you and your allies nearby and restores a fifth of their health. | healAllies + ElementBook shapes | players near the caster, 1 packet, healing decay applies |
| `book_ice_wedge` | `wedge_anchor` | Ice Wedge: Wedge Anchor | starter | original | A wedge of ice is driven in beside one foe and anchors it to the spot (the shared control lock applies). | bind + ICE_WEDGE_FX3 | 1 target, 1 packet, shared control lock |
| `book_ice_wedge` | `wedge_spiral` | Ice Wedge: Wedge Spiral | mid | original | A spiral of wedges drills forward in a line, hitting everything on it and slowing it. | line + ICE_WEDGE_FX1 | up to 6 targets, 1 to 2 packets |
| `book_ice_wedge` | `wedge_cataclysm` | Ice Wedge: Wedge Cataclysm | zone | original | Giant wedges are called down on the aim point and land three quarters of a second later, hurting and pushing back everything under them. | trap (custom, delayed) + ICE_WEDGE_FX1, ICE_WEDGE_FX3 | one hit after a short wait, up to 8 targets, 2 packets |
| `book_gel` | `gel_whip` | Gel Whip | starter | original | A whip of gel wraps one foe and drags it toward you. | reel (custom) + GEL_FX1 | 1 target, 1 packet |
| `book_gel` | `gel_mortar` | Gel Mortar | mid | original | A lump of gel is lobbed and bursts on impact, splattering and slowing foes round the point. | bolt + GEL_FX1, GEL_FX3 | one bolt, up to 6 targets at the burst, 2 packets |
| `book_gel` | `gel_flood` | Gel Flood | zone | original | A flood of gel covers the aim point for five seconds: foes in it are hurt and held down. | field + GEL_FX2 | one zone of 100 ticks every 10 ticks, up to 8 targets per pulse, 1 packet per pulse; 1 of the 2 kit zones |
| `book_black_oil` | `oil_spray` | Oil Spray | starter | original | A spray of black oil washes over the foes in front of you and blights them for two seconds. | cone + BLACK_OIL_FX1 | up to 4 targets, 1 to 2 packets |
| `book_black_oil` | `tar_chains` | Tar Chains | mid | original | Chains of thick tar hold one foe in place while it is blighted (the shared control lock applies). | bind + BLACK_OIL_FX2 | 1 target, 1 packet, shared control lock |
| `book_black_oil` | `oil_geyser` | Oil Geyser | zone | original | Oil seeps into the ground at the aim point and erupts in a geyser a second later, hurting, lifting and blighting foes above it. | trap (custom, delayed) + BLACK_OIL_FX2, BLACK_OIL_FX3 | one hit after a short wait, up to 8 targets, 2 packets |
| `book_eyeball` | `eyeball_volley` | Eyeball Volley | starter | original | Three eyeballs are shot from your hand and home in on the nearest foes. | volley + EYEBALL_FX1 | 2 to 5 bolts, each one hit, no per-tick scan beyond the bolt flight |
| `book_eyeball` | `optic_beam` | Optic Beam | mid | original | Your eyeballs line up and fire one beam along a line, hurting and blinding everything on it. | line + EYEBALL_FX1 | up to 6 targets, 1 to 2 packets |
| `book_eyeball` | `eyeball_storm` | Eyeball Storm | zone | original | A storm of eyeballs circles you for five seconds: foes near you are hurt, weakened and made visible through walls. | field + EYEBALL_FX2 | one zone of 100 ticks every 10 ticks, up to 8 targets per pulse, 1 packet per pulse; 1 of the 2 kit zones |

### Per book

| Book | New pages | Canon | Original |
|---|---|---|---|
| `book_creation` | 12 | 3 | 9 |
| `book_mist` | 11 | 3 | 8 |
| `book_painting` | 11 | 1 | 10 |
| `book_snow` | 7 | 3 | 4 |
| `book_ice` | 6 | 6 | 0 |
| `book_water` | 5 | 2 | 3 |
| `book_demon_water` | 5 | 0 | 5 |
| `book_mucus` | 5 | 0 | 5 |
| `book_mud` | 5 | 0 | 5 |
| `book_food` | 4 | 2 | 2 |
| `book_eye` | 4 | 0 | 4 |
| `book_demon_ice` | 3 | 0 | 3 |
| `book_bubble` | 3 | 0 | 3 |
| `book_ice_wedge` | 3 | 0 | 3 |
| `book_gel` | 3 | 0 | 3 |
| `book_black_oil` | 3 | 0 | 3 |
| `book_eyeball` | 3 | 0 | 3 |
| total | 93 | 20 | 73 |

## Character grimoires (set entries added)

* `character_spells/noelle.json` (book_water): 8 -> 15 pages. Entries added (all `exclusive: false`, so the pages stay in the basic Water book too): `aqua_javelin`, `sea_dragon_god_vortex` (NEW page of this batch),
  `scale_mail`, `torrent`, `mermaid_form`, `tide_lance`, `sea_dragons_wrath` (the last five were existing Water pages that Noelle's set did not list). The older notes say that no source ties Aqua Javelin, Wrath, Tide Lance, Scale Mail and
  Torrent to Noelle by name; they are listed only so her grimoire reaches the owner's 15, and each entry says so in its note. Rules: mastery 0.05 / 0.3 / 0.4, `torrent` after one use of Cradle at 0.45, `mermaid_form` after the Nest page at 0.6, `tide_lance` after the Valkyrie Dress page at 0.75, `sea_dragons_wrath` after five Roars at 0.8.
* `character_spells/rill.json` (book_painting): 14 -> 15 pages. Entry added: `heat_infused_ice` (canon, was in the file's "planned" list), mastery 0.38, `exclusive: false`.
* No other book of this batch has a character set.

## Research sources (rule 10: English wiki first, Japanese wiki as the fallback)

* Fandom pages cannot be fetched from this sandbox. English wiki: read through `WebSearch` summaries and the repo's own copies in `.github/wiki/` (attribute pages and spell pages `spell_*.txt`).
  From the repo copies: Ice Magic's spell list (Ice Funeral, Ice Slicer, Ice Needle, Jötunn's Ice Floe, Cage of Infinite Hail, Endless Ice Fangs), the spell page Cage of Infinite Hail (Ice Mist Compound Magic) and
  Invasion of Heat-Infused Ice (Painting Magic).
* From search summaries of the English wiki: Ice Funeral (the user encases themself in ice with outward icicles; everyone inside dies, the user too), Creation Magic (crystal armour, a hawk of wind streams, objects made of the user's mana,
  the file page "Climbing Coelacanth"), Fragil Tormenta (Miniature Garden of Illusory Snow), Neige (Snow Magic: barriers that hide an area, Snow Binding), Foyal Migusteau (Mist Magic: hides his position, illusions of himself, mist condensed into water bullets
  that break Steel Magic), Charmy / Food Magic (eats the opponent's mana; a plate of food that restores power, Sally page).
* Secondary (not the wiki): a game guide (Roblox "Black Clover Kingdom Grimshot") for the effects of Ice Slicer and Jötunn's Ice Floe; a fan article (Deltia's Gaming, already cited in `noelle.json`) for Sea Dragon God's Vortex / Tail
  (low confidence: marked so in the Origin column).
* Japanese wiki (fallback): searched for the Snow / Ice, Mist / Creation / Mud / Mucus lists (`ブラッククローバー 雪魔法 氷魔法 呪文 一覧`, `霧魔法 創造魔法 泥魔法 粘液魔法 呪文`): the search returned only the English
  Mucus Magic page and a mobile-game page, nothing usable.
* **Could not be fetched / found at all:** the spell lists of Mist Magic, Creation Magic, Snow Magic, Mud Magic, Mucus Magic, Food Magic (beyond Glutton's Banquet), Bubble Magic, Black Oil Magic, Eyeball Magic, Ice Wedge Magic, Gel Magic, Demon Ice / Demon Water Magic
  (their wiki pages list one spell or none and all of it is already a page); the pages of Ice Needle, Endless Ice Fangs (only the titles are known), Sea Dragon God's Vortex / Tail (unconfirmed), Noelle's other water spells; every Japanese wiki page.
  The 73 original pages are therefore this mod's own, in the book's style (rule: canon first, then original).

## Worst case of the batch (rule 11)

* Server, per cast: one page = at most the tier's targets hit (4 / 6 / 8 / 10), at most 10 effect packets to nearby players (these pages use 1 or 2), no entity, no block, no chunk load, no world scan beyond the spell's own box.
* Standing effects: the field pages are one zone of at most 100 ticks with a step of at least 10 ticks (so at most 10 pulses of at most 8 targets), counted in the kit's cap of 2 zones per player; the `trap` pages wait at most 20 ticks once
  (`Ctx.later`, cancelled by death or logout), then hit once. Nothing runs per tick after a spell ends and nothing while nobody casts.
* The five custom casts of `Batch2` (`selfBuff`, `pull`, `reel`, `trap`, and `Mist Doubles` = the existing `ElementBook.decoy()`) add: a self buff with effects of at most 20 s; a pull that changes the motion of at most the tier's targets once;
  a reel of one target. Riders `drain` (at most 2 % of the target's magicule per hit, existing `EnergyBridge`) and `silence` (at most 4 s) are the books' own existing helpers.
* Client: only existing `VfxShape`s of the books (already budgeted by `perf`); no new asset, entity type, packet or shape.
* `Ice Funeral` (signature) also slows the caster for 3 s: a cost, not a danger; it cannot kill the caster.

## New and replaced (rule 5)

All 93 pages are NEW; nothing is replaced and nothing is removed. KEPT: every old page of the 17 books and every old entry of the character sets (entries only appended).

## Honest caveats

* Not seen in a game: the casts compile against the API stubs (the only errors are the stub's "MobEffectInstance is abstract", which every older page shows too) and the pure tests pass, but no spell was cast.
* The sizes, durations and riders were chosen by reading the existing pages of each book and the kit's tier numbers; balance may need a pass after play.
* Several "canon" pages take only the name from the wiki (Ice Needle, Endless Ice Fangs, Climbing Coelacanth); the Origin column says so.
* Decision for the owner: the Noelle entries for Aqua Javelin, Scale Mail, Torrent, Tide Lance and Wrath have no source naming Noelle; they were added only to reach 15.
