package com.newuniverse.nusmp.manacore;

import com.newuniverse.nusmp.avatar.FtbTeamHelper;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.beastwill.BeastRank;
import com.newuniverse.nusmp.beastwill.BeastWillApi;
import com.newuniverse.nusmp.beastwill.BeastWillCatalog;
import com.newuniverse.nusmp.beastwill.BeastWillConfig;
import com.newuniverse.nusmp.beastwill.BeastWillDef;
import com.newuniverse.nusmp.beastwill.BeastWillManager;
import com.newuniverse.nusmp.beastwill.BeastWillNet;
import com.newuniverse.nusmp.beastwill.BeastWillState;
import com.newuniverse.nusmp.beastwill.BeastWillStore;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.manacore.ManaCoreRuntime.Delayed;
import com.newuniverse.nusmp.manacore.ManaCoreRuntime.LastSecond;
import com.newuniverse.nusmp.manacore.ManaCoreRuntime.Rune;
import com.newuniverse.nusmp.manacore.ManaCoreRuntime.Run;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;
import com.newuniverse.nusmp.shadow.ShadowMana;
import com.newuniverse.nusmp.song.SongTargets;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Why: what the 48 actives of the rank Mana Cores do (3.51.0), as {@link ManaCoreRules.Kind}s: nineteen behaviours that the table parameterises, so a number never lives in two places. The server decides everything: a
 * press arrives through the item's {@code use}, is checked (feature on, rank gate, cooldown, hard-control gate, cost payable) and only then charged, and an active that finds nothing to work on costs nothing.
 * Bounds: at most {@code min(table targets, server cap)} creatures per press, one entity query (radius &le; 30) per press, no entity is spawned, no block is placed or broken, particles go through {@link Fx} (24 per call),
 * the frozen-world kinds reuse {@link TimeStop} (one map entry per frozen entity, released on a timer and breakable by damage) and players and bosses in it are still cut by the shared hard-control caps.
 */
public final class ManaCoreActives {
    private ManaCoreActives() {}

    // ================================================================== entry
    public static boolean use(ServerPlayer p, ManaCoreRules.Core core, int n) { return use(p, core, n, false); }

    /** {@code noCost} skips the magicule check and payment only (for the self-test, whose stand-in has no magicule pool); every other check still runs. */
    static boolean use(ServerPlayer p, ManaCoreRules.Core core, int n, boolean noCost) {
        if (!ManaCoreConfig.enabled() || !p.isAlive() || p.isSpectator()) return false;
        if (!ManaCoreRuntime.allowed(p, core)) {
            say(p, "too_weak", ManaCoreRules.RANK_LABEL[core.rank()], (long) ManaCoreRules.EP_GATE[core.rank()]);
            return false;
        }
        ManaCoreRules.Active a = core.active(n);
        if (a.kind() == ManaCoreRules.Kind.BEAST_FORM) return beastForm(p, core);
        Run r = ManaCoreRuntime.run(p);
        long now = p.level().getGameTime();
        long ready = r.readyAt.getOrDefault(a.key(), 0L);
        if (now < ready) {
            say(p, "cooldown", (ready - now + 19) / 20);
            return false;
        }
        double costMult = core.passive().flag("cost_mult") > 0 ? core.passive().flag("cost_mult") : 1.0;
        boolean free = ManaCoreRules.freeCast(r.casts + 1, core.passive().flag("free_every"));
        double cost = free || noCost ? 0 : a.mp() * costMult * ManaCoreConfig.costScale();
        if (!ShadowMana.canPay(p, cost)) {
            say(p, "no_mp", (long) cost);
            return false;
        }
        if (controls(a) && BalanceLaw.controlCooldownLeft(p) > 0) {
            say(p, "control_cd", BalanceLaw.controlCooldownLeft(p) / 20);
            return false;
        }
        double chain = core.passive().flag("chain_mult") > 0 && now - r.lastCast < 40 ? core.passive().flag("chain_mult") : 1.0;
        if (!perform(p, core, a, r, chain, now)) return false;
        if (controls(a)) BalanceLaw.beginControl(p);
        ShadowMana.pay(p, cost);
        r.casts++;
        r.lastCast = now;
        r.readyAt.put(a.key(), now + (free ? ManaCoreRules.MIN_COOLDOWN : ManaCoreRules.cooldown(a.cd(), ManaCoreConfig.cooldownScale(), core.passive().flag("cd_mult"))));
        return true;
    }

