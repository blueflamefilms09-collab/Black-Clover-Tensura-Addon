package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.List;

/**
 * Why: the floor report is one value that three places read: the server builds it from the world (the sector's theme, the mechanics planned for the floor, the
 * depth), the network carries it ({@link FloorInfoNet}), and two consumers turn it into words, the client's window and the chat lines of {@code /dungeon floor}. A plain
 * record with no Minecraft type in it means the words are composed by ONE pure class ({@link FloorCardLines}) that a test can drive, and the wire format is a few
 * bounded strings and numbers. It carries ids and English fallbacks, never translated text: every player reads the card in their own language, because the reader
 * asks the lang file for {@code mechanic.nusmp.<id>.name} and friends and falls back to the English the server sent when a key has no text (a datapack mechanic, a
 * lang file not merged yet).
 *
 * @param reason      {@link FloorInfoRules.Reason#code()}
 * @param floor       the signed floor
 * @param previous    the floor the player came from, 0 when none
 * @param up          the move (or, for an arrival, the road) goes UP
 * @param depth       the floor's distance from the Hub
 * @param boss        a boss floor
 * @param themeId     the theme id ({@code holy_church}), "" when the floor has no theme (a gap between sectors)
 * @param themeName   the English name of the theme (the lang key is {@code message.nusmp.mega.mgd.theme.<id>})
 * @param themeDesc   the English one-line description (the lang key is {@link FloorInfoText#themeDescKey})
 * @param align       {@code holy}, {@code abyssal} or {@code any}, with {@code _only} for a theme drawn on its own road only
 * @param dangerHundredths the theme's danger multiplier times 100
 * @param healthPct   the depth's monster health bonus in percent
 * @param damagePct   the depth's monster damage bonus in percent
 * @param mind        the depth's monster mind level
 * @param xpTimes100  the depth's experience multiplier times 100
 * @param mechanicsOn the mechanics layer is switched on
 * @param hiddenRooms how many planned room, zone and boss mechanics have not shown themselves yet
 * @param floorMechs  the floor-wide mechanics
 * @param hereMechs   the room, zone and boss mechanics that are active where the player stands
 */
public record FloorCard(int reason, int floor, int previous, boolean up, int depth, boolean boss, String themeId, String themeName, String themeDesc, String align,
                        int dangerHundredths, int healthPct, int damagePct, int mind, int xpTimes100, boolean mechanicsOn, int hiddenRooms,
                        List<Mech> floorMechs, List<Mech> hereMechs) {

    /** The most mechanics a card carries in each list (the card is a window, not a manual; the rest is a "+N more" line). */
    public static final int MAX_MECHS = 6;

    /**
     * One mechanic.
     *
     * @param id          {@code floor.rim_curse}: the lang keys are {@code mechanic.nusmp.<id>.name / .desc}
     * @param name        English fallback of the name
     * @param desc        English fallback of the description
     * @param counterKind the counter kind ({@code kill_all} ...) or {@code none}
     * @param hint        the English hint of the counter (the lang key is {@code message.nusmp.mega.mech.hint.<kind>}); "" when the mechanic has no counter
     * @param scope       {@code floor}, {@code room}, {@code zone} or {@code boss}
     * @param state       {@code armed}, {@code waiting} or {@code solved}
     */
    public record Mech(String id, String name, String desc, String counterKind, String hint, String scope, String state) {}

    public FloorInfoRules.Reason reasonKind() { return FloorInfoRules.Reason.fromCode(reason); }

    /** Is this the same report as {@code other} in everything the window shows? (A resend that changes nothing need not redraw.) */
    public boolean sameAs(FloorCard other) { return other != null && equals(other); }

    /** The road word of {@link #align}: holy, abyssal or any. */
    public String alignRoad() {
        int i = align == null ? -1 : align.indexOf('_');
        return i < 0 ? (align == null || align.isEmpty() ? "any" : align) : align.substring(0, i);
    }

    public boolean alignOnly() { return align != null && align.endsWith("_only"); }
}
