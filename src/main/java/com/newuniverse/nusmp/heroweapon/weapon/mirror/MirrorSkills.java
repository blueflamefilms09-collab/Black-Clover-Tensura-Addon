package com.newuniverse.nusmp.heroweapon.weapon.mirror;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.heroweapon.curse.HeroCurse;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the bodies of the Vassal Mirror's techniques, kept apart from {@link MirrorLogic} (which only lists them and wires the hooks) so each can be read on its
 * own. The weapon is the Shield Hero's mirror of Glass' world: magical defence and movement. Its loop is panes of glass and stored light: a Glass Shield
 * stands a pane that turns hostile shots back (and shatters into shards), what is reflected fills the gleam, and the Prism Ray spends it, bouncing off the panes
 * (the canon "Formation: Glass Shield", "Release Rebound", "Stardust Mirror", "Chain Binding Mirror", "Movement Mirror" are the models; the numbers are design).
 * It differs from the Black Clover Mirror Magic grimoire ({@code book.MirrorWorks}) on purpose: no living doubles and no silencing, a pane is a bounded object
 * with a few charges that shatters, the "double" is a silent image that soaks one blow and repeats one technique, and every figure comes from {@link MirrorNumbers}.
 * The Envy techniques (Covet, Ressentiment) belong to the Curse Series that the design gives this weapon; canon ties Envy to a Holy Spear, not to a mirror.
 * Damage goes through {@code HeroCtx.hit}, control through {@code HeroCtx.control}, every delayed step through {@link Safe}; no block is ever changed.
 */
final class MirrorSkills {
    private MirrorSkills() {}

    /** The wielders' working memory (panes, gleam, image, rebound); dropped on logout and when the server stops. */
    static final PlayerState<MirrorLedger> STATE = new PlayerState<>(MirrorLedger::new);

    static final int ICE = 0xFFBFEFFF, AQUA = 0xFF4FD8E8, ENVY = 0xFF2E9F5B;
    private static final int ICE_RGB = 0xBFEFFF;

    static MirrorLedger ledger(HeroCtx c) { return STATE.of(c.player); }

    private static Vec3 flatLook(HeroCtx c) {
        Vec3 look = c.look(), flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
    }

    // ================================================================== Mirror Bash
    /** The edge of the glass: a short cone that hurts, shoves and staggers; the Afterimage repeats it from the image. */
    static boolean bash(HeroCtx c) {
        if (!strike(c, c.eye(), c.look(), 1.0, false)) {
            c.say("Nothing within reach of the glass.");
            return false;
        }
        echoLater(c, "bash", (f, at, dir) -> strike(f, at, dir, MirrorNumbers.AFTERIMAGE_SCALE, true));
        return true;
    }