    /** Does this active apply hard control (so the shared 12 s control gate applies to it)? */
    static boolean controls(ManaCoreRules.Active a) {
        switch (a.kind()) {
            case FREEZE_TARGET, FREEZE_AREA, AREA, LAUNCH, LAST_SECOND: return true;
            default: return "minecraft:slowness".equals(a.effect()) && a.amp() >= 3;
        }
    }

    static void say(ServerPlayer p, String key, Object... args) {
        try {
            p.displayClientMessage(Component.translatable("manacore.msg." + key, args), true);
        } catch (RuntimeException ignored) { /* a connectionless stand-in (the self-test) has nobody to tell */ }
    }

    private static boolean perform(ServerPlayer p, ManaCoreRules.Core core, ManaCoreRules.Active a, Run r, double chain, long now) {
        double aoe = core.passive().flag("aoe_mult") > 0 ? core.passive().flag("aoe_mult") : 1.0;
        double radius = Math.min(ManaCoreRules.MAX_RADIUS, a.radius() * aoe);
        int cap = Math.max(1, Math.min(a.targets(), ManaCoreConfig.targetCap()));
        double dmg = ManaCoreRules.POWER[core.rank()] * a.power() * chain;
        return switch (a.kind()) {
            case BUFF -> buff(p, core, a, r, now);
            case BARRIER -> barrier(p, a, r, now);
            case BURST -> a.delay() > 0 ? queue(p, core, a, r, now) : burst(p, p.position(), core, a, radius, cap, dmg);
            case AREA -> area(p, a, r, radius, cap, dmg, now);
            case LINE -> line(p, a, radius, cap, dmg, false);
            case CHAIN -> line(p, a, radius, cap, dmg, true);
            case DASH, BLINK -> dash(p, core, a, radius, cap, dmg);
            case PULL -> pull(p, a, radius, cap, dmg);
            case LAUNCH -> launch(p, a, radius, dmg);
            case LEAP -> leap(p, a);
            case FREEZE_TARGET -> freezeTarget(p, a, radius);
            case FREEZE_AREA -> freezeArea(p, a, radius, cap);
            case ECHO -> echo(p, r);
            case RUNE -> rune(p, r, now);
            case REDIRECT -> redirect(p, radius, cap);
            case CLEANSE -> cleanse(p, radius, cap);
            case LAST_SECOND -> lastSecond(p, r, now);
            default -> false;
        };
    }

    // ================================================================== targets
    static boolean allowed(ServerPlayer p, LivingEntity t) {
        if (!SongTargets.isFoe(p, t)) return false;
        if (t instanceof Player o) {
            if (!ManaCoreConfig.hitPlayers()) return false;
            if (FtbTeamHelper.sameTeam(p.getUUID(), o.getUUID())) return false;
            if (!p.serverLevel().mayInteract(p, o.blockPosition())) return false;
        }
        return true;
    }

    static boolean bossy(LivingEntity t) { return BossIdentity.isBoss(t) || BalanceLaw.isBoss(t); }

