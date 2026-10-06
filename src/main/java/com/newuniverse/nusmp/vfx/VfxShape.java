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
    TIME_ACCEL;              // Time Acceleration: dial + two ribbons whirling forwards around the caster (follows)

    public static VfxShape byId(int id) {
        VfxShape[] v = values();
        return id >= 0 && id < v.length ? v[id] : MAGIC_CIRCLE;
    }
}
