package com.newuniverse.nusmp.heroweapon.weapon.carriage;

import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Moves;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Status;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Timed;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.newuniverse.nusmp.heroweapon.weapon.carriage.CarriageNumbers.*;

/**
 * Why: the bodies of the Carriage's nine techniques. The carriage is a vehicle, so most of them are orders given to it: call it (or send it away), gallop,
 * sweep a ring round it, raise its barrier, make it faster, charge. Every order that needs the carriage first checks that the wielder is DRIVING it (sitting in
 * the driver's seat of their own carriage) and is refused otherwise, with nothing spent. The blows themselves are struck by the carriage entity while a
 * boost or a charge runs ({@link CarriageEntity}); the Drift Ram's sweep is done here, after a ring that follows the carriage shows where it will land. The
 * Portal Gate moves a vehicle and its riders across the world, so it shows its gates first and re-checks everything when it opens. All damage goes through
 * {@code HeroCtx.hit}, all healing through {@code HeroCtx.heal}, nothing breaks or places a block, and nobody is moved except to a spot with room and ground.
 */
final class CarriageSpells {
    private CarriageSpells() {}

    /** The carriage's colours: gold for the wheels and the gust, blue for the barrier and the gate (0xRRGGBB for the ground rings, 0xAARRGGBB for the effects). */
    static final int GOLD_RGB = 0xFFC247, GOLD = 0xFFFFC247, BLUE_RGB = 0x5AA8FF, BLUE = 0xFF5AA8FF;
    private static final String SYSTEM = "carriage";
    /** How many allies an ally query looks at (the list is cut to the technique's own target count afterwards). */
    private static final int SEARCH = 12;

    // ================================================================ finding things
    /** The wielder's carriage in any loaded dimension, or null. */
    static CarriageEntity mine(HeroCtx c) { return CarriageRegistry.find(c.level.getServer(), c.player.getUUID()); }

    /** The carriage the wielder is driving right now (sitting in the driver's seat of their own), or null. */
    static CarriageEntity driven(HeroCtx c) {
        Entity v = c.player.getVehicle();
        return v instanceof CarriageEntity car && car.isAlive() && !car.isRemoved() && c.player.getUUID().equals(car.ownerUuid()) ? car : null;
    }

    /** What the form's passives make of the carriage. */
    static Chassis chassis(HeroCtx c) {
        return CarriageNumbers.chassis(c.form.has("roomy_cabin"), c.form.has("swift_wheels"), c.form.has("armoured_frame"), c.form.has("queens_pull"));
    }

    /** The horizontal direction an entity faces, as a unit vector. */
    static Vec3 forward(Entity e) {
        double yaw = Math.toRadians(e.getYRot());
        return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
    }

    /** Seats a rider on the carriage by force (the caller has checked the rules); false when the game refused. */
    private static boolean seat(Entity rider, CarriageEntity car) {
        try {
            return rider.startRiding(car, true);
        } catch (RuntimeException e) {
            return false;
        }
    }

    // ================================================================ room for the carriage
    /** The first of the heights y, y+1, y-1, y+2, y-2 where the carriage's whole body fits at (x, z) and there is ground under it, or null. */
    static Vec3 roomFor(ServerLevel level, double x, double y, double z) {
        double half = WIDTH / 2.0;
        for (int dy : new int[]{0, 1, -1, 2, -2}) {
            double yy = y + dy;
            AABB box = new AABB(x - half, yy, z - half, x + half, yy + HEIGHT, z + half);
            if (level.noCollision(box) && Moves.groundBelow(level, BlockPos.containing(x, yy, z))) return new Vec3(x, yy, z);
        }
        return null;
    }

    /** A place for the carriage in front of the wielder, else beside them, else farther ahead; null when there is none. */
    private static Vec3 spotFor(HeroCtx c) {
        Vec3 base = c.player.position(), fwd = forward(c.player);
        Vec3 left = new Vec3(fwd.z, 0.0, -fwd.x);
        double[][] tries = {{CALL_AHEAD, 0.0}, {CALL_AHEAD + 1.2, 0.0}, {CALL_AHEAD, 2.6}, {CALL_AHEAD, -2.6}, {CALL_AHEAD + 2.4, 0.0}, {CALL_AHEAD + 1.2, 2.6}, {CALL_AHEAD + 1.2, -2.6}};
        for (double[] t : tries) {
            Vec3 spot = roomFor(c.level, base.x + fwd.x * t[0] + left.x * t[1], base.y, base.z + fwd.z * t[0] + left.z * t[1]);
            if (spot != null) return spot;
        }
        return null;
    }

