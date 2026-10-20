package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: wave 21: the act bodies of the twelve weapons (the server side of every use, sneak-use and key). The core has already checked attunement, paid
 * mana or existence points and set the cooldown and the daily use counter before a body runs. The numbers are in {@link Wave21Rules}; the things that
 * last (the escape circle, the whirlwind, the climb, the stop) are timers in persistent data or the zone list in {@link Wave21Events}.
 */
final class Wave21Acts {
    private Wave21Acts() {}

    static final String K_WHIRL = "nusmp_w21_whirl_until", K_CLIMB = "nusmp_w21_climb_until", K_STOP = "nusmp_w21_stop_until", K_HEADLESS = "nusmp_w21_headless_until",
            K_SMITE_USED = "nusmp_w21_smite_used", K_FORM = "w21_form";
    private static final int PINK = 0xFF5AC0, GOLD = 0xFFE27A, WIND = 0x9CCBFF, SUN = 0xFFF0AA, VIOLET = 0xB070FF, ORANGE = 0xFF9650, RED = 0xD2242C;

    // ================================================================ aiming
    record Aim(LivingEntity target, Vec3 end) {}

    /** The nearest creature along the look direction within {@code range} (blocks stop the line) among those {@code ok} accepts. */
    static Aim aim(ServerPlayer p, double range, Predicate<LivingEntity> ok) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Vec3 far = eye.add(look.scale(range));
        BlockHitResult blocks = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (blocks.getType() != HitResult.Type.MISS) far = blocks.getLocation();
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        Vec3 bestAt = far;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, far).inflate(1.0), x -> x != p && x.isAlive() && !x.isSpectator() && ok.test(x))) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(0.3).clip(eye, far);
            if (hit.isEmpty()) continue;
            double d = hit.get().distanceToSqr(eye);
            if (d < bestD) {
                bestD = d;
                best = e;
                bestAt = hit.get();
            }
        }
        return new Aim(best, best == null ? far : bestAt);
    }

    /** Foes (Wave 0's fair test) within r of a point, at most 24, nearest first. */
    static List<LivingEntity> foesNear(ServerPlayer p, Vec3 at, double r) {
        List<LivingEntity> found = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r), e -> Wave0Util.fair(p, e) && e.position().distanceToSqr(at) <= r * r);
        found.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(at)));
        return found.size() > 24 ? new ArrayList<>(found.subList(0, 24)) : found;
    }

    private static void beam(ServerLevel l, Vec3 from, Vec3 to, int rgb) {
        com.newuniverse.nusmp.particle.fx.Fx.stream(l, FxThemes.RELIC_WAVE_21, from, to, FxRelic.tone(rgb), 4);
    }

    private static boolean has(ServerPlayer p, String flag) { return RelicFlags.has(p, flag); }

    private static void say(ServerPlayer p, String t) { Wave0Util.note(p, t, net.minecraft.ChatFormatting.GOLD); }

    // ================================================================ Rift Breaker
    /** Rift Strike (four a day): a rift opens by you and by a creature up to 18 blocks away and the spear strikes it from any direction; sneaking sends the spear along the rift line. */
    static void riftStrike(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        if (ctx.sneaking()) {
            Vec3 eye = p.getEyePosition(), end = aim(p, Wave21Rules.RIFT_RANGE, x -> false).end();
            beam(l, eye.add(p.getLookAngle()), end, PINK);
            FxRelic.ring(l, end, 1.2, PINK);
            int n = 0;
            List<LivingEntity> line = new ArrayList<>();
            for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(2.0), x -> Wave0Util.fair(p, x)))
                if (Wave21Rules.distToSegment(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), eye.x, eye.y, eye.z, end.x, end.y, end.z) <= 1.3) line.add(e);
            line.sort(Comparator.comparingDouble(e -> e.distanceToSqr(eye)));
            for (LivingEntity e : line) {
                if (n++ >= 4) break;
                Wave0Util.extra(p, e, 8.0f, "minecraft:trident");
            }
            say(p, n == 0 ? "The spear flies the rift line and finds nothing." : "The spear cuts " + n + " creature(s) along the rift.");
            return;
        }
        Aim a = aim(p, Wave21Rules.RIFT_RANGE, x -> Wave0Util.fair(p, x));
        if (a.target() == null) {
            say(p, "No creature in sight within " + (int) Wave21Rules.RIFT_RANGE + " blocks.");
            return;
        }
        LivingEntity t = a.target();
        FxRelic.ring(l, p.position().add(0, 1.0, 0), 1.0, PINK);
        FxRelic.ring(l, t.position().add(0, t.getBbHeight() * 0.5, 0), 1.0, PINK);
        beam(l, p.getEyePosition(), t.position().add(0, t.getBbHeight() * 0.5, 0), PINK);
        Wave0Util.extra(p, t, 9.0f, "minecraft:trident");
    }

    /** Dimension Door (once a day): a blink along the look direction (the Tensura skill Spatial Motion is lent for a moment if it exists). */
    static void dimensionDoor(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        FxRelic.ring(l, p.position(), 1.2, PINK);
        Fx.lendSkill(p, "tensura:spatial_motion", 200);
        boolean ok = Fx.teleportLook(p, 24.0);
        if (ok) FxRelic.ring(l, p.position(), 1.2, GOLD);
        say(p, ok ? "You step through the rift." : "There is no safe place to step to.");
    }

    /** Escape Rift (once only): drive the butt into the ground; a circle of teleportation opens for a round and the spear is destroyed. */
    static void escapeRift(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        if (!has(p, Wave21Rules.F_RIFT) || ctx.stack().isEmpty()) return;
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 dir = flat.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 at = p.position().add(dir.scale(3.0));
        if (!Wave21Events.openZone(p, at)) {
            say(p, "Too many rifts are open; try again in a moment.");
            return;
        }
        ctx.stack().shrink(1);                                                  // the card: once this ability is used, the spear is destroyed
        Fx.say(p, "The Rift Breaker shatters as it opens the Escape Rift: whoever steps into the circle is carried far away.");
    }

    // ================================================================ Scimitar of the Seasons
    static void commandWord(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        Wave21Rules.Season s = Wave21Rules.season(p.level().getDayTime() / 24000L);
        if (ctx.isOn("command_word")) {
            FxRelic.burst(p.serverLevel(), p.position().add(0, 1.2, 0), Wave21Rules.seasonColour(s));
            say(p, "The scimitar takes on the season: " + Wave21Rules.seasonName(s) + ". Every blow bites for 2d6 more. Use it again to dismiss it.");
        } else {
            say(p, "The seasonal edge is dismissed.");
        }
    }

    // ================================================================ Silver-Wind Mountain Glaive
    private static boolean glaive(ServerPlayer p, ActCtx ctx, int cost) {
        if (!has(p, Wave21Rules.F_GLAIVE)) return false;
        if (!Wave21Charges.spend(ctx.stack(), "sw", cost, Wave21Rules.GLAIVE_MAX)) {
            say(p, "The glaive has too few charges (" + Wave21Charges.held(ctx.stack(), "sw", Wave21Rules.GLAIVE_MAX) + " left, " + cost + " needed).");
            return false;
        }
        say(p, "Charges left: " + Wave21Charges.held(ctx.stack(), "sw", Wave21Rules.GLAIVE_MAX) + " of " + Wave21Rules.GLAIVE_MAX + ".");
        return true;
    }

    /** Forceful Strike (1 charge): a gust of wind in the direction of the blow: foes in the cone are thrown back and cut, missiles are turned aside. */
    static void forcefulStrike(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        if (!glaive(p, ctx, 1)) return;
        ServerLevel l = p.serverLevel();
        Vec3 look = p.getLookAngle();
        for (LivingEntity e : foesNear(p, p.position(), Wave21Rules.GUST_RANGE)) {
            Vec3 d = e.position().subtract(p.position());
            if (!Wave21Rules.inCone(d.x, d.z, look.x, look.z, Wave21Rules.GUST_RANGE, 0.55)) continue;
            Wave0Util.extra(p, e, 3.0f, "minecraft:player_attack");
            e.push(look.x * 1.3, 0.35, look.z * 1.3);
            e.hurtMarked = true;
        }
        for (net.minecraft.world.entity.projectile.Projectile pr : l.getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, p.getBoundingBox().inflate(Wave21Rules.GUST_RANGE))) {
            if (pr.getOwner() != p) pr.setDeltaMovement(pr.getDeltaMovement().add(look.scale(1.2)));
        }
        beam(l, p.getEyePosition(), p.getEyePosition().add(look.scale(Wave21Rules.GUST_RANGE)), WIND);
        FxRelic.burst(l, p.position().add(look.scale(3.0)).add(0, 1.0, 0), WIND);
    }

    /** Whirlwind Maneuver (2 charges, twice a day): five seconds in a storm of 4 blocks: foes are dragged in and cut, you cannot walk (the timer is in persistent data). */
    static void whirlwind(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        if (!glaive(p, ctx, 2)) return;
        p.getPersistentData().putLong(K_WHIRL, p.level().getGameTime() + Wave21Rules.WHIRL_TICKS);
        FxRelic.ring(p.serverLevel(), p.position(), Wave21Rules.WHIRL_RADIUS, WIND);
    }

    /** Become the Hurricane (5 charges): a steel wind strike: up to five foes within 9 blocks are cut in a row, and the wind carries you back to where you stood. */
    static void hurricane(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        if (!glaive(p, ctx, 5)) return;
        ServerLevel l = p.serverLevel();
        Vec3 from = p.getEyePosition();
        int n = 0;
        for (LivingEntity e : foesNear(p, p.position(), 9.0)) {
            if (n++ >= Wave21Rules.HURRICANE_TARGETS) break;
            Vec3 at = e.position().add(0, e.getBbHeight() * 0.5, 0);
            beam(l, from, at, WIND);
            FxRelic.burst(l, at, WIND);
            Wave0Util.extra(p, e, 8.0f, "minecraft:player_attack");
            from = at;
        }
        say(p, n == 0 ? "The hurricane finds no foe." : "The hurricane cuts " + n + " foe(s).");
    }

    // ================================================================ Spider Sword
    /** Spider Climb: a dash, and for ten seconds you stick to walls (push toward a wall to climb, sneak to hold still). */
    static void spiderClimb(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        p.getPersistentData().putLong(K_CLIMB, p.level().getGameTime() + 200);
        Fx.lendSkill(p, "tensura:sticky_steel_thread", 200);
        Fx.dash(p, 3.0);
        FxRelic.burst(p.serverLevel(), p.position().add(0, 0.2, 0), 0xBE2822);
        say(p, "Your feet stick: you can climb walls for ten seconds.");
    }

    /** Web Snare: a web of resin binds the creature in your sights (heavy Slowness, Weakness; players 3 seconds). */
    static void webSnare(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        Aim a = aim(p, 14.0, x -> Wave0Util.fair(p, x));
        if (a.target() == null) {
            say(p, "No creature in sight.");
            return;
        }
        LivingEntity t = a.target();
        int ticks = t instanceof Player ? 60 : 160;
        Fx.addEffect(t, "minecraft:slowness", ticks, 4);
        Fx.addEffect(t, "minecraft:weakness", ticks, 0);
        beam(p.serverLevel(), p.getEyePosition(), t.position().add(0, t.getBbHeight() * 0.5, 0), 0xE8E8E8);
        FxRelic.burst(p.serverLevel(), t.position().add(0, 0.8, 0), 0xE8E8E8);
    }

    // ================================================================ Stick of Youth
    /** Take Form: club, sword, axe, spear, dagger (stored on the stick). */
    static void takeForm(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        int next = Wave21Rules.nextForm(Wave0Util.stackInt(ctx.stack(), K_FORM, 0));
        Wave0Util.edit(ctx.stack(), t -> t.putInt(K_FORM, next));
        FxRelic.burst(p.serverLevel(), p.position().add(0, 1.2, 0), 0x70B040);
        say(p, "The stick takes the form of a " + Wave21Rules.formName(next) + ".");
    }

    // ================================================================ Sun Gift
    private static boolean sun(ServerPlayer p, ActCtx ctx, int cost) {
        if (!has(p, Wave21Rules.F_SUN)) return false;
        if (!Wave21Charges.spend(ctx.stack(), "sun", cost, Wave21Rules.SUN_MAX)) {
            say(p, "The Sun Gift has too few charges (" + Wave21Charges.held(ctx.stack(), "sun", Wave21Rules.SUN_MAX) + " left; it recharges in sunlight).");
            return false;
        }
        say(p, "Charges left: " + Wave21Charges.held(ctx.stack(), "sun", Wave21Rules.SUN_MAX) + " of " + Wave21Rules.SUN_MAX + ".");
        return true;
    }

    /** Guiding Bolt (1 charge): a bolt of radiant light; the creature it hits glows. */
    static void guidingBolt(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        if (!sun(p, ctx, 1)) return;
        Aim a = aim(p, 24.0, x -> Wave0Util.fair(p, x));
        beam(p.serverLevel(), p.getEyePosition(), a.end(), SUN);
        if (a.target() == null) return;
        Wave0Util.extra(p, a.target(), 8.0f, "tensura:holy_damage");
        Fx.addEffect(a.target(), "minecraft:glowing", 200, 0);
        FxRelic.burst(p.serverLevel(), a.end(), SUN);
    }

    private static void blast(ServerPlayer p, Vec3 at, double r, float fire, float radiant, int colour) {
        ServerLevel l = p.serverLevel();
        FxRelic.ring(l, at, r, colour);
        FxRelic.burst(l, at, colour);
        for (LivingEntity e : foesNear(p, at, r)) {
            Wave0Util.extra(p, e, fire, "minecraft:in_fire");
            if (radiant > 0f) Wave0Util.extra(p, e, radiant, "tensura:holy_damage");
            e.setRemainingFireTicks(80);
        }
    }

    /** Fireball (2 charges): a blast of fire where your look ends (4 blocks across, no blocks harmed). */
    static void fireball(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        if (!sun(p, ctx, 2)) return;
        Aim a = aim(p, 22.0, x -> Wave0Util.fair(p, x));
        beam(p.serverLevel(), p.getEyePosition(), a.end(), ORANGE);
        blast(p, a.end(), 4.0, 11.0f, 0f, ORANGE);
    }

    /** Flame Strike (3 charges): a column of fire and light where you look, 3 blocks across. */
    static void flameStrike(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        if (!sun(p, ctx, 3)) return;
        Aim a = aim(p, 20.0, x -> Wave0Util.fair(p, x));
        blast(p, a.end(), 3.0, 14.0f, 10.0f, SUN);
        for (int i = 1; i <= 4; i++) FxRelic.burst(p.serverLevel(), a.end().add(0, i, 0), ORANGE);
    }

    /** Firebolt (the cantrip, no charge): one bolt of fire. */
    static void firebolt(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        if (!has(p, Wave21Rules.F_SUN)) return;
        Aim a = aim(p, 20.0, x -> Wave0Util.fair(p, x));
        beam(p.serverLevel(), p.getEyePosition(), a.end(), ORANGE);
        if (a.target() == null) return;
        Wave0Util.extra(p, a.target(), 6.0f, "minecraft:in_fire");
        a.target().setRemainingFireTicks(60);
    }

    // ================================================================ The Last Horseman
    private static boolean dead(ServerPlayer p, ActCtx ctx) {
        if (!has(p, Wave21Rules.F_HORSEMAN)) return false;
        if (!Wave21Charges.spend(ctx.stack(), "dead", 1, Wave21Rules.DEAD_MAX)) {
            say(p, "The scythe has no charge left (one returns each Minecraft day).");
            return false;
        }
        return true;
    }

    /** Spare the Dying: the most hurt ally (or you) within 6 blocks is steadied: 4 health and a moment of Regeneration. */
    static void spareTheDying(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        LivingEntity best = p;
        float low = p.getHealth() / p.getMaxHealth();
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(6.0), x -> x != p && x.isAlive() && !x.isSpectator() && x.isAlliedTo(p))) {
            float f = e.getHealth() / e.getMaxHealth();
            if (f < low) {
                low = f;
                best = e;
            }
        }
        if (best instanceof ServerPlayer sp) Fx.heal(sp, 4.0f);
        else best.heal(4.0f);
        Fx.addEffect(best, "minecraft:regeneration", 100, 1);
        FxRelic.burst(p.serverLevel(), best.position().add(0, 1.0, 0), VIOLET);
    }

    /** Speak With Dead: for ten seconds every undead creature within 24 blocks glows, and you are told how many there are. */
    static void speakWithDead(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        int n = 0;
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(24.0), x -> x != p && x.isAlive() && x.getType().is(EntityTypeTags.UNDEAD))) {
            if (n++ >= 24) break;
            Fx.addEffect(e, "minecraft:glowing", 200, 0);
        }
        Fx.addEffect(p, "minecraft:night_vision", 200, 0);
        FxRelic.burst(p.serverLevel(), p.position().add(0, 1.0, 0), VIOLET);
        say(p, n == 0 ? "The dead are silent here." : "The dead whisper: " + n + " of them stir nearby.");
    }

    /** Find Steed, Nightmare: a skeletal horse is called (five minutes, tamed to you, saddled) and you mount it. */
    static void findSteed(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        LivingEntity h = Fx.summon(p, "minecraft:skeleton_horse", 6000);
        if (h == null) {
            say(p, "No steed answers.");
            return;
        }
        if (h instanceof AbstractHorse horse) {
            try {
                horse.setTamed(true);
                horse.getSlot(400).set(new ItemStack(Items.SADDLE));
            } catch (RuntimeException ignored) {
                // an unsaddled steed is still a steed
            }
        }
        Fx.addEffect(h, "minecraft:fire_resistance", 6000, 0);
        Fx.addEffect(h, "minecraft:speed", 6000, 1);
        p.startRiding(h);
    }

    /** Create Undead (one charge): three wither skeletons rise as your servants for three minutes. */
    static void createUndead(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        if (!dead(p, ctx)) return;
        int n = 0;
        for (int i = 0; i < 3; i++) if (Fx.summon(p, "minecraft:wither_skeleton", 3600) != null) n++;
        say(p, n + " undead rise at your word.");
    }

    /** Word of Ruin (one charge): a death word that never kills outright: 12 plus a third of the target's maximum health (capped on players), and Wither. */
    static void wordOfRuin(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        Aim a = aim(p, 24.0, x -> Wave0Util.fair(p, x));
        if (a.target() == null) {
            say(p, "No creature in sight: the word is not spoken.");
            return;
        }
        if (!dead(p, ctx)) return;
        LivingEntity t = a.target();
        beam(p.serverLevel(), p.getEyePosition(), t.position().add(0, t.getBbHeight() * 0.5, 0), 0x301040);
        Fx.damageHeavy(p, t, Wave21Rules.ruinDamage(t.getMaxHealth()), "minecraft:wither");
        Fx.addEffect(t, "minecraft:wither", t instanceof Player ? 60 : 120, 2);
    }

    // ================================================================ The Legacy
    /** Toss the Head: the head flies up to 36 blocks along your look and strikes everything on the line (at most six); the hammer is headless for eight seconds. */
    static void tossHead(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        if (!has(p, Wave21Rules.F_LEGACY)) return;
        ServerLevel l = p.serverLevel();
        long now = l.getGameTime();
        if (p.getPersistentData().getLong(K_HEADLESS) > now) {
            say(p, "The head is already out: sneak-use to recall it.");
            return;
        }
        Vec3 eye = p.getEyePosition(), end = aim(p, Wave21Rules.TOSS_RANGE, x -> false).end();
        beam(l, eye, end, 0x78DCFF);
        int n = 0;
        List<LivingEntity> line = new ArrayList<>();
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(2.0), x -> Wave0Util.fair(p, x)))
            if (Wave21Rules.distToSegment(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), eye.x, eye.y, eye.z, end.x, end.y, end.z) <= 1.5) line.add(e);
        line.sort(Comparator.comparingDouble(e -> e.distanceToSqr(eye)));
        for (LivingEntity e : line) {
            if (n++ >= 6) break;
            Wave0Util.extra(p, e, 10.0f, "minecraft:lightning_bolt");
        }
        FxRelic.burst(l, end, 0x78DCFF);
        p.getPersistentData().putLong(K_HEADLESS, now + Wave21Rules.HEADLESS_TICKS);
        say(p, "The head flies: the hammer is headless for eight seconds (a poor club) until you recall it.");
    }

    /** Recall: the head returns at once, cutting what lies on the way home. */
    static void recallHead(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        if (p.getPersistentData().getLong(K_HEADLESS) <= p.level().getGameTime()) return;
        p.getPersistentData().remove(K_HEADLESS);
        FxRelic.burst(p.serverLevel(), p.position().add(0, 1.2, 0), 0x78DCFF);
        for (LivingEntity e : foesNear(p, p.position(), 5.0)) Wave0Util.extra(p, e, 5.0f, "minecraft:lightning_bolt");
        say(p, "The head slams back onto the handle.");
    }

    // ================================================================ The Librarian's Maul
    /** Quiet Please: the book slams shut: foes within 4 blocks are hurt, slowed and silenced (players for a second). */
    static void quietPlease(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        FxRelic.ring(p.serverLevel(), p.position(), 4.0, 0x90F0E0);
        for (LivingEntity e : foesNear(p, p.position(), 4.0)) {
            Wave0Util.extra(p, e, 7.0f, "minecraft:player_attack");
            int t = e instanceof Player ? 40 : 120;
            Fx.addEffect(e, "minecraft:slowness", t, 2);
            Fx.addEffect(e, "tensura:silence", t, 0);
        }
    }

    // ================================================================ Time hammer
    /** Stop: every foe within 12 blocks is frozen for five seconds (players for three); you move at haste for the same time and may call Hammertime in the last two seconds. */
    static void stopTime(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        if (!has(p, Wave21Rules.F_TIME)) return;
        ServerLevel l = p.serverLevel();
        FxRelic.ring(l, p.position(), 6.0, ORANGE);
        FxRelic.ring(l, p.position(), Wave21Rules.STOP_RADIUS, 0xFFD0A0);
        for (LivingEntity e : foesNear(p, p.position(), Wave21Rules.STOP_RADIUS)) {
            int t = e instanceof Player ? 60 : Wave21Rules.STOP_TICKS;
            Fx.addEffect(e, "minecraft:slowness", t, 9);
            Fx.addEffect(e, "minecraft:weakness", t, 3);
            Fx.addEffect(e, "minecraft:mining_fatigue", t, 3);
            if (e instanceof Mob m) m.setTarget(null);
        }
        Fx.addEffect(p, "minecraft:speed", Wave21Rules.STOP_TICKS, 2);
        Fx.addEffect(p, "minecraft:haste", Wave21Rules.STOP_TICKS, 2);
        CompoundTag d = p.getPersistentData();
        d.putLong(K_STOP, l.getGameTime() + Wave21Rules.STOP_TICKS);
        d.putBoolean(K_SMITE_USED, false);
        say(p, "Stop. Time holds its breath: strike in the last two seconds and call Hammertime.");
    }

    // ================================================================ Vanguard Hammer, Willbreaker
    static void frighten(ServerPlayer p, LivingEntity e, double resist) {
        boolean pl = e instanceof Player;
        if (e.getRandom().nextDouble() >= Wave21Rules.fearChance(pl, resist)) return;
        Fx.addEffect(e, "tensura:fear", Wave21Rules.fearTicks(pl), 0);
        if (e instanceof net.minecraft.world.entity.PathfinderMob m) {
            m.setTarget(null);
            Vec3 away = m.position().subtract(p.position());
            Vec3 to = m.position().add(away.x, 0, away.z).normalize().scale(10.0);
            m.getNavigation().moveTo(m.getX() + to.x, m.getY(), m.getZ() + to.z, 1.4);
        }
    }

    /** War Cry (once a day): smash the ground; every creature in a 12-block, 120 degree cone is frightened (the fear effect, and creatures run from you). */
    static void warCry(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        Vec3 look = p.getLookAngle();
        FxRelic.ring(l, p.position().add(look.x * 3.0, 0, look.z * 3.0), 3.0, RED);
        FxRelic.burst(l, p.position().add(look.x * 2.0, 0.5, look.z * 2.0), RED);
        int n = 0;
        for (LivingEntity e : foesNear(p, p.position(), Wave21Rules.CONE_RANGE)) {
            Vec3 d = e.position().subtract(p.position());
            if (!Wave21Rules.inCone(d.x, d.z, look.x, look.z, Wave21Rules.CONE_RANGE, Wave21Rules.CONE_COS)) continue;
            Wave0Util.extra(p, e, 4.0f, "minecraft:player_attack");
            frighten(p, e, 0.0);
            n++;
        }
        say(p, "The war cry rings out: " + n + " foe(s) in the cone.");
    }

    /** Battle Cries: the previous owners shout: every foe within 6 blocks may be frightened. */
    static void battleCries(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        FxRelic.ring(p.serverLevel(), p.position(), 6.0, RED);
        for (LivingEntity e : foesNear(p, p.position(), 6.0)) frighten(p, e, 0.0);
    }
}
