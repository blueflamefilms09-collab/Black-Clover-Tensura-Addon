package com.newuniverse.nusmp.elite.items.weapon;

import java.util.List;

/**
 * Why: one technique of an elite weapon as DATA. The six weapons share a handful of technique KINDS (a long slash, a chain of marked foes, a rift that pulls, a blink, a nova, a ward) and differ in the
 * numbers, the colour and the effects, so the numbers live in {@code data/nusmp/elite/items/abilities/<item>.json} (keyed by the item id: the Void Edge's file is how a sword is "paired" with the Void
 * Edge skill another builder makes, with no import) and a datapack can retune a weapon without a rebuild. Pure (no Minecraft types), parsed by {@link AbilityParser}, run by {@code AbilityRunner}.
 *
 * @param kind        one of {@link AbilityParser#KINDS}
 * @param name        the technique's name (shown on the action bar)
 * @param desc        one line for the tooltip
 * @param cooldown    ticks (the config scales it)
 * @param damage      base damage, run through the balance law like every other technique
 * @param range       blocks: the length of a slash or blink, the reach of a rift, the search radius of a chain
 * @param radius      blocks: the area of a nova, a rift, a ward
 * @param width       blocks: the width of a slash
 * @param count       how many foes a chain marks, or how many pulses a rift makes
 * @param pull        how hard a rift drags foes to its centre (0 for none)
 * @param knockback   how hard a nova throws foes away (0 for none)
 * @param strip       how many beneficial effects it strips from each foe hit
 * @param heal        health restored to the wielder (a ward: Absorption health)
 * @param runeScale   damage and heal grow by this share per slotted rune (Runic Swords)
 * @param color       the VFX colour, 0xRRGGBB
 * @param dark        true = Dark Rukh and a dark tone, false = Light Rukh
 * @param vfx         the mod VFX shape name ({@code WIND_SLASH}, {@code SPATIAL_RIFT} ...), empty for the kind's default
 * @param foeEffects  mob effects put on foes that are hit
 * @param selfEffects mob effects put on the wielder (and allies for a ward / nova)
 */
public record AbilitySpec(String kind, String name, String desc, int cooldown, float damage, double range, double radius, double width, int count, double pull, double knockback,
                          int strip, float heal, double runeScale, int color, boolean dark, String vfx, List<EffectSpec> foeEffects, List<EffectSpec> selfEffects) {
    public AbilitySpec {
        foeEffects = foeEffects == null ? List.of() : List.copyOf(foeEffects);
        selfEffects = selfEffects == null ? List.of() : List.copyOf(selfEffects);
    }

    /** A mob effect to apply: its id ({@code minecraft:slowness}), the duration in ticks and the amplifier (0 = level I). */
    public record EffectSpec(String id, int ticks, int amp) {}
}
