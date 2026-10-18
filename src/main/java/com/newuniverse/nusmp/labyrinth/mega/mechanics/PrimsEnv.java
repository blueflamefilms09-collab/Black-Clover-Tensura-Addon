package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the primitives that act on the surroundings and the senses rather than on one creature: the weather as the players of the area see it, lightning that hurts, shots from the
 * walls, a ring that runs outward, a blast with no block damage, a steady wind, a shaking room, Rukh particles, a sound, a message, a counter for the conditions of other effects, and
 * a reveal of the props. No block is changed here, and the weather is sent as packets to the players of the area only (the dimension's real weather is never touched, because
 * every party shares the one dimension). Twelve of the eighty primitives.
 */
final class PrimsEnv {
    private PrimsEnv() {}

    static void register(Map<String, Prim> m) {
        m.put("weather", Prims.timed(r -> r.ticks("secs"), r -> {
            Set<UUID> sent = new HashSet<>();
            r.mem.put("sent", sent);
            setWeather(r, r.str("mode"), sent);
        }, r -> {
            if (r.age() % 40 == 0) {
                @SuppressWarnings("unchecked")
                Set<UUID> sent = (Set<UUID>) r.mem.get("sent");
                setWeather(r, r.str("mode"), sent);                       // players who walked in late
            }
        }, r -> {
            @SuppressWarnings("unchecked")
            Set<UUID> sent = (Set<UUID>) r.mem.get("sent");
            if (sent == null) return;
            for (UUID id : sent) {
                if (r.level().getPlayerByUUID(id) instanceof ServerPlayer p && p.connection != null) {
                    boolean rain = r.level().isRaining(), thunder = r.level().isThundering();
                    p.connection.send(new ClientboundGameEventPacket(rain ? ClientboundGameEventPacket.START_RAINING : ClientboundGameEventPacket.STOP_RAINING, 0f));
                    p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, rain ? 1f : 0f));
                    p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, thunder ? 1f : 0f));
                }
            }
        }));
        m.put("lightning", Prims.once(r -> {
            List<ServerPlayer> ps = r.players();
            if (ps.isEmpty()) return;
            int spread = (int) r.num("spread");
            for (int i = 0; i < (int) r.num("count"); i++) {
                ServerPlayer p = ps.get(r.inst.rng.nextInt(ps.size()));
                double x = p.getX() + r.inst.rng.nextInt(spread * 2 + 1) - spread, z = p.getZ() + r.inst.rng.nextInt(spread * 2 + 1) - spread;
                BlockPos at = BlockPos.containing(x, p.getY(), z);
                if (!Safety.allowedAt(r.level(), at)) continue;
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(r.level());
                if (bolt != null) {
                    bolt.moveTo(x, p.getY(), z);
                    bolt.setVisualOnly(true);
                    r.level().addFreshEntity(bolt);
                }
                for (ServerPlayer q : r.players()) {
                    if (q.distanceToSqr(x, p.getY(), z) <= 9.0) {
                        PrimUtil.hurt(r.level(), q, r.hard("dmg"), "lightning");
                        q.igniteForSeconds(2.0f);
                    }
                }
            }
        }));
        m.put("projectile", Prims.once(r -> {
            List<ServerPlayer> ps = r.players();
            if (ps.isEmpty()) return;
            EntityType<?> type = switch (r.str("kind")) {
                case "snowball" -> EntityType.SNOWBALL;
                case "small_fireball" -> EntityType.SMALL_FIREBALL;
                case "spectral_arrow" -> EntityType.SPECTRAL_ARROW;
                case "egg" -> EntityType.EGG;
                default -> EntityType.ARROW;
            };
            double speed = r.num("speed");
            for (int i = 0; i < (int) r.num("count"); i++) {
                ServerPlayer p = ps.get(r.inst.rng.nextInt(ps.size()));
                double a = r.inst.rng.nextDouble() * Math.PI * 2;
                Vec3 from = new Vec3(p.getX() + Math.cos(a) * 9, p.getY() + 1.3, p.getZ() + Math.sin(a) * 9);
                Entity e = type.create(r.level());
                if (!(e instanceof Projectile pr)) { if (e != null) e.discard(); continue; }
                pr.setPos(from);
                Vec3 to = p.getEyePosition().subtract(from);
                pr.shoot(to.x, to.y + 0.05, to.z, (float) speed, 6.0f);
                if (pr instanceof AbstractArrow ar) {
                    ar.setBaseDamage(Math.max(0.5, r.hard("dmg") / Math.max(0.3, speed)));
                    ar.pickup = AbstractArrow.Pickup.DISALLOWED;
                }
                r.level().addFreshEntity(pr);
            }
            r.level().playSound(null, r.inst.anchor, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0f, 0.8f);
        }));
        m.put("shockwave", Prims.timed(r -> r.ticks("secs"), r -> r.mem.put("hit", new HashSet<UUID>()), r -> {
            @SuppressWarnings("unchecked")
            Set<UUID> hit = (Set<UUID>) r.mem.get("hit");
            double radius = r.num("radius") * Math.min(1.0, (r.age() + 1.0) / Math.max(1, r.duration));
            if (r.age() % 3 == 0) {
                for (int i = 0; i < 6; i++) {
                    double ang = i * Math.PI / 3.0 + r.age() * 0.2;
                    Vec3 c = Vec3.atCenterOf(r.inst.anchor).add(Math.cos(ang) * radius, 0.3, Math.sin(ang) * radius);
                    RukhFx.burst(r.level(), c, Rukh.align(r.inst, "auto"), 1);
                }
            }
            for (LivingEntity t : r.targets("players")) {
                double d = Math.hypot(t.getX() - (r.inst.anchor.getX() + 0.5), t.getZ() - (r.inst.anchor.getZ() + 0.5));
                if (Math.abs(d - radius) > 1.3 || !hit.add(t.getUUID())) continue;
                PrimUtil.hurt(r.level(), t, r.hard("dmg"), "magic");
                Vec3 dir = PrimUtil.flat(t, r.inst.anchor.getX() + 0.5, r.inst.anchor.getZ() + 0.5, r.inst.rng);
                PrimUtil.push(t, dir.x * r.num("power"), 0.35, dir.z * r.num("power"));
            }
        }, r -> { }));
        m.put("blast", Prims.once(r -> {
            double rad = r.num("radius");
            for (LivingEntity t : r.targets("players")) {
                if (t.distanceToSqr(r.inst.anchor.getX() + 0.5, r.inst.anchor.getY() + 0.5, r.inst.anchor.getZ() + 0.5) > rad * rad) continue;
                PrimUtil.hurt(r.level(), t, r.hard("dmg"), "magic");
                Vec3 dir = PrimUtil.flat(t, r.inst.anchor.getX() + 0.5, r.inst.anchor.getZ() + 0.5, r.inst.rng);
                PrimUtil.push(t, dir.x * r.num("power"), 0.4, dir.z * r.num("power"));
            }
            Rukh.burst(r.inst, "auto", "burst", 12);
            r.level().playSound(null, r.inst.anchor, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.8f, 1.1f);
        }));
        m.put("wind", Prims.timed(r -> r.ticks("secs"), r -> { }, r -> {
            double p = r.num("power");
            String dir = r.str("dir");
            for (LivingEntity t : r.targets("players")) {
                Vec3 d = switch (dir) {
                    case "south" -> new Vec3(0, 0, 1);
                    case "east" -> new Vec3(1, 0, 0);
                    case "west" -> new Vec3(-1, 0, 0);
                    case "up" -> new Vec3(0, 1.4, 0);
                    case "spiral" -> {
                        Vec3 off = new Vec3(t.getX() - (r.inst.anchor.getX() + 0.5), 0, t.getZ() - (r.inst.anchor.getZ() + 0.5));
                        yield new Vec3(-off.z, 0.4, off.x).normalize();
                    }
                    default -> new Vec3(0, 0, -1);
                };
                PrimUtil.push(t, d.x * p, d.y * p, d.z * p);
                if (dir.equals("up")) t.fallDistance = 0;
            }
            if (r.age() % 10 == 0) Rukh.at(r.inst, r.inst.anchor, "light", "column", 6);
        }, r -> { }));
        m.put("quake", Prims.timed(r -> r.ticks("secs"), r -> { }, r -> {
            if (r.age() % 4 == 0) {
                for (LivingEntity t : r.targets("players")) PrimUtil.push(t, (r.inst.rng.nextDouble() - 0.5) * 0.25, 0.04, (r.inst.rng.nextDouble() - 0.5) * 0.25);
            }
            if (r.age() % 10 == 0) {
                r.level().playSound(null, r.inst.anchor, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.4f, 0.5f);
                if (r.num("dmg") > 0) for (LivingEntity t : r.targets("players")) PrimUtil.hurt(r.level(), t, r.hard("dmg"), "generic");
            }
        }, r -> { }));
        m.put("rukh", Prims.once(r -> Rukh.burst(r.inst, r.str("align"), r.str("shape"), (int) r.num("count"))));
        m.put("sound", Prims.once(r -> {
            SoundEvent ev = BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.tryParse(r.str("id")) == null ? ResourceLocation.parse("minecraft:block.note_block.bell") : ResourceLocation.parse(r.str("id")))
                    .orElse(SoundEvents.NOTE_BLOCK_BELL.value());
            r.level().playSound(null, r.inst.anchor, ev, SoundSource.BLOCKS, (float) r.num("vol"), (float) r.num("pitch"));
        }));
        m.put("title", Prims.once(r -> Messages.say(r.inst, r.str("text"), r.str("mode"))));
        m.put("counter_add", Prims.once(r -> r.inst.counters.merge(r.str("name"), (int) Math.round(r.num("n")), Integer::sum)));
        m.put("reveal", Prims.timed(r -> r.ticks("secs"), r -> { }, r -> {
            if (r.age() % 10 != 0) return;
            int n = 0;
            for (Prop p : r.inst.props) {
                Rukh.at(r.inst, p.pos(), "light", "column", 4);
                if (++n >= 6) break;
            }
        }, r -> { }));
    }

    private static void setWeather(Run r, String mode, Set<UUID> sent) {
        for (ServerPlayer p : r.players()) {
            if (p.connection == null || !sent.add(p.getUUID())) continue;
            boolean rain = !mode.equals("clear"), thunder = mode.equals("thunder");
            p.connection.send(new ClientboundGameEventPacket(rain ? ClientboundGameEventPacket.START_RAINING : ClientboundGameEventPacket.STOP_RAINING, 0f));
            p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, rain ? 1f : 0f));
            p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, thunder ? 1f : 0f));
        }
    }
}
