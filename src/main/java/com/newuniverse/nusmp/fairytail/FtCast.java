package com.newuniverse.nusmp.fairytail;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.rules.Tier;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: this is the one place where a Fairy Tail spell row ({@link FtMove}) becomes something that happens in the world. One routine per
 * {@link FtKind}; they read the numbers of the row, scale the damage through the balance law (the caster's EP, the skill's mastery, the
 * form that is on, the charge of eaten flame), hit only what {@link FtWorld} says may be hurt, send the element's effect trio and give the
 * element's status (burning, frost, shock, rust, shatter, drench). The skill class calls {@link #cast} inside the mod's fault guard
 * ({@code SafeCast}), pays nothing itself and sets the cooldown only when the cast says it happened. Server side only.
 */
public final class FtCast {
    private FtCast() {}

    static final String FORM_UNTIL = "FormUntil", FORM_IDX = "FormIdx", CHARGE = "Charge", CHARGE_AT = "ChargeAt";

    /** What a cast did: whether it happened (cost is spent, cooldown starts) and how many creatures it hurt. */
    public record Outcome(boolean done, int hits) {
        static Outcome fizzle() { return new Outcome(false, 0); }
        static Outcome of(int hits) { return new Outcome(true, hits); }
    }

    // ------------------------------------------------------------------------------------------------------------------ state in the skill's tag
    /** The form that is on right now, or null. */
    public static FtForms activeForm(CompoundTag tag, long now) {
        if (tag == null || now >= tag.getLong(FORM_UNTIL)) return null;
        return FtForms.get(tag.getInt(FORM_IDX));
    }

    public static double charge(CompoundTag tag, long now) {
        return FtRules.decayedCharge(tag.getDouble(CHARGE), now - tag.getLong(CHARGE_AT));
    }

    public static void addCharge(CompoundTag tag, long now, int units) {
        tag.putDouble(CHARGE, FtRules.eatCharge(charge(tag, now), units));
        tag.putLong(CHARGE_AT, now);
    }

    // ------------------------------------------------------------------------------------------------------------------ cost
    /** Pays the spell's cost from the Aura (then the magicules). Creative players and a missing pool pay nothing. */
    public static boolean pay(ServerPlayer p, FtMove mv, Tier tier) {
        if (p.isCreative() || mv.costFrac() <= 0) return true;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return true;
        double cost = FtRules.cost(mv, EnergyHelper.getMaxAura(p), tier);
        if (ex.getAura() >= cost) ex.setAura(ex.getAura() - cost);
        else if (ex.getMagicule() >= cost) ex.setMagicule(ex.getMagicule() - cost);
        else {
            say(p, SkillMsg.t("fairytail.no_energy", "Not enough Aura or magicules (need %s).", (long) cost).withStyle(ChatFormatting.RED));
            return false;
        }
        ex.markDirty();
        return true;
    }

    private static void say(ServerPlayer p, Component c) { p.displayClientMessage(c, true); }

    // ------------------------------------------------------------------------------------------------------------------ cast
    /**
     * Runs the spell. The cost is paid here, after the checks that need no cost (a target to aim at, something to eat), so a refused cast is free.
     */
    public static Outcome cast(FtLine line, FtMove mv, CompoundTag tag, ServerPlayer p, Tier tier, double masteryFrac) {
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        FtForms form = activeForm(tag, now);
        double formMul = form == null || form.element() != line.element ? 1.0 : form.dmgMul();
        double charge = charge(tag, now);
        double raw = FtRules.rawDamage(mv, tier, formMul, mv.hurts() ? charge : 0);
        Ctx c = new Ctx(p, level, line, mv, tier, masteryFrac, raw, form);
        return switch (mv.kind()) {
            case BREATH -> breath(c);
            case CONE -> cone(c);
            case DASH -> dash(c);
            case RING -> ring(c);
            case TARGET_AREA -> targetArea(c);
            case RAIN -> rain(c);
            case BARRAGE -> barrage(c);
            case COMBO -> combo(c);
            case BUFF -> buff(c);
            case ENCHANT -> enchant(c);
            case HEAL -> heal(c);
            case SHIELD -> shield(c);
            case EAT -> eat(c, tag, now);
            case FORM -> form(c, tag, now);
            case LIFT -> lift(c);
            case BOMB -> bomb(c);
            case DISPEL -> dispel(c);
            case LOCK -> lock(c);
        };
    }

