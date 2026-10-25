package com.newuniverse.nusmp.corpse;

/**
 * Why (owner report 3.7.0, "Rades's Number Summons are just magic circles", docs/magic/rades_cast_fix.md): when a numbered cast showed a circle and no body, nothing told anyone WHY, so the cause had to be
 * guessed. This pure class (no game types) holds the words of the server log and the three small rules behind them, so a refused cast and a body that vanished within a few seconds each leave one line
 * with the reason: the reason of a refused summon ({@link Refusal}), the reason a body was taken out of the world ({@link #removalReason}), whether a removal counts as "vanished early"
 * ({@link #vanishedEarly}) and the rate limit that keeps a held key from filling the log ({@link #mayLog}). {@code corpse.diagnose} (server config, default true) switches the lines off.
 */
public final class RadesCastDiagnosis {
    private RadesCastDiagnosis() {}

    /** A body that is gone this many ticks (5 s) after it was summoned vanished early, and one line says why. */
    public static final int EARLY_TICKS = 100;
    /** At most one log line per player per this many ticks (one second). */
    public static final int LOG_GAP_TICKS = 20;

    /** Why a numbered summon made no body. {@link #NONE} = it worked. */
    public enum Refusal {
        NONE("it worked"),
        SWITCHED_OFF("corpse.enabled is false on this server"),
        ELITE_STANDS("the elite of this kind already stands"),
        SERVER_CAP("the server-wide cap corpse.namedGlobalCap is reached"),
        NO_ENTITY("the entity type could not be created (EntityType.create returned null)"),
        WORLD_REFUSED("the world refused the body (addFreshEntity returned false: a join event was cancelled or the chunk is not loaded)");

        private final String why;

        Refusal(String why) { this.why = why; }

        public String why() { return why; }

        /** The line the player reads on the action bar (the server decides, the client only shows it). */
        public String playerLine(String title) {
            return switch (this) {
                case NONE -> title + " answers your call.";
                case SWITCHED_OFF -> "Soul Corpse Magic is switched off on this server.";
                case ELITE_STANDS -> title + " already stands at your side.";
                case SERVER_CAP -> "Too many numbered undead stand on this server right now: " + title + " cannot come yet.";
                case NO_ENTITY, WORLD_REFUSED -> title + " will not come here: the world refused the body.";
            };
        }
    }

    /** The server-log line of a refused numbered cast. */
    public static String refusedLine(String player, String kindId, Refusal why) {
        return "[nusmp] numbered undead: the cast of " + kindId + " by " + player + " made no body: " + why.why();
    }

    /** The server-log line of a numbered body that left the world early. */
    public static String vanishedLine(String kindId, String ownerId, int ageTicks, String reason) {
        return "[nusmp] numbered undead: " + kindId + " of " + ownerId + " left the world after " + ageTicks + " ticks (" + ageTicks / 20.0 + " s): " + reason;
    }

    /** True when a body that is removed 'ageTicks' after it appeared (of a removal that is not a normal end) is worth a line. */
    public static boolean vanishedEarly(int ageTicks, boolean normalEnd) {
        return !normalEnd && ageTicks >= 0 && ageTicks < EARLY_TICKS;
    }

    /**
     * A plain-words reason for the removal of a body. 'kind' is the name of the engine's removal reason (KILLED, DISCARDED, UNLOADED_TO_CHUNK, UNLOADED_WITH_PLAYER, CHANGED_DIMENSION), 'ownerGone'
     * says the sweep found the owner offline, dead or in another dimension, 'lifeOver' that the clock ran out.
     */
    public static String removalReason(String kind, boolean ownerGone, boolean lifeOver) {
        if (lifeOver) return "its life (corpse.namedLifeSeconds) ran out";
        if (ownerGone) return "its owner is offline, dead or in another dimension (the sweep dismisses the body)";
        if (kind == null) return "unknown";
        return switch (kind) {
            case "KILLED" -> "it was killed";
            case "DISCARDED" -> "it was discarded by code (crumbled by a second cast, the cap, the sweep, or another mod)";
            case "UNLOADED_TO_CHUNK", "UNLOADED_WITH_PLAYER" -> "its chunk was unloaded";
            case "CHANGED_DIMENSION" -> "it changed dimension";
            default -> "removed: " + kind;
        };
    }

    /** True for the removal kinds that are not a loss: a chunk or a dimension moves the body, it does not end it. */
    public static boolean normalEnd(String kind) {
        return "UNLOADED_TO_CHUNK".equals(kind) || "UNLOADED_WITH_PLAYER".equals(kind) || "CHANGED_DIMENSION".equals(kind);
    }

    /** The rate limit: a line may be written when the last one of this player is at least {@link #LOG_GAP_TICKS} ago (or the clock went backwards). */
    public static boolean mayLog(long lastTick, long now) {
        return lastTick == Long.MIN_VALUE || now < lastTick || now - lastTick >= LOG_GAP_TICKS;
    }
}
