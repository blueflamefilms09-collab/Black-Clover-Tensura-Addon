package com.newuniverse.nusmp.relics.djinn.weapon;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The forty-eight acts of the twenty-four Djinn weapons added in 1.93.0, as data (pure, so a test can check every one): each is one of ten shapes
 * ({@link Kind}) with its own effects, shove and fire. The twelve original weapons keep their hand-written acts in {@code DjinnWeaponActs}; an act
 * key that is not here is one of those. The raw damage, the cost, the cooldown and the reach of an act are the weapon row's ({@link DjinnWeaponRules});
 * this table only says what the act is and what it does besides the damage.
 *
 * <p>An {@link Eff} id is a mob effect ("minecraft:slowness", "tensura:fear"), or one of the pseudo ids {@link #SILENCE}, {@link #FREEZE},
 * {@link #CLEANSE} and {@link #UNVEIL}. An id the game does not know is skipped by the effect helpers, so a wrong guess costs only the effect.
 */
public final class ActLibrary {
    private ActLibrary() {}

    /** The shapes of the generic acts. */
    public enum Kind {
        /** A line along the gaze, up to {@code count} foes in it. */
        BEAM,
        /** A fan in front of the bearer ({@code arc} is the half angle in degrees). */
        CONE,
        /** A burst round the bearer. */
        NOVA,
        /** A dash forward that hurts what it passes. */
        DASH,
        /** A broad band ahead of the bearer ({@code arc} is its half width in blocks). */
        WALL,
        /** Marks up to {@code count} places (on foes first, else ahead); each is struck a moment later. */
        PILLARS,
        /** Jumps from foe to foe, {@code count} links, a little weaker at each. */
        CHAIN,
        /** Heals the bearer and allies near ({@code heal} times the power), hurts the foes near. */
        HEAL,
        /** Drags the foes near in towards the bearer. */
        PULL,
        /** A ward round the bearer: Absorption ({@code heal} points), the self effect, and a push on the foes near. */
        SHIELD
    }

    /** One effect: an id, its length in ticks and its amplifier. */
    public record Eff(String id, int ticks, int amp) {
        public boolean none() { return id == null || id.isEmpty() || ticks <= 0; }
    }

    /** The effect-less value. */
    public static final Eff NO = new Eff("", 0, 0);

    /** Pseudo effect ids: silence (the energy system's), frozen ticks on the target, cleansing the harmful effects of an ally, stripping invisibility. */
    public static final String SILENCE = "silence", FREEZE = "freeze", CLEANSE = "cleanse", UNVEIL = "unveil";

    /**
     * One generic act. {@code mult} scales the act's power for the foes (1.0 = the row's power); {@code arc} is the half angle of a cone, the half
     * width of a wall or the width of a beam; {@code count} the foes a beam pierces, the links of a chain, the marks of pillars; {@code eff1} and
     * {@code eff2} go on every foe hit, {@code self} on the bearer; {@code push} and {@code lift} shove the foes (negative push: none); {@code fire}
     * is the fire ticks set on foes; {@code heal} is a share of the power healed to allies (or the Absorption points of a shield); {@code leech} a
     * share of the damage dealt returned to the bearer as health.
     */
    public record Gen(Kind kind, double mult, double arc, int count, Eff eff1, Eff eff2, Eff self, double push, double lift, int fire, double heal, double leech) {}

    private static final Map<String, Gen> TABLE = new LinkedHashMap<>();

    private static Eff e(String id, int ticks, int amp) { return new Eff(id, ticks, amp); }

    private static void put(String key, Gen g) { TABLE.put(key, g); }

    private static Gen g(Kind k, double mult, double arc, int count, Eff e1, Eff e2, Eff self, double push, double lift, int fire, double heal, double leech) {
        return new Gen(k, mult, arc, count, e1, e2, self, push, lift, fire, heal, leech);
    }

    static {
        // BAEL: a king's command
        put("royal_decree", g(Kind.CONE, 1.0, 55, 0, e("minecraft:slowness", 70, 2), NO, NO, 0.2, 0.0, 0, 0, 0));
        put("throne_quake", g(Kind.NOVA, 1.0, 0, 0, e("minecraft:weakness", 120, 1), e("minecraft:slowness", 80, 1), NO, 0.5, 0.6, 0, 0, 0));
        // AGARES: crocodile tides
        put("tidal_lunge", g(Kind.DASH, 1.0, 0, 0, e("minecraft:slowness", 50, 1), NO, NO, 0.4, 0.1, 0, 0, 0));
        put("undertow", g(Kind.PULL, 0.6, 0, 0, e("minecraft:slowness", 80, 2), NO, NO, 0.0, 0.0, 0, 0, 0));
        // VALEFOR: the storm thief
        put("spark_snatch", g(Kind.CHAIN, 1.0, 0, 3, e("minecraft:weakness", 60, 0), NO, NO, 0.0, 0.0, 0, 0, 0));
        put("storm_heist", g(Kind.DASH, 1.0, 0, 0, e("minecraft:slowness", 40, 2), NO, e("minecraft:speed", 80, 1), 0.3, 0.1, 0, 0, 0));
        // BARBATOS: the wild hunt
        put("wild_arrow", g(Kind.BEAM, 1.0, 0.7, 2, e("minecraft:poison", 80, 0), NO, NO, 0.0, 0.0, 0, 0, 0));
        put("green_ambush", g(Kind.PILLARS, 1.0, 0, 3, e("minecraft:slowness", 80, 3), e("minecraft:poison", 100, 0), NO, 0.0, 0.0, 0, 0, 0));
        // GUSION: the tide mirror
        put("mirror_thrust", g(Kind.BEAM, 1.0, 0.9, 3, e("minecraft:glowing", 120, 0), NO, NO, 0.0, 0.0, 0, 0, 0));
        put("tide_mirror", g(Kind.SHIELD, 0.6, 0, 0, e("minecraft:slowness", 60, 1), NO, e("minecraft:resistance", 100, 1), 0.6, 0.1, 0, 6.0, 0));
        // VEPAR: wrecks
        put("anchor_drop", g(Kind.NOVA, 1.0, 0, 0, e("tensura:fragility", 140, 0), NO, NO, 0.5, 0.55, 0, 0, 0));
        put("shipwreck", g(Kind.WALL, 1.0, 4.0, 0, e("minecraft:slowness", 100, 2), NO, NO, 0.8, 0.2, 0, 0, 0));
        // SHAX: the silencing dove
        put("wing_hush", g(Kind.CONE, 1.0, 50, 0, e(SILENCE, 100, 0), e("minecraft:slowness", 40, 0), NO, 0.5, 0.1, 0, 0, 0));
        put("muted_flurry", g(Kind.NOVA, 0.8, 0, 0, e(SILENCE, 140, 0), e("minecraft:nausea", 100, 0), NO, 0.8, 0.2, 0, 0, 0));
        // BUER: the healing wheel
        put("wheel_mend", g(Kind.HEAL, 0.5, 0, 0, e("minecraft:weakness", 80, 0), NO, e("minecraft:regeneration", 80, 0), 0.0, 0.0, 0, 1.0, 0));
        put("cleansing_spokes", g(Kind.HEAL, 0.5, 0, 0, e("minecraft:slowness", 80, 1), NO, e(CLEANSE, 1, 0), 0.0, 0.0, 0, 1.0, 0));
        // ANDREALPHUS: the star peacock
        put("star_spear", g(Kind.BEAM, 1.0, 0.8, 2, e("minecraft:blindness", 40, 0), NO, NO, 0.0, 0.0, 0, 0, 0));
        put("peacock_fan", g(Kind.CONE, 1.0, 55, 0, e("minecraft:blindness", 100, 0), e("minecraft:glowing", 140, 0), NO, 0.6, 0.1, 0, 0, 0));
        // KIMARIS: the dark steed
        put("charge_lance", g(Kind.DASH, 1.0, 0, 0, e("minecraft:slowness", 40, 1), NO, NO, 0.9, 0.3, 0, 0, 0));
        put("dark_gallop", g(Kind.DASH, 1.0, 0, 0, e("minecraft:weakness", 80, 1), NO, e("minecraft:speed", 80, 1), 1.3, 0.4, 0, 0, 0));
        // AMDUSIAS: the unicorn horn
        put("horn_note", g(Kind.BEAM, 1.0, 0.8, 3, e("minecraft:nausea", 60, 0), NO, NO, 0.5, 0.1, 0, 0, 0));
        put("grand_chord", g(Kind.NOVA, 1.0, 0, 0, e(SILENCE, 80, 0), e("minecraft:slowness", 60, 1), NO, 1.1, 0.25, 0, 0, 0));
        // BELIAL: the hollow throne
        put("hollow_cleave", g(Kind.CONE, 1.0, 65, 0, e("minecraft:weakness", 80, 0), NO, NO, 0.4, 0.1, 140, 0, 0));
        put("throne_ash", g(Kind.NOVA, 1.0, 0, 0, e("minecraft:blindness", 80, 0), NO, NO, 0.7, 0.3, 120, 0, 0));
        // DECARABIA: the star crab
        put("pincer_snap", g(Kind.CONE, 1.0, 35, 0, e("minecraft:slowness", 80, 4), NO, NO, 0.0, 0.0, 0, 0, 0));
        put("star_shell", g(Kind.SHIELD, 1.0, 0, 0, e("minecraft:blindness", 40, 0), NO, e("minecraft:resistance", 120, 1), 0.5, 0.1, 0, 8.0, 0));
        // SEERE: the swift wind
        put("swift_slash", g(Kind.DASH, 1.0, 0, 0, e("minecraft:slowness", 30, 0), NO, e("minecraft:speed", 40, 1), 0.2, 0.0, 0, 0, 0));
        put("gale_step", g(Kind.DASH, 1.0, 0, 0, NO, NO, e("minecraft:speed", 100, 2), 0.3, 0.2, 0, 0, 0));
        // DANTALION: the many faces
        put("forbidden_page", g(Kind.BEAM, 1.0, 0.9, 2, e("minecraft:nausea", 100, 0), e("minecraft:weakness", 60, 0), NO, 0.0, 0.0, 0, 0, 0));
        put("many_faces", g(Kind.PILLARS, 1.0, 0, 3, e("minecraft:nausea", 140, 0), e("minecraft:glowing", 100, 0), NO, 0.0, 0.0, 0, 0, 0));
        // ANDROMALIUS: what is hidden
        put("seeker_bolt", g(Kind.BEAM, 1.0, 0.8, 3, e("minecraft:glowing", 160, 0), NO, NO, 0.0, 0.0, 0, 0, 0));
        put("unveil", g(Kind.NOVA, 1.0, 0, 0, e("minecraft:glowing", 160, 0), e(UNVEIL, 1, 0), NO, 0.3, 0.1, 0, 0, 0));
        // GLASYA-LABOLAS: the hound
        put("savage_rend", g(Kind.CONE, 1.0, 60, 0, e("minecraft:wither", 80, 0), NO, NO, 0.3, 0.0, 0, 0, 0));
        put("bloodhound_rush", g(Kind.DASH, 1.0, 0, 0, e("minecraft:wither", 100, 0), NO, NO, 0.5, 0.1, 0, 0, 0.25));
        // HAAGENTI: the alchemist bull
        put("bull_gore", g(Kind.DASH, 1.0, 0, 0, e("minecraft:weakness", 60, 0), NO, NO, 1.0, 0.3, 0, 0, 0));
        put("great_work", g(Kind.NOVA, 1.0, 0, 0, e("minecraft:weakness", 100, 1), e("minecraft:poison", 80, 0), e("minecraft:regeneration", 100, 1), 0.4, 0.1, 0, 0, 0));
        // CROCELL: the frost sage
        put("frost_bolt", g(Kind.BEAM, 1.0, 0.8, 2, e("minecraft:slowness", 80, 2), e(FREEZE, 100, 0), NO, 0.0, 0.0, 0, 0, 0));
        put("rime_field", g(Kind.NOVA, 1.0, 0, 0, e(FREEZE, 140, 0), e("minecraft:slowness", 120, 2), NO, 0.2, 0.0, 0, 0, 0));
        // FURCAS: the ice wind
        put("ice_wind_sweep", g(Kind.CONE, 1.0, 80, 0, e(FREEZE, 80, 0), e("minecraft:slowness", 60, 1), NO, 1.1, 0.15, 0, 0, 0));
        put("knights_gale", g(Kind.DASH, 1.0, 0, 0, e("minecraft:slowness", 80, 2), e(FREEZE, 60, 0), NO, 0.8, 0.1, 0, 0, 0));
        // BALAM: three heads
        put("triple_flame", g(Kind.CHAIN, 1.0, 0, 3, NO, NO, NO, 0.0, 0.0, 100, 0, 0));
        put("prophecy", g(Kind.PILLARS, 1.0, 0, 3, e("minecraft:weakness", 80, 0), NO, NO, 0.0, 0.0, 120, 0, 0));
        // ALLOCES: the lion's eyes
        put("lion_slash", g(Kind.CONE, 1.0, 60, 0, NO, NO, NO, 0.5, 0.1, 100, 0, 0));
        put("lions_roar", g(Kind.NOVA, 1.0, 0, 0, e("minecraft:weakness", 120, 1), e("tensura:fear", 100, 0), NO, 0.7, 0.1, 0, 0, 0));
        // CAMIO: the crows
        put("crow_flock", g(Kind.CHAIN, 0.9, 0, 4, e("minecraft:slowness", 40, 0), NO, NO, 0.0, 0.0, 0, 0, 0));
        put("murder_of_crows", g(Kind.PILLARS, 1.0, 0, 3, e("tensura:fear", 100, 0), NO, NO, 0.0, 0.0, 0, 0, 0));
        // MURMUR: the drum
        put("death_drum", g(Kind.NOVA, 1.0, 0, 0, e("minecraft:nausea", 100, 0), NO, NO, 0.9, 0.3, 0, 0, 0));
        put("funeral_march", g(Kind.PULL, 1.0, 0, 0, e("minecraft:slowness", 100, 2), e("tensura:fear", 140, 0), NO, 0.0, 0.0, 0, 0, 0));
    }

    /** The generic act of this key, or null for one of the twelve original weapons' hand-written acts. */
    public static Gen gen(String key) { return TABLE.get(key); }

    public static boolean has(String key) { return TABLE.containsKey(key); }

    /** The keys of all forty-eight, in roster order. */
    public static java.util.Set<String> keys() { return java.util.Collections.unmodifiableSet(TABLE.keySet()); }
}
