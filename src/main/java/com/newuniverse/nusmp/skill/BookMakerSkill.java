package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.FictionRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Book Maker (Kumagawa Misogi): pin your target with giant screws and drag it down to your level. Pin: the creature you look at takes a
 * screw (damage), is slowed to a crawl (capped by the balance law's control rules), weakened, its digging and attacking slowed, and is
 * driven to the ground. Barrage (unlocked by mastery): the same pin on every foe in a cone ten blocks long, with less damage each.
 * Allies, pets and summons are never targeted. All Fiction (the Ultimate) is a separate, stronger skill. Numbers: {@link FictionRules}.
 */
public class BookMakerSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("book_maker", "Book Maker",
            "Unique. Pin your target with giant screws and drag it down to your level. Once mastered, Barrage pins every foe in front of you.");

    public BookMakerSkill() { super(SkillType.UNIQUE, TEXT, false); }

    @Override public int getModes(ManasSkillInstance i) { return FictionRules.modes(); }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean r) { return mode == FictionRules.PIN ? FictionRules.BARRAGE : FictionRules.PIN; }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return mode == FictionRules.BARRAGE ? "bookmaker.barrage" : "bookmaker.pin"; }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        return mode == FictionRules.BARRAGE ? SkillMsg.mode("bookmaker.barrage", "Barrage") : SkillMsg.mode("bookmaker.pin", "Pin");
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
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(FictionRules.pinCost()))) return;
            pin(player, target, FictionRules.pinDamage(tier), tier, mastery);
            cooldown(instance, mode, DMUtil.cooldown(FictionRules.pinCooldown()));
        } else if (mode == FictionRules.BARRAGE) {
            if (!FictionRules.barrageUnlocked(tier)) {
                SkillUtil.fail(player, SkillMsg.t("bookmaker.locked", "Master Book Maker to unlock Barrage."));
                return;
            }
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(FictionRules.barrageCost()))) return;
            for (LivingEntity t : SkillTargets.foesInCone(player, FictionRules.barrageRange(), FictionRules.barrageCone())) {
                pin(player, t, FictionRules.pinDamage(tier) * 0.6, tier, mastery);
            }
            cooldown(instance, mode, DMUtil.cooldown(FictionRules.barrageCooldown()));
        } else {
            return;
        }
        SkillFx.sound(level, player.position(), SoundEvents.ANVIL_PLACE, 1.0F, 0.6F);
        SkillUtil.castVfx(player, 0xFF8A8A9A);
        grow(instance, player, 1);
    }

    /** One screw: damage, crawl, weaken, drag down. */
    private static void pin(ServerPlayer player, LivingEntity target, double damage, Tier tier, double mastery) {
        SkillDamage.hit(player, target, damage, mastery);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(target, FictionRules.pinSlowTicks()), 5));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, FictionRules.pinWeakTicks(), FictionRules.pinWeakAmp(tier)));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 2));
        target.setDeltaMovement(target.getDeltaMovement().x * 0.2, Math.min(target.getDeltaMovement().y, -0.8), target.getDeltaMovement().z * 0.2);
        target.hurtMarked = true;
        ServerLevel level = player.serverLevel();
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.5, 0);
        SkillFx.shape(level, VfxShape.SEAL_CHAINS, at, at.add(0, 1, 0), 0xFF9A9AAA, 24, 1.4f);
        SkillFx.spray(level, false, at, 6, 0.4, 0.6, 0.4, 0.1);
    }
}