    // ================================================================ Call the Carriage
    static boolean call(HeroCtx c) {
        CarriageEntity riding = driven(c);
        if (riding != null) {                                                       // used while driving: the carriage is sent away
            riding.dismiss();
            c.say("The carriage leaves");
            return true;
        }
        if (c.player.isPassenger()) {
            c.say("Get off what you are riding first");
            return false;
        }
        Vec3 spot = spotFor(c);
        if (spot == null) {
            c.say("There is no room for the carriage here");
            return false;
        }
        CarriageEntity old = mine(c);                                               // one carriage at a time: a carriage already out is brought here as a fresh one
        if (old != null) old.dismiss();
        CarriageEntity car = CarriageEntity.summon(c.level, c.player, chassis(c), spot, c.player.getYRot());
        if (car == null) {
            c.say("The carriage could not be called");
            return false;
        }
        boolean seated = seat(c.player, car);
        c.fx(VfxShape.WIND_RING, spot.add(0.0, 0.2, 0.0), spot.add(0.0, 1.0, 0.0), GOLD, 20, 1.8f);
        c.sound(SoundEvents.HORSE_GALLOP, 0.8f, 1.5f);
        c.say(seated ? "The carriage is here" : "The carriage is here: step onto it");
        return true;
    }

    // ================================================================ Gallop Boost
    static boolean boost(HeroCtx c) {
        CarriageEntity car = driven(c);
        if (car == null) {
            c.say("Drive the carriage to gallop");
            return false;
        }
        car.startBoost(BOOST_TICKS);
        Vec3 from = car.position().add(0.0, 0.8, 0.0);
        c.fx(VfxShape.WIND_GALE, from, from.add(forward(car).scale(9.0)), GOLD, 14, 1.4f);
        c.sound(SoundEvents.HORSE_GALLOP, 1.0f, 1.2f);
        return true;
    }

    // ================================================================ Drift Ram
    static boolean driftRam(HeroCtx c) {
        CarriageEntity car = driven(c);
        if (car == null) {
            c.say("Drive the carriage to drift");
            return false;
        }
        UUID id = car.getUUID();
        c.sound(SoundEvents.HORSE_ANGRY, 0.9f, 0.7f);
        // a ring that follows the carriage during the wind-up, then the sweep round wherever the carriage is when it ends
        Timed.zone(c, DRIFT_DELAY, 2, (live, age) -> {
            CarriageEntity now = mine(live);
            if (now == null || !now.getUUID().equals(id) || now.level() != live.level) return;
            if (age < DRIFT_DELAY) Tells.ring(live.level, now.position(), DRIFT_RADIUS, GOLD_RGB);
            else sweep(live, now);
        });
        return true;
    }

    private static void sweep(HeroCtx c, CarriageEntity car) {
        Vec3 at = car.position();
        for (LivingEntity t : Foes.around(c, at, DRIFT_RADIUS, DRIFT_RAM.maxTargets())) {
            if (!c.hit(t, (float) DRIFT_RAM.rawBase())) continue;
            c.shove(t, at, DRIFT_PUSH);
            Status.slow(t, DRIFT_SLOW_TICKS, 1);
        }
        c.fx(VfxShape.WIND_RING, at.add(0.0, 0.3, 0.0), at.add(0.0, 1.0, 0.0), GOLD, 18, (float) DRIFT_RADIUS * 0.5f);
        c.fx(VfxShape.GRAVITY_IMPACT, at, at, GOLD, 14, (float) DRIFT_RADIUS * 0.6f);
        c.sound(SoundEvents.IRON_GOLEM_ATTACK, 0.9f, 0.8f);
    }

    // ================================================================ Soothing Ride
    private static void add(List<LivingEntity> list, LivingEntity e, int max) {
        if (list.size() < max && !list.contains(e)) list.add(e);
    }

