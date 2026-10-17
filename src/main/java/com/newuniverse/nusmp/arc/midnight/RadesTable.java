package com.newuniverse.nusmp.arc.midnight;

import java.util.List;

/**
 * Why: Rades Spirito is a Soul Corpse mage, a former Magic Knight of the Purple Orca who was cast out of the kingdom for what his magic is, and who set the dead
 * on the Royal Capital. The fight is a question the party answers by choosing targets: his raised dead stand between him and every blow (the more corpses, the
 * thicker the ward), Jimmy's cursed bullets and Alfred's thunder pick at them from range, the Soul Abductor calls the fallen back, and when he is pressed he
 * leaves through a spatial gate as he did in the story. The counter shown in the story is anti-magic: Asta's sword cut the mana that moved the dead, and here
 * it cuts the ward. This table holds the numbers (pure, unit tested), so {@code RadesBoss} only has to turn each row into a telegraph and a strike. His
 * existing exclusive-caster pages (the player's Soul Corpse book) are not touched; this is the boss who stands on the other side of them.
 */
public final class RadesTable {
    private RadesTable() {}

    public static final String RAISE_DEAD = "raise_dead", JIMMY = "jimmy_bullets", ALFRED = "alfred_thunder", SOUL_THREAD = "soul_thread",
            DEAD_HANDS = "dead_hands", WRAITH_WAIL = "wraith_wail", SPECTRAL_GATE = "spectral_gate", CURSE_MAGIC = "curse_magic",
            CORPSE_BURST = "corpse_burst", SOUL_ABDUCTOR = "soul_abductor", LEGION = "legion", BETRAYED_WRATH = "betrayed_wrath";

    /** Base damage of the moves, before the boss's damage law (power tuning, the victim's EP, the balance law's caps). */
    public static final float BULLET = 5f, THUNDER = 12f, THREAD = 6f, HANDS = 9f, WAIL = 8f, GATE_BURST = 8f, BURST = 11f, LEGION_WAIL = 14f, WRATH_WAIL = 9f,
            SOUL_BURST = 8f, FALLBACK = 6f;

    /** The moves in the order the boss lists them (the client reads names by index: new moves are only ever appended). */
    private static final List<MidnightSpec> SPECS = List.of(
            new MidnightSpec(RAISE_DEAD, "Soul Corpse: Raise the Dead", 20, 24, 300, 0, 48, 4, false, false, 10),
            new MidnightSpec(JIMMY, "No.4 Jimmy: Cursed Bullets", 20, 20, 130, 6, 36, 5, false, false, 8),
            new MidnightSpec(ALFRED, "No.2 Alfred: Thunder", 20, 26, 190, 3, 38, 4, false, true, 10),
            new MidnightSpec(SOUL_THREAD, "Soul Thread", 20, 20, 220, 8, 30, 3, false, false, 8),
            new MidnightSpec(DEAD_HANDS, "Dead Hands", 20, 24, 210, 0, 16, 4, false, false, 10),
            new MidnightSpec(WRAITH_WAIL, "Wraith Wail", 20, 22, 170, 0, 11, 3, false, true, 12),
            new MidnightSpec(SPECTRAL_GATE, "Spatial Retreat", 20, 20, 260, 0, 7, 3, false, false, 10),
            new MidnightSpec(CURSE_MAGIC, "Curse Magic", 20, 18, 250, 6, 30, 2, false, false, 8),
            new MidnightSpec(CORPSE_BURST, "Corpse Burst", 40, 26, 280, 0, 30, 3, false, true, 12),
            new MidnightSpec(SOUL_ABDUCTOR, "Soul Abductor", 40, 30, 520, 0, 48, 2, false, false, 12),
            new MidnightSpec(LEGION, "Legion of the Dead", 60, 56, 1300, 0, 48, 5, true, true, 60),
            new MidnightSpec(BETRAYED_WRATH, "Wrath of the Betrayed", 100, 30, 240, 0, 14, 4, false, true, 14));

    public static List<MidnightSpec> specs() { return SPECS; }

    public static MidnightSpec byId(String id) {
        for (MidnightSpec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }
}
