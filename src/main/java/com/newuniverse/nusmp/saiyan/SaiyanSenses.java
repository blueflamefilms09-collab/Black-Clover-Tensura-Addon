package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.balance.BalanceLaw;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Why: the Saiyan Senses and Ki Suppression (3.65.0, Stage 2). The senses are a RADAR, not a wallhack on the world: the server looks at the creatures near the Saiyan once every two seconds while the
 * sense is on, keeps the nearest few that are detectable (stealth shortens the reach: Ki Suppression, invisibility, sneaking) and sends at most {@link SaiyanSenseRules#MAX_ENTRIES} small entries (where, how
 * strong a presence, what kind) to that player only; the client draws them in the Saiyan panel. The work is bounded: one entity query of a box of the tier's radius (vertical reach cut at 48 blocks) per
 * two seconds per sensing player, at most {@link #SCANS_PER_TICK} such queries on the whole server in a tick (a busy tick delays a scan by a tick, never drops it), at most
 * {@link SaiyanSenseRules#MAX_CANDIDATES} creatures examined, no entity spawned. Ki Suppression hides the Saiyan from other Saiyans' senses and costs a little Ki a second.
 */
final class SaiyanSenses {
    private SaiyanSenses() {}

    static final int SCANS_PER_TICK = 4;

    /** {game tick, scans done in that tick}: the server-wide budget (server thread only). */
    private static long budgetTick = -1;
    private static int budgetUsed;

    static SaiyanSenseRules.Tier tier(SaiyanState s) { return SaiyanSenseRules.tierOf(s.uses("sense"), SaiyanConfig.senseScans()); }

    private static boolean budget(long now) {
        if (now != budgetTick) {
            budgetTick = now;
            budgetUsed = 0;
        }
        return budgetUsed++ < SCANS_PER_TICK;
    }

    /** The sense key: switches the radar on for a few seconds. */
    static boolean sense(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        if (!s.awakened) {
            SaiyanAbilities.say(p, "not_awakened");
            return false;
        }
        if (p.isSpectator() || r.radarLeft > 0 || !s.ready("sense", now)) return false;
        if (!SaiyanKi.pay(p, SaiyanConfig.senseCost())) {
            SaiyanAbilities.say(p, "no_ki");
            return false;
        }
        s.startCooldown("sense", now, SaiyanConfig.senseCooldown() + SaiyanConfig.senseSeconds() * 20);
        r.radarLeft = SaiyanConfig.senseSeconds();
        r.lastPing = -1000;
        SaiyanSenseRules.Tier before = tier(s);
        s.use("sense", 1);
        SaiyanSenseRules.Tier after = tier(s);
        if (after != before) p.displayClientMessage(Component.translatable("nusmp.saiyan.msg.sense_tier", Component.translatable(after.key())), true);
        return true;
    }

    /** The suppression key: hide the Ki (and pay a little each second), or stop hiding it. */
    static boolean suppress(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        if (!s.awakened) {
            SaiyanAbilities.say(p, "not_awakened");
            return false;
        }
        if (r.suppressed) {
            r.suppressed = false;
            return true;
        }
        if (p.isSpectator()) return false;
        if (!SaiyanKi.canPay(p, SaiyanConfig.suppressCost())) {
            SaiyanAbilities.say(p, "no_ki");
            return false;
        }
        r.suppressed = true;
        return true;
    }

    static boolean suppressed(ServerPlayer sp) {
        SaiyanManager.Run r = SaiyanManager.peekRun(sp);
        return r != null && r.suppressed;
    }

    /** Once a second: the radar's time, the suppression's Ki and its mastery seconds. */
    static void second(ServerPlayer p, SaiyanState s, SaiyanManager.Run r) {
        if (r.suppressed) {
            if (SaiyanKi.pay(p, SaiyanConfig.suppressCost())) {
                s.use("suppress", 1);
            } else {
                r.suppressed = false;
                r.dirty = true;
                SaiyanAbilities.say(p, "suppress_off");
            }
        }
        if (r.radarLeft > 0 && --r.radarLeft == 0) {
            SaiyanNet.send(p, new SaiyanNet.Ping(0, 1, List.of()));
            r.dirty = true;
        }
    }

    /** Every tick while the radar is on; it acts once every two seconds. */
    static void radarTick(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        if (r.radarLeft <= 0 || now - r.lastPing < 40 || !budget(now)) return;
        r.lastPing = now;
        SaiyanSenseRules.Tier t = tier(s);
        double radius = SaiyanSenseRules.radius(t, SaiyanConfig.senseMaxRadius());
        AABB box = p.getBoundingBox().inflate(radius, Math.min(radius, 48.0), radius);
        List<LivingEntity> near = p.serverLevel().getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand));
        List<LivingEntity> felt = new ArrayList<>();
        int seen = 0;
        for (LivingEntity e : near) {
            if (++seen > SaiyanSenseRules.MAX_CANDIDATES) break;
            boolean hidden = e instanceof ServerPlayer sp && suppressed(sp);
            if (SaiyanSenseRules.detectable(Math.sqrt(p.distanceToSqr(e)), radius, hidden, e.isInvisible(), e.isShiftKeyDown())) felt.add(e);
        }
        felt.sort(Comparator.comparingDouble((LivingEntity e) -> p.distanceToSqr(e)));
        int n = Math.min(SaiyanSenseRules.MAX_ENTRIES, felt.size());
        List<SaiyanNet.Entry> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            LivingEntity e = felt.get(i);
            int sig = SaiyanSenseRules.signature(SaiyanManager.powerOfAny(e), s.battlePower);
            int kind = BalanceLaw.isBoss(e) ? 2 : e instanceof Player ? 1 : 0;
            int dy = (int) Math.max(-127, Math.min(127, Math.round(e.getY() - p.getY())));
            out.add(new SaiyanNet.Entry(SaiyanSenseRules.pack(e.getX() - p.getX()), SaiyanSenseRules.pack(e.getZ() - p.getZ()), (byte) dy, (byte) sig, (byte) kind));
        }
        SaiyanNet.send(p, new SaiyanNet.Ping(r.radarLeft, (int) radius, out));
    }
}
