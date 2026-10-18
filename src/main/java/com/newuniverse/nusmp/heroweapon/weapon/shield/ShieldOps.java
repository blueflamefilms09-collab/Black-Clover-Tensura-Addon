package com.newuniverse.nusmp.heroweapon.weapon.shield;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.heroweapon.curse.HeroCurse;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.newuniverse.nusmp.heroweapon.weapon.shield.ShieldNumbers.*;

/**
 * Why: this is what each technique of the Legendary Shield DOES, one static method per technique, kept apart from the list of skills (which only names them and
 * gives their texts) so the part that touches the world is in one place that can be read top to bottom. The rules every body keeps: damage goes only through
 * {@link HeroCtx#hit} (after shortening the previous blow's frames of grace, so a technique that lands right after a melee blow is not swallowed by them); a heal
 * goes only through {@link HeroCtx#heal}; anything that holds a foe in place asks {@link HeroCtx#control()} first and takes its length from the balance law; no
 * block is placed or broken; nothing runs longer than 20 seconds; a technique that has a wind-up shows its tell at once and acts after it, and the delayed part is
 * skipped if the wielder died, left or put the weapon away in between; the numbers are {@link ShieldNumbers}'s.
 */
final class ShieldOps {
    private ShieldOps() {}

    // ================================================================ shared helpers
    /** A hit through the balance law, after the previous blow's frames of grace are cut to the point where a new blow lands in full. */
    static boolean strike(HeroCtx c, LivingEntity t, double raw) {
        if (!(raw > 0)) return false;
        if (t.invulnerableTime > 10) t.invulnerableTime = 10;
        return c.hit(t, (float) raw);
    }

    /** The same, limited by a per-target cap so overlapping pulses cannot pass the technique's budget. */
    static boolean strike(HeroCtx c, LivingEntity t, double raw, HitCap cap) {
        double allowed = cap.take(t.getUUID(), raw);
        return allowed > 0 && strike(c, t, allowed);
    }

    /** The horizontal part of a direction, normalised; south when there is none. */
    static Vec3 flat(Vec3 v) {
        Vec3 f = new Vec3(v.x, 0, v.z);
        return f.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : f.normalize();
    }

    private static String name(LivingEntity e) { return e.getName().getString(); }

    private static double bondBoost(HeroCtx c) { return 1.0 + (c.power - 1.0) * 0.5; }

    private static String seconds(ServerPlayer p) { return Integer.toString((BalanceLaw.controlCooldownLeft(p) + 19) / 20); }

    /** True for a projectile that is not the wielder's or a friend's. */
    private static boolean hostileProjectile(ServerPlayer w, Projectile p) {
        if (p.isRemoved()) return false;
        Entity owner = p.getOwner();
        return !(owner instanceof LivingEntity le && ShieldTargets.isFriend(w, le));
    }

