package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.entity.zogratis.MoveSpec;

import java.util.List;

/**
 * Why: Meliodas's move numbers in one pure place, as {@link MoveSpec}s the framework's scheduler can read without Minecraft, so a test can play a
 * whole fight through the real scheduler and prove what he does at each power and distance (the same way {@code DanteTable} does for Dante).
 * {@code MeliodasBoss} turns each row into a {@code Move} by adding its telegraph and its code, found by id.
 *
 * <p>The shape of the fight: a short swordsman who is always close (the slash, the lunge, the wave), a stance that punishes magic (Full Counter,
 * see {@link FullCounter}), his black flame (Hellblaze: a field, a Hellblaze-coated cut, a burst), his speed (Demon Step, the Assault Rush) and the
 * Lostvayne images that guard every side. The Demon Mark makes the later rows available as his power rises ({@link DemonKingMath}); there are two
 * ultimates: the Purgatory (60%) and the Demon King's Wrath (only once the Gates are open).
 */
public final class MeliodasTable {
    private MeliodasTable() {}

    public static final String SLASH = "lostvayne_slash", LUNGE = "lostvayne_lunge", WAVE = "lostvayne_wave", FULL_COUNTER = "full_counter",
            HELLBLAZE = "hellblaze", KAMICHIGIRI = "kamichigiri", DEMON_STEP = "demon_step", BURST = "hellblaze_burst",
            IMAGES = "lostvayne_images", RUSH = "assault_rush", PURGATORY = "hellblaze_purgatory", WRATH = "demon_king_wrath";

    /** Base damage of the moves (before the boss's damage law: power tuning, his own scale, the victim's EP, the balance law's caps). */
    public static final float SLASH_DAMAGE = 14f, LUNGE_DAMAGE = 16f, WAVE_DAMAGE = 8f, KAMICHIGIRI_DAMAGE = 24f, STEP_DAMAGE = 15f,
            BURST_DAMAGE = 14f, IMAGE_DAMAGE = 12f, RUSH_DAMAGE = 13f, PURGATORY_DAMAGE = 17f;

    /** One row of the table. The record's accessors are the {@link MoveSpec} methods. */
    public record Spec(String id, String label, int minPower, int windUpTicks, int cooldownTicks, double minRange, double maxRange,
                       int weight, boolean ultimate, boolean crowd, int recovery) implements MoveSpec {
        /** The mana an ordinary move costs below 60% power (the same rule as the framework's {@code Move}): 6 + a sixteenth of its cooldown, at most 32; 60 for an ultimate. */
        @Override public double manaCost() { return ultimate ? 60 : Math.min(32, 6 + cooldownTicks / 16.0); }
    }

    private static final List<Spec> SPECS = List.of(
            new Spec(SLASH, "Lostvayne Slash", 20, 16, 60, 0, 4.8, 6, false, false, 8),
            new Spec(LUNGE, "Lostvayne Lunge", 20, 16, 120, 4.5, 16, 4, false, false, 10),
            new Spec(WAVE, "Lostvayne Wave", 20, 18, 110, 6, 32, 5, false, false, 8),
            new Spec(FULL_COUNTER, "Full Counter", 20, 16, 340, 0, 48, 3, false, false, 8),
            new Spec(HELLBLAZE, "Hellblaze", 40, 22, 240, 4, 30, 4, false, true, 10),
            new Spec(KAMICHIGIRI, "Kamichigiri", 40, 20, 170, 0, 6.5, 4, false, false, 12),
            new Spec(DEMON_STEP, "Demon Step", 40, 16, 190, 5, 26, 3, false, false, 8),
            new Spec(BURST, "Hellblaze Burst", 40, 20, 200, 0, 9, 3, false, true, 12),
            new Spec(IMAGES, "Lostvayne: Real Image", 60, 26, 520, 0, 40, 3, false, true, 14),
            new Spec(RUSH, "Assault Rush", 80, 18, 300, 3, 26, 4, false, true, 16),
            new Spec(PURGATORY, "Hellblaze: Purgatory", 60, 58, 1300, 0, 40, 5, true, true, 60),
            new Spec(WRATH, "Demon King's Wrath", 100, 60, 1500, 0, 36, 5, true, true, 60));

    /** The table, in the order of the boss's move list (the client reads move names by index; only ever append). */
    public static List<Spec> specs() { return SPECS; }

    public static Spec byId(String id) {
        for (Spec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }
}
