package com.newuniverse.nusmp.elite.items.weapon;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.elite.items.EliteItemsConfig;
import com.newuniverse.nusmp.elite.items.api.EliteItemsApi;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Why: the six technique kinds the ability files can name, run on the server. Every number comes from the {@link AbilitySpec}; the config scales damage; the wielder's paired skill (the Void Edge's)
 * and the slotted runes (the Runic Swords') multiply it. Damage goes through the balance law like every other technique of the mod ({@code BalanceLaw.damage}, so it grows with the wielder's EP and
 * is capped on players), allies and the wielder are never hit ({@code GrimoireBook.around/along} skip them), and the effects are put on with the mod's own VFX only: the mod's VFX shapes and Light /
 * Dark Rukh (no vanilla particles). A technique that cannot happen (no foe, no room) returns false and the caller does not start the cooldown.
 */
public final class AbilityRunner {
    private AbilityRunner() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** Everything a run needs besides the spec. */
    public record Ctx(ServerPlayer player, WeaponAbilities abilities, int runes) {}

    /** Runs a technique. Returns true when it happened. */
    public static boolean run(Ctx c, AbilitySpec s) {
        try {
            return switch (s.kind()) {
                case "line_slash" -> lineSlash(c, s);
                case "chain_marks" -> chainMarks(c, s);
                case "rift_pull" -> riftPull(c, s);
                case "blink" -> blink(c, s);
                case "nova" -> nova(c, s);
                case "ward" -> ward(c, s);
                default -> false;
            };
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] elite weapon technique {} failed", s.kind(), ex);
            return false;
        }
    }

    // ------------------------------------------------------------------ numbers
    /** The damage of a technique for this wielder: base x config scale x paired-skill multiplier x (1 + runes x rune scale). */
    static float damage(Ctx c, AbilitySpec s, float base) {
        double v = base * EliteItemsConfig.get(EliteItemsConfig.ABILITY_DAMAGE_SCALE) * (1.0 + c.runes() * s.runeScale());
        if (!c.abilities().skill().isEmpty() && EliteItemsApi.hasSkill(c.player(), c.abilities().skill())) v *= c.abilities().skillMultiplier();
        return (float) v;
    }

    static void hit(Ctx c, LivingEntity t, float raw) {
        ServerPlayer p = c.player();
        if (t == p || t.isAlliedTo(p) || raw <= 0f) return;
        t.hurt(p.damageSources().playerAttack(p), BalanceLaw.damage(p, t, raw, 0.6));
    }

    static void effects(LivingEntity t, List<AbilitySpec.EffectSpec> list) {
        for (AbilitySpec.EffectSpec e : list) {
            MobEffectInstance inst = instance(e);
            if (inst != null) t.addEffect(inst);
        }
    }

    static MobEffectInstance instance(AbilitySpec.EffectSpec e) {
        ResourceLocation rl = ResourceLocation.tryParse(e.id());
        if (rl == null) return null;
        return BuiltInRegistries.MOB_EFFECT.getHolder(rl).map(h -> new MobEffectInstance(h, e.ticks(), e.amp(), false, true, true)).orElse(null);
    }

    static void strip(LivingEntity t, int n) {
        for (int i = 0; i < n; i++) {
            for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) {
                if (e.getEffect().value().isBeneficial()) { t.removeEffect(e.getEffect()); break; }
            }
        }
    }

    static int argb(AbilitySpec s) { return 0xFF000000 | s.color(); }

    static VfxShape shape(AbilitySpec s, VfxShape def) {
        if (s.vfx().isEmpty()) return def;
        try {
            return VfxShape.valueOf(s.vfx());
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }

    static RukhFx.Alignment align(AbilitySpec s) { return RukhFx.Alignment.of(s.dark()); }

    // ------------------------------------------------------------------ line_slash
    private static boolean lineSlash(Ctx c, AbilitySpec s) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        Vec3 end = eye.add(look.scale(s.range()));
        BlockHitResult bhr = sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (bhr.getType() != HitResult.Type.MISS) end = bhr.getLocation();
        float raw = damage(c, s, s.damage());
        for (LivingEntity t : GrimoireBook.along(p, eye, end, s.width())) {
            hit(c, t, raw);
            effects(t, s.foeEffects());
            strip(t, s.strip());
        }
        Vec3 start = eye.add(look.scale(0.6));
        VfxSpawn.send(sl, shape(s, VfxShape.WIND_SLASH), start, end, argb(s), 16, (float) Math.max(1.0, s.width()));
        RukhFx.burst(sl, end, align(s), 6, 0.6);
        RukhFx.burst(sl, start.add(end).scale(0.5), align(s), 4, 0.6);
        return true;
    }

    // ------------------------------------------------------------------ chain_marks
    private static boolean chainMarks(Ctx c, AbilitySpec s) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        Vec3 origin = p.position();
        List<LivingEntity> foes = new ArrayList<>(GrimoireBook.around(p, origin, s.range()));
        foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(origin)));
        if (foes.isEmpty()) {
            GrimoireBook.fail(p, "No foe in reach.");
            return false;
        }
        float raw = damage(c, s, s.damage());
        int n = Math.min(s.count(), foes.size());
        Vec3[] prev = {p.getEyePosition()};
        for (int i = 0; i < n; i++) {
            LivingEntity t = foes.get(i);
            SpellRuntime.later(sl, 3 + 4 * i, () -> {
                if (!t.isAlive() || p.isRemoved()) return;
                Vec3 at = t.getBoundingBox().getCenter();
                hit(c, t, raw);
                effects(t, s.foeEffects());
                strip(t, s.strip());
                VfxSpawn.send(sl, shape(s, VfxShape.THREAD_LINE), prev[0], at, argb(s), 10, 1.0f);
                RukhFx.burst(sl, at, align(s), 4, 0.4);
                prev[0] = at;
            });
        }
        return true;
    }

    // ------------------------------------------------------------------ rift_pull
    private static boolean riftPull(Ctx c, AbilitySpec s) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        Vec3 center = GrimoireBook.aim(p, s.range());
        VfxSpawn.send(sl, shape(s, VfxShape.SPATIAL_RIFT), center, center.add(0, 1, 0), argb(s), 40, (float) Math.max(0.8, s.radius() / 3.0));
        int pulses = Math.max(1, s.count());
        float raw = damage(c, s, s.damage()) / pulses;
        for (int k = 0; k < pulses; k++) {
            final boolean last = k == pulses - 1;
            SpellRuntime.later(sl, 6 + 10 * k, () -> {
                if (p.isRemoved()) return;
                for (LivingEntity t : GrimoireBook.around(p, center, s.radius())) {
                    Vec3 to = center.subtract(t.position());
                    if (to.lengthSqr() > 0.04 && s.pull() > 0) {
                        t.setDeltaMovement(t.getDeltaMovement().add(to.normalize().scale(s.pull())));
                        t.hurtMarked = true;
                    }
                    hit(c, t, raw);
                    if (last) {
                        effects(t, s.foeEffects());
                        strip(t, s.strip());
                    }
                }
                RukhFx.burst(sl, center.add(0, 0.8, 0), align(s), 6, 0.7);
            });
        }
        return true;
    }

    // ------------------------------------------------------------------ blink
    private static boolean blink(Ctx c, AbilitySpec s) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        Vec3 from = p.position();
        Vec3 dir = p.getViewVector(1f).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(0, 0, 1);
        dir = dir.normalize();
        Vec3 dest = null;
        for (double d = s.range(); d >= 1.0; d -= 0.5) {
            Vec3 cand = from.add(dir.scale(d));
            if (sl.noCollision(p, p.getBoundingBox().move(cand.subtract(from)))) { dest = cand; break; }
        }
        if (dest == null) {
            GrimoireBook.fail(p, "No room to move.");
            return false;
        }
        float raw = damage(c, s, s.damage());
        Vec3 a = from.add(0, 1, 0), b = dest.add(0, 1, 0);
        for (LivingEntity t : GrimoireBook.along(p, a, b, s.width())) {
            hit(c, t, raw);
            effects(t, s.foeEffects());
            strip(t, s.strip());
        }
        VfxSpawn.send(sl, VfxShape.SPATIAL_RIFT, from, from.add(0, 1, 0), argb(s), 14, 0.7f);
        VfxSpawn.send(sl, shape(s, VfxShape.WIND_SLASH), a, b, argb(s), 12, (float) Math.max(1.0, s.width()));
        RukhFx.burst(sl, from.add(0, 1, 0), align(s), 5, 0.5);
        p.teleportTo(dest.x, dest.y, dest.z);
        p.fallDistance = 0f;
        RukhFx.burst(sl, b, align(s), 5, 0.5);
        return true;
    }

    // ------------------------------------------------------------------ nova
    private static boolean nova(Ctx c, AbilitySpec s) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        Vec3 o = p.position();
        float raw = damage(c, s, s.damage());
        for (LivingEntity t : GrimoireBook.around(p, o, s.radius())) {
            hit(c, t, raw);
            effects(t, s.foeEffects());
            strip(t, s.strip());
            if (s.knockback() > 0) {
                Vec3 away = t.position().subtract(o).multiply(1, 0, 1);
                if (away.lengthSqr() > 1e-4) t.knockback(s.knockback(), -away.x, -away.z);
            }
        }
        giveSelf(c, s, s.radius());
        if (s.heal() > 0) p.heal(s.heal() * (float) (1.0 + c.runes() * s.runeScale()));
        VfxSpawn.send(sl, shape(s, VfxShape.WIND_RING), o.add(0, 0.3, 0), o.add(0, 1, 0), argb(s), 22, (float) Math.max(0.8, s.radius() / 3.0 + 0.4));
        RukhFx.burst(sl, o.add(0, 1, 0), align(s), 10, Math.min(2.5, s.radius() / 2.0));
        return true;
    }

    // ------------------------------------------------------------------ ward
    private static boolean ward(Ctx c, AbilitySpec s) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        Vec3 o = p.position();
        float absorb = s.heal() * (float) (1.0 + c.runes() * s.runeScale());
        if (absorb > 0) p.setAbsorptionAmount(Math.max(p.getAbsorptionAmount(), absorb));
        giveSelf(c, s, 0);
        for (Player ally : sl.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(s.radius()), e -> e != p && e.isAlive() && e.isAlliedTo(p))) {
            if (absorb > 0) ally.setAbsorptionAmount(Math.max(ally.getAbsorptionAmount(), absorb * 0.5f));
            effects(ally, s.selfEffects());
        }
        VfxSpawn.send(sl, shape(s, VfxShape.MAGIC_CIRCLE), o.add(0, 0.1, 0), o.add(0, 1, 0), argb(s), 40, (float) Math.max(0.9, s.radius() / 3.0));
        RukhFx.burst(sl, o.add(0, 1, 0), align(s), 8, 1.0);
        return true;
    }

    /** The wielder's own effects, and (for a nova with a radius) allied players near them. */
    private static void giveSelf(Ctx c, AbilitySpec s, double allyRadius) {
        ServerPlayer p = c.player();
        effects(p, s.selfEffects());
        if (allyRadius > 0 && !s.selfEffects().isEmpty()) {
            for (Player ally : p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(allyRadius), e -> e != p && e.isAlive() && e.isAlliedTo(p))) effects(ally, s.selfEffects());
        }
    }

    /** The full-swing strike: extra damage, effects, strip, and a little health back. */
    public static void swing(Ctx c, LivingEntity target) {
        WeaponAbilities w = c.abilities();
        ServerPlayer p = c.player();
        float raw = damage(c, w.primary(), w.swingDamage());
        if (raw > 0) hit(c, target, raw);
        effects(target, w.swingEffects());
        strip(target, w.swingStrip());
        if (w.swingHeal() > 0) p.heal(w.swingHeal());
        Vec3 mid = target.getBoundingBox().getCenter();
        RukhFx.burst(p.serverLevel(), mid, align(w.primary()), 3, 0.3);
    }
}
