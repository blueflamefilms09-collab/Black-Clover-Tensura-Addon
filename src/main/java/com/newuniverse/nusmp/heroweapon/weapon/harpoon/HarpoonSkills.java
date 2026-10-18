package com.newuniverse.nusmp.heroweapon.weapon.harpoon;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.harpoon.ReelRule.Reel;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Effects;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Foes;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Moves;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.PlayerState;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Safe;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the bodies of the Vassal Harpoon's techniques, kept apart from {@link HarpoonLogic}. The weapon is the harpoon of Glass' world (the Shield Hero lore: lightning
 * charges, a whale hunter's throw, a bind-and-burn cross, a fish-shaped surge, and a "0" form that counters illegitimate power). Its loop is the line: the throw skewers one
 * creature and ties a tether to it (to a creature, never to a block: the design changes no block), and the techniques that follow act on whatever is tied, so a
 * wielder reels in, shocks, pins or strips it without aiming again. A light creature is reeled to the wielder; a heavy one or a boss pulls the wielder to it
 * ({@link ReelRule}). Damage goes through {@code HeroCtx.hit}, hard control behind {@code HeroCtx.control}, every delayed step through {@link Safe}; no block is changed.
 */
final class HarpoonSkills {
    private HarpoonSkills() {}

    /** The line of each wielder; dropped on logout and when the server stops. */
    static final PlayerState<TetherBook> TETHERS = new PlayerState<>(TetherBook::new);

    private static final int BLUE = 0xFF7FE6FF, DEEP = 0xFF3C8CFF, AQUA = 0xFF40D0E0;
    private static final int BLUE_RGB = 0x7FE6FF;

    /** The creature the line is tied to, if it is alive, in this level and within {@code range}; a dead or vanished one cuts the line. */
    static LivingEntity tied(HeroCtx c, double range) {
        TetherBook book = TETHERS.of(c.player);
        UUID id = book.target(c.level.getGameTime());
        if (id == null) return null;
        LivingEntity t = Foes.byId(c.level, id);
        if (t == null || !t.isAlive()) {
            book.cut();
            return null;
        }
        return t.distanceToSqr(c.player) <= range * range ? t : null;
    }

    private static Vec3 hand(HeroCtx c) {
        return c.eye().add(c.look().scale(0.6)).add(0, -0.25, 0);
    }

    // ================================================================== Harpoon Throw
    /** Skewer the first foe under the crosshair and tie the line to it. With Storm Charged the lightning arcs to one more foe beside it. */
    static boolean harpoonThrow(HeroCtx c) {
        ServerPlayer p = c.player;
        LivingEntity t = Foes.target(p, HarpoonNumbers.THROW_RANGE);
        if (t == null) {
            c.fx(VfxShape.LIGHTNING_SPEAR, hand(c), Foes.wallEnd(p, c.eye(), c.eye().add(c.look().scale(HarpoonNumbers.THROW_RANGE))), BLUE, 8, 0.8f);
            c.say("The harpoon finds no one.");
            return false;
        }
        Vec3 at = t.getBoundingBox().getCenter();
        c.fx(VfxShape.LIGHTNING_SPEAR, hand(c), at, BLUE, 8, 0.9f);
        c.sound(SoundEvents.TRIDENT_THROW.value(), 1.0f, 1.2f);
        c.hit(t, (float) HarpoonNumbers.THROW.raw());
        TETHERS.of(p).tie(t.getUUID(), c.level.getGameTime());
        c.fx(VfxShape.THREAD_STRINGS, hand(c), at, BLUE, 24, 1.0f);
        if (c.form.has("storm_charged")) {
            for (LivingEntity o : Foes.around(p, t.position(), HarpoonNumbers.ARC_RANGE, 3)) {
                if (o == t) continue;
                c.hit(o, (float) HarpoonNumbers.ARC_RAW);
                c.fx(VfxShape.FX_LIGHTNING_ARC, at, o.getBoundingBox().getCenter(), BLUE, 8, 0.8f);
                break;
            }
        }
        return true;
    }

    /** A melee blow with the barbed head on the tied creature keeps the line alive a little longer. */
    static void onHit(HeroCtx c, LivingEntity target) {
        if (c.form.has("barbed_head")) TETHERS.of(c.player).extend(c.level.getGameTime(), target.getUUID());
    }

    /** Every five ticks while the harpoon is out: redraw the line once a second, and let go of a creature that is gone or far away. */
    static void onTick(HeroCtx c) {
        TetherBook book = TETHERS.peek(c.player);
        if (book == null || book.target(c.level.getGameTime()) == null) return;
        LivingEntity t = tied(c, HarpoonNumbers.TETHER_RANGE + 10.0);
        if (t == null) {
            book.cut();
            return;
        }
        if (book.drawDue(c.level.getGameTime(), 20)) c.fx(VfxShape.THREAD_STRINGS, hand(c), t.getBoundingBox().getCenter(), BLUE, 24, 0.8f);
    }

    // ================================================================== Reel In
    /** Pull the tied creature in (a light one), or yourself to it (a heavy one or a boss). Behind the shared control lock. */
    static boolean reelIn(HeroCtx c) {
        ServerPlayer p = c.player;
        LivingEntity t = tied(c, HarpoonNumbers.TETHER_RANGE);
        if (t == null) {
            c.say("Nothing is tied to the harpoon. Throw it first.");
            return false;
        }
        Reel mode = ReelRule.mode(BalanceLaw.isBoss(t), t.getBbWidth(), t.getBbHeight(), t.distanceTo(p), t.getMaxHealth());
        if (mode == Reel.REFUSE) {
            c.say("It is already within reach.");
            return false;
        }
        if (!c.control()) {
            c.say("Your control magic needs " + (BalanceLaw.controlCooldownLeft(p) + 19) / 20 + "s.");
            return false;
        }
        UUID id = t.getUUID();
        boolean[] arrived = {false};
        c.fx(VfxShape.THREAD_STRINGS, hand(c), t.getBoundingBox().getCenter(), BLUE, HarpoonNumbers.REEL_TICKS, 1.2f);
        c.sound(SoundEvents.FISHING_BOBBER_RETRIEVE, 1.2f, 0.8f);
        Safe.zone(c, HarpoonNumbers.REEL_TICKS, 1, "harpoon.reel", false, (f, age) -> {
            LivingEntity target = Foes.byId(f.level, id);
            if (target == null || !target.isAlive()) return false;
            boolean done = mode == Reel.PULL_TARGET
                    ? Moves.pullStep(target, f.player.position(), HarpoonNumbers.REEL_SPEED, HarpoonNumbers.REEL_STOP, 1.0)
                    : Moves.pullStep(f.player, target.position(), HarpoonNumbers.REEL_SPEED, HarpoonNumbers.REEL_STOP, 1.0);
            if (!done) return true;
            if (!arrived[0]) {
                arrived[0] = true;
                f.hit(target, (float) HarpoonNumbers.REEL_IN.raw());
                f.sound(SoundEvents.TRIDENT_HIT, 1.0f, 0.9f);
            }
            return false;
        });
        return true;
    }

    // ================================================================== Lightning Strike Dragon
    /** A charge wreathed in lightning: dash up to eight blocks hitting what is on the way, then turn and come back the same way. */
    static boolean lightningStrikeDragon(HeroCtx c) {
        ServerPlayer p = c.player;
        Vec3 from = p.position();
        Vec3 landed = Moves.dash(p, c.look(), HarpoonNumbers.DRAGON_DASH);
        if (landed == null) {
            c.say("There is no room to charge.");
            return false;
        }
        leg(c, from, landed, HarpoonNumbers.DRAGON.raw());
        Safe.later(c, HarpoonNumbers.DRAGON_TURN, "harpoon.dragon.back", f -> {
            Vec3 here = f.player.position();
            Vec3 back = from.subtract(here);
            Vec3 landedBack = Moves.dash(f.player, back, Math.min(HarpoonNumbers.DRAGON_DASH, Math.sqrt(back.x * back.x + back.z * back.z)));
            if (landedBack != null) leg(f, here, landedBack, HarpoonNumbers.DRAGON_BACK_RAW);
        });
        return true;
    }

    private static void leg(HeroCtx c, Vec3 from, Vec3 to, double raw) {
        Vec3 a = from.add(0, 1, 0), b = to.add(0, 1, 0);
        c.fx(VfxShape.LIGHTNING_SPEAR, a, b, BLUE, 10, 1.2f);
        VfxSpawn.sendFollowing(c.level, VfxShape.LIGHTNING_FIEND, c.player, c.player.position().add(0, 1, 0), BLUE, 14, 1.2f);
        c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.7f, 1.6f);
        for (LivingEntity t : Foes.along(c.player, a, b, HarpoonNumbers.DRAGON_WIDTH, HarpoonNumbers.DRAGON.targets())) {
            c.hit(t, (float) raw);
            c.shove(t, from, 0.5);
        }
    }

    // ================================================================== Whale Hunting Harpoon
    /** The heavy throw: after a visible gathering, one great blow on the tied creature (or the one you look at), worth a quarter more against a big body, that slams it down. */
    static boolean whaleHunting(HeroCtx c) {
        ServerPlayer p = c.player;
        LivingEntity t = tied(c, HarpoonNumbers.WHALE_RANGE);
        if (t == null) t = Foes.target(p, HarpoonNumbers.WHALE_RANGE);
        if (t == null) {
            c.say("Nothing to hunt in reach.");
            return false;
        }
        final LivingEntity prey = t;
        Vec3 foot = prey.position();
        Tells.ring(c.level, foot, HarpoonNumbers.WHALE_RADIUS, BLUE_RGB);
        Tells.column(c.level, foot, 4.0, BLUE_RGB);
        c.fx(VfxShape.LIGHTNING_SPEAR, hand(c), prey.getBoundingBox().getCenter().add(0, 3, 0), BLUE, HarpoonNumbers.WHALE_TELL, 1.2f);
        c.sound(SoundEvents.TRIDENT_THROW.value(), 1.2f, 0.6f);
        Safe.later(c, HarpoonNumbers.WHALE_TELL, "harpoon.whale", f -> {
            if (!prey.isAlive() || prey.distanceToSqr(f.player) > (HarpoonNumbers.WHALE_RANGE + 6) * (HarpoonNumbers.WHALE_RANGE + 6)) return;
            f.hit(prey, (float) HarpoonNumbers.whaleRaw(prey.getMaxHealth()));
            prey.setDeltaMovement(0, -0.8, 0);
            prey.hurtMarked = true;
            if (f.control()) prey.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(prey, HarpoonNumbers.WHALE_STAGGER), 9, false, true));
            f.fx(VfxShape.LIGHTNING_GOD, prey.position(), prey.position(), BLUE, 20, 1.5f);
            f.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.9f, 1.4f);
        });
        return true;
    }

    // ================================================================== Crucifixion
    /** The bind-and-burn cross: the tied creature is pinned under a cross of lightning that burns it three times. Behind the shared control lock. */
    static boolean crucifixion(HeroCtx c) {
        ServerPlayer p = c.player;
        LivingEntity t = tied(c, HarpoonNumbers.TETHER_RANGE - 2.0);
        if (t == null) {
            c.say("Nothing is tied to the harpoon. Throw it first.");
            return false;
        }
        if (!c.control()) {
            c.say("Your control magic needs " + (BalanceLaw.controlCooldownLeft(p) + 19) / 20 + "s.");
            return false;
        }
        int ticks = BalanceLaw.controlTicks(t, HarpoonNumbers.CRUCIFY_TICKS);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9, false, true));
        t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
        t.hurtMarked = true;
        Vec3 mid = t.getBoundingBox().getCenter();
        c.fx(VfxShape.LIGHTNING_SPEAR, mid.add(0, 3, 0), mid.add(0, -1.5, 0), DEEP, 14, 1.2f);
        c.fx(VfxShape.LIGHTNING_SPEAR, mid.add(-2.2, 0.4, 0), mid.add(2.2, 0.4, 0), DEEP, 14, 1.2f);
        VfxSpawn.sendFollowing(c.level, VfxShape.THREAD_COCOON, t, t.position().add(0, t.getBbHeight() * 0.5, 0), BLUE, ticks + 6, Math.max(0.6f, t.getBbHeight() / 1.8f));
        c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.0f, 0.8f);
        UUID id = t.getUUID();
        Safe.zone(c, (HarpoonNumbers.CRUCIFY_PULSES - 1) * HarpoonNumbers.CRUCIFY_EVERY, HarpoonNumbers.CRUCIFY_EVERY, "harpoon.crucify", false, (f, age) -> {
            LivingEntity target = Foes.byId(f.level, id);
            if (target == null || !target.isAlive()) return false;
            f.hit(target, (float) HarpoonNumbers.CRUCIFY_PULSE_RAW);
            Vec3 m = target.getBoundingBox().getCenter();
            f.fx(VfxShape.FX_LIGHTNING_ARC, m.add(0, 3, 0), m, BLUE, 8, 1.0f);
            return true;
        });
        return true;
    }

    // ================================================================== Lightning Shock Harpoon
    /** Send lightning down the line: the tied creature is shocked and the current jumps on to up to three foes near it. */
    static boolean lightningShock(HeroCtx c) {
        ServerPlayer p = c.player;
        LivingEntity t = tied(c, HarpoonNumbers.TETHER_RANGE - 2.0);
        if (t == null) {
            c.say("Nothing is tied to the harpoon. Throw it first.");
            return false;
        }
        c.hit(t, (float) HarpoonNumbers.SHOCK.raw());
        Vec3 last = t.getBoundingBox().getCenter();
        c.fx(VfxShape.FX_LIGHTNING_ARC, hand(c), last, BLUE, 8, 1.0f);
        List<LivingEntity> hopped = new ArrayList<>();
        hopped.add(t);
        for (LivingEntity o : Foes.around(p, t.position(), HarpoonNumbers.SHOCK_ARC_RANGE, 12)) {
            if (hopped.size() > HarpoonNumbers.SHOCK_ARCS) break;
            if (hopped.contains(o)) continue;
            c.hit(o, (float) HarpoonNumbers.SHOCK_ARC_RAW);
            Vec3 at = o.getBoundingBox().getCenter();
            c.fx(VfxShape.FX_LIGHTNING_ARC, last, at, BLUE, 8, 0.8f);
            last = at;
            hopped.add(o);
        }
        c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.9f, 1.5f);
        return true;
    }

    // ================================================================== Harpoon 0
    /** The 0 series (canon: no damage, effective against illegitimate power): strips up to three helpful effects from the tied or aimed creature, and one from each of up to three foes beside it. */
    static boolean harpoonZero(HeroCtx c) {
        ServerPlayer p = c.player;
        LivingEntity t = tied(c, HarpoonNumbers.TETHER_RANGE);
        if (t == null) t = Foes.target(p, HarpoonNumbers.ZERO_RANGE);
        if (t == null) {
            c.say("Nothing to unbind.");
            return false;
        }
        int stripped = Effects.strip(t, true, HarpoonNumbers.ZERO_STRIP).size();
        if (stripped > 0) shatter(c, t);
        int others = 0;
        for (LivingEntity o : Foes.around(p, t.position(), HarpoonNumbers.ZERO_SPLASH_RADIUS, 8)) {
            if (o == t || others >= HarpoonNumbers.ZERO_OTHERS) continue;
            int n = Effects.strip(o, true, HarpoonNumbers.ZERO_STRIP_OTHERS).size();
            if (n > 0) {
                shatter(c, o);
                others++;
                stripped += n;
            }
        }
        if (stripped == 0) {
            c.say("There is nothing borrowed to strip.");
            return false;
        }
        c.sound(SoundEvents.TOTEM_USE, 0.6f, 1.8f);
        c.say("Harpoon 0 strips " + stripped + " effect(s).");
        return true;
    }

    private static void shatter(HeroCtx c, LivingEntity t) {
        Vec3 at = t.getBoundingBox().getCenter();
        c.fx(VfxShape.KOTO_SHATTER, at, c.eye(), BLUE, 20, 1.0f);
    }

    // ================================================================== Brave Fish Blaster
    /** The ultimate: after a long, visible gathering, a surge of lightning-charged water in the shape of a great fish along the line you aim, hurting everything in it. */
    static boolean braveFishBlaster(HeroCtx c) {
        ServerPlayer p = c.player;
        Vec3 eye = c.eye(), look = c.look();
        Vec3 end = Foes.wallEnd(p, eye, eye.add(look.scale(HarpoonNumbers.FISH_LENGTH)));
        Tells.line(c.level, eye.add(look), end, BLUE_RGB);
        Tells.gather(c.level, eye.add(look.scale(1.5)), 1.0, BLUE_RGB, 14);
        c.sound(SoundEvents.BEACON_ACTIVATE, 1.2f, 1.6f);
        Safe.later(c, HarpoonNumbers.FISH_TELL, "harpoon.fish", f -> {
            Vec3 e2 = f.eye(), l2 = f.look();
            Vec3 from = e2.add(l2.scale(1.2));
            Vec3 stop = Foes.wallEnd(f.player, e2, e2.add(l2.scale(HarpoonNumbers.FISH_LENGTH)));
            f.fx(VfxShape.WATER_DRAGON, from, stop, AQUA, 14, 2.0f);
            f.fx(VfxShape.LIGHTNING_SPEAR, from, stop, BLUE, 10, 1.5f);
            f.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.1f, 1.2f);
            List<LivingEntity> hit = Foes.along(f.player, from, stop, HarpoonNumbers.FISH_WIDTH, HarpoonNumbers.FISH_BLASTER.targets());
            boolean stagger = !hit.isEmpty() && f.control();
            for (LivingEntity t : hit) {
                f.hit(t, (float) HarpoonNumbers.FISH_BLASTER.raw());
                if (stagger) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, HarpoonNumbers.FISH_STAGGER), 9, false, true));
            }
        });
        return true;
    }
}
