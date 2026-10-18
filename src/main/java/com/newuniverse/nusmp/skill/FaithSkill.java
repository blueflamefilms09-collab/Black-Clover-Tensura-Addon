package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.FaithRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Faith: a Unique skill fuelled by devotion. Faith points are saved on the skill itself (tag "Faith", so admins can fix them with
 * /nusmp faith, and they carry over unchanged when the skill evolves into Goddess of Faith). Modes: Bestow (teach the player you look at one
 * random skill from the configured list, for Faith and a long wait), Pray (turn magicules into Faith) and Check Faith. The Ultimate
 * adds Blessing and Miracle. The arithmetic is in {@link FaithRules}.
 */
public class FaithSkill extends NUSkill {
    public static final String FAITH_KEY = FaithRules.FAITH_KEY;
    static final int BESTOW = 0, PRAY = 1, CHECK = 2;

    static final SkillText TEXT = SkillText.of("faith", "Faith",
            "Unique. Pray to gather Faith, then spend it to bestow a skill upon a player you look at. Faith is kept when this skill evolves.");

    public FaithSkill() { this(SkillType.UNIQUE, TEXT, false); }

    protected FaithSkill(SkillType type, SkillText text, boolean ultimate) { super(type, text, ultimate); }

    @Override public int getModes(ManasSkillInstance instance) { return 3; }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        int count = getModes(instance);
        return reverse ? (mode + count - 1) % count : (mode + 1) % count;
    }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) {
        return "faith." + switch (mode) { case BESTOW -> "bestow"; case PRAY -> "pray"; default -> "check"; };
    }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        return switch (mode) {
            case BESTOW -> SkillMsg.mode("faith.bestow", "Bestow");
            case PRAY -> SkillMsg.mode("faith.pray", "Pray");
            default -> SkillMsg.mode("faith.check", "Check Faith");
        };
    }

    public static int getFaith(ManasSkillInstance instance) {
        return FaithRules.clamp(instance.getOrCreateTag().getInt(FAITH_KEY));
    }

    public static void setFaith(ManasSkillInstance instance, int value) {
        instance.getOrCreateTag().putInt(FAITH_KEY, FaithRules.clamp(value));
        instance.markDirty();
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        switch (mode) {
            case BESTOW -> bestow(instance, player);
            case PRAY -> pray(instance, player);
            case CHECK -> check(instance, player);
            default -> { }
        }
    }

    protected final void check(ManasSkillInstance instance, ServerPlayer player) {
        int cost = FaithRules.bestowCost(NUConfig.BESTOW_FAITH_COST.get(), tier(instance, player));
        SkillUtil.actionbar(player, SkillMsg.t("faith.check", "Faith: %s (a bestowal costs %s)", getFaith(instance), cost)
                .withStyle(ChatFormatting.GOLD));
    }

    protected final void pray(ManasSkillInstance instance, ServerPlayer player) {
        if (instance.onCoolDown(PRAY)) {
            SkillUtil.fail(player, SkillMsg.t("faith.pray_cooldown", "You cannot pray again yet."));
            return;
        }
        if (!SkillUtil.spendMagicules(player, NUConfig.PRAY_MAGICULE_COST.get())) return;
        Tier tier = tier(instance, player);
        setFaith(instance, FaithRules.add(getFaith(instance), FaithRules.prayGain(tier)));
        cooldown(instance, PRAY, NUConfig.PRAY_COOLDOWN.get());
        SkillUtil.actionbar(player, SkillMsg.t("faith.prayed", "You pray. Faith: %s", getFaith(instance)).withStyle(ChatFormatting.GOLD));
        var level = player.serverLevel();
        SkillFx.spray(level, false, player.position().add(0, 2.1, 0), 12, 0.4, 0.1, 0.4, 0.01);
        SkillFx.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
        grow(instance, player, 0.5);
    }

    /** Bestow: the target must be a real player in view; the gift is one configured skill they do not own yet. */
    protected final void bestow(ManasSkillInstance instance, ServerPlayer player) {
        if (instance.onCoolDown(BESTOW)) {
            SkillUtil.fail(player, SkillMsg.t("faith.bestow_cooldown", "Bestow is on cooldown."));
            return;
        }
        Tier tier = tier(instance, player);
        int cost = FaithRules.bestowCost(NUConfig.BESTOW_FAITH_COST.get(), tier);
        int faith = getFaith(instance);
        if (!FaithRules.canSpend(faith, cost)) {
            SkillUtil.fail(player, SkillMsg.t("faith.not_enough", "Not enough Faith (need %s, have %s).", cost, faith));
            return;
        }
        ServerPlayer target = SkillUtil.lookTarget(player, tier == Tier.ULTIMATE ? 16.0 : 8.0);
        if (target == null) {
            SkillUtil.fail(player, SkillMsg.t("faith.no_target", "Look at a player to bestow."));
            return;
        }

        Skills targetSkills = SkillAPI.getSkillsFrom(target);
        List<String> pool = FaithRules.giftPool(new ArrayList<String>(NUConfig.BESTOWABLE_SKILLS.get()),
                id -> { ResourceLocation rl = ResourceLocation.tryParse(id); return rl != null && targetSkills.getSkill(rl).isPresent(); },
                id -> { ResourceLocation rl = ResourceLocation.tryParse(id); return rl != null && SkillAPI.getSkillRegistry().get(rl) != null; });
        if (pool.isEmpty()) {
            SkillUtil.fail(player, SkillMsg.t("faith.has_all", "%s already has every skill you can bestow.", target.getName()));
            return;
        }
        int pick = FaithRules.pickIndex(pool.size(), player.getRandom().nextInt());
        ManasSkill gift = SkillAPI.getSkillRegistry().get(ResourceLocation.parse(pool.get(pick)));
        if (gift == null) {
            SkillUtil.fail(player, SkillMsg.t("faith.failed", "The bestowal failed."));
            return;
        }
        boolean learned = targetSkills.learnSkill(gift.createDefaultInstance(),
                SkillMsg.t("faith.bestowed_by", "Bestowed by %s", player.getName()).withStyle(ChatFormatting.GOLD));
        if (!learned) {
            SkillUtil.fail(player, SkillMsg.t("faith.failed", "The bestowal failed."));
            return;
        }

        setFaith(instance, FaithRules.spend(faith, cost));
        cooldown(instance, BESTOW, FaithRules.bestowCooldown(NUConfig.BESTOW_COOLDOWN.get(), tier));
        SkillUtil.castVfx(player, 0xFFFFE08A);
        var level = player.serverLevel();
        Vec3 at = target.position().add(0, 1, 0);
        SkillFx.beam(level, player.getEyePosition(), at, false);
        SkillFx.spray(level, false, at.add(0, 1, 0), 12, 0.4, 0.5, 0.4, 0.02);
        SkillFx.sound(level, target.position(), SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.0F);
        grow(instance, player, 1);

        var server = player.getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(SkillMsg.t("voice", "[Voice of the World] ").withStyle(ChatFormatting.GOLD)
                    .append(SkillMsg.t("faith.bestowed", "%s has bestowed a skill upon %s.", player.getName(), target.getName())
                            .withStyle(ChatFormatting.WHITE)), false);
        }
    }
}