    /**
     * Moves the wielder up to {@code max} blocks along {@code dir} (horizontal) to the farthest free spot with a clear path, and returns whether it moved. A move
     * that cannot be made (no room, no connection) is simply not made.
     */
    static boolean lunge(HeroCtx c, Vec3 dir, double max) {
        ServerPlayer p = c.player;
        Vec3 from = p.position(), lift = new Vec3(0, p.getBbHeight() * 0.6, 0);
        for (double d = max; d >= 1.0; d -= 0.5) {
            Vec3 dest = from.add(dir.scale(d));
            if (!c.level.noCollision(p, p.getBoundingBox().move(dest.subtract(from)))) continue;
            HitResult wall = c.level.clip(new ClipContext(from.add(lift), dest.add(lift), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            if (wall.getType() != HitResult.Type.MISS) continue;
            try {
                p.teleportTo(dest.x, dest.y, dest.z);
                p.fallDistance = 0;
                return true;
            } catch (RuntimeException noConnection) {
                return false;
            }
        }
        return false;
    }

    // ================================================================ the ordinary tree
    /** Change Shield (Attack): a cleaving blow in front whose strength is the stored Counter. */
    static boolean changeAttack(HeroCtx c) {
        Vec3 dir = c.look();
        if (ShieldTargets.foesInCone(c, dir, CHANGE_RANGE + 0.8, CHANGE_HALF_ANGLE).isEmpty()) {
            c.say("No foe in reach");
            return false;
        }
        ShieldFx.tellCone(c.level, c.player.position().add(0, 0.2, 0), dir, CHANGE_RANGE, CHANGE_HALF_ANGLE, ShieldFx.TELL_JEWEL);
        ShieldFx.sound(c.level, c.player.position(), SoundEvents.IRON_GOLEM_REPAIR, 0.8f, 1.5f);
        ShieldRuntime.after(c, CHANGE_DELAY, () -> {
            ShieldState st = ShieldRuntime.state(c.player);
            List<LivingEntity> hit = ShieldTargets.foesInCone(c, dir, CHANGE_RANGE, CHANGE_HALF_ANGLE);
            if (hit.isEmpty()) return;
            double charge = st.guard.counter();
            double raw = CounterMath.scaled(CHANGE_BASE, CHANGE_SPAN, charge);
            st.guard.spendCounter(ShieldNumbers.COUNTER_MAX);
            for (LivingEntity t : hit) {
                strike(c, t, raw);
                c.shove(t, c.player.position(), 0.7 + charge / 200.0);
            }
            Vec3 at = hit.get(0).position();
            ShieldFx.shatter(c.level, at.add(0, 1, 0), c.player.position(), (float) (0.7 + charge / 150.0), ShieldFx.STEEL);
            ShieldFx.arc(c.level, c.eye(), c.eye().add(dir.scale(CHANGE_RANGE)), ShieldFx.SILVER, 1.2f);
            ShieldFx.sound(c.level, at, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f);
        });
        return true;
    }

    /** Air Strike Shield: a floating shield stands where you look and stops what is shot at it. */
    static boolean airStrike(HeroCtx c) {
        ShieldState st = ShieldRuntime.state(c.player);
        Vec3 eye = c.eye(), at = c.aim(PANEL_RANGE);
        Vec3 toMe = new Vec3(eye.x - at.x, 0, eye.z - at.z);
        if (toMe.lengthSqr() < 4.0) {                                            // looking at something right in front: stand it three blocks ahead
            Vec3 ahead = flat(c.look());
            at = c.player.position().add(ahead.scale(3.0));
            toMe = ahead.scale(-1.0);
        }
        Vec3 n = toMe.normalize();
        double feet = c.player.getY();
        double cy = Math.max(feet - 0.5, Math.min(feet + 4.0, at.y + PANEL_HALF_HEIGHT * 0.75));
        Vec3 centre = new Vec3(at.x + n.x * 0.8, cy, at.z + n.z * 0.8);
        long now = c.level.getGameTime();
        ShieldState.Floating f = new ShieldState.Floating(PanelMath.Panel.facing(centre.x, centre.y, centre.z, n.x, n.z, PANEL_HALF_WIDTH, PANEL_HALF_HEIGHT, PANEL_THICKNESS),
                PANEL_HP, now + PANEL_TICKS);
        for (ShieldState.Floating old : st.addFloating(f, now))
            ShieldFx.shatter(c.level, new Vec3(old.geo.cx(), old.geo.cy(), old.geo.cz()), centre, 0.7f, ShieldFx.STEEL);
        ShieldFx.pane(c.level, centre, centre.add(n), PANEL_TICKS);
        ShieldFx.sound(c.level, centre, SoundEvents.IRON_TRAPDOOR_CLOSE, 1.0f, 1.2f);
        ShieldRuntime.every(c, PANEL_TICKS, 1, age -> panelStep(c, st, f, age));
        return true;
    }

    private static void panelStep(HeroCtx c, ShieldState st, ShieldState.Floating f, int age) {
        if (f.dead) return;
        if (c.level.getGameTime() >= f.until) {
            endPanel(c, st, f);
            return;
        }
        PanelMath.Panel g = f.geo;
        Vec3 centre = new Vec3(g.cx(), g.cy(), g.cz());
        double reach = g.halfWidth() + 3.5;
        AABB box = new AABB(centre, centre).inflate(reach, g.halfHeight() + 3.5, reach);
        for (Projectile p : c.level.getEntitiesOfClass(Projectile.class, box, pr -> hostileProjectile(c.player, pr))) {
            Vec3 pos = p.position(), prev = pos.subtract(p.getDeltaMovement());
            if (!PanelMath.crosses(g, prev.x, prev.y, prev.z, pos.x, pos.y, pos.z)) continue;
            p.discard();
            f.hp--;
            ShieldFx.shatter(c.level, pos, centre, 0.5f, ShieldFx.JEWEL);
            ShieldFx.sound(c.level, pos, SoundEvents.SHIELD_BLOCK, 0.8f, 1.3f);
            if (f.hp <= 0) {
                endPanel(c, st, f);
                return;
            }
        }
        if (age % 4 != 0) return;
        for (LivingEntity e : ShieldTargets.foesAround(c, centre, reach)) {
            Vec3 mid = e.getBoundingBox().getCenter();
            if (!g.contains(mid.x, mid.y, mid.z)) continue;
            double side = g.depth(mid.x, mid.z) >= 0 ? 1.0 : -1.0;
            e.setDeltaMovement(e.getDeltaMovement().add(g.nx() * side * PANEL_PUSH, 0.05, g.nz() * side * PANEL_PUSH));
            e.hurtMarked = true;
        }
    }

    private static void endPanel(HeroCtx c, ShieldState st, ShieldState.Floating f) {
        f.dead = true;
        st.removeFloating(f);
        Vec3 centre = new Vec3(f.geo.cx(), f.geo.cy(), f.geo.cz());
        ShieldFx.shatter(c.level, centre, centre.add(0, 1, 0), 0.9f, ShieldFx.STEEL);
        ShieldFx.sound(c.level, centre, SoundEvents.SHIELD_BREAK, 0.8f, 1.2f);
    }

    /** Second Shield: a ward on the friend you look at (the nearest friend, else you) that soaks the next blows. */
    static boolean secondShield(HeroCtx c) {
        LivingEntity target = ShieldTargets.aimFriend(c, WARD_RANGE);
        if (target == null) {
            List<LivingEntity> near = ShieldTargets.friendsAround(c.player, c.player.position(), 12.0, false);
            target = near.isEmpty() ? c.player : near.get(0);
        }
        double bank = Math.min(WARD_BANK_CAP, target.getMaxHealth() * WARD_BANK_SHARE * bondBoost(c));
        ShieldRuntime.book().ward(c.player.getUUID(), target.getUUID(), c.level.getGameTime(), WARD_TICKS, bank);
        ShieldFx.orbit(c.level, target, WARD_TICKS, ShieldFx.JEWEL);
        ShieldFx.sound(c.level, target.position(), SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.6f);
        c.say(target == c.player ? "Second Shield on you" : "Second Shield on " + name(target));
        return true;
    }

    /** Protect: for eight seconds part of every blow your friends take is yours. */
    static boolean cover(HeroCtx c) {
        List<LivingEntity> friends = ShieldTargets.friendsAround(c.player, c.player.position(), COVER_RADIUS, false);
        if (friends.isEmpty()) {
            c.say("No one to protect");
            return false;
        }
        List<UUID> ids = new ArrayList<>();
        for (LivingEntity f : friends) ids.add(f.getUUID());
        int bound = ShieldRuntime.book().cover(c.player.getUUID(), ids, c.level.getGameTime(), COVER_TICKS, COVER_SHARE);
        ShieldFx.seal(c.level, c.player.position(), COVER_RADIUS * 0.6, COVER_TICKS, ShieldFx.JEWEL);
        int shown = 0;
        for (LivingEntity f : friends) {
            if (shown++ >= COVER_MAX_ALLIES) break;
            ShieldFx.tellLine(c.level, c.player.getBoundingBox().getCenter(), f.getBoundingBox().getCenter(), ShieldFx.TELL_JEWEL);
        }
        ShieldFx.sound(c.level, c.player.position(), SoundEvents.BEACON_ACTIVATE, 0.9f, 1.4f);
        c.say("Protecting " + bound + (bound == 1 ? " friend" : " friends"));
        return true;
    }

    /** Provoke: hostile mobs near you (never a boss, whose own script decides whom it fights) turn on you, and you stand a little harder for the length of it. */
    static boolean provoke(HeroCtx c) {
        AABB box = c.player.getBoundingBox().inflate(PROVOKE_RADIUS);
        List<Mob> mobs = new ArrayList<>();
        for (Mob m : c.level.getEntitiesOfClass(Mob.class, box, m -> m.distanceToSqr(c.player) <= PROVOKE_RADIUS * PROVOKE_RADIUS && ShieldTargets.isFoe(c.player, m) && !BalanceLaw.isBoss(m))) {
            mobs.add(m);
            if (mobs.size() >= MAX_TARGETS) break;
        }
        if (mobs.isEmpty()) {
            c.say("Nothing to provoke");
            return false;
        }
        c.player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, PROVOKE_TICKS, 0, true, true));
        ShieldFx.tellRing(c.level, c.player.position(), 3.0, ShieldFx.TELL_EMBER);
        ShieldFx.brand(c.level, c.player.position().add(0, 1, 0), c.player.position().add(0, 2, 0), 0.8f);
        ShieldFx.sound(c.level, c.player.position(), SoundEvents.RAVAGER_ROAR, 0.9f, 1.3f);
        ShieldRuntime.every(c, PROVOKE_TICKS, 10, age -> {
            for (Mob m : mobs) if (m.isAlive() && !m.isRemoved() && m.getTarget() != c.player) m.setTarget(c.player);
        });
        return true;
    }

