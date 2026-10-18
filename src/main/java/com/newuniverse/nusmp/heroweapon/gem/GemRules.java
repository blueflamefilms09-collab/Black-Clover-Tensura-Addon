package com.newuniverse.nusmp.heroweapon.gem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Why: whether a Calling Gem may start its ritual is a list of conditions (the survival route is open, the player carries no weapon yet, a weapon can answer,
 * the player stands still on firm ground in the open), and the player must be told the FIRST one that fails, the same way every time. The decision is a pure
 * function of plain facts so that every refusal path is a unit test and the glue ({@code GemChoosing}) only has to gather the facts from the player and the
 * world. The order is a rule: the server's switch first, then what the player already carries, then whether any weapon can answer, then the body (riding,
 * under water, in the air, moving), then the sky. Also here: the small pure helpers of the Choosing that are not about the ritual's clock: the shuffle that
 * orders the Holy Weapons a Holy Calling Gem tries, and the "all gates gave the same reason" test that tells a global refusal from "every weapon is taken".
 */
public final class GemRules {
    private GemRules() {}

    /** Why a ritual cannot start; {@link #key} is the suffix of the message key {@code hero.msg.gem.<key>}. */
    public enum Denial {
        NONE("none"),
        /** The server switched the survival route off ({@code recipes = false}) and the player is neither an operator nor in creative mode. */
        ROUTE_CLOSED("closed"),
        /** The player already carries a hero weapon. */
        ALREADY_BOUND("bound"),
        /** A gate refused every weapon the gem could give for the same reason (the system is off, a limit is reached): the reason is shown. */
        GATE_REFUSED("refused"),
        /** Every Holy Weapon is held by someone else. */
        NONE_FREE("none_free"),
        RIDING("riding"),
        SUBMERGED("submerged"),
        AIRBORNE("airborne"),
        MOVING("moving"),
        /** There is less clear space above the player than the config asks for. */
        CLOSED_ABOVE("closed_above");

        private final String key;

        Denial(String key) { this.key = key; }

        public String key() { return key; }
    }

    /**
     * The facts of one attempt.
     *
     * @param routeOpen      the survival route is open for this player ({@link #routeOpen})
     * @param bound          the player already carries a hero weapon
     * @param sealed         the gem is a Seal (a named weapon), not the Holy Calling Gem
     * @param freeCandidates for the Holy Calling Gem: how many Holy Weapons the gates would let this player take
     * @param gateRefused    a gate refuses the Seal's weapon, or refuses every Holy Weapon for one common reason
     * @param riding         the player is riding something
     * @param submerged      the player's head is under water
     * @param onGround       the player stands on the ground
     * @param speed          the player's horizontal speed in blocks per tick
     * @param clearAbove     how many blocks of clear space there are above the player's head (counted up to the config's need)
     */
    public record Facts(boolean routeOpen, boolean bound, boolean sealed, int freeCandidates, boolean gateRefused, boolean riding, boolean submerged,
                        boolean onGround, double speed, int clearAbove) {}

    /** Whether a player may use a gem at all: the survival route is open, or the player is an operator or in creative mode (the admin routes stay open). */
    public static boolean routeOpen(boolean recipesEnabled, boolean privileged) { return recipesEnabled || privileged; }

    /**
     * The first condition that fails, or {@link Denial#NONE}.
     *
     * @param speedTolerance the fastest horizontal speed, in blocks per tick, at which the ritual may still begin
     * @param needClear      how many blocks of clear space the config asks for above the player (zero switches the rule off)
     */
    public static Denial decide(Facts f, double speedTolerance, int needClear) {
        Denial first = decideAtEnd(f);
        if (first != Denial.NONE) return first;
        if (f.riding()) return Denial.RIDING;
        if (f.submerged()) return Denial.SUBMERGED;
        if (!f.onGround()) return Denial.AIRBORNE;
        if (f.speed() > speedTolerance) return Denial.MOVING;
        if (needClear > 0 && f.clearAbove() < needClear) return Denial.CLOSED_ABOVE;
        return Denial.NONE;
    }

    /**
     * The conditions that are checked again at the moment the gem answers, because they can change while the player holds still: the survival route, a
     * weapon carried since (an operator's {@code bind}), the gates, and whether a weapon is still free. The body of the player is not asked again: the
     * ritual itself already ended if the player had moved.
     */
    public static Denial decideAtEnd(Facts f) {
        if (!f.routeOpen()) return Denial.ROUTE_CLOSED;
        if (f.bound()) return Denial.ALREADY_BOUND;
        if (f.gateRefused()) return Denial.GATE_REFUSED;
        if (!f.sealed() && f.freeCandidates() <= 0) return Denial.NONE_FREE;
        return Denial.NONE;
    }

    /**
     * The reason every entry of the list shares, or null when the list is empty, an entry is null (that weapon is allowed) or two reasons differ. Used on
     * the refusals of the four Holy Weapons: one common reason (the hero system is off, a limit is reached) is shown as it is, but different reasons
     * ("held by someone") mean the weapons are simply taken.
     */
    public static String commonReason(List<String> reasons) {
        if (reasons.isEmpty()) return null;
        String first = reasons.get(0);
        if (first == null) return null;
        for (String r : reasons) if (!first.equals(r)) return null;
        return first;
    }

    /**
     * A copy of the list in random order (Fisher-Yates), driven by a caller-supplied source of "a number below the bound" so that a test can script it and
     * the game can pass the player's own random source.
     */
    public static <T> List<T> shuffled(List<T> in, IntUnaryOperator nextBelow) {
        List<T> out = new ArrayList<>(in);
        for (int i = out.size() - 1; i > 0; i--) {
            int j = nextBelow.applyAsInt(i + 1);
            T t = out.get(i);
            out.set(i, out.get(j));
            out.set(j, t);
        }
        return out;
    }
}
