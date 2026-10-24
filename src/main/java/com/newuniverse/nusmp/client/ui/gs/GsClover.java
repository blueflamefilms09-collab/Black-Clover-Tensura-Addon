package com.newuniverse.nusmp.client.ui.gs;

/**
 * Which clover frame goes around an icon: three leaves (common), four leaves (rare, glowing) or five leaves (forbidden / ultimate: slime, drips,
 * glow). The tier comes from the grimoire cover ({@code GrimoireCover.tier}) or the Tensura skill type; unknown values are three leaves.
 * The five-leaf frame always uses skin C's styling, for every user. Pure; takes names and numbers so it has no Minecraft or mod types.
 */
public final class GsClover {
    private GsClover() {}

    /** Leaves for a cover tier (3 = common, 4 = rare, 5 and above = five-leaf; anything below 3 is 3). */
    public static int leavesForTier(int tier) { return tier >= 5 ? 5 : tier == 4 ? 4 : 3; }

    /**
     * Leaves for a Tensura {@code Skill.SkillType} name: ULTIMATE is five, UNIQUE four, everything else (EXTRA, COMMON, RESISTANCE, unknown,
     * null) three.
     */
    public static int leavesForSkillType(String type) {
        if (type == null) return 3;
        return switch (type.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "ULTIMATE" -> 5;
            case "UNIQUE" -> 4;
            default -> 3;
        };
    }

    /** The skin a frame is drawn with: five leaves are always C (slime), the other tiers follow the user's skin. */
    public static GsSkin skinFor(int leaves, GsSkin userSkin) { return leaves >= 5 ? GsSkin.C : userSkin; }

    /** Only these many icons animate (pulse, drips) at once: the hovered one and the nearest ones; the rest draw static. */
    public static final int MAX_ANIMATED = 25;

    /** The glow pulse 0..1 of a four- or five-leaf frame at a time in ticks (a slow breath, phase per icon so a row does not blink together). */
    public static float pulse(float time, int phase) {
        return 0.5f + 0.5f * (float) Math.sin(time * 0.11f + phase * 1.7f);
    }
}
