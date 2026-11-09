package com.newuniverse.nusmp.heroweapon.bond;

import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.curse.HeroCurse;
import com.newuniverse.nusmp.heroweapon.rules.HeroAbandonRules;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Why: a player's own way out of a hero weapon (3.72.0, {@code /multiverse hero abandon} then {@code abandon confirm}; the operator's {@code release <player>} stays). The weapon goes through the same
 * {@link HeroBonds#release} an operator uses, so the loan ends, the register frees a unique Holy Weapon for someone else, and the forms and the client are told. Refused while the curse holds the
 * wielder, and when the server setting {@code abandonAllowed} is off. State: one game-time tick per asking player, keyed by UUID, pruned on every call (a question lives 30 s) and capped, so it
 * cannot grow; nothing to clean up on logout. Cost: a map lookup per command.
 */
public final class HeroAbandon {
    private HeroAbandon() {}

    private static final int MAX_OPEN = 64;
    private static final Map<UUID, Long> ASKED = new HashMap<>();

    public enum Result { ASKED, DONE, NO_BOND, DISABLED, LOCKED, NOT_ASKED }

    /** First step: remembers the question. */
    public static Result ask(ServerPlayer p) {
        Result why = refusal(p);
        if (why != null) return why;
        long now = p.level().getGameTime();
        prune(now);
        ASKED.put(p.getUUID(), now);
        return Result.ASKED;
    }

    /** Second step: gives the weapon up when the question is still open. */
    public static Result confirm(ServerPlayer p) {
        Result why = refusal(p);
        if (why != null) return why;
        long now = p.level().getGameTime();
        Long at = ASKED.remove(p.getUUID());
        if (!HeroAbandonRules.confirmed(at, now)) return Result.NOT_ASKED;
        return HeroBonds.release(p) ? Result.DONE : Result.NO_BOND;
    }

    /** The weapon the player would give up (null when none), for the question's text. */
    public static HeroKind weapon(ServerPlayer p) { return HeroBonds.kindOf(p); }

    private static Result refusal(ServerPlayer p) {
        if (!HeroConfig.abandonAllowed()) return Result.DISABLED;
        if (!HeroBonds.has(p)) return Result.NO_BOND;
        if (HeroCurse.locked(p)) return Result.LOCKED;
        return null;
    }

    private static void prune(long now) {
        for (Iterator<Map.Entry<UUID, Long>> it = ASKED.entrySet().iterator(); it.hasNext(); ) {
            Long at = it.next().getValue();
            if (!HeroAbandonRules.confirmed(at, now)) it.remove();
        }
        if (ASKED.size() >= MAX_OPEN) ASKED.clear();
    }
}
