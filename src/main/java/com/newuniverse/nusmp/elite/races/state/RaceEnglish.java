package com.newuniverse.nusmp.elite.races.state;

import com.newuniverse.nusmp.elite.races.state.EntryCondition.Reason;

/**
 * Why: the English of the states, of the reasons a state cannot start and of the reasons one ended, in a class with no Minecraft in it, so a plain unit test can compare it with
 * the language fragment {@code tools/elite/races_lang.json} (the fragment and the code must say the same thing, or the lead's merge would change the wording). The runtime wraps
 * each string in {@code translatableWithFallback}.
 */
public final class RaceEnglish {
    private RaceEnglish() {}

    public static String name(TransformState s) {
        return switch (s) {
            case BASE -> "Base";
            case BOOSTED -> "Ki Boost";
            case ASCENDED -> "Ascension";
            case GREAT_APE -> "Great Ape";
            case DIVINE -> "Divine Form";
        };
    }

    public static String why(Reason r) {
        return switch (r) {
            case TIER -> "Your race is not advanced enough for this form.";
            case EP -> "You do not have enough EP for this form yet.";
            case HEALTH -> "You are not hurt enough to be pushed into this form.";
            case MOON -> "The moon is not full.";
            case SKY -> "You cannot see the sky.";
            case ITEM -> "You are missing the item this form needs.";
            case COOLDOWN -> "This form is still resting.";
            case FROM -> "You cannot enter this form from the one you are in.";
            case ALREADY -> "You are already in this form.";
            case AURA -> "Your ki is spent.";
            case NOT_AVAILABLE -> "Your race has no such form.";
            case DISABLED -> "Transformations are switched off on this server.";
        };
    }

    /** The sentence for an end; the languages use {@code %s} for the form's name where this uses it too. */
    public static String end(TransformMachine.Event e) {
        return switch (e) {
            case EXPIRED -> "Your %s fades.";
            case DAWN -> "The sun rises and the Great Ape shrinks back.";
            case DRAINED -> "Your ki runs dry and the %s collapses.";
            case REMOVED -> "Your race no longer has the %s.";
            case NONE -> "";
        };
    }
}
