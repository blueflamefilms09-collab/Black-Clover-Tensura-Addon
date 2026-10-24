package com.newuniverse.nusmp.mana.zone;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * The CI self-tests of the Mana Zone upgrade (prefix {@code magic.zone_up}; a stand-in player, no connection, a zone driven at later game times without waiting):
 * <ul>
 *   <li>{@code magic.zone_up.rules}: the shipped numbers are inside their ranges and the "off" set is the 1.99.0 zone;</li>
 *   <li>{@code magic.zone_up.buffs_evasion}: the attack and knockback modifiers come with the zone and leave with it; an arrow flying at the owner is dodged (sidestep, cooldown, one
 *       roll per arrow), one flying away or passing wide is not; the owner's state is dropped with the zone;</li>
 *   <li>{@code magic.zone_up.cast}: the aim packet's server side (outside the zone, not finite, too fast, no zone, a foe that is not where it says), a page cast with a fresh aim is
 *       posed at the origin looking at the target and put back exactly (position, look, velocity, also after the page moved the caster), a stale aim, an ineligible page and the
 *       cast cooldown cast from the hand, and nothing is left behind when the zone ends.</li>
 * </ul>
 */
public final class ZoneUpSelfTests {
    private ZoneUpSelfTests() {}

    public static void register() {
        SelfTests.register("magic.zone_up.rules", ZoneUpSelfTests::rules);
        SelfTests.register("magic.zone_up.buffs_evasion", ZoneUpSelfTests::buffsEvasion);
        SelfTests.register("magic.zone_up.cast", ZoneUpSelfTests::cast);
    }

    private static final ResourceLocation ATTACK = ResourceLocation.fromNamespaceAndPath("nusmp", "mana_zone_attack"), KNOCKBACK = ResourceLocation.fromNamespaceAndPath("nusmp", "mana_zone_knockback");

    private static Result rules(MinecraftServer server) {
        ZoneUpRules.Rules r = ManaZoneConfig.upgrade();
        if (r.evasionChance() < 0 || r.evasionChance() > 0.95 || r.evasionCooldown() < 10 || r.evasionLooks() < 1 || r.evasionLooks() > 8) return Result.fail("evasion numbers outside their ranges: " + r);
        if (r.aimGap() < 1 || r.aimTtl() < 5 || r.castGap() < 2 || r.castReach() < 1 || r.tolerance() < 0) return Result.fail("cast numbers outside their ranges: " + r);
        ZoneUpRules.Rules off = ZoneUpRules.sanitize(ZoneUpRules.OFF);
        if (off.evasion() || off.buffs() || off.cast() || off.castRelocate()) return Result.fail("the OFF set still switches something on");
        ZoneUpRules.Rules wild = ZoneUpRules.sanitize(new ZoneUpRules.Rules(true, 9, -4, 99, 500, 500, 99, 99, true, 9, 9, true, null, true, -3, -3, -3, 99, 99, 99));
        if (wild.evasionChance() > 0.95 || wild.evasionLooks() > 8 || wild.aimGap() < 1 || wild.castMode() == null || wild.tolerance() > 4) return Result.fail("a wild config escaped its ranges: " + wild);
        return Result.ok("chance " + r.evasionChance() + ", cooldown " + r.evasionCooldown() + " t, looks " + r.evasionLooks() + ", aim gap " + r.aimGap() + " t, cast gap " + r.castGap() + " t, origin " + r.castMode()
                + ", evasion " + r.evasion() + ", buffs " + r.buffs() + ", cast " + r.cast());
    }

    private static Arrow arrow(ServerLevel sl, Vec3 at, Vec3 v, net.minecraft.world.entity.Entity owner) {
        Arrow a = EntityType.ARROW.create(sl);
        a.moveTo(at.x, at.y, at.z);
        a.setDeltaMovement(v);
        if (owner != null) a.setOwner(owner);
        return a;
    }

