package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.requip.Armour;
import com.newuniverse.nusmp.requip.RequipRules;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Requip: The Knight (Erza Scarlet), a standalone Ultimate skill taught by the book Titania's Requip. One MODE per armour (the order of {@link Armour}, switched with the mode key like every other
 * multi-mode skill) and a last mode, Dismiss.
 * <ul>
 *   <li>Press the skill key in an armour's mode while another armour (or none) is worn: the armour is put on. Pieces of the old armour shatter out, the new armour's pieces forge in, one by one, with a
 *       flash of light and the armour's theme particles: the Djinn Full Equip animation ({@code client.aura.EquipAnimation}), drawn as a render layer, never in an armour slot. Costs a share of the
 *       max Aura (else Magicule), 60 percent of it when swapping from another armour, and a short cooldown.</li>
 *   <li>Press it again in the mode of the armour you wear: its first special attack; sneaking: its second.</li>
 *   <li>The Dismiss mode takes the armour off (it shatters out).</li>
 * </ul>
 * Each armour adds attribute modifiers (speed, attack damage, armour, toughness, attack speed, knockback resistance), takes a share off each damage class, may put its weapon in the hand, flies (the
 * winged ones), and costs a share of the Aura every second to keep. The table is {@link Armour}; the arithmetic is {@link RequipRules}. Armours past the first few unlock with mastery.
 */
public class ErzaRequipSkill extends NUSkill {
    static final String WORN = "Worn", PENDING = "Pending", SWAP_AT = "SwapAt", AURA_AT = "AuraAt", GUARD_UNTIL = "GuardUntil", NONE_NAME = "dismiss";

    static final SkillText TEXT = SkillText.of("erza_requip", "Requip: The Knight",
            "Ultimate. Equip Magic: change between fourteen armours, one per mode, each with its own strength, resistances, weapon and two special attacks. Press the skill key in an armour's mode to put it on, again to attack (sneak for the second attack); the last mode takes the armour off.");

    public ErzaRequipSkill() { super(SkillType.ULTIMATE, TEXT, true); }

