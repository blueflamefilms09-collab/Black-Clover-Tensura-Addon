package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.EnergyBridge;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The Half Equip specials added in 1.93.0 (one for each of the twenty-four new Djinn), the part that touches the world. The numbers that decide how
 * hard the blow lands are {@link TraitRules}'s; this is what else the blow does. Every special is bounded (at most eight creatures touched, short
 * effects, nothing placed or broken, no item is ever destroyed: a wrecked armour piece keeps one point of durability) and runs inside the engine's
 * "in bonus" guard, so what it hurts cannot trigger another rider.
 */
final class DjinnTraits {
    private DjinnTraits() {}

    private static final EquipmentSlot[] ARMOUR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int MAX_TOUCHED = 8;

    /** The harmful effects a creature carries (the Carrion special reads it). */
    static int ailments(LivingEntity t) {
        int n = 0;
        for (MobEffectInstance m : t.getActiveEffects()) if (!m.getEffect().value().isBeneficial()) n++;
        return n;
    }

    /** What else the blow does. {@code bonus} is the rider just added, {@code streak} the blows in a row before this one. */
    static void apply(ServerPlayer p, Djinn.Special sp, LivingEntity target, float bonus, int streak) {
        switch (sp) {
            case KNEEL -> {
                target.setDeltaMovement(target.getDeltaMovement().multiply(0.2, 0.0, 0.2).add(0.0, -0.7, 0.0));
                target.hurtMarked = true;
            }
            case DRAG -> DjinnMagic.pull(target, p.position(), 0.9, 0.1);
            case PLUNDER -> {
                int taken = 0;
                for (MobEffectInstance m : new ArrayList<>(target.getActiveEffects())) {
                    if (taken >= 2) break;
                    Holder<MobEffect> h = m.getEffect();
                    if (h.value().isBeneficial()) {
                        target.removeEffect(h);
                        taken++;
                    }
                }
                if (taken > 0) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40 + 20 * taken, 0, true, false));
            }
            case FORESIGHT -> p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false));
            case WRECK -> {
                for (EquipmentSlot slot : ARMOUR) {
                    ItemStack st = target.getItemBySlot(slot);
                    if (st.isEmpty() || !st.isDamageableItem()) continue;
                    int max = st.getMaxDamage();
                    st.setDamageValue(Math.min(max - 1, st.getDamageValue() + Math.max(1, max / 50)));
                }
            }
            case HUSH -> {
                EnergyBridge.effect(target, "silence", 80, 0);
                if (target instanceof Mob m) {
                    m.setTarget(null);
                    m.getNavigation().stop();
                }
            }
            case MEND -> {
                BalanceLaw.heal(p, bonus * 0.5f);
                for (ServerPlayer a : DjinnTargets.alliesNear(p, 6.0)) if (a != p) BalanceLaw.heal(a, bonus * 0.4f);
            }
            case DAZZLE -> {
                for (LivingEntity o : near(p, target, 3.5)) DjinnFx.effect(o, "minecraft:glowing", 80, 0);
            }
            case TRAMPLE -> DjinnMagic.push(target, p.position(), 0.8, 0.25);
            case SHOCKWAVE -> {
                for (LivingEntity o : near(p, target, 3.5)) DjinnMagic.push(o, target.position(), 0.7, 0.2);
            }
            case CINDER -> {
                int n = 0;
                for (LivingEntity o : near(p, target, 3.5)) {
                    if (n++ >= 3) break;
                    o.setRemainingFireTicks(Math.max(o.getRemainingFireTicks(), 100));
                }
            }
            case SHELL -> p.setAbsorptionAmount(Math.min(6.0f, p.getAbsorptionAmount() + 1.0f));
            case QUICKEN -> p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, true, false));
            case BEGUILE -> {
                if (target instanceof Mob m) {
                    LivingEntity next = null;
                    double best = Double.MAX_VALUE;
                    for (LivingEntity o : DjinnTargets.foesNear(p, target.position(), 8.0)) {
                        double dd = o.distanceToSqr(target);
                        if (o != target && DjinnTargets.isMob(o) && dd < best) {
                            best = dd;
                            next = o;
                        }
                    }
                    if (next != null) m.setTarget(next);
                }
            }
            case REVEAL -> target.removeEffect(MobEffects.INVISIBILITY);
            case TRANSMUTE -> {
                Holder<MobEffect>[] boons = boons();
                String[] banes = {"minecraft:slowness", "minecraft:weakness", "minecraft:poison"};
                p.addEffect(new MobEffectInstance(boons[p.getRandom().nextInt(boons.length)], 80, 0, true, false));
                DjinnFx.effect(target, banes[p.getRandom().nextInt(banes.length)], 80, 0);
            }
            case RIME -> {
                for (LivingEntity o : near(p, target, 3.5)) {
                    o.setTicksFrozen(Math.min(o.getTicksRequiredToFreeze() + 20, o.getTicksFrozen() + 40));
                    DjinnFx.effect(o, "minecraft:slowness", 60, 0);
                }
            }
            case ICEWIND -> {
                DjinnMagic.push(target, p.position(), 1.0, 0.1);
                target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 20, target.getTicksFrozen() + 40));
            }
            case TRIPLE -> {
                int n = 0;
                for (LivingEntity o : near(p, target, 4.0)) {
                    if (n++ >= 2) break;
                    float share = bonus * 0.6f;
                    o.hurt(p.damageSources().indirectMagic(p, p), Math.max(1f, o instanceof Player ? Math.min(share, TraitRules.PLAYER_RIDER_CAP) : share));
                    o.setRemainingFireTicks(Math.max(o.getRemainingFireTicks(), 60));
                    DjinnFx.line(p.serverLevel(), target.position().add(0, 1, 0), o.position().add(0, 1, 0), DjinnFx.particle(Affinity.HEAT), 8);
                }
            }
            case ROAR -> {
                int n = 0;
                for (LivingEntity o : DjinnTargets.foesNear(p, p.position(), 5.0)) {
                    if (o == target) continue;
                    if (n++ >= MAX_TOUCHED) break;
                    DjinnFx.effect(o, "minecraft:weakness", 60, 0);
                    DjinnMagic.push(o, p.position(), 0.5, 0.1);
                }
            }
            case DRUMBEAT -> {
                if (TraitRules.drumbeat(streak)) DjinnMagic.push(target, p.position(), 0.9, 0.2);
            }
            default -> { }                                  // Bloodlust, Hunt and Carrion are in the share (TraitRules), not in what else the blow does
        }
    }

    @SuppressWarnings("unchecked")
    private static Holder<MobEffect>[] boons() {
        return new Holder[]{MobEffects.MOVEMENT_SPEED, MobEffects.DAMAGE_BOOST, MobEffects.REGENERATION, MobEffects.DAMAGE_RESISTANCE};
    }

    /** Up to eight foes within {@code r} of the target (not the target), nearest to the caster first. */
    private static List<LivingEntity> near(ServerPlayer p, LivingEntity target, double r) {
        List<LivingEntity> out = new ArrayList<>();
        Vec3 c = target.position();
        for (LivingEntity o : DjinnTargets.foesNear(p, c, r)) {
            if (o == target) continue;
            out.add(o);
            if (out.size() >= MAX_TOUCHED) break;
        }
        return out;
    }
}