    /** Herb Mend: the Leaf Shield's herbs mend you and your friends around you. */
    static boolean herbMend(HeroCtx c) {
        ShieldFx.herbs(c.level, c.player.position(), HERB_RADIUS);
        ShieldFx.tellRing(c.level, c.player.position(), HERB_RADIUS, 0x7CFF9C);
        ShieldFx.sound(c.level, c.player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.9f);
        ShieldRuntime.after(c, HERB_DELAY, () -> {
            double boost = bondBoost(c);
            for (LivingEntity f : ShieldTargets.friendsAround(c.player, c.player.position(), HERB_RADIUS, true)) {
                double share = Math.min(0.25, HERB_SHARE * boost);
                c.heal(f, (float) (f.getMaxHealth() * share));
                f.addEffect(new MobEffectInstance(MobEffects.REGENERATION, HERB_REGEN_TICKS, 0, true, true));
            }
        });
        return true;
    }

    /** Shield Prison: a cage of shields round the foe you look at. */
    static boolean prison(HeroCtx c) {
        LivingEntity t = ShieldTargets.aimFoe(c, PRISON_RANGE);
        if (t == null) {
            c.say("No foe in sight");
            return false;
        }
        if (!c.control()) {
            c.say("Control is not ready for " + seconds(c.player) + " s");
            return false;
        }
        int ticks = BalanceLaw.controlTicks(t, PRISON_TICKS);
        Vec3 centre = t.position();
        long now = c.level.getGameTime();
        ShieldState st = ShieldRuntime.state(c.player);
        st.cage(t.getUUID(), now + ticks + 20L, now);
        ShieldFx.cage(c.level, centre, PRISON_RADIUS, ticks);
        ShieldFx.sound(c.level, centre, SoundEvents.CHAIN_PLACE, 1.2f, 0.8f);
        ShieldFx.sound(c.level, centre, SoundEvents.IRON_DOOR_CLOSE, 1.0f, 0.9f);
        ShieldRuntime.every(c, ticks, 2, age -> {
            if (!t.isAlive() || t.isRemoved()) return;
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 1, true, false));
            Vec3 off = new Vec3(centre.x - t.getX(), 0, centre.z - t.getZ());
            double d = off.length();
            if (d > PRISON_RADIUS - 0.35 && d > 1.0E-3) {
                Vec3 pull = off.scale(1.0 / d).scale(PRISON_PULL);
                t.setDeltaMovement(pull.x, Math.min(t.getDeltaMovement().y, 0.1), pull.z);
                t.hurtMarked = true;
            }
        });
        return true;
    }

    /** Chain Shield: chains bind up to four foes together, draw them towards each other and bite. */
    static boolean chain(HeroCtx c) {
        LivingEntity first = ShieldTargets.aimFoe(c, 16.0);
        Vec3 centre = first != null ? first.position() : c.aim(12.0);
        List<LivingEntity> victims = ShieldTargets.foesAround(c, centre, CHAIN_RADIUS);
        if (victims.size() > CHAIN_TARGETS) victims = new ArrayList<>(victims.subList(0, CHAIN_TARGETS));
        if (victims.isEmpty()) {
            c.say("No foe in sight");
            return false;
        }
        if (!c.control()) {
            c.say("Control is not ready for " + seconds(c.player) + " s");
            return false;
        }
        List<LivingEntity> bound = victims;
        HitCap cap = new HitCap(CHAIN.total());
        for (LivingEntity v : bound) ShieldFx.chains(c.level, c.eye().add(0, -0.4, 0), v.getBoundingBox().getCenter(), 12);
        ShieldFx.sound(c.level, c.player.position(), SoundEvents.CHAIN_HIT, 1.1f, 0.9f);
        ShieldRuntime.every(c, (CHAIN_PULSES - 1) * CHAIN_INTERVAL, CHAIN_INTERVAL, age -> {
            Vec3 sum = Vec3.ZERO;
            int alive = 0;
            for (LivingEntity v : bound) {
                if (!v.isAlive() || v.isRemoved()) continue;
                sum = sum.add(v.position());
                alive++;
            }
            if (alive == 0) return;
            Vec3 mid = sum.scale(1.0 / alive);
            LivingEntity prev = null;
            for (LivingEntity v : bound) {
                if (!v.isAlive() || v.isRemoved()) continue;
                strike(c, v, CHAIN.raw(), cap);
                Vec3 to = new Vec3(mid.x - v.getX(), 0, mid.z - v.getZ());
                if (to.lengthSqr() > 0.25) {
                    Vec3 pull = to.normalize().scale(CHAIN_PULL);
                    v.setDeltaMovement(v.getDeltaMovement().add(pull.x, 0.05, pull.z));
                    v.hurtMarked = true;
                }
                if (prev != null) ShieldFx.tellLine(c.level, prev.getBoundingBox().getCenter(), v.getBoundingBox().getCenter(), 0xC8D8E8);
                prev = v;
            }
            ShieldFx.sound(c.level, mid, SoundEvents.CHAIN_HIT, 0.7f, 1.0f);
        });
        return true;
    }

    /** Hook: a chain on the foe you look at, which is dragged to you. */
    static boolean hook(HeroCtx c) {
        LivingEntity t = ShieldTargets.aimFoe(c, HOOK_RANGE);
        if (t == null) {
            c.say("No foe in sight");
            return false;
        }
        if (!c.control()) {
            c.say("Control is not ready for " + seconds(c.player) + " s");
            return false;
        }
        ShieldFx.chains(c.level, c.eye().add(0, -0.4, 0), t.getBoundingBox().getCenter(), 10);
        ShieldFx.sound(c.level, c.player.position(), SoundEvents.CHAIN_HIT, 1.2f, 1.1f);
        strike(c, t, HOOK.raw());
        Vec3 to = c.player.position().subtract(t.position());
        double dist = to.length();
        if (dist > HOOK_STOP + 0.5) {
            double speed = Math.min(1.6, 0.35 + dist * 0.12) * (BalanceLaw.isBoss(t) ? 0.25 : 1.0);     // a boss is only staggered, never dragged
            Vec3 v = to.scale(1.0 / dist).scale(speed);
            t.setDeltaMovement(v.x, Math.max(0.25, v.y + 0.2), v.z);
            t.hurtMarked = true;
        }
        return true;
    }

    /** Iron Maiden: the iron maiden closes on a foe held in a Shield Prison; the Counter is spent into it. */
    static boolean ironMaiden(HeroCtx c) {
        ShieldState st = ShieldRuntime.state(c.player);
        long now = c.level.getGameTime();
        LivingEntity t = ShieldTargets.aimFoe(c, IRON_RANGE);
        if (t == null || !st.caged(t.getUUID(), now)) {
            t = null;
            for (LivingEntity e : ShieldTargets.foesAround(c, c.player.position(), IRON_RANGE)) {
                if (st.caged(e.getUUID(), now)) {
                    t = e;
                    break;
                }
            }
        }
        if (t == null) {
            c.say("The Iron Maiden needs a foe in a Shield Prison");
            return false;
        }
        LivingEntity victim = t;
        Vec3 at = victim.position();
        ShieldFx.tellRing(c.level, at, 1.8, ShieldFx.TELL_STEEL);
        ShieldFx.tellColumn(c.level, at, 2.4, ShieldFx.TELL_STEEL);
        ShieldFx.sound(c.level, at, SoundEvents.IRON_DOOR_CLOSE, 1.2f, 0.6f);
        ShieldRuntime.after(c, IRON_DELAY, () -> {
            if (!victim.isAlive() || victim.isRemoved()) return;
            double charge = st.guard.counter();
            double raw = CounterMath.scaled(IRON_BASE, IRON_SPAN, charge);
            st.guard.spendCounter(ShieldNumbers.COUNTER_MAX);
            strike(c, victim, raw);
            st.release(victim.getUUID());
            ShieldFx.slam(c.level, victim.position(), victim.position().add(0, 1, 0), 1.0f);
            ShieldFx.manacle(c.level, victim.position().add(0, 1, 0), victim.position().add(0, 2, 0), 1.0f);
            ShieldFx.sound(c.level, victim.position(), SoundEvents.ANVIL_LAND, 1.3f, 0.6f);
        });
        return true;
    }

    /** Needle Burst: the needles of the Needle Shield all at once, as hard as the Counter you have stored, after a short gathering that shows their reach. */
    static boolean needleBurst(HeroCtx c) {
        if (ShieldTargets.foesAround(c, c.player.position(), NEEDLE_BURST_RADIUS).isEmpty()) {
            c.say("No foe in reach");
            return false;
        }
        Vec3 o = c.player.position().add(0, 1.0, 0);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0;
            ShieldFx.tellLine(c.level, o, o.add(Math.cos(a) * NEEDLE_BURST_RADIUS, 0, Math.sin(a) * NEEDLE_BURST_RADIUS), ShieldFx.TELL_STEEL);
        }
        ShieldFx.sound(c.level, c.player.position(), SoundEvents.IRON_GOLEM_REPAIR, 0.8f, 1.8f);
        ShieldRuntime.after(c, NEEDLE_BURST_DELAY, () -> {
            List<LivingEntity> foes = ShieldTargets.foesAround(c, c.player.position(), NEEDLE_BURST_RADIUS);
            if (foes.isEmpty()) return;                                            // they stepped out: nothing is spent
            ShieldState st = ShieldRuntime.state(c.player);
            double charge = st.guard.counter();
            double raw = CounterMath.scaled(NEEDLE_BURST_BASE, NEEDLE_BURST_SPAN, charge);
            st.guard.spendCounter(charge * NEEDLE_BURST_SPEND);
            Vec3 at = c.player.position().add(0, 1.0, 0);
            for (LivingEntity t : foes) strike(c, t, raw);
            ShieldFx.shatter(c.level, at, at.add(0, 1, 0), 1.0f, ShieldFx.SILVER);
            ShieldFx.sound(c.level, at, SoundEvents.PLAYER_ATTACK_SWEEP, 1.1f, 1.5f);
        });
        return true;
    }

    /** Shooting Star Shield: a comet of glass flies from the shield. */
    static boolean shootingStar(HeroCtx c) {
        ShieldFx.tellGather(c.level, c.eye().add(c.look().scale(0.9)), 0.9, ShieldFx.TELL_JEWEL, 10);             // light gathers at the shield for a moment before the comet leaves
        ShieldFx.sound(c.level, c.player.position(), SoundEvents.BEACON_POWER_SELECT, 0.9f, 1.8f);
        ShieldRuntime.after(c, STAR_DELAY, () -> {
            Vec3 dir = c.look(), start = c.eye().add(dir.scale(0.8));
            ShieldFx.lance(c.level, start, start.add(dir.scale(STAR_SPEED * STAR_LIFE)), STAR_LIFE);
            SpellRuntime.bolt(c.player, start, dir.scale(STAR_SPEED), STAR_RADIUS, STAR_LIFE, false, null,
                    (bolt, target) -> {
                        if (!ShieldTargets.isFoe(c.player, target)) return;
                        strike(c, target, SHOOTING_STAR.raw());
                        c.shove(target, c.player.position(), STAR_PUSH);
                    },
                    (bolt, at) -> ShieldFx.shatter(c.level, at, at.add(0, 1, 0), 0.8f, ShieldFx.JEWEL));
        });
        return true;
    }

    /** Soul Aegis: the souls the Soul Eater Shield has eaten become Absorption for you and your friends. */
    static boolean soulAegis(HeroCtx c) {
        ShieldState st = ShieldRuntime.state(c.player);
        long now = c.level.getGameTime();
        int souls = st.souls.count(now);
        if (souls <= 0) {
            c.say("No souls gathered");
            return false;
        }
        st.souls.spendAll(now);
        for (LivingEntity f : ShieldTargets.friendsAround(c.player, c.player.position(), AEGIS_RADIUS, true)) {
            int level = f == c.player ? souls - 1 : Math.max(0, (souls + 1) / 2 - 1);
            f.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, AEGIS_TICKS, level, true, true));
        }
        ShieldFx.souls(c.level, c.player.position(), AEGIS_RADIUS);
        ShieldFx.sound(c.level, c.player.position(), SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.6f, 1.6f);
        c.say(souls + (souls == 1 ? " soul" : " souls") + " given to the shield");
        return true;
    }

    /** Undead Control: the undead near you lose their will to attack for a few seconds. */
    static boolean undeadControl(HeroCtx c) {
        AABB box = c.player.getBoundingBox().inflate(UNDEAD_RADIUS);
        List<Mob> undead = new ArrayList<>();
        for (Mob m : c.level.getEntitiesOfClass(Mob.class, box, m -> m.isAlive() && m.getType().is(EntityTypeTags.UNDEAD) && !BalanceLaw.isBoss(m)
                && !(m instanceof OwnableEntity o && o.getOwnerUUID() != null) && m.distanceToSqr(c.player) <= UNDEAD_RADIUS * UNDEAD_RADIUS)) {
            undead.add(m);
            if (undead.size() >= MAX_TARGETS) break;
        }
        if (undead.isEmpty()) {
            c.say("No undead near");
            return false;
        }
        if (!c.control()) {
            c.say("Control is not ready for " + seconds(c.player) + " s");
            return false;
        }
        ShieldFx.souls(c.level, c.player.position(), UNDEAD_RADIUS * 0.5);
        ShieldFx.sound(c.level, c.player.position(), SoundEvents.ELDER_GUARDIAN_CURSE, 0.7f, 1.4f);
        ShieldRuntime.every(c, UNDEAD_TICKS, 2, age -> {                          // a mob's own goals look for a target again every few ticks, so it is cleared often
            for (Mob m : undead) {
                if (!m.isAlive() || m.isRemoved()) continue;
                if (m.getLastHurtByMob() == c.player && m.tickCount - m.getLastHurtByMobTimestamp() < 40) continue;      // one that was struck may answer (the timestamp is in the mob's own ticks)
                m.setTarget(null);
            }
        });
        return true;
    }

    /**
     * Absolute Defence: for five seconds a dome of iron stands where the wielder cast it; friends inside take far less, foes are pushed out, shots are stopped. The dome
     * stays where its effect is drawn (a wielder who walks out of it loses its protection and keeps its slowness), so what the eye sees is what the rules apply to.
     */
    static boolean absoluteDefence(HeroCtx c) {
        Vec3 at = c.player.position();
        ShieldFx.guardian(c.level, at, DEFENCE_RADIUS, DEFENCE_TICKS);
        ShieldFx.circle(c.level, at, DEFENCE_RADIUS * 0.5, 30, ShieldFx.STEEL);
        ShieldFx.sound(c.level, at, SoundEvents.BEACON_ACTIVATE, 1.4f, 0.7f);
        ShieldFx.sound(c.level, at, SoundEvents.ANVIL_LAND, 1.0f, 0.5f);
        ShieldRuntime.every(c, DEFENCE_TICKS, 5, age -> {
            for (LivingEntity f : ShieldTargets.friendsAround(c.player, at, DEFENCE_RADIUS, true))
                f.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 12, f == c.player ? DEFENCE_SELF_LEVEL : DEFENCE_ALLY_LEVEL, true, false));
            c.player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, DEFENCE_SLOW_LEVEL, true, false));
            AABB box = new AABB(at, at).inflate(DEFENCE_RADIUS);
            for (Projectile p : c.level.getEntitiesOfClass(Projectile.class, box, pr -> hostileProjectile(c.player, pr) && pr.distanceToSqr(at) <= DEFENCE_RADIUS * DEFENCE_RADIUS)) {
                ShieldFx.shatter(c.level, p.position(), at.add(0, 1, 0), 0.5f, ShieldFx.STEEL);
                p.discard();
            }
            for (LivingEntity e : ShieldTargets.foesAround(c, at, DEFENCE_RADIUS)) {
                Vec3 away = new Vec3(e.getX() - at.x, 0, e.getZ() - at.z);
                if (away.lengthSqr() < 1.0E-4) continue;
                e.setDeltaMovement(e.getDeltaMovement().add(away.normalize().scale(0.35)));
                e.hurtMarked = true;
            }
        });
        return true;
    }

    // ================================================================ the Wrath series
    /** Rage Bash: the Shield of Rage lunges and slams; what it hits burns. */
    static boolean rageBash(HeroCtx c) {
        if (ShieldTargets.foesAround(c, c.player.position(), RAGE_BASH_LUNGE + RAGE_BASH_RADIUS + 1.0).isEmpty()) {
            c.say("No foe in reach");
            return false;
        }
        Vec3 dir = flat(c.look());
        ShieldFx.tellLine(c.level, c.player.position().add(0, 0.2, 0), c.player.position().add(dir.scale(RAGE_BASH_LUNGE)).add(0, 0.2, 0), ShieldFx.TELL_WRATH);
        ShieldFx.tellRing(c.level, c.player.position().add(dir.scale(RAGE_BASH_LUNGE)), RAGE_BASH_RADIUS, ShieldFx.TELL_EMBER);
        ShieldFx.sound(c.level, c.player.position(), SoundEvents.RAVAGER_ROAR, 0.8f, 1.6f);
        ShieldRuntime.after(c, RAGE_BASH_DELAY, () -> {
            lunge(c, dir, RAGE_BASH_LUNGE);
            Vec3 at = c.player.position().add(dir.scale(1.2));
            for (LivingEntity t : ShieldTargets.foesAround(c, at, RAGE_BASH_RADIUS)) {
                strike(c, t, RAGE_BASH.raw());
                t.igniteForSeconds(RAGE_BASH_FIRE_SECONDS);
                c.shove(t, c.player.position(), 0.9);
            }
            ShieldFx.slam(c.level, at, at.add(dir), 1.0f);
            ShieldFx.flame(c.level, at.add(0, 0.5, 0), 1.0f);
            ShieldFx.sound(c.level, at, SoundEvents.MACE_SMASH_GROUND, 1.2f, 0.8f);
        });
        return true;
    }

    /** Rage Roar: a roar that shoves foes back and slows them, and fills the wielder with strength. */
    static boolean rageRoar(HeroCtx c) {
        Vec3 dir = c.look();
        ShieldFx.tellCone(c.level, c.player.position().add(0, 0.2, 0), dir, ROAR_RANGE, ROAR_HALF_ANGLE, ShieldFx.TELL_WRATH);
        ShieldFx.sound(c.level, c.player.position(), SoundEvents.ENDER_DRAGON_GROWL, 1.2f, 0.7f);
        ShieldRuntime.after(c, ROAR_DELAY, () -> {
            List<LivingEntity> in = ShieldTargets.foesInCone(c, dir, ROAR_RANGE, ROAR_HALF_ANGLE);
            boolean hold = !in.isEmpty() && c.control();
            for (LivingEntity t : in) {
                strike(c, t, RAGE_ROAR.raw());
                c.shove(t, c.player.position(), 1.1);
                if (hold) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, ROAR_SLOW_TICKS), 1));
            }
            c.player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ROAR_FRENZY_TICKS, 0, false, true));
            ShieldFx.brand(c.level, c.player.position().add(dir.scale(2.0)).add(0, 1, 0), c.player.position().add(dir.scale(5.0)).add(0, 1, 0), 1.2f);
        });
        return true;
    }

    /**
     * Blood Sacrifice: a ring of blood is drawn under the foe the wielder looks at, and a second later a trap of blood closes on whatever stands in the ring. The ring
     * stays where it was drawn, so a foe that walks out of it is missed; the blood is paid only if the trap catches something.
     */
    static boolean bloodSacrifice(HeroCtx c) {
        LivingEntity t = ShieldTargets.aimFoe(c, 12.0);
        if (t == null) {
            c.say("No foe in sight");
            return false;
        }
        if (CounterMath.price(c.player.getHealth(), c.player.getMaxHealth(), BLOOD_HEALTH_SHARE, BLOOD_HEALTH_FLOOR) < 1.0) {
            c.say("Too weak to pay the blood");
            return false;
        }
        boolean cursed = c.form.curse();
        Vec3 at = t.position();
        ShieldFx.bloodSigil(c.level, at, BLOOD_RADIUS, BLOOD_DELAY + 10);
        ShieldFx.tellRing(c.level, at, BLOOD_RADIUS, ShieldFx.TELL_WRATH);
        ShieldFx.tellGather(c.level, c.player.position().add(0, 1, 0), 1.0, ShieldFx.TELL_WRATH, 12);
        ShieldFx.sound(c.level, at, SoundEvents.WARDEN_SONIC_CHARGE, 1.0f, 0.8f);
        ShieldRuntime.after(c, BLOOD_DELAY, () -> {
            List<LivingEntity> caught = ShieldTargets.foesAround(c, at, BLOOD_RADIUS);
            if (caught.isEmpty()) {
                c.say("The trap closes on nothing");
                return;
            }
            double pay = CounterMath.price(c.player.getHealth(), c.player.getMaxHealth(), BLOOD_HEALTH_SHARE, BLOOD_HEALTH_FLOOR);
            if (pay < 1.0) return;
            c.player.setHealth(c.player.getHealth() - (float) pay);
            boolean hold = c.control();
            for (LivingEntity v : caught) {
                strike(c, v, v == t ? BLOOD_SACRIFICE.raw() : BLOOD_SPLASH);
                if (hold) v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(v, BLOOD_ROOT_TICKS), 6));
            }
            c.player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, BLOOD_CURSE_TICKS, 0));
            c.player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BLOOD_CURSE_TICKS, 0));
            ShieldFx.bloodBurst(c.level, at.add(0, 1, 0), at.add(0, 2, 0), 1.2f);
            ShieldFx.bloodCage(c.level, at, BLOOD_RADIUS, 30);
            ShieldFx.sound(c.level, at, SoundEvents.GRINDSTONE_USE, 1.2f, 0.5f);
            if (cursed) HeroCurse.addCorruption(c.player, BLOOD_CORRUPTION);
        });
        return true;
    }

    /**
     * Megiddo Burst: the Shield of Wrath IV gives a great deal of itself and the ground burns in every direction. The burst is centred on the circle that is drawn
     * where the wielder stood at the cast, so the circle the eye sees is the circle the rules apply to.
     */
    static boolean megiddo(HeroCtx c) {
        if (CounterMath.price(c.player.getHealth(), c.player.getMaxHealth(), MEGIDDO_HEALTH_SHARE, MEGIDDO_HEALTH_FLOOR) < 2.0) {
            c.say("Too weak to pay the blood");
            return false;
        }
        boolean cursed = c.form.curse();
        Vec3 at = c.player.position();
        ShieldFx.circle(c.level, at, MEGIDDO_RADIUS * 0.6, MEGIDDO_DELAY, ShieldFx.WRATH);
        ShieldFx.tellRing(c.level, at, MEGIDDO_RADIUS, ShieldFx.TELL_WRATH);
        ShieldFx.tellRing(c.level, at, MEGIDDO_RADIUS * 0.5, ShieldFx.TELL_EMBER);
        ShieldFx.tellGather(c.level, at.add(0, 1, 0), MEGIDDO_RADIUS * 0.5, ShieldFx.TELL_WRATH, 24);
        ShieldFx.sound(c.level, at, SoundEvents.WARDEN_SONIC_CHARGE, 1.6f, 0.6f);
        ShieldRuntime.after(c, MEGIDDO_DELAY, () -> {
            double pay = CounterMath.price(c.player.getHealth(), c.player.getMaxHealth(), MEGIDDO_HEALTH_SHARE, MEGIDDO_HEALTH_FLOOR);
            if (pay < 2.0) return;
            c.player.setHealth(c.player.getHealth() - (float) pay);
            for (LivingEntity t : ShieldTargets.foesAround(c, at, MEGIDDO_RADIUS)) {
                strike(c, t, MEGIDDO.raw());
                t.igniteForSeconds(4);
                c.shove(t, at, 1.4);
            }
            c.player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, BLOOD_CURSE_TICKS, 1));
            c.player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BLOOD_CURSE_TICKS, 0));
            ShieldFx.wildfire(c.level, at, MEGIDDO_RADIUS, 60);
            ShieldFx.pillar(c.level, at, MEGIDDO_RADIUS * 0.4);
            ShieldFx.brand(c.level, at.add(0, 1, 0), at.add(0, 2, 0), 2.0f);
            ShieldFx.sound(c.level, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.6f, 0.6f);
            ShieldFx.sound(c.level, at, SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.4f, 0.7f);
            if (cursed) HeroCurse.addCorruption(c.player, MEGIDDO_CORRUPTION);
        });
        return true;
    }
}