    // ---------------------------------------------------------------- modes
    @Override public int getModes(ManasSkillInstance i) { return RequipRules.MODES; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) {
        return reverse ? (mode + RequipRules.MODES - 1) % RequipRules.MODES : (mode + 1) % RequipRules.MODES;
    }
    @Override public String getModeId(ManasSkillInstance i, int mode) {
        Armour a = Armour.byOrdinal(mode);
        return "erza." + (a == null ? NONE_NAME : a.id());
    }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        Armour a = Armour.byOrdinal(mode);
        return a == null ? SkillMsg.mode("erza." + NONE_NAME, "Dismiss Armor") : SkillMsg.mode("erza." + a.id(), a.displayName());
    }

    // ---------------------------------------------------------------- state (stored on the skill instance, so it survives relogs and syncs)
    static Armour worn(ManasSkillInstance i) {
        var tag = i.getOrCreateTag();
        return tag.contains(WORN) ? Armour.byOrdinal(tag.getInt(WORN)) : null;
    }

    static Armour pending(ManasSkillInstance i) {
        var tag = i.getOrCreateTag();
        return tag.contains(PENDING) ? Armour.byOrdinal(tag.getInt(PENDING)) : null;
    }

    private static boolean guarding(ManasSkillInstance i, LivingEntity who) {
        return who.level().getGameTime() < i.getOrCreateTag().getLong(GUARD_UNTIL);
    }

    // ---------------------------------------------------------------- the key
    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer p) || !p.isAlive()) return;
        if (pending(instance) != null) { SkillUtil.fail(p, SkillMsg.t("erza.changing", "The armor is still changing.")); return; }
        Armour target = Armour.byOrdinal(mode), now = worn(instance);
        if (target == null) {                                                                 // the Dismiss mode
            if (now == null) { SkillUtil.fail(p, SkillMsg.t("erza.none", "You wear no armor.")); return; }
            dismiss(p, instance, now);
            return;
        }
        if (now == target) {                                                                  // already in it: attack
            Armour.Attack atk = p.isShiftKeyDown() ? target.secondary() : target.primary();
            if (ErzaRequipAttacks.cast(p, instance, target, atk, masteryFrac(instance))) grow(instance, p, 1);
            else instance.markDirty();
            return;
        }
        if (instance.onCoolDown(mode)) { SkillUtil.fail(p, SkillMsg.t("erza.swap_cooldown", "Your magic is still settling.")); return; }
        boolean priv = p.isCreative() || p.hasPermissions(2);
        double mastery = masteryFrac(instance);
        if (!RequipRules.unlocked(target, mastery, priv)) {
            SkillUtil.fail(p, SkillMsg.t("erza.locked", "%s is not yet yours: master Requip to %s percent.", target.displayName(), (int) Math.round(target.unlock() * 100)));
            return;
        }
        if (!EnergyBridge.aura(p, RequipRules.equipShare(target, now != null), RequipRules.EQUIP_FLOOR)) {
            SkillUtil.fail(p, SkillMsg.t("erza.no_power", "Not enough magic power for %s.", target.displayName()));
            return;
        }
        startSwap(p, instance, now, target);
        cooldown(instance, mode, RequipRules.SWAP_COOLDOWN);
        grow(instance, p, 1);
    }

    /** Begins putting {@code target} on: if another armour is worn it shatters out first, and the new one forges in {@link RequipRules#SWAP_GAP} ticks later (from {@link #onTick}). */
    static void startSwap(ServerPlayer p, ManasSkillInstance inst, Armour old, Armour target) {
        if (old == null) { finishEquip(p, inst, target); return; }
        ErzaRequipFx.clearModifiers(p);
        ErzaRequipFx.removeWeapons(p);
        ErzaRequipFx.shatter(p, old);
        var tag = inst.getOrCreateTag();
        tag.remove(WORN);
        tag.putInt(PENDING, target.ordinal());
        tag.putLong(SWAP_AT, p.level().getGameTime() + RequipRules.SWAP_GAP);
        inst.markDirty();
    }

    /** The new armour is on: modifiers, the aura (it forges in on the clients), the flash and particles, and the weapon. */
    static void finishEquip(ServerPlayer p, ManasSkillInstance inst, Armour target) {
        var tag = inst.getOrCreateTag();
        tag.remove(PENDING);
        tag.putInt(WORN, target.ordinal());
        tag.putLong(AURA_AT, p.level().getGameTime());
        tag.putLong(GUARD_UNTIL, 0L);
        ErzaRequipFx.applyModifiers(p, target, masteryFrac(inst));
        ErzaRequipFx.setAura(p, target, RequipRules.AURA_TICKS);
        ErzaRequipFx.flash(p, target);
        ErzaRequipFx.giveWeapon(p, target);
        p.displayClientMessage(SkillMsg.t("erza.equipped", "%s", target.displayName()).withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(target.theme())), true);
        inst.markDirty();
    }

    /** The armour comes off: it shatters out, the bonuses and the weapon go with it. */
    static void dismiss(ServerPlayer p, ManasSkillInstance inst, Armour old) {
        ErzaRequipFx.clearModifiers(p);
        ErzaRequipFx.removeWeapons(p);
        ErzaRequipFx.shatter(p, old);
        var tag = inst.getOrCreateTag();
        tag.remove(WORN);
        tag.remove(PENDING);
        tag.putLong(GUARD_UNTIL, 0L);
        inst.markDirty();
    }

    /** Everything off at once, nothing shatters (death, forgetting the skill). */
    static void hardOff(ServerPlayer p, ManasSkillInstance inst) {
        ErzaRequipFx.clearModifiers(p);
        ErzaRequipFx.removeWeapons(p);
        ErzaRequipFx.clearAura(p);
        var tag = inst.getOrCreateTag();
        tag.remove(WORN);
        tag.remove(PENDING);
        tag.putLong(GUARD_UNTIL, 0L);
        inst.markDirty();
    }

    // ---------------------------------------------------------------- admin entry points (the /requip command)
    /** Puts an armour on a player without cost, cooldown or unlock. Returns a reason on failure, null on success. */
    public static String adminEquip(ServerPlayer p, Armour a) {
        var inst = SkillAPI.getSkillsFrom(p).getSkill(NUSkills.ERZA_REQUIP.getId());
        if (inst.isEmpty()) return p.getName().getString() + " does not know Requip: The Knight (use /requip teach).";
        startSwap(p, inst.get(), worn(inst.get()), a);
        return null;
    }

    public static void adminDismiss(ServerPlayer p) {
        var inst = SkillAPI.getSkillsFrom(p).getSkill(NUSkills.ERZA_REQUIP.getId());
        if (inst.isEmpty()) return;
        Armour w = worn(inst.get());
        if (w != null) dismiss(p, inst.get(), w);
    }

    // ---------------------------------------------------------------- every tick
    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return true; }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity living) {
        if (!(living instanceof ServerPlayer p) || !p.isAlive()) return;
        long now = p.level().getGameTime();
        var tag = instance.getOrCreateTag();
        Armour pend = pending(instance);
        if (pend != null) {
            if (now >= tag.getLong(SWAP_AT)) finishEquip(p, instance, pend);
            return;
        }
        Armour a = worn(instance);
        if (a == null) {
            if (p.tickCount % 40 == 0 && (ErzaRequipFx.strayWeapon(p, null))) ErzaRequipFx.removeWeapons(p);   // a weapon left over from a lost armour
            return;
        }
        ErzaRequipFx.flightTick(p, a);
        if (p.tickCount % 20 != 0) return;
        // the upkeep: the armour is kept up with the Aura (else Magicule); when the power runs dry it comes off
        if (!EnergyBridge.aura(p, a.cost().upkeep(), RequipRules.UPKEEP_FLOOR)) {
            p.displayClientMessage(SkillMsg.t("erza.dry", "Your magic power runs dry: the armor fades.").withStyle(net.minecraft.ChatFormatting.RED), true);
            dismiss(p, instance, a);
            return;
        }
        if (a.upkeepSelfDamage() > 0) {
            float self = RequipRules.selfDamage(p.getHealth(), a.upkeepSelfDamage() / Math.max(1f, p.getHealth()));
            if (self > 0f) { p.setHealth(p.getHealth() - self); p.serverLevel().broadcastEntityEvent(p, (byte) 2); }
        }
        if (!ErzaRequipFx.hasModifiers(p, a, masteryFrac(instance))) ErzaRequipFx.applyModifiers(p, a, masteryFrac(instance));   // after a relog
        if (ErzaRequipFx.strayWeapon(p, a)) ErzaRequipFx.removeWeapons(p);
        if (!PlayerAuras.active(p, Aura.ERZA) || now - tag.getLong(AURA_AT) >= RequipRules.AURA_REFRESH) {
            ErzaRequipFx.setAura(p, a, RequipRules.AURA_TICKS);
            tag.putLong(AURA_AT, now);
        }
        if (a == Armour.SEA_EMPRESS && p.isInWater()) {                                        // the Sea Empress's underwater bonus
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WATER_BREATHING, 60, 0, true, false));
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DOLPHINS_GRACE, 60, 1, true, false));
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 240, 0, true, false));
        }
        instance.markDirty();
    }

    // ---------------------------------------------------------------- damage
    static Armour.DamageClass classify(DamageSource s) {
        if (s.is(DamageTypeTags.IS_FIRE)) return Armour.DamageClass.FIRE;
        if (s.is(DamageTypeTags.IS_LIGHTNING)) return Armour.DamageClass.LIGHTNING;
        if (s.is(DamageTypeTags.IS_DROWNING)) return Armour.DamageClass.WATER;
        if (s.is(DamageTypeTags.IS_EXPLOSION)) return Armour.DamageClass.EXPLOSION;
        if (s.is(DamageTypeTags.IS_PROJECTILE)) return Armour.DamageClass.PROJECTILE;
        if (s.is(DamageTypes.MAGIC) || s.is(DamageTypes.INDIRECT_MAGIC) || s.is(DamageTypes.WITHER) || s.is(DamageTypeTags.WITCH_RESISTANT_TO)) return Armour.DamageClass.MAGIC;
        return Armour.DamageClass.PHYSICAL;
    }

    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        Armour a = worn(instance);
        if (a == null || pending(instance) != null) return true;
        boolean guard = guarding(instance, owner);
        float before = amount.get();
        amount.set(RequipRules.afterResist(a, classify(source), before, guard));
        double reflect = RequipRules.reflect(a, guard);
        Entity direct = source.getDirectEntity();
        if (reflect > 0 && direct instanceof LivingEntity attacker && attacker != owner && !source.is(DamageTypes.THORNS) && classify(source) == Armour.DamageClass.PHYSICAL) {
            attacker.hurt(owner.damageSources().thorns(owner), (float) (before * reflect));
        }
        return true;
    }

    @Override
    public boolean onDamageEntity(ManasSkillInstance instance, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        Armour a = worn(instance);
        if (a == null || pending(instance) != null) return true;
        switch (a) {
            case FLAME_EMPRESS, PURGATORY -> target.igniteForSeconds(3);
            case NAKAGAMI -> {
                amount.set((float) (amount.get() * (1.0 + RequipRules.antiCasterBonus(Nullification.maxEP(target)))));
                if (owner.tickCount % 40 == 0) Nullification.interfere(target, 4, 0.5f, 0.3f);
            }
            case SEA_EMPRESS -> { if (owner.isInWater()) amount.set(amount.get() * 1.25f); }
            default -> { }
        }
        return true;
    }

    @Override
    public boolean onDeath(ManasSkillInstance instance, LivingEntity owner, DamageSource source) {
        if (owner instanceof ServerPlayer p) hardOff(p, instance);
        return true;
    }
}
