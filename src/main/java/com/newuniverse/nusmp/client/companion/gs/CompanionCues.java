package com.newuniverse.nusmp.client.companion.gs;

/**
 * What a line the server sent MEANS, read from its lang key alone (never from the wording, so every language works): the bond system and the companion records answer in {@code <prefix>gift.love},
 * {@code <prefix>act.five}, {@code <prefix>a.join}, {@code nusmp.companion.bond.pouch.put} and so on. The Great Spirit screen turns those keys into a mood for the portrait, a line in the bond story
 * and a crest notice. Nothing is sent or decided here: it only labels something the client already has. Pure, unit tested.
 */
public final class CompanionCues {
    private CompanionCues() {}

    /** What happened. The constants are only ever appended. */
    public enum Cue { NONE, GREET, FIVE, GIFT_LOVE, GIFT_OK, GIFT_HATE, GIFT_NO, FOOD_TALK, JOIN_OK, JOIN_NO, WAIT, USE, STANCE, PUT, TOOK, REFUSED, LIKES }

    private static final String ROOT = "nusmp.companion.";
    /** The third segment of the keys that belong to the mod's common wording, not to one companion. */
    private static final String[] COMMON = {"bond.", "tab.", "tier.", "stance.", "close", "join.", "disabled", "gs."};

    /** The companion's own prefix ("nusmp.companion.charmy.") of a key, or null for a common key or one that is not a companion key. */
    public static String prefixOf(String key) {
        if (key == null || !key.startsWith(ROOT) || key.length() > 160) return null;
        String rest = key.substring(ROOT.length());
        for (String c : COMMON) if (rest.startsWith(c)) return null;
        int dot = rest.indexOf('.');
        if (dot <= 0 || dot == rest.length() - 1) return null;
        return ROOT + rest.substring(0, dot + 1);
    }

    /** The cue of a lang key (NONE for anything else). */
    public static Cue cueOf(String key) {
        if (key == null || !key.startsWith(ROOT)) return Cue.NONE;
        String p = prefixOf(key);
        String rest = p != null ? key.substring(p.length()) : key.substring(ROOT.length());
        if (p != null) {
            return switch (rest) {
                case "greet" -> Cue.GREET;
                case "act.five" -> Cue.FIVE;
                case "gift.love" -> Cue.GIFT_LOVE;
                case "gift.ok" -> Cue.GIFT_OK;
                case "gift.hate" -> Cue.GIFT_HATE;
                case "gift.no" -> Cue.GIFT_NO;
                case "a.food" -> Cue.FOOD_TALK;
                case "a.join" -> Cue.JOIN_OK;
                case "join.need", "join.taken" -> Cue.JOIN_NO;
                case "a.wait" -> Cue.WAIT;
                case "use" -> Cue.USE;
                case "likes.love", "likes.hate" -> Cue.LIKES;
                default -> rest.startsWith("stance.") ? Cue.STANCE : Cue.NONE;
            };
        }
        return switch (rest) {
            case "join.full" -> Cue.JOIN_NO;
            case "bond.pouch.put" -> Cue.PUT;
            case "bond.pouch.took", "bond.pouch.tookall" -> Cue.TOOK;
            case "bond.pouch.full", "bond.pouch.locked", "bond.pouch.empty", "bond.gift.empty", "bond.busy", "bond.nothing" -> Cue.REFUSED;
            default -> Cue.NONE;
        };
    }

    /** The mood a cue puts on the portrait, or null when it leaves the mood alone. */
    public static Mood moodOf(Cue cue) {
        return switch (cue) {
            case FIVE, GIFT_LOVE, GIFT_OK, JOIN_OK, USE -> Mood.HAPPY;
            case GIFT_HATE -> Mood.ANGRY;
            case GIFT_NO, JOIN_NO, REFUSED -> Mood.SHY;
            case FOOD_TALK -> Mood.HUNGRY;
            case WAIT -> Mood.NEUTRAL;
            default -> null;
        };
    }

    /** How long the mood a cue sets lasts before the portrait goes back to its resting face (ticks). */
    public static int holdTicks(Mood m) {
        return switch (m) {
            case HAPPY -> 120;
            case ANGRY -> 140;
            case SHY -> 100;
            case HUNGRY -> 160;
            default -> 60;
        };
    }

    /** The reaction a gift cue stands for, or null when the cue is not a gift answer. */
    public static GiftMemory.Reaction reactionOf(Cue cue) {
        return switch (cue) {
            case GIFT_LOVE -> GiftMemory.Reaction.LOVED;
            case GIFT_OK -> GiftMemory.Reaction.LIKED;
            case GIFT_HATE -> GiftMemory.Reaction.HATED;
            case GIFT_NO -> GiftMemory.Reaction.REFUSED;
            default -> null;
        };
    }
}
