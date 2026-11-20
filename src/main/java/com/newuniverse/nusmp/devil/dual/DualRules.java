package com.newuniverse.nusmp.devil.dual;

import com.newuniverse.nusmp.devil.DevilAttribute;
import com.newuniverse.nusmp.devil.DevilRules;
import com.newuniverse.nusmp.devil.DevilTier;

/**
 * Why: the numbers and decisions of Devil Magic, the Ultimate skill a devil contract gives (docs/devil_double_magic.md), with no Minecraft class in them so a unit test and the CI self-test can pin them.
 * The skill has two arts: the devil's own art (mode 0) and the double art (mode 1), which adds the grimoire magic the player was born with to the devil's attribute (the Dark Triad of the anime, who use their own
 * magic and their devil's). Every number is bounded: at most {@link #MAX_TARGETS} foes a cast, a cooldown of at least eight seconds, a price that grows with the tier.
 */
public final class DualRules {
    private DualRules() {}

    public static final int MAX_TARGETS = 6;
    public static final double RANGE = 10.0, CONE = 0.35, COMBO_FACTOR = 1.25;
    /** Ticks between two casts of one art. */
    public static final int COOLDOWN_ART = 160, COOLDOWN_DOUBLE = 240;
    /** Hard ceiling of one hit's raw number before the balance law (the law's own caps come after). */
    public static final double MAX_RAW = 200.0;

    /** The raw damage of one hit of the art (before the balance law scales it with the caster's existence points). */
    public static double baseDamage(DevilTier t) {
        return switch (t) { case LOW -> 10.0; case MID -> 16.0; case HIGH -> 26.0; case SUPREME -> 40.0; default -> 0.0; };
    }

    /** The flat price in magicules (the class price of the skill raises it with the caster's pool). */
    public static double cost(DevilTier t, boolean combo) {
        double base = switch (t) { case LOW -> 300.0; case MID -> 500.0; case HIGH -> 800.0; case SUPREME -> 1200.0; default -> 0.0; };
        return combo ? base * 1.5 : base;
    }

    /** The raw hit: the tier's base times (1 + the attribute's bonus), 1.25 times that for the double art, times the server's scale; never above {@link #MAX_RAW}, never negative. */
    public static double damage(DevilTier t, double attributeBonus, boolean combo, double scale) {
        double v = baseDamage(t) * (1.0 + Math.max(0.0, Math.min(3.0, attributeBonus))) * (combo ? COMBO_FACTOR : 1.0) * Math.max(0.0, Math.min(10.0, scale));
        return Math.max(0.0, Math.min(MAX_RAW, v));
    }

    public static int cooldown(boolean combo) { return combo ? COOLDOWN_DOUBLE : COOLDOWN_ART; }

    /** The name of the grimoire magic (a {@code MagicType} name) the rules of the game see this devil attribute as, so a devil's Time counts as Time magic where a rule asks. Null for NONE. */
    public static String asMagic(DevilAttribute a) {
        if (a == null) return null;
        return switch (a) {
            case NONE -> null;
            case GRAVITY -> "GRAVITY";
            case SPATIAL -> "SPATIAL";
            case CURSE -> "CURSE";
            case ANTI_MAGIC -> "ANTI_MAGIC";
            case FLAME -> "FLAME";
            case WORD -> "KOTODAMA";
            case TIME -> "TIME";
            case BEAST -> "BEAST";
            case FORESIGHT -> "EYE";
            case GHOST -> "SOUL_CORPSE";
            case SOUL -> "SOUL";
            case ROT -> "POISON";
            case EARTH -> "EARTH";
            case WATER -> "WATER";
            case LIGHT -> "LIGHT";
            case ETHER -> "STAR";
            case CAUSALITY -> "DICE";
        };
    }

    /** The colour (0xRRGGBB) of the attribute's emblem and of the art's pictures. */
    public static int color(DevilAttribute a) {
        if (a == null) return 0x888888;
        return switch (a) {
            case NONE -> 0x888888;
            case GRAVITY -> 0x7A3CC8;
            case SPATIAL -> 0x3A4AC8;
            case CURSE -> 0x8A2A5A;
            case ANTI_MAGIC -> 0x606060;
            case FLAME -> 0xFF6A2A;
            case WORD -> 0xB06AFF;
            case TIME -> 0xF5D76E;
            case BEAST -> 0xE08A2A;
            case FORESIGHT -> 0x3AC8C8;
            case GHOST -> 0x9AA8C8;
            case SOUL -> 0xC81E2D;
            case ROT -> 0x6A8A2A;
            case EARTH -> 0x8A6A3A;
            case WATER -> 0x3A8AE8;
            case LIGHT -> 0xFFF0A8;
            case ETHER -> 0x6A3CFF;
            case CAUSALITY -> 0x8A0A14;
        };
    }

