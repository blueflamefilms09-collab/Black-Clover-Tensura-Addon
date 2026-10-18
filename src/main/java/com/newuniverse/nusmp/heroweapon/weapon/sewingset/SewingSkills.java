package com.newuniverse.nusmp.heroweapon.weapon.sewingset;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.sewingset.NeedleBoard.Side;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Deflect;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Effects;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Foes;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Geo;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Moves;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.PlayerState;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Safe;
import com.newuniverse.nusmp.thread.SafeSpot;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the bodies of the Vassal Sewing Set's techniques, kept apart from {@link SewingSetLogic}. The weapon is the needle and thread of S'yne's world (the Shield Hero
 * lore: Bind Wire, Needle Shot, Jumping Needle to a planted needle, Scissor Cut, Spider Net, Ariadne's Thread that guides people out of a labyrinth). Its loop is the
 * needle: a volley marks foes (stitches) and allies (beacons) and plants a needle in the ground, and the other techniques use the marks: the Scissor Cut cuts a marked
 * foe deeper, the Stitch Mend reaches marked allies from afar, the Jumping Needle jumps to one, Ariadne's Thread draws them in. It differs from the Thread Magic of
 * {@code thread/**} on purpose (red thread, Rouge, traps, the Mana Skin): here the tools are a needle, a pair of scissors and silk, nothing is protected by name, a
 * mark expires, and the barrier is a seam of stitches that catches shots. Damage goes through {@code HeroCtx.hit}, healing through {@code HeroCtx.heal}, hard
 * control behind {@code HeroCtx.control}, every delayed step through {@link Safe}; no block is ever changed.
 */
final class SewingSkills {
    private SewingSkills() {}

    /** The needles of each wielder; dropped on logout and when the server stops. */
    static final PlayerState<NeedleBoard> BOARD = new PlayerState<>(NeedleBoard::new);

    private static final int VIOLET = 0xFFA64DFF, SILK = 0xFFC03A78, SOFT = 0xFFB77CF0;
    private static final int VIOLET_RGB = 0xB77CF0;

    private static Vec3 hand(HeroCtx c) {
        return c.eye().add(c.look().scale(0.6)).add(0, -0.25, 0);
    }

    private static Vec3 flatLook(HeroCtx c) {
        Vec3 look = c.look(), flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
    }

    private static String controlMessage(ServerPlayer p) {
        return "Your control magic needs " + (BalanceLaw.controlCooldownLeft(p) + 19) / 20 + "s.";
    }

    // ================================================================== Needle Shot
    /** Three needles in a narrow fan: each leaves a stitch mark in the foe it strikes; with nothing struck the middle one marks an ally you look at, or plants itself in the ground. */
    static boolean needleShot(HeroCtx c) {
        ServerPlayer p = c.player;
        NeedleBoard board = BOARD.of(p);
        long now = c.level.getGameTime();
        Vec3 eye = c.eye(), look = c.look(), from = hand(c);
        Map<LivingEntity, Integer> hits = new LinkedHashMap<>();
        for (int k = -1; k <= 1; k++) {
            double[] d = Geo.aimOff(look.x, look.y, look.z, k * SewingNumbers.NEEDLE_SPREAD, 0);
            Vec3 dir = new Vec3(d[0], d[1], d[2]);
            LivingEntity foe = Foes.ray(p, eye, dir, SewingNumbers.NEEDLE_RANGE, SewingNumbers.NEEDLE_WIDTH);
            Vec3 end = foe != null ? foe.getBoundingBox().getCenter() : Foes.wallEnd(p, eye, eye.add(dir.scale(SewingNumbers.NEEDLE_RANGE)));
            c.fx(VfxShape.THREAD_LINE, from, end, VIOLET, 8, 0.5f);
            if (foe != null) hits.merge(foe, 1, Integer::sum);
        }
        c.sound(SoundEvents.ARROW_SHOOT, 0.5f, 1.9f);
        if (!hits.isEmpty()) {
            int life = SewingNumbers.foeMarkTicks(c.form.has("fine_stitch"));
            int touched = 0;
            for (Map.Entry<LivingEntity, Integer> e : hits.entrySet()) {
                if (touched++ >= SewingNumbers.NEEDLE_SHOT.targets()) break;
                board.mark(e.getKey().getUUID(), Side.FOE, now, life);
                c.hit(e.getKey(), (float) SewingNumbers.volleyDamage(e.getValue()));
            }
            return true;
        }
        LivingEntity friend = Foes.allyTarget(p, SewingNumbers.NEEDLE_RANGE);
        if (friend != null) {
            board.mark(friend.getUUID(), Side.ALLY, now, SewingNumbers.ALLY_MARK_TICKS);
            c.fx(VfxShape.THREAD_STRINGS, from, friend.getBoundingBox().getCenter(), VIOLET, 24, 0.8f);
            c.say("A needle waits in " + friend.getName().getString() + ".");
            return true;
        }
        Vec3 far = eye.add(look.scale(SewingNumbers.NEEDLE_RANGE)), end = Foes.wallEnd(p, eye, far);
        if (end.distanceToSqr(far) > 0.25) {
            board.plant(end.x, end.y, end.z, now, SewingNumbers.PLACE_TICKS);
            Tells.burst(c.level, end, VIOLET_RGB, 8);
            c.say("A needle is planted. Jumping Needle will take you to it.");
            return true;
        }
        c.say("Your needles find nothing to hold.");
        return false;
    }

    // ================================================================== Bind Wire
    /** Silk binds one foe fast and seals its skills (canon: Bind Wire also stops the target recalling skill names). Behind the shared control lock. */
    static boolean bindWire(HeroCtx c) {
        ServerPlayer p = c.player;
        LivingEntity t = Foes.target(p, SewingNumbers.BIND_RANGE);
        if (t == null) {
            c.say("There is no one to bind.");
            return false;
        }
        if (!c.control()) {
            c.say(controlMessage(p));
            return false;
        }
        long now = c.level.getGameTime();
        int ticks = BalanceLaw.controlTicks(t, SewingNumbers.BIND_TICKS);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9, false, true));
        Effects.named("tensura", "silence").ifPresent(h -> t.addEffect(new MobEffectInstance(h, ticks, 0, false, true)));
        if (t instanceof Player) t.getPersistentData().putLong("nusmp_sealed_until", now + ticks);
        t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
        t.hurtMarked = true;
        c.hit(t, (float) SewingNumbers.BIND_WIRE.raw());
        BOARD.of(p).mark(t.getUUID(), Side.FOE, now, SewingNumbers.foeMarkTicks(c.form.has("fine_stitch")));
        c.fx(VfxShape.THREAD_STRINGS, hand(c), t.getBoundingBox().getCenter(), SILK, 20, 1.0f);
        VfxSpawn.sendFollowing(c.level, VfxShape.THREAD_COCOON, t, t.position().add(0, t.getBbHeight() * 0.5, 0), SILK, ticks + 6, Math.max(0.6f, t.getBbHeight() / 1.8f));
        c.sound(SoundEvents.WOOL_PLACE, 1.2f, 0.8f);
        return true;
    }

    // ================================================================== Jumping Needle
    /** Jump to the nearest ally you have put a needle in, or to the needle planted in the ground; with none, plant one where you stand to jump back to. */
    static boolean jumpingNeedle(HeroCtx c) {
        ServerPlayer p = c.player;
        NeedleBoard board = BOARD.of(p);
        long now = c.level.getGameTime();
        LivingEntity ally = null;
        double best = SewingNumbers.JUMP_RANGE * SewingNumbers.JUMP_RANGE;
        for (UUID id : board.ids(Side.ALLY, now)) {
            LivingEntity a = Foes.byId(c.level, id);
            if (a == null || !a.isAlive() || a == p) continue;
            double d = a.distanceToSqr(p);
            if (d <= best) {
                best = d;
                ally = a;
            }
        }
        boolean place = board.placeLive(now);
        if (ally == null && !place) {
            board.plant(p.getX(), p.getY(), p.getZ(), now, SewingNumbers.PLACE_TICKS);
            Tells.burst(c.level, p.position(), VIOLET_RGB, 8);
            c.say("A needle is planted here. Cast again to jump back to it.");
            return true;
        }
        Vec3 spot;
        if (ally != null) {
            spot = Moves.landingNear(p, p, ally.position(), SewingNumbers.JUMP_LAND, p.getYRot() * Math.PI / 180.0);
        } else {
            Vec3 home = new Vec3(board.placeX(), board.placeY(), board.placeZ());
            if (home.distanceToSqr(p.position()) > SewingNumbers.JUMP_RANGE * SewingNumbers.JUMP_RANGE) {
                c.say("The planted needle is too far away.");
                return false;
            }
            spot = SafeSpot.firstOk(c.level, p, home);
            if (spot == null) spot = Moves.landingNear(p, p, home, SewingNumbers.JUMP_LAND, 0.0);
        }
        if (spot == null) {
            c.say("There is no safe place beside the needle.");
            return false;
        }
        Vec3 was = p.position();
        c.fx(VfxShape.THREAD_STRINGS, was.add(0, 1, 0), spot.add(0, 1, 0), VIOLET, 20, 1.2f);
        p.teleportTo(spot.x, spot.y, spot.z);
        p.fallDistance = 0f;
        if (ally == null) board.clearPlace();
        Tells.burst(c.level, spot, VIOLET_RGB, 10);
        c.sound(SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1.7f);
        return true;
    }

    // ================================================================== Stitch Mend
    /** Close wounds with silk: allies beside you and up to three you have put a needle in (even far away) mend a little and shed one harmful effect. */
    static boolean stitchMend(HeroCtx c) {
        ServerPlayer p = c.player;
        long now = c.level.getGameTime();
        List<LivingEntity> group = new ArrayList<>(Foes.alliesAround(p, p.position(), SewingNumbers.MEND_RADIUS, SewingNumbers.STITCH_MEND.targets()));
        for (UUID id : BOARD.of(p).ids(Side.ALLY, now)) {
            if (group.size() >= SewingNumbers.STITCH_MEND.targets()) break;
            LivingEntity a = Foes.byId(c.level, id);
            if (a != null && a.isAlive() && !group.contains(a) && a.distanceToSqr(p) <= SewingNumbers.MEND_REACH * SewingNumbers.MEND_REACH) group.add(a);
        }
        boolean any = false;
        for (LivingEntity a : group) {
            boolean hurt = a.getHealth() < a.getMaxHealth();
            boolean cured = !Effects.strip(a, false, SewingNumbers.MEND_CURES).isEmpty();
            if (hurt) c.heal(a, (float) (a.getMaxHealth() * SewingNumbers.MEND_SHARE));
            if (hurt || cured) {
                any = true;
                c.fx(VfxShape.SEAL_WOUND, p.getEyePosition(), a.getBoundingBox().getCenter(), VIOLET, 24, 0.8f);
            }
        }
        if (!any) {
            c.say("No one needs mending.");
            return false;
        }
        c.sound(SoundEvents.WOOL_PLACE, 1.0f, 1.5f);
        return true;
    }

    // ================================================================== Scissor Cut
    /** Two great snips crossed on the spot you look at, after a short, visible crossing of lines: a foe with a stitch mark takes 30 percent more. It also snips you free of slowness. */
    static boolean scissorCut(HeroCtx c) {
        ServerPlayer p = c.player;
        LivingEntity aimed = Foes.target(p, SewingNumbers.CUT_RANGE);
        Vec3 centre = aimed != null ? aimed.getBoundingBox().getCenter() : p.getEyePosition().add(c.look().scale(3.5));
        List<LivingEntity> foes = Foes.around(p, centre, SewingNumbers.CUT_RADIUS, SewingNumbers.SCISSOR_CUT.targets());
        if (foes.isEmpty()) {
            c.say("There is nothing within the snip.");
            return false;
        }
        Vec3 right = c.look().cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 a1 = centre.add(right.scale(-2.2)).add(0, 1.6, 0), b1 = centre.add(right.scale(2.2)).add(0, -1.0, 0);
        Vec3 a2 = centre.add(right.scale(2.2)).add(0, 1.6, 0), b2 = centre.add(right.scale(-2.2)).add(0, -1.0, 0);
        Tells.line(c.level, a1, b1, VIOLET_RGB);
        Tells.line(c.level, a2, b2, VIOLET_RGB);
        c.sound(SoundEvents.SHEEP_SHEAR, 1.0f, 0.7f);
        Safe.later(c, SewingNumbers.CUT_TELL, "sewing.scissor", f -> {
            NeedleBoard board = BOARD.of(f.player);
            long now = f.level.getGameTime();
            // who stands in the crossing NOW: a foe that stepped out during the tell is missed (that is the answer to the technique)
            for (LivingEntity t : Foes.around(f.player, centre, SewingNumbers.CUT_RADIUS, SewingNumbers.SCISSOR_CUT.targets())) {
                f.hit(t, (float) SewingNumbers.cutDamage(board.consume(t.getUUID(), now)));
            }
            f.player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            f.fx(VfxShape.SLASH_WAVE, a1, b1, SILK, 10, 1.4f);
            f.fx(VfxShape.SLASH_WAVE, a2, b2, SILK, 10, 1.4f);
            f.sound(SoundEvents.SHEEP_SHEAR, 1.2f, 1.3f);
        });
        return true;
    }

    // ================================================================== Stitched Barrier
    /** A seam of stitches across the ground ahead for six seconds: it catches up to six hostile shots that fly into it, and foes that touch it are slowed and pricked once. */
    static boolean stitchedBarrier(HeroCtx c) {
        ServerPlayer p = c.player;
        Vec3 flat = flatLook(c);
        Vec3 centre = p.position().add(flat.scale(SewingNumbers.BARRIER_DISTANCE));
        Vec3 side = new Vec3(-flat.z, 0, flat.x);
        Vec3 a = centre.add(side.scale(SewingNumbers.BARRIER_HALF)), b = centre.add(side.scale(-SewingNumbers.BARRIER_HALF));
        Vec3 wall = new Vec3(centre.x, centre.y + 1.2, centre.z);
        drawSeam(c, a, b);
        c.sound(SoundEvents.WOOL_PLACE, 1.3f, 0.6f);
        int[] caught = {0};
        Set<LivingEntity> pricked = Collections.newSetFromMap(new IdentityHashMap<>());
        Safe.zone(c, SewingNumbers.BARRIER_TICKS, 1, "sewing.barrier", false, (f, age) -> {
            if (age > 0 && age % 40 == 0) drawSeam(f, a, b);
            if (caught[0] < SewingNumbers.BARRIER_CATCHES) {
                AABB box = new AABB(a, b).inflate(1.5, 4.0, 1.5);
                for (Projectile pr : Deflect.hostileShots(f.player, box)) {
                    Vec3 p1 = pr.position(), p0 = p1.subtract(pr.getDeltaMovement());
                    if (!Geo.sweepsDisc(wall.x, wall.y, wall.z, flat.x, 0, flat.z, SewingNumbers.BARRIER_HALF, 0.5, p0.x, p0.y, p0.z, p1.x, p1.y, p1.z)) continue;
                    pr.discard();
                    f.fx(VfxShape.THREAD_TRAP_LINE, p1.add(0, -0.5, 0), p1.add(0, 0.5, 0), VIOLET, 8, 0.8f);
                    if (++caught[0] >= SewingNumbers.BARRIER_CATCHES) break;
                }
            }
            for (LivingEntity t : Foes.around(f.player, centre, SewingNumbers.BARRIER_HALF + 1.5, SewingNumbers.STITCHED_BARRIER.targets())) {
                Vec3 at = t.position();
                if (Geo.segDist(at.x, a.y, at.z, a.x, a.y, a.z, b.x, a.y, b.z) > SewingNumbers.BARRIER_TOUCH) continue;
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SewingNumbers.BARRIER_SLOW_TICKS, 1, false, true));
                if (pricked.add(t)) f.hit(t, (float) SewingNumbers.BARRIER_PRICK);
            }
            return true;
        });
        return true;
    }

    private static void drawSeam(HeroCtx c, Vec3 a, Vec3 b) {
        c.fx(VfxShape.THREAD_TRAP_LINE, a.add(0, 0.3, 0), b.add(0, 0.3, 0), VIOLET, 50, 1.0f);
        c.fx(VfxShape.THREAD_TRAP_LINE, a.add(0, 1.5, 0), b.add(0, 1.5, 0), SILK, 50, 1.0f);
        c.fx(VfxShape.THREAD_TRAP_LINE, a.add(0, 2.7, 0), b.add(0, 2.7, 0), VIOLET, 50, 1.0f);
    }

    // ================================================================== Spider Net
    /** A net of silk flung where you look: the foes caught in it are held almost fast and pricked. Behind the shared control lock. */
    static boolean spiderNet(HeroCtx c) {
        ServerPlayer p = c.player;
        LivingEntity t = Foes.target(p, SewingNumbers.NET_RANGE);
        if (t == null) {
            c.say("There is no one to net.");
            return false;
        }
        List<LivingEntity> caught = Foes.around(p, t.position(), SewingNumbers.NET_RADIUS, SewingNumbers.SPIDER_NET.targets());
        if (!c.control()) {
            c.say(controlMessage(p));
            return false;
        }
        long now = c.level.getGameTime();
        NeedleBoard board = BOARD.of(p);
        int mark = SewingNumbers.foeMarkTicks(c.form.has("fine_stitch"));
        for (LivingEntity x : caught) {
            int ticks = BalanceLaw.controlTicks(x, SewingNumbers.NET_TICKS);
            x.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 4, false, true));
            c.hit(x, (float) SewingNumbers.SPIDER_NET.raw());
            board.mark(x.getUUID(), Side.FOE, now, mark);
        }
        Vec3 ground = Foes.groundBelow(p, t.position(), 3.0);
        c.fx(VfxShape.THREAD_WEB, ground, ground.add(0, 1, 0), SOFT, SewingNumbers.NET_TICKS, (float) SewingNumbers.NET_RADIUS);
        c.fx(VfxShape.THREAD_STRINGS, hand(c), t.getBoundingBox().getCenter(), SILK, 16, 1.0f);
        c.sound(SoundEvents.WOOL_PLACE, 1.2f, 0.5f);
        return true;
    }

    // ================================================================== Ariadne's Thread
    /**
     * The ultimate: after a visible gathering of threads, up to four of your allies (the ones you put a needle in first, then your party and pets) are drawn to your side,
     * and every foe within twenty-four blocks is lit up for eight seconds, as if the whole labyrinth were mapped.
     */
    static boolean ariadnesThread(HeroCtx c) {
        ServerPlayer p = c.player;
        long now = c.level.getGameTime();
        List<LivingEntity> picks = new ArrayList<>();
        for (UUID id : BOARD.of(p).ids(Side.ALLY, now)) {
            LivingEntity a = Foes.byId(c.level, id);
            if (a != null && a.isAlive() && a != p && picks.size() < SewingNumbers.ARIADNE_PICKS && a.distanceToSqr(p) <= SewingNumbers.ARIADNE_REACH * SewingNumbers.ARIADNE_REACH) picks.add(a);
        }
        for (LivingEntity a : Foes.alliesAround(p, p.position(), SewingNumbers.ARIADNE_REACH, 16)) {
            if (picks.size() >= SewingNumbers.ARIADNE_PICKS) break;
            if (a != p && !picks.contains(a) && Foes.isCompanion(p, a)) picks.add(a);
        }
        picks.removeIf(a -> a.distanceToSqr(p) < 4.0 * 4.0);          // those already beside you need no guiding
        List<LivingEntity> foes = Foes.around(p, p.position(), SewingNumbers.ARIADNE_REVEAL, SewingNumbers.ARIADNE_FOES);
        if (picks.isEmpty() && foes.isEmpty()) {
            c.say("There is no one for the thread to guide.");
            return false;
        }
        Vec3 here = p.position();
        Tells.ring(c.level, here, SewingNumbers.ARIADNE_LAND + 0.6, VIOLET_RGB);
        for (LivingEntity a : picks) c.fx(VfxShape.THREAD_STRINGS, hand(c), a.getBoundingBox().getCenter(), VIOLET, SewingNumbers.ARIADNE_TELL, 1.2f);
        c.sound(SoundEvents.BEACON_ACTIVATE, 1.0f, 1.8f);
        Safe.later(c, SewingNumbers.ARIADNE_TELL, "sewing.ariadne", f -> {
            int i = 0;
            for (LivingEntity a : picks) {
                if (!a.isAlive() || a.level() != f.level) continue;
                Vec3 spot = Moves.landingNear(f.player, a, f.player.position(), SewingNumbers.ARIADNE_LAND, i++ * Math.PI / 2.0);
                if (spot == null) continue;
                f.fx(VfxShape.THREAD_STRINGS, a.position().add(0, 1, 0), spot.add(0, 1, 0), VIOLET, 14, 1.0f);
                a.teleportTo(spot.x, spot.y, spot.z);
                a.fallDistance = 0f;
                if (a instanceof Mob m) m.getNavigation().stop();
                Tells.burst(f.level, spot, VIOLET_RGB, 8);
            }
            for (LivingEntity t : Foes.around(f.player, f.player.position(), SewingNumbers.ARIADNE_REVEAL, SewingNumbers.ARIADNE_FOES)) {
                t.addEffect(new MobEffectInstance(MobEffects.GLOWING, SewingNumbers.ARIADNE_GLOW, 0, false, false));
            }
            f.sound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.4f);
        });
        return true;
    }

    // ================================================================== the thread to the beacons
    /** Every two seconds while the set is out: a faint thread from the wielder to each ally with a needle in them (at most four), and the table pruned. */
    static void onTick(HeroCtx c) {
        NeedleBoard board = BOARD.peek(c.player);
        long now = c.level.getGameTime();
        if (board == null || !board.drawDue(now, 40)) return;
        board.prune(now);
        int shown = 0;
        for (UUID id : board.ids(Side.ALLY, now)) {
            if (shown >= 4) break;
            LivingEntity a = Foes.byId(c.level, id);
            if (a == null || !a.isAlive() || a.distanceToSqr(c.player) > SewingNumbers.JUMP_RANGE * SewingNumbers.JUMP_RANGE) continue;
            c.fx(VfxShape.THREAD_LINE, hand(c), a.getBoundingBox().getCenter(), SOFT, 30, 0.4f);
            shown++;
        }
    }
}
