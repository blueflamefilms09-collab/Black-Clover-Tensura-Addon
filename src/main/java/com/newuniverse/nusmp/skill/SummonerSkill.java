package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Summoner: a Unique skill inspired by Allen from Hell Mode.
 * Modes: Beast Card (tamed fighting wolf), Stone Card (guardian golem), Recall (dismiss all summons).
 */
public class SummonerSkill extends Skill {
    public static final String SUMMON_TAG = "nusmp_summon";
    public static final String OWNER_KEY = "nusmp_owner";

    private static final int BEAST = 0, STONE = 1, RECALL = 2, MODE_COUNT = 3;
    private static final ResourceLocation ICON =
            ResourceLocation.withDefaultNamespace("textures/item/enchanted_book.png");

    protected final boolean ultimate;

    public SummonerSkill() { this(SkillType.UNIQUE, false); }

    protected SummonerSkill(SkillType type, boolean ultimate) {
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
        return "summoner." + modeKey(mode);
    }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        return Component.translatable("nusmp.skill.mode.summoner." + modeKey(mode));
    }

    private static String modeKey(int mode) {
        return switch (mode) {
            case BEAST -> "beast";
            case STONE -> "stone";
            default -> "recall";
        };
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;

        if (mode == RECALL) {
            int removed = 0;
            for (Mob summon : ownedSummons(player)) {
                summon.discard();
                removed++;
            }
            SkillUtil.actionbar(player, Component.literal("Recalled " + removed + " summon(s).").withStyle(ChatFormatting.AQUA));
            return;
        }

        if (instance.onCoolDown(mode)) {
            SkillUtil.fail(player, "Summoner is on cooldown.");
            return;
        }

        boolean mastered = instance.isMastered(player);
        int max = (mastered ? NUConfig.SUMMON_MAX_MASTERED.get() : NUConfig.SUMMON_MAX.get()) * (ultimate ? 2 : 1);
        if (ownedSummons(player).size() >= max) {
            SkillUtil.fail(player, "You can only have " + max + " summons at once.");
            return;
        }
        if (!SkillUtil.spendMagicules(player, NUConfig.SUMMON_MAGICULE_COST.get())) return;

        int count = (ultimate && mode == BEAST) ? 3 : 1;
        for (int i = 0; i < count; i++) {
            Mob summon = mode == BEAST ? makeBeast(player, mastered) : makeGuardian(player, mastered);
            if (summon == null) return;
            if (ultimate) boost(summon, summon.getMaxHealth() * 2, damageOf(summon) * 2);

            Vec3 spot = player.position().add(player.getViewVector(1.0F).multiply(2, 0, 2)).add(i - (count - 1) / 2.0, 0, 0);
            summon.moveTo(spot.x, player.getY(), spot.z, player.getYRot(), 0.0F);
            summon.addTag(SUMMON_TAG);
            summon.getPersistentData().putUUID(OWNER_KEY, player.getUUID());
            summon.setPersistenceRequired();
            player.serverLevel().addFreshEntity(summon);
        }

        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 1.2F);
        instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(NUConfig.SUMMON_COOLDOWN.get()), mode);
        SkillUtil.castVfx(player, 0xFFB8A0FF);
        instance.addMasteryPoint(player, 1);
        instance.markDirty();
    }

    private static Wolf makeBeast(ServerPlayer player, boolean mastered) {
        Wolf wolf = EntityType.WOLF.create(player.serverLevel());
        if (wolf == null) return null;
        wolf.tame(player);
        wolf.setCustomName(Component.literal("Beast Card").withStyle(ChatFormatting.GOLD));
        boost(wolf, mastered ? 60 : 30, mastered ? 10 : 5);
        return wolf;
    }

    private static IronGolem makeGuardian(ServerPlayer player, boolean mastered) {
        IronGolem golem = EntityType.IRON_GOLEM.create(player.serverLevel());
        if (golem == null) return null;
        golem.setPlayerCreated(true); // will not attack players
        golem.setCustomName(Component.literal("Stone Card").withStyle(ChatFormatting.GRAY));
        boost(golem, mastered ? 200 : 100, mastered ? 20 : 15);
        return golem;
    }

    private static double damageOf(Mob mob) {
        var dmg = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        return dmg == null ? 0 : dmg.getBaseValue();
    }

    private static void boost(Mob mob, double health, double damage) {
        var hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(health);
        var dmg = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(damage);
        mob.setHealth(mob.getMaxHealth());
    }

    /** All living summons belonging to this player within 128 blocks. */
    public static List<Mob> ownedSummons(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        UUID id = player.getUUID();
        return level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(128),
                m -> m.getTags().contains(SUMMON_TAG)
                        && m.getPersistentData().hasUUID(OWNER_KEY)
                        && id.equals(m.getPersistentData().getUUID(OWNER_KEY)));
    }
}
