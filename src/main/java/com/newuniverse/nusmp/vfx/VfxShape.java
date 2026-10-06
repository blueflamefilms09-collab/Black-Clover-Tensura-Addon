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
    EARTH_RAMPAGE;           // Rampaging Mother Earth: a heaving wave of stone slabs rolling from -> to

    public static VfxShape byId(int id) {
        VfxShape[] v = values();
        return id >= 0 && id < v.length ? v[id] : MAGIC_CIRCLE;
    }
}
