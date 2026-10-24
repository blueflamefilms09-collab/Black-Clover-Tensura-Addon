package com.newuniverse.nusmp.mana.method;

/**
 * How True Magic hurts (pure, tested). Normal Mana Method bolts hurt through the grimoire's Tensura magic damage type, which Tensura's magic resistance and the magic-blocking skills read.
 * True Magic is "the real element": the actual fire, water, wind, stone, lightning or plant, not a spell shaped like it. So with the setting {@code trueMagic.physicalDamage} its damage
 * comes from a vanilla PHYSICAL / ENVIRONMENTAL damage type instead (attributed to the caster, so a kill is credited to them), and nothing that reads "magic damage" sees it.
 *
 * <p><b>Anti-Magic.</b> The Anti-Magic Lord's Black Form, its Physique, the Magebane shield and the other defences of the mod decide by reading the damage type
 * ({@code AntiMagic.isMagic}). A physical type therefore slips past all of them. The setting {@code trueMagic.antiMagicStillCancels} (default true) keeps Anti-Magic working: against a
 * target that is an Anti-Magic user the old magic route is taken, so Black Form cancels True Magic as it cancels any other magic. With it off, True Magic really does pass Anti-Magic.
 */
public final class TrueDamageRules {
    private TrueDamageRules() {}

    /** MAGIC_UNDONE = the 3.4.0 route (the grimoire's magic damage, the target's resistance cut undone); PHYSICAL = a vanilla physical / environmental type, no resistance to undo. */
    public enum Route { MAGIC_UNDONE, PHYSICAL }

    /** The vanilla damage types True Magic hurts with (the path of {@code minecraft:<path>}). */
    public enum Kind {
        FIRE("in_fire"), KINETIC("player_attack"), WIND("fly_into_wall"), STONE("falling_block"), LIGHTNING("lightning_bolt");

        public final String path;

        Kind(String path) { this.path = path; }
    }

    /**
     * The route of one hit. Not True Magic: always the old route (it is the grimoire's normal magic). True Magic: physical when the setting is on, except against an Anti-Magic user while
     * {@code antiMagicStillCancels} is on.
     */
    public static Route route(boolean trueMagic, boolean physicalSetting, boolean antiMagicStillCancels, boolean targetIsAntiMagicUser) {
        if (!trueMagic || !physicalSetting) return Route.MAGIC_UNDONE;
        return antiMagicStillCancels && targetIsAntiMagicUser ? Route.MAGIC_UNDONE : Route.PHYSICAL;
    }

    /** The damage type of an element's True Magic: real fire burns, a wave and a growing branch strike like a heavy blow, wind throws its target at a wall, stone falls, lightning is lightning. */
    public static Kind kindOf(RuneElement e) {
        if (e == null) return Kind.KINETIC;
        return switch (e) {
            case FIRE -> Kind.FIRE;
            case WATER, PLANT -> Kind.KINETIC;
            case WIND -> Kind.WIND;
            case EARTH -> Kind.STONE;
            case LIGHTNING -> Kind.LIGHTNING;
        };
    }

    /** The factor of the cancelled resistance for a route: the old route undoes it (see {@link RuneRules#bypassFor}), the physical route has nothing to undo. */
    public static double resistanceFactor(Route route, double resistance, boolean boss) {
        return route == Route.MAGIC_UNDONE ? RuneRules.bypassFor(resistance, boss) : 1.0;
    }
}
