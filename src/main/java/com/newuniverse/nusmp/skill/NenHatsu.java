package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.NenRules;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why (3.57.0, docs/nen_rework.md): the six hatsu of Nen are signature moves, not six variations of "hit what you look at". Enhancement is a fist that spends the aura Ko stored and wears armour
 * down; Emission a volley of homing bullets; Transmutation electricity, gum or rubber by stance; Conjuration chains that hold a few foes; Manipulation a needle that turns a creature on its
 * neighbours; Specialization a field that steals buffs. Every move is one press. What runs after the press (the bullets still to leave, the chains, the needle, the field) is a few numbers in the
 * skill tag of the one player who owns them: a game time it ends at, UUIDs of the creatures it holds, and {@link #BUSY}, the latest time anything of it can still be running, so the tick of an
 * idle player is one tag read. Nothing is static or shared; everything is looked up by UUID in the owner's own world each time, so a logout, a death ({@link #clearAll}), a dimension change or a
 * dead target just ends the move. Worst case per owner: 6 bullets (one foe query per three ticks), 5 chains (a hold every 5 ticks, a hit every 20), 1 needle (a query every 10 ticks), 1 field
 * (one query a second for at most 9 pulses). Every hit goes through {@link SkillDamage} (the balance law), every control through {@link BalanceLaw#controlTicks} (3 s players, 5 s mobs, 1 s bosses).
 */
final class NenHatsu {
    private NenHatsu() {}

    static final String BUSY = "NenBusy";
    private static final String VOLLEY_LEFT = "NenVolleyLeft", VOLLEY_NEXT = "NenVolleyNext", VOLLEY_I = "NenVolleyI", VOLLEY_MUL = "NenVolleyMul";
    private static final String CHAIN_N = "NenChainN", CHAIN_MUL = "NenChainMul", CHAIN_T = "NenChainT", CHAIN_E = "NenChainE", CHAIN_FX = "NenChainFx";
    private static final String NEEDLE_T = "NenNeedleT", NEEDLE_E = "NenNeedleE", NEEDLE_NEXT = "NenNeedleNext";
    private static final String FIELD_E = "NenFieldE", FIELD_NEXT = "NenFieldNext", FIELD_MUL = "NenFieldMul", FIELD_VOW = "NenFieldVow";
    private static final String MARK_UNTIL = "nusmp_nen_mark", MARK_BY = "nusmp_nen_mark_by", MARK_MUL = "nusmp_nen_mark_mul";

    private static final int C_ELECTRIC = 0xFFB8D0FF, C_GUM = 0xFFFF7AC8, C_RUBBER = 0xFFFFD25A, C_STEEL = 0xFFB8F0E8, C_NEEDLE = 0xFFE8A0FF;

    private static void busy(CompoundTag tag, long until) { if (until > tag.getLong(BUSY)) tag.putLong(BUSY, until); }

    private static Vec3 centre(LivingEntity e) { return e.position().add(0, e.getBbHeight() * 0.5, 0); }

    private static Vec3 hand(ServerPlayer p) { return p.getEyePosition().add(p.getViewVector(1f).scale(0.5)).add(0, -0.45, 0); }

    private static double mastery(ManasSkillInstance inst) { return NUSkill.masteryFrac(inst); }

    // ------------------------------------------------------------------ marks (a creature that has been read: the owner's blows do more to it for a while)
    /** Marks a creature for this owner: his blows do {@code mul} times as much to it for {@code ticks}. One mark per creature; someone else's live mark is not replaced. */
    static void mark(LivingEntity t, ServerPlayer owner, int ticks, float mul) {
        long now = owner.level().getGameTime();
        CompoundTag d = t.getPersistentData();
        if (d.getLong(MARK_UNTIL) > now && d.hasUUID(MARK_BY) && !owner.getUUID().equals(d.getUUID(MARK_BY))) return;
        d.putLong(MARK_UNTIL, now + ticks);
        d.putUUID(MARK_BY, owner.getUUID());
        d.putFloat(MARK_MUL, Math.max(1f, Math.min(1.5f, mul)));
    }

    /** What this owner's blows are multiplied by on this creature right now (1 when it is not marked by him). */
    static float markMul(LivingEntity t, ServerPlayer owner, long now) {
        CompoundTag d = t.getPersistentData();
        if (d.getLong(MARK_UNTIL) <= now || !d.hasUUID(MARK_BY) || !owner.getUUID().equals(d.getUUID(MARK_BY))) return 1f;
        return Math.max(1f, Math.min(1.5f, d.getFloat(MARK_MUL)));
    }

    // ------------------------------------------------------------------ Enhancement: the fist
    /** A rushing punch at what you look at (6 blocks). With Ko open it spends the aura Ko gathered: up to 2.4 times the blow, and the armour it lands on is worn down. */
    static boolean enhance(ManasSkillInstance inst, ServerPlayer p, CompoundTag tag, long now, boolean vow) {
        LivingEntity t = SkillTargets.lookFoe(p, NenRules.fistReach());
        if (t == null) { SkillUtil.fail(p, SkillMsg.t("nen.no_target", "Nothing in reach.")); return false; }
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.ENHANCE)))) return false;
        long koEnd = tag.getLong(NenRules.untilKey(NenRules.KO));
        double charge = NenRules.koCharge(koEnd, now);
        if (NenRules.active(koEnd, now)) tag.putLong(NenRules.untilKey(NenRules.KO), 0L);      // closed first: the fist spends what Ko gathered, and its own blow must not start Ko's strike
        Vec3 from = p.position().add(0, 1.0, 0), to = centre(t);
        Vec3 d = t.position().subtract(p.position());
        double h = Math.hypot(d.x, d.z);
        if (h > 2.5) { p.setDeltaMovement(d.x / h * NenRules.fistLunge(), Math.max(p.getDeltaMovement().y, 0.12), d.z / h * NenRules.fistLunge()); p.hurtMarked = true; }   // the rush
        SkillDamage.hit(p, t, NenRules.enhanceDamage() * NenRules.fistMultiplier(charge) * NenSkill.hatsuMul(inst, p, NenRules.ENHANCE, vow), mastery(inst));
        NenSkill.knock(p, t, NenRules.enhanceKnockback() * (1.0 + 0.7 * charge));
        shatter(t, NenRules.shatterFraction(charge, vow));
        if (charge >= 0.5) mark(t, p, NenRules.fistMarkTicks(), NenRules.fistMarkMultiplier());
        NenFx.fist(p, from, to, charge, NenRules.typeColor(NenRules.T_ENHANCE));
        return true;
    }

    /** Wears down the four armour pieces by a share of their maximum durability. Never takes the last point: nothing breaks, so a fist can not delete a player's armour. */
    private static void shatter(LivingEntity t, double frac) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = t.getItemBySlot(slot);
            if (s.isEmpty() || !s.isDamageableItem()) continue;
            int max = s.getMaxDamage();
            s.setDamageValue(Math.min(max - 1, s.getDamageValue() + Math.max(1, (int) (max * frac))));
        }
    }

    // ------------------------------------------------------------------ Emission: the volley
    /** Opens a volley: four bullets (six under a vow), one every three ticks, each bending towards the foes in front of you (dealt out in turn, so a crowd shares them). */
    static boolean emit(ManasSkillInstance inst, ServerPlayer p, CompoundTag tag, long now, boolean vow) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.EMIT)))) return false;
        int n = NenRules.volleyBullets(vow);
        tag.putInt(VOLLEY_LEFT, n);
        tag.putInt(VOLLEY_I, 0);
        tag.putLong(VOLLEY_NEXT, now);
        tag.putDouble(VOLLEY_MUL, NenSkill.hatsuMul(inst, p, NenRules.EMIT, vow));
        busy(tag, now + (long) n * NenRules.volleyGap() + 20);
        return true;
    }

    private static void volleyStep(ManasSkillInstance inst, ServerPlayer p, CompoundTag tag, long now) {
        int left = tag.getInt(VOLLEY_LEFT);
        if (left <= 0 || now < tag.getLong(VOLLEY_NEXT)) return;
        int i = tag.getInt(VOLLEY_I);
        tag.putInt(VOLLEY_LEFT, left - 1);
        tag.putInt(VOLLEY_I, i + 1);
        tag.putLong(VOLLEY_NEXT, now + NenRules.volleyGap());
        Vec3 look = p.getViewVector(1f);
        double sl = Math.hypot(look.x, look.z);
        Vec3 side = sl < 1.0e-3 ? new Vec3(1, 0, 0) : new Vec3(-look.z / sl, 0, look.x / sl);
        Vec3 from = p.getEyePosition().add(look.scale(0.6)).add(side.scale(((i % 3) - 1) * 0.55)).add(0, ((i % 2) * 0.35) - 0.25, 0);
        List<LivingEntity> foes = new ArrayList<>(8);
        for (LivingEntity e : SkillTargets.foesInCone(p, NenRules.bulletRange(), NenRules.bulletCone())) if (p.hasLineOfSight(e)) foes.add(e);
        int pick = NenRules.bulletPick(i, foes.size());
        LivingEntity t = pick < 0 ? null : foes.get(pick);
        Vec3 to = t != null ? centre(t) : DMUtil.lookPoint(p, 20);
        NenFx.bullet(p, from, to, NenRules.typeColor(NenSkill.typeOf(inst, p)));
        if (t != null) SkillDamage.hit(p, t, NenRules.bulletDamage() * tag.getDouble(VOLLEY_MUL), mastery(inst), SkillDamage.Kind.MAGIC);
    }

    // ------------------------------------------------------------------ Transmutation: electricity, gum, rubber
    /** One move, three natures by stance: standing, electricity that jumps from foe to foe; sneaking, gum that pulls a cone of foes to you and holds them; in the air, rubber that bounces you and shocks what is round you. */
    static boolean trans(ManasSkillInstance inst, ServerPlayer p, CompoundTag tag, long now, boolean vow) {
        return switch (NenRules.transProperty(p.isShiftKeyDown(), !p.onGround())) {
            case NenRules.GUM -> gum(inst, p, vow);
            case NenRules.RUBBER -> rubber(inst, p, vow);
            default -> electric(inst, p, vow);
        };
    }

    private static boolean electric(ManasSkillInstance inst, ServerPlayer p, boolean vow) {
        LivingEntity first = SkillTargets.lookFoe(p, NenRules.elecRange());
        if (first == null) {
            List<LivingEntity> cone = SkillTargets.foesInCone(p, NenRules.elecRange(), 0.8);
            if (!cone.isEmpty()) first = cone.get(0);
        }
        if (first == null) { SkillUtil.fail(p, SkillMsg.t("nen.no_target", "Nothing in reach.")); return false; }
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.TRANS)))) return false;
        double dmg = NenRules.elecDamage() * NenSkill.hatsuMul(inst, p, NenRules.TRANS, vow);
        List<LivingEntity> hit = new ArrayList<>(6);
        Vec3 prev = hand(p);
        LivingEntity cur = first;
        for (int j = 0; cur != null && j <= NenRules.elecJumps(vow); j++) {
            Vec3 at = centre(cur);
            NenFx.surge(p, NenRules.ELECTRIC, prev, at, 0, C_ELECTRIC);
            SkillDamage.hit(p, cur, dmg, mastery(inst), SkillDamage.Kind.MAGIC);
            cur.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(cur, NenRules.elecSlowTicks()), 1, false, true, true));
            hit.add(cur);
            dmg *= NenRules.elecFalloff();
            prev = at;
            LivingEntity next = null;
            for (LivingEntity e : SkillTargets.foes(p, cur.position(), NenRules.elecJumpRange())) if (!hit.contains(e)) { next = e; break; }
            cur = next;
        }
        return true;
    }

    private static boolean gum(ManasSkillInstance inst, ServerPlayer p, boolean vow) {
        List<LivingEntity> foes = SkillTargets.foesInCone(p, NenRules.gumRange(), 0.35);
        if (foes.isEmpty()) { SkillUtil.fail(p, SkillMsg.t("nen.no_target", "Nothing in reach.")); return false; }
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.TRANS)))) return false;
        double dmg = NenRules.gumDamage() * NenSkill.hatsuMul(inst, p, NenRules.TRANS, vow);
        Vec3 from = hand(p);
        int n = 0;
        for (LivingEntity e : foes) {
            if (n++ >= NenRules.gumMax(vow)) break;
            Vec3 d = p.position().subtract(e.position());
            double h = Math.hypot(d.x, d.z);
            if (h > 1.5) { e.setDeltaMovement(d.x / h * NenRules.gumPull(), 0.25, d.z / h * NenRules.gumPull()); e.hurtMarked = true; }
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(e, NenRules.gumRootTicks()), 3, false, true, true));
            SkillDamage.hit(p, e, dmg, mastery(inst), SkillDamage.Kind.MAGIC);
            NenFx.surge(p, NenRules.GUM, from, centre(e), 0, C_GUM);
        }
        return true;
    }

    private static boolean rubber(ManasSkillInstance inst, ServerPlayer p, boolean vow) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.TRANS)))) return false;
        Vec3 look = p.getViewVector(1f);
        p.setDeltaMovement(look.x * 0.5, NenRules.rubberLeap(), look.z * 0.5);
        p.hurtMarked = true;
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, NenRules.rubberFallTicks(), 0, false, false, true));
        double dmg = NenRules.rubberDamage() * NenSkill.hatsuMul(inst, p, NenRules.TRANS, vow);
        int n = 0;
        for (LivingEntity e : SkillTargets.foes(p, p.position(), NenRules.rubberRadius())) {
            if (n++ >= NenRules.rubberMax()) break;
            NenSkill.knock(p, e, NenRules.rubberKnockback());
            SkillDamage.hit(p, e, dmg, mastery(inst), SkillDamage.Kind.MAGIC);
        }
        NenFx.surge(p, NenRules.RUBBER, p.position(), p.position(), NenRules.rubberRadius(), C_RUBBER);
        return true;
    }

    // ------------------------------------------------------------------ Conjuration: chains
    /** Chains of aura from your hand to up to three foes (five under a vow) in 16 blocks: the one you look at first, then the nearest. Each holds its foe still for as long as the balance law allows (3 s a player, 5 s a mob, 1 s a boss), hurts it once a second and breaks in a burst. */
    static boolean conj(ManasSkillInstance inst, ServerPlayer p, CompoundTag tag, long now, boolean vow) {
        List<LivingEntity> foes = new ArrayList<>(6);
        int max = NenRules.chains(vow);
        LivingEntity aimed = SkillTargets.lookFoe(p, NenRules.conjRange());
        if (aimed != null) foes.add(aimed);
        for (LivingEntity e : SkillTargets.foes(p, p.position(), NenRules.conjRange())) {
            if (foes.size() >= max) break;
            if (e != aimed && p.hasLineOfSight(e)) foes.add(e);
        }
        if (foes.isEmpty()) { SkillUtil.fail(p, SkillMsg.t("nen.no_target", "Nothing in reach.")); return false; }
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.CONJ)))) return false;
        double mul = NenSkill.hatsuMul(inst, p, NenRules.CONJ, vow);
        for (int i = 0; i < NenRules.chainSlots(); i++) tag.remove(CHAIN_T + i);      // a new casting replaces the old chains
        int slot = 0;
        for (LivingEntity e : foes) {
            if (slot >= NenRules.chainSlots()) break;
            tag.putUUID(CHAIN_T + slot, e.getUUID());
            tag.putLong(CHAIN_E + slot, now + BalanceLaw.controlTicks(e, NenRules.chainTicks()));
            SkillDamage.hit(p, e, NenRules.conjDamage() * mul, mastery(inst), SkillDamage.Kind.MAGIC);
            NenFx.link(p, e, hand(p), NenRules.typeColor(NenRules.T_CONJ), 14);
            slot++;
        }
        tag.putInt(CHAIN_N, slot);
        tag.putDouble(CHAIN_MUL, mul);
        tag.putLong(CHAIN_FX, now);
        busy(tag, now + NenRules.chainTicks() + 20);
        return true;
    }

    private static void chainStep(ManasSkillInstance inst, ServerPlayer p, CompoundTag tag, long now) {
        int n = tag.getInt(CHAIN_N);
        if (n <= 0 || now % NenRules.chainEvery() != 0) return;
        double mul = tag.getDouble(CHAIN_MUL);
        boolean hitNow = now % NenRules.chainHitEvery() == 0, fxNow = now - tag.getLong(CHAIN_FX) >= 10;
        int alive = 0;
        for (int i = 0; i < Math.min(n, NenRules.chainSlots()); i++) {
            if (!tag.hasUUID(CHAIN_T + i)) continue;
            Entity e = p.serverLevel().getEntity(tag.getUUID(CHAIN_T + i));
            long end = tag.getLong(CHAIN_E + i);
            if (!(e instanceof LivingEntity le) || !le.isAlive() || now >= end || le.distanceToSqr(p) > NenRules.chainLeash() * NenRules.chainLeash()) {
                if (e instanceof LivingEntity le2 && le2.isAlive() && now >= end) {          // it ran its time: the chain bursts
                    SkillDamage.hit(p, le2, NenRules.chainBurstDamage() * mul, mastery(inst), SkillDamage.Kind.MAGIC);
                    NenFx.burst(p, centre(le2), NenRules.typeColor(NenRules.T_CONJ), 1f, false);
                }
                tag.remove(CHAIN_T + i);
                continue;
            }
            alive++;
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 4, false, false, false));
            Vec3 dm = le.getDeltaMovement();
            le.setDeltaMovement(dm.x * 0.1, Math.min(dm.y, 0.0), dm.z * 0.1);
            if (le instanceof ServerPlayer) le.hurtMarked = true;
            if (hitNow) SkillDamage.hit(p, le, NenRules.chainDamage() * mul, mastery(inst), SkillDamage.Kind.MAGIC);
            if (fxNow) NenFx.link(p, le, hand(p), NenRules.typeColor(NenRules.T_CONJ), 14);
        }
        if (fxNow) tag.putLong(CHAIN_FX, now);
        if (alive == 0) tag.putInt(CHAIN_N, 0);
    }

    // ------------------------------------------------------------------ Manipulation: the needle
    /** A needle into the creature you look at: for as long as the balance law allows it is weakened, read (your blows do more to it) and, if it is a mob, turned on the nearest other foe every half second. A player is only weakened, slowed and made dizzy. */
    static boolean manip(ManasSkillInstance inst, ServerPlayer p, CompoundTag tag, long now, boolean vow) {
        LivingEntity t = SkillTargets.lookFoe(p, NenRules.manipRange());
        if (t == null) { SkillUtil.fail(p, SkillMsg.t("nen.no_target", "Nothing in reach.")); return false; }
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.MANIP)))) return false;
        int ticks = BalanceLaw.controlTicks(t, NenRules.needleTicks(vow));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1, false, true, true));
        if (!(t instanceof Mob)) {
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1, false, true, true));
            t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, Math.min(ticks, 60), 0, false, false, true));
        }
        mark(t, p, ticks, NenRules.needleMark(vow));
        tag.putUUID(NEEDLE_T, t.getUUID());
        tag.putLong(NEEDLE_E, now + ticks);
        tag.putLong(NEEDLE_NEXT, now + NenRules.needleEvery());
        busy(tag, now + ticks + 10);
        if (t instanceof Mob m) redirect(p, m);
        NenFx.needle(p, p.getEyePosition().add(0, -0.3, 0), centre(t), C_NEEDLE, 40);
        return true;
    }

    private static void redirect(ServerPlayer p, Mob m) {
        LivingEntity other = null;
        for (LivingEntity e : SkillTargets.foes(p, m.position(), NenRules.manipSearch())) if (e != m) { other = e; break; }
        m.setTarget(other);                  // null when there is nobody else: it loses its mind and stands there
    }

    private static void needleStep(ManasSkillInstance inst, ServerPlayer p, CompoundTag tag, long now) {
        long end = tag.getLong(NEEDLE_E);
        if (end <= 0 || now < tag.getLong(NEEDLE_NEXT)) return;
        Entity e = tag.hasUUID(NEEDLE_T) ? p.serverLevel().getEntity(tag.getUUID(NEEDLE_T)) : null;
        if (!(e instanceof LivingEntity le) || !le.isAlive() || now >= end) { tag.putLong(NEEDLE_E, 0L); return; }
        tag.putLong(NEEDLE_NEXT, now + NenRules.needleEvery());
        if (le instanceof Mob m) redirect(p, m);
        if (((end - now) / NenRules.needleEvery()) % 2 == 0) NenFx.needle(p, p.getEyePosition().add(0, -0.3, 0), centre(le), C_NEEDLE, 24);
    }

    // ------------------------------------------------------------------ Specialization: the field
    /** A field round you for six seconds (nine under a vow): once a second the foes in it are weakened and hurt, and up to two of them lose a buff, which becomes yours. */
    static boolean spec(ManasSkillInstance inst, ServerPlayer p, CompoundTag tag, long now, boolean vow) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.SPEC)))) return false;
        tag.putLong(FIELD_E, now + NenRules.fieldTicks(vow));
        tag.putLong(FIELD_NEXT, now + 2);
        tag.putDouble(FIELD_MUL, NenSkill.hatsuMul(inst, p, NenRules.SPEC, vow));
        tag.putBoolean(FIELD_VOW, vow);
        busy(tag, now + NenRules.fieldTicks(vow) + 10);
        NenFx.domain(p, NenRules.fieldRadius(vow), NenRules.fieldTicks(vow), NenRules.typeColor(NenRules.T_SPEC));
        return true;
    }

    private static void fieldStep(ManasSkillInstance inst, ServerPlayer p, CompoundTag tag, long now) {
        long end = tag.getLong(FIELD_E);
        if (end <= now || now < tag.getLong(FIELD_NEXT)) return;
        tag.putLong(FIELD_NEXT, now + NenRules.fieldEvery());
        boolean vow = tag.getBoolean(FIELD_VOW);
        double dmg = NenRules.fieldDamage() * tag.getDouble(FIELD_MUL);
        int stolen = 0;
        for (LivingEntity e : SkillTargets.foes(p, p.position(), NenRules.fieldRadius(vow))) {
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, NenRules.fieldWeakTicks(), 0, false, true, true));
            SkillDamage.hit(p, e, dmg, mastery(inst), SkillDamage.Kind.MAGIC);
            if (stolen < NenRules.fieldSteals() && steal(p, e)) stolen++;
        }
    }

    /** Takes the first beneficial effect of a creature and gives it to the owner (at most 12.5 s, at most Level III). True when one was taken. */
    private static boolean steal(ServerPlayer p, LivingEntity e) {
        for (MobEffectInstance fx : new ArrayList<>(e.getActiveEffects())) {
            Holder<MobEffect> h = fx.getEffect();
            if (!h.value().isBeneficial()) continue;
            int ticks = NenRules.stealTicks(fx.getDuration()), amp = NenRules.stealAmp(fx.getAmplifier());
            e.removeEffect(h);
            p.addEffect(new MobEffectInstance(h, ticks, amp, false, true, true));
            NenFx.bullet(p, centre(e), p.position().add(0, 1.2, 0), 0xFF000000 | h.value().getColor());
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ the tick and the clean-up
    /** Runs what the last press left going. One tag read when nothing is: {@link #BUSY} is the latest time anything can still be running. */
    static void tick(ManasSkillInstance inst, ServerPlayer p, CompoundTag tag, long now) {
        if (tag.getLong(BUSY) <= now) return;
        if (!p.isAlive()) { clearAll(tag); return; }
        volleyStep(inst, p, tag, now);
        chainStep(inst, p, tag, now);
        needleStep(inst, p, tag, now);
        fieldStep(inst, p, tag, now);
    }

    /** Ends everything that runs after a press (death, switching the skill off). */
    static void clearAll(CompoundTag tag) {
        tag.putInt(VOLLEY_LEFT, 0);
        tag.putInt(CHAIN_N, 0);
        for (int i = 0; i < NenRules.chainSlots(); i++) tag.remove(CHAIN_T + i);
        tag.putLong(NEEDLE_E, 0L);
        tag.putLong(FIELD_E, 0L);
        tag.putLong(BUSY, 0L);
    }
}
