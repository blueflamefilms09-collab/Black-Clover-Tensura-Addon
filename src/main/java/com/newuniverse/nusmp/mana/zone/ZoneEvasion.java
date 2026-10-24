package com.newuniverse.nusmp.mana.zone;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import com.newuniverse.nusmp.perf.PerfBudget;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The evasion of the Mana Zone (docs/magic/mana_zone_upgrade.md): the owner of a zone reads the flight of a projectile that is about to hit and steps out of its way, the server
 * deciding every time (chance, cooldown, a little mana, a per-tick budget). The zone already DESTROYS a plain foreign projectile that enters it ({@code ManaZoneManager.scan}); this is
 * for what that leaves: a boss's projectile (only slowed), one over the scan's budget, and one born next to the owner between two scans.
 * <ul>
 *   <li><b>The look</b> ({@link #look}): called by the zone scan for the projectiles it already found with its single AABB query and did not destroy; the owner examines at most
 *       {@code evasionLooks} of them per scan (default 4), no world query of its own. The straight-line flight is projected {@code evasionHorizon} ticks ahead ({@link ZoneUpMath#timeToHit});
 *       a projectile that will pass within {@code evasionHitRadius} of the body is rolled once; on a success the owner gets a sideways impulse (perpendicular to the flight, towards the side
 *       already stood on) that carries the body out of the line before it arrives.</li>
 *   <li><b>The backstop</b> ({@link #onImpact}): a projectile that reaches the owner anyway fires {@code ProjectileImpactEvent}; the same decision is taken (the projectile's earlier
 *       roll is remembered), and a dodged impact is cancelled so the projectile flies on through, and stays cancelled for a few ticks while it overlaps the body.</li>
 * </ul>
 * State: one small object per zone owner (the cooldown and the last four decided projectiles), dropped when the zone ends. Cost: nothing while no zone is open; a few multiplications per
 * examined projectile; at most one dodge (a small impulse, one effect packet, one sound) per owner per cooldown and four per server tick.
 */
public final class ZoneEvasion {
    private ZoneEvasion() {}

    private static final int RING = 4;
    /** Ticks a decided projectile is remembered (also how long a dodged one stays cancelled). */
    private static final int REMEMBER = 40;
    private static final PerfBudget DODGES = new PerfBudget("zone-evasion", 4);

    static final class State {
        long nextReady = Long.MIN_VALUE / 4;
        final int[] id = new int[RING];
        final long[] until = new long[RING];
        final boolean[] dodged = new boolean[RING];
        int next;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    /** Projectiles the evasion is about: the ones the zone scan also leaves alone are not (a pearl, a hook, an experience bottle), nor the owner's own and an ally's. */
    static boolean relevant(ServerPlayer p, Projectile pr) {
        if (!pr.isAlive() || pr.isRemoved() || pr instanceof ThrownEnderpearl || pr instanceof FishingHook || pr instanceof ThrownExperienceBottle) return false;
        Entity owner = pr.getOwner();
        return owner != p && !(owner instanceof LivingEntity lo && Foes.isAlly(p, lo));
    }

    private static int slot(State s, int id, long now) {
        for (int i = 0; i < RING; i++) if (s.id[i] == id && s.until[i] > now) return i;
        return -1;
    }

    private static void remember(State s, int id, long now, boolean dodged) {
        int i = s.next;
        s.next = (i + 1) % RING;
        s.id[i] = id;
        s.until[i] = now + REMEMBER;
        s.dodged[i] = dodged;
    }

    // ---------------------------------------------------------------- the look (from the zone scan)
    /** One projectile the scan found and did not destroy. Returns true when the owner dodged it. */
    static boolean look(ServerPlayer p, ManaZoneManager.Zone zone, Projectile pr, long now) {
        ZoneUpRules.Rules r = ManaZoneConfig.upgrade();
        if (!r.evasion()) return false;
        State s = STATES.get(p.getUUID());
        if (s != null && now < s.nextReady) return false;
        if (!relevant(p, pr)) return false;
        Vec3 v = pr.getDeltaMovement(), at = pr.position(), c = p.getBoundingBox().getCenter();
        if (ZoneUpMath.timeToHit(at.x, at.y, at.z, v.x, v.y, v.z, c.x, c.y, c.z, r.evasionHitRadius(), r.evasionHorizon()) < 0) return false;
        if (s == null) {
            s = new State();
            STATES.put(p.getUUID(), s);
        }
        if (slot(s, pr.getId(), now) >= 0) return false;                               // rolled already
        return decide(p, zone, pr, s, r, now);
    }

    // ---------------------------------------------------------------- the backstop
    public static void onImpact(ProjectileImpactEvent e) {
        if (ManaZoneManager.activeCount() == 0) return;                                // the idle path
        Projectile pr = e.getProjectile();
        if (pr.level().isClientSide || !(e.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ServerPlayer p)) return;
        ManaZoneManager.Zone zone = ManaZoneManager.zoneOf(p);
        if (zone == null || zone.dim != p.level().dimension()) return;
        ZoneUpRules.Rules r = ManaZoneConfig.upgrade();
        if (!r.evasion() || !relevant(p, pr)) return;
        long now = p.level().getGameTime();
        State s = STATES.get(p.getUUID());
        if (s != null) {
            int i = slot(s, pr.getId(), now);
            if (i >= 0) {
                if (s.dodged[i]) e.setCanceled(true);                                  // dodged earlier, still overlapping: it flies through
                return;                                                                // rolled and failed: it hits
            }
        } else {
            s = new State();
            STATES.put(p.getUUID(), s);
        }
        if (now < s.nextReady) return;
        if (decide(p, zone, pr, s, r, now)) e.setCanceled(true);
    }

    // ---------------------------------------------------------------- the decision
    /** Rolls once for this projectile: cooldown, chance, the budget and the mana; on success sidesteps. Remembers the outcome. */
    private static boolean decide(ServerPlayer p, ManaZoneManager.Zone zone, Projectile pr, State s, ZoneUpRules.Rules r, long now) {
        boolean boss = pr.getOwner() instanceof LivingEntity lb && BalanceLaw.isBoss(lb);
        double chance = ZoneUpMath.dodgeChance(r.evasionChance(), zone.reinforcement, boss);
        if (p.getRandom().nextDouble() >= chance) {
            remember(s, pr.getId(), now, false);
            return false;
        }
        if (!DODGES.tryTake(now, 1)) return false;                                      // the server is busy: no dodge this tick, roll again at the next look
        if (!zone.free && !p.isCreative() && r.evasionCostPct() > 0) {
            double max = EnergyHelper.getMaxMagicule(p);
            if (EnergyHelper.isOutOfMagiculeConsuming(p, max * r.evasionCostPct() / 100.0, max * 0.05) > 0) {
                remember(s, pr.getId(), now, false);                                    // no mana to pay for it
                return false;
            }
        }
        s.nextReady = now + r.evasionCooldown();
        remember(s, pr.getId(), now, true);
        step(p, pr, r);
        return true;
    }

    private static void step(ServerPlayer p, Projectile pr, ZoneUpRules.Rules r) {
        Vec3 v = pr.getDeltaMovement(), rel = p.position().subtract(pr.position());
        double[] out = new double[2];
        ZoneUpMath.sidestep(v.x, v.z, rel.x, rel.z, r.evasionImpulse(), p.getRandom().nextInt(2), out);
        Vec3 from = p.position();
        p.setDeltaMovement(p.getDeltaMovement().add(out[0], 0.0, out[1]));
        p.hurtMarked = true;                                                           // the client is told its new motion (the same way a knockback is)
        ServerLevel sl = p.serverLevel();
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, from.add(0, 0.1, 0), from.add(0, 1, 0), ManaZoneManager.tintOf(p), 6, 0.3f);
        sl.playSound(null, p.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.7f, 1.6f);
    }

    // ---------------------------------------------------------------- lifecycle
    public static void clear(UUID id) { STATES.remove(id); }

    public static void clearAll() { STATES.clear(); }

    static int trackedCount() { return STATES.size(); }

    /** For the self-test: the owner's cooldown (game time it ends), or a very early time when it has none. */
    static long readyAt(UUID id) {
        State s = STATES.get(id);
        return s == null ? Long.MIN_VALUE / 4 : s.nextReady;
    }
}