    private static List<LivingEntity> around(ServerPlayer p, Vec3 c, double radius, int max) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : SongTargets.foes(p, c, Math.max(0.5, radius))) if (allowed(p, t)) out.add(t);
        out.sort(Comparator.comparingDouble(t -> t.distanceToSqr(c)));
        return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
    }

    private static List<LivingEntity> cone(ServerPlayer p, double range, double minDot, int max) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : SongTargets.foes(p, p.position(), Math.max(0.5, range))) {
            if (!allowed(p, t)) continue;
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            double len = to.length();
            if (len < 1.0e-4 || to.scale(1.0 / len).dot(look) < minDot) continue;
            out.add(t);
        }
        out.sort(Comparator.comparingDouble(t -> t.distanceToSqr(p)));
        return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
    }

    /** The point the player looks at (up to {@code range}); the block hit or the end of the ray. */
    static Vec3 lookPoint(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(range));
        var hit = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    // ================================================================== damage and effects
    static float hurt(ServerPlayer p, LivingEntity t, double raw) {
        if (raw <= 0) return 0f;
        float d = BalanceLaw.damage(p, t, (float) raw, 0.5);
        if (d <= 0.01f) return 0f;
        t.hurt(p.damageSources().playerAttack(p), d);
        return d;
    }

    private static Holder<MobEffect> holder(String id) { return id == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id)).orElse(null); }

    /** The active's status effect on a foe: "fire" ignites; a hard slow goes through the shared control rules. */
    private static void tag(LivingEntity t, ManaCoreRules.Active a) {
        if (a.effect() == null) return;
        if ("fire".equals(a.effect())) {
            t.igniteForSeconds(Math.max(1, a.dur() / 20f));
            return;
        }
        Holder<MobEffect> h = holder(a.effect());
        if (h == null) return;
        boolean hard = "minecraft:slowness".equals(a.effect()) && a.amp() >= 3;
        int ticks = hard ? BalanceLaw.controlTicks(t, a.dur()) : a.dur();
        if (bossy(t) && hard) ticks = Math.min(ticks, 20);
        if (ticks >= 5) t.addEffect(new MobEffectInstance(h, ticks, a.amp(), false, true));
    }

    private static void fx(ServerLevel level, Vec3 at, int want, double spread, boolean dark) {
        Fx.spray(level, FxThemes.SKILL, FxTone.of(dark), at.x, at.y, at.z, Math.min(want, 24), spread, spread, spread, 0.02);
    }

    private static void ring(ServerLevel level, Vec3 c, double radius, boolean dark) {
        for (int i = 0; i < 12; i++) {
            double ang = i * Math.PI / 6.0;
            fx(level, c.add(Math.cos(ang) * radius, 0.4, Math.sin(ang) * radius), 1, 0.05, dark);
        }
    }

    // ================================================================== kinds
    private static boolean buff(ServerPlayer p, ManaCoreRules.Core core, ManaCoreRules.Active a, Run r, long now) {
        if ("overclock_pulse".equals(a.key())) {
            if (p.getHealth() <= ManaCoreRules.OVERCLOCK_SELF_DAMAGE * 2 && !p.isCreative()) {
                say(p, "low_health");
                return false;
            }
            if (!p.isCreative()) p.hurt(p.damageSources().magic(), ManaCoreRules.OVERCLOCK_SELF_DAMAGE);
        }
        Holder<MobEffect> h = holder(a.effect());
        if (h != null) p.addEffect(new MobEffectInstance(h, Math.max(20, a.dur()), a.amp(), false, true));
        if ("core_spark".equals(a.key())) p.addEffect(new MobEffectInstance(MobEffects.JUMP, a.dur(), 0, false, true));
        if ("elemental_infusion".equals(a.key())) r.infuseUntil = now + 160;
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.2f);
        fx(p.serverLevel(), p.position().add(0, 1, 0), 14, 0.5, false);
        return true;
    }

    private static boolean barrier(ServerPlayer p, ManaCoreRules.Active a, Run r, long now) {
        r.barrierUntil = now + a.dur();
        Holder<MobEffect> h = holder(a.effect());
        if (h != null) p.addEffect(new MobEffectInstance(h, a.dur() + 40, a.amp(), false, true));
        p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
        ring(p.serverLevel(), p.position(), 1.6, false);
        return true;
    }

    /** Hits what stands in a sphere, pushes it out, applies the effect. Returns false when nothing was there (nothing is charged). */
    private static boolean burst(ServerPlayer p, Vec3 center, ManaCoreRules.Core core, ManaCoreRules.Active a, double radius, int cap, double dmg) {
        List<LivingEntity> foes = around(p, center, radius, cap);
        boolean clears = "mana_burst".equals(a.key());
        int cleared = clears ? clearProjectiles(p, center, radius) : 0;
        if (foes.isEmpty() && cleared == 0 && !"apex_roar".equals(a.key())) {
            say(p, "no_target");
            return false;
        }
        for (LivingEntity t : foes) {
            hurt(p, t, dmg);
            Vec3 away = t.position().subtract(center).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0e-4 && !bossy(t)) {
                away = away.normalize().scale(0.9);
                t.push(away.x, 0.3, away.z);
                t.hurtMarked = true;
            }
            tag(t, a);
        }
        ServerLevel level = p.serverLevel();
        ring(level, center, radius, core.rank() >= 5);
        fx(level, center.add(0, 1, 0), 18, Math.min(radius, 3), core.rank() >= 5);
        level.playSound(null, p.blockPosition(), core.rank() >= 6 ? SoundEvents.GENERIC_EXPLODE.value() : SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 0.9f);
        return true;
    }

    private static int clearProjectiles(ServerPlayer p, Vec3 c, double radius) {
        int n = 0;
        for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, ManaCoreRuntime.box(c, radius), x -> x.isAlive() && x.getOwner() != p)) {
            pr.discard();
            if (++n >= 8) break;
        }
        return n;
    }

    /** A burst that lands later (the constellation and the singularity): remembered per player, at most two, cancelled on death, logout, dimension change. */
    private static boolean queue(ServerPlayer p, ManaCoreRules.Core core, ManaCoreRules.Active a, Run r, long now) {
        if (r.delayed.size() >= 2) {
            say(p, "busy");
            return false;
        }
        r.delayed.add(new Delayed(now + a.delay(), p.position(), core.id(), a == core.a1() ? 1 : 2, p.level().dimension().location().toString()));
        ring(p.serverLevel(), p.position(), a.radius() * 0.5, true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 0.6f);
        return true;
    }

    private static boolean area(ServerPlayer p, ManaCoreRules.Active a, Run r, double radius, int cap, double dmg, long now) {
        List<LivingEntity> foes = around(p, p.position(), radius, cap);
        if (foes.isEmpty() && !"static_field".equals(a.key())) {
            say(p, "no_target");
            return false;
        }
        for (LivingEntity t : foes) {
            hurt(p, t, dmg);
            tag(t, a);
            if (!"static_field".equals(a.key())) t.addEffect(new MobEffectInstance(MobEffects.GLOWING, a.dur(), 0, false, false));
        }
        if ("static_field".equals(a.key())) r.thornsUntil = now + a.dur();
        ring(p.serverLevel(), p.position(), radius, true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9f, 0.7f);
        return true;
    }

    /** A line along the look; with {@code hop} it leaps from foe to foe (chain lightning) or, for the prism matrix, picks the nearest foes outright. */
    private static boolean line(ServerPlayer p, ManaCoreRules.Active a, double range, int cap, double dmg, boolean hop) {
        List<LivingEntity> hit;
        if (hop) {
            hit = new ArrayList<>();
            boolean matrix = "prismatic_beam_matrix".equals(a.key());
            if (matrix) hit = around(p, p.position(), range, cap);
            else {
                List<LivingEntity> first = cone(p, 20, 0.85, 1);
                if (!first.isEmpty()) {
                    hit.add(first.get(0));
                    while (hit.size() < cap) {
                        LivingEntity last = hit.get(hit.size() - 1), next = null;
                        double best = range * range;
                        for (LivingEntity t : around(p, last.position(), range, 12)) {
                            double d = t.distanceToSqr(last);
                            if (!hit.contains(t) && d < best) {
                                best = d;
                                next = t;
                            }
                        }
                        if (next == null) break;
                        hit.add(next);
                    }
                }
            }
        } else {
            hit = cone(p, range, 0.93, cap);
        }
        if (hit.isEmpty()) {
            say(p, "no_target");
            return false;
        }
        ServerLevel level = p.serverLevel();
        Vec3 from = p.getEyePosition().add(0, -0.3, 0);
        for (LivingEntity t : hit) {
            hurt(p, t, dmg);
            tag(t, a);
            Vec3 to = t.getBoundingBox().getCenter();
            for (int i = 0; i <= 6; i++) fx(level, from.add(to.subtract(from).scale(i / 6.0)), 1, 0.05, p.getRandom().nextBoolean());
            from = hop && !"prismatic_beam_matrix".equals(a.key()) ? to : p.getEyePosition().add(0, -0.3, 0);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.25f, 1.8f);
        return true;
    }

    /** A dash or blink along the look: stops at the first wall, hurts foes near the path. */
    private static boolean dash(ServerPlayer p, ManaCoreRules.Core core, ManaCoreRules.Active a, double dist, int cap, double dmg) {
        ServerLevel level = p.serverLevel();
        Vec3 look = a.kind() == ManaCoreRules.Kind.BLINK ? p.getLookAngle() : p.getLookAngle().multiply(1, 0.2, 1).normalize();
        Vec3 start = p.position(), pos = start;
        for (double d = 0.5; d <= dist; d += 0.5) {
            Vec3 next = start.add(look.scale(d));
            if (!level.noCollision(p, p.getBoundingBox().move(next.subtract(start)))) break;
            pos = next;
        }
        if (pos.distanceToSqr(start) < 1.0) {
            say(p, "blocked");
            return false;
        }
        List<LivingEntity> hurt = new ArrayList<>();
        for (int i = 0; i <= 6 && hurt.size() < cap; i++) {
            Vec3 at = start.add(pos.subtract(start).scale(i / 6.0));
            for (LivingEntity t : around(p, at, 2.2, cap)) if (!hurt.contains(t) && hurt.size() < cap) hurt.add(t);
            fx(level, at.add(0, 1, 0), 3, 0.2, core.rank() >= 4);
        }
        for (LivingEntity t : hurt) {
            hurt(p, t, dmg);
            tag(t, a);
        }
        p.teleportTo(level, pos.x, pos.y, pos.z, p.getYRot(), p.getXRot());
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0;
        p.hurtMarked = true;
        level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.3f);
        return true;
    }

    private static boolean pull(ServerPlayer p, ManaCoreRules.Active a, double radius, int cap, double dmg) {
        Vec3 c = lookPoint(p, 16);
        List<LivingEntity> foes = around(p, c, radius, cap);
        if (foes.isEmpty()) {
            say(p, "no_target");
            return false;
        }
        for (LivingEntity t : foes) {
            Vec3 d = c.subtract(t.position());
            if (d.lengthSqr() > 1.0e-4 && !bossy(t)) {
                Vec3 v = d.normalize().scale(Math.min(1.6, 0.4 + d.length() * 0.15));
                t.setDeltaMovement(v.x, Math.max(0.15, v.y), v.z);
                t.hurtMarked = true;
            }
            hurt(p, t, dmg);
        }
        ring(p.serverLevel(), c, radius, true);
        fx(p.serverLevel(), c, 18, 1.5, true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 0.9f, 0.5f);
        return true;
    }

    private static boolean launch(ServerPlayer p, ManaCoreRules.Active a, double range, double dmg) {
        for (LivingEntity t : cone(p, range, 0.9, 4)) {
            if (bossy(t)) continue;
            hurt(p, t, dmg);
            t.setDeltaMovement(t.getDeltaMovement().x, 1.4, t.getDeltaMovement().z);
            t.hurtMarked = true;
            tag(t, a);
            fx(p.serverLevel(), t.position().add(0, 1, 0), 12, 0.4, true);
            return true;
        }
        say(p, "no_target");
        return false;
    }

    private static boolean leap(ServerPlayer p, ManaCoreRules.Active a) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        Vec3 v = look.lengthSqr() > 1.0e-4 ? look.normalize().scale(0.4) : Vec3.ZERO;
        p.setDeltaMovement(v.x, 1.3, v.z);
        p.hurtMarked = true;
        Holder<MobEffect> h = holder(a.effect());
        if (h != null) p.addEffect(new MobEffectInstance(h, a.dur(), 0, false, false));
        fx(p.serverLevel(), p.position(), 14, 0.5, false);
        p.level().playSound(null, p.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.8f, 1.2f);
        return true;
    }

    private static boolean freezeTarget(ServerPlayer p, ManaCoreRules.Active a, double range) {
        List<LivingEntity> t = cone(p, range, 0.93, 1);
        if (t.isEmpty()) {
            say(p, "no_target");
            return false;
        }
        LivingEntity v = t.get(0);
        int ticks = BalanceLaw.controlTicks(v, a.dur());
        if (ticks < 5) return false;
        TimeStop.freeze(v, ticks);
        fx(p.serverLevel(), v.position().add(0, 1, 0), 16, 0.5, true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0f, 0.6f);
        return true;
    }

    /**
     * The frozen world (World Stills 4 s, World of Still Seconds 10 s): foes, other creatures that are not allies, projectiles and falling blocks in a sphere stand still through {@link TimeStop}, the user is immune. A mob,
     * projectile or block gets the full time; a player and a boss are still cut by the shared hard-control caps (3 s and 1 s), so a core can never lock a player or a boss for long.
     */
    private static boolean freezeArea(ServerPlayer p, ManaCoreRules.Active a, double radius, int cap) {
        ServerLevel level = p.serverLevel();
        int frozen = 0;
        for (Entity e : level.getEntities(p, ManaCoreRuntime.box(p.position(), radius), x -> x != p && x.isAlive() && (x instanceof LivingEntity || x instanceof Projectile || x instanceof FallingBlockEntity))) {
            if (frozen >= cap) break;
            if (e.distanceToSqr(p) > radius * radius) continue;
            int ticks = a.dur();
            if (e instanceof LivingEntity l) {
                if (!allowed(p, l)) continue;
                if (l instanceof Player || bossy(l)) ticks = BalanceLaw.controlTicks(l, a.dur());
            } else if (e instanceof Projectile pr && (pr.getOwner() == p || (pr.getOwner() instanceof Player o && FtbTeamHelper.sameTeam(p.getUUID(), o.getUUID())))) continue;
            if (ticks < 5) continue;
            TimeStop.freeze(e, ticks);
            frozen++;
        }
        TimeStop.immune(p, a.dur() + 20);
        ring(level, p.position(), radius, true);
        fx(level, p.position().add(0, 1, 0), 24, 3, true);
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.2f, 0.4f);
        return true;
    }

    private static boolean echo(ServerPlayer p, Run r) {
        return rewindTo(p, r, ManaCoreRules.historyIndex(r.history.size(), 1)) || fail(p, "no_history");
    }

    private static boolean fail(ServerPlayer p, String key) {
        say(p, key);
        return false;
    }

    /** Puts the player back where they stood (the recorded second); false when there is no record for this dimension. */
    static boolean rewindTo(ServerPlayer p, Run r, int index) {
        if (index < 0 || index >= r.history.size() || !p.level().dimension().location().toString().equals(r.histDim)) return false;
        Vec3 to = r.history.get(index);
        fx(p.serverLevel(), p.position().add(0, 1, 0), 12, 0.4, true);
        p.teleportTo(p.serverLevel(), to.x, to.y, to.z, p.getYRot(), p.getXRot());
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0;
        p.hurtMarked = true;
        fx(p.serverLevel(), p.position().add(0, 1, 0), 12, 0.4, true);
        return true;
    }

    private static boolean rune(ServerPlayer p, Run r, long now) {
        while (r.runes.size() >= ManaCoreRules.MAX_RUNES) r.runes.remove(0);
        r.runes.add(new Rune(p.level().dimension().location().toString(), p.position(), now + ManaCoreRules.RUNE_LIFE_TICKS));
        p.level().playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5f, 1.6f);
        return true;
    }

    private static boolean redirect(ServerPlayer p, double radius, int cap) {
        int n = 0;
        List<LivingEntity> foes = around(p, p.position(), 24, 6);
        for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, ManaCoreRuntime.box(p.position(), radius), x -> x.isAlive() && x.getOwner() != p)) {
            if (n >= cap) break;
            LivingEntity to = foes.stream().min(Comparator.comparingDouble(t -> t.distanceToSqr(pr))).orElse(null);
            double speed = Math.min(3.0, Math.max(0.6, pr.getDeltaMovement().length() * 2.0));
            Vec3 dir = to != null ? to.getBoundingBox().getCenter().subtract(pr.position()).normalize() : pr.getDeltaMovement().reverse().normalize();
            pr.setOwner(p);
            pr.setDeltaMovement(dir.scale(speed));
            pr.hurtMarked = true;
            n++;
        }
        if (n == 0) return fail(p, "no_projectile");
        fx(p.serverLevel(), p.position().add(0, 1, 0), 14, 1.0, true);
        return true;
    }

    private static boolean cleanse(ServerPlayer p, double radius, int cap) {
        List<LivingEntity> first = cone(p, radius, 0.9, 1);
        if (first.isEmpty()) return fail(p, "no_target");
        List<LivingEntity> all = around(p, first.get(0).position(), 6, cap);
        for (LivingEntity t : all) {
            List<Holder<MobEffect>> gone = new ArrayList<>();
            for (MobEffectInstance i : t.getActiveEffects()) if (i.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) gone.add(i.getEffect());
            for (Holder<MobEffect> h : gone) t.removeEffect(h);
            t.setAbsorptionAmount(0f);
            fx(p.serverLevel(), t.position().add(0, 1, 0), 8, 0.4, true);
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.9f, 1.5f);
        return true;
    }

    private static boolean lastSecond(ServerPlayer p, Run r, long now) {
        if (r.last != null) return fail(p, "busy");
        r.last = new LastSecond(p.level().dimension().location().toString(), p.position(), now + ManaCoreRules.LAST_SECOND_TICKS);
        p.displayClientMessage(Component.translatable("manacore.msg.last_second"), false);
        p.level().playSound(null, p.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.5f, 0.5f);
        return true;
    }

    // ================================================================== beast cores (A to SS): the existing Beast Will system does the work
    /** Sneak + press on a beast core: offers the matching Beast Will when the player has none, otherwise starts or ends its Integrate form through the Beast Will manager (which checks assimilation and pays). */
    private static boolean beastForm(ServerPlayer p, ManaCoreRules.Core core) {
        if (!BeastWillConfig.enabled() || core.will() == null) return fail(p, "will_off");
        BeastWillDef def = BeastWillCatalog.get(core.will());
        if (def == null) return fail(p, "will_missing");
        BeastWillState s = BeastWillStore.peek(p);
        if (s == null || !s.has(core.will())) {
            BeastRank rank = core.rank() >= 7 ? BeastRank.SS : core.rank() >= 6 ? BeastRank.S : core.rank() >= 4 ? BeastRank.A : core.rank() == 3 ? BeastRank.B : BeastRank.C;
            return BeastWillApi.offer(p, core.will(), core.name().replaceAll(" Core.*$", ""), rank, "");
        }
        if (!core.will().equals(s.active)) return fail(p, "will_swap");
        BeastWillManager.onAction(p, BeastWillNet.OP_FORM, 2);
        return true;
    }

    // ================================================================== per-tick work of a busy player (runes, delayed bursts, the countdown)
    static void tickBusy(ServerPlayer p, Run r) {
        long now = p.level().getGameTime();
        String dim = p.level().dimension().location().toString();
        if (!r.delayed.isEmpty()) {
            for (Iterator<Delayed> it = r.delayed.iterator(); it.hasNext(); ) {
                Delayed d = it.next();
                if (!d.dim().equals(dim)) {
                    it.remove();
                    continue;
                }
                if (now < d.at()) continue;
                it.remove();
                ManaCoreRules.Core core = ManaCoreRules.byId(d.coreId());
                if (core == null) continue;
                ManaCoreRules.Active a = core.active(d.n());
                int cap = Math.max(1, Math.min(a.targets(), ManaCoreConfig.targetCap()));
                burst(p, d.center(), core, a, a.radius(), cap, ManaCoreRules.POWER[core.rank()] * a.power());
            }
        }
        if (!r.runes.isEmpty() && now % 10 == 0) {
            for (Iterator<Rune> it = r.runes.iterator(); it.hasNext(); ) {
                Rune ru = it.next();
                if (now >= ru.expire() || !ru.dim().equals(dim)) {
                    it.remove();
                    continue;
                }
                for (LivingEntity t : around(p, ru.at(), 1.6, 1)) {
                    int ticks = BalanceLaw.controlTicks(t, 60);
                    if (ticks >= 5) TimeStop.freeze(t, ticks);
                    fx(p.serverLevel(), ru.at().add(0, 0.5, 0), 12, 0.4, true);
                    it.remove();
                    break;
                }
            }
        }
        if (r.last != null) countdown(p, r, now, dim);
    }

    /** The 30-second countdown: every half second everything of the zone is lifted (gravity dies), the user is quickened; at the end a damage burst with no block damage. Ends silently on death, logout, dimension change. */
    private static void countdown(ServerPlayer p, Run r, long now, String dim) {
        LastSecond ls = r.last;
        if (!ls.dim().equals(dim) || !p.isAlive()) {
            r.last = null;
            return;
        }
        ServerLevel level = p.serverLevel();
        long left = ls.end() - now;
        if (now % 10 == 0) {
            int n = 0;
            for (LivingEntity t : around(p, ls.center(), ManaCoreRules.LAST_SECOND_RADIUS, Math.min(ManaCoreRules.LAST_SECOND_TARGETS, ManaCoreConfig.targetCap()))) {
                t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0, false, false));
                t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
                n++;
            }
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 2, true, false));
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 30, 2, true, false));
            ring(level, ls.center(), ManaCoreRules.LAST_SECOND_RADIUS * (1.0 - left / (double) ManaCoreRules.LAST_SECOND_TICKS), true);
        }
        if (now % 100 == 0) p.displayClientMessage(Component.translatable("manacore.msg.countdown", Math.max(0, left / 20)), true);
        if (left > 0) return;
        r.last = null;
        ManaCoreRules.Core core = ManaCoreRules.byId(r.applied);
        if (core == null) core = ManaCoreRules.byId("core_ss_ancient");
        ManaCoreRules.Active a = core.a2();
        int cap = Math.min(ManaCoreRules.LAST_SECOND_TARGETS, ManaCoreConfig.targetCap());
        for (LivingEntity t : around(p, ls.center(), ManaCoreRules.LAST_SECOND_RADIUS, cap)) {
            hurt(p, t, ManaCoreRules.POWER[7] * a.power());
            t.removeEffect(MobEffects.LEVITATION);
            Vec3 away = t.position().subtract(ls.center()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0e-4 && !bossy(t)) {
                away = away.normalize().scale(1.4);
                t.push(away.x, 0.6, away.z);
                t.hurtMarked = true;
            }
        }
        ring(level, ls.center(), ManaCoreRules.LAST_SECOND_RADIUS, true);
        fx(level, ls.center().add(0, 1, 0), 24, 6, true);
        level.playSound(null, net.minecraft.core.BlockPos.containing(ls.center()), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.5f);
    }
}