    private static boolean strike(HeroCtx c, Vec3 origin, Vec3 look, double scale, boolean echo) {
        List<LivingEntity> foes = Foes.coneFrom(c.player, origin, look, MirrorNumbers.BASH_RANGE, MirrorNumbers.BASH_HALF_ANGLE, MirrorNumbers.BASH.targets());
        Vec3 pane = origin.add(look.scale(1.5));
        c.fx(VfxShape.MIRROR_FRAME, pane, pane.add(look), ICE, 10, 0.9f);
        c.sound(echo ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9f, echo ? 1.7f : 1.3f);
        if (foes.isEmpty()) return false;
        MirrorLedger led = ledger(c);
        long now = c.level.getGameTime();
        for (LivingEntity t : foes) {
            if (!c.hit(t, (float) (MirrorNumbers.BASH.raw() * scale))) continue;
            c.shove(t, origin, MirrorNumbers.BASH_KNOCK * scale);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MirrorNumbers.BASH_STAGGER_TICKS, 3, false, true));
            led.gleam.add(now, MirrorNumbers.GLEAM_PER_BASH_HIT);
        }
        return true;
    }

    // ================================================================== Glass Shield (Formation: Glass Shield)
    /** A pane of glass stands in the air ahead: it turns hostile shots back, stores the light, and bursts into shards when spent or out of time. */
    static boolean glassShield(HeroCtx c) {
        MirrorLedger led = ledger(c);
        long now = c.level.getGameTime();
        Vec3 flat = flatLook(c);
        Vec3 centre = c.eye().add(flat.scale(MirrorNumbers.PANE_DISTANCE)).add(0, -0.2, 0);
        for (PaneBook.Pane old : led.panes.makeRoom(now)) shatter(c, old);
        PaneBook.Pane pane = led.panes.place(now, centre.x, centre.y, centre.z, flat.x, 0, flat.z, MirrorNumbers.PANE_RADIUS, MirrorNumbers.PANE_LIFE, MirrorNumbers.PANE_CHARGES);
        c.fx(VfxShape.MIRROR_FRAME, centre, centre.add(flat), ICE, MirrorNumbers.PANE_LIFE, 1.5f);
        c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 1.1f);
        Safe.zone(c, MirrorNumbers.PANE_LIFE, 1, "mirror.pane", false, (f, age) -> paneStep(f, pane));
        return true;
    }

    private static boolean paneStep(HeroCtx f, PaneBook.Pane pane) {
        if (pane.broken) return false;
        long now = f.level.getGameTime();
        if (!pane.standing(now)) {
            pane.broken = true;
            shatter(f, pane);
            return false;
        }
        AABB box = new AABB(pane.x, pane.y, pane.z, pane.x, pane.y, pane.z).inflate(pane.radius + 2.5);
        for (Projectile pr : Deflect.hostileShots(f.player, box)) {
            Vec3 p1 = pr.position(), v = pr.getDeltaMovement(), p0 = p1.subtract(v);
            if (!Geo.sweepsDisc(pane.x, pane.y, pane.z, pane.nx, pane.ny, pane.nz, pane.radius, MirrorNumbers.PANE_THICK, p0.x, p0.y, p0.z, p1.x, p1.y, p1.z)) continue;
            Deflect.turnBack(f.player, pr);
            ledger(f).gleam.add(now, MirrorNumbers.GLEAM_PER_CATCH);
            f.fx(VfxShape.MIRROR_FRAME, new Vec3(pane.x, pane.y, pane.z), new Vec3(pane.x + pane.nx, pane.y, pane.z + pane.nz), ICE, 8, 1.5f);
            f.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.8f);
            if (--pane.charges <= 0) {
                pane.broken = true;
                shatter(f, pane);
                return false;
            }
        }
        return true;
    }

    /** The pane bursts: shards cut the foes near it (the canon "fragments attack the enemy"). */
    private static void shatter(HeroCtx c, PaneBook.Pane pane) {
        Vec3 at = new Vec3(pane.x, pane.y, pane.z);
        c.fx(VfxShape.MIRROR_SHATTER, at, at.add(0, 1, 0), ICE, 24, 1.3f);
        c.sound(SoundEvents.GLASS_BREAK, 1.1f, 1.2f);
        MirrorLedger led = ledger(c);
        long now = c.level.getGameTime();
        for (LivingEntity t : Foes.around(c.player, at, MirrorNumbers.SHARD_RADIUS, MirrorNumbers.GLASS_SHIELD.targets())) {
            if (c.hit(t, (float) MirrorNumbers.GLASS_SHIELD.raw())) led.gleam.add(now, MirrorNumbers.GLEAM_PER_SHARD_HIT);
        }
    }

    // ================================================================== Prism Ray
    /** A beam of refracted light after a short charge: worth more with gleam, and it bounces off a pane it passes. */
    static boolean prismRay(HeroCtx c) {
        Vec3 eye = c.eye(), look = c.look();
        Vec3 end = Foes.wallEnd(c.player, eye, eye.add(look.scale(MirrorNumbers.RAY_RANGE)));
        Tells.line(c.level, eye.add(look.scale(1.0)), end, ICE_RGB);
        Vec3 glass = eye.add(look.scale(1.3));
        c.fx(VfxShape.MIRROR_FRAME, glass, glass.add(look), ICE, 10, 0.8f);
        c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.6f);
        Safe.later(c, MirrorNumbers.RAY_WINDUP, "mirror.prism", f -> {
            MirrorLedger led = ledger(f);
            long now = f.level.getGameTime();
            int gleam = led.gleam.spendAll(now);
            float raw = (float) MirrorNumbers.rayDamage(gleam);
            Vec3 eye2 = f.eye(), look2 = f.look();
            Vec3 stop = ray(f, eye2.add(look2.scale(1.2)), look2, raw, true);
            echoLater(f, "ray", (g, at, dir) -> {
                Vec3 toward = stop.subtract(at);
                ray(g, at, toward.lengthSqr() > 1e-4 ? toward.normalize() : dir, (float) (raw * MirrorNumbers.AFTERIMAGE_SCALE), false);
            });
        });
        return true;
    }

    /** One ray from a point along a unit direction; returns where its first leg ended. With {@code bounce} it turns off the first pane it crosses and strikes the nearest other foe. */
    private static Vec3 ray(HeroCtx f, Vec3 from, Vec3 dir, float raw, boolean bounce) {
        ServerPlayer p = f.player;
        long now = f.level.getGameTime();
        Vec3 end = Foes.wallEnd(p, from, from.add(dir.scale(MirrorNumbers.RAY_RANGE)));
        PaneBook.Pane off = null;
        double bestT = 2.0;
        if (bounce) {
            for (PaneBook.Pane pn : ledger(f).panes.live(now)) {
                double d = Geo.segDist(pn.x, pn.y, pn.z, from.x, from.y, from.z, end.x, end.y, end.z);
                double t = Geo.segParam(pn.x, pn.y, pn.z, from.x, from.y, from.z, end.x, end.y, end.z);
                if (d <= pn.radius && t < bestT) {
                    off = pn;
                    bestT = t;
                }
            }
        }
        Vec3 stop = off != null ? new Vec3(off.x, off.y, off.z) : end;
        f.fx(VfxShape.MIRROR_RAY, from, stop, ICE, 12, 1.0f);
        f.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.1f, 1.4f);
        List<LivingEntity> struck = Foes.along(p, from, stop, MirrorNumbers.RAY_WIDTH, MirrorNumbers.PRISM_RAY.targets());
        for (LivingEntity t : struck) f.hit(t, raw);
        if (off != null) {
            List<LivingEntity> others = Foes.around(p, stop, MirrorNumbers.RAY_BOUNCE_RANGE, 8);
            others.removeAll(struck);
            if (!others.isEmpty()) {
                Vec3 to = others.get(0).getBoundingBox().getCenter().subtract(stop).normalize();
                Vec3 end2 = Foes.wallEnd(p, stop, stop.add(to.scale(MirrorNumbers.RAY_BOUNCE_RANGE + 2.0)));
                f.fx(VfxShape.MIRROR_RAY, stop, end2, ICE, 12, 0.9f);
                for (LivingEntity t : Foes.along(p, stop, end2, MirrorNumbers.RAY_WIDTH, 3)) {
                    if (!struck.contains(t)) f.hit(t, (float) (raw * MirrorNumbers.RAY_BOUNCE_SCALE));
                }
            }
        }
        return stop;
    }

    // ================================================================== Stardust Mirror
    /** A barrier of star-glass round an ally (or the wielder and those beside them): Absorption, and the next two hostile shots aimed at each are turned back. */
    static boolean stardust(HeroCtx c) {
        ServerPlayer p = c.player;
        LivingEntity aimed = Foes.allyTarget(p, MirrorNumbers.STARDUST_RANGE);
        Vec3 near = aimed != null ? aimed.position() : p.position();
        List<LivingEntity> group = Foes.alliesAround(p, near, MirrorNumbers.STARDUST_GROUP_RADIUS, MirrorNumbers.STARDUST.targets());
        if (aimed != null && !group.contains(aimed)) group.add(0, aimed);
        if (aimed == null && !group.contains(p)) group.add(0, p);
        while (group.size() > MirrorNumbers.STARDUST.targets()) group.remove(group.size() - 1);
        for (LivingEntity e : group) {
            e.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, MirrorNumbers.STARDUST_TICKS, MirrorNumbers.absorptionAmp(e.getMaxHealth()), false, true));
            VfxSpawn.sendFollowing(c.level, VfxShape.MIRROR_ARRAY, e, e.position().add(0, 1, 0), ICE, MirrorNumbers.STARDUST_TICKS, 3f);
        }
        c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 1.5f);
        int[] charges = new int[group.size()];
        java.util.Arrays.fill(charges, MirrorNumbers.STARDUST_CATCHES);
        Safe.zone(c, MirrorNumbers.STARDUST_TICKS, 1, "mirror.stardust", false, (f, age) -> {
            for (int i = 0; i < group.size(); i++) {
                LivingEntity e = group.get(i);
                if (charges[i] <= 0 || !e.isAlive()) continue;
                for (Projectile pr : Deflect.hostileShots(f.player, e.getBoundingBox().inflate(MirrorNumbers.STARDUST_CATCH_RADIUS))) {
                    if (charges[i] <= 0) break;
                    Deflect.turnBack(f.player, pr);
                    charges[i]--;
                    f.fx(VfxShape.MIRROR_SHATTER, e.getBoundingBox().getCenter(), e.getBoundingBox().getCenter().add(0, 1, 0), ICE, 14, 0.6f);
                }
            }
            return true;
        });
        return true;
    }

    // ================================================================== Release Rebound
    /** For four seconds spells are turned back (half, capped, with a budget); the wielder is also cleansed of two harmful effects, which may be sent on to a foe. */
    static boolean releaseRebound(HeroCtx c) {
        ServerPlayer p = c.player;
        ledger(c).openRebound(c.level.getGameTime(), MirrorNumbers.REBOUND_TICKS, MirrorNumbers.REBOUND_BUDGET);
        List<LivingEntity> near = Foes.around(p, p.position(), MirrorNumbers.REBOUND_RETURN_RANGE, 1);
        LivingEntity foe = near.isEmpty() ? null : near.get(0);
        List<MobEffectInstance> cleansed = Effects.strip(p, false, MirrorNumbers.REBOUND_CLEANSE);
        for (MobEffectInstance e : cleansed) {
            if (foe == null || !"minecraft".equals(Effects.namespace(e)) || !MirrorNumbers.returnable(Effects.path(e), e.getAmplifier())) continue;
            foe.addEffect(new MobEffectInstance(e.getEffect(), Effects.ticks(e, MirrorNumbers.REBOUND_RETURN_TICKS), Math.min(e.getAmplifier(), MirrorNumbers.REBOUND_RETURN_AMP), false, true));
            c.fx(VfxShape.MIRROR_RAY, p.getEyePosition(), foe.getBoundingBox().getCenter(), ICE, 12, 0.8f);
        }
        VfxSpawn.sendFollowing(c.level, VfxShape.MIRROR_ARRAY, p, p.position().add(0, 1, 0), ICE, MirrorNumbers.REBOUND_TICKS, 4f);
        c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.3f, 0.8f);
        c.say(cleansed.isEmpty() ? "Release Rebound: spells are turned back." : "Release Rebound: " + cleansed.size() + " curse(s) turned back.");
        return true;
    }

    private static boolean reflecting;

    /**
     * The wielder is about to take {@code amount}. In order: an image soaks the blow of a creature; the Spell Ward trims magic; an open rebound turns half of a
     * spell back on its caster (capped by the blow cap, the budget, and the damage taken). Returns what gets through.
     */
    static float onDamaged(HeroCtx c, DamageSource source, float amount) {
        if (amount <= 0f || reflecting) return amount;
        MirrorLedger led = STATE.peek(c.player);
        long now = c.level.getGameTime();
        LivingEntity attacker = source.getEntity() instanceof LivingEntity a && a != c.player ? a : null;
        float out = amount;
        if (led != null && attacker != null && led.imageLive(now)) {
            float soaked = MirrorNumbers.imageSoak(out, c.player.getMaxHealth());
            if (soaked > 0f) {
                out -= soaked;
                Vec3 at = new Vec3(led.imageX(), led.imageY() + 1.0, led.imageZ());
                led.clearImage();
                c.fx(VfxShape.MIRROR_SHATTER, at, at.add(0, 1, 0), ICE, 24, 1.2f);
                c.sound(SoundEvents.GLASS_BREAK, 1.0f, 1.0f);
                c.say("Your image takes the blow.");
            }
        }
        boolean magic = AntiMagic.isMagic(source);
        if (magic && c.summoned() && c.form.has("spell_ward")) out = MirrorNumbers.wardCut(out);
        if (led != null && magic && attacker != null && led.reboundOpen(now)) {
            float back = MirrorNumbers.reboundBack(out, led.reboundLeft(now));
            if (back > 0f) {
                led.spendRebound(back);
                out -= back;
                reflecting = true;
                try {
                    attacker.hurt(c.player.damageSources().indirectMagic(c.player, c.player), BalanceLaw.cap(attacker, back));
                } finally {
                    reflecting = false;
                }
                led.gleam.add(now, Math.min(20, Math.round(back * 1.5f)));
                c.fx(VfxShape.MIRROR_RAY, c.player.getEyePosition(), attacker.getBoundingBox().getCenter(), ICE, 10, 0.8f);
            }
        }
        return out;
    }

    // ================================================================== Movement Mirror
    /**
     * Step through the glass: forward up to nine blocks, leaving an image where you stood; cast again while the image stands to step back into its place. The image
     * soaks one blow (see {@link #onDamaged}) and, with the Afterimage, repeats your next Mirror Bash or Prism Ray once.
     */
    static boolean movementMirror(HeroCtx c) {
        ServerPlayer p = c.player;
        MirrorLedger led = ledger(c);
        long now = c.level.getGameTime();
        Vec3 from = p.position();
        if (led.imageLive(now)) {
            Vec3 home = new Vec3(led.imageX(), led.imageY(), led.imageZ());
            Vec3 spot = SafeSpot.firstOk(c.level, p, home);
            if (spot == null) {
                c.say("The image has nowhere to stand.");
                return false;
            }
            led.clearImage();
            c.fx(VfxShape.MIRROR_STEP, from, from.add(0, 2, 0), ICE, 18, 1.8f);
            p.teleportTo(spot.x, spot.y, spot.z);
            p.fallDistance = 0f;
            c.fx(VfxShape.MIRROR_STEP, spot, spot.add(0, 2, 0), ICE, 18, 1.8f);
            c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 1.8f);
            return true;
        }
        Vec3 landed = Moves.dash(p, c.look(), MirrorNumbers.MOVE_RANGE);
        if (landed == null) {
            c.say("There is no room to step.");
            return false;
        }
        led.placeImage(now, from.x, from.y, from.z, p.getYRot(), MirrorNumbers.IMAGE_LIFE);
        Vec3 facing = c.look();
        c.fx(VfxShape.MIRROR_STEP, from, from.add(0, 2, 0), ICE, 18, 1.8f);
        c.fx(VfxShape.MIRROR_FRAME, from.add(0, 1.1, 0), from.add(0, 1.1, 0).add(facing.x, 0, facing.z), AQUA, MirrorNumbers.IMAGE_LIFE, 1.2f);
        c.fx(VfxShape.MIRROR_STEP, landed, landed.add(0, 2, 0), ICE, 18, 1.8f);
        c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 1.6f);
        return true;
    }

    // ================================================================== Chain Binding Mirror
    /** Chains link the standing glass (the panes and the image) and bind up to four foes near it; the shared control lock applies. */
    static boolean chainBinding(HeroCtx c) {
        ServerPlayer p = c.player;
        MirrorLedger led = ledger(c);
        long now = c.level.getGameTime();
        List<Vec3> anchors = new ArrayList<>();
        for (PaneBook.Pane pn : led.panes.live(now)) anchors.add(new Vec3(pn.x, pn.y, pn.z));
        if (led.imageLive(now)) anchors.add(new Vec3(led.imageX(), led.imageY() + 1.0, led.imageZ()));
        if (anchors.isEmpty()) {
            c.say("Raise a Glass Shield first: the chains need glass to hang from.");
            return false;
        }
        List<LivingEntity> foes = new ArrayList<>();
        for (Vec3 a : anchors) {
            for (LivingEntity t : Foes.around(p, a, MirrorNumbers.CHAIN_BIND_RADIUS, MirrorNumbers.CHAIN_BIND_MAX)) if (!foes.contains(t) && foes.size() < MirrorNumbers.CHAIN_BIND_MAX) foes.add(t);
        }
        if (foes.isEmpty()) {
            c.say("No one stands near your glass.");
            return false;
        }
        if (!c.control()) {
            c.say("Your control magic needs " + (BalanceLaw.controlCooldownLeft(p) + 19) / 20 + "s.");
            return false;
        }
        for (LivingEntity t : foes) {
            Vec3 from = anchors.get(0);
            double best = Double.MAX_VALUE;
            for (Vec3 a : anchors) if (a.distanceToSqr(t.position()) < best) {
                best = a.distanceToSqr(t.position());
                from = a;
            }
            int ticks = BalanceLaw.controlTicks(t, MirrorNumbers.CHAIN_BIND_TICKS);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9, false, true));
            t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
            t.hurtMarked = true;
            c.hit(t, (float) MirrorNumbers.CHAIN_BINDING.raw());
            c.fx(VfxShape.CHAIN_FX1, from, t.getBoundingBox().getCenter(), ICE, 10, 1.0f);
            led.gleam.add(now, MirrorNumbers.GLEAM_PER_BIND);
        }
        for (int i = 0; i + 1 < anchors.size(); i++) c.fx(VfxShape.MIRROR_RAY, anchors.get(i), anchors.get(i + 1), AQUA, 14, 0.8f);
        c.sound(SoundEvents.CHAIN_PLACE, 1.2f, 0.9f);
        return true;
    }

    // ================================================================== Kaleidoscope
    /** The ultimate: a ring of six panes round the aimed ground, and six pulses of light in it, once every half second. */
    static boolean kaleidoscope(HeroCtx c) {
        ServerPlayer p = c.player;
        Vec3 centre = Foes.groundBelow(p, c.aim(MirrorNumbers.KALEIDO_RANGE), 8.0);
        Tells.ring(c.level, centre, MirrorNumbers.KALEIDO_RADIUS, ICE_RGB);
        List<Vec3> panes = new ArrayList<>();
        for (int k = 0; k < 6; k++) {
            double a = Math.PI * 2 * k / 6;
            Vec3 at = centre.add(Math.cos(a) * MirrorNumbers.KALEIDO_RING, 1.4, Math.sin(a) * MirrorNumbers.KALEIDO_RING);
            panes.add(at);
            c.fx(VfxShape.MIRROR_FRAME, at, centre.add(0, 1.4, 0), ICE, MirrorNumbers.KALEIDO_TELL + (MirrorNumbers.KALEIDO_PULSES - 1) * MirrorNumbers.KALEIDO_EVERY + 6, 1.4f);
        }
        c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6f, 0.6f);
        Safe.later(c, MirrorNumbers.KALEIDO_TELL, "mirror.kaleidoscope.start", f -> {
            Safe.zone(f, (MirrorNumbers.KALEIDO_PULSES - 1) * MirrorNumbers.KALEIDO_EVERY, MirrorNumbers.KALEIDO_EVERY, "mirror.kaleidoscope", true, (g, age) -> {
                for (LivingEntity t : Foes.around(g.player, centre, MirrorNumbers.KALEIDO_RADIUS, MirrorNumbers.KALEIDOSCOPE.targets())) {
                    if (g.hit(t, (float) MirrorNumbers.KALEIDOSCOPE.raw())) ledger(g).gleam.add(g.level.getGameTime(), MirrorNumbers.GLEAM_PER_PULSE_HIT);
                }
                int i = (age / MirrorNumbers.KALEIDO_EVERY) % panes.size();
                g.fx(VfxShape.MIRROR_RAY, panes.get(i), panes.get((i + 3) % panes.size()), ICE, 8, 1.1f);
                g.fx(VfxShape.MIRROR_RAY, panes.get((i + 1) % panes.size()), panes.get((i + 4) % panes.size()), AQUA, 8, 0.9f);
                g.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 0.8f + 0.1f * (age / MirrorNumbers.KALEIDO_EVERY));
                return true;
            });
        });
        return true;
    }

    // ================================================================== Envy: Covet
    /** Envy: copy up to three of a foe's helpful effects onto yourself for ten seconds at most (strength capped at level III); the foe keeps theirs. */
    static boolean covet(HeroCtx c) {
        ServerPlayer p = c.player;
        LivingEntity t = Foes.target(p, MirrorNumbers.COVET_RANGE);
        if (t == null) {
            c.say("There is no one to covet.");
            return false;
        }
        List<MobEffectInstance> gifts = Effects.strongest(t, true);
        if (gifts.isEmpty()) {
            c.say("They have nothing you want.");
            return false;
        }
        if (!c.pay(MirrorNumbers.COVET_EXTRA_COST)) {
            c.say("Envy costs more than you can pay.");
            return false;
        }
        int taken = 0;
        for (MobEffectInstance e : gifts) {
            if (taken >= MirrorNumbers.COVET_MAX) break;
            p.addEffect(new MobEffectInstance(e.getEffect(), Effects.ticks(e, MirrorNumbers.COVET_TICKS), Math.min(e.getAmplifier(), MirrorNumbers.COVET_AMP_CAP), false, true));
            taken++;
        }
        c.fx(VfxShape.MIRROR_RAY, t.getBoundingBox().getCenter(), p.getEyePosition(), ENVY, 14, 1.0f);
        c.fx(VfxShape.MIRROR_STEP, p.position(), p.position().add(0, 2, 0), ENVY, 16, 1.5f);
        c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 0.5f);
        HeroCurse.addCorruption(p, MirrorNumbers.COVET_CORRUPTION);
        c.say("You covet " + taken + " of their gifts.");
        return true;
    }

    // ================================================================== Envy: Ressentiment
    /** Envy: the creatures near you turn on each other for four seconds (creatures only, never players; the shared control lock applies). */
    static boolean ressentiment(HeroCtx c) {
        ServerPlayer p = c.player;
        List<Mob> mobs = new ArrayList<>();
        for (LivingEntity e : Foes.around(p, p.position(), MirrorNumbers.RESSENT_RADIUS, 12)) {
            if (e instanceof Mob m && !BalanceLaw.isBoss(m) && mobs.size() < MirrorNumbers.RESSENT_MAX) mobs.add(m);
        }
        if (mobs.size() < 2) {
            c.say("Envy needs two creatures to turn on each other.");
            return false;
        }
        if (!c.control()) {
            c.say("Your control magic needs " + (BalanceLaw.controlCooldownLeft(p) + 19) / 20 + "s.");
            return false;
        }
        c.fx(VfxShape.MIRROR_STEP, p.position(), p.position().add(0, 2, 0), ENVY, 20, 2.0f);
        c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 0.4f);
        Safe.zone(c, MirrorNumbers.RESSENT_TICKS, MirrorNumbers.RESSENT_EVERY, "mirror.ressentiment", false, (f, age) -> {
            for (Mob m : mobs) {
                if (!m.isAlive()) continue;
                Mob rival = null;
                double best = Double.MAX_VALUE;
                for (Mob o : mobs) {
                    if (o == m || !o.isAlive()) continue;
                    double d = o.distanceToSqr(m);
                    if (d < best) {
                        best = d;
                        rival = o;
                    }
                }
                if (rival != null) m.setTarget(rival);
            }
            return true;
        });
        HeroCurse.addCorruption(p, MirrorNumbers.RESSENT_CORRUPTION);
        return true;
    }

    // ================================================================== the Afterimage
    private interface EchoBody {
        void run(HeroCtx f, Vec3 at, Vec3 dir);
    }

    /** With the Afterimage passive and a live image that has not yet repeated anything: the image repeats this technique a moment later at half strength, then dissolves. */
    private static void echoLater(HeroCtx c, String what, EchoBody body) {
        if (!c.form.has("afterimage")) return;
        MirrorLedger led = ledger(c);
        long now = c.level.getGameTime();
        if (!led.imageCanEcho(now)) return;
        led.markEchoed();
        Safe.later(c, MirrorNumbers.AFTERIMAGE_DELAY, "mirror.echo." + what, f -> {
            MirrorLedger l = ledger(f);
            if (!l.imageLive(f.level.getGameTime())) return;
            Vec3 from = new Vec3(l.imageX(), l.imageY() + 1.5, l.imageZ());
            List<LivingEntity> near = Foes.around(f.player, from, 10.0, 1);
            Vec3 dir = f.look();
            if (!near.isEmpty()) {
                Vec3 to = near.get(0).getBoundingBox().getCenter().subtract(from);
                if (to.lengthSqr() > 1e-4) dir = to.normalize();
            }
            f.fx(VfxShape.MIRROR_STEP, from.add(0, -1.5, 0), from.add(0, 0.5, 0), AQUA, 14, 1.5f);
            body.run(f, from, dir);
            l.clearImage();
        });
    }
}
