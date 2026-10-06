package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Faith: a Unique skill that bestows skills on others.
 * Faith points are saved on the skill itself, so admins can fix them with /nusmp faith.
 * Modes: Bestow (grant a skill to the player you look at), Pray (magicules -> faith), Check Faith.
 */
public class FaithSkill extends Skill {
    public static final String FAITH_KEY = "Faith";
    private static final int BESTOW = 0, PRAY = 1, CHECK = 2, MODE_COUNT = 3;
    private static final ResourceLocation ICON =
            ResourceLocation.withDefaultNamespace("textures/mob_effect/hero_of_the_village.png");

    protected final boolean ultimate;

    public FaithSkill() { this(SkillType.UNIQUE, false); }

    protected FaithSkill(SkillType type, boolean ultimate) {
        super(type);
        this.ultimate = ultimate;
    }

    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override public int getModes(ManasSkillInstance instance) { return MODE_COUNT; }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        return reverse ? (mode + MODE_COUNT - 1) % MODE_COUNT : (mode + 1) % MODE_COUNT;
    }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) {
        return "faith." + modeKey(mode);
    }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        return Component.translatable("nusmp.skill.mode.faith." + modeKey(mode));
    }

    private static String modeKey(int mode) {
        return switch (mode) {
            case BESTOW -> "bestow";
            case PRAY -> "pray";
            default -> "check";
        };
    }

    public static int getFaith(ManasSkillInstance instance) {
        return instance.getOrCreateTag().getInt(FAITH_KEY);
    }

    public static void setFaith(ManasSkillInstance instance, int value) {
        instance.getOrCreateTag().putInt(FAITH_KEY, Math.max(0, value));
        instance.markDirty();
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        switch (mode) {
            case BESTOW -> bestow(instance, player);
            case PRAY -> pray(instance, player);
            default -> SkillUtil.actionbar(player,
                    Component.literal("Faith: " + getFaith(instance)).withStyle(ChatFormatting.GOLD));
        }
    }

    private void pray(ManasSkillInstance instance, ServerPlayer player) {
        if (instance.onCoolDown(PRAY)) {
            SkillUtil.fail(player, "You cannot pray again yet.");
            return;
        }
        if (!SkillUtil.spendMagicules(player, NUConfig.PRAY_MAGICULE_COST.get())) return;
        setFaith(instance, getFaith(instance) + (ultimate ? 3 : 1));
        instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(NUConfig.PRAY_COOLDOWN.get()), PRAY);
        SkillUtil.actionbar(player, Component.literal("Faith: " + getFaith(instance)).withStyle(ChatFormatting.GOLD));
    }

    private void bestow(ManasSkillInstance instance, ServerPlayer player) {
        if (instance.onCoolDown(BESTOW)) {
            SkillUtil.fail(player, "Bestow is on cooldown.");
            return;
        }
        int cost = NUConfig.BESTOW_FAITH_COST.get() / (ultimate ? 2 : 1);
        if (getFaith(instance) < cost) {
            SkillUtil.fail(player, "Not enough Faith (need " + cost + ", have " + getFaith(instance) + ").");
            return;
        }
        ServerPlayer target = SkillUtil.lookTarget(player, 8.0);
        if (target == null) {
            SkillUtil.fail(player, "Look at a player within 8 blocks to bestow.");
            return;
        }

        Skills targetSkills = SkillAPI.getSkillsFrom(target);
        List<ManasSkill> options = new ArrayList<>();
        for (String id : NUConfig.BESTOWABLE_SKILLS.get()) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null || targetSkills.getSkill(rl).isPresent()) continue;
            ManasSkill skill = SkillAPI.getSkillRegistry().get(rl);
            if (skill != null) options.add(skill);
        }
        if (options.isEmpty()) {
            SkillUtil.fail(player, target.getName().getString() + " already has every skill you can bestow.");
            return;
        }

        Collections.shuffle(options);
        ManasSkill gift = options.get(0);
        boolean learned = targetSkills.learnSkill(gift.createDefaultInstance(),
                Component.literal("Bestowed by " + player.getName().getString()).withStyle(ChatFormatting.GOLD));
        if (!learned) {
            SkillUtil.fail(player, "The bestowal failed.");
            return;
        }

        setFaith(instance, getFaith(instance) - cost);
        instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(NUConfig.BESTOW_COOLDOWN.get() / (ultimate ? 2 : 1)), BESTOW);
        SkillUtil.castVfx(player, 0xFFFFE08A);
        instance.addMasteryPoint(player, 1);
        instance.markDirty();

        player.serverLevel().playSound(null, target.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.getServer().getPlayerList().broadcastSystemMessage(Component.literal("[Voice of the World] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(player.getName().getString() + " has bestowed a skill upon "
                        + target.getName().getString() + ".").withStyle(ChatFormatting.WHITE)), false);
    }
}
