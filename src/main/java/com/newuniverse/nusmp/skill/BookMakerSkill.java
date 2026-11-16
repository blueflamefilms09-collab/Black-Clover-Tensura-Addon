package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.skill.rules.FictionRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

/**
 * Book Maker (Kumagawa Misogi): pin your target with giant screws and drag it down to your level. Pin: the creature you look at takes a
 * screw (damage), is slowed to a crawl (capped by the balance law's control rules), weakened, its digging and attacking slowed, and is
 * driven to the ground. Barrage (unlocked by mastery): the same pin on every foe in a cone ten blocks long, with less damage each.
 * Allies, pets and summons are never targeted. All Fiction (the Ultimate) is a separate, stronger skill. Numbers: {@link FictionRules}.
 * Lead-in modes (server setting allFictionGod, docs/all_fiction_rework.md): Equalize (strips the target's boons, weakens it, and once mastered silences it: "bring them down to your level") and
 * Glimpse of the Fiction (mastered only: a short window in which a quarter of your maximum health of harm is made not to have happened, the first taste of All Fiction).
 */
public class BookMakerSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("book_maker", "Book Maker",
            "Unique. Pin your target with giant screws and drag it down to your level. Equalize strips its boons. Once mastered, Barrage pins every foe in front of you, Equalize silences, and Glimpse of the Fiction undoes a little harm.");

    public BookMakerSkill() { super(SkillType.UNIQUE, TEXT, false); }

    private static final String GLIMPSE_UNTIL = "GlimpseUntil", GLIMPSE_POOL = "GlimpsePool";

    /** Two modes (Pin, Barrage) as before, four with the lead-in modes on (server setting allFictionGod). */
    private static int wheel() { return FictionRules.bookMakerModes(NUConfig.allFictionGod()); }

    @Override public int getModes(ManasSkillInstance i) { return wheel(); }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean r) { return Math.floorMod(mode + (r ? -1 : 1), wheel()); }

    @Override
    public String getModeId(ManasSkillInstance i, int mode) {
        return switch (mode) { case FictionRules.BARRAGE -> "bookmaker.barrage"; case FictionRules.EQUALIZE -> "bookmaker.equalize"; case FictionRules.GLIMPSE -> "bookmaker.glimpse"; default -> "bookmaker.pin"; };
    }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        return switch (mode) {
            case FictionRules.BARRAGE -> SkillMsg.mode("bookmaker.barrage", "Barrage");
            case FictionRules.EQUALIZE -> SkillMsg.mode("bookmaker.equalize", "Equalize");
            case FictionRules.GLIMPSE -> SkillMsg.mode("bookmaker.glimpse", "Glimpse of the Fiction");
            default -> SkillMsg.mode("bookmaker.pin", "Pin");
        };
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, SkillMsg.t("bookmaker.cooldown", "Book Maker is on cooldown.")); return; }
        Tier tier = tier(instance, player);
        ServerLevel level = player.serverLevel();
        double mastery = masteryFrac(instance);

        if (mode == FictionRules.PIN) {
            LivingEntity target = SkillTargets.lookFoe(player, FictionRules.pinRange());
            if (target == null) { SkillUtil.fail(player, SkillMsg.t("bookmaker.no_target", "No target.")); return; }
            if (!pay(instance, player, mode, DMUtil.cost(FictionRules.pinCost()))) return;
            pin(player, target, FictionRules.pinDamage(tier), tier, mastery, instance);
            cooldown(instance, mode, DMUtil.cooldown(FictionRules.pinCooldown()));
        } else if (mode == FictionRules.BARRAGE) {
            if (!FictionRules.barrageUnlocked(tier)) {
                SkillUtil.fail(player, SkillMsg.t("bookmaker.locked", "Master Book Maker to unlock Barrage."));
                return;
            }
            if (!pay(instance, player, mode, DMUtil.cost(FictionRules.barrageCost()))) return;
            for (LivingEntity t : SkillTargets.foesInCone(player, FictionRules.barrageRange(), FictionRules.barrageCone())) {
                pin(player, t, FictionRules.pinDamage(tier) * 0.6, tier, mastery, instance);
            }
            cooldown(instance, mode, DMUtil.cooldown(FictionRules.barrageCooldown()));
        } else if (mode == FictionRules.EQUALIZE && NUConfig.allFictionGod()) {
            LivingEntity target = SkillTargets.lookFoe(player, FictionRules.equalizeRange());
            if (target == null) { SkillUtil.fail(player, SkillMsg.t("bookmaker.no_target", "No target.")); return; }
            if (!pay(instance, player, mode, DMUtil.cost(FictionRules.equalizeCost()))) return;
            equalize(player, target, tier);
            cooldown(instance, mode, DMUtil.cooldown(FictionRules.equalizeCooldown()));
        } else if (mode == FictionRules.GLIMPSE && NUConfig.allFictionGod()) {
            if (!FictionRules.glimpseUnlocked(tier)) {
                SkillUtil.fail(player, SkillMsg.t("bookmaker.glimpse_locked", "Master Book Maker to unlock Glimpse of the Fiction."));
                return;
            }
            if (!pay(instance, player, mode, DMUtil.cost(FictionRules.glimpseCost()))) return;
            CompoundTag tag = instance.getOrCreateTag();
            tag.putLong(GLIMPSE_UNTIL, level.getGameTime() + FictionRules.glimpseTicks());
            tag.putDouble(GLIMPSE_POOL, FictionRules.glimpsePool(player.getMaxHealth()));
            instance.markDirty();
            SkillUtil.actionbar(player, SkillMsg.t("bookmaker.glimpse", "A little of the harm never happened.").withStyle(ChatFormatting.GRAY));
            cooldown(instance, mode, DMUtil.cooldown(FictionRules.glimpseCooldown()));
        } else {
            return;
        }
        SkillFx.sound(level, player.position(), SoundEvents.ANVIL_PLACE, 1.0F, 0.6F);
        SkillUtil.castVfx(player, 0xFF8A8A9A);
        grow(instance, player, 1);
    }

    /** Equalize: the target's boons are torn off (at most six), it is weakened, and a mastered Book Maker also silences it (balance-law control time). */
    private static void equalize(ServerPlayer player, LivingEntity target, Tier tier) {
        int stripped = 0;
        for (MobEffectInstance e : new ArrayList<>(target.getActiveEffects())) {
            if (stripped >= FictionRules.equalizeStripMax()) break;
            if (e.getEffect().value().isBeneficial() && target.removeEffect(e.getEffect())) stripped++;
        }
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, FictionRules.equalizeWeakTicks(), FictionRules.equalizeWeakAmp(tier)));
        if (FictionRules.equalizeSilences(tier)) EnergyBridge.effect(target, "silence", BalanceLaw.controlTicks(target, 100), 0);
        ServerLevel level = player.serverLevel();
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.5, 0);
        SkillFx.shape(level, VfxShape.SEAL_CHAINS, at, at.add(0, 1, 0), 0xFF9A9AAA, 24, 1.4f);
        SkillAnimeFx2.screw(player, target);
    }

    /** Glimpse of the Fiction: while its window is open hits are taken out of a small pool (a quarter of your maximum health) as if they never happened. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (amount.get() <= 0 || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !NUConfig.allFictionGod()) return true;
        CompoundTag tag = instance.getTag();
        if (tag == null || !FictionRules.windowOpen(tag.getLong(GLIMPSE_UNTIL), owner.level().getGameTime())) return true;
        double pool = tag.getDouble(GLIMPSE_POOL);
        double taken = FictionRules.annulAbsorb(pool, amount.get());
        amount.set((float) (amount.get() - taken));
        tag.putDouble(GLIMPSE_POOL, pool - taken);
        if (pool - taken <= 0.0) tag.putLong(GLIMPSE_UNTIL, 0L);
        return true;
    }

    /** One screw: damage, crawl, weaken, drag down. */
    private static void pin(ServerPlayer player, LivingEntity target, double damage, Tier tier, double mastery, ManasSkillInstance instance) {
        SkillDamage.hit(player, target, damage, mastery, instance);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(target, FictionRules.pinSlowTicks()), 5));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, FictionRules.pinWeakTicks(), FictionRules.pinWeakAmp(tier)));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 2));
        target.setDeltaMovement(target.getDeltaMovement().x * 0.2, Math.min(target.getDeltaMovement().y, -0.8), target.getDeltaMovement().z * 0.2);
        target.hurtMarked = true;
        ServerLevel level = player.serverLevel();
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.5, 0);
        SkillFx.shape(level, VfxShape.SEAL_CHAINS, at, at.add(0, 1, 0), 0xFF9A9AAA, 24, 1.4f);
        SkillFx.spray(level, false, at, 6, 0.4, 0.6, 0.4, 0.1);
        SkillAnimeFx2.screw(player, target);                                           // VFX wave 4: the giant screw (the wave 3 screw shape; at most 4 per press)
    }
}
