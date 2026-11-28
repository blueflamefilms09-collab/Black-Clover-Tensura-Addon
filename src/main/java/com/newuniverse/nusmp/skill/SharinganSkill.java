package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import io.github.manasmods.manascore.network.api.util.Changeable;

/**
 * Sharingan (Unique, 5.8.0): the eye ripped out of Kakashi in the eye-rip mini-game ({@code arc/kakashi/KakashiEyeRip}). Modes: Sharingan Eyes (30 seconds of sharpened sight: night vision, a little speed,
 * and a one-in-five chance to read a melee or arrow blow and dodge it; 90 second rest) and Read. It is the first stage: the owner will add its evolution, the Rinnegan, later (docs/bosses/kakashi.md).
 * Everything is server side; no tick work (a timestamp in the player's persistent data and one check when a blow lands).
 */
public class SharinganSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("sharingan", "Sharingan",
            "Unique. Torn from Kakashi's eye. Modes: Sharingan Eyes (30 seconds of night vision, speed and a one-in-five chance to read and dodge a blow; 90 second rest) and Read. It can one day evolve into the Rinnegan.");
    private static final String[] IDS = {"eyes", "read"};
    private static final String[] NAMES = {"Sharingan Eyes", "Read"};
    static final String UNTIL = "anime_multiverse_sharingan_until";
    public static final int EYES_TICKS = 600, REST_TICKS = 1800;
    public static final double DODGE = 0.2;

    public SharinganSkill() { super(SkillType.UNIQUE, TEXT, false); }

    @Override public int getModes(ManasSkillInstance instance) { return IDS.length; }

    @Override public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) { return Math.floorMod(mode + (reverse ? -1 : 1), IDS.length); }

    @Override public String getModeId(ManasSkillInstance instance, int mode) { return "sharingan." + IDS[Math.floorMod(mode, IDS.length)]; }

    @Override public Component getModeName(ManasSkillInstance instance, int mode) { return SkillMsg.mode("sharingan." + IDS[Math.floorMod(mode, IDS.length)], NAMES[Math.floorMod(mode, IDS.length)]); }

    @Override public boolean canTick(ManasSkillInstance i, LivingEntity e) { return false; }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer p) || !p.isAlive()) return;
        if (Math.floorMod(mode, IDS.length) == 1) {
            p.displayClientMessage(Component.literal("Sharingan: " + (active(p) ? "your eyes are open." : "your eyes are closed.") + " First stage. Its evolution is the Rinnegan.").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (instance.onCoolDown(0)) { p.displayClientMessage(Component.literal("Your eyes are tired (" + instance.getCoolDown(0) + "s).").withStyle(ChatFormatting.RED), true); return; }
        p.getPersistentData().putLong(UNTIL, p.serverLevel().getGameTime() + EYES_TICKS);
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EYES_TICKS, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EYES_TICKS, 0, false, false));
        instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(REST_TICKS), 0);
        instance.markDirty();
        p.displayClientMessage(Component.literal("Sharingan!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
    }

    @Override public boolean onHeld(ManasSkillInstance i, LivingEntity e, int heldTicks, int mode) { return true; }

    @Override public void onRelease(ManasSkillInstance i, LivingEntity e, int heldTicks, int keyNumber, int mode) { }

    static boolean active(ServerPlayer p) { return p.serverLevel().getGameTime() < p.getPersistentData().getLong(UNTIL); }

    /** While the eyes are open one blow in five (a melee hit or an arrow) is read and dodged. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (!(owner instanceof ServerPlayer p) || !active(p) || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        if ((source.getDirectEntity() instanceof LivingEntity || source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) && p.getRandom().nextDouble() < DODGE) amount.set(0f);
        return true;
    }

    // ---------------------------------------------------------------- the grant
    private static Skills skillsOf(ServerPlayer p) {
        try { return SkillAPI.getSkillsFrom(p); } catch (RuntimeException noSkills) { return null; }
    }

    public static boolean has(ServerPlayer p) {
        Skills s = skillsOf(p);
        return s != null && s.getSkill(NUSkills.SHARINGAN.getId()).isPresent();
    }

    /** Learns the skill once; false when it is already held or the player has no skill data. */
    public static boolean grant(ServerPlayer p) {
        Skills s = skillsOf(p);
        if (s == null || s.getSkill(NUSkills.SHARINGAN.getId()).isPresent()) return false;
        s.learnSkill(NUSkills.SHARINGAN.get().createDefaultInstance(), Component.literal("You tore out Kakashi's eye: the unique skill Sharingan is yours.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        return true;
    }
}
