package com.newuniverse.nusmp.core.magic.profile;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.blackclover.MagicType;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.function.BooleanSupplier;

/**
 * The runtime guard around one magic's code. A single magic's cast (or its effect, or one of its per-tick callbacks) that throws must
 * never crash the server tick or the client frame: the exception is caught, logged ONCE per magic id, the magic goes on a short
 * recovery cooldown (100 ticks, doubling with every further fault up to 60 s) and the generic effect plays instead.
 *
 * <pre>
 *   // central cast entry (book.GrimoireBook.castPage):
 *   if (SafeCast.blocked(magicId, now)) { fail(...); return; }                 // before any mana is spent
 *   boolean done = SafeCast.run(magicId, now, player, () -&gt; page.cast().cast(this, instance, player, mode));
 * </pre>
 */
public final class SafeCast {
    private SafeCast() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final int BASE_COOLDOWN_TICKS = 100, MAX_COOLDOWN_TICKS = 1200;

    private static final SafeRunner SERVER = new SafeRunner(new FaultLedger(BASE_COOLDOWN_TICKS, MAX_COOLDOWN_TICKS, m -> LOG.error("[nusmp] {}", m)));
    private static final SafeRunner CLIENT = new SafeRunner(new FaultLedger(BASE_COOLDOWN_TICKS, MAX_COOLDOWN_TICKS, m -> LOG.error("[nusmp] {}", m)));

    /** True while this magic is on its recovery cooldown after a fault. Check it BEFORE spending mana. */
    public static boolean blocked(String magicId, long gameTime) { return SERVER.ledger().blocked(magicId, gameTime); }

    public static boolean blocked(MagicType magic, long gameTime) { return magic != null && blocked(magic.name(), gameTime); }

    public static int ticksLeft(String magicId, long gameTime) { return SERVER.ledger().ticksLeft(magicId, gameTime); }

    /** Runs a cast; on a fault plays the magic's generic fallback effect for {@code player} and reports the cast as spent. */
    public static boolean run(String magicId, long gameTime, ServerPlayer player, BooleanSupplier cast) {
        return SERVER.run(magicId, gameTime, "cast", cast, () -> MagicProfiles.of(magicId).fallbackCast(player));
    }

    /** Same with a caller-supplied fallback (a merged magic, a test). */
    public static boolean run(String magicId, long gameTime, BooleanSupplier cast, Runnable fallback) {
        return SERVER.run(magicId, gameTime, "cast", cast, fallback);
    }

    /**
     * Runs a server-side callback that has no magic id of its own (a bolt hit, a zone step, a delayed task): the exception is logged
     * once per {@code key}, never rethrown. Returns false if it threw.
     */
    public static boolean tick(String key, Runnable code) { return SERVER.guard(key, 0L, key, code); }

    /** Same for client-side drawing code: logged once per key, never rethrown. Returns false if it threw. */
    public static boolean client(String key, Runnable code) { return CLIENT.guard(key, 0L, key, code); }

    /** Records a server-side fault that was caught elsewhere (logged once per key). */
    public static void fault(String key, Throwable t) { SERVER.ledger().record(key, 0L, key, t); }

    /** Records a fault that was caught elsewhere (the VFX manager removing a broken instance). */
    public static void clientFault(String key, Throwable t) { CLIENT.ledger().record(key, 0L, key, t); }

    /** Forgets every fault (a world reload, a test, a command). */
    public static void reset() { SERVER.ledger().clearAll(); CLIENT.ledger().clearAll(); }

    public static FaultLedger serverLedger() { return SERVER.ledger(); }
    public static FaultLedger clientLedger() { return CLIENT.ledger(); }
}
