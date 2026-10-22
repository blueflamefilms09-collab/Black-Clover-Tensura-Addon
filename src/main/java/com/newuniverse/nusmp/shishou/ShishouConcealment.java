package com.newuniverse.nusmp.shishou;

import java.util.List;

/**
 * Why: Stat Concealment. Anyone but the wielder who appraises Shishou (our own Appraisal readout, the tooltip of the item) sees a false, ordinary "Rusty Sword"; only the wielder sees the truth. This is the
 * pure choice of the readout, so a leak is a failing test. The numbers of the false readout are fixed (they never depend on the real ones).
 */
public final class ShishouConcealment {
    private ShishouConcealment() {}

    public static final String FALSE_NAME_KEY = "shishou.false.name";
    public static final String TRUE_NAME_KEY = "item.nusmp.shishou";

    /** One readout: the name key, the health, the level, the mana, the skill lang keys. */
    public record Readout(String nameKey, float health, float maxHealth, int level, int mana, int maxMana, List<String> skills, boolean concealed) {}

    /** What a non-wielder sees of Shishou: a rusty sword with one health and nothing else. */
    public static Readout falseReadout() { return new Readout(FALSE_NAME_KEY, 1f, 1f, 1, 0, 0, List.of(), true); }

    /** The readout the viewer is allowed to see: the true one for the wielder, the false one for everybody else (and when the viewer is unknown). */
    public static Readout view(boolean viewerIsWielder, boolean skillAvailable, Readout truth) {
        if (!skillAvailable || viewerIsWielder) return truth;
        return falseReadout();
    }

    /** The tooltip lines a non-wielder sees: the false name only (the client replaces the true lines). */
    public static List<String> falseTooltipKeys() { return List.of(FALSE_NAME_KEY, "shishou.false.line"); }
}
