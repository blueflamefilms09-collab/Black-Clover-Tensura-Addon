package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.List;
import java.util.Locale;

/**
 * Why: a floor quest is "an objective per floor" (the owner's words), and the five objectives differ in exactly the ways the code has to care about: HOW the
 * quest is fed (rooms cleared, monsters killed, damage taken, time passing, an ambush group killed), WHEN it can succeed (the counting kinds succeed the moment
 * the count is reached, which is always before the next staircase; the two "reach the next floor" kinds can only be judged on arrival, so they succeed or fail
 * at the staircase), and WHICH stat they pay when the reward is a stat point. The brief names the kinds; this enum is the one place that says what each
 * one means, so the state machine ({@link QuestProgress}), the roller, the reward roll and the overlay all read the same facts. Only kinds whose progress the
 * server can observe reliably are here. The ORDER is not saved (kinds are stored by name), so a kind can be appended later without touching old worlds.
 */
public enum QuestKind {
    /** Clear N rooms: stand in each for 40 ticks with no hostile alive in it (optionally without taking magic damage). Counting. */
    CLEAR_ROOMS(false, Stat.INT),
    /** Defeat N hostile creatures on the floor. Counting. */
    KILL_N(false, Stat.STR, Stat.AGI),
    /** Reach the next floor without losing health (any damage fails it). Judged on arrival. */
    NO_DAMAGE_STRETCH(true, Stat.VIT),
    /** Reach the next floor within T seconds spent on this floor. Judged on arrival. */
    SPEED(true, Stat.AGI),
    /** Defeat N creatures of the Dungeon Master's ambushes (entity tag {@code nusmp_dm}). Counting. */
    SURVIVE_AMBUSH(false, Stat.SENSE);

    private final boolean onArrival;
    private final List<Stat> defaultStats;

    QuestKind(boolean onArrival, Stat... stats) {
        this.onArrival = onArrival;
        this.defaultStats = List.of(stats);
    }

    /** True for the kinds that are judged when the player enters the next floor (the rest succeed as soon as the count is reached). */
    public boolean completesOnArrival() { return onArrival; }

    /** True for the kind that runs a clock. */
    public boolean timed() { return this == SPEED; }

    /** The stats a quest of this kind pays when its template names none: combat STR or AGI, endurance VIT, ambush SENSE, clearing INT. */
    public List<Stat> defaultStats() { return defaultStats; }

    /** The lower-case id used in lang keys and the sync payload. */
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** The English title of a quest of this kind that has none of its own (a template that was removed while a player still had its quest): {@code {n}} / {@code {t}} stand for the target. */
    public String defaultTitle() {
        return switch (this) {
            case CLEAR_ROOMS -> "Clear {n} rooms";
            case KILL_N -> "Defeat {n} monsters on this floor";
            case NO_DAMAGE_STRETCH -> "Reach the next floor without taking damage";
            case SPEED -> "Reach the next floor within {t} seconds";
            case SURVIVE_AMBUSH -> "Defeat {n} ambushers";
        };
    }

    /** {@link #defaultTitle} with the target put in. */
    public String defaultTitle(int target) {
        String n = Integer.toString(target);
        return defaultTitle().replace("{n}", n).replace("{t}", n);
    }

    /** The kind with this name (case-insensitive, surrounding spaces ignored), or null. */
    public static QuestKind fromId(String s) {
        if (s == null) return null;
        String t = s.trim();
        for (QuestKind k : values()) if (k.name().equalsIgnoreCase(t)) return k;
        return null;
    }
}
