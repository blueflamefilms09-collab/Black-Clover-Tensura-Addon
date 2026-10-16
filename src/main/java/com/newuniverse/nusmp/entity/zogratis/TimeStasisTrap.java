package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.boss.god.TrapLedger;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * Why: a Time Stasis Trap is a lingering patch of stopped time laid under a player who keeps dodging, and it needs a body in the world
 * with a visible ring, a warning before it bites, a bounded freeze and the mark that makes Lucius's next hit on the victim a critical.
 * It is deliberately <i>not</i> an entity: an entity would need a registration, a renderer and a save format for something that lives
 * twelve seconds, and the whole rule set ({@link TrapLedger}: six at once, twelve seconds, armed after a second, one catch per player per
 * twelve seconds) is pure. A trap is one record in the boss's ledger plus the rings drawn with the existing Zogratis layers (the ring
 * is the telegraph, the Time Magic clock marks it once it is armed). Whoever steps in is held by the existing Time Magic freeze under the
 * balance law (3 seconds at the most on a player, less on a repeat, never creative or spectator, never a holder of anti-magic) and the
 * next blow of Lucius on them within twelve seconds is a guaranteed critical (x1.5, still under the pressure cap). A boss that is gone
 * or a fight that ended takes its traps with it ({@link LuciusGod#clearTraps}).
 */
final class TimeStasisTrap {
    private TimeStasisTrap() {}

    static final String CRIT_KEY = "nusmp_lu_crit_until";
    private static final int GOLD = FxMath.LUCIUS_GOLD;

    /** Lays a trap at 'at' for 'lifeTicks'. Returns whether one was laid (false when six are already live). */
    static boolean lay(LuciusBoss b, TrapLedger ledger, ServerLevel sl, Vec3 at, int lifeTicks) {
        long now = sl.getGameTime();
        TrapLedger.Trap t = ledger.lay(at.x, at.y, at.z, now, lifeTicks);
        if (t == null) return false;
        int life = (int) (t.dieAt() - now);
        ZogratisVfx.divineSigil(sl, at, TrapLedger.RADIUS + 0.4, life, GOLD, true);                      // the ring is the warning, and it stays
        ZogratisVfx.well(sl, at, TrapLedger.RADIUS, TrapLedger.ARM_TICKS + 4, 0x5B2A8C, true);
        LuciusFx.sound(sl, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 0.7f);
        b.later(TrapLedger.ARM_TICKS, LuciusFx.safe("stasis trap arming", () -> {
            if (!t.live(sl.getGameTime())) return;
            VfxSpawn.send(sl, VfxShape.TIME_CLOCK, at, at.add(0, 2.5, 0), 0, Math.max(10, (int) (t.dieAt() - sl.getGameTime())), 0.9f);      // armed: the existing Time Magic clock
            LuciusFx.sound(sl, at, SoundEvents.BELL_RESONATE, 1.0f, 1.6f);
        }));
        return true;
    }

    /** One look at one player: caught by an armed trap? Then held (bounded) and marked for a critical. Returns the ticks held, 0 if not caught or not holdable. */
    static int check(LuciusBoss b, TrapLedger ledger, ServerPlayer p, long now) {
        if (!ArcSteps.fair(p)) return 0;
        TrapLedger.Trap t = ledger.catchAt(p.getUUID(), p.getX(), p.getY(), p.getZ(), now);
        if (t == null) return 0;
        int held = ArcSteps.hold(b, b, p, 60, false);
        ServerLevel sl = (ServerLevel) p.level();
        ZogratisVfx.spaceShatter(sl, new Vec3(t.x(), t.y() + 0.2, t.z()), 1.6, GOLD);
        if (held > 0) {
            p.getPersistentData().putLong(CRIT_KEY, now + 240);
            p.displayClientMessage(Component.literal("A Time Stasis Trap: you are held for " + String.format(java.util.Locale.ROOT, "%.1f", held / 20.0)
                    + " seconds, and Lucius's next blow will be a critical.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        } else {
            p.displayClientMessage(Component.literal("The Time Stasis Trap cannot hold you.").withStyle(ChatFormatting.GRAY), true);
        }
        return held;
    }

    /** Is a critical waiting for this player (and not yet used)? Reading does not use it. */
    static boolean marked(ServerPlayer p, long now) { return p.getPersistentData().getLong(CRIT_KEY) > now; }

    /** Uses the critical mark: true if there was one. */
    static boolean take(ServerPlayer p, long now) {
        if (!marked(p, now)) return false;
        p.getPersistentData().remove(CRIT_KEY);
        return true;
    }

    static void clearMark(ServerPlayer p) { p.getPersistentData().remove(CRIT_KEY); }
}
