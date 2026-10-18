package com.newuniverse.nusmp.elite.hub;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Why: the two Hub NPCs (the Quest Giver and the Merchant) are villager-style creatures that behave identically in everything but what a right click does, so this base class is everything they
 * share. They are {@code PathfinderMob}s (the shape of a villager and of the mod's {@code ArcNpcEntity}) that PERSIST and NEVER DESPAWN (persistence required, {@code removeWhenFarAway} false),
 * are INVULNERABLE to everything except the void, {@code /kill} and a creative player's hand (so a stray creeper cannot cost the Hub its shop), cannot be pushed off their post or leashed or
 * poisoned, FACE the player who comes near (a look goal with a high chance, and the head follows), and stay within two blocks of the {@code home} post they were placed at, which is saved. A
 * right click on the server asks the subclass to open its screen; on the client it just swings the hand. There is no profession, trade list or villager AI: the trading is the mod's own
 * screen, server-validated, and the entity is only the face that opens it. Their look is a villager model with their own texture (client: {@code HubNpcRenderer}).
 */
public abstract class HubNpcBase extends PathfinderMob {
    /** How far from its post the NPC may drift (blocks). */
    public static final int HOME_RADIUS = 2;

    private BlockPos home;
    private boolean homeApplied;
    private long nextUseAt;

    protected HubNpcBase(EntityType<? extends HubNpcBase> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 16.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /** What a right click does on the server: open this NPC's screen for the player. */
    protected abstract void open(ServerPlayer player);

    // ================================================================ the post
    public BlockPos home() { return home; }

    public void setHome(BlockPos pos) {
        home = pos == null ? null : pos.immutable();
        homeApplied = false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (home == null) setHome(blockPosition());
        if (!homeApplied) {
            homeApplied = true;
            restrictTo(home, HOME_RADIUS);
        }
    }

    // ================================================================ talking
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp && level().getGameTime() >= nextUseAt) {
            nextUseAt = level().getGameTime() + 10;
            getNavigation().stop();
            getLookControl().setLookAt(player, 30.0f, 30.0f);
            try {
                open(sp);
            } catch (RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().warn("[nusmp] a Hub NPC could not open its screen: {}", e.toString());       // never out of an interaction handler
            }
        }
        return InteractionResult.CONSUME;
    }

    // ================================================================ staying alive, in place and facing the player
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MoveTowardsRestrictionGoal(this, 0.6));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 12.0f, 1.0f));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override public boolean removeWhenFarAway(double distanceSqr) { return false; }

    @Override public boolean canBeLeashed() { return false; }

    @Override public boolean isPushable() { return false; }

    @Override public boolean shouldShowName() { return true; }

    /** Nothing hurts a Hub NPC except {@code /kill}, the void and a creative player's hand. */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.isCreativePlayer()) return true;
        return super.isInvulnerableTo(source);
    }

    /** No poison, no wither: harmful effects do not stick. */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) return false;
        return super.canBeAffected(effect);
    }

    // ================================================================ save data
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) tag.put("HubHome", NbtUtils.writeBlockPos(home));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "HubHome").ifPresent(this::setHome);
        setPersistenceRequired();
    }

    /** Opens a menu of this NPC for the player and sends its content (shared by the two subclasses). */
    protected void openMenu(ServerPlayer player, net.minecraft.world.inventory.MenuConstructor menu) {
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(menu, getDisplayName() == null ? Component.empty() : getDisplayName()), buf -> buf.writeVarInt(getId()));
    }
}
