package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.FictionRules;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * All Fiction (Kumagawa Misogi): the Ultimate evolution of Book Maker (a standalone skill otherwise): make things never have happened.
 * Undo: your health returns to what it was five seconds ago and harmful effects vanish. Fiction: erase the creature you look at (a
 * boss, or a player when the config protects them, instead loses its effects and a quarter of its health). Passive: once every fifteen
 * minutes your own death becomes fiction and you stand up at full health. The five-second health memory is kept in the skill tag (keys
 * H0..H4, unchanged). Numbers: {@link FictionRules}.
 */
public class AllFictionSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("all_fiction", "All Fiction",
            "Ultimate. Make injuries, enemies and even your own death never have happened.");

    public AllFictionSkill() { super(SkillType.ULTIMATE, TEXT, true); }

    @Override public int getModes(ManasSkillInstance i) { return 2; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean r) { return mode == FictionRules.UNDO ? FictionRules.ERASE : FictionRules.UNDO; }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return mode == FictionRules.UNDO ? "fiction.undo" : "fiction.erase"; }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        return mode == FictionRules.UNDO ? SkillMsg.mode("fiction.undo", "Undo") : SkillMsg.mode("fiction.erase", "Fiction");
    }

    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return true; }

    /** Remembers your health once a second for the last five seconds. */
    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity living) {
        if (living.level().isClientSide || living.tickCount % 20 != 0) return;
        CompoundTag tag = instance.getOrCreateTag();
        float[] hist = new float[FictionRules.HISTORY_SECONDS];
        for (int i = 0; i < hist.length; i++) hist[i] = tag.getFloat("H" + i);
        FictionRules.push(hist, living.getHealth());
        for (int i = 0; i < hist.length; i++) tag.putFloat("H" + i, hist[i]);
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, SkillMsg.t("fiction.cooldown", "Not yet.")); return; }
        ServerLevel level = player.serverLevel();

        if (mode == FictionRules.UNDO) {
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(FictionRules.undoCost()))) return;
            float past = instance.getOrCreateTag().getFloat("H" + FictionRules.oldestIndex());
            player.setHealth(FictionRules.undoHealth(past, player.getHealth(), player.getMaxHealth()));
            SkillUtil.cleanse(player);
            SkillUtil.actionbar(player, SkillMsg.t("fiction.undo", "It's All Fiction.").withStyle(ChatFormatting.DARK_PURPLE));
            SkillFx.spray(level, true, player.position().add(0, 1, 0), 12, 0.5, 0.8, 0.5, 0.05);
            cooldown(instance, mode, DMUtil.cooldown(FictionRules.undoCooldown()));
        } else {
            LivingEntity target = SkillTargets.lookFoe(player, FictionRules.eraseRange());
            if (target == null) { SkillUtil.fail(player, SkillMsg.t("fiction.no_target", "No target.")); return; }
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(FictionRules.eraseCost()))) return;
            Vec3 at = target.position().add(0, target.getBbHeight() * 0.5, 0);
            SkillFx.spray(level, true, at, 12, 0.4, 0.8, 0.4, 0.05);
            if (FictionRules.erases(GodTier.fullyAffected(target))) {
                if (target instanceof ServerPlayer) GodTier.kill(player, target);   // a player is killed, never silently removed from the world
                else target.discard();                                              // erased: no body, no drops
            } else {
                target.removeAllEffects();
                GodTier.percentDamage(player, target, FictionRules.eraseBossShare());
            }
            cooldown(instance, mode, DMUtil.cooldown(FictionRules.eraseCooldown()));
        }
        SkillFx.sound(level, player.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 0.5F);
        SkillUtil.castVfx(player, 0xFF5A2A7A);
        grow(instance, player, 1);
    }

    /** Once per cooldown your own death becomes fiction. */
    @Override
    public boolean onDeath(ManasSkillInstance instance, LivingEntity owner, DamageSource source) {
        if (!(owner instanceof ServerPlayer player)) return true;
        if (player.level().getGameTime() < instance.getOrCreateTag().getLong("DeathReadyAt")) return true;
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        player.setHealth(player.getMaxHealth());
        player.removeAllEffects();
        player.clearFire();
        var server = player.getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(SkillMsg.t("fiction.death", "%s's death has been made fiction.", player.getName())
                    .withStyle(ChatFormatting.DARK_PURPLE), false);
        }
        instance.getOrCreateTag().putLong("DeathReadyAt", player.level().getGameTime() + DMUtil.cooldown(FictionRules.deathCooldown()));
        instance.markDirty();
        return false;
    }
}