    static boolean comfort(HeroCtx c) {
        CarriageEntity car = mine(c);
        boolean near = car != null && car.level() == c.level;
        int max = COMFORT.maxTargets();
        List<LivingEntity> who = new ArrayList<>();
        add(who, c.player, max);
        if (near) for (Entity p : car.getPassengers()) if (p instanceof LivingEntity l && Foes.isAlly(c.player, l)) add(who, l, max);
        for (LivingEntity e : Foes.alliesAround(c, c.player.position(), COMFORT_RADIUS, SEARCH)) add(who, e, max);
        if (near) for (LivingEntity e : Foes.alliesAround(c, car.position(), COMFORT_RADIUS, SEARCH)) add(who, e, max);
        Vec3 from = (near ? car.position() : c.player.position()).add(0.0, 1.2, 0.0);
        int helped = 0;
        for (LivingEntity t : who) {
            boolean hurt = t.getHealth() < t.getMaxHealth();
            boolean cured = Status.cleanse(t);
            if (hurt) c.heal(t, (float) (t.getMaxHealth() * COMFORT.heal()));
            if (!hurt && !cured) continue;
            helped++;
            c.fx(VfxShape.SEAL_WOUND, from, t.position().add(0.0, 1.0, 0.0), GOLD, 20, 1.0f);
        }
        if (helped == 0) {
            c.say("Everyone is well");
            return false;
        }
        c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 1.2f);
        return true;
    }

    // ================================================================ Cabin Barrier
    static boolean cabinBarrier(HeroCtx c) {
        CarriageEntity car = driven(c);
        if (car == null) {
            c.say("Raise the barrier from the driver's seat");
            return false;
        }
        car.startBarrier(BARRIER_TICKS);
        for (Entity p : car.getPassengers()) if (p instanceof LivingEntity l) Status.add(l, MobEffects.DAMAGE_RESISTANCE, BARRIER_TICKS, BARRIER_AMP);
        VfxSpawn.sendFollowing(c.level, VfxShape.BARRIER_FX2, car, car.position(), BLUE, BARRIER_TICKS, 2.6f);
        c.sound(SoundEvents.SHIELD_BLOCK, 0.9f, 0.7f);
        return true;
    }

    // ================================================================ Haikuikku
    static boolean haikuikku(HeroCtx c) {
        CarriageEntity car = driven(c);
        List<LivingEntity> who = new ArrayList<>();
        if (car != null) {
            car.startHaste(HAIKUIKKU_TICKS);
            for (Entity p : car.getPassengers()) if (p instanceof LivingEntity l) add(who, l, HAIKUIKKU.maxTargets());
        } else {                                                                    // on foot: the wielder and the allies beside them
            for (LivingEntity e : Foes.alliesAround(c, c.player.position(), COMFORT_RADIUS, HAIKUIKKU.maxTargets())) add(who, e, HAIKUIKKU.maxTargets());
        }
        for (LivingEntity t : who) Status.add(t, MobEffects.MOVEMENT_SPEED, HAIKUIKKU_TICKS, HAIKUIKKU_AMP);
        Entity follow = car != null ? car : c.player;
        VfxSpawn.sendFollowing(c.level, VfxShape.WIND_ZEPHYR, follow, follow.position(), GOLD, 40, 1.4f);
        c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.6f);
        return true;
    }

    // ================================================================ Mark Waypoint
    static boolean waypoint(HeroCtx c) {
        Entity base = c.player.getVehicle() instanceof CarriageEntity car ? car : c.player;
        Vec3 at = base.position();
        CompoundTag tag = c.bond.extra(SYSTEM);
        tag.putDouble("wpx", at.x);
        tag.putDouble("wpy", at.y);
        tag.putDouble("wpz", at.z);
        tag.putString("wpdim", c.level.dimension().location().toString());
        Tells.ring(c.level, at, 1.5, GOLD_RGB);
        c.fx(VfxShape.COMPASS_LOCK, at.add(0.0, 0.1, 0.0), at, GOLD, 40, 1.4f);
        c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 1.6f);
        c.say("Waypoint marked");
        return true;
    }

    // ================================================================ Portal Gate
    static boolean portal(HeroCtx c) {
        CarriageEntity car = driven(c);
        if (car == null) {
            c.say("Open the gate from the driver's seat");
            return false;
        }
        CompoundTag tag = c.bond.extra(SYSTEM);
        boolean marked = tag.contains("wpx", Tag.TAG_DOUBLE) && tag.contains("wpy", Tag.TAG_DOUBLE) && tag.contains("wpz", Tag.TAG_DOUBLE) && tag.contains("wpdim", Tag.TAG_STRING);
        Vec3 dest = marked ? new Vec3(tag.getDouble("wpx"), tag.getDouble("wpy"), tag.getDouble("wpz")) : null;
        boolean sameDimension = marked && tag.getString("wpdim").equals(c.level.dimension().location().toString());
        String problem = CarriageRules.portalProblem(marked, sameDimension, marked ? car.position().distanceTo(dest) : 0.0);
        if (problem != null) {
            c.say(problem);
            return false;
        }
        BlockPos destPos = BlockPos.containing(dest);
        c.level.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(destPos), 2, destPos);      // the land at the waypoint loads while the gates turn
        Vec3 here = car.position(), fwd = forward(car);
        Tells.ring(c.level, here, 2.4, BLUE_RGB);
        Tells.ring(c.level, dest, 2.4, BLUE_RGB);
        c.fx(VfxShape.SPACE_RINGGATE, here.add(fwd.scale(3.0)).add(0.0, 1.4, 0.0), here.add(0.0, 1.4, 0.0), BLUE, PORTAL_TELL + 10, 2.0f);
        VfxSpawn.send(c.level, VfxShape.SPACE_RINGGATE, dest.add(0.0, 1.4, 0.0), dest.add(fwd).add(0.0, 1.4, 0.0), BLUE, PORTAL_TELL + 10, 2.0f);
        c.sound(SoundEvents.ENDERMAN_TELEPORT, 0.8f, 0.6f);
        UUID id = car.getUUID();
        Timed.later(c, PORTAL_TELL, live -> cross(live, id, dest));
        return true;
    }

    /** The gate opens: the carriage and everybody aboard are carried to the waypoint, if everything is still as it was. */
    private static void cross(HeroCtx c, UUID carriageId, Vec3 dest) {
        CarriageEntity car = driven(c);
        if (car == null || !car.getUUID().equals(carriageId)) {
            c.say("The gate closed: you are no longer driving");
            return;
        }
        ServerLevel level = c.level;
        if (!level.hasChunkAt(BlockPos.containing(dest))) {
            c.say("The land at the waypoint is not ready");
            return;
        }
        Vec3 spot = roomFor(level, dest.x, dest.y, dest.z);
        if (spot == null) {
            c.say("There is no room for the carriage at the waypoint");
            return;
        }
        List<UUID> order = new ArrayList<>();                                      // the wielder first, so the driver's seat is theirs again
        order.add(c.player.getUUID());
        List<Entity> riders = new ArrayList<>(car.getPassengers());
        for (Entity r : riders) if (!order.contains(r.getUUID())) order.add(r.getUUID());
        for (Entity r : riders) r.stopRiding();
        car.moveTo(spot.x, spot.y, spot.z, car.getYRot(), 0.0f);
        Vec3 stand = spot.add(0.0, 0.6, 0.0);
        for (Entity r : riders) {
            if (r instanceof ServerPlayer sp) sp.teleportTo(level, stand.x, stand.y, stand.z, sp.getYRot(), sp.getXRot());
            else r.teleportTo(stand.x, stand.y, stand.z);
        }
        c.fx(VfxShape.SPACE_RINGGATE, spot.add(0.0, 1.4, 0.0), spot.add(forward(car)).add(0.0, 1.4, 0.0), BLUE, 30, 2.0f);
        c.sound(SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1.2f);
        Timed.later(c, 4, live -> board(live, carriageId, order));
    }

    /** A moment after the crossing everybody sits down again (the wielder in the driver's seat) if they are still beside the carriage. */
    private static void board(HeroCtx c, UUID carriageId, List<UUID> order) {
        CarriageEntity car = mine(c);
        if (car == null || !car.getUUID().equals(carriageId)) return;
        for (UUID id : order) {
            if (car.getPassengers().size() >= car.seats()) break;
            Entity e = c.level.getEntity(id);
            if (e == null || !e.isAlive() || e.isPassenger() || e.distanceToSqr(car) > 100.0) continue;
            seat(e, car);
        }
    }

    // ================================================================ Crash! Charge! (ultimate)
    static boolean crashCharge(HeroCtx c) {
        CarriageEntity car = driven(c);
        if (car == null) {
            c.say("Crash! Charge! is called from the driver's seat");
            return false;
        }
        Vec3 from = car.position().add(0.0, 0.5, 0.0);
        Tells.line(c.level, from, from.add(forward(car).scale(CHARGE_LANE)), GOLD_RGB);
        VfxSpawn.sendFollowing(c.level, VfxShape.GRAVITY_AURA, car, car.position(), GOLD, CHARGE_WARNING + 6, 1.8f);
        c.sound(SoundEvents.HORSE_ANGRY, 1.0f, 0.6f);
        UUID id = car.getUUID();
        Timed.later(c, CHARGE_WARNING, live -> {
            CarriageEntity now = driven(live);
            if (now == null || !now.getUUID().equals(id)) {
                live.say("The charge was called off");
                return;
            }
            now.startCharge(CHARGE_TICKS);
            Vec3 p = now.position().add(0.0, 0.8, 0.0);
            live.fx(VfxShape.WIND_GALE, p, p.add(forward(now).scale(10.0)), GOLD, 16, 1.8f);
            live.sound(SoundEvents.HORSE_GALLOP, 1.0f, 0.7f);
        });
        return true;
    }
}
