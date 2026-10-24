package com.newuniverse.nusmp.vfx;

/**
 * Every effect the server can ask clients to draw. The ordinal is sent over the network,
 * so ONLY ADD NEW SHAPES AT THE END.
 */
public enum VfxShape {
    MAGIC_CIRCLE,            // spinning grimoire circle at 'from', facing toward 'to'
    MAGIC_CIRCLE_EXPLOSION,  // circle charges up, then bursts
    FLAME_TRAIL,             // flame stream from 'from' to 'to'
    FLAME_EXPLOSION,         // fireball burst at 'to'
    WIND_SLASH,              // crescent blade flying from 'from' to 'to'
    WIND_RING,               // expanding gust rings around 'from'
    ANTI_MAGIC_SLASH,        // negative-blend slash + shatter (anti-magic, devil pages)
    WATER_SPLASH,            // splash burst at 'to'
    WATER_RING,              // expanding rings at 'from' (water, ice, Time stasis)
    ELF_CIRCLE,              // green rune circle (plant, light)
    DEVIL_CIRCLE,            // red-black explosive circle (five-leaf, triple spade, Devil Union)
    LIGHTNING_SPEAR,         // jagged bolt from -> to
    EARTH_SPIKES,            // spikes rising along from -> to
    SPATIAL_RIFT,            // spinning rift disc at 'from'
    MIRROR_PANE,             // glowing pane at 'from' facing 'to'
    THREAD_LINE,             // thin glowing line from -> to
    SPIRIT_AURA,             // spirit spirals orbiting the caster (Spirit Dive, devil, reinforcement)
    MANA_CHARGE,             // mana motes converging into the caster while chanting
    SPELL_CARD,              // floating spell card with the grimoire's icon (seed = magic ordinal)
    WEAPON_CONSTRUCTS,       // spectral swords / axes / shields orbiting the caster
    // Spec-driven effects (vfx.fx): built once, animated, rule-checked headlessly
    FX_LIGHTNING_ARC,        // Chain Lightning jump: from -> to
    FX_FIRE_ERUPTION,        // Calderos pillar: at 'from'
    FX_WATER_CRASH,          // Sea Dragon's Roar impact: at 'from'
    FX_WIND_GUST,            // Gust Lane: from -> to
    FX_EARTH_SPIKES,         // Earth Spikes: from -> to
    FX_SPIRIT_AURA,          // Spirit Channeling hold (follows caster); payload colour = element 0-3
    FX_SPIRIT_OVERDRIVE,     // 25% release: dash from -> to
    FX_SPIRIT_NOVA,          // 50% release: burst at 'from'
    FX_SPIRIT_CATACLYSM,     // 100% release: 6 s around the caster (follows)
    // Time Magic (TimeMagicLayer), drawn after the anime
    TIME_STASIS,             // Chrono Stasis: glass sphere + orbiting Roman-numeral ribbon at 'from' (follows); power = radius
    TIME_CLOCK,              // Chrono Anastasis: clock face at 'to' over ground centre 'from', gold rain; power = radius
    TIME_REWIND,             // Time Reversal / passive rewind: dial at 'from', hands and ribbon run backwards
    TIME_ACCEL,              // Time Acceleration: dial + two ribbons whirling forwards around the caster (follows)
    // Elements drawn after the anime / wiki (FireSpellLayer, WaterSpellLayer, WindSpellLayer, EarthSpellLayer)
    FIRE_LION,               // Leo Rugiens: a lion of flame charges from -> to, mane blazing; power = size
    FIRE_SPEAR,              // Sol Linea: spiralling flame spear from -> to
    FIRE_PILLAR,             // Ignis Columna / Calderos: flame column erupting at 'from'; power = radius
    FIRE_BURST,              // flame explosion at 'from'
    WATER_DRAGON,            // Sea Dragon's Roar: water dragon from -> to (duration = flight time)
    WATER_CRADLE,            // Sea Dragon's Cradle: whirling water sphere ringed by globs at 'from' (follows); power = radius
    WATER_BURST,             // splash crown, droplets and ripples at 'from'
    WIND_TORNADO,            // Spirit Storm / Tornado Fang: green-white tornado at 'from' (follows); power = radius
    WIND_GALE,               // Swallow's Gale / Gust Lane: wind swallows and streaks from -> to
    STONE_SPIKES,            // stone spikes rising one after another from -> to (one per block, 2 ticks apart)
    EARTH_RISE,              // Ground Wall: crack, dust and debris along the wall base from -> to
    EARTH_FISSURE,           // Mother Earth Split: crack runs from -> to, stone slabs push up along it
    // 0.31: the wiki spells of the four elements (appended so the ids above never move)
    FIRE_SPIRAL,             // Spiral Flame: a widening corkscrew vortex of flame drilling from -> to
    FIRE_WILD,               // Wild Bursting Flame: flame bursting out in every direction from 'from' in waves; power = radius
    WATER_JAVELIN,           // Aqua Javelin: a high-pressure lance of water from -> to, spray cone at the head
    WATER_NEST,              // Sea Dragon's Nest: a dome of whirling water over 'from', spouts and currents; power = radius
    WATER_DRESS,             // Valkyrie Dress: flowing water armour on the caster (follows); power = size
    WIND_EMPEROR,            // Slicing Wind Emperor: a huge crescent made of many wind blades flying from -> to
    WIND_ZEPHYR,             // Spirit of Zephyr: gale wings and a wind mantle round the caster (follows); power = size
    EARTH_CLAWS,             // Witch Hunter Claws: stone claws burst up round 'from' and clamp shut; power = radius
    EARTH_RAMPAGE,           // Rampaging Mother Earth: a heaving wave of stone slabs rolling from -> to
    // 0.34: the other magics, rebuilt after the wiki (ArcaneSpellLayer / ArcaneSpellLayer2); appended
    THREAD_WEB,              // Arachne's Web: a web of thread spreading over the ground at 'from'; power = radius
    THREAD_STRINGS,          // Dancing Doll / Red Thread: puppet threads from the caster's hand 'from' to the target 'to'
    SEAL_CHAINS,             // Seal chains: rune-inscribed chains closing round 'from' (the target); power = size
    SEAL_TRINITY,            // Trinity Seal: three seal circles converge on 'from' and crystallise; power = size
    LIGHTNING_FIEND,         // Thunder Fiend: crackling arcs round the caster (follows); power = size (> 1.5 = black lightning)
    LIGHTNING_GOD,           // Rising Salim: a pillar of lightning crashing down on 'from' with branching bolts; power = radius
    ALCHEMY_CIRCLE,          // Magic Convert / transmutation: a transmutation circle at 'from' facing 'to', motes converging
    POISON_CURTAIN,          // Violett Schirm: a hanging curtain of purple poison along from -> to; power = height
    POISON_BREATH,           // Basilisk's Breath: a cone of toxic mist rolling from -> to
    SPACE_PORTAL,            // Fallen Angel Gate / Door of Fate: a dimensional gate at 'from' facing 'to'; power = radius
    SPACE_CUBE,              // Unopening Red Room: a box of warped space round 'from'; power = half size
    ASH_FORMATION,           // Ash Absorbing Formation: a ring of swirling ash at 'from' drawing everything in; power = radius
    MIRROR_RAY,              // Reflect Ray: a mirror at 'from' fires a beam of light to 'to'
    MIRROR_DOUBLE,           // Real Double: mirror-glass doubles circling the caster (follows); power = number of doubles
    COTTON_SHEEP,            // Sleeping Sheep Strike (from -> to) / Sheep Cook (from == to: sheep round the caster)
    FOOD_MAW,                // Glutton's Banquet: a giant maw at 'from' facing 'to' opens and devours; power = size
    SHADOW_POOL,             // Dark Garden Invitation / Kids' Playground: an inky pool at 'from', shadow hands rising; power = radius
    SHADOW_UNITE,            // Unite Mode: devil-dark aura round the caster (follows); power = size
    RECOMBINE_CONSTRUCT,     // The Raging Black Bull: planks of a magic house fly in and assemble round 'from'; power = size
    ROUGE_CAT,               // Red Thread of Fate: Rouge, a cat woven of red thread, on the caster's head (follows); power >= 2 = unravelling
    // 0.40: Dream Magic (the pocket dimension) and Painting Magic
    DREAM_TRANSITION,        // Glamour World opening / closing: pastel mist and starlight swirl out from 'from' and close in; power = radius
    DREAM_DOME,              // the dream's sky: an iridescent dome over 'from', bubbles and drifting stars; power = radius
    DREAM_MANIFEST,          // Imagination Manifestation: smoke swirls in at 'from' and solidifies (a flash, a pop of stars); power = size
    DREAM_SHATTER,           // the dream breaks from inside: the dome cracks into shards that burst outward; power = radius
    PAINT_STROKE,            // a wet brush stroke painted through the air from 'from' to 'to', splashing ink where it lands; power = width
    PAINT_SPLAT,             // a sticky ink splatter on the ground at 'from' that dries to lacquer; power = radius
    PAINT_BEAST,             // a painting comes to life at 'from': flat brushstrokes lift off and wrap into a glowing outline; power = size
    PAINT_CAMO,              // Camouflage: a refraction shimmer and paint flecks round the caster (follows); power = size
    // 0.42: Mirror Magic overhaul
    MIRROR_FRAME,            // a real ornate mirror at 'from' facing 'to' (shape from the seed); power = half-height
    MIRROR_ARRAY,            // mirrors of mixed shapes turning round the caster (follows); power = number of mirrors
    MIRROR_SHATTER,          // a mirror bursts into shards at 'from'; power = size
    MIRROR_STEP,             // a Real Double steps out of the glass at 'from': prismatic streaks round a forming figure; power = height
    // 0.44 Painting Magic remake (palette & brush)
    PAINT_PALETTE,           // the palette manifests at 'from', board facing 'to': blobs of paint land in rippling pools; power = size
    PAINT_TRAIL,             // a glossy wet brush sweep 'from' -> 'to' with a specular streak, drips and a splash; power = width
    PAINT_EMERGE,            // a living illustration painted on the ground at 'from' stands up facing 'to'; seed & 3 = kind; power = height
    PAINT_SHIFT,             // the paint changes element at 'from': the palette's colours swirl into the new one; power = size
    // 0.45 extended attributes
    LIGHT_BLADE,             // a geometric light sword 'from' -> 'to' at light speed: blinding core, streak, lens flare at the head; power = size
    LIGHT_FLARE,             // a lens flare / light burst at 'from' (star rays, ghosts, bloom); power = size
    TREE_ROOTS,              // ancient roots erupt round 'from' out to radius power, emerald leaves and motes; seed & 15 = root count
    TREE_CANOPY,             // a great world tree grows at 'from' (height power), emerald canopy, mana drawn up its trunk
    TREE_DRAIN,              // a stream of emerald mana flows 'from' (drained) -> 'to' (receiver); power = width
    DICE_D6,                 // two elemental d6 tumble from 'from' and land at 'to'; seed & 7 = die A (1..6), (seed >> 3) & 7 = die B
    DICE_D20,                // a resin d20 with a galaxy core tumbles 'from' -> 'to'; seed & 31 = face (1..20); power = size
    SLASH_BLADES,            // jagged green mana blades along both forearms of the followed caster; power = length
    SLASH_WAVE,              // a projected jagged crescent cut 'from' -> 'to'; power = width
    SLASH_SCYTHE,            // Death Scythe: a giant reaping arc round 'from' facing 'to'; power = radius
    COMPASS_ARRAY,           // brass compasses hovering in an arc behind the followed caster, needles turned toward 'to'; power = count
    COMPASS_NEEDLE,          // a golden compass needle homing 'from' -> 'to' with a gilt trail; power = size
    COMPASS_LOCK,            // a compass-rose lock sigil on 'from' (the target's magicule signature); power = size
    MERCURY_DOME,            // a flowing liquid-silver dome round 'from'; power = radius
    MERCURY_SPEAR,           // a hyper-dense chrome spear 'from' -> 'to' with a liquid tail; power = size
    MERCURY_EAGLE,           // a giant liquid-silver eagle swoops 'from' -> 'to'; power = wingspan
    // 0.47 Kotodama (Word Soul) Magic, Zagred
    KOTO_WORDS,              // a spoken command: demonic glyphs flash in a line before 'from' facing 'to', then burst; seed & 7 = word; power = size
    KOTO_AURA,               // billowing purple-black smoke round the followed caster (or 'from'); power = size
    KOTO_SHATTER,            // a spell stripped back to magicules at 'from': it cracks and dissolves into motes drawn to 'to'; power = size
    KOTO_SWORDS,             // a storm of demon swords falls out of a void rift over 'from'; power = radius
    KOTO_TRIDENT,            // the otherworldly trident drawn out of a void rift at 'from' (pointing 'to'); power = size
    KOTO_SLUDGE,             // underworld matter boiling up at 'from': black bubbles, violet tendrils, drain motes; power = size
    // 0.48 Genesis Demon-Slayer
    DEMON_METEOR,            // Black Meteorite: an anti-magic meteor 'from' -> 'to' (black core, crimson rim, torn void streaks, shed shards); power = size
    NIHILITY_ZONE,           // a Nihility zone round 'from': a black dome edge with crimson cracks, magic motes snuffed out; power = radius
    // 0.49 Game Magic
    GAME_BOARD,              // a glowing game board of squares on the ground round 'from' (gold, pulsing); power = radius
    // 0.53 the Black Clover Magic and VFX expansion: three effects per attribute (their meaning is in <Name>Layer's javadoc)
    BEAST_FX1, BEAST_FX2, BEAST_FX3,
    DEMON_BEAST_FX1, DEMON_BEAST_FX2, DEMON_BEAST_FX3,
    BODY_FX1, BODY_FX2, BODY_FX3,
    BLOOD_FX1, BLOOD_FX2, BLOOD_FX3,
    BONE_FX1, BONE_FX2, BONE_FX3,
    EYE_FX1, EYE_FX2, EYE_FX3,
    EYEBALL_FX1, EYEBALL_FX2, EYEBALL_FX3,
    CURSE_FX1, CURSE_FX2, CURSE_FX3,
    CURSE_WARDING_FX1, CURSE_WARDING_FX2, CURSE_WARDING_FX3,
    DEMON_FIRE_FX1, DEMON_FIRE_FX2, DEMON_FIRE_FX3,
    DEMON_ICE_FX1, DEMON_ICE_FX2, DEMON_ICE_FX3,
    DEMON_LIGHT_FX1, DEMON_LIGHT_FX2, DEMON_LIGHT_FX3,
    DEMON_WATER_FX1, DEMON_WATER_FX2, DEMON_WATER_FX3,
    GRAVITY_FX1, GRAVITY_FX2, GRAVITY_FX3,
    SEALING_FX1, SEALING_FX2, SEALING_FX3,
    BARRIER_FX1, BARRIER_FX2, BARRIER_FX3,
    IMITATION_FX1, IMITATION_FX2, IMITATION_FX3,
    KEY_FX1, KEY_FX2, KEY_FX3,
    CHAIN_FX1, CHAIN_FX2, CHAIN_FX3,
    BUTOH_FX1, BUTOH_FX2, BUTOH_FX3,
    BRIAR_FX1, BRIAR_FX2, BRIAR_FX3,
    CHERRY_BLOSSOM_FX1, CHERRY_BLOSSOM_FX2, CHERRY_BLOSSOM_FX3,
    FUNGUS_FX1, FUNGUS_FX2, FUNGUS_FX3,
    FOOD_FX1, FOOD_FX2, FOOD_FX3,
    CRYSTAL_FX1, CRYSTAL_FX2, CRYSTAL_FX3,
    CORUNDUM_FX1, CORUNDUM_FX2, CORUNDUM_FX3,
    BRONZE_FX1, BRONZE_FX2, BRONZE_FX3,
    COPPER_FX1, COPPER_FX2, COPPER_FX3,
    IRON_FX1, IRON_FX2, IRON_FX3,
    BLACK_OIL_FX1, BLACK_OIL_FX2, BLACK_OIL_FX3,
    GEL_FX1, GEL_FX2, GEL_FX3,
    GLASS_FX1, GLASS_FX2, GLASS_FX3,
    BUBBLE_FX1, BUBBLE_FX2, BUBBLE_FX3,
    ICE_FX1, ICE_FX2, ICE_FX3,
    ICE_WEDGE_FX1, ICE_WEDGE_FX2, ICE_WEDGE_FX3,
    LEGION_FX1, LEGION_FX2, LEGION_FX3,
    LIGHT_FX1, LIGHT_FX2, LIGHT_FX3,
    // 0.53: the seven core magics audited against the master framework
    PAINTING_FX1, PAINTING_FX2, PAINTING_FX3,
    KOTODAMA_FX1, KOTODAMA_FX2, KOTODAMA_FX3,
    WORLD_TREE_FX1, WORLD_TREE_FX2, WORLD_TREE_FX3,
    DICE_FX1, DICE_FX2, DICE_FX3,
    SLASH_FX1, SLASH_FX2, SLASH_FX3,
    COMPASS_FX1, COMPASS_FX2, COMPASS_FX3,
    MERCURY_FX1, MERCURY_FX2, MERCURY_FX3,
    // 0.56: Yami's dark slashes (DarkSlashLayer) and Secre's sealing magic (SealSecreLayer), appended
    DARK_SLASH_DIMENSION,    // Dark Cloaked Dimension Slash: a spatial fracture cut from 'from' to 'to' (pitch-black core, purple aura, shader); power = width
    DARK_SLASH_BLADE,        // Dark Cloaked Black Blade: darkness coats the blade and grows a long black blade; 'from' = hand, 'to' = tip; power = size
    DARK_SLASH_AVIDYA,       // Dark Cloaked Avidya Slash: a wide crescent of darkness thrown from 'from' towards 'to'; power = size
    DARK_SLASH_WILD,         // Yami slashing wildly: a flurry of crossed dark cuts round 'from'; power = size
    SEAL_BASIC,              // Basic seal: a magic circle (power = radius) with orbiting orbs; colour = elemental tint; duration = seal time
    SEAL_BRANCH,             // Secre's Branching Array: cyan square-edged lines branching in a 3D web from 'from'; power = reach
    SEAL_CUBE,               // Secre's Eternal Prison: a translucent glowing cube round 'from' with speed lines on its edges; power = edge length
    SEAL_WOUND,              // Secre's Wound Sealing: a gentle blue stream from 'from' (grimoire) to 'to' (the ally) that closes wounds
    SEAL_ORBIT,              // Secre's Orbital Bind: fast blue rings orbiting a sphere round 'from'; power = radius
    // 0.57: the rest of Yami's Dark Magic (DarkMagicLayer)
    DARK_BLACK_HOLE,         // Black Hole: a black sphere with a violet accretion ring at 'from'; power = radius; pulls motes inward
    DARK_BLACK_MOON,         // Black Moon: a black moon overhead at 'from' with a violet corona and falling dark motes; power = radius
    DARK_THRUST,             // Death Thrust: a spear of darkness from 'from' to 'to' with a shock cone at the tip; power = size
    DARK_IAI,                // Iai Slash: one bright cut line from 'from' to 'to' with black afterimage and a sheath flash; power = size
    // 0.62: distinct visual identities for Star, Sand, and Mist Magic (append-only network ids)
    STAR_MAGIC,              // starlight projectiles and constellations from 'from' to 'to'
    SAND_MAGIC,              // rolling grains, dust veils and cutting grit from 'from' to 'to'
    MIST_MAGIC,              // layered drifting vapor and soft water-light from 'from' to 'to'
    LEGION_BOARD,             // toggleable upright checkerboard and chess sprites following the summoned grimoire's owner
    // 0.89: shared layered spell families for the newly covered wiki attributes; each book supplies its own palette.
    WIKI_MAGIC_CAST, WIKI_MAGIC_FIELD, WIKI_MAGIC_BURST,
    // 1.16.0: a wizard's own spells (BornMagicLayer): the payload's colour is the first parent's, the seed's high 32 bits the second parent's, its low byte a style
    BORN_BEAM,               // a lance / orb / corkscrew from 'from' to 'to' (style bit 3 = orb, bit 4 = corkscrew); power = size
    BORN_NOVA,               // a two-colour circle, a shock ring and thrown motes at 'from'; power = radius
    BORN_WAVE,               // rings alternating the two colours racing out from 'from' towards 'to'
    BORN_RAIN,               // a bolt falling from 'from' to 'to' with a ring, motes and a flash where it lands; power = size
    // 1.27.0: the held cast key (CastHoldLayer): ONE circle follows the caster for the whole hold instead of a new circle every few ticks (anim.CastHold)
    MAGIC_CIRCLE_HOLD,       // start / charge step / keep-alive of the hold circle (follows the caster); duration = how long it lives without news; power = size
    MAGIC_CIRCLE_HOLD_END,   // the key was released: the hold circle stays for the wind-up and fades; duration = wind-up + fade
    // 1.27.0 merged-magic shapes: RETIRED in 1.93.0 (the merge generator was removed); the constants stay so the ordinals of the shapes after them never move
    // magic's key (core.magic.merge.MergedKeys), which the client resolves to the synced definition (parents, palette, weights).
    MERGED_CAST,             // a bolt / trail from 'from' to 'to': one strand of motes per parent in the parent's colour, a blended core; power = size
    MERGED_FIELD,            // a zone at 'from' (power = radius / 3): counter-turning rune rings, one per parent, joined by a blended ring
    MERGED_BURST,            // an impact at 'from': a shock ring that changes colour from parent to parent, sparks alternating the palette
    MERGED_AURA,             // the caster's casting stance (follows): all parents' circles stacked under the feet, motes rising in the palette; power = size
    // 1.35.0: the Zogratis bosses' own effects (ZogratisGravityLayer / ZogratisBloodLayer / ZogratisSpaceLayer / ZogratisDivineLayer). seed bit 0 = the
    // telegraph form (thin, dim, no flash) of a shape that also has a strike form; the meaning of from / to / power is in each layer's javadoc.
    GRAVITY_WELL,            // Dante: a well of warped space on the ground at 'from': rings crowd inward, motes fall in; power = radius
    GRAVITY_IMPACT,          // Dante: a crushing blow at 'from': a warped-space shock ring, ground cracks, a dust ring; power = radius
    GRAVITY_LANES,           // Dante: a lane of compressing chevrons from 'from' to 'to' (rock swords, spikes, pull lines); power = width
    GRAVITY_SINGULARITY,     // Dante: a black sphere with a lensing halo over 'from' that swells and collapses; power = radius
    GRAVITY_AURA,            // Dante: the heat and weight round the followed body (Body Surge, a charged fist); power = size
    BLOOD_CRESCENT,          // Vanica: a scythe crescent of blood round 'from' facing 'to'; power = radius
    BLOOD_LANCE,             // Vanica: a lance of blood from 'from' to 'to' (telegraph: a thin dashed aim line and a sigil at the tail); power = width
    BLOOD_SIGIL,             // Vanica: a thorny crimson sigil on the ground at 'from', thorns rising at the end; power = radius
    BLOOD_ARCS,              // Vanica: crimson arcs of blood leaping from 'from' to 'to' (whip, dash trail, bonds); power = size
    BLOOD_DOWNPOUR,          // Vanica: a column of blood falling on 'from' and splashing; power = radius
    SPACE_CRACK,             // Zenon: a pane of cracked space at 'from' facing 'to' (seed bit 1 = lying on the ground); power = size
    SPACE_TEAR,              // Zenon: a slit torn in space from 'from' to 'to', its rim glowing, shards at the ends; power = width
    SPACE_RINGGATE,          // Zenon: a spinning portal ring at 'from' facing 'to'; power = radius
    SPACE_SHATTER,           // Zenon: space shatters at 'from': glass shards burst out and a ring runs; power = radius
    DIVINE_PILLAR,           // Lucius: a pillar of light with halo rings at 'from'; colour gold, or purple for the soul form; power = radius
    DIVINE_HALO,             // Lucius: a halo and feather blades behind the followed body; power = size
    SOUL_VORTEX,             // Lucius: soul wisps spiralling in to 'from'; power = radius
    DIVINE_RAY,              // Lucius: a beam of light from 'from' to 'to' (telegraph: thin; strike: wide with a flash at the end); power = width
    DIVINE_SIGIL,            // Lucius: a holy sigil on the ground at 'from', turning; power = radius
    // 1.35.0 boss VFX (agent b2): ChronoLayers of the floor, relic and Avatar bosses (DevilBossLayer / StoneBossLayer / TideBossLayer / OrderBossLayer)
    BOSS_DEVIL_SIGIL, BOSS_DEVIL_CHAINS, BOSS_DEVIL_CRACKS, BOSS_DEVIL_BRAND,
    BOSS_STONE_RING, BOSS_STONE_COLUMN, BOSS_STONE_TRAIL, BOSS_STONE_FISSURE,
    BOSS_TIDE_VORTEX, BOSS_TIDE_SPOUT, BOSS_TIDE_SWEEP, BOSS_TIDE_RING,
    BOSS_ORDER_SIGIL, BOSS_ORDER_GRID, BOSS_ORDER_BEAM, BOSS_ORDER_LEDGER,
    // 1.35.0 boss VFX (agent b3): Zagred, the Blood-Red Commander and the Juggernaut (ZagredBossLayer / CommanderBossLayer / JuggernautBossLayer)
    ZAGRED_INK_BARS, ZAGRED_GLYPH_RING, ZAGRED_RULE_STONE, ZAGRED_TEXT_CORRUPT, ZAGRED_REWRITE_STRIKE,
    CMD_BANNERS, CMD_CHAINS, CMD_VAULT_LIGHT, CMD_CRIMSON_STRIKE,
    JUG_ASH_PULSE, JUG_ANTIBODY_RING,
    // 1.42.0: Lucius's final arc (LuciusCoreGemLayer)
    LUCIUS_CORE_GEM,
    // 1.44.0: the exclusive casters (Vanessa, Kahono, Rades, Kiato)
    ROUGE_FATE_TAP,       // Rouge's tap: red thread burst round the saved ally
    THREAD_COCOON,        // Arresting Dark Weave: a thread cocoon round the bound target
    THREAD_TRAP_LINE,     // a faint tripwire thread between trap nodes
    THREAD_SKIN,          // Mana Skin: a thread skin on the caster
    SONG_NOTES,           // Kahono: notes rising from the followed singer; power = size
    SONG_RING,            // Kahono: a sound ring on the ground running out to radius = power
    CORPSE_RAISE,         // a soul ripped up out of the ground at 'from' (a body rising); power = size
    CORPSE_PACT,          // the pact sigil round the followed caster (seed bit 0 = the short flash form); power = size
    BUTOH_DANCE_RIPPLE,   // Kiato: ripples under the dancer's feet
    BUTOH_AFTERIMAGE,     // Kiato: an ink after-image of the dancer
    // 1.93.0: the earth family (Sandstone, Rock, Red Ochre, Stone, Earth, Mineral): one layer (EarthFamilyLayer), the magic is read from the colour (vfx.EarthFxStyle)
    EARTHFAM_CAST,        // a charge at 'from' and a thrown body (chunks, grains, shards, pigment) flying to 'to'; power = size
    EARTHFAM_ZONE,        // a ground sigil with a rising rim and the magic's debris, radius = power
    EARTHFAM_BURST,       // an impact: flash, ring, the magic's debris flying out, a lingering afterglow; power = scale
    //     // the gem on the chest of the followed body while the core is exposed: a pulsing diamond, a halo and rising motes; power = size
    // 1.93.0: the rebuilt wiki magics, three effects each (the meaning of from / to / power is in each <Name>Layer's javadoc): FX1 cast, FX2 zone, FX3 impact
    SHAKUDO_FX1, SHAKUDO_FX2, SHAKUDO_FX3,
    SCALE_FX1, SCALE_FX2, SCALE_FX3,
    SPIKE_FX1, SPIKE_FX2, SPIKE_FX3,
    NAIL_FX1, NAIL_FX2, NAIL_FX3,
    HAIR_FX1, HAIR_FX2, HAIR_FX3,      // the gem on the chest of the followed body while the core is exposed: a pulsing diamond, a halo and rising motes; power = size
    // 1.93.0 magic rebuild (Song, Sound, Soul, Soul Corpse, Memory): FX1 = cast / projectile, FX2 = zone / field, FX3 = impact / signature (the shape protocol of docs/attribute_authoring_guide.md section 4)
    SONG_FX1, SONG_FX2, SONG_FX3,
    SOUND_FX1, SOUND_FX2, SOUND_FX3,
    SOUL_FX1, SOUL_FX2, SOUL_FX3,
    SOUL_CORPSE_FX1, SOUL_CORPSE_FX2, SOUL_CORPSE_FX3,
    MEMORY_FX1, MEMORY_FX2, MEMORY_FX3,      // the gem on the chest of the followed body while the core is exposed: a pulsing diamond, a halo and rising motes; power = size
    // 1.93.0 rebuild: Smoke, Snow, Wing, Vortex, Air and Ash Magic (<NAME>_FX1 cast / projectile, _FX2 zone (power = radius, drawn 1.5x), _FX3 impact; layers vfx/client/layer/<Name>Layer)
    SMOKE_FX1, SMOKE_FX2, SMOKE_FX3,
    SNOW_FX1, SNOW_FX2, SNOW_FX3,
    WING_FX1, WING_FX2, WING_FX3,
    VORTEX_FX1, VORTEX_FX2, VORTEX_FX3,
    AIR_FX1, AIR_FX2, AIR_FX3,
    ASH_FX1, ASH_FX2, ASH_FX3,
    // 1.93.0 magic rebuild: the own effects of Spatial, Mirror and Permeation Magic (Gravity and Slash already had their FX1..3 slots); cast / field / burst by the shape protocol of the authoring guide
    SPATIAL_FX1, SPATIAL_FX2, SPATIAL_FX3,
    MIRROR_FX1, MIRROR_FX2, MIRROR_FX3,
    PERMEATION_FX1, PERMEATION_FX2, PERMEATION_FX3,
    // 1.93.0 magic rebuild (appended): the own effect triples of Transmutation, Recombination and Modification (Mercury's MERCURY_FX1..3 exist above)
    TRANSMUTATION_FX1, TRANSMUTATION_FX2, TRANSMUTATION_FX3,
    RECOMBINATION_FX1, RECOMBINATION_FX2, RECOMBINATION_FX3,
    MODIFICATION_FX1, MODIFICATION_FX2, MODIFICATION_FX3,     // Kiato: an ink after-image of the dancer      // the gem on the chest of the followed body while the core is exposed: a pulsing diamond, a halo and rising motes; power = size
    // 1.93.0 (Water / Dark / Lightning / Flame rebuild): CAST = a travelling effect from 'from' to 'to', ZONE = a field at 'from' (power = radius), BURST = an impact at 'from' (client.layer.WaterFxLayer ...)
    WATERFX_CAST,
    WATERFX_ZONE,
    WATERFX_BURST,
    DARKFX_CAST,
    DARKFX_ZONE,
    DARKFX_BURST,
    LIGHTNINGFX_CAST,
    LIGHTNINGFX_ZONE,
    LIGHTNINGFX_BURST,
    FLAMEFX_CAST,
    FLAMEFX_ZONE,
    FLAMEFX_BURST,     // Kiato: an ink after-image of the dancer      // the gem on the chest of the followed body while the core is exposed: a pulsing diamond, a halo and rising motes; power = size
    // 1.93.0 nature-magic rebuild (vfx.client.layer.TreeLayer, VineLayer, PlantLayer, PoisonPlantLayer, MudLayer, MucusLayer): the shape protocol of the attribute guide for each magic.
    // FX1 = cast / projectile (from = origin, to = target, power = size), FX2 = zone (from = centre on the ground, power = RADIUS), FX3 = impact / signature (from = centre, to = direction hint, power = scale)
    TREE_FX1,
    TREE_FX2,
    TREE_FX3,
    VINE_FX1,
    VINE_FX2,
    VINE_FX3,
    PLANT_FX1,
    PLANT_FX2,
    PLANT_FX3,
    POISON_PLANT_FX1,
    POISON_PLANT_FX2,
    POISON_PLANT_FX3,
    MUD_FX1,
    MUD_FX2,
    MUD_FX3,
    MUCUS_FX1,
    MUCUS_FX2,
    MUCUS_FX3,
    // 1.99.0 Mana Zone (vfx.client.layer.ManaZoneLayer). Protocol: to.x = ticks already elapsed since the start tick (the radius is computed from it, so a refresh does not restart the growth),
    // to.y = style (ManaZoneGeometry.STYLE_*), to.z = growth ticks, power = the radius cap, seed = the zone's serial; SHELL = the sphere of ambient mana + its glow, GROUND = rings, footholds, motes, flow lines;
    // BURST: to.x = 0 opening pulse, 1 collapse finisher; BUBBLE = a pressurised protective space (from = centre, power = radius); GRAND_ZONE_ENV = the weather and scenery of a Grand Magic Zone round the viewer
    // (to.y = theme, to.z = effect mask, power = 0..1 intensity, colour = the zone's tint)
    MANA_ZONE_SHELL,
    MANA_ZONE_GROUND,
    MANA_ZONE_BURST,
    MANA_ZONE_BUBBLE,
    GRAND_ZONE_ENV,
    // 1.102.0 visual upgrade (vfx.client.layer.UpgradeSignatureLayer): the signature effect of an act of an upgraded weapon. The packet's seed carries the signature id of (item, act) in its upper 32 bits
    // (relics.upgrade.UpgradeItems); from = the caster (the effect follows him), to = the point he looks at; the recipe is read from assets/nusmp/visual_upgrade/<item>.json
    UPGRADE_SIGNATURE,
    // 1.110.0 (vfx.client.layer.DeckSummonLayer): the card summon of the Deck of Many Things from the worn duel disk; the seed carries the card, the zone and the alignment (vfx.deck.DeckSummonGeometry.pack); follows the player
    DECK_SUMMON,
    // Storm Magic of the Stormcaller Staff (vfx.client.layer.StormLayer). The seed carries the weather parts and the spell (fairytail.storm.StormMath.packSeed); every position in time is a function of (seed, age).
    // STORM_CLOUD: from = ground centre at the start, to = at the end of the drift, power = radius: the dark cloud deck, the funnel, the eye. STORM_FALL: the same arguments: rain curtains, hail, gust rings.
    // STORM_BOLT: from = the cloud, to = the mark: one forked bolt. STORM_FRONT: from = start of the line, to = its far end, power = half width: a gale front or a storm surge. STORM_CANCEL: ends the storm of that seed early.
    STORM_CLOUD,
    STORM_FALL,
    STORM_BOLT,
    STORM_FRONT,
    STORM_CANCEL,
    // Star Magic nodes (vfx.client.layer.StarMagicLayer, docs/vfx_remake/star_ingame.md); the seed carries a flag the layer reads. STAR_NODE: from = the hand, to = the star, seed 0 = a star forming (comet + ground sigil);
    // STAR_LINK: one constellation line from -> to (seed = the line's index in the loop); STAR_CONJ: Conjunction, from = where the mage left, to = where they arrive; STAR_PANE: a pane hit (seed 0) or its breaking (seed 1) at
    // from, the pane centred on to; STAR_LANCE: the spear of light of Quartile Hasta from -> to (power = width); STAR_WHIP: one leg of the whip (seed = its index 0..4, so the ribbon tapers along the legs);
    // STAR_DOME: Black Neverland round from, power = radius. STAR_MAGIC stays the old shape of every other page.
    STAR_NODE,
    STAR_LINK,
    STAR_CONJ,
    STAR_PANE,
    STAR_LANCE,
    STAR_WHIP,
    STAR_DOME;

    public static VfxShape byId(int id) {
        VfxShape[] v = values();
        return id >= 0 && id < v.length ? v[id] : MAGIC_CIRCLE;
    }
}