    private record Ctx(ServerPlayer p, ServerLevel level, FtLine line, FtMove mv, Tier tier, double mastery, double raw, FtForms form) {
        FtElement el() { return line.element; }
        boolean magic() { return mv.kind() != FtKind.CONE && mv.kind() != FtKind.DASH; }
        boolean pay() { return FtCast.pay(p, mv, tier); }
        Vec3 hand() { return p.getEyePosition().add(p.getViewVector(1f).scale(0.7)).add(0, -0.3, 0); }
    }

    private static float strike(Ctx c, LivingEntity t, double raw) {
        float d = FtWorld.hit(c.p, t, raw, c.mastery, c.magic());
        if (d > 0f) status(c, t);
        return d;
    }

    /** The element's mark on a creature that was hit. */
    private static void status(Ctx c, LivingEntity t) {
        int amp = c.tier == Tier.ULTIMATE ? 2 : c.tier == Tier.MASTERED ? 1 : 0;
        switch (c.el()) {
            case FIRE -> t.setRemainingFireTicks(80 + 40 * amp);
            case ICE -> { t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80 + 20 * amp, 1 + amp)); t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 40, t.getTicksFrozen() + 60)); }
            case LIGHTNING -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30 + 10 * amp, 3));
            case IRON -> t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, amp));
            case WATER -> { t.clearFire(); t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 + 20 * amp, 1)); }
            case CRASH -> shatter(t, 6 + 4 * amp);
            case SKY -> { }
        }
    }

    /** Crash: the creature's armour loses durability and its resistance is shredded for a moment. */
    private static void shatter(LivingEntity t, int wear) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = t.getItemBySlot(slot);
            if (!s.isEmpty() && s.isDamageableItem()) s.setDamageValue(Math.min(s.getMaxDamage() - 1, s.getDamageValue() + wear));
        }
        t.setAbsorptionAmount(0f);
        EnergyBridge.effect(t, "fragility", 100, 0);
    }

    private static SoundEvent sound(FtElement e) {
        return switch (e) {
            case FIRE -> SoundEvents.BLAZE_SHOOT;
            case ICE -> SoundEvents.GLASS_BREAK;
            case SKY -> SoundEvents.PLAYER_ATTACK_SWEEP;
            case LIGHTNING -> SoundEvents.LIGHTNING_BOLT_THUNDER;
            case IRON -> SoundEvents.ANVIL_LAND;
            case CRASH -> SoundEvents.ANVIL_BREAK;
            case WATER -> SoundEvents.PLAYER_SPLASH;
        };
    }

    private static void tell(Ctx c, Vec3 at, float volume) { FtWorld.sound(c.level, at, sound(c.el()), volume, 0.9f); }

    // ------------------------------------------------------------------------------------------------------------------ shapes
    private static Outcome breath(Ctx c) {
        if (!c.pay()) return Outcome.fizzle();
        return Outcome.of(lane(c, c.p.getViewVector(1f), c.raw));
    }

    /** One lane of damage along {@code dir} from the caster's mouth. Returns the creatures hurt. */
    private static int lane(Ctx c, Vec3 dir, double raw) {
        ServerPlayer p = c.p;
        Vec3 from = c.hand();
        Vec3 d = dir.normalize();
        Vec3 end = from.add(d.scale(c.mv.range()));
        Vec3 aim = FtWorld.aimPoint(p, c.mv.range());
        if (aim.distanceTo(p.getEyePosition()) < c.mv.range() && d.dot(p.getViewVector(1f)) > 0.99) end = aim;
        FtWorld.fx(c.level, c.el().cast, from, end, c.el().argb, 18, (float) (1.0 + c.mv.radius() * 0.5));
        tell(c, from, 1.0f);
        int hits = 0;
        for (LivingEntity t : FtWorld.foesAlong(p, from, end, c.mv.radius())) {
            if (strike(c, t, raw) > 0f) { hits++; FtWorld.shove(t, p.position(), 0.5); }
        }
        FtWorld.fx(c.level, c.el().burst, end, end.add(d), c.el().argb, 14, 0.8f);
        return hits;
    }

    private static Outcome cone(Ctx c) {
        if (!c.pay()) return Outcome.fizzle();
        ServerPlayer p = c.p;
        double minDot = Math.max(0.15, Math.min(0.95, 1.0 - c.mv.radius()));
        int hits = 0;
        for (LivingEntity t : FtWorld.foesInCone(p, c.mv.range(), minDot)) {
            if (strike(c, t, c.raw) > 0f) { hits++; FtWorld.shove(t, p.position(), 0.9); }
        }
        Vec3 at = p.getEyePosition().add(p.getViewVector(1f).scale(Math.min(2.5, c.mv.range() * 0.6)));
        FtWorld.fx(c.level, c.el().burst, at, at.add(p.getViewVector(1f)), c.el().argb, 14, 1.0f);
        tell(c, at, 1.0f);
        return Outcome.of(hits);
    }

    private static Outcome dash(Ctx c) {
        if (!c.pay()) return Outcome.fizzle();
        ServerPlayer p = c.p;
        Vec3 look = p.getViewVector(1f);
        Vec3 start = p.position().add(0, 0.9, 0);
        Vec3 end = start.add(look.scale(c.mv.range()));
        double power = 0.35 + c.mv.range() * 0.2;
        p.setDeltaMovement(look.x * power, Math.max(0.2, look.y * power * 0.6), look.z * power);
        p.hurtMarked = true;
        p.fallDistance = 0;
        int hits = 0;
        for (LivingEntity t : FtWorld.foesAlong(p, start, end, c.mv.radius())) {
            if (strike(c, t, c.raw) > 0f) { hits++; FtWorld.shove(t, p.position(), 0.7); }
        }
        FtWorld.fx(c.level, c.el().cast, start, end, c.el().argb, 14, (float) (0.8 + c.mv.radius() * 0.4));
        tell(c, start, 1.0f);
        return Outcome.of(hits);
    }

    private static Outcome ring(Ctx c) {
        if (!c.pay()) return Outcome.fizzle();
        ServerPlayer p = c.p;
        Vec3 at = p.position();
        int hits = 0;
        for (LivingEntity t : FtWorld.foesAround(p, at, c.mv.radius())) {
            if (strike(c, t, c.raw) > 0f) {
                hits++;
                FtWorld.shove(t, at, 1.1);
                if (c.mv.ticks() > 0) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, c.mv.ticks(), Math.max(0, c.mv.count())));
            }
        }
        FtWorld.fx(c.level, c.el().zone, at, at.add(0, 1, 0), c.el().argb, 30, (float) c.mv.radius());
        FtWorld.fx(c.level, c.el().burst, at, at.add(0, 1, 0), c.el().argb, 16, (float) (c.mv.radius() * 0.3));
        tell(c, at, 1.2f);
        return Outcome.of(hits);
    }

    private static Outcome targetArea(Ctx c) {
        if (!c.pay()) return Outcome.fizzle();
        ServerPlayer p = c.p;
        Vec3 at = FtWorld.aimPoint(p, c.mv.range());
        FtWorld.fx(c.level, c.el().cast, c.hand(), at, c.el().argb, 14, 1.2f);
        int hits = 0;
        for (LivingEntity t : FtWorld.foesAround(p, at, c.mv.radius())) {
            if (strike(c, t, c.raw) > 0f) { hits++; FtWorld.shove(t, at, 0.9); }
        }
        FtWorld.fx(c.level, c.el().burst, at, at.add(0, 1, 0), c.el().argb, 20, (float) (c.mv.radius() * 0.45));
        FtWorld.fx(c.level, c.el().zone, at, at.add(0, 1, 0), c.el().argb, 24, (float) c.mv.radius());
        tell(c, at, 1.4f);
        return Outcome.of(hits);
    }

    private static Outcome rain(Ctx c) {
        if (!c.pay()) return Outcome.fizzle();
        ServerPlayer p = c.p;
        Vec3 centre = FtWorld.aimPoint(p, c.mv.range());
        FtWorld.fx(c.level, c.el().zone, centre, centre.add(0, 1, 0), c.el().argb, 6 * c.mv.count() + 20, (float) c.mv.radius());
        tell(c, centre, 1.2f);
        for (int i = 0; i < c.mv.count(); i++) {
            int delay = 6 + i * 6;
            FtWorld.later(c.level, delay, () -> {
                if (!p.isAlive() || p.serverLevel() != c.level) return;
                var rnd = c.level.getRandom();
                double a = rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(rnd.nextDouble()) * c.mv.radius() * 0.8;
                Vec3 spot = centre.add(Math.cos(a) * r, 0, Math.sin(a) * r);
                FtWorld.fx(c.level, c.el().burst, spot, spot.add(0, 1, 0), c.el().argb, 14, 0.9f);
                for (LivingEntity t : FtWorld.foesAround(p, spot, 2.8)) strike(c, t, c.raw);
            });
        }
        return Outcome.of(0);
    }

    private static Outcome barrage(Ctx c) {
        if (!c.pay()) return Outcome.fizzle();
        ServerPlayer p = c.p;
        int n = Math.max(1, c.mv.count() + (c.tier == Tier.ULTIMATE ? 2 : c.tier == Tier.MASTERED ? 1 : 0));
        int hits = lane(c, p.getViewVector(1f), c.raw);
        for (int i = 1; i < n; i++) {
            final int k = i;
            FtWorld.later(c.level, i * 4, () -> {
                if (!p.isAlive() || p.serverLevel() != c.level) return;
                double yaw = Math.toRadians(((k % 2 == 0) ? 1 : -1) * (2.0 + 2.0 * ((k + 1) / 2)));
                Vec3 v = p.getViewVector(1f);
                Vec3 skew = new Vec3(v.x * Math.cos(yaw) - v.z * Math.sin(yaw), v.y, v.x * Math.sin(yaw) + v.z * Math.cos(yaw));
                lane(c, skew, c.raw);
            });
        }
        return Outcome.of(hits);
    }

    private static Outcome combo(Ctx c) {
        ServerPlayer p = c.p;
        LivingEntity first = FtWorld.lookFoe(p, c.mv.range());
        if (first == null) {
            List<LivingEntity> near = FtWorld.foesAround(p, p.position(), c.mv.range());
            first = near.isEmpty() ? null : near.get(0);
        }
        if (first == null) { say(p, SkillMsg.t("fairytail.no_target", "No target in reach.").withStyle(ChatFormatting.RED)); return Outcome.fizzle(); }
        if (!c.pay()) return Outcome.fizzle();
        final LivingEntity target = first;
        Vec3 back = p.getViewVector(1f).scale(-1.4);
        p.teleportTo(target.getX() + back.x, target.getY(), target.getZ() + back.z);
        FtWorld.fx(c.level, c.el().cast, p.position().add(0, 1, 0), target.position().add(0, 1, 0), c.el().argb, 14, 1.4f);
        int blows = Math.max(1, c.mv.count());
        for (int i = 0; i < blows; i++) {
            FtWorld.later(c.level, 4 + i * 6, () -> {
                if (!p.isAlive() || p.serverLevel() != c.level) return;
                Vec3 at = target.isAlive() ? target.position() : p.position();
                for (LivingEntity t : FtWorld.foesAround(p, at, 3.5)) strike(c, t, c.raw);
                FtWorld.fx(c.level, c.el().burst, at.add(0, 1, 0), at.add(0, 2, 0), c.el().argb, 12, 1.0f);
                tell(c, at, 1.0f);
            });
        }
        FtWorld.later(c.level, 4 + blows * 6 + 6, () -> {
            if (!p.isAlive() || p.serverLevel() != c.level) return;
            Vec3 at = target.isAlive() ? target.position() : p.position();
            for (LivingEntity t : FtWorld.foesAround(p, at, c.mv.radius())) {
                if (strike(c, t, c.raw * 1.6) > 0f) FtWorld.shove(t, at, 1.4);
            }
            FtWorld.fx(c.level, "FIRE_PILLAR", at, at.add(0, 1, 0), c.el().argb, 30, (float) c.mv.radius());
            FtWorld.fx(c.level, c.el().burst, at, at.add(0, 1, 0), c.el().argb, 24, (float) (c.mv.radius() * 0.5));
            tell(c, at, 1.8f);
        });
        return Outcome.of(0);
    }

    // ------------------------------------------------------------------------------------------------------------------ support
    private static Outcome buff(Ctx c) {
        if (!c.pay()) return Outcome.fizzle();
        c.p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, c.mv.ticks(), Math.max(0, c.mv.count())));
        c.p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, c.mv.ticks(), 0));
        FtWorld.fxFollow(c.level, c.el().zone, c.p, c.p.position(), c.el().argb, 40, 1.2f);
        return Outcome.of(0);
    }

    /** Vernier (speed and haste, {@code count} is the amplifier) or, with {@code count >= 100}, Enchant: Troia (regeneration and absorption). */
    private static Outcome enchant(Ctx c) {
        if (!c.pay()) return Outcome.fizzle();
        int ticks = FtRules.formTicks(c.mv, c.tier);
        boolean troia = c.mv.count() >= 100;
        for (LivingEntity a : FtWorld.alliesAround(c.p, c.p.position(), c.mv.radius())) {
            if (troia) {
                a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 1));
                a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 1));
            } else {
                a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, c.mv.count()));
                a.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, ticks, 1));
                a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 0));
            }
            FtWorld.fxFollow(c.level, c.el().zone, a, a.position(), c.el().argb, 30, 0.9f);
        }
        FtWorld.fx(c.level, c.el().zone, c.p.position(), c.p.position().add(0, 1, 0), c.el().argb, 30, (float) c.mv.radius());
        tell(c, c.p.position(), 1.0f);
        return Outcome.of(0);
    }

    private static Outcome heal(Ctx c) {
        if (!c.pay()) return Outcome.fizzle();
        double scale = Math.min(3.0, Math.sqrt(Math.max(1.0, BalanceLaw.epScale(c.p))));
        float amount = (float) (c.raw * scale);
        for (LivingEntity a : FtWorld.alliesAround(c.p, c.p.position(), c.mv.radius())) {
            BalanceLaw.heal(a, amount);
            cleanse(a);
            FtWorld.fxFollow(c.level, c.el().zone, a, a.position(), c.el().argb, 30, 0.9f);
        }
        FtWorld.fx(c.level, c.el().zone, c.p.position(), c.p.position().add(0, 1, 0), c.el().argb, 36, (float) c.mv.radius());
        tell(c, c.p.position(), 1.0f);
        return Outcome.of(0);
    }

    /** Removes harmful effects and fire from a creature. Returns how many effects went. */
    static int cleanse(LivingEntity e) {
        int removed = 0;
        for (MobEffectInstance m : new ArrayList<>(e.getActiveEffects())) {
            if (!m.getEffect().value().isBeneficial() && e.removeEffect(m.getEffect())) removed++;
        }
        e.clearFire();
        return removed;
    }

    private static Outcome shield(Ctx c) {
        if (!c.pay()) return Outcome.fizzle();
        int ticks = FtRules.formTicks(c.mv, c.tier);
        c.p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, c.mv.count()));
        c.p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 1));
        FtWorld.fxFollow(c.level, c.el().zone, c.p, c.p.position(), c.el().argb, Math.min(ticks, 200), 1.6f);
        tell(c, c.p.position(), 1.0f);
        return Outcome.of(0);
    }

    // ------------------------------------------------------------------------------------------------------------------ eating
    private static Outcome eat(Ctx c, CompoundTag tag, long now) {
        ServerPlayer p = c.p;
        int units = 0;
        if (c.line.eat.equals("fire")) {
            int r = (int) c.mv.range();
            BlockPos o = p.blockPosition();
            for (BlockPos b : BlockPos.betweenClosed(o.offset(-r, -3, -r), o.offset(r, 3, r))) {
                if (units >= 8) break;
                BlockState st = c.level.getBlockState(b);
                if (st.getBlock() instanceof BaseFireBlock) {
                    c.level.removeBlock(b.immutable(), false);
                    units++;
                }
            }
            for (LivingEntity e : c.level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), x -> x.isOnFire() && x.isAlive())) {
                if (units >= 8) break;
                e.clearFire();
                units++;
            }
        } else if (c.line.eat.equals("iron")) {
            var inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize() && units < 6; i++) {
                ItemStack s = inv.getItem(i);
                if (s.is(Items.IRON_INGOT)) {
                    int take = Math.min(s.getCount(), 6 - units);
                    s.shrink(take);
                    units += take;
                }
            }
        }
        if (units <= 0) {
            say(p, SkillMsg.t(c.line.eat.equals("iron") ? "fairytail.no_iron" : "fairytail.no_flame",
                    c.line.eat.equals("iron") ? "You have no iron to eat." : "There is no flame to eat.").withStyle(ChatFormatting.RED));
            return Outcome.fizzle();
        }
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex != null) {
            double max = EnergyHelper.getMaxAura(p);
            ex.setAura(Math.min(max, ex.getAura() + max * FtRules.eatRestoreFrac(units)));
            ex.markDirty();
        }
        BalanceLaw.heal(p, 2f * units);
        addCharge(tag, now, units);
        FtWorld.fxFollow(c.level, c.el().zone, p, p.position(), c.el().argb, 30, 1.0f);
        FtWorld.sound(c.level, p.position(), SoundEvents.GENERIC_EAT, 1.0f, 0.7f);
        return Outcome.of(0);
    }

    // ------------------------------------------------------------------------------------------------------------------ forms
    private static Outcome form(Ctx c, CompoundTag tag, long now) {
        ServerPlayer p = c.p;
        FtForms f = FtForms.get(c.mv.form());
        if (f == null) return Outcome.fizzle();
        FtForms on = activeForm(tag, now);
        if (on != null && on == f) {                       // casting the form again ends it
            tag.putLong(FORM_UNTIL, 0L);
            PlayerAuras.clear(p, Aura.FAIRY_TAIL);
            return Outcome.of(0);
        }
        if (!c.pay()) return Outcome.fizzle();
        int ticks = FtRules.formTicks(c.mv, c.tier);
        tag.putLong(FORM_UNTIL, now + ticks);
        tag.putInt(FORM_IDX, c.mv.form());
        if (f.speedAmp() > 0) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, f.speedAmp() - 1));
        if (f.strengthAmp() > 0) p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, f.strengthAmp() - 1));
        if (f.resistAmp() > 0) p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, f.resistAmp() - 1));
        if (f.absorbAmp() > 0) p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, f.absorbAmp() - 1));
        if (f.regenAmp() > 0) p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, f.regenAmp() - 1));
        PlayerAuras.set(p, Aura.FAIRY_TAIL, ticks, c.mv.form());
        FtWorld.fxFollow(c.level, f.element().zone, p, p.position(), f.element().argb, 40, 1.4f);
        FtWorld.fx(c.level, f.element().burst, p.position(), p.position().add(0, 1, 0), f.element().argb, 20, 1.2f);
        tell(c, p.position(), 1.4f);
        if (f.key().equals("rain_woman")) {                // the rain heals the party and chills the foes
            for (LivingEntity a : FtWorld.alliesAround(p, p.position(), 10)) a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 0));
            for (LivingEntity t : FtWorld.foesAround(p, p.position(), 10)) {
                t.clearFire();
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 1));
            }
        }
        return Outcome.of(0);
    }

    // ------------------------------------------------------------------------------------------------------------------ crash and water
    private static Outcome lift(Ctx c) {
        ServerPlayer p = c.p;
        List<LivingEntity> foes = FtWorld.foesAround(p, p.position(), c.mv.radius());
        if (foes.isEmpty()) { say(p, SkillMsg.t("fairytail.no_target", "No target in reach.").withStyle(ChatFormatting.RED)); return Outcome.fizzle(); }
        if (!c.pay()) return Outcome.fizzle();
        for (LivingEntity t : foes) {
            t.setDeltaMovement(t.getDeltaMovement().x, 0.9, t.getDeltaMovement().z);
            t.hurtMarked = true;
            t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, c.mv.ticks(), 2));
        }
        FtWorld.fx(c.level, c.el().zone, p.position(), p.position().add(0, 1, 0), c.el().argb, c.mv.ticks() + 10, (float) c.mv.radius());
        tell(c, p.position(), 1.2f);
        FtWorld.later(c.level, c.mv.ticks(), () -> {
            if (!p.isAlive() || p.serverLevel() != c.level) return;
            for (LivingEntity t : foes) {
                if (!t.isAlive()) continue;
                t.removeEffect(MobEffects.LEVITATION);
                t.setDeltaMovement(0, -2.2, 0);
                t.hurtMarked = true;
                strike(c, t, c.raw);
                FtWorld.fx(c.level, c.el().burst, t.position(), t.position().add(0, 1, 0), c.el().argb, 16, 0.9f);
            }
            tell(c, p.position(), 1.6f);
        });
        return Outcome.of(0);
    }

    private static Outcome bomb(Ctx c) {
        ServerPlayer p = c.p;
        LivingEntity mark = FtWorld.lookFoe(p, c.mv.range());
        if (mark == null) { say(p, SkillMsg.t("fairytail.aim", "Aim at a target.").withStyle(ChatFormatting.RED)); return Outcome.fizzle(); }
        if (!c.pay()) return Outcome.fizzle();
        FtWorld.fx(c.level, c.el().zone, mark.position(), mark.position().add(0, 1, 0), c.el().argb, c.mv.ticks(), 2.4f);
        FtWorld.fxFollow(c.level, c.el().cast, mark, mark.position(), c.el().argb, c.mv.ticks(), 1.0f);
        FtWorld.later(c.level, c.mv.ticks(), () -> {
            if (!p.isAlive() || p.serverLevel() != c.level) return;
            Vec3 at = mark.position();
            for (LivingEntity t : FtWorld.foesAround(p, at, c.mv.radius())) {
                if (strike(c, t, c.raw) > 0f) FtWorld.shove(t, at, 1.3);
            }
            FtWorld.fx(c.level, c.el().burst, at, at.add(0, 1, 0), c.el().argb, 24, (float) (c.mv.radius() * 0.5));
            tell(c, at, 1.8f);
        });
        return Outcome.of(0);
    }

    private static Outcome dispel(Ctx c) {
        if (!c.pay()) return Outcome.fizzle();
        ServerPlayer p = c.p;
        double minDot = Math.max(0.15, Math.min(0.95, 1.0 - c.mv.radius()));
        int hits = 0;
        for (LivingEntity t : FtWorld.foesInCone(p, c.mv.range(), minDot)) {
            for (MobEffectInstance m : new ArrayList<>(t.getActiveEffects())) {
                if (m.getEffect().value().isBeneficial()) t.removeEffect(m.getEffect());
            }
            if (strike(c, t, c.raw) > 0f) { hits++; FtWorld.shove(t, p.position(), 0.6); }
        }
        Vec3 at = p.getEyePosition().add(p.getViewVector(1f).scale(2.5));
        FtWorld.fx(c.level, c.el().burst, at, at.add(p.getViewVector(1f)), c.el().argb, 16, 1.2f);
        tell(c, at, 1.2f);
        return Outcome.of(hits);
    }

    private static Outcome lock(Ctx c) {
        ServerPlayer p = c.p;
        LivingEntity t = FtWorld.lookFoe(p, c.mv.range());
        if (t == null) { say(p, SkillMsg.t("fairytail.aim", "Aim at a target.").withStyle(ChatFormatting.RED)); return Outcome.fizzle(); }
        if (!BalanceLaw.beginControl(p)) {
            say(p, SkillMsg.t("fairytail.control_cooldown", "Binding spells are still recovering (%s s).", BalanceLaw.controlCooldownLeft(p) / 20).withStyle(ChatFormatting.RED));
            return Outcome.fizzle();
        }
        if (!c.pay()) return Outcome.fizzle();
        int ticks = BalanceLaw.controlTicks(t, c.mv.ticks());
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 2));
        t.clearFire();
        FtWorld.fxFollow(c.level, c.el().zone, t, t.position(), c.el().argb, ticks, 1.8f);
        FtWorld.fx(c.level, c.el().cast, c.hand(), t.position().add(0, 1, 0), c.el().argb, 14, 1.0f);
        tell(c, t.position(), 1.0f);
        return Outcome.of(strike(c, t, c.raw) > 0f ? 1 : 0);
    }
}