    /** True for the devil attribute that counts as Time magic. */
    public static boolean isTime(DevilAttribute a) { return a == DevilAttribute.TIME; }

    // ---------------------------------------------------------------- what a grimoire magic adds in the double art
    /** The effect the double art puts on a foe for the grimoire magic's hit kind (a {@code MagicType.HitEffect} name): a mob effect, or a special push / pull / burn / drain / strip. */
    public record Hit(String effectId, int ticks, int amp, String special, double bonus) {}

    private static final Hit NO_HIT = new Hit("", 0, 0, "", 0.0);

    public static Hit hit(String hitEffect) {
        if (hitEffect == null) return NO_HIT;
        return switch (hitEffect) {
            case "BURN" -> new Hit("", 0, 0, "burn", 0.0);
            case "FREEZE" -> new Hit("minecraft:slowness", 100, 3, "", 0.0);
            case "SHOCK" -> new Hit("minecraft:slowness", 20, 2, "", 0.10);
            case "SLOW" -> new Hit("minecraft:slowness", 120, 3, "", 0.0);
            case "POISON" -> new Hit("minecraft:poison", 160, 1, "", 0.0);
            case "WITHER" -> new Hit("minecraft:wither", 140, 1, "", 0.0);
            case "BLIND" -> new Hit("minecraft:blindness", 100, 0, "", 0.0);
            case "LEVITATE" -> new Hit("minecraft:levitation", 40, 1, "", 0.0);
            case "PULL" -> new Hit("", 0, 0, "pull", 0.0);
            case "PUSH" -> new Hit("", 0, 0, "push", 0.0);
            case "WEAKEN" -> new Hit("minecraft:weakness", 160, 2, "", 0.0);
            case "PIERCE" -> new Hit("", 0, 0, "", 0.15);
            case "DRAIN" -> new Hit("", 0, 0, "drain", 0.0);
            case "NULLIFY" -> new Hit("", 0, 0, "strip", 0.0);
            default -> NO_HIT;
        };
    }

    // ---------------------------------------------------------------- what the devil's own art does, per attribute
    /** What the art puts on a foe: a mob effect ("" = none), a special ("burn", "pull", "push" or ""), whether it strips protections, the share of the damage healed back, the attribute's damage bonus. */
    public record Art(String effectId, int ticks, int amp, String special, boolean strip, double heal, double bonus) {
        /** True when the art does something besides damage (so no attribute is a dead ability). */
        public boolean active() { return !effectId.isEmpty() || !special.isEmpty() || strip || heal > 0; }
    }

    /**
     * The art of an attribute: the effect of {@link DevilRules#effect} (the same one the host's blows carry) plus a push or pull so each attribute has a shape of its own. Attributes whose blow effect is only
     * damage (Spatial, Beast, Ether) get a mob effect or a push here. NONE has no art.
     */
    public static Art art(DevilAttribute a) {
        if (a == null || a == DevilAttribute.NONE) return new Art("", 0, 0, "", false, 0, 0);
        DevilRules.Effect e = DevilRules.effect(a);
        String id = e.effectId();
        int ticks = e.ticks(), amp = e.amp();
        String special = switch (a) {
            case GRAVITY, BEAST, GHOST -> "pull";
            case SPATIAL, ETHER, EARTH, WATER, LIGHT -> "push";
            case FLAME -> "burn";
            default -> "";
        };
        if (id.equals("anime_multiverse:burn")) { id = ""; ticks = 0; amp = 0; }                       // fire is set directly, not as a mob effect
        if (id.isEmpty() && !e.strip() && e.heal() <= 0 && special.isEmpty()) special = "push";
        if (id.isEmpty() && a == DevilAttribute.SPATIAL) { id = "minecraft:nausea"; ticks = 100; }
        if (id.isEmpty() && a == DevilAttribute.ETHER) { id = "minecraft:levitation"; ticks = 30; amp = 0; }
        if (id.isEmpty() && a == DevilAttribute.CAUSALITY) { id = "minecraft:unluck"; ticks = 200; amp = 1; }
        return new Art(id, ticks, amp, special, e.strip(), e.heal(), e.bonus());
    }

    // ---------------------------------------------------------------- the grant of the skill
    public enum Verdict { GRANT, REVOKE, KEEP }

    /** The one rule of the grant: a contract holder has the skill exactly once, anybody else has none. Asking twice never grants twice. */
    public static Verdict verdict(boolean shouldHave, boolean has) {
        if (shouldHave && !has) return Verdict.GRANT;
        if (!shouldHave && has) return Verdict.REVOKE;
        return Verdict.KEEP;
    }
}
