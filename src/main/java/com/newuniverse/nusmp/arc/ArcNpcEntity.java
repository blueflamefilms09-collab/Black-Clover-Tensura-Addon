package com.newuniverse.nusmp.arc;

import com.newuniverse.nusmp.arc.dialogue.ArcDialogue;
import com.newuniverse.nusmp.arc.dialogue.DialogueContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Why: the friendly characters of the arcs (thirty of them) are ONE entity type, {@code nusmp:arc_npc}, whose identity is a synced string, exactly like a
 * villager's profession. Their look (a model of the art kit), their lines and their quests are data keyed by that id, so adding an NPC is a record in the
 * roster, a Python model spec and a dialogue file, never a new Java class, a new registration or a new renderer. This class is everything an NPC does in
 * the world: it persists and never despawns; it cannot be hurt (only {@code /kill}, the void and creative players can, unless {@code arc.npcsMortal}); it
 * stays around the post where it was placed ({@link #home}) and looks at players who come near; a right-click starts a conversation
 * ({@link ArcDialogue#talk}) during which it stands still, faces the player and shows the synced {@link #talking()} state the renderer plays its talking
 * clip for; its name is the character's name.
 */
public class ArcNpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> CHARACTER = SynchedEntityData.defineId(ArcNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> TALKING = SynchedEntityData.defineId(ArcNpcEntity.class, EntityDataSerializers.BOOLEAN);

    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    /** Ticks the talking state lasts after the last conversation; the farewell is spoken when it runs out. */
    public static final int TALK_TICKS = 80;
    /** A player further away than this (blocks) ends the conversation. */
    private static final double TALK_RANGE_SQR = 10.0 * 10.0;

    private BlockPos home;
    private boolean homeApplied;
    private int talkTicks;
    private UUID talkTarget;
    private long nextTalkAt;
    private final int[] lastLine = new int[DialogueContext.values().length];

    public ArcNpcEntity(EntityType<? extends ArcNpcEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        Arrays.fill(lastLine, -1);
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARACTER, "");
        builder.define(TALKING, false);
    }

    // ================================================================ who it is
    /** The roster id of the character this NPC is ("asta"), or empty for a blank one. */
    public String characterId() { return entityData.get(CHARACTER); }

    /** The character record, or null when the id is empty or not registered (an old world whose character was removed). */
    public ArcCharacter character() { return ArcRoster.byId(characterId()); }

    public void setCharacter(String id) { entityData.set(CHARACTER, id == null ? "" : id); }

    /** The character's name in its own colour (the name tag, the death message, the hover text); the entity type's name for a blank one. */
    @Override
    protected Component getTypeName() {
        ArcCharacter c = character();
        return c == null ? super.getTypeName() : Component.literal(c.name()).withStyle(Style.EMPTY.withColor(c.color() & 0xFFFFFF));
    }

    /** The renderer draws a name tag over a character that is known (the one nameplate of the NPC; a renderer must not add a second). */
    @Override
    public boolean shouldShowName() { return character() != null || super.shouldShowName(); }

    // ================================================================ the post
    /** The post it was placed at, or null before its first tick. */
    public BlockPos home() { return home; }

    /** Sets the post it stays around ({@code arc.npcWanderRadius} blocks; 0 = it stands on it). */
    public void setHome(BlockPos pos) {
        home = pos == null ? null : pos.immutable();
        homeApplied = false;
    }

    private void applyHome() {
        homeApplied = true;
        int r = ArcConfig.npcWander();
        restrictTo(home, Math.max(1, r));
    }

    // ================================================================ talking
    /** True while the character is in a conversation (synced: the renderer plays its talking clip). */
    public boolean talking() { return entityData.get(TALKING); }

    /** The index of the line this character said last in a topic (so it does not repeat it), or -1. */
    public int lastLine(DialogueContext c) { return lastLine[c.ordinal()]; }

    public void rememberLine(DialogueContext c, int index) { lastLine[c.ordinal()] = index; }

    /** A conversation with a player starts or goes on: the character stops, faces them and shows the talking state for {@link #TALK_TICKS}. */
    public void beginTalk(Player with) {
        talkTarget = with.getUUID();
        talkTicks = TALK_TICKS;
        entityData.set(TALKING, true);
        getNavigation().stop();
    }

    private void endTalk(boolean farewell) {
        UUID who = talkTarget;
        talkTicks = 0;
        talkTarget = null;
        entityData.set(TALKING, false);
        if (!farewell || who == null || !(level().getPlayerByUUID(who) instanceof ServerPlayer sp) || !sp.isAlive() || sp.distanceToSqr(this) >= 24 * 24) return;
        try {
            ArcDialogue.farewell(sp, this);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] {} could not say farewell: {}", characterId(), e.toString());
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer shopper && com.newuniverse.nusmp.skilltree.shop.ShopHook.tryOpen(shopper, this)) {      // 1.62.0: the Black Market: a sneaking right click on the old witch opens her shop (the hook answers false for every other character)
            return InteractionResult.CONSUME;
        }
        if (player instanceof ServerPlayer sp && level().getGameTime() >= nextTalkAt) {
            nextTalkAt = level().getGameTime() + ArcConfig.talkCooldown();
            try {
                ArcDialogue.talk(sp, this);
            } catch (RuntimeException e) {
                LOG.warn("[nusmp] a conversation with {} failed: {}", characterId(), e.toString());       // never out of an interaction handler: the player must not be kicked for a line of text
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (home == null) setHome(blockPosition());
        if (!homeApplied) applyHome();
        if (talkTicks > 0) {
            talkTicks--;
            Player target = talkTarget == null ? null : level().getPlayerByUUID(talkTarget);
            if (target == null || !target.isAlive() || target.distanceToSqr(this) > TALK_RANGE_SQR) endTalk(target != null && target.isAlive());
            else if (talkTicks == 0) endTalk(true);
        } else if (talking()) {
            entityData.set(TALKING, false);
        }
    }

    // ================================================================ staying alive and in place
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FaceTalkerGoal());
        goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.5, 0.004f) {
            @Override public boolean canUse() { return ArcConfig.npcWander() > 0 && super.canUse(); }
        });
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    /** While talking it does not walk off and keeps its eyes on the player. */
    private final class FaceTalkerGoal extends Goal {
        FaceTalkerGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override public boolean canUse() { return talkTicks > 0 && talkTarget != null; }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public void start() { getNavigation().stop(); }

        @Override public void tick() {
            Player t = talkTarget == null ? null : level().getPlayerByUUID(talkTarget);
            if (t != null) getLookControl().setLookAt(t, 30.0f, 30.0f);
        }
    }

    @Override public boolean removeWhenFarAway(double distanceSqr) { return false; }

    @Override public boolean canBeLeashed() { return false; }

    /** Nothing hurts a friendly character except {@code /kill}, the void and a creative player's hand, unless {@code arc.npcsMortal} is on. */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (!ArcConfig.npcsMortal() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.isCreativePlayer()) return true;
        return super.isInvulnerableTo(source);
    }

    /** No poison, no wither: harmful effects do not stick to a character that cannot be hurt. */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (!ArcConfig.npcsMortal() && effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) return false;
        return super.canBeAffected(effect);
    }

    // ================================================================ save data
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Character", characterId());
        if (home != null) tag.put("ArcHome", NbtUtils.writeBlockPos(home));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setCharacter(tag.getString("Character"));
        NbtUtils.readBlockPos(tag, "ArcHome").ifPresent(this::setHome);
    }
}
