package com.newuniverse.nusmp.entity.zogratis;

import java.util.List;

/**
 * Vanica's move table (pure, unit tested): every move's numbers in one place, as {@link MoveSpec}s the scheduler can read without
 * Minecraft. {@code VanicaBoss} turns each entry into a {@link Move} by adding its telegraph and its code, looked up by id.
 * <p>
 * The shape of the fight: a fast striker-caster who lives at mid range. Blood Magic is her toolbox (lances, a scythe, a whip, a
 * dash, a burst, a rain of spears, the Blood Beasts, a frenzy), Megicula's Curse-Warding is the pressure (a rotting field that
 * spreads and a branding curse that weakens and withers), the Dark Disciples are the crowd, and Crimson Crown (60% and up, when
 * her mana is unbound) is the only ultimate: a ring of blood thorns, a stampede of beasts and a curse field at once.
 */
public final class VanicaTable {
    private VanicaTable() {}

    public static final String BLOOD_LANCES = "blood_lances", BLOOD_SCYTHE = "blood_scythe", BLOOD_WHIP = "blood_whip", CRIMSON_DASH = "crimson_dash",
            BLOOD_BURST = "blood_burst", CRIMSON_RAIN = "crimson_rain", ROT_FIELD = "rot_field", DECAY_CURSE = "decay_curse",
            BLOOD_BEASTS = "blood_beasts", BLOOD_FRENZY = "blood_frenzy", CALL_DISCIPLES = "call_disciples", TENSURA_HEX = "tensura_hex",
            CRIMSON_CROWN = "crimson_crown",
            REAPER_DANCE = "reaper_dance", BLOOD_STORM = "blood_storm";

    /** Base damage of the moves (before the boss's damage law: power tuning, victim EP, the balance law's caps). */
    public static final float LANCE = 6f, SCYTHE = 11f, WHIP = 8f, DASH = 12f, BURST = 9f, RAIN = 9f, THORN = 14f, ROT_PULSE = 2.5f,
            CURSE_DOT = 1.6f;

    /** One row of the table. The record's accessors are the {@link MoveSpec} methods. */
    public record Spec(String id, String label, int minPower, int windUpTicks, int cooldownTicks, double minRange, double maxRange,
                       int weight, boolean ultimate, boolean crowd, int recovery) implements MoveSpec {
        /** The mana an ordinary move costs below 60% power (the same rule as {@link Move}): 6 + a sixteenth of its cooldown, at most 32; 60 for an ultimate. */
        @Override public double manaCost() { return ultimate ? 60 : Math.min(32, 6 + cooldownTicks / 16.0); }
    }

    private static final List<Spec> SPECS = List.of(
            new Spec(BLOOD_LANCES, "Blood Lances", 20, 20, 110, 5, 34, 6, false, false, 8),
            new Spec(BLOOD_SCYTHE, "Blood Scythe", 20, 14, 70, 0, 6.5, 6, false, false, 10),
            new Spec(BLOOD_WHIP, "Blood Whip", 20, 16, 120, 3.5, 14, 4, false, false, 8),
            new Spec(CRIMSON_DASH, "Crimson Dash", 20, 18, 150, 6, 26, 4, false, false, 12),
            new Spec(BLOOD_BURST, "Blood Burst", 20, 22, 180, 0, 8.5, 4, false, true, 12),
            new Spec(CRIMSON_RAIN, "Crimson Rain", 40, 24, 210, 4, 36, 4, false, true, 10),
            new Spec(ROT_FIELD, "Rotting Field", 20, 28, 260, 4, 34, 4, false, true, 12),
            new Spec(DECAY_CURSE, "Curse of Decay", 20, 26, 300, 4, 30, 4, false, false, 10),
            new Spec(BLOOD_BEASTS, "Blood Beasts", 20, 26, 380, 0, 48, 3, false, false, 12),
            new Spec(BLOOD_FRENZY, "Blood Frenzy", 40, 20, 520, 0, 48, 1, false, false, 12),
            new Spec(CALL_DISCIPLES, "Call the Disciples", 20, 30, 700, 0, 48, 2, false, false, 10),
            new Spec(TENSURA_HEX, "Hex Magic", 20, 16, 260, 6, 30, 2, false, false, 8),
            new Spec(CRIMSON_CROWN, "Crimson Crown", 60, 60, 1300, 0, 40, 5, true, false, 60),
            // 1.35.0: two late-phase moves appended (the client reads move names by index)
            new Spec(REAPER_DANCE, "Reaper's Dance", 60, 22, 300, 0, 8, 4, false, false, 14),
            new Spec(BLOOD_STORM, "Blood Storm", 80, 26, 340, 5, 34, 3, false, true, 14));

    /** The table, in the order of the boss's move list (the client reads move names by index). */
    public static List<Spec> specs() { return SPECS; }

    public static Spec byId(String id) {
        for (Spec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }
}
