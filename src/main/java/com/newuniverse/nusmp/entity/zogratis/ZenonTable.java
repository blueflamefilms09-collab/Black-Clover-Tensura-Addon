package com.newuniverse.nusmp.entity.zogratis;

import java.util.List;

/**
 * Zenon's move table (pure, unit tested): every move's numbers in one place, as {@link MoveSpec}s the scheduler can read without
 * Minecraft. {@code ZenonBoss} turns each entry into a {@link Move} by adding its telegraph and its code, looked up by id.
 * <p>
 * The shape of the fight: a fast assassin-caster who keeps a mid distance and strikes from odd angles. Bone Magic is the hand
 * (spears, blades, the lance, the garden, the armour); Beelzebub's Spatial Magic is the feet (the backstab, the portal link, the warp
 * barrage, the Spatial Cut). His hits are small and precise, never a single big number, and every one that matters is shown first:
 * a portal where he will appear, an aim line where a spear will leave, a ring where bone will erupt. The only ultimate, Spatial Mana
 * Domination, needs 60% power (the unbound mana) and leaves him exposed for three seconds.
 */
public final class ZenonTable {
    private ZenonTable() {}

    public static final String BONE_SPEARS = "bone_spears", BONE_BLADES = "bone_blades", BONE_GARDEN = "bone_garden", BACKSTAB = "rift_backstab",
            PORTAL_LINK = "portal_link", BONE_LANCE = "bone_lance", BONE_ARMOR = "bone_armor", WARP_BARRAGE = "warp_barrage",
            TENSURA_SPACE = "tensura_space", CALL_DISCIPLES = "call_disciples", DOMINATION = "spatial_domination",
            RIFT_CUTS = "rift_cuts", GATE_STORM = "gate_storm";

    /** Base damage of the moves (before the boss's damage law: power tuning, victim EP, the balance law's caps). */
    public static final float SPEAR = 5.5f, PORTAL_SPEAR = 6.5f, WARP_SPEAR = 5.0f, LANCE = 20f, BLADE = 6.5f, BACKSTAB_HIT = 13f,
            GARDEN = 11f, SHARDS = 5f;

    /** His build: medium health, light armour, very fast; precise blows (a damage factor a little over one). */
    public static final double HEALTH = 1350, ATTACK = 15, SPEED = 0.34, ARMOR = 8, TOUGHNESS = 4;
    public static final float DAMAGE_SCALE = 1.15f, HYBRID_SCALE = 1.10f;

    /** One row of the table. The record's accessors are the {@link MoveSpec} methods. */
    public record Spec(String id, String label, int minPower, int windUpTicks, int cooldownTicks, double minRange, double maxRange,
                       int weight, boolean ultimate, boolean crowd, int recovery) implements MoveSpec {
        /** The mana an ordinary move costs below 60% power (the same rule as {@link Move}): 6 + a sixteenth of its cooldown, at most 32; 60 for an ultimate. */
        @Override public double manaCost() { return ultimate ? 60 : Math.min(32, 6 + cooldownTicks / 16.0); }
    }

    private static final List<Spec> SPECS = List.of(
            new Spec(BONE_SPEARS, "Bone Spear Barrage", 20, 18, 100, 5, 36, 6, false, false, 8),
            new Spec(BONE_BLADES, "Bone Blade Dance", 20, 14, 90, 0, 12, 6, false, false, 26),
            new Spec(BONE_GARDEN, "Bone Garden", 20, 26, 200, 3, 34, 4, false, true, 10),
            new Spec(BACKSTAB, "Rift Backstab", 20, 16, 130, 4, 30, 5, false, false, 30),
            new Spec(PORTAL_LINK, "Portal Link", 40, 24, 190, 6, 38, 4, false, true, 14),
            new Spec(BONE_LANCE, "Dense Bone Lance", 20, 34, 220, 10, 42, 3, false, false, 14),
            new Spec(BONE_ARMOR, "Bone Armour", 40, 18, 520, 0, 48, 2, false, false, 10),
            new Spec(WARP_BARRAGE, "Warp Barrage", 40, 22, 280, 5, 32, 3, false, false, 36),
            new Spec(TENSURA_SPACE, "Spatial Cut", 20, 16, 240, 6, 30, 2, false, false, 8),
            new Spec(CALL_DISCIPLES, "Beelzebub's Disciples", 20, 30, 700, 0, 48, 2, false, false, 10),
            new Spec(DOMINATION, "Spatial Mana Domination", 60, 60, 1200, 0, 36, 5, true, false, 60),
            // 1.35.0: two late-phase moves appended (the client reads move names by index)
            new Spec(RIFT_CUTS, "Rift Collapse", 60, 28, 260, 4, 30, 4, false, true, 12),
            new Spec(GATE_STORM, "Gate Storm", 80, 30, 340, 6, 38, 3, false, true, 14));

    /** The table, in the order of the boss's move list (the client reads move names by index). */
    public static List<Spec> specs() { return SPECS; }

    public static Spec byId(String id) {
        for (Spec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }

    // ---------------------------------------------------------------- Spatial Evasion (the passive)
    /** The least blow he bothers to slip (chip damage does not make him blink). */
    public static final float EVADE_MIN = 3f;
    private static final double[] EVADE = {160, 140, 120, 90, 60};

    /**
     * Ticks between two Spatial Evasions: the first blow after the cooldown is slipped through a rift (and he is not hit by it), so
     * the cooldown shortens as the devil's power is drawn: 8 s at the opening, 3 s at the Gates. He never slips a blow while he casts
     * or is exposed.
     */
    public static int evadeCooldown(int power) {
        double x = (Math.max(PowerCurve.OPENING, Math.min(PowerCurve.FULL, power)) - PowerCurve.OPENING) / 20.0;
        int i = (int) Math.floor(x);
        if (i >= EVADE.length - 1) return (int) EVADE[EVADE.length - 1];
        return (int) Math.round(EVADE[i] + (EVADE[i + 1] - EVADE[i]) * (x - i));
    }
}
