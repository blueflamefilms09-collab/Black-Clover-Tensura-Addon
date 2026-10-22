package com.newuniverse.nusmp.firecast;

import com.newuniverse.nusmp.anim.CastAnim;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Why: Magna Swing. The Fire page's spell does not leave the caster's hand: it is a ball tossed in front, and the bat sends it. Flow (docs/magic/fire_casting.md, section 1):
 * <pre>
 *   tick 0   the grimoire's cast path pays the mana, sets the cooldown and calls {@link #begin}: the ball prop (a hovering flame sphere) appears in front of the eye, the swing clip starts
 *   tick 8   the contact frame of the clip: the server tests the swing ONCE (distance and cone, FireCastRules.swingHits) against the ball
 *              hit  -> the page's own spell is cast with its start moved to the ball and the look as its aim (a bolt may home on a target inside a small cone, found now, once)
 *              miss -> the ball drops and bursts weakly (2 damage in 2 blocks, at most 4 foes)
 * </pre>
 * Per caster there is one ball at a time and at least {@link FireCastRules#MIN_SWING_GAP} ticks between swings; the page's own cooldown still applies; across the server the ball cap of
 * {@link FireCastConfig} holds. The swing needs the bat in the main hand on the contact tick too.
 */
final class MagnaSwing {
    private MagnaSwing() {}

    static boolean begin(GrimoireBook book, BookPage page, ManasSkillInstance inst, ServerPlayer p, int mode) {
        ServerLevel sl = p.serverLevel();
        FireCasting.State s = FireCasting.stateOf(p);
        long now = sl.getGameTime();
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        if (!FireCasting.BALLS.acquire(p.getUUID(), FireCastRules.BALLS_PER_PLAYER, FireCastConfig.maxBalls())) return page.cast().cast(book, inst, p, mode);      // over the cap: the plain cast, never nothing
        double[] b = FireCastRules.ballPosition(eye.x, eye.y, eye.z, look.x, look.y, look.z);
        Vec3 ball = new Vec3(b[0], b[1], b[2]);
        MagicPropEntity prop = MagicProps.spawn(sl, PropKind.FIRECAST_1, ball.subtract(0, 0.35, 0), p.getYRot(), 1.0f, FireCastRules.BALL_LIFE, FireCastRules.param(FireCastRules.V_BALL, 0), p);
        if (prop == null) { FireCasting.BALLS.release(p.getUUID()); return page.cast().cast(book, inst, p, mode); }
        UUID id = prop.getUUID();
        s.ball = id;
        s.lastSwing = now;
        FireCasting.markLive(p);
        CastAnim.play(p, CastAnim.FIRE_BAT_SWING);
        sl.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7f, 1.3f);
        UUID owner = p.getUUID();
        FireCasting.later(sl, FireCastRules.SWING_IMPACT_TICK, () -> impact(book, page, inst, p, mode, ball, id, owner));
        return true;                                                                                                          // the mana is spent and the cooldown set whatever the swing does
    }

    /** The contact frame: the one place the swing is tested and the spell launched. */
    private static void impact(GrimoireBook book, BookPage page, ManasSkillInstance inst, ServerPlayer p, int mode, Vec3 ball, UUID ballId, UUID owner) {
        FireCasting.State s = FireCasting.STATES.get(owner);
        if (s != null && ballId.equals(s.ball)) { s.ball = null; FireCasting.BALLS.release(owner); }
        else if (s == null) return;                                                                                          // the player is gone (logout, death): everything was cleaned up
        ServerLevel sl = p.serverLevel();
        MagicPropEntity prop = FireCasting.prop(sl, ballId);
        if (prop != null) prop.discard();
        if (!p.isAlive() || p.hasDisconnected() || p.isRemoved()) return;
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        boolean hit = FireCasting.hasBat(p) && FireCastRules.swingHits(eye.x, eye.y, eye.z, look.x, look.y, look.z, ball.x, ball.y, ball.z);
        if (hit) {
            sl.playSound(null, ball.x, ball.y, ball.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.1f, 0.8f);
            FireCasting.flare(p, book, ball, 0.7f);
            LivingEntity homing = homingTarget(p, eye, look);
            FireCasting.withEmitter(p, ball, homing, () -> FireCasting.safe(book, p, () -> page.cast().cast(book, inst, p, mode)));
            return;
        }
        sl.playSound(null, ball.x, ball.y, ball.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.4f, 1.6f);
        Vec3 down = ball.subtract(0, 0.6, 0);
        FireCasting.flare(p, book, down, 0.45f);
        List<LivingEntity> near = GrimoireBook.around(p, down, FireCastRules.WEAK_BURST_RADIUS);
        int n = 0;
        for (LivingEntity t : near) {
            if (n++ >= FireCastRules.WEAK_BURST_TARGETS) break;
            book.hurt(inst, p, t, mode, FireCastRules.WEAK_BURST_DAMAGE);
            t.igniteForSeconds(1);
        }
    }

    /** The nearest living thing inside the small homing cone, found once on the contact tick (null when none). */
    private static LivingEntity homingTarget(ServerPlayer p, Vec3 eye, Vec3 look) {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        int seen = 0;
        for (LivingEntity t : GrimoireBook.around(p, eye.add(look.scale(FireCastRules.HOMING_RANGE * 0.5)), FireCastRules.HOMING_RANGE * 0.5)) {
            if (seen++ >= 32) break;
            Vec3 d = t.getBoundingBox().getCenter().subtract(eye);
            if (!FireCastRules.inHomingCone(look.x, look.y, look.z, d.x, d.y, d.z)) continue;
            double q = d.lengthSqr();
            if (q < bestD) { bestD = q; best = t; }
        }
        return best;
    }
}
