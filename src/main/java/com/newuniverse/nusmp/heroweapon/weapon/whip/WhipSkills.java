package com.newuniverse.nusmp.heroweapon.weapon.whip;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.zogratis.ZogratisVfx;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.weapon.strike.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.strike.Strike;
import com.newuniverse.nusmp.heroweapon.weapon.strike.StrikeFx;
import com.newuniverse.nusmp.heroweapon.weapon.strike.StrikeSkill;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.djinn.weapon.WeaponGeo;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the nine techniques of the Vassal Whip, the weapon of reach and control. Bind Whip, Lightning Whip, Gigantic Star, Portal Whip and Dimension Whip carry the names of the
 * whip skills of the series; the lash, the pull and the swing from a tether are the ordinary work of a whip, and the Command is its special ability. Every technique that holds,
 * pulls or turns a foe passes through the shared control lock first ({@code HeroCtx#control}) and then through the balance law's own caps ({@link BalanceLaw#controlTicks}: three
 * seconds on a player, five on a mob, one on a boss), and a boss is never pulled or rooted, only slowed. The lightning and the star warn before they land; the lash, the bind and
 * the pull are instant because they do little damage. Nothing here changes a block or heals the wielder. The numbers are {@link WhipNumbers}'s; the Command's machinery is
 * {@link WhipCommand}; the speeds of the tether are {@link TetherMath}'s.
 */
final class WhipSkills {
    private WhipSkills() {}

    private static final int VIOLET = WhipCommand.VIOLET, GOLD = WhipCommand.GOLD;

    static List<HeroSkill> all() {
        return List.of(lash(), bindWhip(), reelIn(), tetherSwing(), lightningWhip(), command(), giganticStar(), portalWhip(), dimensionWhip());
    }

    /** Where the lash leaves the wielder's hand. */
    private static Vec3 hand(ServerPlayer p) { return p.getEyePosition().add(p.getLookAngle().scale(0.6)).add(0, -0.3, 0); }

    /** The whip can never hold a boss: it is only slowed. Everything else is held for the time the balance law allows, and marked as bound. */
    private static void hold(HeroCtx c, LivingEntity t, int baseTicks) {
        if (BalanceLaw.isBoss(t)) {
            Strike.effect(t, "minecraft:slowness", 20, 2);
            return;
        }
        int ticks = BalanceLaw.controlTicks(t, baseTicks);
        Strike.effect(t, "minecraft:slowness", ticks, 6);
        Strike.effect(t, "tensura:movement_interference", ticks, 0);
        WhipCommand.markBound(t, WhipNumbers.BOUND_TICKS);
        StrikeFx.shape(c.level, VfxShape.SEAL_CHAINS, t.position(), t.position().add(0, 1, 0), GOLD, ticks, 1.2f);
    }

    private static String controlWait(HeroCtx c) { return "control is ready in " + (BalanceLaw.controlCooldownLeft(c.player) + 19) / 20 + " s"; }

    // ================================================================ Lash
    private static StrikeSkill lash() {
        return StrikeSkill.of(WhipNumbers.LASH, "Lash",
                "Cracks the whip along your look, seven blocks: it cuts up to three foes in the line and slows each for a second. With the Storm Whip the lash also charges the foe for the next Lightning Whip.",
                c -> {
                    ServerPlayer p = c.player;
                    Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
                    Vec3 end = Strike.stopAtWall(p, eye, eye.add(look.scale(WhipNumbers.LASH_RANGE)));
                    float damage = (float) WhipNumbers.LASH.raw();
                    for (LivingEntity t : Strike.foesLine(p, eye, end, WhipNumbers.LASH_HALF_WIDTH, WhipNumbers.LASH.maxTargets())) {
                        if (WhipCommand.isCommanded(t) || !Strike.hit(c, t, damage, false)) continue;
                        if (c.form.has(WhipNumbers.COIL)) Strike.effect(t, "minecraft:slowness", WhipNumbers.COIL_TICKS, 0);
                        if (c.form.has(WhipNumbers.STATIC_CHARGE)) WhipCommand.markCharged(t, WhipNumbers.CHARGE_TICKS);
                    }
                    ZogratisVfx.bloodArcs(c.level, hand(p), end, 1.0, 10, VIOLET);
                    StrikeFx.sound(c.level, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.8f);
                    StrikeFx.sound(c.level, end, SoundEvents.CHAIN_HIT, 0.8f, 1.6f);
                    return true;
                });
    }

    // ================================================================ Bind Whip (canon)
    private static StrikeSkill bindWhip() {
        return StrikeSkill.of(WhipNumbers.BIND_WHIP, "Bind Whip",
                "The whip coils round the foe you face like a living snake and holds it still for two seconds (a boss is only slowed). If your control is not ready it only slows. The foe is marked as bound.",
                c -> {
                    ServerPlayer p = c.player;
                    LivingEntity t = Strike.pick(p, WhipNumbers.BIND_RANGE, 20);
                    if (t == null || WhipCommand.isCommanded(t)) {
                        c.say("No foe in reach of the whip.");
                        return false;
                    }
                    ZogratisVfx.bloodArcs(c.level, hand(p), Strike.center(t), 1.0, 10, VIOLET);
                    Strike.hit(c, t, (float) WhipNumbers.BIND_WHIP.raw(), false);
                    if (BalanceLaw.isBoss(t)) {
                        hold(c, t, WhipNumbers.BIND_TICKS);
                        c.say("A boss is only slowed by the whip.");
                    } else if (c.control()) {
                        hold(c, t, WhipNumbers.BIND_TICKS);
                    } else {
                        Strike.effect(t, "minecraft:slowness", WhipNumbers.BIND_SLOW_TICKS, 2);
                        c.say("The bind only slows: " + controlWait(c) + ".");
                    }
                    StrikeFx.sound(c.level, Strike.center(t), SoundEvents.CHAIN_HIT, 1.0f, 0.8f);
                    return true;
                });
    }

    // ================================================================ Reel In
    private static StrikeSkill reelIn() {
        return StrikeSkill.of(WhipNumbers.REEL_IN, "Reel In",
                "Snares the foe you face from up to eleven blocks away and yanks it to you. Uses your control lock; a boss cannot be pulled. The foe is marked as bound, and arrives where your friends can strike it.",
                c -> {
                    ServerPlayer p = c.player;
                    LivingEntity t = Strike.pick(p, WhipNumbers.REEL_RANGE, 20);
                    if (t == null || WhipCommand.isCommanded(t)) {
                        c.say("No foe in reach of the whip.");
                        return false;
                    }
                    if (BalanceLaw.isBoss(t)) {
                        c.say("A boss cannot be pulled.");
                        return false;
                    }
                    Vec3 to = p.position().subtract(t.position());
                    double dist = Math.sqrt(to.x * to.x + to.z * to.z);
                    if (!TetherMath.needsPull(dist)) {
                        c.say("The foe is already close.");
                        return false;
                    }
                    if (!c.control()) {
                        c.say("The whip cannot hold it yet: " + controlWait(c) + ".");
                        return false;
                    }
                    ZogratisVfx.bloodArcs(c.level, hand(p), Strike.center(t), 1.0, 10, VIOLET);
                    Strike.hit(c, t, (float) WhipNumbers.REEL_IN.raw(), false);                            // first the blow, whose shove would undo the pull
                    double[] v = TetherMath.toward(to.x, to.z, TetherMath.pullSpeed(dist));
                    t.setDeltaMovement(v[0], TetherMath.PULL_LIFT, v[1]);
                    t.hurtMarked = true;
                    WhipCommand.markBound(t, WhipNumbers.BOUND_TICKS);
                    StrikeFx.sound(c.level, Strike.center(t), SoundEvents.CHAIN_HIT, 1.0f, 0.6f);
                    return true;
                });
    }

    // ================================================================ Tether Swing
    private static StrikeSkill tetherSwing() {
        return StrikeSkill.of(WhipNumbers.TETHER_SWING, "Tether Swing",
                "Throws the whip at the foe or the ledge you look at, up to sixteen blocks away, and swings yourself to it. You drift down softly afterwards. Nothing is pulled but you.",
                c -> {
                    ServerPlayer p = c.player;
                    LivingEntity t = Strike.pick(p, WhipNumbers.TETHER_RANGE, 20);
                    HitResult ledge = t != null ? null : p.pick(WhipNumbers.TETHER_RANGE, 1.0f, false);
                    if (t == null && ledge.getType() != HitResult.Type.BLOCK) {
                        c.say("The whip has nothing to catch on.");
                        return false;
                    }
                    Vec3 mark = t != null ? t.position() : ledge.getLocation();
                    Vec3 from = p.position();
                    double dist = from.distanceTo(mark);
                    if (!TetherMath.canSwing(dist)) {
                        c.say("There is nothing far enough to swing to.");
                        return false;
                    }
                    double[] v = TetherMath.toward(mark.x - from.x, mark.z - from.z, TetherMath.swingSpeed(dist));
                    p.setDeltaMovement(v[0], TetherMath.swingLift(mark.y - from.y, dist), v[1]);
                    p.hurtMarked = true;
                    p.fallDistance = 0f;
                    Strike.effect(p, "minecraft:slow_falling", WhipNumbers.TETHER_SOFT_TICKS, 0);
                    ZogratisVfx.bloodArcs(c.level, hand(p), mark.add(0, 0.5, 0), 1.0, 12, VIOLET);
                    StrikeFx.sound(c.level, from, SoundEvents.CHAIN_HIT, 1.0f, 1.2f);
                    return true;
                }).cooldownFactor(c -> c.form.has(WhipNumbers.HOURGLASS_FLOW) ? WhipNumbers.HOURGLASS_FACTOR : 1.0);
    }

    // ================================================================ Lightning Whip (canon)
    private static StrikeSkill lightningWhip() {
        return StrikeSkill.of(WhipNumbers.LIGHTNING_WHIP, "Lightning Whip",
                "Cracks the whip on the foe you face; after a short warning lightning jumps from it to the nearest foes, up to five, losing a fifth of its strength at each jump. A foe charged by the Storm Whip's lash takes 30 percent more.",
                c -> {
                    ServerPlayer p = c.player;
                    LivingEntity first = Strike.pick(p, WhipNumbers.LIGHTNING_RANGE, 35);
                    if (first == null || WhipCommand.isCommanded(first)) {
                        c.say("No foe in reach of the whip.");
                        return false;
                    }
                    Vec3 from = hand(p);
                    StrikeFx.line(c.level, from, Strike.center(first), VIOLET);
                    StrikeFx.gather(c.level, Strike.center(first), 0.8, VIOLET, 10);
                    StrikeFx.sound(c.level, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.9f);
                    Strike.later(p, WhipNumbers.LIGHTNING_DELAY, cx -> arc(cx, first, from));
                    return true;
                });
    }

    private static void arc(HeroCtx c, LivingEntity first, Vec3 from) {
        Set<LivingEntity> struck = new HashSet<>();
        LivingEntity cur = first;
        Vec3 prev = from;
        for (int i = 0; i < WhipNumbers.LIGHTNING_FOES && cur != null; i++) {
            if (!cur.isAlive() || cur.level() != c.level) break;
            double charged = c.form.has(WhipNumbers.STATIC_CHARGE) && WhipCommand.takeCharge(cur) ? WhipNumbers.CHARGE_BONUS : 1.0;
            float damage = (float) (WhipNumbers.LIGHTNING_WHIP.raw() * WeaponGeo.chainFactor(i, WhipNumbers.LIGHTNING_FALLOFF) * charged);
            Strike.hit(c, cur, damage, false);
            Strike.effect(cur, "minecraft:slowness", 30, 0);
            Vec3 at = Strike.center(cur);
            StrikeFx.shape(c.level, VfxShape.LIGHTNING_SPEAR, prev, at, VIOLET, 8, 1.0f);
            struck.add(cur);
            prev = at;
            LivingEntity next = null;
            for (LivingEntity f : Strike.foesNear(c.player, cur.position(), WhipNumbers.LIGHTNING_JUMP, 8)) {
                if (!struck.contains(f) && !WhipCommand.isCommanded(f)) {
                    next = f;
                    break;
                }
            }
            cur = next;
        }
        StrikeFx.sound(c.level, prev, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6f, 1.6f);
    }

    // ================================================================ Command (the Vassal Whip's special ability)
    private static StrikeSkill command() {
        return StrikeSkill.of(WhipNumbers.COMMAND, "Command",
                "The whip takes the power of monsters: up to three hostile monsters within fourteen blocks turn on the nearest other monster for six seconds, on violet strings from your hand. They never hurt you, your friends or any player. Not on bosses, creepers, ghasts or blazes. Uses your control lock.",
                c -> {
                    List<Mob> mobs = WhipCommand.candidates(c.player, WhipNumbers.COMMAND_RANGE, WhipNumbers.COMMAND_MAX);
                    if (mobs.isEmpty()) {
                        c.say("No monster within reach to command.");
                        return false;
                    }
                    if (!c.control()) {
                        c.say("The whip cannot take them yet: " + controlWait(c) + ".");
                        return false;
                    }
                    WhipCommand.begin(c, mobs);
                    StrikeFx.sound(c.level, c.player.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0f, 1.2f);
                    StrikeFx.sound(c.level, c.player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 0.7f);
                    c.say("Command: " + mobs.size() + " monster" + (mobs.size() == 1 ? "" : "s") + " obey.");
                    return true;
                });
    }

    // ================================================================ Gigantic Star (canon, morning star form)
    private static StrikeSkill giganticStar() {
        return StrikeSkill.of(WhipNumbers.GIGANTIC_STAR, "Gigantic Star",
                "Whirls the spiked star on the end of the whip over your head and slams it down four and a half blocks ahead; a ring shows where. Heavy damage to everything within three blocks, a shove and a lift. A bound foe takes a quarter more with the Morning Star's Star Weight.",
                c -> {
                    ServerPlayer p = c.player;
                    Vec3 base = p.position().add(0, 0.6, 0), flat = Strike.flatLook(p);
                    Vec3 hit = Strike.stopAtWall(p, base, base.add(flat.scale(WhipNumbers.STAR_REACH)));
                    if (hit.distanceToSqr(base.add(flat.scale(WhipNumbers.STAR_REACH))) > 1.0E-6) hit = hit.subtract(flat.scale(0.4));
                    Vec3 spot = new Vec3(hit.x, p.getY(), hit.z);
                    StrikeFx.tellRing(c.level, spot, WhipNumbers.STAR_RADIUS, VIOLET, WhipNumbers.STAR_DELAY + 4);
                    StrikeFx.ring(c.level, spot, WhipNumbers.STAR_RADIUS, VIOLET);
                    StrikeFx.sound(c.level, p.position(), SoundEvents.CHAIN_HIT, 1.0f, 0.5f);
                    Strike.effect(p, "minecraft:slowness", WhipNumbers.STAR_DELAY, 2);
                    Strike.later(p, WhipNumbers.STAR_DELAY, cx -> slam(cx, spot));
                    return true;
                });
    }

    private static void slam(HeroCtx c, Vec3 spot) {
        int n = 0;
        for (LivingEntity t : Strike.foesNear(c.player, spot, WhipNumbers.STAR_RADIUS, WhipNumbers.GIGANTIC_STAR.maxTargets())) {
            if (WhipCommand.isCommanded(t)) continue;
            double bonus = c.form.has(WhipNumbers.STAR_WEIGHT) && WhipCommand.isBound(t) ? WhipNumbers.STAR_BONUS : 1.0;
            float damage = (float) Math.min(WhipNumbers.GIGANTIC_STAR.raw() * bonus, Budget.cap(WhipNumbers.GIGANTIC_STAR.kind()));
            if (!Strike.hit(c, t, damage, false)) continue;
            n++;
            Strike.shove(c, t, spot, 0.9);
            Strike.lift(t, 0.2);
        }
        StrikeFx.strikeRing(c.level, spot, WhipNumbers.STAR_RADIUS, VIOLET);
        StrikeFx.impact(c.level, spot, WhipNumbers.STAR_RADIUS * 0.8, VIOLET);
        StrikeFx.sound(c.level, spot, SoundEvents.IRON_GOLEM_ATTACK, 1.0f, 0.7f);
        StrikeFx.sound(c.level, spot, SoundEvents.ANVIL_LAND, 0.8f, 0.8f);
        if (n == 0) c.say("The star lands on nothing.");
    }

    // ================================================================ Portal Whip (canon, Dragon Hourglass Sand Whip)
    private static StrikeSkill portalWhip() {
        return StrikeSkill.of(WhipNumbers.PORTAL_WHIP, "Portal Whip",
                "Cracks the whip into a gate of sand and steps through it up to fourteen blocks along your look, to a place with room to stand. Foes round where you arrive are lashed and slowed.",
                c -> {
                    ServerPlayer p = c.player;
                    Vec3 start = p.position(), facing = p.getLookAngle();
                    if (!Fx.teleportLook(p, WhipNumbers.PORTAL_RANGE)) {
                        c.say("There is no place to step through to.");
                        return false;
                    }
                    Vec3 end = p.position();
                    ZogratisVfx.gate(c.level, start.add(0, 1.0, 0), facing, 1.3, 20, VIOLET);
                    ZogratisVfx.gate(c.level, end.add(0, 1.0, 0), facing.reverse(), 1.3, 20, VIOLET);
                    float damage = (float) WhipNumbers.PORTAL_WHIP.raw();
                    for (LivingEntity t : Strike.foesNear(p, end, WhipNumbers.PORTAL_RADIUS, WhipNumbers.PORTAL_WHIP.maxTargets())) {
                        if (WhipCommand.isCommanded(t) || !Strike.hit(c, t, damage, false)) continue;
                        Strike.effect(t, "minecraft:slowness", 40, 1);
                        Strike.shove(c, t, end, 0.4);
                    }
                    StrikeFx.sound(c.level, start, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.3f);
                    StrikeFx.sound(c.level, end, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 0.9f);
                    return true;
                }).cooldownFactor(c -> c.form.has(WhipNumbers.HOURGLASS_FLOW) ? WhipNumbers.HOURGLASS_FACTOR : 1.0);
    }

    // ================================================================ Dimension Whip (canon, ultimate)
    private static StrikeSkill dimensionWhip() {
        return StrikeSkill.of(WhipNumbers.DIMENSION_WHIP, "Dimension Whip",
                "The whip splits into countless lashes that reach every foe within eleven blocks at once, after a warning: each takes a heavy lash and is bound, as far as your control lock and the balance law allow. A boss is only slowed. You are held still while it gathers.",
                c -> {
                    ServerPlayer p = c.player;
                    List<LivingEntity> foes = new ArrayList<>();
                    for (LivingEntity t : Strike.foesAround(p, WhipNumbers.DIMENSION_RADIUS, WhipNumbers.DIMENSION_WHIP.maxTargets())) if (!WhipCommand.isCommanded(t)) foes.add(t);
                    if (foes.isEmpty()) {
                        c.say("No foe within reach.");
                        return false;
                    }
                    Vec3 hand = hand(p);
                    StrikeFx.ring(c.level, p.position(), WhipNumbers.DIMENSION_RADIUS, VIOLET);
                    ZogratisVfx.gate(c.level, p.position().add(0, 2.4, 0), new Vec3(0, -1, 0), 2.0, WhipNumbers.DIMENSION_DELAY + 6, VIOLET);
                    for (LivingEntity t : foes) StrikeFx.shape(c.level, VfxShape.THREAD_STRINGS, hand, Strike.center(t), VIOLET, WhipNumbers.DIMENSION_DELAY + 6, 1.0f);
                    StrikeFx.sound(c.level, p.position(), SoundEvents.ENDERMAN_TELEPORT, 1.0f, 0.5f);
                    Strike.effect(p, "minecraft:slowness", WhipNumbers.DIMENSION_DELAY, 3);
                    Strike.later(p, WhipNumbers.DIMENSION_DELAY, cx -> lashAll(cx, foes));
                    return true;
                });
    }

    private static void lashAll(HeroCtx c, List<LivingEntity> foes) {
        boolean control = c.control();
        float damage = (float) WhipNumbers.DIMENSION_WHIP.raw();
        int n = 0;
        for (LivingEntity t : foes) {
            if (!t.isAlive() || t.level() != c.level || t.distanceTo(c.player) > WhipNumbers.DIMENSION_RADIUS + 2.0) continue;
            if (Strike.hit(c, t, damage, false)) n++;
            if (BalanceLaw.isBoss(t)) Strike.effect(t, "minecraft:slowness", 20, 2);
            else if (control) hold(c, t, WhipNumbers.DIMENSION_BIND_TICKS);
            else Strike.effect(t, "minecraft:slowness", 40, 2);
            ZogratisVfx.bloodArcs(c.level, hand(c.player), Strike.center(t), 1.0, 10, VIOLET);
        }
        StrikeFx.sound(c.level, c.player.position(), SoundEvents.LIGHTNING_BOLT_IMPACT, 0.8f, 1.5f);
        c.say(n == 0 ? "Dimension Whip: nothing was left in reach." : "Dimension Whip: " + n + " foes lashed" + (control ? " and bound." : "; control was not ready, so they are only slowed."));
    }
}
