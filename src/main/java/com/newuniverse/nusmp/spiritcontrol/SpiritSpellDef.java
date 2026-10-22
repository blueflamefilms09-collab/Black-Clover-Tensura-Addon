package com.newuniverse.nusmp.spiritcontrol;

import java.util.Locale;

/**
 * Why: one spell a spirit may cast on its own in combat mode, as plain data (pure, no Minecraft types; the game side resolves {@link #shape} to a VfxShape and applies the {@link Effect}). The decision rules
 * ({@link CombatRules}) only read the numbers.
 *
 * @param raw      base damage before the balance law's scaling (a starter page is about 7)
 * @param units    price in mana units; the cast splits it between the spirit's pool and the user's ({@link ManaSplit})
 * @param cooldownTicks wait before this spell may be cast again by this spirit
 * @param range    furthest target distance in blocks
 * @param shape    name of the VfxShape the cast draws (the self-test checks it exists)
 * @param color    ARGB tint of the effect
 * @param sound    short key of the cast sound (fire, water, wind, earth, light, dark, space)
 */
public record SpiritSpellDef(String id, String label, float raw, double units, int cooldownTicks, double range, Effect effect, String shape, int color, String sound) {
    /** What a hit adds besides damage. */
    public enum Effect { NONE, IGNITE, SLOW, KNOCKUP, GLOW, WEAKEN }

    public SpiritSpellDef {
        if (id == null || id.isBlank() || label == null || shape == null || sound == null || effect == null) throw new IllegalArgumentException("spell fields");
        id = id.toLowerCase(Locale.ROOT);
        if (raw < 0 || units <= 0 || cooldownTicks < 1 || range <= 0) throw new IllegalArgumentException("spell numbers");
    }
}