    private static Result buffsEvasion(MinecraftServer server) {
        ServerLevel sl = server.overworld();
        ServerPlayer p = SelfTests.standIn(sl, "zone_up_buffs", 8.5, 120, 8.5);
        UUID id = p.getUUID();
        ManaZoneManager.end(p, ManaZoneManager.End.ADMIN);
        ZoneUpRules.Rules r = ManaZoneConfig.upgrade();
        ManaZoneManager.startForTest(p, 600);
        ManaZoneManager.Zone zone = ManaZoneManager.zoneOf(p);
        if (zone == null) return Result.fail("no zone after startForTest");
        boolean atk = p.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(ATTACK), kb = p.getAttribute(Attributes.KNOCKBACK_RESISTANCE).hasModifier(KNOCKBACK);
        if (r.buffs() && r.attackBonus() > 0 && !atk) return Result.fail("the attack bonus is missing while the zone is open");
        if (r.buffs() && r.knockbackResist() > 0 && !kb) return Result.fail("the knockback resistance is missing while the zone is open");
        if (!r.buffs() && (atk || kb)) return Result.fail("the buffs are switched off and were applied");

        String evasionNote = "evasion switched off";
        if (r.evasion()) {
            Zombie foe = EntityType.ZOMBIE.create(sl);
            Vec3 body = p.getBoundingBox().getCenter();
            long now = sl.getGameTime();
            // an arrow flying away and one passing wide are never dodged
            Arrow away = arrow(sl, body.add(6, 0, 0), new Vec3(1.2, 0, 0), foe), wide = arrow(sl, body.add(6, 0, 5), new Vec3(-1.2, 0, 0), foe);
            if (ZoneEvasion.look(p, zone, away, now) || ZoneEvasion.look(p, zone, wide, now)) return Result.fail("an arrow that does not threaten the owner was dodged");
            if (ZoneEvasion.readyAt(id) > now) return Result.fail("a harmless arrow started the cooldown");
            // an arrow flying at the owner: rolled once, dodged within a handful of arrows (chance default 0.6), then the cooldown holds
            int tries = 0;
            Arrow hit = null;
            boolean dodged = false;
            Vec3 before = p.getDeltaMovement();
            while (!dodged && tries < 60) {
                hit = arrow(sl, body.add(6, 0, 0.2), new Vec3(-1.2, 0, 0), foe);
                dodged = ZoneEvasion.look(p, zone, hit, now);
                tries++;
                if (!dodged && r.evasionChance() <= 0) break;
            }
            if (r.evasionChance() > 0) {
                if (!dodged) return Result.fail("no arrow was dodged in " + tries + " tries");
                Vec3 push = p.getDeltaMovement().subtract(before);
                if (Math.abs(push.x) > 1e-6 || Math.abs(push.z + r.evasionImpulse()) > 1e-6) return Result.fail("the sidestep is not a sideways impulse of " + r.evasionImpulse() + " away from the line: " + push);
                if (ZoneEvasion.readyAt(id) != now + r.evasionCooldown()) return Result.fail("the cooldown is not " + r.evasionCooldown() + " ticks");
                Arrow next = arrow(sl, body.add(6, 0, 0.2), new Vec3(-1.2, 0, 0), foe);
                if (ZoneEvasion.look(p, zone, next, now + 1)) return Result.fail("a second dodge inside the cooldown");
                Arrow later;
                int more = 0;
                boolean again = false;
                while (!again && more < 60) {
                    later = arrow(sl, body.add(6, 0, 0.2), new Vec3(-1.2, 0, 0), foe);
                    again = ZoneEvasion.look(p, zone, later, now + r.evasionCooldown() + 1);
                    more++;
                }
                if (!again) return Result.fail("no dodge after the cooldown ended");
                evasionNote = "dodged after " + tries + " arrows, sidestep " + r.evasionImpulse() + ", cooldown " + r.evasionCooldown() + " t, harmless arrows ignored";
            }
            if (ZoneEvasion.trackedCount() < 1) return Result.fail("the evasion keeps no state for the owner");
        }
        ManaZoneManager.end(p, ManaZoneManager.End.DIMENSION);
        if (p.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(ATTACK) || p.getAttribute(Attributes.KNOCKBACK_RESISTANCE).hasModifier(KNOCKBACK)) return Result.fail("a buff outlived the zone");
        if (ZoneEvasion.readyAt(id) != Long.MIN_VALUE / 4) return Result.fail("the owner's evasion state outlived the zone");
        return Result.ok("buffs " + (atk ? "attack " : "") + (kb ? "knockback " : "") + "on and off with the zone; " + evasionNote);
    }

    /** What the page handler saw while it ran. */
    private static final class Seen {
        Vec3 aim, eye, pos, delta;
        float yRot, xRot;
        net.minecraft.world.entity.LivingEntity foe;
        boolean ran;
    }

    private static boolean run(ServerPlayer p, String page, long at, Seen seen, boolean moveCaster) {
        return ZoneCast.aroundAt(p, page, at, () -> {
            seen.ran = true;
            seen.aim = ZoneCast.aimOverride(p);
            seen.foe = ZoneCast.targetOverride(p);
            seen.eye = p.getEyePosition();
            seen.pos = p.position();
            seen.yRot = p.getYRot();
            seen.xRot = p.getXRot();
            seen.delta = p.getDeltaMovement();
            if (moveCaster) {
                p.setPos(p.getX() + 30, p.getY() + 5, p.getZ());
                p.setDeltaMovement(3, 3, 3);
            }
            return true;
        });
    }

