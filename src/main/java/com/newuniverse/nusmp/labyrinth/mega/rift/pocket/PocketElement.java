package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import java.util.Locale;

/**
 * Why: the elemental traps of the ancient tombs ("fire, plants ... that trigger on contact", the owner's Black Clover canon) as DATA of one table: for every element, what it looks like (particle
 * and colour), what it sounds like, which kind of damage it deals, which status it leaves and how hard it hits. Every id is a string of the vanilla registries (a particle, a sound, a damage type,
 * a mob effect), so this class holds no Minecraft type and a test can check the table; {@code PocketTraps} resolves the strings at run time and falls back to a plain magic hit if one does not
 * resolve. The damage is a SHARE of the victim's maximum health, capped by {@link PocketRules#trapDamage}, never lethal; NO element uses hunger or exhaustion (the owner's strict ban). The
 * element also names the family of magic that unravels the matching seal ({@link PocketDef.Seal}) and the Black Clover attribute that resonates with the pocket.
 */
public enum PocketElement {
    //            id          particle                        rgb       sound                                      damage type          effect                  s  amp ignite share
    FIRE("fire", "minecraft:flame", 0xFF6A1A, "minecraft:entity.blaze.shoot", "minecraft:in_fire", "", 0, 0, 4, 0.12),
    PLANT("plant", "minecraft:spore_blossom_air", 0x58C23A, "minecraft:entity.evoker_fangs.attack", "minecraft:cactus", "minecraft:slowness", 4, 2, 0, 0.08),
    FROST("frost", "minecraft:snowflake", 0x9FE3FF, "minecraft:block.glass.break", "minecraft:freeze", "minecraft:slowness", 4, 1, 0, 0.10),
    LIGHTNING("lightning", "minecraft:electric_spark", 0xFFF07A, "minecraft:entity.lightning_bolt.impact", "minecraft:lightning_bolt", "minecraft:weakness", 4, 0, 0, 0.14),
    POISON("poison", "minecraft:sneeze", 0x8BE04A, "minecraft:entity.spider.hurt", "minecraft:magic", "minecraft:poison", 5, 0, 0, 0.05),
    EARTH("earth", "minecraft:falling_dust", 0xB48A55, "minecraft:entity.iron_golem.damage", "minecraft:magic", "minecraft:slowness", 3, 2, 0, 0.08),
    WIND("wind", "minecraft:cloud", 0xCFEFE6, "minecraft:entity.phantom.flap", "minecraft:magic", "minecraft:levitation", 1, 0, 0, 0.05),
    DARK("dark", "minecraft:squid_ink", 0x6A2CA0, "minecraft:entity.wither.ambient", "minecraft:wither", "minecraft:darkness", 4, 0, 0, 0.08),
    LIGHT("light", "minecraft:end_rod", 0xFFF3B0, "minecraft:block.beacon.activate", "minecraft:magic", "minecraft:blindness", 2, 0, 0, 0.10),
    WATER("water", "minecraft:bubble_pop", 0x4AA8FF, "minecraft:entity.generic.splash", "minecraft:magic", "minecraft:nausea", 4, 0, 0, 0.05),
    TIME("time", "minecraft:reverse_portal", 0xB9A3FF, "minecraft:block.respawn_anchor.charge", "minecraft:magic", "minecraft:slowness", 5, 3, 0, 0.04),
    BLOOD("blood", "minecraft:crimson_spore", 0xC01818, "minecraft:entity.vex.charge", "minecraft:wither", "minecraft:wither", 3, 0, 0, 0.07);

    private final String id, particle, sound, damageType, effect;
    private final int rgb, effectSeconds, effectAmplifier, igniteSeconds;
    private final double share;

    PocketElement(String id, String particle, int rgb, String sound, String damageType, String effect, int effectSeconds, int effectAmplifier, int igniteSeconds, double share) {
        this.id = id;
        this.particle = particle;
        this.rgb = rgb;
        this.sound = sound;
        this.damageType = damageType;
        this.effect = effect;
        this.effectSeconds = effectSeconds;
        this.effectAmplifier = effectAmplifier;
        this.igniteSeconds = igniteSeconds;
        this.share = share;
    }

    public String id() { return id; }

    /** The registry id of the rune block at the middle of a trap tile of this element (a patterned block of its colour, so a careful delver can read the floor). */
    public String rune() {
        return switch (this) {
            case FIRE -> "minecraft:orange_glazed_terracotta";
            case PLANT -> "minecraft:lime_glazed_terracotta";
            case FROST -> "minecraft:light_blue_glazed_terracotta";
            case LIGHTNING -> "minecraft:yellow_glazed_terracotta";
            case POISON -> "minecraft:green_glazed_terracotta";
            case EARTH -> "minecraft:brown_glazed_terracotta";
            case WIND -> "minecraft:white_glazed_terracotta";
            case DARK -> "minecraft:purple_glazed_terracotta";
            case LIGHT -> "minecraft:glowstone";
            case WATER -> "minecraft:blue_glazed_terracotta";
            case TIME -> "minecraft:magenta_glazed_terracotta";
            case BLOOD -> "minecraft:red_glazed_terracotta";
        };
    }

    /** The vanilla particle type id the trap draws. */
    public String particle() { return particle; }

    public int rgb() { return rgb; }

    public String sound() { return sound; }

    /** The vanilla damage type id of the hit. */
    public String damageType() { return damageType; }

    /** The vanilla mob effect id it leaves ("" = none). */
    public String effect() { return effect; }

    public int effectSeconds() { return effectSeconds; }

    public int effectAmplifier() { return effectAmplifier; }

    /** Seconds the victim burns (0 = not at all). */
    public int igniteSeconds() { return igniteSeconds; }

    /** The hit as a share of the victim's maximum health, before the caps of {@link PocketRules#trapDamage}. */
    public double share() { return share; }

    public static PocketElement byId(String id) {
        if (id == null) return null;
        String s = id.toLowerCase(Locale.ROOT);
        for (PocketElement e : values()) if (e.id.equals(s)) return e;
        return null;
    }

    /** True for a status the owner's hunger ban forbids (none of the table's may be one; the tests walk the table). */
    public static boolean forbiddenEffect(String effectId) { boolean refuses = "minecraft:hunger".equals(effectId); return refuses || "minecraft:saturation".equals(effectId); }
}
