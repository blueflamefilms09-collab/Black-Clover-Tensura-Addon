package com.newuniverse.nusmp.quests;

/**
 * Why: whether a fired event satisfies an advancement criterion is the one rule everything else depends on, so it lives in a Minecraft-free
 * function that JUnit can pin down. {@link NusmpEventTrigger.TriggerInstance} calls it.
 *
 * <p>Rule: the keys must be equal; if the criterion names an {@code arg} the event must carry exactly that argument (a criterion without an
 * {@code arg} accepts any argument, and also the plain event); and the matching counter (see {@link EventCounters#read}) must have reached
 * the criterion's {@code count}.
 */
public final class TriggerRule {
    private TriggerRule() {}

    /**
     * @param instKey   the criterion's key
     * @param instArg   the criterion's argument, or null for "any"
     * @param instCount the criterion's required count (values below 1 count as 1)
     * @param evKey     the fired event's key
     * @param evArg     the fired event's argument, or null
     * @param counter   the player's counter for {@code instKey|instArg} (or the plain key when {@code instArg} is null)
     */
    public static boolean matches(String instKey, String instArg, int instCount, String evKey, String evArg, int counter) {
        if (instKey == null || !instKey.equals(evKey)) return false;
        if (instArg != null && !instArg.isEmpty() && !instArg.equals(evArg)) return false;
        return counter >= Math.max(1, instCount);
    }
}
