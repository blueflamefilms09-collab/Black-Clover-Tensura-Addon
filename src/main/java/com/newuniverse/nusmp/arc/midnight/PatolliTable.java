package com.newuniverse.nusmp.arc.midnight;

import java.util.List;

/**
 * Why: Patolli, the elf who leads the Eye of the Midnight Sun behind the face of Licht, fights with Light Magic, whose defining trait in the story is
 * speed: his spells cross a room before the eye follows them, he vanishes and is behind you, and the one thing that matched him was Julius Novachrono's
 * time foresight. The fight therefore has to be fast and yet readable: every move is a shape that is SHOWN before it lands, the lanes freeze a few ticks
 * before the strike so a sidestep is the answer, and what the party cannot outrun it can predict. This table is where those numbers live (pure, unit
 * tested), so {@code PatolliBoss} only has to turn each row into a telegraph and a strike.
 * <p>
 * The shape of the fight: a volley of light swords and a whip for the middle, Light Step to be behind you (and overreach if it finds nothing), a flash to
 * punish the ones who stare, orbs and arrows for the crowd, the Light Shaft (the beam that was turned back on him by a mirror in the story), the lamp, and one
 * ultimate, Heaven's Verdict, that the party can break. At the Gates he is the Dark Elf and one move is added: the Dance of Cuts.
 */
public final class PatolliTable {
    private PatolliTable() {}

    public static final String LIGHT_SWORD = "light_sword", JUDGMENT_WHIP = "judgment_whip", LIGHT_STEP = "light_step", RADIANT_BURST = "radiant_burst",
            BLINDING_FLASH = "blinding_flash", SOLAR_MAGIC = "solar_magic", HEAVEN_SPLITTING = "heaven_splitting_flash", LIGHT_SHAFT = "light_shaft",
            ARROWS = "arrows_of_judgment", LAMP = "lamp_of_avior", VERDICT = "verdict", DEMON_DANCE = "demon_light_dance";

    /** Base damage of the moves, before the boss's damage law (power tuning, the victim's EP, the balance law's caps). */
    public static final float SWORD = 6.5f, WHIP = 11f, STEP_LANE = 9f, STEP_STRIKE = 12f, BURST = 9f, FLASH = 3f, ORB = 8f, SHAFT = 19f, ARROW = 8.5f,
            LAMP_BLADE = 6f, VERDICT_HIT = 26f, DANCE_CUT = 6.5f, FALLBACK = 6.5f;

    /** The moves in the order the boss lists them (the client reads names by index: new moves are only ever appended). */
    private static final List<MidnightSpec> SPECS = List.of(
            new MidnightSpec(LIGHT_SWORD, "Light Sword of Judgment", 20, 18, 110, 5, 34, 5, false, false, 8),
            new MidnightSpec(JUDGMENT_WHIP, "Bright Judgment Whip", 20, 16, 80, 0, 8.5, 6, false, false, 10),
            new MidnightSpec(LIGHT_STEP, "Light Step", 20, 20, 150, 5, 38, 5, false, false, 8),
            new MidnightSpec(RADIANT_BURST, "Radiant Burst", 20, 18, 170, 0, 6, 2, false, true, 12),
            new MidnightSpec(BLINDING_FLASH, "Blinding Flash", 20, 22, 360, 0, 20, 2, false, true, 10),
            new MidnightSpec(SOLAR_MAGIC, "Solar Magic", 20, 18, 240, 6, 30, 2, false, false, 8),
            new MidnightSpec(HEAVEN_SPLITTING, "Heaven-Splitting Flash", 40, 30, 260, 4, 38, 4, false, true, 12),
            new MidnightSpec(LIGHT_SHAFT, "Light Shaft of Divine Punishment", 40, 30, 240, 6, 36, 4, false, false, 10),
            new MidnightSpec(ARROWS, "Arrows of Judgment", 40, 34, 340, 0, 30, 3, false, true, 12),
            new MidnightSpec(LAMP, "Lamp of Avior Gloria", 60, 28, 420, 0, 28, 3, false, true, 12),
            new MidnightSpec(VERDICT, "Heaven's Verdict", 60, 60, 1200, 0, 48, 5, true, true, 60),
            new MidnightSpec(DEMON_DANCE, "Demon Light: Dance of Cuts", 100, 24, 200, 0, 12, 4, false, false, 12));

    public static List<MidnightSpec> specs() { return SPECS; }

    public static MidnightSpec byId(String id) {
        for (MidnightSpec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }
}