    private static Result cast(MinecraftServer server) {
        ServerLevel sl = server.overworld();
        ServerPlayer p = SelfTests.standIn(sl, "zone_up_cast", 8.5, 120, 8.5);
        ServerPlayer other = SelfTests.standIn(sl, "zone_up_cast_other", 8.5, 120, 8.5);
        ManaZoneManager.end(p, ManaZoneManager.End.ADMIN);
        ZoneCast.clear(p.getUUID());
        ZoneCast.clear(other.getUUID());
        ZoneUpRules.Rules r = ManaZoneConfig.upgrade();
        if (!r.cast()) return Result.ok("zone-cast switched off in the config (nothing to check)");
        long start = sl.getGameTime();

        // a player with no zone: the packet is dropped and no state is made for it
        ZoneCast.onAim(other, 10, 120, 8.5, -1, start + 100);
        if (ZoneCast.hasPending(other.getUUID())) return Result.fail("an aim from a player without a zone was kept");
        // before the zone has grown nothing far away is inside it
        ManaZoneManager.startForTest(p, 4000);
        long base = start + ManaZoneData.zone().growTicks() + 100;
        double cap = ManaZoneManager.capOf(p);
        double cx = p.getX(), cy = p.getY() + 0.9, cz = p.getZ();
        ZoneCast.onAim(p, cx + 8, cy, cz, -1, start);
        if (ZoneCast.hasPending(p.getUUID())) return Result.fail("an aim was kept while the zone had no radius yet");

        // the packet's rejections
        ZoneCast.onAim(p, Double.NaN, cy, cz, -1, base);
        ZoneCast.onAim(p, cx + cap + 20, cy, cz, -1, base);
        ZoneCast.onAim(p, 1.0e12, cy, cz, -1, base);
        if (ZoneCast.hasPending(p.getUUID())) return Result.fail("a NaN, a far or an absurd target was kept");
        ZoneCast.onAim(p, cx + 8, cy, cz, -1, base);
        if (!ZoneCast.hasPending(p.getUUID())) return Result.fail("a valid aim inside the zone was dropped");
        ZoneCast.onAim(p, cx - 8, cy, cz, -1, base);                                                 // same tick: too fast, the first one stays
        Seen s = new Seen();
        Vec3 pos0 = p.position(), delta0 = new Vec3(0.1, 0.2, 0.3);
        float yaw0 = 33f, pitch0 = -12f;
        p.setDeltaMovement(delta0);
        p.setYRot(yaw0);
        p.setXRot(pitch0);

        // a fresh aim: the page sees the target and is posed at the origin looking at it; everything is put back
        if (!run(p, "fire_ball", base + 1, s, false) || !s.ran) return Result.fail("the page handler did not run");
        if (s.aim == null || Math.abs(s.aim.x - (cx + 8)) > 1e-9) return Result.fail("the page did not see the aimed point: " + s.aim);
        boolean posed = s.eye.distanceTo(new Vec3(pos0.x, pos0.y + p.getEyeHeight(), pos0.z)) > 0.5;
        if (r.castRelocate() && posed) {
            Vec3 toTarget = new Vec3(cx + 8, cy, cz).subtract(s.eye).normalize();
            Vec3 look = Vec3.directionFromRotation(s.xRot, s.yRot);
            if (toTarget.dot(look) < 0.999) return Result.fail("the posed caster does not look at the target (dot " + toTarget.dot(look) + ")");
            if (!ManaZoneMath.inside(s.eye.x - cx, s.eye.y - cy, s.eye.z - cz, ManaZoneManager.radius(ManaZoneManager.zoneOf(p), base + 1))) return Result.fail("the origin lies outside the zone");
        }
        if (p.position().distanceTo(pos0) > 1e-9 || p.getDeltaMovement().distanceTo(delta0) > 1e-9 || p.getYRot() != yaw0 || p.getXRot() != pitch0) return Result.fail("the caster's pose was not put back");
        if (ZoneCast.aimOverride(p) != null) return Result.fail("the override outlived the cast");
        if (ZoneCast.hasPending(p.getUUID())) return Result.fail("an aim served two casts");

        // no aim: the page is cast from the hand
        Seen plain = new Seen();
        run(p, "fire_ball", base + 20, plain, false);
        if (plain.aim != null || plain.pos.distanceTo(pos0) > 1e-9) return Result.fail("a cast without an aim was changed");

        // the cast cooldown: an aim 3 ticks after a cast is dropped (the page is cast from the hand), later one is served
        ZoneCast.onAim(p, cx + 6, cy, cz + 2, -1, base + 20);
        Seen gap = new Seen();
        run(p, "fire_ball", base + 21, gap, false);
        if (gap.aim == null) return Result.fail("the first cast after a long pause was refused");
        ZoneCast.onAim(p, cx + 6, cy, cz + 2, -1, base + 23);
        Seen tooSoon = new Seen();
        run(p, "fire_ball", base + 24, tooSoon, false);
        if (tooSoon.aim != null && r.castGap() > 3) return Result.fail("a second zone-cast inside the cast cooldown was served");
        ZoneCast.onAim(p, cx + 6, cy, cz + 2, -1, base + 30);
        Seen again = new Seen();
        run(p, "fire_ball", base + 31, again, false);
        if (again.aim == null) return Result.fail("a zone-cast after the cooldown was refused");

        // an ineligible page is never zone-cast (and uses the aim up)
        ZoneCast.onAim(p, cx + 6, cy, cz + 2, -1, base + 50);
        Seen bad = new Seen();
        run(p, "zone_omni_volley", base + 51, bad, false);
        if (bad.aim != null || ZoneCast.hasPending(p.getUUID())) return Result.fail("an ineligible page was zone-cast or left the aim pending");

        // a stale aim is not used
        ZoneCast.onAim(p, cx + 6, cy, cz + 2, -1, base + 60);
        Seen stale = new Seen();
        run(p, "fire_ball", base + 60 + r.aimTtl() + 2, stale, false);
        if (stale.aim != null) return Result.fail("an aim older than " + r.aimTtl() + " ticks was served");

        // a page that moves the caster is undone
        ZoneCast.onAim(p, cx + 6, cy, cz + 2, -1, base + 200);
        Seen mover = new Seen();
        run(p, "fire_ball", base + 201, mover, true);
        boolean undone = p.position().distanceTo(pos0) <= 1e-9 && p.getDeltaMovement().distanceTo(delta0) <= 1e-9;
        if (r.castRelocate() && posed && !undone) return Result.fail("a page that moved the posed caster left it moved: " + p.position() + " " + p.getDeltaMovement());
        p.setPos(pos0.x, pos0.y, pos0.z);                                                           // (a cast that was not posed is the page's own business)
        p.setDeltaMovement(delta0);

        // a foe: the id is kept only when the foe is where the packet says
        Zombie z1 = EntityType.ZOMBIE.create(sl), z2 = EntityType.ZOMBIE.create(sl);
        z1.moveTo(cx + 7, p.getY(), cz);
        z2.moveTo(cx - 9, p.getY(), cz + 9);
        sl.addFreshEntity(z1);
        sl.addFreshEntity(z2);
        try {
            Vec3 c1 = z1.getBoundingBox().getCenter();
            ZoneCast.onAim(p, c1.x, c1.y, c1.z, z1.getId(), base + 300);
            Seen foe = new Seen();
            run(p, "fire_ball", base + 301, foe, false);
            if (foe.foe != z1) return Result.fail("the foe the aim named was not seen by the page");
            ZoneCast.onAim(p, c1.x, c1.y, c1.z, z2.getId(), base + 320);                           // an id that is not where the point is
            Seen lie = new Seen();
            run(p, "fire_ball", base + 321, lie, false);
            if (lie.foe != null || lie.aim == null) return Result.fail("a foe id that is not at the aimed point was believed");
            ZoneCast.onAim(p, c1.x, c1.y, c1.z, 123456789, base + 340);
            Seen junk = new Seen();
            run(p, "fire_ball", base + 341, junk, false);
            if (junk.foe != null) return Result.fail("an id that names no entity was believed");
        } finally {
            z1.discard();
            z2.discard();
        }

        // the end of the zone drops everything of this caster
        ZoneCast.onAim(p, cx + 6, cy, cz + 2, -1, base + 400);
        ManaZoneManager.end(p, ManaZoneManager.End.LOGOUT);
        if (ZoneCast.hasPending(p.getUUID())) return Result.fail("an aim outlived the zone");
        ZoneCast.onAim(p, cx + 6, cy, cz + 2, -1, base + 500);
        if (ZoneCast.hasPending(p.getUUID())) return Result.fail("an aim was kept after the zone ended");
        return Result.ok("aim validated (zone, finite, rate, radius, foe id), posed at origin " + r.castMode() + (r.castRelocate() ? (posed ? " (posed)" : " (no clear origin here: helpers only)") : " (helpers only)")
                + ", pose/velocity restored, cooldown " + r.castGap() + " t, ttl " + r.aimTtl() + " t, ineligible page and stale aim cast from the hand, state dropped with the zone");
    }
}
